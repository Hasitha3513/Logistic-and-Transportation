package com.transportlogistics.app.compliance.domain;

import java.util.Objects;
import java.util.UUID;

public record CompliancePolicyIdentity(
        UUID tenantId,
        String policyCode,
        JurisdictionScopeReference jurisdictionScope) {

    public CompliancePolicyIdentity {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(jurisdictionScope, "jurisdictionScope must not be null");
        if (policyCode == null || policyCode.isBlank()) {
            throw new IllegalArgumentException("policyCode must not be blank");
        }
        policyCode = policyCode.trim();
        if (policyCode.length() > 80 || !policyCode.matches("[A-Za-z0-9][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException("policyCode must be an opaque code of at most 80 characters");
        }
    }
}
