package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.application.RouteDeviationManagementService;
import com.transportlogistics.app.tracking.domain.routedeviation.DistanceMeters;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationReview;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteVersion;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationReviewUseCase;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationRuleManagementUseCase;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEpisodeRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class RouteDeviationManagementPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired RouteDeviationManagementService service;
    @Autowired RouteDeviationEpisodeRepositoryPort episodes;
    @Autowired JdbcTemplate jdbc;

    @Test
    void ruleCommandsAreTenantScopedIdempotentVersionedAndAudited() {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        var context = ruleContext(tenant, actor);
        RouteDeviationRule created = service.create(context, UUID.randomUUID(), "REVISION:1",
                new BigDecimal("150.000"), "create-rule");

        assertThat(service.create(context, created.routeId(), "REVISION:1",
                new BigDecimal("150.000"), "create-rule")).isEqualTo(created);
        assertThat(service.rule(UUID.randomUUID(), created.id())).isEmpty();
        RouteDeviationRule updated = service.update(context, created.id(), 0,
                new BigDecimal("175.000"), "update-rule");
        RouteDeviationRule active = service.activate(context, created.id(), 1, "activate-rule");

        assertThat(updated.manageVersion()).isEqualTo(1);
        assertThat(active.lifecycle()).isEqualTo(RouteDeviationRule.Lifecycle.ACTIVE);
        assertThat(active.ruleVersion()).isEqualTo(1);
        assertThatThrownBy(() -> service.disable(context, created.id(), 1,
                "planned change", "stale-rule")).isInstanceOf(ConflictException.class);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_audit_event
                WHERE tenant_id=? AND target_id=? AND action IN (
                  'ROUTE_DEVIATION_RULE_CREATED','ROUTE_DEVIATION_RULE_UPDATE',
                  'ROUTE_DEVIATION_RULE_ACTIVATE')
                """, Integer.class, tenant, created.id())).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_audit_event WHERE tenant_id=? AND target_id=?
                  AND (safe_detail ~ '[-]?[0-9]+[.][0-9]+[,;][-]?[0-9]+[.][0-9]+'
                       OR lower(safe_detail) LIKE '%token%')
                """, Integer.class, tenant, created.id())).isZero();
    }

    @Test
    void ruleBoundariesAndBoundedDeterministicEpisodeQueriesAreEnforced() {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        var context = ruleContext(tenant, actor);
        assertThat(service.create(context, UUID.randomUUID(), "REVISION:10",
                new BigDecimal("10"), "lower-bound").configuredTolerance().value())
                .isEqualByComparingTo("10");
        assertThat(service.create(context, UUID.randomUUID(), "REVISION:11",
                new BigDecimal("5000"), "upper-bound").configuredTolerance().value())
                .isEqualByComparingTo("5000");
        assertThatThrownBy(() -> service.create(context, UUID.randomUUID(), "REVISION:12",
                new BigDecimal("9.999"), "below-bound"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.create(context, UUID.randomUUID(), "REVISION:13",
                new BigDecimal("5000.001"), "above-bound"))
                .isInstanceOf(BusinessRuleException.class);

        UUID vehicle = UUID.randomUUID();
        RouteDeviationEpisode newest = highEpisode(tenant, vehicle,
                Instant.parse("2026-09-14T02:00:00Z"));
        RouteDeviationEpisode older = highEpisode(tenant, vehicle,
                Instant.parse("2026-09-14T01:00:00Z")).close(
                        Instant.parse("2026-09-14T01:05:00Z"),
                        RouteDeviationEpisode.TerminalOutcome.RETURNED_TO_ROUTE);
        episodes.save(older);
        episodes.save(newest);
        var first = service.episodes(tenant, vehicle, null, null,
                RouteDeviationEpisode.Severity.HIGH, null,
                Instant.parse("2026-09-14T00:00:00Z"),
                Instant.parse("2026-09-15T00:00:00Z"), null, 1);
        var second = service.episodes(tenant, vehicle, null, null,
                RouteDeviationEpisode.Severity.HIGH, null,
                Instant.parse("2026-09-14T00:00:00Z"),
                Instant.parse("2026-09-15T00:00:00Z"), first.nextCursor(), 1);

        assertThat(first.items()).extracting(RouteDeviationEpisode::id).containsExactly(newest.id());
        assertThat(second.items()).extracting(RouteDeviationEpisode::id).containsExactly(older.id());
        assertThat(service.episodes(UUID.randomUUID(), vehicle, null, null, null, null,
                Instant.parse("2026-09-14T00:00:00Z"),
                Instant.parse("2026-09-15T00:00:00Z"), null, 100).items()).isEmpty();
        assertThatThrownBy(() -> service.episodes(tenant, null, null, null, null, null,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-15T00:00:00Z"), null, 100))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void highEpisodeReviewIsImmutableCorrectableByDifferentActorAndIdempotent() {
        UUID tenant = UUID.randomUUID();
        UUID firstActor = UUID.randomUUID();
        UUID secondActor = UUID.randomUUID();
        RouteDeviationEpisode episode = highEpisode(tenant);
        episodes.save(episode);

        RouteDeviationReview approved = service.approve(reviewContext(tenant, firstActor),
                episode.id(), 0, RouteDeviationReview.Reason.ROAD_CLOSURE, null, "approve-once");
        assertThat(service.approve(reviewContext(tenant, firstActor), episode.id(), 0,
                RouteDeviationReview.Reason.ROAD_CLOSURE, null, "approve-once"))
                .isEqualTo(approved);
        assertThatThrownBy(() -> service.correct(reviewContext(tenant, firstActor), episode.id(), 1,
                RouteDeviationReview.Status.REJECTED, RouteDeviationReview.Reason.UNKNOWN,
                "Incorrect first review", "self-reversal"))
                .isInstanceOf(ConflictException.class);

        RouteDeviationReview corrected = service.correct(reviewContext(tenant, secondActor),
                episode.id(), 1, RouteDeviationReview.Status.REJECTED,
                RouteDeviationReview.Reason.UNKNOWN, "Incorrect first review", "correction");

        assertThat(corrected.reviewVersion()).isEqualTo(2);
        assertThat(corrected.compensatesReviewId()).isEqualTo(approved.id());
        assertThat(service.reviews(tenant, episode.id(), 100)).hasSize(2);
        assertThat(service.episode(tenant, episode.id()).orElseThrow().reviewStatus())
                .isEqualTo(RouteDeviationReview.Status.REJECTED);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_audit_event WHERE tenant_id=? AND target_id=?
                  AND action IN ('ROUTE_DEVIATION_REVIEW_APPROVED',
                                 'ROUTE_DEVIATION_REVIEW_CORRECTED')
                """, Integer.class, tenant, episode.id())).isEqualTo(2);
    }

    @Test
    void concurrentReviewAcceptsExactlyOneFirstDecision() throws Exception {
        UUID tenant = UUID.randomUUID();
        RouteDeviationEpisode episode = highEpisode(tenant);
        episodes.save(episode);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> decideAfter(start, tenant, UUID.randomUUID(),
                    episode.id(), "concurrent-a"));
            var second = executor.submit(() -> decideAfter(start, tenant, UUID.randomUUID(),
                    episode.id(), "concurrent-b"));
            start.countDown();
            assertThat(java.util.List.of(first.get(), second.get()).stream()
                    .filter(Boolean::booleanValue).count()).isEqualTo(1);
        }
        assertThat(service.reviews(tenant, episode.id(), 100)).hasSize(1);
    }

    private boolean decideAfter(CountDownLatch start, UUID tenant, UUID actor,
            UUID episode, String key) throws InterruptedException {
        start.await();
        try {
            service.reject(reviewContext(tenant, actor), episode, 0,
                    RouteDeviationReview.Reason.OPERATIONAL_NECESSITY, null, key);
            return true;
        } catch (ConflictException exception) {
            return false;
        }
    }

    private static RouteDeviationEpisode highEpisode(UUID tenant) {
        return highEpisode(tenant, UUID.randomUUID(), Instant.parse("2026-09-14T00:00:00Z"));
    }

    private static RouteDeviationEpisode highEpisode(UUID tenant, UUID vehicle, Instant started) {
        return new RouteDeviationEpisode(UUID.randomUUID(), tenant, vehicle,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new RouteVersion("REVISION:1"),
                UUID.randomUUID(), 1, new DistanceMeters(new BigDecimal("100")),
                new DistanceMeters(new BigDecimal("100")), UUID.randomUUID(), UUID.randomUUID(),
                started, started.plusSeconds(30), null, new DistanceMeters(new BigDecimal("250")),
                2, RouteDeviationEpisode.Severity.HIGH, RouteDeviationReview.Status.PENDING,
                0, null, null, true, false);
    }

    private static RouteDeviationRuleManagementUseCase.Context ruleContext(UUID tenant, UUID actor) {
        return new RouteDeviationRuleManagementUseCase.Context(tenant, actor, "rule-test",
                Instant.parse("2026-09-14T01:00:00Z"));
    }

    private static RouteDeviationReviewUseCase.Context reviewContext(UUID tenant, UUID actor) {
        return new RouteDeviationReviewUseCase.Context(tenant, actor, "review-test",
                Instant.parse("2026-09-14T02:00:00Z"));
    }
}
