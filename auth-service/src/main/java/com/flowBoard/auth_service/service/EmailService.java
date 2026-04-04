package com.flowBoard.auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationOtp(String toEmail, String otp) {
        log.info("Sending verification OTP to: {}", toEmail);
        String subject = "FlowBoard — Verify your email";
        String body = buildOtpEmail(
                "Email Verification",
                "Thank you for signing up on FlowBoard. Use the OTP below to verify your email address.",
                otp,
                "This OTP expires in 10 minutes. Do not share it with anyone."
        );
        send(toEmail, subject, body);
    }

    @Async
    public void sendForgotPasswordOtp(String toEmail, String otp) {
        log.info("Sending forgot password OTP to: {}", toEmail);
        String subject = "FlowBoard — Password Reset OTP";
        String body = buildOtpEmail(
                "Password Reset",
                "We received a request to reset your FlowBoard password. Use the OTP below to proceed.",
                otp,
                "This OTP expires in 10 minutes. If you did not request a password reset, ignore this email."
        );
        send(toEmail, subject, body);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            log.info("Connecting to SMTP — from: {} to: {}", fromEmail, to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            // Now logs the full stack trace so you can see exactly what went wrong
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String buildOtpEmail(String title, String intro, String otp, String footer) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto; padding: 32px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #1a1a2e;">FlowBoard — %s</h2>
                    <p style="color: #444; font-size: 15px;">%s</p>
                    <div style="margin: 24px 0; text-align: center;">
                        <span style="display: inline-block; font-size: 36px; font-weight: bold; letter-spacing: 10px; color: #4f46e5; background: #f0f0ff; padding: 16px 32px; border-radius: 8px;">%s</span>
                    </div>
                    <p style="color: #888; font-size: 13px;">%s</p>
                </div>
                """.formatted(title, intro, otp, footer);
    }
}