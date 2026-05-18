package com.flowboard.card_service.dto;

import com.flowboard.card_service.enums.CardStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetStatusRequest {
    @NotNull(message = "Status is required")
    private CardStatus status;
}
