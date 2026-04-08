package com.flowboard.board_service.dto;

import com.flowboard.board_service.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBoardRequest {

    @NotBlank(message = "Board name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;

    private String description;
    private String background;
    private Visibility visibility;
}
