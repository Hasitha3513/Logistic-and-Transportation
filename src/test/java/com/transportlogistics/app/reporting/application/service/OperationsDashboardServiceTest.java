package com.transportlogistics.app.reporting.application.service;

import com.transportlogistics.app.fleet.FleetDocumentAlert;
import com.transportlogistics.app.fleet.FleetDriverSummary;
import com.transportlogistics.app.fleet.FleetExceptionAlert;
import com.transportlogistics.app.fleet.FleetVehicleSummary;
import com.transportlogistics.app.reporting.application.ports.out.FleetReportReadPort;
import com.transportlogistics.app.reporting.application.ports.out.TripReportReadPort;
import com.transportlogistics.app.trip.TripReportItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationsDashboardServiceTest {

    @Mock
    private FleetReportReadPort fleetReports;

    @Mock
    private TripReportReadPort tripReports;

    private OperationsDashboardService service;

    @BeforeEach
    void setUp() {
        service = new OperationsDashboardService(fleetReports, tripReports);
    }

    @Test
    void calculatesOperationalMetricsAccurately() {
        var v1 = new FleetVehicleSummary(UUID.randomUUID(), "WP-CAB-1201", "AVAILABLE", 1000.0, true);
        var v2 = new FleetVehicleSummary(UUID.randomUUID(), "WP-CAA-2202", "ALLOCATED", 2000.0, true);
        var v3 = new FleetVehicleSummary(UUID.randomUUID(), "CP-CAB-3303", "MAINTENANCE", 3000.0, true);
        var v4 = new FleetVehicleSummary(UUID.randomUUID(), "WP-CAD-5505", "OUT_OF_SERVICE", 4000.0, false);
        when(fleetReports.findAllVehicles()).thenReturn(List.of(v1, v2, v3, v4));

        var d1 = new FleetDriverSummary(UUID.randomUUID(), "DRV-001", "Kasun", "Silva", "AVAILABLE", true);
        var d2 = new FleetDriverSummary(UUID.randomUUID(), "DRV-002", "Nuwan", "Fernando", "ASSIGNED", true);
        var d3 = new FleetDriverSummary(UUID.randomUUID(), "DRV-003", "Ruwan", "Perera", "SUSPENDED", false);
        when(fleetReports.findAllDrivers()).thenReturn(List.of(d1, d2, d3));

        var now = OffsetDateTime.now();
        var t1 = new TripReportItem(UUID.randomUUID(), "TRIP-01", "DRAFT", UUID.randomUUID(), null, null, null, now, now, null, null, null, null, null, now);
        var t2 = new TripReportItem(UUID.randomUUID(), "TRIP-02", "SUBMITTED", UUID.randomUUID(), null, null, null, now, now, null, null, null, null, null, now);
        var t3 = new TripReportItem(UUID.randomUUID(), "TRIP-03", "APPROVED", UUID.randomUUID(), null, null, null, now, now, null, null, null, null, null, now);
        var t4 = new TripReportItem(UUID.randomUUID(), "TRIP-04", "ASSIGNED", UUID.randomUUID(), null, null, null, now, now, null, null, null, null, null, now);
        var t5 = new TripReportItem(UUID.randomUUID(), "TRIP-05", "DISPATCHED", UUID.randomUUID(), null, null, null, now, now, null, null, null, null, null, now);
        var t6 = new TripReportItem(UUID.randomUUID(), "TRIP-06", "IN_PROGRESS", UUID.randomUUID(), null, null, null, now, now, null, null, null, null, null, now);
        var t7 = new TripReportItem(UUID.randomUUID(), "TRIP-07", "COMPLETED", UUID.randomUUID(), null, null, null, now, now, null, null, null, null, null, now);
        when(tripReports.findAllTrips()).thenReturn(List.of(t1, t2, t3, t4, t5, t6, t7));

        var docAlert = new FleetDocumentAlert(UUID.randomUUID(), "INSURANCE", "INS-001", "WP-CAB-1201", LocalDate.now().plusDays(10), "WARNING");
        when(fleetReports.findExpiringDocuments(any())).thenReturn(List.of(docAlert));

        var excAlert = new FleetExceptionAlert(UUID.randomUUID(), "MEDICAL_LEAVE", "Kasun Silva", "CRITICAL", "ACTIVE", "Knee therapy");
        when(fleetReports.findActiveExceptions()).thenReturn(List.of(excAlert));

        var response = service.getOperationsDashboard(LocalDate.of(2026, 8, 23));

        assertThat(response.status()).isEqualTo("READY");
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 8, 23));

        // Vehicles: 1 available, 1 allocated, 1 maintenance, 1 out of service => total 4, 1/4 = 25%
        assertThat(response.vehicles().available()).isEqualTo(1);
        assertThat(response.vehicles().allocated()).isEqualTo(1);
        assertThat(response.vehicles().maintenance()).isEqualTo(1);
        assertThat(response.vehicles().outOfService()).isEqualTo(1);
        assertThat(response.vehicles().availabilityPercent()).isEqualTo(25);

        // Drivers: 1 available, 1 assigned (1 inactive ignored) => total 2, 1/2 = 50%
        assertThat(response.drivers().available()).isEqualTo(1);
        assertThat(response.drivers().assigned()).isEqualTo(1);
        assertThat(response.drivers().availabilityPercent()).isEqualTo(50);

        // Trips: 1 of each => 7 total, 1 completed => 1/7 = 14%
        assertThat(response.trips().draft()).isEqualTo(1);
        assertThat(response.trips().pendingApproval()).isEqualTo(1);
        assertThat(response.trips().approved()).isEqualTo(1);
        assertThat(response.trips().assigned()).isEqualTo(1);
        assertThat(response.trips().dispatched()).isEqualTo(1);
        assertThat(response.trips().inProgress()).isEqualTo(1);
        assertThat(response.trips().completed()).isEqualTo(1);
        assertThat(response.trips().completionPercent()).isEqualTo(14);

        // Alerts
        assertThat(response.alerts().expiringDocuments()).hasSize(1);
        assertThat(response.alerts().expiringDocuments().get(0).title()).contains("INSURANCE");
        assertThat(response.alerts().criticalExceptions()).hasSize(1);
        assertThat(response.alerts().criticalExceptions().get(0).detail()).contains("Kasun Silva");
    }
}
