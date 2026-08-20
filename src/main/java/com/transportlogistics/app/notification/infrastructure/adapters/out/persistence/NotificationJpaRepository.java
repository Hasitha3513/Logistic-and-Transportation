package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {
    boolean existsByEventIdAndRuleIdAndRecipient(UUID eventId, UUID ruleId, String recipient);

    @Query("SELECT n FROM NotificationEntity n WHERE n.recipient IN :recipients ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByRecipients(@Param("recipients") Collection<String> recipients, Pageable pageable);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.recipient IN :recipients AND n.status <> 'READ'")
    long countUnreadByRecipients(@Param("recipients") Collection<String> recipients);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.status = 'READ', n.readAt = :now WHERE n.recipient IN :recipients AND n.status <> 'READ'")
    int markAllAsReadForRecipients(@Param("recipients") Collection<String> recipients, @Param("now") OffsetDateTime now);
}
