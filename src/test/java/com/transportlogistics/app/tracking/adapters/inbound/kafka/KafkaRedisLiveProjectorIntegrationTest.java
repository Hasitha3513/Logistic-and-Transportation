package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transportlogistics.app.tracking.adapters.outbound.redis.RedisLiveTelemetryProjectionAdapter;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

class KafkaRedisLiveProjectorIntegrationTest {
    @Test
    void consumesRealKafkaRecordIntoRealRedisBeforeAcknowledging() throws Exception {
        try (var kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.2"));
                var redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                        .withExposedPorts(6379)) {
            kafka.start();
            redis.start();
            String topic = "tracking.telemetry.ingested.v1";
            try (var admin = AdminClient.create(Map.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
                admin.createTopics(java.util.List.of(new NewTopic(topic, 6, (short) 1))).all().get();
            }
            var connection = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
            connection.afterPropertiesSet();
            try {
                var redisTemplate = new StringRedisTemplate(connection);
                redisTemplate.afterPropertiesSet();
                var liveState = new RedisLiveTelemetryProjectionAdapter(redisTemplate,
                        new ObjectMapper().registerModule(new JavaTimeModule()),
                        Duration.ofHours(24), 10_000);
                var event = event();
                String key = event.tenantId() + ":" + event.vehicleId();
                var producerFactory = new DefaultKafkaProducerFactory<String, TrackingTelemetryIngestedV1>(
                        Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                                ProducerConfig.ACKS_CONFIG, "all",
                                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true));
                var template = new KafkaTemplate<>(producerFactory);
                var outgoing = new ProducerRecord<String, TrackingTelemetryIngestedV1>(topic, key, event);
                outgoing.headers().add(new RecordHeader("tenantId",
                        event.tenantId().toString().getBytes(StandardCharsets.UTF_8)));
                outgoing.headers().add(new RecordHeader("eventType",
                        event.eventType().getBytes(StandardCharsets.UTF_8)));
                outgoing.headers().add(new RecordHeader("eventVersion", "1".getBytes(StandardCharsets.UTF_8)));
                template.send(outgoing).get();

                var valueDeserializer = new JsonDeserializer<>(TrackingTelemetryIngestedV1.class);
                valueDeserializer.addTrustedPackages(event.getClass().getPackageName());
                var consumerFactory = new DefaultKafkaConsumerFactory<String, TrackingTelemetryIngestedV1>(
                        Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                                ConsumerConfig.GROUP_ID_CONFIG, "tracking-live-projector-v1-test",
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                        new StringDeserializer(), valueDeserializer);
                try (var consumer = consumerFactory.createConsumer()) {
                    consumer.subscribe(java.util.List.of(topic));
                    var records = consumer.poll(Duration.ofSeconds(10));
                    assertThat(records).hasSize(1);
                    var acknowledgment = mock(Acknowledgment.class);
                    var reliability = mock(com.transportlogistics.app.tracking.application.GpsReliabilityEvaluationService.class);
                    when(reliability.evaluateAndRecord(org.mockito.ArgumentMatchers.any(),
                            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                            .thenReturn(new com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Assessment(
                                    com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Trust.TRUSTED,
                                    com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Ordering.IN_ORDER,
                                    com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Connectivity.LIVE,
                                    com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ReliabilityState.NORMAL,
                                    java.util.Set.of(com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Quality.NORMAL),
                                    true, true));
                    new TrackingLiveTelemetryProjector(liveState, reliability, new SimpleMeterRegistry(),
                            Clock.fixed(Instant.parse("2026-09-13T12:00:10Z"), ZoneOffset.UTC),
                            "tracking.telemetry.ingested.v1", "tracking.telemetry.ingested.v2")
                            .consume(records.iterator().next(), acknowledgment);
                    verify(acknowledgment).acknowledge();
                    assertThat(liveState.find(event.tenantId(), event.vehicleId()))
                            .get().extracting(state -> state.telemetry().eventId()).isEqualTo(event.eventId());
                } finally {
                    template.destroy();
                }
            } finally {
                connection.destroy();
            }
        }
    }

    private static TrackingTelemetryIngestedV1 event() {
        return new TrackingTelemetryIngestedV1(UUID.randomUUID(),
                TrackingTelemetryIngestedV1.TYPE, 1, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "GENERIC", "message-1", "b".repeat(64),
                new BigDecimal("6.9271"), new BigDecimal("79.8612"),
                new BigDecimal("42.5"), new BigDecimal("90"), new BigDecimal("4.2"),
                null, EngineState.UNKNOWN, null, null,
                Instant.parse("2026-09-13T12:00:00Z"), Instant.parse("2026-09-13T12:00:05Z"));
    }
}
