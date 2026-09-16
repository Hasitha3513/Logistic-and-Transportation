package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
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
class TrackingHistoricalTelemetryPersister {
    private final HistoricalTelemetryStorePort history;
    private final MeterRegistry meters;
    private final String topic;
    private final String v2Topic;

    @org.springframework.beans.factory.annotation.Autowired
    TrackingHistoricalTelemetryPersister(
            HistoricalTelemetryStorePort history,
            MeterRegistry meters,
            @Value("${app.tracking.kafka.topic:tracking.telemetry.ingested.v1}") String topic,
            @Value("${app.tracking.kafka.v2-topic:tracking.telemetry.ingested.v2}") String v2Topic) {
        this.history = history;
        this.meters = meters;
        this.topic = topic;
        this.v2Topic = v2Topic;
    }

    TrackingHistoricalTelemetryPersister(HistoricalTelemetryStorePort history, MeterRegistry meters,
            String topic) {
        this(history, meters, topic, "tracking.telemetry.ingested.v2");
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

    @KafkaListener(
            topics = "${app.tracking.kafka.v2-topic:tracking.telemetry.ingested.v2}",
            groupId = "${app.tracking.kafka.history-persister-group:tracking-telemetry-persister-group}",
            containerFactory = "trackingHistoryPersisterV2ContainerFactory")
    void consumeV2(
            List<ConsumerRecord<String, TrackingTelemetryIngestedV2>> records,
            Acknowledgment acknowledgment) {
        if (records.isEmpty() || records.size() > 500) {
            throw new IllegalArgumentException("Historical telemetry batch size must be 1..500");
        }
        records.forEach(record -> TrackingTelemetryContractValidator.validate(
                record, record.value(), v2Topic, TrackingTelemetryIngestedV2.VERSION));
        var result = history.persist(records.stream().map(ConsumerRecord::value).toList());
        meters.counter("tracking.timescale.history", "result", "persisted").increment(result.persisted());
        meters.counter("tracking.timescale.history", "result", "duplicate").increment(result.duplicate());
        meters.counter("tracking.timescale.history", "result", "reduced").increment(result.reduced());
        acknowledgment.acknowledge();
    }
}
