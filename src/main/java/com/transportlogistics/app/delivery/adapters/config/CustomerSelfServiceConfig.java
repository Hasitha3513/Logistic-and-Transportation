package com.transportlogistics.app.delivery.adapters.config;

import com.transportlogistics.app.delivery.application.CustomerSelfServiceService;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryEtaUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.notification.CustomerOperationalPreferenceManagement;
import com.transportlogistics.app.organization.CustomerNotificationContactLookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class CustomerSelfServiceConfig {
    @Bean
    CustomerSelfServiceService customerSelfServiceService(DeliverySelfServiceAccessRepository access,
            DeliveryCustomerSubmissionRepository submissions, DeliveryOrderRepository orders,
            DeliveryAttemptRepository attempts, DeliveryBatchRepository batches, ProofOfDeliveryRepository proofs,
            DeliveryLocationLookupPort locations, DeliveryEtaUseCase eta, CustomerNotificationContactLookup customers,
            CustomerOperationalPreferenceManagement preferences, DeliveryTenantContextPort tenant,
            SelfServiceTenantExecutor tenantExecutor, DeliveryOrderTransaction transactions, Clock clock,
            @Value("${app.delivery.self-service.contact-hmac-secret}") String secret,
            @Value("${app.delivery.self-service.contact-hash-key-version}") String keyVersion,
            @Value("${app.delivery.self-service.customer-origin}") String customerOrigin) {
        return new CustomerSelfServiceService(access, submissions, orders, attempts, batches, proofs, locations,
                eta, customers, preferences, tenant, tenantExecutor, transactions, clock, secret, keyVersion, customerOrigin);
    }

}
