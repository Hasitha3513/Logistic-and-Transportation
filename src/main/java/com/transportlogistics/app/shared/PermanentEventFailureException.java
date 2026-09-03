package com.transportlogistics.app.shared;

/** Signals a deterministic event defect that must be parked without retry. */
public final class PermanentEventFailureException extends RuntimeException {
    private final String errorCode;

    public PermanentEventFailureException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
