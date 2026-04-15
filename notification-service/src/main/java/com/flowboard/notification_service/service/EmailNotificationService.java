package com.flowboard.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendNotificationEmail(String toEmail,
                                      String title,
                                      String message,
                                      String deepLinkUrl){
        try{
            log.info("Sending notification email to: {}", toEmail);
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("FlowBoard -- "+title);
            helper.setText(buildEmailBody(title, message, deepLinkUrl), true);

            mailSender.send(mail);
            log.info("Notification email sent to: {}",toEmail);

        } catch(MessagingException e){
                log.error("Failed to send notification email to {} : {}",
                        toEmail, e.getMessage(), e);
        }
    }

    @Async
    public void sendAssignmentEmail(String toEmail,
                                    String cardTitle,
                                    String assignedBy,
                                    String deepLinkUrl){
        String title = "You have been assigned a card";
        String message = assignedBy +" assigned you to '"
                +cardTitle+ "'. Click below to view ";
        sendNotificationEmail(toEmail, title, message, deepLinkUrl);
    }

    @Async
    public void sendOverdueEmail(String toEmail,
                                 String cardTitle,
                                 String dueDate){
        String title = "Overdue card - action is required";
        String message = "The card '"+cardTitle+"' was due on "+dueDate
                +" and has not been marked as done.";

        sendNotificationEmail(toEmail, title, message, null);
    }

    private String buildEmailBody(String title, String message, String deepLinkUrl){

        String linkHtml = (deepLinkUrl !=null && !deepLinkUrl.isBlank())
                ? """
                <div style = "margin:24px 0;text-align:center;">
                    <a href="http://localhost:8080%s
                       style="background:#4f46e5;color:#fff;padding:12px 28px;
                              border-radius:6px;text-decoration:none;
                              font-weight:500;font-size:14px;">
                       View in FlowBoard
                    </a>
                </div>
                """.formatted(deepLinkUrl) :"";

        return """
                <div style="font-family:Arial,sans-serif;max-width:520px;
                           margin:auto;padding:32px;
                           border:1px solid #e0e0e0;border-radius:8px;">
                 <h2 style="color:#1a1a2e;margin-bottom:8px;">
                   FlowBoard Notification
                 </h2>
                 <h3 style="color:#4f46e5;margin-bottom:16px;">%s</h3>
                 <p style="color:#444;font-size:15px;line-height:1.6;">%s</p>
                 %s
                 <hr style="border:none;border-top:1px solid #eee;
                            margin:24px 0;">
                 <p style="color:#aaa;font-size:12px;">
                   FlowBoard — Organise Work. Collaborate Seamlessly.
                 </p>
               </div>
               """.formatted(title, message, linkHtml);
    }
}
