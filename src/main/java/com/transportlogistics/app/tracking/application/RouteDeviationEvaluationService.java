package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.tracking.domain.routedeviation.DistanceMeters;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationAvailability;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEvaluationResult;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEvaluator;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationPosition;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDistanceCalculator;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteVersion;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationAssignmentLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEvaluationTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEventPublisherPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationGeometryLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationRuleRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationStateRepositoryPort;
import java.time.Clock;
import java.util.Objects;

public final class RouteDeviationEvaluationService implements RouteDeviationEvaluationUseCase {
    private final RouteDeviationAssignmentLookupPort assignments;
    private final RouteDeviationGeometryLookupPort geometries;
    private final RouteDeviationRuleRepositoryPort rules;
    private final RouteDeviationStateRepositoryPort states;
    private final RouteDeviationEpisodeRepositoryPort episodes;
    private final RouteDeviationEvaluationTransactionPort transactions;
    private final RouteDeviationEventPublisherPort events;
    private final Clock clock;

    public RouteDeviationEvaluationService(RouteDeviationAssignmentLookupPort assignments,
            RouteDeviationGeometryLookupPort geometries, RouteDeviationRuleRepositoryPort rules,
            RouteDeviationStateRepositoryPort states, RouteDeviationEpisodeRepositoryPort episodes,
            RouteDeviationEvaluationTransactionPort transactions, Clock clock) {
        this(assignments, geometries, rules, states, episodes, transactions, clock, new NoEvents());
    }

    public RouteDeviationEvaluationService(RouteDeviationAssignmentLookupPort assignments,
            RouteDeviationGeometryLookupPort geometries, RouteDeviationRuleRepositoryPort rules,
            RouteDeviationStateRepositoryPort states, RouteDeviationEpisodeRepositoryPort episodes,
            RouteDeviationEvaluationTransactionPort transactions, Clock clock,
            RouteDeviationEventPublisherPort events) {
        this.assignments = assignments;
        this.geometries = geometries;
        this.rules = rules;
        this.states = states;
        this.episodes = episodes;
        this.transactions = transactions;
        this.clock = clock;
        this.events = events;
    }

    @Override
    public void evaluate(RouteDeviationPosition position) {
        evaluateResult(position);
    }

    public ProcessingResult evaluateResult(RouteDeviationPosition position) {
        Objects.requireNonNull(position, "Position is required");
        RouteDeviationAvailability eligibility = eligibility(position);
        if (position.tenantId() == null || position.vehicleId() == null) {
            return new ProcessingResult(eligibility, null);
        }
        if (eligibility != RouteDeviationAvailability.AVAILABLE) {
            return unavailable(position, eligibility);
        }
        RouteDeviationAssignmentLookupPort.Assignment assignment;
        try {
            assignment = assignments.findAt(position.tenantId(), position.vehicleId(),
                    position.sourceTimestamp()).orElse(null);
        } catch (RuntimeException exception) {
            return unavailable(position, RouteDeviationAvailability.PROVIDER_UNAVAILABLE);
        }
        if (assignment == null) return unavailable(position, RouteDeviationAvailability.NO_TRIP);
        if (assignment.routeId() == null) {
            return unavailable(position, RouteDeviationAvailability.NO_ASSIGNED_ROUTE);
        }
        if (assignment.routeVersion() == null) {
            return unavailable(position, RouteDeviationAvailability.NO_ROUTE_REVISION);
        }
        RouteVersion routeVersion;
        try {
            routeVersion = new RouteVersion(assignment.routeVersion());
        } catch (RuntimeException exception) {
            return unavailable(position, RouteDeviationAvailability.NO_ROUTE_REVISION);
        }
        var geometry = geometry(position, assignment, routeVersion);
        if (geometry.value() == null) return unavailable(position, geometry.reason());
        var rule = rule(position, assignment, routeVersion);
        if (rule.value() == null) return unavailable(position, rule.reason());
        DistanceMeters distance = RouteDistanceCalculator.minimumDistance(position.point(), geometry.value());
        return transactions.execute(() -> transition(position, assignment, routeVersion,
                rule.value(), distance));
    }

    private ProcessingResult transition(RouteDeviationPosition position,
            RouteDeviationAssignmentLookupPort.Assignment assignment, RouteVersion routeVersion,
            com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule rule,
            DistanceMeters distance) {
        VehicleRouteDeviationState state = states.lockAndFind(position.tenantId(), position.vehicleId())
                .orElseGet(() -> VehicleRouteDeviationState.unknown(
                        position.tenantId(), position.vehicleId()));
        RouteDeviationEpisode active = episodes.findOpen(position.tenantId(), position.vehicleId()).orElse(null);
        boolean contextChanged = active != null && (!Objects.equals(active.tripId(), assignment.tripId())
                || !active.routeId().equals(assignment.routeId())
                || !active.routeVersion().equals(routeVersion));
        if (contextChanged && state.newer(position)) {
            episodes.save(active.close(position.sourceTimestamp(),
                    RouteDeviationEpisode.TerminalOutcome.SUPERSEDED));
            state = VehicleRouteDeviationState.unknown(position.tenantId(), position.vehicleId());
            active = null;
        }
        RouteDeviationEvaluationResult result = RouteDeviationEvaluator.evaluate(state, active,
                position, clock.instant(), assignment.tripId(), assignment.driverId(),
                assignment.routeId(), routeVersion, rule, distance);
        if (result.state() != state) states.save(result.state());
        if (result.episode() != null) episodes.save(result.episode());
        if (result.detectionPublication()) publishDetection(result.episode());
        if (result.escalationPublication()) publishDistanceEscalation(
                result.episode(), result.state().lastSourceTimestamp());
        return new ProcessingResult(RouteDeviationAvailability.AVAILABLE, result);
    }

