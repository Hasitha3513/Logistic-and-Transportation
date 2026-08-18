package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fleet.application.ports.in.*;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleCategory;
import com.transportlogistics.app.fleet.domain.model.VehicleType;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.*;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response.*;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers.FleetWebMapper;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
public class FleetController {

    private final DriverUseCase drivers;
    private final DriverAvailabilityUseCase driverAvailability;
    private final DriverLicenseUseCase licenses;
    private final VehicleUseCase vehicles;
    private final VehicleAvailabilityUseCase vehicleAvailability;
    private final VehicleCategoryUseCase categories;
    private final VehicleTypeUseCase types;
    private final VehicleDocumentUseCase documents;
    private final FleetWebMapper mapper;

    public FleetController(DriverUseCase d, DriverAvailabilityUseCase driverAvailability,
                           DriverLicenseUseCase licenses, VehicleUseCase v,
                           VehicleAvailabilityUseCase vehicleAvailability, VehicleCategoryUseCase c,
                           VehicleTypeUseCase t, VehicleDocumentUseCase documents,
                           FleetWebMapper mapper) {
        this.drivers = d;
        this.driverAvailability = driverAvailability;
        this.licenses = licenses;
        this.vehicles = v;
        this.vehicleAvailability = vehicleAvailability;
        this.categories = c;
        this.types = t;
        this.documents = documents;
        this.mapper = mapper;
    }

