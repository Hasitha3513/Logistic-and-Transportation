package com.transportlogistics.app.tracking.adapters.inbound.flespi;

final class FlespiFailure extends RuntimeException {
    enum Kind { AUTHENTICATION, TRANSIENT, PERMANENT, MAPPING, DOWNSTREAM }
    private final Kind kind;
    private final String safeCode;

    FlespiFailure(Kind kind, String safeCode, Throwable cause) {
        super(safeCode, cause);
        this.kind = kind;
        this.safeCode = safeCode;
    }

    Kind kind() { return kind; }
    String safeCode() { return safeCode; }
}
