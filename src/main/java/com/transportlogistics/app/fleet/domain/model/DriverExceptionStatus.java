package com.transportlogistics.app.fleet.domain.model;

public enum DriverExceptionStatus {
    SCHEDULED,
    ACTIVE,
    COMPLETED,
    CANCELLED;

    public boolean isBlocking() {
        return this == SCHEDULED || this == ACTIVE;
    }
}
