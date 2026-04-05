package com.flowboard.workspace_service.controller;

import com.flowboard.workspace_service.dto.*;
import com.flowboard.workspace_service.entity.WorkspaceMember;
import com.flowboard.workspace_service.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
            @RequestHeader("X-User-Id") Long userId
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.createWorkspace(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId){
        return ResponseEntity.ok(workspaceService.getById(id, userId));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<WorkspaceResponse>> getByOwner(
            @PathVariable Long ownerId){
        return ResponseEntity.ok(workspaceService.getByOwner(ownerId));
    }

    @GetMapping("/member/{userId}")
    public ResponseEntity<List<WorkspaceResponse>> getByMember(
            @PathVariable Long userId){
        return ResponseEntity.ok(workspaceService.getByMember(userId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<WorkspaceResponse>> getPublic(){
        return ResponseEntity.ok(workspaceService.getPublicWorkspaces());
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            @RequestHeader("X-User-Id") Long userId){
        return ResponseEntity.ok(workspaceService.updateWorkspace(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId){
        workspaceService.deleteWorkspace(id, userId);
        return ResponseEntity.ok("Workspace deleted successfully");
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<WorkspaceMember> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            @RequestHeader("X-User-Id") Long userId){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.addMember(id,request, userId));
    }

    @DeleteMapping("/{id}/member/{memberId}")
    public ResponseEntity<String> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader("X-User-Id") Long userId){
        workspaceService.removeMember(id, memberId, userId);
        return ResponseEntity.ok("Member removed successfully");
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<String> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @RequestHeader("X-User-Id") Long userId){
        workspaceService.updateMemberRole(id, memberId, request, userId);
        return ResponseEntity.ok("Member role updated successfully");
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceMember>> getMembers(
            @PathVariable Long id){
        return ResponseEntity.ok(workspaceService.getMembers(id));
    }
}
