package com.transportlogistics.app.tracking.adapters.outbound.kafka;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryStreamPublisherPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
final class KafkaTelemetryStreamPublisher implements TelemetryStreamPublisherPort {
    private final KafkaTemplate<String, Object> kafka;
    private final MeterRegistry meters;
    private final String v1Topic;
    private final String v2Topic;

    @org.springframework.beans.factory.annotation.Autowired
    KafkaTelemetryStreamPublisher(
            KafkaTemplate<String, ?> kafka,
            MeterRegistry meters,
            @Value("${app.tracking.kafka.topic:tracking.telemetry.ingested.v1}") String v1Topic,
            @Value("${app.tracking.kafka.v2-topic:tracking.telemetry.ingested.v2}") String v2Topic) {
        this.kafka = cast(kafka);
        this.meters = meters;
        this.v1Topic = v1Topic;
        this.v2Topic = v2Topic;
    }

    KafkaTelemetryStreamPublisher(
            KafkaTemplate<String, ?> kafka, MeterRegistry meters, String topic) {
        this(kafka, meters, topic, "tracking.telemetry.ingested.v2");
    }

    @SuppressWarnings("unchecked")
    private static KafkaTemplate<String, Object> cast(KafkaTemplate<String, ?> kafka) {
        return (KafkaTemplate<String, Object>) kafka;
    }

    @Override
    public Publication publishDurably(
            String key,
            CanonicalTelemetryEvent event,
            String correlationId,
            Duration timeout) {
        String topic = switch (event.eventVersion()) {
            case TrackingTelemetryIngestedV1.VERSION -> v1Topic;
            case TrackingTelemetryIngestedV2.VERSION -> v2Topic;
            default -> throw new IllegalArgumentException("Unsupported telemetry event version");
        };
        var record = new ProducerRecord<String, Object>(topic, key, event);
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

    @Override
    public List<Publication> publishBatchDurably(
            List<PublicationRequest> requests, Duration timeout) {
        if (requests.isEmpty()) {
            return List.of();
        }
        Timer.Sample sample = Timer.start(meters);
        long deadline = System.nanoTime() + timeout.toNanos();
        List<CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>>> futures =
                new ArrayList<>(requests.size());
        try {
            for (PublicationRequest request : requests) {
                futures.add(kafka.send(record(
                        request.partitionKey(), request.event(), request.correlationId())));
            }
            List<Publication> publications = new ArrayList<>(requests.size());
            for (CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>> future : futures) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new TimeoutException("Telemetry batch acknowledgement timed out");
                }
                var metadata = future.get(remaining, TimeUnit.NANOSECONDS).getRecordMetadata();
                publications.add(new Publication(metadata.partition(), metadata.offset()));
            }
            meters.counter("tracking.kafka.publications.durable").increment(requests.size());
            return List.copyOf(publications);
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

    private ProducerRecord<String, Object> record(
            String key, CanonicalTelemetryEvent event, String correlationId) {
        String topic = switch (event.eventVersion()) {
            case TrackingTelemetryIngestedV1.VERSION -> v1Topic;
            case TrackingTelemetryIngestedV2.VERSION -> v2Topic;
            default -> throw new IllegalArgumentException("Unsupported telemetry event version");
        };
        var record = new ProducerRecord<String, Object>(topic, key, event);
        header(record, "tenantId", event.tenantId().toString());
        header(record, "eventType", event.eventType());
        header(record, "eventVersion", Integer.toString(event.eventVersion()));
        if (correlationId != null && !correlationId.isBlank() && correlationId.length() <= 128) {
            header(record, "correlationId", correlationId);
        }
        return record;
    }

    private static void header(ProducerRecord<?, ?> record, String name, String value) {
        record.headers().add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8)));
    }

    private static DependencyUnavailableException unavailable(String code, String message) {
        return new DependencyUnavailableException(code, message, null);
    }
}
