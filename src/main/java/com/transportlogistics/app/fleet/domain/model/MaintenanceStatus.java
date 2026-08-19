package com.transportlogistics.app.fleet.domain.model;

public enum MaintenanceStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean isBlocking() {
        return this == SCHEDULED || this == IN_PROGRESS;
    }
}
