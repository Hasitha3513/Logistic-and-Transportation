package com.transportlogistics.app.tracking.domain.speed;

public final class SpeedMonitoringException extends RuntimeException {
    private final String code;

    public SpeedMonitoringException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
