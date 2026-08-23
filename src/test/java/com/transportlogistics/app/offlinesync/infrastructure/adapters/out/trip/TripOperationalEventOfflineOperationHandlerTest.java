package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncPayloadException;
import com.transportlogistics.app.trip.TripOperationalEventRecorder;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripOperationalEventOfflineOperationHandlerTest {
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-08-20T10:15:00+05:30");
    private final ObjectMapper json = new ObjectMapper();
    private final RecordingBoundary boundary = new RecordingBoundary();
    private final OfflineOperationContext context = new OfflineOperationContext(
            UUID.randomUUID(), UUID.randomUUID(), "dispatcher.alice", UUID.randomUUID(),
            OffsetDateTime.parse("2026-08-20T10:20:00Z"));

    @Test
    void checkpointSupportsEveryTypeAndPreservesEventFacts() throws Exception {
        var handler = new TripCheckpointOfflineOperationHandler(boundary);
        UUID locationId = UUID.randomUUID();
        for (TripOperationalEventRecorder.CheckpointType type : TripOperationalEventRecorder.CheckpointType.values()) {
            handler.apply(context, json.readTree("""
                    {"checkpointType":"%s","occurredAt":"%s","locationId":"%s",
                     "locationDescription":" Depot gate ","remarks":" observed "}
                    """.formatted(type, OCCURRED_AT, locationId)));
            assertEquals(type, boundary.checkpoint.checkpointType());
            assertEquals(OCCURRED_AT, boundary.checkpoint.occurredAt());
            assertEquals(locationId, boundary.checkpoint.locationId());
            assertEquals("Depot gate", boundary.checkpoint.locationDescription());
            assertEquals("observed", boundary.checkpoint.remarks());
            assertEquals("dispatcher.alice", boundary.checkpoint.actor());
        }
        assertContract(handler, "TRIP_CHECKPOINT_RECORD");
    }

    @Test
    void delayRequiresPositiveIntegerReasonAndExactLimits() throws Exception {
        var handler = new TripDelayOfflineOperationHandler(boundary);
        handler.apply(context, json.readTree("""
                {"delayMinutes":1,"reason":" Road closure ","occurredAt":"2026-08-20T10:15:00+05:30"}
                """));
        assertEquals(1, boundary.delay.delayMinutes());
        assertEquals("Road closure", boundary.delay.reason());
        assertEquals(OCCURRED_AT, boundary.delay.occurredAt());
        assertContract(handler, "TRIP_DELAY_RECORD");

        for (String payload : new String[] {
                "{\"delayMinutes\":0,\"reason\":\"x\",\"occurredAt\":\"2026-08-20T10:15:00Z\"}",
                "{\"delayMinutes\":-1,\"reason\":\"x\",\"occurredAt\":\"2026-08-20T10:15:00Z\"}",
                "{\"delayMinutes\":1.5,\"reason\":\"x\",\"occurredAt\":\"2026-08-20T10:15:00Z\"}",
                "{\"delayMinutes\":1,\"reason\":\"  \",\"occurredAt\":\"2026-08-20T10:15:00Z\"}",
                json.createObjectNode().put("delayMinutes", 1).put("reason", "x".repeat(501))
                        .put("occurredAt", "2026-08-20T10:15:00Z").toString()
        }) {
            assertThrows(OfflineSyncPayloadException.class, () -> handler.apply(context, json.readTree(payload)));
        }
    }

    @Test
    void incidentSupportsEverySeverityAndRejectsMalformedSharedFields() throws Exception {
        var handler = new TripIncidentOfflineOperationHandler(boundary);
        for (TripOperationalEventRecorder.IncidentSeverity severity : TripOperationalEventRecorder.IncidentSeverity.values()) {
            handler.apply(context, json.readTree("""
                    {"incidentSeverity":"%s","description":" Tyre damage ",
                     "occurredAt":"2026-08-20T10:15:00+05:30"}
                    """.formatted(severity)));
            assertEquals(severity, boundary.incident.incidentSeverity());
            assertEquals("Tyre damage", boundary.incident.description());
            assertEquals(OCCURRED_AT, boundary.incident.occurredAt());
        }
        assertContract(handler, "TRIP_INCIDENT_RECORD");

        for (String payload : new String[] {
                "{\"incidentSeverity\":\"SEVERE\",\"description\":\"x\",\"occurredAt\":\"2026-08-20T10:15:00Z\"}",
                "{\"incidentSeverity\":\"LOW\",\"description\":\" \",\"occurredAt\":\"2026-08-20T10:15:00Z\"}",
                "{\"incidentSeverity\":\"LOW\",\"description\":\"x\",\"occurredAt\":\"bad\"}",
                "{\"incidentSeverity\":\"LOW\",\"description\":\"x\",\"occurredAt\":\"2026-08-20T10:15:00Z\",\"locationId\":\"bad\"}",
                json.createObjectNode().put("incidentSeverity", "LOW").put("description", "x")
                        .put("occurredAt", "2026-08-20T10:15:00Z").put("locationDescription", "x".repeat(256)).toString(),
                json.createObjectNode().put("incidentSeverity", "LOW").put("description", "x".repeat(501))
                        .put("occurredAt", "2026-08-20T10:15:00Z").toString(),
                json.createObjectNode().put("incidentSeverity", "LOW").put("description", "x")
                        .put("occurredAt", "2026-08-20T10:15:00Z").put("remarks", "x".repeat(2001)).toString(),
                "{\"incidentSeverity\":\"LOW\",\"description\":\"x\",\"occurredAt\":\"2026-08-20T10:15:00Z\",\"tripId\":\"00000000-0000-0000-0000-000000000001\"}"
        }) {
            assertThrows(OfflineSyncPayloadException.class, () -> handler.apply(context, json.readTree(payload)));
        }
    }

    private void assertContract(AbstractTripOperationalEventOfflineHandler handler, String operationType) {
        assertEquals(operationType, handler.operationType());
        assertEquals(1, handler.operationVersion());
        assertEquals(Set.of("TRIP_DISPATCH", "TRIP_LOG_MANAGE", "TRIP_UPDATE"), handler.requiredAuthorities());
        assertTrue(handler.isAuthorized(Set.of("TRIP_DISPATCH")));
        assertTrue(handler.isAuthorized(Set.of("TRIP_LOG_MANAGE")));
        assertTrue(handler.isAuthorized(Set.of("TRIP_UPDATE")));
        assertFalse(handler.isAuthorized(Set.of("TRIP_VIEW")));
    }

    private static final class RecordingBoundary implements TripOperationalEventRecorder {
        CheckpointCommand checkpoint;
        DelayCommand delay;
        IncidentCommand incident;

        @Override
        public Result recordCheckpoint(CheckpointCommand command) {
            checkpoint = command;
            return result(command.tripId(), command.occurredAt());
        }

        @Override
        public Result recordDelay(DelayCommand command) {
            delay = command;
            return result(command.tripId(), command.occurredAt());
        }

        @Override
        public Result recordIncident(IncidentCommand command) {
            incident = command;
            return result(command.tripId(), command.occurredAt());
        }

        private Result result(UUID tripId, OffsetDateTime occurredAt) {
            return new Result(UUID.randomUUID(), tripId, occurredAt);
        }
    }
}
