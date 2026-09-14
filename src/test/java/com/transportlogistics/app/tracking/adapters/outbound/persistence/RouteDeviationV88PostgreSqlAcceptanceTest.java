package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.routing.PlannedRouteGeometry;
import com.transportlogistics.app.routing.PlannedRouteGeometryLookup;
import com.transportlogistics.app.routing.Wgs84Point;
import com.transportlogistics.app.routing.application.ports.out.RouteRevisionGeometryRepository;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.domain.routedeviation.DistanceMeters;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationPosition;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationReview;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import com.transportlogistics.app.tracking.domain.routedeviation.RoutePoint;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteVersion;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;
import com.transportlogistics.app.tracking.application.RouteDeviationEvaluationService;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationAssignmentLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEvaluationTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationReviewRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationRuleRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationStateRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class RouteDeviationV88PostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RouteRevisionGeometryRepository geometries;
    @Autowired PlannedRouteGeometryLookup lookup;
    @Autowired RouteDeviationRuleRepositoryPort rules;
    @Autowired RouteDeviationStateRepositoryPort states;
    @Autowired RouteDeviationEpisodeRepositoryPort episodes;
    @Autowired RouteDeviationReviewRepositoryPort reviews;
    @Autowired RouteDeviationEvaluationTransactionPort evaluationTransaction;
    @Autowired Flyway flyway;

    @Test
    void geometryPersistsAtomicallyAndResolvesOnlyExactTenantRevision() {
        UUID tenant = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        UUID revision = insertRevision(tenant, route, 1);
        var geometry = new PlannedRouteGeometry(route, "REVISION:1", List.of(
                point("79.8612000", "6.9271000"), point("79.8613000", "6.9272000")));

        assertThat(geometries.save(tenant, revision, geometry)).isEqualTo(geometry);
        assertThat(lookup.find(tenant, route, "REVISION:1")).contains(geometry);
        assertThat(lookup.find(UUID.randomUUID(), route, "REVISION:1")).isEmpty();
        assertThat(lookup.find(tenant, route, "REVISION:2")).isEmpty();
        assertThat(geometries.save(tenant, revision, geometry)).isEqualTo(geometry);

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE route_revision_geometry_point SET latitude=7 WHERE tenant_id=?",
                tenant)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void maximumGeometryRoundTripsInDeterministicOrder() {
        UUID tenant = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        UUID revision = insertRevision(tenant, route, 2);
        List<Wgs84Point> points = new ArrayList<>();
        for (int index = 0; index < 2_000; index++) {
            points.add(new Wgs84Point(new BigDecimal("79.0000000").add(
                    BigDecimal.valueOf(index, 7)), new BigDecimal("6.0000000")));
        }
        var geometry = new PlannedRouteGeometry(route, "REVISION:2", points);

        geometries.save(tenant, revision, geometry);

        assertThat(lookup.find(tenant, route, "REVISION:2")).contains(geometry);
    }

    @Test
    void databaseRejectsInvalidGeometryAndDuplicatePointOrder() {
        UUID tenant = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        UUID revision = insertRevision(tenant, route, 3);
        UUID geometry = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO route_revision_geometry(
                  id,tenant_id,route_revision_id,route_id,route_version,point_count)
                VALUES(?,?,?,?,?,2)
                """, geometry, tenant, revision, route, "REVISION:3");
        jdbc.update("""
                INSERT INTO route_revision_geometry_point(
                  tenant_id,geometry_id,point_order,longitude,latitude) VALUES(?,?,0,79,6)
                """, tenant, geometry);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO route_revision_geometry_point(
                  tenant_id,geometry_id,point_order,longitude,latitude) VALUES(?,?,0,79,6)
                """, tenant, geometry)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO route_revision_geometry_point(
                  tenant_id,geometry_id,point_order,longitude,latitude) VALUES(?,?,1,181,6)
                """, tenant, geometry)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ruleStateCandidateEpisodeAndReviewRoundTripWithTenantIsolation() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        UUID trip = UUID.randomUUID();
        UUID driver = UUID.randomUUID();
        Instant firstAt = Instant.now().minusSeconds(2).truncatedTo(ChronoUnit.MICROS);
        var version = new RouteVersion("REVISION:4");
        var rule = new RouteDeviationRule(UUID.randomUUID(), tenant, route, version,
                new DistanceMeters(new BigDecimal("50.000")), RouteDeviationRule.Lifecycle.ACTIVE,
                1, 1, firstAt.minusSeconds(10));
        rules.save(rule);
        assertThat(rules.findActive(tenant, route, version)).contains(rule);

        var first = position(tenant, vehicle, firstAt, "79.8612000", "6.9271000");
        VehicleRouteDeviationState state = VehicleRouteDeviationState.unknown(tenant, vehicle)
                .candidate(first, trip, driver, route, version, rule,
                        new DistanceMeters(new BigDecimal("55.000")),
                        new DistanceMeters(new BigDecimal("75.000")));
        states.save(state);
        assertThat(states.find(tenant, vehicle)).contains(state);
        assertThat(states.find(UUID.randomUUID(), vehicle)).isEmpty();
        assertThat(states.find(tenant, vehicle).orElseThrow().candidate().point())
                .isEqualTo(first.point());
        assertThat(states.find(tenant, vehicle).orElseThrow().candidate().accuracy())
                .isEqualTo(first.accuracy());

        var second = position(tenant, vehicle, firstAt.plusSeconds(1), "79.8613000", "6.9272000");
        var episode = com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode
                .confirm(tenant, vehicle, trip, driver, route, version, rule, first, second,
                        new DistanceMeters(new BigDecimal("75.000")),
                        new DistanceMeters(new BigDecimal("120.000")),
                        new DistanceMeters(new BigDecimal("55.000")));
        episodes.save(episode);
        assertThat(episodes.find(tenant, episode.id())).contains(episode);
        assertThat(episodes.find(UUID.randomUUID(), episode.id())).isEmpty();
        assertThat(episodes.save(episode)).isEqualTo(episode);

        var review = RouteDeviationReview.decide(tenant, episode.id(),
                RouteDeviationReview.Status.APPROVED,
                RouteDeviationReview.Reason.AUTHORIZED_DETOUR, "Approved route", UUID.randomUUID(),
                Instant.now().truncatedTo(ChronoUnit.MICROS), 0);
        reviews.append(review);
        assertThat(reviews.history(tenant, episode.id())).containsExactly(review);
        assertThat(reviews.history(UUID.randomUUID(), episode.id())).isEmpty();
    }

    @Test
    void flywayHeadAndOwnershipShapeAreV88WithoutCrossModuleForeignKeys() {
        assertThat(jdbc.queryForObject("""
                SELECT version FROM flyway_schema_history WHERE success
                ORDER BY installed_rank DESC LIMIT 1
                """, String.class)).isEqualTo("92");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables WHERE table_schema='public'
                AND table_name IN ('route_revision_geometry','route_revision_geometry_point',
                'tracking_route_deviation_rule','tracking_route_deviation_state',
                'tracking_route_deviation_episode','tracking_route_deviation_review')
                """, Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.table_constraints
                WHERE constraint_type='FOREIGN KEY'
                AND table_name LIKE 'tracking_route_deviation_%'
                AND constraint_name NOT LIKE '%review%'
                """, Integer.class)).isZero();
    }

    @Test
    void v87TelemetryHistorySurvivesForwardMigrationToV88() {
        flyway.clean();
        Flyway throughV87 = Flyway.configure()
                .configuration(flyway.getConfiguration())
                .target("87")
                .load();
        throughV87.migrate();
        UUID historyId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_position_history(
                  tenant_id,source_timestamp,id,device_id,vehicle_id,provider_alias,
                  dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                  ordering_classification,retention_policy,retention_policy_version)
                VALUES(?,?,?,?,?,'V88-UPGRADE',?,now(),6.9271,79.8612,'UNKNOWN','TRUSTED',
                  'GOOD','IN_ORDER','STANDARD','1')
                """, UUID.randomUUID(), OffsetDateTime.now(), historyId, UUID.randomUUID(),
                UUID.randomUUID(), "a".repeat(64));

        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("92");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM tracking_position_history WHERE id=?",
                Integer.class, historyId)).isOne();
    }

    @Test
    void concurrentStateWritesRetainNewestSourceEvidenceAcrossTenants() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        UUID trip = UUID.randomUUID();
        UUID driver = UUID.randomUUID();
        Instant base = Instant.now().truncatedTo(ChronoUnit.MICROS);
        var version = new RouteVersion("REVISION:5");
        var rule = new RouteDeviationRule(UUID.randomUUID(), tenant, route, version,
                new DistanceMeters(new BigDecimal("50.000")), RouteDeviationRule.Lifecycle.ACTIVE,
                1, 1, base.minusSeconds(10));
        var older = VehicleRouteDeviationState.unknown(tenant, vehicle).candidate(
                position(tenant, vehicle, base.plusSeconds(1), "79.8612000", "6.9271000"),
                trip, driver, route, version, rule, new DistanceMeters(new BigDecimal("55.000")),
                new DistanceMeters(new BigDecimal("70.000")));
        var newer = VehicleRouteDeviationState.unknown(tenant, vehicle).candidate(
                position(tenant, vehicle, base.plusSeconds(2), "79.8613000", "6.9272000"),
                trip, driver, route, version, rule, new DistanceMeters(new BigDecimal("55.000")),
                new DistanceMeters(new BigDecimal("80.000")));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var olderAttempt = executor.submit(() -> saveAfter(start, older));
            var newerAttempt = executor.submit(() -> saveAfter(start, newer));
            start.countDown();
            olderAttempt.get();
            newerAttempt.get();
        }

        assertThat(states.find(tenant, vehicle).orElseThrow().lastPositionId())
                .isEqualTo(newer.lastPositionId());
        var otherRule = new RouteDeviationRule(rule.id(), otherTenant, route, version,
                rule.configuredTolerance(), rule.lifecycle(), 1, 1, rule.effectiveAt());
        var otherState = VehicleRouteDeviationState.unknown(otherTenant, vehicle).candidate(
                position(otherTenant, vehicle, base.plusSeconds(1), "79.8612000", "6.9271000"),
                trip, driver, route, version, otherRule,
                new DistanceMeters(new BigDecimal("55.000")),
                new DistanceMeters(new BigDecimal("70.000")));
        states.save(otherState);
        assertThat(states.find(otherTenant, vehicle)).contains(otherState);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM tracking_route_deviation_state WHERE vehicle_id=?",
                Integer.class, vehicle)).isEqualTo(2);
    }

    @Test
    void applicationEvaluationPersistsOneLifecycleAndRollsBackAtomically() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        UUID trip = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        RouteVersion version = RouteVersion.ofRevision(6);
        RouteDeviationRule rule = new RouteDeviationRule(UUID.randomUUID(), tenant, route,
                version, DistanceMeters.of(100), RouteDeviationRule.Lifecycle.ACTIVE,
                1, 1, now.minusSeconds(10));
        rules.save(rule);
        var service = evaluationService(tenant, vehicle, route, trip, version, now, episodes);

        RouteDeviationPosition first = position(tenant, vehicle, now.minusSeconds(2),
                "79.0010000", "6.0010000");
        RouteDeviationPosition second = position(tenant, vehicle, now.minusSeconds(1),
                "79.0020000", "6.0020000");
        service.evaluate(first);
        service.evaluate(second);
        service.evaluate(second);

        RouteDeviationEpisode open = episodes.findOpen(tenant, vehicle).orElseThrow();
        assertThat(open.firstCandidatePositionId()).isEqualTo(first.positionId());
        assertThat(open.confirmingPositionId()).isEqualTo(second.positionId());
        assertThat(open.eligibleOutsideSampleCount()).isEqualTo(2);
        service.evaluate(position(tenant, vehicle, now, "79.0000001", "6.0000001"));
        assertThat(episodes.findOpen(tenant, vehicle)).isEmpty();
        assertThat(episodes.find(tenant, open.id()).orElseThrow().terminalOutcome())
                .isEqualTo(com.transportlogistics.app.tracking.domain.routedeviation
                        .RouteDeviationEpisode.TerminalOutcome.RETURNED_TO_ROUTE);

        UUID rollbackVehicle = UUID.randomUUID();
        RouteDeviationEpisodeRepositoryPort failing = new FailingEpisodeStore(episodes);
        var rollbackService = evaluationService(tenant, rollbackVehicle, route, trip,
                version, now, failing);
        rollbackService.evaluate(position(tenant, rollbackVehicle, now.minusSeconds(2),
                "79.0010000", "6.0010000"));
        assertThatThrownBy(() -> rollbackService.evaluate(position(tenant, rollbackVehicle,
                now.minusSeconds(1), "79.0020000", "6.0020000")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(states.find(tenant, rollbackVehicle).orElseThrow().state())
                .isNotEqualTo(VehicleRouteDeviationState.State.DEVIATING);
        assertThat(episodes.findOpen(tenant, rollbackVehicle)).isEmpty();
    }

    @Test
    void concurrentConfirmationsOpenExactlyOneEpisode() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        UUID trip = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        RouteVersion version = RouteVersion.ofRevision(7);
        rules.save(new RouteDeviationRule(UUID.randomUUID(), tenant, route, version,
                DistanceMeters.of(100), RouteDeviationRule.Lifecycle.ACTIVE,
                1, 1, now.minusSeconds(10)));
        var service = evaluationService(tenant, vehicle, route, trip, version, now, episodes);
        service.evaluate(position(tenant, vehicle, now.minusSeconds(3),
                "79.0010000", "6.0010000"));

        RouteDeviationPosition earlierConfirmation = position(tenant, vehicle,
                now.minusSeconds(2), "79.0020000", "6.0020000");
        RouteDeviationPosition laterConfirmation = position(tenant, vehicle,
                now.minusSeconds(1), "79.0030000", "6.0030000");
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var earlier = executor.submit(() -> evaluateAfter(start, service, earlierConfirmation));
            var later = executor.submit(() -> evaluateAfter(start, service, laterConfirmation));
            start.countDown();
            earlier.get();
            later.get();
        }

        RouteDeviationEpisode open = episodes.findOpen(tenant, vehicle).orElseThrow();
        assertThat(open.firstCandidatePositionId()).isNotNull();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_route_deviation_episode
                WHERE tenant_id=? AND vehicle_id=? AND end_source_timestamp IS NULL
                """, Integer.class, tenant, vehicle)).isOne();
        assertThat(states.find(tenant, vehicle).orElseThrow().lastSourceTimestamp())
                .isEqualTo(laterConfirmation.sourceTimestamp());
    }

    private RouteDeviationEvaluationService evaluationService(UUID tenant, UUID vehicle,
            UUID route, UUID trip, RouteVersion version, Instant now,
            RouteDeviationEpisodeRepositoryPort episodeStore) {
        return new RouteDeviationEvaluationService(
                (requestedTenant, requestedVehicle, at) -> requestedTenant.equals(tenant)
                        && requestedVehicle.equals(vehicle) ? java.util.Optional.of(
                                new RouteDeviationAssignmentLookupPort.Assignment(
                                        trip, null, route, version.value())) : java.util.Optional.empty(),
                (requestedTenant, requestedRoute, requestedVersion) -> java.util.Optional.of(
                        new com.transportlogistics.app.tracking.domain.routedeviation.RoutePolyline(
                                requestedVersion, List.of(
                                new RoutePoint(new BigDecimal("79.0000000"), new BigDecimal("6.0000000")),
                                new RoutePoint(new BigDecimal("79.0100000"), new BigDecimal("6.0000000"))))),
                rules, states, episodeStore, evaluationTransaction,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private record FailingEpisodeStore(RouteDeviationEpisodeRepositoryPort delegate)
            implements RouteDeviationEpisodeRepositoryPort {
        @Override public com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode save(
                com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode episode) {
            throw new IllegalStateException("simulated episode failure");
        }
        @Override public java.util.Optional<com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode> find(
                UUID tenant, UUID episode) { return delegate.find(tenant, episode); }
        @Override public java.util.Optional<com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode> findOpen(
                UUID tenant, UUID vehicle) { return delegate.findOpen(tenant, vehicle); }
        @Override public List<com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode> history(
                UUID tenant, UUID vehicle, Instant from, Instant to, String cursor, int limit) {
            return delegate.history(tenant, vehicle, from, to, cursor, limit);
        }
    }

    private boolean saveAfter(CountDownLatch start, VehicleRouteDeviationState state)
            throws InterruptedException {
        start.await();
        try {
            states.save(state);
            return true;
        } catch (IllegalStateException stale) {
            return false;
        }
    }

    private static boolean evaluateAfter(CountDownLatch start,
            RouteDeviationEvaluationService service, RouteDeviationPosition position)
            throws InterruptedException {
        start.await();
        service.evaluate(position);
        return true;
    }

    private UUID insertRevision(UUID tenant, UUID route, int number) {
        UUID origin = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        jdbc.update("INSERT INTO location(id,code,name,active,tenant_id) VALUES(?,?,?,true,?)",
                origin, "O-" + origin, "Origin", tenant);
        jdbc.update("INSERT INTO location(id,code,name,active,tenant_id) VALUES(?,?,?,true,?)",
                destination, "D-" + destination, "Destination", tenant);
        jdbc.update("""
                INSERT INTO route(id,code,name,origin_location_id,destination_location_id,
                  planned_distance_km,estimated_duration_minutes,active,tenant_id)
                VALUES(?,?,?,?,?,1,10,true,?)
                """, route, "R-" + route, "Route", origin, destination, tenant);
        UUID revision = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO route_revision(id,route_id,revision_number,code,name,origin_location_id,
                  destination_location_id,planned_distance_km,estimated_duration_minutes,active,
                  changed_at,changed_by,tenant_id)
                VALUES(?,?,?,?,?,?,?,?,?,true,?,?,?)
                """, revision, route, number, "R-" + route, "Route", origin,
                destination, 1d, 10, OffsetDateTime.now(), "test", tenant);
        return revision;
    }

    private static Wgs84Point point(String longitude, String latitude) {
        return new Wgs84Point(new BigDecimal(longitude), new BigDecimal(latitude));
    }

    private static RouteDeviationPosition position(UUID tenant, UUID vehicle, Instant source,
            String longitude, String latitude) {
        return new RouteDeviationPosition(UUID.randomUUID(), tenant, vehicle, source,
                new RoutePoint(new BigDecimal(longitude), new BigDecimal(latitude)),
                new DistanceMeters(new BigDecimal("5.000")), true, false,
                RouteDeviationPosition.Trust.TRUSTED, true, RouteDeviationPosition.Ordering.IN_ORDER);
    }
}
