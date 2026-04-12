package com.flowboard.card_service.dto;

import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CardResponse {

    private Long id;
    private Long listId;
    private Long boardId;
    private String title;
    private String description;
    private Integer position;
    private Priority priority;
    private CardStatus status;
    private LocalDate dueDate;
    private LocalDate startDate;
    private Long assigneeId;
    private Long createdById;
    private boolean isArchived;
    private boolean isOverdue;
    private String coverColor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
