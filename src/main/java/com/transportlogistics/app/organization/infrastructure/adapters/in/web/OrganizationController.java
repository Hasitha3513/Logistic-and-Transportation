package com.transportlogistics.app.organization.infrastructure.adapters.in.web;

import com.transportlogistics.app.organization.application.ports.in.CustomerUseCase;
import com.transportlogistics.app.organization.application.ports.in.DepartmentUseCase;
import com.transportlogistics.app.organization.application.ports.in.LocationUseCase;
import com.transportlogistics.app.organization.application.ports.in.ProjectUseCase;
import com.transportlogistics.app.organization.application.ports.in.VendorUseCase;
import com.transportlogistics.app.organization.domain.model.Customer;
import com.transportlogistics.app.organization.domain.model.Department;
import com.transportlogistics.app.organization.domain.model.Location;
import com.transportlogistics.app.organization.domain.model.Project;
import com.transportlogistics.app.organization.domain.model.Vendor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class OrganizationController {
    private final CustomerUseCase customers;
    private final DepartmentUseCase departments;
    private final LocationUseCase locations;
    private final ProjectUseCase projects;
    private final VendorUseCase vendors;

    OrganizationController(CustomerUseCase c, DepartmentUseCase d, LocationUseCase l, ProjectUseCase p,
                           VendorUseCase v) {
        customers = c;
        departments = d;
        locations = l;
        projects = p;
        vendors = v;
    }

    @PostMapping("/customers")
    ResponseEntity<Customer> createCustomer(@Valid @RequestBody CustomerRequest r) {
        return ResponseEntity.status(201).body(customers.create(new Customer(UUID.randomUUID(), r.code(), r.name(), r.contactPerson(), r.phone(), r.email(), r.active() == null || r.active())));
    }

    @GetMapping("/customers")
    List<Customer> listCustomers() {
        return customers.list();
    }

    @GetMapping("/customers/{id}")
    Customer getCustomer(@PathVariable UUID id) {
        return customers.get(id);
    }

    @PutMapping("/customers/{id}")
    Customer updateCustomer(@PathVariable UUID id, @Valid @RequestBody CustomerRequest r) {
        return customers.update(id, new Customer(id, r.code(), r.name(), r.contactPerson(), r.phone(), r.email(), r.active() == null || r.active()));
    }

    @DeleteMapping("/customers/{id}")
    MessageResponse deactivateCustomer(@PathVariable UUID id) {
        customers.deactivate(id);
        return new MessageResponse("Customer deactivated");
    }

    @PostMapping("/departments")
    ResponseEntity<Department> createDepartment(@Valid @RequestBody DepartmentRequest r) {
        return ResponseEntity.status(201).body(departments.create(new Department(UUID.randomUUID(), r.code(), r.name(), r.description(), r.active() == null || r.active())));
    }

    @GetMapping("/departments")
    List<Department> listDepartments() {
        return departments.list();
    }

    @PutMapping("/departments/{id}")
    Department updateDepartment(@PathVariable UUID id, @Valid @RequestBody DepartmentRequest r) {
        return departments.update(id, new Department(id, r.code(), r.name(), r.description(),
                r.active() == null || r.active()));
    }

    @PostMapping("/locations")
    ResponseEntity<Location> createLocation(@Valid @RequestBody LocationRequest r) {
        return ResponseEntity.status(201).body(locations.create(new Location(UUID.randomUUID(), r.code(), r.name(), r.address(), r.latitude(), r.longitude(), r.active() == null || r.active())));
    }

    @GetMapping("/locations")
    List<Location> listLocations() {
        return locations.list();
    }

    @GetMapping("/locations/{id}")
    Location getLocation(@PathVariable UUID id) {
        return locations.get(id);
    }

    @PutMapping("/locations/{id}")
    Location updateLocation(@PathVariable UUID id, @Valid @RequestBody LocationRequest r) {
        return locations.update(id, new Location(id, r.code(), r.name(), r.address(), r.latitude(), r.longitude(), r.active() == null || r.active()));
    }

    @DeleteMapping("/locations/{id}")
    MessageResponse deactivateLocation(@PathVariable UUID id) {
        locations.deactivate(id);
        return new MessageResponse("Location deactivated");
    }

    @PostMapping("/projects")
    ResponseEntity<Project> createProject(@Valid @RequestBody ProjectRequest r) {
        return ResponseEntity.status(201).body(projects.create(new Project(UUID.randomUUID(), r.code(), r.name(), r.departmentId(), r.active() == null || r.active())));
    }

    @GetMapping("/projects")
    List<Project> listProjects() {
        return projects.list();
    }

    @PutMapping("/projects/{id}")
    Project updateProject(@PathVariable UUID id, @Valid @RequestBody ProjectRequest r) {
        return projects.update(id, new Project(id, r.code(), r.name(), r.departmentId(),
                r.active() == null || r.active()));
    }

    @PostMapping("/vendors")
    ResponseEntity<Vendor> createVendor(@Valid @RequestBody VendorRequest r) {
        return ResponseEntity.status(201).body(vendors.create(new Vendor(UUID.randomUUID(), r.code(), r.name(),
                r.contactPerson(), r.phone(), r.email(), r.active() == null || r.active())));
    }

    @GetMapping("/vendors")
    List<Vendor> listVendors(@RequestParam(required = false) Boolean active) {
        return vendors.list(active);
    }

    @GetMapping("/vendors/{id}")
    Vendor getVendor(@PathVariable UUID id) {
        return vendors.get(id);
    }

    @PutMapping("/vendors/{id}")
    Vendor updateVendor(@PathVariable UUID id, @Valid @RequestBody VendorRequest r) {
        return vendors.update(id, new Vendor(id, r.code(), r.name(), r.contactPerson(), r.phone(), r.email(),
                r.active() == null || r.active()));
    }

    @DeleteMapping("/vendors/{id}")
    MessageResponse deactivateVendor(@PathVariable UUID id) {
        vendors.deactivate(id);
        return new MessageResponse("Vendor deactivated");
    }

    record CustomerRequest(@NotBlank String code, @NotBlank String name, String contactPerson, String phone,
                           @Email String email, Boolean active) {
    }

    record DepartmentRequest(@NotBlank String code, @NotBlank String name, String description, Boolean active) {
    }

    record LocationRequest(@NotBlank String code, @NotBlank String name, String address, Double latitude,
                           Double longitude, Boolean active) {
    }

    record ProjectRequest(@NotBlank String code, @NotBlank String name, UUID departmentId, Boolean active) {
    }

    record VendorRequest(@NotBlank String code, @NotBlank String name, String contactPerson, String phone,
                         @Email String email, Boolean active) {
    }

    record MessageResponse(String message) {
    }
}
