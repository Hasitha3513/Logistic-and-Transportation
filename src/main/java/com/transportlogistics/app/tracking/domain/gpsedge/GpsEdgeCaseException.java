package com.transportlogistics.app.tracking.domain.gpsedge;

public final class GpsEdgeCaseException extends RuntimeException {
    private final String code;

    public GpsEdgeCaseException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
