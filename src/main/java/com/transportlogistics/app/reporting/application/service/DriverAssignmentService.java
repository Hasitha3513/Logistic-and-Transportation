package com.transportlogistics.app.reporting.application.service;

import com.transportlogistics.app.fleet.FleetDriverSummary;
import com.transportlogistics.app.fleet.FleetVehicleSummary;
import com.transportlogistics.app.reporting.application.model.DriverAssignmentReportRecord;
import com.transportlogistics.app.reporting.application.ports.in.DriverAssignmentUseCase;
import com.transportlogistics.app.reporting.application.ports.out.FleetReportReadPort;
import com.transportlogistics.app.reporting.application.ports.out.TripReportReadPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.trip.DriverAssignmentReportItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverAssignmentService implements DriverAssignmentUseCase {

    private final TripReportReadPort tripReports;
    private final FleetReportReadPort fleetReports;

    @Override
    public List<DriverAssignmentReportRecord> getDriverAssignmentReport(LocalDate fromDate, LocalDate toDate, UUID driverId) {
        if (fromDate == null || toDate == null) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "Both fromDate and toDate are required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "fromDate cannot be after toDate");
        }

        var from = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        var to = toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        var assignmentItems = tripReports.findDriverAssignments(from, to, driverId);

        Map<UUID, FleetVehicleSummary> vehicleMap = fleetReports.findAllVehicles().stream()
                .collect(Collectors.toMap(FleetVehicleSummary::id, v -> v, (v1, v2) -> v1));
        Map<UUID, FleetDriverSummary> driverMap = fleetReports.findAllDrivers().stream()
                .collect(Collectors.toMap(FleetDriverSummary::id, d -> d, (d1, d2) -> d1));

        return assignmentItems.stream()
                .map(item -> toRecord(item, vehicleMap, driverMap))
                .toList();
    }

    private DriverAssignmentReportRecord toRecord(DriverAssignmentReportItem item,
                                                 Map<UUID, FleetVehicleSummary> vehicleMap,
                                                 Map<UUID, FleetDriverSummary> driverMap) {
        String registration = null;
        if (item.vehicleId() != null && vehicleMap.containsKey(item.vehicleId())) {
            registration = vehicleMap.get(item.vehicleId()).registrationNumber();
        }

        String employeeNumber = null;
        String driverName = null;
        String driverStatus = null;
        if (item.driverId() != null && driverMap.containsKey(item.driverId())) {
            var driver = driverMap.get(item.driverId());
            employeeNumber = driver.employeeNumber();
            driverStatus = driver.status();
            driverName = (driver.firstName() != null ? driver.firstName() : "") +
                    (driver.lastName() != null ? " " + driver.lastName() : "");
            driverName = driverName.trim().isEmpty() ? null : driverName.trim();
        }

        return new DriverAssignmentReportRecord(
                item.driverId(),
                employeeNumber,
                driverName,
                driverStatus,
                item.tripId(),
                item.tripNumber(),
                item.status(),
                item.requestedStartTime() != null ? item.requestedStartTime().toLocalDate() : null,
                item.requestedEndTime() != null ? item.requestedEndTime().toLocalDate() : null,
                item.actualStartTime() != null ? item.actualStartTime().toLocalDate() : null,
                item.actualEndTime() != null ? item.actualEndTime().toLocalDate() : null,
                item.vehicleId(),
                registration,
                item.routeId(),
                item.createdAt()
        );
    }
}
