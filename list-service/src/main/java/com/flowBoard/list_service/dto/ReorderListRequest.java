package com.flowBoard.list_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderListRequest {

    @NotNull(message = "Board ID is required")
    private Long boardId;

    @NotEmpty(message = "List order is required")
    private List<Long> orderedListIds;
}
