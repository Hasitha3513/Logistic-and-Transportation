package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.transportlogistics.app.fleet.application.ports.in.*;
import com.transportlogistics.app.fleet.domain.model.VehicleDocument;
import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FleetControllerVehicleDocumentTest {
    private VehicleDocumentUseCase documents;
    private MockMvc mvc;
    private UUID vehicleId;

    @BeforeEach
    void setUp() {
        documents = mock(VehicleDocumentUseCase.class);
        var controller = new FleetController(mock(DriverUseCase.class), mock(DriverAvailabilityUseCase.class),
                mock(DriverLicenseUseCase.class),
                mock(VehicleUseCase.class), mock(VehicleAvailabilityUseCase.class),
                mock(VehicleCategoryUseCase.class), mock(VehicleTypeUseCase.class), documents);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        vehicleId = UUID.randomUUID();
    }

    @Test
    void createsAndListsDocumentsWithFullPersistentContract() throws Exception {
        var document = document();
        when(documents.create(eq(vehicleId), any(), anyString())).thenReturn(document);
        when(documents.list(vehicleId)).thenReturn(List.of(document));

        mvc.perform(post("/vehicles/{vehicleId}/documents", vehicleId).principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentType":"insurance","documentNumber":"POL-1","issueDate":"2025-01-01",
                                 "expiryDate":"2027-01-01","fileReference":"https://files.example/POL-1",
                                 "mandatoryForDispatch":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()))
                .andExpect(jsonPath("$.mandatoryForDispatch").value(true))
                .andExpect(jsonPath("$.createdBy").value("alice"));

        mvc.perform(get("/vehicles/{vehicleId}/documents", vehicleId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].documentNumber").value("POL-1"));
    }

    @Test
    void patchAndDeleteUseRequiredHttpContracts() throws Exception {
        var document = document();
        when(documents.update(eq(vehicleId), eq(document.id()), any(), anyString())).thenReturn(document);

        mvc.perform(patch("/vehicles/{vehicleId}/documents/{documentId}", vehicleId, document.id())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mandatoryForDispatch\":false}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/vehicles/{vehicleId}/documents/{documentId}", vehicleId, document.id()))
                .andExpect(status().isNoContent());

        verify(documents).delete(eq(vehicleId), eq(document.id()), anyString());
    }

    @Test
    void documentNumberIsRequiredOnCreate() throws Exception {
        mvc.perform(post("/vehicles/{vehicleId}/documents", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"documentType\":\"INSURANCE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private VehicleDocument document() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new VehicleDocument(UUID.randomUUID(), vehicleId, "INSURANCE", "POL-1",
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1), "https://files.example/POL-1", true,
                VehicleDocumentStatus.ACTIVE, true, now, now, "alice", "alice");
    }
}
