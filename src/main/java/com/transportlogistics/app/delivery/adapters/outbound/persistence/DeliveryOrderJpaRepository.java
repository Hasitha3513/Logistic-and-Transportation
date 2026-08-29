package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

interface DeliveryOrderJpaRepository extends JpaRepository<DeliveryOrderEntity, UUID>, JpaSpecificationExecutor<DeliveryOrderEntity> {
    Optional<DeliveryOrderEntity> findByDeliveryNumber(String deliveryNumber);
}
