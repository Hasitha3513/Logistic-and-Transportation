package com.transportlogistics.app.compliance.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Inactive structural request; it carries no policy authority or enforcement instruction. */
public record ComplianceEvaluationRequest(
        UUID tenantId,
        UUID evaluationId,
        String operationType,
        UUID operationId,
        Instant evaluationTime,
        JurisdictionScopeReference jurisdictionScope,
        Set<ComplianceCheckType> requestedChecks) {

    public ComplianceEvaluationRequest {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(evaluationId, "evaluationId must not be null");
        operationType = requiredCode(operationType);
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(evaluationTime, "evaluationTime must not be null");
        Objects.requireNonNull(jurisdictionScope, "jurisdictionScope must not be null");
        requestedChecks = Set.copyOf(Objects.requireNonNull(requestedChecks, "requestedChecks must not be null"));
        if (requestedChecks.isEmpty()) {
            throw new IllegalArgumentException("requestedChecks must not be empty");
        }
    }

    private static String requiredCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("operationType must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > 80 || !normalized.matches("[A-Za-z0-9][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException("operationType must be a code of at most 80 characters");
        }
        return normalized;
    }
}
