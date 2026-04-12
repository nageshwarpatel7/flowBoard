package com.flowboard.card_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveCardRequest {

    @NotNull(message = "Target list ID is required")
    private Long targetListId;

    @NotNull(message = "Target board ID is required")
    private Long targetBoardId;

    private Integer targetPosition;
}
