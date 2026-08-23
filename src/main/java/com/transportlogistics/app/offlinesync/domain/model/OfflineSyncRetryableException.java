package com.transportlogistics.app.offlinesync.domain.model;

public class OfflineSyncRetryableException extends RuntimeException {
    public OfflineSyncRetryableException(String message) {
        super(message);
    }

    public OfflineSyncRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
