# Hybrid Telemetry Redis Live Projection Runbook

## Signals

Monitor Kafka consumer-group `tracking-live-projector-v1` lag, internal DLT volume, Redis health,
memory pressure, evictions and `tracking.redis.live.projections` outcomes. Metric labels contain
only result categories. Never log or label Tenant/Vehicle UUIDs, coordinates, payloads or secrets.

## Redis outage

1. Confirm Kafka remains healthy; it is the durable rebuild source.
2. Disable the projector with `TRACKING_HYBRID_STORAGE_ENABLED=false` if repeated Redis failures
   continue after the bounded retries.
3. Restore Redis with the configured `noeviction` and AOF policy.
4. Re-enable the projector and verify consumer lag falls to zero.
5. Verify representative same-Tenant live keys have a TTL no greater than 86,400 seconds and the
   Tenant index contains no expired members.

Do not synthesize live state from another Tenant or silently fall back to PostgreSQL history.

## Dead-letter handling

`tracking.telemetry.ingested.v1.dlt` retains poison records for seven days and is an internal,
access-controlled operational topic. Diagnose using safe event/header identities without printing
coordinates or payloads. Correct the producer/configuration cause, then replay through the original
topic under change control. Do not edit Redis keys manually to manufacture live state.

## Cache rebuild and rollback

Redis hashes and Tenant indexes are disposable projections. A controlled offset replay safely
rebuilds them because source-time/event-ID comparison is deterministic. To roll back TS03, disable
the projector and deploy the prior application version; do not alter V86 or delete Kafka history.
