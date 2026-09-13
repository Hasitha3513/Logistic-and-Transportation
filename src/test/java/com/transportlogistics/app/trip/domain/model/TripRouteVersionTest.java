package com.transportlogistics.app.trip.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripRouteVersionTest {
    @Test
    void acceptsCanonicalAssignedRevisionAndHistoricalNull() {
        assertDoesNotThrow(() -> trip(UUID.randomUUID(), "REVISION:1"));
        assertDoesNotThrow(() -> trip(UUID.randomUUID(), null));
        assertDoesNotThrow(() -> trip(null, null));
    }

    @Test
    void rejectsInvalidAssignedRevision() {
        var routeId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> trip(routeId, "REVISION:0"));
        assertThrows(IllegalArgumentException.class, () -> trip(routeId, "REVISION:-1"));
        assertThrows(IllegalArgumentException.class, () -> trip(routeId, " REVISION:1"));
        assertThrows(IllegalArgumentException.class, () -> trip(routeId, "REVISION:1 "));
        assertThrows(IllegalArgumentException.class, () -> trip(routeId, "R".repeat(121)));
        assertThrows(IllegalArgumentException.class, () -> trip(null, "REVISION:1"));
    }

    private Trip trip(UUID routeId, String routeVersion) {
        var now = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-REV", null, null, null, routeId, routeVersion, "NORMAL",
                "DRAFT", UUID.randomUUID(), UUID.randomUUID(), now.plusHours(1), now.plusHours(2), null,
                null, null, 0, null, null, null, null, null, null, null, null, null, now, now);
    }
}
