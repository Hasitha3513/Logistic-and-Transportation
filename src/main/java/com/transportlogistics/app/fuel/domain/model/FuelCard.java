package com.transportlogistics.app.fuel.domain.model;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public record FuelCard(UUID id, UUID tenantId, UUID providerId, String alias, String providerCardReference,
                       String maskedIdentifier, String lastFour, int expiryMonth, int expiryYear,
                       Status status, long version, UUID createdBy, OffsetDateTime createdAt,
                       OffsetDateTime updatedAt) {
    public enum Status { DRAFT, ACTIVE, SUSPENDED, BLOCKED, EXPIRED, CANCELLED }

    public FuelCard {
        Objects.requireNonNull(id); Objects.requireNonNull(tenantId); Objects.requireNonNull(providerId);
        Objects.requireNonNull(alias); Objects.requireNonNull(providerCardReference);
        Objects.requireNonNull(maskedIdentifier); Objects.requireNonNull(status);
        if (alias.isBlank() || providerCardReference.isBlank() || maskedIdentifier.isBlank()) {
            throw new IllegalArgumentException("Card alias and identifiers are required");
        }
        if (lastFour != null && !lastFour.matches("[0-9]{4}")) throw new IllegalArgumentException("Invalid last four");
        YearMonth.of(expiryYear, expiryMonth);
    }

    public Status effectiveStatus(OffsetDateTime now, ZoneId tenantZone) {
        if (status != Status.CANCELLED
                && YearMonth.of(expiryYear, expiryMonth).atEndOfMonth().atTime(23, 59, 59, 999_999_999)
                .atZone(tenantZone).toOffsetDateTime().isBefore(now)) return Status.EXPIRED;
        return status;
    }

    public FuelCard transition(Status target, OffsetDateTime now, ZoneId zone) {
        Status current = effectiveStatus(now, zone);
        boolean allowed = switch (current) {
            case DRAFT -> target == Status.ACTIVE || target == Status.CANCELLED;
            case ACTIVE -> target == Status.SUSPENDED || target == Status.BLOCKED;
            case SUSPENDED -> target == Status.ACTIVE || target == Status.BLOCKED || target == Status.CANCELLED;
            case BLOCKED -> target == Status.CANCELLED;
            case EXPIRED, CANCELLED -> false;
        };
        if (!allowed) throw new IllegalStateException(current == Status.EXPIRED
                ? "FUEL_CARD_EXPIRED" : "FUEL_CARD_INVALID_STATE");
        return new FuelCard(id, tenantId, providerId, alias, providerCardReference, maskedIdentifier, lastFour,
                expiryMonth, expiryYear, target, version + 1, createdBy, createdAt, now);
    }
}
