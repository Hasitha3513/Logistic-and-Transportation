package com.transportlogistics.app.billing.adapters.outbound.compliance;

import com.transportlogistics.app.billing.domain.TransportBillingRecord;
import com.transportlogistics.app.billing.ports.outbound.BillingCompliancePort;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class ConfiguredBillingComplianceAdapter implements BillingCompliancePort {
    private final boolean authorizedNotRequired;
    ConfiguredBillingComplianceAdapter(@Value("${app.billing.compliance-not-required:true}") boolean value) {
        authorizedNotRequired=value;
    }
    @Override public TransportBillingRecord.Compliance decision(UUID tenantId, TransportBillingRecord record) {
        return authorizedNotRequired ? TransportBillingRecord.Compliance.NOT_REQUIRED
            : TransportBillingRecord.Compliance.PENDING;
    }
}
