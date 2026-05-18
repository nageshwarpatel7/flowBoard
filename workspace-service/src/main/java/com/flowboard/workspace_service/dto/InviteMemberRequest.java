package com.flowboard.workspace_service.dto;

import com.flowboard.workspace_service.enums.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteMemberRequest {

    @Email
    @NotBlank
    private String email;

    @NotNull(message = "member role is required")
    private MemberRole role;
}
