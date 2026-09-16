package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.LiveTelemetryProjection;
import com.transportlogistics.app.tracking.application.GpsReliabilityEvaluationService;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.outbound.LiveTelemetryProjectionPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class TrackingLiveTelemetryProjectorTest {
    @Test
    void acknowledgesOnlyAfterSuccessfulProjection() {
        var port = mock(LiveTelemetryProjectionPort.class);
        var acknowledgment = mock(Acknowledgment.class);
        var event = event();
        when(port.project(new LiveTelemetryProjection(event, now())))
                .thenReturn(LiveTelemetryProjectionPort.ProjectionResult.UPDATED);

        projector(port).consume(record(event), acknowledgment);

        verify(port).project(new LiveTelemetryProjection(event, now()));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void mismatchAndRedisFailureNeverAcknowledge() {
        var port = mock(LiveTelemetryProjectionPort.class);
        var acknowledgment = mock(Acknowledgment.class);
        var event = event();
        var mismatched = record(event);
        mismatched.headers().remove("tenantId");
        mismatched.headers().add(new RecordHeader("tenantId",
                UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> projector(port).consume(mismatched, acknowledgment))
                .isInstanceOf(TrackingLiveTelemetryProjector.TelemetryContractException.class)
                .hasMessageNotContaining(event.latitude().toPlainString());
        verify(acknowledgment, never()).acknowledge();

        when(port.project(new LiveTelemetryProjection(event, now())))
                .thenThrow(new DependencyUnavailableException("REDIS", "unavailable", null));
        assertThatThrownBy(() -> projector(port).consume(record(event), acknowledgment))
                .isInstanceOf(DependencyUnavailableException.class);
        verify(acknowledgment, never()).acknowledge();
    }

    private static TrackingLiveTelemetryProjector projector(LiveTelemetryProjectionPort port) {
        var reliability = mock(GpsReliabilityEvaluationService.class);
        when(reliability.evaluateAndRecord(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(eligible());
        return new TrackingLiveTelemetryProjector(port, reliability, new SimpleMeterRegistry(),
                Clock.fixed(now(), ZoneOffset.UTC), "tracking.telemetry.ingested.v1",
                "tracking.telemetry.ingested.v2");
    }

    private static com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Assessment eligible() {
        return new com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Assessment(
                com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Trust.TRUSTED,
                com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Ordering.IN_ORDER,
                com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Connectivity.LIVE,
                com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ReliabilityState.NORMAL,
                Set.of(com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Quality.NORMAL),
                true, true);
    }

    private static ConsumerRecord<String, TrackingTelemetryIngestedV1> record(
            TrackingTelemetryIngestedV1 event) {
        var headers = new RecordHeaders();
        headers.add("tenantId", event.tenantId().toString().getBytes(StandardCharsets.UTF_8));
        headers.add("eventType", event.eventType().getBytes(StandardCharsets.UTF_8));
        headers.add("eventVersion", "1".getBytes(StandardCharsets.UTF_8));
        return new ConsumerRecord<>("tracking.telemetry.ingested.v1", 0, 1L, now().toEpochMilli(),
                org.apache.kafka.common.record.TimestampType.CREATE_TIME, 0, 0,
                event.tenantId() + ":" + event.vehicleId(), event, headers,
                java.util.Optional.empty());
    }

    private static TrackingTelemetryIngestedV1 event() {
        return new TrackingTelemetryIngestedV1(UUID.randomUUID(),
                TrackingTelemetryIngestedV1.TYPE, 1, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "FLESPI", "message-1", "a".repeat(64),
                new BigDecimal("6.9271"), new BigDecimal("79.8612"), null, null, null,
                null, EngineState.UNKNOWN, null, null,
                Instant.parse("2026-09-13T12:00:00Z"), now());
    }

    private static Instant now() {
        return Instant.parse("2026-09-13T12:00:05Z");
    }
}
