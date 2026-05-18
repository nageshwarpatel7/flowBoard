package com.flowboard.board_service.entity;

import com.flowboard.board_service.enums.BoardMemberRole;
import com.flowboard.board_service.enums.Visibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_members", uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BoardMemberRole role = BoardMemberRole.MEMBER;

    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
