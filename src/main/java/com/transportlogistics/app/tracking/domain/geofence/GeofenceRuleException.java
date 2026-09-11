package com.transportlogistics.app.tracking.domain.geofence;

public final class GeofenceRuleException extends IllegalArgumentException {
    private final String code;

    public GeofenceRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
