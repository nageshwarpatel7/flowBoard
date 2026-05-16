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

    public record AuthUserResponse(Long id, String fullName, String username, String email) {
    }
}
