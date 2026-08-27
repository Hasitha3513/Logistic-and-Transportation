package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web;

import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.controllers.LoadPlanController;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.mappers.LoadPlanWebMapper;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanItemPlacement;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanViolation;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanViolationCode;
import com.transportlogistics.app.freight.loadplanning.domain.LoadValidationResult;
import com.transportlogistics.app.freight.loadplanning.domain.LoadValidationViolation;
import com.transportlogistics.app.freight.loadplanning.domain.ValidationOutcome;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.LoadPlanUseCase;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoadPlanControllerTest {

    private LoadPlanUseCase loadPlanUseCase;
    private MockMvc mvc;
    private final UUID id = UUID.randomUUID();
    private final UUID manifestId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        loadPlanUseCase = mock(LoadPlanUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new LoadPlanController(loadPlanUseCase, new LoadPlanWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsLoadPlan() throws Exception {
        when(loadPlanUseCase.create(any(), eq("manager"))).thenReturn(samplePlan());

        String json = """
                {
                    "cargoManifestId": "%s",
                    "vehicleId": "%s",
                    "notes": "Urgent load",
                    "placements": []
                }
                """.formatted(manifestId, vehicleId);

        mvc.perform(post("/v1/freight/load-plans")
                        .principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loadPlanNumber").value("LP-2026-000001"))
                .andExpect(jsonPath("$.cargoManifestId").value(manifestId.toString()))
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()));
    }

    @Test
    void validatesRequiredFieldsOnCreate() throws Exception {
        mvc.perform(post("/v1/freight/load-plans")
                        .principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(loadPlanUseCase);
    }

    @Test
    void getsLoadPlanById() throws Exception {
        when(loadPlanUseCase.get(id)).thenReturn(samplePlan());

        mvc.perform(get("/v1/freight/load-plans/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.loadPlanNumber").value("LP-2026-000001"));
    }

    @Test
    void mapsNotFoundTo404() throws Exception {
        when(loadPlanUseCase.get(id)).thenThrow(new NotFoundException("LOAD_PLAN_NOT_FOUND", "Not found"));

        mvc.perform(get("/v1/freight/load-plans/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOAD_PLAN_NOT_FOUND"));
    }

    @Test
    void mapsStaleVersionTo409() throws Exception {
        when(loadPlanUseCase.update(eq(id), any(), eq("manager")))
                .thenThrow(new ConflictException("LOAD_PLAN_CONCURRENT_UPDATE", "Stale update"));

        String json = """
                {
                    "vehicleId": "%s",
                    "version": 0,
                    "placements": []
                }
                """.formatted(vehicleId);

        mvc.perform(patch("/v1/freight/load-plans/{id}", id)
                        .principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAD_PLAN_CONCURRENT_UPDATE"));
    }

    @Test
    void validatesLayoutEndpoint() throws Exception {
        when(loadPlanUseCase.validateLayout(id)).thenReturn(List.of(
                new LoadPlanViolation(LoadPlanViolationCode.ITEM_NOT_PLACED, "Item 1 unplaced"),
                new LoadPlanViolation(LoadPlanViolationCode.LOAD_PLAN_SPECIAL_CARGO_CLASSIFICATION_MISSING, "Special cargo classification missing"),
                new LoadPlanViolation(LoadPlanViolationCode.LOAD_PLAN_FRAGILE_RULE_FAILED, "Fragile rule failed"),
                new LoadPlanViolation(LoadPlanViolationCode.LOAD_PLAN_TEMPERATURE_RULE_FAILED, "Temperature rule failed")
        ));

        mvc.perform(post("/v1/freight/load-plans/{id}/validate-layout", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.violations[0].code").value("ITEM_NOT_PLACED"))
                .andExpect(jsonPath("$.violations[1].code").value("LOAD_PLAN_SPECIAL_CARGO_CLASSIFICATION_MISSING"))
                .andExpect(jsonPath("$.violations[2].code").value("LOAD_PLAN_FRAGILE_RULE_FAILED"))
                .andExpect(jsonPath("$.violations[3].code").value("LOAD_PLAN_TEMPERATURE_RULE_FAILED"));
    }

    @Test
    void validatesWeightAndVolumeEndpoint() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-25T10:00:00Z");
        LoadValidationResult result = new LoadValidationResult(
                id,
                now,
                "manager",
                ValidationOutcome.INCOMPLETE,
                null,
                null,
                null,
                ValidationOutcome.INCOMPLETE,
                ValidationOutcome.INCOMPLETE,
                ValidationOutcome.INCOMPLETE,
                List.of(new LoadValidationViolation("LOAD_WEIGHT_DATA_MISSING", "Weight missing")),
                List.of("CARGO_ITEM_WEIGHT_DATA_MISSING")
        );

        when(loadPlanUseCase.validateWeightAndVolume(eq(id), eq("manager"))).thenReturn(result);

        mvc.perform(post("/v1/freight/load-plans/{id}/validate-weight-volume", id)
                        .principal(() -> "manager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallOutcome").value("INCOMPLETE"))
                .andExpect(jsonPath("$.payloadResult").value("INCOMPLETE"))
                .andExpect(jsonPath("$.violations[0].code").value("LOAD_WEIGHT_DATA_MISSING"))
                .andExpect(jsonPath("$.missingData[0]").value("CARGO_ITEM_WEIGHT_DATA_MISSING"));
    }

    @Test
    void validatesWeightAndVolumePassOutcome() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-25T10:00:00Z");
        LoadValidationResult result = new LoadValidationResult(
                id,
                now,
                "manager",
                ValidationOutcome.PASS,
                new BigDecimal("4500.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("90.00"),
                new BigDecimal("20.000"),
                new BigDecimal("25.000"),
                new BigDecimal("80.00"),
                new BigDecimal("7500.00"),
                new BigDecimal("8000.00"),
                new BigDecimal("3000.00"),
                ValidationOutcome.PASS,
                ValidationOutcome.PASS,
                ValidationOutcome.PASS,
                ValidationOutcome.INCOMPLETE,
                List.of(),
                List.of()
        );

        when(loadPlanUseCase.validateWeightAndVolume(eq(id), eq("manager"))).thenReturn(result);

        mvc.perform(post("/v1/freight/load-plans/{id}/validate-weight-volume", id)
                        .principal(() -> "manager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallOutcome").value("PASS"))
                .andExpect(jsonPath("$.cargoWeightKg").value(4500.00))
                .andExpect(jsonPath("$.payloadCapacityKg").value(5000.00))
                .andExpect(jsonPath("$.payloadUtilizationPercent").value(90.00))
                .andExpect(jsonPath("$.cargoVolumeM3").value(20.000))
                .andExpect(jsonPath("$.volumeCapacityM3").value(25.000))
                .andExpect(jsonPath("$.volumeUtilizationPercent").value(80.00))
                .andExpect(jsonPath("$.projectedGrossWeightKg").value(7500.00))
                .andExpect(jsonPath("$.grossWeightLimitKg").value(8000.00))
                .andExpect(jsonPath("$.payloadResult").value("PASS"))
                .andExpect(jsonPath("$.volumeResult").value("PASS"))
                .andExpect(jsonPath("$.gvwResult").value("PASS"));
    }

    @Test
    void validatesWeightAndVolumeFailOutcome() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-25T10:00:00Z");
        LoadValidationResult result = new LoadValidationResult(
                id,
                now,
                "manager",
                ValidationOutcome.FAIL,
                new BigDecimal("6000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("120.00"),
                new BigDecimal("28.000"),
                new BigDecimal("25.000"),
                new BigDecimal("112.00"),
                new BigDecimal("9500.00"),
                new BigDecimal("8000.00"),
                new BigDecimal("3500.00"),
                ValidationOutcome.FAIL,
                ValidationOutcome.FAIL,
                ValidationOutcome.FAIL,
                ValidationOutcome.INCOMPLETE,
                List.of(
                        new LoadValidationViolation("VEHICLE_PAYLOAD_EXCEEDED", "Payload exceeded"),
                        new LoadValidationViolation("VEHICLE_VOLUME_CAPACITY_EXCEEDED", "Volume exceeded"),
                        new LoadValidationViolation("VEHICLE_GVW_EXCEEDED", "GVW exceeded")
                ),
                List.of()
        );

        when(loadPlanUseCase.validateWeightAndVolume(eq(id), eq("manager"))).thenReturn(result);

        mvc.perform(post("/v1/freight/load-plans/{id}/validate-weight-volume", id)
                        .principal(() -> "manager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallOutcome").value("FAIL"))
                .andExpect(jsonPath("$.payloadResult").value("FAIL"))
                .andExpect(jsonPath("$.volumeResult").value("FAIL"))
                .andExpect(jsonPath("$.gvwResult").value("FAIL"))
                .andExpect(jsonPath("$.violations[0].code").value("VEHICLE_PAYLOAD_EXCEEDED"))
                .andExpect(jsonPath("$.violations[1].code").value("VEHICLE_VOLUME_CAPACITY_EXCEEDED"))
                .andExpect(jsonPath("$.violations[2].code").value("VEHICLE_GVW_EXCEEDED"));
    }

    @Test
    void marksLoadPlanStructurallyReady() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-25T10:00:00Z");
        LoadPlanItemPlacement placement = new LoadPlanItemPlacement(
                UUID.randomUUID(), UUID.randomUUID(), 0, "FRONT", "S1", "PALLET-1", 1, null
        );
        LoadPlan readyPlan = new LoadPlan(
                id,
                "LP-2026-000001",
                manifestId,
                vehicleId,
                List.of(placement),
                "Notes",
                com.transportlogistics.app.freight.loadplanning.domain.LoadPlanReadinessStatus.STRUCTURALLY_READY,
                now,
                "manager",
                now,
                now,
                "manager",
                "manager",
                1L
        );

        when(loadPlanUseCase.markReady(eq(id), eq(0L), eq("manager"))).thenReturn(readyPlan);

        mvc.perform(post("/v1/freight/load-plans/{id}/ready", id)
                        .principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readinessStatus").value("STRUCTURALLY_READY"))
                .andExpect(jsonPath("$.readyBy").value("manager"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void rejectsMarkReadyWithoutVersion() throws Exception {
        mvc.perform(post("/v1/freight/load-plans/{id}/ready", id)
                        .principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsMarkReadyOnStaleVersion() throws Exception {
        when(loadPlanUseCase.markReady(eq(id), eq(0L), eq("manager")))
                .thenThrow(new ConflictException("LOAD_PLAN_STALE_VERSION", "Stale ready attempt"));

        mvc.perform(post("/v1/freight/load-plans/{id}/ready", id)
                        .principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAD_PLAN_STALE_VERSION"));
    }

    @Test
    void rejectsMarkReadyOnStructuralViolations() throws Exception {
        when(loadPlanUseCase.markReady(eq(id), eq(0L), eq("manager")))
                .thenThrow(new com.transportlogistics.app.shared.domain.BusinessRuleException(
                        "LOAD_PLAN_STRUCTURAL_VIOLATIONS", "ITEM_NOT_PLACED: Item unplaced"
                ));

        mvc.perform(post("/v1/freight/load-plans/{id}/ready", id)
                        .principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOAD_PLAN_STRUCTURAL_VIOLATIONS"));
    }

    @Test
    void rejectsMarkReadyWhenNotFound() throws Exception {
        when(loadPlanUseCase.markReady(eq(id), eq(0L), eq("manager")))
                .thenThrow(new com.transportlogistics.app.shared.domain.NotFoundException(
                        "LOAD_PLAN_NOT_FOUND", "Load plan not found: " + id
                ));

        mvc.perform(post("/v1/freight/load-plans/{id}/ready", id)
                        .principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOAD_PLAN_NOT_FOUND"));
    }

    private LoadPlan samplePlan() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-25T10:00:00Z");
        LoadPlanItemPlacement placement = new LoadPlanItemPlacement(
                UUID.randomUUID(), UUID.randomUUID(), 0, "FRONT", "S1", "PALLET-1", 1, null
        );
        return new LoadPlan(
                id,
                "LP-2026-000001",
                manifestId,
                vehicleId,
                List.of(placement),
                "Notes",
                now,
                now,
                "manager",
                "manager",
                0L
        );
    }
}
