package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import com.transportlogistics.app.tracking.application.telemetry.LiveTelemetryProjection;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.ports.outbound.LiveTelemetryProjectionPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
final class TrackingLiveTelemetryProjector {
    private final LiveTelemetryProjectionPort liveState;
    private final MeterRegistry meters;
    private final Clock clock;

    TrackingLiveTelemetryProjector(
            LiveTelemetryProjectionPort liveState,
            MeterRegistry meters) {
        this(liveState, meters, Clock.systemUTC());
    }

    TrackingLiveTelemetryProjector(
            LiveTelemetryProjectionPort liveState,
            MeterRegistry meters,
            Clock clock) {
        this.liveState = liveState;
        this.meters = meters;
        this.clock = clock;
    }

    @KafkaListener(
            topics = "${app.tracking.kafka.topic:tracking.telemetry.ingested.v1}",
            groupId = "${app.tracking.kafka.live-projector-group:tracking-live-projector-v1}",
            containerFactory = "trackingLiveProjectorContainerFactory")
    void consume(
            ConsumerRecord<String, TrackingTelemetryIngestedV1> record,
            Acknowledgment acknowledgment) {
        TrackingTelemetryIngestedV1 event = record.value();
        validate(record, event);
        var result = liveState.project(new LiveTelemetryProjection(event, Instant.now(clock)));
        meters.counter("tracking.redis.live.projections", "result", result.name()).increment();
        acknowledgment.acknowledge();
    }

    private static void validate(
            ConsumerRecord<String, TrackingTelemetryIngestedV1> record,
            TrackingTelemetryIngestedV1 event) {
        if (event == null
                || !TrackingTelemetryIngestedV1.TYPE.equals(event.eventType())
                || event.eventVersion() != TrackingTelemetryIngestedV1.VERSION) {
            throw new TelemetryContractException("Unsupported telemetry event contract");
        }
        String expectedKey = event.tenantId() + ":" + event.vehicleId();
        if (!expectedKey.equals(record.key())
                || !event.tenantId().equals(uuidHeader(record, "tenantId"))
                || !event.eventType().equals(textHeader(record, "eventType"))
                || !Integer.toString(event.eventVersion()).equals(textHeader(record, "eventVersion"))) {
            throw new TelemetryContractException("Telemetry authority mismatch");
        }
    }

    private static UUID uuidHeader(ConsumerRecord<?, ?> record, String name) {
        try {
            return UUID.fromString(textHeader(record, name));
        } catch (RuntimeException exception) {
            throw new TelemetryContractException("Invalid telemetry authority header");
        }
    }

    private static String textHeader(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) {
            throw new TelemetryContractException("Missing telemetry authority header");
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    static final class TelemetryContractException extends IllegalArgumentException {
        TelemetryContractException(String message) {
            super(message);
        }
    }
}
