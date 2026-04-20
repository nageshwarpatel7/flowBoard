package com.flowboard.payment_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long userId;
    private String planName;
    private String planDisplayName;
    private String status;
    private String billingCycle;
    private LocalDate currentPeriodEnd;
    private boolean hasAdvancedAnalytics;
    private boolean hasPrioritySupport;
    private boolean hasCustomFields;
    private boolean hasAutomation;
    private int maxWorkspaces;
    private int maxBoardsPerWorkspace;
}
