package com.flowBoard.auth_service.security;

import com.flowBoard.auth_service.dto.AuthResponse;
import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.repository.UserRepository;
import com.flowBoard.auth_service.service.AuthService;
import com.flowBoard.auth_service.service.OAuthUserService;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SucceessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException{
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("OAuth user not found after login: "+email));

        String token = jwtUtil.generateToken(user.getEmail());

        AuthResponse authResponse = new AuthResponse("OAuth2 login successful", token);

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), authResponse);
    }
}
