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
@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    @Mock NotificationRepository repository;
    @Mock EmailNotificationService emailService;

    @InjectMocks NotificationServiceImpl notificationService;

    private Notification sampleNotif;

    @BeforeEach
    void setUp() {
        sampleNotif = Notification.builder()
                .id(1L).recipientId(2L).actorId(1L)
                .type(NotificationType.ASSIGNMENT)
                .title("Card assigned").message("You were assigned")
                .isRead(false).createdAt(LocalDateTime.now()).build();
    }

    @Test @DisplayName("send() — saves notification, no email when sendEmail=false")
    void send_noEmail_success() {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(2L); req.setActorId(1L);
        req.setType(NotificationType.ASSIGNMENT);
        req.setTitle("Test"); req.setMessage("...");
        req.setSendEmail(false);

        when(repository.save(any())).thenReturn(sampleNotif);

        NotificationResponse resp = notificationService.send(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getType()).isEqualTo(NotificationType.ASSIGNMENT);
        verify(emailService, never()).sendNotificationEmail(
                any(), any(), any(), any());
    }

    @Test @DisplayName("send() — triggers email when sendEmail=true with email")
    void send_withEmail_triggersEmail() {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(2L); req.setType(NotificationType.ASSIGNMENT);
        req.setTitle("Test"); req.setMessage("...");
        req.setSendEmail(true);
        req.setRecipientEmail("user@example.com");

        when(repository.save(any())).thenReturn(sampleNotif);

        notificationService.send(req);

        verify(emailService).sendNotificationEmail(
                eq("user@example.com"), any(), any(), any());
    }

    @Test @DisplayName("sendBulk() — saves all notifications in one call")
    void sendBulk_success() {
        SendBulkNotificationRequest req = new SendBulkNotificationRequest();
        req.setRecipientIds(List.of(1L, 2L, 3L));
        req.setType(NotificationType.BROADCAST);
        req.setTitle("Maintenance"); req.setMessage("...");

        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<NotificationResponse> responses = notificationService.sendBulk(req);

        assertThat(responses).hasSize(3);
    }

    @Test @DisplayName("markAsRead() — sets isRead=true and readAt")
    void markAsRead_success() {
        when(repository.existsByIdAndRecipientId(1L, 2L)).thenReturn(true);
        when(repository.findById(1L)).thenReturn(Optional.of(sampleNotif));
        when(repository.save(any())).thenReturn(sampleNotif);

        notificationService.markAsRead(1L, 2L);

        assertThat(sampleNotif.isRead()).isTrue();
        assertThat(sampleNotif.getReadAt()).isNotNull();
    }

    @Test @DisplayName("markAsRead() — throws 404 for wrong recipient")
    void markAsRead_wrongRecipient_throws() {
        when(repository.existsByIdAndRecipientId(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @DisplayName("getUnreadCount() — returns count from repository")
    void getUnreadCount_success() {
        when(repository.countByRecipientIdAndIsReadFalse(2L)).thenReturn(7L);

        long count = notificationService.getUnreadCount(2L);

        assertThat(count).isEqualTo(7L);
    }
}