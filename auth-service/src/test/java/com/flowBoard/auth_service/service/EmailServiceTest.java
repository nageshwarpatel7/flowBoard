package com.flowBoard.auth_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@flowboard.com");
    }

    @Test
    @DisplayName("sendVerificationOtp should send email successfully")
    void sendVerificationOtp_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationOtp("test@gmail.com", "123456");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendForgotPasswordOtp should send email successfully")
    void sendForgotPasswordOtp_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendForgotPasswordOtp("test@gmail.com", "123456");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendAccountStatusEmail active should send email successfully")
    void sendAccountStatusEmail_active_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAccountStatusEmail("test@gmail.com", "Test User", true);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendAccountStatusEmail suspended should send email successfully")
    void sendAccountStatusEmail_suspended_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAccountStatusEmail("test@gmail.com", "Test User", false);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendReactivationOtp should send email successfully")
    void sendReactivationOtp_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendReactivationOtp("test@gmail.com", "Test User", "123456");

        verify(mailSender).send(mimeMessage);
    }

}
