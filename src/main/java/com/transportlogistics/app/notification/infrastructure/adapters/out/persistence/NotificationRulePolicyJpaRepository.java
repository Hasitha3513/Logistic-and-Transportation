package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRulePolicyJpaRepository extends JpaRepository<NotificationRulePolicyEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from NotificationRulePolicyEntity p where p.ruleId = :ruleId")
    Optional<NotificationRulePolicyEntity> findByRuleIdForUpdate(@Param("ruleId") UUID ruleId);
}
