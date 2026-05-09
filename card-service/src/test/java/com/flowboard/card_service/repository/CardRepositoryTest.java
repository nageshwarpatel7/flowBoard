package com.flowboard.card_service.repository;

import com.flowboard.card_service.entity.Card;
import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CardRepository – @DataJpaTest")
class CardRepositoryTest {

    @Autowired CardRepository repo;
    @Autowired TestEntityManager entityManager;

    private Card c1, c2, c3Archived, c4Overdue, c5DifferentBoard;

    @BeforeEach
    void setUp() {
        c1 = repo.save(Card.builder()
                .listId(10L).boardId(100L).title("Design login page")
                .position(0).priority(Priority.HIGH).status(CardStatus.TO_DO)
                .isArchived(false).createdById(1L).createdAt(LocalDateTime.now()).build());

        c2 = repo.save(Card.builder()
                .listId(10L).boardId(100L).title("Implement JWT auth")
                .position(1).priority(Priority.MEDIUM).status(CardStatus.IN_PROGRESS)
                .isArchived(false).assigneeId(2L).createdById(1L)
                .createdAt(LocalDateTime.now()).build());

        c3Archived = repo.save(Card.builder()
                .listId(10L).boardId(100L).title("Old archived task")
                .position(0).priority(Priority.LOW).status(CardStatus.TO_DO)
                .isArchived(true).createdById(1L).createdAt(LocalDateTime.now()).build());

        c4Overdue = repo.save(Card.builder()
                .listId(20L).boardId(100L).title("Fix critical bug")
                .position(0).priority(Priority.CRITICAL).status(CardStatus.TO_DO)
                .dueDate(LocalDate.now().minusDays(3))
                .isArchived(false).assigneeId(2L).createdById(1L)
                .createdAt(LocalDateTime.now()).build());

        c5DifferentBoard = repo.save(Card.builder()
                .listId(30L).boardId(200L).title("Other board card")
                .position(0).priority(Priority.LOW).status(CardStatus.DONE)
                .isArchived(false).createdById(3L).createdAt(LocalDateTime.now()).build());
    }

    // ── findByListIdAndIsArchivedFalseOrderByPosition ────────────────────────
    @Test @DisplayName("findByList active – returns only non-archived, sorted by position")
    void findByList_activeOnly() {
        List<Card> cards = repo.findByListIdAndIsArchivedFalseOrderByPosition(10L);
        assertThat(cards).hasSize(2);
        assertThat(cards.get(0).getPosition()).isLessThanOrEqualTo(cards.get(1).getPosition());
        assertThat(cards).noneMatch(Card::isArchived);
    }

    @Test @DisplayName("findByList active – empty for non-existent list")
    void findByList_empty() {
        assertThat(repo.findByListIdAndIsArchivedFalseOrderByPosition(999L)).isEmpty();
    }

    // ── findByBoardIdAndIsArchivedFalse ──────────────────────────────────────
    @Test @DisplayName("findByBoard active – all active in board")
    void findByBoard_active() {
        List<Card> cards = repo.findByBoardIdAndIsArchivedFalse(100L);
        assertThat(cards).hasSize(3); // c1, c2, c4Overdue
        assertThat(cards).noneMatch(Card::isArchived);
    }

    @Test @DisplayName("findByBoard active – excludes other boards")
    void findByBoard_excludesOtherBoards() {
        List<Card> cards = repo.findByBoardIdAndIsArchivedFalse(100L);
        assertThat(cards).noneMatch(c -> c.getBoardId().equals(200L));
    }

