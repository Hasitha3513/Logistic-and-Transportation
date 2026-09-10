package com.transportlogistics.app.tracking.adapters.inbound.geofence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.tracking.adapters.configuration.GeofenceEvaluatorSettings;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceEvaluationJob;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePosition;
import com.transportlogistics.app.tracking.domain.geofence.Wgs84Coordinate;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceEvaluationJobRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofencePositionRepositoryPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GeofenceEvaluationCoordinatorTest {
    private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");

    @Test
    void saturatedFixedWorkerReleasesExcessClaimWithoutGrowingQueue() throws Exception {
        var jobs = mock(GeofenceEvaluationJobRepositoryPort.class);
        var positions = mock(GeofencePositionRepositoryPort.class);
        var evaluator = mock(GeofenceEvaluationUseCase.class);
        var tenantContexts = mock(TenantContextExecutor.class);
        when(tenantContexts.within(any(), any(Supplier.class))).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(1)).get());
        UUID tenant = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(jobs.backlog(NOW)).thenReturn(
                new GeofenceEvaluationJobRepositoryPort.JobBacklog(2, 0, NOW));
        when(jobs.claimDue("owner", NOW, NOW.plusSeconds(30), 2)).thenReturn(List.of(
                job(tenant, first), job(tenant, second)));
        when(jobs.renew(eq(tenant), any(), eq("owner"), eq(NOW), eq(NOW.plusSeconds(30))))
                .thenReturn(true);
        when(positions.find(eq(tenant), any())).thenAnswer(invocation -> Optional.of(
                position(tenant, invocation.getArgument(1))));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        when(evaluator.evaluate(any(), eq(NOW))).thenAnswer(invocation -> {
            started.countDown();
            proceed.await(2, TimeUnit.SECONDS);
            return List.of();
        });
        var coordinator = new GeofenceEvaluationCoordinator(jobs, positions, evaluator,
                new GeofenceEvaluatorSettings(2, 1, 0, Duration.ofSeconds(30),
                        Duration.ofSeconds(5), Duration.ofSeconds(1)),
                new SimpleMeterRegistry(), Clock.fixed(NOW, ZoneOffset.UTC), tenantContexts, "owner");

        coordinator.tick();
        started.await(2, TimeUnit.SECONDS);
        verify(jobs, timeout(2_000)).release(tenant, second, "owner", NOW);
        proceed.countDown();
        verify(jobs, timeout(2_000)).complete(tenant, first, "owner", NOW);
        coordinator.shutdown();
    }

    private static GeofenceEvaluationJob job(UUID tenant, UUID position) {
        return new GeofenceEvaluationJob(tenant, position,
                GeofenceEvaluationJob.Status.PROCESSING, 1, "owner", NOW.plusSeconds(30), NOW, NOW);
    }

    private static GeofencePosition position(UUID tenant, UUID id) {
        return new GeofencePosition(tenant, id, UUID.randomUUID(), NOW,
                new Wgs84Coordinate(79, 6), Trust.TRUSTED, Ordering.IN_ORDER, false);
    }
}
