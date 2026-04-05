package com.flowboard.workspace_service.service;

import com.flowboard.workspace_service.dto.*;
import com.flowboard.workspace_service.entity.WorkspaceMember;

import java.util.List;

public interface WorkspaceService {

    // workspace CRUD
    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long ownerId);
    WorkspaceResponse getById(Long workspaceId, Long requesterId);
    List<WorkspaceResponse> getByOwner(Long ownerId);
    List<WorkspaceResponse> getByMember(Long userId);
    List<WorkspaceResponse> getPublicWorkspaces();
    WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request, Long requesterId);
    void deleteWorkspace(Long workspaceId, Long requesterId);

    // Member management
    WorkspaceMember addMember(Long workspaceId, AddMemberRequest request, Long requesterId);
    void removeMember(Long workspaceId, Long userId, Long requesterId);
    void updateMemberRole(Long workspaceId,Long userId, UpdateMemberRoleRequest requesr, Long requesterId);
    List<WorkspaceMember> getMembers(Long workspaceId);
}
