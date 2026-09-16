package com.transportlogistics.app.tracking.adapters.inbound.web.dto.response;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Connectivity;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Freshness;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.IncidentType;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Motion;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.ProducerStatus;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.SourceStatus;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Trust;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TrackingDashboardResponses {
    private TrackingDashboardResponses() { }
    public record Observation(Instant sourceTimestamp, Instant receivedAt, Trust trust,
            BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters, BigDecimal speedKph) { }
    public record Trip(UUID tripId, String lifecycle, UUID routeId, Long routeVersion) { }
    public record Vehicle(UUID vehicleId, Freshness freshness, Connectivity connectivity, Motion motion,
            Observation latestReceived, Observation latestTrusted, Trip activeTrip,
            Map<IncidentType, Integer> incidentCounts, boolean journeyReplayAvailable) { }
    public record Incident(UUID evidenceId, IncidentType type, UUID vehicleId, UUID tripId,
            UUID routeId, Long routeVersion, String severity, String status,
            Instant sourceTimestamp, ProducerStatus producerStatus) { }
    public record HeatCell(BigDecimal latitude, BigDecimal longitude, long count) { }
    public record Summary(long matchingVehicleCount, Map<Freshness, Long> freshnessCounts,
            Map<Connectivity, Long> connectivityCounts, Map<Motion, Long> motionCounts) { }
    public record Dashboard(Instant evaluatedAt, Instant lastSuccessfulRefreshAt,
            SourceStatus sourceStatus, Map<IncidentType, SourceStatus> incidentSourceStatuses,
            Map<String, ProducerStatus> producerStatuses, Summary summary, List<Vehicle> vehicles,
            List<Incident> incidents, List<HeatCell> heatMapCells, String nextCursor) { }
}
