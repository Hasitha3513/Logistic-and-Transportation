package com.transportlogistics.app.delivery.adapters.config;

import com.transportlogistics.app.delivery.application.DeliveryEtaService;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryEtaUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeliveryEtaConfig {

    @Bean
    public DeliveryEtaUseCase deliveryEtaUseCase(
            DeliveryOrderRepository orderRepository,
            DeliveryBatchRepository batchRepository,
            DeliveryZoneRepository zoneRepository,
            RiderEtaContextPort riderEtaContextPort,
            DeliveryLocationLookupPort locationLookupPort,
            LastMileRoutingPort routingPort,
            EtaCachePort cachePort,
            DeliveryEtaEventPublisherPort eventPublisherPort,
            DeliveryTenantContextPort tenantContextPort
    ) {
        return new DeliveryEtaService(
                orderRepository,
                batchRepository,
                zoneRepository,
                riderEtaContextPort,
                locationLookupPort,
                routingPort,
                cachePort,
                eventPublisherPort,
                tenantContextPort
        );
    }
}
