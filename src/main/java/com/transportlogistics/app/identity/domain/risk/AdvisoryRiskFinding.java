package com.transportlogistics.app.identity.domain.risk;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Immutable first-wave advisory finding; it grants no enforcement authority. */
public record AdvisoryRiskFinding(
        UUID findingId,
        UUID tenantId,
        UUID subjectUserId,
        UUID ruleVersionId,
        UserRiskFirstWaveContract.ActionFamily actionFamily,
        Instant windowStart,
        Instant windowEnd,
        Instant openedAt,
        Set<UUID> evidenceEventIds,
        UserRiskFirstWaveContract.ReviewPriority priority,
        UserRiskFirstWaveContract.Effect effect,
        UserRiskFirstWaveContract.FindingState state) {

    public AdvisoryRiskFinding {
        Objects.requireNonNull(findingId, "findingId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(subjectUserId, "subjectUserId must not be null");
        Objects.requireNonNull(ruleVersionId, "ruleVersionId must not be null");
        Objects.requireNonNull(actionFamily, "actionFamily must not be null");
        Objects.requireNonNull(windowStart, "windowStart must not be null");
        Objects.requireNonNull(windowEnd, "windowEnd must not be null");
        Objects.requireNonNull(openedAt, "openedAt must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(effect, "effect must not be null");
        Objects.requireNonNull(state, "state must not be null");
        evidenceEventIds = Set.copyOf(Objects.requireNonNull(evidenceEventIds, "evidenceEventIds must not be null"));
        if (evidenceEventIds.size() < UserRiskFirstWaveContract.DISTINCT_FACT_THRESHOLD) {
            throw new IllegalArgumentException("A finding requires three distinct evidence facts");
        }
        if (windowEnd.isBefore(windowStart)
                || Duration.between(windowStart, windowEnd).compareTo(UserRiskFirstWaveContract.EVALUATION_WINDOW) > 0) {
            throw new IllegalArgumentException("Evidence must fit the approved 15-minute window");
        }
        if (openedAt.isBefore(windowEnd)) {
            throw new IllegalArgumentException("openedAt must not precede the last contributing fact");
        }
        if (priority != UserRiskFirstWaveContract.ReviewPriority.MEDIUM
                || effect != UserRiskFirstWaveContract.Effect.ADVISORY_REVIEW_ONLY) {
            throw new IllegalArgumentException("The first wave is MEDIUM and advisory-only");
        }
    }
}
