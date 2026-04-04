package com.flowBoard.auth_service.config;

import com.flowBoard.auth_service.security.JwtFilter;
import com.flowBoard.auth_service.security.OAuth2SucceessHandler;
import com.flowBoard.auth_service.service.OAuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Autowired(required = false)
    private OAuthUserService oAuthUserService;

    @Autowired(required = false)
    private OAuth2SucceessHandler oAuth2SucceessHandler;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/validate",
                                "/api/v1/auth/verify-email",        // new
                                "/api/v1/auth/resend-verification", // new
                                "/api/v1/auth/forgot-password",     // new
                                "/api/v1/auth/reset-password",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/auth/admin/**").authenticated()
                        .anyRequest().authenticated()
                );

        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(ui -> ui.userService(oAuthUserService))
                    .successHandler(oAuth2SucceessHandler)
            );
        }

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}