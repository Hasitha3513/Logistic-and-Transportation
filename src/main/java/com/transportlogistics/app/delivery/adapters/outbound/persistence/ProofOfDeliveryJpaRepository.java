package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

interface ProofOfDeliveryJpaRepository extends JpaRepository<ProofOfDeliveryEntity, UUID> {
    Optional<ProofOfDeliveryEntity> findByDeliveryOrderId(UUID deliveryOrderId);
}
