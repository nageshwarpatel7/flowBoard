package com.flowboard.workspace_service.dto;

import com.flowboard.workspace_service.enums.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private MemberRole role = MemberRole.MEMBER;
}
