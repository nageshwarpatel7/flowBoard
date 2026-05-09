package com.flowboard.board_service.repository;

import com.flowboard.board_service.entity.Board;
import com.flowboard.board_service.entity.BoardMember;
import com.flowboard.board_service.enums.BoardMemberRole;
import com.flowboard.board_service.enums.Visibility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BoardRepository – @DataJpaTest")
class BoardRepositoryTest {

    @Autowired BoardRepository boardRepo;
    @Autowired BoardMemberRepository memberRepo;

    private Board b1Public, b2Private, b3Closed;

    @BeforeEach
    void setUp() {
        b1Public = boardRepo.save(Board.builder()
                .workspaceId(10L).name("Sprint Board").visibility(Visibility.PUBLIC)
                .createdById(1L).isClosed(false).createdAt(LocalDateTime.now()).build());

        b2Private = boardRepo.save(Board.builder()
                .workspaceId(10L).name("Private Board").visibility(Visibility.PRIVATE)
                .createdById(2L).isClosed(false).createdAt(LocalDateTime.now()).build());

        b3Closed = boardRepo.save(Board.builder()
                .workspaceId(10L).name("Closed Board").visibility(Visibility.PUBLIC)
                .createdById(1L).isClosed(true).createdAt(LocalDateTime.now()).build());
    }

    // ── findByWorkspaceId ───────────────────────────────────────────────────
    @Test @DisplayName("findByWorkspaceId – returns all boards in workspace")
    void findByWorkspace_all() {
        assertThat(boardRepo.findByWorkspaceId(10L)).hasSize(3);
    }

    @Test @DisplayName("findByWorkspaceId – empty for unknown workspace")
    void findByWorkspace_empty() {
        assertThat(boardRepo.findByWorkspaceId(999L)).isEmpty();
    }

    // ── findByWorkspaceIdAndIsClosed ─────────────────────────────────────
    @Test @DisplayName("findByWorkspace active – excludes closed boards")
    void findByWorkspace_active() {
        List<Board> active = boardRepo.findByWorkspaceIdAndIsClosed(10L, false);
        assertThat(active).hasSize(2).noneMatch(Board::isClosed);
    }

    // ── findByCreatedById ───────────────────────────────────────────────────
    @Test @DisplayName("findByCreatedById – boards created by user1")
    void findByCreator_user1() {
        List<Board> boards = boardRepo.findByCreatedById(1L);
        assertThat(boards).hasSize(2).allMatch(b -> b.getCreatedById().equals(1L));
    }

    @Test @DisplayName("findByCreatedById – empty for user with no boards")
    void findByCreator_empty() {
        assertThat(boardRepo.findByCreatedById(99L)).isEmpty();
    }

    // ── findByVisibility ────────────────────────────────────────────────────
    @Test @DisplayName("findByVisibility PUBLIC – returns only public boards")
    void findPublic() {
        List<Board> pub = boardRepo.findByVisibility(Visibility.PUBLIC);
        assertThat(pub).hasSize(2).allMatch(b -> b.getVisibility() == Visibility.PUBLIC);
    }

    @Test @DisplayName("findByVisibility PRIVATE – returns only private boards")
    void findPrivate() {
        List<Board> priv = boardRepo.findByVisibility(Visibility.PRIVATE);
        assertThat(priv).hasSize(1)
                .first().extracting(Board::getName).isEqualTo("Private Board");
    }

    // ── findByMemberUserId ──────────────────────────────────────────────────
    @Test @DisplayName("findByMemberUserId – boards user is member of")
    void findByMember() {
        memberRepo.save(BoardMember.builder()
                .board(b1Public).userId(5L)
                .role(BoardMemberRole.MEMBER).addedAt(LocalDateTime.now()).build());

        List<Board> boards = boardRepo.findByMemberUserId(5L);
        assertThat(boards).hasSize(1)
                .first().extracting(Board::getName).isEqualTo("Sprint Board");
    }

