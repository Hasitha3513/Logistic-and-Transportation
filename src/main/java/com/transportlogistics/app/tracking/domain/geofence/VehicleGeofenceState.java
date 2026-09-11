package com.transportlogistics.app.tracking.domain.geofence;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record VehicleGeofenceState(UUID tenantId, UUID geofenceId, UUID vehicleId,
                                   long definitionVersion, GeofenceMembership stableState,
                                   GeofenceMembership pendingCandidate, int pendingCount,
                                   UUID pendingPositionId, UUID lastEvaluatedPositionId,
                                   Instant lastEvaluatedSourceTimestamp, long version) {
    public VehicleGeofenceState {
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        Objects.requireNonNull(geofenceId, "Geofence ID is required");
        Objects.requireNonNull(vehicleId, "Vehicle ID is required");
        if (definitionVersion < 0 || version < 0 || pendingCount < 0 || pendingCount > 1) {
            throw new IllegalArgumentException("Geofence state versions and pending count are invalid");
        }
        if (stableState == null && (pendingCandidate != null || pendingCount != 0)) {
            throw new IllegalArgumentException("Uninitialized state cannot have a pending candidate");
        }
        if ((pendingCandidate == null) != (pendingCount == 0)
                || (pendingCandidate == null) != (pendingPositionId == null)) {
            throw new IllegalArgumentException("Pending geofence state must be complete");
        }
    }

    public static VehicleGeofenceState uninitialized(UUID tenantId, UUID geofenceId,
                                                      UUID vehicleId, long definitionVersion) {
        return new VehicleGeofenceState(tenantId, geofenceId, vehicleId, definitionVersion,
                null, null, 0, null, null, null, 0);
    }

    public static VehicleGeofenceState initializeOutside(
            UUID tenantId, UUID geofenceId, UUID vehicleId, long definitionVersion,
            GeofencePosition position, Instant evaluatedAt) {
        if (!tenantId.equals(position.tenantId()) || !vehicleId.equals(position.vehicleId())
                || !GeofencePositionEligibility.isEligible(position, evaluatedAt)) {
            throw new GeofenceRuleException("GEOFENCE_SCOPE_INVALID",
                    "Outside initialization requires an eligible position in the same scope");
        }
        return new VehicleGeofenceState(tenantId, geofenceId, vehicleId, definitionVersion,
                GeofenceMembership.OUTSIDE, null, 0, null, position.positionId(),
                position.sourceTimestamp(), 1);
    }

    public GeofenceEvaluationResult observe(Geofence geofence, GeofencePosition position,
                                            Instant evaluatedAt) {
        requireSameScope(geofence, position);
        if (!GeofencePositionEligibility.isEligible(position, evaluatedAt)
                || !isAfterLast(position)) {
            return GeofenceEvaluationResult.withoutTransition(this);
        }
        GeofenceMembership observed = geofence.polygon().contains(position.coordinate())
                ? GeofenceMembership.INSIDE : GeofenceMembership.OUTSIDE;
        if (stableState == null || definitionVersion != geofence.version()) {
            return GeofenceEvaluationResult.withoutTransition(new VehicleGeofenceState(
                    tenantId, geofenceId, vehicleId, geofence.version(), observed,
                    null, 0, null, position.positionId(), position.sourceTimestamp(), version + 1));
        }
        if (observed == stableState) {
            return GeofenceEvaluationResult.withoutTransition(next(stableState, null, 0,
                    null, position));
        }
        if (pendingCandidate != observed) {
            return GeofenceEvaluationResult.withoutTransition(next(stableState, observed, 1,
                    position.positionId(), position));
        }
        if (position.positionId().equals(pendingPositionId)) {
            return GeofenceEvaluationResult.withoutTransition(this);
        }
        VehicleGeofenceState changed = next(observed, null, 0, null, position);
        GeofenceTransitionType transitionType = transitionType(geofence.type(), observed);
        GeofenceSeverity severity = transitionType == GeofenceTransitionType.UNAUTHORIZED_ZONE_ENTERED
                ? GeofenceSeverity.HIGH : GeofenceSeverity.NORMAL;
        UUID transitionId = GeofenceTransitionIdentity.create(tenantId, geofenceId,
                definitionVersion, vehicleId, stableState, observed, position.positionId());
        GeofenceTransition transition = new GeofenceTransition(transitionId, tenantId,
                geofenceId, vehicleId, geofence.locationId(), geofence.type(), transitionType,
                severity, position.sourceTimestamp(), definitionVersion, position.positionId(),
                stableState, observed);
        return new GeofenceEvaluationResult(changed, Optional.of(transition));
    }

    private boolean isAfterLast(GeofencePosition position) {
        if (lastEvaluatedSourceTimestamp == null) {
            return true;
        }
        int timeComparison = position.sourceTimestamp().compareTo(lastEvaluatedSourceTimestamp);
        if (timeComparison != 0) {
            return timeComparison > 0;
        }
        return lastEvaluatedPositionId == null
                || position.positionId().compareTo(lastEvaluatedPositionId) > 0;
    }

    private VehicleGeofenceState next(GeofenceMembership stable,
                                      GeofenceMembership pending, int count,
                                      UUID candidatePositionId, GeofencePosition position) {
        return new VehicleGeofenceState(tenantId, geofenceId, vehicleId, definitionVersion,
                stable, pending, count, candidatePositionId, position.positionId(),
                position.sourceTimestamp(), version + 1);
    }

    private void requireSameScope(Geofence geofence, GeofencePosition position) {
        if (!tenantId.equals(geofence.tenantId()) || !tenantId.equals(position.tenantId())
                || !geofenceId.equals(geofence.id()) || !vehicleId.equals(position.vehicleId())) {
            throw new GeofenceRuleException("GEOFENCE_SCOPE_INVALID",
                    "Geofence evaluation facts must share Tenant, geofence and Vehicle scope");
        }
    }

    private static GeofenceTransitionType transitionType(GeofenceType type,
                                                          GeofenceMembership observed) {
        if (observed == GeofenceMembership.OUTSIDE) {
            return GeofenceTransitionType.EXITED;
        }
        return type == GeofenceType.UNAUTHORIZED_ZONE
                ? GeofenceTransitionType.UNAUTHORIZED_ZONE_ENTERED
                : GeofenceTransitionType.ENTERED;
    }
}
