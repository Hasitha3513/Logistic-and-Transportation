package com.transportlogistics.app.tracking.adapters.outbound.kafka;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryStreamPublisherPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
final class KafkaTelemetryStreamPublisher implements TelemetryStreamPublisherPort {
    private final KafkaTemplate<String, TrackingTelemetryIngestedV1> kafka;
    private final MeterRegistry meters;
    private final String topic;

    KafkaTelemetryStreamPublisher(
            KafkaTemplate<String, TrackingTelemetryIngestedV1> kafka,
            MeterRegistry meters,
            @Value("${app.tracking.kafka.topic:tracking.telemetry.ingested.v1}") String topic) {
        this.kafka = kafka;
        this.meters = meters;
        this.topic = topic;
    }

    @Override
    public Publication publishDurably(
            String key,
            TrackingTelemetryIngestedV1 event,
            String correlationId,
            Duration timeout) {
        var record = new ProducerRecord<String, TrackingTelemetryIngestedV1>(topic, key, event);
        header(record, "tenantId", event.tenantId().toString());
        header(record, "eventType", event.eventType());
        header(record, "eventVersion", Integer.toString(event.eventVersion()));
        if (correlationId != null && !correlationId.isBlank() && correlationId.length() <= 128) {
            header(record, "correlationId", correlationId);
        }
        Timer.Sample sample = Timer.start(meters);
        try {
            var metadata = kafka.send(record).get(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .getRecordMetadata();
            meters.counter("tracking.kafka.publications.durable").increment();
            return new Publication(metadata.partition(), metadata.offset());
        } catch (TimeoutException exception) {
            meters.counter("tracking.kafka.publications.timeout").increment();
            throw unavailable("TRACKING_KAFKA_ACK_TIMEOUT", "Telemetry stream acknowledgement timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            meters.counter("tracking.kafka.publications.failure").increment();
            throw unavailable("TRACKING_KAFKA_UNAVAILABLE", "Telemetry stream is unavailable");
        } catch (Exception exception) {
            meters.counter("tracking.kafka.publications.failure").increment();
            throw unavailable("TRACKING_KAFKA_UNAVAILABLE", "Telemetry stream is unavailable");
        } finally {
            sample.stop(meters.timer("tracking.kafka.acknowledgement.latency"));
        }
    }

    private static void header(ProducerRecord<?, ?> record, String name, String value) {
        record.headers().add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8)));
    }

    private static DependencyUnavailableException unavailable(String code, String message) {
        return new DependencyUnavailableException(code, message, null);
    }
}
