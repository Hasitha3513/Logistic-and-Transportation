package com.transportlogistics.app.tracking.adapters.outbound.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transportlogistics.app.tracking.application.telemetry.LiveTelemetryProjection;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.outbound.LiveTelemetryProjectionPort.ProjectionResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class RedisLiveTelemetryProjectionAdapterIntegrationTest {
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    private LettuceConnectionFactory connection;
    private StringRedisTemplate template;
    private ObjectMapper json;

    @BeforeEach
    void startRedis() {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
        connection = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connection.afterPropertiesSet();
        template = new StringRedisTemplate(connection);
        template.afterPropertiesSet();
        template.getConnectionFactory().getConnection().serverCommands().flushAll();
        json = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @AfterEach
    void closeConnection() {
        connection.destroy();
    }

    @Test
    void atomicallyProjectsNewestStateWithExactTtlAndTenantIsolation() throws Exception {
        var adapter = new RedisLiveTelemetryProjectionAdapter(
                template, json, Duration.ofHours(24), 10_000);
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-13T12:00:10Z");
        var first = projection(event(tenant, vehicle, "00000000-0000-0000-0000-000000000001",
                "2026-09-13T12:00:00Z", "6.9271"), now);

        assertThat(adapter.project(first)).isEqualTo(ProjectionResult.UPDATED);
        assertThat(adapter.project(first)).isEqualTo(ProjectionResult.DUPLICATE);
        assertThat(template.getExpire(
                RedisLiveTelemetryProjectionAdapter.liveKey(tenant, vehicle)))
                .isBetween(Duration.ofHours(23).plusMinutes(59).getSeconds(),
                        Duration.ofHours(24).getSeconds());
        assertThat(adapter.find(otherTenant, vehicle)).isEmpty();
        assertThat(adapter.findLive(otherTenant, now, 100)).isEmpty();

        var older = projection(event(tenant, vehicle,
                "00000000-0000-0000-0000-000000000009", "2026-09-13T11:59:59Z", "1"), now);
        assertThat(adapter.project(older)).isEqualTo(ProjectionResult.STALE);
        assertThat(adapter.find(tenant, vehicle).orElseThrow().telemetry().latitude())
                .isEqualByComparingTo("6.9271");

        var newer = projection(event(tenant, vehicle,
                "00000000-0000-0000-0000-000000000002", "2026-09-13T12:00:01Z", "7"), now);
        assertThat(adapter.project(newer)).isEqualTo(ProjectionResult.UPDATED);
        assertThat(adapter.find(tenant, vehicle).orElseThrow().telemetry().latitude())
                .isEqualByComparingTo("7");
    }

    @Test
    void sameTimeUsesLexicographicallyGreatestEventIdAndConcurrentDeliveryConverges()
            throws Exception {
        var adapter = new RedisLiveTelemetryProjectionAdapter(
                template, json, Duration.ofHours(24), 10_000);
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-13T12:00:10Z");
        var lower = projection(event(tenant, vehicle,
                "00000000-0000-0000-0000-000000000001", "2026-09-13T12:00:00Z", "1"), now);
        var higher = projection(event(tenant, vehicle,
                "00000000-0000-0000-0000-000000000002", "2026-09-13T12:00:00Z", "2"), now);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var one = executor.submit(() -> adapter.project(lower));
            var two = executor.submit(() -> adapter.project(higher));
            one.get();
            two.get();
        }
        assertThat(adapter.find(tenant, vehicle).orElseThrow().telemetry().eventId())
                .isEqualTo(higher.telemetry().eventId());
    }

    @Test
    void boundedIndexPrunesEarliestExpiryAndProjectionContainsNoSensitiveFields() {
        var adapter = new RedisLiveTelemetryProjectionAdapter(
                template, json, Duration.ofHours(24), 2);
        UUID tenant = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-13T12:00:10Z");
        UUID firstVehicle = UUID.randomUUID();
        adapter.project(projection(event(tenant, firstVehicle,
                "00000000-0000-0000-0000-000000000001", "2026-09-13T12:00:00Z", "1"), now));
        adapter.project(projection(event(tenant, UUID.randomUUID(),
                "00000000-0000-0000-0000-000000000002", "2026-09-13T12:00:01Z", "2"), now.plusMillis(1)));
        adapter.project(projection(event(tenant, UUID.randomUUID(),
                "00000000-0000-0000-0000-000000000003", "2026-09-13T12:00:02Z", "3"), now.plusMillis(2)));

        assertThat(template.opsForZSet().size(
                RedisLiveTelemetryProjectionAdapter.indexKey(tenant))).isEqualTo(2);
        assertThat(adapter.findLive(tenant, now, 100)).hasSize(2);
        String stored = String.valueOf(template.opsForHash().get(
                RedisLiveTelemetryProjectionAdapter.liveKey(tenant, firstVehicle), "projection"));
        assertThat(stored).doesNotContainIgnoringCase(
                "credential", "signature", "secret", "rawPayload", "customer", "driver");
    }

    private static LiveTelemetryProjection projection(
            TrackingTelemetryIngestedV1 event, Instant projectedAt) {
        return new LiveTelemetryProjection(event, projectedAt);
    }

    private static TrackingTelemetryIngestedV1 event(
            UUID tenant, UUID vehicle, String eventId, String recordedAt, String latitude) {
        return new TrackingTelemetryIngestedV1(UUID.fromString(eventId),
                TrackingTelemetryIngestedV1.TYPE, 1, tenant, vehicle, UUID.randomUUID(),
                "FLESPI", "message-1", "a".repeat(64), new BigDecimal(latitude),
                new BigDecimal("79.8612"), new BigDecimal("42.5"), new BigDecimal("90"),
                new BigDecimal("4.2"), null, EngineState.UNKNOWN, null, null,
                Instant.parse(recordedAt), Instant.parse("2026-09-13T12:00:05Z"));
    }
}
