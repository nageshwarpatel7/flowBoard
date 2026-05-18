package com.flowboard.workspace_service.repository;

import com.flowboard.workspace_service.entity.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceInvitationRepository
        extends JpaRepository<WorkspaceInvitation, Long> {

    Optional<WorkspaceInvitation> findByToken(String token);

    List<WorkspaceInvitation> findByWorkspaceIdAndStatus(
            Long workspaceId, String status);

    boolean existsByWorkspaceIdAndInviteeEmailAndStatus(
            Long workspaceId, String email, String status);

    List<WorkspaceInvitation> findByInviteeEmail(String email);
}