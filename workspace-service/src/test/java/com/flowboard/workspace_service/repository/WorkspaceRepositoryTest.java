package com.flowboard.workspace_service.repository;

import com.flowboard.workspace_service.entity.*;
import com.flowboard.workspace_service.enums.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Workspace Repositories – @DataJpaTest")
class WorkspaceRepositoryTest {

    @Autowired WorkspaceRepository            wsRepo;
    @Autowired WorkspaceMemberRepository      memberRepo;
    @Autowired WorkspaceInvitationRepository  inviteRepo;

    private Workspace ws1Public, ws2Private;
    private WorkspaceMember adminMember, regularMember;

    @BeforeEach
    void setUp() {
        ws1Public = wsRepo.save(Workspace.builder()
                .name("Dev Team").ownerId(1L).visibility(Visibility.PUBLIC)
                .createdAt(LocalDateTime.now()).build());

        ws2Private = wsRepo.save(Workspace.builder()
                .name("Secret Project").ownerId(2L).visibility(Visibility.PRIVATE)
                .createdAt(LocalDateTime.now()).build());

        adminMember = memberRepo.save(WorkspaceMember.builder()
                .workspace(ws1Public).userId(1L).role(MemberRole.ADMIN)
                .joinedAt(LocalDateTime.now()).build());

        regularMember = memberRepo.save(WorkspaceMember.builder()
                .workspace(ws1Public).userId(3L).role(MemberRole.MEMBER)
                .joinedAt(LocalDateTime.now()).build());
    }

    // ── WorkspaceRepository ─────────────────────────────────────────────────
    @Test @DisplayName("findByOwnerId – returns owned workspaces")
    void findByOwner() {
        List<Workspace> result = wsRepo.findByOwnerId(1L);
        assertThat(result).hasSize(1)
                .first().extracting(Workspace::getName).isEqualTo("Dev Team");
    }

    @Test @DisplayName("findByOwnerId – empty for user with no workspaces")
    void findByOwner_empty() {
        assertThat(wsRepo.findByOwnerId(99L)).isEmpty();
    }

    @Test @DisplayName("findByVisibility PUBLIC – returns public only")
    void findByVisibility_public() {
        List<Workspace> pub = wsRepo.findByVisibility(Visibility.PUBLIC);
        assertThat(pub).hasSize(1).allMatch(w -> w.getVisibility() == Visibility.PUBLIC);
    }

    @Test @DisplayName("findByVisibility PRIVATE – returns private only")
    void findByVisibility_private() {
        List<Workspace> priv = wsRepo.findByVisibility(Visibility.PRIVATE);
        assertThat(priv).hasSize(1)
                .first().extracting(Workspace::getName).isEqualTo("Secret Project");
    }

    @Test @DisplayName("findByMemberUserId – finds workspaces user belongs to")
    void findByMember() {
        List<Workspace> result = wsRepo.findByMemberUserId(3L);
        assertThat(result).hasSize(1)
                .first().extracting(Workspace::getName).isEqualTo("Dev Team");
    }

    @Test @DisplayName("findByMemberUserId – empty for non-member")
    void findByMember_empty() {
        assertThat(wsRepo.findByMemberUserId(99L)).isEmpty();
    }

    @Test @DisplayName("existsByNameAndOwnerId – true for duplicate name+owner")
    void existsByNameAndOwner_true() {
        assertThat(wsRepo.existsByNameAndOwnerId("Dev Team", 1L)).isTrue();
    }

    @Test @DisplayName("existsByNameAndOwnerId – false for different owner")
    void existsByNameAndOwner_diffOwner() {
        assertThat(wsRepo.existsByNameAndOwnerId("Dev Team", 99L)).isFalse();
    }

    @Test @DisplayName("existsByNameAndOwnerId – false for different name")
    void existsByNameAndOwner_diffName() {
        assertThat(wsRepo.existsByNameAndOwnerId("Other Name", 1L)).isFalse();
    }

    // ── WorkspaceMemberRepository ───────────────────────────────────────────
    @Test @DisplayName("findByWorkspaceId – returns all members")
    void findMembersByWorkspace() {
        List<WorkspaceMember> members = memberRepo.findByWorkspaceId(ws1Public.getId());
        assertThat(members).hasSize(2);
    }

    @Test @DisplayName("findByWorkspaceIdAndUserId – finds specific member")
    void findMemberByWorkspaceAndUser() {
        Optional<WorkspaceMember> m = memberRepo.findByWorkspaceIdAndUserId(ws1Public.getId(), 1L);
        assertThat(m).isPresent().get().extracting(WorkspaceMember::getRole).isEqualTo(MemberRole.ADMIN);
    }

