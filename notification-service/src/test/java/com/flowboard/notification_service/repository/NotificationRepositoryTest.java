package com.flowboard.notification_service.repository;

import com.flowboard.notification_service.entity.Notification;
import com.flowboard.notification_service.enums.NotificationType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("NotificationRepository – @DataJpaTest")
class NotificationRepositoryTest {

    @Autowired NotificationRepository repo;

    private Notification n1Unread, n2Read, n3DiffUser, n4OverdueType;

    @BeforeEach
    void setUp() {
        n1Unread = repo.save(Notification.builder()
                .recipientId(1L).type(NotificationType.ASSIGNMENT)
                .title("New Assignment")
                .message("Card assigned to you").isRead(false)
                .createdAt(LocalDateTime.now()).build());

        n2Read = repo.save(Notification.builder()
                .recipientId(1L).type(NotificationType.OVERDUE)
                .title("Task Overdue")
                .message("Card is overdue").isRead(true)
                .createdAt(LocalDateTime.now().minusHours(1)).build());

        n3DiffUser = repo.save(Notification.builder()
                .recipientId(2L).type(NotificationType.INVITE)
                .title("Invitation")
                .message("Workspace invite").isRead(false)
                .createdAt(LocalDateTime.now()).build());

        n4OverdueType = repo.save(Notification.builder()
                .recipientId(1L).type(NotificationType.OVERDUE)
                .title("Task Overdue")
                .message("Another overdue card").isRead(false)
                .createdAt(LocalDateTime.now().minusMinutes(30)).build());
    }

    // ── findByRecipientIdOrderByCreatedAtDesc ───────────────────────────────
    @Test @DisplayName("findByRecipientId – returns all for user, newest first")
    void findByRecipient_all() {
        List<Notification> list = repo.findByRecipientIdOrderByCreatedAtDesc(1L);
        assertThat(list).hasSize(3);
        assertThat(list.get(0).getCreatedAt())
                .isAfterOrEqualTo(list.get(1).getCreatedAt());
    }

    @Test @DisplayName("findByRecipientId – excludes other users")
    void findByRecipient_isolated() {
        List<Notification> list = repo.findByRecipientIdOrderByCreatedAtDesc(1L);
        assertThat(list).noneMatch(n -> n.getRecipientId().equals(2L));
    }

    @Test @DisplayName("findByRecipientId – empty for user with no notifications")
    void findByRecipient_empty() {
        assertThat(repo.findByRecipientIdOrderByCreatedAtDesc(99L)).isEmpty();
    }

    // ── findByRecipientIdAndIsReadFalse ─────────────────────────────────────
    @Test @DisplayName("findUnread – returns only unread for user")
    void findUnread() {
        List<Notification> unread = repo.findByRecipientIdAndIsReadFalse(1L);
        assertThat(unread).hasSize(2).allMatch(n -> !n.isRead());
    }

    @Test @DisplayName("findUnread – empty when all read")
    void findUnread_allRead() {
        n1Unread.setRead(true); n4OverdueType.setRead(true);
        repo.save(n1Unread); repo.save(n4OverdueType);
        assertThat(repo.findByRecipientIdAndIsReadFalse(1L)).isEmpty();
    }

    // ── countByRecipientIdAndIsReadFalse ────────────────────────────────────
    @Test @DisplayName("countUnread – returns badge count")
    void countUnread() {
        assertThat(repo.countByRecipientIdAndIsReadFalse(1L)).isEqualTo(2);
    }

    @Test @DisplayName("countUnread – 0 for user with all read")
    void countUnread_zero() {
        assertThat(repo.countByRecipientIdAndIsReadFalse(3L)).isEqualTo(0);
    }

    // ── findByRecipientIdAndType ────────────────────────────────────────────
    @Test @DisplayName("findByRecipientIdAndType – filters by OVERDUE type")
    void findByType_overdue() {
        List<Notification> overdue = repo.findByRecipientIdAndType(1L, NotificationType.OVERDUE);
        assertThat(overdue).hasSize(2)
                .allMatch(n -> n.getType() == NotificationType.OVERDUE);
    }

    @Test @DisplayName("findByRecipientIdAndType – filters by ASSIGNMENT type")
    void findByType_assignment() {
        List<Notification> assignments = repo.findByRecipientIdAndType(1L, NotificationType.ASSIGNMENT);
        assertThat(assignments).hasSize(1);
    }

    // ── paged findByRecipientIdOrderByCreatedAtDesc ─────────────────────────
    @Test @DisplayName("paged – respects page size")
    void paged_size1() {
        Page<Notification> page = repo.findByRecipientIdOrderByCreatedAtDesc(
                1L, PageRequest.of(0, 1));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test @DisplayName("paged – page 1 returns second item")
    void paged_page1() {
        Page<Notification> page = repo.findByRecipientIdOrderByCreatedAtDesc(
                1L, PageRequest.of(1, 1));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getNumber()).isEqualTo(1);
    }

    // ── markAllAsReadForUser ─────────────────────────────────────────────────
    @Test @DisplayName("markAllAsRead – sets all unread to read")
    void markAllAsRead() {
        repo.markAllAsReadForUser(1L);
        repo.flush();
        assertThat(repo.countByRecipientIdAndIsReadFalse(1L)).isEqualTo(0);
    }

    // ── deleteById ──────────────────────────────────────────────────────────
    @Test @DisplayName("deleteById – removes notification")
    void deleteById() {
        Long id = n1Unread.getId();
        repo.deleteById(id);
        assertThat(repo.findById(id)).isEmpty();
    }

    // ── deleteReadNotifications ──────────────────────────────────────────────
    @Test @DisplayName("deleteReadByRecipient – removes only read notifications")
    void deleteRead() {
        repo.deleteByRecipientIdAndIsReadTrue(1L);
        repo.flush();
        List<Notification> remaining = repo.findByRecipientIdOrderByCreatedAtDesc(1L);
        assertThat(remaining).allMatch(n -> !n.isRead());
    }

    // ── CRUD ────────────────────────────────────────────────────────────────
    @Test @DisplayName("save – assigns ID on persist")
    void save_assignsId() {
        Notification n = repo.save(Notification.builder()
                .recipientId(5L).type(NotificationType.BROADCAST)
                .title("Announcement")
                .message("Platform announcement").isRead(false)
                .createdAt(LocalDateTime.now()).build());
        assertThat(n.getId()).isNotNull();
    }

    @Test @DisplayName("save – updates isRead field")
    void save_updatesRead() {
        n1Unread.setRead(true);
        repo.save(n1Unread);
        assertThat(repo.findById(n1Unread.getId()).orElseThrow().isRead()).isTrue();
    }

    @Test @DisplayName("findAll – returns all seeded notifications")
    void findAll_all() {
        assertThat(repo.findAll()).hasSizeGreaterThanOrEqualTo(4);
    }
}