package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.NotificationRuleExecutionOutcome;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRuleExecutionJpaRepository extends JpaRepository<NotificationRuleExecutionEntity, UUID> {
    boolean existsByExecutionKey(String executionKey);

    Optional<NotificationRuleExecutionEntity> findFirstBySuppressionKeyAndOutcomeAndCompletedAtAfterOrderByCompletedAtDesc(
        String suppressionKey, NotificationRuleExecutionOutcome outcome, OffsetDateTime after);

    @Query("select e from NotificationRuleExecutionEntity e where (:ruleId is null or e.ruleId = :ruleId) " +
        "and (:eventId is null or e.eventId = :eventId) order by e.createdAt desc")
    List<NotificationRuleExecutionEntity> findRecent(@Param("ruleId") UUID ruleId,
                                                     @Param("eventId") UUID eventId,
                                                     Pageable pageable);
}
