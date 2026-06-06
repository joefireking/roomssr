package com.apartment.hub.aspect;

import com.apartment.hub.security.LoginUser;
import com.apartment.hub.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint pjp, OperationLog annotation) throws Throwable {
        long start = System.currentTimeMillis();
        String username = "anonymous";
        Long userId = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            username = loginUser.getUsername();
            userId = loginUser.getUserId();
        }

        String method = pjp.getSignature().toShortString();
        String params = serializeParams(pjp.getArgs());
        int status = 1;

        try {
            return pjp.proceed();
        } catch (Throwable t) {
            status = 0;
            throw t;
        } finally {
            try {
                long duration = System.currentTimeMillis() - start;
                com.apartment.hub.entity.OperationLog logEntry = new com.apartment.hub.entity.OperationLog();
                logEntry.setUserId(userId);
                logEntry.setUsername(username);
                logEntry.setModule(annotation.module());
                logEntry.setOperation(annotation.operation());
                logEntry.setMethod(method);
                logEntry.setParams(params.length() > 2000 ? params.substring(0, 2000) : params);
                logEntry.setIp(getClientIp());
                logEntry.setDuration(duration);
                logEntry.setStatus(status);
                operationLogService.save(logEntry);
            } catch (Exception e) {
                log.warn("Failed to save operation log", e);
            }
        }
    }

    private String serializeParams(Object[] args) {
        if (args == null || args.length == 0) return "{}";
        try {
            // Skip request/response objects
            Object[] filtered = java.util.Arrays.stream(args)
                    .filter(a -> !(a instanceof jakarta.servlet.http.HttpServletRequest)
                            && !(a instanceof jakarta.servlet.http.HttpServletResponse))
                    .toArray();
            if (filtered.length == 0) return "{}";
            if (filtered.length == 1) return objectMapper.writeValueAsString(filtered[0]);
            return objectMapper.writeValueAsString(filtered);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "";
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
            if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
            return ip != null ? ip.split(",")[0].trim() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
