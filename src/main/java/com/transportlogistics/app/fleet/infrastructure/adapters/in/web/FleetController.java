package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.transportlogistics.app.fleet.application.ports.in.DriverUseCase;
import com.transportlogistics.app.fleet.application.ports.in.DriverLicenseUseCase;
import com.transportlogistics.app.fleet.application.ports.in.VehicleCategoryUseCase;
import com.transportlogistics.app.fleet.application.ports.in.VehicleTypeUseCase;
import com.transportlogistics.app.fleet.application.ports.in.VehicleUseCase;
import com.transportlogistics.app.fleet.application.ports.in.VehicleAvailabilityUseCase;
import com.transportlogistics.app.fleet.application.ports.in.VehicleDocumentUseCase;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleAvailability;
import com.transportlogistics.app.fleet.domain.model.VehicleCategory;
import com.transportlogistics.app.fleet.domain.model.VehicleType;
import com.transportlogistics.app.fleet.domain.model.VehicleDocument;
import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class FleetController {
    private final DriverUseCase drivers;
    private final DriverLicenseUseCase licenses;
    private final VehicleUseCase vehicles;
    private final VehicleAvailabilityUseCase vehicleAvailability;
    private final VehicleCategoryUseCase categories;
    private final VehicleTypeUseCase types;
    private final VehicleDocumentUseCase documents;

    FleetController(DriverUseCase d, DriverLicenseUseCase licenses, VehicleUseCase v,
                    VehicleAvailabilityUseCase vehicleAvailability, VehicleCategoryUseCase c,
                    VehicleTypeUseCase t, VehicleDocumentUseCase documents) {
        drivers = d;
        this.licenses = licenses;
        vehicles = v;
        this.vehicleAvailability = vehicleAvailability;
        categories = c;
        types = t;
        this.documents = documents;
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
        var availability = drivers.availability(id, null, from.toLocalDate());
        return new AvailabilityResponse(availability.available(), availability.reason());
    }

    @GetMapping("/drivers/available")
    List<Driver> availableDrivers(@RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to, @RequestParam(required = false) String requiredLicenseClass) {
        return drivers.list().stream()
                .filter(driver -> drivers.availability(driver.id(), requiredLicenseClass, from.toLocalDate()).available())
                .toList();
    }

    @GetMapping("/drivers/{driverId}/licenses")
    List<DriverLicense> listLicenses(@PathVariable UUID driverId) {
        return licenses.list(driverId);
    }

    @PostMapping("/drivers/{driverId}/licenses")
    ResponseEntity<DriverLicense> createLicense(@PathVariable UUID driverId,
                                                @Valid @RequestBody DriverLicenseRequest request,
                                                Principal principal) {
        var command = new DriverLicenseUseCase.CreateCommand(request.licenseNumber(), request.licenseClass(),
                request.issueDate(), request.expiryDate(), request.status(), request.active());
        return ResponseEntity.status(201).body(licenses.create(driverId, command, actor(principal)));
    }

    @PatchMapping("/drivers/{driverId}/licenses/{licenseId}")
    DriverLicense updateLicense(@PathVariable UUID driverId, @PathVariable UUID licenseId,
                                @RequestBody DriverLicensePatchRequest request, Principal principal) {
        var command = new DriverLicenseUseCase.UpdateCommand(request.licenseNumber(), request.licenseClass(),
                request.issueDate(), request.expiryDate(), request.status(), request.active());
        return licenses.update(driverId, licenseId, command, actor(principal));
    }

    @DeleteMapping("/drivers/{driverId}/licenses/{licenseId}")
    ResponseEntity<Void> deleteLicense(@PathVariable UUID driverId, @PathVariable UUID licenseId,
                                       Principal principal) {
        licenses.delete(driverId, licenseId, actor(principal));
        return ResponseEntity.noContent().build();
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
    VehicleAvailability vehicleAvailability(@PathVariable UUID id, @RequestParam OffsetDateTime from,
                                            @RequestParam OffsetDateTime to,
                                            @RequestParam(required = false) UUID requiredVehicleTypeId,
                                            @RequestParam(required = false) Double requiredCapacityKg,
                                            @RequestParam(required = false) UUID excludeTripId) {
        return vehicleAvailability.evaluate(new VehicleAvailabilityUseCase.Query(id, from, to,
                requiredVehicleTypeId, requiredCapacityKg, excludeTripId));
    }

    @GetMapping("/vehicles/available")
    List<Vehicle> availableVehicles(@RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to,
                                    @RequestParam(required = false) UUID vehicleTypeId,
                                    @RequestParam(required = false) UUID categoryId,
                                    @RequestParam(required = false) Double minimumCapacityKg,
                                    @RequestParam(required = false) UUID excludeTripId) {
        return vehicles.list().stream()
                .filter(vehicle -> vehicleAvailability.evaluate(new VehicleAvailabilityUseCase.Query(vehicle.id(),
                        from, to, vehicleTypeId, minimumCapacityKg, excludeTripId)).available())
                .filter(vehicle -> categoryId == null || categoryId.equals(vehicle.categoryId()))
                .toList();
    }

    @GetMapping("/vehicles/{vehicleId}/documents")
    List<VehicleDocument> docs(@PathVariable UUID vehicleId) {
        return documents.list(vehicleId);
    }

    @PostMapping("/vehicles/{vehicleId}/documents")
    ResponseEntity<VehicleDocument> createDoc(@PathVariable UUID vehicleId, @Valid @RequestBody DocumentRequest r,
                                               Principal principal) {
        var command = new VehicleDocumentUseCase.CreateCommand(r.documentType(), r.documentNumber(), r.issueDate(),
                r.expiryDate(), r.fileReference(), Boolean.TRUE.equals(r.mandatoryForDispatch()), r.status(), r.active());
        return ResponseEntity.status(201).body(documents.create(vehicleId, command, actor(principal)));
    }

    @PatchMapping("/vehicles/{vehicleId}/documents/{documentId}")
    VehicleDocument updateDoc(@PathVariable UUID vehicleId, @PathVariable UUID documentId,
                              @RequestBody DocumentPatchRequest r, Principal principal) {
        var command = new VehicleDocumentUseCase.UpdateCommand(r.documentType(), r.documentNumber(), r.issueDate(),
                r.expiryDate(), r.fileReference(), r.mandatoryForDispatch(), r.status(), r.active());
        return documents.update(vehicleId, documentId, command, actor(principal));
    }

    @DeleteMapping("/vehicles/{vehicleId}/documents/{documentId}")
    ResponseEntity<Void> deleteDoc(@PathVariable UUID vehicleId, @PathVariable UUID documentId, Principal principal) {
        documents.delete(vehicleId, documentId, actor(principal));
        return ResponseEntity.noContent().build();
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

    record DriverLicenseRequest(@NotBlank String licenseNumber, @NotBlank String licenseClass,
                                @NotNull LocalDate issueDate, @NotNull LocalDate expiryDate,
                                DriverLicenseStatus status, Boolean active) {
    }

    record DriverLicensePatchRequest(String licenseNumber, String licenseClass, LocalDate issueDate,
                                     LocalDate expiryDate, DriverLicenseStatus status, Boolean active) {
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

    private String actor(Principal principal) {
        return principal == null ? "system" : principal.getName();
    }

    record DocumentRequest(@NotBlank String documentType, @NotBlank String documentNumber, LocalDate issueDate,
                           LocalDate expiryDate, String fileReference, Boolean mandatoryForDispatch,
                           VehicleDocumentStatus status, Boolean active) {
    }

    record DocumentPatchRequest(String documentType, String documentNumber, LocalDate issueDate,
                                LocalDate expiryDate, String fileReference, Boolean mandatoryForDispatch,
                                VehicleDocumentStatus status, Boolean active) {
    }
}
