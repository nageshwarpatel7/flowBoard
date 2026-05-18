package com.flowBoard.auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowBoard.auth_service.dto.*;
import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.exception.CustomException;
import com.flowBoard.auth_service.repository.UserRepository;
import com.flowBoard.auth_service.security.JwtUtil;
import com.flowBoard.auth_service.service.AuthService;
import com.flowBoard.auth_service.service.TokenBlacklistService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AuthController – MockMvc Tests")
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean  AuthService authService;
    @MockBean  JwtUtil jwtUtil;
    @MockBean  UserRepository userRepository;
    @MockBean  TokenBlacklistService tokenBlacklistService;

    private static final String BASE = "/api/v1/auth";

    private User sampleUser() {
        return User.builder().id(1L).fullName("Nageshwar Patel")
                .email("nageshwar@gmail.com").username("nageshwar")
                .role(ROLE.MEMBER).active(true).emailVerified(true)
                .createdAt(LocalDateTime.now()).build();
    }

    // ── POST /register ────────────────────────────────────────────────────────
    @Test @DisplayName("POST /register → 200 success + OTP message")
    void register_200() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Nageshwar"); req.setEmail("n@g.com");
        req.setUsername("nag"); req.setPassword("Pass@123");
        when(authService.register(any())).thenReturn(
                new AuthResponse("Registration successful! Please verify email.", null));

        mvc.perform(post(BASE + "/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful! Please verify email."));
        verify(authService).register(any());
    }

    @Test @DisplayName("POST /register → 400 duplicate email")
    void register_duplicateEmail_400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("X"); req.setEmail("dup@g.com"); req.setUsername("x"); req.setPassword("P@1");
        when(authService.register(any())).thenThrow(new CustomException("Email already exists", HttpStatus.BAD_REQUEST));

        mvc.perform(post(BASE + "/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("POST /register → 400 duplicate username")
    void register_duplicateUsername_400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("X"); req.setEmail("x@g.com"); req.setUsername("taken"); req.setPassword("P@1");
        when(authService.register(any())).thenThrow(new CustomException("Username already taken", HttpStatus.BAD_REQUEST));

        mvc.perform(post(BASE + "/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /login ───────────────────────────────────────────────────────────
    @Test @DisplayName("POST /login → 200 returns JWT")
    void login_200() throws Exception {
        LoginRequest req = new LoginRequest(); req.setEmail("n@g.com"); req.setPassword("Pass@123");
        when(authService.login(any())).thenReturn(new AuthResponse("Login successful", "jwt.token.here"));

        mvc.perform(post(BASE + "/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"));
    }

    @Test @DisplayName("POST /login → 401 wrong password")
    void login_wrongPassword_401() throws Exception {
        LoginRequest req = new LoginRequest(); req.setEmail("n@g.com"); req.setPassword("bad");
        when(authService.login(any())).thenThrow(new CustomException("Invalid password", HttpStatus.UNAUTHORIZED));

        mvc.perform(post(BASE + "/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("POST /login → 403 email not verified")
    void login_unverified_403() throws Exception {
        LoginRequest req = new LoginRequest(); req.setEmail("u@g.com"); req.setPassword("P@1");
        when(authService.login(any())).thenThrow(new CustomException("Email not verified", HttpStatus.FORBIDDEN));

        mvc.perform(post(BASE + "/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("POST /login → 403 account inactive")
    void login_inactive_403() throws Exception {
        LoginRequest req = new LoginRequest(); req.setEmail("s@g.com"); req.setPassword("P@1");
        when(authService.login(any())).thenThrow(new CustomException("Account suspended", HttpStatus.FORBIDDEN));

        mvc.perform(post(BASE + "/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("POST /login → 404 email not found")
    void login_notFound_404() throws Exception {
        LoginRequest req = new LoginRequest(); req.setEmail("nobody@g.com"); req.setPassword("P@1");
        when(authService.login(any())).thenThrow(new CustomException("User not found", HttpStatus.NOT_FOUND));

        mvc.perform(post(BASE + "/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── POST /logout ──────────────────────────────────────────────────────────
    @Test @WithMockUser @DisplayName("POST /logout → 200 blacklists token")
    void logout_200() throws Exception {
        doNothing().when(authService).logout("jwt.token");

        mvc.perform(post(BASE + "/logout").with(csrf())
                        .header("Authorization", "Bearer jwt.token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));
        verify(authService).logout("jwt.token");
    }

    // ── POST /verify-email ────────────────────────────────────────────────────
    @Test @DisplayName("POST /verify-email → 200 valid OTP")
    void verifyEmail_200() throws Exception {
        VerifyOtpRequest req = new VerifyOtpRequest(); req.setEmail("n@g.com"); req.setOtp("123456");
        doNothing().when(authService).verifyEmail(anyString(), anyString());

        mvc.perform(post(BASE + "/verify-email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Email verified successfully"));
    }

    @Test @DisplayName("POST /verify-email → 400 invalid OTP")
    void verifyEmail_badOtp_400() throws Exception {
        VerifyOtpRequest req = new VerifyOtpRequest(); req.setEmail("n@g.com"); req.setOtp("000000");
        doThrow(new CustomException("Invalid OTP", HttpStatus.BAD_REQUEST))
                .when(authService).verifyEmail(anyString(), anyString());

        mvc.perform(post(BASE + "/verify-email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /forgot-password ─────────────────────────────────────────────────
    @Test @DisplayName("POST /forgot-password → 200")
    void forgotPassword_200() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest(); req.setEmail("n@g.com");
        doNothing().when(authService).sendForgotPasswordOtp(anyString());

        mvc.perform(post(BASE + "/forgot-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP sent"));
    }

    // ── POST /reset-password ──────────────────────────────────────────────────
    @Test @DisplayName("POST /reset-password → 200")
    void resetPassword_200() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("n@g.com"); req.setOtp("123456"); req.setNewPassword("New@1234");
        doNothing().when(authService).resetPassword(any());

        mvc.perform(post(BASE + "/reset-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset successfully"));
    }

    // ── GET /validate ─────────────────────────────────────────────────────────
    @Test @DisplayName("GET /validate → 200 valid token")
    void validateToken_200() throws Exception {
        when(authService.validateToken("valid.jwt")).thenReturn("n@g.com");

        mvc.perform(get(BASE + "/validate").param("token", "valid.jwt"))
                .andExpect(status().isOk())
                .andExpect(content().string("n@g.com"));
    }

    // ── POST /refresh ─────────────────────────────────────────────────────────
    @Test @DisplayName("POST /refresh → 200 new token")
    void refreshToken_200() throws Exception {
        TokenRefreshRequest req = new TokenRefreshRequest(); req.setRefreshToken("old.jwt");
        when(authService.refreshToken("old.jwt")).thenReturn("new.jwt");

        mvc.perform(post(BASE + "/refresh").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("new.jwt"));
    }

    @Test @DisplayName("POST /refresh → 401 expired token")
    void refreshToken_expired_401() throws Exception {
        TokenRefreshRequest req = new TokenRefreshRequest(); req.setRefreshToken("expired");
        when(authService.refreshToken("expired")).thenThrow(new CustomException("Token expired", HttpStatus.UNAUTHORIZED));

        mvc.perform(post(BASE + "/refresh").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /search ───────────────────────────────────────────────────────────
    @Test @WithMockUser @DisplayName("GET /search → 200 with matches")
    void search_200() throws Exception {
        when(authService.searchUsers("nag")).thenReturn(List.of(sampleUser()));

        mvc.perform(get(BASE + "/search").param("key", "nag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("nageshwar@gmail.com"));
    }

    @Test @WithMockUser @DisplayName("GET /search → 200 empty result")
    void search_empty() throws Exception {
        when(authService.searchUsers("xyz")).thenReturn(List.of());

        mvc.perform(get(BASE + "/search").param("key", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── POST /resend-verification ─────────────────────────────────────────────
    @Test @DisplayName("POST /resend-verification → 200")
    void resendVerification_200() throws Exception {
        doNothing().when(authService).sendVerificationOtp(anyString());

        mvc.perform(post(BASE + "/resend-verification").with(csrf())
                        .param("email", "n@g.com"))
                .andExpect(status().isOk());
    }

    // ── POST /reactivate-request ──────────────────────────────────────────────
    @Test @DisplayName("POST /reactivate-request → 200")
    void sendReactivationOtp_200() throws Exception {
        doNothing().when(authService).sendReactivationOtp(anyString());

        mvc.perform(post(BASE + "/reactivate-request").with(csrf())
                        .param("email", "n@g.com"))
                .andExpect(status().isOk());
    }

    // ── POST /reactivate-verify ───────────────────────────────────────────────
    @Test @DisplayName("POST /reactivate-verify → 200")
    void reactivateVerify_200() throws Exception {
        VerifyOtpRequest req = new VerifyOtpRequest(); req.setEmail("n@g.com"); req.setOtp("123456");
        doNothing().when(authService).reactivateWithOtp(anyString(), anyString());

        mvc.perform(post(BASE + "/reactivate-verify").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Account successfully reactivated. You can now log in."));
    }

    // ── ADMIN ENDPOINTS ───────────────────────────────────────────────────────
    @Test @WithMockUser(authorities = "PLATFORM_ADMIN")
    @DisplayName("GET /admin/users → 200 for admin")
    void getAllUsers_admin_200() throws Exception {
        when(authService.getAllUsers()).thenReturn(List.of(sampleUser()));

        mvc.perform(get(BASE + "/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("nageshwar@gmail.com"));
    }

/* 
    @Test @WithMockUser(authorities = "MEMBER")
    @DisplayName("GET /admin/users → 403 for non-admin")
    void getAllUsers_member_403() throws Exception {
        mvc.perform(get(BASE + "/admin/users")).andExpect(status().isForbidden());
    }
*/

    @Test @WithMockUser(authorities = "PLATFORM_ADMIN")
    @DisplayName("GET /admin/stats → 200")
    void getAdminStats_200() throws Exception {
        when(authService.getAdminStats()).thenReturn(new AdminStatsResponse(100L, 10L, 5L, 3L));

        mvc.perform(get(BASE + "/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100));
    }

    @Test @WithMockUser(authorities = "PLATFORM_ADMIN")
    @DisplayName("GET /admin/users/role/{role} → 200")
    void getUsersByRole_200() throws Exception {
        when(authService.getUsersByRole(ROLE.MEMBER)).thenReturn(List.of(sampleUser()));

        mvc.perform(get(BASE + "/admin/users/role/MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("nageshwar@gmail.com"));
    }

    @Test @WithMockUser(authorities = "PLATFORM_ADMIN")
    @DisplayName("PUT /admin/users/{id}/role → 200")
    void updateUserRole_200() throws Exception {
        doNothing().when(authService).updateUserRole(1L, ROLE.PLATFORM_ADMIN);

        mvc.perform(put(BASE + "/admin/users/1/role").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("role", "PLATFORM_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(authorities = "PLATFORM_ADMIN")
    @DisplayName("PUT /admin/users/{id}/suspend → 200")
    void suspendUser_200() throws Exception {
        doNothing().when(authService).suspendUser(1L);
        mvc.perform(put(BASE + "/admin/users/1/suspend").with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string("User suspended"));
    }

    @Test @WithMockUser(authorities = "PLATFORM_ADMIN")
    @DisplayName("PUT /admin/users/{id}/suspend → 400 already suspended")
    void suspendUser_alreadySuspended_400() throws Exception {
        doThrow(new CustomException("Already suspended", HttpStatus.BAD_REQUEST)).when(authService).suspendUser(2L);
        mvc.perform(put(BASE + "/admin/users/2/suspend").with(csrf())).andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(authorities = "PLATFORM_ADMIN")
    @DisplayName("PUT /admin/users/{id}/reactivate → 200")
    void reactivateUser_200() throws Exception {
        doNothing().when(authService).reactivateUser(1L);
        mvc.perform(put(BASE + "/admin/users/1/reactivate").with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string("User reactivated"));
    }

    @Test @WithMockUser(authorities = "PLATFORM_ADMIN")
    @DisplayName("DELETE /admin/users/{id} → 200")
    void deleteUser_200() throws Exception {
        doNothing().when(authService).deleteUser(1L);
        mvc.perform(delete(BASE + "/admin/users/1").with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string("User permanently deleted"));
    }

    @Test @WithMockUser(authorities = "PLATFORM_ADMIN")
    @DisplayName("DELETE /admin/users/{id} → 404 not found")
    void deleteUser_notFound_404() throws Exception {
        doThrow(new CustomException("User not found", HttpStatus.NOT_FOUND)).when(authService).deleteUser(999L);
        mvc.perform(delete(BASE + "/admin/users/999").with(csrf())).andExpect(status().isNotFound());
    }
}