package com.flowboard.workspace_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthUserClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${auth.service.base-url:http://auth-service:8081/api/v1/auth}")
    private String authBaseUrl;

    public Optional<Long> findUserIdByEmail(String email) {
        try {
            AuthUserResponse user = restClientBuilder.build()
                    .get()
                    .uri(authBaseUrl + "/internal/users/by-email?email={email}", email)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    })
                    .body(AuthUserResponse.class);

            return user == null || user.id() == null
                    ? Optional.empty()
                    : Optional.of(user.id());
        } catch (Exception ex) {
            log.warn("Could not resolve invitee email {} to a user id: {}", email, ex.getMessage());
            return Optional.empty();
        }
    }

    public java.util.Map<Long, AuthUserResponse> findUsersByIds(java.util.List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Map.of();
        }
        try {
            AuthUserResponse[] users = restClientBuilder.build()
                    .post()
                    .uri(authBaseUrl + "/internal/users/by-ids")
                    .body(ids)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    })
                    .body(AuthUserResponse[].class);

            if (users == null) return java.util.Map.of();
            return java.util.Arrays.stream(users).collect(java.util.stream.Collectors.toMap(AuthUserResponse::id, u -> u));
        } catch (Exception ex) {
            log.warn("Could not fetch users by ids: {}", ex.getMessage());
            return java.util.Map.of();
        }
    }

    public record AuthUserResponse(Long id, String fullName, String username, String email, String avatarUrl) {
    }
}
