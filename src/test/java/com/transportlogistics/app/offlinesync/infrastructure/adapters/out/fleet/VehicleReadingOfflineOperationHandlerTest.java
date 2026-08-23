package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.fleet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.ManualVehicleReadingRecorder;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncPayloadException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VehicleReadingOfflineOperationHandlerTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void parsesExactPayloadAndUsesServerActorAndEnvelopeIdentity() throws Exception {
        RecordingBoundary boundary = new RecordingBoundary();
        var handler = new VehicleReadingOfflineOperationHandler(boundary);
        UUID operationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        var result = handler.apply(new OfflineOperationContext(operationId, actorId, "offline.reader", vehicleId,
                        OffsetDateTime.parse("2026-08-20T10:00:00Z")),
                json.readTree("""
                        {"readingType":"ODOMETER","value":123.456,
                         "recordedAt":"2026-08-20T10:00:00+05:30","notes":" checked "}
                        """));

        assertEquals("APPLIED", result.status().name());
        assertEquals(vehicleId, boundary.command.vehicleId());
        assertEquals(actorId, boundary.command.actorId());
        assertEquals(operationId.toString(), boundary.command.idempotencyKey());
        assertEquals("checked", boundary.command.notes());
        assertEquals("VEHICLE_READING_RECORD", handler.operationType());
        assertEquals(1, handler.operationVersion());
        assertEquals(Set.of("VEHICLE_READING_CREATE"), handler.requiredAuthorities());
    }

    @Test
    void rejectsUnknownFieldsInvalidScaleAndMissingOffset() throws Exception {
        var handler = new VehicleReadingOfflineOperationHandler(new RecordingBoundary());
        var context = new OfflineOperationContext(UUID.randomUUID(), UUID.randomUUID(), "offline.reader", UUID.randomUUID(),
                OffsetDateTime.parse("2026-08-20T10:00:00Z"));

        assertThrows(OfflineSyncPayloadException.class, () -> handler.apply(context,
                json.readTree("{\"readingType\":\"ODOMETER\",\"value\":1,\"recordedAt\":\"2026-08-20T10:00:00Z\",\"extra\":1}")));
        assertThrows(OfflineSyncPayloadException.class, () -> handler.apply(context,
                json.readTree("{\"readingType\":\"ODOMETER\",\"value\":1.0001,\"recordedAt\":\"2026-08-20T10:00:00Z\"}")));
        assertThrows(OfflineSyncPayloadException.class, () -> handler.apply(context,
                json.readTree("{\"readingType\":\"ODOMETER\",\"value\":1,\"recordedAt\":\"2026-08-20T10:00:00\"}")));
    }

    @Test
    void acceptsEngineHoursAndRejectsEveryFrozenInvalidPayloadShape() throws Exception {
        RecordingBoundary boundary = new RecordingBoundary();
        var handler = new VehicleReadingOfflineOperationHandler(boundary);
        var context = new OfflineOperationContext(UUID.randomUUID(), UUID.randomUUID(), "offline.reader", UUID.randomUUID(),
                OffsetDateTime.parse("2026-08-20T10:00:00Z"));

        handler.apply(context, json.readTree("""
                {"readingType":"ENGINE_HOURS","value":12.500,
                 "recordedAt":"2026-08-20T10:00:00+05:30"}
                """));
        assertEquals(ManualVehicleReadingRecorder.ReadingType.ENGINE_HOURS, boundary.command.readingType());

        String longNotes = "x".repeat(1001);
        String[] invalidPayloads = {
                "{\"readingType\":\"FUEL_LEVEL\",\"value\":1,\"recordedAt\":\"2026-08-20T10:00:00Z\"}",
                "{\"readingType\":\"ODOMETER\",\"value\":-1,\"recordedAt\":\"2026-08-20T10:00:00Z\"}",
                "{\"readingType\":\"ODOMETER\",\"value\":\"NaN\",\"recordedAt\":\"2026-08-20T10:00:00Z\"}",
                "{\"readingType\":\"ODOMETER\",\"value\":1,\"recordedAt\":\"not-a-time\"}",
                json.createObjectNode().put("readingType", "ODOMETER").put("value", 1)
                        .put("recordedAt", "2026-08-20T10:00:00Z").put("notes", longNotes).toString()
        };
        for (String payload : invalidPayloads) {
            assertThrows(OfflineSyncPayloadException.class, () -> handler.apply(context, json.readTree(payload)));
        }
    }

    private static final class RecordingBoundary implements ManualVehicleReadingRecorder {
        private Command command;
        @Override
        public Result recordManual(Command command) {
            this.command = command;
            return new Result(UUID.randomUUID(), command.vehicleId(), command.readingType(), command.value(),
                    command.recordedAt());
        }
    }
}
