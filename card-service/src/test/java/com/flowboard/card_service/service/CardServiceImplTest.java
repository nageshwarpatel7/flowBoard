package com.flowboard.card_service.service;

import com.flowboard.card_service.dto.*;
import com.flowboard.card_service.entity.Card;
import com.flowboard.card_service.entity.CardActivity;
import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import com.flowboard.card_service.exception.CustomException;
import com.flowboard.card_service.repository.CardActivityRepository;
import com.flowboard.card_service.repository.CardRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardServiceImpl – full coverage suite")
class CardServiceImplTest {

    @Mock CardRepository         cardRepository;
    @Mock CardActivityRepository activityRepository;
    @Mock org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @InjectMocks CardServiceImpl cardService;

    private Card card;

    @BeforeEach
    void setUp() {
        card = Card.builder()
                .id(1L).listId(10L).boardId(100L)
                .title("Design login page")
                .position(4).priority(Priority.MEDIUM)
                .status(CardStatus.TO_DO).isArchived(false)
                .createdById(1L).createdAt(LocalDateTime.now()).build();
    }

    // ── createCard ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("createCard()")
    class CreateCardTests {

        @Test @DisplayName("appends at end when position null")
        void createCard_appendsAtEnd() {
            CreateCardRequest req = new CreateCardRequest();
            req.setListId(10L); req.setBoardId(100L); req.setTitle("New"); req.setPosition(null);

            when(cardRepository.findMaxPositionByListId(anyLong())).thenReturn(Optional.of(2));
            when(cardRepository.save(any())).thenReturn(card);
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            CardResponse r = cardService.createCard(req, 1L);
            assertThat(r).isNotNull();
            verify(cardRepository, never()).shiftPositionsRight(anyLong(), anyInt());
        }

        @Test @DisplayName("inserts at position and shifts siblings right")
        void createCard_insertsAtPosition() {
            CreateCardRequest req = new CreateCardRequest();
            req.setListId(10L); req.setBoardId(100L); req.setTitle("Insert"); req.setPosition(1);

            when(cardRepository.save(any())).thenReturn(card);
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            cardService.createCard(req, 1L);
            verify(cardRepository).shiftPositionsRight(10L, 1);
        }

        @Test @DisplayName("position 0 when list is empty")
        void createCard_emptyList_position0() {
            CreateCardRequest req = new CreateCardRequest();
            req.setListId(10L); req.setBoardId(100L); req.setTitle("First");

            when(cardRepository.findMaxPositionByListId(anyLong())).thenReturn(Optional.empty());
            when(cardRepository.save(any())).thenAnswer(inv -> {
                Card c = inv.getArgument(0);
                assertThat(c.getPosition()).isEqualTo(0);
                return c;
            });
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            cardService.createCard(req, 1L);
        }

        @Test @DisplayName("default priority is MEDIUM when not set")
        void createCard_defaultPriority() {
            CreateCardRequest req = new CreateCardRequest();
            req.setListId(10L); req.setBoardId(100L); req.setTitle("T"); req.setPriority(null);

            when(cardRepository.findMaxPositionByListId(anyLong())).thenReturn(Optional.empty());
            when(cardRepository.save(any())).thenReturn(card);
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            cardService.createCard(req, 1L);
            verify(cardRepository).save(argThat(c -> c.getPriority() == Priority.MEDIUM));
        }
    }

    // ── getCardById ────────────────────────────────────────────────────────────

    @Test @DisplayName("getCardById – throws 404 when not found")
    void getCardById_notFound() {
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cardService.getCardById(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException)e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @DisplayName("getCardById – returns card when found")
    void getCardById_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        CardResponse r = cardService.getCardById(1L);
        assertThat(r.getId()).isEqualTo(1L);
    }

    // ── getCardByList / getCardByBoard / getCardByAssignee ────────────────────

    @Test @DisplayName("getCardByList – returns active cards sorted by position")
    void getCardByList_success() {
        when(cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(10L))
                .thenReturn(List.of(card));
        assertThat(cardService.getCardByList(10L)).hasSize(1);
    }

    @Test @DisplayName("getCardByBoard – returns non-archived board cards")
    void getCardByBoard_success() {
        when(cardRepository.findByBoardIdAndIsArchivedFalse(100L)).thenReturn(List.of(card));
        assertThat(cardService.getCardByBoard(100L)).hasSize(1);
    }

    @Test @DisplayName("getCardByAssignee – returns cards for given user")
    void getCardByAssignee_success() {
        when(cardRepository.findByAssigneeIdAndIsArchivedFalse(5L)).thenReturn(List.of(card));
        assertThat(cardService.getCardByAssignee(5L)).hasSize(1);
    }

    // ── updateCard ────────────────────────────────────────────────────────────

