package com.transportlogistics.app.routing.infrastructure.adapters.in.web;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.domain.model.*;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.controllers.RouteController;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.mappers.RouteWebMapper;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        var mapper = Mappers.getMapper(RouteWebMapper.class);
        mvc = MockMvcBuilders.standaloneSetup(new RouteController(routes, mapper))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void createsTypedMultiStopRoute() throws Exception {
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var stop = UUID.randomUUID();
        when(routes.create(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

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

    @Test
    void retrievesRouteRevisions() throws Exception {
        var routeId = UUID.randomUUID();
        var revision = new RouteRevision(UUID.randomUUID(), routeId, 1, "RT-1", "Central route",
                UUID.randomUUID(), UUID.randomUUID(), 100.0, 90, true, List.of(), OffsetDateTime.now(), "admin");
        when(routes.getRevisions(routeId)).thenReturn(List.of(revision));

        mvc.perform(get("/routes/{id}/revisions", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revisionNumber").value(1))
                .andExpect(jsonPath("$[0].code").value("RT-1"))
                .andExpect(jsonPath("$[0].changedBy").value("admin"));
    }

    @Test
    void createsRouteDisruption() throws Exception {
        var routeId = UUID.randomUUID();
        var disruption = new RouteDisruption(UUID.randomUUID(), routeId, RouteDisruptionType.ROAD_CLOSURE,
                DisruptionSeverity.HIGH, "Flooded bridge", OffsetDateTime.now(), null, null,
                DisruptionStatus.ACTIVE, OffsetDateTime.now(), "traffic_lead", null, null);
        when(routes.createDisruption(eq(routeId), eq(RouteDisruptionType.ROAD_CLOSURE), eq(DisruptionSeverity.HIGH),
                eq("Flooded bridge"), any(), any(), any(), any())).thenReturn(disruption);

        mvc.perform(post("/routes/{id}/disruptions", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"disruptionType":"ROAD_CLOSURE","severity":"HIGH",
                         "description":"Flooded bridge","effectiveFrom":"2026-08-24T12:00:00Z"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.disruptionType").value("ROAD_CLOSURE"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void resolvesRouteDisruption() throws Exception {
        var routeId = UUID.randomUUID();
        var disruptionId = UUID.randomUUID();
        var resolved = new RouteDisruption(disruptionId, routeId, RouteDisruptionType.WEATHER,
                DisruptionSeverity.MEDIUM, "Fog cleared", OffsetDateTime.now().minusHours(1), null, null,
                DisruptionStatus.RESOLVED, OffsetDateTime.now().minusHours(1), "dispatcher",
                OffsetDateTime.now(), "supervisor");
        when(routes.resolveDisruption(eq(routeId), eq(disruptionId), any())).thenReturn(resolved);

        mvc.perform(post("/routes/{id}/disruptions/{disruptionId}/resolve", routeId, disruptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedBy").value("supervisor"));
    }

    @Test
    void optimizesRouteStopsAndReturnsPreview() throws Exception {
        var routeId = UUID.randomUUID();
        var s1 = UUID.randomUUID();
        var s2 = UUID.randomUUID();
        var result = new RouteOptimizationResult(
                routeId, List.of(s1, s2), List.of(s2, s1),
                100.0, 80.0, 120, 96, 20.0, 24, 20.0
        );
        when(routes.optimizeRoute(routeId)).thenReturn(result);

        mvc.perform(post("/routes/{id}/optimize", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(routeId.toString()))
                .andExpect(jsonPath("$.originalEstimatedDistanceKm").value(100.0))
                .andExpect(jsonPath("$.optimizedEstimatedDistanceKm").value(80.0))
                .andExpect(jsonPath("$.distanceSavedKm").value(20.0))
                .andExpect(jsonPath("$.durationSavedMinutes").value(24))
                .andExpect(jsonPath("$.percentageDistanceImprovement").value(20.0));
    }

    @Test
    void appliesOptimizedRouteSequence() throws Exception {
        var routeId = UUID.randomUUID();
        var s1 = UUID.randomUUID();
        var s2 = UUID.randomUUID();
        var updated = new Route(routeId, "RT-1", "Central route", UUID.randomUUID(), UUID.randomUUID(), 80.0, 96, true, List.of(s2, s1));
        when(routes.applyOptimization(eq(routeId), eq(List.of(s2, s1)), any())).thenReturn(updated);

        mvc.perform(post("/routes/{id}/apply-optimization", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"optimizedStopLocationIds":["%s","%s"]}
                        """.formatted(s2, s1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedDistanceKm").value(80.0))
                .andExpect(jsonPath("$.estimatedDurationMinutes").value(96))
                .andExpect(jsonPath("$.stopLocationIds[0]").value(s2.toString()))
                .andExpect(jsonPath("$.stopLocationIds[1]").value(s1.toString()));
    }

    @Test
    void retrievesRoutePerformanceMetrics() throws Exception {
        var routeId = UUID.randomUUID();
        var analytics = new RoutePerformanceAnalytics(
                routeId, "RT-1", "Central route",
                5, 4, 100.0, 105.0, 5.0, 5.0,
                120, 130, 10, 8.33, 3, 1, 15.0
        );
        when(routes.getRoutePerformance(eq(routeId), any(), any())).thenReturn(analytics);

        mvc.perform(get("/routes/{id}/performance", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(routeId.toString()))
                .andExpect(jsonPath("$.totalTripCount").value(5))
                .andExpect(jsonPath("$.completedTripCount").value(4))
                .andExpect(jsonPath("$.averageActualDistanceKm").value(105.0))
                .andExpect(jsonPath("$.distanceVarianceKm").value(5.0))
                .andExpect(jsonPath("$.distanceVariancePercent").value(5.0))
                .andExpect(jsonPath("$.onTimeTripCount").value(3))
                .andExpect(jsonPath("$.delayedTripCount").value(1))
                .andExpect(jsonPath("$.averageDelayMinutes").value(15.0));
    }
}
