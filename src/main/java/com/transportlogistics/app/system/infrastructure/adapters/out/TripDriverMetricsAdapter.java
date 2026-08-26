package com.transportlogistics.app.system.infrastructure.adapters.out;

import com.transportlogistics.app.fleet.DriverTripMetricsProvider;
import com.transportlogistics.app.trip.TripReportingQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class TripDriverMetricsAdapter implements DriverTripMetricsProvider {

    private final TripReportingQuery tripReportingQuery;

    public TripDriverMetricsAdapter(@Autowired(required = false) TripReportingQuery tripReportingQuery) {
        this.tripReportingQuery = tripReportingQuery;
    }

    @Override
    public DriverTripSummary getTripSummary(UUID driverId) {
        if (tripReportingQuery == null || driverId == null) {
            return new DriverTripSummary(0, 0, 0);
        }

        var assignments = tripReportingQuery.findDriverAssignments(null, null, driverId);
        if (assignments == null || assignments.isEmpty()) {
            return new DriverTripSummary(0, 0, 0);
        }

        int totalAssigned = assignments.size();
        int totalCompleted = (int) assignments.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.status()) || "CLOSED".equalsIgnoreCase(a.status()))
                .count();
        int totalCancelled = (int) assignments.stream()
                .filter(a -> "CANCELLED".equalsIgnoreCase(a.status()) || "REJECTED".equalsIgnoreCase(a.status()))
                .count();

        return new DriverTripSummary(totalAssigned, totalCompleted, totalCancelled);
    }
}
