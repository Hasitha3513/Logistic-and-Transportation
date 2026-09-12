package com.transportlogistics.app.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.tracking.domain.speed.SpeedAttribution;
import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationResult;
import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationJob;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedPosition;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import com.transportlogistics.app.tracking.domain.speed.VehicleSpeedState;
import com.transportlogistics.app.tracking.ports.outbound.SpeedAttributionLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedEvaluationJobRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedPositionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedRuleRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodePublisherPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleSpeedStateRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SpeedEvaluationServiceTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID VEHICLE = UUID.randomUUID();
    private static final UUID RULE = UUID.randomUUID();
    private static final Instant BASE = Instant.parse("2026-09-12T00:00:00Z");
    private final SpeedPositionRepositoryPort positions = mock(SpeedPositionRepositoryPort.class);
    private final SpeedRuleRepositoryPort rules = mock(SpeedRuleRepositoryPort.class);
    private final VehicleSpeedStateRepositoryPort states = mock(VehicleSpeedStateRepositoryPort.class);
    private final SpeedingEpisodeRepositoryPort episodes = mock(SpeedingEpisodeRepositoryPort.class);
    private final SpeedEvaluationJobRepositoryPort jobs = mock(SpeedEvaluationJobRepositoryPort.class);
    private final SpeedAttributionLookupPort attribution = mock(SpeedAttributionLookupPort.class);
    private final SpeedingEpisodePublisherPort publisher = mock(SpeedingEpisodePublisherPort.class);
    private SpeedEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new SpeedEvaluationService(positions, rules, states, episodes, jobs,
                attribution, publisher, new com.transportlogistics.app.tracking.ports.outbound.SpeedEvaluationTransactionPort() {
                    @Override public <T> T execute(Supplier<T> operation) { return operation.get(); }
                });
        when(states.findForUpdate(TENANT, VEHICLE))
                .thenReturn(Optional.of(VehicleSpeedState.unknown(TENANT, VEHICLE)));
        when(states.save(any(), anyLong())).thenAnswer(invocation -> invocation.getArgument(0));
        when(episodes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(attribution.findAt(any(), any(), any())).thenReturn(Optional.empty());
        when(rules.findActiveTenantRule(TENANT)).thenReturn(Optional.of(tenantRule()));
        when(episodes.findActive(TENANT, VEHICLE)).thenReturn(Optional.empty());
    }

    @Test
    void initializesNormalAtOrBelowThresholdWithoutPublishing() {
        var result = service.evaluate(position(1, "60.000"), BASE.plusSeconds(1));
        assertThat(result.outcome()).isEqualTo(SpeedEvaluationResult.Outcome.STATE_INITIALIZED);
        assertThat(result.state().monitoringState()).isEqualTo(VehicleSpeedState.MonitoringState.NORMAL);
        verify(publisher, never()).publish(any(), any());
    }

    @Test
    void firstAboveIsSilentAndSecondConfirmsExactMinimizedEvent() {
        var first = service.evaluate(position(1, "70"), BASE.plusSeconds(1));
        when(states.findForUpdate(TENANT, VEHICLE)).thenReturn(Optional.of(first.state()));
        var confirming = position(2, "75");
        var second = service.evaluate(confirming, BASE.plusSeconds(2));

        assertThat(first.outcome()).isEqualTo(SpeedEvaluationResult.Outcome.CANDIDATE_UPDATED);
        assertThat(second.outcome()).isEqualTo(SpeedEvaluationResult.Outcome.EPISODE_CONFIRMED);
        ArgumentCaptor<SpeedingEpisodePublisherPort.VehicleSpeedingDetectedV1> event =
                ArgumentCaptor.forClass(SpeedingEpisodePublisherPort.VehicleSpeedingDetectedV1.class);
        verify(publisher).publish(org.mockito.ArgumentMatchers.eq(TENANT), event.capture());
        assertThat(event.getValue().observedSpeedKph()).isEqualTo(new SpeedKph(new BigDecimal("75")));
        assertThat(event.getValue().sourceTimestamp()).isEqualTo(confirming.sourceTimestamp());
        assertThat(event.getValue().speedEpisodeId()).isEqualTo(second.episode().orElseThrow().id());
    }

    @Test
    void routeRuleWinsAndAttributionFailureFallsBackSafely() {
        UUID route = UUID.randomUUID();
        SpeedRule routeRule = new SpeedRule(UUID.randomUUID(), TENANT, "Route",
                SpeedRule.Scope.ROUTE_VERSION, route, "R7", new SpeedKph(new BigDecimal("40")),
                SpeedRule.Lifecycle.ACTIVE, 3, BASE);
        when(attribution.findAt(TENANT, VEHICLE, BASE.plusSeconds(1)))
                .thenReturn(Optional.of(new SpeedAttribution(UUID.randomUUID(), null, route, "R7")));
        when(rules.findActiveRouteRule(TENANT, route, "R7")).thenReturn(Optional.of(routeRule));
        assertThat(service.evaluate(position(1, "50"), BASE.plusSeconds(2)).state().effectiveRuleId())
                .isEqualTo(routeRule.id());

        doThrow(new IllegalStateException("trip unavailable")).when(attribution)
                .findAt(TENANT, VEHICLE, BASE.plusSeconds(2));
        when(states.findForUpdate(TENANT, VEHICLE))
                .thenReturn(Optional.of(VehicleSpeedState.unknown(TENANT, VEHICLE)));
        assertThat(service.evaluate(position(2, "50"), BASE.plusSeconds(3)).state().effectiveRuleId())
                .isEqualTo(RULE);
    }

    @Test
    void configurationUnavailableAndStalePositionNeverPublish() {
        when(rules.findActiveTenantRule(TENANT)).thenReturn(Optional.empty());
        var unavailable = service.evaluate(position(1, "70"), BASE.plusSeconds(1));
        assertThat(unavailable.outcome())
                .isEqualTo(SpeedEvaluationResult.Outcome.CONFIGURATION_UNAVAILABLE);
        var stale = service.evaluate(position(2, "70"), BASE.plusSeconds(303));
        assertThat(stale.outcome()).isEqualTo(SpeedEvaluationResult.Outcome.NO_OP);
        verify(publisher, never()).publish(any(), any());
    }

    @Test
    void continuedSpeedingProgressesAndOneEligibleNormalSampleClosesWithoutRepublishing() {
        SpeedEvaluationResult first = service.evaluate(position(1, "70"), BASE.plusSeconds(1));
        when(states.findForUpdate(TENANT, VEHICLE)).thenReturn(Optional.of(first.state()));
        SpeedEvaluationResult confirmed = service.evaluate(position(2, "75"), BASE.plusSeconds(2));
        SpeedingEpisode episode = confirmed.episode().orElseThrow();
        when(states.findForUpdate(TENANT, VEHICLE)).thenReturn(Optional.of(confirmed.state()));
        when(episodes.findActive(TENANT, VEHICLE)).thenReturn(Optional.of(episode));
        SpeedEvaluationResult progressed = service.evaluate(position(3, "80"), BASE.plusSeconds(3));
        when(states.findForUpdate(TENANT, VEHICLE)).thenReturn(Optional.of(progressed.state()));
        when(episodes.findActive(TENANT, VEHICLE)).thenReturn(progressed.episode());
        SpeedEvaluationResult closed = service.evaluate(position(4, "55"), BASE.plusSeconds(4));

        assertThat(progressed.outcome()).isEqualTo(SpeedEvaluationResult.Outcome.EPISODE_PROGRESSED);
        assertThat(closed.outcome()).isEqualTo(SpeedEvaluationResult.Outcome.EPISODE_CLOSED);
        verify(publisher).publish(any(), any());
    }

    @Test
    void missingOrIneligibleDoesNotClearAnActiveEpisode() {
        SpeedPosition missing = new SpeedPosition(TENANT, VEHICLE, UUID.randomUUID(),
                BASE.plusSeconds(3), null, false, true, true, true);
        VehicleSpeedState speeding = speedingState();
        SpeedingEpisode active = activeEpisode();
        when(states.findForUpdate(TENANT, VEHICLE)).thenReturn(Optional.of(speeding));
        when(episodes.findActive(TENANT, VEHICLE)).thenReturn(Optional.of(active));
        assertThat(service.evaluate(missing, BASE.plusSeconds(4)).outcome())
                .isEqualTo(SpeedEvaluationResult.Outcome.NO_OP);
        verify(episodes, never()).save(any());
    }

    @Test
    void leasedJobLoadsPositionEvaluatesAndCompletesWithItsOwner() {
        SpeedPosition position = position(1, "55");
        SpeedEvaluationJob job = new SpeedEvaluationJob(TENANT, position.positionId(), VEHICLE,
                position.sourceTimestamp(), SpeedEvaluationJob.Status.PROCESSING, 1, BASE,
                "worker", BASE.plusSeconds(120), null, BASE);
        when(positions.find(TENANT, position.positionId())).thenReturn(Optional.of(position));
        assertThat(service.process(job, "worker", BASE.plusSeconds(2))).isPresent();
        verify(jobs).complete(TENANT, position.positionId(), "worker", BASE.plusSeconds(2));
    }

    private static SpeedRule tenantRule() {
        return new SpeedRule(RULE, TENANT, "Tenant", SpeedRule.Scope.TENANT, null, null,
                new SpeedKph(new BigDecimal("60")), SpeedRule.Lifecycle.ACTIVE, 1, BASE);
    }

    private static SpeedPosition position(int sequence, String speed) {
        return new SpeedPosition(TENANT, VEHICLE, new UUID(0, sequence), BASE.plusSeconds(sequence),
                new SpeedKph(new BigDecimal(speed)), false, true, true, true);
    }

    private static SpeedingEpisode activeEpisode() {
        return SpeedingEpisode.confirm(TENANT, VEHICLE,
                new com.transportlogistics.app.tracking.domain.speed.ResolvedSpeedThreshold(tenantRule(),
                        com.transportlogistics.app.tracking.domain.speed.ResolvedSpeedThreshold.ThresholdSource.TENANT_CONFIG),
                SpeedAttribution.unknown(), new UUID(0, 1), BASE.plusSeconds(1),
                new SpeedKph(new BigDecimal("70")), position(2, "75"), null);
    }

    private static VehicleSpeedState speedingState() {
        VehicleSpeedState unknown = VehicleSpeedState.unknown(TENANT, VEHICLE);
        var threshold = new com.transportlogistics.app.tracking.domain.speed.ResolvedSpeedThreshold(
                tenantRule(), com.transportlogistics.app.tracking.domain.speed.ResolvedSpeedThreshold.ThresholdSource.TENANT_CONFIG);
        var first = unknown.evaluate(position(1, "70"), Optional.of(threshold),
                SpeedAttribution.unknown(), BASE.plusSeconds(1), null, null);
        return first.state().evaluate(position(2, "75"), Optional.of(threshold),
                SpeedAttribution.unknown(), BASE.plusSeconds(2), null, null).state();
    }
}
