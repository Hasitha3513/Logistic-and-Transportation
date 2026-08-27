package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.VehicleAllocationLookup;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static com.transportlogistics.app.support.ReferenceFixtures.*;

@SpringBootTest
@Transactional
class VehicleAllocationLookupIntegrationTest {
    @Autowired TripRepository trips;
    @Autowired VehicleAllocationLookup allocations;
    @Autowired JdbcTemplate jdbc;

    @Test
    void detectsOnlyActiveHalfOpenOverlapsAndSupportsExcludingCurrentTrip() {
        var vehicleId = UUID.randomUUID();
        var start = OffsetDateTime.parse("2026-02-01T08:00:00Z");
        var allocated = trip(vehicleId, "APPROVED", start, start.plusHours(2));
        vehicleReference(jdbc, vehicleId);
        tripLocations(jdbc, allocated);
        trips.save(allocated);
        var completed = trip(vehicleId, "COMPLETED", start, start.plusHours(4));
        tripLocations(jdbc, completed);
        trips.save(completed);

        assertTrue(allocations.hasOverlap(vehicleId, start.plusHours(1), start.plusHours(3), null));
        assertFalse(allocations.hasOverlap(vehicleId, start.plusHours(2), start.plusHours(3), null));
        assertFalse(allocations.hasOverlap(vehicleId, start.plusHours(1), start.plusHours(3), allocated.id()));
        assertFalse(allocations.hasOverlap(UUID.randomUUID(), start, start.plusHours(1), null));
    }

    private Trip trip(UUID vehicleId, String status, OffsetDateTime start, OffsetDateTime end) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-" + UUID.randomUUID(), null, null, null, null, "NORMAL", status,
                UUID.randomUUID(), UUID.randomUUID(), start, end, null, null, null, null, null, null, vehicleId,
                null, null, null, null, null, null, now, now);
    }
}
