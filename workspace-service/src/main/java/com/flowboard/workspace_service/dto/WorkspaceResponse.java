package com.flowboard.workspace_service.dto;

import com.flowboard.workspace_service.enums.MemberRole;
import com.flowboard.workspace_service.enums.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private Visibility visibility;
    private String logoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MemberDto> members;

    @Data
    @Builder
    public static class MemberDto{
        private Long userId;
        private MemberRole role;
        private LocalDateTime joinedAt;
    }
}
