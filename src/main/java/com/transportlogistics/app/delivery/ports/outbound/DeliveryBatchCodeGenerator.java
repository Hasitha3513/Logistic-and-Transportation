package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryBatchCode;

import java.time.Year;
import java.util.UUID;

public interface DeliveryBatchCodeGenerator {
    DeliveryBatchCode next(UUID tenantId, Year year);
}
