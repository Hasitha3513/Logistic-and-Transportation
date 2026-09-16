package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Connectivity;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardFilter;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Freshness;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.LiveVehicleState;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Observation;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.SourceStatus;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Trust;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardPolicy;
import com.transportlogistics.app.tracking.ports.outbound.LiveTelemetryProjectionPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardLiveStatePort;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class TrackingDashboardLiveStateAdapter implements TrackingDashboardLiveStatePort {
    private static final int MAXIMUM_SOURCE_VEHICLES = 10_000;
    private static final String POLICY_VERSION = "US54-DASHBOARD-V1";

    private final LiveTelemetryProjectionPort redis;
    private final NamedParameterJdbcTemplate jdbc;

    TrackingDashboardLiveStateAdapter(
            LiveTelemetryProjectionPort redis, NamedParameterJdbcTemplate jdbc) {
        this.redis = redis;
        this.jdbc = jdbc;
    }

    @Override
    public LiveStatePage find(
            UUID tenantId, DashboardFilter filter, UUID afterVehicleId, int limit, Instant evaluatedAt) {
        Map<UUID, PositionPair> database = databasePositions(tenantId);
        Map<UUID, TrackingTelemetryIngestedV1> projected = new HashMap<>();
        SourceStatus status = SourceStatus.AVAILABLE;
        try {
            redis.findLive(tenantId, evaluatedAt, 500).forEach(value ->
                    projected.put(value.telemetry().vehicleId(), value.telemetry()));
        } catch (DependencyUnavailableException exception) {
            status = SourceStatus.DEGRADED;
        }

        Set<UUID> vehicleIds = new LinkedHashSet<>(database.keySet());
        vehicleIds.addAll(projected.keySet());
        List<LiveVehicleState> matching = vehicleIds.stream()
                .map(vehicleId -> state(vehicleId, database.get(vehicleId), projected.get(vehicleId), evaluatedAt))
                .filter(value -> matches(value, filter))
                .sorted(Comparator.comparing(LiveVehicleState::vehicleId))
                .toList();
        List<LiveVehicleState> page = matching.stream()
                .filter(value -> afterVehicleId == null || value.vehicleId().compareTo(afterVehicleId) > 0)
                .limit(limit + 1L)
                .toList();
        boolean hasMore = page.size() > limit;
        if (hasMore) {
            page = page.subList(0, limit);
        }
        Instant lastRefresh = page.stream().map(LiveVehicleState::latestReceived)
                .filter(java.util.Objects::nonNull).map(Observation::receivedAt)
                .max(Comparator.naturalOrder()).orElse(null);
        return new LiveStatePage(page, matching.size(), hasMore, status, lastRefresh);
    }

    private Map<UUID, PositionPair> databasePositions(UUID tenantId) {
        List<UUID> vehicleIds = jdbc.query("""
                SELECT DISTINCT vehicle_id
                FROM tracking_position_history
                WHERE tenant_id = :tenantId
                ORDER BY vehicle_id
                LIMIT :limit
                """, Map.of("tenantId", tenantId, "limit", MAXIMUM_SOURCE_VEHICLES),
                (row, number) -> row.getObject(1, UUID.class));
        if (vehicleIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Observation> received = latest(tenantId, vehicleIds, false);
        Map<UUID, Observation> trusted = latest(tenantId, vehicleIds, true);
        Map<UUID, PositionPair> result = new HashMap<>();
        vehicleIds.forEach(id -> result.put(id, new PositionPair(received.get(id), trusted.get(id))));
        return result;
    }

    private Map<UUID, Observation> latest(UUID tenantId, List<UUID> vehicleIds, boolean trusted) {
        String trustPredicate = trusted ? " AND trust = 'TRUSTED'" : "";
        List<VehicleObservation> rows = jdbc.query("""
                SELECT DISTINCT ON (vehicle_id)
                       vehicle_id, source_timestamp, received_at, trust,
                       latitude, longitude, horizontal_accuracy_meters, speed_kph
                FROM tracking_position_history
                WHERE tenant_id = :tenantId AND vehicle_id IN (:vehicleIds)
                """ + trustPredicate + " ORDER BY vehicle_id, source_timestamp DESC, id DESC",
                Map.of("tenantId", tenantId, "vehicleIds", vehicleIds), this::mapObservation);
        Map<UUID, Observation> result = new HashMap<>();
        rows.forEach(row -> result.put(row.vehicleId(), row.observation()));
        return result;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private VehicleObservation mapObservation(ResultSet row, int number) throws SQLException {
        return new VehicleObservation(row.getObject("vehicle_id", UUID.class), new Observation(
                row.getTimestamp("source_timestamp").toInstant(),
                row.getTimestamp("received_at").toInstant(), Trust.valueOf(row.getString("trust")),
                row.getBigDecimal("latitude"), row.getBigDecimal("longitude"),
                row.getBigDecimal("horizontal_accuracy_meters"), row.getBigDecimal("speed_kph")));
    }

    private LiveVehicleState state(
            UUID vehicleId, PositionPair pair, TrackingTelemetryIngestedV1 event, Instant evaluatedAt) {
        Observation trusted = pair == null ? null : pair.trusted();
        Observation received = pair == null ? null : pair.received();
        if (event != null && (received == null || event.recordedAt().isAfter(received.sourceTimestamp()))) {
            Trust eventTrust = trust(event, trusted);
            received = observation(event, eventTrust);
            if (eventTrust == Trust.TRUSTED
                    && (trusted == null || event.recordedAt().isAfter(trusted.sourceTimestamp()))) {
                trusted = received;
            }
        }
        Instant latestReceipt = received == null ? null : received.receivedAt();
        return new LiveVehicleState(vehicleId, received, trusted,
                freshness(trusted, latestReceipt, evaluatedAt),
                connectivity(latestReceipt, evaluatedAt), POLICY_VERSION, evaluatedAt);
    }

    private static Observation observation(TrackingTelemetryIngestedV1 event, Trust trust) {
        return new Observation(event.recordedAt(), event.receivedAt(), trust, event.latitude(),
                event.longitude(), event.horizontalAccuracyMeters(), event.speedKph());
    }

    private static Trust trust(TrackingTelemetryIngestedV1 event, Observation currentTrusted) {
        Duration age = Duration.between(event.recordedAt(), event.receivedAt());
        if (age.compareTo(Duration.ofHours(24)) > 0
                || event.recordedAt().isAfter(event.receivedAt().plusSeconds(120))
                || currentTrusted != null && event.recordedAt().isBefore(currentTrusted.sourceTimestamp())) {
            return Trust.UNTRUSTED;
        }
        BigDecimal accuracy = event.horizontalAccuracyMeters();
        if (accuracy == null) {
            return Trust.UNKNOWN;
        }
        return accuracy.compareTo(BigDecimal.valueOf(1_000)) <= 0 ? Trust.TRUSTED : Trust.UNTRUSTED;
    }

    private static Freshness freshness(Observation trusted, Instant receipt, Instant now) {
        if (trusted == null) {
            return Freshness.UNKNOWN;
        }
        Duration sourceAge = Duration.between(trusted.sourceTimestamp(), now);
        Duration receiptAge = receipt == null ? Duration.ofDays(365) : Duration.between(receipt, now);
        if (!sourceAge.isNegative() && sourceAge.compareTo(Duration.ofSeconds(60)) <= 0
                && !receiptAge.isNegative() && receiptAge.compareTo(Duration.ofSeconds(60)) <= 0) {
            return Freshness.LIVE;
        }
        return sourceAge.compareTo(Duration.ofMinutes(5)) <= 0 ? Freshness.RECENT : Freshness.STALE;
    }

    private static Connectivity connectivity(Instant receipt, Instant now) {
        if (receipt == null) {
            return Connectivity.UNKNOWN;
        }
        Duration age = Duration.between(receipt, now);
        if (!age.isNegative() && age.compareTo(Duration.ofSeconds(60)) <= 0) {
            return Connectivity.CONNECTED;
        }
        return age.compareTo(Duration.ofMinutes(5)) <= 0
                ? Connectivity.DEGRADED : Connectivity.OFFLINE;
    }

    private static boolean matches(LiveVehicleState state, DashboardFilter filter) {
        return (filter.vehicleIds().isEmpty() || filter.vehicleIds().contains(state.vehicleId()))
                && (filter.freshness().isEmpty() || filter.freshness().contains(state.freshness()))
                && (filter.connectivity().isEmpty()
                        || filter.connectivity().contains(state.connectivity()))
                && (filter.motion().isEmpty()
                        || filter.motion().contains(TrackingDashboardPolicy.motion(state.latestTrusted())));
    }

    private record VehicleObservation(UUID vehicleId, Observation observation) {
    }

    private record PositionPair(Observation received, Observation trusted) {
    }
}
