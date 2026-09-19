package com.transportlogistics.app.identity.domain.risk;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** One internal appeal request and its optional distinct-reviewer decision. */
public record UserRiskAppeal(
        UUID appealId,
        UUID findingId,
        UUID tenantId,
        UUID subjectUserId,
        UUID requestedByUserId,
        UUID originalReviewerUserId,
        Instant originalDispositionAt,
        Instant requestedAt,
        Optional<UserRiskReview> decision) {

    public UserRiskAppeal {
        Objects.requireNonNull(appealId, "appealId must not be null");
        Objects.requireNonNull(findingId, "findingId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(subjectUserId, "subjectUserId must not be null");
        Objects.requireNonNull(requestedByUserId, "requestedByUserId must not be null");
        Objects.requireNonNull(originalReviewerUserId, "originalReviewerUserId must not be null");
        Objects.requireNonNull(originalDispositionAt, "originalDispositionAt must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        decision = decision == null ? Optional.empty() : decision;
        Duration elapsed = Duration.between(originalDispositionAt, requestedAt);
        if (elapsed.isNegative() || elapsed.compareTo(UserRiskFirstWaveContract.APPEAL_WINDOW) > 0) {
            throw new IllegalArgumentException("Appeal must be requested within 30 days of disposition");
        }
        if (requestedByUserId.equals(subjectUserId)) {
            throw new IllegalArgumentException("Appeal must be submitted by an internal operator");
        }
        decision.ifPresent(review -> validateDecision(review, findingId, tenantId, subjectUserId,
                originalReviewerUserId, requestedAt));
    }

    private static void validateDecision(
            UserRiskReview review,
            UUID findingId,
            UUID tenantId,
            UUID subjectUserId,
            UUID originalReviewerUserId,
            Instant requestedAt) {
        if (!review.findingId().equals(findingId)
                || !review.tenantId().equals(tenantId)
                || !review.subjectUserId().equals(subjectUserId)) {
            throw new IllegalArgumentException("Appeal decision must match the finding and Tenant");
        }
        if (review.reviewerUserId().equals(originalReviewerUserId)) {
            throw new IllegalArgumentException("Appeal decision requires a different reviewer");
        }
        if (review.decidedAt().isBefore(requestedAt)) {
            throw new IllegalArgumentException("Appeal decision must not precede the request");
        }
    }
}
