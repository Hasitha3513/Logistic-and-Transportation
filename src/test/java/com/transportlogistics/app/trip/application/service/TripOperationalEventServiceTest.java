package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.trip.application.ports.in.TripOperationalEventUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripOperationalEventRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TripOperationalEventServiceTest {

    private TripRepository tripRepo;
    private TripOperationalEventRepository eventRepo;
    private TripHistoryRepository historyRepo;
    private TripOperationalEventService service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-19T10:00:00Z"), ZoneOffset.UTC);
    private final UUID tripId = UUID.randomUUID();
    private Trip activeTrip;
    private org.springframework.context.ApplicationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        tripRepo = mock(TripRepository.class);
        eventRepo = mock(TripOperationalEventRepository.class);
        historyRepo = mock(TripHistoryRepository.class);
        publisher = mock(org.springframework.context.ApplicationEventPublisher.class);
        service = new TripOperationalEventService(tripRepo, eventRepo, historyRepo, clock, publisher);

        activeTrip = new Trip(
                tripId, "TRP-2026-001", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "HIGH", "IN_PROGRESS", UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now(clock), OffsetDateTime.now(clock).plusHours(4),
                UUID.randomUUID(), 1000.0, "General Cargo", 0, "Handle with care", "Notes",
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(clock), null,
                50000.0, null, null, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );
    }

    @Test
    @DisplayName("Should record checkpoint event on active trip")
    void shouldRecordCheckpointEvent() {
        when(tripRepo.findById(tripId)).thenReturn(Optional.of(activeTrip));
        when(eventRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new TripOperationalEventUseCase.RecordCheckpointCommand(
                TripCheckpointType.PICKUP,
                OffsetDateTime.now(clock),
                UUID.randomUUID(),
                "Customer Warehouse A",
                "Cargo loaded and verified"
        );

        var result = service.recordCheckpoint(tripId, cmd, "dispatcher.alice");

        assertNotNull(result);
        assertEquals(tripId, result.tripId());
        assertEquals(TripOperationalEventType.CHECKPOINT, result.eventType());
        assertEquals(TripCheckpointType.PICKUP, result.checkpointType());
        assertEquals("Customer Warehouse A", result.locationDescription());
        verify(eventRepo).save(any());
        verify(historyRepo).save(any());
    }

    @Test
    @DisplayName("Should record delay event on active trip")
    void shouldRecordDelayEvent() {
        when(tripRepo.findById(tripId)).thenReturn(Optional.of(activeTrip));
        when(eventRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new TripOperationalEventUseCase.RecordDelayCommand(
                30,
                "Traffic checkpoint delay",
                OffsetDateTime.now(clock),
                null,
                "Toll Plaza",
                "Queue at gate"
        );

        var result = service.recordDelay(tripId, cmd, "driver.bob");

        assertNotNull(result);
        assertEquals(TripOperationalEventType.DELAY, result.eventType());
        assertEquals(30, result.delayMinutes());
        assertEquals("Traffic checkpoint delay", result.reason());
        verify(eventRepo).save(any());
        // Verify that a notification event was published
        org.mockito.ArgumentCaptor<com.transportlogistics.app.notification.OperationalNotificationEvent> captor =
                org.mockito.ArgumentCaptor.forClass(com.transportlogistics.app.notification.OperationalNotificationEvent.class);
        verify(publisher).publishEvent(captor.capture());
        com.transportlogistics.app.notification.OperationalNotificationEvent published = captor.getValue();
        assertEquals("TRIP_DELAY_RECORDED", published.eventType());
        assertEquals(result.id(), published.eventId());
        assertEquals("TRIP", published.aggregateType());
        assertEquals(tripId, published.aggregateId());
        assertEquals(com.transportlogistics.app.notification.OperationalNotificationEvent.Severity.WARNING,
                published.severity());
        assertEquals(result.occurredAt(), published.occurredAt());
        assertEquals(activeTrip.tripNumber(), published.metadata().get("tripNumber"));
        assertEquals("30", published.metadata().get("delayMinutes"));
        assertEquals("Traffic checkpoint delay", published.metadata().get("reason"));
        assertEquals("Toll Plaza", published.metadata().get("locationDescription"));
    }

    @Test
    @DisplayName("Should record incident event on active trip")
    void shouldRecordIncidentEvent() {
        when(tripRepo.findById(tripId)).thenReturn(Optional.of(activeTrip));
        when(eventRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new TripOperationalEventUseCase.RecordIncidentCommand(
                TripIncidentSeverity.MEDIUM,
                "Flat tire on rear axle",
                OffsetDateTime.now(clock),
                null,
                "Highway Mile 12",
                "Replaced with spare tire"
        );

        var result = service.recordIncident(tripId, cmd, "driver.bob");

        assertNotNull(result);
        assertEquals(TripOperationalEventType.INCIDENT, result.eventType());
        assertEquals(TripIncidentSeverity.MEDIUM, result.incidentSeverity());
        assertEquals("Flat tire on rear axle", result.reason());
        verify(eventRepo).save(any());
        var captor = org.mockito.ArgumentCaptor.forClass(
                com.transportlogistics.app.notification.OperationalNotificationEvent.class);
        verify(publisher).publishEvent(captor.capture());
        var published = captor.getValue();
        assertEquals(result.id(), published.eventId());
        assertEquals("TRIP_INCIDENT_RECORDED", published.eventType());
        assertEquals(com.transportlogistics.app.notification.OperationalNotificationEvent.Severity.WARNING,
                published.severity());
        assertEquals("MEDIUM", published.metadata().get("incidentSeverity"));
        assertEquals("Flat tire on rear axle", published.metadata().get("description"));
        assertEquals("Highway Mile 12", published.metadata().get("locationDescription"));
    }

    @Test
    void mapsEveryIncidentSeverityAndKeepsStableOperationalEventIdentity() {
        when(tripRepo.findById(tripId)).thenReturn(Optional.of(activeTrip));
        when(eventRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var expected = java.util.List.of(
                com.transportlogistics.app.notification.OperationalNotificationEvent.Severity.INFO,
                com.transportlogistics.app.notification.OperationalNotificationEvent.Severity.WARNING,
                com.transportlogistics.app.notification.OperationalNotificationEvent.Severity.WARNING,
                com.transportlogistics.app.notification.OperationalNotificationEvent.Severity.CRITICAL);
        var savedIds = new java.util.ArrayList<UUID>();

        for (var severity : TripIncidentSeverity.values()) {
            savedIds.add(service.recordIncident(tripId, new TripOperationalEventUseCase.RecordIncidentCommand(
                    severity, "Incident " + severity, OffsetDateTime.now(clock), null, null, null), "driver").id());
        }

        var captor = org.mockito.ArgumentCaptor.forClass(
                com.transportlogistics.app.notification.OperationalNotificationEvent.class);
        verify(publisher, times(4)).publishEvent(captor.capture());
        assertEquals(expected, captor.getAllValues().stream().map(
                com.transportlogistics.app.notification.OperationalNotificationEvent::severity).toList());
        assertEquals(savedIds, captor.getAllValues().stream().map(
                com.transportlogistics.app.notification.OperationalNotificationEvent::eventId).toList());
    }

    @Test
    void notificationFailureDoesNotRollBackAcceptedIncident() {
        when(tripRepo.findById(tripId)).thenReturn(Optional.of(activeTrip));
        when(eventRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("listener failed")).when(publisher).publishEvent(any());

        var result = service.recordIncident(tripId, new TripOperationalEventUseCase.RecordIncidentCommand(
                TripIncidentSeverity.HIGH, "Road closure", OffsetDateTime.now(clock), null, null, null), "driver");

        assertNotNull(result);
        verify(eventRepo).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when trip does not exist")
    void shouldThrowWhenTripNotFound() {
        when(tripRepo.findById(tripId)).thenReturn(Optional.empty());

        var cmd = new TripOperationalEventUseCase.RecordCheckpointCommand(
                TripCheckpointType.DEPARTURE, OffsetDateTime.now(clock), null, null, null
        );

        assertThrows(NotFoundException.class, () -> service.recordCheckpoint(tripId, cmd, "dispatcher"));
    }

    @Test
    @DisplayName("Should throw ConflictException when recording event on DRAFT or CANCELLED trip")
    void shouldThrowWhenTripNotInActiveState() {
        var draftTrip = new Trip(
                tripId, "TRP-2026-001", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "HIGH", "DRAFT", UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now(clock), OffsetDateTime.now(clock).plusHours(4),
                UUID.randomUUID(), 1000.0, "General Cargo", 0, null, null,
                null, null, null, null, null, null, null, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );
        when(tripRepo.findById(tripId)).thenReturn(Optional.of(draftTrip));

        var cmd = new TripOperationalEventUseCase.RecordCheckpointCommand(
                TripCheckpointType.DEPARTURE, OffsetDateTime.now(clock), null, null, null
        );

        assertThrows(ConflictException.class, () -> service.recordCheckpoint(tripId, cmd, "dispatcher"));
    }

    @Test
    @DisplayName("Should return chronological trip events")
    void shouldReturnTripEventsInChronologicalOrder() {
        when(tripRepo.findById(tripId)).thenReturn(Optional.of(activeTrip));

        var event1 = TripOperationalEvent.createCheckpoint(
                UUID.randomUUID(), tripId, TripCheckpointType.DEPARTURE,
                OffsetDateTime.now(clock).minusHours(2), null, "Depot", null, "actor", OffsetDateTime.now(clock)
        );
        var event2 = TripOperationalEvent.createDelay(
                UUID.randomUUID(), tripId, 20, "Traffic",
                OffsetDateTime.now(clock).minusHours(1), null, "Bridge", null, "actor", OffsetDateTime.now(clock)
        );

        when(eventRepo.findByTripIdOrderByOccurredAtAsc(tripId)).thenReturn(List.of(event1, event2));

        var events = service.getTripEvents(tripId);

        assertEquals(2, events.size());
        assertEquals(TripOperationalEventType.CHECKPOINT, events.get(0).eventType());
        assertEquals(TripOperationalEventType.DELAY, events.get(1).eventType());
    }
}
