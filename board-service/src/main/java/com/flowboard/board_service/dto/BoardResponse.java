package com.flowboard.board_service.dto;

import com.flowboard.board_service.enums.BoardMemberRole;
import com.flowboard.board_service.enums.Visibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BoardResponse {

    private Long id;
    private Long workspaceId;
    private String name;
    private String description;
    private String background;
    private Visibility visibility;
    private Long createdById;
    private boolean isClosed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int memberCount;
    private List<MemberDTO> members;

    private BoardAnalytics analytics;

    @Data
    @Builder
    public static class MemberDTO{
        private Long userId;
        private BoardMemberRole role;
        private LocalDateTime addedAt;
    }

    @Data
    @Builder
    public static class BoardAnalytics{
        private long totalMembers;
        private long observerCount;
        private long memberCount;
        private long adminCount;
    }
}
