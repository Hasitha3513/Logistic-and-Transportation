package com.transportlogistics.app.trip.infrastructure.adapters.in.web;

import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripControllerDispatchTest {
    @Test
    void passesAuthenticatedActorAndRemarksToDispatchUseCase() throws Exception {
        var trips = mock(TripUseCase.class);
        var tripId = UUID.randomUUID();
        var mvc = MockMvcBuilders.standaloneSetup(new TripController(trips)).build();

        mvc.perform(post("/trips/{tripId}/dispatch", tripId).principal(() -> "dispatcher")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"remarks\":\"Gate 4\"}"))
                .andExpect(status().isOk());

        verify(trips).dispatch(tripId, "dispatcher", "Gate 4");
    }
}
