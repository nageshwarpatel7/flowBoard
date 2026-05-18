package com.flowboard.workspace_service.service;

import com.flowboard.workspace_service.dto.*;
import com.flowboard.workspace_service.entity.WorkspaceMember;

import java.util.List;

public interface WorkspaceService {

    // workspace CRUD
    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long ownerId);
    WorkspaceResponse getById(Long workspaceId, Long requesterId, String userRole);
    List<WorkspaceResponse> getByOwner(Long ownerId);
    List<WorkspaceResponse> getByMember(Long userId);
    List<WorkspaceResponse> getPublicWorkspaces();
    List<WorkspaceResponse> getAllWorkspaces();
    WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request, Long requesterId, String userRole);
    void deleteWorkspace(Long workspaceId, Long requesterId, String userRole);

    // Member management
    WorkspaceMember addMember(Long workspaceId, AddMemberRequest request, Long requesterId, String userRole);
    void removeMember(Long workspaceId, Long userId, Long requesterId, String userRole);
    void updateMemberRole(Long workspaceId,Long userId, UpdateMemberRoleRequest request, Long requesterId, String userRole);
    List<WorkspaceMember> getMembers(Long workspaceId);

    void inviteMember(Long workspaceId, InviteMemberRequest request, Long requesterId, String userRole);
    void acceptInvitation(String token, Long userId);
    void revokeInvitation(Long workspaceId, Long invitationId, Long requesterId, String userRole);
    List<com.flowboard.workspace_service.entity.WorkspaceInvitation>
    getPendingInvitations(Long workspaceId, Long requesterId, String userRole);
}

