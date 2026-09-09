package com.transportlogistics.app.tracking.application.provider;

import java.util.Objects;

public record ConnectionTestResult(Status status, String detailCode) {
    public enum Status { PASS, AUTH_FAILED, UNREACHABLE, INVALID_CONFIGURATION }

    public ConnectionTestResult {
        Objects.requireNonNull(status, "status");
        if (detailCode != null && (detailCode.isBlank() || detailCode.length() > 80)) {
            throw new IllegalArgumentException("Connection test detail code is invalid");
        }
    }

    public static ConnectionTestResult passed() {
        return new ConnectionTestResult(Status.PASS, null);
    }
}
