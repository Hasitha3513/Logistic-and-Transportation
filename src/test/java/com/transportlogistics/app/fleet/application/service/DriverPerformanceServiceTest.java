package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverTripMetricsPort;
import com.transportlogistics.app.fleet.application.ports.out.DriverViolationRepository;
import com.transportlogistics.app.fleet.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverPerformanceServiceTest {

    @Mock
    private DriverRepository drivers;

    @Mock
    private DriverViolationRepository violations;

    @Mock
    private DriverTripMetricsPort tripMetrics;

    private DriverPerformanceService service;
    private final UUID driverId = UUID.randomUUID();
    private final Driver testDriver = new Driver(driverId, "EMP-100", "Alex", "Driver", "555-4321", "alex@test.com", "AVAILABLE", true);

    @BeforeEach
    void setUp() {
        service = new DriverPerformanceService(drivers, violations, tripMetrics);
    }

    @Test
    void computesPerformanceSummaryForDriver() {
        when(drivers.findById(driverId)).thenReturn(Optional.of(testDriver));

        var v1 = DriverViolation.record(driverId, null, DriverViolationType.SPEEDING, ViolationSeverity.MINOR,
                OffsetDateTime.now(), 2, new BigDecimal("100.00"), "Location 1", "desc", "admin");
        var v2 = DriverViolation.record(driverId, null, DriverViolationType.RED_LIGHT, ViolationSeverity.MODERATE,
                OffsetDateTime.now(), 3, new BigDecimal("200.00"), "Location 2", "desc", "admin");

        when(violations.findByDriverId(driverId)).thenReturn(List.of(v1, v2));
        when(tripMetrics.getTripSummary(driverId)).thenReturn(new DriverTripMetricsPort.DriverTripSummary(10, 9, 1));

        var summary = service.getPerformanceSummary(driverId);

        assertEquals(driverId, summary.driverId());
        assertEquals("Alex Driver", summary.driverName());
        assertEquals(10, summary.totalTripsAssigned());
        assertEquals(9, summary.totalTripsCompleted());
        assertEquals(1, summary.totalTripsCancelled());
        assertEquals(90.0, summary.tripCompletionRate());
        assertEquals(2, summary.totalViolations());
        assertEquals(5, summary.totalPenaltyPoints());
        assertEquals(0, summary.criticalViolations());
        assertEquals(new BigDecimal("300.00"), summary.totalFines());
        assertEquals(new BigDecimal("300.00"), summary.unpaidFines()); // Both unpaid
        assertEquals(80, summary.safetyScore()); // 100 - (5 * 4) = 80
    }
}
