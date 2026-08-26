package com.transportlogistics.app.offlinesync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResult;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResultStatus;
import com.transportlogistics.app.trip.application.ports.in.TripOperationalEventUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripOperationalEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static com.transportlogistics.app.support.ReferenceFixtures.locations;
import static com.transportlogistics.app.support.ReferenceFixtures.vehicleHierarchy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "app.dev.identity-bootstrap.enabled=false",
        "app.dev.sample-data.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TripOperationalEventOfflineSyncIntegrationTest {
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-08-20T10:15:00Z");

    @Autowired OfflineSyncUseCase offlineSync;
    @Autowired TripOperationalEventUseCase tripEvents;
    @Autowired TripRepository trips;
    @Autowired VehicleRepository vehicles;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private UUID actorId;
    private UUID secondActorId;
    private UUID tripId;
    private UUID draftTripId;
    private UUID originId;
    private UUID destinationId;
    private UUID ruleId;
    private String username;
    private String secondUsername;
    private Vehicle mixedVehicle;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
        secondActorId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        draftTripId = UUID.randomUUID();
        originId = UUID.randomUUID();
        destinationId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        username = "offline-trip-" + actorId;
        secondUsername = "offline-trip-" + secondActorId;
        insertUser(actorId, username);
        insertUser(secondActorId, secondUsername);
        locations(jdbc, originId, destinationId);
        trips.save(trip(tripId, "IN_PROGRESS"));
        trips.save(trip(draftTripId, "DRAFT"));
        insertDelayNotificationRule();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM notification_delivery_attempt WHERE notification_id IN (SELECT id FROM notification WHERE rule_id = ?)", ruleId);
        jdbc.update("DELETE FROM notification_rule_execution WHERE rule_id = ?", ruleId);
        jdbc.update("DELETE FROM notification WHERE rule_id = ?", ruleId);
        jdbc.update("DELETE FROM notification_rule_policy WHERE rule_id = ?", ruleId);
        jdbc.update("DELETE FROM notification_rule WHERE id = ?", ruleId);
        jdbc.update("DELETE FROM offline_sync_operation WHERE actor_id IN (?, ?)", actorId, secondActorId);
        jdbc.update("DELETE FROM trip_status_history WHERE trip_id IN (?, ?)", tripId, draftTripId);
        jdbc.update("DELETE FROM trip_operational_event WHERE trip_id IN (?, ?)", tripId, draftTripId);
        jdbc.update("DELETE FROM trip WHERE id IN (?, ?)", tripId, draftTripId);
        if (mixedVehicle != null) {
            jdbc.update("DELETE FROM vehicle_reading WHERE vehicle_id = ?", mixedVehicle.id());
            jdbc.update("DELETE FROM vehicle WHERE id = ?", mixedVehicle.id());
            jdbc.update("DELETE FROM vehicle_type WHERE id = ?", mixedVehicle.typeId());
            jdbc.update("DELETE FROM vehicle_category WHERE id = ?", mixedVehicle.categoryId());
        }
        jdbc.update("DELETE FROM location WHERE id IN (?, ?)", originId, destinationId);
        jdbc.update("DELETE FROM app_user_role WHERE user_id IN (?, ?)", actorId, secondActorId);
        jdbc.update("DELETE FROM refresh_token WHERE user_id IN (?, ?)", actorId, secondActorId);
        jdbc.update("DELETE FROM app_user WHERE id IN (?, ?)", actorId, secondActorId);
    }

    @Test
    void appliesAndReplaysAllThreeOperationsWithOneHistoryRowEachAndOneDelayNotification() {
        var checkpoint = command(UUID.randomUUID(), "TRIP_CHECKPOINT_RECORD", tripId, Map.of(
                "checkpointType", "DELIVERY", "occurredAt", OCCURRED_AT.toString(),
                "locationId", originId.toString(), "locationDescription", "Customer gate", "remarks", "Delivered"));
        var delay = command(UUID.randomUUID(), "TRIP_DELAY_RECORD", tripId, Map.of(
                "delayMinutes", 15, "reason", "Road closure", "occurredAt", OCCURRED_AT.plusMinutes(5).toString()));
        var incident = command(UUID.randomUUID(), "TRIP_INCIDENT_RECORD", tripId, Map.of(
                "incidentSeverity", "HIGH", "description", "Tyre damage",
                "occurredAt", OCCURRED_AT.plusMinutes(10).toString(), "remarks", "Spare fitted"));

        for (var command : List.of(checkpoint, delay, incident)) {
            assertEquals(OfflineSyncResultStatus.APPLIED, sync(username, command, Set.of("TRIP_LOG_MANAGE")).status());
            assertEquals(OfflineSyncResultStatus.ALREADY_APPLIED,
                    sync(username, command, Set.of("TRIP_LOG_MANAGE")).status());
        }

        var stored = tripEvents.getTripEvents(tripId);
        assertEquals(List.of(TripOperationalEventType.CHECKPOINT, TripOperationalEventType.DELAY,
                TripOperationalEventType.INCIDENT), stored.stream().map(event -> event.eventType()).toList());
        assertEquals(OCCURRED_AT, stored.getFirst().occurredAt());
        assertEquals(username, stored.getFirst().recordedBy());
        assertEquals(3, count("trip_status_history", "trip_id", tripId));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE rule_id = ? AND event_type = 'TRIP_DELAY_RECORDED'",
                Integer.class, ruleId));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_rule_execution WHERE rule_id = ?", Integer.class, ruleId));
    }

    @Test
    void mapsPayloadStateMissingPermissionAndIdentityConflictsWithoutMutation() {
        var invalidPayload = command(UUID.randomUUID(), "TRIP_DELAY_RECORD", tripId, Map.of(
                "delayMinutes", 0, "reason", "Bad", "occurredAt", OCCURRED_AT.toString()));
        assertEquals("OFFLINE_SYNC_PAYLOAD_INVALID",
                sync(username, invalidPayload, Set.of("TRIP_UPDATE")).errorCode());

        var invalidState = command(UUID.randomUUID(), "TRIP_CHECKPOINT_RECORD", draftTripId, Map.of(
                "checkpointType", "DEPARTURE", "occurredAt", OCCURRED_AT.toString()));
        assertEquals("OFFLINE_SYNC_CONFLICT", sync(username, invalidState, Set.of("TRIP_DISPATCH")).errorCode());

        var missing = command(UUID.randomUUID(), "TRIP_INCIDENT_RECORD", UUID.randomUUID(), Map.of(
                "incidentSeverity", "LOW", "description", "Observation", "occurredAt", OCCURRED_AT.toString()));
        assertEquals("NOT_FOUND", sync(username, missing, Set.of("TRIP_LOG_MANAGE")).errorCode());

        var forbidden = command(UUID.randomUUID(), "TRIP_CHECKPOINT_RECORD", tripId, Map.of(
                "checkpointType", "ARRIVAL", "occurredAt", OCCURRED_AT.toString()));
        assertEquals("OFFLINE_SYNC_FORBIDDEN", sync(username, forbidden, Set.of("TRIP_VIEW")).errorCode());

        UUID operationId = UUID.randomUUID();
        var applied = command(operationId, "TRIP_INCIDENT_RECORD", tripId, Map.of(
                "incidentSeverity", "MEDIUM", "description", "First facts", "occurredAt", OCCURRED_AT.toString()));
        assertEquals(OfflineSyncResultStatus.APPLIED, sync(username, applied, Set.of("TRIP_UPDATE")).status());
        var changed = command(operationId, "TRIP_INCIDENT_RECORD", tripId, Map.of(
                "incidentSeverity", "MEDIUM", "description", "Changed facts", "occurredAt", OCCURRED_AT.toString()));
        assertEquals("OFFLINE_SYNC_IDEMPOTENCY_MISMATCH",
                sync(username, changed, Set.of("TRIP_UPDATE")).errorCode());
        assertEquals("OFFLINE_SYNC_IDEMPOTENCY_MISMATCH",
                sync(secondUsername, applied, Set.of("TRIP_UPDATE")).errorCode());

        UUID wrongAggregateId = UUID.randomUUID();
        var wrongAggregate = new OfflineSyncUseCase.OperationCommand(wrongAggregateId, "TRIP_CHECKPOINT_RECORD", 1,
                "VEHICLE", tripId, json.valueToTree(Map.of(
                "checkpointType", "ARRIVAL", "occurredAt", OCCURRED_AT.toString())),
                OCCURRED_AT, OCCURRED_AT, UUID.randomUUID(), wrongAggregateId.toString(), null);
        assertEquals("OFFLINE_SYNC_PAYLOAD_INVALID",
                sync(username, wrongAggregate, Set.of("TRIP_UPDATE")).errorCode());
        assertEquals(1, tripEvents.getTripEvents(tripId).size());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void concurrentDelayAppliesOnceAndPublishesOneNotification() throws Exception {
        var command = command(UUID.randomUUID(), "TRIP_DELAY_RECORD", tripId, Map.of(
                "delayMinutes", 20, "reason", "Traffic", "occurredAt", OCCURRED_AT.toString()));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> sync(username, command, Set.of("TRIP_DISPATCH")).status());
            var second = executor.submit(() -> sync(username, command, Set.of("TRIP_DISPATCH")).status());
            assertEquals(Set.of(OfflineSyncResultStatus.APPLIED, OfflineSyncResultStatus.ALREADY_APPLIED),
                    Set.of(first.get(), second.get()));
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, tripEvents.getTripEvents(tripId).size());
        assertEquals(1, count("trip_status_history", "trip_id", tripId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE rule_id = ?", Integer.class, ruleId));
    }

    @Test
    void mixedVehicleAndTripBatchPreservesOrderAndIndependentResults() {
        mixedVehicle = new Vehicle(UUID.randomUUID(), "MIX-" + UUID.randomUUID(), null, null,
                UUID.randomUUID(), UUID.randomUUID(), "Maker", "Model", 2026, "OWNED", "AVAILABLE",
                null, null, 1000d, true);
        vehicleHierarchy(jdbc, mixedVehicle);
        vehicles.save(mixedVehicle);
        var vehicle = command(UUID.randomUUID(), "VEHICLE_READING_RECORD", mixedVehicle.id(), Map.of(
                "readingType", "ODOMETER", "value", 100, "recordedAt", OCCURRED_AT.toString()));
        var checkpoint = command(UUID.randomUUID(), "TRIP_CHECKPOINT_RECORD", tripId, Map.of(
                "checkpointType", "PICKUP", "occurredAt", OCCURRED_AT.toString()));
        var invalidDelay = command(UUID.randomUUID(), "TRIP_DELAY_RECORD", tripId, Map.of(
                "delayMinutes", 0, "reason", "Invalid", "occurredAt", OCCURRED_AT.toString()));
        var conflictingIncident = command(UUID.randomUUID(), "TRIP_INCIDENT_RECORD", draftTripId, Map.of(
                "incidentSeverity", "LOW", "description", "Blocked state", "occurredAt", OCCURRED_AT.toString()));

        var results = offlineSync.synchronize(new OfflineSyncUseCase.BatchCommand(username,
                Set.of("VEHICLE_READING_CREATE", "TRIP_LOG_MANAGE"),
                List.of(vehicle, checkpoint, invalidDelay, conflictingIncident))).results();

        assertEquals(List.of(OfflineSyncResultStatus.APPLIED, OfflineSyncResultStatus.APPLIED,
                        OfflineSyncResultStatus.REJECTED, OfflineSyncResultStatus.CONFLICT),
                results.stream().map(OfflineSyncResult::status).toList());
        assertEquals(1, tripEvents.getTripEvents(tripId).size());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM vehicle_reading WHERE vehicle_id = ?",
                Integer.class, mixedVehicle.id()));
    }

    private OfflineSyncResult sync(String actor, OfflineSyncUseCase.OperationCommand command,
                                   Set<String> authorities) {
        return offlineSync.synchronize(new OfflineSyncUseCase.BatchCommand(actor, authorities, List.of(command)))
                .results().getFirst();
    }

    private OfflineSyncUseCase.OperationCommand command(UUID operationId, String operationType, UUID aggregateId,
                                                         Map<String, Object> payload) {
        String aggregateType = operationType.equals("VEHICLE_READING_RECORD") ? "VEHICLE" : "TRIP";
        return new OfflineSyncUseCase.OperationCommand(operationId, operationType, 1, aggregateType, aggregateId,
                json.valueToTree(payload), OCCURRED_AT, OCCURRED_AT, UUID.randomUUID(), operationId.toString(), null);
    }

    private Trip trip(UUID id, String status) {
        return new Trip(id, "TRP-OFF-" + id.toString().substring(0, 8), null, null, null, null,
                "HIGH", status, originId, destinationId, OCCURRED_AT.minusHours(1), OCCURRED_AT.plusHours(4),
                null, null, "Cargo", null, null, null, null, null, null, null, null, null, null,
                OCCURRED_AT.minusDays(1), OCCURRED_AT.minusDays(1));
    }

    private void insertUser(UUID id, String actor) {
        jdbc.update("""
                INSERT INTO app_user
                    (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, actor, id + "@test.local", "unused", "Offline", "Trip", true, OCCURRED_AT, OCCURRED_AT);
    }

    private void insertDelayNotificationRule() {
        jdbc.update("""
                INSERT INTO notification_rule
                    (id, name, description, event_type, channel, recipient_type, recipient_value, enabled,
                     severity_threshold, created_at, updated_at, template_code)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ruleId, "Offline delay", "Offline delay test", "TRIP_DELAY_RECORDED", "IN_APP", "USER",
                username, true, "INFO", OCCURRED_AT, OCCURRED_AT, "TRIP_DELAY");
        jdbc.update("""
                INSERT INTO notification_rule_policy
                    (rule_id, quiet_hours_enabled, suppression_window_minutes, escalation_enabled,
                     created_at, updated_at, version)
                VALUES (?, FALSE, 0, FALSE, ?, ?, 0)
                """, ruleId, OCCURRED_AT, OCCURRED_AT);
    }

    private int count(String table, String column, UUID id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, id);
    }
}
