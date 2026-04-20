package com.flowboard.workspace_service.service;

import com.flowboard.workspace_service.dto.*;
import com.flowboard.workspace_service.entity.*;
import com.flowboard.workspace_service.enums.MemberRole;
import com.flowboard.workspace_service.enums.Visibility;
import com.flowboard.workspace_service.exception.CustomException;
import com.flowboard.workspace_service.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceServiceImpl – full coverage suite")
class WorkspaceServiceImplTest {

    @Mock WorkspaceRepository       workspaceRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock WorkspaceInvitationRepository invitationRepository;
    @Mock RabbitTemplate            rabbitTemplate;

    @InjectMocks WorkspaceServiceImpl workspaceService;

    private Workspace        workspace;
    private WorkspaceMember  adminMember;
    private WorkspaceMember  regularMember;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder()
                .id(1L).name("Dev Team").ownerId(1L)
                .visibility(Visibility.PRIVATE)
                .createdAt(LocalDateTime.now()).build();

        adminMember = WorkspaceMember.builder()
                .id(1L).workspace(workspace).userId(1L)
                .role(MemberRole.ADMIN).joinedAt(LocalDateTime.now()).build();

        regularMember = WorkspaceMember.builder()
                .id(2L).workspace(workspace).userId(2L)
                .role(MemberRole.MEMBER).joinedAt(LocalDateTime.now()).build();
    }

    // ── createWorkspace ────────────────────────────────────────────────────────

    @Test @DisplayName("createWorkspace – success, owner added as ADMIN")
    void createWorkspace_success() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("Dev Team"); req.setVisibility(Visibility.PRIVATE);

        when(workspaceRepository.existsByNameAndOwnerId("Dev Team", 1L)).thenReturn(false);
        when(workspaceRepository.save(any())).thenReturn(workspace);
        when(memberRepository.save(any())).thenReturn(adminMember);
        when(memberRepository.findByWorkspaceId(any())).thenReturn(List.of(adminMember));

        WorkspaceResponse response = workspaceService.createWorkspace(req, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Dev Team");
        verify(memberRepository).save(argThat(m ->
                m.getRole() == MemberRole.ADMIN && m.getUserId().equals(1L)));
    }

    @Test @DisplayName("createWorkspace – duplicate name throws 400")
    void createWorkspace_duplicate_throws() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("Dev Team");
        when(workspaceRepository.existsByNameAndOwnerId("Dev Team", 1L)).thenReturn(true);

        assertThatThrownBy(() -> workspaceService.createWorkspace(req, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(workspaceRepository, never()).save(any());
    }

    @Test @DisplayName("createWorkspace – default visibility is PRIVATE when null")
    void createWorkspace_defaultVisibility() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("New WS"); req.setVisibility(null);

        when(workspaceRepository.existsByNameAndOwnerId(anyString(), anyLong())).thenReturn(false);
        when(workspaceRepository.save(any())).thenReturn(workspace);
        when(memberRepository.save(any())).thenReturn(adminMember);
        when(memberRepository.findByWorkspaceId(any())).thenReturn(List.of(adminMember));

        workspaceService.createWorkspace(req, 1L);

        verify(workspaceRepository).save(argThat(w -> w.getVisibility() == Visibility.PRIVATE));
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test @DisplayName("getById – public workspace accessible to non-member")
    void getById_public_accessible() {
        workspace.setVisibility(Visibility.PUBLIC);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(adminMember));

        WorkspaceResponse response = workspaceService.getById(1L, 99L);
        assertThat(response).isNotNull();
    }

    @Test @DisplayName("getById – private workspace throws 403 for non-member")
    void getById_private_nonMember_throws() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> workspaceService.getById(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @DisplayName("getById – private workspace accessible to member")
    void getById_private_member_success() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 1L)).thenReturn(true);
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(adminMember));

        WorkspaceResponse response = workspaceService.getById(1L, 1L);
        assertThat(response).isNotNull();
    }

    @Test @DisplayName("getById – throws 404 when workspace not found")
    void getById_notFound_throws() {
        when(workspaceRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> workspaceService.getById(999L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── getByOwner / getByMember / getPublic ───────────────────────────────────

    @Test @DisplayName("getByOwner – returns all owned workspaces")
    void getByOwner_success() {
        when(workspaceRepository.findByOwnerId(1L)).thenReturn(List.of(workspace));
        when(memberRepository.findByWorkspaceId(anyLong())).thenReturn(List.of(adminMember));

        List<WorkspaceResponse> result = workspaceService.getByOwner(1L);
        assertThat(result).hasSize(1);
    }

    @Test @DisplayName("getByMember – returns member workspaces")
    void getByMember_success() {
        when(workspaceRepository.findByMemberUserId(2L)).thenReturn(List.of(workspace));
        when(memberRepository.findByWorkspaceId(anyLong())).thenReturn(List.of(adminMember));

        List<WorkspaceResponse> result = workspaceService.getByMember(2L);
        assertThat(result).hasSize(1);
    }

    @Test @DisplayName("getPublicWorkspaces – returns only PUBLIC visibility workspaces")
    void getPublicWorkspaces_success() {
        workspace.setVisibility(Visibility.PUBLIC);
        when(workspaceRepository.findByVisibility(Visibility.PUBLIC)).thenReturn(List.of(workspace));
        when(memberRepository.findByWorkspaceId(anyLong())).thenReturn(List.of(adminMember));

        List<WorkspaceResponse> result = workspaceService.getPublicWorkspaces();
        assertThat(result).hasSize(1);
    }

    // ── updateWorkspace ────────────────────────────────────────────────────────

    @Test @DisplayName("updateWorkspace – admin can update name and description")
    void updateWorkspace_success() {
        UpdateWorkspaceRequest req = new UpdateWorkspaceRequest();
        req.setName("Updated"); req.setDescription("New desc");
        req.setVisibility(Visibility.PUBLIC); req.setLogoUrl("https://logo.png");

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(workspaceRepository.save(any())).thenReturn(workspace);
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(adminMember));

        WorkspaceResponse response = workspaceService.updateWorkspace(1L, req, 1L);
        assertThat(response).isNotNull();
        verify(workspaceRepository).save(argThat(w -> "Updated".equals(w.getName())));
    }

    @Test @DisplayName("updateWorkspace – non-admin throws 403")
    void updateWorkspace_nonAdmin_throws() {
        UpdateWorkspaceRequest req = new UpdateWorkspaceRequest();
        req.setName("X");

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 2L)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> workspaceService.updateWorkspace(1L, req, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── deleteWorkspace ────────────────────────────────────────────────────────

    @Test @DisplayName("deleteWorkspace – owner can delete")
    void deleteWorkspace_owner_success() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        workspaceService.deleteWorkspace(1L, 1L);
        verify(workspaceRepository).delete(workspace);
    }

    @Test @DisplayName("deleteWorkspace – non-owner throws 403")
    void deleteWorkspace_nonOwner_throws() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        assertThatThrownBy(() -> workspaceService.deleteWorkspace(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(workspaceRepository, never()).delete(any());
    }

    // ── addMember ──────────────────────────────────────────────────────────────

    @Test @DisplayName("addMember – admin can add new member")
    void addMember_success() {
        AddMemberRequest req = new AddMemberRequest();
        req.setUserId(3L); req.setRole(MemberRole.MEMBER);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 3L)).thenReturn(false);
        when(memberRepository.save(any())).thenReturn(regularMember);

        workspaceService.addMember(1L, req, 1L);
        verify(memberRepository).save(any());
    }

    @Test @DisplayName("addMember – throws 400 when user already member")
    void addMember_alreadyMember_throws() {
        AddMemberRequest req = new AddMemberRequest(); req.setUserId(1L);

        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> workspaceService.addMember(1L, req, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("already a member");
    }

    @Test @DisplayName("addMember – default role is MEMBER when null")
    void addMember_defaultRole() {
        AddMemberRequest req = new AddMemberRequest(); req.setUserId(5L); req.setRole(null);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 5L)).thenReturn(false);
        when(memberRepository.save(any())).thenReturn(regularMember);

        workspaceService.addMember(1L, req, 1L);
        verify(memberRepository).save(argThat(m -> m.getRole() == MemberRole.MEMBER));
    }

    // ── removeMember ───────────────────────────────────────────────────────────

    @Test @DisplayName("removeMember – admin removes regular member")
    void removeMember_success() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 2L)).thenReturn(true);

        workspaceService.removeMember(1L, 2L, 1L);
        verify(memberRepository).deleteByWorkspaceIdAndUserId(1L, 2L);
    }

    @Test @DisplayName("removeMember – throws 400 when removing the owner")
    void removeMember_owner_throws() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 1L)).thenReturn(true);

        // workspace.ownerId = 1L, trying to remove userId = 1L
        assertThatThrownBy(() -> workspaceService.removeMember(1L, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("owner");
    }

    @Test @DisplayName("removeMember – throws 404 when user not member")
    void removeMember_notMember_throws() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> workspaceService.removeMember(1L, 99L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── updateMemberRole ────────────────────────────────────────────────────────

    @Test @DisplayName("updateMemberRole – admin can promote member to admin")
    void updateMemberRole_success() {
        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRole(MemberRole.ADMIN);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 2L)).thenReturn(Optional.of(regularMember));
        when(memberRepository.save(any())).thenReturn(regularMember);

        workspaceService.updateMemberRole(1L, 2L, req, 1L);
        assertThat(regularMember.getRole()).isEqualTo(MemberRole.ADMIN);
    }

    @Test @DisplayName("updateMemberRole – throws 404 when target user not a member")
    void updateMemberRole_targetNotMember_throws() {
        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRole(MemberRole.ADMIN);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.updateMemberRole(1L, 99L, req, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── getMembers ─────────────────────────────────────────────────────────────

    @Test @DisplayName("getMembers – returns list of all workspace members")
    void getMembers_success() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(adminMember, regularMember));

        List<WorkspaceMember> members = workspaceService.getMembers(1L);
        assertThat(members).hasSize(2);
    }

    // ── inviteMember ───────────────────────────────────────────────────────────

    @Test @DisplayName("inviteMember – admin sends valid invite")
    void inviteMember_success() {
        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("new@example.com"); req.setRole(MemberRole.MEMBER);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(invitationRepository.existsByWorkspaceIdAndInviteeEmailAndStatus(
                1L, "new@example.com", "PENDING")).thenReturn(false);
        when(invitationRepository.save(any())).thenReturn(new WorkspaceInvitation());

        workspaceService.inviteMember(1L, req, 1L);
        verify(invitationRepository).save(any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test @DisplayName("inviteMember – throws 400 on duplicate pending invite")
    void inviteMember_duplicate_throws() {
        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("dup@example.com"); req.setRole(MemberRole.MEMBER);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(invitationRepository.existsByWorkspaceIdAndInviteeEmailAndStatus(
                1L, "dup@example.com", "PENDING")).thenReturn(true);

        assertThatThrownBy(() -> workspaceService.inviteMember(1L, req, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("pending invitation");
    }

    // ── acceptInvitation ────────────────────────────────────────────────────────

    @Test @DisplayName("acceptInvitation – valid token adds user as member")
    void acceptInvitation_success() {
        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id(1L).workspaceId(1L).inviteeEmail("new@ex.com")
                .role(MemberRole.MEMBER).token("valid-token")
                .status("PENDING").createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7)).build();

        when(invitationRepository.findByToken("valid-token")).thenReturn(Optional.of(inv));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 5L)).thenReturn(false);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.save(any())).thenReturn(regularMember);

        workspaceService.acceptInvitation("valid-token", 5L);

        assertThat(inv.getStatus()).isEqualTo("ACCEPTED");
        verify(memberRepository).save(any());
    }

    @Test @DisplayName("acceptInvitation – throws 404 for invalid token")
    void acceptInvitation_invalidToken_throws() {
        when(invitationRepository.findByToken("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> workspaceService.acceptInvitation("bad", 5L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @DisplayName("acceptInvitation – throws 400 for already-accepted invite")
    void acceptInvitation_alreadyAccepted_throws() {
        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .token("t").status("ACCEPTED").build();
        when(invitationRepository.findByToken("t")).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> workspaceService.acceptInvitation("t", 5L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @DisplayName("acceptInvitation – expired invite sets EXPIRED status")
    void acceptInvitation_expired_throws() {
        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .token("exp").status("PENDING")
                .expiresAt(LocalDateTime.now().minusDays(1)).build();
        when(invitationRepository.findByToken("exp")).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> workspaceService.acceptInvitation("exp", 5L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("expired");
        assertThat(inv.getStatus()).isEqualTo("EXPIRED");
    }

    // ── revokeInvitation ────────────────────────────────────────────────────────

    @Test @DisplayName("revokeInvitation – admin can revoke pending invitation")
    void revokeInvitation_success() {
        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id(1L).workspaceId(1L).status("PENDING").build();

        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));

        workspaceService.revokeInvitation(1L, 1L, 1L);
        assertThat(inv.getStatus()).isEqualTo("REVOKED");
    }

    @Test @DisplayName("revokeInvitation – throws 400 when invitation is not PENDING")
    void revokeInvitation_notPending_throws() {
        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id(2L).workspaceId(1L).status("ACCEPTED").build();

        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(invitationRepository.findById(2L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> workspaceService.revokeInvitation(1L, 2L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}