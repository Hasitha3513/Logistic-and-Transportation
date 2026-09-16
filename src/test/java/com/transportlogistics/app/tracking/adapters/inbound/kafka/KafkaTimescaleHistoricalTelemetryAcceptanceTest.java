package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.transportlogistics.app.tracking.HybridTelemetryTimescaleMigrationAcceptanceTest;
import com.transportlogistics.app.tracking.adapters.outbound.persistence.JdbcHistoricalTelemetryStore;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import com.transportlogistics.app.support.AcceptanceDatabaseGuard;

class KafkaTimescaleHistoricalTelemetryAcceptanceTest {
    @Test
    void consumesCanonicalKafkaBatchIntoTimescaleIdempotently() throws Exception {
        var timescaleImage = DockerImageName.parse("timescale/timescaledb:latest-pg16")
                .asCompatibleSubstituteFor("postgres");
        try (var kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.2"));
                var database = new PostgreSQLContainer<>(timescaleImage)
                        .withDatabaseName(AcceptanceDatabaseGuard.REQUIRED_DATABASE)
                        .withUsername("transport_test").withPassword("transport_test")) {
            kafka.start();
            database.start();
            var safetyDataSource = new DriverManagerDataSource(
                    database.getJdbcUrl(), database.getUsername(), database.getPassword());
            AcceptanceDatabaseGuard.verify(safetyDataSource);
            String topic = "tracking.telemetry.ingested.v1";
            try (var admin = AdminClient.create(Map.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
                admin.createTopics(List.of(new NewTopic(topic, 6, (short) 1))).all().get();
            }
            Flyway.configure().dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                    .placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04())
                    .load().migrate();
            var dataSource = new DriverManagerDataSource(
                    database.getJdbcUrl(), database.getUsername(), database.getPassword());
            var history = new JdbcHistoricalTelemetryStore(new JdbcTemplate(dataSource),
                    new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
            var producerFactory = new DefaultKafkaProducerFactory<String, TrackingTelemetryIngestedV1>(
                    Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                            ProducerConfig.ACKS_CONFIG, "all",
                            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true));
            var template = new KafkaTemplate<>(producerFactory);
            try {
                var newer = event("a".repeat(64), Instant.now().minusSeconds(5));
                var older = event("b".repeat(64), newer.recordedAt().minusSeconds(60));
                send(template, topic, newer);
                send(template, topic, older);
                send(template, topic, newer);

                var deserializer = new JsonDeserializer<>(TrackingTelemetryIngestedV1.class);
                deserializer.addTrustedPackages(TrackingTelemetryIngestedV1.class.getPackageName());
                var consumers = new DefaultKafkaConsumerFactory<String, TrackingTelemetryIngestedV1>(
                        Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                                ConsumerConfig.GROUP_ID_CONFIG, "tracking-telemetry-persister-group-test",
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                                ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500),
                        new StringDeserializer(), deserializer);
                try (var consumer = consumers.createConsumer()) {
                    consumer.subscribe(List.of(topic));
                    var records = consumer.poll(Duration.ofSeconds(10));
                    assertThat(records).hasSize(3);
                    var acknowledgment = mock(Acknowledgment.class);
                    var batch = java.util.stream.StreamSupport.stream(
                            records.records(topic).spliterator(), false).toList();
                    new TrackingHistoricalTelemetryPersister(history, new SimpleMeterRegistry(), topic)
                            .consume(batch, acknowledgment);
                    verify(acknowledgment).acknowledge();
                    assertThat(history.find(newer.tenantId(), newer.vehicleId(),
                            older.recordedAt().minusSeconds(1), newer.recordedAt().plusSeconds(1), 10))
                            .hasSize(2);
                }
            } finally {
                template.destroy();
            }
        }
    }

    private static void send(KafkaTemplate<String, TrackingTelemetryIngestedV1> template,
            String topic, TrackingTelemetryIngestedV1 event) throws Exception {
        var record = new ProducerRecord<String, TrackingTelemetryIngestedV1>(
                topic, event.tenantId() + ":" + event.vehicleId(), event);
        record.headers().add(new RecordHeader("tenantId",
                event.tenantId().toString().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("eventType",
                event.eventType().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("eventVersion", "1".getBytes(StandardCharsets.UTF_8)));
        template.send(record).get();
    }

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID VEHICLE = UUID.randomUUID();

    private static TrackingTelemetryIngestedV1 event(String dedupe, Instant recordedAt) {
        return new TrackingTelemetryIngestedV1(UUID.nameUUIDFromBytes(dedupe.getBytes(StandardCharsets.UTF_8)),
                TrackingTelemetryIngestedV1.TYPE, 1, TENANT, VEHICLE, UUID.randomUUID(),
                "GENERIC", null, dedupe, new BigDecimal("6.9271"), new BigDecimal("79.8612"),
                BigDecimal.ONE, null, BigDecimal.ONE, null, EngineState.ON, null, null,
                recordedAt, recordedAt.plusSeconds(1));
    }
}
