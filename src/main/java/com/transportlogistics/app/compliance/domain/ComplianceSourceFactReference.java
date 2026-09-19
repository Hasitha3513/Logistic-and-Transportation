package com.transportlogistics.app.compliance.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Minimized logical reference to an immutable or versioned owner fact. */
public record ComplianceSourceFactReference(
        UUID tenantId,
        String sourceModule,
        String factType,
        UUID factId,
        String factVersion,
        Instant effectiveAt) {

    public ComplianceSourceFactReference {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        sourceModule = requiredCode(sourceModule, "sourceModule");
        factType = requiredCode(factType, "factType");
        Objects.requireNonNull(factId, "factId must not be null");
        factVersion = requiredCode(factVersion, "factVersion");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
    }

    private static String requiredCode(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > 128 || !normalized.matches("[A-Za-z0-9][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException(name + " must be a code of at most 128 characters");
        }
        return normalized;
    }
}
