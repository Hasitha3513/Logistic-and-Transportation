package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV3;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.outbound.HistoricalTelemetryStorePort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class TrackingTelemetryV3ConsumerTest {
    @Test
    void acknowledgesOnlyAfterTheCompleteV3BatchPersists() {
        var history = new RecordingHistory();
        var consumer = new TrackingHistoricalTelemetryPersister(history,
                new SimpleMeterRegistry(), "v1", "v2", TrackingTelemetryIngestedV3.TOPIC);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        TrackingTelemetryIngestedV3 event = event();

        consumer.consumeV3(List.of(record(event)), acknowledgment);

        assertThat(history.events).hasSize(1);
        assertThat(history.events.getFirst()).isEqualTo(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void storageFailureLeavesTheKafkaRecordUnacknowledgedForRetry() {
        HistoricalTelemetryStorePort unavailable = new RecordingHistory() {
            @Override
            public BatchResult persist(
                    List<? extends com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent>
                            telemetry) {
                throw new DependencyUnavailableException("TRACKING_HISTORY_UNAVAILABLE",
                        "Historical telemetry storage is unavailable", null);
            }
        };
        var consumer = new TrackingHistoricalTelemetryPersister(unavailable,
                new SimpleMeterRegistry(), "v1", "v2", TrackingTelemetryIngestedV3.TOPIC);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        assertThatThrownBy(() -> consumer.consumeV3(List.of(record(event())), acknowledgment))
                .isInstanceOf(DependencyUnavailableException.class);
        verify(acknowledgment, never()).acknowledge();
    }

    private static ConsumerRecord<String, TrackingTelemetryIngestedV3> record(
            TrackingTelemetryIngestedV3 event) {
        var record = new ConsumerRecord<String, TrackingTelemetryIngestedV3>(
                TrackingTelemetryIngestedV3.TOPIC, 0, 1,
                event.tenantId() + ":" + event.vehicleId(), event);
        record.headers().add(new RecordHeader("tenantId",
                event.tenantId().toString().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("eventType",
                event.eventType().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("eventVersion", "3".getBytes(StandardCharsets.UTF_8)));
        return record;
    }

    private static TrackingTelemetryIngestedV3 event() {
        return new TrackingTelemetryIngestedV3(UUID.randomUUID(), TrackingTelemetryIngestedV3.TYPE,
                3, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "TEST_FIXTURE", null,
                "a".repeat(64), new BigDecimal("6.9"), new BigDecimal("79.8"), BigDecimal.ZERO,
                null, new BigDecimal("4"), null, EngineState.ON, null, null,
                Instant.parse("2026-09-18T10:00:00Z"), Instant.parse("2026-09-18T10:00:01Z"),
                null, null, null, null, null, TrackingTelemetryIngestedV3.IgnitionState.ON,
                TrackingTelemetryIngestedV3.EngineRunningState.RUNNING,
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_CAN);
    }

    private static class RecordingHistory implements HistoricalTelemetryStorePort {
        private List<? extends com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent>
                events = List.of();

        @Override
        public BatchResult persist(
                List<? extends com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent>
                        telemetry) {
            events = telemetry;
            return new BatchResult(telemetry.size(), 0, 0);
        }

        @Override
        public List<HistoricalTelemetry> find(UUID tenantId, UUID vehicleId, Instant fromInclusive,
                Instant toExclusive, int limit) {
            return List.of();
        }
    }
}
