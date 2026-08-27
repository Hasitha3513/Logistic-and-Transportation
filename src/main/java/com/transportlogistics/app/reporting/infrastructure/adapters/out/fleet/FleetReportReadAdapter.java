package com.transportlogistics.app.reporting.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.FleetDriverSummary;
import com.transportlogistics.app.fleet.FleetReportingQuery;
import com.transportlogistics.app.fleet.FleetVehicleSummary;
import com.transportlogistics.app.reporting.application.ports.out.FleetReportReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class FleetReportReadAdapter implements FleetReportReadPort {

    private final FleetReportingQuery query;

    @Override
    public Optional<FleetVehicleSummary> findVehicle(UUID vehicleId) {
        return query.findVehicle(vehicleId);
    }

    @Override
    public List<FleetVehicleSummary> findAllVehicles() {
        return query.findAllVehicles();
    }

    @Override
    public Optional<FleetDriverSummary> findDriver(UUID driverId) {
        return query.findDriver(driverId);
    }

    @Override
    public List<FleetDriverSummary> findAllDrivers() {
        return query.findAllDrivers();
    }

    @Override
    public List<com.transportlogistics.app.fleet.FleetDocumentAlert> findExpiringDocuments(java.time.LocalDate cutoff) {
        return query.findExpiringDocuments(cutoff);
    }

    @Override
    public List<com.transportlogistics.app.fleet.FleetExceptionAlert> findActiveExceptions() {
        return query.findActiveExceptions();
    }
}
