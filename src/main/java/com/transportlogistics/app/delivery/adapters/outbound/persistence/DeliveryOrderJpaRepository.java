package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

interface DeliveryOrderJpaRepository extends JpaRepository<DeliveryOrderEntity, UUID>, JpaSpecificationExecutor<DeliveryOrderEntity> {
    Optional<DeliveryOrderEntity> findByDeliveryNumber(String deliveryNumber);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM DeliveryOrderEntity o WHERE o.id = :id")
    Optional<DeliveryOrderEntity> findByIdWithLock(@org.springframework.data.repository.query.Param("id") UUID id);
}
