package com.transportlogistics.app.tracking.adapters.inbound.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.tracking.adapters.configuration.ProviderCoordinatorSettings;
import com.transportlogistics.app.tracking.application.provider.ClaimedProviderConnection;
import com.transportlogistics.app.tracking.application.provider.ConfigurationValidation;
import com.transportlogistics.app.tracking.application.provider.ConnectionTestResult;
import com.transportlogistics.app.tracking.application.provider.DiscoveryResult;
import com.transportlogistics.app.tracking.application.provider.FetchResult;
import com.transportlogistics.app.tracking.application.provider.ProviderCapabilities;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionExecution;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderDiscoveryRequest;
import com.transportlogistics.app.tracking.application.provider.ProviderFetchRequest;
import com.transportlogistics.app.tracking.application.provider.ProviderHealth;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderAdapterRegistry;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderIngestionPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDeviceProviderBindingStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderAdapter;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderExecutionStore;
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

class ProviderPollingCoordinatorTest {
    private static final Instant NOW = Instant.parse("2026-09-09T10:00:00Z");

    @Test
    void saturatedDirectHandoffNeverExceedsWorkerBoundAndReleasesUnstartedLease() throws Exception {
        TrackingProviderExecutionStore executions = mock(TrackingProviderExecutionStore.class);
        TrackingDeviceProviderBindingStore bindings = mock(TrackingDeviceProviderBindingStore.class);
        TrackingProviderIngestionPort ingestion = mock(TrackingProviderIngestionPort.class);
        IntegrationSecretResolver secrets = mock(IntegrationSecretResolver.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TrackingProviderAdapter adapter = new BlockingAdapter(entered, release);
        var registry = new TrackingProviderAdapterRegistry(List.of(adapter));
        ClaimedProviderConnection first = connection("first");
        ClaimedProviderConnection second = connection("second");
        when(executions.claimDueConnections(anyString(), eq(NOW), any(), eq(2)))
                .thenReturn(List.of(first, second));
        when(executions.reloadActive(any(), anyString(), any())).thenAnswer(invocation -> {
            ProviderConnectionId id = invocation.getArgument(0);
            return Optional.of(id.equals(first.id()) ? first : second);
        });
        when(executions.renewLease(any(), anyString(), any(), any())).thenReturn(true);
        TrackingDeviceProviderBinding binding = mock(TrackingDeviceProviderBinding.class);
        when(binding.externalDeviceReference()).thenReturn("device-reference");
        when(binding.tenantId()).thenReturn(first.tenantId());
        when(binding.id()).thenReturn(UUID.randomUUID());
        when(binding.updatedBy()).thenReturn(UUID.randomUUID());
        when(executions.findDueActiveBindings(any(), any(), any(), any(Integer.class)))
                .thenReturn(List.of(binding), List.of());
        when(executions.releaseSuccess(any(), anyString(), any(), any(), any())).thenReturn(true);
        when(executions.releaseFailure(any(), anyString(), any(), any(), anyString())).thenReturn(true);
        when(secrets.resolve(anyString())).thenReturn(Optional.of("temporary".toCharArray()));
        ProviderPollingCoordinator coordinator = coordinator(
                executions, bindings, registry, ingestion, secrets, settings(1, 0));
        try {
            coordinator.tick();
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(coordinator.workerPoolSize()).isEqualTo(1);
            assertThat(coordinator.workerQueueSize()).isZero();
            verify(executions).releaseFailure(
                    eq(second.id()), anyString(), any(), any(), eq("WORKER_SATURATED"));
            release.countDown();
        } finally {
            release.countDown();
            coordinator.shutdown();
        }
        System.out.println("US48_CS04_WORKER_SATURATION_RACE=1/1 PASS");
    }

    @Test
    void featureSettingsRequireBoundedWorkersLeaseAndDeadline() {
        ProviderCoordinatorSettings settings = settings(4, 8);
        assertThat(settings.maxWorkers()).isEqualTo(4);
        assertThat(settings.queueCapacity()).isEqualTo(8);
        assertThat(settings.leaseDuration()).isGreaterThan(settings.fetchDeadline());
        assertThat(settings.maxDevicesPerFetch()).isEqualTo(100);
    }

    private static ProviderPollingCoordinator coordinator(
            TrackingProviderExecutionStore executions,
            TrackingDeviceProviderBindingStore bindings,
            TrackingProviderAdapterRegistry registry,
            TrackingProviderIngestionPort ingestion,
            IntegrationSecretResolver secrets,
            ProviderCoordinatorSettings settings) {
        return new ProviderPollingCoordinator(
                executions, bindings, registry, ingestion, secrets, settings,
                new ProviderCoordinatorState(), new SimpleMeterRegistry(),
                Clock.fixed(NOW, ZoneOffset.UTC), "test-instance");
    }

    private static ProviderCoordinatorSettings settings(int workers, int queue) {
        return new ProviderCoordinatorSettings(
                2, workers, queue, 100, 2, 1, 1_048_576,
                Duration.ofMinutes(2), Duration.ofSeconds(45), Duration.ofSeconds(5),
                Duration.ofSeconds(2));
    }

    private static ClaimedProviderConnection connection(String suffix) {
        return new ClaimedProviderConnection(
                new ProviderConnectionId(UUID.nameUUIDFromBytes(suffix.getBytes())), UUID.randomUUID(),
                ProviderType.of("FIXTURE"), "FIXTURE", "reference-" + suffix, null,
                ProviderSafeConfiguration.empty(), 5, 100, "test-instance", NOW.plusSeconds(120));
    }

    private static final class BlockingAdapter implements TrackingProviderAdapter {
        private final CountDownLatch entered;
        private final CountDownLatch release;

        private BlockingAdapter(CountDownLatch entered, CountDownLatch release) {
            this.entered = entered;
            this.release = release;
        }

        @Override public ProviderType providerType() { return ProviderType.of("FIXTURE"); }
        @Override public ProviderCapabilities capabilities() { return ProviderCapabilities.of(); }
        @Override public ConfigurationValidation validateConfiguration(
                com.transportlogistics.app.tracking.application.provider.ProviderConnectionConfiguration c) {
            return ConfigurationValidation.valid();
        }
        @Override public ConnectionTestResult testConnection(
                ProviderConnectionExecution connection, char[] secret) {
            throw new UnsupportedOperationException();
        }
        @Override public FetchResult fetchPositions(ProviderFetchRequest request, char[] secret) {
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return FetchResult.empty();
        }
        @Override public DiscoveryResult discoverDevices(
                ProviderDiscoveryRequest request, char[] secret) {
            return DiscoveryResult.unsupported();
        }
        @Override public ProviderHealth health(ProviderConnectionExecution connection) {
            throw new UnsupportedOperationException();
        }
    }
}
