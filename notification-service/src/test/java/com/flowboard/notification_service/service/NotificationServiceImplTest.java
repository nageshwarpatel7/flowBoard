package com.flowboard.notification_service.service;

import com.flowboard.notification_service.dto.*;
import com.flowboard.notification_service.entity.Notification;
import com.flowboard.notification_service.enums.NotificationType;
import com.flowboard.notification_service.exception.CustomException;
import com.flowboard.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

    @Mock NotificationRepository repository;
    @Mock EmailNotificationService emailService;

    @InjectMocks NotificationServiceImpl notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .id(1L).recipientId(2L).actorId(1L)
                .type(NotificationType.ASSIGNMENT)
                .title("Card assigned to you")
                .message("Nageshwar assigned 'Design login page' to you")
                .relatedId(5L).relatedType("CARD")
                .isRead(false)
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("send() should save notification and return response")
    void send_success() {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(2L); req.setActorId(1L);
        req.setType(NotificationType.ASSIGNMENT);
        req.setTitle("Card assigned"); req.setMessage("...");
        req.setSendEmail(false);

        when(repository.save(any())).thenReturn(sampleNotification);

        NotificationResponse response = notificationService.send(req);

        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(NotificationType.ASSIGNMENT);
        verify(emailService, never()).sendNotificationEmail(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("send() should trigger email when sendEmail=true")
    void send_withEmail_triggersEmailService() {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(2L); req.setType(NotificationType.ASSIGNMENT);
        req.setTitle("Test"); req.setMessage("...");
        req.setSendEmail(true);
        req.setRecipientEmail("user@example.com");

        when(repository.save(any())).thenReturn(sampleNotification);

        notificationService.send(req);

        verify(emailService).sendNotificationEmail(
                eq("user@example.com"), any(), any(), any());
    }

    @Test
    @DisplayName("sendBulk() should save all notifications")
    void sendBulk_success() {
        SendBulkNotificationRequest req = new SendBulkNotificationRequest();
        req.setRecipientIds(List.of(1L, 2L, 3L));
        req.setType(NotificationType.BROADCAST);
        req.setTitle("Maintenance"); req.setMessage("...");

        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<NotificationResponse> responses = notificationService.sendBulk(req);

        assertThat(responses).hasSize(3);
        verify(repository).saveAll(argThat(
                list -> ((List<?>) list).size() == 3));
    }

    @Test
    @DisplayName("markAsRead() should throw 404 when notification not found")
    void markAsRead_notFound_throws() {
        when(repository.existsByIdAndRecipientId(999L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("markAsRead() should set isRead=true")
    void markAsRead_success() {
        when(repository.existsByIdAndRecipientId(1L, 2L)).thenReturn(true);
        when(repository.findById(1L))
                .thenReturn(Optional.of(sampleNotification));
        when(repository.save(any())).thenReturn(sampleNotification);

        NotificationResponse response = notificationService.markAsRead(1L, 2L);

        assertThat(sampleNotification.isRead()).isTrue();
        assertThat(sampleNotification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("getUnreadCount() should return count from repository")
    void getUnreadCount_success() {
        when(repository.countByRecipientIdAndIsReadFalse(2L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(2L);

        assertThat(count).isEqualTo(5L);
    }
}