package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.VehicleDispatchEligibility;
import com.transportlogistics.app.fleet.VehicleAssignmentEligibility;
import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.fleet.VehicleFuelContextLookup;
import com.transportlogistics.app.fleet.application.ports.in.VehicleAvailabilityUseCase;
import com.transportlogistics.app.fleet.application.ports.in.VehicleUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.application.service.VehicleService;
import com.transportlogistics.app.fleet.application.service.VehicleAvailabilityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
class VehicleConfig {
    @Bean
    VehicleUseCase vehicleUseCase(VehicleRepository repo) {
        return new VehicleService(repo);
    }

    @Bean
    VehicleFuelContextLookup vehicleFuelContextLookup(VehicleRepository vehicles) {
        return vehicleId -> vehicles.findById(vehicleId).map(vehicle ->
                new VehicleFuelContextLookup.VehicleFuelContext(vehicle.id(), vehicle.registrationNumber(),
                        vehicle.active(), vehicle.operationalStatus(), decimal(vehicle.currentOdometerKm()),
                        decimal(vehicle.engineHours())));
    }

    @Bean
    VehicleAvailabilityUseCase vehicleAvailabilityUseCase(VehicleRepository vehicles,
                                                           VehicleDocumentRepository documents,
                                                           VehicleAllocationAvailability allocations) {
        return new VehicleAvailabilityService(vehicles, documents, allocations);
    }

    @Bean
    VehicleDispatchEligibility vehicleDispatchEligibility(VehicleAvailabilityUseCase availability) {
        return (vehicleId, from, to, requiredType, requiredCapacity, excludeTripId) -> {
            var result = availability.evaluate(new VehicleAvailabilityUseCase.Query(vehicleId, from, to,
                    requiredType, requiredCapacity, excludeTripId));
            if (!result.available()) {
                var codes = result.reasons().stream().map(reason -> reason.code().name()).toList();
                throw new IllegalArgumentException("Vehicle is unavailable for allocation or dispatch: " + codes);
            }
        };
    }

    @Bean
    VehicleAssignmentEligibility vehicleAssignmentEligibility(VehicleAvailabilityUseCase availability) {
        return (vehicleId, from, to, requiredType, requiredCapacity) -> {
            var result = availability.evaluate(new VehicleAvailabilityUseCase.Query(vehicleId, from, to,
                    requiredType, requiredCapacity, null, false, true));
            if (!result.available()) {
                var codes = result.reasons().stream().map(reason -> reason.code().name()).toList();
                throw new IllegalArgumentException("Vehicle is ineligible for assignment: " + codes);
            }
        };
    }

    private static BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
