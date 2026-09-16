package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Connectivity;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardPage;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardQuery;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Disclosure;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Freshness;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.HeatMapCell;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Incident;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.IncidentType;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.LiveVehicleState;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Motion;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Observation;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.ProducerStatus;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Summary;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.TripContext;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.VehicleRow;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardPolicy;
import com.transportlogistics.app.tracking.ports.inbound.TrackingDashboardQueryUseCase;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardIncidentPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardLiveStatePort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardTripContextPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TrackingDashboardQueryService implements TrackingDashboardQueryUseCase {
    private static final Duration INCIDENT_WINDOW = Duration.ofHours(24);
    private static final Duration CURSOR_LIFETIME = Duration.ofMinutes(5);

    private final TrackingDashboardLiveStatePort liveState;
    private final TrackingDashboardIncidentPort incidents;
    private final TrackingDashboardTripContextPort trips;
    private final TrackingDashboardCursorPort cursors;
    private final Clock clock;

    public TrackingDashboardQueryService(
            TrackingDashboardLiveStatePort liveState,
            TrackingDashboardIncidentPort incidents,
            TrackingDashboardTripContextPort trips,
            TrackingDashboardCursorPort cursors,
            Clock clock) {
        this.liveState = liveState;
        this.incidents = incidents;
        this.trips = trips;
        this.cursors = cursors;
        this.clock = clock;
    }

    @Override
    public DashboardPage query(DashboardQuery supplied, Disclosure disclosure) {
        DashboardQuery query = TrackingDashboardPolicy.validate(supplied);
        Instant now = Instant.now(clock);
        TrackingDashboardCursorPort.CursorState cursor = query.cursor() == null ? null
                : cursors.decode(query.tenantId(), query.filter(), query.cursor(), now);
        Instant evaluatedAt = cursor == null ? now : cursor.evaluatedAt();
        UUID afterVehicleId = cursor == null ? null : cursor.afterVehicleId();
        var live = liveState.find(query.tenantId(), query.filter(), afterVehicleId,
                query.pageSize(), evaluatedAt);
        Set<UUID> vehicleIds = live.items().stream().map(LiveVehicleState::vehicleId)
                .collect(Collectors.toUnmodifiableSet());
        Map<UUID, TripContext> tripContexts = trips.findActiveContexts(
                        query.tenantId(), vehicleIds, evaluatedAt).stream()
                .collect(Collectors.toMap(TripContext::vehicleId, Function.identity()));

        Set<IncidentType> permittedTypes = permittedTypes(query, disclosure);
        TrackingDashboardIncidentPort.IncidentResult incidentResult = query.filter().includeIncidents()
                && !permittedTypes.isEmpty()
                ? incidents.find(query.tenantId(), vehicleIds, permittedTypes,
                        evaluatedAt.minus(INCIDENT_WINDOW), evaluatedAt,
                        TrackingDashboardPolicy.MAXIMUM_INCIDENTS_PER_PRODUCER,
                        TrackingDashboardPolicy.MAXIMUM_INCIDENTS)
                : new TrackingDashboardIncidentPort.IncidentResult(List.of(), Map.of());
        Map<UUID, Map<IncidentType, Integer>> incidentCounts = counts(incidentResult.items());
        List<VehicleRow> rows = live.items().stream().map(state -> row(
                state, tripContexts.get(state.vehicleId()), incidentCounts.get(state.vehicleId()), disclosure))
                .toList();
        List<HeatMapCell> heatMap = disclosure.coordinates() && query.filter().includeHeatMap()
                ? heatMap(live.items()) : List.of();
        String nextCursor = live.hasMore() && !live.items().isEmpty()
                ? cursors.encode(new TrackingDashboardCursorPort.CursorState(query.tenantId(), query.filter(),
                        live.items().get(live.items().size() - 1).vehicleId(), evaluatedAt,
                        evaluatedAt.plus(CURSOR_LIFETIME))) : null;

        return new DashboardPage(evaluatedAt, live.lastSuccessfulRefreshAt(), live.sourceStatus(),
                incidentResult.sourceStatuses(), producerStatuses(), summary(rows, live.matchingVehicleCount()),
                rows, incidentResult.items(), heatMap, nextCursor);
    }

    private static Set<IncidentType> permittedTypes(DashboardQuery query, Disclosure disclosure) {
        var result = java.util.EnumSet.noneOf(IncidentType.class);
        if (disclosure.geofenceIncidents()) {
            result.add(IncidentType.GEOFENCE);
        }
        if (disclosure.speedIncidents()) {
            result.add(IncidentType.SPEED);
        }
        if (disclosure.routeDeviationIncidents()) {
            result.add(IncidentType.ROUTE_DEVIATION);
        }
        if (!query.filter().incidentTypes().isEmpty()) {
            result.retainAll(query.filter().incidentTypes());
        }
        return Set.copyOf(result);
    }

    private static VehicleRow row(
            LiveVehicleState state, TripContext tripContext,
            Map<IncidentType, Integer> counts, Disclosure disclosure) {
        return new VehicleRow(state.vehicleId(), state.freshness(), state.connectivity(),
                TrackingDashboardPolicy.motion(state.latestTrusted()), redact(state.latestReceived(), disclosure),
                redact(state.latestTrusted(), disclosure), tripContext,
                counts == null ? Map.of() : counts, disclosure.journeyReplay());
    }

    private static Observation redact(Observation value, Disclosure disclosure) {
        if (value == null || disclosure.coordinates()) {
            return value;
        }
        return new Observation(value.sourceTimestamp(), value.receivedAt(), value.trust(),
                null, null, null, value.speedKph());
    }

    private static Map<UUID, Map<IncidentType, Integer>> counts(List<Incident> incidents) {
        Map<UUID, Map<IncidentType, Integer>> result = new HashMap<>();
        incidents.forEach(value -> result.computeIfAbsent(value.vehicleId(), ignored ->
                new EnumMap<>(IncidentType.class)).merge(value.type(), 1, Integer::sum));
        return result;
    }

    private static Summary summary(List<VehicleRow> rows, long matchingCount) {
        return new Summary(matchingCount, count(rows, VehicleRow::freshness, Freshness.class),
                count(rows, VehicleRow::connectivity, Connectivity.class),
                count(rows, VehicleRow::motion, Motion.class));
    }

    private static <E extends Enum<E>> Map<E, Long> count(
            List<VehicleRow> rows, Function<VehicleRow, E> classifier, Class<E> type) {
        Map<E, Long> result = new EnumMap<>(type);
        rows.forEach(row -> result.merge(classifier.apply(row), 1L, Long::sum));
        return result;
    }

    private static List<HeatMapCell> heatMap(List<LiveVehicleState> states) {
        Map<CellKey, Long> counts = new HashMap<>();
        states.stream().filter(value -> TrackingDashboardPolicy.heatEligible(
                        value.freshness(), value.latestTrusted()))
                .forEach(value -> counts.merge(CellKey.from(value.latestTrusted()), 1L, Long::sum));
        List<HeatMapCell> result = new ArrayList<>();
        counts.forEach((key, count) -> result.add(new HeatMapCell(key.latitude(), key.longitude(), count)));
        return result.stream().sorted(java.util.Comparator.comparing(HeatMapCell::count).reversed()
                        .thenComparing(HeatMapCell::latitude).thenComparing(HeatMapCell::longitude))
                .limit(TrackingDashboardPolicy.MAXIMUM_HEAT_MAP_CELLS).toList();
    }

    private static Map<String, ProducerStatus> producerStatuses() {
        return Map.of("US-48", ProducerStatus.FIELD_ACCEPTANCE_PENDING,
                "US-49", ProducerStatus.ACCEPTED,
                "US-50", ProducerStatus.FIELD_FIDELITY_PENDING,
                "US-51", ProducerStatus.UNAVAILABLE,
                "US-52", ProducerStatus.FIELD_ACCEPTANCE_PENDING,
                "US-53", ProducerStatus.FIELD_ACCEPTANCE_PENDING);
    }

    private record CellKey(BigDecimal latitude, BigDecimal longitude) {
        private static CellKey from(Observation observation) {
            HeatMapCell cell = TrackingDashboardPolicy.heatCell(observation, 1);
            return new CellKey(cell.latitude(), cell.longitude());
        }
    }
}
