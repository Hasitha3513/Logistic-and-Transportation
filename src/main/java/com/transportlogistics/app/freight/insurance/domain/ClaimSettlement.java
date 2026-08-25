package com.transportlogistics.app.freight.insurance.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record ClaimSettlement(
        UUID id,
        UUID claimId,
        String settlementReference,
        BigDecimal amount,
        String currency,
        String notes,
        String settledBy,
        OffsetDateTime settledAt
) {
    public ClaimSettlement {
        Objects.requireNonNull(id, "Settlement ID is required");
        Objects.requireNonNull(claimId, "Claim ID is required");
        if (settlementReference == null || settlementReference.isBlank()) {
            throw new IllegalArgumentException("Settlement reference is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Settlement amount must be greater than zero");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (settledBy == null || settledBy.isBlank()) {
            throw new IllegalArgumentException("Settled-by actor is required");
        }
        Objects.requireNonNull(settledAt, "Settled-at timestamp is required");
    }
}
