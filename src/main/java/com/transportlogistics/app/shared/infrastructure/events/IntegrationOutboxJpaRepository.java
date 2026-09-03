package com.transportlogistics.app.shared.infrastructure.events;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface IntegrationOutboxJpaRepository extends JpaRepository<IntegrationOutboxEventEntity, UUID> {
    boolean existsByEventIdAndConsumerName(UUID eventId, String consumerName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select event from IntegrationOutboxEventEntity event
        where event.attemptCount < :maxAttempts and ((event.status in :readyStatuses and event.nextAttemptAt <= :now)
            or (event.status = :processingStatus and event.lockedUntil <= :now))
        order by event.occurredAt, event.createdAt
        """)
    List<IntegrationOutboxEventEntity> findClaimable(
        @Param("now") OffsetDateTime now,
        @Param("readyStatuses") Collection<IntegrationOutboxStatus> readyStatuses,
        @Param("processingStatus") IntegrationOutboxStatus processingStatus,
        @Param("maxAttempts") int maxAttempts,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select event from IntegrationOutboxEventEntity event
        where event.status = :processingStatus and event.lockedUntil <= :now
            and event.attemptCount >= :maxAttempts
        order by event.updatedAt
        """)
    List<IntegrationOutboxEventEntity> findExpiredExhaustedClaims(
        @Param("now") OffsetDateTime now,
        @Param("processingStatus") IntegrationOutboxStatus processingStatus,
        @Param("maxAttempts") int maxAttempts,
        Pageable pageable
    );
}
