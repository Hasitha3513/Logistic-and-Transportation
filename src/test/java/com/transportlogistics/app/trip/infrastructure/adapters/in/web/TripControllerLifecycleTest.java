package com.transportlogistics.app.trip.infrastructure.adapters.in.web;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.controllers.TripController;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.mappers.TripWebMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripControllerLifecycleTest {
    @Test
    void createPassesBusinessInputsWithoutGeneratingLifecycleDefaultsInController() throws Exception {
        var trips = mock(TripUseCase.class);
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var mapper = Mappers.getMapper(TripWebMapper.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TripController(trips, mapper)).build();

        mvc.perform(post("/trips").contentType(MediaType.APPLICATION_JSON).content("""
                {"originLocationId":"%s","destinationLocationId":"%s",
                 "requestedStartTime":"2026-09-01T08:00:00Z",
                 "requestedEndTime":"2026-09-01T12:00:00Z"}
                """.formatted(origin, destination)))
                .andExpect(status().isCreated());

        verify(trips).create(argThat(command -> command.originLocationId().equals(origin)
                && command.destinationLocationId().equals(destination) && command.priority() == null));
    }

    @Test
    void lifecycleEndpointsPassAuthenticatedActorAndTypedExecutionValues() throws Exception {
        var trips = mock(TripUseCase.class);
        var tripId = UUID.randomUUID();
        var mapper = Mappers.getMapper(TripWebMapper.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TripController(trips, mapper)).build();

        mvc.perform(post("/trips/{id}/submit", tripId).principal(() -> "requester"))
                .andExpect(status().isOk());
        mvc.perform(post("/trips/{id}/approve", tripId).principal(() -> "approver")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post("/trips/{id}/start", tripId).principal(() -> "driver")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"startOdometerKm\":1250.0}"))
                .andExpect(status().isOk());
        mvc.perform(post("/trips/{id}/complete", tripId).principal(() -> "driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endOdometerKm\":1300.0,\"completionRemarks\":\"Delivered\"}"))
                .andExpect(status().isOk());

        verify(trips).transition(tripId, new TripCommand.Submit(), "requester");
        verify(trips).transition(tripId, new TripCommand.Approve(), "approver");
        verify(trips).transition(tripId, new TripCommand.Start(1250.0), "driver");
        verify(trips).transition(tripId, new TripCommand.Complete(1300.0, "Delivered"), "driver");
    }

    @Test
    void routeAssignmentPassesTypedRouteAndAuthenticatedActor() throws Exception {
        var trips = mock(TripUseCase.class);
        var tripId = UUID.randomUUID();
        var routeId = UUID.randomUUID();
        var mapper = Mappers.getMapper(TripWebMapper.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TripController(trips, mapper)).build();

        mvc.perform(post("/trips/{id}/assign-route", tripId).principal(() -> "planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeId\":\"" + routeId + "\"}"))
                .andExpect(status().isOk());

        verify(trips).assignRoute(tripId, routeId, "planner");
    }

    @Test
    void lifecycleConflictsUseTheirSpecificApiErrorCode() throws Exception {
        var trips = mock(TripUseCase.class);
        var tripId = UUID.randomUUID();
        when(trips.transition(tripId, new TripCommand.Approve(), "approver"))
                .thenThrow(new ConflictException("TRIP_NOT_APPROVABLE", "Approve requires a SUBMITTED trip"));
        var mapper = Mappers.getMapper(TripWebMapper.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TripController(trips, mapper))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/trips/{id}/approve", tripId).principal(() -> "approver")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_APPROVABLE"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void missingLifecycleReasonUsesSpecificBadRequestCode() throws Exception {
        var trips = mock(TripUseCase.class);
        var tripId = UUID.randomUUID();
        when(trips.transition(tripId, new TripCommand.Cancel(null), "dispatcher"))
                .thenThrow(new BusinessRuleException("CANCELLATION_REASON_REQUIRED",
                        "Cancellation reason is required"));
        var mapper = Mappers.getMapper(TripWebMapper.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TripController(trips, mapper))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/trips/{id}/cancel", tripId).principal(() -> "dispatcher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANCELLATION_REASON_REQUIRED"));
    }
}
