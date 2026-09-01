package com.transportlogistics.app.delivery.adapters.config;

import com.transportlogistics.app.delivery.application.DeliveryBatchService;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryBatchUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchCodeGenerator;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchEventPublisherPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DriverEligibilityPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DeliveryBatchConfig {

    @Bean
    public DeliveryBatchUseCase deliveryBatchUseCase(
            DeliveryBatchRepository batchRepository,
            DeliveryOrderRepository orderRepository,
            DeliveryRiderRepository riderRepository,
            DriverEligibilityPort driverEligibilityPort,
            @org.springframework.context.annotation.Lazy DeliveryZoneLookupPort zoneLookupPort,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            DeliveryBatchCodeGenerator codeGenerator,
            DeliveryBatchEventPublisherPort eventPublisher,
            Clock clock
    ) {
        return new DeliveryBatchService(
                batchRepository,
                orderRepository,
                riderRepository,
                driverEligibilityPort,
                zoneLookupPort,
                tenantContext,
                transactions,
                codeGenerator,
                eventPublisher,
                clock
        );
    }
}
