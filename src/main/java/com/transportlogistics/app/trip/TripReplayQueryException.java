package com.transportlogistics.app.trip;

public final class TripReplayQueryException extends RuntimeException {
    public static final String RESULT_LIMIT_EXCEEDED = "TRIP_ASSIGNMENT_RESULT_LIMIT_EXCEEDED";

    private final String code;

    public TripReplayQueryException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
