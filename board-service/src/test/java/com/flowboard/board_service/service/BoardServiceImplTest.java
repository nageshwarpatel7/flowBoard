package com.flowboard.board_service.service;

import com.flowboard.board_service.dto.*;
import com.flowboard.board_service.entity.Board;
import com.flowboard.board_service.entity.BoardMember;
import com.flowboard.board_service.enums.BoardMemberRole;
import com.flowboard.board_service.enums.Visibility;
import com.flowboard.board_service.exception.CustomException;
import com.flowboard.board_service.repository.BoardMemberRepository;
import com.flowboard.board_service.repository.BoardRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardServiceImpl Unit Tests")
class BoardServiceImplTest {

    @Mock BoardRepository boardRepository;
    @Mock BoardMemberRepository memberRepository;
    @InjectMocks BoardServiceImpl boardService;

    private Board openBoard;
    private Board closedBoard;
    private BoardMember adminMember;
    private BoardMember regularMember;

    @BeforeEach
    void setUp() {
        openBoard = Board.builder()
                .id(1L).workspaceId(10L).name("Sprint Board")
                .visibility(Visibility.PRIVATE)
                .createdById(1L).isClosed(false)
                .createdAt(LocalDateTime.now()).build();

        closedBoard = Board.builder()
                .id(2L).workspaceId(10L).name("Old Board")
                .visibility(Visibility.PRIVATE)
                .createdById(1L).isClosed(true)
                .createdAt(LocalDateTime.now()).build();

        adminMember = BoardMember.builder()
                .id(1L).board(openBoard).userId(1L)
                .role(BoardMemberRole.ADMIN)
                .addedAt(LocalDateTime.now()).build();

        regularMember = BoardMember.builder()
                .id(2L).board(openBoard).userId(2L)
                .role(BoardMemberRole.MEMBER)
                .addedAt(LocalDateTime.now()).build();
    }

    // ── createBoard ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBoard should save board and auto-add creator as ADMIN")
    void createBoard_success() {
        CreateBoardRequest req = new CreateBoardRequest();
        req.setWorkspaceId(10L);
        req.setName("Sprint Board");
        req.setVisibility(Visibility.PRIVATE);

        when(boardRepository.save(any())).thenReturn(openBoard);
        when(memberRepository.save(any())).thenReturn(adminMember);
        when(memberRepository.findByBoardId(any())).thenReturn(List.of(adminMember));

        BoardResponse response = boardService.createBoard(req, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Sprint Board");
        verify(memberRepository).save(argThat(m ->
                m.getRole() == BoardMemberRole.ADMIN &&
                        m.getUserId().equals(1L)));
    }

    // ── closeBoard ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("closeBoard should set isClosed=true for admin")
    void closeBoard_success() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(adminMember));
        when(boardRepository.save(any())).thenReturn(openBoard);
        when(memberRepository.findByBoardId(1L))
                .thenReturn(List.of(adminMember));

        BoardResponse response = boardService.closeBoard(1L, 1L);

