package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
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

class TrackingDashboardV95IndexPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT_A = UUID.fromString("54000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("54000000-0000-0000-0000-000000000002");
    private static final Instant BASE = Instant.parse("2026-09-16T00:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired DataSource dataSource;

    @Test
    void cleanV95HasExactlyOneReadyTenantLeadingDashboardIndex() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("98");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version='95' AND success",
                Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT pg_get_indexdef(?::regclass)", String.class,
                "idx_tracking_speed_episode_dashboard_recent"))
                .contains("USING btree (tenant_id, confirmation_source_timestamp DESC, id DESC)")
                .doesNotContain("INCLUDE", "WHERE");
        Map<String, Object> state = jdbc.queryForMap("""
                SELECT indisready,indisvalid FROM pg_index
                WHERE indexrelid='idx_tracking_speed_episode_dashboard_recent'::regclass
                """);
        assertThat(state).containsEntry("indisready", true).containsEntry("indisvalid", true);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_indexes WHERE schemaname='public'
                  AND tablename='tracking_speed_episode'
                  AND indexdef LIKE '%(tenant_id, confirmation_source_timestamp DESC, id DESC)%'
                """, Integer.class)).isOne();

        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE INDEX cs05_v95_transaction_probe ON tracking_speed_episode(tenant_id)");
                statement.execute("CREATE INDEX cs05_v95_forced_failure ON missing_table(id)");
            } catch (SQLException expected) {
                connection.rollback();
            }
        }
        assertThat(jdbc.queryForObject("SELECT to_regclass('cs05_v95_transaction_probe')", String.class)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM pg_index WHERE NOT indisvalid OR NOT indisready", Integer.class)).isZero();
    }

    @Test
    void v94ToV95PreservesResultsEliminatesScanAndSortAndExcludesForeignTenant() {
        flyway.clean();
        Flyway throughV94 = Flyway.configure().configuration(flyway.getConfiguration())
                .target("94").load();
        throughV94.migrate();
        assertThat(throughV94.info().current().getVersion().getVersion()).isEqualTo("94");
        seed(TENANT_A, 10_000, 50, 0);
        seed(TENANT_B, 2_000, 20, 20_000);
        jdbc.execute("ANALYZE tracking_speed_episode");

        List<UUID> vehicles = vehicleIds(40);
        List<Map<String, Object>> before = query(TENANT_A, vehicles);
        String beforePlan = plan(TENANT_A, vehicles, "US54_V95_PLAN_BEFORE");
        assertThat(beforePlan).contains("Seq Scan on tracking_speed_episode", "Sort");

        flyway.migrate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("98");
        jdbc.execute("ANALYZE tracking_speed_episode");
        List<Map<String, Object>> after = query(TENANT_A, vehicles);
        String afterPlan = plan(TENANT_A, vehicles, "US54_V95_PLAN_AFTER");

        assertThat(after).isEqualTo(before).hasSize(20);
        assertThat(afterPlan).contains("idx_tracking_speed_episode_dashboard_recent")
                .doesNotContain("Seq Scan on tracking_speed_episode", "Sort");
        assertThat(query(TENANT_B, vehicles)).isEmpty();
        assertThat(after).isSortedAccordingTo((left, right) -> {
            int timestamp = ((Timestamp) right.get("source_timestamp"))
                    .compareTo((Timestamp) left.get("source_timestamp"));
            return timestamp != 0 ? timestamp
                    : ((UUID) right.get("id")).compareTo((UUID) left.get("id"));
        });
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version='95' AND success",
                Integer.class)).isOne();
    }

    private void seed(UUID tenantId, int count, int vehicles, int offset) {
        jdbc.update("""
                INSERT INTO tracking_speed_episode(
                  id,tenant_id,vehicle_id,rule_id,rule_version,threshold_source,
                  effective_threshold_kph,start_source_timestamp,confirmation_source_timestamp,
                  end_source_timestamp,max_observed_speed_kph,eligible_above_threshold_sample_count,
                  severity,repeat_count,first_candidate_position_id,confirming_position_id)
                SELECT md5(? || ':speed:' || value)::uuid,?,
                  md5('54000000-vehicle-' || (value % ?))::uuid,
                  md5(? || ':rule')::uuid,1,'TENANT_CONFIG',80,
                  CAST(? AS timestamptz) - ((value + ?) * interval '10 seconds'),
                  CAST(? AS timestamptz) - ((value + ?) * interval '10 seconds'),
                  CAST(? AS timestamptz) - ((value + ?) * interval '10 seconds') + interval '1 second',
                  90,2,CASE WHEN value % 2=0 THEN 'WARNING' ELSE 'HIGH' END,0,
                  md5(? || ':first:' || value)::uuid,md5(? || ':confirm:' || value)::uuid
                FROM generate_series(1,?) value
                """, tenantId.toString(), tenantId, vehicles, tenantId.toString(), Timestamp.from(BASE), offset,
                Timestamp.from(BASE), offset, Timestamp.from(BASE), offset,
                tenantId.toString(), tenantId.toString(), count);
    }

    private List<UUID> vehicleIds(int count) {
        return jdbc.queryForList("""
                SELECT md5('54000000-vehicle-' || value)::uuid
                FROM generate_series(0,?) value
                """, UUID.class, count - 1);
    }

    private List<Map<String, Object>> query(UUID tenantId, List<UUID> vehicles) {
        var parameters = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("tenantId", tenantId).addValue("vehicleIds", vehicles)
                .addValue("fromInclusive", Timestamp.from(BASE.minusSeconds(86_400)))
                .addValue("toExclusive", Timestamp.from(BASE.plusSeconds(1))).addValue("limit", 20);
        return new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc).queryForList("""
                SELECT id,vehicle_id,trip_id,route_id,route_version,severity,
                       CASE WHEN end_source_timestamp IS NULL THEN 'OPEN' ELSE 'CLOSED' END AS status,
                       confirmation_source_timestamp AS source_timestamp
                FROM tracking_speed_episode
                WHERE tenant_id=:tenantId
                  AND confirmation_source_timestamp>=:fromInclusive
                  AND confirmation_source_timestamp<:toExclusive
                  AND vehicle_id IN (:vehicleIds)
                ORDER BY confirmation_source_timestamp DESC,id DESC LIMIT :limit
                """, parameters);
    }

    private String plan(UUID tenantId, List<UUID> vehicles, String label) {
        var parameters = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("tenantId", tenantId).addValue("vehicleIds", vehicles)
                .addValue("fromInclusive", Timestamp.from(BASE.minusSeconds(86_400)))
                .addValue("toExclusive", Timestamp.from(BASE.plusSeconds(1))).addValue("limit", 20);
        String value = String.join("\n", new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc)
                .queryForList("EXPLAIN (ANALYZE,BUFFERS,COSTS OFF) " + """
                        SELECT id,vehicle_id,trip_id,route_id,route_version,severity,
                               CASE WHEN end_source_timestamp IS NULL THEN 'OPEN' ELSE 'CLOSED' END AS status,
                               confirmation_source_timestamp AS source_timestamp
                        FROM tracking_speed_episode
                        WHERE tenant_id=:tenantId
                          AND confirmation_source_timestamp>=:fromInclusive
                          AND confirmation_source_timestamp<:toExclusive
                          AND vehicle_id IN (:vehicleIds)
                        ORDER BY confirmation_source_timestamp DESC,id DESC LIMIT :limit
                        """, parameters, String.class));
        System.out.println(label + "\n" + value);
        return value;
    }
}
