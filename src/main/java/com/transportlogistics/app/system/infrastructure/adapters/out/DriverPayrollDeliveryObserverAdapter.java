package com.transportlogistics.app.system.infrastructure.adapters.out;

import com.transportlogistics.app.fleet.DriverPayrollDeliveryPort;
import com.transportlogistics.app.billing.BillingDeliveryPort;
import com.transportlogistics.app.integration.IntegrationDeliveryObserver;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class DriverPayrollDeliveryObserverAdapter implements IntegrationDeliveryObserver {
    private final DriverPayrollDeliveryPort payroll;
    private final BillingDeliveryPort billing;

    DriverPayrollDeliveryObserverAdapter(DriverPayrollDeliveryPort payroll, BillingDeliveryPort billing) {
        this.payroll = payroll;
        this.billing = billing;
    }

    @Override
    public void delivered(UUID tenantId, UUID sourceEventId, String sourceEventType,
                          String payloadHash, String targetFilename, OffsetDateTime deliveredAt) {
        if ("DRIVER_PAYROLL_INPUT_V1".equals(sourceEventType)) {
            payroll.fileDelivered(tenantId, sourceEventId, payloadHash, targetFilename, deliveredAt);
        }
        if ("TRANSPORT_BILLING_V1".equals(sourceEventType)) {
            billing.fileDelivered(tenantId, sourceEventId, payloadHash, targetFilename, deliveredAt);
        }
    }
}
