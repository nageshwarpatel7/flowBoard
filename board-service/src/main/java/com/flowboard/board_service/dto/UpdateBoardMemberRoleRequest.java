package com.flowboard.board_service.dto;

import com.flowboard.board_service.enums.BoardMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBoardMemberRoleRequest {

    @NotNull(message = "Role is required")
    private BoardMemberRole role;
}
