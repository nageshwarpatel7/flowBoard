package com.flowboard.payment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(nullable = false)
    private String status;

    private String billingCycle;
    private String stripeCustomerId;
    private String stripeSubscriptionId;

    private LocalDate currentPeriodStart;
    private LocalDate currentPeriodEnd;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime cancelledAt;
}
