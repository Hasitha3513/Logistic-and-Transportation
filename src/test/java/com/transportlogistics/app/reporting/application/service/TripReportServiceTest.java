package com.transportlogistics.app.reporting.application.service;

import com.transportlogistics.app.fleet.FleetDriverSummary;
import com.transportlogistics.app.fleet.FleetVehicleSummary;
import com.transportlogistics.app.reporting.application.ports.out.FleetReportReadPort;
import com.transportlogistics.app.reporting.application.ports.out.TripReportReadPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.trip.TripReportItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TripReportServiceTest {

    private TripReportReadPort tripReports;
    private FleetReportReadPort fleetReports;
    private TripReportService service;

    @BeforeEach
    void setUp() {
        tripReports = mock(TripReportReadPort.class);
        fleetReports = mock(FleetReportReadPort.class);
        service = new TripReportService(tripReports, fleetReports);
    }

    @Test
    void throwsWhenDateRangeIsInvalid() {
        assertThatThrownBy(() -> service.getTripReport(null, LocalDate.now(), 0, 20, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Both fromDate and toDate are required");

        assertThatThrownBy(() -> service.getTripReport(LocalDate.now(), LocalDate.now().minusDays(1), 0, 20, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fromDate cannot be after toDate");
    }

    @Test
    void returnsEmptyReportWhenNoTripsFound() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 15);

        when(tripReports.findTripReports(any(), any(), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(fleetReports.findAllVehicles()).thenReturn(List.of());
        when(fleetReports.findAllDrivers()).thenReturn(List.of());

        var result = service.getTripReport(from, to, 0, 20, null, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void returnsEnrichedTripReports() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 15);

        UUID tripId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();

        var tripItem = new TripReportItem(
                tripId,
                "TRIP-1001",
                "COMPLETED",
                customerId,
                vehicleId,
                driverId,
                routeId,
                OffsetDateTime.of(2026, 8, 5, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 5, 12, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 5, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 5, 11, 45, 0, 0, ZoneOffset.UTC),
                1000.0,
                1150.0,
                "Delivered successfully",
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        when(tripReports.findTripReports(any(), any(), eq("COMPLETED"), eq(customerId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(tripItem), PageRequest.of(0, 20), 1));

        when(fleetReports.findAllVehicles()).thenReturn(List.of(
                new FleetVehicleSummary(vehicleId, "WP-CAB-1234", "AVAILABLE", 1150.0, true)
        ));
        when(fleetReports.findAllDrivers()).thenReturn(List.of(
                new FleetDriverSummary(driverId, "DRV-001", "John", "Doe", "AVAILABLE", true)
        ));

        var result = service.getTripReport(from, to, 0, 20, "COMPLETED", customerId);

        assertThat(result.getContent()).hasSize(1);
        var record = result.getContent().get(0);
        assertThat(record.tripId()).isEqualTo(tripId);
        assertThat(record.tripNumber()).isEqualTo("TRIP-1001");
        assertThat(record.status()).isEqualTo("COMPLETED");
        assertThat(record.vehicleRegistrationNumber()).isEqualTo("WP-CAB-1234");
        assertThat(record.driverEmployeeNumber()).isEqualTo("DRV-001");
        assertThat(record.driverName()).isEqualTo("John Doe");
        assertThat(record.distanceKm()).isEqualTo(150.0);
    }
}
