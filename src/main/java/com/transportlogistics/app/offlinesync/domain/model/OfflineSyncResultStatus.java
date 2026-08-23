package com.transportlogistics.app.offlinesync.domain.model;

public enum OfflineSyncResultStatus {
    APPLIED,
    ALREADY_APPLIED,
    REJECTED,
    CONFLICT,
    RETRYABLE_ERROR;

    public boolean stored() {
        return this == APPLIED || this == REJECTED || this == CONFLICT;
    }
}
