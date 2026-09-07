package com.transportlogistics.app.billing.ports.outbound;

import com.transportlogistics.app.billing.TransportBillingExportRequestedV1;
import java.util.Optional;
import java.util.UUID;

public interface BillingIntegrationPort {
    Optional<UUID> activeConfiguration(UUID tenantId);
    void publish(TransportBillingExportRequestedV1 event);
}
