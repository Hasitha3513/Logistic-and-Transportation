package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.fleet.application.ports.in.MaintenanceScheduleUseCase;
import com.transportlogistics.app.fleet.application.ports.out.MaintenanceScheduleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.application.service.MaintenanceScheduleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MaintenanceScheduleConfig {

    @Bean
    MaintenanceScheduleUseCase maintenanceScheduleUseCase(
            MaintenanceScheduleRepository maintenanceSchedules,
            VehicleRepository vehicles,
            VehicleAllocationAvailability allocations
    ) {
        return new MaintenanceScheduleService(maintenanceSchedules, vehicles, allocations);
    }
}
