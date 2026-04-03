package com.flowBoard.auth_service.service;

import com.flowBoard.auth_service.dto.*;
import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.exception.CustomException;
import com.flowBoard.auth_service.repository.UserRepository;
import com.flowBoard.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already exists", HttpStatus.BAD_REQUEST);
        }
        if (repository.existsByUsername(request.getUsername())) {
            throw new CustomException("Username already taken", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(ROLE.MEMBER)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse("User registered successfully", token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("Invalid email", HttpStatus.NOT_FOUND));

        // Check account is active before verifying password
        if (!user.isActive()) {
            throw new CustomException("Account is deactivated", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException("Invalid password", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse("Login successful", token);
    }

    @Override
    public String validateToken(String token) {
        try {
            String email = jwtUtil.extractEmail(token);
            return "Valid token for user: " + email;
        } catch (Exception e) {
            throw new CustomException("Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public String refreshToken(String token) {
        if (!jwtUtil.isTokenValid(token)) {
            throw new CustomException("Token is invalid or expired", HttpStatus.UNAUTHORIZED);
        }
        try {
            String email = jwtUtil.extractEmail(token);
            // Verify user still exists and is active before issuing new token
            User user = repository.findByEmail(email)
                    .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
            if (!user.isActive()) {
                throw new CustomException("Account is deactivated", HttpStatus.FORBIDDEN);
            }
            return jwtUtil.generateToken(email);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Cannot refresh token", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public User getUserByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public User getUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);
        user.setFullName(request.getFullname());
        user.setUsername(request.getUsername());
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        repository.save(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new CustomException("Old password is incorrect", HttpStatus.BAD_REQUEST);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
    }

    // logout: stateless JWT has no server-side session to clear.
    // Real blacklisting requires a Redis store — adding a simple log for now.
    // To fully implement: store token in a Redis blacklist with TTL = token expiry.
    @Override
    public void logout(String token) {
        log.info("User logged out. Token invalidation requires a Redis blacklist for full security.");
    }

    @Override
    public void deactivateAccount(Long id) {
        User user = getUserById(id);
        user.setActive(false);
        repository.save(user);
        log.info("Account deactivated for userId: {}", id);
    }

    @Override
    public List<User> searchUsers(String key) {
        // Searches by full name — matches the repository method searchByFullName
        return repository.searchByNameOrUsername(key);
    }

    @Override
    public List<User> getAllUsers(){
        return repository.findAll();
    }

    @Override
    public List<User> getUsersByRole(ROLE role){
        return repository.findAllByRole(role);
    }

    @Override
    public void suspendUser(Long id){
        User user = getUserById(id);
        if(!user.isActive()){
            throw new CustomException("User is already suspended", HttpStatus.BAD_REQUEST);
        }
        user.setActive(false);
        repository.save(user);
        log.info("User suspended by admin: userId={}", id);
    }

    @Override
    public void reactivateUser(Long id) {
        User user = getUserById(id);
        if (user.isActive()) {
            throw new CustomException("User is already active", HttpStatus.BAD_REQUEST);
        }
        user.setActive(true);
        repository.save(user);
        log.info("User reactivated by admin: userId={}", id);
    }

    // Permanent hard delete — case study §2.4
    @Override
    public void deleteUser(Long id) {
        if (!repository.existsById(id)) {
            throw new CustomException("User not found", HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
        log.info("User permanently deleted by admin: userId={}", id);
    }
}