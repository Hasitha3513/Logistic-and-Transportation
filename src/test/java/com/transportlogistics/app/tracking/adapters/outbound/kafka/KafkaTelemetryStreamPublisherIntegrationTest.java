package com.transportlogistics.app.tracking.adapters.outbound.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

class KafkaTelemetryStreamPublisherIntegrationTest {
    @Test
    void returnsOnlyAfterRealBrokerDurablyAcknowledgesCanonicalRecord() throws Exception {
        try (var kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.2"))) {
            kafka.start();
            String topic = "tracking.telemetry.ingested.v1";
            try (var admin = AdminClient.create(Map.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
                admin.createTopics(java.util.List.of(new NewTopic(topic, 6, (short) 1))).all().get();
            }
            var producerFactory = new DefaultKafkaProducerFactory<String, TrackingTelemetryIngestedV1>(Map.of(
                    org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                    org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class,
                    org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                    org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "all",
                    org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true));
            var template = new KafkaTemplate<>(producerFactory);
            var publisher = new KafkaTelemetryStreamPublisher(template, new SimpleMeterRegistry(), topic);
            UUID tenant = UUID.randomUUID();
            UUID vehicle = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            var event = new TrackingTelemetryIngestedV1(eventId,
                    TrackingTelemetryIngestedV1.TYPE, 1, tenant, vehicle, UUID.randomUUID(),
                    "FLESPI", "message-1", "a".repeat(64), new BigDecimal("6.9271"),
                    new BigDecimal("79.8612"), new BigDecimal("42.5"), null,
                    new BigDecimal("4.2"), null, EngineState.UNKNOWN, null, null,
                    Instant.parse("2026-09-13T12:00:00Z"), Instant.parse("2026-09-13T12:00:01Z"));

            var result = publisher.publishDurably(
                    tenant + ":" + vehicle, event, "correlation-1", Duration.ofSeconds(10));
            assertThat(result.offset()).isNotNegative();

            var valueDeserializer = new JsonDeserializer<>(TrackingTelemetryIngestedV1.class);
            valueDeserializer.addTrustedPackages("com.transportlogistics.app.tracking.application.telemetry");
            var consumerFactory = new DefaultKafkaConsumerFactory<String, TrackingTelemetryIngestedV1>(Map.of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                    ConsumerConfig.GROUP_ID_CONFIG, "ts02-contract-test",
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                    new StringDeserializer(), valueDeserializer);
            try (var consumer = consumerFactory.createConsumer()) {
                consumer.subscribe(java.util.List.of(topic));
                var records = consumer.poll(Duration.ofSeconds(10));
                assertThat(records).hasSize(1);
                var record = records.iterator().next();
                assertThat(record.key()).isEqualTo(tenant + ":" + vehicle);
                assertThat(record.value()).isEqualTo(event);
                assertThat(new String(record.headers().lastHeader("tenantId").value(), java.nio.charset.StandardCharsets.UTF_8))
                        .isEqualTo(tenant.toString());
            } finally {
                template.destroy();
            }
        }
    }
}
