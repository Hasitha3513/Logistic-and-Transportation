package com.transportlogistics.app.tracking.domain.routedeviation;

public final class RouteDeviationException extends RuntimeException {
    private final String code;

    public RouteDeviationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
