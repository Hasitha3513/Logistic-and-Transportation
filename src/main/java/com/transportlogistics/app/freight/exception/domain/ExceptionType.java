package com.transportlogistics.app.freight.exception.domain;

/**
 * The six source-defined categories of cargo exception (US-30 authoritative).
 */
public enum ExceptionType {
    DAMAGE,
    PARTIAL_SHIPMENT,
    WEIGHT_DISCREPANCY,
    HAZARDOUS_MATERIAL,
    UNMANIFESTED_CARGO,
    SEAL_TAMPERING
}
