package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import com.transportlogistics.app.tracking.application.provider.ProviderPollingFailure;

final class FlespiFailure extends RuntimeException implements ProviderPollingFailure {
    enum Kind { AUTHENTICATION, TRANSIENT, PERMANENT, MAPPING, DOWNSTREAM }
    private final Kind kind;
    private final String safeCode;

    FlespiFailure(Kind kind, String safeCode, Throwable cause) {
        super(safeCode, cause);
        this.kind = kind;
        this.safeCode = safeCode;
    }

    Kind kind() { return kind; }
    @Override
    public String safeCode() { return safeCode; }

    FlespiAdapterState.FailureCategory healthCategory() {
        return switch (kind) {
            case AUTHENTICATION -> FlespiAdapterState.FailureCategory.AUTHENTICATION;
            case TRANSIENT -> FlespiAdapterState.FailureCategory.PROVIDER;
            case MAPPING, PERMANENT -> FlespiAdapterState.FailureCategory.MAPPING;
            case DOWNSTREAM -> FlespiAdapterState.FailureCategory.DOWNSTREAM;
        };
    }
}
