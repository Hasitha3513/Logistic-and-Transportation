# Hybrid Telemetry TS03 — Kafka to Redis Live Projector

**Status:** COMPLETE

**Date:** 2026-09-13

**Flyway head:** V86 (no TS03 migration)

## Runtime contract

The Tracking-owned `tracking-live-projector-v1` consumer reads canonical
`TRACKING_TELEMETRY_INGESTED_V1` records from `tracking.telemetry.ingested.v1`. It validates the
Kafka key, Tenant/event headers, payload Tenant/Vehicle authority, event type and version before
calling the Redis projection port. Manual acknowledgement occurs only after the atomic projection
classifies the event as updated, duplicate or stale. Invalid contracts are sent to the internal
seven-day `tracking.telemetry.ingested.v1.dlt`. Transient Redis failures receive two bounded retries,
remain unacknowledged and are not converted into poison records; Kafka retains them for recovery.
No precise coordinates or Tenant identifiers are used as metric labels or error text.

## Redis contract

- Live hash: `tracking:live:{tenantId}:{vehicleId}`
- Tenant index: `tracking:live-index:{tenantId}`
- TTL: exactly 24 hours, sliding on accepted updates and exact replay
- Index structure: expiry-scored sorted set, maximum 10,000 Vehicle members
- Read bound: 1–500 entries, never `KEYS` or Tenant-wide `SCAN`
- Cleanup: expired scores are removed on projection and read; missing live hashes are lazily removed
- Cluster atomicity: both keys use the same `{tenantId}` Redis hash slot

The hash stores source second/nanosecond, immutable event ID and the canonical live projection.
The projection contains the complete accepted V1 normalized facts and projection time, but no
provider secret, credential reference, signature, raw payload, Driver or Customer data.

## Atomic ordering rule

One Lua operation compares, writes, refreshes TTL and maintains/prunes the Tenant index. Ordering is
`recordedAt epoch-second`, then nanosecond, then lexicographically greatest immutable event UUID.
A newer event replaces older state; an older event is stale; exact event replay is idempotent and
only refreshes TTL/index expiry. Equal-time conflicting events converge to the same UUID winner
regardless of arrival order. There is no application-side GET/SET race.

## Read side and scope

`LiveTelemetryProjectionPort` exposes exact Tenant/Vehicle lookup and bounded same-Tenant live
enumeration for later governed slices. It exposes no Redis type and has no controller or public API.
The ingress controller still writes neither Redis nor TimescaleDB. TS03 adds no frontend, detector,
Timescale persistence, database table, migration, cross-module event or story acceptance.

## Verification

- Focused unit/real Redis/real Kafka-to-Redis projector: 6 tests, all passed
- Real Kafka poison record to bounded DLT: 1 test passed
- TS02 plus Tracking/US-48/49/50/52 regression: 250 tests, all passed (03:29)
- Architecture/Modulith/table ownership: 58 tests, all passed
- Complete post-hardening clean Maven suite: 1,702 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS (09:35)
- PMD: PASS; SpotBugs: zero findings; Checkstyle: zero configured violations
- Docker Compose, dependency consistency and `git diff --check`: PASS
- Flyway V1→V86 regression: PASS; no migration added

The real infrastructure evidence uses `apache/kafka:3.7.2` and `redis:7.4-alpine`. It proves durable
Kafka input, real Redis projection, exact TTL, newer/older/duplicate/equal-time ordering, atomic
parallel convergence, bounded Tenant isolation and DLT recovery. Production source contains no
Timescale write in this change set.

## Disablement, recovery and risks

Set `TRACKING_HYBRID_STORAGE_ENABLED=false` to stop the projector without deleting Kafka history.
After Redis recovery, re-enable the consumer and replay retained Kafka offsets to rebuild live
state. Redis loss degrades live reads but does not remove the durable Kafka source. Monitor consumer
lag, DLT volume, Redis availability/memory and index cardinality; never inspect precise location in
logs or metric labels. DLT replay requires a controlled operator process after correcting the cause.

The Timescale image remains the pre-existing mutable `latest-pg16` tag and is intentionally unchanged.
TS04 owns V87 history persistence/policies; US-52 CS02 remains V88.

Next queue: `HYBRID-TELEMETRY-TS04-V87-TIMESCALE-CONSUMER-AND-POLICIES`.
