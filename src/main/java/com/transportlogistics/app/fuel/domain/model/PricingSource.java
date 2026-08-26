package com.transportlogistics.app.fuel.domain.model;

/**
 * Source of the pricing used for a fuel line.
 */
public enum PricingSource {
    /**
     * Price taken from the original FuelIssue (snapshot at issuance).
     */
    ISSUE_PRICE,
    /**
     * Price taken from the effective fuel price catalogue at the time of calculation.
     */
    EFFECTIVE_PRICE,
    /**
     * No price available.
     */
    UNPRICED
}
