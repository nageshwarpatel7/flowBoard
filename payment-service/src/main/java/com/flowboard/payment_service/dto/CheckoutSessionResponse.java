package com.flowboard.payment_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutSessionResponse {
    private String sessionId;
    private String checkoutUrl;
}
