package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.hashing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase.OperationCommand;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class Sha256OfflineRequestHasherTest {
    private static final UUID OPERATION_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID AGGREGATE_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID CLIENT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-22T10:00:00Z");
    private final ObjectMapper json = new ObjectMapper();
    private final Sha256OfflineRequestHasher hasher = new Sha256OfflineRequestHasher();

    @Test
    void canonicalizesKeysNumbersUuidsTimestampsAndOptionalNulls() throws Exception {
        var first = command(OPERATION_ID, "VEHICLE_READING_RECORD", AGGREGATE_ID,
                json.readTree("{\"recordedAt\":\"2026-08-22T15:30:00+05:30\",\"locationId\":\"AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA\",\"notes\":null,\"value\":1.000}"));
        var second = command(UUID.randomUUID(), "VEHICLE_READING_RECORD", AGGREGATE_ID,
                json.readTree("{\"value\":1,\"locationId\":\"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa\",\"recordedAt\":\"2026-08-22T10:00:00Z\"}"));

        assertEquals(hasher.hash(first), hasher.hash(second));
    }

    @Test
    void excludesOperationActorAndQueueMetadataButIncludesBusinessIdentity() throws Exception {
        var base = command(OPERATION_ID, "VEHICLE_READING_RECORD", AGGREGATE_ID, json.readTree("{\"value\":1}"));
        var otherOperation = new OperationCommand(UUID.randomUUID(), base.operationType(), base.operationVersion(),
                base.aggregateType(), base.aggregateId(), base.payload(), NOW.plusDays(1), NOW.plusDays(2),
                UUID.randomUUID(), UUID.randomUUID().toString(), null);
        assertEquals(hasher.hash(base), hasher.hash(otherOperation));

        assertNotEquals(hasher.hash(base), hasher.hash(command(OPERATION_ID, "TRIP_DELAY_RECORD", AGGREGATE_ID,
                json.readTree("{\"value\":1}"))));
        assertNotEquals(hasher.hash(base), hasher.hash(command(OPERATION_ID, "VEHICLE_READING_RECORD", UUID.randomUUID(),
                json.readTree("{\"value\":1}"))));
        assertNotEquals(hasher.hash(base), hasher.hash(command(OPERATION_ID, "VEHICLE_READING_RECORD", AGGREGATE_ID,
                json.readTree("{\"value\":2}"))));
    }

    private OperationCommand command(UUID operationId, String type, UUID aggregateId,
                                     com.fasterxml.jackson.databind.JsonNode payload) {
        String aggregateType = type.startsWith("VEHICLE") ? "VEHICLE" : "TRIP";
        return new OperationCommand(operationId, type, 1, aggregateType, aggregateId, payload,
                NOW, NOW, CLIENT_ID, operationId.toString(), null);
    }
}
