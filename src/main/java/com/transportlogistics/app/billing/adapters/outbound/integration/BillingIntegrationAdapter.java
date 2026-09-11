package com.transportlogistics.app.billing.adapters.outbound.integration;

import com.transportlogistics.app.billing.TransportBillingExportRequestedV1;
import com.transportlogistics.app.billing.ports.outbound.BillingIntegrationPort;
import com.transportlogistics.app.integration.BillingIntegrationConfigurationLookup;
import com.transportlogistics.app.shared.DurableEventPublisher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class BillingIntegrationAdapter implements BillingIntegrationPort {
    private final BillingIntegrationConfigurationLookup configurations;
    private final DurableEventPublisher events;
    BillingIntegrationAdapter(BillingIntegrationConfigurationLookup configurations, DurableEventPublisher events) {
        this.configurations=configurations; this.events=events;
    }
    @Override public Optional<UUID> activeConfiguration(UUID tenantId){return configurations.activeBillingConfiguration(tenantId);}
    @Override public void publish(TransportBillingExportRequestedV1 event){events.publish(event);}
}
