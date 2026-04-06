package com.flowboard.api_gateway.filter;

import com.flowboard.api_gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    public AuthFilter(){
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config){
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if(isExcluded(path, config.getExcludedPaths())){
                log.debug("Path {} is public - skipping JWT check", path);
                return chain.filter(exchange);
            }

            if(!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                log.warn("Missing Authorization header for path: {}", path);
                return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            String token = authHeader.substring(7);
            if(!jwtUtil.isTokenValid(token)){
                log.warn("Invalid JWT token for path: {}", path);
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            String email = jwtUtil.extractEmail(token);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .build();

            log.debug("JWT valid for {} - forwording to downstream", email);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private boolean isExcluded(String path, String excludedPaths){
        if(excludedPaths==null || excludedPaths.isBlank()) return false;

        List<String> excluded = Arrays.stream(excludedPaths.split(","))
                .map(String::trim).toList();

        return excluded.stream().anyMatch(path::equals);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status){
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

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
