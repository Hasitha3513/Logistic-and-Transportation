package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.support.ReferenceFixtures;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class RouteDeviationV92IndexPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT = UUID.fromString("4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a");
    private static final UUID ORIGIN = UUID.fromString("52010000-0000-0000-0000-000000000010");
    private static final UUID DESTINATION = UUID.fromString("52020000-0000-0000-0000-000000000011");
    private static final Instant BASE = Instant.parse("2026-09-14T00:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired DataSource dataSource;

    @Test
    void v92IsTransactionalExactEfficientAndTenantSafeAtTenThousandRows() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("101");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version='92' AND success",
                Integer.class)).isOne();
        assertThat(indexes()).hasSize(2).allSatisfy(index -> {
            assertThat(index.valid()).isTrue();
            assertThat(index.ready()).isTrue();
        });
        assertThat(indexDefinition("idx_tracking_route_deviation_episode_keyset"))
                .contains("USING btree (tenant_id, vehicle_id, start_source_timestamp DESC, id DESC)")
                .doesNotContain("INCLUDE", "WHERE");
        assertThat(indexDefinition("idx_trip_tenant_vehicle_source_assignment"))
                .contains("USING btree (tenant_id, vehicle_id, actual_start_time DESC, id DESC)")
                .contains("INCLUDE (actual_end_time, status, driver_id, route_id, route_version)")
                .contains("actual_start_time IS NOT NULL", "CANCELLED", "REJECTED");
        assertThat(indexCountByKeyShape()).containsEntry("episode", 1L).containsEntry("trip", 1L);
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TEMP TABLE cs07_transaction_probe(id uuid)");
                statement.execute("CREATE INDEX cs07_transaction_probe_index ON cs07_transaction_probe(id)");
                statement.execute("CREATE INDEX cs07_transaction_failure ON missing_table(id)");
            } catch (SQLException expected) {
                connection.rollback();
            }
        }
        assertThat(jdbc.queryForObject("SELECT to_regclass('pg_temp.cs07_transaction_probe_index')",
                String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_index WHERE NOT indisvalid OR NOT indisready",
                Integer.class)).isZero();

        UUID vehicle = UUID.randomUUID();
        UUID route = seedTripReferences(vehicle);
        seedEpisodes(vehicle, 10_000);
        seedTrips(vehicle, route, 10_000);
        jdbc.execute("ANALYZE tracking_route_deviation_episode");
        jdbc.execute("ANALYZE trip");
        warmPlans(vehicle);

        String episodePlan = episodePlan(vehicle);
        assertThat(episodePlan).contains("idx_tracking_route_deviation_episode_keyset")
                .doesNotContain("Seq Scan on tracking_route_deviation_episode", "Sort  (");
        List<Map<String, Object>> page = episodePage(TENANT, vehicle);
        assertThat(page).hasSize(100);
        for (int index = 1; index < page.size(); index++) {
            Instant previous = ((Timestamp) page.get(index - 1).get("start_source_timestamp")).toInstant();
            Instant current = ((Timestamp) page.get(index).get("start_source_timestamp")).toInstant();
            assertThat(previous).isAfterOrEqualTo(current);
        }

        Instant sourceTime = BASE.plusSeconds(5_000);
        String tripPlan = tripPlan(vehicle, sourceTime);
        assertThat(tripPlan).contains("idx_trip_tenant_vehicle_source_assignment")
                .doesNotContain("Seq Scan on trip", "Sort  (")
                .doesNotContain("Rows Removed by Filter: 9940");
        Map<String, Object> assignment = assignment(TENANT, vehicle, sourceTime);
        assertThat(assignment).containsEntry("route_id", route).containsEntry("route_version", "REVISION:7");
        assertThat(assignment(UUID.randomUUID(), vehicle, sourceTime)).isEmpty();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM trip WHERE tenant_id=? AND vehicle_id=?
                  AND status IN ('CANCELLED','REJECTED')
                """, Integer.class, TENANT, vehicle)).isGreaterThan(0);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM trip WHERE tenant_id=? AND vehicle_id=? AND route_version IS NULL
                """, Integer.class, TENANT, vehicle)).isGreaterThan(0);
    }

    private List<IndexState> indexes() {
        return jdbc.query("""
                SELECT index_class.relname,idx.indisvalid,idx.indisready
                FROM pg_index idx JOIN pg_class index_class ON index_class.oid=idx.indexrelid
                WHERE index_class.relname IN (
                  'idx_tracking_route_deviation_episode_keyset',
                  'idx_trip_tenant_vehicle_source_assignment') ORDER BY index_class.relname
                """, (row, number) -> new IndexState(row.getString(1), row.getBoolean(2), row.getBoolean(3)));
    }

    private Map<String, Long> indexCountByKeyShape() {
        return Map.of(
                "episode", jdbc.queryForObject("""
                        SELECT count(*) FROM pg_indexes WHERE tablename='tracking_route_deviation_episode'
                          AND indexdef LIKE '%(tenant_id, vehicle_id, start_source_timestamp DESC, id DESC)%'
                        """, Long.class),
                "trip", jdbc.queryForObject("""
                        SELECT count(*) FROM pg_indexes WHERE tablename='trip'
                          AND indexdef LIKE '%(tenant_id, vehicle_id, actual_start_time DESC, id DESC)%'
                        """, Long.class));
    }

    private String indexDefinition(String name) {
        return jdbc.queryForObject("SELECT pg_get_indexdef(?::regclass)", String.class, name);
    }

    private String episodePlan(UUID vehicle) {
        return plan("""
                SELECT id,start_source_timestamp FROM tracking_route_deviation_episode
                WHERE tenant_id=? AND vehicle_id=?
                  AND start_source_timestamp>=? AND start_source_timestamp<=?
                  AND (start_source_timestamp,id)<(?,?)
                ORDER BY start_source_timestamp DESC,id DESC LIMIT 100
                """, TENANT, vehicle, Timestamp.from(BASE.minusSeconds(20_000)),
                Timestamp.from(BASE.plusSeconds(1)), Timestamp.from(BASE.plusSeconds(1)),
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
    }

    private String tripPlan(UUID vehicle, Instant sourceTime) {
        return plan("""
                SELECT id,driver_id,route_id,route_version FROM trip
                WHERE tenant_id=? AND vehicle_id=? AND actual_start_time IS NOT NULL
                  AND actual_start_time<=? AND (actual_end_time IS NULL OR actual_end_time>=?)
                  AND status NOT IN('CANCELLED','REJECTED')
                ORDER BY actual_start_time DESC,id DESC LIMIT 1
                """, TENANT, vehicle, Timestamp.from(sourceTime), Timestamp.from(sourceTime));
    }

    private String plan(String sql, Object... arguments) {
        String value = String.join("\n", jdbc.queryForList(
                "EXPLAIN (ANALYZE,BUFFERS,COSTS OFF) " + sql, String.class, arguments));
        System.out.println("US52_V92_PLAN\n" + value);
        return value;
    }

    private void warmPlans(UUID vehicle) {
        episodePage(TENANT, vehicle);
        assignment(TENANT, vehicle, BASE.plusSeconds(5_000));
    }

    private List<Map<String, Object>> episodePage(UUID tenant, UUID vehicle) {
        return jdbc.queryForList("""
                SELECT id,start_source_timestamp FROM tracking_route_deviation_episode
                WHERE tenant_id=? AND vehicle_id=? ORDER BY start_source_timestamp DESC,id DESC LIMIT 100
                """, tenant, vehicle);
    }

    private Map<String, Object> assignment(UUID tenant, UUID vehicle, Instant sourceTime) {
        List<Map<String, Object>> found = jdbc.queryForList("""
                SELECT id,driver_id,route_id,route_version FROM trip
                WHERE tenant_id=? AND vehicle_id=? AND actual_start_time IS NOT NULL
                  AND actual_start_time<=? AND (actual_end_time IS NULL OR actual_end_time>=?)
                  AND status NOT IN('CANCELLED','REJECTED')
                ORDER BY actual_start_time DESC,id DESC LIMIT 1
                """, tenant, vehicle, Timestamp.from(sourceTime), Timestamp.from(sourceTime));
        return found.isEmpty() ? Map.of() : found.getFirst();
    }

    private void seedEpisodes(UUID vehicle, int count) {
        jdbc.update("""
                INSERT INTO tracking_route_deviation_episode(
                 id,tenant_id,vehicle_id,route_id,route_version,rule_id,rule_version,
                 configured_tolerance_meters,effective_tolerance_meters,first_candidate_position_id,
                 confirming_position_id,start_source_timestamp,confirmation_source_timestamp,
                 end_source_timestamp,maximum_distance_meters,eligible_outside_sample_count,severity,
                 review_status)
                SELECT gen_random_uuid(),?,?,gen_random_uuid(),'REVISION:1',gen_random_uuid(),1,
                 100,100,gen_random_uuid(),gen_random_uuid(),CAST(? AS timestamptz)-(value*interval '1 second'),
                 CAST(? AS timestamptz)-(value*interval '1 second'),
                 CAST(? AS timestamptz)-(value*interval '1 second'),150,2,'WARNING','NOT_REQUIRED'
                FROM generate_series(1,?) value
                """, TENANT, vehicle, Timestamp.from(BASE), Timestamp.from(BASE.plusSeconds(1)),
                Timestamp.from(BASE.plusSeconds(60)), count);
    }

    private UUID seedTripReferences(UUID vehicle) {
        ReferenceFixtures.vehicleReference(jdbc, vehicle);
        ReferenceFixtures.locations(jdbc, ORIGIN, DESTINATION);
        UUID route = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO route(id,code,name,origin_location_id,destination_location_id,
                 planned_distance_km,estimated_duration_minutes,active,tenant_id)
                VALUES(?,?, 'CS07 route',?,?,10,30,true,?)
                """, route, "CS07-" + route.toString().substring(0, 12), ORIGIN, DESTINATION, TENANT);
        return route;
    }

    private void seedTrips(UUID vehicle, UUID route, int count) {
        jdbc.update("""
                INSERT INTO trip(id,trip_number,priority,status,origin_location_id,destination_location_id,
                 requested_start_time,requested_end_time,vehicle_id,actual_start_time,actual_end_time,
                 created_at,updated_at,tenant_id,route_id,route_version)
                SELECT gen_random_uuid(),'CS07-'||value,'NORMAL',
                 CASE WHEN value%101=0 THEN 'CANCELLED' WHEN value%103=0 THEN 'REJECTED' ELSE 'IN_PROGRESS' END,
                 ?,?,?,?, ?,CAST(? AS timestamptz)+value*interval '1 second',
                 CAST(? AS timestamptz)+value*interval '1 second',now(),now(),?,?,
                 CASE WHEN value%17=0 THEN NULL ELSE 'REVISION:7' END
                FROM generate_series(1,?) value
                """, ORIGIN, DESTINATION, Timestamp.from(BASE), Timestamp.from(BASE.plusSeconds(600)), vehicle,
                Timestamp.from(BASE), Timestamp.from(BASE.plusSeconds(600)), TENANT, route, count);
    }

    private record IndexState(String name, boolean valid, boolean ready) { }
}
