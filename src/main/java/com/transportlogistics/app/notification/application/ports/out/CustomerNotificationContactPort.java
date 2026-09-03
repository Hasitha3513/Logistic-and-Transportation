package com.transportlogistics.app.notification.application.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface CustomerNotificationContactPort {
    Optional<CustomerContact> find(UUID customerId);

    record CustomerContact(UUID customerId, boolean active, String displayName, String phone, String email) {
    }
}
