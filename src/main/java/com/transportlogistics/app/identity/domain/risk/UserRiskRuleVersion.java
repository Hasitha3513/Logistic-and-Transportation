package com.transportlogistics.app.identity.domain.risk;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable Tenant-qualified rule-version identity with half-open effective time. */
public record UserRiskRuleVersion(
        UUID tenantId,
        UUID ruleVersionId,
        String ruleKey,
        int version,
        Instant effectiveFrom,
        Instant effectiveUntil) {

    public static final String FIRST_WAVE_RULE_KEY = "IDENTITY_PERMISSION_CEILING_REPEATED_DENIAL";

    public UserRiskRuleVersion {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(ruleVersionId, "ruleVersionId must not be null");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        if (!FIRST_WAVE_RULE_KEY.equals(ruleKey)) {
            throw new IllegalArgumentException("Only the approved first-wave rule key is permitted");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        if (effectiveUntil != null && !effectiveUntil.isAfter(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveUntil must be after effectiveFrom");
        }
    }

    public boolean appliesAt(Instant sourceTime) {
        Objects.requireNonNull(sourceTime, "sourceTime must not be null");
        return !sourceTime.isBefore(effectiveFrom)
                && (effectiveUntil == null || sourceTime.isBefore(effectiveUntil));
    }
}
