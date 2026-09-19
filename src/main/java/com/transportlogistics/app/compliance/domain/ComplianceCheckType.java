package com.transportlogistics.app.compliance.domain;

/** Structural US-72 check identifiers. They do not activate or define policy. */
public enum ComplianceCheckType {
    VEHICLE_DOCUMENT_ELIGIBILITY,
    DRIVER_ELIGIBILITY,
    CARGO_DOCUMENT_ELIGIBILITY,
    HAZMAT_ELIGIBILITY,
    BILLING_TAX_FACT_ELIGIBILITY,
    REGIONAL_OPERATION_ELIGIBILITY,
    RETENTION_DISPOSITION_ELIGIBILITY
}