    @PostMapping("/drivers")
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody DriverRequest r) {
        var created = drivers.create(new Driver(UUID.randomUUID(), r.employeeNumber(), r.firstName(), r.lastName(),
                r.phone(), r.email(), r.status() == null ? "AVAILABLE" : r.status(), r.active() == null || r.active()));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/drivers")
    public List<DriverResponse> listDrivers() {
        return mapper.toDriverResponseList(drivers.list());
    }

    @GetMapping("/drivers/{id}")
    public DriverResponse getDriver(@PathVariable UUID id) {
        return mapper.toResponse(drivers.get(id));
    }

    @PutMapping("/drivers/{id}")
    public DriverResponse updateDriver(@PathVariable UUID id, @Valid @RequestBody DriverRequest r) {
        var updated = drivers.update(id, new Driver(id, r.employeeNumber(), r.firstName(), r.lastName(), r.phone(),
                r.email(), r.status() == null ? "AVAILABLE" : r.status(), r.active() == null || r.active()));
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/drivers/{id}")
    public MessageResponse deactivateDriver(@PathVariable UUID id) {
        drivers.deactivate(id);
        return new MessageResponse("Driver deactivated");
    }

    @GetMapping("/drivers/{id}/availability")
    public DriverAvailabilityResponse driverAvailability(@PathVariable UUID id,
                                                         @RequestParam OffsetDateTime from,
                                                         @RequestParam OffsetDateTime to,
                                                         @RequestParam(required = false) String requiredLicenseClass,
                                                         @RequestParam(required = false) UUID excludeTripId) {
        return mapper.toResponse(driverAvailability.evaluate(new DriverAvailabilityUseCase.Query(id, from, to,
                requiredLicenseClass, excludeTripId)));
    }

    @GetMapping("/drivers/available")
    public List<DriverResponse> availableDrivers(@RequestParam OffsetDateTime from,
                                                 @RequestParam OffsetDateTime to,
                                                 @RequestParam(required = false) String requiredLicenseClass) {
        var list = drivers.list().stream()
                .filter(driver -> driverAvailability.evaluate(new DriverAvailabilityUseCase.Query(driver.id(), from,
                        to, requiredLicenseClass, null)).available())
                .toList();
        return mapper.toDriverResponseList(list);
    }

    @GetMapping("/drivers/{driverId}/licenses")
    public List<DriverLicenseResponse> listLicenses(@PathVariable UUID driverId) {
        return mapper.toDriverLicenseResponseList(licenses.list(driverId));
    }

    @PostMapping("/drivers/{driverId}/licenses")
    public ResponseEntity<DriverLicenseResponse> createLicense(@PathVariable UUID driverId,
                                                               @Valid @RequestBody DriverLicenseRequest request,
                                                               Principal principal) {
        var command = new DriverLicenseUseCase.CreateCommand(request.licenseNumber(), request.licenseClass(),
                request.issueDate(), request.expiryDate(), request.status(), request.active());
        var created = licenses.create(driverId, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @PatchMapping("/drivers/{driverId}/licenses/{licenseId}")
    public DriverLicenseResponse updateLicense(@PathVariable UUID driverId,
                                               @PathVariable UUID licenseId,
                                               @RequestBody DriverLicensePatchRequest request,
                                               Principal principal) {
        var command = new DriverLicenseUseCase.UpdateCommand(request.licenseNumber(), request.licenseClass(),
                request.issueDate(), request.expiryDate(), request.status(), request.active());
        return mapper.toResponse(licenses.update(driverId, licenseId, command, actor(principal)));
    }

    @DeleteMapping("/drivers/{driverId}/licenses/{licenseId}")
    public ResponseEntity<Void> deleteLicense(@PathVariable UUID driverId,
                                              @PathVariable UUID licenseId,
                                              Principal principal) {
        licenses.delete(driverId, licenseId, actor(principal));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/vehicles")
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest r) {
        var created = vehicles.create(vehicle(UUID.randomUUID(), r));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/vehicles")
    public List<VehicleResponse> listVehicles() {
        return mapper.toVehicleResponseList(vehicles.list());
    }

    @GetMapping("/vehicles/{id}")
    public VehicleResponse getVehicle(@PathVariable UUID id) {
        return mapper.toResponse(vehicles.get(id));
    }

    @PutMapping("/vehicles/{id}")
    public VehicleResponse updateVehicle(@PathVariable UUID id, @Valid @RequestBody VehicleRequest r) {
        var updated = vehicles.update(id, vehicle(id, r));
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/vehicles/{id}")
    public MessageResponse deactivateVehicle(@PathVariable UUID id) {
        vehicles.deactivate(id);
        return new MessageResponse("Vehicle deactivated");
    }

    @GetMapping("/vehicles/{id}/availability")
    public VehicleAvailabilityResponse vehicleAvailability(@PathVariable UUID id,
                                                           @RequestParam OffsetDateTime from,
                                                           @RequestParam OffsetDateTime to,
                                                           @RequestParam(required = false) UUID requiredVehicleTypeId,
                                                           @RequestParam(required = false) Double requiredCapacityKg,
                                                           @RequestParam(required = false) UUID excludeTripId) {
        return mapper.toResponse(vehicleAvailability.evaluate(new VehicleAvailabilityUseCase.Query(id, from, to,
                requiredVehicleTypeId, requiredCapacityKg, excludeTripId)));
    }

    @GetMapping("/vehicles/available")
    public List<VehicleResponse> availableVehicles(@RequestParam OffsetDateTime from,
                                                   @RequestParam OffsetDateTime to,
                                                   @RequestParam(required = false) UUID vehicleTypeId,
                                                   @RequestParam(required = false) UUID categoryId,
                                                   @RequestParam(required = false) Double minimumCapacityKg,
                                                   @RequestParam(required = false) UUID excludeTripId) {
        var list = vehicles.list().stream()
                .filter(vehicle -> vehicleAvailability.evaluate(new VehicleAvailabilityUseCase.Query(vehicle.id(),
                        from, to, vehicleTypeId, minimumCapacityKg, excludeTripId)).available())
                .filter(vehicle -> categoryId == null || categoryId.equals(vehicle.categoryId()))
                .toList();
        return mapper.toVehicleResponseList(list);
    }

    @GetMapping("/vehicles/{vehicleId}/documents")
    public List<VehicleDocumentResponse> docs(@PathVariable UUID vehicleId) {
        return mapper.toVehicleDocumentResponseList(documents.list(vehicleId));
    }

    @PostMapping("/vehicles/{vehicleId}/documents")
    public ResponseEntity<VehicleDocumentResponse> createDoc(@PathVariable UUID vehicleId,
                                                             @Valid @RequestBody DocumentRequest r,
                                                             Principal principal) {
        var command = new VehicleDocumentUseCase.CreateCommand(r.documentType(), r.documentNumber(), r.issueDate(),
                r.expiryDate(), r.fileReference(), Boolean.TRUE.equals(r.mandatoryForDispatch()), r.status(), r.active());
        var created = documents.create(vehicleId, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @PatchMapping("/vehicles/{vehicleId}/documents/{documentId}")
    public VehicleDocumentResponse updateDoc(@PathVariable UUID vehicleId,
                                             @PathVariable UUID documentId,
                                             @RequestBody DocumentPatchRequest r,
                                             Principal principal) {
        var command = new VehicleDocumentUseCase.UpdateCommand(r.documentType(), r.documentNumber(), r.issueDate(),
                r.expiryDate(), r.fileReference(), r.mandatoryForDispatch(), r.status(), r.active());
        return mapper.toResponse(documents.update(vehicleId, documentId, command, actor(principal)));
    }

    @DeleteMapping("/vehicles/{vehicleId}/documents/{documentId}")
    public ResponseEntity<Void> deleteDoc(@PathVariable UUID vehicleId,
                                          @PathVariable UUID documentId,
                                          Principal principal) {
        documents.delete(vehicleId, documentId, actor(principal));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/vehicle-categories")
    public ResponseEntity<VehicleCategoryResponse> createCategory(@Valid @RequestBody CategoryRequest r) {
        var created = categories.create(new VehicleCategory(UUID.randomUUID(), r.code(), r.name(), r.description(),
                r.active() == null || r.active()));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/vehicle-categories")
    public List<VehicleCategoryResponse> listCategories() {
        return mapper.toVehicleCategoryResponseList(categories.list());
    }

    @GetMapping("/vehicle-categories/{id}")
    public VehicleCategoryResponse getCategory(@PathVariable UUID id) {
        return mapper.toResponse(categories.get(id));
    }

    @PutMapping("/vehicle-categories/{id}")
    public VehicleCategoryResponse updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest r) {
        var updated = categories.update(id, new VehicleCategory(id, r.code(), r.name(), r.description(),
                r.active() == null || r.active()));
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/vehicle-categories/{id}")
    public MessageResponse deactivateCategory(@PathVariable UUID id) {
        categories.deactivate(id);
        return new MessageResponse("Vehicle category deactivated");
    }

    @PostMapping("/vehicle-types")
    public ResponseEntity<VehicleTypeResponse> createType(@Valid @RequestBody TypeRequest r) {
        var created = types.create(new VehicleType(UUID.randomUUID(), r.categoryId(), r.code(), r.name(),
                r.description(), r.active() == null || r.active()));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/vehicle-types")
    public List<VehicleTypeResponse> listTypes() {
        return mapper.toVehicleTypeResponseList(types.list());
    }

    @GetMapping("/vehicle-types/{id}")
    public VehicleTypeResponse getType(@PathVariable UUID id) {
        return mapper.toResponse(types.get(id));
    }

    @PutMapping("/vehicle-types/{id}")
    public VehicleTypeResponse updateType(@PathVariable UUID id, @Valid @RequestBody TypeRequest r) {
        var updated = types.update(id, new VehicleType(id, r.categoryId(), r.code(), r.name(), r.description(),
                r.active() == null || r.active()));
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/vehicle-types/{id}")
    public MessageResponse deactivateType(@PathVariable UUID id) {
        types.deactivate(id);
        return new MessageResponse("Vehicle type deactivated");
    }

    private Vehicle vehicle(UUID id, VehicleRequest r) {
        return new Vehicle(id, r.registrationNumber(), r.chassisNumber(), r.engineNumber(), r.categoryId(),
                r.typeId(), r.manufacturer(), r.model(), r.manufactureYear(),
                r.ownershipType() == null ? "COMPANY_OWNED" : r.ownershipType(),
                r.operationalStatus() == null ? "AVAILABLE" : r.operationalStatus(),
                r.currentOdometerKm(), r.engineHours(), r.capacityKg(),
                r.active() == null || r.active());
    }

    private String actor(Principal principal) {
        return PrincipalUtils.resolveActorName(principal, "system");
    }
}
