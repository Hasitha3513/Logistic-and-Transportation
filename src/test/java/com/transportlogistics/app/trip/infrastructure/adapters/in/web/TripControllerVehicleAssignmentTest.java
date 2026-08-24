package com.transportlogistics.app.trip.infrastructure.adapters.in.web;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.controllers.TripController;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.mappers.TripWebMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TripControllerVehicleAssignmentTest {
    @Test
    void allocationConflictUsesConsistent409ApiError() throws Exception {
        var trips = mock(TripUseCase.class);
        var tripId = UUID.randomUUID();
        var vehicleId = UUID.randomUUID();
        when(trips.assignVehicle(tripId, vehicleId, "dispatcher"))
                .thenThrow(new ConflictException("Vehicle already has an overlapping trip allocation"));
        var mapper = Mappers.getMapper(TripWebMapper.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TripController(trips, mapper))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/trips/{tripId}/assign-vehicle", tripId).principal(() -> "dispatcher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"%s\"}".formatted(vehicleId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALLOCATION_CONFLICT"))
                .andExpect(jsonPath("$.status").value(409));
    }
}
