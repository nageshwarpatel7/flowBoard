package com.flowBoard.list_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ListResponse {
    private Long id;
    private Long boardId;
    private String name;
    private Integer position;
    private String color;
    private boolean isArchived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private int cardCount;
}
