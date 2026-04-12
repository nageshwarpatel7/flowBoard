package com.flowboard.card_service.service;

import com.flowboard.card_service.dto.*;
import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;

import java.util.List;
import java.util.Optional;

public interface CardService {

    CardResponse createCard(CreateCardRequest request, Long userId);
    CardResponse getCardById(Long cardId);
    List<CardResponse> getCardByList(Long listId);
    List<CardResponse> getCardByBoard(Long boardId);
    List<CardResponse> getCardByAssignee(Long assigneeId);
    CardResponse updateCard(Long cardId, UpdateCardRequest request, Long userId);
    void deleteCard(Long cardId, Long userId);

    CardResponse moveCard(Long cardId, MoveCardRequest request, Long userId);
    List<CardResponse> reorderCards(ReorderCardRequest request, Long userId);

    CardResponse archiveCard(Long cardId, Long userId);
    CardResponse unarchiveCard(Long cardId, Long userId);
    List<CardResponse> getArchivedCardsByBoard(Long boardId);
    List<CardResponse> getArchivedCardsByList(Long listId);

    CardResponse setAssignee(Long cardId, AssignCardRequest request, Long userId);

    CardResponse setPriority(Long cardId, SetPriorityRequest request, Long userId);
    CardResponse setStatus(Long cardId, SetStatusRequest request, Long userId);

    List<CardResponse> getCardsByStatus(Long boardId, CardStatus status);
    List<CardResponse> getCardsByPriority(Long boardId, Priority priority);

    List<CardResponse> getOverdueCardsByBoard(Long boardId);
    List<CardResponse> getAllOverdueCards();

    List<CardResponse> searchCards(Long boardId, String keyword);
    List<CardResponse> searchByTitleOrAssignee(String keyword, Long assigneeId);

    List<CardActivityResponse> getCardActivity(Long cardId);
}
