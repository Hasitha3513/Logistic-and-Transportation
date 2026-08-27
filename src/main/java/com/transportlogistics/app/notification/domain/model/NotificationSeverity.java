package com.transportlogistics.app.notification.domain.model;

public enum NotificationSeverity {
    INFO,
    WARNING,
    CRITICAL;

    public boolean meetsThreshold(NotificationSeverity threshold) {
        if (threshold == null) {
            return true;
        }
        return this.ordinal() >= threshold.ordinal();
    }
}
