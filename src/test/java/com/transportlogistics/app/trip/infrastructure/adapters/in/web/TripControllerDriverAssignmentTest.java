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

class TripControllerDriverAssignmentTest {
    @Test
    void schedulingConflictUsesConsistent409ApiError() throws Exception {
        var trips = mock(TripUseCase.class);
        var tripId = UUID.randomUUID();
        var driverId = UUID.randomUUID();
        when(trips.assignDriver(tripId, driverId, "B", "dispatcher"))
                .thenThrow(new ConflictException("Driver already has an overlapping trip assignment"));
        var mapper = Mappers.getMapper(TripWebMapper.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TripController(trips, mapper))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/trips/{tripId}/assign-driver", tripId).principal(() -> "dispatcher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"%s\",\"requiredLicenseClass\":\"B\"}".formatted(driverId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALLOCATION_CONFLICT"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void licenseClassIsRequired() throws Exception {
        var trips = mock(TripUseCase.class);
        var mapper = Mappers.getMapper(TripWebMapper.class);
        var mvc = MockMvcBuilders.standaloneSetup(new TripController(trips, mapper))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/trips/{tripId}/assign-driver", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(trips);
    }
}