        assertThat(openBoard.isClosed()).isTrue();
        verify(boardRepository).save(openBoard);
    }

    @Test
    @DisplayName("closeBoard should throw 400 when board already closed")
    void closeBoard_alreadyClosed_throws() {
        when(boardRepository.findById(2L)).thenReturn(Optional.of(closedBoard));
        when(memberRepository.findByBoardIdAndUserId(2L, 1L))
                .thenReturn(Optional.of(adminMember));

        assertThatThrownBy(() -> boardService.closeBoard(2L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── deleteBoard ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteBoard should succeed for board creator")
    void deleteBoard_creatorCanDelete() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));

        boardService.deleteBoard(1L, 1L);

        verify(boardRepository).delete(openBoard);
    }

    @Test
    @DisplayName("deleteBoard should throw 403 for non-creator")
    void deleteBoard_nonCreator_throws() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));

        assertThatThrownBy(() -> boardService.deleteBoard(1L, 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(boardRepository, never()).delete(any());
    }

    // ── addMember ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addMember should add user with default MEMBER role")
    void addMember_success() {
        AddBoardMemberRequest req = new AddBoardMemberRequest();
        req.setUserId(3L);
        req.setRole(null); // should default to MEMBER

        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByBoardIdAndUserId(1L, 3L))
                .thenReturn(false);
        when(memberRepository.save(any())).thenReturn(regularMember);

        BoardMember result = boardService.addMember(1L, req, 1L);

        verify(memberRepository).save(argThat(m ->
                m.getRole() == BoardMemberRole.MEMBER));
    }

    @Test
    @DisplayName("addMember to closed board should throw 400")
    void addMember_closedBoard_throws() {
        AddBoardMemberRequest req = new AddBoardMemberRequest();
        req.setUserId(3L);

        when(boardRepository.findById(2L)).thenReturn(Optional.of(closedBoard));
        when(memberRepository.findByBoardIdAndUserId(2L, 1L))
                .thenReturn(Optional.of(adminMember));

        assertThatThrownBy(() -> boardService.addMember(2L, req, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("closed");
    }

    // ── getBoardById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBoardById should throw 404 when board not found")
    void getBoardById_notFound() {
        when(boardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getBoardById(999L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("getBoardById private board should throw 403 for non-member")
    void getBoardById_private_nonMember_throws() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));
        when(memberRepository.existsByBoardIdAndUserId(1L, 5L))
                .thenReturn(false);

        assertThatThrownBy(() -> boardService.getBoardById(1L, 5L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── getBoardAnalytics ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getBoardAnalytics should return correct member counts")
    void getBoardAnalytics_success() {
        BoardMember observer = BoardMember.builder()
                .userId(3L).role(BoardMemberRole.OBSERVER)
                .board(openBoard).addedAt(LocalDateTime.now()).build();

        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));
        when(memberRepository.existsByBoardIdAndUserId(1L, 1L)).thenReturn(true);
        when(memberRepository.findByBoardId(1L))
                .thenReturn(List.of(adminMember, regularMember, observer));

        BoardResponse.BoardAnalytics analytics =
                boardService.getBoardAnalytics(1L, 1L);

        assertThat(analytics.getTotalMembers()).isEqualTo(3);
        assertThat(analytics.getAdminCount()).isEqualTo(1);
        assertThat(analytics.getMemberCount()).isEqualTo(1);
        assertThat(analytics.getObserverCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getBoardsByWorkspace should return boards user can see")
    void getBoardsByWorkspace_success() {
        Board publicBoard = Board.builder().id(3L).workspaceId(10L).visibility(Visibility.PUBLIC).build();
        when(boardRepository.findByWorkspaceId(10L)).thenReturn(List.of(openBoard, publicBoard));
        when(memberRepository.existsByBoardIdAndUserId(1L, 1L)).thenReturn(true);
        when(memberRepository.findByBoardId(any())).thenReturn(List.of());

        List<BoardResponse> boards = boardService.getBoardsByWorkspace(10L, 1L);

        assertThat(boards).hasSize(2);
    }

    @Test
    @DisplayName("getBoardsByMember should return boards")
    void getBoardsByMember_success() {
        when(boardRepository.findByMemberUserId(1L)).thenReturn(List.of(openBoard));
        when(memberRepository.findByBoardId(any())).thenReturn(List.of());

        List<BoardResponse> boards = boardService.getBoardsByMember(1L);

        assertThat(boards).hasSize(1);
    }

    @Test
    @DisplayName("getBoardsByCreator should return boards")
    void getBoardsByCreator_success() {
        when(boardRepository.findByCreatedById(1L)).thenReturn(List.of(openBoard));
        when(memberRepository.findByBoardId(any())).thenReturn(List.of());

        List<BoardResponse> boards = boardService.getBoardsByCreator(1L);

        assertThat(boards).hasSize(1);
    }

    @Test
    @DisplayName("getPublicBoards should return public boards")
    void getPublicBoards_success() {
        when(boardRepository.findByVisibility(Visibility.PUBLIC)).thenReturn(List.of(openBoard));
        when(memberRepository.findByBoardId(any())).thenReturn(List.of());

        List<BoardResponse> boards = boardService.getPublicBoards();

        assertThat(boards).hasSize(1);
    }

    @Test
    @DisplayName("getClosedBoards should return closed boards")
    void getClosedBoards_success() {
        when(boardRepository.findByWorkspaceIdAndIsClosed(10L, true)).thenReturn(List.of(closedBoard));
        when(memberRepository.existsByBoardIdAndUserId(2L, 1L)).thenReturn(true);
        when(memberRepository.findByBoardId(any())).thenReturn(List.of());

        List<BoardResponse> boards = boardService.getClosedBoards(10L, 1L);

        assertThat(boards).hasSize(1);
    }

    @Test
    @DisplayName("updateBoard should update details")
    void updateBoard_success() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(boardRepository.save(any())).thenReturn(openBoard);
        when(memberRepository.findByBoardId(any())).thenReturn(List.of());

        UpdateBoardRequest req = new UpdateBoardRequest();
        req.setName("New Name");

        BoardResponse res = boardService.updateBoard(1L, req, 1L);

        assertThat(res.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("updateBoard should throw 400 if closed")
    void updateBoard_closed_throws() {
        when(boardRepository.findById(2L)).thenReturn(Optional.of(closedBoard));
        when(memberRepository.findByBoardIdAndUserId(2L, 1L)).thenReturn(Optional.of(adminMember));

        UpdateBoardRequest req = new UpdateBoardRequest();
        assertThatThrownBy(() -> boardService.updateBoard(2L, req, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("reopenBoard should reopen")
    void reopenBoard_success() {
        when(boardRepository.findById(2L)).thenReturn(Optional.of(closedBoard));
        when(memberRepository.findByBoardIdAndUserId(2L, 1L)).thenReturn(Optional.of(adminMember));
        when(boardRepository.save(any())).thenReturn(closedBoard);
        when(memberRepository.findByBoardId(any())).thenReturn(List.of());

        BoardResponse res = boardService.reopenBoard(2L, 1L);

        assertThat(res.isClosed()).isFalse();
    }

    @Test
    @DisplayName("reopenBoard should throw 400 if already open")
    void reopenBoard_alreadyOpen_throws() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));

        assertThatThrownBy(() -> boardService.reopenBoard(1L, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("removeMember should delete member")
    void removeMember_success() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByBoardIdAndUserId(1L, 2L)).thenReturn(true);

        boardService.removeMember(1L, 2L, 1L);

        verify(memberRepository).deleteByBoardIdAndUserId(1L, 2L);
    }

    @Test
    @DisplayName("removeMember should throw 400 if removing creator")
    void removeMember_creator_throws() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));

        assertThatThrownBy(() -> boardService.removeMember(1L, 1L, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateMemberRole should update role")
    void updateMemberRole_success() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByBoardIdAndUserId(1L, 2L)).thenReturn(Optional.of(regularMember));

        UpdateBoardMemberRoleRequest req = new UpdateBoardMemberRoleRequest();
        req.setRole(BoardMemberRole.ADMIN);

        boardService.updateMemberRole(1L, 2L, req, 1L);

        verify(memberRepository).save(argThat(m -> m.getRole() == BoardMemberRole.ADMIN));
    }

    @Test
    @DisplayName("getMembers should return members")
    void getMembers_success() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(openBoard));
        when(memberRepository.findByBoardId(1L)).thenReturn(List.of(adminMember, regularMember));

        List<BoardMember> members = boardService.getMembers(1L);

        assertThat(members).hasSize(2);
    }
}