    @Test @DisplayName("findByWorkspaceIdAndUserId – empty for non-member")
    void findMember_notFound() {
        assertThat(memberRepo.findByWorkspaceIdAndUserId(ws1Public.getId(), 99L)).isEmpty();
    }

    @Test @DisplayName("existsByWorkspaceIdAndUserId – true for existing member")
    void memberExists_true() {
        assertThat(memberRepo.existsByWorkspaceIdAndUserId(ws1Public.getId(), 1L)).isTrue();
    }

    @Test @DisplayName("existsByWorkspaceIdAndUserId – false for non-member")
    void memberExists_false() {
        assertThat(memberRepo.existsByWorkspaceIdAndUserId(ws1Public.getId(), 99L)).isFalse();
    }

    @Test @DisplayName("deleteByWorkspaceIdAndUserId – removes member")
    void deleteMember() {
        memberRepo.deleteByWorkspaceIdAndUserId(ws1Public.getId(), 3L);
        assertThat(memberRepo.existsByWorkspaceIdAndUserId(ws1Public.getId(), 3L)).isFalse();
    }

    @Test @DisplayName("member save – updates role")
    void memberSave_updatesRole() {
        regularMember.setRole(MemberRole.ADMIN);
        memberRepo.save(regularMember);
        WorkspaceMember reloaded = memberRepo.findById(regularMember.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(MemberRole.ADMIN);
    }

    // ── WorkspaceInvitationRepository ───────────────────────────────────────
    @Test @DisplayName("findByToken – finds invitation by token")
    void findByToken_found() {
        inviteRepo.save(WorkspaceInvitation.builder()
                .workspaceId(ws1Public.getId()).inviteeEmail("test@invite.com")
                .invitedBy(1L)
                .role(MemberRole.MEMBER).token("unique-token-abc")
                .status("PENDING").createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7)).build());

        Optional<WorkspaceInvitation> found = inviteRepo.findByToken("unique-token-abc");
        assertThat(found).isPresent();
        assertThat(found.get().getInviteeEmail()).isEqualTo("test@invite.com");
    }

    @Test @DisplayName("findByToken – empty for unknown token")
    void findByToken_notFound() {
        assertThat(inviteRepo.findByToken("bad-token")).isEmpty();
    }

    @Test @DisplayName("existsByWorkspaceIdAndInviteeEmailAndStatus – true for PENDING")
    void existsPendingInvite_true() {
        inviteRepo.save(WorkspaceInvitation.builder()
                .workspaceId(ws1Public.getId()).inviteeEmail("dup@ex.com")
                .invitedBy(1L)
                .role(MemberRole.MEMBER).token("tok-dup").status("PENDING")
                .createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(7)).build());

        assertThat(inviteRepo.existsByWorkspaceIdAndInviteeEmailAndStatus(
                ws1Public.getId(), "dup@ex.com", "PENDING")).isTrue();
    }

    @Test @DisplayName("existsByWorkspaceIdAndInviteeEmailAndStatus – false for ACCEPTED")
    void existsPendingInvite_false() {
        assertThat(inviteRepo.existsByWorkspaceIdAndInviteeEmailAndStatus(
                ws1Public.getId(), "nobody@ex.com", "PENDING")).isFalse();
    }

    @Test @DisplayName("findByWorkspaceIdAndStatus – returns all PENDING invitations")
    void findByWorkspaceAndStatus() {
        inviteRepo.save(WorkspaceInvitation.builder()
                .workspaceId(ws1Public.getId()).inviteeEmail("a@b.com")
                .invitedBy(1L)
                .role(MemberRole.MEMBER).token("tok-pend").status("PENDING")
                .createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(7)).build());

        List<WorkspaceInvitation> pending = inviteRepo
                .findByWorkspaceIdAndStatus(ws1Public.getId(), "PENDING");
        assertThat(pending).isNotEmpty()
                .allMatch(i -> "PENDING".equals(i.getStatus()));
    }

    @Test @DisplayName("invitation save – updates status to ACCEPTED")
    void inviteSave_updatesStatus() {
        WorkspaceInvitation inv = inviteRepo.save(WorkspaceInvitation.builder()
                .workspaceId(ws1Public.getId()).inviteeEmail("z@z.com")
                .invitedBy(1L)
                .role(MemberRole.MEMBER).token("tok-z").status("PENDING")
                .createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(7)).build());

        inv.setStatus("ACCEPTED");
        inviteRepo.save(inv);
        assertThat(inviteRepo.findById(inv.getId()).orElseThrow().getStatus()).isEqualTo("ACCEPTED");
    }
}