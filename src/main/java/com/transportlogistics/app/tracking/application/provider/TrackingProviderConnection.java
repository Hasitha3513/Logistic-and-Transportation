package com.transportlogistics.app.tracking.application.provider;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TrackingProviderConnection(
        ProviderConnectionId id,
        UUID tenantId,
        String providerKeyId,
        String providerAlias,
        String credentialReference,
        ProviderType providerType,
        String displayName,
        URI endpoint,
        ProviderSafeConfiguration safeConfiguration,
        int pollIntervalSeconds,
        int pageSize,
        ProviderConnectionLifecycle lifecycle,
        ProviderConnectionTestStatus testStatus,
        Instant lastTestedAt,
        Instant lastSuccessfulPollAt,
        Instant lastProviderMessageAt,
        String lastErrorCategory,
        Instant nextPollAt,
        String leaseOwner,
        Instant leaseUntil,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        long version) {

    public TrackingProviderConnection {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        validateKey(providerKeyId, "providerKeyId", 160);
        validateAlias(providerAlias);
        validateKey(credentialReference, "credentialReference", 160);
        Objects.requireNonNull(providerType, "providerType");
        validateDisplayName(displayName);
        safeConfiguration = Objects.requireNonNullElseGet(
                safeConfiguration, ProviderSafeConfiguration::empty);
        new ProviderConnectionConfiguration(providerType, endpoint, safeConfiguration);
        if (pollIntervalSeconds < 5 || pollIntervalSeconds > 86_400) {
            throw new IllegalArgumentException("Poll interval must be between 5 and 86400 seconds");
        }
        if (pageSize < 1 || pageSize > 500) {
            throw new IllegalArgumentException("Page size must be between 1 and 500");
        }
        Objects.requireNonNull(lifecycle, "lifecycle");
        Objects.requireNonNull(testStatus, "testStatus");
        validateErrorCategory(lastErrorCategory);
        validateLease(leaseOwner, leaseUntil);
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(updatedAt, "updatedAt");
        Objects.requireNonNull(updatedBy, "updatedBy");
        if (version < 0) {
            throw new IllegalArgumentException("Version cannot be negative");
        }
    }

    static void validateAlias(String alias) {
        if (alias == null || !alias.matches("[A-Z][A-Z0-9_]{0,79}")) {
            throw new IllegalArgumentException("Provider alias is invalid");
        }
    }

    static void validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank() || displayName.length() > 120
                || !displayName.equals(displayName.trim())) {
            throw new IllegalArgumentException("Provider display name is invalid");
        }
    }

    static void validateErrorCategory(String category) {
        if (category != null && !category.matches("[A-Z][A-Z0-9_]{0,39}")) {
            throw new IllegalArgumentException("Provider error category is invalid");
        }
    }

    static void validateLease(String owner, Instant until) {
        if ((owner == null) != (until == null)
                || owner != null && (owner.isBlank() || owner.length() > 120)) {
            throw new IllegalArgumentException("Provider lease must be complete and bounded");
        }
    }

    static void validateKey(String value, String field, int maximumLength) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw new IllegalArgumentException(field + " is invalid");
        }
    }
}
