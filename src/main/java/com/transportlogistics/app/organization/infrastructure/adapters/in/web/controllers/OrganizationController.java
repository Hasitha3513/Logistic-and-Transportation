package com.transportlogistics.app.organization.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.organization.application.ports.in.*;
import com.transportlogistics.app.organization.domain.model.*;
import com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.request.*;
import com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.response.*;
import com.transportlogistics.app.organization.infrastructure.adapters.in.web.mappers.OrganizationWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrganizationController {

    private final CustomerUseCase customers;
    private final DepartmentUseCase departments;
    private final LocationUseCase locations;
    private final ProjectUseCase projects;
    private final VendorUseCase vendors;
    private final OrganizationWebMapper mapper;

    @PostMapping("/customers")
    ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest r) {
        var created = customers.create(new Customer(UUID.randomUUID(), r.code(), r.name(), r.contactPerson(), r.phone(), r.email(), r.active() == null || r.active()));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/customers")
    List<CustomerResponse> listCustomers() {
        return mapper.toCustomerResponseList(customers.list());
    }

    @GetMapping("/customers/{id}")
    CustomerResponse getCustomer(@PathVariable UUID id) {
        return mapper.toResponse(customers.get(id));
    }

    @PutMapping("/customers/{id}")
    CustomerResponse updateCustomer(@PathVariable UUID id, @Valid @RequestBody CustomerRequest r) {
        var updated = customers.update(id, new Customer(id, r.code(), r.name(), r.contactPerson(), r.phone(), r.email(), r.active() == null || r.active()));
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/customers/{id}")
    MessageResponse deactivateCustomer(@PathVariable UUID id) {
        customers.deactivate(id);
        return new MessageResponse("Customer deactivated");
    }

    @PostMapping("/departments")
    ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest r) {
        var created = departments.create(new Department(UUID.randomUUID(), r.code(), r.name(), r.description(), r.active() == null || r.active()));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/departments")
    List<DepartmentResponse> listDepartments() {
        return mapper.toDepartmentResponseList(departments.list());
    }

    @PutMapping("/departments/{id}")
    DepartmentResponse updateDepartment(@PathVariable UUID id, @Valid @RequestBody DepartmentRequest r) {
        var updated = departments.update(id, new Department(id, r.code(), r.name(), r.description(),
                r.active() == null || r.active()));
        return mapper.toResponse(updated);
    }

    @PostMapping("/locations")
    ResponseEntity<LocationResponse> createLocation(@Valid @RequestBody LocationRequest r) {
        var created = locations.create(new Location(UUID.randomUUID(), r.code(), r.name(), r.address(), r.latitude(), r.longitude(), r.active() == null || r.active()));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/locations")
    List<LocationResponse> listLocations() {
        return mapper.toLocationResponseList(locations.list());
    }

    @GetMapping("/locations/{id}")
    LocationResponse getLocation(@PathVariable UUID id) {
        return mapper.toResponse(locations.get(id));
    }

    @PutMapping("/locations/{id}")
    LocationResponse updateLocation(@PathVariable UUID id, @Valid @RequestBody LocationRequest r) {
        var updated = locations.update(id, new Location(id, r.code(), r.name(), r.address(), r.latitude(), r.longitude(), r.active() == null || r.active()));
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/locations/{id}")
    MessageResponse deactivateLocation(@PathVariable UUID id) {
        locations.deactivate(id);
        return new MessageResponse("Location deactivated");
    }

    @PostMapping("/projects")
    ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest r) {
        var created = projects.create(new Project(UUID.randomUUID(), r.code(), r.name(), r.departmentId(), r.active() == null || r.active()));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/projects")
    List<ProjectResponse> listProjects() {
        return mapper.toProjectResponseList(projects.list());
    }

    @PutMapping("/projects/{id}")
    ProjectResponse updateProject(@PathVariable UUID id, @Valid @RequestBody ProjectRequest r) {
        var updated = projects.update(id, new Project(id, r.code(), r.name(), r.departmentId(),
                r.active() == null || r.active()));
        return mapper.toResponse(updated);
    }

    @PostMapping("/vendors")
    ResponseEntity<VendorResponse> createVendor(@Valid @RequestBody VendorRequest r) {
        var created = vendors.create(new Vendor(UUID.randomUUID(), r.code(), r.name(),
                r.contactPerson(), r.phone(), r.email(), r.active() == null || r.active()));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/vendors")
    List<VendorResponse> listVendors(@RequestParam(required = false) Boolean active) {
        return mapper.toVendorResponseList(vendors.list(active));
    }

    @GetMapping("/vendors/{id}")
    VendorResponse getVendor(@PathVariable UUID id) {
        return mapper.toResponse(vendors.get(id));
    }

    @PutMapping("/vendors/{id}")
    VendorResponse updateVendor(@PathVariable UUID id, @Valid @RequestBody VendorRequest r) {
        var updated = vendors.update(id, new Vendor(id, r.code(), r.name(), r.contactPerson(), r.phone(), r.email(),
                r.active() == null || r.active()));
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/vendors/{id}")
    MessageResponse deactivateVendor(@PathVariable UUID id) {
        vendors.deactivate(id);
        return new MessageResponse("Vendor deactivated");
    }
}
