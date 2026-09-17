package com.transportlogistics.app.tracking.adapters.inbound.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.provider.ClaimedProviderConnection;
import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.NormalizedPositionCandidate;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryStreamPublisherPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderExecutionStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JdbcTrackingProviderIngestionAdapterTest {
    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");
    private static final UUID TENANT = UUID.fromString("55000000-0000-0000-0000-000000000001");
    private static final UUID CONNECTION = UUID.fromString("55000000-0000-0000-0000-000000000002");
    private static final UUID BINDING = UUID.fromString("55000000-0000-0000-0000-000000000003");
    private static final UUID DEVICE = UUID.fromString("55000000-0000-0000-0000-000000000004");
    private static final UUID VEHICLE = UUID.fromString("55000000-0000-0000-0000-000000000005");

    private final TrackingProviderExecutionStore executions = mock(TrackingProviderExecutionStore.class);
    private final TrackingStore tracking = mock(TrackingStore.class);
    private final TelemetryStreamPublisherPort stream = mock(TelemetryStreamPublisherPort.class);
    private final JdbcTrackingProviderIngestionAdapter adapter =
            new JdbcTrackingProviderIngestionAdapter(executions, tracking, stream, Duration.ofSeconds(10));

    @Test
    void publishesCanonicalV2WithTrustedTenantBindingAndOptionalSignals() {
        arrangeAuthority();
        var candidate = candidate();

        var result = adapter.ingest(new ProviderConnectionId(CONNECTION), "lease",
                List.of(candidate), NOW);

        assertThat(result).singleElement().satisfies(outcome -> {
            assertThat(outcome.bindingId()).isEqualTo(BINDING);
            assertThat(outcome.result().name()).isEqualTo("ACCEPTED");
        });
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TelemetryStreamPublisherPort.PublicationRequest>> requests =
                ArgumentCaptor.forClass(List.class);
        verify(stream).publishBatchDurably(requests.capture(), any());
        var request = requests.getValue().getFirst();
        assertThat(request.partitionKey()).isEqualTo(TENANT + ":" + VEHICLE);
        assertThat(request.event()).isInstanceOfSatisfying(TrackingTelemetryIngestedV2.class, event -> {
            assertThat(event.tenantId()).isEqualTo(TENANT);
            assertThat(event.deviceId()).isEqualTo(DEVICE);
            assertThat(event.vehicleId()).isEqualTo(VEHICLE);
            assertThat(event.tamperState()).isEqualTo(TrackingTelemetryIngestedV2.TamperState.DETECTED);
            assertThat(event.batteryLevelPercent()).isEqualByComparingTo("75.5");
            assertThat(event.externalPowerState())
                    .isEqualTo(TrackingTelemetryIngestedV2.ExternalPowerState.CONNECTED);
        });
    }

    @Test
    void publicationFailurePropagatesSoCoordinatorCannotAdvanceWatermark() {
        arrangeAuthority();
        doThrow(new DependencyUnavailableException(
                "TRACKING_KAFKA_UNAVAILABLE", "Telemetry stream is unavailable", null))
                .when(stream).publishBatchDurably(any(), any());

        assertThatThrownBy(() -> adapter.ingest(new ProviderConnectionId(CONNECTION), "lease",
                List.of(candidate()), NOW))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessage("Telemetry stream is unavailable");
    }

    @Test
    void rejectsForeignOrMissingSourceTimeAuthorityWithoutPublication() {
        when(executions.reloadActive(any(), anyString(), any())).thenReturn(Optional.of(connection()));
        when(executions.lockActiveBindingForIngestion(any(), any(), anyString(), anyString(), any()))
                .thenReturn(Optional.of(binding()));
        when(tracking.ingressDeviceAuthority(any(), any(), anyString(), any()))
                .thenReturn(Optional.empty());

        var result = adapter.ingest(new ProviderConnectionId(CONNECTION), "lease",
                List.of(candidate()), NOW);

        assertThat(result).singleElement().satisfies(outcome ->
                assertThat(outcome.result().name()).isEqualTo("REJECTED"));
        verify(stream, org.mockito.Mockito.never()).publishBatchDurably(any(), any());
    }

    private void arrangeAuthority() {
        when(executions.reloadActive(any(), anyString(), any())).thenReturn(Optional.of(connection()));
        when(executions.lockActiveBindingForIngestion(any(), any(), anyString(), anyString(), any()))
                .thenReturn(Optional.of(binding()));
        when(tracking.ingressDeviceAuthority(TENANT, CONNECTION, "device-55", NOW))
                .thenReturn(Optional.of(new TrackingStore.IngressDeviceAuthority(DEVICE, VEHICLE)));
        when(stream.publishBatchDurably(any(), any())).thenAnswer(invocation -> {
            List<?> requests = invocation.getArgument(0);
            return requests.stream().map(ignored ->
                    new TelemetryStreamPublisherPort.Publication(0, 1)).toList();
        });
    }

    private static ClaimedProviderConnection connection() {
        return new ClaimedProviderConnection(new ProviderConnectionId(CONNECTION), TENANT,
                ProviderType.of("FLESPI"), "FLESPI", "secret-reference", null,
                ProviderSafeConfiguration.empty(), 30, 100, "lease", NOW.plusSeconds(60));
    }

    private static TrackingDeviceProviderBinding binding() {
        return new TrackingDeviceProviderBinding(BINDING, TENANT, DEVICE,
                new ProviderConnectionId(CONNECTION), "device-55", ProviderSafeConfiguration.empty(),
                DeviceProviderBindingLifecycle.ACTIVE, null, null, null,
                NOW, DEVICE, NOW, DEVICE, 7);
    }

    private static NormalizedPositionCandidate candidate() {
        return new NormalizedPositionCandidate("device-55", NOW,
                new BigDecimal("6.9271"), new BigDecimal("79.8612"), new BigDecimal("4.0"),
                new BigDecimal("35.5"), new BigDecimal("180"), new BigDecimal("12"),
                EngineState.ON, new BigDecimal("1000"), new BigDecimal("200"), "message-55", 9L,
                TrackingTelemetryIngestedV2.TamperState.DETECTED, new BigDecimal("75.5"),
                new BigDecimal("12.6"), TrackingTelemetryIngestedV2.ExternalPowerState.CONNECTED,
                TrackingTelemetryIngestedV2.BatteryChargingState.CHARGING);
    }
}
