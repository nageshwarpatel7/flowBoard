package com.flowboard.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyDueDateRequest {
    private Long   recipientId;
    private Long   cardId;
    private String cardTitle;
    private String timeLeft;
    private String recipientEmail; // optional — used for email delivery
}
