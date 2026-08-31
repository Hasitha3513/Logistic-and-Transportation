package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAttemptRepository {
    DeliveryAttempt save(DeliveryAttempt attempt);

    Optional<DeliveryAttempt> findById(UUID id);

    List<DeliveryAttempt> findByDeliveryId(UUID deliveryId);

    int countByDeliveryId(UUID deliveryId);

    Optional<DeliveryAttempt> findLatestByDeliveryId(UUID deliveryId);
}
