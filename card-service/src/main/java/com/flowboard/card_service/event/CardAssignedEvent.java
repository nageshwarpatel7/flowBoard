package com.flowboard.card_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardAssignedEvent implements Serializable {
    private Long cardId;
    private Long boardId;
    private String cardTitle;
    private Long assigneeId;
    private Long assignedByUserId;
    private String assigneeEmail;  // for email notification
    private String assignedByName;
}