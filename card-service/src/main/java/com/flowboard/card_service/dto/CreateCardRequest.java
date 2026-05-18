package com.flowboard.card_service.dto;

import com.flowboard.card_service.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateCardRequest {

    @NotNull(message = "List Id is required")
    private Long listId;

    @NotNull(message = "Board Id is required")
    private Long boardId;

    @NotBlank(message = "Title is required")
    @Size(min = 1, max=255, message = "Title must be 1-255 characters")
    private String title;

    private String description;

    private Integer position;

    private Priority priority = Priority.MEDIUM;

    private LocalDate dueDate;

    private LocalDate startDate;

    private Long assigneeId;

    private String coverColor;
}
