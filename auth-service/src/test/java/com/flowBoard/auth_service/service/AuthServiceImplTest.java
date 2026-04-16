package com.flowBoard.auth_service.service;

import com.flowBoard.auth_service.dto.*;
import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.exception.CustomException;
import com.flowBoard.auth_service.repository.UserRepository;
import com.flowBoard.auth_service.security.JwtUtil;
import com.flowBoard.auth_service.security.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock UserRepository repository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock OtpService otpService;

    @InjectMocks AuthServiceImpl authService;

    private User activeVerifiedUser;
    private User inactiveUser;
    private User unverifiedUser;

    @BeforeEach
    void setUp() {
        activeVerifiedUser = User.builder()
                .id(1L).fullName("Nageshwar Patel")
                .email("nageshwar@gmail.com").username("nageshwar")
                .password("hashed_password").role(ROLE.MEMBER)
                .active(true).emailVerified(true)
                .createdAt(LocalDateTime.now()).build();

        inactiveUser = User.builder()
                .id(2L).email("inactive@gmail.com")
                .active(false).emailVerified(true).build();

        unverifiedUser = User.builder()
                .id(3L).email("unverified@gmail.com")
                .active(true).emailVerified(false).build();
    }

    // ── Register ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register user and send OTP when email and username are unique")
        void register_success() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("Nageshwar Patel");
            req.setEmail("nageshwar@gmail.com");
            req.setUsername("nageshwar");
            req.setPassword("password123");

            when(repository.existsByEmail(req.getEmail())).thenReturn(false);
            when(repository.existsByUsername(req.getUsername())).thenReturn(false);
            when(passwordEncoder.encode(req.getPassword()))
                    .thenReturn("hashed_password");
            when(repository.save(any(User.class)))
                    .thenReturn(activeVerifiedUser);

            AuthResponse response = authService.register(req);

            assertThat(response).isNotNull();
            assertThat(response.getMessage())
                    .contains("Registration successful");
            assertThat(response.getToken()).isNull(); // no token until OTP verified

            verify(repository).save(any(User.class));
            verify(otpService).sendVerificationOtp(req.getEmail());
        }

        @Test
        @DisplayName("should throw 400 when email already exists")
        void register_duplicateEmail_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("nageshwar@gmail.com");
            req.setUsername("newuser");

            when(repository.existsByEmail(req.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Email already exists");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should throw 400 when username already taken")
        void register_duplicateUsername_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@gmail.com");
            req.setUsername("nageshwar");

            when(repository.existsByEmail(req.getEmail())).thenReturn(false);
            when(repository.existsByUsername(req.getUsername())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Username already taken");
        }
    }

    // ── Login ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should return JWT token on successful login")
        void login_success() {
            LoginRequest req = new LoginRequest();
            req.setEmail("nageshwar@gmail.com");
            req.setPassword("password123");

            when(repository.findByEmail(req.getEmail()))
                    .thenReturn(Optional.of(activeVerifiedUser));
            when(passwordEncoder.matches(req.getPassword(),
                    activeVerifiedUser.getPassword())).thenReturn(true);
            when(jwtUtil.generateToken(anyString(), anyLong(), anyString()))
                    .thenReturn("jwt.token.here");

            AuthResponse response = authService.login(req);

            assertThat(response.getToken()).isEqualTo("jwt.token.here");
            assertThat(response.getMessage()).contains("Login successful");
        }

        @Test
        @DisplayName("should throw 404 when email not found")
        void login_emailNotFound_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail("nobody@gmail.com");
            req.setPassword("password");

            when(repository.findByEmail(req.getEmail()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should throw 403 when account is deactivated")
        void login_inactiveAccount_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail(inactiveUser.getEmail());
            req.setPassword("password");

            when(repository.findByEmail(req.getEmail()))
                    .thenReturn(Optional.of(inactiveUser));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("should throw 403 when email not verified and resend OTP")
        void login_emailNotVerified_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail(unverifiedUser.getEmail());
            req.setPassword("password");

            when(repository.findByEmail(req.getEmail()))
                    .thenReturn(Optional.of(unverifiedUser));
            when(otpService.hasActiveOtp(unverifiedUser.getEmail()))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(otpService).sendVerificationOtp(unverifiedUser.getEmail());
        }

        @Test
        @DisplayName("should throw 401 when password is wrong")
        void login_wrongPassword_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail(activeVerifiedUser.getEmail());
            req.setPassword("wrongpassword");

            when(repository.findByEmail(req.getEmail()))
                    .thenReturn(Optional.of(activeVerifiedUser));
            when(passwordEncoder.matches(req.getPassword(),
                    activeVerifiedUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── verifyEmail ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmailTests {

        @Test
        @DisplayName("should set emailVerified=true and save user")
        void verifyEmail_success() {
            when(repository.findByEmail(unverifiedUser.getEmail()))
                    .thenReturn(Optional.of(unverifiedUser));
            doNothing().when(otpService)
                    .verifyOtp(unverifiedUser.getEmail(), "123456");

            authService.verifyEmail(unverifiedUser.getEmail(), "123456");

            assertThat(unverifiedUser.isEmailVerified()).isTrue();
            verify(repository).save(unverifiedUser);
        }

        @Test
        @DisplayName("should throw 400 when email already verified")
        void verifyEmail_alreadyVerified_throws() {
            when(repository.findByEmail(activeVerifiedUser.getEmail()))
                    .thenReturn(Optional.of(activeVerifiedUser));

            assertThatThrownBy(() ->
                    authService.verifyEmail(activeVerifiedUser.getEmail(), "123456"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("already verified");
        }
    }

    // ── changePassword ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("should update password when old password matches")
        void changePassword_success() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("oldPass");
            req.setNewPassword("newPass");

            when(repository.findById(1L))
                    .thenReturn(Optional.of(activeVerifiedUser));
            when(passwordEncoder.matches("oldPass",
                    activeVerifiedUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("newPass"))
                    .thenReturn("new_hashed");

            authService.changePassword(1L, req);

            assertThat(activeVerifiedUser.getPassword()).isEqualTo("new_hashed");
            verify(repository).save(activeVerifiedUser);
        }

        @Test
        @DisplayName("should throw 400 when old password is wrong")
        void changePassword_wrongOldPassword_throws() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("wrongOld");
            req.setNewPassword("newPass");

            when(repository.findById(1L))
                    .thenReturn(Optional.of(activeVerifiedUser));
            when(passwordEncoder.matches(anyString(), anyString()))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(1L, req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Old password is incorrect");
        }
    }

    // ── Admin operations ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Admin operations")
    class AdminTests {

        @Test
        @DisplayName("suspendUser should set active=false")
        void suspendUser_success() {
            when(repository.findById(1L))
                    .thenReturn(Optional.of(activeVerifiedUser));

            authService.suspendUser(1L);

            assertThat(activeVerifiedUser.isActive()).isFalse();
            verify(repository).save(activeVerifiedUser);
        }

        @Test
        @DisplayName("reactivateUser should set active=true")
        void reactivateUser_success() {
            when(repository.findById(2L))
                    .thenReturn(Optional.of(inactiveUser));

            authService.reactivateUser(2L);

            assertThat(inactiveUser.isActive()).isTrue();
            verify(repository).save(inactiveUser);
        }

        @Test
        @DisplayName("deleteUser should call deleteById")
        void deleteUser_success() {
            when(repository.existsById(1L)).thenReturn(true);

            authService.deleteUser(1L);

            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("getAllUsers should return all users")
        void getAllUsers_success() {
            when(repository.findAll())
                    .thenReturn(List.of(activeVerifiedUser, inactiveUser));

            List<User> users = authService.getAllUsers();

            assertThat(users).hasSize(2);
        }
    }
}