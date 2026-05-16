package com.flowBoard.auth_service.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService Unit Tests")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("blacklist should store token in redis")
    void blacklist_success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.blacklist("token123", 3600);

        verify(valueOperations).set("blacklist:token123", "revoked", 3600, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("isBlacklisted should return true if token exists in redis")
    void isBlacklisted_true() {
        when(redisTemplate.hasKey("blacklist:token123")).thenReturn(true);

        boolean result = tokenBlacklistService.isBlacklisted("token123");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted should return false if token does not exist")
    void isBlacklisted_false() {
        when(redisTemplate.hasKey("blacklist:token123")).thenReturn(false);

        boolean result = tokenBlacklistService.isBlacklisted("token123");

        assertThat(result).isFalse();
    }
}
