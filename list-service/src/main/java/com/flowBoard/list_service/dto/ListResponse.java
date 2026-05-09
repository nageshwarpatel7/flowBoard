package com.flowBoard.list_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
