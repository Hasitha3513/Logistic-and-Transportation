package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryEscalation;
import com.transportlogistics.app.delivery.domain.model.DeliveryEscalationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryEscalationRepository {
    DeliveryEscalation save(DeliveryEscalation escalation);

    Optional<DeliveryEscalation> findById(UUID id);

    List<DeliveryEscalation> findByDeliveryId(UUID deliveryId);

    Optional<DeliveryEscalation> findLatestByDeliveryId(UUID deliveryId);

    List<DeliveryEscalation> findByStatus(DeliveryEscalationStatus status);
}
