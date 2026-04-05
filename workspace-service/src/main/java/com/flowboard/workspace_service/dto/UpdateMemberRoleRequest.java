package com.flowboard.workspace_service.dto;

import com.flowboard.workspace_service.enums.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {
    @NotNull(message = "Role is required")
    private MemberRole role;
}
