package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.FleetFuelPerformanceLookup;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
class FuelPerformanceFleetConfig {
    @Bean
    FleetFuelPerformanceLookup fleetFuelPerformanceLookup(VehicleRepository vehicles, DriverRepository drivers) {
        return new FleetFuelPerformanceLookup() {
            @Override
            public java.util.Map<java.util.UUID, VehicleContext> vehicles(java.util.Set<java.util.UUID> ids) {
                return vehicles.findAllByIds(ids).stream().map(vehicle -> new VehicleContext(vehicle.id(),
                                vehicle.registrationNumber(), vehicle.typeId(), vehicle.active()))
                        .collect(Collectors.toMap(VehicleContext::vehicleId, Function.identity()));
            }

            @Override
            public java.util.Map<java.util.UUID, DriverContext> drivers(java.util.Set<java.util.UUID> ids) {
                return drivers.findAllByIds(ids).stream().map(driver -> new DriverContext(driver.id(),
                                display(driver.firstName(), driver.lastName(), driver.employeeNumber()), driver.active()))
                        .collect(Collectors.toMap(DriverContext::driverId, Function.identity()));
            }
        };
    }

    private static String display(String firstName, String lastName, String employeeNumber) {
        var name = ((firstName == null ? "" : firstName.trim()) + " "
                + (lastName == null ? "" : lastName.trim())).trim();
        return name.isEmpty() ? employeeNumber : name;
    }
}
