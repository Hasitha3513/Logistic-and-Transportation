package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceEvaluationResult;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceLifecycle;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePosition;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePositionEligibility;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransition;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransitionType;
import com.transportlogistics.app.tracking.domain.geofence.VehicleGeofenceState;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceEvaluationTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionPublisherPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleGeofenceStateRepositoryPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class GeofenceEvaluationService implements GeofenceEvaluationUseCase {
    static final int MAXIMUM_ACTIVE_GEOFENCES = 500;
    private final GeofenceRepositoryPort geofences;
    private final VehicleGeofenceStateRepositoryPort states;
    private final GeofenceTransitionRepositoryPort transitions;
    private final GeofenceTransitionPublisherPort publisher;
    private final GeofenceEvaluationTransactionPort transaction;

    public GeofenceEvaluationService(
            GeofenceRepositoryPort geofences, VehicleGeofenceStateRepositoryPort states,
            GeofenceTransitionRepositoryPort transitions,
            GeofenceTransitionPublisherPort publisher,
            GeofenceEvaluationTransactionPort transaction) {
        this.geofences = geofences;
        this.states = states;
        this.transitions = transitions;
        this.publisher = publisher;
        this.transaction = transaction;
    }

    @Override
    public List<GeofenceEvaluationResult> evaluate(
            GeofencePosition position, Instant evaluatedAt) {
        if (!GeofencePositionEligibility.isEligible(position, evaluatedAt)) {
            return List.of();
        }
        long activeCount = transaction.execute(
                () -> geofences.countActiveForUpdate(position.tenantId()));
        if (activeCount > MAXIMUM_ACTIVE_GEOFENCES) {
            throw new BusinessRuleException("GEOFENCE_ACTIVE_LIMIT_EXCEEDED",
                    "Tenant has more than 500 active geofences");
        }
        geofences.findActiveOutsideWithoutState(position.tenantId(), position.vehicleId(),
                position.coordinate().longitude(), position.coordinate().latitude(),
                MAXIMUM_ACTIVE_GEOFENCES).forEach(reference -> transaction.execute(() -> {
                    initializeOutside(reference, position, evaluatedAt);
                    return Boolean.TRUE;
                }));
        List<Geofence> candidates = geofences.findActiveCandidates(
                position.tenantId(), position.vehicleId(), position.coordinate().longitude(),
                position.coordinate().latitude(), MAXIMUM_ACTIVE_GEOFENCES);
        List<GeofenceEvaluationResult> results = new ArrayList<>(candidates.size());
        for (Geofence candidate : candidates) {
            GeofenceEvaluationResult result = transaction.execute(
                    () -> evaluateCandidate(candidate, position, evaluatedAt));
            results.add(result);
        }
        return List.copyOf(results);
    }

    private void initializeOutside(
            GeofenceRepositoryPort.ActiveGeofenceReference reference,
            GeofencePosition position, Instant evaluatedAt) {
        Geofence current = geofences.findForUpdate(
                position.tenantId(), reference.geofenceId()).orElse(null);
        if (current == null || current.lifecycle() != GeofenceLifecycle.ACTIVE
                || current.version() != reference.definitionVersion()) {
            return;
        }
        if (states.findForUpdate(position.tenantId(), current.id(), position.vehicleId()).isEmpty()) {
            states.save(VehicleGeofenceState.initializeOutside(position.tenantId(), current.id(),
                    position.vehicleId(), current.version(), position, evaluatedAt), 0);
        }
    }

    private GeofenceEvaluationResult evaluateCandidate(
            Geofence expected, GeofencePosition position, Instant evaluatedAt) {
        Geofence current = geofences.findForUpdate(position.tenantId(), expected.id()).orElse(null);
        if (current == null || current.lifecycle() != GeofenceLifecycle.ACTIVE
                || current.version() != expected.version()) {
            return GeofenceEvaluationResult.withoutTransition(
                    VehicleGeofenceState.uninitialized(position.tenantId(), expected.id(),
                            position.vehicleId(), expected.version()));
        }
        VehicleGeofenceState state = states.findForUpdate(
                position.tenantId(), current.id(), position.vehicleId()).orElseGet(() ->
                    VehicleGeofenceState.uninitialized(position.tenantId(), current.id(),
                            position.vehicleId(), current.version()));
        GeofenceEvaluationResult result = state.observe(current, position, evaluatedAt);
        if (!result.state().equals(state)) {
            states.save(result.state(), state.version());
            result.transition().ifPresent(transition -> {
                transitions.append(transition);
                if (publicationEnabled(current, result)) {
                    publisher.publish(toEvent(transition));
                }
            });
        }
        return result;
    }

    private static boolean publicationEnabled(
            Geofence geofence, GeofenceEvaluationResult result) {
        GeofenceTransitionType type = result.transition().orElseThrow().transitionType();
        return type == GeofenceTransitionType.UNAUTHORIZED_ZONE_ENTERED
                || type == GeofenceTransitionType.ENTERED && geofence.alertPolicy().alertOnEntry()
                || type == GeofenceTransitionType.EXITED && geofence.alertPolicy().alertOnExit();
    }

    private static GeofenceTransitionPublisherPort.VehicleGeofenceTransitionedV1 toEvent(
            GeofenceTransition transition) {
        return new GeofenceTransitionPublisherPort.VehicleGeofenceTransitionedV1(
                transition.transitionId(), transition.tenantId(), transition.geofenceId(),
                transition.vehicleId(), transition.locationId(), transition.geofenceType(),
                transition.transitionType(), transition.severity(), transition.sourceTimestamp(),
                transition.definitionVersion());
    }
}
