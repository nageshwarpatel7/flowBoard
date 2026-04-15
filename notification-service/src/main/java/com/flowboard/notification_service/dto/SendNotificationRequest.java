package com.flowboard.notification_service.dto;

import com.flowboard.notification_service.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendNotificationRequest {

    @NotNull(message = "Recipient ID is required")
    private Long recipientId;

    private Long actorId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private Long relatedId;

    private String relatedType;

    private String deepLinkUrl;

    private boolean sendEmail = false;

    private String recipientEmail;
}
