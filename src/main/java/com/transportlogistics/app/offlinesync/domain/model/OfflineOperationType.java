package com.transportlogistics.app.offlinesync.domain.model;

public enum OfflineOperationType {
    VEHICLE_READING_RECORD,
    TRIP_CHECKPOINT_RECORD,
    TRIP_DELAY_RECORD,
    TRIP_INCIDENT_RECORD;

    public static boolean supports(String value) {
        if (value == null) {
            return false;
        }
        try {
            valueOf(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
