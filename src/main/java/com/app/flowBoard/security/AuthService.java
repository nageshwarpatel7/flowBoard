package com.app.flowBoard.security;

import com.app.flowBoard.dto.AuthResponse;
import com.app.flowBoard.dto.LoginRequest;
import com.app.flowBoard.dto.RegisterRequest;
import org.springframework.stereotype.Service;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
