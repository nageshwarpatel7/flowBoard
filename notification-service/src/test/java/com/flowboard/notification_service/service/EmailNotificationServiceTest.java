package com.flowboard.notification_service.service;

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
@DisplayName("EmailNotificationService Unit Tests")
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailNotificationService, "fromEmail", "noreply@flowboard.com");
    }

    @Test
    @DisplayName("sendNotificationEmail should send email successfully")
    void sendNotificationEmail_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailNotificationService.sendNotificationEmail("test@gmail.com", "Title", "Message", "/link");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendAssignmentEmail should send email successfully")
    void sendAssignmentEmail_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailNotificationService.sendAssignmentEmail("test@gmail.com", "Card", "Assigner", "/link");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendOverdueEmail should send email successfully")
    void sendOverdueEmail_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailNotificationService.sendOverdueEmail("test@gmail.com", "Card", "Tomorrow");

        verify(mailSender).send(mimeMessage);
    }
}
