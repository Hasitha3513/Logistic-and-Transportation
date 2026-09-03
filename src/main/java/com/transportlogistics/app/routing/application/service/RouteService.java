package com.transportlogistics.app.routing.application.service;

import com.transportlogistics.app.routing.RoutePerformanceTripLookupPort;
import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.application.ports.out.RouteDistancePort;
import com.transportlogistics.app.routing.application.ports.out.RouteDisruptionRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteEventPublisher;
import com.transportlogistics.app.routing.application.ports.out.RouteOperationalExceptionPublisher;
import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteRevisionRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteTransaction;
import com.transportlogistics.app.routing.domain.event.RouteDisruptionCreatedEvent;
import com.transportlogistics.app.routing.domain.event.RouteDisruptionResolvedEvent;
import com.transportlogistics.app.routing.domain.model.*;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class RouteService implements RouteUseCase {
    private static final int DEFAULT_ANALYTICS_DAYS = 30;
    private static final int MAX_ANALYTICS_DAYS = 366;
    private final RouteRepository repo;
    private final RouteRevisionRepository revisionRepo;
    private final RouteDisruptionRepository disruptionRepo;
    private final RouteEventPublisher eventPublisher;
    private final RouteOperationalExceptionPublisher operationalExceptions;
    private final RouteDistancePort distancePort;
    private final RoutePerformanceTripLookupPort performanceTripLookup;
    private final RouteTransaction transaction;
    private final Clock clock;

    public RouteService(RouteRepository repo,
                        RouteRevisionRepository revisionRepo,
                        RouteDisruptionRepository disruptionRepo,
                        RouteEventPublisher eventPublisher,
                        RouteDistancePort distancePort,
                        RoutePerformanceTripLookupPort performanceTripLookup,
                        RouteTransaction transaction,
                        Clock clock) {
        this(repo, revisionRepo, disruptionRepo, eventPublisher, RouteOperationalExceptionPublisher.noop(),
            distancePort, performanceTripLookup, transaction, clock);
    }

    public RouteService(RouteRepository repo,
                        RouteRevisionRepository revisionRepo,
                        RouteDisruptionRepository disruptionRepo,
                        RouteEventPublisher eventPublisher,
                        RouteOperationalExceptionPublisher operationalExceptions,
                        RouteDistancePort distancePort,
                        RoutePerformanceTripLookupPort performanceTripLookup,
                        RouteTransaction transaction,
                        Clock clock) {
        this.repo = Objects.requireNonNull(repo, "RouteRepository is required");
        this.revisionRepo = Objects.requireNonNull(revisionRepo, "RouteRevisionRepository is required");
        this.disruptionRepo = Objects.requireNonNull(disruptionRepo, "RouteDisruptionRepository is required");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "RouteEventPublisher is required");
        this.operationalExceptions = Objects.requireNonNull(operationalExceptions,
            "RouteOperationalExceptionPublisher is required");
        this.distancePort = Objects.requireNonNull(distancePort, "RouteDistancePort is required");
        this.performanceTripLookup = Objects.requireNonNull(performanceTripLookup, "RoutePerformanceTripLookupPort is required");
        this.transaction = Objects.requireNonNull(transaction, "RouteTransaction is required");
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    @Override
    public Route create(Route value, String actor) {
        var created = repo.save(value);
        var revision = RouteRevision.from(created, 1, OffsetDateTime.now(clock), actor);
        revisionRepo.save(revision);
        return created;
    }

    @Override
    public Route get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Route not found: " + id));
    }

    @Override
    public List<Route> list() {
        return repo.findAll();
    }

    @Override
    public List<Route> search(String query, UUID originLocationId, UUID destinationLocationId, Boolean active) {
        var normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        if (normalizedQuery == null && originLocationId == null && destinationLocationId == null && active == null) {
            return list();
        }
        return repo.search(normalizedQuery, originLocationId, destinationLocationId, active);
    }

    @Override
    public Route update(UUID id, Route value, String actor) {
        get(id);
        var updated = repo.save(value);
        int nextRev = revisionRepo.findLatestRevisionNumber(id) + 1;
        var revision = RouteRevision.from(updated, nextRev, OffsetDateTime.now(clock), actor);
        revisionRepo.save(revision);
        return updated;
    }

    @Override
    public void deactivate(UUID id, String actor) {
        var v = get(id);
        if (!v.active()) {
            return;
        }
        var deactivated = new Route(v.id(), v.code(), v.name(), v.originLocationId(), v.destinationLocationId(),
                v.plannedDistanceKm(), v.estimatedDurationMinutes(), false, v.stopLocationIds());
        repo.save(deactivated);
        int nextRev = revisionRepo.findLatestRevisionNumber(id) + 1;
        var revision = RouteRevision.from(deactivated, nextRev, OffsetDateTime.now(clock), actor);
        revisionRepo.save(revision);
    }

    @Override
    public List<RouteRevision> getRevisions(UUID routeId) {
        get(routeId);
        return revisionRepo.findByRouteIdOrderByRevisionNumberDesc(routeId);
    }

    @Override
    public RouteRevision getRevision(UUID routeId, int revisionNumber) {
        get(routeId);
        return revisionRepo.findByRouteIdAndRevisionNumber(routeId, revisionNumber)
                .orElseThrow(() -> new NotFoundException("Route revision not found for route " + routeId + " revision " + revisionNumber));
    }

    @Override
    public RouteDisruption createDisruption(UUID routeId, RouteDisruptionType type, DisruptionSeverity severity,
                                            String description, OffsetDateTime effectiveFrom, OffsetDateTime effectiveUntil,
                                            UUID detourRouteId, String actor) {
        return transaction.execute(() -> createDisruptionAtomically(routeId, type, severity, description,
            effectiveFrom, effectiveUntil, detourRouteId, actor));
    }

    private RouteDisruption createDisruptionAtomically(UUID routeId, RouteDisruptionType type,
            DisruptionSeverity severity, String description, OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveUntil, UUID detourRouteId, String actor) {
        get(routeId);
        if (detourRouteId != null) {
            if (detourRouteId.equals(routeId)) {
                throw new IllegalArgumentException("Detour route cannot be the same as the disrupted route");
            }
            var detour = get(detourRouteId);
            if (!detour.active()) {
                throw new IllegalArgumentException("Detour route must be active");
            }
        }
        var disruption = RouteDisruption.create(routeId, type, severity, description, effectiveFrom, effectiveUntil, detourRouteId, OffsetDateTime.now(clock), actor);
        var saved = disruptionRepo.save(disruption);
        eventPublisher.publish(new RouteDisruptionCreatedEvent(saved.id(), saved.routeId(), saved.disruptionType(), saved.severity(), saved.detourRouteId(), saved.effectiveFrom(), saved.effectiveUntil()));
        operationalExceptions.publish(saved);
        return saved;
    }

    @Override
    public RouteDisruption resolveDisruption(UUID routeId, UUID disruptionId, String actor) {
        get(routeId);
        var disruption = disruptionRepo.findById(disruptionId)
                .orElseThrow(() -> new NotFoundException("Route disruption not found: " + disruptionId));
        if (!disruption.routeId().equals(routeId)) {
            throw new NotFoundException("Disruption " + disruptionId + " does not belong to route " + routeId);
        }
        var resolved = disruption.resolve(OffsetDateTime.now(clock), actor);
        var saved = disruptionRepo.save(resolved);
        eventPublisher.publish(new RouteDisruptionResolvedEvent(saved.id(), saved.routeId(), saved.resolvedAt(), saved.resolvedBy()));
        return saved;
    }

    @Override
    public List<RouteDisruption> getDisruptions(UUID routeId) {
        get(routeId);
        return disruptionRepo.findByRouteId(routeId);
    }

    @Override
    public List<RouteDisruption> getActiveDisruptions() {
        return disruptionRepo.findByStatus(DisruptionStatus.ACTIVE);
    }

    // US-20: Route Optimization
    @Override
    public RouteOptimizationResult optimizeRoute(UUID routeId) {
        var route = get(routeId);
        if (!route.active()) {
            throw new BusinessRuleException("ROUTE_OPTIMIZATION_NOT_APPLICABLE", "Cannot optimize an inactive route: " + routeId);
        }
        if (route.stopLocationIds().size() < 2) {
            throw new BusinessRuleException("ROUTE_OPTIMIZATION_NOT_APPLICABLE",
                    "At least two intermediate stops are required for route optimization");
        }

        validateOptimizationLocations(route);

        return RouteOptimizer.optimize(
                route.id(),
                route.originLocationId(),
                route.destinationLocationId(),
                route.stopLocationIds(),
                route.plannedDistanceKm(),
                route.estimatedDurationMinutes(),
                distancePort::getDistanceKm
        );
    }

    @Override
    public Route applyOptimization(UUID routeId, List<UUID> optimizedStopLocationIds, String actor) {
        return transaction.execute(() -> applyOptimizationAtomically(routeId, optimizedStopLocationIds, actor));
    }

    private Route applyOptimizationAtomically(UUID routeId, List<UUID> optimizedStopLocationIds, String actor) {
        var route = get(routeId);
        if (!route.active()) {
            throw new BusinessRuleException("ROUTE_OPTIMIZATION_NOT_APPLICABLE", "Cannot apply optimization to an inactive route: " + routeId);
        }

        List<UUID> proposedStops = optimizedStopLocationIds == null ? List.of() : optimizedStopLocationIds;
        var existingStops = route.stopLocationIds();

        if (proposedStops.size() != existingStops.size() || !new HashSet<>(proposedStops).equals(new HashSet<>(existingStops))) {
            throw new BusinessRuleException("ROUTE_OPTIMIZATION_INVALID_STOPS",
                    "Optimized stops must be a valid permutation of the existing stops on route: " + routeId);
        }

        var optResult = optimizeRoute(routeId);
        if (!optResult.optimizedStopLocationIds().equals(proposedStops)) {
            throw new BusinessRuleException("ROUTE_OPTIMIZATION_INVALID_STOPS",
                    "The submitted sequence is stale or does not match the current optimization preview");
        }

        var updatedRoute = new Route(
                route.id(),
                route.code(),
                route.name(),
                route.originLocationId(),
                route.destinationLocationId(),
                optResult.optimizedEstimatedDistanceKm(),
                optResult.optimizedEstimatedDurationMinutes(),
                route.active(),
                optResult.optimizedStopLocationIds()
        );

        var saved = repo.save(updatedRoute);
        int nextRev = revisionRepo.findLatestRevisionNumber(routeId) + 1;
        var revision = RouteRevision.from(saved, nextRev, OffsetDateTime.now(clock), actor);
        revisionRepo.save(revision);
        return saved;
    }

    // US-22: Route Performance Analytics
    @Override
    public RoutePerformanceAnalytics getRoutePerformance(UUID routeId, OffsetDateTime from, OffsetDateTime to) {
        var route = get(routeId);
        var effectiveTo = to == null ? OffsetDateTime.now(clock) : to;
        var effectiveFrom = from == null ? effectiveTo.minusDays(DEFAULT_ANALYTICS_DAYS) : from;
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "from cannot be after to");
        }
        if (effectiveFrom.isBefore(effectiveTo.minusDays(MAX_ANALYTICS_DAYS))) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "Route performance range cannot exceed 366 days");
        }
        var trips = performanceTripLookup.findTripsForRoute(routeId, effectiveFrom, effectiveTo);
        return RoutePerformanceCalculator.calculate(route, trips);
    }

    private void validateOptimizationLocations(Route route) {
        var locationIds = new java.util.ArrayList<UUID>();
        locationIds.add(route.originLocationId());
        locationIds.addAll(route.stopLocationIds());
        locationIds.add(route.destinationLocationId());
        for (var locationId : locationIds) {
            var location = distancePort.getLocation(locationId).orElseThrow(() -> new BusinessRuleException(
                    "ROUTE_OPTIMIZATION_DATA_UNAVAILABLE", "Location not found: " + locationId));
            if (!location.hasCoordinates()) {
                throw new BusinessRuleException("ROUTE_OPTIMIZATION_DATA_UNAVAILABLE",
                        "Coordinates are required for location: " + locationId);
            }
        }
    }
}
