package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.in.DriverMedicalRecordUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverMedicalRecordRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.service.DriverMedicalRecordService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DriverMedicalConfig {

    @Bean
    DriverMedicalRecordUseCase driverMedicalRecordUseCase(DriverRepository drivers, DriverMedicalRecordRepository medicalRecords) {
        return new DriverMedicalRecordService(drivers, medicalRecords);
    }
}
