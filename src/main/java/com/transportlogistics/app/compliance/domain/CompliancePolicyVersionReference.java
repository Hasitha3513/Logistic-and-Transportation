package com.transportlogistics.app.compliance.domain;

import java.util.Objects;
import java.util.UUID;

public record CompliancePolicyVersionReference(
        CompliancePolicyIdentity policyIdentity,
        UUID policyVersionId,
        long version,
        EffectiveTimeWindow effectiveTime) {

    public CompliancePolicyVersionReference {
        Objects.requireNonNull(policyIdentity, "policyIdentity must not be null");
        Objects.requireNonNull(policyVersionId, "policyVersionId must not be null");
        Objects.requireNonNull(effectiveTime, "effectiveTime must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
    }
}
