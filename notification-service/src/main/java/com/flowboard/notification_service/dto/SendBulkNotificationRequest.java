package com.flowboard.notification_service.dto;

import com.flowboard.notification_service.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SendBulkNotificationRequest {

    @NotEmpty(message = "At least one recipient is required")
    private List<Long> recipientIds;

    private Long actorId;

    @NotNull(message = "Type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private Long relatedId;
    private String relatedType;
    private String deepLinkUrl;
}
