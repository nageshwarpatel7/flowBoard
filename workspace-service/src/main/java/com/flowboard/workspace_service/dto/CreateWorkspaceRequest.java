package com.flowboard.workspace_service.dto;

import com.flowboard.workspace_service.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    @Size(min=2, max=50, message = "Name must be 2-50 charcters long")
    private String name;

    private String description;

    private Visibility visibility= Visibility.PRIVATE;

    private String logoUrl;
}
