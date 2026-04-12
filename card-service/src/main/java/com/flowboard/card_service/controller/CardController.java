package com.flowboard.card_service.controller;

import com.flowboard.card_service.dto.*;
import com.flowboard.card_service.entity.Card;
import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import com.flowboard.card_service.exception.CustomException;
import com.flowboard.card_service.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    private Long resolveUserId(Long userIdHeader){
        if(userIdHeader!=null) return userIdHeader;
        throw new CustomException("X-User-Id header is required",
                HttpStatus.BAD_REQUEST);
    }

    @PostMapping
    public ResponseEntity<CardResponse> create(
            @Valid @RequestBody CreateCardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.createCard(request, resolveUserId(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getById(@PathVariable Long id){
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @GetMapping("/list/{listId}")
    public ResponseEntity<List<CardResponse>> getByList(
            @PathVariable Long listId){
        return ResponseEntity.ok(cardService.getCardByList(listId));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<CardResponse>> getByBoard(
            @PathVariable Long boardId){
        return ResponseEntity.ok(cardService.getCardByBoard(boardId));
    }

    @GetMapping("/assignee/{userId}")
    public ResponseEntity<List<CardResponse>> getByAssignee(
            @PathVariable Long userId){
        return ResponseEntity.ok(cardService.getCardByAssignee(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){
        return ResponseEntity.ok(cardService.updateCard(id, request, resolveUserId(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){
        cardService.deleteCard(id, resolveUserId(userId));
        return ResponseEntity.ok("Card deleted successfully");
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<CardResponse> move(
            @PathVariable Long id,
            @Valid @RequestBody MoveCardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){
        return ResponseEntity.ok(cardService.moveCard(id, request, resolveUserId(userId)));
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<CardResponse>> reorder(
            @Valid @RequestBody ReorderCardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){
        return ResponseEntity.ok(cardService.reorderCards(request, resolveUserId(userId)));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<CardResponse> archive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){
        return ResponseEntity.ok(cardService.archiveCard(id, resolveUserId(userId)));
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<CardResponse> unarchive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){
        return ResponseEntity.ok(cardService.unarchiveCard(id, resolveUserId(userId)));
    }

    @GetMapping("/board/{boardId}/archived")
    public ResponseEntity<List<CardResponse>> getArchivedByBoard(
            @PathVariable Long boardId){
        return ResponseEntity.ok(cardService.getArchivedCardsByBoard(boardId));
    }

    @GetMapping("/list/{listId}/archived")
    public ResponseEntity<List<CardResponse>> getArchivedByList(
            @PathVariable Long listId){
        return ResponseEntity.ok(cardService.getArchivedCardsByList(listId));
    }

    @PutMapping("/{id}/assignee")
    public ResponseEntity<CardResponse> setAssignment(
            @PathVariable Long id,
            @RequestBody AssignCardRequest request,
            @RequestHeader(value = "X-User-id", required = false) Long userId){
        return ResponseEntity.ok(cardService.setAssignee(id, request, resolveUserId(userId)));
    }

    @PutMapping("/{id}/priority")
    public ResponseEntity<CardResponse> setPriority(
            @PathVariable Long id,
            @Valid @RequestBody SetPriorityRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){
        return ResponseEntity.ok(cardService.setPriority(id, request, resolveUserId(userId)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CardResponse> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody SetStatusRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){
        return ResponseEntity.ok(cardService.setStatus(id, request, resolveUserId(userId)));
    }

    @GetMapping("/board/{boardId}/status/{status}")
    public ResponseEntity<List<CardResponse>> getByStatus(
            @PathVariable Long boardId,
            @PathVariable CardStatus status){
        return ResponseEntity.ok(cardService.getCardsByStatus(boardId, status));
    }

    @GetMapping("/board/{boardId}/priority/{priority}")
    public ResponseEntity<List<CardResponse>> getByPriority(
            @PathVariable Long boardId,
            @PathVariable Priority priority) {
        return ResponseEntity.ok(
                cardService.getCardsByPriority(boardId, priority));
    }

    @GetMapping("/board/{boardId}/overdue")
    public ResponseEntity<List<CardResponse>> getOverdueByBoard(
            @PathVariable Long boardId) {
        return ResponseEntity.ok(cardService.getOverdueCardsByBoard(boardId));
    }

    @GetMapping("/overdue/all")
    public ResponseEntity<List<CardResponse>> getAllOverdue(){
        return ResponseEntity.ok(cardService.getAllOverdueCards());
    }

    @GetMapping("/board/{boardId}/search")
    public ResponseEntity<List<CardResponse>> search(
            @PathVariable Long boardId,
            @RequestParam String keyword){
        return ResponseEntity.ok(cardService.searchCards(boardId, keyword));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CardResponse>> searchGlobal(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long assigneeId){
        return ResponseEntity.ok(cardService.searchByTitleOrAssignee(keyword, assigneeId));
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<List<CardActivityResponse>> getActivity(
            @PathVariable Long id){
        return ResponseEntity.ok(cardService.getCardActivity(id));
    }
}
