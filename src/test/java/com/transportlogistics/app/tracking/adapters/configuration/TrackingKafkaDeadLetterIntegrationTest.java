package com.transportlogistics.app.tracking.adapters.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

class TrackingKafkaDeadLetterIntegrationTest {
    @Test
    void invalidV2RecordReachesVersionSpecificDeadLetterTopic() throws Exception {
        try (var broker = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.2"))) {
            broker.start();
            String source = "tracking.telemetry.ingested.v2";
            String deadLetter = source + ".dlt";
            try (var admin = AdminClient.create(Map.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers()))) {
                admin.createTopics(List.of(
                        new NewTopic(source, 1, (short) 1),
                        new NewTopic(deadLetter, 1, (short) 1))).all().get();
            }
            var properties = new KafkaProperties();
            properties.setBootstrapServers(List.of(broker.getBootstrapServers()));
            var template = new TrackingKafkaConfiguration()
                    .trackingTelemetryKafkaTemplate(properties);
            var factory = new TrackingKafkaConsumerConfiguration()
                    .trackingHistoryPersisterV2ContainerFactory(properties, template, deadLetter, 500);
            var container = factory.createContainer(source);
            container.getContainerProperties().setGroupId("tracking-telemetry-persister-group");
            container.setupMessageListener((org.springframework.kafka.listener.BatchAcknowledgingMessageListener
                    <String, TrackingTelemetryIngestedV2>) (records, acknowledgment) -> {
                                throw new IllegalArgumentException("poison telemetry contract");
                            });
            container.start();
            try {
                long assignmentDeadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                while (container.getAssignedPartitions().isEmpty()
                        && System.nanoTime() < assignmentDeadline) {
                    Thread.sleep(50L);
                }
                assertThat(container.getAssignedPartitions()).isNotEmpty();
                template.send(source, "invalid-authority", event()).get();
                var deserializer = new JsonDeserializer<>(TrackingTelemetryIngestedV2.class);
                deserializer.addTrustedPackages(TrackingTelemetryIngestedV2.class.getPackageName());
                var consumers = new DefaultKafkaConsumerFactory<String, TrackingTelemetryIngestedV2>(
                        Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers(),
                                ConsumerConfig.GROUP_ID_CONFIG, "tracking-live-projector-dlt-observer",
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                        new StringDeserializer(), deserializer);
                try (var consumer = consumers.createConsumer()) {
                    consumer.subscribe(List.of(deadLetter));
                    assertThat(consumer.poll(Duration.ofSeconds(15))).hasSize(1);
                }
            } finally {
                container.stop();
                template.destroy();
            }
        }
    }

    private static TrackingTelemetryIngestedV2 event() {
        return new TrackingTelemetryIngestedV2(UUID.randomUUID(),
                TrackingTelemetryIngestedV2.TYPE, 2, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "FLESPI", "message-1", "a".repeat(64),
                new BigDecimal("6.9271"), new BigDecimal("79.8612"), null, null, null,
                null, EngineState.UNKNOWN, null, null, Instant.now(), Instant.now(),
                TrackingTelemetryIngestedV2.TamperState.UNKNOWN, null, null,
                TrackingTelemetryIngestedV2.ExternalPowerState.UNKNOWN,
                TrackingTelemetryIngestedV2.BatteryChargingState.UNKNOWN);
    }
}
