package com.flowboard.board_service.repository;

import com.flowboard.board_service.entity.BoardMember;
import com.flowboard.board_service.enums.BoardMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

    List<BoardMember> findByBoardId(Long boardId);

    Optional<BoardMember> findByBoardIdAndUserId(Long boardId, Long userId);

    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    void deleteByBoardIdAndUserId(Long boardId, Long userId);

    List<BoardMember> findByBoardIdAndRole(Long boardId, BoardMemberRole role);

    List<BoardMember> findByUserId(Long userId);
}
