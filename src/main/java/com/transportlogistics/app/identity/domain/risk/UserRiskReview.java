package com.transportlogistics.app.identity.domain.risk;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable human review without free-form allegation content. */
public record UserRiskReview(
        UUID reviewId,
        UUID findingId,
        UUID tenantId,
        UUID subjectUserId,
        UUID reviewerUserId,
        UserRiskFirstWaveContract.ReviewDisposition disposition,
        String reasonCode,
        Instant decidedAt) {

    public UserRiskReview {
        Objects.requireNonNull(reviewId, "reviewId must not be null");
        Objects.requireNonNull(findingId, "findingId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(subjectUserId, "subjectUserId must not be null");
        Objects.requireNonNull(reviewerUserId, "reviewerUserId must not be null");
        Objects.requireNonNull(disposition, "disposition must not be null");
        Objects.requireNonNull(decidedAt, "decidedAt must not be null");
        if (subjectUserId.equals(reviewerUserId)) {
            throw new IllegalArgumentException("Reviewer must be distinct from the subject");
        }
        reasonCode = safeReasonCode(reasonCode);
    }

    private static String safeReasonCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reasonCode must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > 80 || !normalized.matches("[A-Z0-9][A-Z0-9_]*")) {
            throw new IllegalArgumentException("reasonCode must be an opaque uppercase code");
        }
        return normalized;
    }
}
