package com.transportlogistics.app.notification;

import java.util.UUID;

/** Module-root contract for customer operational notification preferences. */
public interface CustomerOperationalPreferenceManagement {
    PreferenceView get(UUID customerId);
    PreferenceView replace(UUID customerId, ReplaceCommand command);

    record ReplaceCommand(boolean emailEnabled, boolean smsEnabled, Long version) {}

    record PreferenceView(UUID customerId, boolean explicitProfile, boolean emailEnabled, boolean smsEnabled,
                          String maskedEmail, String maskedPhone, Long version) {}
}
