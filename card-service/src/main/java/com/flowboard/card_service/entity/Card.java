package com.flowboard.card_service.entity;

import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="cards")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long listId;

    @Column(nullable = false)
    private Long boardId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CardStatus status = CardStatus.TO_DO;

    private LocalDate dueDate;

    private LocalDate startDate;

    private Long assigneeId;

    @Column(nullable = false)
    private Long createdById;

    @Builder.Default
    private boolean isArchived = false;

    private String coverColor;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @Transient
    private boolean overdue;

    @PostLoad
    public void computeOverdue(){
        this.overdue = dueDate!= null
                &&  LocalDate.now().isAfter(dueDate)
                && status!= CardStatus.DONE;
    }
}
