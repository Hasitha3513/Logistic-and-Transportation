package com.transportlogistics.app.tracking.domain.journeyreplay;

import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReplayQueryPolicyTest {
    private static final Instant NOW = Instant.parse("2026-09-15T12:00:00Z");
    private static final TenantContext TENANT = new TenantContext(UUID.randomUUID(), UUID.randomUUID());

    @Test void acceptsVehicleAndTripSelectorsAndDefaultsVehicleWindow() {
        UUID vehicle = UUID.randomUUID();
        ReplayQuery vehicleQuery = validate(new ReplayRequest(vehicle, null, null, null, null,
                null, Set.of(), false));
        assertThat(vehicleQuery.selector()).isEqualTo(ReplaySelector.vehicle(vehicle));
        assertThat(vehicleQuery.requestedRange()).isEqualTo(new TimeRange(NOW.minusSeconds(21_600), NOW));
        assertThat(vehicleQuery.limit()).isEqualTo(1_000);
        assertThat(vehicleQuery.browserPointCeiling()).isEqualTo(20_000);

        UUID trip = UUID.randomUUID();
        ReplayQuery tripQuery = validate(request(null, trip, NOW.minusSeconds(60), NOW, 2_000));
        assertThat(tripQuery.selector()).isEqualTo(ReplaySelector.trip(trip));
    }

    @Test void rejectsMissingAmbiguousAndTripWithoutExplicitRange() {
        assertError(new ReplayRequest(null, null, NOW.minusSeconds(1), NOW, 1, null, Set.of(), false),
                JourneyReplayError.INVALID_SELECTOR);
        assertError(new ReplayRequest(UUID.randomUUID(), UUID.randomUUID(), NOW.minusSeconds(1), NOW,
                1, null, Set.of(), false), JourneyReplayError.INVALID_SELECTOR);
        assertError(new ReplayRequest(null, UUID.randomUUID(), null, null, 1, null, Set.of(), false),
                JourneyReplayError.INVALID_RANGE);
    }

    @Test void enforcesRangeBoundaryDirectionAndFutureRules() {
        UUID vehicle = UUID.randomUUID();
        assertThat(validate(request(vehicle, null, NOW.minusSeconds(604_800), NOW, 1))
                .requestedRange().duration()).isEqualTo(java.time.Duration.ofDays(7));
        assertError(request(vehicle, null, NOW.minusSeconds(604_801), NOW, 1),
                JourneyReplayError.RANGE_TOO_LARGE);
        assertError(request(vehicle, null, NOW, NOW, 1), JourneyReplayError.INVALID_RANGE);
        assertError(request(vehicle, null, NOW, NOW.minusSeconds(1), 1), JourneyReplayError.INVALID_RANGE);
        assertError(request(vehicle, null, NOW.minusSeconds(1), NOW.plusSeconds(1), 1),
                JourneyReplayError.INVALID_RANGE);
    }

    @Test void enforcesPageAndUnsupportedCapabilityRules() {
        UUID vehicle = UUID.randomUUID();
        assertThat(validate(request(vehicle, null, NOW.minusSeconds(1), NOW, null)).limit()).isEqualTo(1_000);
        assertThat(validate(request(vehicle, null, NOW.minusSeconds(1), NOW, 2_000)).limit()).isEqualTo(2_000);
        assertError(request(vehicle, null, NOW.minusSeconds(1), NOW, 2_001),
                JourneyReplayError.INVALID_PAGE_SIZE);
        assertError(new ReplayRequest(vehicle, null, NOW.minusSeconds(1), NOW, 1, null,
                Set.of(OverlayType.IDLE), false), JourneyReplayError.UNSUPPORTED_ENGINE_EVIDENCE);
        assertError(new ReplayRequest(vehicle, null, NOW.minusSeconds(1), NOW, 1, null,
                Set.of(), true), JourneyReplayError.UNSUPPORTED_EXPORT);
    }

    @Test void ordersEqualTimesByHistoryIdentityAndDeduplicatesIdentity() {
        UUID vehicle = UUID.randomUUID();
        UUID first = new UUID(0, 1), second = new UUID(0, 2);
        JourneyPoint laterIdentity = point(second, vehicle, NOW);
        JourneyPoint earlierIdentity = point(first, vehicle, NOW);
        assertThat(ReplayQueryPolicy.orderedDistinct(List.of(laterIdentity, earlierIdentity, earlierIdentity)))
                .extracting(JourneyPoint::historyId).containsExactly(first, second);
    }

    @Test void validatesCursorBindingTenantSelectorRangeAndExpiry() {
        ReplayQuery query = validate(request(UUID.randomUUID(), null, NOW.minusSeconds(60), NOW, 10));
        CursorState cursor = cursor(query);
        ReplayQueryPolicy.validateCursor(cursor, query, NOW);

        ReplayQuery otherTenant = new ReplayQuery(new TenantContext(UUID.randomUUID(), UUID.randomUUID()),
                query.selector(), query.requestedRange(), query.effectiveRange(), query.limit(), null,
                Set.of(), query.direction(), query.browserPointCeiling());
        assertThatThrownBy(() -> ReplayQueryPolicy.validateCursor(cursor, otherTenant, NOW))
                .isInstanceOfSatisfying(JourneyReplayException.class,
                        error -> assertThat(error.error()).isEqualTo(JourneyReplayError.CURSOR_QUERY_MISMATCH));

        ReplayQuery otherRange = validate(request(query.selector().id(), null, NOW.minusSeconds(30), NOW, 10));
        assertThatThrownBy(() -> ReplayQueryPolicy.validateCursor(cursor, otherRange, NOW))
                .isInstanceOfSatisfying(JourneyReplayException.class,
                        error -> assertThat(error.error()).isEqualTo(JourneyReplayError.CURSOR_QUERY_MISMATCH));
        assertThatThrownBy(() -> ReplayQueryPolicy.validateCursor(cursor, query, NOW.plusSeconds(901)))
                .isInstanceOfSatisfying(JourneyReplayException.class,
                        error -> assertThat(error.error()).isEqualTo(JourneyReplayError.INVALID_CURSOR));
    }

    @Test void cursorPortContractCanRoundTripOpaqueStateWithoutSensitiveFacts() {
        ReplayQuery query = validate(request(UUID.randomUUID(), null, NOW.minusSeconds(60), NOW, 10));
        CursorState state = cursor(query);
        Map<String, CursorState> storage = new HashMap<>();
        com.transportlogistics.app.tracking.ports.outbound.JourneyReplayCursorPort codec =
                new com.transportlogistics.app.tracking.ports.outbound.JourneyReplayCursorPort() {
                    public String encode(CursorState value) {
                        String token = UUID.randomUUID().toString(); storage.put(token, value); return token;
                    }
                    public CursorState decode(String token) { return storage.get(token); }
                };
        String opaque = codec.encode(state);
        assertThat(opaque).doesNotContain(query.tenant().tenantId().toString(), query.selector().id().toString());
        assertThat(codec.decode(opaque)).isEqualTo(state);
    }

    @Test void rejectsStructurallyMalformedCursorState() {
        ReplayQuery query = validate(request(UUID.randomUUID(), null, NOW.minusSeconds(60), NOW, 10));
        CursorState malformed = new CursorState(
                new CursorBinding(null, query.selector(), query.requestedRange(), NOW),
                new CursorPosition(NOW, UUID.randomUUID()), NOW.plusSeconds(900));
        assertThatThrownBy(() -> ReplayQueryPolicy.validateCursor(malformed, query, NOW))
                .isInstanceOfSatisfying(JourneyReplayException.class,
                        error -> assertThat(error.error()).isEqualTo(JourneyReplayError.INVALID_CURSOR));
    }

    @Test void replayContractsContainNoProhibitedSensitiveFields() {
        Set<String> prohibitedFragments = Set.of(
                "credential", "signature", "provider", "device", "rawtelemetry", "driver", "customer");
        List<Class<? extends Record>> exposedRecords = List.of(
                JourneyPoint.class, ConfirmedStop.class, IncidentOverlay.class, ReplayPage.class);

        assertThat(exposedRecords)
                .flatExtracting(type -> Arrays.asList(type.getRecordComponents()))
                .extracting(component -> component.getName().toLowerCase(Locale.ROOT))
                .allSatisfy(name -> assertThat(prohibitedFragments)
                        .noneMatch(name::contains));
    }

    @Test void coverageDistinguishesPartialNoDataAndBrowserCeiling() {
        TimeRange requested = new TimeRange(NOW.minusSeconds(120), NOW);
        TimeRange available = new TimeRange(NOW.minusSeconds(60), NOW);
        ReplayPage partial = new ReplayPage(List.of(), null, requested, available,
                Coverage.PARTIAL_RETENTION, List.of(new DataGap(requested.from(), available.from(),
                Set.of(QualityFlag.PARTIAL_RETENTION))), true, true, Set.of("ENGINE_STATE"));
        assertThat(partial.coverage()).isEqualTo(Coverage.PARTIAL_RETENTION);
        assertThat(partial.browserCeilingWarning()).isTrue();
        ReplayPage none = new ReplayPage(List.of(), null, requested, null, Coverage.NO_DATA,
                List.of(), false, false, Set.of());
        ReplayPage completeEmpty = new ReplayPage(List.of(), null, requested, requested, Coverage.COMPLETE,
                List.of(), false, false, Set.of());
        assertThat(none.coverage()).isNotEqualTo(completeEmpty.coverage());
    }

    @Test void freezesOverlayAcceptanceWithoutUpgradingProducers() {
        assertThat(ReplayQueryPolicy.acceptance(OverlayType.GEOFENCE)).isEqualTo(ProducerAcceptance.ACCEPTED);
        assertThat(ReplayQueryPolicy.acceptance(OverlayType.SPEED))
                .isEqualTo(ProducerAcceptance.FIELD_FIDELITY_PENDING);
        assertThat(ReplayQueryPolicy.acceptance(OverlayType.ROUTE_DEVIATION))
                .isEqualTo(ProducerAcceptance.FIELD_ACCEPTANCE_PENDING);
        assertThatThrownBy(() -> ReplayQueryPolicy.acceptance(OverlayType.IDLE))
                .isInstanceOf(JourneyReplayException.class);
    }

    private static ReplayRequest request(UUID vehicle, UUID trip, Instant from, Instant to, Integer limit) {
        return new ReplayRequest(vehicle, trip, from, to, limit, null, Set.of(), false);
    }
    private static ReplayQuery validate(ReplayRequest request) { return ReplayQueryPolicy.validate(TENANT, request, NOW); }
    private static void assertError(ReplayRequest request, JourneyReplayError expected) {
        assertThatThrownBy(() -> validate(request)).isInstanceOfSatisfying(JourneyReplayException.class,
                error -> assertThat(error.error()).isEqualTo(expected));
    }
    private static JourneyPoint point(UUID id, UUID vehicle, Instant time) {
        return new JourneyPoint(id, vehicle, time, time.plusSeconds(1),
                new Coordinate(BigDecimal.ONE, BigDecimal.ONE), BigDecimal.ZERO, BigDecimal.ONE,
                Trust.TRUSTED, "GOOD", Ordering.IN_ORDER,
                Attribution.unavailable(AttributionStatus.UNATTRIBUTED), Set.of());
    }
    private static CursorState cursor(ReplayQuery query) {
        return new CursorState(new CursorBinding(query.tenant().tenantId(), query.selector(),
                query.requestedRange(), NOW), new CursorPosition(NOW.minusSeconds(1), UUID.randomUUID()),
                NOW.plusSeconds(900));
    }
}
