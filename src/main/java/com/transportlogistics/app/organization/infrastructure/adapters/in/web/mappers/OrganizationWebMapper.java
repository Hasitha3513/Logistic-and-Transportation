package com.transportlogistics.app.organization.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.organization.domain.model.Customer;
import com.transportlogistics.app.organization.domain.model.Department;
import com.transportlogistics.app.organization.domain.model.Location;
import com.transportlogistics.app.organization.domain.model.Project;
import com.transportlogistics.app.organization.domain.model.Vendor;
import com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.response.CustomerResponse;
import com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.response.DepartmentResponse;
import com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.response.LocationResponse;
import com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.response.ProjectResponse;
import com.transportlogistics.app.organization.infrastructure.adapters.in.web.dto.response.VendorResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrganizationWebMapper {

    CustomerResponse toResponse(Customer customer);
    List<CustomerResponse> toCustomerResponseList(List<Customer> customers);

    DepartmentResponse toResponse(Department department);
    List<DepartmentResponse> toDepartmentResponseList(List<Department> departments);

    LocationResponse toResponse(Location location);
    List<LocationResponse> toLocationResponseList(List<Location> locations);

    ProjectResponse toResponse(Project project);
    List<ProjectResponse> toProjectResponseList(List<Project> projects);

    VendorResponse toResponse(Vendor vendor);
    List<VendorResponse> toVendorResponseList(List<Vendor> vendors);
}
