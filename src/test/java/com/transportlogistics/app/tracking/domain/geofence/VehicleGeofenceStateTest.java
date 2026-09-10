package com.transportlogistics.app.tracking.domain.geofence;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehicleGeofenceStateTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID VEHICLE = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Test
    void firstInsideAndOutsideObservationsInitializeSilently() {
        Geofence geofence = geofence(GeofenceType.DEPOT);

        GeofenceEvaluationResult inside = state(geofence).observe(geofence,
                position(UUID.randomUUID(), NOW.minusSeconds(1), 2, 2), NOW);
        GeofenceEvaluationResult outside = state(geofence).observe(geofence,
                position(UUID.randomUUID(), NOW.minusSeconds(1), 20, 20), NOW);

        assertThat(inside.state().stableState()).isEqualTo(GeofenceMembership.INSIDE);
        assertThat(outside.state().stableState()).isEqualTo(GeofenceMembership.OUTSIDE);
        assertThat(inside.transition()).isEmpty();
        assertThat(outside.transition()).isEmpty();
    }

    @Test
    void twoDistinctInsideObservationsProduceOneEnteredTransition() {
        Geofence geofence = geofence(GeofenceType.DEPOT);
        VehicleGeofenceState outside = initialize(geofence, 20, 20);
        GeofenceEvaluationResult pending = outside.observe(geofence,
                position(UUID.randomUUID(), NOW.plusSeconds(1), 2, 2), NOW.plusSeconds(1));
        GeofenceEvaluationResult entered = pending.state().observe(geofence,
                position(UUID.randomUUID(), NOW.plusSeconds(2), 3, 2), NOW.plusSeconds(2));

        assertThat(pending.transition()).isEmpty();
        assertThat(pending.state().pendingCandidate()).isEqualTo(GeofenceMembership.INSIDE);
        assertThat(entered.transition()).get().extracting(GeofenceTransition::transitionType)
                .isEqualTo(GeofenceTransitionType.ENTERED);
        assertThat(entered.state().stableState()).isEqualTo(GeofenceMembership.INSIDE);
    }

    @Test
    void twoDistinctOutsideObservationsProduceExit() {
        Geofence geofence = geofence(GeofenceType.DEPOT);
        VehicleGeofenceState inside = initialize(geofence, 2, 2);
        GeofenceEvaluationResult pending = inside.observe(geofence,
                position(UUID.randomUUID(), NOW.plusSeconds(1), 20, 20), NOW.plusSeconds(1));
        GeofenceEvaluationResult exited = pending.state().observe(geofence,
                position(UUID.randomUUID(), NOW.plusSeconds(2), 21, 20), NOW.plusSeconds(2));

        assertThat(exited.transition()).get().extracting(GeofenceTransition::transitionType)
                .isEqualTo(GeofenceTransitionType.EXITED);
        assertThat(exited.transition()).get().extracting(GeofenceTransition::severity)
                .isEqualTo(GeofenceSeverity.NORMAL);
    }

    @Test
    void samePositionCannotSatisfyConfirmationAndStableObservationResetsPending() {
        Geofence geofence = geofence(GeofenceType.DEPOT);
        VehicleGeofenceState outside = initialize(geofence, 20, 20);
        UUID candidateId = UUID.randomUUID();
        GeofencePosition candidate = position(candidateId, NOW.plusSeconds(1), 2, 2);
        VehicleGeofenceState pending = outside.observe(geofence, candidate,
                NOW.plusSeconds(1)).state();

        assertThat(pending.observe(geofence, candidate, NOW.plusSeconds(1)).transition()).isEmpty();
        VehicleGeofenceState reset = pending.observe(geofence,
                position(UUID.randomUUID(), NOW.plusSeconds(2), 20, 20),
                NOW.plusSeconds(2)).state();
        assertThat(reset.pendingCandidate()).isNull();
        assertThat(reset.pendingCount()).isZero();
    }

    @Test
    void unauthorizedEntryIsHighAndMandatoryWhileExitIsNormal() {
        Geofence geofence = geofence(GeofenceType.UNAUTHORIZED_ZONE);
        VehicleGeofenceState outside = initialize(geofence, 20, 20);
        GeofenceEvaluationResult pending = outside.observe(geofence,
                position(UUID.randomUUID(), NOW.plusSeconds(1), 2, 2), NOW.plusSeconds(1));
        GeofenceTransition entered = pending.state().observe(geofence,
                position(UUID.randomUUID(), NOW.plusSeconds(2), 3, 2),
                NOW.plusSeconds(2)).transition().orElseThrow();

        assertThat(entered.transitionType()).isEqualTo(GeofenceTransitionType.UNAUTHORIZED_ZONE_ENTERED);
        assertThat(entered.severity()).isEqualTo(GeofenceSeverity.HIGH);
        assertThat(entered.alertRequired(geofence.alertPolicy())).isTrue();
    }

    @Test
    void delayedAndEqualTimestampLowerIdentityCannotRewindState() {
        Geofence geofence = geofence(GeofenceType.DEPOT);
        UUID lastId = new UUID(0, 1);
        VehicleGeofenceState current = new VehicleGeofenceState(TENANT, geofence.id(), VEHICLE,
                geofence.version(), GeofenceMembership.OUTSIDE, null, 0, null, lastId, NOW, 1);

        assertThat(current.observe(geofence,
                position(UUID.randomUUID(), NOW.minusSeconds(1), 2, 2), NOW).state()).isSameAs(current);
        assertThat(current.observe(geofence,
                position(new UUID(0, 0), NOW, 2, 2), NOW).state()).isSameAs(current);
    }

    @Test
    void equalTimestampHigherIdentityWinsDeterministically() {
        Geofence geofence = geofence(GeofenceType.DEPOT);
        VehicleGeofenceState current = new VehicleGeofenceState(TENANT, geofence.id(), VEHICLE,
                geofence.version(), GeofenceMembership.OUTSIDE, null, 0, null,
                new UUID(0, 0), NOW, 1);

        VehicleGeofenceState next = current.observe(geofence,
                position(new UUID(0, 1), NOW, 2, 2), NOW).state();
        assertThat(next.pendingCandidate()).isEqualTo(GeofenceMembership.INSIDE);
    }

    @Test
    void twoGeofencesEvaluateSameVehicleIndependently() {
        Geofence first = geofence(GeofenceType.DEPOT);
        Geofence second = geofence(GeofenceType.CUSTOMER_SITE);
        GeofencePosition position = position(UUID.randomUUID(), NOW, 2, 2);

        assertThat(state(first).observe(first, position, NOW).state().stableState())
                .isEqualTo(GeofenceMembership.INSIDE);
        assertThat(state(second).observe(second, position, NOW).state().stableState())
                .isEqualTo(GeofenceMembership.INSIDE);
    }

    private static VehicleGeofenceState initialize(Geofence geofence, double longitude,
                                                    double latitude) {
        return state(geofence).observe(geofence,
                position(UUID.randomUUID(), NOW.minusSeconds(10), longitude, latitude), NOW).state();
    }

    private static VehicleGeofenceState state(Geofence geofence) {
        return VehicleGeofenceState.uninitialized(TENANT, geofence.id(), VEHICLE, geofence.version());
    }

    private static Geofence geofence(GeofenceType type) {
        UUID locationId = type == GeofenceType.UNAUTHORIZED_ZONE ? null : UUID.randomUUID();
        GeofenceAlertPolicy policy = type == GeofenceType.UNAUTHORIZED_ZONE
                ? GeofenceAlertPolicy.unauthorizedZone() : new GeofenceAlertPolicy(true, true);
        Geofence geofence = Geofence.draft(UUID.randomUUID(), TENANT, "Fence", type,
                GeofencePolygon.of(List.of(new Wgs84Coordinate(0, 0),
                        new Wgs84Coordinate(10, 0), new Wgs84Coordinate(0, 10))),
                locationId, policy, NOW.minusSeconds(60), ACTOR);
        geofence.activate(NOW.minusSeconds(59), ACTOR);
        return geofence;
    }

    private static GeofencePosition position(UUID id, Instant sourceTimestamp,
                                             double longitude, double latitude) {
        return new GeofencePosition(TENANT, id, VEHICLE, sourceTimestamp,
                new Wgs84Coordinate(longitude, latitude), Trust.TRUSTED, Ordering.IN_ORDER, false);
    }
}
