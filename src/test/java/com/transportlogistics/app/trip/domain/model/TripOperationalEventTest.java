package com.transportlogistics.app.trip.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TripOperationalEventTest {

    private final UUID tripId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    @DisplayName("Should create valid checkpoint event")
    void shouldCreateValidCheckpointEvent() {
        var event = TripOperationalEvent.createCheckpoint(
                UUID.randomUUID(),
                tripId,
                TripCheckpointType.DEPARTURE,
                now,
                UUID.randomUUID(),
                "Main Depot Gate 1",
                "Departed on time",
                "dispatcher.john",
                now
        );

        assertNotNull(event.id());
        assertEquals(tripId, event.tripId());
        assertEquals(TripOperationalEventType.CHECKPOINT, event.eventType());
        assertEquals(TripCheckpointType.DEPARTURE, event.checkpointType());
        assertEquals("Main Depot Gate 1", event.locationDescription());
        assertNull(event.delayMinutes());
        assertNull(event.incidentSeverity());
    }

    @Test
    @DisplayName("Should create valid delay event")
    void shouldCreateValidDelayEvent() {
        var event = TripOperationalEvent.createDelay(
                UUID.randomUUID(),
                tripId,
                45,
                "Heavy traffic on expressway",
                now,
                null,
                "E01 Interchange",
                "Traffic jam due to road construction",
                "driver.sam",
                now
        );

        assertEquals(TripOperationalEventType.DELAY, event.eventType());
        assertEquals(45, event.delayMinutes());
        assertEquals("Heavy traffic on expressway", event.reason());
        assertNull(event.checkpointType());
        assertNull(event.incidentSeverity());
    }

    @Test
    @DisplayName("Should create valid incident event")
    void shouldCreateValidIncidentEvent() {
        var event = TripOperationalEvent.createIncident(
                UUID.randomUUID(),
                tripId,
                TripIncidentSeverity.HIGH,
                "Engine overheating on highway",
                now,
                null,
                "Milepost 42",
                "Driver pulled over safely",
                "driver.sam",
                now
        );

        assertEquals(TripOperationalEventType.INCIDENT, event.eventType());
        assertEquals(TripIncidentSeverity.HIGH, event.incidentSeverity());
        assertEquals("Engine overheating on highway", event.reason());
        assertNull(event.checkpointType());
        assertNull(event.delayMinutes());
    }

    @Test
    @DisplayName("Should throw exception when checkpointType is missing for CHECKPOINT event")
    void shouldThrowWhenCheckpointTypeMissing() {
        assertThrows(IllegalArgumentException.class, () ->
                new TripOperationalEvent(
                        UUID.randomUUID(),
                        tripId,
                        TripOperationalEventType.CHECKPOINT,
                        now,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "actor",
                        now,
                        now
                )
        );
    }

    @Test
    @DisplayName("Should throw exception when delayMinutes is zero or negative for DELAY event")
    void shouldThrowWhenDelayMinutesInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new TripOperationalEvent(
                        UUID.randomUUID(),
                        tripId,
                        TripOperationalEventType.DELAY,
                        now,
                        null,
                        null,
                        null,
                        0,
                        "Reason",
                        null,
                        null,
                        "actor",
                        now,
                        now
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                new TripOperationalEvent(
                        UUID.randomUUID(),
                        tripId,
                        TripOperationalEventType.DELAY,
                        now,
                        null,
                        null,
                        null,
                        -15,
                        "Reason",
                        null,
                        null,
                        "actor",
                        now,
                        now
                )
        );
    }

    @Test
    @DisplayName("Should throw exception when reason is missing for DELAY or INCIDENT event")
    void shouldThrowWhenReasonMissing() {
        assertThrows(IllegalArgumentException.class, () ->
                new TripOperationalEvent(
                        UUID.randomUUID(),
                        tripId,
                        TripOperationalEventType.DELAY,
                        now,
                        null,
                        null,
                        null,
                        30,
                        "",
                        null,
                        null,
                        "actor",
                        now,
                        now
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                new TripOperationalEvent(
                        UUID.randomUUID(),
                        tripId,
                        TripOperationalEventType.INCIDENT,
                        now,
                        null,
                        null,
                        null,
                        null,
                        null,
                        TripIncidentSeverity.MEDIUM,
                        null,
                        "actor",
                        now,
                        now
                )
        );
    }

    @Test
    @DisplayName("Should throw exception when incidentSeverity is missing for INCIDENT event")
    void shouldThrowWhenIncidentSeverityMissing() {
        assertThrows(IllegalArgumentException.class, () ->
                new TripOperationalEvent(
                        UUID.randomUUID(),
                        tripId,
                        TripOperationalEventType.INCIDENT,
                        now,
                        null,
                        null,
                        null,
                        null,
                        "Flat tire",
                        null,
                        null,
                        "actor",
                        now,
                        now
                )
        );
    }
}
