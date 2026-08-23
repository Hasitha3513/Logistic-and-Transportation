package com.transportlogistics.app.postgresql;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.DockerClientFactory;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("postgres")
@EnabledIf("dockerAvailable")
class OfflineSyncPostgreSqlInvariantIntegrationTest extends PostgreSqlIntegrationTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID CLIENT_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID AGGREGATE_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-22T10:00:00Z");

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;

    private static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @BeforeEach
    void migrateCleanDatabase() {
        flyway.clean();
        flyway.migrate();
        jdbc.update("INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ACTOR_ID, "offline.postgres", "offline.postgres@example.test", "hash", "Offline", "Postgres",
                true, NOW, NOW);
    }

    @Test
    void v29UsesPostgreSqlUuidTimestamptzIndexesAndConstraints() {
        UUID operationId = UUID.randomUUID();
        insert(operationId, 1, ACTOR_ID, "APPLIED");

        assertEquals("uuid", columnType("operation_id"));
        assertEquals("timestamp with time zone", columnType("processed_at"));
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'offline_sync_operation' AND indexname LIKE 'idx_offline_sync_%'",
                Integer.class));
        assertThrows(DataIntegrityViolationException.class, () -> insert(operationId, 1, ACTOR_ID, "APPLIED"));
        assertThrows(DataIntegrityViolationException.class, () -> insert(UUID.randomUUID(), 0, ACTOR_ID, "APPLIED"));
        assertThrows(DataIntegrityViolationException.class, () -> insert(UUID.randomUUID(), 1, UUID.randomUUID(), "APPLIED"));
        assertThrows(DataIntegrityViolationException.class, () -> insert(UUID.randomUUID(), 1, ACTOR_ID, "RETRYABLE_ERROR"));
    }

    private String columnType(String column) {
        return jdbc.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_name = 'offline_sync_operation' AND column_name = ?",
                String.class, column);
    }

    private void insert(UUID operationId, int version, UUID actorId, String status) {
        jdbc.update("INSERT INTO offline_sync_operation (operation_id, operation_type, operation_version, actor_id, client_instance_id, aggregate_type, aggregate_id, request_hash, result_status, processed_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                operationId, "VEHICLE_READING_RECORD", version, actorId, CLIENT_ID, "VEHICLE", AGGREGATE_ID,
                "a".repeat(64), status, NOW, NOW);
    }
}
