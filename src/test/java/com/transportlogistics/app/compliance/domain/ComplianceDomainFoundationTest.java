package com.transportlogistics.app.compliance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.compliance.ports.outbound.DriverEligibilityFactsQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComplianceDomainFoundationTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void exposesExactlySevenStructuralCheckIdentifiers() {
        assertThat(ComplianceCheckType.values()).containsExactly(
                ComplianceCheckType.VEHICLE_DOCUMENT_ELIGIBILITY,
                ComplianceCheckType.DRIVER_ELIGIBILITY,
                ComplianceCheckType.CARGO_DOCUMENT_ELIGIBILITY,
                ComplianceCheckType.HAZMAT_ELIGIBILITY,
                ComplianceCheckType.BILLING_TAX_FACT_ELIGIBILITY,
                ComplianceCheckType.REGIONAL_OPERATION_ELIGIBILITY,
                ComplianceCheckType.RETENTION_DISPOSITION_ELIGIBILITY);
    }

    @Test
    void validatesTenantQualifiedPolicyVersionAndHalfOpenEffectiveTime() {
        UUID tenantId = UUID.randomUUID();
        var window = new EffectiveTimeWindow(START, START.plusSeconds(60));
        var policy = new CompliancePolicyIdentity(
                tenantId, "POLICY_A", new JurisdictionScopeReference("UNSELECTED", "PHASE1"));
        var version = new CompliancePolicyVersionReference(policy, UUID.randomUUID(), 1, window);

        assertThat(version.policyIdentity().tenantId()).isEqualTo(tenantId);
        assertThat(window.includes(START)).isTrue();
        assertThat(window.includes(START.plusSeconds(59))).isTrue();
        assertThat(window.includes(START.plusSeconds(60))).isFalse();
        assertThatThrownBy(() -> new EffectiveTimeWindow(START, START))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompliancePolicyVersionReference(policy, UUID.randomUUID(), 0, window))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requestDefensivelyCopiesChecksAndRequiresTenantAndChecks() {
        UUID tenantId = UUID.randomUUID();
        var mutableChecks = new java.util.HashSet<ComplianceCheckType>();
        mutableChecks.add(ComplianceCheckType.VEHICLE_DOCUMENT_ELIGIBILITY);
        var request = new ComplianceEvaluationRequest(
                tenantId,
                UUID.randomUUID(),
                "TRIP_DISPATCH",
                UUID.randomUUID(),
                START,
                new JurisdictionScopeReference("UNSELECTED", "PHASE1"),
                mutableChecks);

        mutableChecks.add(ComplianceCheckType.DRIVER_ELIGIBILITY);

        assertThat(request.requestedChecks())
                .containsExactly(ComplianceCheckType.VEHICLE_DOCUMENT_ELIGIBILITY);
        assertThatThrownBy(() -> new ComplianceEvaluationRequest(
                tenantId,
                UUID.randomUUID(),
                "TRIP_DISPATCH",
                UUID.randomUUID(),
                START,
                new JurisdictionScopeReference("UNSELECTED", "PHASE1"),
                Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unavailableResultCannotCarryAllowOrAnyOtherEffect() {
        var unavailable = new ComplianceCheckResult(
                ComplianceCheckType.BILLING_TAX_FACT_ELIGIBILITY,
                ComplianceEvaluationStatus.POLICY_UNAVAILABLE,
                ComplianceEvidenceState.UNKNOWN,
                Optional.empty(),
                List.of(),
                List.of("POLICY_AUTHORITY_UNAVAILABLE"));

        assertThat(unavailable.effect()).isEmpty();
        assertThatThrownBy(() -> new ComplianceCheckResult(
                ComplianceCheckType.BILLING_TAX_FACT_ELIGIBILITY,
                ComplianceEvaluationStatus.POLICY_UNAVAILABLE,
                ComplianceEvidenceState.UNKNOWN,
                Optional.of(ComplianceDecisionEffect.ALLOW),
                List.of(),
                List.of("POLICY_AUTHORITY_UNAVAILABLE")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void evaluatedResultRequiresEffectButDefinesNoAggregatePrecedence() {
        assertThatThrownBy(() -> new ComplianceCheckResult(
                ComplianceCheckType.CARGO_DOCUMENT_ELIGIBILITY,
                ComplianceEvaluationStatus.EVALUATED,
                ComplianceEvidenceState.PRESENT_VALID,
                Optional.empty(),
                List.of(),
                List.of("CONTROLLED_FIXTURE")))
                .isInstanceOf(IllegalArgumentException.class);

        UUID tenantId = UUID.randomUUID();
        var results = new ArrayList<ComplianceCheckResult>();
        results.add(new ComplianceCheckResult(
                ComplianceCheckType.CARGO_DOCUMENT_ELIGIBILITY,
                ComplianceEvaluationStatus.UNEVALUATED,
                ComplianceEvidenceState.UNKNOWN,
                Optional.empty(),
                List.of(),
                List.of("POLICY_NOT_APPROVED")));
        var evaluation = new ComplianceEvaluationResult(
                tenantId, UUID.randomUUID(), START, Optional.empty(), results);
        results.clear();

        assertThat(evaluation.policyVersion()).isEmpty();
        assertThat(evaluation.checkResults()).hasSize(1);
    }

    @Test
    void rejectsForeignTenantPolicyAndSourceFacts() {
        UUID tenantId = UUID.randomUUID();
        UUID foreignTenantId = UUID.randomUUID();
        var foreignPolicy = new CompliancePolicyVersionReference(
                new CompliancePolicyIdentity(
                        foreignTenantId,
                        "POLICY_A",
                        new JurisdictionScopeReference("UNSELECTED", "PHASE1")),
                UUID.randomUUID(),
                1,
                new EffectiveTimeWindow(START, null));
        var foreignFact = new ComplianceSourceFactReference(
                foreignTenantId, "FLEET", "VEHICLE_DOCUMENT", UUID.randomUUID(), "1", START);
        var result = new ComplianceCheckResult(
                ComplianceCheckType.VEHICLE_DOCUMENT_ELIGIBILITY,
                ComplianceEvaluationStatus.UNEVALUATED,
                ComplianceEvidenceState.UNKNOWN,
                Optional.empty(),
                List.of(foreignFact),
                List.of("POLICY_NOT_APPROVED"));

        assertThatThrownBy(() -> new ComplianceEvaluationResult(
                tenantId, UUID.randomUUID(), START, Optional.of(foreignPolicy), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ComplianceEvaluationResult(
                tenantId, UUID.randomUUID(), START, Optional.empty(), List.of(result)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void driverFactContractIsImmutableAndRejectsForeignTenantFacts() {
        UUID tenantId = UUID.randomUUID();
        var facts = new ArrayList<DriverEligibilityFactsQuery.LicenceFact>();
        facts.add(new DriverEligibilityFactsQuery.LicenceFact(
                new ComplianceSourceFactReference(
                        tenantId, "FLEET", "DRIVER_LICENCE", UUID.randomUUID(), "1", START),
                "CONTROLLED_FIXTURE",
                "ACTIVE",
                null,
                null));
        var result = new DriverEligibilityFactsQuery.DriverEligibilityFacts(
                tenantId, UUID.randomUUID(), facts, List.of());

        facts.clear();

        assertThat(result.licences()).hasSize(1);
        assertThatThrownBy(() -> new DriverEligibilityFactsQuery.DriverEligibilityFacts(
                tenantId,
                UUID.randomUUID(),
                List.of(new DriverEligibilityFactsQuery.LicenceFact(
                        new ComplianceSourceFactReference(
                                UUID.randomUUID(),
                                "FLEET",
                                "DRIVER_LICENCE",
                                UUID.randomUUID(),
                                "1",
                                START),
                        "CONTROLLED_FIXTURE",
                        "ACTIVE",
                        null,
                        null)),
                List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
