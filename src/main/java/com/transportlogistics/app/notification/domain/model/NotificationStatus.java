package com.transportlogistics.app.notification.domain.model;

public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    READ;

    public boolean canTransitionTo(NotificationStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == SENT || target == FAILED;
            case SENT -> target == READ;
            case FAILED, READ -> false;
        };
    }
}
