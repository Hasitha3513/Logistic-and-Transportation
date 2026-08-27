package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.in.LubricantLogUseCase;
import com.transportlogistics.app.fleet.application.ports.out.LubricantLogRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.application.service.LubricantLogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LubricantLogConfig {

    @Bean
    public LubricantLogUseCase lubricantLogUseCase(
            VehicleRepository vehicles,
            LubricantLogRepository lubricantLogs
    ) {
        return new LubricantLogService(vehicles, lubricantLogs);
    }
}
