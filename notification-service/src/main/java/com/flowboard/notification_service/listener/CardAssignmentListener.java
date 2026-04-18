package com.flowboard.notification_service.listener;

import com.flowboard.notification_service.config.RabbitMQConfig;
import com.flowboard.notification_service.dto.SendNotificationRequest;
import com.flowboard.notification_service.enums.NotificationType;
import com.flowboard.notification_service.event.CardAssignedEvent;
import com.flowboard.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardAssignmentListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.CARD_ASSIGNMENT_QUEUE)
    public void handleCardAssigned(CardAssignedEvent event) {
        log.info("Received CardAssignedEvent: cardId={} assigneeId={}",
                event.getCardId(), event.getAssigneeId());

        try {
            SendNotificationRequest req = new SendNotificationRequest();
            req.setRecipientId(event.getAssigneeId());
            req.setActorId(event.getAssignedByUserId());
            req.setType(NotificationType.ASSIGNMENT);
            req.setTitle("Card assigned to you");
            req.setMessage("You have been assigned to card: '"
                    + event.getCardTitle() + "'");
            req.setRelatedId(event.getCardId());
            req.setRelatedType("CARD");
            req.setDeepLinkUrl("/boards/" + event.getBoardId()
                    + "/cards/" + event.getCardId());

            // Send email if email is provided
            if (event.getAssigneeEmail() != null) {
                req.setSendEmail(true);
                req.setRecipientEmail(event.getAssigneeEmail());
            }

            notificationService.send(req);
            log.info("Assignment notification sent for cardId={}",
                    event.getCardId());

        } catch (Exception e) {
            log.error("Failed to process CardAssignedEvent: {}",
                    e.getMessage(), e);
            // RabbitMQ will retry — do not re-throw unless fatal
        }
    }
}