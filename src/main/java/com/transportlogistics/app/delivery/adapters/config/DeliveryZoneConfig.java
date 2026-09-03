package com.transportlogistics.app.delivery.adapters.config;

import com.transportlogistics.app.delivery.application.DeliveryZoneService;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryZoneRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DeliveryZoneConfig {

    @Bean
    public DeliveryZoneService deliveryZoneService(
            DeliveryZoneRepository zoneRepository,
            DeliveryLocationLookupPort locationLookupPort,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            Clock clock
    ) {
        return new DeliveryZoneService(zoneRepository, locationLookupPort, tenantContext, transactions, clock);
    }
}
