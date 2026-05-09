package com.flowboard.workspace_service.service;

import com.flowboard.workspace_service.config.RabbitMQConfig;
import com.flowboard.workspace_service.dto.*;
import com.flowboard.workspace_service.entity.Workspace;
import com.flowboard.workspace_service.entity.WorkspaceInvitation;
import com.flowboard.workspace_service.entity.WorkspaceMember;
import com.flowboard.workspace_service.enums.MemberRole;
import com.flowboard.workspace_service.enums.Visibility;
import com.flowboard.workspace_service.event.WorkspaceInviteEvent;
import com.flowboard.workspace_service.exception.CustomException;
import com.flowboard.workspace_service.repository.WorkspaceInvitationRepository;
import com.flowboard.workspace_service.repository.WorkspaceMemberRepository;
import com.flowboard.workspace_service.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceInvitationRepository invitationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long ownerId) {

        if (workspaceRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new CustomException(
                    "You already have a workspace named '" + request.getName() + "'",
                    HttpStatus.BAD_REQUEST);
        }

        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(ownerId)
                .visibility(request.getVisibility() != null
                        ? request.getVisibility() : Visibility.PRIVATE)
                .logoUrl(request.getLogoUrl())
                .createdAt(LocalDateTime.now())
                .build();

        workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(workspace)
                .userId(ownerId)
                .role(MemberRole.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepository.save(ownerMember);

        log.info("Workspace created: id={} name={} owner={}",
                workspace.getId(), workspace.getName(), ownerId);
        return toResponse(workspace);
    }

    @Override
    @Transactional
    public void inviteMember(Long workspaceId,
                             InviteMemberRequest request,
                             Long invitedBy,
                             String userRole) {

        Workspace workspace = findWorkspace(workspaceId);
        requireAdmin(workspaceId, invitedBy, userRole);

        // Prevent duplicate pending invitations
        if (invitationRepository.existsByWorkspaceIdAndInviteeEmailAndStatus(
                workspaceId, request.getEmail(), "PENDING")) {
            throw new CustomException(
                    "A pending invitation already exists for this email",
                    HttpStatus.BAD_REQUEST);
        }

        // NOTE: Checking whether the invitee is already a member by userId requires a
        //       Feign call to auth-service to resolve email → userId.
        //       TODO: inject AuthFeignClient, resolve userId, then call
        //             memberRepository.existsByWorkspaceIdAndUserId(workspaceId, resolvedUserId)

        String token = UUID.randomUUID().toString();
        String acceptUrl = "http://localhost:4200/invite/accept?token=" + token;

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspaceId(workspaceId)
                .invitedBy(invitedBy)
                .inviteeEmail(request.getEmail())
                .role(request.getRole())
                .token(token)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        invitationRepository.save(invitation);

        WorkspaceInviteEvent event = new WorkspaceInviteEvent(
                workspaceId,
                workspace.getName(),
                request.getEmail(),
                token,
                request.getRole().name(),
                invitedBy,
                acceptUrl);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FLOWBOARD_EXCHANGE,
                RabbitMQConfig.INVITE_KEY,
                event);

        log.info("Invitation sent: workspaceId={} email={} role={}",
                workspaceId, request.getEmail(), request.getRole());
    }

    @Override
    @Transactional
    public void acceptInvitation(String token, Long userId) {
        WorkspaceInvitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new CustomException(
                        "Invalid invitation token", HttpStatus.NOT_FOUND));

        if (!"PENDING".equals(inv.getStatus())) {
            throw new CustomException(
                    "This invitation is no longer valid", HttpStatus.BAD_REQUEST);
        }

        if (LocalDateTime.now().isAfter(inv.getExpiresAt())) {
            inv.setStatus("EXPIRED");
            invitationRepository.save(inv);
            throw new CustomException(
                    "Invitation has expired. Please request a new one.",
                    HttpStatus.BAD_REQUEST);
        }

        // Already a member — mark accepted and silently succeed
        if (memberRepository.existsByWorkspaceIdAndUserId(inv.getWorkspaceId(), userId)) {
            inv.setStatus("ACCEPTED");
            inv.setAcceptedAt(LocalDateTime.now());
            invitationRepository.save(inv);
            return;
        }

        Workspace workspace = findWorkspace(inv.getWorkspaceId());

        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .userId(userId)
                .role(inv.getRole())
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);

        inv.setStatus("ACCEPTED");
        inv.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(inv);

        log.info("Invitation accepted: userId={} workspaceId={} role={}",
                userId, inv.getWorkspaceId(), inv.getRole());
    }

    @Override
    @Transactional
    public void revokeInvitation(Long workspaceId, Long invitationId,
                                 Long requesterId, String userRole) {
        requireAdmin(workspaceId, requesterId, userRole);

        WorkspaceInvitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new CustomException(
                        "Invitation not found", HttpStatus.NOT_FOUND));

        if (!inv.getWorkspaceId().equals(workspaceId)) {
            throw new CustomException(
                    "Invitation does not belong to this workspace",
                    HttpStatus.BAD_REQUEST);
        }

        if (!"PENDING".equals(inv.getStatus())) {
            throw new CustomException(
                    "Only pending invitations can be revoked",
                    HttpStatus.BAD_REQUEST);
        }

        inv.setStatus("REVOKED");
        invitationRepository.save(inv);
        log.info("Invitation revoked: id={} by userId={}", invitationId, requesterId);
    }

    @Override
    public List<WorkspaceInvitation> getPendingInvitations(Long workspaceId,
                                                           Long requesterId,
                                                           String userRole) {
        requireAdmin(workspaceId, requesterId, userRole);
        return invitationRepository.findByWorkspaceIdAndStatus(workspaceId, "PENDING");
    }

    @Override
    public List<WorkspaceResponse> getAllWorkspaces() {
        return workspaceRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public WorkspaceResponse getById(Long workspaceId, Long requesterId, String userRole) {
        Workspace workspace = findWorkspace(workspaceId);
        if (workspace.getVisibility() == Visibility.PRIVATE) {
            requireMember(workspaceId, requesterId, userRole);
        }
        return toResponse(workspace);
    }

    @Override
    public List<WorkspaceResponse> getByOwner(Long ownerId) {
        return workspaceRepository.findByOwnerId(ownerId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkspaceResponse> getByMember(Long userId) {
        return workspaceRepository.findByMemberUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkspaceResponse> getPublicWorkspaces() {
        return workspaceRepository.findByVisibility(Visibility.PUBLIC)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public WorkspaceResponse updateWorkspace(Long workspaceId,
                                             UpdateWorkspaceRequest request,
                                             Long requesterId,
                                             String userRole) {
        Workspace workspace = findWorkspace(workspaceId);
        requireAdmin(workspaceId, requesterId, userRole);

        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());

        if (request.getVisibility() != null) {
            workspace.setVisibility(request.getVisibility());
        }
        if (request.getLogoUrl() != null) {
            workspace.setLogoUrl(request.getLogoUrl());
        }

        workspace.setUpdatedAt(LocalDateTime.now());
        workspaceRepository.save(workspace);

        log.info("Workspace updated: id={}", workspaceId);
        return toResponse(workspace);
    }

    @Override
    @Transactional
    public void deleteWorkspace(Long workspaceId, Long requesterId, String userRole) {
        Workspace workspace = findWorkspace(workspaceId);

        if (!"PLATFORM_ADMIN".equals(userRole) && !workspace.getOwnerId().equals(requesterId)) {
            throw new CustomException(
                    "Only the workspace owner can delete it", HttpStatus.FORBIDDEN);
        }

        workspaceRepository.delete(workspace);
        log.info("Workspace deleted: id={}", workspaceId);
    }

    @Override
    @Transactional
    public WorkspaceMember addMember(Long workspaceId,
                                     AddMemberRequest request,
                                     Long requesterId,
                                     String userRole) {
        requireAdmin(workspaceId, requesterId, userRole);

        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, request.getUserId())) {
            throw new CustomException(
                    "User is already a member of this workspace", HttpStatus.BAD_REQUEST);
        }

        Workspace workspace = findWorkspace(workspaceId);
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .userId(request.getUserId())
                .role(request.getRole() != null ? request.getRole() : MemberRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);
        log.info("Member added: workspaceId={} userId={} role={}",
                workspaceId, request.getUserId(), member.getRole());
        return member;
    }

    @Override
    @Transactional
    public void removeMember(Long workspaceId, Long userId, Long requesterId, String userRole) {
        findWorkspace(workspaceId);
        requireAdmin(workspaceId, requesterId, userRole);

        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new CustomException(
                    "User is not a member of this workspace", HttpStatus.NOT_FOUND);
        }

        Workspace workspace = findWorkspace(workspaceId);
        if (workspace.getOwnerId().equals(userId)) {
            throw new CustomException(
                    "Cannot remove the workspace owner", HttpStatus.BAD_REQUEST);
        }

        memberRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId);
        log.info("Member removed: workspaceId={} userId={}", workspaceId, userId);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long workspaceId, Long userId,
                                 UpdateMemberRoleRequest request,
                                 Long requesterId, String userRole) {
        findWorkspace(workspaceId);
        requireAdmin(workspaceId, requesterId, userRole);

        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new CustomException(
                        "User is not a member of this workspace", HttpStatus.NOT_FOUND));

        member.setRole(request.getRole());
        memberRepository.save(member);

        // FIX: typo "workdspaceId" → "workspaceId" in log statement
        log.info("Member role updated: workspaceId={} userId={} newRole={}",
                workspaceId, userId, request.getRole());
    }

    @Override
    public List<WorkspaceMember> getMembers(Long workspaceId) {
        findWorkspace(workspaceId);
        return memberRepository.findByWorkspaceId(workspaceId);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Workspace findWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(
                        "Workspace not found", HttpStatus.NOT_FOUND));
    }

    private void requireMember(Long workspaceId, Long userId, String userRole) {
        if ("PLATFORM_ADMIN".equals(userRole)) return;
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new CustomException(
                    "Access denied — you are not a member of this workspace",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void requireAdmin(Long workspaceId, Long userId, String userRole) {
        if ("PLATFORM_ADMIN".equals(userRole)) return;
        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new CustomException(
                        "Access denied — you are not a member of this workspace",
                        HttpStatus.FORBIDDEN));

        if (member.getRole() != MemberRole.ADMIN) {
            throw new CustomException(
                    "Access denied — admin role required", HttpStatus.FORBIDDEN);
        }
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        List<WorkspaceResponse.MemberDto> memberDtos = memberRepository
                .findByWorkspaceId(workspace.getId())
                .stream()
                .map(m -> WorkspaceResponse.MemberDto.builder()
                        .userId(m.getUserId())
                        .role(m.getRole())
                        .joinedAt(m.getJoinedAt())
                        .build())
                .toList();

        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .ownerId(workspace.getOwnerId())
                .visibility(workspace.getVisibility())
                .logoUrl(workspace.getLogoUrl())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .members(memberDtos)
                .build();
    }
}
