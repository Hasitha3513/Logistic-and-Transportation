package com.transportlogistics.app.compliance.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ComplianceCheckResult(
        ComplianceCheckType checkType,
        ComplianceEvaluationStatus status,
        ComplianceEvidenceState evidenceState,
        Optional<ComplianceDecisionEffect> effect,
        List<ComplianceSourceFactReference> sourceFacts,
        List<String> reasonCodes) {

    public ComplianceCheckResult {
        Objects.requireNonNull(checkType, "checkType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(evidenceState, "evidenceState must not be null");
        effect = Objects.requireNonNull(effect, "effect must not be null");
        sourceFacts = List.copyOf(Objects.requireNonNull(sourceFacts, "sourceFacts must not be null"));
        reasonCodes = List.copyOf(Objects.requireNonNull(reasonCodes, "reasonCodes must not be null"));
        if (status == ComplianceEvaluationStatus.EVALUATED && effect.isEmpty()) {
            throw new IllegalArgumentException("evaluated result requires an effect");
        }
        if (status != ComplianceEvaluationStatus.EVALUATED && effect.isPresent()) {
            throw new IllegalArgumentException("unavailable or unevaluated result must not carry an effect");
        }
        if (sourceFacts.stream().map(ComplianceSourceFactReference::tenantId).distinct().count() > 1) {
            throw new IllegalArgumentException("source facts must belong to one tenant");
        }
        reasonCodes = reasonCodes.stream().map(ComplianceCheckResult::validateReasonCode).toList();
    }

    private static String validateReasonCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reason code must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > 80 || !normalized.matches("[A-Za-z0-9][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException("reason code must be a code of at most 80 characters");
        }
        return normalized;
    }
}
