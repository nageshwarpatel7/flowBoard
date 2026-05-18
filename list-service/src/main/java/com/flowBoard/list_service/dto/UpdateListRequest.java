package com.flowBoard.list_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateListRequest {

    @NotBlank(message = "List name is required")
    @Size(min = 1, max = 100, message = "Name must be 1-100 characters")
    private String name;

    private String color;
}
