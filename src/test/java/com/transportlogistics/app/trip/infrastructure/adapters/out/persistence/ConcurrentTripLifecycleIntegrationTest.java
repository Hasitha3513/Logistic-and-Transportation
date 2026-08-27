package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.transportlogistics.app.support.ReferenceFixtures.tripLocations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ConcurrentTripLifecycleIntegrationTest {
    @Autowired TripUseCase trips;
    @Autowired TripRepository repository;
    @Autowired TripHistoryRepository history;
    @Autowired JdbcTemplate jdbc;

    @Test
    void concurrentApprovalPersistsExactlyOneTransitionAndHistoryEntry() throws Exception {
        var draft = draft();
        tripLocations(jdbc, draft);
        repository.save(draft);
        trips.transition(draft.id(), new TripCommand.Submit(), "requester");

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> approve(draft.id(), ready, start));
            var second = executor.submit(() -> approve(draft.id(), ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var results = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(Trip.class::isInstance).count());
            assertEquals(1, results.stream().filter(ConflictException.class::isInstance).count());
        }

        assertEquals("APPROVED", repository.findById(draft.id()).orElseThrow().status());
        assertEquals(List.of("TRIP_SUBMITTED", "TRIP_APPROVED"),
                history.findByTripId(draft.id()).stream().map(entry -> entry.action()).toList());
    }

    private Object approve(UUID id, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await(10, TimeUnit.SECONDS);
            return trips.transition(id, new TripCommand.Approve(), "approver");
        } catch (RuntimeException exception) {
            return exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return exception;
        }
    }

    private Trip draft() {
        var now = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-LOCK-" + UUID.randomUUID().toString().substring(0, 8),
                null, null, null, null, "NORMAL", "DRAFT", UUID.randomUUID(), UUID.randomUUID(),
                now.plusDays(1), now.plusDays(2), null, null, null, 0, null, null, null, null,
                null, null, null, null, null, now, now);
    }
}
