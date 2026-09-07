package com.transportlogistics.app.billing.ports.outbound;

import com.transportlogistics.app.billing.domain.TransportBillingRecord;
import java.util.UUID;

public interface BillingCompliancePort {
    TransportBillingRecord.Compliance decision(UUID tenantId, TransportBillingRecord record);
}
