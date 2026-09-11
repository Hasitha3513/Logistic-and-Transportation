package com.transportlogistics.app.tracking.application.provider;

public enum DeviceProviderBindingLifecycle {
    DRAFT,
    ACTIVE,
    DISABLED,
    RETIRED;

    public boolean canTransitionTo(DeviceProviderBindingLifecycle target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case DRAFT, DISABLED -> target == ACTIVE || target == RETIRED;
            case ACTIVE -> target == DISABLED || target == RETIRED;
            case RETIRED -> false;
        };
    }
}
