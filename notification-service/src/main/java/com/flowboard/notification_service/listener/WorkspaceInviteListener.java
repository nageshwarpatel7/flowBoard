package com.flowboard.notification_service.listener;

import com.flowboard.notification_service.config.RabbitMQConfig;
import com.flowboard.notification_service.event.WorkspaceInviteEvent;
import com.flowboard.notification_service.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceInviteListener {

    private final EmailNotificationService emailService;

    @RabbitListener(queues = RabbitMQConfig.INVITE_QUEUE)
    public void handleWorkspaceInvite(WorkspaceInviteEvent event) {
        log.info("Received WorkspaceInviteEvent: workspaceId={} invitee={}",
                event.getWorkspaceId(), event.getInviteeEmail());

        try {
            String subject = "You're invited to join " +
                    event.getWorkspaceName() + " on FlowBoard";

            String message = "You have been invited to join the workspace '"
                    + event.getWorkspaceName() + "' as "
                    + event.getRole() + ".\n\n"
                    + "Click the link below to accept:\n"
                    + event.getAcceptUrl() + "\n\n"
                    + "This invitation expires in 7 days.";

            emailService.sendNotificationEmail(
                    event.getInviteeEmail(),
                    subject,
                    message,
                    event.getAcceptUrl()
            );

            log.info("Invite email sent to {}", event.getInviteeEmail());

        } catch (Exception e) {
            log.error("Failed to send invite email to {}: {}",
                    event.getInviteeEmail(), e.getMessage());
        }
    }
}