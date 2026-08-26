package com.transportlogistics.app.reporting.domain.model;

import java.time.LocalDate;
import java.util.List;

public record OperationsDashboard(
        LocalDate date,
        String status,
        VehicleMetrics vehicles,
        DriverMetrics drivers,
        TripMetrics trips,
        DashboardAlerts alerts) {

    public record VehicleMetrics(
            int available,
            int allocated,
            int maintenance,
            int outOfService,
            int availabilityPercent) {}

    public record DriverMetrics(
            int available,
            int assigned,
            int availabilityPercent) {}

    public record TripMetrics(
            int draft,
            int pendingApproval,
            int approved,
            int assigned,
            int dispatched,
            int inProgress,
            int completed,
            int completionPercent) {}

    public record DashboardAlerts(
            List<DashboardAlertItem> expiringDocuments,
            List<DashboardAlertItem> criticalExceptions) {}

    public record DashboardAlertItem(
            String id,
            String title,
            String detail,
            String severity,
            String dueDate) {}
}
