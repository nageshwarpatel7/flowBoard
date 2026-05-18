package com.flowBoard.list_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateListRequest {

    @NotNull(message = "Board ID is required")
    private Long boardId;

    @NotBlank(message = "List name is required")
    @Size(min=1, max = 100, message = "Name must be 1-100 characters")
    private String name;

    private Integer position;

    private String color;
}
