package com.apartment.hub.service;

import com.apartment.hub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    public void blacklist(String token) {
        try {
            long remaining = jwtUtil.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
            if (remaining > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", remaining, TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
