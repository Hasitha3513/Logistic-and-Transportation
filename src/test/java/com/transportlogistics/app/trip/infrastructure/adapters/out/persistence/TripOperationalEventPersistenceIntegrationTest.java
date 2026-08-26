package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCheckpointType;
import com.transportlogistics.app.trip.domain.model.TripOperationalEvent;
import com.transportlogistics.app.trip.domain.model.TripOperationalEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TripOperationalEventPersistenceAdapter.class, TripPersistenceAdapter.class, TripMapperImpl.class})
class TripOperationalEventPersistenceIntegrationTest {

    @Autowired
    private TripOperationalEventPersistenceAdapter adapter;

    @Autowired
    private TripPersistenceAdapter tripAdapter;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Should save and retrieve operational events in chronological order")
    void shouldSaveAndRetrieveOperationalEvents() {
        var tripId = UUID.randomUUID();
        var originId = UUID.randomUUID();
        var destId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, ?)",
                originId, "ORIGIN-" + tripId.toString().substring(0, 6), "Origin", true);
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, ?)",
                destId, "DEST-" + tripId.toString().substring(0, 6), "Destination", true);

        var trip = new Trip(
                tripId, "TRP-INTEG-001", null, null, null, null,
                "HIGH", "IN_PROGRESS", originId, destId,
                now, now.plusHours(4), null, null, "Cargo", null, null, null,
                null, null, now, null, 100.0, null, null, now, now
        );
        tripAdapter.save(trip);

        var event1 = TripOperationalEvent.createCheckpoint(
                UUID.randomUUID(), tripId, TripCheckpointType.DEPARTURE,
                now.minusMinutes(30), null, "Origin Gate", "Departed", "dispatcher", now
        );
        var event2 = TripOperationalEvent.createDelay(
                UUID.randomUUID(), tripId, 15, "Signal check",
                now.minusMinutes(10), null, "Checkpoint B", null, "driver", now
        );

        adapter.save(event2);
        adapter.save(event1);

        var list = adapter.findByTripIdOrderByOccurredAtAsc(tripId);

        assertEquals(2, list.size());
        assertEquals(TripOperationalEventType.CHECKPOINT, list.get(0).eventType());
        assertEquals(TripOperationalEventType.DELAY, list.get(1).eventType());
    }
}