    // ── findByBoardIdAndIsArchivedTrue ───────────────────────────────────────
    @Test @DisplayName("findByBoard archived – returns only archived")
    void findByBoard_archived() {
        List<Card> cards = repo.findByBoardIdAndIsArchivedTrue(100L);
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getTitle()).isEqualTo("Old archived task");
    }

    // ── findByListIdAndIsArchivedTrue ────────────────────────────────────────
    @Test @DisplayName("findByList archived – returns archived in list")
    void findByList_archived() {
        List<Card> cards = repo.findByListIdAndIsArchivedTrue(10L);
        assertThat(cards).hasSize(1).allMatch(Card::isArchived);
    }

    // ── findByAssigneeIdAndIsArchivedFalse ───────────────────────────────────
    @Test @DisplayName("findByAssignee – returns active assigned cards")
    void findByAssignee() {
        List<Card> cards = repo.findByAssigneeIdAndIsArchivedFalse(2L);
        assertThat(cards).hasSize(2); // c2 + c4Overdue
        assertThat(cards).allMatch(c -> Long.valueOf(2L).equals(c.getAssigneeId()));
    }

    @Test @DisplayName("findByAssignee – empty for user with no cards")
    void findByAssignee_empty() {
        assertThat(repo.findByAssigneeIdAndIsArchivedFalse(99L)).isEmpty();
    }

    // ── findMaxPositionByListId ──────────────────────────────────────────────
    @Test @DisplayName("findMaxPositionByListId – returns highest position")
    void findMaxPosition() {
        Optional<Integer> max = repo.findMaxPositionByListId(10L);
        assertThat(max).isPresent().hasValue(1);
    }

    @Test @DisplayName("findMaxPositionByListId – empty for empty list")
    void findMaxPosition_emptyList() {
        assertThat(repo.findMaxPositionByListId(999L)).isEmpty();
    }

    // ── findOverdueByBoardId ─────────────────────────────────────────────────
    @Test @DisplayName("findOverdueByBoardId – returns past-due active cards")
    void findOverdue_byBoard() {
        List<Card> overdue = repo.findOverdueByBoardId(100L, LocalDate.now());
        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getTitle()).isEqualTo("Fix critical bug");
    }

    @Test @DisplayName("findOverdueByBoardId – excludes archived overdue")
    void findOverdue_excludesArchived() {
        c4Overdue.setArchived(true);
        repo.save(c4Overdue);
        List<Card> overdue = repo.findOverdueByBoardId(100L, LocalDate.now());
        assertThat(overdue).isEmpty();
    }

    @Test @DisplayName("findOverdueByBoardId – excludes DONE even if past due")
    void findOverdue_excludesDone() {
        c4Overdue.setStatus(CardStatus.DONE);
        repo.save(c4Overdue);
        List<Card> overdue = repo.findOverdueByBoardId(100L, LocalDate.now());
        assertThat(overdue).isEmpty();
    }

    // ── findAllOverdue ───────────────────────────────────────────────────────
    @Test @DisplayName("findAllOverdue – finds across all boards")
    void findAllOverdue() {
        List<Card> all = repo.findAllOverdue(LocalDate.now());
        assertThat(all).hasSize(1);
    }

    // ── findByBoardIdAndStatusAndIsArchivedFalse ─────────────────────────────
    @Test @DisplayName("findByStatus TO_DO – returns correct count")
    void findByStatus_toDo() {
        List<Card> todos = repo.findByBoardIdAndStatusAndIsArchivedFalse(100L, CardStatus.TO_DO);
        assertThat(todos).hasSize(2); // c1 + c4Overdue
    }

    @Test @DisplayName("findByStatus IN_PROGRESS – single match")
    void findByStatus_inProgress() {
        List<Card> inProgress = repo.findByBoardIdAndStatusAndIsArchivedFalse(100L, CardStatus.IN_PROGRESS);
        assertThat(inProgress).hasSize(1)
                .first().extracting(Card::getTitle).isEqualTo("Implement JWT auth");
    }

    @Test @DisplayName("findByStatus DONE – empty in board 100")
    void findByStatus_done() {
        assertThat(repo.findByBoardIdAndStatusAndIsArchivedFalse(100L, CardStatus.DONE)).isEmpty();
    }

    // ── findByBoardIdAndPriorityAndIsArchivedFalse ───────────────────────────
    @Test @DisplayName("findByPriority HIGH – returns HIGH priority card")
    void findByPriority_high() {
        List<Card> high = repo.findByBoardIdAndPriorityAndIsArchivedFalse(100L, Priority.HIGH);
        assertThat(high).hasSize(1).first()
                .extracting(Card::getTitle).isEqualTo("Design login page");
    }

    @Test @DisplayName("findByPriority CRITICAL – finds overdue card")
    void findByPriority_critical() {
        List<Card> critical = repo.findByBoardIdAndPriorityAndIsArchivedFalse(100L, Priority.CRITICAL);
        assertThat(critical).hasSize(1);
    }

    // ── shiftPositionsRight / Left ───────────────────────────────────────────
    @Test @DisplayName("shiftPositionsRight – increments positions >= given value")
    void shiftRight() {
        repo.shiftPositionsRight(10L, 0);
        repo.flush();
        entityManager.clear();
        assertThat(repo.findById(c1.getId()).orElseThrow().getPosition()).isEqualTo(1);
        assertThat(repo.findById(c2.getId()).orElseThrow().getPosition()).isEqualTo(2);
    }

    @Test @DisplayName("shiftPositionsLeft – decrements positions > given value")
    void shiftLeft() {
        repo.shiftPositionsLeft(10L, 0);
        repo.flush();
        entityManager.clear();
        assertThat(repo.findById(c2.getId()).orElseThrow().getPosition()).isEqualTo(0);
    }

    // ── searchByTitle ────────────────────────────────────────────────────────
    @Test @DisplayName("searchByTitle – finds card by keyword")
    void searchByTitle_found() {
        List<Card> results = repo.searchByTitle(100L, "login");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).containsIgnoringCase("login");
    }

    @Test @DisplayName("searchByTitle – empty for no match")
    void searchByTitle_noMatch() {
        assertThat(repo.searchByTitle(100L, "xyznomatch")).isEmpty();
    }

    @Test @DisplayName("searchByTitle – excludes other boards")
    void searchByTitle_boardIsolation() {
        List<Card> results = repo.searchByTitle(100L, "Other");
        assertThat(results).isEmpty();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────
    @Test @DisplayName("save – persists with auto-generated ID")
    void save_persistsEntity() {
        Card saved = repo.save(Card.builder()
                .listId(30L).boardId(200L).title("New task")
                .position(0).priority(Priority.LOW).status(CardStatus.TO_DO)
                .isArchived(false).createdById(1L).createdAt(LocalDateTime.now()).build());
        assertThat(saved.getId()).isNotNull();
    }

    @Test @DisplayName("delete – removes entity")
    void delete_removes() {
        repo.delete(c1);
        assertThat(repo.findById(c1.getId())).isEmpty();
    }

    @Test @DisplayName("findById – returns correct card")
    void findById_correct() {
        Optional<Card> card = repo.findById(c2.getId());
        assertThat(card).isPresent();
        assertThat(card.get().getTitle()).isEqualTo("Implement JWT auth");
    }

    @Test @DisplayName("count – returns total card count")
    void count_total() {
        assertThat(repo.count()).isGreaterThanOrEqualTo(5);
    }
}