    @Test @DisplayName("findByMemberUserId – empty for non-member")
    void findByMember_empty() {
        assertThat(boardRepo.findByMemberUserId(99L)).isEmpty();
    }

    // ── existsByNameAndWorkspaceId ──────────────────────────────────────────
    @Test @DisplayName("existsByNameAndWorkspaceId – true for duplicate")
    void existsByNameAndWorkspace_true() {
        assertThat(boardRepo.existsByNameAndWorkspaceId("Sprint Board", 10L)).isTrue();
    }

    @Test @DisplayName("existsByNameAndWorkspaceId – false for different workspace")
    void existsByNameAndWorkspace_diffWorkspace() {
        assertThat(boardRepo.existsByNameAndWorkspaceId("Sprint Board", 99L)).isFalse();
    }

    // ── BoardMemberRepository ───────────────────────────────────────────────
    @Test @DisplayName("memberRepo – find by board and user")
    void findByBoardAndUser() {
        memberRepo.save(BoardMember.builder()
                .board(b1Public).userId(3L)
                .role(BoardMemberRole.ADMIN).addedAt(LocalDateTime.now()).build());

        var found = memberRepo.findByBoardIdAndUserId(b1Public.getId(), 3L);
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(BoardMemberRole.ADMIN);
    }

    @Test @DisplayName("memberRepo – findByBoardId returns all members")
    void findMembersByBoard() {
        memberRepo.save(BoardMember.builder().board(b1Public).userId(1L)
                .role(BoardMemberRole.ADMIN).addedAt(LocalDateTime.now()).build());
        memberRepo.save(BoardMember.builder().board(b1Public).userId(2L)
                .role(BoardMemberRole.MEMBER).addedAt(LocalDateTime.now()).build());

        assertThat(memberRepo.findByBoardId(b1Public.getId())).hasSize(2);
    }

    @Test @DisplayName("memberRepo – existsByBoardIdAndUserId true")
    void memberExists_true() {
        memberRepo.save(BoardMember.builder().board(b1Public).userId(7L)
                .role(BoardMemberRole.MEMBER).addedAt(LocalDateTime.now()).build());

        assertThat(memberRepo.existsByBoardIdAndUserId(b1Public.getId(), 7L)).isTrue();
    }

    @Test @DisplayName("memberRepo – existsByBoardIdAndUserId false")
    void memberExists_false() {
        assertThat(memberRepo.existsByBoardIdAndUserId(b1Public.getId(), 99L)).isFalse();
    }

    @Test @DisplayName("memberRepo – deleteByBoardIdAndUserId removes member")
    void deleteMember() {
        memberRepo.save(BoardMember.builder().board(b1Public).userId(8L)
                .role(BoardMemberRole.MEMBER).addedAt(LocalDateTime.now()).build());
        memberRepo.deleteByBoardIdAndUserId(b1Public.getId(), 8L);
        assertThat(memberRepo.existsByBoardIdAndUserId(b1Public.getId(), 8L)).isFalse();
    }

    // ── CRUD ────────────────────────────────────────────────────────────────
    @Test @DisplayName("save – assigns ID on persist")
    void save_assignsId() {
        Board b = boardRepo.save(Board.builder()
                .workspaceId(20L).name("New Board").visibility(Visibility.PUBLIC)
                .createdById(3L).isClosed(false).createdAt(LocalDateTime.now()).build());
        assertThat(b.getId()).isNotNull();
    }

    @Test @DisplayName("save – updates name")
    void save_updatesName() {
        b1Public.setName("Renamed Sprint");
        boardRepo.save(b1Public);
        assertThat(boardRepo.findById(b1Public.getId()).orElseThrow().getName())
                .isEqualTo("Renamed Sprint");
    }

    @Test @DisplayName("delete – removes board")
    void delete_removes() {
        boardRepo.delete(b2Private);
        assertThat(boardRepo.findById(b2Private.getId())).isEmpty();
    }

    @Test @DisplayName("count – returns all boards")
    void count_all() {
        assertThat(boardRepo.count()).isGreaterThanOrEqualTo(3);
    }
}