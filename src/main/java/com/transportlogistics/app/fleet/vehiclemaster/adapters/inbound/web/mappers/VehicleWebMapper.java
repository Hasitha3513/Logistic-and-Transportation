package com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.mappers;

import com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.dto.response.VehicleResponse;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleWebMapper {
    VehicleResponse toResponse(Vehicle vehicle);

    List<VehicleResponse> toResponseList(List<Vehicle> vehicles);
}
