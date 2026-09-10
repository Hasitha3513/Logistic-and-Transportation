package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehicleGeofenceTransitionedV1Test {
    @Test
    void exposesExactMinimizedVersionOneEnvelopeIncludingNullableLocation() {
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID geofenceId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        var event = new VehicleGeofenceTransitionedV1(eventId, tenantId,
                OffsetDateTime.parse("2026-09-11T01:00:00Z"), geofenceId, vehicleId, null,
                "DEPOT", "ENTERED", "NORMAL", "2026-09-11T01:00:00Z", 3);

        assertThat(event.eventType()).isEqualTo("VEHICLE_GEOFENCE_TRANSITIONED_V1");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.aggregateType()).isEqualTo("GEOFENCE_TRANSITION");
        assertThat(event.aggregateId()).isEqualTo(eventId);
        assertThat(event.payload()).containsOnlyKeys("geofenceId", "vehicleId", "locationId",
                "geofenceType", "transition", "severity", "sourceTimestamp", "definitionVersion");
        assertThat(event.payload()).containsEntry("locationId", null);
    }

    @Test
    void rejectsUnsupportedContractValues() {
        assertThatThrownBy(() -> new VehicleGeofenceTransitionedV1(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), UUID.randomUUID(),
                UUID.randomUUID(), null, "CIRCLE", "ENTERED", "NORMAL",
                "2026-09-11T01:00:00Z", 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
