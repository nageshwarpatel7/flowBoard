package com.flowboard.card_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_activities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private String actionType;

    private String description;

    private String oldValue;

    private String newValue;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