    private void publishDetection(RouteDeviationEpisode episode) {
        events.publishDetected(episode.tenantId(), new RouteDeviationEventPublisherPort.Detected(
                episode.id(), episode.vehicleId(), episode.tripId(), episode.driverId(), episode.routeId(),
                episode.routeVersion().value(), episode.severity().name(), episode.maximumDistance().value(),
                episode.effectiveTolerance().value(), episode.confirmationSourceTimestamp(),
                episode.severity() == RouteDeviationEpisode.Severity.HIGH));
    }

    private void publishDistanceEscalation(RouteDeviationEpisode episode, java.time.Instant sourceTimestamp) {
        events.publishEscalated(episode.tenantId(), new RouteDeviationEventPublisherPort.Escalated(
                escalationId(episode, "DISTANCE_HIGH"), episode.id(), episode.vehicleId(), episode.tripId(),
                episode.driverId(), episode.routeId(), episode.routeVersion().value(), episode.severity().name(),
                episode.maximumDistance().value(), episode.effectiveTolerance().value(),
                sourceTimestamp, true, "DISTANCE_HIGH"));
    }

    private static java.util.UUID escalationId(RouteDeviationEpisode episode, String reason) {
        return java.util.UUID.nameUUIDFromBytes((episode.tenantId() + "|" + episode.id() + "|" + reason)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Lookup<com.transportlogistics.app.tracking.domain.routedeviation.RoutePolyline> geometry(
            RouteDeviationPosition position, RouteDeviationAssignmentLookupPort.Assignment assignment,
            RouteVersion routeVersion) {
        try {
            return geometries.find(position.tenantId(), assignment.routeId(), routeVersion)
                    .map(value -> new Lookup<>(value, RouteDeviationAvailability.AVAILABLE))
                    .orElseGet(() -> new Lookup<>(null, RouteDeviationAvailability.GEOMETRY_UNAVAILABLE));
        } catch (IllegalArgumentException exception) {
            return new Lookup<>(null, RouteDeviationAvailability.MALFORMED_GEOMETRY);
        } catch (RuntimeException exception) {
            return new Lookup<>(null, RouteDeviationAvailability.PROVIDER_UNAVAILABLE);
        }
    }

    private Lookup<com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule> rule(
            RouteDeviationPosition position, RouteDeviationAssignmentLookupPort.Assignment assignment,
            RouteVersion routeVersion) {
        try {
            return rules.findActive(position.tenantId(), assignment.routeId(), routeVersion)
                    .map(value -> new Lookup<>(value, RouteDeviationAvailability.AVAILABLE))
                    .orElseGet(() -> new Lookup<>(null, RouteDeviationAvailability.RULE_UNAVAILABLE));
        } catch (RuntimeException exception) {
            return new Lookup<>(null, RouteDeviationAvailability.CONFIGURATION_UNAVAILABLE);
        }
    }

    private ProcessingResult unavailable(RouteDeviationPosition position,
            RouteDeviationAvailability reason) {
        return transactions.execute(() -> {
            VehicleRouteDeviationState state = states.lockAndFind(position.tenantId(), position.vehicleId())
                    .orElseGet(() -> VehicleRouteDeviationState.unknown(
                            position.tenantId(), position.vehicleId()));
            VehicleRouteDeviationState unavailable = state.unavailable(reason);
            states.save(unavailable);
            return new ProcessingResult(reason, null);
        });
    }

    private RouteDeviationAvailability eligibility(RouteDeviationPosition position) {
        if (position.positionId() == null || position.tenantId() == null
                || position.vehicleId() == null || position.sourceTimestamp() == null
                || position.point() == null) return RouteDeviationAvailability.INVALID_COORDINATE;
        if (position.accuracy() == null) return RouteDeviationAvailability.ACCURACY_UNKNOWN;
        if (position.accuracy().value().compareTo(java.math.BigDecimal.valueOf(1_000)) > 0) {
            return RouteDeviationAvailability.EXCESSIVE_ACCURACY;
        }
        if (position.trust() != RouteDeviationPosition.Trust.TRUSTED) {
            return RouteDeviationAvailability.UNTRUSTED_POSITION;
        }
        if (position.ordering() != RouteDeviationPosition.Ordering.IN_ORDER) {
            return RouteDeviationAvailability.OUT_OF_ORDER_POSITION;
        }
        if (!position.accepted() || position.duplicate() || !position.vehicleAssociated()) {
            return RouteDeviationAvailability.POSITION_INELIGIBLE;
        }
        if (position.sourceTimestamp().isAfter(clock.instant())
                || java.time.Duration.between(position.sourceTimestamp(), clock.instant())
                .compareTo(java.time.Duration.ofMinutes(5)) > 0) {
            return RouteDeviationAvailability.STALE_POSITION;
        }
        return RouteDeviationAvailability.AVAILABLE;
    }

    public record ProcessingResult(RouteDeviationAvailability availability,
            RouteDeviationEvaluationResult evaluation) {
    }

    private record Lookup<T>(T value, RouteDeviationAvailability reason) {
    }

    private static final class NoEvents implements RouteDeviationEventPublisherPort {
        @Override public void publishDetected(java.util.UUID tenantId, Detected event) { }
        @Override public void publishEscalated(java.util.UUID tenantId, Escalated event) { }
    }
}
