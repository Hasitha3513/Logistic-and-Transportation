package com.transportlogistics.app.integration.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record IntegrationConfiguration(
        UUID id,
        UUID tenantId,
        String name,
        String normalizedName,
        Type type,
        Protocol protocol,
        Direction direction,
        String endpointAlias,
        String credentialReference,
        UUID currentMappingId,
        DataClassification dataClassification,
        RetryPolicy retryPolicy,
        Lifecycle lifecycle,
        Health health,
        OffsetDateTime lastTestedAt,
        Long lastTestedVersion,
        OffsetDateTime lastSuccessfulExchangeAt,
        long version,
        OffsetDateTime createdAt,
        String createdBy,
        OffsetDateTime updatedAt,
        String updatedBy
) {
    public static final Duration TEST_FRESHNESS = Duration.ofMinutes(15);
    private static final Pattern CREDENTIAL_REFERENCE = Pattern.compile("env:[A-Z][A-Z0-9_]{0,126}");

    public IntegrationConfiguration {
        Objects.requireNonNull(id, "Integration configuration id is required");
        Objects.requireNonNull(tenantId, "Tenant is required");
        name = required(name, "Integration name is required", 160);
        normalizedName = required(normalizedName, "Normalized integration name is required", 160);
        if (!normalizedName.equals(normalizeName(name))) {
            throw invalid("Normalized integration name does not match the integration name");
        }
        Objects.requireNonNull(type, "Integration type is required");
        Objects.requireNonNull(protocol, "Integration protocol is required");
        Objects.requireNonNull(direction, "Integration direction is required");
        endpointAlias = required(endpointAlias, "Endpoint alias is required", 80);
        credentialReference = optional(credentialReference, 160);
        if (!"CONTROLLED_SANDBOX".equals(endpointAlias)) {
            throw invalid("Endpoint alias is not allow-listed");
        }
        if (credentialReference != null && !CREDENTIAL_REFERENCE.matcher(credentialReference).matches()) {
            throw invalid("Credential reference is invalid");
        }
        Objects.requireNonNull(dataClassification, "Data classification is required");
        Objects.requireNonNull(retryPolicy, "Retry policy is required");
        Objects.requireNonNull(lifecycle, "Lifecycle is required");
        Objects.requireNonNull(health, "Health is required");
        Objects.requireNonNull(createdAt, "Creation time is required");
        createdBy = required(createdBy, "Creation actor is required", 255);
        Objects.requireNonNull(updatedAt, "Update time is required");
        updatedBy = required(updatedBy, "Update actor is required", 255);
        validateCapability(type, protocol, direction, dataClassification, retryPolicy);
    }

    public static IntegrationConfiguration draft(UUID tenantId, String name, Type type, Protocol protocol,
                                                   Direction direction, String endpointAlias,
                                                   String credentialReference, DataClassification classification,
                                                   OffsetDateTime now, String actor) {
        return new IntegrationConfiguration(UUID.randomUUID(), tenantId, name, normalizeName(name), type, protocol,
            direction, endpointAlias, credentialReference, null, classification, RetryPolicy.US73_BOUNDED_V1,
            Lifecycle.DRAFT, Health.UNKNOWN, null, null, null, 0, now, actor, now, actor);
    }

    public IntegrationConfiguration withMapping(UUID mappingId, OffsetDateTime now, String actor) {
        ensureEditable();
        return copy(name, endpointAlias, credentialReference, mappingId, Health.UNKNOWN, null, null,
            lastSuccessfulExchangeAt, now, actor);
    }

    public IntegrationConfiguration update(String nextName, String nextAlias, String nextCredentialReference,
                                           UUID nextMappingId, OffsetDateTime now, String actor) {
        ensureEditable();
        return copy(nextName, nextAlias, nextCredentialReference, nextMappingId, Health.UNKNOWN, null, null,
            lastSuccessfulExchangeAt, now, actor);
    }

    public IntegrationConfiguration tested(OffsetDateTime now, boolean success, String actor) {
        Long testedVersion = success ? Long.valueOf(version + 1) : lastTestedVersion;
        return copy(name, endpointAlias, credentialReference, currentMappingId,
            success ? Health.HEALTHY : Health.UNAVAILABLE, success ? now : lastTestedAt,
            testedVersion, lastSuccessfulExchangeAt, now, actor);
    }

    public IntegrationConfiguration activate(OffsetDateTime now, String actor) {
        if (lifecycle == Lifecycle.ACTIVE) {
            return this;
        }
        if (currentMappingId == null || lastTestedAt == null || lastTestedVersion == null
                || lastTestedVersion != version || lastTestedAt.isBefore(now.minus(TEST_FRESHNESS))) {
            throw invalid("A successful test for the current version within 15 minutes is required");
        }
        return new IntegrationConfiguration(id, tenantId, name, normalizedName, type, protocol, direction,
            endpointAlias, credentialReference, currentMappingId, dataClassification, retryPolicy, Lifecycle.ACTIVE,
            health, lastTestedAt, lastTestedVersion, lastSuccessfulExchangeAt, version, createdAt, createdBy, now, actor);
    }

    public IntegrationConfiguration disable(OffsetDateTime now, String actor) {
        if (lifecycle != Lifecycle.ACTIVE) {
            throw new BusinessRuleException("INTEGRATION_DISABLED", "Only an active integration can be disabled");
        }
        return new IntegrationConfiguration(id, tenantId, name, normalizedName, type, protocol, direction,
            endpointAlias, credentialReference, currentMappingId, dataClassification, retryPolicy,
            Lifecycle.DISABLED, health, lastTestedAt, lastTestedVersion, lastSuccessfulExchangeAt, version,
            createdAt, createdBy, now, actor);
    }

    public IntegrationConfiguration exchangeSucceeded(OffsetDateTime now) {
        return new IntegrationConfiguration(id, tenantId, name, normalizedName, type, protocol, direction,
            endpointAlias, credentialReference, currentMappingId, dataClassification, retryPolicy, lifecycle,
            Health.HEALTHY, lastTestedAt, lastTestedVersion, now, version, createdAt, createdBy, now, updatedBy);
    }

    public IntegrationConfiguration exchangeFailed(boolean retryable, boolean authenticationFailure,
                                                   OffsetDateTime now) {
        Health nextHealth = authenticationFailure ? Health.AUTH_FAILED
            : retryable ? Health.DEGRADED : Health.UNAVAILABLE;
        return new IntegrationConfiguration(id, tenantId, name, normalizedName, type, protocol, direction,
            endpointAlias, credentialReference, currentMappingId, dataClassification, retryPolicy, lifecycle,
            nextHealth, lastTestedAt, lastTestedVersion, lastSuccessfulExchangeAt, version, createdAt, createdBy,
            now, updatedBy);
    }

    private IntegrationConfiguration copy(String nextName, String nextAlias, String nextCredentialReference,
                                          UUID nextMappingId, Health nextHealth, OffsetDateTime testedAt,
                                          Long testedVersion, OffsetDateTime lastSuccess, OffsetDateTime now,
                                          String actor) {
        return new IntegrationConfiguration(id, tenantId, nextName, normalizeName(nextName), type, protocol,
            direction, nextAlias, nextCredentialReference, nextMappingId, dataClassification, retryPolicy,
            lifecycle, nextHealth, testedAt, testedVersion, lastSuccess, version, createdAt, createdBy, now, actor);
    }

    private void ensureEditable() {
        if (lifecycle == Lifecycle.ACTIVE) {
            throw invalid("Active integration configuration is immutable; disable it first");
        }
    }

    public static String normalizeName(String value) {
        return required(value, "Integration name is required", 160).trim().replaceAll("\\s+", " ")
            .toUpperCase(Locale.ROOT);
    }

    public static void validateCapability(Type type, Protocol protocol, Direction direction,
                                          DataClassification classification, RetryPolicy retryPolicy) {
        if (type != Type.FILE_EXCHANGE || protocol != Protocol.FILE_JSON_V1 || direction != Direction.OUTBOUND
                || classification != DataClassification.INTERNAL_OPERATIONAL_NON_SENSITIVE
                || retryPolicy != RetryPolicy.US73_BOUNDED_V1) {
            throw new BusinessRuleException("INTEGRATION_CAPABILITY_UNSUPPORTED",
                "Unsupported US-73 integration capability");
        }
    }

    private static String required(String value, String message, int max) {
        if (value == null || value.trim().isEmpty()) throw invalid(message);
        String clean = value.trim();
        if (clean.length() > max) throw invalid(message);
        return clean;
    }

    private static String optional(String value, int max) {
        if (value == null || value.trim().isEmpty()) return null;
        String clean = value.trim();
        if (clean.length() > max) throw invalid("Value is too long");
        return clean;
    }

    private static BusinessRuleException invalid(String message) {
        return new BusinessRuleException("INTEGRATION_CONFIGURATION_INVALID", message);
    }

    public enum Type { FILE_EXCHANGE }
    public enum Protocol { FILE_JSON_V1 }
    public enum Direction { OUTBOUND, INBOUND, BIDIRECTIONAL }
    public enum DataClassification { INTERNAL_OPERATIONAL_NON_SENSITIVE, FINANCIAL, RESTRICTED }
    public enum RetryPolicy { US73_BOUNDED_V1 }
    public enum Lifecycle { DRAFT, ACTIVE, DISABLED }
    public enum Health { UNKNOWN, HEALTHY, DEGRADED, UNAVAILABLE, AUTH_FAILED }
}
