package com.flowboard.card_service.repository;

import com.flowboard.card_service.entity.Card;
import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {

    // Active cards in a list ordered by position (for Kanban display)
    List<Card> findByListIdAndIsArchivedFalseOrderByPosition(Long listId);

    // All cards in a board (for board-level operations)
    List<Card> findByBoardIdAndIsArchivedFalse(Long boardId);

    // Cards assigned to a specific user across all boards
    List<Card> findByAssigneeIdAndIsArchivedFalse(Long assigneeId);

    // Cards by status in a board
    List<Card> findByBoardIdAndStatusAndIsArchivedFalse(
            Long boardId, CardStatus status);

    // Cards by priority in a board
    List<Card> findByBoardIdAndPriorityAndIsArchivedFalse(
            Long boardId, Priority priority);

    // Overdue cards — dueDate before today and not done
    @Query("SELECT c FROM Card c WHERE c.boardId = :boardId " +
            "AND c.isArchived = false " +
            "AND c.dueDate < :today " +
            "AND c.status != 'DONE'")
    List<Card> findOverdueByBoardId(
            @Param("boardId") Long boardId,
            @Param("today") LocalDate today);

    // Platform-wide overdue cards — for admin SLA monitoring
    @Query("SELECT c FROM Card c WHERE c.isArchived = false " +
            "AND c.dueDate < :today " +
            "AND c.status != 'DONE'")
    List<Card> findAllOverdue(@Param("today") LocalDate today);

    // Archived cards in a board
    List<Card> findByBoardIdAndIsArchivedTrue(Long boardId);

    // Archived cards in a list
    List<Card> findByListIdAndIsArchivedTrue(Long listId);

    // Count cards in a list
    long countByListIdAndIsArchivedFalse(Long listId);

    // Count cards in a board
    long countByBoardIdAndIsArchivedFalse(Long boardId);

    // Max position in a list — to append new card at end
    @Query("SELECT MAX(c.position) FROM Card c " +
            "WHERE c.listId = :listId AND c.isArchived = false")
    java.util.Optional<Integer> findMaxPositionByListId(
            @Param("listId") Long listId);

    // Shift card positions right when inserting at specific position
    @Modifying
    @Query("UPDATE Card c SET c.position = c.position + 1 " +
            "WHERE c.listId = :listId " +
            "AND c.position >= :fromPosition " +
            "AND c.isArchived = false")
    void shiftPositionsRight(
            @Param("listId") Long listId,
            @Param("fromPosition") int fromPosition);

    // Shift card positions left after removal
    @Modifying
    @Query("UPDATE Card c SET c.position = c.position - 1 " +
            "WHERE c.listId = :listId " +
            "AND c.position > :fromPosition " +
            "AND c.isArchived = false")
    void shiftPositionsLeft(
            @Param("listId") Long listId,
            @Param("fromPosition") int fromPosition);

    // Search cards by title across boards
    @Query("SELECT c FROM Card c WHERE c.boardId = :boardId " +
            "AND c.isArchived = false " +
            "AND LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Card> searchByTitle(
            @Param("boardId") Long boardId,
            @Param("keyword") String keyword);

    // Search by assignee and title
    @Query("SELECT c FROM Card c WHERE c.isArchived = false " +
            "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR c.assigneeId = :assigneeId)")
    List<Card> searchByTitleOrAssignee(
            @Param("keyword") String keyword,
            @Param("assigneeId") Long assigneeId);
}