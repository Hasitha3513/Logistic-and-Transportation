package com.transportlogistics.app.tracking.adapters.inbound.traccar;

final class TraccarFailure extends RuntimeException {
    enum Kind { AUTHENTICATION, TRANSIENT, PERMANENT, MAPPING }

    private final Kind kind;
    private final String safeCode;

    TraccarFailure(Kind kind, String safeCode, Throwable cause) {
        super(safeCode, cause);
        this.kind = kind;
        this.safeCode = safeCode;
    }

    Kind kind() {
        return kind;
    }

    String safeCode() {
        return safeCode;
    }

    TraccarAdapterState.FailureCategory healthCategory() {
        return switch (kind) {
            case AUTHENTICATION -> TraccarAdapterState.FailureCategory.AUTHENTICATION;
            case TRANSIENT -> TraccarAdapterState.FailureCategory.PROVIDER;
            case MAPPING, PERMANENT -> TraccarAdapterState.FailureCategory.MAPPING;
        };
    }
}
