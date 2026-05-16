package com.flowboard.payment_service.dto;

import lombok.Data;

@Data
public class ConfirmCheckoutSessionRequest {
    private String sessionId;
}
