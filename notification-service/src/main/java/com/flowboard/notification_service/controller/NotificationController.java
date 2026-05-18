package com.flowboard.notification_service.controller;

import com.flowboard.notification_service.dto.NotificationResponse;
import com.flowboard.notification_service.dto.NotifyDueDateRequest;
import com.flowboard.notification_service.dto.NotifyOverdueRequest;
import com.flowboard.notification_service.dto.SendBulkNotificationRequest;
import com.flowboard.notification_service.dto.SendNotificationRequest;
import com.flowboard.notification_service.enums.NotificationType;
import com.flowboard.notification_service.exception.CustomException;
import com.flowboard.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private Long resolveUserId(Long userIdHeader) {
        if (userIdHeader != null) return userIdHeader;
        throw new CustomException("X-User-Id header is required", HttpStatus.BAD_REQUEST);
    }

    // ── Internal send endpoints (called by Feign clients) ─────────────────────

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> send(
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.send(request));
    }

    @PostMapping("/send/bulk")
    public ResponseEntity<List<NotificationResponse>> sendBulk(
            @Valid @RequestBody SendBulkNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendBulk(request));
    }

    @PostMapping("/notify/assignment")
    public ResponseEntity<String> notifyAssignment(
            @RequestParam Long recipientId,
            @RequestParam Long actorId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle,
            @RequestParam(required = false) String recipientEmail) {
        notificationService.notifyAssignment(
                recipientId, actorId, cardId, cardTitle, recipientEmail);
        return ResponseEntity.ok("Assignment notification sent");
    }

    /**
     * FIX: was accepting {@code Map<String, Object>} and calling {@code .toString()}
     * on every field — fragile, no Jackson validation, breaks silently on field rename.
     *
     * Now accepts the typed {@link NotifyDueDateRequest} DTO that matches what
     * card-service's NotificationClient already sends.
     *
     * NOTE: add NotifyDueDateRequest to notification-service's dto package (see below).
     */
    @PostMapping("/notify/due-date-body")
    public ResponseEntity<String> notifyDueDateBody(
            @RequestBody NotifyDueDateRequest req) {
        notificationService.notifyDueDateApproaching(
                req.getRecipientId(),
                req.getCardId(),
                req.getCardTitle(),
                req.getTimeLeft());
        return ResponseEntity.ok("Due date notification sent");
    }

    /**
     * FIX: same as above — replaced raw Map with typed {@link NotifyOverdueRequest}.
     */
    @PostMapping("/notify/overdue-body")
    public ResponseEntity<String> notifyOverdueBody(
            @RequestBody NotifyOverdueRequest req) {
        notificationService.notifyOverdue(
                req.getRecipientId(),
                req.getCardId(),
                req.getCardTitle(),
                req.getDueDate(),
                req.getRecipientEmail());
        return ResponseEntity.ok("Overdue notification sent");
    }

    @PostMapping("/notify/mention")
    public ResponseEntity<String> notifyMention(
            @RequestParam Long recipientId,
            @RequestParam Long actorId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle) {
        notificationService.notifyMention(recipientId, actorId, cardId, cardTitle);
        return ResponseEntity.ok("Mention notification sent");
    }

    @PostMapping("/notify/due-date")
    public ResponseEntity<String> notifyDueDate(
            @RequestParam Long recipientId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle,
            @RequestParam String timeLeft) {
        notificationService.notifyDueDateApproaching(
                recipientId, cardId, cardTitle, timeLeft);
        return ResponseEntity.ok("Due date notification sent");
    }

    @PostMapping("/notify/done")
    public ResponseEntity<String> notifyDone(
            @RequestParam Long recipientId,
            @RequestParam Long actorId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle) {
        notificationService.notifyCardMovedToDone(
                recipientId, actorId, cardId, cardTitle);
        return ResponseEntity.ok("Done notification sent");
    }

    @PostMapping("/notify/reply")
    public ResponseEntity<String> notifyReply(
            @RequestParam Long recipientId,
            @RequestParam Long actorId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle) {
        notificationService.notifyCommentReply(
                recipientId, actorId, cardId, cardTitle);
        return ResponseEntity.ok("Reply notification sent");
    }

    @PostMapping("/notify/overdue")
    public ResponseEntity<String> notifyOverdue(
            @RequestParam Long recipientId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle,
            @RequestParam String dueDate,
            @RequestParam(required = false) String recipientEmail) {
        notificationService.notifyOverdue(
                recipientId, cardId, cardTitle, dueDate, recipientEmail);
        return ResponseEntity.ok("Overdue notification sent");
    }

    // ── Retrieval ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(
                notificationService.getByRecipient(resolveUserId(userId)));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(
                notificationService.getUnreadByRecipient(resolveUserId(userId)));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(
                notificationService.getUnreadCount(resolveUserId(userId)));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<NotificationResponse>> getByType(
            @PathVariable NotificationType type,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(
                notificationService.getByRecipientAndType(resolveUserId(userId), type));
    }

    @GetMapping("/all")
    public ResponseEntity<List<NotificationResponse>> getAll(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"PLATFORM_ADMIN".equals(role)) {
            throw new CustomException(
                    "Admin access required", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(notificationService.getAll());
    }

    // ── Read state ────────────────────────────────────────────────────────────

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(
                notificationService.markAsRead(id, resolveUserId(userId)));
    }

    @PutMapping("/read/all")
    public ResponseEntity<String> markAllAsRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        notificationService.markAllAsRead(resolveUserId(userId));
        return ResponseEntity.ok("All notifications marked as read");
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        notificationService.deleteNotification(id, resolveUserId(userId));
        return ResponseEntity.ok("Notification deleted");
    }

    @DeleteMapping("/read/all")
    public ResponseEntity<String> deleteRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        notificationService.deleteReadNotifications(resolveUserId(userId));
        return ResponseEntity.ok("Read notifications deleted");
    }
}
