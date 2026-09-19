package com.transportlogistics.app.compliance.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Per-check structural result. CS01 intentionally defines no aggregate clearance or precedence. */
public record ComplianceEvaluationResult(
        UUID tenantId,
        UUID evaluationId,
        Instant evaluatedAt,
        Optional<CompliancePolicyVersionReference> policyVersion,
        List<ComplianceCheckResult> checkResults) {

    public ComplianceEvaluationResult {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(evaluationId, "evaluationId must not be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        policyVersion = Objects.requireNonNull(policyVersion, "policyVersion must not be null");
        checkResults = List.copyOf(Objects.requireNonNull(checkResults, "checkResults must not be null"));
        if (policyVersion.isPresent()
                && !tenantId.equals(policyVersion.orElseThrow().policyIdentity().tenantId())) {
            throw new IllegalArgumentException("policyVersion must belong to the evaluation tenant");
        }
        if (checkResults.stream().flatMap(result -> result.sourceFacts().stream())
                .anyMatch(fact -> !tenantId.equals(fact.tenantId()))) {
            throw new IllegalArgumentException("all source facts must belong to the evaluation tenant");
        }
    }
}
