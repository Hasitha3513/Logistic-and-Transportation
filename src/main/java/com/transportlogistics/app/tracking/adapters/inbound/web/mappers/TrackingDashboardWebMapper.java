package com.transportlogistics.app.tracking.adapters.inbound.web.mappers;

import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.TrackingDashboardResponses;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardPage;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Observation;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.TripContext;
import org.springframework.stereotype.Component;

@Component
public final class TrackingDashboardWebMapper {
    public TrackingDashboardResponses.Dashboard response(DashboardPage page) {
        var summary = new TrackingDashboardResponses.Summary(page.summary().matchingVehicleCount(),
                page.summary().freshnessCounts(), page.summary().connectivityCounts(), page.summary().motionCounts());
        var vehicles = page.vehicles().stream().map(value -> new TrackingDashboardResponses.Vehicle(
                value.vehicleId(), value.freshness(), value.connectivity(), value.motion(),
                observation(value.latestReceived()), observation(value.latestTrusted()), trip(value.tripContext()),
                value.incidentCounts(), value.journeyReplayAvailable())).toList();
        var incidents = page.incidents().stream().map(value -> new TrackingDashboardResponses.Incident(
                value.evidenceId(), value.type(), value.vehicleId(), value.tripId(), value.routeId(),
                value.routeVersion(), value.severity(), value.status(), value.sourceTimestamp(),
                value.producerStatus())).toList();
        var heat = page.heatMapCells().stream().map(value -> new TrackingDashboardResponses.HeatCell(
                value.latitude(), value.longitude(), value.count())).toList();
        return new TrackingDashboardResponses.Dashboard(page.evaluatedAt(), page.lastSuccessfulRefreshAt(),
                page.sourceStatus(), page.incidentSourceStatuses(), page.producerStatuses(), summary,
                vehicles, incidents, heat, page.nextCursor());
    }

    private static TrackingDashboardResponses.Observation observation(Observation value) {
        return value == null ? null : new TrackingDashboardResponses.Observation(value.sourceTimestamp(),
                value.receivedAt(), value.trust(), value.latitude(), value.longitude(),
                value.accuracyMeters(), value.speedKph());
    }

    private static TrackingDashboardResponses.Trip trip(TripContext value) {
        return value == null ? null : new TrackingDashboardResponses.Trip(value.tripId(), value.lifecycle(),
                value.routeId(), value.routeVersion());
    }
}
