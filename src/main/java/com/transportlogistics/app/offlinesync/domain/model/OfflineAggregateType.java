package com.transportlogistics.app.offlinesync.domain.model;

public enum OfflineAggregateType {
    VEHICLE,
    TRIP,
    DELIVERY;

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
