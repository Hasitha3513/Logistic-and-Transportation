package com.transportlogistics.app.tracking.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class TrackingModels {
 private TrackingModels() {}
 public enum DeviceLifecycle { DRAFT, ACTIVE, DISABLED, RETIRED }
 public enum EngineState { ON, OFF, UNKNOWN }
 public enum Trust { TRUSTED, UNTRUSTED, UNKNOWN }
 public enum Freshness { LIVE, RECENT, STALE, UNKNOWN }
 public enum Connectivity { CONNECTED, DEGRADED, OFFLINE, UNKNOWN }
 public enum Ordering { IN_ORDER, CLOCK_SKEW, OUT_OF_ORDER, LATE, FUTURE }
 public enum ProviderBindingLifecycle { DRAFT, ACTIVE, DISABLED, RETIRED }
 public record Device(UUID id,UUID tenantId,String externalReference,String providerAlias,String hardwareSerialReference,DeviceLifecycle lifecycle,Instant registeredAt,UUID registeredBy,Instant lastSeenAt,long version) {}
 public record Association(UUID id,UUID tenantId,UUID deviceId,UUID vehicleId,Instant effectiveFrom,Instant effectiveTo,Instant createdAt,UUID createdBy) {}
 public record Observation(UUID id,UUID tenantId,UUID deviceId,UUID vehicleId,String providerAlias,String providerMessageId,Long providerSequence,String dedupeIdentity,String payloadHash,Instant sourceTimestamp,Instant receivedAt,BigDecimal latitude,BigDecimal longitude,BigDecimal accuracyMeters,BigDecimal speedKph,BigDecimal headingDegrees,BigDecimal altitudeMeters,EngineState engineState,BigDecimal odometerKm,BigDecimal engineHours,Trust trust,String quality,Ordering ordering,String retentionPolicy,String retentionPolicyVersion,Instant retainUntil,Map<String,String> safeMetadata) {}
 public record State(UUID vehicleId,Observation latestReceived,Observation latestTrusted,Freshness freshness,Connectivity connectivity,String policyVersion,Instant evaluatedAt) {}
 public record ProviderBinding(UUID id,UUID tenantId,String providerKeyId,String providerAlias,String credentialReference,ProviderBindingLifecycle lifecycle,Instant createdAt,UUID createdBy,Instant updatedAt,UUID updatedBy,long version) {}
 public record RetentionPolicy(UUID id,UUID tenantId,long retentionDurationSeconds,String policyVersion,Instant effectiveAt,long version) {}
 public record TrackingHealth(int configuredBindings,int activeBindings,int staleDevices,int offlineDevices,Instant latestSuccessfulIngest) {}
}
