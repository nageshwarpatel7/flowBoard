package com.flowBoard.auth_service.service;

import com.flowBoard.auth_service.dto.*;
import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.exception.CustomException;
import com.flowBoard.auth_service.repository.UserRepository;
import com.flowBoard.auth_service.security.JwtUtil;
import com.flowBoard.auth_service.security.OtpService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock UserRepository repository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock OtpService otpService;
    @Mock TokenBlacklistService blacklistService;

    @InjectMocks AuthServiceImpl authService;

    private User verifiedUser;
    private User unverifiedUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        verifiedUser = User.builder()
                .id(1L).fullName("Nageshwar Patel")
                .email("nageshwar@gmail.com").username("nageshwar")
                .password("hashed_pw").role(ROLE.MEMBER)
                .active(true).emailVerified(true)
                .createdAt(LocalDateTime.now()).build();

        unverifiedUser = User.builder()
                .id(2L).email("unverified@gmail.com")
                .active(true).emailVerified(false).build();

        inactiveUser = User.builder()
                .id(3L).email("inactive@gmail.com")
                .active(false).emailVerified(true).build();
    }

    @Nested @DisplayName("register()")
    class RegisterTests {

        @Test @DisplayName("success — saves user and sends OTP")
        void register_success() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("Test User"); req.setEmail("test@gmail.com");
            req.setUsername("testuser"); req.setPassword("pass123");

            when(repository.existsByEmail(req.getEmail())).thenReturn(false);
            when(repository.existsByUsername(req.getUsername())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(repository.save(any())).thenReturn(verifiedUser);

            AuthResponse response = authService.register(req);

            assertThat(response.getMessage()).contains("Registration successful");
            assertThat(response.getToken()).isNull();
            verify(otpService).sendVerificationOtp(req.getEmail());
        }

        @Test @DisplayName("duplicate email — throws 400")
        void register_duplicateEmail_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("nageshwar@gmail.com"); req.setUsername("newuser");

            when(repository.existsByEmail(req.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Email already exists");

            verify(repository, never()).save(any());
        }

        @Test @DisplayName("duplicate username — throws 400")
        void register_duplicateUsername_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@gmail.com"); req.setUsername("nageshwar");

            when(repository.existsByEmail(req.getEmail())).thenReturn(false);
            when(repository.existsByUsername(req.getUsername())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Username already taken");
        }
    }

    @Nested @DisplayName("login()")
    class LoginTests {

        @Test @DisplayName("success — returns JWT token")
        void login_success() {
            LoginRequest req = new LoginRequest();
            req.setEmail(verifiedUser.getEmail()); req.setPassword("pass123");

            when(repository.findByEmail(req.getEmail()))
                    .thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches(req.getPassword(),
                    verifiedUser.getPassword())).thenReturn(true);
            when(jwtUtil.generateToken(anyString(), anyLong(), anyString()))
                    .thenReturn("jwt.token.xyz");

            AuthResponse response = authService.login(req);

            assertThat(response.getToken()).isEqualTo("jwt.token.xyz");
            assertThat(response.getMessage()).contains("Login successful");
        }

        @Test @DisplayName("email not found — throws 404")
        void login_emailNotFound_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail("nobody@gmail.com"); req.setPassword("pass");

            when(repository.findByEmail(req.getEmail()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test @DisplayName("account inactive — throws 403")
        void login_inactiveAccount_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail(inactiveUser.getEmail()); req.setPassword("pass");

            when(repository.findByEmail(req.getEmail()))
                    .thenReturn(Optional.of(inactiveUser));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test @DisplayName("email not verified — throws 403 and resends OTP")
        void login_unverifiedEmail_throws_and_resends() {
            LoginRequest req = new LoginRequest();
            req.setEmail(unverifiedUser.getEmail()); req.setPassword("pass");

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

        @Test @DisplayName("wrong password — throws 401")
        void login_wrongPassword_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail(verifiedUser.getEmail()); req.setPassword("wrong");

            when(repository.findByEmail(req.getEmail()))
                    .thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches(anyString(), anyString()))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested @DisplayName("verifyEmail()")
    class VerifyEmailTests {

        @Test @DisplayName("success — sets emailVerified=true")
        void verifyEmail_success() {
            when(repository.findByEmail(unverifiedUser.getEmail()))
                    .thenReturn(Optional.of(unverifiedUser));
            doNothing().when(otpService)
                    .verifyOtp(unverifiedUser.getEmail(), "123456");

            authService.verifyEmail(unverifiedUser.getEmail(), "123456");

            assertThat(unverifiedUser.isEmailVerified()).isTrue();
            verify(repository).save(unverifiedUser);
        }

        @Test @DisplayName("already verified — throws 400")
        void verifyEmail_alreadyVerified_throws() {
            when(repository.findByEmail(verifiedUser.getEmail()))
                    .thenReturn(Optional.of(verifiedUser));

            assertThatThrownBy(() ->
                    authService.verifyEmail(verifiedUser.getEmail(), "123456"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("already verified");
        }
    }

    @Nested @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test @DisplayName("success — encodes and saves new password")
        void changePassword_success() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("oldPass"); req.setNewPassword("newPass");

            when(repository.findById(1L))
                    .thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches("oldPass", verifiedUser.getPassword()))
                    .thenReturn(true);
            when(passwordEncoder.encode("newPass")).thenReturn("newHashed");

            authService.changePassword(1L, req);

            assertThat(verifiedUser.getPassword()).isEqualTo("newHashed");
            verify(repository).save(verifiedUser);
        }

        @Test @DisplayName("wrong old password — throws 400")
        void changePassword_wrongOld_throws() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("wrong"); req.setNewPassword("newPass");

            when(repository.findById(1L))
                    .thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches(anyString(), anyString()))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(1L, req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Old password is incorrect");
        }
    }

    @Nested @DisplayName("Admin operations")
    class AdminTests {

        @Test @DisplayName("suspendUser — sets active=false")
        void suspendUser_success() {
            when(repository.findById(1L))
                    .thenReturn(Optional.of(verifiedUser));

            authService.suspendUser(1L);

            assertThat(verifiedUser.isActive()).isFalse();
            verify(repository).save(verifiedUser);
        }

        @Test @DisplayName("reactivateUser — sets active=true")
        void reactivateUser_success() {
            when(repository.findById(3L))
                    .thenReturn(Optional.of(inactiveUser));

            authService.reactivateUser(3L);

            assertThat(inactiveUser.isActive()).isTrue();
            verify(repository).save(inactiveUser);
        }

        @Test @DisplayName("deleteUser — calls deleteById")
        void deleteUser_success() {
            when(repository.existsById(1L)).thenReturn(true);

            authService.deleteUser(1L);

            verify(repository).deleteById(1L);
        }

        @Test @DisplayName("deleteUser — throws 404 when not found")
        void deleteUser_notFound_throws() {
            when(repository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> authService.deleteUser(999L))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}