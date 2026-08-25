package com.transportlogistics.app.routing.application.service;

import com.transportlogistics.app.routing.RoutePerformanceTripLookupPort;
import com.transportlogistics.app.routing.RouteTripMetric;
import com.transportlogistics.app.routing.application.ports.out.RouteDistancePort;
import com.transportlogistics.app.routing.application.ports.out.RouteDisruptionRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteEventPublisher;
import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteRevisionRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteTransaction;
import com.transportlogistics.app.routing.domain.event.RouteDisruptionCreatedEvent;
import com.transportlogistics.app.routing.domain.event.RouteDisruptionResolvedEvent;
import com.transportlogistics.app.routing.domain.model.*;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
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

class RouteServiceTest {
    private RouteRepository repository;
    private RouteRevisionRepository revisionRepository;
    private RouteDisruptionRepository disruptionRepository;
    private RouteEventPublisher eventPublisher;
    private RouteDistancePort distancePort;
    private RoutePerformanceTripLookupPort performanceTripLookup;
    private RouteTransaction transaction;
    private Clock clock;
    private RouteService service;

    @BeforeEach
    void setUp() {
        repository = mock(RouteRepository.class);
        revisionRepository = mock(RouteRevisionRepository.class);
        disruptionRepository = mock(RouteDisruptionRepository.class);
        eventPublisher = mock(RouteEventPublisher.class);
        distancePort = mock(RouteDistancePort.class);
        performanceTripLookup = mock(RoutePerformanceTripLookupPort.class);
        transaction = mock(RouteTransaction.class);
        when(transaction.execute(any())).thenAnswer(invocation ->
                ((java.util.function.Supplier<?>) invocation.getArgument(0)).get());
        clock = Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC);
        service = new RouteService(repository, revisionRepository, disruptionRepository, eventPublisher, distancePort, performanceTripLookup, transaction, clock);
    }

    @Test
    void createRouteSavesRouteAndInitialRevision() {
        var route = route();
        when(repository.save(any())).thenReturn(route);

        var created = service.create(route, "operator1");

        assertNotNull(created);
        verify(repository).save(route);
        verify(revisionRepository).save(argThat(rev ->
                rev.revisionNumber() == 1
                        && rev.routeId().equals(route.id())
                        && rev.changedBy().equals("operator1")
                        && rev.code().equals(route.code())
        ));
    }

    @Test
    void updateRouteIncrementsRevisionNumber() {
        var route = route();
        when(repository.findById(route.id())).thenReturn(Optional.of(route));
        when(repository.save(any())).thenReturn(route);
        when(revisionRepository.findLatestRevisionNumber(route.id())).thenReturn(1);

        service.update(route.id(), route, "planner");

        verify(revisionRepository).save(argThat(rev ->
                rev.revisionNumber() == 2
                        && rev.routeId().equals(route.id())
                        && rev.changedBy().equals("planner")
        ));
    }

    @Test
    void deactivationPreservesStopsAndCreatesRevision() {
        var route = route();
        when(repository.findById(route.id())).thenReturn(Optional.of(route));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(revisionRepository.findLatestRevisionNumber(route.id())).thenReturn(2);

        service.deactivate(route.id(), "admin");

        verify(repository).save(argThat(saved -> !saved.active()));
        verify(revisionRepository).save(argThat(rev ->
                rev.revisionNumber() == 3
                        && !rev.active()
                        && rev.changedBy().equals("admin")
        ));
    }

    @Test
    void createsDisruptionAndPublishesEvent() {
        var route = route();
        var detour = route();
        when(repository.findById(route.id())).thenReturn(Optional.of(route));
        when(repository.findById(detour.id())).thenReturn(Optional.of(detour));
        when(disruptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var from = OffsetDateTime.now(clock);
        var until = from.plusHours(6);
        var disruption = service.createDisruption(
                route.id(),
                RouteDisruptionType.ROAD_CLOSURE,
                DisruptionSeverity.HIGH,
                "Flooding on bridge",
                from,
                until,
                detour.id(),
                "traffic_lead"
        );

        assertNotNull(disruption);
        assertEquals(DisruptionStatus.ACTIVE, disruption.status());
        verify(disruptionRepository).save(any());
        verify(eventPublisher).publish(isA(RouteDisruptionCreatedEvent.class));
    }

    @Test
    void resolvesDisruptionAndPublishesEvent() {
        var routeId = UUID.randomUUID();
        var disruptionId = UUID.randomUUID();
        var disruption = RouteDisruption.create(
                routeId,
                RouteDisruptionType.WEATHER,
                DisruptionSeverity.MEDIUM,
                "Heavy fog",
                OffsetDateTime.now(clock).minusHours(2),
                null,
                null,
                OffsetDateTime.now(clock).minusHours(2),
                "dispatcher"
        );
        when(repository.findById(routeId)).thenReturn(Optional.of(route()));
        when(disruptionRepository.findById(disruptionId)).thenReturn(Optional.of(disruption));
        when(disruptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resolved = service.resolveDisruption(routeId, disruptionId, "safety_officer");

        assertEquals(DisruptionStatus.RESOLVED, resolved.status());
        assertEquals("safety_officer", resolved.resolvedBy());
        verify(eventPublisher).publish(isA(RouteDisruptionResolvedEvent.class));
    }

    @Test
    void rejectsDisruptionWithInactiveDetourRoute() {
        var route = route();
        var inactiveDetour = new Route(UUID.randomUUID(), "RT-DET", "Detour", UUID.randomUUID(), UUID.randomUUID(), 40.0, 50, false, List.of());
        when(repository.findById(route.id())).thenReturn(Optional.of(route));
        when(repository.findById(inactiveDetour.id())).thenReturn(Optional.of(inactiveDetour));

        assertThrows(IllegalArgumentException.class, () ->
                service.createDisruption(route.id(), RouteDisruptionType.ROAD_CLOSURE, DisruptionSeverity.HIGH,
                        "Blocked", OffsetDateTime.now(clock), null, inactiveDetour.id(), "user")
        );
    }

    @Test
    void optimizeRouteReturnsValidPreview() {
        var origin = UUID.randomUUID();
        var dest = UUID.randomUUID();
        var s1 = UUID.randomUUID();
        var s2 = UUID.randomUUID();
        var route = new Route(UUID.randomUUID(), "RTE-OPT", "Opt Route", origin, dest, 100.0, 120, true, List.of(s1, s2));

        when(repository.findById(route.id())).thenReturn(Optional.of(route));
        when(distancePort.getDistanceKm(any(), any())).thenReturn(15.0);
        when(distancePort.getLocation(any())).thenAnswer(invocation -> Optional.of(
                new RouteLocation(invocation.getArgument(0), "LOC", "Location", 6.9, 79.8)));

        var preview = service.optimizeRoute(route.id());

        assertNotNull(preview);
        assertEquals(route.id(), preview.routeId());
        assertEquals(2, preview.originalStopLocationIds().size());
        assertEquals(2, preview.optimizedStopLocationIds().size());
    }

    @Test
    void applyOptimizationUpdatesRouteAndCreatesRevision() {
        var origin = UUID.randomUUID();
        var dest = UUID.randomUUID();
        var s1 = UUID.randomUUID();
        var s2 = UUID.randomUUID();
        var route = new Route(UUID.randomUUID(), "RTE-OPT", "Opt Route", origin, dest, 100.0, 120, true, List.of(s1, s2));

        when(repository.findById(route.id())).thenReturn(Optional.of(route));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(revisionRepository.findLatestRevisionNumber(route.id())).thenReturn(3);
        when(distancePort.getLocation(any())).thenAnswer(invocation -> Optional.of(
                new RouteLocation(invocation.getArgument(0), "LOC", "Location", 6.9, 79.8)));
        when(distancePort.getDistanceKm(origin, s1)).thenReturn(20.0);
        when(distancePort.getDistanceKm(s1, s2)).thenReturn(20.0);
        when(distancePort.getDistanceKm(s2, dest)).thenReturn(20.0);
        when(distancePort.getDistanceKm(origin, s2)).thenReturn(10.0);
        when(distancePort.getDistanceKm(s2, s1)).thenReturn(10.0);
        when(distancePort.getDistanceKm(s1, dest)).thenReturn(10.0);

        var applied = service.applyOptimization(route.id(), List.of(s2, s1), "route_planner");

        assertNotNull(applied);
        assertEquals(List.of(s2, s1), applied.stopLocationIds());
        verify(repository).save(any());
        verify(revisionRepository).save(argThat(rev ->
                rev.revisionNumber() == 4
                        && rev.changedBy().equals("route_planner")
                        && rev.stopLocationIds().equals(List.of(s2, s1))
        ));
    }

    @Test
    void getRoutePerformanceCalculatesOperationalMetrics() {
        var route = route();
        when(repository.findById(route.id())).thenReturn(Optional.of(route));

        var from = OffsetDateTime.now(clock).minusDays(7);
        var to = OffsetDateTime.now(clock);

        var trip1 = new RouteTripMetric(
                UUID.randomUUID(), "TRP-1", "COMPLETED",
                from, to, from, to, 100.0, 150.0, 10
        );
        when(performanceTripLookup.findTripsForRoute(route.id(), from, to)).thenReturn(List.of(trip1));

        var analytics = service.getRoutePerformance(route.id(), from, to);

        assertNotNull(analytics);
        assertEquals(route.id(), analytics.routeId());
        assertEquals(1, analytics.totalTripCount());
        assertEquals(1, analytics.completedTripCount());
        assertEquals(50.0, analytics.averageActualDistanceKm());
    }

    @Test
    void optimizationRejectsMissingCoordinatesInsteadOfFabricatingDistance() {
        var origin = UUID.randomUUID();
        var route = new Route(UUID.randomUUID(), "RTE-NO-GEO", "No geometry", origin, UUID.randomUUID(),
                100.0, 120, true, List.of(UUID.randomUUID(), UUID.randomUUID()));
        when(repository.findById(route.id())).thenReturn(Optional.of(route));
        when(distancePort.getLocation(origin)).thenReturn(Optional.of(new RouteLocation(origin, "ORIGIN", "Origin", null, null)));

        var error = assertThrows(com.transportlogistics.app.shared.domain.BusinessRuleException.class,
                () -> service.optimizeRoute(route.id()));

        assertEquals("ROUTE_OPTIMIZATION_DATA_UNAVAILABLE", error.code());
        verify(distancePort, never()).getDistanceKm(any(), any());
    }

    @Test
    void optimizationRejectsMissingInactiveAndSingleStopRoutesWithStableErrors() {
        var missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.optimizeRoute(missingId));

        var active = route();
        var inactive = new Route(active.id(), active.code(), active.name(), active.originLocationId(),
                active.destinationLocationId(), active.plannedDistanceKm(), active.estimatedDurationMinutes(),
                false, List.of(UUID.randomUUID(), UUID.randomUUID()));
        when(repository.findById(inactive.id())).thenReturn(Optional.of(inactive));
        var inactiveError = assertThrows(com.transportlogistics.app.shared.domain.BusinessRuleException.class,
                () -> service.optimizeRoute(inactive.id()));
        assertEquals("ROUTE_OPTIMIZATION_NOT_APPLICABLE", inactiveError.code());

        var singleStop = route();
        when(repository.findById(singleStop.id())).thenReturn(Optional.of(singleStop));
        var singleStopError = assertThrows(com.transportlogistics.app.shared.domain.BusinessRuleException.class,
                () -> service.optimizeRoute(singleStop.id()));
        assertEquals("ROUTE_OPTIMIZATION_NOT_APPLICABLE", singleStopError.code());
        verify(distancePort, never()).getDistanceKm(any(), any());
    }

    @Test
    void routePerformanceDefaultsToBoundedThirtyDayRange() {
        var route = route();
        when(repository.findById(route.id())).thenReturn(Optional.of(route));
        when(performanceTripLookup.findTripsForRoute(any(), any(), any())).thenReturn(List.of());

        service.getRoutePerformance(route.id(), null, null);

        var expectedTo = OffsetDateTime.now(clock);
        verify(performanceTripLookup).findTripsForRoute(route.id(), expectedTo.minusDays(30), expectedTo);
    }

    @Test
    void routePerformanceRejectsReversedAndOverlongDateRangesBeforeQueryingTrips() {
        var route = route();
        when(repository.findById(route.id())).thenReturn(Optional.of(route));
        var to = OffsetDateTime.now(clock);

        var reversed = assertThrows(com.transportlogistics.app.shared.domain.BusinessRuleException.class,
                () -> service.getRoutePerformance(route.id(), to.plusMinutes(1), to));
        assertEquals("INVALID_DATE_RANGE", reversed.code());

        var overlong = assertThrows(com.transportlogistics.app.shared.domain.BusinessRuleException.class,
                () -> service.getRoutePerformance(route.id(), to.minusDays(367), to));
        assertEquals("INVALID_DATE_RANGE", overlong.code());
        verifyNoInteractions(performanceTripLookup);
    }

    @Test
    void searchDelegatesProperly() {
        var origin = UUID.randomUUID();
        service.search("  central  ", origin, null, true);
        verify(repository).search("central", origin, null, true);
    }

    private Route route() {
        return new Route(UUID.randomUUID(), "RT-1", "Central route", UUID.randomUUID(), UUID.randomUUID(),
                50.0, 75, true, List.of(UUID.randomUUID()));
    }
}
