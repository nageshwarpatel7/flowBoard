package com.flowboard.board_service.controller;

import com.flowboard.board_service.dto.*;
import com.flowboard.board_service.entity.BoardMember;
import com.flowboard.board_service.exception.CustomException;
import com.flowboard.board_service.service.BoardService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    //--- Resolve userId from headers ----

    private Long resolveUserId(Long userIdHeader, String userEmail){
        if(userIdHeader !=null) return userIdHeader;

        throw new CustomException("X-User-Id header is required", HttpStatus.BAD_REQUEST);
    }

    @PostMapping
    public ResponseEntity<BoardResponse> create(
            @Valid @RequestBody CreateBoardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
            ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createBoard(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.getBoardById(id,userId));
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<BoardResponse>> getByWorkspace(
            @PathVariable Long workspaceId,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.getBoardsByWorkspace(workspaceId, userId));
    }

    @GetMapping("member/{userId}")
    public ResponseEntity<List<BoardResponse>> getByMember(
            @PathVariable Long userId
    ){
        return ResponseEntity.ok(boardService.getBoardsByMember(userId));
    }

    @GetMapping("/creator/{createdById}")
    public ResponseEntity<List<BoardResponse>> getByCreator(@PathVariable Long createdById){
        return ResponseEntity.ok(boardService.getBoardsByCreator(createdById));
    }

    @GetMapping("/public")
    public ResponseEntity<List<BoardResponse>> getPublic(){
        return ResponseEntity.ok(boardService.getPublicBoards());
    }

    @GetMapping("/workspace/{workspaceId}/closed")
    public ResponseEntity<List<BoardResponse>> getClosedBoards(
            @PathVariable Long workspaceId,
            @RequestHeader(name = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(name = "X-User-Email", required = false ) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.getClosedBoards(workspaceId, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoardResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
            ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.updateBoard(id, request, userId));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<BoardResponse> close(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.closeBoard(id, userId));
    }

    @PutMapping("/{id}/reopen")
    public ResponseEntity<BoardResponse> reopen(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.reopenBoard(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        boardService.deleteBoard(id, userId);
        return ResponseEntity.ok("Board deleted successfully");
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<BoardMember> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddBoardMemberRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
            ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.addMember(id, request, userId));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<String> deleteMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHHeader, userEmail);
        boardService.removeMember(id, memberId, userId);
        return ResponseEntity.ok("Member removed successfully");
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<String> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateBoardMemberRoleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
            ){
        Long userId = resolveUserId(userIdHHeader, userEmail);
        boardService.updateMemberRole(id, memberId, request, userId);
        return ResponseEntity.ok("Member role updated successfully");
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<BoardMember>> getMembers(@PathVariable Long id){
        return ResponseEntity.ok(boardService.getMembers(id));
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<BoardResponse.BoardAnalytics> getAnalytics(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHHeader, userEmail);
        return ResponseEntity.ok(boardService.getBoardAnalytics(id, userId));
    }

}
