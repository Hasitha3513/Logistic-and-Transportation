package com.transportlogistics.app.freight.exception.adapters.config;

import com.transportlogistics.app.freight.exception.application.CargoExceptionService;
import com.transportlogistics.app.freight.exception.ports.inbound.CargoExceptionUseCase;
import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionNumberGenerator;
import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionRepository;
import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionTransaction;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CargoExceptionConfig {

    @Bean
    public CargoExceptionUseCase cargoExceptionUseCase(
            CargoExceptionRepository repository,
            CargoExceptionNumberGenerator numberGenerator,
            CargoExceptionTransaction transactions,
            FreightOrderLookup freightOrderLookup,
            Clock clock) {
        return new CargoExceptionService(
                repository,
                numberGenerator,
                transactions,
                freightOrderLookup,
                clock
        );
    }
}
