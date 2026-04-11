package com.flowBoard.list_service.repository;

import com.flowBoard.list_service.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ListRepository extends JpaRepository<TaskList, Long> {

    List<TaskList> findByBoardIdAndIsArchivedFalseOrderByPosition(Long boardId);

    List<TaskList> findByBoardIdAndIsArchivedTrue(Long boardId);

    List<TaskList> findByBoardId(Long boardId);

    Optional<TaskList> findByIdAndBoardId(Long id, Long boardId);

    long countByBoardIdAndIsArchivedFalse(Long boardId);

    @Query("SELECT MAX(t.position) FROM TaskList t "+
    "WHERE t.boardId = :boardId AND t.isArchived= false")
    Optional<Integer> findMaxPositionByBoardId(@Param("boardId") Long boardId);

    @Modifying
    @Query("UPDATE TaskList t SET t.position = t.position + 1 "+
    "WHERE t.boardId = :boardId "+
    "AND t.position >= :fromPosition "+
    "AND t.isArchived = false")
    void shiftPositionsRight(@Param("boardId") Long boardId,
                             @Param("fromPosition") int fromPosition);

    @Modifying
    @Query("UPDATE TaskList t SET t.position = t.position -1 "+
    "WHERE t.boardId = :boardId "+
    "AND t.position > :fromPosition "+
    "AND t.isArchived = false")
    void shiftPositionsLeft(@Param("boardId") Long boardId,
                            @Param("fromPosition") int fromPosition);

    boolean existsByBoardIdAndPositionAndIsArchivedFalse( Long boardId, int position);
}
