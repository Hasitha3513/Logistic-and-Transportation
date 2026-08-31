package com.transportlogistics.app.delivery.adapters.config;

import com.transportlogistics.app.delivery.application.DeliverySlotService;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliverySlotRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DeliverySlotConfig {

    @Bean
    public DeliverySlotService deliverySlotService(
            DeliverySlotRepository slotRepository,
            @org.springframework.context.annotation.Lazy com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort zoneLookupPort,
            DeliveryOrderRepository orderRepository,
            DeliveryLocationLookupPort locationLookupPort,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            Clock clock
    ) {
        return new DeliverySlotService(
                slotRepository,
                zoneLookupPort,
                orderRepository,
                locationLookupPort,
                tenantContext,
                transactions,
                clock
        );
    }
}
