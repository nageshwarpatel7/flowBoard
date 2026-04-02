package com.app.flowBoard.service;

import com.app.flowBoard.dto.AuthResponse;
import com.app.flowBoard.dto.LoginRequest;
import com.app.flowBoard.dto.RegisterRequest;
import com.app.flowBoard.entity.ROLE;
import com.app.flowBoard.entity.User;
import com.app.flowBoard.exception.CustomException;
import com.app.flowBoard.repository.UserRepository;
import com.app.flowBoard.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request){

        log.info("Register request received from email: {}", request.getEmail());
        if(userRepository.existsByEmail(request.getEmail())){
            throw new CustomException("Email already exists", HttpStatus.BAD_REQUEST);
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(ROLE.MEMBER)
                .isActive(true) // ✅ THIS IS THE FIX
                .build();
        userRepository.save(user);

        log.info("User registered successfully with email: {}", user.getEmail());
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse("User registered successfully", token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        log.info("Login attempt for email: {}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("Invalid email", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.error("Login failed: Wrong password for email -> {}", user.getEmail());
            throw new CustomException("Invalid password", HttpStatus.UNAUTHORIZED);
        }

        log.info("Login successful for email: {}", request.getEmail());
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse("Login successful", token);
    }
}
