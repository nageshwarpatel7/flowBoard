package com.flowBoard.auth_service.service;

import com.flowBoard.auth_service.dto.*;
import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.exception.CustomException;
import com.flowBoard.auth_service.repository.UserRepository;
import com.flowBoard.auth_service.security.JwtUtil;
import com.flowBoard.auth_service.security.OtpService;
import com.flowBoard.auth_service.service.EmailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl – full coverage suite")
class AuthServiceImplTest {

    @Mock UserRepository      repository;
    @Mock PasswordEncoder     passwordEncoder;
    @Mock JwtUtil             jwtUtil;
    @Mock OtpService          otpService;
    @Mock TokenBlacklistService blacklistService;
    @Mock EmailService        emailService;

    @InjectMocks AuthServiceImpl authService;

    private User verifiedUser, unverifiedUser, inactiveUser;

    @BeforeEach
    void setUp() {
        verifiedUser = User.builder()
                .id(1L).fullName("Nageshwar Patel")
                .email("nageshwar@gmail.com").username("nageshwar")
                .password("hashed_pw").role(ROLE.MEMBER)
                .active(true).emailVerified(true)
                .createdAt(LocalDateTime.now()).build();

        unverifiedUser = User.builder()
                .id(2L).email("unverified@gmail.com").username("unverified")
                .active(true).emailVerified(false).build();

        inactiveUser = User.builder()
                .id(3L).email("inactive@gmail.com").username("inactive")
                .active(false).emailVerified(true).build();
    }

    // ── register ───────────────────────────────────────────────────────────────

    @Nested @DisplayName("register()")
    class RegisterTests {

        @Test @DisplayName("success – saves user and sends OTP, no token returned")
        void register_success() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("Test"); req.setEmail("t@g.com");
            req.setUsername("tuser"); req.setPassword("pw");

            when(repository.existsByEmail(anyString())).thenReturn(false);
            when(repository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(repository.save(any())).thenReturn(verifiedUser);

            AuthResponse r = authService.register(req);
            assertThat(r.getMessage()).contains("Registration successful");
            assertThat(r.getToken()).isNull();
            verify(otpService).sendVerificationOtp("t@g.com");
        }

