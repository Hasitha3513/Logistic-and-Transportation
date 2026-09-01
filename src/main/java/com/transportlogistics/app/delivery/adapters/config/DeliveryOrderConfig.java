package com.transportlogistics.app.delivery.adapters.config;

import com.transportlogistics.app.delivery.application.DeliveryExceptionService;
import com.transportlogistics.app.delivery.application.DeliveryOrderService;
import com.transportlogistics.app.delivery.application.FailedDeliveryService;
import com.transportlogistics.app.delivery.application.ProofOfDeliveryService;
import com.transportlogistics.app.delivery.application.RedeliveryService;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryAnalyticsUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryExceptionUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.delivery.ports.inbound.FailedDeliveryUseCase;
import com.transportlogistics.app.delivery.ports.inbound.RedeliveryUseCase;
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
                                              DeliveryOrderEventPublisherPort eventPublisher, Clock clock) {
        return new DeliveryOrderService(orders, numbers, customers, locations, tenantContext, transactions, eventPublisher, clock);
    }
    @Bean
    ProofOfDeliveryService proofOfDeliveryUseCase(
            DeliveryOrderRepository orders, ProofOfDeliveryRepository proofs, DeliveryEvidenceStoragePort storage,
            DeliveryTenantContextPort tenantContext, DeliveryExceptionRepository exceptions,
            DeliveryOrderTransaction transactions, Clock clock) {
        return new ProofOfDeliveryService(orders, proofs, storage, tenantContext, exceptions, transactions, clock);
    }

    @Bean
    FailedDeliveryUseCase failedDeliveryUseCase(
            DeliveryOrderRepository orders,
            ProofOfDeliveryRepository proofs,
            DeliveryAttemptRepository attempts,
            DeliveryContactAttemptRepository contactAttempts,
            DeliveryEscalationRepository escalations,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            Clock clock) {
        return new FailedDeliveryService(orders, proofs, attempts, contactAttempts, escalations, tenantContext, transactions, clock);
    }

    @Bean
    RedeliveryUseCase redeliveryUseCase(
            DeliveryOrderRepository orders,
            ProofOfDeliveryRepository proofs,
            DeliveryAttemptRepository attempts,
            DeliveryRedeliveryScheduleRepository schedules,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            Clock clock) {
        return new RedeliveryService(
                orders, proofs, attempts, schedules, tenantContext, transactions, clock
        );
    }

    @Bean
    DeliveryAnalyticsUseCase deliveryAnalyticsUseCase(
            DeliveryAnalyticsPersistencePort analyticsPersistencePort,
            DeliveryTenantContextPort tenantContextPort,
            DeliveryLocationLookupPort locationLookupPort) {
        return new com.transportlogistics.app.delivery.application.DeliveryAnalyticsService(
                analyticsPersistencePort, tenantContextPort, locationLookupPort
        );
    }

    @Bean
    DeliveryExceptionUseCase deliveryExceptionUseCase(
            DeliveryOrderRepository orders,
            DeliveryAttemptRepository attempts,
            DeliveryExceptionRepository exceptions,
            DeliveryEvidenceStoragePort storage,
            DeliveryLocationLookupPort locations,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            Clock clock) {
        return new DeliveryExceptionService(
                orders, attempts, exceptions, storage, locations, tenantContext, transactions, clock
        );
    }
}


