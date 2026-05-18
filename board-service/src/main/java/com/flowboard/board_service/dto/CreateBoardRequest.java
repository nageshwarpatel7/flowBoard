package com.flowboard.board_service.dto;

import com.flowboard.board_service.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Create board request")
public class CreateBoardRequest {

    @NotNull(message = "Workspace ID is required")
    private Long workspaceId;

    @Schema(example = "Project Management Board")
    @NotBlank(message = "Board name is required")
    @Size(min = 2, max = 100, message = "Name nust be 2-100 characters")
    private String name;

    @Schema(example = "Workspace for team")
    private String description;

    private String background;

    private Visibility visibility = Visibility.PRIVATE;
}
