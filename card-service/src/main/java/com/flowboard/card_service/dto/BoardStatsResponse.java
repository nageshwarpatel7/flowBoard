package com.flowboard.card_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BoardStatsResponse {
    private Long boardId;
    private long totalCards;
    private long completedCards;
    private long overdueCards;
    private long archivedCards;
    private double completionRate;
    private double overdueRate;
    private Map<String, Long> cardsByStatus;
    private Map<String, Long> cardsByPriority;
    private Map<Long, Long>   cardsByAssignee;
}