package com.flowboard.api_gateway.filter;

import com.flowboard.api_gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public AuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // --- 1. ALLOW PRE-FLIGHT OPTIONS REQUESTS ---
            if (request.getMethod().name().equals("OPTIONS")) {
                return chain.filter(exchange);
            }

            String path = request.getURI().getPath();

            // --- 2. Skip JWT check for public routes ---
            // FIX: use prefix matching so paths like /api/v1/auth/login/ (trailing slash)
            //      or sub-paths of an excluded prefix are correctly allowed through.
            if (isExcluded(path, config.getExcludedPaths())) {
                log.debug("Public route - skipping JWT: {}", path);
                return chain.filter(exchange);
            }

            // --- 3. Check Authorization header exists ---
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("No Authorization header - blocking request to: {}", path);
                return reject(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return reject(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            // --- 4. Validate the JWT ---
            String token = authHeader.substring(7);

            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid or expired JWT - blocking request to: {}", path);
                return reject(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            // --- 5. Extract claims and forward as headers ---
            String email  = jwtUtil.extractEmail(token);
            Long   userId = jwtUtil.extractUserId(token);
            String role   = jwtUtil.extractRole(token);

            log.debug("JWT valid -> email={} userId={} role={} path={}", email, userId, role, path);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Id",    String.valueOf(userId))
                    .header("X-User-Role",  role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    /**
     * FIX: exact equality replaced with prefix matching.
     * An excluded path "/api/v1/auth/login" now also covers
     * "/api/v1/auth/login/" and any further sub-path.
     */
    private boolean isExcluded(String path, String excludedPaths) {
        if (excludedPaths == null || excludedPaths.isBlank()) return false;
        List<String> excluded = Arrays.stream(excludedPaths.split(","))
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .toList();
        return excluded.stream().anyMatch(e -> path.equals(e) || path.startsWith(e + "/"));
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"status":%d,"error":"%s","message":"%s"}
                """.formatted(status.value(), status.getReasonPhrase(), message);
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes()))
        );
    }

    public static class Config {
        private String excludedPaths;
        public String getExcludedPaths()                    { return excludedPaths; }
        public void   setExcludedPaths(String excludedPaths) { this.excludedPaths = excludedPaths; }
    }
}
