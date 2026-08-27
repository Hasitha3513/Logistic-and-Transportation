package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.transportlogistics.app.fleet.application.ports.in.*;
import com.transportlogistics.app.fleet.vehiclemaster.ports.inbound.VehicleUseCase;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers.FleetController;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers.FleetWebMapper;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
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

class FleetControllerDriverLicenseTest {
    private DriverLicenseUseCase licenses;
    private MockMvc mvc;
    private UUID driverId;

    @BeforeEach
    void setUp() {
        licenses = mock(DriverLicenseUseCase.class);
        var mapper = Mappers.getMapper(FleetWebMapper.class);
        var controller = new FleetController(mock(DriverUseCase.class), mock(DriverAvailabilityUseCase.class),
                licenses, mock(VehicleUseCase.class),
                mock(VehicleAvailabilityUseCase.class), mock(VehicleCategoryUseCase.class), mock(VehicleTypeUseCase.class),
                mock(VehicleDocumentUseCase.class), mapper);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        driverId = UUID.randomUUID();
    }

    @Test
    void createsAndListsLicenses() throws Exception {
        var license = license();
        when(licenses.create(eq(driverId), any(), anyString())).thenReturn(license);
        when(licenses.list(driverId)).thenReturn(List.of(license));

        mvc.perform(post("/drivers/{driverId}/licenses", driverId).principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"licenseNumber":"DL-1","licenseClass":"B","issueDate":"2025-01-01",
                                 "expiryDate":"2027-01-01","status":"ACTIVE","active":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.driverId").value(driverId.toString()))
                .andExpect(jsonPath("$.licenseClass").value("B"))
                .andExpect(jsonPath("$.createdBy").value("alice"));

        mvc.perform(get("/drivers/{driverId}/licenses", driverId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].licenseNumber").value("DL-1"));
    }

    @Test
    void patchAndDeleteUseRequiredContracts() throws Exception {
        var license = license();
        when(licenses.update(eq(driverId), eq(license.id()), any(), anyString())).thenReturn(license);

        mvc.perform(patch("/drivers/{driverId}/licenses/{licenseId}", driverId, license.id())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"licenseClass\":\"C\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/drivers/{driverId}/licenses/{licenseId}", driverId, license.id()))
                .andExpect(status().isNoContent());

        verify(licenses).delete(eq(driverId), eq(license.id()), anyString());
    }

    @Test
    void issueAndExpiryDatesAreRequiredOnCreate() throws Exception {
        mvc.perform(post("/drivers/{driverId}/licenses", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licenseNumber\":\"DL-1\",\"licenseClass\":\"B\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private DriverLicense license() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new DriverLicense(UUID.randomUUID(), driverId, "DL-1", "B", LocalDate.of(2025, 1, 1),
                LocalDate.of(2027, 1, 1), DriverLicenseStatus.ACTIVE, true, now, now, "alice", "alice");
    }
}
