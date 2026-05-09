package com.flowboard.notification_service.repository;

import com.flowboard.notification_service.entity.Notification;
import com.flowboard.notification_service.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    // Paged overload used by tests
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);

    // Convenience: unread without ordering
    List<Notification> findByRecipientIdAndIsReadFalse(Long recipientId);

    List<Notification> findByRecipientIdAndIsReadTrueOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    List<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(
            Long recipientId, NotificationType type);

    // Convenience: filter by type without ordering
    List<Notification> findByRecipientIdAndType(Long recipientId, NotificationType type);

    List<Notification> findByRelatedIdAndRelatedType( Long relatedId, String relatedType);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, "+
    "n.readAt = CURRENT_TIMESTAMP "+
    "WHERE n.recipientId = :recipientId AND n.isRead = false")
    void markAllAsRead(@Param("recipientId") Long recipientId);

    // Alias used by tests
    default void markAllAsReadForUser(Long recipientId) {
        markAllAsRead(recipientId);
    }

    @Modifying
    @Query("DELETE FROM Notification n "+
    "WHERE n.recipientId = :recipientId AND n.isRead = true")
    void deleteReadByRecipientId(@Param("recipientId") Long recipientId);

    // Derived delete used by tests
    void deleteByRecipientIdAndIsReadTrue(Long recipientId);

    boolean existsByIdAndRecipientId(Long id, Long recipientId);
}
