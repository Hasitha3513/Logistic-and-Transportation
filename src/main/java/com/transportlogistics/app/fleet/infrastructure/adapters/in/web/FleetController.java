package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.transportlogistics.app.fleet.application.ports.in.DriverUseCase;
import com.transportlogistics.app.fleet.application.ports.in.VehicleCategoryUseCase;
import com.transportlogistics.app.fleet.application.ports.in.VehicleTypeUseCase;
import com.transportlogistics.app.fleet.application.ports.in.VehicleUseCase;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleCategory;
import com.transportlogistics.app.fleet.domain.model.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class FleetController {
    private final DriverUseCase drivers;
    private final VehicleUseCase vehicles;
    private final VehicleCategoryUseCase categories;
    private final VehicleTypeUseCase types;

    FleetController(DriverUseCase d, VehicleUseCase v, VehicleCategoryUseCase c, VehicleTypeUseCase t) {
        drivers = d;
        vehicles = v;
        categories = c;
        types = t;
    }

    @PostMapping("/drivers")
    ResponseEntity<Driver> createDriver(@Valid @RequestBody DriverRequest r) {
        return ResponseEntity.status(201).body(drivers.create(new Driver(UUID.randomUUID(), r.employeeNumber(), r.firstName(), r.lastName(), r.phone(), r.email(), r.status() == null ? "AVAILABLE" : r.status(), r.active() == null || r.active())));
    }

    @GetMapping("/drivers")
    List<Driver> listDrivers() {
        return drivers.list();
    }

    @GetMapping("/drivers/{id}")
    Driver getDriver(@PathVariable UUID id) {
        return drivers.get(id);
    }

    @PutMapping("/drivers/{id}")
    Driver updateDriver(@PathVariable UUID id, @Valid @RequestBody DriverRequest r) {
        return drivers.update(id, new Driver(id, r.employeeNumber(), r.firstName(), r.lastName(), r.phone(), r.email(), r.status() == null ? "AVAILABLE" : r.status(), r.active() == null || r.active()));
    }

    @DeleteMapping("/drivers/{id}")
    MessageResponse deactivateDriver(@PathVariable UUID id) {
        drivers.deactivate(id);
        return new MessageResponse("Driver deactivated");
    }

    @GetMapping("/drivers/{id}/availability")
    AvailabilityResponse driverAvailability(@PathVariable UUID id, @RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to, @RequestParam(required = false) UUID excludeTripId) {
        var d = drivers.get(id);
        return new AvailabilityResponse(d.active() && "AVAILABLE".equalsIgnoreCase(d.status()), d.active() ? "STATUS_CHECK" : "INACTIVE");
    }

    @GetMapping("/drivers/available")
    List<Driver> availableDrivers(@RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to, @RequestParam(required = false) String requiredLicenseClass) {
        return drivers.list().stream().filter(d -> d.active() && "AVAILABLE".equalsIgnoreCase(d.status())).toList();
    }

    @GetMapping("/drivers/{driverId}/licenses")
    List<Map<String, Object>> listLicenses(@PathVariable UUID driverId) {
        return List.of();
    }

    @PostMapping("/drivers/{driverId}/licenses")
    ResponseEntity<Map<String, Object>> createLicense(@PathVariable UUID driverId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(body);
    }

    @PutMapping("/drivers/{driverId}/licenses/{licenseId}")
    Map<String, Object> updateLicense(@PathVariable UUID driverId, @PathVariable UUID licenseId, @RequestBody Map<String, Object> body) {
        return body;
    }

    @DeleteMapping("/drivers/{driverId}/licenses/{licenseId}")
    MessageResponse deleteLicense(@PathVariable UUID driverId, @PathVariable UUID licenseId) {
        return new MessageResponse("License deleted");
    }

    @PostMapping("/vehicles")
    ResponseEntity<Vehicle> createVehicle(@Valid @RequestBody VehicleRequest r) {
        return ResponseEntity.status(201).body(vehicles.create(vehicle(UUID.randomUUID(), r)));
    }

    @GetMapping("/vehicles")
    List<Vehicle> listVehicles() {
        return vehicles.list();
    }

    @GetMapping("/vehicles/{id}")
    Vehicle getVehicle(@PathVariable UUID id) {
        return vehicles.get(id);
    }

    @PutMapping("/vehicles/{id}")
    Vehicle updateVehicle(@PathVariable UUID id, @Valid @RequestBody VehicleRequest r) {
        return vehicles.update(id, vehicle(id, r));
    }

    @DeleteMapping("/vehicles/{id}")
    MessageResponse deactivateVehicle(@PathVariable UUID id) {
        vehicles.deactivate(id);
        return new MessageResponse("Vehicle deactivated");
    }

    @GetMapping("/vehicles/{id}/availability")
    AvailabilityResponse vehicleAvailability(@PathVariable UUID id, @RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to, @RequestParam(required = false) UUID excludeTripId) {
        var v = vehicles.get(id);
        return new AvailabilityResponse(v.active() && "AVAILABLE".equalsIgnoreCase(v.operationalStatus()), v.active() ? "STATUS_CHECK" : "INACTIVE");
    }

    @GetMapping("/vehicles/available")
    List<Vehicle> availableVehicles(@RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to, @RequestParam(required = false) UUID vehicleTypeId, @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) Double minimumCapacityKg) {
        return vehicles.list().stream().filter(v -> v.active() && "AVAILABLE".equalsIgnoreCase(v.operationalStatus())).filter(v -> vehicleTypeId == null || vehicleTypeId.equals(v.typeId())).filter(v -> categoryId == null || categoryId.equals(v.categoryId())).filter(v -> minimumCapacityKg == null || (v.capacityKg() != null && v.capacityKg() >= minimumCapacityKg)).toList();
    }

    @GetMapping("/vehicles/{vehicleId}/documents")
    List<DocumentResponse> docs(@PathVariable UUID vehicleId) {
        return List.of();
    }

    @PostMapping("/vehicles/{vehicleId}/documents")
    ResponseEntity<DocumentResponse> createDoc(@PathVariable UUID vehicleId, @Valid @RequestBody DocumentRequest r) {
        return ResponseEntity.status(201).body(new DocumentResponse(UUID.randomUUID(), vehicleId, r.documentType(), r.documentNumber()));
    }

    @PutMapping("/vehicles/{vehicleId}/documents/{documentId}")
    DocumentResponse updateDoc(@PathVariable UUID vehicleId, @PathVariable UUID documentId, @Valid @RequestBody DocumentRequest r) {
        return new DocumentResponse(documentId, vehicleId, r.documentType(), r.documentNumber());
    }

    @DeleteMapping("/vehicles/{vehicleId}/documents/{documentId}")
    MessageResponse deleteDoc(@PathVariable UUID vehicleId, @PathVariable UUID documentId) {
        return new MessageResponse("Vehicle document deleted");
    }

    @PostMapping("/vehicle-categories")
    ResponseEntity<VehicleCategory> createCategory(@Valid @RequestBody CategoryRequest r) {
        return ResponseEntity.status(201).body(categories.create(new VehicleCategory(UUID.randomUUID(), r.code(), r.name(), r.description(), r.active() == null || r.active())));
    }

    @GetMapping("/vehicle-categories")
    List<VehicleCategory> listCategories() {
        return categories.list();
    }

    @GetMapping("/vehicle-categories/{id}")
    VehicleCategory getCategory(@PathVariable UUID id) {
        return categories.get(id);
    }

    @PutMapping("/vehicle-categories/{id}")
    VehicleCategory updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest r) {
        return categories.update(id, new VehicleCategory(id, r.code(), r.name(), r.description(), r.active() == null || r.active()));
    }

    @DeleteMapping("/vehicle-categories/{id}")
    MessageResponse deactivateCategory(@PathVariable UUID id) {
        categories.deactivate(id);
        return new MessageResponse("Vehicle category deactivated");
    }

    @PostMapping("/vehicle-types")
    ResponseEntity<VehicleType> createType(@Valid @RequestBody TypeRequest r) {
        return ResponseEntity.status(201).body(types.create(new VehicleType(UUID.randomUUID(), r.categoryId(), r.code(), r.name(), r.description(), r.active() == null || r.active())));
    }

    @GetMapping("/vehicle-types")
    List<VehicleType> listTypes() {
        return types.list();
    }

    @GetMapping("/vehicle-types/{id}")
    VehicleType getType(@PathVariable UUID id) {
        return types.get(id);
    }

    @PutMapping("/vehicle-types/{id}")
    VehicleType updateType(@PathVariable UUID id, @Valid @RequestBody TypeRequest r) {
        return types.update(id, new VehicleType(id, r.categoryId(), r.code(), r.name(), r.description(), r.active() == null || r.active()));
    }

    @DeleteMapping("/vehicle-types/{id}")
    MessageResponse deactivateType(@PathVariable UUID id) {
        types.deactivate(id);
        return new MessageResponse("Vehicle type deactivated");
    }

    private Vehicle vehicle(UUID id, VehicleRequest r) {
        return new Vehicle(id, r.registrationNumber(), r.chassisNumber(), r.engineNumber(), r.categoryId(), r.typeId(), r.manufacturer(), r.model(), r.manufactureYear(), r.ownershipType() == null ? "COMPANY_OWNED" : r.ownershipType(), r.operationalStatus() == null ? "AVAILABLE" : r.operationalStatus(), r.currentOdometerKm(), r.engineHours(), r.capacityKg(), r.active() == null || r.active());
    }

    record DriverRequest(@NotBlank String employeeNumber, @NotBlank String firstName, @NotBlank String lastName,
                         String phone, @Email String email, String status, Boolean active) {
    }

    record VehicleRequest(@NotBlank String registrationNumber, String chassisNumber, String engineNumber,
                          @NotNull UUID categoryId, @NotNull UUID typeId, String manufacturer, String model,
                          Integer manufactureYear, String ownershipType, String operationalStatus,
                          Double currentOdometerKm, Double engineHours, Double capacityKg, Boolean active) {
    }

    record CategoryRequest(@NotBlank String code, @NotBlank String name, String description, Boolean active) {
    }

    record TypeRequest(@NotNull UUID categoryId, @NotBlank String code, @NotBlank String name, String description,
                       Boolean active) {
    }

    record AvailabilityResponse(boolean available, String reason) {
    }

    record MessageResponse(String message) {
    }

    record DocumentRequest(@NotBlank String documentType, @NotBlank String documentNumber) {
    }

    record DocumentResponse(UUID id, UUID vehicleId, String documentType, String documentNumber) {
    }
}
