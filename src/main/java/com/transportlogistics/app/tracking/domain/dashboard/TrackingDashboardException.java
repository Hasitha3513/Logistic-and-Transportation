package com.transportlogistics.app.tracking.domain.dashboard;

public final class TrackingDashboardException extends RuntimeException {
    private final String code;

    public TrackingDashboardException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
