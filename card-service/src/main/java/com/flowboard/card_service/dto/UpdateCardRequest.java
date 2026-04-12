package com.flowboard.card_service.dto;

import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCardRequest {

    @NotBlank(message = "Title is required")
    @Size(min=1, max = 255)
    private String title;

    private String description;
    private Priority priority;
    private CardStatus status;
    private LocalDate dueDate;
    private LocalDate startDate;
    private String coverColor;
}
