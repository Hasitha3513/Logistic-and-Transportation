package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverPerformanceUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverTripMetricsPort;
import com.transportlogistics.app.fleet.application.ports.out.DriverViolationRepository;
import com.transportlogistics.app.fleet.domain.model.DriverPerformanceSummary;
import com.transportlogistics.app.fleet.domain.model.DriverViolation;
import com.transportlogistics.app.fleet.domain.model.FinePaymentStatus;
import com.transportlogistics.app.fleet.domain.model.ViolationSeverity;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Transactional(readOnly = true)
public class DriverPerformanceService implements DriverPerformanceUseCase {

    private final DriverRepository drivers;
    private final DriverViolationRepository violations;
    private final DriverTripMetricsPort tripMetrics;

    public DriverPerformanceService(
            DriverRepository drivers,
            DriverViolationRepository violations,
            DriverTripMetricsPort tripMetrics
    ) {
        this.drivers = drivers;
        this.violations = violations;
        this.tripMetrics = tripMetrics;
    }

    @Override
    public DriverPerformanceSummary getPerformanceSummary(UUID driverId) {
        var driver = drivers.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));

        var driverViolations = violations.findByDriverId(driverId);
        var tripSummary = tripMetrics != null
                ? tripMetrics.getTripSummary(driverId)
                : new DriverTripMetricsPort.DriverTripSummary(0, 0, 0);

        int totalViolations = driverViolations.size();
        int totalPenaltyPoints = driverViolations.stream()
                .mapToInt(DriverViolation::penaltyPoints)
                .sum();
        int criticalViolations = (int) driverViolations.stream()
                .filter(v -> v.severity() == ViolationSeverity.CRITICAL)
                .count();
        BigDecimal totalFines = driverViolations.stream()
                .map(DriverViolation::fineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unpaidFines = driverViolations.stream()
                .filter(v -> v.paymentStatus() == FinePaymentStatus.UNPAID)
                .map(DriverViolation::fineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var driverName = ((driver.firstName() != null ? driver.firstName() : "") + " "
                + (driver.lastName() != null ? driver.lastName() : "")).trim();

        return DriverPerformanceSummary.calculate(
                driverId,
                driverName,
                tripSummary.totalTripsAssigned(),
                tripSummary.totalTripsCompleted(),
                tripSummary.totalTripsCancelled(),
                totalViolations,
                totalPenaltyPoints,
                criticalViolations,
                totalFines,
                unpaidFines
        );
    }
}
