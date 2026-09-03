package com.transportlogistics.app.notification.application.ports.out;

import com.transportlogistics.app.notification.domain.model.CustomerNotificationPreference;

import java.util.Optional;
import java.util.UUID;

public interface CustomerNotificationPreferenceRepository {
    Optional<CustomerNotificationPreference> findByCustomerId(UUID customerId);
    CustomerNotificationPreference save(CustomerNotificationPreference preference);
}
