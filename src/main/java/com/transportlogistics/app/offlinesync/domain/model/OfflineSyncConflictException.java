package com.transportlogistics.app.offlinesync.domain.model;

public class OfflineSyncConflictException extends RuntimeException {
    public OfflineSyncConflictException(String message) {
        super(message);
    }
}
