package com.flowboard.payment_service.dto;

import lombok.Data;

@Data
public class CreateCheckoutSessionRequest {

    private Long planId;
    private String billingCycle;
}
