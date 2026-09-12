package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceAlertPolicy;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceEvaluationJob;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceLifecycle;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceMembership;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePolygon;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceSeverity;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransition;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransitionType;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.domain.geofence.VehicleGeofenceState;
import com.transportlogistics.app.tracking.domain.geofence.Wgs84Coordinate;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceEvaluationJobRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleGeofenceStateRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;

class GeofencePersistencePostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Autowired GeofenceRepositoryPort geofences;
    @Autowired VehicleGeofenceStateRepositoryPort states;
    @Autowired GeofenceTransitionRepositoryPort transitions;
    @Autowired GeofenceEvaluationJobRepositoryPort jobs;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @Test
    void cleanMigrationReachesV77WithFourTrackingOwnedTablesAndIndexes() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("82");
        assertThat(tables()).contains("tracking_geofence", "tracking_vehicle_geofence_state",
                "tracking_geofence_transition", "tracking_geofence_evaluation_job");
        assertThat(indexes()).contains("uq_tracking_geofence_tenant_name",
                "idx_tracking_geofence_active_bbox", "idx_tracking_geofence_location",
                "idx_tracking_geofence_state_vehicle", "uq_tracking_geofence_transition_identity",
                "idx_tracking_geofence_transition_geofence", "idx_tracking_geofence_transition_vehicle",
                "idx_tracking_geofence_transition_unauthorized", "idx_tracking_geofence_job_due",
                "idx_tracking_geofence_job_global_due",
                "idx_tracking_geofence_active_bbox_upper");
        assertThat(columns("tracking_geofence_transition"))
                .doesNotContain("latitude", "longitude", "geometry", "raw_payload");
        assertThat(extensionExists("postgis")).isFalse();
    }

    @Test
    void upgradesV76ToV77WithoutChangingEarlierHistory() {
        flyway.clean();
        Flyway to76 = Flyway.configure().dataSource(configuredJdbcUrl(),
                        configuredDatabaseUsername(), configuredDatabasePassword())
                .cleanDisabled(false).placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("76")).load();
        to76.migrate();
        assertThat(to76.info().current().getVersion().getVersion()).isEqualTo("76");
        flyway.migrate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("82");
        assertThat(tables()).contains("tracking_geofence", "tracking_geofence_evaluation_job");
    }

    @Test
    void upgradesV79ToV80ByAddingOnlyTheTwoAuthorizedIndexes() {
        flyway.clean();
        Flyway to79 = Flyway.configure().dataSource(configuredJdbcUrl(),
                        configuredDatabaseUsername(), configuredDatabasePassword())
                .cleanDisabled(false).placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("79")).load();
        to79.migrate();
        assertThat(to79.info().current().getVersion().getVersion()).isEqualTo("79");
        assertThat(indexes()).doesNotContain("idx_tracking_geofence_job_global_due",
                "idx_tracking_geofence_active_bbox_upper");

        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("82");
        assertThat(indexes()).contains("idx_tracking_geofence_job_global_due",
                "idx_tracking_geofence_active_bbox_upper");
    }

    @Test
    void geofenceRoundTripsCanonicalPolygonBboxLifecycleAndOptimisticVersion() {
        UUID tenant = UUID.randomUUID();
        Geofence draft = geofence(tenant, UUID.randomUUID(), " Depot Boundary ", GeofenceType.DEPOT,
                UUID.randomUUID());
        Geofence stored = geofences.save(draft, 0);
        assertThat(stored.name()).isEqualTo("Depot Boundary");
        assertThat(stored.polygon().vertices()).containsExactlyElementsOf(draft.polygon().vertices());
        assertThat(jdbc.queryForMap("""
                SELECT jsonb_array_length(polygon_vertices) vertices,min_longitude,max_longitude,
                       min_latitude,max_latitude FROM tracking_geofence WHERE tenant_id=? AND id=?
                """, tenant, draft.id())).containsEntry("vertices", 4);
        draft.updateDefinition("Depot Boundary Updated", GeofenceType.DEPOT, polygon(),
                draft.locationId(), new GeofenceAlertPolicy(true, true), NOW.plusSeconds(1), ACTOR);
        Geofence updated = geofences.save(draft, 0);
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.alertPolicy().alertOnExit()).isTrue();
        assertThatThrownBy(() -> geofences.save(draft, 0))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("GEOFENCE_STALE_VERSION"));
        assertThat(geofences.find(UUID.randomUUID(), draft.id())).isEmpty();
    }

    @Test
    void definitionConstraintsAndTenantNameUniquenessAreEnforced() {
        UUID tenant = UUID.randomUUID();
        Geofence first = geofence(tenant, UUID.randomUUID(), "Unique", GeofenceType.DEPOT,
                UUID.randomUUID());
        geofences.save(first, 0);
        assertThatThrownBy(() -> geofences.save(geofence(
                tenant, UUID.randomUUID(), "Unique", GeofenceType.CUSTOMER_SITE, UUID.randomUUID()), 0))
                .isInstanceOf(BusinessRuleException.class);
        geofences.save(geofence(UUID.randomUUID(), UUID.randomUUID(), "Unique",
                GeofenceType.DEPOT, UUID.randomUUID()), 0);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE tracking_geofence SET type='UNAUTHORIZED_ZONE' WHERE tenant_id=? AND id=?
                """, tenant, first.id())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE tracking_geofence SET lifecycle='UNKNOWN' WHERE tenant_id=? AND id=?",
                tenant, first.id())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void activeCandidateAndDefinitionLockQueriesStayTenantScopedAndBounded() {
        UUID tenant = UUID.randomUUID();
        Geofence draft = geofence(tenant, UUID.randomUUID(), "Active", GeofenceType.DEPOT,
                UUID.randomUUID());
        geofences.save(draft, 0);
        draft.activate(NOW.plusSeconds(1), ACTOR);
        geofences.save(draft, 0);
        assertThat(geofences.findActiveCandidates(tenant, UUID.randomUUID(), 79.5, 6.5, 10))
                .extracting(Geofence::id).containsExactly(draft.id());
        assertThat(geofences.findActiveCandidates(
                UUID.randomUUID(), UUID.randomUUID(), 79.5, 6.5, 10)).isEmpty();
        assertThat(geofences.findActiveOutsideWithoutState(
                tenant, UUID.randomUUID(), 81, 8, 10)).singleElement()
                .extracting(GeofenceRepositoryPort.ActiveGeofenceReference::geofenceId)
                .isEqualTo(draft.id());
        assertThat(geofences.countActiveForUpdate(tenant)).isEqualTo(1);
        assertThatThrownBy(() -> geofences.findActiveCandidates(
                tenant, UUID.randomUUID(), 79.5, 6.5, 501))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stateRoundTripsPendingFactsAndRejectsStaleOrCrossTenantRelationships() {
        UUID tenant = UUID.randomUUID();
        Geofence definition = geofences.save(geofence(
                tenant, UUID.randomUUID(), "State", GeofenceType.DEPOT, UUID.randomUUID()), 0);
        UUID vehicle = UUID.randomUUID();
        UUID pendingPosition = UUID.randomUUID();
        VehicleGeofenceState initial = new VehicleGeofenceState(tenant, definition.id(), vehicle,
                definition.version(), GeofenceMembership.OUTSIDE, GeofenceMembership.INSIDE, 1,
                pendingPosition, pendingPosition, NOW, 0);
        assertThat(states.save(initial, 0)).isEqualTo(initial);
        VehicleGeofenceState updated = new VehicleGeofenceState(tenant, definition.id(), vehicle,
                definition.version(), GeofenceMembership.INSIDE, null, 0, null,
                UUID.randomUUID(), NOW.plusSeconds(1), 1);
        assertThat(states.save(updated, 0)).isEqualTo(updated);
        assertThatThrownBy(() -> states.save(updated, 0)).isInstanceOf(BusinessRuleException.class);
        assertThat(states.find(UUID.randomUUID(), vehicle, definition.id(), 0, 10)).isEmpty();
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tracking_vehicle_geofence_state(
                 tenant_id,geofence_id,vehicle_id,definition_version,pending_count,version,created_at,updated_at)
                VALUES(?,?,?,0,0,0,now(),now())
                """, UUID.randomUUID(), definition.id(), UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void transitionIsIdempotentImmutableOrderedFilteredAndCoordinateFree() {
        Fixture fixture = fixture("transition");
        GeofenceTransition first = transition(fixture, NOW, GeofenceTransitionType.ENTERED,
                GeofenceSeverity.NORMAL);
        assertThat(transitions.append(first)).isEqualTo(first);
        assertThat(transitions.append(first)).isEqualTo(first);
        GeofenceTransition unauthorized = transition(fixture, NOW.plusSeconds(1),
                GeofenceTransitionType.UNAUTHORIZED_ZONE_ENTERED, GeofenceSeverity.HIGH);
        transitions.append(unauthorized);
        assertThat(transitions.find(fixture.tenant(), fixture.geofence().id(), null, null,
                null, null, null, 10, false)).extracting(GeofenceTransition::transitionId)
                .containsExactly(unauthorized.transitionId(), first.transitionId());
        assertThat(transitions.find(fixture.tenant(), null, fixture.vehicle(), null,
                null, null, null, 10, true)).containsExactly(unauthorized);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE tracking_geofence_transition SET severity='HIGH' WHERE id=?",
                first.transitionId())).isInstanceOf(UncategorizedSQLException.class);
        assertThatThrownBy(() -> jdbc.update(
                "DELETE FROM tracking_geofence_transition WHERE id=?", first.transitionId()))
                .isInstanceOf(UncategorizedSQLException.class);
    }

    @Test
    void jobsAreIdempotentClaimedBoundedlyAndProtectedByLeaseOwner() {
        Fixture fixture = fixture("jobs");
        GeofenceEvaluationJob enqueued = jobs.enqueue(fixture.tenant(), fixture.position(), NOW);
        assertThat(jobs.enqueue(fixture.tenant(), fixture.position(), NOW)).isEqualTo(enqueued);
        GeofenceEvaluationJob claimed = jobs.claimDue(
                "worker-a", NOW, NOW.plusSeconds(30), 1).getFirst();
        assertThat(claimed.status()).isEqualTo(GeofenceEvaluationJob.Status.PROCESSING);
        assertThat(claimed.attempt()).isEqualTo(1);
        assertThat(jobs.renew(fixture.tenant(), fixture.position(), "worker-b", NOW,
                NOW.plusSeconds(40))).isFalse();
        assertThat(jobs.release(fixture.tenant(), fixture.position(), "worker-b", NOW)).isFalse();
        assertThat(jobs.renew(fixture.tenant(), fixture.position(), "worker-a", NOW,
                NOW.plusSeconds(40))).isTrue();
        jobs.retry(fixture.tenant(), fixture.position(), "worker-a", NOW, NOW.plusSeconds(5));
        GeofenceEvaluationJob retried = jobs.claimDue(
                "worker-b", NOW.plusSeconds(5), NOW.plusSeconds(35), 1).getFirst();
        assertThat(retried.attempt()).isEqualTo(2);
        jobs.complete(fixture.tenant(), fixture.position(), "worker-b", NOW.plusSeconds(6));
        assertThat(jobs.find(fixture.tenant(), fixture.position()).orElseThrow().status())
                .isEqualTo(GeofenceEvaluationJob.Status.COMPLETED);
        assertThatThrownBy(() -> jobs.claimDue("worker", NOW, NOW.plusSeconds(1), 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiredLeaseIsReclaimedAndConcurrentClaimHasOneWinner() throws Exception {
        Fixture fixture = fixture("claim");
        jobs.enqueue(fixture.tenant(), fixture.position(), NOW);
        jobs.claimDue("expired", NOW, NOW.plusSeconds(1), 1);
        assertThat(jobs.claimDue("reclaimer", NOW.plusSeconds(2), NOW.plusSeconds(20), 1))
                .hasSize(1);
        assertThat(jobs.release(fixture.tenant(), fixture.position(), "expired", NOW.plusSeconds(2)))
                .isFalse();
        jobs.release(fixture.tenant(), fixture.position(), "reclaimer", NOW.plusSeconds(2));
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> claimAfter(barrier, "one"));
            var second = executor.submit(() -> claimAfter(barrier, "two"));
            assertThat(first.get() + second.get()).isEqualTo(1);
        }
    }

    @Test
    void twoWorkersClaimDistinctJobsAndPreserveTheBoundedBatch() throws Exception {
        Fixture fixture = fixture("multi-worker");
        for (int index = 0; index < 7; index++) {
            UUID position = position(fixture, "multi-" + index);
            jobs.enqueue(fixture.tenant(), position, NOW.plusMillis(index));
        }
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> claimIdsAfter(barrier, "worker-one", 3));
            var second = executor.submit(() -> claimIdsAfter(barrier, "worker-two", 3));
            var firstIds = first.get();
            var secondIds = second.get();
            assertThat(firstIds).hasSize(3);
            assertThat(secondIds).hasSize(3);
            assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
        }
        assertThat(jobs.backlog(NOW.plusSeconds(1))).satisfies(backlog -> {
            assertThat(backlog.claimed()).isEqualTo(6);
            assertThat(backlog.queued()).isEqualTo(1);
        });
    }

    @Test
    void reclaimedLeaseRejectsEveryStaleOwnerMutation() {
        Fixture fixture = fixture("stale-owner");
        jobs.enqueue(fixture.tenant(), fixture.position(), NOW);
        jobs.claimDue("worker-a", NOW, NOW.plusSeconds(1), 1);
        jobs.claimDue("worker-b", NOW.plusSeconds(2), NOW.plusSeconds(30), 1);

        assertThat(jobs.renew(fixture.tenant(), fixture.position(), "worker-a",
                NOW.plusSeconds(2), NOW.plusSeconds(40))).isFalse();
        assertThat(jobs.release(fixture.tenant(), fixture.position(), "worker-a",
                NOW.plusSeconds(2))).isFalse();
        assertThatThrownBy(() -> jobs.complete(fixture.tenant(), fixture.position(), "worker-a",
                NOW.plusSeconds(2))).isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("GEOFENCE_JOB_STALE_LEASE"));
        assertThatThrownBy(() -> jobs.retry(fixture.tenant(), fixture.position(), "worker-a",
                NOW.plusSeconds(2), NOW.plusSeconds(5))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> jobs.fail(fixture.tenant(), fixture.position(), "worker-a",
                NOW.plusSeconds(2))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void realisticProductionPlansUseV80AndExistingTenantIndexes() {
        Fixture fixture = fixture("plans");
        seedPlanGeofences(fixture.tenant(), 5_000);
        seedPlanJobs(fixture, 5_000);
        jdbc.execute("ANALYZE tracking_geofence");
        jdbc.execute("ANALYZE tracking_geofence_evaluation_job");

        String candidatePlan = String.join("\n", jdbc.queryForList("""
                EXPLAIN (ANALYZE,COSTS OFF) WITH candidate_id AS (
                 SELECT id FROM tracking_geofence
                 WHERE tenant_id=? AND lifecycle='ACTIVE'
                   AND min_longitude<=CAST(? AS numeric) AND max_longitude>=CAST(? AS numeric)
                   AND min_latitude<=CAST(? AS numeric) AND max_latitude>=CAST(? AS numeric)
                 UNION
                 SELECT state.geofence_id FROM tracking_vehicle_geofence_state state
                 JOIN tracking_geofence definition
                   ON definition.tenant_id=state.tenant_id AND definition.id=state.geofence_id
                 WHERE state.tenant_id=? AND state.vehicle_id=?
                   AND definition.lifecycle='ACTIVE'
                )
                SELECT geofence.* FROM candidate_id candidate
                JOIN tracking_geofence geofence ON geofence.tenant_id=? AND geofence.id=candidate.id
                ORDER BY geofence.id LIMIT 500
                """, String.class, fixture.tenant(), 79.85, 79.85, 6.85, 6.85,
                fixture.tenant(), fixture.vehicle(), fixture.tenant()));
        System.out.println("US49_V80_BBOX_PLAN\n" + candidatePlan);
        assertThat(candidatePlan).contains("idx_tracking_geofence_active_bbox_upper")
                .doesNotContain("Rows Removed by Filter: 4990");

        String jobPlan = String.join("\n", jdbc.queryForList("""
                EXPLAIN (ANALYZE,COSTS OFF)
                SELECT tenant_id,position_id FROM tracking_geofence_evaluation_job
                WHERE next_attempt_at<=? AND (status IN('PENDING','FAILED')
                  OR (status='PROCESSING' AND lease_until<=?))
                ORDER BY next_attempt_at,tenant_id,position_id
                FOR UPDATE SKIP LOCKED LIMIT 16
                """, String.class, timestamp(NOW.plusSeconds(10)), timestamp(NOW.plusSeconds(10))));
        System.out.println("US49_V80_DUE_JOB_PLAN\n" + jobPlan);
        assertThat(jobPlan).contains("idx_tracking_geofence_job_global_due")
                .doesNotContain("Seq Scan on tracking_geofence_evaluation_job")
                .doesNotContain("Sort Key: next_attempt_at, tenant_id, position_id");

        assertThat(explain("""
                SELECT * FROM tracking_geofence_transition
                 WHERE tenant_id=? ORDER BY source_timestamp DESC,id DESC LIMIT 10
                """)).contains("idx_tracking_geofence_transition");
    }

    private int claimAfter(CyclicBarrier barrier, String owner) throws Exception {
        barrier.await();
        return jobs.claimDue(owner, NOW.plusSeconds(2), NOW.plusSeconds(30), 1).size();
    }

    private List<UUID> claimIdsAfter(CyclicBarrier barrier, String owner, int limit)
            throws Exception {
        barrier.await();
        return jobs.claimDue(owner, NOW.plusSeconds(1), NOW.plusSeconds(31), limit).stream()
                .map(GeofenceEvaluationJob::positionId).toList();
    }

    private Fixture fixture(String suffix) {
        UUID tenant = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_device(
                 id,tenant_id,external_device_reference,provider_alias,lifecycle,registered_at,
                 registered_by,version,created_at,updated_at)
                VALUES(?,?,?,?,'ACTIVE',?,?,0,?,?)
                """, device, tenant, "device-" + suffix, "FIXTURE", timestamp(NOW), ACTOR,
                timestamp(NOW), timestamp(NOW));
        UUID position = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_position(
                 id,tenant_id,device_id,vehicle_id,provider_alias,dedupe_identity,payload_hash,
                 source_timestamp,received_at,latitude,longitude,engine_state,trust,quality,
                 ordering_classification,retention_policy,retention_policy_version)
                VALUES(?,?,?,?,?,?,?, ?,?,6.5,79.5,'UNKNOWN','TRUSTED','GOOD','IN_ORDER','TEST','1')
                """, position, tenant, device, vehicle, "FIXTURE", hex(position), hex(UUID.randomUUID()),
                timestamp(NOW), timestamp(NOW));
        Geofence geofence = geofences.save(geofence(tenant, UUID.randomUUID(),
                "Fence " + suffix, GeofenceType.UNAUTHORIZED_ZONE, null), 0);
        return new Fixture(tenant, vehicle, position, geofence);
    }

    private UUID position(Fixture fixture, String suffix) {
        UUID position = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_position(
                 id,tenant_id,device_id,vehicle_id,provider_alias,dedupe_identity,payload_hash,
                 source_timestamp,received_at,latitude,longitude,engine_state,trust,quality,
                 ordering_classification,retention_policy,retention_policy_version)
                SELECT ?,tenant_id,id,?,'FIXTURE',?,?, ?,?,6.5,79.5,'UNKNOWN','TRUSTED','GOOD',
                 'IN_ORDER','TEST','1' FROM tracking_device WHERE tenant_id=? LIMIT 1
                """, position, fixture.vehicle(), hex(position), hex(UUID.randomUUID()),
                timestamp(NOW.plusSeconds(1)), timestamp(NOW.plusSeconds(1)), fixture.tenant());
        return position;
    }

    private void seedPlanGeofences(UUID tenantId, int count) {
        jdbc.update("""
                INSERT INTO tracking_geofence(
                 id,tenant_id,name,type,polygon_vertices,min_longitude,max_longitude,min_latitude,
                 max_latitude,location_id,alert_enter_enabled,alert_exit_enabled,lifecycle,version,
                 created_at,created_by,updated_at,updated_by)
                SELECT gen_random_uuid(),?,'Plan ' || value,'UNAUTHORIZED_ZONE',
                 '[{"longitude":0,"latitude":0},{"longitude":1,"latitude":0},
                   {"longitude":1,"latitude":1},{"longitude":0,"latitude":0}]'::jsonb,
                 CASE WHEN value<=10 THEN 79.8 ELSE 0 END,
                 CASE WHEN value<=10 THEN 79.9 ELSE 1 END,
                 CASE WHEN value<=10 THEN 6.8 ELSE 0 END,
                 CASE WHEN value<=10 THEN 6.9 ELSE 1 END,
                 NULL,TRUE,FALSE,'ACTIVE',1,now(),?,now(),?
                FROM generate_series(1,?) value
                """, tenantId, ACTOR, ACTOR, count);
    }

    private void seedPlanJobs(Fixture fixture, int count) {
        jdbc.update("""
                WITH device AS (
                 SELECT id FROM tracking_device WHERE tenant_id=? LIMIT 1
                ), generated AS (
                 SELECT gen_random_uuid() id FROM generate_series(1,?)
                ), inserted AS (
                 INSERT INTO tracking_position(
                  id,tenant_id,device_id,vehicle_id,provider_alias,dedupe_identity,payload_hash,
                  source_timestamp,received_at,latitude,longitude,engine_state,trust,quality,
                  ordering_classification,retention_policy,retention_policy_version)
                 SELECT generated.id,?,device.id,?,'FIXTURE','plan-' || generated.id,
                  repeat('d',64),?,?,6.5,79.5,'UNKNOWN','TRUSTED','GOOD','IN_ORDER','TEST','1'
                 FROM generated CROSS JOIN device RETURNING tenant_id,id
                )
                INSERT INTO tracking_geofence_evaluation_job(
                 tenant_id,position_id,status,attempt,next_attempt_at,created_at,updated_at)
                SELECT tenant_id,id,'PENDING',0,
                 CAST(? AS timestamptz)-(row_number() OVER())*interval '1 millisecond',?,?
                FROM inserted
                """, fixture.tenant(), count, fixture.tenant(), fixture.vehicle(), timestamp(NOW),
                timestamp(NOW), timestamp(NOW), timestamp(NOW), timestamp(NOW));
    }

    private static GeofenceTransition transition(
            Fixture fixture, Instant sourceTime, GeofenceTransitionType type, GeofenceSeverity severity) {
        UUID id = UUID.randomUUID();
        return new GeofenceTransition(id, fixture.tenant(), fixture.geofence().id(), fixture.vehicle(),
                null, GeofenceType.UNAUTHORIZED_ZONE, type, severity, sourceTime, 0,
                fixture.position(), GeofenceMembership.OUTSIDE, GeofenceMembership.INSIDE);
    }

    private static Geofence geofence(
            UUID tenant, UUID id, String name, GeofenceType type, UUID locationId) {
        GeofenceAlertPolicy policy = type == GeofenceType.UNAUTHORIZED_ZONE
                ? GeofenceAlertPolicy.unauthorizedZone() : new GeofenceAlertPolicy(true, false);
        return Geofence.draft(id, tenant, name, type, polygon(), locationId, policy, NOW, ACTOR);
    }

    private static GeofencePolygon polygon() {
        return GeofencePolygon.of(List.of(new Wgs84Coordinate(79, 6),
                new Wgs84Coordinate(80, 6), new Wgs84Coordinate(80, 7)));
    }

    private List<String> tables() {
        return jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema='public' AND table_name LIKE 'tracking_geofence%'
                   OR table_schema='public' AND table_name='tracking_vehicle_geofence_state'
                """, String.class);
    }

    private List<String> indexes() {
        return jdbc.queryForList("""
                SELECT indexname FROM pg_indexes WHERE schemaname='public'
                 AND (tablename LIKE 'tracking_geofence%' OR tablename='tracking_vehicle_geofence_state')
                """, String.class);
    }

    private List<String> columns(String table) {
        return jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema='public' AND table_name=? ORDER BY ordinal_position
                """, String.class, table);
    }

    private boolean extensionExists(String extension) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname=?)", Boolean.class, extension);
        return Boolean.TRUE.equals(exists);
    }

    private String explain(String sql) {
        UUID tenant = UUID.randomUUID();
        return String.join("\n", jdbc.queryForList("EXPLAIN " + sql, String.class, tenant));
    }

    private static String hex(UUID value) {
        return value.toString().replace("-", "") + value.toString().replace("-", "");
    }

    private static java.sql.Timestamp timestamp(Instant value) {
        return java.sql.Timestamp.from(value);
    }

    private record Fixture(UUID tenant, UUID vehicle, UUID position, Geofence geofence) {
    }
}
