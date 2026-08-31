package com.transportlogistics.app.delivery.adapters.config;

import com.transportlogistics.app.delivery.application.DeliveryRiderService;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryRiderUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderEventPublisherPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DriverEligibilityPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DeliveryRiderConfig {

    @Bean
    public DeliveryRiderUseCase deliveryRiderUseCase(
            DeliveryRiderRepository riderRepository,
            DeliveryOrderRepository orderRepository,
            DriverEligibilityPort driverEligibilityPort,
            @org.springframework.context.annotation.Lazy DeliveryZoneLookupPort zoneLookupPort,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            DeliveryRiderEventPublisherPort eventPublisher,
            Clock clock
    ) {
        return new DeliveryRiderService(
                riderRepository,
                orderRepository,
                driverEligibilityPort,
                zoneLookupPort,
                tenantContext,
                transactions,
                eventPublisher,
                clock
        );
    }
}
