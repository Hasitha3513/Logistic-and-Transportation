package com.transportlogistics.app.tracking.domain.speed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpeedRuleTest {
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ROUTE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");

    @Test
    void validatesSpeedAndThresholdBounds() {
        assertEquals(0, new SpeedKph(BigDecimal.ZERO).value().compareTo(BigDecimal.ZERO));
        assertEquals(0, new SpeedKph(new BigDecimal("400")).value().compareTo(new BigDecimal("400")));
        assertThrows(SpeedMonitoringException.class, () -> new SpeedKph(new BigDecimal("-0.01")));
        assertThrows(SpeedMonitoringException.class, () -> new SpeedKph(new BigDecimal("400.01")));
        assertThrows(SpeedMonitoringException.class, () -> SpeedKph.threshold(BigDecimal.ZERO));
    }

    @Test
    void validatesTenantAndRouteScopes() {
        assertEquals(SpeedRule.Scope.TENANT, tenant(SpeedRule.Lifecycle.DRAFT).scope());
        assertEquals(ROUTE, route(SpeedRule.Lifecycle.DRAFT, "R1").routeId());
        assertThrows(SpeedMonitoringException.class, () -> new SpeedRule(UUID.randomUUID(), TENANT,
                "bad", SpeedRule.Scope.TENANT, ROUTE, "R1", speed("60"),
                SpeedRule.Lifecycle.DRAFT, 1, null));
        assertThrows(SpeedMonitoringException.class, () -> new SpeedRule(UUID.randomUUID(), TENANT,
                "bad", SpeedRule.Scope.ROUTE_VERSION, null, null, speed("60"),
                SpeedRule.Lifecycle.DRAFT, 1, null));
    }

    @Test
    void enforcesLifecycleAndEditability() {
        SpeedRule draft = tenant(SpeedRule.Lifecycle.DRAFT);
        assertTrue(draft.editable());
        SpeedRule active = draft.activate(NOW);
        assertFalse(active.editable());
        assertEquals(2, active.ruleVersion());
        assertEquals(SpeedRule.Lifecycle.DISABLED, active.disable().lifecycle());
        assertEquals(3, active.disable().ruleVersion());
        assertEquals(SpeedRule.Lifecycle.RETIRED, active.disable().retire().lifecycle());
        assertThrows(SpeedMonitoringException.class, active::retire);
        SpeedRule retired = draft.retire();
        assertThrows(SpeedMonitoringException.class, () -> retired.activate(NOW));
        assertThrows(SpeedMonitoringException.class, () -> retired.update("new", speed("70"), null, null));
    }

    @Test
    void resolvesRouteBeforeTenantAndIgnoresInactiveOrMismatchedRules() {
        SpeedRule fallback = tenant(SpeedRule.Lifecycle.ACTIVE);
        SpeedRule route = route(SpeedRule.Lifecycle.ACTIVE, "R1");
        SpeedThresholdResolver resolver = new SpeedThresholdResolver();
        SpeedAttribution matching = new SpeedAttribution(UUID.randomUUID(), null, ROUTE, "R1");
        assertEquals(ResolvedSpeedThreshold.ThresholdSource.ROUTE_CONFIG,
                resolver.resolve(TENANT, matching, List.of(fallback, route)).orElseThrow().source());
        assertEquals(ResolvedSpeedThreshold.ThresholdSource.TENANT_CONFIG,
                resolver.resolve(TENANT, SpeedAttribution.unknown(), List.of(fallback, route)).orElseThrow().source());
        assertEquals(ResolvedSpeedThreshold.ThresholdSource.TENANT_CONFIG,
                resolver.resolve(TENANT, new SpeedAttribution(null, null, ROUTE, "R2"),
                        List.of(fallback, route)).orElseThrow().source());
        assertTrue(resolver.resolve(TENANT, matching,
                List.of(route(SpeedRule.Lifecycle.DISABLED, "R1"))).isEmpty());
        assertTrue(resolver.resolve(TENANT, matching,
                List.of(route(SpeedRule.Lifecycle.RETIRED, "R1"))).isEmpty());
    }

    private static SpeedRule tenant(SpeedRule.Lifecycle lifecycle) {
        return new SpeedRule(UUID.fromString("30000000-0000-0000-0000-000000000001"), TENANT,
                "Tenant threshold", SpeedRule.Scope.TENANT, null, null, speed("60"), lifecycle, 1,
                lifecycle == SpeedRule.Lifecycle.ACTIVE ? NOW : null);
    }

    private static SpeedRule route(SpeedRule.Lifecycle lifecycle, String version) {
        return new SpeedRule(UUID.fromString("30000000-0000-0000-0000-000000000002"), TENANT,
                "Route threshold", SpeedRule.Scope.ROUTE_VERSION, ROUTE, version, speed("50"), lifecycle, 2,
                lifecycle == SpeedRule.Lifecycle.ACTIVE ? NOW : null);
    }

    private static SpeedKph speed(String value) {
        return new SpeedKph(new BigDecimal(value));
    }
}
