package com.transportlogistics.app.tracking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.transportlogistics.app.tracking.domain.routedeviation.DistanceMeters;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationAvailability;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationPosition;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import com.transportlogistics.app.tracking.domain.routedeviation.RoutePoint;
import com.transportlogistics.app.tracking.domain.routedeviation.RoutePolyline;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteVersion;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationAssignmentLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEvaluationTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationRuleRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationStateRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RouteDeviationEvaluationServiceTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID VEHICLE = UUID.randomUUID();
    private static final UUID TRIP = UUID.randomUUID();
    private static final UUID ROUTE = UUID.randomUUID();
    private static final RouteVersion VERSION = RouteVersion.ofRevision(7);
    private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");
    private final MemoryStore store = new MemoryStore();
    private final EpisodeStore episodeStore = new EpisodeStore();
    private RouteDeviationEvaluationService service;

    @BeforeEach
    void setUp() {
        RouteDeviationRule rule = new RouteDeviationRule(UUID.randomUUID(), TENANT, ROUTE,
                VERSION, DistanceMeters.of(100), RouteDeviationRule.Lifecycle.ACTIVE,
                1, 1, NOW.minusSeconds(60));
        service = new RouteDeviationEvaluationService(
                (tenant, vehicle, at) -> Optional.of(
                        new RouteDeviationAssignmentLookupPort.Assignment(TRIP, null, ROUTE,
                                VERSION.value())),
                (tenant, route, version) -> Optional.of(new RoutePolyline(version, List.of(
                        point(0, 0), point(0.01, 0)))),
                new RuleStore(rule), store, episodeStore, directTransaction(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void preservesFirstCandidateEvidenceWhenSecondPointConfirms() {
        RouteDeviationPosition first = position(NOW.minusSeconds(2), 0.001, 0.001,
                10, UUID.randomUUID());
        RouteDeviationPosition confirming = position(NOW.minusSeconds(1), 0.002, 0.002,
                20, UUID.randomUUID());

        service.evaluate(first);
        service.evaluate(confirming);

        assertEquals(VehicleRouteDeviationState.State.DEVIATING, store.state.state());
        RouteDeviationEpisode episode = episodeStore.open;
        assertEquals(first.positionId(), episode.firstCandidatePositionId());
        assertEquals(first.sourceTimestamp(), episode.startSourceTimestamp());
        assertEquals(confirming.positionId(), episode.confirmingPositionId());
        assertNotEquals(first.point(), confirming.point());
        assertEquals(first.point(), store.firstCandidate.point());
        assertEquals(first.accuracy(), store.firstCandidate.accuracy());
    }

    @Test
    void duplicateCannotConfirmAndOneInsidePointClears() {
        RouteDeviationPosition first = position(NOW.minusSeconds(3), 0.001, 0.001,
                0, UUID.randomUUID());
        service.evaluate(first);
        service.evaluate(first);
        assertNull(episodeStore.open);

        service.evaluate(position(NOW.minusSeconds(2), 0.0015, 0.0015,
                0, UUID.randomUUID()));
        RouteDeviationEpisode episode = episodeStore.open;
        service.evaluate(position(NOW.minusSeconds(1), 0.00001, 0.00001,
                0, UUID.randomUUID()));

        assertEquals(RouteDeviationEpisode.TerminalOutcome.RETURNED_TO_ROUTE,
                episodeStore.savedEpisodes.get(episodeStore.savedEpisodes.size() - 1).terminalOutcome());
        assertEquals(VehicleRouteDeviationState.State.ON_ROUTE, store.state.state());
        assertEquals(episode.id(), episodeStore.savedEpisodes
                .get(episodeStore.savedEpisodes.size() - 1).id());
    }

    @Test
    void missingAccuracyIsTruthfulAndDoesNotClearExistingCandidate() {
        RouteDeviationPosition first = position(NOW.minusSeconds(2), 0.001, 0.001,
                0, UUID.randomUUID());
        service.evaluate(first);
        VehicleRouteDeviationState.Candidate candidate = store.state.candidate();
        RouteDeviationPosition unknown = new RouteDeviationPosition(UUID.randomUUID(), TENANT,
                VEHICLE, NOW.minusSeconds(1), point(0, 0), null, true, false,
                RouteDeviationPosition.Trust.TRUSTED, true,
                RouteDeviationPosition.Ordering.IN_ORDER);

        var result = service.evaluateResult(unknown);

        assertEquals(RouteDeviationAvailability.ACCURACY_UNKNOWN, result.availability());
        assertEquals(candidate, store.state.candidate());
    }

    @Test
    void routeRevisionChangeClosesOldEpisodeAsSupersededAndStartsFreshState() {
        service.evaluate(position(NOW.minusSeconds(4), 0.001, 0.001, 0, UUID.randomUUID()));
        service.evaluate(position(NOW.minusSeconds(3), 0.0015, 0.0015, 0, UUID.randomUUID()));
        RouteDeviationEpisode old = episodeStore.open;
        RouteVersion next = RouteVersion.ofRevision(8);
        RouteDeviationRule nextRule = new RouteDeviationRule(UUID.randomUUID(), TENANT, ROUTE,
                next, DistanceMeters.of(100), RouteDeviationRule.Lifecycle.ACTIVE,
                1, 1, NOW.minusSeconds(30));
        service = new RouteDeviationEvaluationService(
                (tenant, vehicle, at) -> Optional.of(new RouteDeviationAssignmentLookupPort.Assignment(
                        TRIP, null, ROUTE, next.value())),
                (tenant, route, version) -> Optional.of(new RoutePolyline(version,
                        List.of(point(0, 0), point(0.01, 0)))),
                new RuleStore(nextRule), store, episodeStore, directTransaction(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        service.evaluate(position(NOW.minusSeconds(2), 0.001, 0.001, 0, UUID.randomUUID()));

        RouteDeviationEpisode closed = episodeStore.savedEpisodes.stream()
                .filter(value -> value.id().equals(old.id()) && !value.open()).findFirst().orElseThrow();
        assertEquals(RouteDeviationEpisode.TerminalOutcome.SUPERSEDED, closed.terminalOutcome());
        assertEquals(next, store.state.candidate().routeVersion());
        assertNull(episodeStore.open);
    }

    @Test
    void distinguishesMissingAttributionGeometryRuleAndProviderFailure() {
        RouteDeviationPosition position = position(NOW.minusSeconds(1), 0, 0,
                0, UUID.randomUUID());
        assertEquals(RouteDeviationAvailability.NO_TRIP,
                serviceWith((tenant, vehicle, at) -> Optional.empty(),
                        (tenant, route, version) -> Optional.empty(), new RuleStore(null))
                        .evaluateResult(position).availability());
        assertEquals(RouteDeviationAvailability.NO_ASSIGNED_ROUTE,
                serviceWith((tenant, vehicle, at) -> Optional.of(
                                new RouteDeviationAssignmentLookupPort.Assignment(TRIP, null, null, null)),
                        (tenant, route, version) -> Optional.empty(), new RuleStore(null))
                        .evaluateResult(position).availability());
        assertEquals(RouteDeviationAvailability.NO_ROUTE_REVISION,
                serviceWith((tenant, vehicle, at) -> Optional.of(
                                new RouteDeviationAssignmentLookupPort.Assignment(TRIP, null, ROUTE, null)),
                        (tenant, route, version) -> Optional.empty(), new RuleStore(null))
                        .evaluateResult(position).availability());
        assertEquals(RouteDeviationAvailability.GEOMETRY_UNAVAILABLE,
                serviceWith((tenant, vehicle, at) -> Optional.of(
                                new RouteDeviationAssignmentLookupPort.Assignment(TRIP, null, ROUTE,
                                        VERSION.value())),
                        (tenant, route, version) -> Optional.empty(), new RuleStore(null))
                        .evaluateResult(position).availability());
        assertEquals(RouteDeviationAvailability.PROVIDER_UNAVAILABLE,
                serviceWith((tenant, vehicle, at) -> { throw new IllegalStateException("provider"); },
                        (tenant, route, version) -> Optional.empty(), new RuleStore(null))
                        .evaluateResult(position).availability());
    }

    private RouteDeviationEvaluationService serviceWith(
            RouteDeviationAssignmentLookupPort assignmentLookup,
            com.transportlogistics.app.tracking.ports.outbound.RouteDeviationGeometryLookupPort geometryLookup,
            RouteDeviationRuleRepositoryPort ruleStore) {
        return new RouteDeviationEvaluationService(assignmentLookup, geometryLookup, ruleStore,
                store, episodeStore, directTransaction(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static RouteDeviationPosition position(Instant at, double longitude, double latitude,
            double accuracy, UUID id) {
        return new RouteDeviationPosition(id, TENANT, VEHICLE, at, point(longitude, latitude),
                DistanceMeters.of(accuracy), true, false, RouteDeviationPosition.Trust.TRUSTED,
                true, RouteDeviationPosition.Ordering.IN_ORDER);
    }

    private static RoutePoint point(double longitude, double latitude) {
        return new RoutePoint(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude));
    }

    private record RuleStore(RouteDeviationRule rule) implements RouteDeviationRuleRepositoryPort {
        @Override
        public Optional<RouteDeviationRule> findActive(UUID tenant, UUID route, RouteVersion version) {
            return rule != null && rule.tenantId().equals(tenant) && rule.routeId().equals(route)
                    && rule.routeVersion().equals(version) ? Optional.of(rule) : Optional.empty();
        }

        @Override
        public RouteDeviationRule save(RouteDeviationRule value) {
            throw new UnsupportedOperationException();
        }
    }

    private static RouteDeviationEvaluationTransactionPort directTransaction() {
        return new RouteDeviationEvaluationTransactionPort() {
            @Override public <T> T execute(Supplier<T> operation) { return operation.get(); }
        };
    }

    private static final class MemoryStore implements RouteDeviationStateRepositoryPort {
        private VehicleRouteDeviationState state;
        private VehicleRouteDeviationState.Candidate firstCandidate;

        @Override public Optional<VehicleRouteDeviationState> find(UUID tenant, UUID vehicle) {
            return scoped(tenant, vehicle) ? Optional.ofNullable(state) : Optional.empty();
        }
        @Override public Optional<VehicleRouteDeviationState> lockAndFind(UUID tenant, UUID vehicle) {
            return find(tenant, vehicle);
        }
        @Override public VehicleRouteDeviationState save(VehicleRouteDeviationState value) {
            state = value;
            if (value.candidate() != null && firstCandidate == null) firstCandidate = value.candidate();
            return value;
        }
        private boolean scoped(UUID tenant, UUID vehicle) {
            return state != null && state.tenantId().equals(tenant)
                    && state.vehicleId().equals(vehicle);
        }
    }

    private static final class EpisodeStore implements RouteDeviationEpisodeRepositoryPort {
        private RouteDeviationEpisode open;
        private final List<RouteDeviationEpisode> savedEpisodes = new ArrayList<>();

        @Override public RouteDeviationEpisode save(RouteDeviationEpisode value) {
            savedEpisodes.add(value);
            open = value.open() ? value : null;
            return value;
        }
        @Override public Optional<RouteDeviationEpisode> find(UUID tenant, UUID episode) {
            return savedEpisodes.stream().filter(value -> value.tenantId().equals(tenant)
                    && value.id().equals(episode)).reduce((first, second) -> second);
        }
        @Override public Optional<RouteDeviationEpisode> findOpen(UUID tenant, UUID vehicle) {
            return open != null && open.tenantId().equals(tenant) && open.vehicleId().equals(vehicle)
                    ? Optional.of(open) : Optional.empty();
        }
        @Override public List<RouteDeviationEpisode> history(UUID tenant, UUID vehicle,
                Instant from, Instant to, String cursor, int limit) { return List.copyOf(savedEpisodes); }
    }
}
