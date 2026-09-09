package com.transportlogistics.app.tracking.application.provider;

public enum ProviderConnectionLifecycle {
    DRAFT,
    ACTIVE,
    DISABLED,
    RETIRED;

    public boolean canTransitionTo(ProviderConnectionLifecycle target) {
        if (target == null || this == RETIRED) {
            return false;
        }
        return this == target || target == RETIRED
                || this == DRAFT && target == ACTIVE
                || this == ACTIVE && target == DISABLED
                || this == DISABLED && target == ACTIVE;
    }
}
