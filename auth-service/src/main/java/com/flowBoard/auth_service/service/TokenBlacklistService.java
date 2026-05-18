package com.flowBoard.auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "blacklist:";

    public void blacklist(String token, long ttlSeconds){
        redisTemplate.opsForValue()
                .set(PREFIX+token, "revoked", ttlSeconds, TimeUnit.SECONDS);
        log.info("Token blacklisted, TTL={}s", ttlSeconds);
    }

    public boolean isBlacklisted(String token){
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(PREFIX+token));
    }
}
