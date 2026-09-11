package com.transportlogistics.app.tracking.domain.speed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpeedingEpisodeTest {
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID VEHICLE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_VEHICLE = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID RULE = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID FIRST = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Instant BASE = Instant.parse("2026-09-11T10:00:00Z");

    @Test
    void deterministicIdentityIsStableAndSensitiveToEveryComponent() {
        UUID first = SpeedingEpisodeIdentity.create(TENANT, VEHICLE, RULE, 1, FIRST);
        assertEquals(first, SpeedingEpisodeIdentity.create(TENANT, VEHICLE, RULE, 1, FIRST));
        assertNotEquals(first, SpeedingEpisodeIdentity.create(UUID.randomUUID(), VEHICLE, RULE, 1, FIRST));
        assertNotEquals(first, SpeedingEpisodeIdentity.create(TENANT, OTHER_VEHICLE, RULE, 1, FIRST));
        assertNotEquals(first, SpeedingEpisodeIdentity.create(TENANT, VEHICLE, UUID.randomUUID(), 1, FIRST));
        assertNotEquals(first, SpeedingEpisodeIdentity.create(TENANT, VEHICLE, RULE, 2, FIRST));
        assertNotEquals(first, SpeedingEpisodeIdentity.create(TENANT, VEHICLE, RULE, 1, UUID.randomUUID()));
    }

    @Test
    void repeatAtTenMinuteBoundaryIsHighAndIncrementsCount() {
        SpeedingEpisode prior = closedEpisode(VEHICLE, RULE, 1, BASE, 2);
        SpeedingEpisode repeat = confirm(VEHICLE, RULE, 1, BASE.plusSeconds(660), prior);
        assertEquals(SpeedingEpisode.Severity.HIGH, repeat.severity());
        assertEquals(3, repeat.repeatCount());
    }

    @Test
    void outsideWindowDifferentVehicleOrRuleStartsWarningChain() {
        SpeedingEpisode prior = closedEpisode(VEHICLE, RULE, 1, BASE, 1);
        assertWarning(confirm(VEHICLE, RULE, 1, BASE.plusSeconds(661), prior));
        assertWarning(confirm(OTHER_VEHICLE, RULE, 1, BASE.plusSeconds(660), prior));
        assertWarning(confirm(VEHICLE, UUID.randomUUID(), 1, BASE.plusSeconds(660), prior));
        assertWarning(confirm(VEHICLE, RULE, 2, BASE.plusSeconds(660), prior));
    }

    private static void assertWarning(SpeedingEpisode episode) {
        assertEquals(SpeedingEpisode.Severity.WARNING, episode.severity());
        assertEquals(0, episode.repeatCount());
    }

    private static SpeedingEpisode closedEpisode(UUID vehicle, UUID ruleId, long version,
                                                  Instant start, int repeatCount) {
        SpeedingEpisode episode = confirm(vehicle, ruleId, version, start, null);
        SpeedPosition close = position(vehicle, UUID.randomUUID(), start.plusSeconds(60), "50");
        SpeedingEpisode closed = episode.close(close);
        return new SpeedingEpisode(closed.id(), closed.tenantId(), closed.vehicleId(), closed.attribution(),
                closed.ruleId(), closed.ruleVersion(), closed.thresholdSource(),
                closed.effectiveThresholdKph(), closed.startSourceTimestamp(),
                closed.confirmationSourceTimestamp(), closed.endSourceTimestamp(), closed.firstPositionId(),
                closed.confirmingPositionId(), closed.maxObservedSpeedKph(),
                closed.eligibleAboveThresholdSampleCount(), repeatCount == 0
                ? SpeedingEpisode.Severity.WARNING : SpeedingEpisode.Severity.HIGH, repeatCount);
    }

    private static SpeedingEpisode confirm(UUID vehicle, UUID ruleId, long version,
                                           Instant start, SpeedingEpisode previous) {
        SpeedRule rule = new SpeedRule(ruleId, TENANT, "rule", SpeedRule.Scope.TENANT,
                null, null, speed("60"), SpeedRule.Lifecycle.ACTIVE, version, start);
        ResolvedSpeedThreshold threshold = new ResolvedSpeedThreshold(rule,
                ResolvedSpeedThreshold.ThresholdSource.TENANT_CONFIG);
        return SpeedingEpisode.confirm(TENANT, vehicle, threshold, SpeedAttribution.unknown(),
                FIRST, start, speed("70"), position(vehicle, UUID.randomUUID(),
                        start.plusSeconds(1), "75"), previous);
    }

    private static SpeedPosition position(UUID vehicle, UUID id, Instant time, String value) {
        return new SpeedPosition(TENANT, vehicle, id, time, speed(value), false, true, true, true);
    }

    private static SpeedKph speed(String value) {
        return new SpeedKph(new BigDecimal(value));
    }
}
