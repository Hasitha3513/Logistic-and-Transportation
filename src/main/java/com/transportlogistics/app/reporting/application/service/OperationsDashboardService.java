package com.transportlogistics.app.reporting.application.service;

import com.transportlogistics.app.reporting.application.ports.in.OperationsDashboardUseCase;
import com.transportlogistics.app.reporting.application.ports.out.FleetReportReadPort;
import com.transportlogistics.app.reporting.application.ports.out.TripReportReadPort;
import com.transportlogistics.app.reporting.domain.model.OperationsDashboard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationsDashboardService implements OperationsDashboardUseCase {

    private final FleetReportReadPort fleetReports;
    private final TripReportReadPort tripReports;

    @Override
    public OperationsDashboard getOperationsDashboard(LocalDate date) {
        var reportingDate = date == null ? LocalDate.now() : date;

        // Vehicle Metrics
        var vehicles = fleetReports.findAllVehicles();
        int vAvailable = 0, vAllocated = 0, vMaintenance = 0, vOutOfService = 0;
        for (var vehicle : vehicles) {
            var status = vehicle.operationalStatus() != null ? vehicle.operationalStatus().toUpperCase() : "AVAILABLE";
            if (!vehicle.active() || "OUT_OF_SERVICE".equals(status) || "DECOMMISSIONED".equals(status)) {
                vOutOfService++;
            } else if ("MAINTENANCE".equals(status) || "UNDER_MAINTENANCE".equals(status)) {
                vMaintenance++;
            } else if ("ALLOCATED".equals(status) || "ON_TRIP".equals(status) || "ASSIGNED".equals(status)) {
                vAllocated++;
            } else {
                vAvailable++;
            }
        }
        int totalVehicles = vAvailable + vAllocated + vMaintenance + vOutOfService;
        int vPercent = totalVehicles > 0 ? (int) Math.round((double) vAvailable * 100 / totalVehicles) : 0;
        var vehicleMetrics = new OperationsDashboard.VehicleMetrics(vAvailable, vAllocated, vMaintenance, vOutOfService, vPercent);

        // Driver Metrics
        var drivers = fleetReports.findAllDrivers();
        int dAvailable = 0, dAssigned = 0;
        for (var driver : drivers) {
            if (driver.active()) {
                var status = driver.status() != null ? driver.status().toUpperCase() : "AVAILABLE";
                if ("ASSIGNED".equals(status) || "ON_TRIP".equals(status)) {
                    dAssigned++;
                } else {
                    dAvailable++;
                }
            }
        }
        int totalDrivers = dAvailable + dAssigned;
        int dPercent = totalDrivers > 0 ? (int) Math.round((double) dAvailable * 100 / totalDrivers) : 0;
        var driverMetrics = new OperationsDashboard.DriverMetrics(dAvailable, dAssigned, dPercent);

        // Trip Metrics
        var trips = tripReports.findAllTrips();
        int tDraft = 0, tPendingApproval = 0, tApproved = 0, tAssigned = 0, tDispatched = 0, tInProgress = 0, tCompleted = 0;
        for (var trip : trips) {
            var status = trip.status() != null ? trip.status().toUpperCase() : "";
            switch (status) {
                case "DRAFT" -> tDraft++;
                case "SUBMITTED", "PENDING_APPROVAL" -> tPendingApproval++;
                case "APPROVED" -> tApproved++;
                case "ASSIGNED" -> tAssigned++;
                case "DISPATCHED" -> tDispatched++;
                case "IN_PROGRESS", "STARTED" -> tInProgress++;
                case "COMPLETED", "CLOSED" -> tCompleted++;
                default -> {}
            }
        }
        int totalTrips = tDraft + tPendingApproval + tApproved + tAssigned + tDispatched + tInProgress + tCompleted;
        int tPercent = totalTrips > 0 ? (int) Math.round((double) tCompleted * 100 / totalTrips) : 0;
        var tripMetrics = new OperationsDashboard.TripMetrics(tDraft, tPendingApproval, tApproved, tAssigned, tDispatched, tInProgress, tCompleted, tPercent);

        // Alerts
        var expiringDocs = fleetReports.findExpiringDocuments(reportingDate.plusDays(30));
        List<OperationsDashboard.DashboardAlertItem> docAlerts = expiringDocs.stream()
                .map(doc -> new OperationsDashboard.DashboardAlertItem(
                        doc.id().toString(),
                        doc.documentType() != null ? doc.documentType().replace('_', ' ') + " document expiry" : "Document expiry",
                        doc.registrationNumber() + " (" + doc.documentNumber() + ")",
                        doc.severity(),
                        doc.expiryDate() != null ? doc.expiryDate().toString() : null
                ))
                .toList();

        var activeExceptions = fleetReports.findActiveExceptions();
        List<OperationsDashboard.DashboardAlertItem> excAlerts = activeExceptions.stream()
                .map(exc -> new OperationsDashboard.DashboardAlertItem(
                        exc.id().toString(),
                        exc.exceptionType() != null ? exc.exceptionType().replace('_', ' ') : "Driver Exception",
                        exc.driverName() + (exc.reason() != null && !exc.reason().isBlank() ? " - " + exc.reason() : ""),
                        exc.severity(),
                        null
                ))
                .toList();

        var alerts = new OperationsDashboard.DashboardAlerts(docAlerts, excAlerts);

        return new OperationsDashboard(reportingDate, "READY", vehicleMetrics, driverMetrics, tripMetrics, alerts);
    }
}
