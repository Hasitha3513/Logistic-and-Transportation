package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.application.ports.out.CustomerNotificationPreferenceRepository;
import com.transportlogistics.app.notification.domain.model.CustomerNotificationPreference;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CustomerNotificationPreferencePersistenceAdapter implements CustomerNotificationPreferenceRepository {
    private final CustomerNotificationPreferenceJpaRepository repository;

    public CustomerNotificationPreferencePersistenceAdapter(CustomerNotificationPreferenceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CustomerNotificationPreference> findByCustomerId(UUID customerId) {
        return repository.findByCustomerId(customerId).map(CustomerNotificationPreferenceEntity::toDomain);
    }

    @Override
    public CustomerNotificationPreference save(CustomerNotificationPreference preference) {
        return repository.saveAndFlush(CustomerNotificationPreferenceEntity.fromDomain(preference)).toDomain();
    }
}
