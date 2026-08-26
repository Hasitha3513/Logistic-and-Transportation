package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.CoverageStatus;
import com.transportlogistics.app.fleet.VehicleMileageSummary;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingUnit;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers.VehicleReadingController;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordManualVehicleReadingRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordVehicleMeterResetRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordVehicleReadingCorrectionRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers.VehicleReadingWebMapper;
import com.transportlogistics.app.identity.AuthenticatedUserLookup;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VehicleReadingControllerTest {

    private static final UUID VEHICLE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime RECORDED_TIME = OffsetDateTime.parse("2026-08-16T10:00:00Z");

    @Mock
    private VehicleReadingUseCase readingUseCase;

    @Mock
    private AuthenticatedUserLookup userLookup;

    private VehicleReadingController controller;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var mapper = Mappers.getMapper(VehicleReadingWebMapper.class);
        controller = new VehicleReadingController(readingUseCase, userLookup, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void listReadingsReturnsPagedResults() throws Exception {
        var reading = sampleReading(UUID.randomUUID(), new BigDecimal("10500.000"), VehicleReadingType.ODOMETER);
        var pageResult = new VehicleReadingUseCase.PageResult<>(List.of(reading), 0, 20, 1L, 1);
        when(readingUseCase.list(any())).thenReturn(pageResult);

        mockMvc.perform(get("/vehicles/{vehicleId}/readings", VEHICLE_ID)
                        .param("page", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(reading.id().toString()))
                .andExpect(jsonPath("$.content[0].value").value(10500.0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getLatestReadingsReturnsSnapshots() throws Exception {
        var odo = sampleReading(UUID.randomUUID(), new BigDecimal("12000.000"), VehicleReadingType.ODOMETER);
        var engine = sampleReading(UUID.randomUUID(), new BigDecimal("450.000"), VehicleReadingType.ENGINE_HOURS);
        when(readingUseCase.latest(VEHICLE_ID)).thenReturn(new VehicleReadingUseCase.LatestReadings(
                VEHICLE_ID, Optional.of(odo), Optional.of(engine)
        ));

        mockMvc.perform(get("/vehicles/{vehicleId}/readings/latest", VEHICLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(VEHICLE_ID.toString()))
                .andExpect(jsonPath("$.odometer.value").value(12000.0))
                .andExpect(jsonPath("$.engineHours.value").value(450.0));
    }

    @Test
    void recordManualReadingCreatesResourceWith201() throws Exception {
        when(userLookup.findByUsername("operator")).thenReturn(Optional.of(
                new AuthenticatedUserLookup.AuthenticatedUser(ACTOR_ID, "operator")
        ));
        var created = sampleReading(UUID.randomUUID(), new BigDecimal("15000.000"), VehicleReadingType.ODOMETER);
        when(readingUseCase.record(any())).thenReturn(created);

        var request = new RecordManualVehicleReadingRequest(
                VehicleReadingType.ODOMETER,
                new BigDecimal("15000.000"),
                RECORDED_TIME,
                "manual-key-1",
                "Odometer check"
        );

        mockMvc.perform(post("/vehicles/{vehicleId}/readings", VEHICLE_ID)
                        .principal((Principal) () -> "operator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(created.id().toString()))
                .andExpect(jsonPath("$.value").value(15000.0))
                .andExpect(jsonPath("$.sourceType").value("MANUAL"));
    }

    @Test
    void correctReadingSucceeds() throws Exception {
        var readingId = UUID.randomUUID();
        when(userLookup.findByUsername("operator")).thenReturn(Optional.of(
                new AuthenticatedUserLookup.AuthenticatedUser(ACTOR_ID, "operator")
        ));
        var corrected = sampleReading(UUID.randomUUID(), new BigDecimal("15500.000"), VehicleReadingType.ODOMETER);
        when(readingUseCase.correct(any())).thenReturn(corrected);

        var request = new RecordVehicleReadingCorrectionRequest(
                new BigDecimal("15500.000"),
                "Corrected typo",
                RECORDED_TIME
        );

        mockMvc.perform(post("/vehicles/{vehicleId}/readings/{readingId}/correct", VEHICLE_ID, readingId)
                        .principal((Principal) () -> "operator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(15500.0));
    }

    @Test
    void resetMeterSucceedsWith201() throws Exception {
        when(userLookup.findByUsername("operator")).thenReturn(Optional.of(
                new AuthenticatedUserLookup.AuthenticatedUser(ACTOR_ID, "operator")
        ));
        var reset = new VehicleMeterReset(
                UUID.randomUUID(), VEHICLE_ID, VehicleReadingType.ODOMETER, 0, 1,
                new BigDecimal("250000.000"), new BigDecimal("0.000"), RECORDED_TIME,
                "Meter replaced", ACTOR_ID, RECORDED_TIME
        );
        when(readingUseCase.resetMeter(any())).thenReturn(reset);

        var request = new RecordVehicleMeterResetRequest(
                VehicleReadingType.ODOMETER,
                new BigDecimal("0.000"),
                RECORDED_TIME,
                "Meter replaced"
        );

        mockMvc.perform(post("/vehicles/{vehicleId}/meter-resets", VEHICLE_ID)
                        .principal((Principal) () -> "operator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromEpoch").value(0))
                .andExpect(jsonPath("$.toEpoch").value(1))
                .andExpect(jsonPath("$.newMeterValue").value(0.0));
    }

    @Test
    void getMileageReturnsSummary() throws Exception {
        var summary = new VehicleMileageSummary(
                VEHICLE_ID, RECORDED_TIME.minusDays(7), RECORDED_TIME,
                new BigDecimal("10000.000"), new BigDecimal("10500.000"), new BigDecimal("500.000"),
                new BigDecimal("200.000"), new BigDecimal("210.000"), new BigDecimal("10.000"),
                0, CoverageStatus.COMPLETE, false
        );
        when(readingUseCase.getMileage(VEHICLE_ID, null, null)).thenReturn(summary);

        mockMvc.perform(get("/vehicles/{vehicleId}/mileage", VEHICLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(VEHICLE_ID.toString()))
                .andExpect(jsonPath("$.distanceTravelledKm").value(500.0))
                .andExpect(jsonPath("$.coverageStatus").value("COMPLETE"));
    }

    private VehicleReading sampleReading(UUID id, BigDecimal value, VehicleReadingType type) {
        return new VehicleReading(
                id, VEHICLE_ID, type, value,
                type.unit(),
                0, VehicleReadingSourceType.MANUAL, null, RECORDED_TIME, RECORDED_TIME,
                ACTOR_ID, null, null, "key-" + id, "test notes", RECORDED_TIME
        );
    }
}