    @Test @DisplayName("updateCard – updates fields and logs activity")
    void updateCard_success() {
        UpdateCardRequest req = new UpdateCardRequest();
        req.setTitle("Updated title"); req.setStatus(CardStatus.IN_PROGRESS);
        req.setPriority(Priority.HIGH);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(card);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        CardResponse r = cardService.updateCard(1L, req, 1L);
        assertThat(r).isNotNull();
        verify(activityRepository, atLeastOnce()).save(any());
    }

    @Test @DisplayName("updateCard – throws 400 on archived card")
    void updateCard_archived_throws() {
        card.setArchived(true);
        UpdateCardRequest req = new UpdateCardRequest(); req.setTitle("X");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.updateCard(1L, req, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException)e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── deleteCard ────────────────────────────────────────────────────────────

    @Test @DisplayName("deleteCard – shifts left and deletes")
    void deleteCard_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        cardService.deleteCard(1L, 1L);
        verify(cardRepository).shiftPositionsLeft(card.getListId(), card.getPosition());
        verify(cardRepository).delete(card);
    }

    // ── moveCard ──────────────────────────────────────────────────────────────

    @Test @DisplayName("moveCard – closes gap in source and opens in target")
    void moveCard_success() {
        MoveCardRequest req = new MoveCardRequest();
        req.setTargetListId(20L); req.setTargetBoardId(100L); req.setTargetPosition(null);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.findMaxPositionByListId(20L)).thenReturn(Optional.of(3));
        when(cardRepository.save(any())).thenReturn(card);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.moveCard(1L, req, 1L);

