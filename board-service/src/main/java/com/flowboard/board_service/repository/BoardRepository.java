package com.flowboard.board_service.repository;

import com.flowboard.board_service.entity.Board;
import com.flowboard.board_service.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {
    //All boards in a workspace
    List<Board> findByWorkspaceId(Long workspaceId);

    //All board created by a user
    List<Board> findByCreatedById(Long createdById);

    //All boards where user is a member
    @Query("SELECT b FROM Board b JOIN b.members m where m.userId = :userId")
    List<Board> findByMemberUserId(@Param("userId") Long userId);

    //All boards in a workspace with specific visibility
    List<Board> findByWorkspaceIdAndIsClosed(Long workspaceId, boolean isClosed);

    // Count boards in a workspace
    long countByWorkspaceId(Long workspaceId);

    // Public boards across all workspaces
    List<Board> findByVisibility(Visibility visibility);
}
