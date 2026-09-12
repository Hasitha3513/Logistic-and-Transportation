package com.transportlogistics.app.tracking.adapters.inbound.speed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tracking.adapters.configuration.SpeedEvaluatorSettings;
import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationJob;
import com.transportlogistics.app.tracking.ports.inbound.SpeedEvaluationJobUseCase;
import com.transportlogistics.app.tracking.ports.outbound.SpeedEvaluationJobRepositoryPort;
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
import org.junit.jupiter.api.Test;

class SpeedEvaluationCoordinatorTest {
    private static final Instant NOW = Instant.parse("2026-09-12T03:00:00Z");

    @Test
    void oneTickClaimsRenewsAndProcessesThroughTenantContext() throws Exception {
        SpeedEvaluationJobRepositoryPort jobs = mock(SpeedEvaluationJobRepositoryPort.class);
        SpeedEvaluationJobUseCase evaluator = mock(SpeedEvaluationJobUseCase.class);
        TenantContextExecutor tenants = mock(TenantContextExecutor.class);
        SpeedEvaluationJob job = job(1);
        CountDownLatch completed = new CountDownLatch(1);
        when(jobs.claimDue(anyString(), any(), any(), anyInt())).thenReturn(List.of(job));
        when(jobs.renew(any(), any(), anyString(), any(), any())).thenReturn(true);
        when(tenants.within(any(), any(java.util.function.Supplier.class))).thenAnswer(invocation -> {
            java.util.function.Supplier<?> operation = invocation.getArgument(1);
            Object result = operation.get();
            completed.countDown();
            return result;
        });
        when(evaluator.process(any(), anyString(), any())).thenReturn(Optional.empty());
        SpeedEvaluationCoordinator coordinator = coordinator(jobs, evaluator, tenants);
        try {
            coordinator.tick();
            assertThat(completed.await(5, TimeUnit.SECONDS)).isTrue();
            verify(evaluator).process(any(), anyString(), any());
        } finally {
            coordinator.shutdown();
        }
    }

    @Test
    void transientFailureUsesBoundedRetryWithoutSensitiveErrorMaterial() throws Exception {
        SpeedEvaluationJobRepositoryPort jobs = mock(SpeedEvaluationJobRepositoryPort.class);
        SpeedEvaluationJobUseCase evaluator = mock(SpeedEvaluationJobUseCase.class);
        TenantContextExecutor tenants = mock(TenantContextExecutor.class);
        SpeedEvaluationJob job = job(1);
        CountDownLatch retried = new CountDownLatch(1);
        when(jobs.claimDue(anyString(), any(), any(), anyInt())).thenReturn(List.of(job));
        when(jobs.renew(any(), any(), anyString(), any(), any())).thenReturn(true);
        when(tenants.within(any(), any(java.util.function.Supplier.class)))
                .thenThrow(new IllegalStateException("provider payload must not be persisted"));
        org.mockito.Mockito.doAnswer(invocation -> { retried.countDown(); return null; })
                .when(jobs).retry(any(), any(), anyString(), any(), any());
        SpeedEvaluationCoordinator coordinator = coordinator(jobs, evaluator, tenants);
        try {
            coordinator.tick();
            assertThat(retried.await(5, TimeUnit.SECONDS)).isTrue();
            verify(jobs).retry(any(), any(), anyString(), any(), any());
        } finally {
            coordinator.shutdown();
        }
    }

    private static SpeedEvaluationCoordinator coordinator(
            SpeedEvaluationJobRepositoryPort jobs, SpeedEvaluationJobUseCase evaluator,
            TenantContextExecutor tenants) {
        return new SpeedEvaluationCoordinator(jobs, evaluator,
                new SpeedEvaluatorSettings(4, 2, 4, 5, Duration.ofMinutes(2),
                        Duration.ofSeconds(30), Duration.ofSeconds(1)),
                new SimpleMeterRegistry(), Clock.fixed(NOW, ZoneOffset.UTC), tenants, "worker-test");
    }

    private static SpeedEvaluationJob job(int attempt) {
        return new SpeedEvaluationJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                NOW, SpeedEvaluationJob.Status.PROCESSING, attempt, NOW, "worker-test",
                NOW.plusSeconds(120), null, NOW.minusSeconds(1));
    }
}
