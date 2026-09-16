package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.tracking.application.telemetry.LiveTelemetryProjection;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.outbound.HistoricalTelemetryStorePort;
import com.transportlogistics.app.tracking.ports.outbound.LiveTelemetryProjectionPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class TrackingTelemetryV2ConsumerTest {

    @Test
    void v2HistoryAndLiveConsumersPreserveSignalsAndAcknowledgeAfterWork() {
        var event = event();
        var history = new RecordingHistory();
        var live = new RecordingLive();
        var historyAck = new RecordingAck();
        var liveAck = new RecordingAck();
        var persister = new TrackingHistoricalTelemetryPersister(
                history, new SimpleMeterRegistry(), "tracking.telemetry.ingested.v1",
                "tracking.telemetry.ingested.v2");
        var projector = new TrackingLiveTelemetryProjector(live, new SimpleMeterRegistry(),
                Clock.fixed(event.receivedAt(), ZoneOffset.UTC), "tracking.telemetry.ingested.v1",
                "tracking.telemetry.ingested.v2");

        persister.consumeV2(List.of(record(event, "tracking.telemetry.ingested.v2", "2")), historyAck);
        projector.consumeV2(record(event, "tracking.telemetry.ingested.v2", "2"), liveAck);

        assertThat(history.events).containsExactly(event);
        assertThat(live.projection.telemetry()).isSameAs(event);
        assertThat(event.tamperState()).isEqualTo(TrackingTelemetryIngestedV2.TamperState.CLEAR);
        assertThat(historyAck.acknowledged).isTrue();
        assertThat(liveAck.acknowledged).isTrue();
    }

    @Test
    void topicAndEnvelopeVersionMismatchFailBeforeAtomicWork() {
        var history = new RecordingHistory();
        var ack = new RecordingAck();
        var consumer = new TrackingHistoricalTelemetryPersister(
                history, new SimpleMeterRegistry(), "tracking.telemetry.ingested.v1",
                "tracking.telemetry.ingested.v2");

        assertThatThrownBy(() -> consumer.consumeV2(
                List.of(record(event(), "tracking.telemetry.ingested.v1", "2")), ack))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.consumeV2(
                List.of(record(event(), "tracking.telemetry.ingested.v2", "1")), ack))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(history.events).isEmpty();
        assertThat(ack.acknowledged).isFalse();
    }

    private static ConsumerRecord<String, TrackingTelemetryIngestedV2> record(
            TrackingTelemetryIngestedV2 event, String topic, String version) {
        var record = new ConsumerRecord<String, TrackingTelemetryIngestedV2>(topic, 0, 1L,
                event.tenantId() + ":" + event.vehicleId(), event);
        record.headers().add("tenantId", bytes(event.tenantId().toString()));
        record.headers().add("eventType", bytes(event.eventType()));
        record.headers().add("eventVersion", bytes(version));
        return record;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static TrackingTelemetryIngestedV2 event() {
        return new TrackingTelemetryIngestedV2(UUID.randomUUID(), TrackingTelemetryIngestedV2.TYPE, 2,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "FLESPI", "fictional-v2",
                "b".repeat(64), new BigDecimal("6.9"), new BigDecimal("79.8"), BigDecimal.ZERO,
                null, null, null, EngineState.UNKNOWN, null, null,
                Instant.parse("2026-09-16T10:00:00Z"), Instant.parse("2026-09-16T10:00:01Z"),
                TrackingTelemetryIngestedV2.TamperState.CLEAR, BigDecimal.ZERO,
                new BigDecimal("3.92"), TrackingTelemetryIngestedV2.ExternalPowerState.CONNECTED,
                TrackingTelemetryIngestedV2.BatteryChargingState.CHARGING);
    }

    private static final class RecordingHistory implements HistoricalTelemetryStorePort {
        private final List<com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent>
                events = new ArrayList<>();

        @Override
        public BatchResult persist(List<? extends com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent> telemetry) {
            events.addAll(telemetry);
            return new BatchResult(telemetry.size(), 0, 0);
        }

        @Override
        public List<com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry> find(
                UUID tenantId, UUID vehicleId, Instant fromInclusive, Instant toExclusive, int limit) {
            return List.of();
        }
    }

    private static final class RecordingLive implements LiveTelemetryProjectionPort {
        private LiveTelemetryProjection projection;
        @Override public ProjectionResult project(LiveTelemetryProjection value) {
            projection = value;
            return ProjectionResult.UPDATED;
        }
        @Override public java.util.Optional<LiveTelemetryProjection> find(UUID tenantId, UUID vehicleId) {
            return java.util.Optional.empty();
        }
        @Override public List<LiveTelemetryProjection> findLive(UUID tenantId, Instant now, int limit) {
            return List.of();
        }
    }

    private static final class RecordingAck implements Acknowledgment {
        private boolean acknowledged;
        @Override public void acknowledge() { acknowledged = true; }
    }
}
