package com.app.flowBoard.service;

import com.app.flowBoard.dto.AuthResponse;
import com.app.flowBoard.dto.LoginRequest;
import com.app.flowBoard.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
