package com.flowBoard.auth_service.service;

import com.flowBoard.auth_service.dto.*;
import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;

import java.util.List;

public interface AuthService {

    //Auth
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    String validateToken(String token);
    String refreshToken(String token);
    void logout(String token);

    //Profile
    User getUserByEmail(String email);
    User getUserById(Long id);
    void updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    void deactivateAccount(Long id);

    //search
    List<User> searchUsers(String key);

    //Admin operations - case study
    List<User> getAllUsers();
    List<User> getUsersByRole(ROLE role);
    void suspendUser(Long id);
    void reactivateUser(Long id);
    void deleteUser(Long id);
}
