package com.flowboard.workspace_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceInviteEvent implements Serializable {
    private Long workspaceId;
    private String workspaceName;
    private String inviteeEmail;
    private String token;
    private String role;
    private Long invitedByUserId;
    private Long inviteeUserId;
    private String acceptUrl;    // http://localhost:4200/invite/accept?token=xxx
}
