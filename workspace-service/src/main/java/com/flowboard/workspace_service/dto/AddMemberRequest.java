package com.flowboard.workspace_service.dto;

import com.flowboard.workspace_service.enums.MemberRole;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotBlank(message = "User ID is required")
    private Long userId;

    private MemberRole role = MemberRole.MEMBER;
}
