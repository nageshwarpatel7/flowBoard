package com.flowboard.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    // Rate limit per user — uses X-User-Id header
    // Falls back to IP if header not present (public endpoints)
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            // For unauthenticated requests (login/register) — limit by IP
            String ip = exchange.getRequest()
                    .getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress()
                    .getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    // Default rate limiter: 20 requests per second, burst up to 30
    @Bean
    @Primary
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(20, 30, 1);
    }

    // Strict rate limiter for auth endpoints: 5 requests per second
    // Prevents brute-force on login
    @Bean("authRateLimiter")
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }
}