package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.DriverPerformanceUseCase;
import com.transportlogistics.app.fleet.application.ports.in.DriverViolationUseCase;
import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.DriverViolationRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.PayFineRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.WaiveFineRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers.FleetWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FleetControllerViolationTest {

    private MockMvc mockMvc;
    private DriverViolationUseCase driverViolations;
    private DriverPerformanceUseCase driverPerformance;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final UUID driverId = UUID.randomUUID();
    private final UUID violationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        driverViolations = mock(DriverViolationUseCase.class);
        driverPerformance = mock(DriverPerformanceUseCase.class);
        var mapper = Mappers.getMapper(FleetWebMapper.class);

        var controller = new FleetController(
                mock(com.transportlogistics.app.fleet.application.ports.in.DriverUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.DriverAvailabilityUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.DriverLicenseUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.DriverExceptionUseCase.class),
                driverViolations,
                driverPerformance,
                mock(com.transportlogistics.app.fleet.vehiclemaster.ports.inbound.VehicleUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleAvailabilityUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleCategoryUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleTypeUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleDocumentUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.MaintenanceScheduleUseCase.class),
                mapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void recordsDriverViolationSuccessfully() throws Exception {
        var date = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var request = new DriverViolationRequest(
                null,
                DriverViolationType.SPEEDING,
                ViolationSeverity.MODERATE,
                date,
                3,
                new BigDecimal("150.00"),
                "Highway 101",
                "Speeding violation"
        );

        var violation = new DriverViolation(
                violationId, driverId, null, DriverViolationType.SPEEDING, ViolationSeverity.MODERATE,
                date, 3, new BigDecimal("150.00"), FinePaymentStatus.UNPAID, null, null,
                "Highway 101", "Speeding violation", OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );

        when(driverViolations.recordViolation(any())).thenReturn(violation);

        mockMvc.perform(post("/drivers/" + driverId + "/violations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(violationId.toString()))
                .andExpect(jsonPath("$.violationType").value("SPEEDING"))
                .andExpect(jsonPath("$.fineAmount").value(150.00))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));
    }

    @Test
    void listsDriverViolations() throws Exception {
        var date = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var violation = new DriverViolation(
                violationId, driverId, null, DriverViolationType.RED_LIGHT, ViolationSeverity.MAJOR,
                date, 4, new BigDecimal("250.00"), FinePaymentStatus.UNPAID, null, null,
                "Downtown", "Red light", OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );

        when(driverViolations.listViolations(driverId)).thenReturn(List.of(violation));

        mockMvc.perform(get("/drivers/" + driverId + "/violations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(violationId.toString()))
                .andExpect(jsonPath("$[0].violationType").value("RED_LIGHT"));
    }

    @Test
    void paysDriverViolationFine() throws Exception {
        var payRequest = new PayFineRequest(OffsetDateTime.now(), "REC-555");
        var date = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var violation = new DriverViolation(
                violationId, driverId, null, DriverViolationType.SPEEDING, ViolationSeverity.MINOR,
                date, 1, new BigDecimal("50.00"), FinePaymentStatus.PAID, OffsetDateTime.now(), "REC-555",
                "Highway", "desc", OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );

        when(driverViolations.payFine(any())).thenReturn(violation);

        mockMvc.perform(post("/drivers/" + driverId + "/violations/" + violationId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentReference").value("REC-555"));
    }

    @Test
    void waivesDriverViolationFine() throws Exception {
        var waiveRequest = new WaiveFineRequest("First offence waiver");
        var date = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var violation = new DriverViolation(
                violationId, driverId, null, DriverViolationType.UNAUTHORIZED_STOP, ViolationSeverity.MINOR,
                date, 0, new BigDecimal("50.00"), FinePaymentStatus.WAIVED, null, null,
                "Depot", "desc", OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );

        when(driverViolations.waiveFine(any())).thenReturn(violation);

        mockMvc.perform(post("/drivers/" + driverId + "/violations/" + violationId + "/waive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(waiveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("WAIVED"));
    }

    @Test
    void getsDriverPerformanceSummary() throws Exception {
        var summary = new DriverPerformanceSummary(
                driverId,
                "Jane Doe",
                20,
                19,
                1,
                95.0,
                1,
                2,
                0,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                92,
                PerformanceRating.EXCELLENT,
                OffsetDateTime.now()
        );

        when(driverPerformance.getPerformanceSummary(driverId)).thenReturn(summary);

        mockMvc.perform(get("/drivers/" + driverId + "/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value(driverId.toString()))
                .andExpect(jsonPath("$.driverName").value("Jane Doe"))
                .andExpect(jsonPath("$.safetyScore").value(92))
                .andExpect(jsonPath("$.overallRating").value("EXCELLENT"));
    }
}