        @Test @DisplayName("duplicate email – throws 400, user never saved")
        void register_duplicateEmail() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("nageshwar@gmail.com"); req.setUsername("other");
            when(repository.existsByEmail(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Email already exists");
            verify(repository, never()).save(any());
        }

        @Test @DisplayName("duplicate username – throws 400")
        void register_duplicateUsername() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@g.com"); req.setUsername("nageshwar");
            when(repository.existsByEmail(anyString())).thenReturn(false);
            when(repository.existsByUsername(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Username already taken");
        }
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Nested @DisplayName("login()")
    class LoginTests {

        @Test @DisplayName("success – returns JWT")
        void login_success() {
            LoginRequest req = new LoginRequest();
            req.setEmail(verifiedUser.getEmail()); req.setPassword("pass");

            when(repository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt");

            AuthResponse r = authService.login(req);
            assertThat(r.getToken()).isEqualTo("jwt");
        }

        @Test @DisplayName("email not found – throws 404")
        void login_emailNotFound() {
            LoginRequest req = new LoginRequest();
            req.setEmail("x@x.com"); req.setPassword("p");
            when(repository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException)e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test @DisplayName("inactive account – throws 403")
        void login_inactive() {
            LoginRequest req = new LoginRequest();
            req.setEmail(inactiveUser.getEmail()); req.setPassword("p");
            when(repository.findByEmail(anyString())).thenReturn(Optional.of(inactiveUser));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException)e).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test @DisplayName("unverified email – throws 403 and resends OTP when none active")
        void login_unverified_resends() {
            LoginRequest req = new LoginRequest();
            req.setEmail(unverifiedUser.getEmail()); req.setPassword("p");
            when(repository.findByEmail(anyString())).thenReturn(Optional.of(unverifiedUser));
            when(otpService.hasActiveOtp(anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException)e).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            verify(otpService).sendVerificationOtp(unverifiedUser.getEmail());
        }

        @Test @DisplayName("unverified but OTP active – throws 403, does NOT resend")
        void login_unverified_otpActive() {
            LoginRequest req = new LoginRequest();
            req.setEmail(unverifiedUser.getEmail()); req.setPassword("p");
            when(repository.findByEmail(anyString())).thenReturn(Optional.of(unverifiedUser));
            when(otpService.hasActiveOtp(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException)e).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            verify(otpService, never()).sendVerificationOtp(anyString());
        }

        @Test @DisplayName("wrong password – throws 401")
        void login_wrongPassword() {
            LoginRequest req = new LoginRequest();
            req.setEmail(verifiedUser.getEmail()); req.setPassword("wrong");
            when(repository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException)e).getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── verifyEmail ────────────────────────────────────────────────────────────

    @Nested @DisplayName("verifyEmail()")
    class VerifyEmailTests {

        @Test @DisplayName("success – sets emailVerified=true")
        void verifyEmail_success() {
            when(repository.findByEmail(anyString())).thenReturn(Optional.of(unverifiedUser));
            doNothing().when(otpService).verifyOtp(anyString(), anyString());

            authService.verifyEmail(unverifiedUser.getEmail(), "123456");

            assertThat(unverifiedUser.isEmailVerified()).isTrue();
            verify(repository).save(unverifiedUser);
        }

        @Test @DisplayName("already verified – throws 400")
        void verifyEmail_alreadyVerified() {
            when(repository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));

            assertThatThrownBy(() -> authService.verifyEmail(verifiedUser.getEmail(), "otp"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("already verified");
        }
    }

    // ── sendVerificationOtp ────────────────────────────────────────────────────

    @Test @DisplayName("sendVerificationOtp – throws 400 if already verified")
    void sendVerificationOtp_alreadyVerified() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));
        assertThatThrownBy(() -> authService.sendVerificationOtp(verifiedUser.getEmail()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException)e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @DisplayName("sendVerificationOtp – sends OTP to unverified user")
    void sendVerificationOtp_success() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(unverifiedUser));
        authService.sendVerificationOtp(unverifiedUser.getEmail());
        verify(otpService).sendVerificationOtp(unverifiedUser.getEmail());
    }

    // ── forgotPassword / resetPassword ────────────────────────────────────────

    @Test @DisplayName("sendForgotPasswordOtp – sends OTP for active user")
    void forgotPassword_success() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));
        authService.sendForgotPasswordOtp(verifiedUser.getEmail());
        verify(otpService).sendForgotPasswordOtp(verifiedUser.getEmail());
    }

