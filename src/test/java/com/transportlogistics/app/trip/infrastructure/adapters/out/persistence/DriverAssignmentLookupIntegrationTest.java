package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.DriverAssignmentLookup;
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
class DriverAssignmentLookupIntegrationTest {
    @Autowired TripRepository trips;
    @Autowired DriverAssignmentLookup assignments;
    @Autowired JdbcTemplate jdbc;

    @Test
    void detectsOnlyActiveHalfOpenConflictsAndSupportsExcludingCurrentTrip() {
        var driverId = UUID.randomUUID();
        var start = OffsetDateTime.parse("2026-02-01T08:00:00Z");
        var assigned = trip(driverId, "APPROVED", start, start.plusHours(2));
        driverReference(jdbc, driverId);
        tripLocations(jdbc, assigned);
        trips.save(assigned);
        var cancelled = trip(driverId, "CANCELLED", start, start.plusHours(4));
        tripLocations(jdbc, cancelled);
        trips.save(cancelled);

        assertTrue(assignments.hasOverlap(driverId, start.plusHours(1), start.plusHours(3), null));
        assertFalse(assignments.hasOverlap(driverId, start.plusHours(2), start.plusHours(3), null));
        assertFalse(assignments.hasOverlap(driverId, start.plusHours(1), start.plusHours(3), assigned.id()));
        assertFalse(assignments.hasOverlap(UUID.randomUUID(), start, start.plusHours(1), null));
    }

    private Trip trip(UUID driverId, String status, OffsetDateTime start, OffsetDateTime end) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-" + UUID.randomUUID(), null, null, null, null, "NORMAL", status,
                UUID.randomUUID(), UUID.randomUUID(), start, end, null, null, null, null, null, null, null,
                driverId, null, null, null, null, null, now, now);
    }
}
