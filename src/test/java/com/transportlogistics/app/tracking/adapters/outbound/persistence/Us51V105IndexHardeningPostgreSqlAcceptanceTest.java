package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.AcceptanceDatabaseGuard;
import com.transportlogistics.app.tracking.HybridTelemetryTimescaleMigrationAcceptanceTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class Us51V105IndexHardeningPostgreSqlAcceptanceTest {
    private static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>(DockerImageName
            .parse("timescale/timescaledb:latest-pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName(AcceptanceDatabaseGuard.REQUIRED_DATABASE)
            .withUsername("transport_test").withPassword("transport_test");
    private static JdbcTemplate jdbc;
    private static Flyway flyway;

    @BeforeAll static void start() {
        DB.start();
        var dataSource = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        AcceptanceDatabaseGuard.verify(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04()).load();
    }

    @AfterAll static void stop() { DB.stop(); }

    @BeforeEach void reset() { flyway.clean(); flyway.migrate(); }

    @Test void cleanHeadContainsExactlyTheThreeReadyValidV105Indexes() {
        assertThat(jdbc.queryForObject("select max(version::integer) from flyway_schema_history where success",
                Integer.class)).isEqualTo(105);
        Map<String, String> definitions = jdbc.query("""
                SELECT c.relname, pg_get_indexdef(c.oid)
                FROM pg_class c JOIN pg_index i ON i.indexrelid=c.oid
                WHERE c.relname IN ('idx_tracking_idle_state_keyset',
                  'idx_tracking_idle_episode_tenant_keyset',
                  'idx_tracking_telemetry_dispatch_idle_due')
                  AND i.indisready AND i.indisvalid
                """, result -> {
                    var value = new java.util.HashMap<String, String>();
                    while (result.next()) value.put(result.getString(1), result.getString(2));
                    return value;
                });
        assertThat(definitions).hasSize(3);
        assertThat(definitions.get("idx_tracking_idle_state_keyset"))
                .contains("tenant_id, latest_source_timestamp DESC, vehicle_id DESC");
        assertThat(definitions.get("idx_tracking_idle_episode_tenant_keyset"))
                .contains("tenant_id, start_source_timestamp DESC, id DESC")
                .contains("lifecycle").contains("CANDIDATE");
        assertThat(definitions.get("idx_tracking_telemetry_dispatch_idle_due"))
                .contains("next_attempt_at, tenant_id, vehicle_id, source_timestamp, dispatch_id")
                .contains("evaluator_type").contains("IDLE");
    }

    @Test void populatedV104UpgradePreservesDataAndUsesAlignedProductionPlans() {
        flyway.clean();
        Flyway.configure().dataSource(jdbc.getDataSource()).target("104")
                .placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04())
                .load().migrate();
        UUID tenant = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update("""
                INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,lifecycle,
                 registered_at,registered_by,version,created_at,updated_at)
                VALUES(?,?,?,'FIXTURE','ACTIVE',?,?,0,?,?)
                """, device, tenant, device.toString(), Timestamp.from(base), UUID.randomUUID(),
                Timestamp.from(base), Timestamp.from(base));
        jdbc.update("""
                INSERT INTO tracking_idle_state(tenant_id,vehicle_id,device_id,state,capability_state,
                 latest_source_timestamp,credited_seconds,evidence_count,last_dedupe_identity)
                SELECT ?,md5('v105-state-'||g)::uuid,?,'NORMAL','SUPPORTED',?::timestamptz
                 + g*interval '1 second',0,1,md5('a'||g)||md5('b'||g)
                FROM generate_series(1,10000) g
                """, tenant, device, Timestamp.from(base));
        jdbc.update("""
                INSERT INTO tracking_idle_episode(id,tenant_id,vehicle_id,device_id,lifecycle,
                 start_source_timestamp,confirmed_at,last_source_timestamp,end_source_timestamp,end_reason,
                 credited_seconds,evidence_count)
                SELECT md5('v105-episode-'||g)::uuid,?,md5('v105-episode-vehicle-'||g)::uuid,?,'CLOSED',
                 ?::timestamptz+g*interval '1 second',?::timestamptz+g*interval '1 second',
                 ?::timestamptz+g*interval '1 second',?::timestamptz+g*interval '1 second',
                 'ENGINE_STOPPED',300,3 FROM generate_series(1,10000) g
                """, tenant, device, Timestamp.from(base), Timestamp.from(base.plusSeconds(300)),
                Timestamp.from(base.plusSeconds(300)), Timestamp.from(base.plusSeconds(300)));
        jdbc.update("""
                INSERT INTO tracking_telemetry_evaluation_dispatch(dispatch_id,tenant_id,source_timestamp,
                 history_id,dedupe_identity,vehicle_id,evaluator_type,status,next_attempt_at,
                 lease_owner,lease_until,completed_at)
                SELECT md5('v105-dispatch-'||g)::uuid,?,?::timestamptz+g*interval '1 second',
                 md5('v105-history-'||g)::uuid,md5('c'||g)||md5('d'||g),
                 md5('v105-dispatch-vehicle-'||(g%100))::uuid,'IDLE',
                 CASE WHEN g%10=0 THEN 'COMPLETED' WHEN g%10=1 THEN 'PROCESSING'
                      WHEN g%10=2 THEN 'FAILED' ELSE 'PENDING' END,
                 CASE WHEN g<=1000 THEN ?::timestamptz+g*interval '1 second'
                      ELSE ?::timestamptz+g*interval '1 second' END,
                 CASE WHEN g%10=1 THEN 'worker-a' ELSE NULL END,
                 CASE WHEN g%10=1 AND g<=1000 THEN ?::timestamptz-interval '1 minute'
                      WHEN g%10=1 THEN ?::timestamptz+interval '1 hour' ELSE NULL END,
                 CASE WHEN g%10=0 THEN ?::timestamptz+g*interval '1 second' ELSE NULL END
                FROM generate_series(1,10000) g
                """, tenant, Timestamp.from(base), Timestamp.from(base),
                Timestamp.from(Instant.parse("2028-01-01T00:00:00Z")),
                Timestamp.from(Instant.parse("2027-01-01T00:00:00Z")),
                Timestamp.from(Instant.parse("2027-01-01T00:00:00Z")), Timestamp.from(base));
        flyway.migrate();
        jdbc.execute("ANALYZE tracking_idle_state");
        jdbc.execute("ANALYZE tracking_idle_episode");
        jdbc.execute("ANALYZE tracking_telemetry_evaluation_dispatch");
        assertPlanUses("""
                SELECT vehicle_id,state,capability_state,latest_source_timestamp,candidate_started_at,
                 last_qualifying_at,credited_seconds,evidence_count,version
                FROM tracking_idle_state WHERE tenant_id='%s'
                ORDER BY latest_source_timestamp DESC,vehicle_id DESC LIMIT 50
                """.formatted(tenant), "idx_tracking_idle_state_keyset");
        assertPlanUses("""
                SELECT vehicle_id,state,capability_state,latest_source_timestamp,candidate_started_at,
                 last_qualifying_at,credited_seconds,evidence_count,version
                FROM tracking_idle_state WHERE tenant_id='%s'
                 AND (latest_source_timestamp,vehicle_id)<('2026-01-01 02:00:00+00',
                  md5('v105-state-7200')::uuid)
                ORDER BY latest_source_timestamp DESC,vehicle_id DESC LIMIT 50
                """.formatted(tenant), "idx_tracking_idle_state_keyset");
        assertPlanUses("""
                SELECT id,vehicle_id,lifecycle,start_source_timestamp,confirmed_at,last_source_timestamp,
                 end_source_timestamp,end_reason,credited_seconds,evidence_count,version
                FROM tracking_idle_episode WHERE tenant_id='%s' AND lifecycle<>'CANDIDATE'
                 AND start_source_timestamp>='2026-01-01' AND start_source_timestamp<'2027-01-01'
                ORDER BY start_source_timestamp DESC,id DESC LIMIT 50
                """.formatted(tenant), "idx_tracking_idle_episode_tenant_keyset");
        assertPlanUses("""
                SELECT id,vehicle_id,lifecycle,start_source_timestamp,confirmed_at,last_source_timestamp,
                 end_source_timestamp,end_reason,credited_seconds,evidence_count,version
                FROM tracking_idle_episode WHERE tenant_id='%s' AND lifecycle<>'CANDIDATE'
                 AND start_source_timestamp>='2026-01-01' AND start_source_timestamp<'2027-01-01'
                 AND (start_source_timestamp,id)<('2026-01-01 02:00:00+00',
                  md5('v105-episode-7200')::uuid)
                ORDER BY start_source_timestamp DESC,id DESC LIMIT 50
                """.formatted(tenant), "idx_tracking_idle_episode_tenant_keyset");
        assertPlanUses("""
                SELECT dispatch_id FROM tracking_telemetry_evaluation_dispatch
                WHERE evaluator_type='IDLE' AND next_attempt_at<='2027-01-01'
                 AND (status IN('PENDING','FAILED') OR (status='PROCESSING' AND lease_until<='2027-01-01'))
                ORDER BY next_attempt_at,tenant_id,vehicle_id,source_timestamp,evaluator_type,dispatch_id LIMIT 100
                """, "idx_tracking_telemetry_dispatch_idle_due");
        assertThat(jdbc.queryForObject("select count(*) from tracking_idle_state where tenant_id=?",
                Integer.class, tenant)).isEqualTo(10000);
    }

    private static void assertPlanUses(String sql, String index) {
        String plan = String.join("\n", jdbc.queryForList("EXPLAIN (ANALYZE,BUFFERS) " + sql, String.class));
        System.out.println("V105_PLAN[" + index + "]\n" + plan);
        assertThat(plan).contains(index).doesNotContain("Seq Scan");
    }
}
