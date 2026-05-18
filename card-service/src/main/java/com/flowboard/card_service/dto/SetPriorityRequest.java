package com.flowboard.card_service.dto;

import com.flowboard.card_service.enums.Priority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetPriorityRequest {

    @NotNull(message = "Priority is required")
    private Priority priority;
}
