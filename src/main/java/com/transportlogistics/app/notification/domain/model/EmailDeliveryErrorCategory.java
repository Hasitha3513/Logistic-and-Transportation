package com.transportlogistics.app.notification.domain.model;

public enum EmailDeliveryErrorCategory {
    CONNECTION(true),
    TIMEOUT(true),
    INTERRUPTION(true),
    HTTP_408(true),
    HTTP_429(true),
    THROTTLING(true),
    PROVIDER_5XX(true),
    INVALID_RECIPIENT(false),
    AUTHENTICATION(false),
    CONFIGURATION(false),
    TEMPLATE_VALIDATION(false),
    RECIPIENT_VALIDATION(false),
    PROVIDER_4XX(false);

    private final boolean retryable;

    EmailDeliveryErrorCategory(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
