package com.flowboard.notification_service.service;

import com.flowboard.notification_service.dto.NotificationResponse;
import com.flowboard.notification_service.dto.SendBulkNotificationRequest;
import com.flowboard.notification_service.dto.SendNotificationRequest;
import com.flowboard.notification_service.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    NotificationResponse send(SendNotificationRequest request);

    List<NotificationResponse> sendBulk(SendBulkNotificationRequest request);

    List<NotificationResponse> getByRecipient(Long recipientId);
    List<NotificationResponse> getUnreadByRecipient(Long recipientId);
    List<NotificationResponse> getByRecipientAndType(
            Long recipientId, NotificationType type);
    List<NotificationResponse> getAll();

    NotificationResponse markAsRead(Long notificationId, Long recipientId);
    void markAllAsRead(Long recipientId);

    long getUnreadCount(Long recipientId);

    void deleteNotification(Long notificationId, Long recipientId);
    void deleteReadNotifications(Long recipientId);

    void notifyAssignment(Long recipientId, Long actorId,
                          Long cardId, String cardTitle,
                          String recipientEmail);

    void notifyMention(Long recipientId, Long actorId,
                       Long cardId, String cardTitle);

    void notifyDueDateApproaching(Long recipientId, Long cardId,
                                  String cardTitle, String timeLeft);

    void notifyCardMovedToDone(Long recipientId, Long actorId,
                               Long cardId, String cardTitle);

    void notifyCommentReply(Long recipientId, Long actorId,
                            Long cardId, String cardTitle);

    void notifyOverdue(Long recipientId, Long cardId,
                       String cardTitle, String dueDate,
                       String recipientEmail);
}
