package com.transportlogistics.app.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionEvidenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryCapabilityLookupPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class GpsReliabilityEvaluationServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");

    @Test
    void unsupportedOptionalSignalsDoNotCreateTamperOrBatteryIncidents() {
        var capabilities = mock(TelemetryCapabilityLookupPort.class);
        when(capabilities.resolveAll(any(), any(), any(), any())).thenReturn(Map.of());
        var episodes = mock(GpsExceptionRepositoryPort.class);
        when(episodes.findActiveByDeviceForUpdate(any(), any())).thenReturn(List.of());
        var evidence = mock(GpsExceptionEvidenceRepositoryPort.class);
        var service = service(capabilities, episodes, evidence);

        var assessment = service.evaluateAndRecord(event("a", new BigDecimal("10"),
                TrackingTelemetryIngestedV2.TamperState.DETECTED), Optional.empty(), NOW);

        assertThat(assessment.latestTrustedEligible()).isTrue();
        verify(episodes, never()).save(any());
        verify(evidence, never()).append(any());
    }

    @Test
    void supportedTamperCreatesOneHighEpisodeAndDuplicateEvidenceIsIdempotent() {
        var capabilities = mock(TelemetryCapabilityLookupPort.class);
        when(capabilities.resolveAll(any(), any(), any(), any())).thenReturn(Map.of(
                com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability.TAMPER,
                TelemetryCapabilityState.SUPPORTED));
        var episodes = mock(GpsExceptionRepositoryPort.class);
        when(episodes.findActiveByDeviceForUpdate(any(), any())).thenReturn(List.of());
        var evidence = mock(GpsExceptionEvidenceRepositoryPort.class);
        when(evidence.append(any())).thenReturn(true);
        AtomicReference<GpsExceptionEpisode> saved = new AtomicReference<>();
        when(episodes.save(any())).thenAnswer(call -> {
            saved.set(call.getArgument(0));
            return call.getArgument(0);
        });
        var service = service(capabilities, episodes, evidence);

        var assessment = service.evaluateAndRecord(event("b", new BigDecimal("80"),
                TrackingTelemetryIngestedV2.TamperState.DETECTED), Optional.empty(), NOW);

        assertThat(assessment.latestTrustedEligible()).isFalse();
        assertThat(saved.get().type()).isEqualTo(ExceptionType.DEVICE_TAMPER);
        assertThat(saved.get().severity()).isEqualTo(
                com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Severity.HIGH);
        verify(evidence).append(any());
    }

    private static GpsReliabilityEvaluationService service(
            TelemetryCapabilityLookupPort capabilities, GpsExceptionRepositoryPort episodes,
            GpsExceptionEvidenceRepositoryPort evidence) {
        GpsExceptionTransactionPort transaction = new GpsExceptionTransactionPort() {
            @Override
            public <T> T execute(java.util.function.Supplier<T> operation) {
                return operation.get();
            }
        };
        return new GpsReliabilityEvaluationService(capabilities, episodes, evidence, transaction,
                org.mockito.Mockito.mock(com.transportlogistics.app.tracking.ports.outbound.GpsExceptionEventPublisherPort.class));
    }

    private static TrackingTelemetryIngestedV2 event(
            String identity, BigDecimal battery, TrackingTelemetryIngestedV2.TamperState tamper) {
        return new TrackingTelemetryIngestedV2(UUID.randomUUID(), TrackingTelemetryIngestedV2.TYPE, 2,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "FLESPI", identity,
                identity.repeat(64), new BigDecimal("6.9271"), new BigDecimal("79.8612"),
                BigDecimal.ZERO, null, new BigDecimal("10"), null, EngineState.UNKNOWN,
                null, null, NOW.minusSeconds(1), NOW, tamper, battery, new BigDecimal("4.1"),
                TrackingTelemetryIngestedV2.ExternalPowerState.CONNECTED,
                TrackingTelemetryIngestedV2.BatteryChargingState.CHARGING);
    }
}
