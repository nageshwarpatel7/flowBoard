package com.flowboard.notification_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.notification_service.dto.*;
import com.flowboard.notification_service.enums.NotificationType;
import com.flowboard.notification_service.exception.CustomException;
import com.flowboard.notification_service.service.NotificationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("NotificationController – MockMvc Tests")
class NotificationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean  NotificationService notificationService;

    private static final String BASE    = "/api/v1/notifications";
    private static final Long   USER_ID = 1L;

    private NotificationResponse sample() {
        NotificationResponse r = new NotificationResponse();
        r.setId(1L); r.setRecipientId(USER_ID);
        r.setType(NotificationType.ASSIGNMENT);
        r.setMessage("Card assigned to you");
        r.setRead(false); r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    // ── POST /send ────────────────────────────────────────────────────────────
    @Test @DisplayName("POST /send → 201 notification sent")
    void send_201() throws Exception {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1L); req.setType(NotificationType.ASSIGNMENT);
        req.setTitle("Card Assigned");
        req.setMessage("You have been assigned a card");
        when(notificationService.send(any())).thenReturn(sample());

        mvc.perform(post(BASE + "/send").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Card assigned to you"));
    }

    // ── POST /send/bulk ───────────────────────────────────────────────────────
    @Test @DisplayName("POST /send/bulk → 201 bulk notifications")
    void sendBulk_201() throws Exception {
        SendBulkNotificationRequest req = new SendBulkNotificationRequest();
        req.setRecipientIds(List.of(1L, 2L, 3L));
        req.setType(NotificationType.BROADCAST);
        req.setTitle("Announcement");
        req.setMessage("Platform announcement");
        when(notificationService.sendBulk(any())).thenReturn(List.of(sample(), sample(), sample()));

        mvc.perform(post(BASE + "/send/bulk").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // ── GET / ─────────────────────────────────────────────────────────────────
    @Test @DisplayName("GET / → 200 all notifications for user")
    void getMyNotifications_200() throws Exception {
        when(notificationService.getByRecipient(USER_ID)).thenReturn(List.of(sample(), sample()));

        mvc.perform(get(BASE).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test @DisplayName("GET / → 400 missing X-User-Id")
    void getMyNotifications_missingHeader_400() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("GET / → 200 empty list when no notifications")
    void getMyNotifications_empty() throws Exception {
        when(notificationService.getByRecipient(USER_ID)).thenReturn(List.of());
        mvc.perform(get(BASE).header("X-User-Id", USER_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /unread ───────────────────────────────────────────────────────────
    @Test @DisplayName("GET /unread → 200 unread notifications")
    void getUnread_200() throws Exception {
        when(notificationService.getUnreadByRecipient(USER_ID)).thenReturn(List.of(sample()));

        mvc.perform(get(BASE + "/unread").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(false));
    }

    // ── GET /unread/count ─────────────────────────────────────────────────────
    @Test @DisplayName("GET /unread/count → 200 badge count")
    void getUnreadCount_200() throws Exception {
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(5L);

        mvc.perform(get(BASE + "/unread/count").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test @DisplayName("GET /unread/count → 200 zero count")
    void getUnreadCount_zero() throws Exception {
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(0L);
        mvc.perform(get(BASE + "/unread/count").header("X-User-Id", USER_ID))
                .andExpect(status().isOk()).andExpect(content().string("0"));
    }

    // ── GET /type/{type} ──────────────────────────────────────────────────────
    @Test @DisplayName("GET /type/{type} → 200 filtered by type")
    void getByType_200() throws Exception {
        when(notificationService.getByRecipientAndType(USER_ID, NotificationType.ASSIGNMENT))
                .thenReturn(List.of(sample()));

        mvc.perform(get(BASE + "/type/ASSIGNMENT").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ASSIGNMENT"));
    }

    // ── GET /all (admin) ──────────────────────────────────────────────────────
    @Test @DisplayName("GET /all → 200 for admin role")
    void getAll_admin_200() throws Exception {
        when(notificationService.getAll()).thenReturn(List.of(sample()));

        mvc.perform(get(BASE + "/all").header("X-User-Role", "PLATFORM_ADMIN"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("GET /all → 403 for non-admin")
    void getAll_nonAdmin_403() throws Exception {
        mvc.perform(get(BASE + "/all").header("X-User-Role", "MEMBER"))
                .andExpect(status().isForbidden());
    }

    // ── PUT /{id}/read ────────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id}/read → 200 marked as read")
    void markAsRead_200() throws Exception {
        NotificationResponse read = sample(); read.setRead(true);
        when(notificationService.markAsRead(1L, USER_ID)).thenReturn(read);

        mvc.perform(put(BASE + "/1/read").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test @DisplayName("PUT /{id}/read → 404 notification not found")
    void markAsRead_404() throws Exception {
        when(notificationService.markAsRead(eq(999L), anyLong()))
                .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

        mvc.perform(put(BASE + "/999/read").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    // ── PUT /read/all ─────────────────────────────────────────────────────────
    @Test @DisplayName("PUT /read/all → 200 all marked as read")
    void markAllAsRead_200() throws Exception {
        doNothing().when(notificationService).markAllAsRead(USER_ID);

        mvc.perform(put(BASE + "/read/all").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("All notifications marked as read"));
    }

    // ── DELETE /{id} ──────────────────────────────────────────────────────────
    @Test @DisplayName("DELETE /{id} → 200 deleted")
    void delete_200() throws Exception {
        doNothing().when(notificationService).deleteNotification(1L, USER_ID);

        mvc.perform(delete(BASE + "/1").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification deleted"));
    }

    @Test @DisplayName("DELETE /{id} → 400 missing header")
    void delete_missingHeader_400() throws Exception {
        mvc.perform(delete(BASE + "/1").with(csrf())).andExpect(status().isBadRequest());
    }

    // ── DELETE /read/all ──────────────────────────────────────────────────────
    @Test @DisplayName("DELETE /read/all → 200 read notifications deleted")
    void deleteRead_200() throws Exception {
        doNothing().when(notificationService).deleteReadNotifications(USER_ID);

        mvc.perform(delete(BASE + "/read/all").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("Read notifications deleted"));
    }

    // ── POST /notify/* endpoints ──────────────────────────────────────────────
    @Test @DisplayName("POST /notify/assignment → 200")
    void notifyAssignment_200() throws Exception {
        doNothing().when(notificationService).notifyAssignment(anyLong(), anyLong(), anyLong(), anyString(), any());

        mvc.perform(post(BASE + "/notify/assignment").with(csrf())
                        .param("recipientId", "2").param("actorId", "1")
                        .param("cardId", "10").param("cardTitle", "Login Page"))
                .andExpect(status().isOk())
                .andExpect(content().string("Assignment notification sent"));
    }

    @Test @DisplayName("POST /notify/due-date → 200")
    void notifyDueDate_200() throws Exception {
        doNothing().when(notificationService).notifyDueDateApproaching(anyLong(), anyLong(), anyString(), anyString());

        mvc.perform(post(BASE + "/notify/due-date").with(csrf())
                        .param("recipientId", "1").param("cardId", "10")
                        .param("cardTitle", "Login Page").param("timeLeft", "24 hours"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("POST /notify/overdue → 200")
    void notifyOverdue_200() throws Exception {
        doNothing().when(notificationService).notifyOverdue(anyLong(), anyLong(), anyString(), anyString(), any());

        mvc.perform(post(BASE + "/notify/overdue").with(csrf())
                        .param("recipientId", "1").param("cardId", "10")
                        .param("cardTitle", "Login Page").param("dueDate", "2024-01-01"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("POST /notify/done → 200")
    void notifyDone_200() throws Exception {
        doNothing().when(notificationService).notifyCardMovedToDone(anyLong(), anyLong(), anyLong(), anyString());

        mvc.perform(post(BASE + "/notify/done").with(csrf())
                        .param("recipientId", "1").param("actorId", "2")
                        .param("cardId", "10").param("cardTitle", "Login Page"))
                .andExpect(status().isOk());
    }
}