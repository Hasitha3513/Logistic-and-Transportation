package com.transportlogistics.app.routing.infrastructure.adapters.in.web;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.domain.model.Route;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RouteControllerTest {
    private RouteUseCase routes;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        routes = mock(RouteUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new RouteController(routes))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void createsTypedMultiStopRoute() throws Exception {
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var stop = UUID.randomUUID();
        when(routes.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(post("/routes").contentType(MediaType.APPLICATION_JSON).content("""
                {"code":"RT-1","name":"Central route","originLocationId":"%s",
                 "destinationLocationId":"%s","plannedDistanceKm":120.5,
                 "estimatedDurationMinutes":180,"stops":["%s"],"active":true}
                """.formatted(origin, destination, stop)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plannedDistanceKm").value(120.5))
                .andExpect(jsonPath("$.estimatedDurationMinutes").value(180))
                .andExpect(jsonPath("$.stopLocationIds[0]").value(stop.toString()));
    }

    @Test
    void validatesRequiredPlanningFields() throws Exception {
        mvc.perform(post("/routes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"RT-1\",\"name\":\"Route\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(routes);
    }

    @Test
    void passesOptionalSearchFiltersToUseCase() throws Exception {
        var origin = UUID.randomUUID();
        when(routes.search("central", origin, null, true)).thenReturn(List.of());

        mvc.perform(get("/routes").param("query", "central")
                        .param("originLocationId", origin.toString()).param("active", "true"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));

        verify(routes).search("central", origin, null, true);
    }
}
