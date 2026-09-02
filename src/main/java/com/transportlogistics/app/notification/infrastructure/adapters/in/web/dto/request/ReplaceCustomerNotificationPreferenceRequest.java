package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReplaceCustomerNotificationPreferenceRequest(
        @NotNull Boolean emailEnabled,
        @NotNull Boolean smsEnabled,
        Long version
) {
}
