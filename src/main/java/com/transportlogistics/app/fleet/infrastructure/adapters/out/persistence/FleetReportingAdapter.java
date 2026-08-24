package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.FleetDocumentAlert;
import com.transportlogistics.app.fleet.FleetDriverSummary;
import com.transportlogistics.app.fleet.FleetExceptionAlert;
import com.transportlogistics.app.fleet.FleetReportingQuery;
import com.transportlogistics.app.fleet.FleetVehicleSummary;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class FleetReportingAdapter implements FleetReportingQuery {

    private final VehicleRepository vehicleRepo;
    private final DriverJpaRepository driverRepo;
    private final VehicleDocumentJpaRepository documentRepo;
    private final DriverExceptionJpaRepository exceptionRepo;

    @Override
    public Optional<FleetVehicleSummary> findVehicle(UUID vehicleId) {
        return vehicleRepo.findById(vehicleId)
                .map(v -> new FleetVehicleSummary(v.id(), v.registrationNumber(), v.operationalStatus(),
                        v.currentOdometerKm(), v.active()));
    }

    @Override
    public List<FleetVehicleSummary> findAllVehicles() {
        return vehicleRepo.findAll().stream()
                .map(v -> new FleetVehicleSummary(v.id(), v.registrationNumber(), v.operationalStatus(),
                        v.currentOdometerKm(), v.active()))
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

    @Override
    public List<FleetDocumentAlert> findExpiringDocuments(LocalDate cutoff) {
        var today = LocalDate.now();
        return documentRepo.findByActiveTrueAndMandatoryForDispatchTrueAndExpiryDateLessThanEqualOrderByExpiryDateAsc(cutoff).stream()
                .map(doc -> {
                    var regNo = vehicleRepo.findById(doc.getVehicleId()).map(vehicle -> vehicle.registrationNumber())
                            .orElse(doc.getVehicleId().toString());
                    var severity = doc.getExpiryDate() != null && doc.getExpiryDate().isBefore(today) ? "CRITICAL" : "WARNING";
                    return new FleetDocumentAlert(doc.getId(), doc.getDocumentType(), doc.getDocumentNumber(), regNo, doc.getExpiryDate(), severity);
                })
                .toList();
    }

    @Override
    public List<FleetExceptionAlert> findActiveExceptions() {
        return exceptionRepo.findAll().stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()) || "APPROVED".equalsIgnoreCase(e.getStatus()) || "PENDING".equalsIgnoreCase(e.getStatus()))
                .map(e -> {
                    var driverName = driverRepo.findById(e.getDriverId()).map(d -> d.getFirstName() + " " + d.getLastName()).orElse(e.getDriverId().toString());
                    return new FleetExceptionAlert(e.getId(), e.getExceptionType(), driverName, "CRITICAL", e.getStatus(), e.getReason());
                })
                .toList();
    }
}
