package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.FleetDriverSummary;
import com.transportlogistics.app.fleet.FleetReportingQuery;
import com.transportlogistics.app.fleet.FleetVehicleSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class FleetReportingAdapter implements FleetReportingQuery {

    private final VehicleJpaRepository vehicleRepo;
    private final DriverJpaRepository driverRepo;

    @Override
    public Optional<FleetVehicleSummary> findVehicle(UUID vehicleId) {
        return vehicleRepo.findById(vehicleId)
                .map(v -> new FleetVehicleSummary(v.getId(), v.getRegistrationNumber(), v.getOperationalStatus(), v.getCurrentOdometerKm(), v.isActive()));
    }

    @Override
    public List<FleetVehicleSummary> findAllVehicles() {
        return vehicleRepo.findAll().stream()
                .map(v -> new FleetVehicleSummary(v.getId(), v.getRegistrationNumber(), v.getOperationalStatus(), v.getCurrentOdometerKm(), v.isActive()))
                .toList();
    }

    @Override
    public Optional<FleetDriverSummary> findDriver(UUID driverId) {
        return driverRepo.findById(driverId)
                .map(d -> new FleetDriverSummary(d.getId(), d.getEmployeeNumber(), d.getFirstName(), d.getLastName(), d.getStatus(), d.isActive()));
    }

    @Override
    public List<FleetDriverSummary> findAllDrivers() {
        return driverRepo.findAll().stream()
                .map(d -> new FleetDriverSummary(d.getId(), d.getEmployeeNumber(), d.getFirstName(), d.getLastName(), d.getStatus(), d.isActive()))
                .toList();
    }
}
