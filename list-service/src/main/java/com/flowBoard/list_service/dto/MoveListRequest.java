package com.flowBoard.list_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveListRequest {

    @NotNull(message = "Target board ID is required")
    private Long targetBoardId;

    private Integer targetPosition;
}
