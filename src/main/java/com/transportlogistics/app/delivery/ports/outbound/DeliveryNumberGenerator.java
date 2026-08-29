package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryNumber;

import java.time.Year;
import java.util.UUID;

public interface DeliveryNumberGenerator {
    DeliveryNumber next(UUID tenantId, Year tenantLocalYear);
}
