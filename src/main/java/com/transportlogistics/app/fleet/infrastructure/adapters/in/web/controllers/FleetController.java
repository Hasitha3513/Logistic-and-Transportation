package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fleet.application.ports.in.*;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionStatus;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionType;
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
    private final DriverExceptionUseCase driverExceptions;
    private final DriverViolationUseCase driverViolations;
    private final DriverPerformanceUseCase driverPerformance;
    private final DriverMedicalRecordUseCase driverMedicalRecords;
    private final DriverDrugTestUseCase driverDrugTests;
    private final VehicleUseCase vehicles;
    private final VehicleAvailabilityUseCase vehicleAvailability;
    private final VehicleCategoryUseCase categories;
    private final VehicleTypeUseCase types;
    private final VehicleDocumentUseCase documents;
    private final MaintenanceScheduleUseCase maintenanceSchedules;
    private final LubricantLogUseCase lubricantLogs;
    private final FleetWebMapper mapper;

    @org.springframework.beans.factory.annotation.Autowired
    public FleetController(DriverUseCase d, DriverAvailabilityUseCase driverAvailability,
                           DriverLicenseUseCase licenses, DriverExceptionUseCase driverExceptions,
                           DriverViolationUseCase driverViolations, DriverPerformanceUseCase driverPerformance,
                           DriverMedicalRecordUseCase driverMedicalRecords, DriverDrugTestUseCase driverDrugTests,
                           VehicleUseCase v, VehicleAvailabilityUseCase vehicleAvailability,
                           VehicleCategoryUseCase c, VehicleTypeUseCase t,
                           VehicleDocumentUseCase documents,
                           MaintenanceScheduleUseCase maintenanceSchedules,
                           LubricantLogUseCase lubricantLogs,
                           FleetWebMapper mapper) {
        this.drivers = d;
        this.driverAvailability = driverAvailability;
        this.licenses = licenses;
        this.driverExceptions = driverExceptions;
        this.driverViolations = driverViolations;
        this.driverPerformance = driverPerformance;
        this.driverMedicalRecords = driverMedicalRecords;
        this.driverDrugTests = driverDrugTests;
        this.vehicles = v;
        this.vehicleAvailability = vehicleAvailability;
        this.categories = c;
        this.types = t;
        this.documents = documents;
        this.maintenanceSchedules = maintenanceSchedules;
        this.lubricantLogs = lubricantLogs;
        this.mapper = mapper;
    }

    public FleetController(DriverUseCase d, DriverAvailabilityUseCase driverAvailability,
                           DriverLicenseUseCase licenses, DriverExceptionUseCase driverExceptions,
                           DriverViolationUseCase driverViolations, DriverPerformanceUseCase driverPerformance,
                           DriverMedicalRecordUseCase driverMedicalRecords, DriverDrugTestUseCase driverDrugTests,
                           VehicleUseCase v, VehicleAvailabilityUseCase vehicleAvailability,
                           VehicleCategoryUseCase c, VehicleTypeUseCase t,
                           VehicleDocumentUseCase documents,
                           MaintenanceScheduleUseCase maintenanceSchedules,
                           FleetWebMapper mapper) {
        this(d, driverAvailability, licenses, driverExceptions, driverViolations, driverPerformance, driverMedicalRecords, driverDrugTests, v, vehicleAvailability, c, t, documents, maintenanceSchedules, null, mapper);
    }

    public FleetController(DriverUseCase d, DriverAvailabilityUseCase driverAvailability,
                           DriverLicenseUseCase licenses, DriverExceptionUseCase driverExceptions,
                           DriverViolationUseCase driverViolations, DriverPerformanceUseCase driverPerformance,
                           VehicleUseCase v, VehicleAvailabilityUseCase vehicleAvailability,
                           VehicleCategoryUseCase c, VehicleTypeUseCase t,
                           VehicleDocumentUseCase documents,
                           MaintenanceScheduleUseCase maintenanceSchedules,
                           FleetWebMapper mapper) {
        this(d, driverAvailability, licenses, driverExceptions, driverViolations, driverPerformance, null, null, v, vehicleAvailability, c, t, documents, maintenanceSchedules, null, mapper);
    }

    public FleetController(DriverUseCase d, DriverAvailabilityUseCase driverAvailability,
                           DriverLicenseUseCase licenses, DriverExceptionUseCase driverExceptions,
                           VehicleUseCase v, VehicleAvailabilityUseCase vehicleAvailability,
                           VehicleCategoryUseCase c, VehicleTypeUseCase t,
                           VehicleDocumentUseCase documents,
                           MaintenanceScheduleUseCase maintenanceSchedules,
                           FleetWebMapper mapper) {
        this(d, driverAvailability, licenses, driverExceptions, null, null, null, null, v, vehicleAvailability, c, t, documents, maintenanceSchedules, null, mapper);
    }

    public FleetController(DriverUseCase d, DriverAvailabilityUseCase driverAvailability,
                           DriverLicenseUseCase licenses, VehicleUseCase v,
                           VehicleAvailabilityUseCase vehicleAvailability, VehicleCategoryUseCase c,
                           VehicleTypeUseCase t, VehicleDocumentUseCase documents,
                           MaintenanceScheduleUseCase maintenanceSchedules,
                           FleetWebMapper mapper) {
        this(d, driverAvailability, licenses, null, null, null, null, null, v, vehicleAvailability, c, t, documents, maintenanceSchedules, null, mapper);
    }

    public FleetController(DriverUseCase d, DriverAvailabilityUseCase driverAvailability,
                           DriverLicenseUseCase licenses, VehicleUseCase v,
                           VehicleAvailabilityUseCase vehicleAvailability, VehicleCategoryUseCase c,
                           VehicleTypeUseCase t, VehicleDocumentUseCase documents,
                           FleetWebMapper mapper) {
        this(d, driverAvailability, licenses, null, null, null, null, null, v, vehicleAvailability, c, t, documents, null, null, mapper);
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

    // Driver Exception endpoints
    @GetMapping("/drivers/{driverId}/exceptions")
    public List<DriverExceptionResponse> listDriverExceptions(@PathVariable UUID driverId) {
        return mapper.toDriverExceptionResponseList(driverExceptions.list(driverId));
    }

    @PostMapping("/drivers/{driverId}/exceptions")
    public ResponseEntity<DriverExceptionResponse> createDriverException(
            @PathVariable UUID driverId,
            @Valid @RequestBody DriverExceptionRequest request,
            Principal principal
    ) {
        var type = DriverExceptionType.valueOf(request.exceptionType());
        var command = new DriverExceptionUseCase.CreateCommand(
                type,
                request.startTime(),
                request.endTime(),
                request.reason(),
                request.remarks()
        );
        var created = driverExceptions.create(driverId, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/drivers/{driverId}/exceptions/{exceptionId}")
    public DriverExceptionResponse getDriverException(
            @PathVariable UUID driverId,
            @PathVariable UUID exceptionId
    ) {
        return mapper.toResponse(driverExceptions.get(driverId, exceptionId));
    }

    @PutMapping("/drivers/{driverId}/exceptions/{exceptionId}")
    public DriverExceptionResponse updateDriverException(
            @PathVariable UUID driverId,
            @PathVariable UUID exceptionId,
            @Valid @RequestBody DriverExceptionRequest request,
            Principal principal
    ) {
        var type = request.exceptionType() != null ? DriverExceptionType.valueOf(request.exceptionType()) : null;
        var command = new DriverExceptionUseCase.UpdateCommand(
                type,
                request.startTime(),
                request.endTime(),
                null,
                request.reason(),
                request.remarks()
        );
        return mapper.toResponse(driverExceptions.update(driverId, exceptionId, command, actor(principal)));
    }

    @PatchMapping("/drivers/{driverId}/exceptions/{exceptionId}")
    public DriverExceptionResponse patchDriverException(
            @PathVariable UUID driverId,
            @PathVariable UUID exceptionId,
            @RequestBody DriverExceptionPatchRequest request,
            Principal principal
    ) {
        var type = request.exceptionType() != null ? DriverExceptionType.valueOf(request.exceptionType()) : null;
        var status = request.status() != null ? DriverExceptionStatus.valueOf(request.status()) : null;
        var command = new DriverExceptionUseCase.UpdateCommand(
                type,
                request.startTime(),
                request.endTime(),
                status,
                request.reason(),
                request.remarks()
        );
        return mapper.toResponse(driverExceptions.update(driverId, exceptionId, command, actor(principal)));
    }

    @PostMapping("/drivers/{driverId}/exceptions/{exceptionId}/cancel")
    public DriverExceptionResponse cancelDriverException(
            @PathVariable UUID driverId,
            @PathVariable UUID exceptionId,
            @RequestBody(required = false) DriverExceptionActionRequest request,
            Principal principal
    ) {
        var remarks = request != null ? request.remarks() : null;
        return mapper.toResponse(driverExceptions.cancel(driverId, exceptionId, remarks, actor(principal)));
    }

    @PostMapping("/drivers/{driverId}/exceptions/{exceptionId}/complete")
    public DriverExceptionResponse completeDriverException(
            @PathVariable UUID driverId,
            @PathVariable UUID exceptionId,
            @RequestBody(required = false) DriverExceptionActionRequest request,
            Principal principal
    ) {
        var remarks = request != null ? request.remarks() : null;
        return mapper.toResponse(driverExceptions.complete(driverId, exceptionId, remarks, actor(principal)));
    }

    @GetMapping("/drivers/{driverId}/violations")
    public List<DriverViolationResponse> listDriverViolations(@PathVariable UUID driverId) {
        return mapper.toDriverViolationResponseList(driverViolations.listViolations(driverId));
    }

    @PostMapping("/drivers/{driverId}/violations")
    public ResponseEntity<DriverViolationResponse> recordDriverViolation(
            @PathVariable UUID driverId,
            @Valid @RequestBody DriverViolationRequest request,
            Principal principal
    ) {
        var command = new DriverViolationUseCase.RecordCommand(
                driverId,
                request.tripId(),
                request.violationType(),
                request.severity(),
                request.violationDate(),
                request.penaltyPoints() != null ? request.penaltyPoints() : 0,
                request.fineAmount() != null ? request.fineAmount() : java.math.BigDecimal.ZERO,
                request.location(),
                request.description(),
                actor(principal)
        );
        var created = driverViolations.recordViolation(command);
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/drivers/{driverId}/violations/{violationId}")
    public DriverViolationResponse getDriverViolation(@PathVariable UUID driverId, @PathVariable UUID violationId) {
        return mapper.toResponse(driverViolations.getViolation(driverId, violationId));
    }

    @PostMapping("/drivers/{driverId}/violations/{violationId}/pay")
    public DriverViolationResponse payDriverViolation(
            @PathVariable UUID driverId,
            @PathVariable UUID violationId,
            @RequestBody(required = false) PayFineRequest request,
            Principal principal
    ) {
        var paidAt = request != null ? request.paidAt() : null;
        var paymentReference = request != null ? request.paymentReference() : null;
        var command = new DriverViolationUseCase.PayCommand(
                driverId,
                violationId,
                paidAt,
                paymentReference,
                actor(principal)
        );
        return mapper.toResponse(driverViolations.payFine(command));
    }

    @PostMapping("/drivers/{driverId}/violations/{violationId}/waive")
    public DriverViolationResponse waiveDriverViolation(
            @PathVariable UUID driverId,
            @PathVariable UUID violationId,
            @Valid @RequestBody WaiveFineRequest request,
            Principal principal
    ) {
        var command = new DriverViolationUseCase.WaiveCommand(
                driverId,
                violationId,
                request.reason(),
                actor(principal)
        );
        return mapper.toResponse(driverViolations.waiveFine(command));
    }

    @PostMapping("/drivers/{driverId}/violations/{violationId}/dispute")
    public DriverViolationResponse disputeDriverViolation(
            @PathVariable UUID driverId,
            @PathVariable UUID violationId,
            @Valid @RequestBody WaiveFineRequest request,
            Principal principal
    ) {
        var command = new DriverViolationUseCase.DisputeCommand(
                driverId,
                violationId,
                request.reason(),
                actor(principal)
        );
        return mapper.toResponse(driverViolations.disputeFine(command));
    }

    @GetMapping("/drivers/{driverId}/performance")
    public DriverPerformanceResponse getDriverPerformance(@PathVariable UUID driverId) {
        return mapper.toResponse(driverPerformance.getPerformanceSummary(driverId));
    }

    // Medical Records
    @GetMapping("/drivers/{driverId}/medical-records")
    public List<DriverMedicalRecordResponse> listDriverMedicalRecords(@PathVariable UUID driverId) {
        return mapper.toDriverMedicalRecordResponseList(driverMedicalRecords.list(driverId));
    }

    @PostMapping("/drivers/{driverId}/medical-records")
    public ResponseEntity<DriverMedicalRecordResponse> createDriverMedicalRecord(
            @PathVariable UUID driverId,
            @Valid @RequestBody DriverMedicalRecordRequest request,
            Principal principal
    ) {
        var fitnessStatus = com.transportlogistics.app.fleet.domain.model.DriverMedicalStatus.valueOf(request.fitnessStatus());
        var visionStatus = request.visionTestStatus() != null && !request.visionTestStatus().isBlank()
                ? com.transportlogistics.app.fleet.domain.model.VisionTestStatus.valueOf(request.visionTestStatus()) : null;
        var command = new DriverMedicalRecordUseCase.CreateCommand(
                request.assessmentDate(),
                request.validFrom(),
                request.validUntil(),
                fitnessStatus,
                visionStatus,
                request.restrictions(),
                request.examinerOrProvider(),
                request.certificateReference(),
                request.remarks()
        );
        var created = driverMedicalRecords.create(driverId, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/drivers/{driverId}/medical-records/{recordId}")
    public DriverMedicalRecordResponse getDriverMedicalRecord(
            @PathVariable UUID driverId,
            @PathVariable UUID recordId
    ) {
        return mapper.toResponse(driverMedicalRecords.get(driverId, recordId));
    }

    @PutMapping("/drivers/{driverId}/medical-records/{recordId}")
    public DriverMedicalRecordResponse updateDriverMedicalRecord(
            @PathVariable UUID driverId,
            @PathVariable UUID recordId,
            @Valid @RequestBody DriverMedicalRecordRequest request,
            Principal principal
    ) {
        var fitnessStatus = request.fitnessStatus() != null ? com.transportlogistics.app.fleet.domain.model.DriverMedicalStatus.valueOf(request.fitnessStatus()) : null;
        var visionStatus = request.visionTestStatus() != null && !request.visionTestStatus().isBlank()
                ? com.transportlogistics.app.fleet.domain.model.VisionTestStatus.valueOf(request.visionTestStatus()) : null;
        var command = new DriverMedicalRecordUseCase.UpdateCommand(
                request.assessmentDate(),
                request.validFrom(),
                request.validUntil(),
                fitnessStatus,
                visionStatus,
                request.restrictions(),
                request.examinerOrProvider(),
                request.certificateReference(),
                request.remarks(),
                null
        );
        return mapper.toResponse(driverMedicalRecords.update(driverId, recordId, command, actor(principal)));
    }

    @PatchMapping("/drivers/{driverId}/medical-records/{recordId}")
    public DriverMedicalRecordResponse patchDriverMedicalRecord(
            @PathVariable UUID driverId,
            @PathVariable UUID recordId,
            @RequestBody DriverMedicalRecordPatchRequest request,
            Principal principal
    ) {
        var fitnessStatus = request.fitnessStatus() != null && !request.fitnessStatus().isBlank()
                ? com.transportlogistics.app.fleet.domain.model.DriverMedicalStatus.valueOf(request.fitnessStatus()) : null;
        var visionStatus = request.visionTestStatus() != null && !request.visionTestStatus().isBlank()
                ? com.transportlogistics.app.fleet.domain.model.VisionTestStatus.valueOf(request.visionTestStatus()) : null;
        var command = new DriverMedicalRecordUseCase.UpdateCommand(
                request.assessmentDate(),
                request.validFrom(),
                request.validUntil(),
                fitnessStatus,
                visionStatus,
                request.restrictions(),
                request.examinerOrProvider(),
                request.certificateReference(),
                request.remarks(),
                request.active()
        );
        return mapper.toResponse(driverMedicalRecords.update(driverId, recordId, command, actor(principal)));
    }

    // Drug Tests
    @GetMapping("/drivers/{driverId}/drug-tests")
    public List<DriverDrugTestResponse> listDriverDrugTests(@PathVariable UUID driverId) {
        return mapper.toDriverDrugTestResponseList(driverDrugTests.list(driverId));
    }

    @PostMapping("/drivers/{driverId}/drug-tests")
    public ResponseEntity<DriverDrugTestResponse> scheduleDriverDrugTest(
            @PathVariable UUID driverId,
            @Valid @RequestBody DriverDrugTestRequest request,
            Principal principal
    ) {
        var testType = com.transportlogistics.app.fleet.domain.model.DrugTestType.valueOf(request.testType());
        var command = new DriverDrugTestUseCase.ScheduleCommand(
                testType,
                request.scheduledDate(),
                request.laboratoryOrProvider(),
                request.referenceNumber(),
                request.remarks()
        );
        var scheduled = driverDrugTests.schedule(driverId, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(scheduled));
    }

    @GetMapping("/drivers/{driverId}/drug-tests/{testId}")
    public DriverDrugTestResponse getDriverDrugTest(
            @PathVariable UUID driverId,
            @PathVariable UUID testId
    ) {
        return mapper.toResponse(driverDrugTests.get(driverId, testId));
    }

    @PostMapping("/drivers/{driverId}/drug-tests/{testId}/sample")
    public DriverDrugTestResponse recordDrugTestSample(
            @PathVariable UUID driverId,
            @PathVariable UUID testId,
            @RequestBody(required = false) DriverDrugTestSampleRequest request,
            Principal principal
    ) {
        var sampleTime = request != null ? request.sampleCollectedAt() : null;
        var command = new DriverDrugTestUseCase.RecordSampleCommand(sampleTime);
        return mapper.toResponse(driverDrugTests.recordSample(driverId, testId, command, actor(principal)));
    }

    @PostMapping("/drivers/{driverId}/drug-tests/{testId}/result")
    public DriverDrugTestResponse recordDrugTestResult(
            @PathVariable UUID driverId,
            @PathVariable UUID testId,
            @Valid @RequestBody DriverDrugTestResultRequest request,
            Principal principal
    ) {
        var result = com.transportlogistics.app.fleet.domain.model.DrugTestResult.valueOf(request.result());
        var command = new DriverDrugTestUseCase.RecordResultCommand(
                result,
                request.resultDate(),
                request.remarks(),
                request.returnToDutyRequired()
        );
        return mapper.toResponse(driverDrugTests.recordResult(driverId, testId, command, actor(principal)));
    }

    @PostMapping("/drivers/{driverId}/drug-tests/{testId}/return-to-duty-clear")
    public DriverDrugTestResponse clearReturnToDuty(
            @PathVariable UUID driverId,
            @PathVariable UUID testId,
            @RequestBody(required = false) DriverDrugTestClearanceRequest request,
            Principal principal
    ) {
        var clearedAt = request != null ? request.clearedAt() : null;
        var remarks = request != null ? request.remarks() : null;
        var command = new DriverDrugTestUseCase.ReturnToDutyClearanceCommand(clearedAt, remarks);
        return mapper.toResponse(driverDrugTests.clearReturnToDuty(driverId, testId, command, actor(principal)));
    }

    @PostMapping("/drivers/{driverId}/drug-tests/{testId}/cancel")
    public DriverDrugTestResponse cancelDriverDrugTest(
            @PathVariable UUID driverId,
            @PathVariable UUID testId,
            @RequestBody(required = false) DriverDrugTestClearanceRequest request,
            Principal principal
    ) {
        var remarks = request != null ? request.remarks() : null;
        return mapper.toResponse(driverDrugTests.cancel(driverId, testId, remarks, actor(principal)));
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
                                                  @RequestParam(required = false) UUID requiredVehicleTypeId,
                                                  @RequestParam(required = false) Double requiredCapacityKg) {
        var list = vehicles.list().stream()
                .filter(vehicle -> vehicleAvailability.evaluate(new VehicleAvailabilityUseCase.Query(vehicle.id(),
                        from, to, requiredVehicleTypeId, requiredCapacityKg, null)).available())
                .toList();
        return mapper.toVehicleResponseList(list);
    }

    @GetMapping("/vehicles/{vehicleId}/documents")
    public List<VehicleDocumentResponse> listDocuments(@PathVariable UUID vehicleId) {
        return mapper.toVehicleDocumentResponseList(documents.list(vehicleId));
    }

    @PostMapping("/vehicles/{vehicleId}/documents")
    public ResponseEntity<VehicleDocumentResponse> createDocument(@PathVariable UUID vehicleId,
                                                                  @Valid @RequestBody DocumentRequest request,
                                                                  Principal principal) {
        var command = new VehicleDocumentUseCase.CreateCommand(request.documentType(), request.documentNumber(),
                request.issueDate(), request.expiryDate(), request.fileReference(), request.mandatoryForDispatch(),
                request.status(), request.active());
        var created = documents.create(vehicleId, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @PatchMapping("/vehicles/{vehicleId}/documents/{documentId}")
    public VehicleDocumentResponse updateDocument(@PathVariable UUID vehicleId,
                                                  @PathVariable UUID documentId,
                                                  @RequestBody DocumentPatchRequest request,
                                                  Principal principal) {
        var command = new VehicleDocumentUseCase.UpdateCommand(request.documentType(), request.documentNumber(),
                request.issueDate(), request.expiryDate(), request.fileReference(), request.mandatoryForDispatch(),
                request.status(), request.active());
        return mapper.toResponse(documents.update(vehicleId, documentId, command, actor(principal)));
    }

    @DeleteMapping("/vehicles/{vehicleId}/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID vehicleId,
                                               @PathVariable UUID documentId,
                                               Principal principal) {
        documents.delete(vehicleId, documentId, actor(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vehicles/{vehicleId}/maintenance-schedules")
    public List<MaintenanceScheduleResponse> listMaintenanceSchedules(@PathVariable UUID vehicleId) {
        return mapper.toMaintenanceScheduleResponseList(maintenanceSchedules.list(vehicleId));
    }

    @PostMapping("/vehicles/{vehicleId}/maintenance-schedules")
    public ResponseEntity<MaintenanceScheduleResponse> createMaintenanceSchedule(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody MaintenanceScheduleRequest request,
            Principal principal
    ) {
        var command = new MaintenanceScheduleUseCase.CreateCommand(
                request.maintenanceType(),
                request.scheduledStart(),
                request.scheduledEnd(),
                request.description(),
                request.serviceProvider(),
                request.cost()
        );
        var created = maintenanceSchedules.create(vehicleId, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/vehicles/{vehicleId}/maintenance-schedules/{scheduleId}")
    public MaintenanceScheduleResponse getMaintenanceSchedule(@PathVariable UUID vehicleId,
                                                              @PathVariable UUID scheduleId) {
        return mapper.toResponse(maintenanceSchedules.get(vehicleId, scheduleId));
    }

    @PutMapping("/vehicles/{vehicleId}/maintenance-schedules/{scheduleId}")
    public MaintenanceScheduleResponse updateMaintenanceSchedule(
            @PathVariable UUID vehicleId,
            @PathVariable UUID scheduleId,
            @Valid @RequestBody MaintenanceScheduleRequest request,
            Principal principal
    ) {
        var command = new MaintenanceScheduleUseCase.UpdateCommand(
                request.maintenanceType(),
                request.scheduledStart(),
                request.scheduledEnd(),
                null,
                request.description(),
                request.serviceProvider(),
                request.cost()
        );
        return mapper.toResponse(maintenanceSchedules.update(vehicleId, scheduleId, command, actor(principal)));
    }

    @PatchMapping("/vehicles/{vehicleId}/maintenance-schedules/{scheduleId}")
    public MaintenanceScheduleResponse patchMaintenanceSchedule(
            @PathVariable UUID vehicleId,
            @PathVariable UUID scheduleId,
            @RequestBody MaintenanceSchedulePatchRequest request,
            Principal principal
    ) {
        var command = new MaintenanceScheduleUseCase.UpdateCommand(
                request.maintenanceType(),
                request.scheduledStart(),
                request.scheduledEnd(),
                request.status(),
                request.description(),
                request.serviceProvider(),
                request.cost()
        );
        return mapper.toResponse(maintenanceSchedules.update(vehicleId, scheduleId, command, actor(principal)));
    }

    @PostMapping("/vehicles/{vehicleId}/maintenance-schedules/{scheduleId}/cancel")
    public MaintenanceScheduleResponse cancelMaintenanceSchedule(@PathVariable UUID vehicleId,
                                                                 @PathVariable UUID scheduleId,
                                                                 @RequestBody(required = false) MaintenanceActionRequest r,
                                                                 Principal principal) {
        var reason = r != null ? r.remarks() : null;
        return mapper.toResponse(maintenanceSchedules.cancel(vehicleId, scheduleId, reason, actor(principal)));
    }

    @PostMapping("/vehicles/{vehicleId}/maintenance-schedules/{scheduleId}/complete")
    public MaintenanceScheduleResponse completeMaintenanceSchedule(@PathVariable UUID vehicleId,
                                                                    @PathVariable UUID scheduleId,
                                                                    @RequestBody(required = false) MaintenanceActionRequest r,
                                                                    Principal principal) {
        var remarks = r != null ? r.remarks() : null;
        return mapper.toResponse(maintenanceSchedules.complete(vehicleId, scheduleId, remarks, actor(principal)));
    }

    @GetMapping("/vehicles/{vehicleId}/lubricant-logs")
    public List<LubricantLogResponse> listLubricantLogs(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) String fluidType,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to
    ) {
        var type = fluidType != null && !fluidType.isBlank()
                ? com.transportlogistics.app.fleet.domain.model.FluidType.valueOf(fluidType.trim())
                : null;
        return mapper.toLubricantLogResponseList(lubricantLogs.list(vehicleId, type, from, to));
    }

    @PostMapping("/vehicles/{vehicleId}/lubricant-logs")
    public ResponseEntity<LubricantLogResponse> createLubricantLog(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody LubricantLogRequest r,
            Principal principal
    ) {
        var command = new LubricantLogUseCase.CreateCommand(
                com.transportlogistics.app.fleet.domain.model.FluidType.fromString(r.fluidType()),
                r.quantity(),
                com.transportlogistics.app.fleet.domain.model.MeasurementUnit.fromString(r.unit()),
                r.recordedAt(),
                r.odometerKm(),
                r.engineHours(),
                r.vendorId(),
                r.supplierName(),
                r.referenceNumber(),
                r.remarks()
        );
        var created = lubricantLogs.create(vehicleId, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/vehicles/{vehicleId}/lubricant-logs/{logId}")
    public LubricantLogResponse getLubricantLog(
            @PathVariable UUID vehicleId,
            @PathVariable UUID logId
    ) {
        return mapper.toResponse(lubricantLogs.get(vehicleId, logId));
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
