package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.ports.outbound.HistoricalTelemetryStorePort;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
final class TrackingHistoricalTelemetryPersister {
    private final HistoricalTelemetryStorePort history;
    private final MeterRegistry meters;
    private final String topic;

    TrackingHistoricalTelemetryPersister(
            HistoricalTelemetryStorePort history,
            MeterRegistry meters,
            @Value("${app.tracking.kafka.topic:tracking.telemetry.ingested.v1}") String topic) {
        this.history = history;
        this.meters = meters;
        this.topic = topic;
    }

    @KafkaListener(
            topics = "${app.tracking.kafka.topic:tracking.telemetry.ingested.v1}",
            groupId = "${app.tracking.kafka.history-persister-group:tracking-telemetry-persister-group}",
            containerFactory = "trackingHistoryPersisterContainerFactory")
    void consume(
            List<ConsumerRecord<String, TrackingTelemetryIngestedV1>> records,
            Acknowledgment acknowledgment) {
        if (records.isEmpty() || records.size() > 500) {
            throw new IllegalArgumentException("Historical telemetry batch size must be 1..500");
        }
        records.forEach(record -> TrackingTelemetryContractValidator.validate(
                record, record.value(), topic));
        var result = history.persist(records.stream().map(ConsumerRecord::value).toList());
        meters.counter("tracking.timescale.history", "result", "persisted")
                .increment(result.persisted());
        meters.counter("tracking.timescale.history", "result", "duplicate")
                .increment(result.duplicate());
        meters.counter("tracking.timescale.history", "result", "reduced")
                .increment(result.reduced());
        acknowledgment.acknowledge();
    }
}
