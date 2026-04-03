package com.flowBoard.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    private String fullname;

    @NotBlank(message = "Username is required")
    private String username;

    private String avatarUrl;
    private String bio;
}
