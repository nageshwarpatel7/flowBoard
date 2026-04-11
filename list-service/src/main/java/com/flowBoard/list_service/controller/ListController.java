package com.flowBoard.list_service.controller;

import com.flowBoard.list_service.dto.*;
import com.flowBoard.list_service.exception.CustomException;
import com.flowBoard.list_service.service.ListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/lists")
@RequiredArgsConstructor
public class ListController {

    private final ListService listService;

    private Long resolveUserId(Long userIdHeader){
        if(userIdHeader!=null) return userIdHeader;

        throw new CustomException("X-User-Id header is required", HttpStatus.BAD_REQUEST);
    }

    @PostMapping
    public ResponseEntity<ListResponse> create(
            @Valid @RequestBody CreateListRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listService.createList(request, resolveUserId(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListResponse> getById(@PathVariable Long id){
        return ResponseEntity.ok(listService.getListById(id));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<ListResponse>> getByBoard(
            @PathVariable Long boardId
    ){
        return ResponseEntity.ok(listService.getListsByBoard(boardId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
            ){
        return ResponseEntity.ok(listService.updateList(id,request, resolveUserId(userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ){
        listService.deleteList(id, resolveUserId(userId));
        return ResponseEntity.ok("List deleted successfully");
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<ListResponse>> reorder(
            @Valid @RequestBody ReorderListRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
            ){
        return ResponseEntity.ok(listService.reorderLists(request, resolveUserId(userId)));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<ListResponse> archive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ){
        return ResponseEntity.ok(listService.archiveList(id, resolveUserId(userId)));
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<ListResponse> unarchive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ){
        return ResponseEntity.ok(listService.unarchiveList(id, resolveUserId(userId)));
    }

    @GetMapping("/board/{boardId}/archived")
    public ResponseEntity<List<ListResponse>> getArchived(
            @PathVariable Long boardId
    ){
        return ResponseEntity.ok(listService.getArchivedLists(boardId));
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<ListResponse> move(
            @PathVariable Long id,
            @Valid @RequestBody MoveListRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
            ){
        return ResponseEntity.ok(listService.moveList(id, request, resolveUserId(userId)));
    }
}
