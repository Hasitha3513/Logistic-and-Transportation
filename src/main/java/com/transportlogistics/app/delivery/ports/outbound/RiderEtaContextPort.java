package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;

import java.util.Optional;
import java.util.UUID;

public interface RiderEtaContextPort {

    record RiderEtaContext(UUID riderId, DeliveryTransportMode transportMode) {}

    Optional<RiderEtaContext> findForRider(UUID tenantId, UUID riderId);

    Optional<RiderEtaContext> findForOrder(UUID tenantId, UUID deliveryOrderId);
}
