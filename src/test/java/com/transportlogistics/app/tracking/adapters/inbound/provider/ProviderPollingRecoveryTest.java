package com.transportlogistics.app.tracking.adapters.inbound.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.tracking.adapters.configuration.ProviderCoordinatorSettings;
import com.transportlogistics.app.tracking.application.provider.ClaimedProviderConnection;
import com.transportlogistics.app.tracking.application.provider.ConfigurationValidation;
import com.transportlogistics.app.tracking.application.provider.ConnectionTestResult;
import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.DiscoveryResult;
import com.transportlogistics.app.tracking.application.provider.FetchResult;
import com.transportlogistics.app.tracking.application.provider.ProviderCapabilities;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionExecution;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderDiscoveryRequest;
import com.transportlogistics.app.tracking.application.provider.ProviderFetchRequest;
import com.transportlogistics.app.tracking.application.provider.ProviderHealth;
import com.transportlogistics.app.tracking.application.provider.ProviderPollingFailure;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderAdapterRegistry;
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
import org.junit.jupiter.api.Test;

class ProviderPollingRecoveryTest {
    private static final Instant NOW = Instant.parse("2026-09-18T10:00:00Z");

    @Test
    void preservesSafeProviderFailureCategoryWithoutAdvancingProgress() {
        Fixture fixture = new Fixture(new FailingAdapter("provider_response_overflow"), "overflow");
        try {
            fixture.coordinator.tick();
            verify(fixture.executions, timeout(5_000)).releaseFailure(
                    eq(fixture.connection.id()), anyString(), any(), any(),
                    eq("PROVIDER_RESPONSE_OVERFLOW"));
            verify(fixture.bindings, never()).updateWatermark(
                    any(), any(), any(Long.class), any(), any(), any(), any(), any());
            verify(fixture.executions, never()).releaseSuccess(any(), anyString(), any(), any(), any());
        } finally {
            fixture.coordinator.shutdown();
        }
    }

    @Test
    void emptyReachableResponseRecordsPollWithoutManufacturingTelemetry() {
        Fixture fixture = new Fixture(new EmptyAdapter(), "empty");
        when(fixture.executions.findDueActiveBindings(any(), any(), any(), any(Integer.class)))
                .thenReturn(List.of(fixture.binding), List.of());
        when(fixture.executions.releaseSuccess(any(), anyString(), any(), any(), any())).thenReturn(true);
        try {
            fixture.coordinator.tick();
            verify(fixture.executions, timeout(5_000)).releaseSuccess(
                    eq(fixture.connection.id()), anyString(), any(), any(), eq(null));
            verify(fixture.ingestion, never()).ingest(any(), anyString(), any(), any());
            verify(fixture.bindings, timeout(5_000)).updateNextPoll(
                    any(), any(), any(Long.class), any(), any(), any());
        } finally {
            fixture.coordinator.shutdown();
        }
    }

    private static final class Fixture {
        private final TrackingProviderExecutionStore executions = mock(TrackingProviderExecutionStore.class);
        private final TrackingDeviceProviderBindingStore bindings = mock(TrackingDeviceProviderBindingStore.class);
        private final TrackingProviderIngestionPort ingestion = mock(TrackingProviderIngestionPort.class);
        private final IntegrationSecretResolver secrets = mock(IntegrationSecretResolver.class);
        private final ClaimedProviderConnection connection;
        private final TrackingDeviceProviderBinding binding;
        private final ProviderPollingCoordinator coordinator;

        private Fixture(TrackingProviderAdapter adapter, String suffix) {
            connection = connection(suffix);
            binding = binding(connection);
            when(executions.claimDueConnections(anyString(), eq(NOW), any(), eq(2)))
                    .thenReturn(List.of(connection));
            when(executions.reloadActive(any(), anyString(), any())).thenReturn(Optional.of(connection));
            when(executions.renewLease(any(), anyString(), any(), any())).thenReturn(true);
            when(executions.findDueActiveBindings(any(), any(), any(), any(Integer.class)))
                    .thenReturn(List.of(binding));
            when(executions.releaseFailure(any(), anyString(), any(), any(), anyString())).thenReturn(true);
            when(secrets.resolve(anyString())).thenReturn(Optional.of("temporary".toCharArray()));
            coordinator = new ProviderPollingCoordinator(
                    executions, bindings, new TrackingProviderAdapterRegistry(List.of(adapter)),
                    ingestion, secrets, settings(), new ProviderCoordinatorState(),
                    new SimpleMeterRegistry(), Clock.fixed(NOW, ZoneOffset.UTC), "test-instance");
        }
    }

    private static ProviderCoordinatorSettings settings() {
        return new ProviderCoordinatorSettings(2, 1, 0, 100, 2, 1, 1_048_576,
                Duration.ofMinutes(2), Duration.ofSeconds(45), Duration.ofSeconds(5),
                Duration.ofSeconds(2));
    }

    private static ClaimedProviderConnection connection(String suffix) {
        return new ClaimedProviderConnection(
                new ProviderConnectionId(UUID.nameUUIDFromBytes(suffix.getBytes())), UUID.randomUUID(),
                ProviderType.of("FIXTURE"), "FIXTURE", "reference-" + suffix, null,
                ProviderSafeConfiguration.empty(), 5, 100, "test-instance", NOW.plusSeconds(120));
    }

    private static TrackingDeviceProviderBinding binding(ClaimedProviderConnection connection) {
        return new TrackingDeviceProviderBinding(
                UUID.randomUUID(), connection.tenantId(), UUID.randomUUID(), connection.id(),
                "device-reference", ProviderSafeConfiguration.empty(),
                DeviceProviderBindingLifecycle.ACTIVE, null, null, null,
                NOW, UUID.randomUUID(), NOW, UUID.randomUUID(), 1);
    }

    private static class EmptyAdapter implements TrackingProviderAdapter {
        @Override public ProviderType providerType() { return ProviderType.of("FIXTURE"); }
        @Override public ProviderCapabilities capabilities() { return ProviderCapabilities.of(); }
        @Override public ConfigurationValidation validateConfiguration(
                com.transportlogistics.app.tracking.application.provider.ProviderConnectionConfiguration value) {
            return ConfigurationValidation.valid();
        }
        @Override public ConnectionTestResult testConnection(
                ProviderConnectionExecution connection, char[] secret) { return ConnectionTestResult.passed(); }
        @Override public FetchResult fetchPositions(ProviderFetchRequest request, char[] secret) {
            return FetchResult.empty();
        }
        @Override public DiscoveryResult discoverDevices(
                ProviderDiscoveryRequest request, char[] secret) { return DiscoveryResult.unsupported(); }
        @Override public ProviderHealth health(ProviderConnectionExecution connection) {
            return ProviderHealth.unknown();
        }
    }

    private static final class FailingAdapter extends EmptyAdapter {
        private final String safeCode;
        private FailingAdapter(String safeCode) { this.safeCode = safeCode; }
        @Override public FetchResult fetchPositions(ProviderFetchRequest request, char[] secret) {
            throw new SafeFailure(safeCode);
        }
    }

    private static final class SafeFailure extends RuntimeException implements ProviderPollingFailure {
        private static final long serialVersionUID = 1L;
        private final String safeCode;
        private SafeFailure(String safeCode) { this.safeCode = safeCode; }
        @Override public String safeCode() { return safeCode; }
    }
}
