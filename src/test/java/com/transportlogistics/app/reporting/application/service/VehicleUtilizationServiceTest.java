package com.transportlogistics.app.reporting.application.service;

import com.transportlogistics.app.fleet.FleetVehicleSummary;
import com.transportlogistics.app.reporting.application.ports.out.FleetReportReadPort;
import com.transportlogistics.app.reporting.application.ports.out.TripReportReadPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.trip.VehicleTripReportItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class VehicleUtilizationServiceTest {

    private TripReportReadPort tripReports;
    private FleetReportReadPort fleetReports;
    private VehicleUtilizationService service;

    @BeforeEach
    void setUp() {
        tripReports = mock(TripReportReadPort.class);
        fleetReports = mock(FleetReportReadPort.class);
        service = new VehicleUtilizationService(tripReports, fleetReports);
    }

    @Test
    void throwsOnInvalidDateRange() {
        assertThatThrownBy(() -> service.getVehicleUtilizationReport(null, LocalDate.now(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void throwsWhenSpecificVehicleNotFound() {
        UUID vehicleId = UUID.randomUUID();
        when(fleetReports.findVehicle(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVehicleUtilizationReport(LocalDate.now().minusDays(1), LocalDate.now(), vehicleId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void calculatesMetricsForSpecificVehicleWithCompletedTrips() {
        UUID vehicleId = UUID.randomUUID();
        var vehicle = new FleetVehicleSummary(vehicleId, "WP-CAB-9999", "AVAILABLE", 15000.0, true);
        when(fleetReports.findVehicle(vehicleId)).thenReturn(Optional.of(vehicle));

        var trip1 = new VehicleTripReportItem(
                UUID.randomUUID(),
                "TRIP-01",
                "COMPLETED",
                vehicleId,
                OffsetDateTime.of(2026, 8, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                1000.0,
                1120.0
        );

        var trip2 = new VehicleTripReportItem(
                UUID.randomUUID(),
                "TRIP-02",
                "COMPLETED",
                vehicleId,
                OffsetDateTime.of(2026, 8, 2, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 2, 12, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 2, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 2, 12, 0, 0, 0, ZoneOffset.UTC),
                1120.0,
                1300.0
        );

        when(tripReports.findVehicleTrips(any(), any(), eq(vehicleId))).thenReturn(List.of(trip1, trip2));

        var result = service.getVehicleUtilizationReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), vehicleId);

        assertThat(result).hasSize(1);
        var record = result.get(0);
        assertThat(record.vehicleId()).isEqualTo(vehicleId);
        assertThat(record.registrationNumber()).isEqualTo("WP-CAB-9999");
        assertThat(record.totalAssignedTrips()).isEqualTo(2);
        assertThat(record.completedTrips()).isEqualTo(2);
        assertThat(record.totalDistanceKm()).isEqualTo(300.0); // 120 + 180
        assertThat(record.totalAllocatedHours()).isEqualTo(5.0); // 2h + 3h
    }

    @Test
    void calculatesMetricsForFleetWhenVehicleIdNull() {
        UUID vehicleId1 = UUID.randomUUID();
        UUID vehicleId2 = UUID.randomUUID();

        when(fleetReports.findAllVehicles()).thenReturn(List.of(
                new FleetVehicleSummary(vehicleId1, "WP-AAA-1111", "AVAILABLE", 5000.0, true),
                new FleetVehicleSummary(vehicleId2, "WP-BBB-2222", "MAINTENANCE", 8000.0, true)
        ));

        var trip1 = new VehicleTripReportItem(
                UUID.randomUUID(),
                "TRIP-01",
                "COMPLETED",
                vehicleId1,
                OffsetDateTime.of(2026, 8, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                null,
                null,
                1000.0,
                1100.0
        );

        when(tripReports.findVehicleTrips(any(), any(), eq(null))).thenReturn(List.of(trip1));

        var result = service.getVehicleUtilizationReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).registrationNumber()).isEqualTo("WP-AAA-1111");
        assertThat(result.get(0).totalAssignedTrips()).isEqualTo(1);
        assertThat(result.get(0).totalDistanceKm()).isEqualTo(100.0);

        assertThat(result.get(1).registrationNumber()).isEqualTo("WP-BBB-2222");
        assertThat(result.get(1).totalAssignedTrips()).isEqualTo(0);
        assertThat(result.get(1).totalDistanceKm()).isEqualTo(0.0);
    }
}
