package com.transportlogistics.app.reporting.application.service;

import com.transportlogistics.app.fleet.FleetDriverSummary;
import com.transportlogistics.app.fleet.FleetVehicleSummary;
import com.transportlogistics.app.reporting.application.ports.out.FleetReportReadPort;
import com.transportlogistics.app.reporting.application.ports.out.TripReportReadPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.trip.DriverAssignmentReportItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DriverAssignmentServiceTest {

    private TripReportReadPort tripReports;
    private FleetReportReadPort fleetReports;
    private DriverAssignmentService service;

    @BeforeEach
    void setUp() {
        tripReports = mock(TripReportReadPort.class);
        fleetReports = mock(FleetReportReadPort.class);
        service = new DriverAssignmentService(tripReports, fleetReports);
    }

    @Test
    void throwsOnInvalidDateRange() {
        assertThatThrownBy(() -> service.getDriverAssignmentReport(null, LocalDate.now(), null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.getDriverAssignmentReport(LocalDate.now(), LocalDate.now().minusDays(2), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void returnsEmptyListWhenNoAssignments() {
        when(tripReports.findDriverAssignments(any(), any(), any())).thenReturn(List.of());
        when(fleetReports.findAllVehicles()).thenReturn(List.of());
        when(fleetReports.findAllDrivers()).thenReturn(List.of());

        var result = service.getDriverAssignmentReport(LocalDate.now().minusDays(7), LocalDate.now(), null);
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEnrichedDriverAssignments() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();

        var item = new DriverAssignmentReportItem(
                tripId,
                "TRIP-2002",
                "ASSIGNED",
                driverId,
                vehicleId,
                routeId,
                OffsetDateTime.of(2026, 8, 10, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 10, 17, 0, 0, 0, ZoneOffset.UTC),
                null,
                null,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        when(tripReports.findDriverAssignments(any(), any(), eq(driverId))).thenReturn(List.of(item));
        when(fleetReports.findAllVehicles()).thenReturn(List.of(
                new FleetVehicleSummary(vehicleId, "WP-CAD-5678", "AVAILABLE", 5000.0, true)
        ));
        when(fleetReports.findAllDrivers()).thenReturn(List.of(
                new FleetDriverSummary(driverId, "DRV-007", "Jane", "Smith", "AVAILABLE", true)
        ));

        var result = service.getDriverAssignmentReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15), driverId);

        assertThat(result).hasSize(1);
        var record = result.get(0);
        assertThat(record.driverId()).isEqualTo(driverId);
        assertThat(record.employeeNumber()).isEqualTo("DRV-007");
        assertThat(record.driverName()).isEqualTo("Jane Smith");
        assertThat(record.tripNumber()).isEqualTo("TRIP-2002");
        assertThat(record.vehicleRegistrationNumber()).isEqualTo("WP-CAD-5678");
    }
}
