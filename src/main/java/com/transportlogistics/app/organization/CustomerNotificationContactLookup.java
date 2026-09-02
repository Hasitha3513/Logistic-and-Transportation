package com.transportlogistics.app.organization;

import java.util.Optional;
import java.util.UUID;

/** Tenant-scoped customer contact projection for operational notifications. */
public interface CustomerNotificationContactLookup {
    Optional<CustomerNotificationContact> find(UUID customerId);

    record CustomerNotificationContact(UUID customerId, boolean active, String displayName,
                                       String phone, String email) {
    }
}
