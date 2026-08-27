package com.transportlogistics.app.freight.exception.domain;

/**
 * Lifecycle states for a CargoException (US-30 authoritative).
 */
public enum ExceptionStatus {
    OPEN,
    HELD,
    ESCALATED,
    RESOLVED,
    REJECTED
}
