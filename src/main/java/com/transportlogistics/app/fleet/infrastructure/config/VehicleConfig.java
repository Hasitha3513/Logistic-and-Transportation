package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.VehicleDispatchEligibility;
import com.transportlogistics.app.fleet.VehicleAssignmentEligibility;
import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.fleet.application.ports.in.VehicleAvailabilityUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.application.service.VehicleAvailabilityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VehicleConfig {
    @Bean
    VehicleAvailabilityUseCase vehicleAvailabilityUseCase(VehicleRepository vehicles,
                                                           VehicleDocumentRepository documents,
                                                           VehicleAllocationAvailability allocations,
                                                           com.transportlogistics.app.fleet.application.ports.out.MaintenanceScheduleRepository maintenanceSchedules) {
        return new VehicleAvailabilityService(vehicles, documents, allocations, maintenanceSchedules);
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
}
