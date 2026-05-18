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

    @Test @DisplayName("getByRecipient() — success")
    void getByRecipient_success() {
        when(repository.findByRecipientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(sampleNotif));
        List<NotificationResponse> list = notificationService.getByRecipient(1L);
        assertThat(list).hasSize(1);
    }

    @Test @DisplayName("getUnreadByRecipient() — success")
    void getUnreadByRecipient_success() {
        when(repository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of(sampleNotif));
        List<NotificationResponse> list = notificationService.getUnreadByRecipient(1L);
        assertThat(list).hasSize(1);
    }

    @Test @DisplayName("getByRecipientAndType() — success")
    void getByRecipientAndType_success() {
        when(repository.findByRecipientIdAndTypeOrderByCreatedAtDesc(1L, NotificationType.ASSIGNMENT)).thenReturn(List.of(sampleNotif));
        List<NotificationResponse> list = notificationService.getByRecipientAndType(1L, NotificationType.ASSIGNMENT);
        assertThat(list).hasSize(1);
    }

    @Test @DisplayName("getAll() — success")
    void getAll_success() {
        when(repository.findAll()).thenReturn(List.of(sampleNotif));
        List<NotificationResponse> list = notificationService.getAll();
        assertThat(list).hasSize(1);
    }

    @Test @DisplayName("markAllAsRead() — calls repository")
    void markAllAsRead_success() {
        doNothing().when(repository).markAllAsRead(1L);
        notificationService.markAllAsRead(1L);
        verify(repository).markAllAsRead(1L);
    }

    @Test @DisplayName("deleteNotification() — success")
    void deleteNotification_success() {
        when(repository.existsByIdAndRecipientId(1L, 1L)).thenReturn(true);
        notificationService.deleteNotification(1L, 1L);
        verify(repository).deleteById(1L);
    }

    @Test @DisplayName("deleteNotification() — throws 404")
    void deleteNotification_throws() {
        when(repository.existsByIdAndRecipientId(1L, 1L)).thenReturn(false);
        assertThatThrownBy(() -> notificationService.deleteNotification(1L, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test @DisplayName("deleteReadNotifications() — calls repository")
    void deleteReadNotifications_success() {
        doNothing().when(repository).deleteReadByRecipientId(1L);
        notificationService.deleteReadNotifications(1L);
        verify(repository).deleteReadByRecipientId(1L);
    }

    @Test @DisplayName("notifyAssignment() — triggers email")
    void notifyAssignment_success() {
        when(repository.save(any())).thenReturn(sampleNotif);
        notificationService.notifyAssignment(1L, 2L, 3L, "Task", "user@email.com");
        verify(repository).save(any());
        verify(emailService).sendNotificationEmail(eq("user@email.com"), anyString(), anyString(), anyString());
    }

    @Test @DisplayName("notifyMention() — success")
    void notifyMention_success() {
        when(repository.save(any())).thenReturn(sampleNotif);
        notificationService.notifyMention(1L, 2L, 3L, "Task");
        verify(repository).save(any());
    }

    @Test @DisplayName("notifyDueDateApproaching() — success")
    void notifyDueDateApproaching_success() {
        when(repository.save(any())).thenReturn(sampleNotif);
        notificationService.notifyDueDateApproaching(1L, 3L, "Task", "1 hour");
        verify(repository).save(any());
    }

    @Test @DisplayName("notifyCardMovedToDone() — success")
    void notifyCardMovedToDone_success() {
        when(repository.save(any())).thenReturn(sampleNotif);
        notificationService.notifyCardMovedToDone(1L, 2L, 3L, "Task");
        verify(repository).save(any());
    }

    @Test @DisplayName("notifyCommentReply() — success")
    void notifyCommentReply_success() {
        when(repository.save(any())).thenReturn(sampleNotif);
        notificationService.notifyCommentReply(1L, 2L, 3L, "Task");
        verify(repository).save(any());
    }

    @Test @DisplayName("notifyOverdue() — triggers email")
    void notifyOverdue_success() {
        when(repository.save(any())).thenReturn(sampleNotif);
        notificationService.notifyOverdue(1L, 3L, "Task", "Yesterday", "user@email.com");
        verify(repository).save(any());
        verify(emailService).sendNotificationEmail(eq("user@email.com"), anyString(), anyString(), anyString());
    }
}