package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.Result;
import com.apartment.hub.dto.LoginDTO;
import com.apartment.hub.security.LoginUser;
import com.apartment.hub.service.SysUserService;
import com.apartment.hub.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final TokenService tokenService;

    @OperationLog(module = "Authentication", operation = "User Login")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(sysUserService.login(dto));
    }

    @OperationLog(module = "Authentication", operation = "User Logout")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            tokenService.blacklist(header.substring(7));
        }
        return Result.success();
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(sysUserService.getUserInfo(loginUser.getUserId()));
    }
}
