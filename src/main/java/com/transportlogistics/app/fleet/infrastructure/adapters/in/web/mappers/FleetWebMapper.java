package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response.*;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FleetWebMapper {

    DriverResponse toResponse(Driver driver);
    List<DriverResponse> toDriverResponseList(List<Driver> drivers);

    DriverLicenseResponse toResponse(DriverLicense license);
    List<DriverLicenseResponse> toDriverLicenseResponseList(List<DriverLicense> licenses);

    VehicleResponse toResponse(Vehicle vehicle);
    List<VehicleResponse> toVehicleResponseList(List<Vehicle> vehicles);

    VehicleDocumentResponse toResponse(VehicleDocument document);
    List<VehicleDocumentResponse> toVehicleDocumentResponseList(List<VehicleDocument> documents);

    VehicleCategoryResponse toResponse(VehicleCategory category);
    List<VehicleCategoryResponse> toVehicleCategoryResponseList(List<VehicleCategory> categories);

    VehicleTypeResponse toResponse(VehicleType type);
    List<VehicleTypeResponse> toVehicleTypeResponseList(List<VehicleType> types);

    default DriverAvailabilityResponse toResponse(DriverAvailability availability) {
        return DriverAvailabilityResponse.from(availability);
    }

    default VehicleAvailabilityResponse toResponse(VehicleAvailability availability) {
        return VehicleAvailabilityResponse.from(availability);
    }
}
