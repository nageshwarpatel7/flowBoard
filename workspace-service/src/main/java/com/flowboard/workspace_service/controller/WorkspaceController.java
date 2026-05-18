package com.flowboard.workspace_service.controller;

import com.flowboard.workspace_service.dto.*;
import com.flowboard.workspace_service.entity.WorkspaceInvitation;
import com.flowboard.workspace_service.entity.WorkspaceMember;
import com.flowboard.workspace_service.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.createWorkspace(request, resolveUserId(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(workspaceService.getById(id, resolveUserId(userId), userRole));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspaces() {
        return ResponseEntity.ok(workspaceService.getAllWorkspaces());
    }


    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<WorkspaceResponse>> getByOwner(
            @PathVariable Long ownerId) {
        return ResponseEntity.ok(workspaceService.getByOwner(ownerId));
    }

    @GetMapping("/member/{userId}")
    public ResponseEntity<List<WorkspaceResponse>> getByMember(
            @PathVariable Long userId) {
        return ResponseEntity.ok(workspaceService.getByMember(userId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<WorkspaceResponse>> getPublic() {
        return ResponseEntity.ok(workspaceService.getPublicWorkspaces());
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(
                workspaceService.updateWorkspace(id, request, resolveUserId(userId), userRole));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        workspaceService.deleteWorkspace(id, resolveUserId(userId), userRole);
        return ResponseEntity.ok("Workspace deleted successfully");
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<WorkspaceMember> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.addMember(id, request, resolveUserId(userId), userRole));
    }

    @DeleteMapping("/{id}/member/{memberId}")
    public ResponseEntity<String> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        workspaceService.removeMember(id, memberId, resolveUserId(userId), userRole);
        return ResponseEntity.ok("Member removed successfully");
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<String> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        workspaceService.updateMemberRole(id, memberId, request, resolveUserId(userId), userRole);
        return ResponseEntity.ok("Member role updated successfully");
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceMember>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.getMembers(id));
    }

    // ── Invitation endpoints (FIXED) ──────────────────────────────────────────

    @PostMapping("/{id}/invite")
    public ResponseEntity<String> inviteMember(
            @PathVariable Long id,
            @Valid @RequestBody InviteMemberRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        workspaceService.inviteMember(id, request, resolveUserId(userId), userRole);
        return ResponseEntity.ok("Invitation sent to " + request.getEmail());
    }

    // This endpoint is called from the link in the invitation email
    // Token comes as query param → user must be logged in (JWT required)
    @GetMapping("/invite/accept")
    public ResponseEntity<String> acceptInvitation(
            @RequestParam String token,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        workspaceService.acceptInvitation(token, resolveUserId(userId));
        return ResponseEntity.ok(
                "Invitation accepted! You have joined the workspace.");
    }

    // Revoke a pending invitation (ADMIN only)
    @DeleteMapping("/{id}/invitations/{invitationId}")
    public ResponseEntity<String> revokeInvitation(
            @PathVariable Long id,
            @PathVariable Long invitationId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        workspaceService.revokeInvitation(id, invitationId, resolveUserId(userId), userRole);
        return ResponseEntity.ok("Invitation revoked");
    }

    // Get all pending invitations for a workspace (ADMIN only)
    @GetMapping("/{id}/invitations")
    public ResponseEntity<List<com.flowboard.workspace_service.entity.WorkspaceInvitation>> getPendingInvitations(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(
                workspaceService.getPendingInvitations(id, resolveUserId(userId), userRole));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Long resolveUserId(Long userId) {
        if (userId != null) return userId;
        throw new com.flowboard.workspace_service.exception.CustomException(
                "X-User-Id header is required",
                org.springframework.http.HttpStatus.BAD_REQUEST);
    }
}