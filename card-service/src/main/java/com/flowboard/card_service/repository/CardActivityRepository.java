package com.flowboard.card_service.repository;

import com.flowboard.card_service.entity.CardActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardActivityRepository  extends JpaRepository<CardActivity, Long> {

    List<CardActivity> findByCardIdOrderByCreatedAtDesc(Long cardId);

    List<CardActivity> findByActorIdOrderByCreatedAtDesc(Long actorId);

    List<CardActivity> findTop10ByCardIdOrderByCreatedAtDesc(Long cardId);

    Page<CardActivity> findByCardIdOrderByCreatedAtDesc(
            Long cardId, Pageable pageable);
}
