package com.transportlogistics.app.reporting.infrastructure.adapters.in.web;

import com.transportlogistics.app.reporting.application.model.DriverAssignmentReportRecord;
import com.transportlogistics.app.reporting.application.model.TripReportRecord;
import com.transportlogistics.app.reporting.application.model.VehicleUtilizationReportRecord;
import com.transportlogistics.app.reporting.application.ports.in.DriverAssignmentUseCase;
import com.transportlogistics.app.reporting.application.ports.in.TripReportUseCase;
import com.transportlogistics.app.reporting.application.ports.in.VehicleUtilizationUseCase;
import com.transportlogistics.app.reporting.application.ports.in.FreightReportUseCase;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.controllers.ReportingController;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.mappers.ReportingWebMapper;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transportlogistics.app.reporting.application.ports.in.OperationsDashboardUseCase;
import com.transportlogistics.app.reporting.domain.model.OperationsDashboard;

class ReportingControllerTest {

    private MockMvc mockMvc;
    private OperationsDashboardUseCase operationsDashboard;
    private TripReportUseCase tripReports;
    private DriverAssignmentUseCase driverAssignments;
    private VehicleUtilizationUseCase vehicleUtilization;
    private FreightReportUseCase freightReports;

    @BeforeEach
    void setUp() {
        operationsDashboard = mock(OperationsDashboardUseCase.class);
        tripReports = mock(TripReportUseCase.class);
        driverAssignments = mock(DriverAssignmentUseCase.class);
        vehicleUtilization = mock(VehicleUtilizationUseCase.class);
        freightReports = mock(FreightReportUseCase.class);
        var mapper = Mappers.getMapper(ReportingWebMapper.class);

        mockMvc = MockMvcBuilders.standaloneSetup(new ReportingController(operationsDashboard, tripReports, driverAssignments,
                        vehicleUtilization, freightReports, mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getTripReportReturnsOkWithPaginatedContent() throws Exception {
        UUID tripId = UUID.randomUUID();
        var record = new TripReportRecord(
                tripId,
                "TRIP-100",
                "COMPLETED",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                UUID.randomUUID(),
                "WP-CAB-1234",
                UUID.randomUUID(),
                "DRV-01",
                "Alice Bob",
                UUID.randomUUID(),
                120.0,
                UUID.randomUUID(),
                "Done",
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        when(tripReports.getTripReport(any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(record), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/reports/trips")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .param("page", "0")
                        .param("limit", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tripId").value(tripId.toString()))
                .andExpect(jsonPath("$.content[0].tripNumber").value("TRIP-100"))
                .andExpect(jsonPath("$.content[0].vehicleRegistrationNumber").value("WP-CAB-1234"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getDriverAssignmentReportReturnsOk() throws Exception {
        UUID driverId = UUID.randomUUID();
        var record = new DriverAssignmentReportRecord(
                driverId,
                "DRV-02",
                "Charlie",
                "AVAILABLE",
                UUID.randomUUID(),
                "TRIP-200",
                "ASSIGNED",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                null,
                null,
                UUID.randomUUID(),
                "WP-CAD-5555",
                UUID.randomUUID(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        when(driverAssignments.getDriverAssignmentReport(any(), any(), eq(driverId)))
                .thenReturn(List.of(record));

        mockMvc.perform(get("/reports/driver-assignments")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .param("driverId", driverId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].driverId").value(driverId.toString()))
                .andExpect(jsonPath("$[0].driverName").value("Charlie"))
                .andExpect(jsonPath("$[0].tripNumber").value("TRIP-200"));
    }

    @Test
    void getVehicleUtilizationReportReturnsOk() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        var record = new VehicleUtilizationReportRecord(
                vehicleId,
                "WP-CAB-7777",
                "AVAILABLE",
                5,
                4,
                850.5,
                18.0
        );

        when(vehicleUtilization.getVehicleUtilizationReport(any(), any(), eq(vehicleId)))
                .thenReturn(List.of(record));

        mockMvc.perform(get("/reports/vehicle-utilization")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-10")
                        .param("vehicleId", vehicleId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value(vehicleId.toString()))
                .andExpect(jsonPath("$[0].registrationNumber").value("WP-CAB-7777"))
                .andExpect(jsonPath("$[0].totalAssignedTrips").value(5))
                .andExpect(jsonPath("$[0].completedTrips").value(4))
                .andExpect(jsonPath("$[0].totalDistanceKm").value(850.5));
    }

    @Test
    void returns400WhenDateRangeInvalid() throws Exception {
        when(tripReports.getTripReport(any(), any(), anyInt(), anyInt(), any(), any()))
                .thenThrow(new BusinessRuleException("INVALID_DATE_RANGE", "fromDate cannot be after toDate"));

        mockMvc.perform(get("/reports/trips")
                        .param("fromDate", "2026-08-10")
                        .param("toDate", "2026-08-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    void getOperationsDashboardReturnsFullMetrics() throws Exception {
        var response = new OperationsDashboard(
                LocalDate.of(2026, 8, 23),
                "READY",
                new OperationsDashboard.VehicleMetrics(5, 3, 1, 1, 50),
                new OperationsDashboard.DriverMetrics(4, 2, 67),
                new OperationsDashboard.TripMetrics(1, 1, 1, 1, 1, 1, 4, 40),
                new OperationsDashboard.DashboardAlerts(
                        List.of(new OperationsDashboard.DashboardAlertItem("a-1", "Insurance expiring", "WP-CAB-1201", "WARNING", "2026-09-01")),
                        List.of()
                )
        );

        when(operationsDashboard.getOperationsDashboard(any())).thenReturn(response);

        mockMvc.perform(get("/dashboard/operations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.vehicles.available").value(5))
                .andExpect(jsonPath("$.drivers.available").value(4))
                .andExpect(jsonPath("$.trips.completed").value(4))
                .andExpect(jsonPath("$.alerts.expiringDocuments[0].title").value("Insurance expiring"));
    }
}
