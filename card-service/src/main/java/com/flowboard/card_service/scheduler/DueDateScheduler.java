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
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DueDateScheduler {

    private final CardRepository cardRepository;
    private final NotificationClient notificationClient;

    @Scheduled(cron = "0 0 8 * * *")
    public void notifyDueTomorrow(){
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Card> cards = cardRepository.findByDueDateAndIsArchivedFalseAndStatusNot(
                tomorrow, CardStatus.DONE
        );

        log.info("[Schedular] Due-tomorrow check: {} cards due on {}", cards.size(), tomorrow);

        for(Card card: cards){
            if(card.getAssigneeId()==null) continue;

            try{
                notificationClient.notifyDueDate(new NotifyDueDateRequest(card.getAssigneeId(),
                        card.getId(),
                        card.getTitle(), "24 hours", null));

                log.debug("Due-tomorrow notification sent for cardId={}", card.getId());
            }catch (Exception e){
                log.error("Failed to notify due-date for cardId={}: {}", card.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void notifyDueInTwoHours(){
        LocalDate today = LocalDate.now();
        int currentHour = LocalDateTime.now().getHour();

        if(currentHour!=22) return;

        List<Card> cards = cardRepository
                .findByDueDateAndIsArchivedFalseAndStatusNot(
                        today, CardStatus.DONE
                );

        log.info("[Schedular] Due-today urgent check: {} cards", cards.size());

        for(Card card: cards){

            if(card.getAssigneeId() == null) continue;

            try{
                notificationClient.notifyDueDate(new NotifyDueDateRequest(
                        card.getAssigneeId(),
                        card.getId(),
                        card.getTitle(),
                        "2 hours",
                        null
                ));
            }catch (Exception e){
                log.error("Failed urgent-due-date notification cardId={}: {}",
                        card.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void notifyOverdueCards(){

        List<Card> overdueCards = cardRepository.findAllOverdueBeforeDate(LocalDate.now());

        log.info("[Schedular] Overdue check: {} cards overdue", overdueCards.size());

        for(Card card: overdueCards){
            if(card.getAssigneeId()==null) continue;;

            try{
                notificationClient.notifyOverdue(new NotifyOverdueRequest(
                        card.getAssigneeId(),
                        card.getId(),
                        card.getTitle(),
                        card.getDueDate().toString(),
                        null
                ));

                log.debug("Overdue notification sent for cardId={}", card.getId());
            }catch (Exception e){
                log.error("Failed overdue notification cardId={}: {}", card.getId(), e.getMessage());
            }
        }

    }
}
