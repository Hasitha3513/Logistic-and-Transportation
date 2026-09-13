package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class HybridTelemetryTimescaleMigrationAcceptanceTest {

    @Test
    void cleanV1ToV87CreatesExactTimescalePolicies() throws Exception {
        var image = DockerImageName.parse("timescale/timescaledb:latest-pg16")
                .asCompatibleSubstituteFor("postgres");
        try (var database = new PostgreSQLContainer<>(image)
                .withDatabaseName("transport_timescale_acceptance")
                .withUsername("transport_test")
                .withPassword("transport_test")) {
            database.start();
            Flyway.configure()
                    .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                    .placeholders(placeholdersForTs04())
                    .load()
                    .migrate();

            try (var connection = DriverManager.getConnection(
                    database.getJdbcUrl(), database.getUsername(), database.getPassword());
                    var statement = connection.createStatement()) {
                try (var result = statement.executeQuery(
                        "select extversion from pg_extension where extname='timescaledb'")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isNotBlank();
                }
                try (var result = statement.executeQuery(
                        "select count(*) from timescaledb_information.hypertables "
                                + "where hypertable_schema='public' "
                                + "and hypertable_name='tracking_position_history'")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isOne();
                }
                try (var result = statement.executeQuery(
                        "select count(*) from information_schema.columns "
                                + "where table_schema='public' "
                                + "and table_name='tracking_position_history' "
                                + "and column_name='tenant_id' and is_nullable='NO'")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isOne();
                }
                try (var result = statement.executeQuery(
                        "select time_interval from timescaledb_information.dimensions "
                                + "where hypertable_name='tracking_position_history'")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo("7 days");
                }
                try (var result = statement.executeQuery(
                        "select count(*) from timescaledb_information.jobs "
                                + "where hypertable_name='tracking_position_history' "
                                + "and proc_name='policy_compression' "
                                + "and config->>'compress_after'='7 days'")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isOne();
                }
                try (var result = statement.executeQuery(
                        "select count(*) from timescaledb_information.jobs "
                                + "where hypertable_name='tracking_position_history' "
                                + "and proc_name='policy_retention' "
                                + "and config->>'drop_after'='180 days'")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isOne();
                }
            }
        }
    }

    @Test
    void v86ToV87PreservesExistingHistory() throws Exception {
        var image = DockerImageName.parse("timescale/timescaledb:latest-pg16")
                .asCompatibleSubstituteFor("postgres");
        try (var database = new PostgreSQLContainer<>(image)
                .withDatabaseName("transport_timescale_upgrade")
                .withUsername("transport_test")
                .withPassword("transport_test")) {
            database.start();
            var configuration = Flyway.configure()
                    .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                    .placeholders(placeholdersForTs04());
            configuration.target("86").load().migrate();
            UUID tenant = UUID.randomUUID();
            UUID event = UUID.randomUUID();
            try (var connection = DriverManager.getConnection(
                    database.getJdbcUrl(), database.getUsername(), database.getPassword());
                    var statement = connection.prepareStatement("""
                            INSERT INTO tracking_position_history(
                              tenant_id,source_timestamp,id,device_id,vehicle_id,provider_alias,
                              dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                              ordering_classification,retention_policy,retention_policy_version)
                            VALUES(?,?,?,?,?,'GENERIC',?,now(),1,2,'UNKNOWN','UNKNOWN',
                              'ACCURACY_UNKNOWN','IN_ORDER','EXTERNAL','V86')
                            """)) {
                statement.setObject(1, tenant);
                statement.setTimestamp(2, java.sql.Timestamp.from(java.time.Instant.now()));
                statement.setObject(3, event);
                statement.setObject(4, UUID.randomUUID());
                statement.setObject(5, UUID.randomUUID());
                statement.setString(6, "a".repeat(64));
                statement.executeUpdate();
            }
            configuration.target("87").load().migrate();
            try (var connection = DriverManager.getConnection(
                    database.getJdbcUrl(), database.getUsername(), database.getPassword());
                    var statement = connection.prepareStatement(
                            "select event_version from tracking_position_history "
                                    + "where tenant_id=? and id=?")) {
                statement.setObject(1, tenant);
                statement.setObject(2, event);
                try (var result = statement.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isOne();
                }
            }
        }
    }

    public static Map<String, String> placeholdersForTs04() {
        return Map.of(
                "deliveryStatusConstraintUpgrade",
                "ALTER TABLE delivery_order DROP CONSTRAINT IF EXISTS delivery_order_status_check;"
                        + "ALTER TABLE delivery_order DROP CONSTRAINT IF EXISTS ck_delivery_order_status;"
                        + "ALTER TABLE delivery_order ADD CONSTRAINT ck_delivery_order_status "
                        + "CHECK (status IN ('DRAFT','READY_FOR_ASSIGNMENT','DELIVERED'));",
                "podEvidenceUniquenessIndexes",
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_pod_single_signature "
                        + "ON pod_evidence(proof_of_delivery_id) WHERE evidence_type='SIGNATURE';"
                        + "CREATE UNIQUE INDEX IF NOT EXISTS uk_pod_single_barcode "
                        + "ON pod_evidence(proof_of_delivery_id) WHERE evidence_type='BARCODE';",
                "trackingTimescaleSetup", "CREATE EXTENSION IF NOT EXISTS timescaledb;",
                "trackingTimescaleHypertable",
                "SELECT create_hypertable('tracking_position_history',"
                        + "by_range('source_timestamp', INTERVAL '1 day'),if_not_exists=>TRUE);",
                "trackingTimescalePolicyHardening",
                "SELECT set_chunk_time_interval('tracking_position_history', INTERVAL '7 days');"
                        + "ALTER TABLE tracking_position_history SET (timescaledb.compress,"
                        + "timescaledb.compress_segmentby='tenant_id,vehicle_id',"
                        + "timescaledb.compress_orderby='source_timestamp DESC,id DESC');"
                        + "SELECT add_compression_policy('tracking_position_history',"
                        + "INTERVAL '7 days',if_not_exists=>TRUE);"
                        + "SELECT add_retention_policy('tracking_position_history',"
                        + "INTERVAL '180 days',if_not_exists=>TRUE);");
    }
}
