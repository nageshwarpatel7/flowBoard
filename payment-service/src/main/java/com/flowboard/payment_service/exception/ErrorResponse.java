package com.flowboard.payment_service.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * NEW FILE — ErrorResponse DTO for payment-service.
 * Mirrors the structure used in auth-service, workspace-service, board-service, etc.
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timeStamp;
    private int status;
    private String error;
    private String message;
}
