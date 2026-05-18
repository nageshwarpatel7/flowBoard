package com.flowboard.card_service.scheduler;

import com.flowboard.card_service.client.NotificationClient;
import com.flowboard.card_service.client.dto.NotifyDueDateRequest;
import com.flowboard.card_service.client.dto.NotifyOverdueRequest;
import com.flowboard.card_service.entity.Card;
import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DueDateScheduler {

    private final CardRepository cardRepository;
    private final NotificationClient notificationClient;

    /** Runs every morning at 08:00 — notifies assignees whose card is due tomorrow. */
    @Scheduled(cron = "0 0 8 * * *")
    public void notifyDueTomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Card> cards = cardRepository.findByDueDateAndIsArchivedFalseAndStatusNot(
                tomorrow, CardStatus.DONE
        );

        // FIX: typo "Schedular" → "Scheduler" (affects log grep / monitoring queries)
        log.info("[Scheduler] Due-tomorrow check: {} cards due on {}", cards.size(), tomorrow);

        for (Card card : cards) {
            if (card.getAssigneeId() == null) continue;

            try {
                notificationClient.notifyDueDate(new NotifyDueDateRequest(
                        card.getAssigneeId(),
                        card.getId(),
                        card.getTitle(),
                        "24 hours",
                        null));

                log.debug("Due-tomorrow notification sent for cardId={}", card.getId());
            } catch (Exception e) {
                log.error("Failed to notify due-date for cardId={}: {}", card.getId(), e.getMessage());
            }
        }
    }

    /**
     * FIX: was "@Scheduled(cron = "0 0 * * * *")" (fires every hour) with an in-body
     *      "if (currentHour != 22) return;" guard — fragile, timezone-sensitive, and wasteful.
     *      Replaced with a direct cron that fires exactly at 22:00 server time.
     *
     * Notifies assignees whose card is due today (2-hour warning at 22:00).
     */
    @Scheduled(cron = "0 0 22 * * *")
    public void notifyDueInTwoHours() {
        LocalDate today = LocalDate.now();

        List<Card> cards = cardRepository
                .findByDueDateAndIsArchivedFalseAndStatusNot(today, CardStatus.DONE);

        log.info("[Scheduler] Due-today urgent check: {} cards", cards.size());

        for (Card card : cards) {
            if (card.getAssigneeId() == null) continue;

            try {
                notificationClient.notifyDueDate(new NotifyDueDateRequest(
                        card.getAssigneeId(),
                        card.getId(),
                        card.getTitle(),
                        "2 hours",
                        null));
            } catch (Exception e) {
                log.error("Failed urgent-due-date notification cardId={}: {}",
                        card.getId(), e.getMessage());
            }
        }
    }

    /** Runs every morning at 09:00 — notifies assignees of all overdue cards. */
    @Scheduled(cron = "0 0 9 * * *")
    public void notifyOverdueCards() {

        List<Card> overdueCards = cardRepository.findAllOverdueBeforeDate(LocalDate.now());

        log.info("[Scheduler] Overdue check: {} cards overdue", overdueCards.size());

        for (Card card : overdueCards) {
            // FIX: removed stray double semicolon (;;) that was after this continue statement
            if (card.getAssigneeId() == null) continue;

            try {
                notificationClient.notifyOverdue(new NotifyOverdueRequest(
                        card.getAssigneeId(),
                        card.getId(),
                        card.getTitle(),
                        card.getDueDate().toString(),
                        null));

                log.debug("Overdue notification sent for cardId={}", card.getId());
            } catch (Exception e) {
                log.error("Failed overdue notification cardId={}: {}", card.getId(), e.getMessage());
            }
        }
    }
}
