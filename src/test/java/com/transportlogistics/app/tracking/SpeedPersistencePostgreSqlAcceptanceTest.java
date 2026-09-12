package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.domain.speed.ResolvedSpeedThreshold;
import com.transportlogistics.app.tracking.domain.speed.SpeedAttribution;
import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationJob;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import com.transportlogistics.app.tracking.domain.speed.VehicleSpeedState;
import com.transportlogistics.app.tracking.ports.outbound.SpeedEvaluationJobRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedRuleRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleSpeedStateRepositoryPort;
import java.math.BigDecimal;
import java.sql.Timestamp;
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

class SpeedPersistencePostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-11T12:00:00Z");

    @Autowired SpeedRuleRepositoryPort rules;
    @Autowired VehicleSpeedStateRepositoryPort states;
    @Autowired SpeedingEpisodeRepositoryPort episodes;
    @Autowired SpeedEvaluationJobRepositoryPort jobs;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @Test
    void cleanMigrationReachesV82WithOnlyFourSpeedTablesAndRequiredIndexes() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("83");
        assertThat(speedTables()).containsExactlyInAnyOrder("tracking_speed_rule", "tracking_speed_state",
                "tracking_speed_episode", "tracking_speed_evaluation_job");
        assertThat(speedIndexes()).contains("uq_tracking_speed_rule_active_tenant",
                "uq_tracking_speed_rule_active_route", "idx_tracking_speed_rule_route_lookup",
                "idx_tracking_speed_rule_tenant_lookup", "tracking_speed_state_pkey",
                "uq_tracking_speed_episode_active_vehicle", "idx_tracking_speed_episode_vehicle_history",
                "idx_tracking_speed_episode_severity_history", "idx_tracking_speed_episode_repeat",
                "idx_tracking_speed_job_global_due");
        assertThat(speedTables()).noneMatch(name -> name.contains("outbox")
                || name.contains("notification") || name.contains("audit"));
    }

    @Test
    void upgradesV80ThroughV82ForwardOnly() {
        flyway.clean();
        Flyway to80 = Flyway.configure().dataSource(configuredJdbcUrl(),
                        configuredDatabaseUsername(), configuredDatabasePassword())
                .cleanDisabled(false).placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("80")).load();
        to80.migrate();
        assertThat(to80.info().current().getVersion().getVersion()).isEqualTo("80");
        assertThat(speedTables()).isEmpty();
        flyway.migrate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("83");
    }

    @Test
    void rulesRoundTripDecimalScopesLifecycleOptimisticVersionAndTenantIsolation() {
        UUID tenant = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        SpeedRule draft = rule(tenant, UUID.randomUUID(), SpeedRule.Scope.ROUTE_VERSION,
                route, "route-v7", "67.125", SpeedRule.Lifecycle.DRAFT, 1, null);
        assertThat(rules.save(draft, 0)).isEqualTo(draft);
        SpeedRule active = draft.activate(NOW);
        assertThat(rules.save(active, 1)).isEqualTo(active);
        assertThat(rules.findActiveRouteRule(tenant, route, "route-v7")).contains(active);
        assertThat(rules.findActiveRouteRule(UUID.randomUUID(), route, "route-v7")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT threshold_kph FROM tracking_speed_rule WHERE id=?",
                BigDecimal.class, draft.id())).isEqualByComparingTo("67.125");
        assertThatThrownBy(() -> rules.save(active, 1)).isInstanceOfSatisfying(
                BusinessRuleException.class,
                error -> assertThat(error.code()).isEqualTo("SPEED_RULE_STALE_VERSION"));
    }

    @Test
    void databaseEnforcesRuleScopeThresholdLifecycleAndActiveUniquenessPerTenant() {
        UUID tenant = UUID.randomUUID();
        SpeedRule fallback = rule(tenant, UUID.randomUUID(), SpeedRule.Scope.TENANT,
                null, null, "80", SpeedRule.Lifecycle.ACTIVE, 1, NOW);
        rules.save(fallback, 0);
        assertThatThrownBy(() -> rules.save(rule(tenant, UUID.randomUUID(), SpeedRule.Scope.TENANT,
                null, null, "90", SpeedRule.Lifecycle.ACTIVE, 1, NOW), 0))
                .isInstanceOf(BusinessRuleException.class);
        rules.save(rule(UUID.randomUUID(), UUID.randomUUID(), SpeedRule.Scope.TENANT,
                null, null, "90", SpeedRule.Lifecycle.ACTIVE, 1, NOW), 0);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tracking_speed_rule(
                 id,tenant_id,name,scope,route_id,threshold_kph,lifecycle,version,effective_at)
                VALUES(?,?,?,'TENANT',?,0,'UNKNOWN',0,now())
                """, UUID.randomUUID(), tenant, "invalid", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void stateRoundTripsCandidateAndSpeedingFactsWithOptimisticConcurrency() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID rule = UUID.randomUUID();
        UUID position = UUID.randomUUID();
        VehicleSpeedState candidate = new VehicleSpeedState(tenant, vehicle,
                VehicleSpeedState.MonitoringState.UNKNOWN, VehicleSpeedState.Availability.AVAILABLE,
                rule, 3, position, NOW, speed("81.375"), 1, null, NOW, position, 0);
        assertThat(states.save(candidate, 0)).isEqualTo(candidate);
        VehicleSpeedState speeding = new VehicleSpeedState(tenant, vehicle,
                VehicleSpeedState.MonitoringState.SPEEDING, VehicleSpeedState.Availability.AVAILABLE,
                rule, 3, null, null, null, 0, UUID.randomUUID(), NOW.plusSeconds(1),
                UUID.randomUUID(), 1);
        assertThat(states.save(speeding, 0)).isEqualTo(speeding);
        assertThat(states.find(UUID.randomUUID(), null, 0, 10)).isEmpty();
        assertThatThrownBy(() -> states.save(speeding, 0)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void concurrentFirstStateCreationConvergesOnOneRow() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        VehicleSpeedState state = VehicleSpeedState.unknown(tenant, vehicle);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> saveStateAfter(barrier, state));
            var second = executor.submit(() -> saveStateAfter(barrier, state));
            assertThat(first.get() + second.get()).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_speed_state WHERE tenant_id=? AND vehicle_id=?
                """, Long.class, tenant, vehicle)).isEqualTo(1L);
    }

    @Test
    void episodeRoundTripsProgressesClosesAndLatestRepeatLookupIsTenantScoped() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID rule = UUID.randomUUID();
        SpeedingEpisode initial = episode(tenant, vehicle, rule, 4, NOW, null, "88.125", 2);
        assertThat(episodes.save(initial)).isEqualTo(initial);
        SpeedingEpisode progressed = copy(initial, null, "92.875", 3);
        assertThat(episodes.save(progressed)).isEqualTo(progressed);
        SpeedingEpisode closed = copy(progressed, NOW.plusSeconds(30), "92.875", 3);
        assertThat(episodes.save(closed)).isEqualTo(closed);
        SpeedingEpisode later = episode(tenant, vehicle, rule, 4, NOW.plusSeconds(60),
                NOW.plusSeconds(90), "95.500", 2);
        episodes.save(later);
        assertThat(episodes.findLatestClosed(tenant, vehicle, rule, 4)).contains(later);
        assertThat(episodes.findLatestClosed(UUID.randomUUID(), vehicle, rule, 4)).isEmpty();
        assertThat(episodes.find(tenant, vehicle, null, NOW.minusSeconds(1),
                NOW.plusSeconds(120), null, 10)).extracting(SpeedingEpisode::id)
                .containsExactly(later.id(), closed.id());
    }

    @Test
    void deterministicEpisodeInsertIsIdempotentAndClosedEvidenceIsDatabaseImmutable() throws Exception {
        SpeedingEpisode episode = episode(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                2, NOW, NOW.plusSeconds(20), "90", 2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> saveEpisodeAfter(barrier, episode));
            var second = executor.submit(() -> saveEpisodeAfter(barrier, episode));
            assertThat(first.get()).isEqualTo(episode);
            assertThat(second.get()).isEqualTo(episode);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_speed_episode WHERE id=?",
                Long.class, episode.id())).isEqualTo(1L);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE tracking_speed_episode SET severity='HIGH' WHERE tenant_id=? AND id=?
                """, episode.tenantId(), episode.id())).isInstanceOf(UncategorizedSQLException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM tracking_speed_episode WHERE id=?", episode.id()))
                .isInstanceOf(UncategorizedSQLException.class);
    }

    @Test
    void jobsAreIdempotentLeasedRetryableRecoverableAndOwnerQualified() {
        Position position = position("job");
        SpeedEvaluationJob enqueued = jobs.enqueue(position.tenant(), position.id(), position.vehicle(), NOW, NOW);
        assertThat(jobs.enqueue(position.tenant(), position.id(), position.vehicle(), NOW, NOW))
                .isEqualTo(enqueued);
        SpeedEvaluationJob claimed = jobs.claimDue("worker-a", NOW, NOW.plusSeconds(10), 1).getFirst();
        assertThat(claimed.status()).isEqualTo(SpeedEvaluationJob.Status.PROCESSING);
        assertThat(claimed.attemptCount()).isEqualTo(1);
        assertThat(jobs.renew(position.tenant(), position.id(), "worker-b", NOW,
                NOW.plusSeconds(20))).isFalse();
        assertThat(jobs.renew(position.tenant(), position.id(), "worker-a", NOW,
                NOW.plusSeconds(20))).isTrue();
        jobs.retry(position.tenant(), position.id(), "worker-a", NOW, NOW.plusSeconds(2));
        jobs.claimDue("worker-b", NOW.plusSeconds(2), NOW.plusSeconds(12), 1);
        assertThatThrownBy(() -> jobs.complete(position.tenant(), position.id(), "worker-a",
                NOW.plusSeconds(3))).isInstanceOfSatisfying(BusinessRuleException.class,
                error -> assertThat(error.code()).isEqualTo("SPEED_JOB_STALE_LEASE"));
        jdbc.update("UPDATE tracking_speed_evaluation_job SET lease_until=? WHERE tenant_id=? AND position_id=?",
                timestamp(NOW.plusSeconds(3)), position.tenant(), position.id());
        assertThat(jobs.claimDue("worker-c", NOW.plusSeconds(4), NOW.plusSeconds(14), 1)).hasSize(1);
        jobs.complete(position.tenant(), position.id(), "worker-c", NOW.plusSeconds(5));
        assertThat(jobs.find(position.tenant(), position.id()).orElseThrow().status())
                .isEqualTo(SpeedEvaluationJob.Status.COMPLETED);
        assertThat(jobs.find(UUID.randomUUID(), position.id())).isEmpty();
    }

    @Test
    void twoWorkersUseSkipLockedToClaimDistinctJobs() throws Exception {
        Position base = position("claims");
        for (int index = 0; index < 7; index++) {
            Position item = positionFrom(base, "claim-" + index, NOW.plusMillis(index));
            jobs.enqueue(item.tenant(), item.id(), item.vehicle(), item.sourceTime(), NOW);
        }
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> claimAfter(barrier, "one", 3));
            var second = executor.submit(() -> claimAfter(barrier, "two", 3));
            List<UUID> firstIds = first.get();
            List<UUID> secondIds = second.get();
            assertThat(firstIds).hasSize(3).doesNotContainAnyElementsOf(secondIds);
            assertThat(secondIds).hasSize(3);
        }
    }

    @Test
    void realisticPlansUseGlobalClaimRuleStateRepeatAndHistoryIndexes() {
        Position base = position("plans");
        seedPlans(base, 5_000);
        jdbc.execute("ANALYZE tracking_speed_rule");
        jdbc.execute("ANALYZE tracking_speed_state");
        jdbc.execute("ANALYZE tracking_speed_episode");
        jdbc.execute("ANALYZE tracking_speed_evaluation_job");

        assertPlanContains("""
                SELECT tenant_id,position_id FROM tracking_speed_evaluation_job
                WHERE next_attempt_at<=? AND (status IN('PENDING','FAILED')
                  OR (status='PROCESSING' AND lease_until<=?))
                ORDER BY next_attempt_at,tenant_id,position_id FOR UPDATE SKIP LOCKED LIMIT 16
                """, "idx_tracking_speed_job_global_due", timestamp(NOW.plusSeconds(10)),
                timestamp(NOW.plusSeconds(10)));
        assertPlanContains("""
                SELECT * FROM tracking_speed_rule WHERE tenant_id=? AND route_id=?
                  AND route_version=? AND lifecycle='ACTIVE'
                """, "idx_tracking_speed_rule_route_lookup", base.tenant(), base.vehicle(), "v1");
        assertPlanContains("SELECT * FROM tracking_speed_state WHERE tenant_id=? AND vehicle_id=?",
                "tracking_speed_state_pkey", base.tenant(), base.vehicle());
        assertPlanContains("""
                SELECT * FROM tracking_speed_episode WHERE tenant_id=? AND vehicle_id=?
                  AND rule_id=? AND rule_version=? AND end_source_timestamp IS NOT NULL
                ORDER BY end_source_timestamp DESC,id DESC LIMIT 1
                """, "idx_tracking_speed_episode_repeat", base.tenant(), base.vehicle(),
                base.device(), 1L);
        assertPlanContains("""
                SELECT * FROM tracking_speed_episode WHERE tenant_id=? AND vehicle_id=?
                ORDER BY start_source_timestamp DESC,id DESC LIMIT 10
                """, "idx_tracking_speed_episode_vehicle_history", base.tenant(), base.vehicle());
    }

    private int saveStateAfter(CyclicBarrier barrier, VehicleSpeedState state) throws Exception {
        barrier.await();
        try { states.save(state, 0); return 1; } catch (BusinessRuleException exception) { return 0; }
    }

    private SpeedingEpisode saveEpisodeAfter(CyclicBarrier barrier, SpeedingEpisode episode) throws Exception {
        barrier.await();
        return episodes.save(episode);
    }

    private List<UUID> claimAfter(CyclicBarrier barrier, String owner, int limit) throws Exception {
        barrier.await();
        return jobs.claimDue(owner, NOW.plusSeconds(1), NOW.plusSeconds(31), limit).stream()
                .map(SpeedEvaluationJob::positionId).toList();
    }

    private Position position(String suffix) {
        UUID tenant = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,lifecycle,
                 registered_at,registered_by,version,created_at,updated_at)
                VALUES(?,?,?,'FIXTURE','ACTIVE',?, ?,0,?,?)
                """, device, tenant, "speed-" + suffix, timestamp(NOW), UUID.randomUUID(),
                timestamp(NOW), timestamp(NOW));
        return positionFrom(new Position(tenant, device, vehicle, null, NOW), suffix, NOW);
    }

    private Position positionFrom(Position base, String suffix, Instant sourceTime) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_position(id,tenant_id,device_id,vehicle_id,provider_alias,
                 dedupe_identity,payload_hash,source_timestamp,received_at,latitude,longitude,speed_kph,
                 engine_state,trust,quality,ordering_classification,retention_policy,retention_policy_version)
                VALUES(?,?,?,?, 'FIXTURE',?,?, ?,?,6.5,79.5,80,'UNKNOWN','TRUSTED','GOOD','IN_ORDER','TEST','1')
                """, id, base.tenant(), base.device(), base.vehicle(), hex(id), hex(UUID.randomUUID()),
                timestamp(sourceTime), timestamp(sourceTime));
        return new Position(base.tenant(), base.device(), base.vehicle(), id, sourceTime);
    }

    private void seedPlans(Position base, int count) {
        jdbc.update("""
                WITH generated AS (SELECT value,gen_random_uuid() position_id
                  FROM generate_series(1,?) value), positions AS (
                 INSERT INTO tracking_position(id,tenant_id,device_id,vehicle_id,provider_alias,
                  dedupe_identity,payload_hash,source_timestamp,received_at,latitude,longitude,speed_kph,
                  engine_state,trust,quality,ordering_classification,retention_policy,retention_policy_version)
                 SELECT position_id,?, ?,gen_random_uuid(),'FIXTURE','speed-plan-'||position_id,
                  repeat('d',64),?, ?,6.5,79.5,80,'UNKNOWN','TRUSTED','GOOD','IN_ORDER','TEST','1'
                 FROM generated RETURNING tenant_id,id,vehicle_id,source_timestamp)
                INSERT INTO tracking_speed_evaluation_job(tenant_id,position_id,vehicle_id,source_timestamp,
                 status,attempt_count,next_attempt_at,created_at,updated_at)
                SELECT tenant_id,id,vehicle_id,source_timestamp,'PENDING',0,
                 CAST(? AS timestamptz)-(row_number() OVER())*interval '1 millisecond',?,? FROM positions
                """, count, base.tenant(), base.device(), timestamp(NOW), timestamp(NOW),
                timestamp(NOW), timestamp(NOW), timestamp(NOW));
        jdbc.update("""
                INSERT INTO tracking_speed_rule(id,tenant_id,name,scope,route_id,route_version,
                 threshold_kph,lifecycle,version,effective_at)
                SELECT gen_random_uuid(),?,'Plan '||value,'ROUTE_VERSION',
                 CASE WHEN value=1 THEN ? ELSE gen_random_uuid() END,'v1',80,
                 CASE WHEN value=1 THEN 'ACTIVE' ELSE 'DRAFT' END,1,
                 CASE WHEN value=1 THEN CAST(? AS timestamptz) ELSE NULL END
                 FROM generate_series(1,?) value
                """, base.tenant(), base.vehicle(), timestamp(NOW), count);
        jdbc.update("""
                INSERT INTO tracking_speed_state(tenant_id,vehicle_id,state,availability,version)
                SELECT ?,CASE WHEN value=1 THEN ? ELSE gen_random_uuid() END,'NORMAL','AVAILABLE',0
                FROM generate_series(1,?) value
                """, base.tenant(), base.vehicle(), count);
        jdbc.update("""
                INSERT INTO tracking_speed_episode(id,tenant_id,vehicle_id,rule_id,rule_version,
                 threshold_source,effective_threshold_kph,start_source_timestamp,
                 confirmation_source_timestamp,end_source_timestamp,max_observed_speed_kph,
                 eligible_above_threshold_sample_count,severity,repeat_count,
                 first_candidate_position_id,confirming_position_id)
                SELECT gen_random_uuid(),?,CASE WHEN value<=10 THEN ? ELSE gen_random_uuid() END,
                 CASE WHEN value<=10 THEN ? ELSE gen_random_uuid() END,1,'TENANT_CONFIG',80,
                 CAST(? AS timestamptz)-(value*interval '1 second'),
                 CAST(? AS timestamptz)-(value*interval '1 second'),
                 CAST(? AS timestamptz)-(value*interval '1 second'),
                 90,2,'WARNING',0,gen_random_uuid(),gen_random_uuid()
                FROM generate_series(1,?) value
                """, base.tenant(), base.vehicle(), base.device(), timestamp(NOW), timestamp(NOW),
                timestamp(NOW), count);
    }

    private void assertPlanContains(String sql, String index, Object... arguments) {
        String plan = String.join("\n", jdbc.queryForList(
                "EXPLAIN (ANALYZE,COSTS OFF) " + sql, String.class, arguments));
        System.out.println("US50_V81_PLAN " + index + "\n" + plan);
        assertThat(plan).contains(index);
    }

    private List<String> speedTables() {
        return jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema='public' AND table_name LIKE 'tracking_speed_%'
                """, String.class);
    }

    private List<String> speedIndexes() {
        return jdbc.queryForList("""
                SELECT indexname FROM pg_indexes
                WHERE schemaname='public' AND tablename LIKE 'tracking_speed_%'
                """, String.class);
    }

    private static SpeedRule rule(UUID tenant, UUID id, SpeedRule.Scope scope, UUID route,
                                  String routeVersion, String threshold, SpeedRule.Lifecycle lifecycle,
                                  long version, Instant effectiveAt) {
        return new SpeedRule(id, tenant, "Rule " + id, scope, route, routeVersion, speed(threshold),
                lifecycle, version, effectiveAt);
    }

    private static SpeedingEpisode episode(UUID tenant, UUID vehicle, UUID rule, long ruleVersion,
                                            Instant start, Instant end, String maximum, int count) {
        UUID first = UUID.randomUUID();
        UUID id = com.transportlogistics.app.tracking.domain.speed.SpeedingEpisodeIdentity.create(
                tenant, vehicle, rule, ruleVersion, first);
        return new SpeedingEpisode(id, tenant, vehicle, SpeedAttribution.unknown(), rule, ruleVersion,
                ResolvedSpeedThreshold.ThresholdSource.TENANT_CONFIG, speed("80"), start,
                start.plusSeconds(1), end, first, UUID.randomUUID(), speed(maximum), count,
                SpeedingEpisode.Severity.WARNING, 0);
    }

    private static SpeedingEpisode copy(SpeedingEpisode episode, Instant end, String maximum, int count) {
        return new SpeedingEpisode(episode.id(), episode.tenantId(), episode.vehicleId(),
                episode.attribution(), episode.ruleId(), episode.ruleVersion(), episode.thresholdSource(),
                episode.effectiveThresholdKph(), episode.startSourceTimestamp(),
                episode.confirmationSourceTimestamp(), end, episode.firstPositionId(),
                episode.confirmingPositionId(), speed(maximum), count, episode.severity(),
                episode.repeatCount());
    }

    private static SpeedKph speed(String value) { return new SpeedKph(new BigDecimal(value)); }
    private static Timestamp timestamp(Instant value) { return Timestamp.from(value); }
    private static String hex(UUID value) {
        return value.toString().replace("-", "") + value.toString().replace("-", "");
    }

    private record Position(UUID tenant, UUID device, UUID vehicle, UUID id, Instant sourceTime) { }
}
