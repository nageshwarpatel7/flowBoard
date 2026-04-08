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
            String path = request.getURI().getPath();

            //--- skip JWT check for public routes -----
            if (isExcluded(path, config.getExcludedPaths())) {
                log.debug("Public routes - skipping JWT: {}", path);
                return chain.filter(exchange);
            }

            //--- check Authorization header exists ---
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("No Authorization header - blocking request to: {}", path);
                return reject(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return reject(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            // ---- validate the JWT -----
            String token = authHeader.substring(7);

            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid or expired JWT - blocking request to: {}", path);
                return reject(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            // ---- extract claims and forward as headers ----
            String email = jwtUtil.extractEmail(token);
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extraxctRole(token);

            log.debug("JWT valid -> email={} userId={} role={} path={}",
                    email, userId, role, path);


            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-User-Role", role)
                    .headers(h->h.remove(HttpHeaders.AUTHORIZATION))
                    .build();

            log.debug("JWT valid — email={} userId={} path={}", email, userId, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private boolean isExcluded(String path, String excludedPaths) {
        if (excludedPaths == null || excludedPaths.isBlank()) return false;
        List<String> excluded = Arrays.stream(excludedPaths.split(","))
                .map(String::trim).toList();
        return excluded.stream().anyMatch(path::equals);
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
        public String getExcludedPaths() { return excludedPaths; }
        public void setExcludedPaths(String excludedPaths) { this.excludedPaths = excludedPaths; }
    }
}