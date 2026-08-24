package com.transportlogistics.app.offlinesync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResult;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResultStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.transportlogistics.app.support.ReferenceFixtures.vehicleHierarchy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties = {
        "app.dev.identity-bootstrap.enabled=false",
        "app.dev.sample-data.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VehicleReadingOfflineSyncIntegrationTest {
    private static final OffsetDateTime RECORDED_AT = OffsetDateTime.parse("2026-08-20T10:00:00Z");

    @Autowired OfflineSyncUseCase offlineSync;
    @Autowired VehicleReadingUseCase readings;
    @Autowired VehicleRepository vehicles;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private UUID actorId;
    private UUID vehicleId;
    private String username;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM offline_sync_operation");
        jdbc.update("DELETE FROM vehicle_reading");
        actorId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        username = "offline-reading-" + actorId;
        jdbc.update("""
                INSERT INTO app_user
                    (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, actorId, username, actorId + "@test.local", "unused", "Offline", "Reader", true,
                RECORDED_AT, RECORDED_AT);
        Vehicle vehicle = new Vehicle(vehicleId, "OFF-" + actorId, null, null, UUID.randomUUID(), UUID.randomUUID(),
                "Maker", "Model", 2026, "OWNED", "AVAILABLE", null, null, 1000d, true);
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM offline_sync_operation WHERE actor_id = ?", actorId);
        jdbc.update("DELETE FROM vehicle_reading WHERE vehicle_id = ?", vehicleId);
        jdbc.update("DELETE FROM vehicle WHERE id = ?", vehicleId);
        jdbc.update("DELETE FROM app_user_role WHERE user_id = ?", actorId);
        jdbc.update("DELETE FROM refresh_token WHERE user_id = ?", actorId);
        jdbc.update("DELETE FROM app_user WHERE id = ?", actorId);
    }

    @Test
    void appliesOnceReplaysAndPersistsManualFactsFromServerContext() {
        UUID operationId = UUID.randomUUID();
        var command = command(operationId, vehicleId, Map.of(
                "readingType", "ODOMETER", "value", 123.456,
                "recordedAt", RECORDED_AT.toString(), "notes", "offline capture"));

        assertEquals(OfflineSyncResultStatus.APPLIED, sync(command, Set.of("VEHICLE_READING_CREATE")).status());
        assertEquals(OfflineSyncResultStatus.ALREADY_APPLIED,
                sync(command, Set.of("VEHICLE_READING_CREATE")).status());

        var stored = readings.list(new VehicleReadingUseCase.SearchQuery(vehicleId, null, null,
                null, null, 0, 20)).content();
        assertEquals(1, stored.size());
        assertEquals(VehicleReadingSourceType.MANUAL, stored.getFirst().sourceType());
        assertEquals(actorId, stored.getFirst().createdBy());
        assertEquals(operationId.toString(), stored.getFirst().idempotencyKey());
        assertNull(stored.getFirst().sourceReferenceId());
    }

    @Test
    void classifiesPayloadConflictMissingVehicleAndRevokedPermission() {
        var invalid = command(UUID.randomUUID(), vehicleId, Map.of(
                "readingType", "ODOMETER", "value", -1, "recordedAt", RECORDED_AT.toString()));
        assertEquals("OFFLINE_SYNC_PAYLOAD_INVALID", sync(invalid, Set.of("VEHICLE_READING_CREATE")).errorCode());

        UUID firstId = UUID.randomUUID();
        assertEquals(OfflineSyncResultStatus.APPLIED, sync(command(firstId, vehicleId, Map.of(
                "readingType", "ODOMETER", "value", 200, "recordedAt", RECORDED_AT.toString())),
                Set.of("VEHICLE_READING_CREATE")).status());
        var decreasing = sync(command(UUID.randomUUID(), vehicleId, Map.of(
                "readingType", "ODOMETER", "value", 100, "recordedAt", RECORDED_AT.plusHours(1).toString())),
                Set.of("VEHICLE_READING_CREATE"));
        assertEquals("OFFLINE_SYNC_CONFLICT", decreasing.errorCode());

        var missing = sync(command(UUID.randomUUID(), UUID.randomUUID(), Map.of(
                "readingType", "ENGINE_HOURS", "value", 1, "recordedAt", RECORDED_AT.toString())),
                Set.of("VEHICLE_READING_CREATE"));
        assertEquals("VEHICLE_NOT_FOUND", missing.errorCode());

        var forbidden = sync(command(UUID.randomUUID(), vehicleId, Map.of(
                "readingType", "ENGINE_HOURS", "value", 1, "recordedAt", RECORDED_AT.toString())), Set.of());
        assertEquals("OFFLINE_SYNC_FORBIDDEN", forbidden.errorCode());
    }

    @Test
    void changedPayloadForSameEnvelopeIsAnIdempotencyMismatch() {
        UUID operationId = UUID.randomUUID();
        sync(command(operationId, vehicleId, Map.of(
                "readingType", "ENGINE_HOURS", "value", 10, "recordedAt", RECORDED_AT.toString())),
                Set.of("VEHICLE_READING_CREATE"));
        OfflineSyncResult changed = sync(command(operationId, vehicleId, Map.of(
                "readingType", "ENGINE_HOURS", "value", 11, "recordedAt", RECORDED_AT.toString())),
                Set.of("VEHICLE_READING_CREATE"));
        assertEquals("OFFLINE_SYNC_IDEMPOTENCY_MISMATCH", changed.errorCode());
    }

    @Test
    void appliesEngineHoursAndKeepsUnsupportedTripIndependentInAMixedBatch() {
        var vehicle = command(UUID.randomUUID(), vehicleId, Map.of(
                "readingType", "ENGINE_HOURS", "value", 12.5, "recordedAt", RECORDED_AT.toString()));
        UUID tripOperationId = UUID.randomUUID();
        var forbiddenTrip = new OfflineSyncUseCase.OperationCommand(tripOperationId, "TRIP_CHECKPOINT_RECORD", 1,
                "TRIP", UUID.randomUUID(), json.valueToTree(Map.of("checkpoint", "Depot")),
                RECORDED_AT, RECORDED_AT, UUID.randomUUID(), tripOperationId.toString(), null);

        var results = offlineSync.synchronize(new OfflineSyncUseCase.BatchCommand(username,
                Set.of("VEHICLE_READING_CREATE"), List.of(vehicle, forbiddenTrip))).results();

        assertEquals(OfflineSyncResultStatus.APPLIED, results.get(0).status());
        assertEquals(OfflineSyncResultStatus.REJECTED, results.get(1).status());
        assertEquals("OFFLINE_SYNC_FORBIDDEN", results.get(1).errorCode());
        assertEquals(VehicleReadingSourceType.MANUAL, readings.list(new VehicleReadingUseCase.SearchQuery(
                vehicleId, null, null, null, null, 0, 20)).content().getFirst().sourceType());
    }

    @Test
    void rejectsVehicleReadingWithWrongAggregateTypeBeforeMutation() {
        UUID operationId = UUID.randomUUID();
        var wrongAggregate = new OfflineSyncUseCase.OperationCommand(operationId, "VEHICLE_READING_RECORD", 1,
                "TRIP", vehicleId, json.valueToTree(Map.of(
                "readingType", "ODOMETER", "value", 10, "recordedAt", RECORDED_AT.toString())),
                RECORDED_AT, RECORDED_AT, UUID.randomUUID(), operationId.toString(), null);

        OfflineSyncResult result = sync(wrongAggregate, Set.of("VEHICLE_READING_CREATE"));

        assertEquals(OfflineSyncResultStatus.REJECTED, result.status());
        assertEquals("OFFLINE_SYNC_PAYLOAD_INVALID", result.errorCode());
        assertEquals(0, readings.list(new VehicleReadingUseCase.SearchQuery(
                vehicleId, null, null, null, null, 0, 20)).content().size());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void concurrentReplayCreatesOnlyOneVehicleReading() throws Exception {
        UUID operationId = UUID.randomUUID();
        var command = command(operationId, vehicleId, Map.of(
                "readingType", "ODOMETER", "value", 321, "recordedAt", RECORDED_AT.toString()));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> sync(command, Set.of("VEHICLE_READING_CREATE")).status());
            var second = executor.submit(() -> sync(command, Set.of("VEHICLE_READING_CREATE")).status());
            assertEquals(Set.of(OfflineSyncResultStatus.APPLIED, OfflineSyncResultStatus.ALREADY_APPLIED),
                    Set.of(first.get(), second.get()));
            assertEquals(1, readings.list(new VehicleReadingUseCase.SearchQuery(vehicleId, null, null,
                    null, null, 0, 20)).content().size());
        } finally {
            executor.shutdownNow();
        }
    }

    private OfflineSyncResult sync(OfflineSyncUseCase.OperationCommand command, Set<String> authorities) {
        return offlineSync.synchronize(new OfflineSyncUseCase.BatchCommand(username, authorities, List.of(command)))
                .results().getFirst();
    }

    private OfflineSyncUseCase.OperationCommand command(UUID operationId, UUID aggregateId,
                                                         Map<String, Object> payload) {
        return new OfflineSyncUseCase.OperationCommand(operationId, "VEHICLE_READING_RECORD", 1,
                "VEHICLE", aggregateId, json.valueToTree(payload), RECORDED_AT, RECORDED_AT,
                UUID.randomUUID(), operationId.toString(), null);
    }
}
