package com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.controllers.VehicleController;
import com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.mappers.VehicleWebMapper;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.dto.request.VehicleRequest;
import com.transportlogistics.app.fleet.vehiclemaster.ports.inbound.VehicleUseCase;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VehicleControllerTest {

    private VehicleUseCase vehicleUseCase;
    private MockMvc mvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        vehicleUseCase = mock(VehicleUseCase.class);
        var mapper = Mappers.getMapper(VehicleWebMapper.class);
        var controller = new VehicleController(vehicleUseCase, mapper);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /vehicles creates vehicle and returns 201")
    void createVehicleSuccess() throws Exception {
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        var vehicleId = UUID.randomUUID();

        var request = new VehicleRequest("WP-CAB-1201", "CH-12345", "ENG-67890", categoryId, typeId,
                "Toyota", "Dyna", 2022, "COMPANY_OWNED", "AVAILABLE", 1000.0, 50.0, 3500.0, true);

        var savedVehicle = new Vehicle(vehicleId, "WP-CAB-1201", "CH-12345", "ENG-67890", categoryId, typeId,
                "Toyota", "Dyna", 2022, "COMPANY_OWNED", "AVAILABLE", 1000.0, 50.0, 3500.0, true);

        when(vehicleUseCase.create(any(Vehicle.class))).thenReturn(savedVehicle);

        mvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.registrationNumber").value("WP-CAB-1201"))
                .andExpect(jsonPath("$.manufacturer").value("Toyota"))
                .andExpect(jsonPath("$.operationalStatus").value("AVAILABLE"));
    }

    @Test
    @DisplayName("POST /vehicles returns 400 when required fields are missing or invalid")
    void createVehicleValidationFailure() throws Exception {
        var invalidRequest = new VehicleRequest("", null, null, null, null, null, null, null, null, null, -5.0, null, null, true);

        mvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /vehicles returns 409 Conflict when registration is duplicate")
    void createVehicleDuplicateConflict() throws Exception {
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        var request = new VehicleRequest("WP-CAB-1201", "CH-12345", "ENG-67890", categoryId, typeId,
                "Toyota", "Dyna", 2022, "COMPANY_OWNED", "AVAILABLE", 1000.0, 50.0, 3500.0, true);

        when(vehicleUseCase.create(any(Vehicle.class)))
                .thenThrow(new ConflictException("VEHICLE_REGISTRATION_DUPLICATE", "Vehicle registration already exists"));

        mvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_REGISTRATION_DUPLICATE"));
    }

    @Test
    @DisplayName("GET /vehicles returns vehicle list")
    void listVehicles() throws Exception {
        var vehicle = new Vehicle(UUID.randomUUID(), "WP-CAB-1201", "CH-12345", "ENG-67890",
                UUID.randomUUID(), UUID.randomUUID(), "Toyota", "Dyna", 2022, "COMPANY_OWNED",
                "AVAILABLE", 1000.0, 50.0, 3500.0, true);

        when(vehicleUseCase.list()).thenReturn(List.of(vehicle));

        mvc.perform(get("/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationNumber").value("WP-CAB-1201"));
    }

    @Test
    @DisplayName("GET /vehicles/{id} returns vehicle or 404")
    void getVehicle() throws Exception {
        var id = UUID.randomUUID();
        var vehicle = new Vehicle(id, "WP-CAB-1201", "CH-12345", "ENG-67890",
                UUID.randomUUID(), UUID.randomUUID(), "Toyota", "Dyna", 2022, "COMPANY_OWNED",
                "AVAILABLE", 1000.0, 50.0, 3500.0, true);

        when(vehicleUseCase.get(id)).thenReturn(vehicle);

        mvc.perform(get("/vehicles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        var missingId = UUID.randomUUID();
        when(vehicleUseCase.get(missingId)).thenThrow(new NotFoundException("Vehicle not found: " + missingId));

        mvc.perform(get("/vehicles/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /vehicles/{id} updates vehicle and returns 200")
    void updateVehicleSuccess() throws Exception {
        var id = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();

        var request = new VehicleRequest("WP-CAB-1201", "CH-12345", "ENG-67890", categoryId, typeId,
                "Toyota", "Dyna Updated", 2022, "COMPANY_OWNED", "AVAILABLE", 2000.0, 80.0, 3500.0, true);

        var updated = new Vehicle(id, "WP-CAB-1201", "CH-12345", "ENG-67890", categoryId, typeId,
                "Toyota", "Dyna Updated", 2022, "COMPANY_OWNED", "AVAILABLE", 2000.0, 80.0, 3500.0, true);

        when(vehicleUseCase.update(eq(id), any(Vehicle.class))).thenReturn(updated);

        mvc.perform(put("/vehicles/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Dyna Updated"));
    }

    @Test
    @DisplayName("DELETE /vehicles/{id} deactivates vehicle")
    void deactivateVehicleSuccess() throws Exception {
        var id = UUID.randomUUID();
        doNothing().when(vehicleUseCase).deactivate(id);

        mvc.perform(delete("/vehicles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vehicle deactivated"));

        verify(vehicleUseCase).deactivate(id);
    }
}
