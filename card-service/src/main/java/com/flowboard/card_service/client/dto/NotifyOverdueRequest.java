package com.flowboard.card_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyOverdueRequest {
    private Long recipientId;
    private Long cardId;
    private String cardTitle;
    private String dueDate;
    private String recipientEmail;
}
