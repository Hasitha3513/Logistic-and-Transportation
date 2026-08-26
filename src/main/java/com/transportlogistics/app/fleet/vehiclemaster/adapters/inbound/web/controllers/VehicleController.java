package com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.controllers;

import com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.dto.request.VehicleRequest;
import com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.dto.response.VehicleOperationResponse;
import com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.dto.response.VehicleResponse;
import com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.mappers.VehicleWebMapper;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.fleet.vehiclemaster.ports.inbound.VehicleUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class VehicleController {
    private final VehicleUseCase vehicles;
    private final VehicleWebMapper mapper;

    public VehicleController(VehicleUseCase vehicles, VehicleWebMapper mapper) {
        this.vehicles = vehicles;
        this.mapper = mapper;
    }

    @PostMapping("/vehicles")
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest request) {
        var created = vehicles.create(toDomain(UUID.randomUUID(), request));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/vehicles")
    public List<VehicleResponse> listVehicles() {
        return mapper.toResponseList(vehicles.list());
    }

    @GetMapping("/vehicles/{id}")
    public VehicleResponse getVehicle(@PathVariable UUID id) {
        return mapper.toResponse(vehicles.get(id));
    }

    @PutMapping("/vehicles/{id}")
    public VehicleResponse updateVehicle(@PathVariable UUID id, @Valid @RequestBody VehicleRequest request) {
        return mapper.toResponse(vehicles.update(id, toDomain(id, request)));
    }

    @DeleteMapping("/vehicles/{id}")
    public VehicleOperationResponse deactivateVehicle(@PathVariable UUID id) {
        vehicles.deactivate(id);
        return new VehicleOperationResponse("Vehicle deactivated");
    }

    private Vehicle toDomain(UUID id, VehicleRequest request) {
        return new Vehicle(id, request.registrationNumber(), request.chassisNumber(), request.engineNumber(),
                request.categoryId(), request.typeId(), request.manufacturer(), request.model(),
                request.manufactureYear(), request.ownershipType() == null ? "COMPANY_OWNED" : request.ownershipType(),
                request.operationalStatus() == null ? "AVAILABLE" : request.operationalStatus(),
                request.currentOdometerKm(), request.engineHours(), request.capacityKg(),
                request.active() == null || request.active());
    }
}
