package com.flowboard.workspace_service.service;

import com.flowboard.workspace_service.dto.*;
import com.flowboard.workspace_service.entity.Workspace;
import com.flowboard.workspace_service.entity.WorkspaceMember;
import com.flowboard.workspace_service.enums.MemberRole;
import com.flowboard.workspace_service.enums.Visibility;
import com.flowboard.workspace_service.exception.CustomException;
import com.flowboard.workspace_service.repository.WorkspaceInvitationRepository;
import com.flowboard.workspace_service.repository.WorkspaceMemberRepository;
import com.flowboard.workspace_service.repository.WorkspaceRepository;
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
@DisplayName("WorkspaceServiceImpl Unit Tests")
class WorkspaceServiceImplTest {

    @Mock WorkspaceRepository workspaceRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock WorkspaceInvitationRepository invitationRepository;
    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks WorkspaceServiceImpl workspaceService;

    private Workspace workspace;
    private WorkspaceMember adminMember;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder()
                .id(1L).name("Dev Team").ownerId(1L)
                .visibility(Visibility.PRIVATE)
                .createdAt(LocalDateTime.now()).build();

        adminMember = WorkspaceMember.builder()
                .id(1L).workspace(workspace).userId(1L)
                .role(MemberRole.ADMIN)
                .joinedAt(LocalDateTime.now()).build();
    }

    // ── createWorkspace ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createWorkspace should save workspace and add owner as ADMIN")
    void createWorkspace_success() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("Dev Team");
        req.setVisibility(Visibility.PRIVATE);

        when(workspaceRepository.existsByNameAndOwnerId("Dev Team", 1L))
                .thenReturn(false);
        when(workspaceRepository.save(any())).thenReturn(workspace);
        when(memberRepository.save(any())).thenReturn(adminMember);
        when(memberRepository.findByWorkspaceId(any()))
                .thenReturn(List.of(adminMember));

        WorkspaceResponse response = workspaceService.createWorkspace(req, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Dev Team");
        verify(memberRepository).save(argThat(m ->
                m.getRole() == MemberRole.ADMIN &&
                        m.getUserId().equals(1L)));
    }

    @Test
    @DisplayName("createWorkspace should throw 400 for duplicate name")
    void createWorkspace_duplicate_throws() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("Dev Team");

        when(workspaceRepository.existsByNameAndOwnerId("Dev Team", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> workspaceService.createWorkspace(req, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(workspaceRepository, never()).save(any());
    }

    // ── deleteWorkspace ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteWorkspace should succeed for owner")
    void deleteWorkspace_owner_success() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));

        workspaceService.deleteWorkspace(1L, 1L);

        verify(workspaceRepository).delete(workspace);
    }

    @Test
    @DisplayName("deleteWorkspace should throw 403 for non-owner")
    void deleteWorkspace_nonOwner_throws() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));

        assertThatThrownBy(() -> workspaceService.deleteWorkspace(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(workspaceRepository, never()).delete(any());
    }

    // ── addMember ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addMember should succeed for workspace admin")
    void addMember_success() {
        AddMemberRequest req = new AddMemberRequest();
        req.setUserId(3L);
        req.setRole(MemberRole.MEMBER);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 3L))
                .thenReturn(false);
        when(memberRepository.save(any())).thenReturn(new WorkspaceMember());

        workspaceService.addMember(1L, req, 1L);

        verify(memberRepository).save(any());
    }

    @Test
    @DisplayName("addMember should throw 400 if user already member")
    void addMember_alreadyMember_throws() {
        AddMemberRequest req = new AddMemberRequest();
        req.setUserId(1L);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> workspaceService.addMember(1L, req, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("already a member");
    }

    // ── removeMember ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeMember should throw 400 if trying to remove owner")
    void removeMember_owner_throws() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(true);

        // workspace.ownerId = 1L, trying to remove userId = 1L
        assertThatThrownBy(() -> workspaceService.removeMember(1L, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("owner");
    }
}