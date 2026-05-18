package com.flowboard.card_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CardActivityResponse {

    private Long id;
    private Long cardId;
    private Long actorId;
    private String actionType;
    private String description;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
}
