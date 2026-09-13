package com.transportlogistics.app.tracking.domain.routedeviation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record VehicleRouteDeviationState(
        UUID tenantId, UUID vehicleId, State state, RouteDeviationAvailability availability,
        UUID currentTripId, UUID routeId, RouteVersion routeVersion, UUID ruleId, long ruleVersion,
        DistanceMeters configuredTolerance, DistanceMeters effectiveTolerance, Candidate candidate,
        UUID activeEpisodeId, Instant lastSourceTimestamp, UUID lastPositionId) {

    public enum State { UNKNOWN, ON_ROUTE, DEVIATING }

    public record Candidate(UUID positionId, Instant sourceTimestamp, UUID tripId, UUID driverId,
            UUID routeId, RouteVersion routeVersion, UUID ruleId, long ruleVersion,
            DistanceMeters configuredTolerance, DistanceMeters effectiveTolerance,
            DistanceMeters distance, RoutePoint point, DistanceMeters accuracy) { }

    public VehicleRouteDeviationState {
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        Objects.requireNonNull(vehicleId, "Vehicle ID is required");
        Objects.requireNonNull(state, "State is required");
        Objects.requireNonNull(availability, "Availability is required");
    }

    public static VehicleRouteDeviationState unknown(UUID tenantId, UUID vehicleId) {
        return new VehicleRouteDeviationState(tenantId, vehicleId, State.UNKNOWN,
                RouteDeviationAvailability.POSITION_INELIGIBLE, null, null, null, null, 0,
                null, null, null, null, null, null);
    }

    public VehicleRouteDeviationState unavailable(RouteDeviationAvailability reason) {
        if (reason == RouteDeviationAvailability.AVAILABLE) {
            throw new IllegalArgumentException("Unavailable state requires a reason");
        }
        return new VehicleRouteDeviationState(tenantId, vehicleId, state, reason, currentTripId,
                routeId, routeVersion, ruleId, ruleVersion, configuredTolerance,
                effectiveTolerance, candidate, activeEpisodeId, lastSourceTimestamp, lastPositionId);
    }

    public VehicleRouteDeviationState baselineInside(RouteDeviationPosition position) {
        if (ruleId == null) {
            throw new IllegalStateException("Evaluation context is required");
        }
        return advanced(position, State.ON_ROUTE, null, null);
    }

    public VehicleRouteDeviationState baselineInside(RouteDeviationPosition position, UUID tripId,
            UUID nextRouteId, RouteVersion nextRouteVersion, RouteDeviationRule rule,
            DistanceMeters effective) {
        return advanced(position, State.ON_ROUTE, tripId, nextRouteId, nextRouteVersion,
                rule, effective, null, null);
    }

    public VehicleRouteDeviationState candidate(RouteDeviationPosition position, UUID tripId,
            UUID driverId, UUID nextRouteId, RouteVersion nextRouteVersion, RouteDeviationRule rule,
            DistanceMeters effective, DistanceMeters distance) {
        Candidate next = new Candidate(position.positionId(), position.sourceTimestamp(), tripId,
                driverId, nextRouteId, nextRouteVersion, rule.id(), rule.ruleVersion(),
                rule.configuredTolerance(), effective, distance, position.point(), position.accuracy());
        return advanced(position, state, tripId, nextRouteId, nextRouteVersion, rule,
                effective, next, activeEpisodeId);
    }

    public boolean confirms(RouteDeviationPosition position, UUID tripId, UUID nextRouteId,
            RouteVersion nextRouteVersion, RouteDeviationRule rule) {
        return candidate != null && !candidate.positionId().equals(position.positionId())
                && Objects.equals(candidate.tripId(), tripId)
                && candidate.routeId().equals(nextRouteId)
                && candidate.routeVersion().equals(nextRouteVersion)
                && candidate.ruleId().equals(rule.id())
                && candidate.ruleVersion() == rule.ruleVersion();
    }

    public VehicleRouteDeviationState confirmed(RouteDeviationPosition position, UUID episodeId) {
        return advanced(position, State.DEVIATING, null, episodeId);
    }

    public VehicleRouteDeviationState inside(RouteDeviationPosition position) {
        return advanced(position, State.ON_ROUTE, null, null);
    }

    public VehicleRouteDeviationState reset(RouteDeviationPosition position) {
        return advanced(position, state, null, activeEpisodeId);
    }

    public boolean newer(RouteDeviationPosition position) {
        return lastSourceTimestamp == null || position.sourceTimestamp().isAfter(lastSourceTimestamp)
                || position.sourceTimestamp().equals(lastSourceTimestamp)
                && position.positionId().compareTo(lastPositionId) > 0;
    }

    private VehicleRouteDeviationState advanced(RouteDeviationPosition position, State nextState,
            Candidate nextCandidate, UUID episodeId) {
        if (!newer(position)) return this;
        return new VehicleRouteDeviationState(tenantId, vehicleId, nextState,
                RouteDeviationAvailability.AVAILABLE, currentTripId, routeId, routeVersion, ruleId,
                ruleVersion, configuredTolerance, effectiveTolerance, nextCandidate, episodeId,
                position.sourceTimestamp(), position.positionId());
    }

    private VehicleRouteDeviationState advanced(RouteDeviationPosition position, State nextState,
            UUID tripId, UUID nextRouteId, RouteVersion nextRouteVersion, RouteDeviationRule rule,
            DistanceMeters effective, Candidate nextCandidate, UUID episodeId) {
        if (!newer(position)) return this;
        return new VehicleRouteDeviationState(tenantId, vehicleId, nextState,
                RouteDeviationAvailability.AVAILABLE, tripId, nextRouteId, nextRouteVersion,
                rule.id(), rule.ruleVersion(), rule.configuredTolerance(), effective, nextCandidate,
                episodeId, position.sourceTimestamp(), position.positionId());
    }
}
