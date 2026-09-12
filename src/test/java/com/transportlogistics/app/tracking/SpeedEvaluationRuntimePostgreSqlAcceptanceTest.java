package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.tracking.application.SpeedEvaluationService;
import com.transportlogistics.app.tracking.domain.speed.SpeedAttribution;
import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationResult;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedPosition;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import com.transportlogistics.app.tracking.ports.outbound.SpeedAttributionLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedRuleRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodePublisherPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleSpeedStateRepositoryPort;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = {com.transportlogistics.app.TransportLogisticsApplication.class,
        SpeedEvaluationRuntimePostgreSqlAcceptanceTest.Stubs.class})
class SpeedEvaluationRuntimePostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final Instant BASE = Instant.parse("2026-09-12T02:00:00Z");
    @Autowired SpeedEvaluationService service;
    @Autowired SpeedRuleRepositoryPort rules;
    @Autowired VehicleSpeedStateRepositoryPort states;
    @Autowired SpeedingEpisodeRepositoryPort episodes;
    @Autowired CapturingPublisher publisher;
    @Autowired MutableAttribution attribution;
    @Autowired JdbcTemplate jdbc;
    private UUID tenant;
    private UUID vehicle;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM tracking_speed_evaluation_job");
        jdbc.update("DELETE FROM tracking_speed_state");
        jdbc.update("DELETE FROM tracking_speed_episode");
        jdbc.update("DELETE FROM tracking_speed_rule");
        publisher.events.clear();
        attribution.value = SpeedAttribution.unknown();
        tenant = UUID.randomUUID();
        vehicle = UUID.randomUUID();
    }

    @Test
    void twoSamplesConfirmProgressClearAndPublishOnlyOnce() {
        SpeedRule rule = activeTenantRule("60", 1);
        rules.save(rule, 0);
        assertThat(service.evaluate(position(vehicle, 1, "70"), BASE.plusSeconds(2)).outcome())
                .isEqualTo(SpeedEvaluationResult.Outcome.CANDIDATE_UPDATED);
        assertThat(service.evaluate(position(vehicle, 2, "75"), BASE.plusSeconds(3)).outcome())
                .isEqualTo(SpeedEvaluationResult.Outcome.EPISODE_CONFIRMED);
        assertThat(service.evaluate(position(vehicle, 3, "80"), BASE.plusSeconds(4)).outcome())
                .isEqualTo(SpeedEvaluationResult.Outcome.EPISODE_PROGRESSED);
        assertThat(service.evaluate(position(vehicle, 4, "60"), BASE.plusSeconds(5)).outcome())
                .isEqualTo(SpeedEvaluationResult.Outcome.EPISODE_CLOSED);
        assertThat(publisher.events).hasSize(1);
        SpeedingEpisode stored = episodes.findLatestClosed(tenant, vehicle, rule.id(), 1).orElseThrow();
        assertThat(stored.maxObservedSpeedKph()).isEqualTo(new SpeedKph(new BigDecimal("80")));
        assertThat(stored.eligibleAboveThresholdSampleCount()).isEqualTo(3);
    }

    @Test
    void simultaneousConfirmationConvergesToOneEpisodeAndPublication() throws Exception {
        SpeedRule rule = activeTenantRule("60", 1);
        rules.save(rule, 0);
        service.evaluate(position(vehicle, 1, "70"), BASE.plusSeconds(2));
        SpeedPosition confirming = position(vehicle, 2, "75");
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Callable<SpeedEvaluationResult>> work = List.of(
                    () -> service.evaluate(confirming, BASE.plusSeconds(3)),
                    () -> service.evaluate(confirming, BASE.plusSeconds(3)));
            List<SpeedEvaluationResult.Outcome> outcomes = pool.invokeAll(work).stream()
                    .map(future -> {
                        try { return future.get().outcome(); }
                        catch (Exception exception) { throw new IllegalStateException(exception); }
                    }).toList();
            assertThat(outcomes).containsExactlyInAnyOrder(
                    SpeedEvaluationResult.Outcome.EPISODE_CONFIRMED,
                    SpeedEvaluationResult.Outcome.NO_OP);
        }
        assertThat(publisher.events).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_speed_episode", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void routeRulePrecedesFallbackAndAttributionIsSnapshotted() {
        SpeedRule fallback = activeTenantRule("80", 1);
        UUID routeId = UUID.randomUUID();
        SpeedRule route = new SpeedRule(UUID.randomUUID(), tenant, "Route", SpeedRule.Scope.ROUTE_VERSION,
                routeId, "route-v2", new SpeedKph(new BigDecimal("50")),
                SpeedRule.Lifecycle.ACTIVE, 1, BASE);
        rules.save(fallback, 0);
        rules.save(route, 0);
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        attribution.value = new SpeedAttribution(tripId, driverId, routeId, "route-v2");
        service.evaluate(position(vehicle, 1, "60"), BASE.plusSeconds(2));
        SpeedEvaluationResult confirmed = service.evaluate(position(vehicle, 2, "65"), BASE.plusSeconds(3));
        attribution.value = SpeedAttribution.unknown();
        SpeedingEpisode episode = confirmed.episode().orElseThrow();
        assertThat(episode.ruleId()).isEqualTo(route.id());
        assertThat(episode.attribution().tripId()).isEqualTo(tripId);
        assertThat(episodes.findEpisode(tenant, episode.id()).orElseThrow().attribution().driverId())
                .isEqualTo(driverId);
    }

    @Test
    void repeatAtInclusiveTenMinutesIsHighAndOutsideWindowResets() {
        SpeedRule rule = activeTenantRule("60", 1);
        rules.save(rule, 0);
        SpeedingEpisode first = confirmAndClose(vehicle, 1, BASE);
        Instant repeatBase = first.endSourceTimestamp().plusSeconds(600);
        SpeedingEpisode repeat = confirmAndClose(vehicle, 10, repeatBase.minusSeconds(1));
        assertThat(repeat.severity()).isEqualTo(SpeedingEpisode.Severity.HIGH);
        assertThat(repeat.repeatCount()).isEqualTo(1);
        Instant outside = repeat.endSourceTimestamp().plusSeconds(601);
        SpeedingEpisode independent = confirmAndClose(vehicle, 20, outside.minusSeconds(1));
        assertThat(independent.severity()).isEqualTo(SpeedingEpisode.Severity.WARNING);
        assertThat(independent.repeatCount()).isZero();
    }

    @Test
    void staleAndConfigurationUnavailableCannotCreateEpisode() {
        assertThat(service.evaluate(position(vehicle, 1, "70"), BASE.plusSeconds(2)).outcome())
                .isEqualTo(SpeedEvaluationResult.Outcome.CONFIGURATION_UNAVAILABLE);
        rules.save(activeTenantRule("60", 1), 0);
        assertThat(service.evaluate(position(vehicle, 2, "70"), BASE.plusSeconds(303)).outcome())
                .isEqualTo(SpeedEvaluationResult.Outcome.NO_OP);
        assertThat(publisher.events).isEmpty();
    }

    private SpeedingEpisode confirmAndClose(UUID targetVehicle, int sequence, Instant base) {
        SpeedEvaluationResult first = service.evaluate(
                position(targetVehicle, sequence, "70", base.plusSeconds(1)), base.plusSeconds(2));
        SpeedEvaluationResult confirmed = service.evaluate(
                position(targetVehicle, sequence + 1, "75", base.plusSeconds(2)), base.plusSeconds(3));
        service.evaluate(position(targetVehicle, sequence + 2, "55", base.plusSeconds(3)),
                base.plusSeconds(4));
        assertThat(first.outcome()).isEqualTo(SpeedEvaluationResult.Outcome.CANDIDATE_UPDATED);
        SpeedingEpisode created = confirmed.episode().orElseThrow();
        return episodes.findEpisode(tenant, created.id()).orElseThrow();
    }

    private SpeedRule activeTenantRule(String threshold, long version) {
        return new SpeedRule(UUID.randomUUID(), tenant, "Tenant", SpeedRule.Scope.TENANT, null, null,
                new SpeedKph(new BigDecimal(threshold)), SpeedRule.Lifecycle.ACTIVE, version, BASE);
    }

    private SpeedPosition position(UUID targetVehicle, int sequence, String speed) {
        return position(targetVehicle, sequence, speed, BASE.plusSeconds(sequence));
    }

    private SpeedPosition position(UUID targetVehicle, int sequence, String speed, Instant sourceTime) {
        return new SpeedPosition(tenant, targetVehicle, new UUID(0, sequence), sourceTime,
                new SpeedKph(new BigDecimal(speed)), false, true, true, true);
    }

    @TestConfiguration
    static class Stubs {
        @Bean @Primary CapturingPublisher capturingSpeedPublisher() { return new CapturingPublisher(); }
        @Bean @Primary MutableAttribution speedAttributionFixture() { return new MutableAttribution(); }
    }

    static final class CapturingPublisher implements SpeedingEpisodePublisherPort {
        final List<VehicleSpeedingDetectedV1> events = java.util.Collections.synchronizedList(new ArrayList<>());
        @Override public void publish(UUID tenantId, VehicleSpeedingDetectedV1 event) { events.add(event); }
    }

    static final class MutableAttribution implements SpeedAttributionLookupPort {
        volatile SpeedAttribution value = SpeedAttribution.unknown();
        @Override public Optional<SpeedAttribution> findAt(UUID tenantId, UUID vehicleId, Instant sourceTimestamp) {
            return Optional.ofNullable(value);
        }
    }
}