    @Test @DisplayName("sendForgotPasswordOtp – allows inactive user for recovery")
    void forgotPassword_inactiveUser_allowed() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(inactiveUser));
        authService.sendForgotPasswordOtp(inactiveUser.getEmail());
        verify(otpService).sendForgotPasswordOtp(inactiveUser.getEmail());
    }

    @Test @DisplayName("resetPassword – verifies OTP and encodes new password")
    void resetPassword_success() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(verifiedUser.getEmail()); req.setOtp("123"); req.setNewPassword("newPw");

        when(repository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));
        doNothing().when(otpService).verifyOtp(anyString(), anyString());
        when(passwordEncoder.encode(anyString())).thenReturn("newHashed");

        authService.resetPassword(req);
        assertThat(verifiedUser.getPassword()).isEqualTo("newHashed");
        verify(repository).save(verifiedUser);
    }

    // ── changePassword ─────────────────────────────────────────────────────────

    @Nested @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test @DisplayName("success – saves new encoded password")
        void changePassword_success() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("old"); req.setNewPassword("new");

            when(repository.findById(1L)).thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches("old", verifiedUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("new")).thenReturn("newHashed");

            authService.changePassword(1L, req);
            assertThat(verifiedUser.getPassword()).isEqualTo("newHashed");
        }

        @Test @DisplayName("wrong old password – throws 400")
        void changePassword_wrongOld() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("bad"); req.setNewPassword("new");

            when(repository.findById(1L)).thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(1L, req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Old password is incorrect");
        }
    }

    // ── refreshToken ───────────────────────────────────────────────────────────

    @Test @DisplayName("refreshToken – issues new token for valid token")
    void refreshToken_success() {
        when(jwtUtil.isTokenValid(anyString())).thenReturn(true);
        when(jwtUtil.extractEmail(anyString())).thenReturn(verifiedUser.getEmail());
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("newJwt");

        String result = authService.refreshToken("oldToken");
        assertThat(result).isEqualTo("newJwt");
    }

    @Test @DisplayName("refreshToken – throws 401 for invalid/expired token")
    void refreshToken_invalid() {
        when(jwtUtil.isTokenValid(anyString())).thenReturn(false);
        assertThatThrownBy(() -> authService.refreshToken("bad"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException)e).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── updateProfile ──────────────────────────────────────────────────────────

    @Test @DisplayName("updateProfile – updates name, avatarUrl, bio and calls save()")
    void updateProfile_success() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullname("New Name"); req.setUsername("newuser");
        req.setAvatarUrl("https://avatar.png"); req.setBio("Hello");

        when(repository.findById(1L)).thenReturn(Optional.of(verifiedUser));

        authService.updateProfile(1L, req);
        assertThat(verifiedUser.getFullName()).isEqualTo("New Name");
        verify(repository).save(argThat(u -> {
            return "New Name".equals(u.getFullName());
        }));
    }

    // ── logout ─────────────────────────────────────────────────────────────────

    @Test @DisplayName("logout – blacklists token with TTL")
    void logout_blacklistsToken() {
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        Date expiry = new Date(System.currentTimeMillis() + 60_000);
        when(claims.getExpiration()).thenReturn(expiry);
        when(jwtUtil.extractAllClaims(anyString())).thenReturn(claims);

        authService.logout("valid.jwt.token");

        verify(blacklistService).blacklist(eq("valid.jwt.token"), anyLong());
    }

    // ── admin ops ──────────────────────────────────────────────────────────────

    @Nested @DisplayName("Admin operations")
    class AdminTests {

        @Test @DisplayName("deactivateAccount – sets active=false")
        void deactivateAccount() {
            when(repository.findById(1L)).thenReturn(Optional.of(verifiedUser));
            authService.deactivateAccount(1L);
            assertThat(verifiedUser.isActive()).isFalse();
            verify(repository).save(verifiedUser);
        }

        @Test @DisplayName("suspendUser – sets active=false on active user")
        void suspendUser_success() {
            when(repository.findById(1L)).thenReturn(Optional.of(verifiedUser));
            authService.suspendUser(1L);
            assertThat(verifiedUser.isActive()).isFalse();
        }

        @Test @DisplayName("suspendUser – throws 400 if already suspended")
        void suspendUser_alreadySuspended() {
            when(repository.findById(3L)).thenReturn(Optional.of(inactiveUser));
            assertThatThrownBy(() -> authService.suspendUser(3L))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException)e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test @DisplayName("reactivateUser – sets active=true")
        void reactivateUser_success() {
            when(repository.findById(3L)).thenReturn(Optional.of(inactiveUser));
            authService.reactivateUser(3L);
            assertThat(inactiveUser.isActive()).isTrue();
        }

        @Test @DisplayName("reactivateUser – throws 400 if already active")
        void reactivateUser_alreadyActive() {
            when(repository.findById(1L)).thenReturn(Optional.of(verifiedUser));
            assertThatThrownBy(() -> authService.reactivateUser(1L))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException)e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test @DisplayName("deleteUser – calls deleteById for existing user")
        void deleteUser_success() {
            when(repository.existsById(1L)).thenReturn(true);
            authService.deleteUser(1L);
            verify(repository).deleteById(1L);
        }

        @Test @DisplayName("deleteUser – throws 404 for missing user")
        void deleteUser_notFound() {
            when(repository.existsById(999L)).thenReturn(false);
            assertThatThrownBy(() -> authService.deleteUser(999L))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException)e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test @DisplayName("searchUsers – delegates to repository")
        void searchUsers_success() {
            when(repository.searchByNameOrUsername("na")).thenReturn(List.of(verifiedUser));
            List<User> result = authService.searchUsers("na");
            assertThat(result).hasSize(1);
        }

        @Test @DisplayName("getAllUsers – returns all users")
        void getAllUsers_success() {
            when(repository.findAll()).thenReturn(List.of(verifiedUser, inactiveUser));
            List<User> result = authService.getAllUsers();
            assertThat(result).hasSize(2);
        }

        @Test @DisplayName("getUsersByRole – filters by role")
        void getUsersByRole_success() {
            when(repository.findAllByRole(ROLE.MEMBER)).thenReturn(List.of(verifiedUser));
            List<User> result = authService.getUsersByRole(ROLE.MEMBER);
            assertThat(result).hasSize(1).first().extracting(User::getRole).isEqualTo(ROLE.MEMBER);
        }

        @Test @DisplayName("updateUserRole – updates role")
        void updateUserRole_success() {
            when(repository.findById(1L)).thenReturn(Optional.of(verifiedUser));
            authService.updateUserRole(1L, ROLE.PLATFORM_ADMIN);
            assertThat(verifiedUser.getRole()).isEqualTo(ROLE.PLATFORM_ADMIN);
            verify(repository).save(verifiedUser);
        }

        @Test @DisplayName("getAdminStats – returns stats")
        void getAdminStats_success() {
            when(repository.count()).thenReturn(10L);
            when(repository.countByLastLoginAtAfter(any())).thenReturn(5L);
            
            AdminStatsResponse stats = authService.getAdminStats();
            assertThat(stats.getTotalUsers()).isEqualTo(10L);
            assertThat(stats.getActiveUsersToday()).isEqualTo(5L);
        }
    }

    @Test @DisplayName("sendReactivationOtp – sends OTP for inactive user")
    void sendReactivationOtp_success() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(inactiveUser));
        authService.sendReactivationOtp(inactiveUser.getEmail());
        verify(otpService).sendReactivationOtp(inactiveUser.getEmail(), inactiveUser.getFullName());
    }

    @Test @DisplayName("sendReactivationOtp – throws 400 for active user")
    void sendReactivationOtp_activeUser_throws() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));
        assertThatThrownBy(() -> authService.sendReactivationOtp(verifiedUser.getEmail()))
                .isInstanceOf(CustomException.class);
    }

    @Test @DisplayName("reactivateWithOtp – activates account")
    void reactivateWithOtp_success() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(inactiveUser));
        authService.reactivateWithOtp(inactiveUser.getEmail(), "123");
        assertThat(inactiveUser.isActive()).isTrue();
        verify(otpService).verifyOtp(anyString(), anyString());
    }

    @Test @DisplayName("reactivateWithOtp – throws 400 if already active")
    void reactivateWithOtp_activeUser_throws() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));
        assertThatThrownBy(() -> authService.reactivateWithOtp(verifiedUser.getEmail(), "123"))
                .isInstanceOf(CustomException.class);
    }

    @Test @DisplayName("getUserByEmail – success")
    void getUserByEmail_success() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));
        User user = authService.getUserByEmail(verifiedUser.getEmail());
        assertThat(user.getEmail()).isEqualTo(verifiedUser.getEmail());
    }

    @Test @DisplayName("validateToken – success")
    void validateToken_success() {
        when(jwtUtil.extractEmail(anyString())).thenReturn(verifiedUser.getEmail());
        String msg = authService.validateToken("token");
        assertThat(msg).contains(verifiedUser.getEmail());
    }

    @Test @DisplayName("validateToken – failure")
    void validateToken_failure() {
        when(jwtUtil.extractEmail(anyString())).thenThrow(new RuntimeException("invalid"));
        assertThatThrownBy(() -> authService.validateToken("bad"))
                .isInstanceOf(CustomException.class);
    }
}