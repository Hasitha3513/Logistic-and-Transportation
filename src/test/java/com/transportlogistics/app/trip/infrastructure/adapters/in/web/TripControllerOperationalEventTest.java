package com.transportlogistics.app.trip.infrastructure.adapters.in.web;

import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import com.transportlogistics.app.trip.application.ports.in.TripOperationalEventUseCase;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.domain.model.TripCheckpointType;
import com.transportlogistics.app.trip.domain.model.TripIncidentSeverity;
import com.transportlogistics.app.trip.domain.model.TripOperationalEvent;
import com.transportlogistics.app.trip.domain.model.TripOperationalEventType;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.controllers.TripController;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.mappers.TripWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Mockito imports (only mock needed)
import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripControllerOperationalEventTest {

    private TripUseCase trips;
    private TripOperationalEventUseCase operationalEvents;
    private MockMvc mvc;

    private final UUID tripId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        trips = mock(TripUseCase.class);
        operationalEvents = new TripOperationalEventUseCase() {
            private final java.util.List<TripOperationalEvent> events = new java.util.ArrayList<>();
            public TripOperationalEvent recordCheckpoint(UUID tripId, RecordCheckpointCommand command, String actor) {
            OffsetDateTime occurredAt = command.occurredAt();
            if (occurredAt == null) {
                occurredAt = OffsetDateTime.now();
            }
            TripOperationalEvent ev = new TripOperationalEvent(UUID.randomUUID(), tripId, TripOperationalEventType.CHECKPOINT,
                    occurredAt, command.locationId(), command.locationDescription(),
                    command.checkpointType(), null, null, null, command.remarks(), actor,
                    OffsetDateTime.now(), OffsetDateTime.now());
                



                events.add(ev);
                return ev;
            }

            @Override
    public TripOperationalEvent recordDelay(UUID tripId, RecordDelayCommand command, String actor) {
        OffsetDateTime occurredAt = command.occurredAt();
        if (occurredAt == null) {
            occurredAt = OffsetDateTime.now();
        }
        TripOperationalEvent ev = new TripOperationalEvent(UUID.randomUUID(), tripId, TripOperationalEventType.DELAY,
                occurredAt, command.locationId(), command.locationDescription(),
                null, command.delayMinutes(), command.reason(), null, command.remarks(), actor,
                OffsetDateTime.now(), OffsetDateTime.now());
        events.add(ev);
        return ev;
    }



            @Override
            public TripOperationalEvent recordIncident(UUID tripId, RecordIncidentCommand command, String actor) {
                OffsetDateTime occurredAt = command.occurredAt();
                if (occurredAt == null) {
                    occurredAt = OffsetDateTime.now();
                }
                TripOperationalEvent ev = new TripOperationalEvent(UUID.randomUUID(), tripId, TripOperationalEventType.INCIDENT,
                        occurredAt, command.locationId(), command.locationDescription(),
                        null, null, command.description(), command.incidentSeverity(), null, actor,
                        OffsetDateTime.now(), OffsetDateTime.now());
                events.add(ev);
                return ev;
            }

            @Override
            public java.util.List<TripOperationalEvent> getTripEvents(UUID tripId) {
                return events.stream().filter(e -> e.tripId().equals(tripId)).toList();
            }

            @Override
            public TripOperationalEvent getEvent(UUID tripId, UUID eventId) {
                return events.stream().filter(e -> e.tripId().equals(tripId) && e.id().equals(eventId)).findFirst().orElse(null);
            }
        };
        var mapper = Mappers.getMapper(TripWebMapper.class);
        var controller = new TripController(trips, operationalEvents, mapper);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /trips/{id}/operational-events should return events list")
    void shouldListOperationalEvents() throws Exception {
        var now = OffsetDateTime.now();
        // Populate stub with a checkpoint event using the stub's method
        operationalEvents.recordCheckpoint(tripId,
                new TripOperationalEventUseCase.RecordCheckpointCommand(
                        TripCheckpointType.DEPARTURE, now, null, "Depot Gate", "Departed"),
                "dispatcher");

        mvc.perform(get("/trips/{id}/operational-events", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("CHECKPOINT"))
                .andExpect(jsonPath("$[0].checkpointType").value("DEPARTURE"))
                .andExpect(jsonPath("$[0].locationDescription").value("Depot Gate"));
    }

    @Test
    @DisplayName("POST /trips/{id}/checkpoints should record checkpoint event")
    void shouldRecordCheckpoint() throws Exception {
        var now = OffsetDateTime.now();
        // Record a checkpoint event using the stub
        operationalEvents.recordCheckpoint(tripId,
                new TripOperationalEventUseCase.RecordCheckpointCommand(
                        TripCheckpointType.ARRIVAL, now, null, "Warehouse A", "Arrived"),
                "driver");
        mvc.perform(post("/trips/{id}/checkpoints", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checkpointType": "ARRIVAL",
                                  "locationDescription": "Warehouse A",
                                  "remarks": "Arrived"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("CHECKPOINT"))
                .andExpect(jsonPath("$.checkpointType").value("ARRIVAL"));
    }

    @Test
    @DisplayName("POST /trips/{id}/delays should record delay event")
    void shouldRecordDelay() throws Exception {
        var now = OffsetDateTime.now();
        // Record a delay event using the stub
        operationalEvents.recordDelay(tripId,
                new TripOperationalEventUseCase.RecordDelayCommand(
                        30, "Traffic", now, null, "Highway 1", "Congestion"),
                "driver");

        mvc.perform(post("/trips/{id}/delays", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "delayMinutes": 30,
                                  "reason": "Traffic",
                                  "locationDescription": "Highway 1",
                                  "remarks": "Congestion"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("DELAY"))
                .andExpect(jsonPath("$.delayMinutes").value(30))
                .andExpect(jsonPath("$.reason").value("Traffic"));
    }

    @Test
    @DisplayName("POST /trips/{id}/incidents should record incident event")
    void shouldRecordIncident() throws Exception {
        var now = OffsetDateTime.now();
        // Record an incident event using the stub
        operationalEvents.recordIncident(tripId,
                new TripOperationalEventUseCase.RecordIncidentCommand(
                        TripIncidentSeverity.MEDIUM, "Puncture", now, null, "Bridge Exit", "Changed tire"),
                "driver");

        mvc.perform(post("/trips/{id}/incidents", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentSeverity": "MEDIUM",
                                  "description": "Puncture",
                                  "locationDescription": "Bridge Exit",
                                  "remarks": "Changed tire"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("INCIDENT"))
                .andExpect(jsonPath("$.incidentSeverity").value("MEDIUM"))
                .andExpect(jsonPath("$.reason").value("Puncture"));
    }

    @Test
    @DisplayName("POST /trips/{id}/delays with zero or negative delay should return 400")
    void shouldRejectInvalidDelay() throws Exception {
        mvc.perform(post("/trips/{id}/delays", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "delayMinutes": 0,
                                  "reason": "Traffic"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
