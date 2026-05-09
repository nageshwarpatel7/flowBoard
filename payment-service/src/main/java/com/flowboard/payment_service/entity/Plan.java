package com.flowboard.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "subscription_plans")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Plan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String displayName;
    private String description;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private String stripePriceIdMonthly;
    private String stripePriceIdYearly;

    private int maxWorkspaces;
    private int maxBoardsPerWorkspace;
    private int maxMembersPerWorkspace;
    private boolean hasAdvancedAnalytics;
    private boolean hasPrioritySupport;
    private boolean hasCustomFields;
    private boolean hasAutomation;
}