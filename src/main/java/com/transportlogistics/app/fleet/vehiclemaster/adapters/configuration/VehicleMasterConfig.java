package com.transportlogistics.app.fleet.vehiclemaster.adapters.configuration;

import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.fleet.VehicleFuelContextLookup;
import com.transportlogistics.app.fleet.application.ports.out.VehicleCategoryRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleTypeRepository;
import com.transportlogistics.app.fleet.vehiclemaster.application.service.VehicleService;
import com.transportlogistics.app.fleet.vehiclemaster.ports.inbound.VehicleUseCase;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
class VehicleMasterConfig {

    @Bean
    VehicleUseCase vehicleUseCase(VehicleRepository repository,
                                  VehicleCategoryRepository categories,
                                  VehicleTypeRepository types,
                                  @Autowired(required = false) VehicleAllocationAvailability allocations) {
        return new VehicleService(repository, categories, types, allocations);
    }

    @Bean
    VehicleFuelContextLookup vehicleFuelContextLookup(VehicleRepository vehicles) {
        return vehicleId -> vehicles.findById(vehicleId).map(vehicle ->
                new VehicleFuelContextLookup.VehicleFuelContext(vehicle.id(), vehicle.registrationNumber(),
                        vehicle.active(), vehicle.operationalStatus(), decimal(vehicle.currentOdometerKm()),
                        decimal(vehicle.engineHours())));
    }

    private static BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
