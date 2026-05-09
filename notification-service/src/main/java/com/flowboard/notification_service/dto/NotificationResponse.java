package com.flowboard.notification_service.dto;

import com.flowboard.notification_service.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long recipientId;
    private Long actorId;
    private NotificationType type;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
    private String deepLinkUrl;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
