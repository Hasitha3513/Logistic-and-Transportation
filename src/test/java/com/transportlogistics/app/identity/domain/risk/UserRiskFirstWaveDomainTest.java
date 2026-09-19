package com.transportlogistics.app.identity.domain.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserRiskFirstWaveDomainTest {
    private static final Instant START = Instant.parse("2026-09-19T00:00:00Z");

    @Test
    void exposesOnlyTheApprovedFirstWaveVocabularyAndThresholds() {
        assertThat(UserRiskFirstWaveContract.SignalType.values())
                .containsExactly(UserRiskFirstWaveContract.SignalType.UNAUTHORIZED_OVERRIDE_ATTEMPT);
        assertThat(UserRiskFirstWaveContract.SourceModule.values())
                .containsExactly(UserRiskFirstWaveContract.SourceModule.IDENTITY);
        assertThat(UserRiskFirstWaveContract.Action.values()).containsExactly(
                UserRiskFirstWaveContract.Action.IDENTITY_USER_CREATE_ROLE_PERMISSION_CEILING,
                UserRiskFirstWaveContract.Action.IDENTITY_USER_UPDATE_ROLE_PERMISSION_CEILING,
                UserRiskFirstWaveContract.Action.IDENTITY_ROLE_CREATE_PERMISSION_CEILING,
                UserRiskFirstWaveContract.Action.IDENTITY_ROLE_UPDATE_PERMISSION_CEILING);
        assertThat(UserRiskFirstWaveContract.Reason.values()).containsExactly(
                UserRiskFirstWaveContract.Reason.REQUESTED_PERMISSION_EXCEEDS_ACTOR_CEILING);
        assertThat(UserRiskFirstWaveContract.DISTINCT_FACT_THRESHOLD).isEqualTo(3);
        assertThat(UserRiskFirstWaveContract.EVALUATION_WINDOW).hasMinutes(15);
        assertThat(UserRiskFirstWaveContract.DELIVERY_LATENESS).hasMinutes(5);
        assertThat(UserRiskFirstWaveContract.RETENTION).hasDays(180);
        assertThat(UserRiskFirstWaveContract.APPEAL_WINDOW).hasDays(30);
    }

    @Test
    void signalIsTenantQualifiedMinimizedAndUsesStableRetryIdentity() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        var action = UserRiskFirstWaveContract.Action.IDENTITY_ROLE_UPDATE_PERMISSION_CEILING;
        UUID eventId = PermissionCeilingDenialSignal.retryIdentity(
                tenantId, actorId, action, UserRiskFirstWaveContract.TargetType.ROLE, targetId, "request-42");

        var signal = signal(eventId, tenantId, actorId, targetId, action, "request-42", START, START);

        assertThat(signal.signalType())
                .isEqualTo(UserRiskFirstWaveContract.SignalType.UNAUTHORIZED_OVERRIDE_ATTEMPT);
        assertThat(signal.actionFamily())
                .isEqualTo(UserRiskFirstWaveContract.ActionFamily.IDENTITY_PERMISSION_CEILING);
        assertThat(signal.subjectUserId()).isEqualTo(actorId);
        assertThat(signal.sourceModule()).isEqualTo(UserRiskFirstWaveContract.SourceModule.IDENTITY);
        assertThat(signal.sourceEventId()).isEqualTo(eventId);
        assertThat(signal.mayContributeToFinding()).isTrue();
        assertThat(PermissionCeilingDenialSignal.retryIdentity(
                tenantId, actorId, action, UserRiskFirstWaveContract.TargetType.ROLE, targetId, "request-42"))
                .isEqualTo(eventId);
    }

    @Test
    void rejectsMismatchedTenantSubjectTimeAndRetryIdentity() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        var action = UserRiskFirstWaveContract.Action.IDENTITY_USER_CREATE_ROLE_PERMISSION_CEILING;
        UUID eventId = PermissionCeilingDenialSignal.retryIdentity(
                tenantId, actorId, action, UserRiskFirstWaveContract.TargetType.USER, targetId, "request-7");

        assertThatThrownBy(() -> new PermissionCeilingDenialSignal(
                eventId, tenantId, UserRiskFirstWaveContract.SourceModule.IDENTITY, eventId,
                UUID.randomUUID(), actorId, action,
                UserRiskFirstWaveContract.Reason.REQUESTED_PERMISSION_EXCEEDS_ACTOR_CEILING,
                UserRiskFirstWaveContract.TargetType.USER, targetId, START, START, "request-7",
                UserRiskFirstWaveContract.EvidenceState.SUFFICIENT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signal(
                UUID.randomUUID(), tenantId, actorId, targetId, action, "request-7", START, START))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signal(
                eventId, tenantId, actorId, targetId, action, "request-7", START, START.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PermissionCeilingDenialSignal(
                eventId, tenantId, UserRiskFirstWaveContract.SourceModule.IDENTITY, eventId,
                actorId, actorId, action,
                UserRiskFirstWaveContract.Reason.REQUESTED_PERMISSION_EXCEEDS_ACTOR_CEILING,
                UserRiskFirstWaveContract.TargetType.ROLE, targetId, START, START, "request-7",
                UserRiskFirstWaveContract.EvidenceState.SUFFICIENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateAndConflictingIdentitySemanticsAreExplicit() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        var action = UserRiskFirstWaveContract.Action.IDENTITY_ROLE_CREATE_PERMISSION_CEILING;
        UUID eventId = PermissionCeilingDenialSignal.retryIdentity(
                tenantId, actorId, action, UserRiskFirstWaveContract.TargetType.ROLE, targetId, "request-9");
        var original = signal(eventId, tenantId, actorId, targetId, action, "request-9", START, START);
        var duplicate = signal(eventId, tenantId, actorId, targetId, action, "request-9", START, START);
        var conflicting = signal(eventId, tenantId, actorId, targetId, action, "request-9", START,
                START.plusSeconds(1));
        UUID distinctId = PermissionCeilingDenialSignal.retryIdentity(
                tenantId, actorId, action, UserRiskFirstWaveContract.TargetType.ROLE, targetId, "request-10");
        var distinct = signal(distinctId, tenantId, actorId, targetId, action, "request-10", START, START);

        assertThat(original.compareIdentity(duplicate))
                .isEqualTo(UserRiskFirstWaveContract.SignalComparison.DUPLICATE);
        assertThat(original.compareIdentity(conflicting))
                .isEqualTo(UserRiskFirstWaveContract.SignalComparison.CONFLICTING);
        assertThat(original.compareIdentity(distinct))
                .isEqualTo(UserRiskFirstWaveContract.SignalComparison.DISTINCT);
    }

    @Test
    void onlySufficientEvidenceMayContributeToFinding() {
        assertThat(UserRiskFirstWaveContract.EvidenceState.SUFFICIENT.mayContributeToFinding()).isTrue();
        assertThat(UserRiskFirstWaveContract.EvidenceState.INSUFFICIENT.mayContributeToFinding()).isFalse();
        assertThat(UserRiskFirstWaveContract.EvidenceState.UNKNOWN.mayContributeToFinding()).isFalse();
        assertThat(UserRiskFirstWaveContract.EvidenceState.CONFLICTING.mayContributeToFinding()).isFalse();
        assertThat(UserRiskFirstWaveContract.EvidenceState.STALE.mayContributeToFinding()).isFalse();
    }

    @Test
    void ruleVersionUsesTenantIdentityAndHalfOpenEffectiveTime() {
        var rule = new UserRiskRuleVersion(
                UUID.randomUUID(), UUID.randomUUID(), UserRiskRuleVersion.FIRST_WAVE_RULE_KEY,
                1, START, START.plusSeconds(60));

        assertThat(rule.appliesAt(START)).isTrue();
        assertThat(rule.appliesAt(START.plusSeconds(59))).isTrue();
        assertThat(rule.appliesAt(START.plusSeconds(60))).isFalse();
        assertThatThrownBy(() -> new UserRiskRuleVersion(
                UUID.randomUUID(), UUID.randomUUID(), "UNAPPROVED_RULE", 1, START, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findingIsImmutableRequiresThreeFactsAndRemainsAdvisoryOnly() {
        var mutableEvidence = new HashSet<>(Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        var finding = new AdvisoryRiskFinding(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UserRiskFirstWaveContract.ActionFamily.IDENTITY_PERMISSION_CEILING,
                START, START.plusSeconds(900), START.plusSeconds(901), mutableEvidence,
                UserRiskFirstWaveContract.ReviewPriority.MEDIUM,
                UserRiskFirstWaveContract.Effect.ADVISORY_REVIEW_ONLY,
                UserRiskFirstWaveContract.FindingState.OPEN_REVIEW);
        mutableEvidence.add(UUID.randomUUID());

        assertThat(finding.evidenceEventIds()).hasSize(3);
        assertThat(finding.effect()).isEqualTo(UserRiskFirstWaveContract.Effect.ADVISORY_REVIEW_ONLY);
        assertThatThrownBy(() -> new AdvisoryRiskFinding(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UserRiskFirstWaveContract.ActionFamily.IDENTITY_PERMISSION_CEILING,
                START, START.plusSeconds(1), START.plusSeconds(2), Set.of(UUID.randomUUID(), UUID.randomUUID()),
                UserRiskFirstWaveContract.ReviewPriority.MEDIUM,
                UserRiskFirstWaveContract.Effect.ADVISORY_REVIEW_ONLY,
                UserRiskFirstWaveContract.FindingState.OPEN_REVIEW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reviewAndAppealEnforceReviewerSeparationAndThirtyDayWindow() {
        UUID findingId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID originalReviewer = UUID.randomUUID();
        var original = review(findingId, tenantId, subjectId, originalReviewer, START);
        var appealDecision = review(
                findingId, tenantId, subjectId, UUID.randomUUID(), START.plusSeconds(86_400));

        var appeal = new UserRiskAppeal(
                UUID.randomUUID(), findingId, tenantId, subjectId, UUID.randomUUID(), originalReviewer,
                original.decidedAt(), START.plusSeconds(60), Optional.of(appealDecision));

        assertThat(appeal.decision()).contains(appealDecision);
        assertThatThrownBy(() -> review(findingId, tenantId, subjectId, subjectId, START))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UserRiskAppeal(
                UUID.randomUUID(), findingId, tenantId, subjectId, UUID.randomUUID(), originalReviewer,
                START, START.plusSeconds(31L * 86_400), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UserRiskAppeal(
                UUID.randomUUID(), findingId, tenantId, subjectId, UUID.randomUUID(), originalReviewer,
                START, START.plusSeconds(60), Optional.of(original)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UserRiskAppeal(
                UUID.randomUUID(), findingId, tenantId, subjectId, subjectId, originalReviewer,
                START, START.plusSeconds(60), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PermissionCeilingDenialSignal signal(
            UUID eventId,
            UUID tenantId,
            UUID actorId,
            UUID targetId,
            UserRiskFirstWaveContract.Action action,
            String correlationId,
            Instant occurredAt,
            Instant receivedAt) {
        return new PermissionCeilingDenialSignal(
                eventId, tenantId, UserRiskFirstWaveContract.SourceModule.IDENTITY, eventId,
                actorId, actorId, action,
                UserRiskFirstWaveContract.Reason.REQUESTED_PERMISSION_EXCEEDS_ACTOR_CEILING,
                action.name().contains("USER_")
                        ? UserRiskFirstWaveContract.TargetType.USER
                        : UserRiskFirstWaveContract.TargetType.ROLE,
                targetId, occurredAt, receivedAt, correlationId,
                UserRiskFirstWaveContract.EvidenceState.SUFFICIENT);
    }

    private UserRiskReview review(
            UUID findingId,
            UUID tenantId,
            UUID subjectId,
            UUID reviewerId,
            Instant decidedAt) {
        return new UserRiskReview(
                UUID.randomUUID(), findingId, tenantId, subjectId, reviewerId,
                UserRiskFirstWaveContract.ReviewDisposition.FALSE_POSITIVE,
                "AUTHORIZED_ADMINISTRATIVE_ERROR", decidedAt);
    }
}
