package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class HybridTelemetryTimescaleMigrationAcceptanceTest {

    @Test
    void v86EnablesTimescaleAndCreatesTenantQualifiedHistoryHypertable() throws Exception {
        var image = DockerImageName.parse("timescale/timescaledb:latest-pg16")
                .asCompatibleSubstituteFor("postgres");
        try (var database = new PostgreSQLContainer<>(image)
                .withDatabaseName("transport_timescale_acceptance")
                .withUsername("transport_test")
                .withPassword("transport_test")) {
            database.start();
            Flyway.configure()
                    .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                    .placeholders(placeholders())
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
            }
        }
    }

    private static Map<String, String> placeholders() {
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
                        + "by_range('source_timestamp', INTERVAL '1 day'),if_not_exists=>TRUE);");
    }
}