        verify(cardRepository).shiftPositionsLeft(10L, 4);
        assertThat(card.getListId()).isEqualTo(20L);
        assertThat(card.getPosition()).isEqualTo(4);
    }

    @Test @DisplayName("moveCard – with targetPosition inserts at specific slot")
    void moveCard_withPosition() {
        MoveCardRequest req = new MoveCardRequest();
        req.setTargetListId(20L); req.setTargetBoardId(100L); req.setTargetPosition(2);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(card);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.moveCard(1L, req, 1L);
        verify(cardRepository).shiftPositionsRight(20L, 2);
        assertThat(card.getPosition()).isEqualTo(2);
    }

    // ── archiveCard / unarchiveCard ───────────────────────────────────────────

    @Nested @DisplayName("archiveCard()")
    class ArchiveTests {

        @Test @DisplayName("archives card and shifts siblings left")
        void archiveCard_success() {
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
            when(cardRepository.save(any())).thenReturn(card);
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            CardResponse r = cardService.archiveCard(1L, 1L);
            assertThat(r).isNotNull();
            verify(cardRepository).shiftPositionsLeft(card.getListId(), card.getPosition());
            assertThat(card.isArchived()).isTrue();
        }

        @Test @DisplayName("throws 400 when already archived")
        void archiveCard_alreadyArchived() {
            card.setArchived(true);
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
            assertThatThrownBy(() -> cardService.archiveCard(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException)e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test @DisplayName("unarchiveCard – restores and appends at end")
    void unarchiveCard_success() {
        card.setArchived(true);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.findMaxPositionByListId(anyLong())).thenReturn(Optional.of(5));
        when(cardRepository.save(any())).thenReturn(card);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.unarchiveCard(1L, 1L);
        assertThat(card.isArchived()).isFalse();
        assertThat(card.getPosition()).isEqualTo(6);
    }

    // ── setPriority / setStatus / setAssignee ─────────────────────────────────

    @Test @DisplayName("setPriority – updates priority and logs PRIORITY_CHANGE")
    void setPriority_success() {
        SetPriorityRequest req = new SetPriorityRequest(); req.setPriority(Priority.HIGH);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(card);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.setPriority(1L, req, 1L);
        assertThat(card.getPriority()).isEqualTo(Priority.HIGH);
        verify(activityRepository).save(argThat(a -> a.getActionType().equals("PRIORITY_CHANGE")));
    }

    @Test @DisplayName("setStatus – updates status and logs STATUS_CHANGE")
    void setStatus_success() {
        SetStatusRequest req = new SetStatusRequest(); req.setStatus(CardStatus.IN_PROGRESS);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(card);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.setStatus(1L, req, 1L);
        assertThat(card.getStatus()).isEqualTo(CardStatus.IN_PROGRESS);
        verify(activityRepository).save(argThat(a ->
                a.getActionType().equals("STATUS_CHANGE") &&
                        a.getOldValue().equals("TO_DO") &&
                        a.getNewValue().equals("IN_PROGRESS")));
    }

    @Test @DisplayName("setAssignee – assigns user and publishes RabbitMQ event")
    void setAssignee_publishesEvent() {
        AssignCardRequest req = new AssignCardRequest(); req.setAssigneeId(5L);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(card);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.setAssignee(1L, req, 1L);
        assertThat(card.getAssigneeId()).isEqualTo(5L);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test @DisplayName("setAssignee – removes assignee when null, no rabbit event")
    void setAssignee_unassign() {
        AssignCardRequest req = new AssignCardRequest(); req.setAssigneeId(null);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(card);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.setAssignee(1L, req, 1L);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // ── overdue / search / stats ──────────────────────────────────────────────

    @Test @DisplayName("getOverdueCardsByBoard – returns overdue cards")
    void getOverdueByBoard_success() {
        when(cardRepository.findOverdueByBoardId(100L, LocalDate.now())).thenReturn(List.of(card));
        assertThat(cardService.getOverdueCardsByBoard(100L)).hasSize(1);
    }

    @Test @DisplayName("getAllOverdueCards – platform-wide overdue")
    void getAllOverdue_success() {
        when(cardRepository.findAllOverdue(any())).thenReturn(List.of(card));
        assertThat(cardService.getAllOverdueCards()).hasSize(1);
    }

    @Test @DisplayName("searchCards – filters by board and keyword")
    void searchCards_success() {
        when(cardRepository.searchByTitle(100L, "login")).thenReturn(List.of(card));
        assertThat(cardService.searchCards(100L, "login")).hasSize(1);
    }

    @Test @DisplayName("getBoardStats – returns correct counts and rates")
    void getBoardStats_success() {
        Card doneCard = Card.builder().id(2L).boardId(100L).status(CardStatus.DONE)
                .priority(Priority.HIGH).isArchived(false).createdAt(LocalDateTime.now()).build();

        when(cardRepository.findByBoardIdAndIsArchivedFalse(100L)).thenReturn(List.of(card, doneCard));
        when(cardRepository.findByBoardIdAndIsArchivedTrue(100L)).thenReturn(List.of());
        when(cardRepository.findOverdueByBoardId(eq(100L), any())).thenReturn(List.of());

        BoardStatsResponse stats = cardService.getBoardStats(100L);
        assertThat(stats.getTotalCards()).isEqualTo(2);
        assertThat(stats.getCompletedCards()).isEqualTo(1);
        assertThat(stats.getCompletionRate()).isEqualTo(50.0);
    }

    // ── copyCard ──────────────────────────────────────────────────────────────

    @Test @DisplayName("copyCard – creates new card in same list with 'Copy of' prefix")
    void copyCard_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.findMaxPositionByListId(anyLong())).thenReturn(Optional.of(3));
        when(cardRepository.save(any())).thenReturn(card);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        CardResponse r = cardService.copyCard(1L, 10L, 1L);
        verify(cardRepository).save(argThat(c -> c.getTitle().startsWith("Copy of")));
    }

    // ── activity paged ────────────────────────────────────────────────────────

    @Test @DisplayName("getCardActivityPaged – returns page of activity logs")
    void getCardActivityPaged_success() {
        CardActivity act = CardActivity.builder().id(1L).cardId(1L).actorId(1L)
                .actionType("CREATE").createdAt(LocalDateTime.now()).build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(activityRepository.findByCardIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(act)));

        PagedResponse<CardActivityResponse> page = cardService.getCardActivityPaged(1L, 0, 10);
        assertThat(page.getContent()).hasSize(1);
    }

    // ── getArchivedCards ──────────────────────────────────────────────────────

    @Test @DisplayName("getArchivedCardsByBoard – returns archived cards")
    void getArchivedByBoard() {
        card.setArchived(true);
        when(cardRepository.findByBoardIdAndIsArchivedTrue(100L)).thenReturn(List.of(card));
        assertThat(cardService.getArchivedCardsByBoard(100L)).hasSize(1);
    }

    @Test @DisplayName("getArchivedCardsByList – returns archived cards in list")
    void getArchivedByList() {
        card.setArchived(true);
        when(cardRepository.findByListIdAndIsArchivedTrue(10L)).thenReturn(List.of(card));
        assertThat(cardService.getArchivedCardsByList(10L)).hasSize(1);
    }

    // ── getCardsByStatus / getCardsByPriority ─────────────────────────────────

    @Test @DisplayName("getCardsByStatus – filters by status in board")
    void getCardsByStatus() {
        when(cardRepository.findByBoardIdAndStatusAndIsArchivedFalse(100L, CardStatus.TO_DO))
                .thenReturn(List.of(card));
        assertThat(cardService.getCardsByStatus(100L, CardStatus.TO_DO)).hasSize(1);
    }

    @Test @DisplayName("getCardsByPriority – filters by priority in board")
    void getCardsByPriority() {
        when(cardRepository.findByBoardIdAndPriorityAndIsArchivedFalse(100L, Priority.MEDIUM))
                .thenReturn(List.of(card));
        assertThat(cardService.getCardsByPriority(100L, Priority.MEDIUM)).hasSize(1);
    }
}