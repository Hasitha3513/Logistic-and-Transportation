package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {
    boolean existsByEventIdAndRuleIdAndRecipient(UUID eventId, UUID ruleId, String recipient);

    @Query("SELECT n FROM NotificationEntity n WHERE n.recipient IN :recipients ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByRecipients(@Param("recipients") Collection<String> recipients, Pageable pageable);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.recipient IN :recipients AND n.channel = 'IN_APP' AND n.status <> 'READ'")
    long countUnreadByRecipients(@Param("recipients") Collection<String> recipients);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.status = 'READ', n.readAt = :now WHERE n.recipient IN :recipients AND n.channel = 'IN_APP' AND n.status <> 'READ'")
    int markAllAsReadForRecipients(@Param("recipients") Collection<String> recipients, @Param("now") OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificationEntity n WHERE n.id = :id")
    java.util.Optional<NotificationEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT n FROM NotificationEntity n WHERE n.channel = 'EMAIL' AND n.status = 'PENDING' " +
        "AND (n.nextDeliveryAt IS NULL OR n.nextDeliveryAt <= :now) ORDER BY n.createdAt")
    List<NotificationEntity> findDuePendingEmails(@Param("now") OffsetDateTime now, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.channel = 'EMAIL' AND n.status = 'FAILED' " +
        "AND n.escalationLevel = 0 ORDER BY n.createdAt")
    List<NotificationEntity> findFailedEmails(Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE (:status IS NULL OR n.status = :status) " +
        "AND (:eventType IS NULL OR n.eventType = :eventType) " +
        "AND (:fromTime IS NULL OR n.createdAt >= :fromTime) AND (:toTime IS NULL OR n.createdAt <= :toTime) " +
        "ORDER BY n.createdAt DESC")
    List<NotificationEntity> findDeliveries(@Param("status") NotificationStatus status,
        @Param("eventType") String eventType, @Param("fromTime") OffsetDateTime from,
        @Param("toTime") OffsetDateTime to, Pageable pageable);

    boolean existsByParentNotificationIdAndRecipient(UUID parentNotificationId, String recipient);

    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.parentNotificationId IS NOT NULL")
    void deleteEscalationChildren();
}
