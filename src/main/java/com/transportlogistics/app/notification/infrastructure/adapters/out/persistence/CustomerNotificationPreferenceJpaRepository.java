package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CustomerNotificationPreferenceJpaRepository
        extends JpaRepository<CustomerNotificationPreferenceEntity, UUID> {
    Optional<CustomerNotificationPreferenceEntity> findByCustomerId(UUID customerId);
}
