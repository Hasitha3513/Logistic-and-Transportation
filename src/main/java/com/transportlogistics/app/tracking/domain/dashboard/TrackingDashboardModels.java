package com.transportlogistics.app.tracking.domain.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TrackingDashboardModels {
    private TrackingDashboardModels() {
    }

    public enum Freshness { LIVE, RECENT, STALE, UNKNOWN }
    public enum Connectivity { CONNECTED, DEGRADED, OFFLINE, UNKNOWN }
    public enum Trust { TRUSTED, UNTRUSTED, UNKNOWN }
    public enum Motion { MOVING, STATIONARY, UNKNOWN }
    public enum IncidentType { GEOFENCE, SPEED, ROUTE_DEVIATION }
    public enum SourceStatus { AVAILABLE, DEGRADED, UNAVAILABLE }
    public enum ProducerStatus { ACCEPTED, FIELD_FIDELITY_PENDING, FIELD_ACCEPTANCE_PENDING, UNAVAILABLE }

    public record DashboardFilter(
            Set<UUID> vehicleIds,
            Set<Freshness> freshness,
            Set<Connectivity> connectivity,
            Set<Motion> motion,
            Set<IncidentType> incidentTypes,
            boolean includeHeatMap,
            boolean includeIncidents) {
        public DashboardFilter {
            vehicleIds = copy(vehicleIds);
            freshness = copy(freshness);
            connectivity = copy(connectivity);
            motion = copy(motion);
            incidentTypes = copy(incidentTypes);
        }

        private static <T> Set<T> copy(Set<T> values) {
            return values == null ? Set.of() : Set.copyOf(values);
        }
    }

    public record DashboardQuery(
            UUID tenantId,
            DashboardFilter filter,
            String cursor,
            int pageSize) {
    }

    public record Disclosure(
            boolean coordinates,
            boolean geofenceIncidents,
            boolean speedIncidents,
            boolean routeDeviationIncidents,
            boolean journeyReplay) {
    }

    public record Observation(
            Instant sourceTimestamp,
            Instant receivedAt,
            Trust trust,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal accuracyMeters,
            BigDecimal speedKph) {
    }

    public record LiveVehicleState(
            UUID vehicleId,
            Observation latestReceived,
            Observation latestTrusted,
            Freshness freshness,
            Connectivity connectivity,
            String policyVersion,
            Instant evaluatedAt) {
    }

    public record TripContext(
            UUID vehicleId,
            UUID tripId,
            String lifecycle,
            UUID routeId,
            Long routeVersion) {
    }

    public record Incident(
            UUID evidenceId,
            IncidentType type,
            UUID vehicleId,
            UUID tripId,
            UUID routeId,
            Long routeVersion,
            String severity,
            String status,
            Instant sourceTimestamp,
            ProducerStatus producerStatus) {
    }

    public record VehicleRow(
            UUID vehicleId,
            Freshness freshness,
            Connectivity connectivity,
            Motion motion,
            Observation latestReceived,
            Observation latestTrusted,
            TripContext tripContext,
            Map<IncidentType, Integer> incidentCounts,
            boolean journeyReplayAvailable) {
        public VehicleRow {
            incidentCounts = incidentCounts == null ? Map.of() : Map.copyOf(incidentCounts);
        }
    }

    public record Summary(
            long matchingVehicleCount,
            Map<Freshness, Long> freshnessCounts,
            Map<Connectivity, Long> connectivityCounts,
            Map<Motion, Long> motionCounts) {
        public Summary {
            freshnessCounts = Map.copyOf(freshnessCounts);
            connectivityCounts = Map.copyOf(connectivityCounts);
            motionCounts = Map.copyOf(motionCounts);
        }
    }

    public record HeatMapCell(BigDecimal latitude, BigDecimal longitude, long count) {
    }

    public record DashboardPage(
            Instant evaluatedAt,
            Instant lastSuccessfulRefreshAt,
            SourceStatus sourceStatus,
            Map<IncidentType, SourceStatus> incidentSourceStatuses,
            Map<String, ProducerStatus> producerStatuses,
            Summary summary,
            List<VehicleRow> vehicles,
            List<Incident> incidents,
            List<HeatMapCell> heatMapCells,
            String nextCursor) {
        public DashboardPage {
            incidentSourceStatuses = Map.copyOf(incidentSourceStatuses);
            producerStatuses = Map.copyOf(producerStatuses);
            vehicles = List.copyOf(vehicles);
            incidents = List.copyOf(incidents);
            heatMapCells = List.copyOf(heatMapCells);
        }
    }
}
