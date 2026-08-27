package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.trip.application.ports.out.*;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TripServiceRouteAssignmentTest {
    @Test
    void assignsRouteThroughBoundaryAndRecordsAudit() {
        var repository = mock(TripRepository.class);
        var routes = mock(RouteEligibilityPort.class);
        var history = mock(TripHistoryRepository.class);
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var routeId = UUID.randomUUID();
        var trip = trip(origin, destination, "APPROVED");
        when(repository.findByIdForUpdate(trip.id())).thenReturn(Optional.of(trip));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(repository, routes, history);

        var assigned = service.assignRoute(trip.id(), routeId, "planner");

        assertEquals(routeId, assigned.routeId());
        assertEquals("APPROVED", assigned.status());
        verify(routes).assertAssignable(routeId, origin, destination);
        verify(history).save(argThat(entry -> entry.action().equals("ROUTE_ASSIGNED")
                && entry.actor().equals("planner") && entry.details().contains(routeId.toString())));
    }

    @Test
    void rejectsRouteAssignmentFromSubmittedStateBeforeCallingRouting() {
        var repository = mock(TripRepository.class);
        var routes = mock(RouteEligibilityPort.class);
        var trip = trip(UUID.randomUUID(), UUID.randomUUID(), "SUBMITTED");
        when(repository.findByIdForUpdate(trip.id())).thenReturn(Optional.of(trip));
        var service = service(repository, routes, mock(TripHistoryRepository.class));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.assignRoute(trip.id(), UUID.randomUUID(), "planner"));

        verifyNoInteractions(routes);
        verify(repository, never()).save(any());
    }

    private TripService service(TripRepository repository, RouteEligibilityPort routes,
                                TripHistoryRepository history) {
        return new TripService(repository, mock(VehicleEligibilityPort.class), mock(DriverEligibilityPort.class),
                routes, history, new DirectTransaction(), mock(TripDispatchRepository.class),
                Clock.fixed(java.time.Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC));
    }

    private static final class DirectTransaction implements TripTransaction {
        @Override
        public <T> T execute(java.util.function.Supplier<T> operation) {
            return operation.get();
        }
    }

    private Trip trip(UUID origin, UUID destination, String status) {
        var now = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-ROUTE", null, null, null, null, "NORMAL", status,
                origin, destination, now.plusDays(1), now.plusDays(2), null, null, null, 0, null, null,
                null, null, null, null, null, null, null, now, now);
    }
}
