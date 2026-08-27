package com.transportlogistics.app.freight.exception.adapters.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.controllers.CargoExceptionController;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.CreateCargoExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.EscalateExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.HoldExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.RejectExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.ReleaseExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.ResolveExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.mappers.CargoExceptionWebMapper;
import com.transportlogistics.app.freight.exception.domain.CargoException;
import com.transportlogistics.app.freight.exception.domain.CargoExceptionHistoryEntry;
import com.transportlogistics.app.freight.exception.domain.ExceptionSeverity;
import com.transportlogistics.app.freight.exception.domain.ExceptionStatus;
import com.transportlogistics.app.freight.exception.domain.ExceptionType;
import com.transportlogistics.app.freight.exception.ports.inbound.CargoExceptionUseCase;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
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

class CargoExceptionControllerTest {

    private CargoExceptionUseCase useCase;
    private MockMvc mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID exceptionId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-27T10:00:00Z");

    @BeforeEach
    void setUp() {
        useCase = mock(CargoExceptionUseCase.class);
        CargoExceptionWebMapper mapper = new CargoExceptionWebMapper();
        CargoExceptionController controller = new CargoExceptionController(useCase, mapper);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CargoException stubException(ExceptionStatus status) {
        return new CargoException(
                exceptionId,
                "CEX-2026-000001",
                ExceptionType.DAMAGE,
                status,
                ExceptionSeverity.HIGH,
                orderId,
                null,
                null,
                "Pallet crushed during cross-dock",
                "Crushed packaging",
                status == ExceptionStatus.HELD ? "Hold for Bay 3" : null,
                "Repackage into crate",
                status == ExceptionStatus.RESOLVED ? "Repackaged and cleared" : null,
                status == ExceptionStatus.RESOLVED ? now : null,
                status == ExceptionStatus.RESOLVED ? "test_officer" : null,
                List.of(new CargoExceptionHistoryEntry(
                        UUID.randomUUID(),
                        exceptionId,
                        "RECORDED",
                        "test_officer",
                        now,
                        "Initial report",
                        null
                )),
                now,
                now,
                "test_officer",
                "test_officer",
                0L
        );
    }

    @Test
    @DisplayName("POST /v1/freight/exceptions - Success (201)")
    void recordException_success() throws Exception {
        when(useCase.record(any(), any())).thenReturn(stubException(ExceptionStatus.OPEN));

        CreateCargoExceptionRequest req = new CreateCargoExceptionRequest(
                ExceptionType.DAMAGE,
                ExceptionSeverity.HIGH,
                orderId,
                null,
                null,
                "Pallet crushed during cross-dock",
                "Crushed packaging",
                "Hold for Bay 3",
                "Repackage into crate"
        );

        mvc.perform(post("/v1/freight/exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(exceptionId.toString()))
                .andExpect(jsonPath("$.exceptionNumber").value("CEX-2026-000001"))
                .andExpect(jsonPath("$.exceptionType").value("DAMAGE"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.severity").value("HIGH"));
    }

    @Test
    @DisplayName("GET /v1/freight/exceptions - List all (200)")
    void listExceptions_success() throws Exception {
        when(useCase.list(any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(List.of(stubException(ExceptionStatus.OPEN)));

        mvc.perform(get("/v1/freight/exceptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(exceptionId.toString()))
                .andExpect(jsonPath("$[0].exceptionNumber").value("CEX-2026-000001"));
    }

    @Test
    @DisplayName("GET /v1/freight/exceptions/{id} - Success (200)")
    void getException_success() throws Exception {
        when(useCase.get(exceptionId)).thenReturn(stubException(ExceptionStatus.OPEN));

        mvc.perform(get("/v1/freight/exceptions/{id}", exceptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exceptionId.toString()))
                .andExpect(jsonPath("$.description").value("Pallet crushed during cross-dock"));
    }

    @Test
    @DisplayName("GET /v1/freight/exceptions/{id} - Not Found (404)")
    void getException_notFound() throws Exception {
        when(useCase.get(exceptionId))
                .thenThrow(new NotFoundException("CARGO_EXCEPTION_NOT_FOUND", "Not found"));

        mvc.perform(get("/v1/freight/exceptions/{id}", exceptionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARGO_EXCEPTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /v1/freight/exceptions/{id}/hold - Success (200)")
    void holdException_success() throws Exception {
        when(useCase.hold(eq(exceptionId), any(), any())).thenReturn(stubException(ExceptionStatus.HELD));

        HoldExceptionRequest req = new HoldExceptionRequest("Hold for Bay 3", "Physical deformation", 0L);

        mvc.perform(post("/v1/freight/exceptions/{id}/hold", exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HELD"));
    }

    @Test
    @DisplayName("POST /v1/freight/exceptions/{id}/escalate - Success (200)")
    void escalateException_success() throws Exception {
        when(useCase.escalate(eq(exceptionId), any(), any())).thenReturn(stubException(ExceptionStatus.ESCALATED));

        EscalateExceptionRequest req = new EscalateExceptionRequest("Critical damage, supervisor review required", 0L);

        mvc.perform(post("/v1/freight/exceptions/{id}/escalate", exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"));
    }

    @Test
    @DisplayName("POST /v1/freight/exceptions/{id}/release - Success (200)")
    void releaseException_success() throws Exception {
        when(useCase.release(eq(exceptionId), any(), any())).thenReturn(stubException(ExceptionStatus.OPEN));

        ReleaseExceptionRequest req = new ReleaseExceptionRequest("Cleared by safety officer", 0L);

        mvc.perform(post("/v1/freight/exceptions/{id}/release", exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /v1/freight/exceptions/{id}/reject - Success (200)")
    void rejectException_success() throws Exception {
        when(useCase.reject(eq(exceptionId), any(), any())).thenReturn(stubException(ExceptionStatus.REJECTED));

        RejectExceptionRequest req = new RejectExceptionRequest("False report, pre-existing container mark", 0L);

        mvc.perform(post("/v1/freight/exceptions/{id}/reject", exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("POST /v1/freight/exceptions/{id}/resolve - Success (200)")
    void resolveException_success() throws Exception {
        when(useCase.resolve(eq(exceptionId), any(), any())).thenReturn(stubException(ExceptionStatus.RESOLVED));

        ResolveExceptionRequest req = new ResolveExceptionRequest(
                "Pallet repackaged and cleared for dispatch",
                "Replaced outer crate",
                "Passed QC check",
                0L
        );

        mvc.perform(post("/v1/freight/exceptions/{id}/resolve", exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolution").value("Repackaged and cleared"));
    }

    @Test
    @DisplayName("POST /v1/freight/exceptions/{id}/resolve - Conflict Exception (409)")
    void resolveException_conflict() throws Exception {
        when(useCase.resolve(eq(exceptionId), any(), any()))
                .thenThrow(new ConflictException("CARGO_EXCEPTION_INVALID_STATE", "Already closed"));

        ResolveExceptionRequest req = new ResolveExceptionRequest("Done", null, null, 0L);

        mvc.perform(post("/v1/freight/exceptions/{id}/resolve", exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARGO_EXCEPTION_INVALID_STATE"));
    }
}
