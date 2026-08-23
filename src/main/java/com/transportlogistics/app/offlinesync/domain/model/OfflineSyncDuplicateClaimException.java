package com.transportlogistics.app.offlinesync.domain.model;

public class OfflineSyncDuplicateClaimException extends RuntimeException {
    public OfflineSyncDuplicateClaimException(Throwable cause) {
        super("Offline operation was claimed concurrently", cause);
    }
}
