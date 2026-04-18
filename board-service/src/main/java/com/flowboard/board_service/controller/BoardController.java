package com.flowboard.board_service.controller;

import com.flowboard.board_service.dto.*;
import com.flowboard.board_service.entity.BoardMember;
import com.flowboard.board_service.exception.CustomException;
import com.flowboard.board_service.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Board Management", description = "Board CRUD, members, analytics")
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

    @Operation(summary = "Create board", description = "Creates a new board in workspace")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Board created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<BoardResponse> create(
            @Valid @RequestBody CreateBoardRequest request,

            @Parameter(description = "User ID", example = "1")
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,

            @Parameter(description = "User email", example = "test@gmail.com")
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createBoard(request, userId));
    }

    @Operation(summary = "Get board by ID")
    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.getBoardById(id,userId));
    }

    @Operation(summary = "Get boards by workspace")
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<BoardResponse>> getByWorkspace(
            @PathVariable Long workspaceId,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.getBoardsByWorkspace(workspaceId, userId));
    }

    @Operation(summary = "Get public boards")
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

    @Operation(summary = "Update board details")
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

    @Operation(summary = "Close board")
    @PutMapping("/{id}/close")
    public ResponseEntity<BoardResponse> close(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.closeBoard(id, userId));
    }

    @Operation(summary = "Reopen board")
    @PutMapping("/{id}/reopen")
    public ResponseEntity<BoardResponse> reopen(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ){
        Long userId = resolveUserId(userIdHeader, userEmail);
        return ResponseEntity.ok(boardService.reopenBoard(id, userId));
    }

    @Operation(summary = "Delete board")
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

    @Operation(summary = "Add member to board")
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

    @Operation(summary = "Remove member from board")
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

    @Operation(summary = "Update member role")
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

    @Operation(summary = "Get all board members")
    @GetMapping("/{id}/members")
    public ResponseEntity<List<BoardMember>> getMembers(@PathVariable Long id){
        return ResponseEntity.ok(boardService.getMembers(id));
    }

    @Operation(summary = "Get board analytics")
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
