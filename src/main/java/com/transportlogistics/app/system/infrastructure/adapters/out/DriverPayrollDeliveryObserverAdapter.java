package com.transportlogistics.app.system.infrastructure.adapters.out;

import com.transportlogistics.app.fleet.DriverPayrollDeliveryPort;
import com.transportlogistics.app.integration.IntegrationDeliveryObserver;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class DriverPayrollDeliveryObserverAdapter implements IntegrationDeliveryObserver {
    private final DriverPayrollDeliveryPort payroll;

    DriverPayrollDeliveryObserverAdapter(DriverPayrollDeliveryPort payroll) {
        this.payroll = payroll;
    }

    @Override
    public void delivered(UUID tenantId, UUID sourceEventId, String sourceEventType,
                          String payloadHash, String targetFilename, OffsetDateTime deliveredAt) {
        if ("DRIVER_PAYROLL_INPUT_V1".equals(sourceEventType)) {
            payroll.fileDelivered(tenantId, sourceEventId, payloadHash, targetFilename, deliveredAt);
        }
    }
}
