package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayHistoryPort;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("postgres")
@SpringBootTest(properties = {
        "app.tracking.hybrid-storage.enabled=true",
        "app.tracking.journey-replay.cursor-secret=acceptance_cursor_secret_at_least_32_bytes_2026"
})
class JourneyReplayTimescalePostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID VEHICLE = UUID.randomUUID();
    private static final Instant FROM = Instant.now().minusSeconds(3_600);
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired JourneyReplayHistoryPort history;
    @Autowired JourneyReplayCursorPort cursors;

    @BeforeEach
    void reset() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void pagesBySourceTimeAndIdentityWithSnapshotAndTenantIsolation() {
        UUID first = new UUID(0, 1);
        UUID second = new UUID(0, 2);
        UUID third = new UUID(0, 3);
        insert(TENANT, VEHICLE, first, FROM.plusSeconds(10), Instant.now().minusSeconds(30));
        insert(TENANT, VEHICLE, second, FROM.plusSeconds(10), Instant.now().minusSeconds(20));
        insert(TENANT, VEHICLE, third, FROM.plusSeconds(20), Instant.now().minusSeconds(10));
        insert(UUID.randomUUID(), VEHICLE, UUID.randomUUID(), FROM.plusSeconds(5), Instant.now());
        ReplayQuery query = query(TENANT, VEHICLE, 2, null, FROM, FROM.plusSeconds(100));

        ReplayPage firstPage = history.query(query, VEHICLE, null);
        assertThat(firstPage.snapshotRecordedAt()).isNotNull();
        assertThat(firstPage.items()).extracting(JourneyPoint::historyId)
                .containsExactly(first, second);
        CursorState cursor = cursors.decode(firstPage.nextCursor());
        insert(TENANT, VEHICLE, UUID.randomUUID(), FROM.plusSeconds(15),
                cursor.binding().snapshotRecordedAt().plusSeconds(1));
        ReplayQuery continuation = query(TENANT, VEHICLE, 2, firstPage.nextCursor(),
                FROM, FROM.plusSeconds(100));

        ReplayPage secondPage = history.query(continuation, VEHICLE, cursor);
        assertThat(secondPage.snapshotRecordedAt()).isEqualTo(firstPage.snapshotRecordedAt());
        assertThat(secondPage.items()).extracting(JourneyPoint::historyId).containsExactly(third);
        assertThat(secondPage.nextCursor()).isNull();
    }

    @Test
    void exposesBoundedTenantQualifiedBoundaryEvidenceWithoutAddingItToItems() {
        insert(TENANT, VEHICLE, new UUID(0, 20), FROM.minusSeconds(1), Instant.now().minusSeconds(20));
        UUID visible = new UUID(0, 21);
        insert(TENANT, VEHICLE, visible, FROM.plusSeconds(1), Instant.now().minusSeconds(10));
        insert(TENANT, VEHICLE, new UUID(0, 22), FROM.plusSeconds(10), Instant.now().minusSeconds(5));
        ReplayPage page = history.query(query(TENANT, VEHICLE, 10, null, FROM,
                FROM.plusSeconds(10)), VEHICLE, null);
        assertThat(page.items()).extracting(JourneyPoint::historyId).containsExactly(visible);
        assertThat(page.boundaryEvidence().lowerBoundary().adjacentPoint()).isNotNull();
        assertThat(page.boundaryEvidence().upperBoundary().adjacentPoint()).isNotNull();
    }

    @Test
    void preservesExactDecimalsNullableTelemetryAndNoSensitiveProviderFields() {
        UUID id = UUID.randomUUID();
        insert(TENANT, VEHICLE, id, FROM.plusSeconds(1), Instant.now().minusSeconds(1));
        JourneyPoint point = history.query(query(TENANT, VEHICLE, 10, null, FROM,
                FROM.plusSeconds(10)), VEHICLE, null).items().getFirst();
        assertThat(point.coordinate().latitude()).isEqualByComparingTo("6.9271000");
        assertThat(point.coordinate().longitude()).isEqualByComparingTo("79.8612000");
        assertThat(point.speedKph()).isNull();
        assertThat(point.accuracyMeters()).isNull();
        assertThat(JourneyPoint.class.getRecordComponents()).extracting("name")
                .doesNotContain("providerAlias", "deviceId", "providerMessageId", "rawPayload");
    }

    @Test
    void reportsCompleteNoDataAndPartialRetentionWithoutScanningTenantHistory() {
        ReplayPage empty = history.query(query(TENANT, VEHICLE, 10, null, FROM,
                FROM.plusSeconds(10)), VEHICLE, null);
        assertThat(empty.coverage()).isEqualTo(Coverage.NO_DATA);
        Instant old = Instant.now().minusSeconds(181L * 86_400L);
        ReplayPage partial = history.query(query(TENANT, VEHICLE, 10, null, old,
                old.plusSeconds(7L * 86_400L)), VEHICLE, null);
        assertThat(partial.coverage()).isEqualTo(Coverage.PARTIAL_RETENTION);
        assertThat(partial.gaps()).hasSize(1);
    }

    @Test
    void carriesOnePredecessorAcrossPageBoundaryForGapAndLargeJumpWarnings() {
        insert(TENANT, VEHICLE, new UUID(0, 10), FROM.plusSeconds(1), Instant.now().minusSeconds(20));
        insertAt(TENANT, VEHICLE, new UUID(0, 11), FROM.plusSeconds(302),
                Instant.now().minusSeconds(10), new BigDecimal("7.9271000"),
                new BigDecimal("80.8612000"));
        ReplayQuery first = query(TENANT, VEHICLE, 1, null, FROM, FROM.plusSeconds(600));
        ReplayPage firstPage = history.query(first, VEHICLE, null);
        CursorState cursor = cursors.decode(firstPage.nextCursor());
        ReplayQuery second = query(TENANT, VEHICLE, 1, firstPage.nextCursor(),
                FROM, FROM.plusSeconds(600));
        JourneyPoint point = history.query(second, VEHICLE, cursor).items().getFirst();
        assertThat(point.qualityFlags()).contains(QualityFlag.TIME_GAP, QualityFlag.LARGE_JUMP);
    }

    @Test
    void usesTenantVehicleTimeIndexForBoundedQuery() {
        jdbc.update("""
                INSERT INTO tracking_position_history(
                  tenant_id,source_timestamp,id,event_version,device_id,vehicle_id,provider_alias,
                  dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                  ordering_classification,retention_policy,retention_policy_version,safe_metadata)
                SELECT ?,?::timestamptz+(value*interval '1 second'),gen_random_uuid(),1,gen_random_uuid(),
                  CASE WHEN value%100=0 THEN ? ELSE gen_random_uuid() END,'TEST',
                  repeat(md5(value::text),2),now(),6.9271,79.8612,'UNKNOWN','TRUSTED','ACCEPTABLE',
                  'IN_ORDER','TIMESCALE_RAW_180_DAYS','V87','{}'::jsonb
                FROM generate_series(1,10000) value
                """, TENANT, Timestamp.from(FROM), VEHICLE);
        jdbc.execute("ANALYZE tracking_position_history");
        String plan = String.join("\n", jdbc.queryForList("""
                EXPLAIN (ANALYZE,BUFFERS)
                SELECT id FROM tracking_position_history
                WHERE tenant_id=? AND vehicle_id=? AND source_timestamp>=? AND source_timestamp<?
                  AND received_at<=?
                ORDER BY source_timestamp ASC,id ASC LIMIT 2001
                """, String.class, TENANT, VEHICLE, Timestamp.from(FROM),
                Timestamp.from(FROM.plusSeconds(20_000)), Timestamp.from(Instant.now())));
        assertThat(plan).contains("idx_tracking_history_vehicle_time");
        assertThat(plan).doesNotContain("Seq Scan on tracking_position_history");
    }

    private void insert(UUID tenant, UUID vehicle, UUID id, Instant source, Instant received) {
        insertAt(tenant, vehicle, id, source, received, new BigDecimal("6.9271000"),
                new BigDecimal("79.8612000"));
    }

    private void insertAt(UUID tenant, UUID vehicle, UUID id, Instant source, Instant received,
            BigDecimal latitude, BigDecimal longitude) {
        jdbc.update("""
                INSERT INTO tracking_position_history(
                  tenant_id,source_timestamp,id,event_version,device_id,vehicle_id,provider_alias,
                  dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                  ordering_classification,retention_policy,retention_policy_version,safe_metadata)
                VALUES(?,?,?,1,?,?, 'TEST',?,?,?,?,'UNKNOWN','TRUSTED',
                  'ACCURACY_UNKNOWN','IN_ORDER','TIMESCALE_RAW_180_DAYS','V87','{}'::jsonb)
                """, tenant, Timestamp.from(source), id, UUID.randomUUID(), vehicle,
                String.format("%064x", id.getLeastSignificantBits()), Timestamp.from(received),
                latitude, longitude);
    }

    private static ReplayQuery query(UUID tenant, UUID vehicle, int limit, String cursor,
            Instant from, Instant to) {
        TimeRange range = new TimeRange(from, to);
        return new ReplayQuery(new TenantContext(tenant, UUID.randomUUID()),
                ReplaySelector.vehicle(vehicle), range, range, limit, cursor, Set.of(),
                Direction.CHRONOLOGICAL_ASCENDING, 20_000);
    }
}
