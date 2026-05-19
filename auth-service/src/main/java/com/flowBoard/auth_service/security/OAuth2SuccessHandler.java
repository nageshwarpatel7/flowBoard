package com.flowBoard.auth_service.security;

import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * FIX: renamed from OAuth2SucceessHandler (triple-e typo) to OAuth2SuccessHandler.
 *      Update the reference in SecurityConfig accordingly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "OAuth user not found after login: " + email));

        String token = jwtUtil.generateToken(
                user.getEmail(), user.getId(), user.getRole().name());

        log.info("OAuth2 login success: email={} userId={}", email, user.getId());

        String frontendUrl = "http://flowboard-frontend-nag.s3-website.ap-south-1.amazonaws.com/oauth2/callback?token=" + token
                + "&userId=" + user.getId()
                + "&role=" + user.getRole().name();

        response.sendRedirect(frontendUrl);
    }
}
