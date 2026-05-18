package com.flowboard.payment_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlanResponse {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private int maxWorkspaces;
    private int maxBoardsPerWorkspace;
    private int maxMembersPerWorkspace;
    private boolean hasAdvancedAnalytics;
    private boolean hasPrioritySupport;
    private boolean hasCustomFields;
    private boolean hasAutomation;
}
