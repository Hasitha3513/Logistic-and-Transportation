package com.transportlogistics.app.tracking.adapters.inbound.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePosition;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationPosition;
import com.transportlogistics.app.tracking.domain.speed.SpeedPosition;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.inbound.SpeedEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.outbound.HistoricalTelemetryLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryEvaluationDispatchPort;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryEvaluationDispatchPort.Dispatch;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryEvaluationDispatchPort.Evaluator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class TelemetryEvaluationDispatchWorkerTest {
    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Test
    void routesExactImmutableHistoryToAllThreeAuthorizedEvaluators() {
        var dispatches = mock(TelemetryEvaluationDispatchPort.class);
        var history = mock(HistoricalTelemetryLookupPort.class);
        var geofence = mock(GeofenceEvaluationUseCase.class);
        var speed = mock(SpeedEvaluationUseCase.class);
        var routeDeviation = mock(RouteDeviationEvaluationUseCase.class);
        var fact = fact(UUID.randomUUID());
        var jobs = List.of(job(fact, Evaluator.GEOFENCE, 1), job(fact, Evaluator.SPEED, 1),
                job(fact, Evaluator.ROUTE_DEVIATION, 1));
        when(dispatches.claim(anyString(), any(), any(), anyInt())).thenReturn(jobs);
        when(history.findExact(fact.tenantId(), fact.recordedAt(), fact.eventId()))
                .thenReturn(Optional.of(fact));

        worker(dispatches, history, geofence, speed, routeDeviation).tick();

        verify(geofence).evaluate(any(GeofencePosition.class), any(Instant.class));
        verify(speed).evaluate(any(SpeedPosition.class), any(Instant.class));
        verify(routeDeviation).evaluate(any(RouteDeviationPosition.class));
        for (var job : jobs) {
            verify(dispatches).complete(org.mockito.ArgumentMatchers.eq(job.id()), anyString(), any());
        }
        verify(dispatches, never()).retry(any(), anyString(), any(), any(), anyString());
    }

    @Test
    void rejectsForeignOrMismatchedHistoryAndRetainsDispatchForRetry() {
        var dispatches = mock(TelemetryEvaluationDispatchPort.class);
        var history = mock(HistoricalTelemetryLookupPort.class);
        var geofence = mock(GeofenceEvaluationUseCase.class);
        var speed = mock(SpeedEvaluationUseCase.class);
        var routeDeviation = mock(RouteDeviationEvaluationUseCase.class);
        var expected = fact(UUID.randomUUID());
        var dispatch = job(expected, Evaluator.GEOFENCE, 1);
        when(dispatches.claim(anyString(), any(), any(), anyInt()))
                .thenReturn(List.of(dispatch));
        when(history.findExact(expected.tenantId(), expected.recordedAt(), expected.eventId()))
                .thenReturn(Optional.of(fact(UUID.randomUUID())));

        worker(dispatches, history, geofence, speed, routeDeviation).tick();

        verify(geofence, never()).evaluate(any(), any());
        verify(dispatches).retry(org.mockito.ArgumentMatchers.eq(dispatch.id()), anyString(), any(),
                any(), org.mockito.ArgumentMatchers.eq("HISTORY_IDENTITY_INVALID"));
        verify(dispatches, never()).complete(any(), anyString(), any());
    }

    @Test
    void evaluatorFailureDoesNotDeleteHistoryAndBecomesBoundedFailureAtLimit() {
        var dispatches = mock(TelemetryEvaluationDispatchPort.class);
        var history = mock(HistoricalTelemetryLookupPort.class);
        var geofence = mock(GeofenceEvaluationUseCase.class);
        var speed = mock(SpeedEvaluationUseCase.class);
        var routeDeviation = mock(RouteDeviationEvaluationUseCase.class);
        var fact = fact(UUID.randomUUID());
        var dispatch = job(fact, Evaluator.ROUTE_DEVIATION, 10);
        when(dispatches.claim(anyString(), any(), any(), anyInt()))
                .thenReturn(List.of(dispatch));
        when(history.findExact(fact.tenantId(), fact.recordedAt(), fact.eventId()))
                .thenReturn(Optional.of(fact));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("sensitive detail"))
                .when(routeDeviation).evaluate(any());

        worker(dispatches, history, geofence, speed, routeDeviation).tick();

        verify(history).findExact(fact.tenantId(), fact.recordedAt(), fact.eventId());
        verify(dispatches).fail(org.mockito.ArgumentMatchers.eq(dispatch.id()), anyString(), any(),
                org.mockito.ArgumentMatchers.eq("EVALUATOR_FAILED"));
        verify(dispatches, never()).complete(any(), anyString(), any());
    }

    private static TelemetryEvaluationDispatchWorker worker(
            TelemetryEvaluationDispatchPort dispatches, HistoricalTelemetryLookupPort history,
            GeofenceEvaluationUseCase geofence, SpeedEvaluationUseCase speed,
            RouteDeviationEvaluationUseCase routeDeviation) {
        TenantContextExecutor tenantContexts = new TenantContextExecutor() {
            @Override
            public <T> T within(TenantExecutionContext context, Supplier<T> work) {
                return work.get();
            }
        };
        return new TelemetryEvaluationDispatchWorker(dispatches, history, geofence, speed,
                routeDeviation, tenantContexts, new SimpleMeterRegistry(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Dispatch job(HistoricalTelemetry fact, Evaluator evaluator, int attempts) {
        return new Dispatch(UUID.randomUUID(), fact.tenantId(), fact.recordedAt(), fact.eventId(),
                fact.dedupeIdentity(), fact.vehicleId(), evaluator, attempts);
    }

    private static HistoricalTelemetry fact(UUID tenantId) {
        UUID eventId = UUID.randomUUID();
        return new HistoricalTelemetry(eventId, 1, tenantId, UUID.randomUUID(), UUID.randomUUID(),
                "GENERIC", "message-1", "a".repeat(64), new BigDecimal("6.9271"),
                new BigDecimal("79.8612"), new BigDecimal("42.5"), new BigDecimal("90"),
                new BigDecimal("5"), null, EngineState.ON, null, null, NOW.minusSeconds(1), NOW,
                Trust.TRUSTED, "GOOD", Ordering.IN_ORDER, null, null, null, null, null);
    }
}
