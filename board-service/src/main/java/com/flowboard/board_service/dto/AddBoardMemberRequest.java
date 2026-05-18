package com.flowboard.board_service.dto;

import com.flowboard.board_service.enums.BoardMemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddBoardMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private BoardMemberRole role = BoardMemberRole.MEMBER;
}
