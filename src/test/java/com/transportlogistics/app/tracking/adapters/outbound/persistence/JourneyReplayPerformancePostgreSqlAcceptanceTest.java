package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorState;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Direction;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplaySelector;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.TenantContext;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.TimeRange;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayHistoryPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("postgres")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(properties = {
        "app.tracking.hybrid-storage.enabled=true",
        "app.tracking.journey-replay.cursor-secret=acceptance_cursor_secret_at_least_32_bytes_2026"
})
class JourneyReplayPerformancePostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final Instant FROM = Instant.now().minusSeconds(7_200);
    private static final List<UUID> VEHICLES = java.util.stream.IntStream.range(0, 20)
            .mapToObj(ignored -> UUID.randomUUID()).toList();
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired JourneyReplayHistoryPort history;
    @Autowired JourneyReplayCursorPort cursors;

    @BeforeAll
    void seed() {
        flyway.clean();
        flyway.migrate();
        for (UUID vehicle : VEHICLES) {
            jdbc.update("""
                    INSERT INTO tracking_position_history(
                      tenant_id,source_timestamp,id,event_version,device_id,vehicle_id,provider_alias,
                      dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                      ordering_classification,retention_policy,retention_policy_version,safe_metadata)
                    SELECT ?,?::timestamptz+(value*interval '1 second'),gen_random_uuid(),1,gen_random_uuid(),
                      ?,'CS07',repeat(md5((?::text)||value::text),2),now(),6.9271,79.8612,
                      'UNKNOWN','TRUSTED','ACCEPTABLE','IN_ORDER','TIMESCALE_RAW_180_DAYS','V87','{}'::jsonb
                    FROM generate_series(1,2000) value
                    """, TENANT, Timestamp.from(FROM), vehicle, vehicle);
        }
        jdbc.execute("ANALYZE tracking_position_history");
        history.query(query(VEHICLES.getFirst(), null), VEHICLES.getFirst(), null);
    }

    @Test
    void meetsFrozenInitialAndSubsequentPageTargetsAcrossTwentyConcurrentSessions() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Long> initialMillis = java.util.Collections.synchronizedList(new ArrayList<>());
        List<Long> subsequentMillis = java.util.Collections.synchronizedList(new ArrayList<>());
        long workloadStart = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = VEHICLES.stream().map(vehicle -> executor.submit(() -> {
                start.await();
                long initialStart = System.nanoTime();
                ReplayPage first = history.query(query(vehicle, null), vehicle, null);
                initialMillis.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialStart));
                assertThat(first.items()).hasSize(1000);
                CursorState cursor = cursors.decode(first.nextCursor());
                long subsequentStart = System.nanoTime();
                ReplayPage second = history.query(query(vehicle, first.nextCursor()), vehicle, cursor);
                subsequentMillis.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - subsequentStart));
                assertThat(second.items()).hasSize(1000);
                assertThat(second.nextCursor()).isNull();
                return null;
            })).toList();
            start.countDown();
            for (var future : futures) future.get(15, TimeUnit.SECONDS);
        }

        long workloadMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - workloadStart);
        long initialP95 = percentile(initialMillis, 0.95);
        long subsequentP95 = percentile(subsequentMillis, 0.95);
        double requestsPerSecond = 40_000.0 / workloadMillis;
        System.out.printf("US53_CS07_PERFORMANCE sessions=20 points=40000 "
                + "initial_p50_ms=%d initial_p95_ms=%d initial_p99_ms=%d "
                + "subsequent_p50_ms=%d subsequent_p95_ms=%d subsequent_p99_ms=%d "
                + "requests_per_second=%.2f workload_ms=%d%n",
                percentile(initialMillis, 0.50), initialP95, percentile(initialMillis, 0.99),
                percentile(subsequentMillis, 0.50), subsequentP95, percentile(subsequentMillis, 0.99),
                requestsPerSecond, workloadMillis);
        assertThat(initialMillis).hasSize(20);
        assertThat(subsequentMillis).hasSize(20);
        assertThat(initialP95).isLessThanOrEqualTo(2_000);
        assertThat(subsequentP95).isLessThanOrEqualTo(1_000);
    }

    private static ReplayQuery query(UUID vehicle, String cursor) {
        TimeRange range = new TimeRange(FROM, FROM.plusSeconds(2_001));
        return new ReplayQuery(new TenantContext(TENANT, UUID.randomUUID()), ReplaySelector.vehicle(vehicle),
                range, range, 1000, cursor, Set.of(), Direction.CHRONOLOGICAL_ASCENDING, 20_000);
    }

    private static long percentile(List<Long> values, double percentile) {
        List<Long> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1);
        return sorted.get(index);
    }
}
