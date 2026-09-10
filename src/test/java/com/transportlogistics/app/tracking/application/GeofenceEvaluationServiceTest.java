package com.transportlogistics.app.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceAlertPolicy;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePolygon;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePosition;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransition;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransitionType;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.domain.geofence.VehicleGeofenceState;
import com.transportlogistics.app.tracking.domain.geofence.Wgs84Coordinate;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceEvaluationTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionPublisherPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleGeofenceStateRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeofenceEvaluationServiceTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID VEHICLE = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");
    private final GeofenceRepositoryPort geofences = mock(GeofenceRepositoryPort.class);
    private final VehicleGeofenceStateRepositoryPort states =
            mock(VehicleGeofenceStateRepositoryPort.class);
    private final GeofenceTransitionRepositoryPort transitions =
            mock(GeofenceTransitionRepositoryPort.class);
    private final GeofenceTransitionPublisherPort publisher =
            mock(GeofenceTransitionPublisherPort.class);
    private final GeofenceEvaluationTransactionPort transaction =
            mock(GeofenceEvaluationTransactionPort.class);
    private final AtomicReference<VehicleGeofenceState> stored = new AtomicReference<>();
    private Geofence geofence;
    private GeofenceEvaluationService service;

    @BeforeEach
    void setUp() {
        geofence = activeGeofence();
        when(transaction.execute(any())).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(0)).get());
        when(geofences.countActiveForUpdate(TENANT)).thenReturn(1L);
        when(geofences.findActiveCandidates(any(), any(), anyDouble(), anyDouble(), anyInt()))
                .thenAnswer(ignored -> List.of(geofence));
        when(geofences.findActiveOutsideWithoutState(
                any(), any(), anyDouble(), anyDouble(), anyInt())).thenReturn(List.of());
        when(geofences.findForUpdate(any(), any())).thenAnswer(ignored -> Optional.of(geofence));
        when(states.findForUpdate(any(), any(), any()))
                .thenAnswer(ignored -> Optional.ofNullable(stored.get()));
        when(states.save(any(), anyLong())).thenAnswer(invocation -> {
            VehicleGeofenceState state = invocation.getArgument(0);
            stored.set(state);
            return state;
        });
        when(transitions.append(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new GeofenceEvaluationService(
                geofences, states, transitions, publisher, transaction);
    }

    @Test
    void initialObservationIsSilentAndTwoDistinctPositionsConfirmEntry() {
        service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(3), 20, 20), NOW);
        service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(2), 2, 2), NOW);
        var confirmed = service.evaluate(
                position(UUID.randomUUID(), NOW.minusSeconds(1), 2, 2), NOW);

        assertThat(confirmed).singleElement().satisfies(result ->
                assertThat(result.transition()).get().extracting(GeofenceTransition::transitionType)
                        .isEqualTo(GeofenceTransitionType.UNAUTHORIZED_ZONE_ENTERED));
        verify(transitions).append(any());
        verify(publisher).publish(any());
    }

    @Test
    void stalePositionDoesNotQueryOrMutateGeofences() {
        assertThat(service.evaluate(position(
                UUID.randomUUID(), NOW.minusSeconds(301), 2, 2), NOW)).isEmpty();

        verify(geofences, never()).findActiveCandidates(any(), any(),
                any(Double.class), any(Double.class), any(Integer.class));
        verify(states, never()).save(any(), anyLong());
    }

    @Test
    void corruptActiveCountFailsClosedBeforeCandidateLoad() {
        when(geofences.countActiveForUpdate(TENANT)).thenReturn(501L);

        assertThatThrownBy(() -> service.evaluate(
                position(UUID.randomUUID(), NOW, 2, 2), NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code())
                                .isEqualTo("GEOFENCE_ACTIVE_LIMIT_EXCEEDED"));
    }

    @Test
    void requiredPublicationFailureEscapesTheEvaluationTransaction() {
        service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(3), 20, 20), NOW);
        service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(2), 2, 2), NOW);
        doThrow(new IllegalStateException("outbox unavailable")).when(publisher).publish(any());

        assertThatThrownBy(() -> service.evaluate(
                position(UUID.randomUUID(), NOW.minusSeconds(1), 2, 2), NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox unavailable");
    }

    @Test
    void configuredEntryAndExitAlertsPublishOnlyConfirmedTransitions() {
        geofence = activeGeofence(GeofenceType.DEPOT, new GeofenceAlertPolicy(true, true));
        service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(5), 20, 20), NOW);
        service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(4), 2, 2), NOW);
        service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(3), 2, 2), NOW);
        service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(2), 20, 20), NOW);
        service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(1), 20, 20), NOW);

        verify(publisher, org.mockito.Mockito.times(2)).publish(any());
    }

    @Test
    void disabledEntryAlertAndStablePositionsDoNotPublishPerPositionEvents() {
        geofence = activeGeofence(GeofenceType.DEPOT, new GeofenceAlertPolicy(false, false));
        service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(3), 2, 2), NOW);
        clearInvocations(publisher);
        for (int index = 0; index < 101; index++) {
            service.evaluate(position(UUID.randomUUID(), NOW.minusSeconds(2), 2, 2), NOW);
        }

        verify(publisher, never()).publish(any());
    }

    private static Geofence activeGeofence() {
        return activeGeofence(GeofenceType.UNAUTHORIZED_ZONE,
                GeofenceAlertPolicy.unauthorizedZone());
    }

    private static Geofence activeGeofence(
            GeofenceType type, GeofenceAlertPolicy alertPolicy) {
        Geofence value = Geofence.draft(UUID.randomUUID(), TENANT, "Geofence",
                type,
                GeofencePolygon.of(List.of(new Wgs84Coordinate(0, 0),
                        new Wgs84Coordinate(10, 0), new Wgs84Coordinate(0, 10))),
                type == GeofenceType.UNAUTHORIZED_ZONE ? null : UUID.randomUUID(),
                alertPolicy, NOW.minusSeconds(60), UUID.randomUUID());
        value.activate(NOW.minusSeconds(59), UUID.randomUUID());
        return value;
    }

    private static GeofencePosition position(
            UUID id, Instant source, double longitude, double latitude) {
        return new GeofencePosition(TENANT, id, VEHICLE, source,
                new Wgs84Coordinate(longitude, latitude), Trust.TRUSTED, Ordering.IN_ORDER, false);
    }
}
