package com.flowBoard.auth_service.service;

import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthUserService extends DefaultOAuth2UserService{
    private final UserRepository repository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();

        String email = extractEmail(oAuth2User, provider);
        String name = extractName(oAuth2User, provider);

        if(email==null || email.isBlank()){
            log.warn("OAuth2 login from {} returned no email", provider);
            throw new OAuth2AuthenticationException("No Email returned from OAuth2 provider: "+provider);
        }
        Optional<User> existingUser = repository.findByEmail(email);

        User user = existingUser.orElseGet(() -> {
            // First-time OAuth2 login — create a new local user account
            String baseUsername = email.split("@")[0];
            String uniqueUsername = makeUniqueUsername(baseUsername);

            User newUser = User.builder()
                    .fullName(name != null ? name : baseUsername)
                    .email(email)
                    .username(uniqueUsername)
                    .password("") // No password for OAuth users — login is via token only
                    .role(ROLE.MEMBER)
                    .provider(provider.toUpperCase())
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.save(newUser);
            log.info("New user created via OAuth2 provider={} email={}", provider, email);
            return newUser;
        });

        if(!user.isActive()){
            throw new OAuth2AuthenticationException("Account is deactivated");
        }

        if (user.getProvider() == null || user.getProvider().isBlank()) {
            user.setProvider(provider.toUpperCase());
            repository.save(user);
        }

        return oAuth2User;
    }

    private String extractEmail(OAuth2User user, String provider){
        if("github".equals(provider)){
            Object email = user.getAttribute("email");
            return email!=null? email.toString(): null;
        }
        return user.getAttribute("email");
    }
    private String extractName(OAuth2User user, String provider){
        if ("github".equals(provider)) {
            Object name = user.getAttribute("name");
            Object login = user.getAttribute("login"); // GitHub username as fallback
            return name != null ? name.toString() : (login != null ? login.toString() : null);
        }
        return user.getAttribute("name");
    }

    private String makeUniqueUsername(String base){
        String candidate = base;
        int suffix = 1;
        while(repository.existsByUsername(candidate)){
            candidate=base+suffix++;
        }
        return candidate;
    }
}
