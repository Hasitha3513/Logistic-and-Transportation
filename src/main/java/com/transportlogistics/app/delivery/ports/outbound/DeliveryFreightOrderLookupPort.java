package com.transportlogistics.app.delivery.ports.outbound;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryFreightOrderLookupPort {
    Optional<FreightOrderDeliveryReference> findFreightOrder(UUID freightOrderId);

    record FreightOrderDeliveryReference(UUID freightOrderId, String orderNumber, UUID customerId,
                                         UUID originLocationId, UUID destinationLocationId,
                                         OffsetDateTime requestedDeliveryAt) {}
}
