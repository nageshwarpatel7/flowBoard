package com.flowBoard.auth_service.controller;

import com.flowBoard.auth_service.dto.*;
import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, otp, profile")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Register new user", description = "Creates a new account and sends OTP")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate email")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request){
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Login user", description = "Authenticate user and return JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Email not verified or account inactive")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Logout user", description = "Invalidate JWT token")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Parameter(description = "Bearer token", required = true)
            @RequestHeader("Authorization") String authHeader){
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestParam String token){
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshToken(@Valid @RequestBody TokenRefreshRequest request){
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @Operation(summary = "Get user profile")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(
            @AuthenticationPrincipal User user){
        User found = authService.getUserById(user.getId());
        return ResponseEntity.ok(toProfileDto(found));
    }

    @Operation(summary = "Update user profile")
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request){
        authService.updateProfile(user.getId(), request);
        return ResponseEntity.ok("Profile updated successfully");
    }

    @Operation(summary = "Change user password")
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String key){
        return ResponseEntity.ok(authService.searchUsers(key));
    }

    @DeleteMapping("/deactivate")
    public ResponseEntity<String> deactivate(@AuthenticationPrincipal User user){
        authService.deactivateAccount(user.getId());
        return ResponseEntity.ok("Account deactivated");
    }

    @Operation(summary = "Get all users (Admin only)")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<List<User>> getAllUsers(){
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<AdminStatsResponse> getAdminStats() {
        return ResponseEntity.ok(authService.getAdminStats());
    }

    @GetMapping("/admin/users/role/{role}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable ROLE role){
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }

    @PutMapping("/admin/users/{id}/role")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<String> updateUserRole(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload) {
        ROLE role = ROLE.valueOf(payload.get("role"));
        authService.updateUserRole(id, role);
        return ResponseEntity.ok("User role updated to " + role);
    }

    @PutMapping("/admin/users/{id}/suspend")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<String> suspendUser(@PathVariable Long id){
        authService.suspendUser(id);
        return ResponseEntity.ok("User suspended");
    }

    @PutMapping("/admin/users/{id}/reactivate")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<String> reactivateUser(@PathVariable Long id){
        authService.reactivateUser(id);
        return ResponseEntity.ok("User reactivated");
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id){
        authService.deleteUser(id);
        return ResponseEntity.ok("User permanently deleted");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerifiation(@RequestParam @Email @NotBlank String email){
        authService.sendVerificationOtp(email);
        return ResponseEntity.ok("Verification OTP send to "+email);
    }

    @Operation(summary = "Verify email using OTP")
    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @Valid @RequestBody VerifyOtpRequest request){
        authService.verifyEmail(request.getEmail(), request.getOtp());
        return ResponseEntity.ok("Email verified successfully");
    }

    @Operation(summary = "Send OTP for password reset")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request){
        authService.sendForgotPasswordOtp(request.getEmail());
        return ResponseEntity.ok("OTP sent");
    }

    @Operation(summary = "Reset password using OTP")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request){
        authService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }

    @PostMapping("/reactivate-request")
    public ResponseEntity<String> sendReactivationOtp(@RequestParam String email) {
        authService.sendReactivationOtp(email);
        return ResponseEntity.ok("Reactivation OTP sent to " + email);
    }

    @PostMapping("/reactivate-verify")
    public ResponseEntity<String> reactivateAccount(
            @Valid @RequestBody VerifyOtpRequest request) {
        authService.reactivateWithOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok("Account successfully reactivated. You can now log in.");
    }


    private UserProfileDto toProfileDto(User user){
        return new UserProfileDto(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
