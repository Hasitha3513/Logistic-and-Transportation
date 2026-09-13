package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.outbound.HistoricalTelemetryStorePort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class TrackingHistoricalTelemetryPersisterTest {
    @Test
    void acknowledgesOnlyAfterStoreReturns() {
        var store = mock(HistoricalTelemetryStorePort.class);
        var acknowledgment = mock(Acknowledgment.class);
        var event = event(UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-09-13T12:00:00Z"));
        when(store.persist(List.of(event))).thenReturn(new HistoricalTelemetryStorePort.BatchResult(1, 0, 0));
        var consumer = new TrackingHistoricalTelemetryPersister(
                store, new SimpleMeterRegistry(), "tracking.telemetry.ingested.v1");

        consumer.consume(List.of(record(event)), acknowledgment);

        var order = inOrder(store, acknowledgment);
        order.verify(store).persist(List.of(event));
        order.verify(acknowledgment).acknowledge();
    }

    @Test
    void storageFailureLeavesBatchUnacknowledged() {
        var store = mock(HistoricalTelemetryStorePort.class);
        var acknowledgment = mock(Acknowledgment.class);
        var event = event(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        when(store.persist(List.of(event))).thenThrow(new DependencyUnavailableException(
                "TRACKING_HISTORY_UNAVAILABLE", "Historical telemetry storage is unavailable", null));
        var consumer = new TrackingHistoricalTelemetryPersister(
                store, new SimpleMeterRegistry(), "tracking.telemetry.ingested.v1");

        assertThatThrownBy(() -> consumer.consume(List.of(record(event)), acknowledgment))
                .isInstanceOf(DependencyUnavailableException.class);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void authorityMismatchFailsBeforePersistence() {
        var store = mock(HistoricalTelemetryStorePort.class);
        var acknowledgment = mock(Acknowledgment.class);
        var event = event(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        var record = record(event);
        record.headers().remove("tenantId");
        record.headers().add("tenantId", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        var consumer = new TrackingHistoricalTelemetryPersister(
                store, new SimpleMeterRegistry(), "tracking.telemetry.ingested.v1");

        assertThatThrownBy(() -> consumer.consume(List.of(record), acknowledgment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telemetry authority mismatch");
        verify(store, never()).persist(org.mockito.ArgumentMatchers.any());
        verify(acknowledgment, never()).acknowledge();
    }

    private static ConsumerRecord<String, TrackingTelemetryIngestedV1> record(
            TrackingTelemetryIngestedV1 event) {
        var record = new ConsumerRecord<String, TrackingTelemetryIngestedV1>(
                "tracking.telemetry.ingested.v1", 0, 1L,
                event.tenantId() + ":" + event.vehicleId(), event);
        record.headers().add("tenantId", event.tenantId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", event.eventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventVersion", "1".getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private static TrackingTelemetryIngestedV1 event(
            UUID tenant, UUID vehicle, Instant recordedAt) {
        String dedupe = "a".repeat(64);
        return new TrackingTelemetryIngestedV1(UUID.nameUUIDFromBytes(dedupe.getBytes()),
                TrackingTelemetryIngestedV1.TYPE, 1, tenant, vehicle, UUID.randomUUID(),
                "GENERIC", null, dedupe, BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ZERO,
                null, BigDecimal.ONE, null, EngineState.OFF, null, null,
                recordedAt, recordedAt.plusSeconds(1));
    }
}
