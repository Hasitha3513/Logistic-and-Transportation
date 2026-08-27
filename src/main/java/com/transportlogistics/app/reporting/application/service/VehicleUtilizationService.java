package com.transportlogistics.app.reporting.application.service;

import com.transportlogistics.app.fleet.FleetVehicleSummary;
import com.transportlogistics.app.reporting.application.model.VehicleUtilizationReportRecord;
import com.transportlogistics.app.reporting.application.ports.in.VehicleUtilizationUseCase;
import com.transportlogistics.app.reporting.application.ports.out.FleetReportReadPort;
import com.transportlogistics.app.reporting.application.ports.out.TripReportReadPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.trip.VehicleTripReportItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleUtilizationService implements VehicleUtilizationUseCase {

    private final TripReportReadPort tripReports;
    private final FleetReportReadPort fleetReports;

    @Override
    public List<VehicleUtilizationReportRecord> getVehicleUtilizationReport(LocalDate fromDate, LocalDate toDate, UUID vehicleId) {
        if (fromDate == null || toDate == null) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "Both fromDate and toDate are required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "fromDate cannot be after toDate");
        }

        var from = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        var to = toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        if (vehicleId != null) {
            var vehicle = fleetReports.findVehicle(vehicleId)
                    .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId));
            var trips = tripReports.findVehicleTrips(from, to, vehicleId);
            return List.of(calculateMetrics(vehicle, trips));
        }

        var vehicles = fleetReports.findAllVehicles();
        var allTrips = tripReports.findVehicleTrips(from, to, null);

        Map<UUID, List<VehicleTripReportItem>> tripsByVehicle = allTrips.stream()
                .filter(t -> t.vehicleId() != null)
                .collect(Collectors.groupingBy(VehicleTripReportItem::vehicleId));

        return vehicles.stream()
                .map(v -> calculateMetrics(v, tripsByVehicle.getOrDefault(v.id(), List.of())))
                .sorted(Comparator.comparing(VehicleUtilizationReportRecord::registrationNumber, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private VehicleUtilizationReportRecord calculateMetrics(FleetVehicleSummary vehicle, List<VehicleTripReportItem> trips) {
        long totalAssigned = trips.size();
        long completed = trips.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.status()) || "CLOSED".equalsIgnoreCase(t.status()))
                .count();

        double totalDistanceKm = trips.stream()
                .filter(t -> t.startOdometerKm() != null && t.endOdometerKm() != null && t.endOdometerKm() >= t.startOdometerKm())
                .mapToDouble(t -> t.endOdometerKm() - t.startOdometerKm())
                .sum();

        double totalAllocatedHours = trips.stream()
                .filter(t -> t.requestedStartTime() != null && t.requestedEndTime() != null && t.requestedEndTime().isAfter(t.requestedStartTime()))
                .mapToDouble(t -> Duration.between(t.requestedStartTime(), t.requestedEndTime()).toMinutes() / 60.0)
                .sum();

        return new VehicleUtilizationReportRecord(
                vehicle.id(),
                vehicle.registrationNumber(),
                vehicle.operationalStatus(),
                totalAssigned,
                completed,
                Math.round(totalDistanceKm * 100.0) / 100.0,
                Math.round(totalAllocatedHours * 100.0) / 100.0
        );
    }
}
