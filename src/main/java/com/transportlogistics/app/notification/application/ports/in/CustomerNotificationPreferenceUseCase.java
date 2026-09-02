package com.transportlogistics.app.notification.application.ports.in;

import java.util.UUID;

public interface CustomerNotificationPreferenceUseCase {
    PreferenceView get(UUID customerId);
    PreferenceView replace(UUID customerId, ReplaceCommand command);

    record ReplaceCommand(boolean emailEnabled, boolean smsEnabled, Long version) {
    }

    record PreferenceView(UUID customerId, boolean explicitProfile, boolean emailEnabled, boolean smsEnabled,
                          String maskedEmail, String maskedPhone, Long version) {
    }
}
