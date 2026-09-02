package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response;

import java.util.UUID;

public record CustomerNotificationPreferenceResponse(
        UUID customerId,
        boolean explicitProfile,
        boolean emailEnabled,
        boolean smsEnabled,
        String maskedEmail,
        String maskedPhone,
        Long version
) {
}
