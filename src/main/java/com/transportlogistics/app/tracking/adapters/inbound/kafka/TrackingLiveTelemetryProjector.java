package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import com.transportlogistics.app.tracking.application.telemetry.LiveTelemetryProjection;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.ports.outbound.LiveTelemetryProjectionPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
class TrackingLiveTelemetryProjector {
    private final LiveTelemetryProjectionPort liveState;
    private final MeterRegistry meters;
    private final Clock clock;
    private final String topic;

    TrackingLiveTelemetryProjector(
            LiveTelemetryProjectionPort liveState,
            MeterRegistry meters,
            @org.springframework.beans.factory.annotation.Value(
                    "${app.tracking.kafka.topic:tracking.telemetry.ingested.v1}") String topic) {
        this(liveState, meters, Clock.systemUTC(), topic);
    }

    TrackingLiveTelemetryProjector(
            LiveTelemetryProjectionPort liveState,
            MeterRegistry meters,
            Clock clock,
            String topic) {
        this.liveState = liveState;
        this.meters = meters;
        this.clock = clock;
        this.topic = topic;
    }

    TrackingLiveTelemetryProjector(
            LiveTelemetryProjectionPort liveState,
            MeterRegistry meters,
            Clock clock) {
        this(liveState, meters, clock, "tracking.telemetry.ingested.v1");
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

    private void validate(
            ConsumerRecord<String, TrackingTelemetryIngestedV1> record,
            TrackingTelemetryIngestedV1 event) {
        try {
            TrackingTelemetryContractValidator.validate(record, event, topic);
        } catch (TrackingTelemetryContractValidator.TelemetryContractException exception) {
            throw new TelemetryContractException(exception.getMessage());
        }
    }

    static final class TelemetryContractException extends IllegalArgumentException {
        TelemetryContractException(String message) {
            super(message);
        }
    }

}
