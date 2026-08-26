package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.in.DriverDrugTestUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverDrugTestRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.FleetOperationalNotificationPublisher;
import com.transportlogistics.app.fleet.application.service.DriverDrugTestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DriverDrugTestConfig {

    @Bean
    DriverDrugTestUseCase driverDrugTestUseCase(DriverRepository drivers, DriverDrugTestRepository drugTests,
                                                FleetOperationalNotificationPublisher notifications) {
        return new DriverDrugTestService(drivers, drugTests, notifications);
    }
}
