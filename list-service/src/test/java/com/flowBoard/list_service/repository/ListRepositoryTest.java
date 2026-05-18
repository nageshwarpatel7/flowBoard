package com.flowBoard.list_service.repository;

import com.flowBoard.list_service.entity.TaskList;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ListRepository – @DataJpaTest")
class ListRepositoryTest {

    @Autowired ListRepository repo;
    @Autowired TestEntityManager entityManager;

    private TaskList todo, inProgress, done, archived;

    @BeforeEach
    void setUp() {
        todo = repo.save(TaskList.builder()
                .boardId(10L).name("To Do").position(0)
                .isArchived(false).createdAt(LocalDateTime.now()).build());

        inProgress = repo.save(TaskList.builder()
                .boardId(10L).name("In Progress").position(1)
                .isArchived(false).createdAt(LocalDateTime.now()).build());

        done = repo.save(TaskList.builder()
                .boardId(10L).name("Done").position(2)
                .isArchived(false).createdAt(LocalDateTime.now()).build());

        archived = repo.save(TaskList.builder()
                .boardId(10L).name("Archived List").position(0)
                .isArchived(true).createdAt(LocalDateTime.now()).build());
    }

    // ── findByBoardIdAndIsArchivedFalseOrderByPosition ──────────────────────
    @Test @DisplayName("findByBoard active – sorted, no archived")
    void findByBoard_active_sorted() {
        List<TaskList> lists = repo.findByBoardIdAndIsArchivedFalseOrderByPosition(10L);
        assertThat(lists).hasSize(3).noneMatch(TaskList::isArchived);
        assertThat(lists.get(0).getPosition()).isLessThanOrEqualTo(lists.get(1).getPosition());
        assertThat(lists.get(1).getPosition()).isLessThanOrEqualTo(lists.get(2).getPosition());
    }

    @Test @DisplayName("findByBoard active – empty for unknown board")
    void findByBoard_empty() {
        assertThat(repo.findByBoardIdAndIsArchivedFalseOrderByPosition(999L)).isEmpty();
    }

    // ── findByBoardIdOrderByPosition ────────────────────────────────────────
    @Test @DisplayName("findByBoardId all – includes archived")
    void findByBoard_all() {
        List<TaskList> all = repo.findByBoardIdOrderByPosition(10L);
        assertThat(all).hasSize(4);
    }

    // ── findByBoardIdAndIsArchivedTrue ──────────────────────────────────────
    @Test @DisplayName("findByBoard archived – returns archived only")
    void findByBoard_archived() {
        List<TaskList> archLists = repo.findByBoardIdAndIsArchivedTrue(10L);
        assertThat(archLists).hasSize(1)
                .first().extracting(TaskList::getName).isEqualTo("Archived List");
    }

    // ── findMaxPositionByBoardId ────────────────────────────────────────────
    @Test @DisplayName("findMaxPositionByBoardId – returns highest position")
    void findMaxPosition() {
        Optional<Integer> max = repo.findMaxPositionByBoardId(10L);
        assertThat(max).isPresent().hasValue(2);
    }

    @Test @DisplayName("findMaxPositionByBoardId – empty for empty board")
    void findMaxPosition_emptyBoard() {
        assertThat(repo.findMaxPositionByBoardId(999L)).isEmpty();
    }

    // ── shiftPositionsRight / Left ──────────────────────────────────────────
    @Test @DisplayName("shiftPositionsRight – shifts positions >= given value")
    void shiftRight() {
        repo.shiftPositionsRight(10L, 0);
        repo.flush();
        entityManager.clear();
        assertThat(repo.findById(todo.getId()).orElseThrow().getPosition()).isEqualTo(1);
        assertThat(repo.findById(inProgress.getId()).orElseThrow().getPosition()).isEqualTo(2);
        assertThat(repo.findById(done.getId()).orElseThrow().getPosition()).isEqualTo(3);
    }

    @Test @DisplayName("shiftPositionsLeft – shifts positions > given value")
    void shiftLeft() {
        repo.shiftPositionsLeft(10L, 0);
        repo.flush();
        entityManager.clear();
        assertThat(repo.findById(inProgress.getId()).orElseThrow().getPosition()).isEqualTo(0);
        assertThat(repo.findById(done.getId()).orElseThrow().getPosition()).isEqualTo(1);
    }

    // ── countByBoardId ──────────────────────────────────────────────────────
    @Test @DisplayName("countByBoardId – returns total list count")
    void countByBoardId() {
        assertThat(repo.countByBoardId(10L)).isEqualTo(4);
    }

    @Test @DisplayName("countByBoardId – zero for unknown board")
    void countByBoardId_zero() {
        assertThat(repo.countByBoardId(999L)).isEqualTo(0);
    }

    // ── existsById ──────────────────────────────────────────────────────────
    @Test @DisplayName("existsById – true for saved list")
    void existsById_true() {
        assertThat(repo.existsById(todo.getId())).isTrue();
    }

    @Test @DisplayName("existsById – false for missing list")
    void existsById_false() {
        assertThat(repo.existsById(99999L)).isFalse();
    }

    // ── CRUD ────────────────────────────────────────────────────────────────
    @Test @DisplayName("save – assigns ID on persist")
    void save_assignsId() {
        TaskList l = repo.save(TaskList.builder()
                .boardId(20L).name("Review").position(0)
                .isArchived(false).createdAt(LocalDateTime.now()).build());
        assertThat(l.getId()).isNotNull();
    }

    @Test @DisplayName("save – updates name field")
    void save_updatesName() {
        todo.setName("Backlog");
        repo.save(todo);
        assertThat(repo.findById(todo.getId()).orElseThrow().getName()).isEqualTo("Backlog");
    }

    @Test @DisplayName("delete – removes list")
    void delete_removes() {
        repo.delete(archived);
        assertThat(repo.findById(archived.getId())).isEmpty();
    }

    @Test @DisplayName("findAll – includes all seeded lists")
    void findAll_all() {
        assertThat(repo.findAll()).hasSizeGreaterThanOrEqualTo(4);
    }
}