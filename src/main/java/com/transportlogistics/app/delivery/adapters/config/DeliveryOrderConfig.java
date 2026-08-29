package com.transportlogistics.app.delivery.adapters.config;

import com.transportlogistics.app.delivery.application.DeliveryOrderService;
import com.transportlogistics.app.delivery.application.ProofOfDeliveryService;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class DeliveryOrderConfig {
    @Bean
    DeliveryOrderUseCase deliveryOrderUseCase(DeliveryOrderRepository orders, DeliveryNumberGenerator numbers,
                                              DeliveryCustomerLookupPort customers, DeliveryLocationLookupPort locations,
                                              DeliveryTenantContextPort tenantContext, DeliveryOrderTransaction transactions,
                                              Clock clock) {
        return new DeliveryOrderService(orders, numbers, customers, locations, tenantContext, transactions, clock);
    }
    @Bean
    com.transportlogistics.app.delivery.ports.inbound.ProofOfDeliveryUseCase proofOfDeliveryUseCase(
            DeliveryOrderRepository orders, ProofOfDeliveryRepository proofs, DeliveryEvidenceStoragePort storage,
            DeliveryTenantContextPort tenantContext, DeliveryOrderTransaction transactions, Clock clock) {
        return new ProofOfDeliveryService(orders, proofs, storage, tenantContext, transactions, clock);
    }
}
