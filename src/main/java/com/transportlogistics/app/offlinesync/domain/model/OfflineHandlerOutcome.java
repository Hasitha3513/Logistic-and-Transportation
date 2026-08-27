package com.transportlogistics.app.offlinesync.domain.model;

public record OfflineHandlerOutcome(OfflineSyncResultStatus status, String errorCode, String message) {
    public OfflineHandlerOutcome {
        if (status != OfflineSyncResultStatus.APPLIED
                && status != OfflineSyncResultStatus.REJECTED
                && status != OfflineSyncResultStatus.CONFLICT) {
            throw new IllegalArgumentException("Handler outcomes must be APPLIED, REJECTED, or CONFLICT");
        }
    }

    public static OfflineHandlerOutcome applied() {
        return new OfflineHandlerOutcome(OfflineSyncResultStatus.APPLIED, null, null);
    }

    public static OfflineHandlerOutcome rejected(String code, String message) {
        return new OfflineHandlerOutcome(OfflineSyncResultStatus.REJECTED, code, message);
    }

    public static OfflineHandlerOutcome conflict(String code, String message) {
        return new OfflineHandlerOutcome(OfflineSyncResultStatus.CONFLICT, code, message);
    }
}
