package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryContactAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryContactAttemptRepository {
    DeliveryContactAttempt save(DeliveryContactAttempt contactAttempt);

    List<DeliveryContactAttempt> saveAll(List<DeliveryContactAttempt> contactAttempts);

    List<DeliveryContactAttempt> findByDeliveryAttemptId(UUID deliveryAttemptId);

    Optional<DeliveryContactAttempt> findById(UUID id);
}
