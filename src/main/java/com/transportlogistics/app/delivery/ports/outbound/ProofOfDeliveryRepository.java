package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.ProofOfDelivery;
import java.util.Optional;
import java.util.UUID;

public interface ProofOfDeliveryRepository {
    ProofOfDelivery save(ProofOfDelivery proof);
    Optional<ProofOfDelivery> findByDeliveryOrderId(UUID deliveryOrderId);
}
