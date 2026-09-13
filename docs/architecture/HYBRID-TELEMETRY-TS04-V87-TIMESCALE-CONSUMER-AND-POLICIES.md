# Hybrid Telemetry TS04 — V87 Timescale Consumer and Policies

**Status:** `IMPLEMENTATION_IN_PROGRESS / TS04_COMPLETE`

**Date:** 2026-09-13

**Flyway head:** V87

## Runtime and ownership

Tracking consumes canonical `tracking.telemetry.ingested.v1` records in consumer group
`tracking-telemetry-persister-group`. The infrastructure adapter validates the Kafka topic, the
`{tenantId}:{vehicleId}` key, Tenant/event/version headers and the canonical V1 payload before it
maps the accepted fact to the framework-neutral historical-storage port. Batches are bounded by
`app.tracking.hybrid-storage.stream-batch-size`, default 500.

The complete accepted batch is persisted inside one Tracking-owned PostgreSQL transaction. Kafka
manual acknowledgement occurs only after that transaction returns successfully. A database outage
rolls back the batch, leaves the source offset unacknowledged and raises a dependency failure for
bounded retry/redelivery. Poison records follow the privacy-minimized, access-controlled
`tracking.telemetry.ingested.v1.dlt` path. Logs and metric labels contain neither coordinates nor
Tenant, Vehicle, provider-secret, signature or raw-payload material.

## Historical fact and idempotency

`tracking_position_history` stores immutable normalized V1 facts: event and deduplication identity,
trusted Tenant/Vehicle/device/provider identity, WGS84 position, accuracy, motion and meter facts,
separate source and receipt times, schema version, ordering, quality and trust classifications, and
the raw-retention classification. It stores no credentials, signatures, provider request bodies,
Driver PII or Customer PII.

V87 adds `event_version`, validates version 1, and enforces database idempotency with the unique
Tenant-scoped key `(tenant_id, source_timestamp, dedupe_identity)`. Replays are safe at the database
boundary; the same external identity in a different Tenant remains legal. PostgreSQL advisory
transaction locks serialize reduction decisions for one Tenant/Vehicle without coupling different
Tenant or Vehicle streams. Late and out-of-order valid facts remain append-only history and never
replace newer rows.

## Deterministic static reduction

A point is reducible only when the previous accepted historical fact for the same Tenant and Vehicle
has exactly equal decimal latitude and longitude, speed is exactly zero, the non-null engine state
is unchanged, and accuracy, odometer, engine-hours, ordering, quality and trust facts are unchanged.
Any changed coordinate, non-zero speed, engine transition, changed quality/trust fact, different
Tenant/Vehicle, or missing/uncertain comparison state is retained. Coordinate comparison uses exact
numeric comparison, not a tolerance. A null engine state is uncertain and therefore retained.

The predecessor is selected by source-time order, so Kafka replay, restart, batch boundaries and
concurrent partition processing produce the same decision. Database conflict handling is applied
before reduction classification, so a replay is idempotent rather than incorrectly counted as a
new reducible observation.

## V87 Timescale policy hardening

V87 is forward-only and preserves V1–V86 and existing history. It keeps
`tracking_position_history` as a hypertable partitioned on `source_timestamp`, sets a seven-day
chunk interval, and installs exactly one compression policy after seven days plus exactly one raw
retention policy after 180 days. Compression segments by `tenant_id, vehicle_id` and orders by
`source_timestamp DESC, id DESC`. Existing Tenant/Vehicle/source-time indexes remain the bounded
history-query path. No continuous aggregate, foreign-module table, public API or frontend was added.

The migration was proven both clean V1→V87 and as V86→V87 with pre-existing rows. Real Timescale
catalogue assertions cover the hypertable, source-time dimension, chunk interval, compression keys,
policy counts and intervals. Representative Tenant/Vehicle/range queries are bounded and
foreign-Tenant reads return absence.

## Verification evidence

- TS04 focused consumer/store/migration/Kafka/DLT acceptance: 11/11 PASS.
- Current-head PostgreSQL remediation group: 61/61 PASS and global follow-up 48/48 PASS.
- Complete affected Tracking/Kafka/geofence/speed/route-deviation regression: 247/247 PASS.
- Architecture/Modulith/table ownership: 58/58 PASS.
- Full clean Maven: 1,711 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS in 09:31.
- Checkstyle: PASS, zero configured violations (pre-existing warnings remain).
- PMD: PASS after correcting one TS04-local unused callback parameter finding.
- SpotBugs: PASS, zero findings.
- Docker Compose configuration and `git diff --check`: PASS.
- Frontend: not run because no frontend file changed and TS04 exposes no UI or public API.

The real Kafka/Timescale acceptance publishes canonical V1 records through a real broker and
persists them in a real TimescaleDB container. It proves exact normalized persistence, replay
idempotency, out-of-order retention, transactional rollback, Tenant isolation and the DLT contract.
Acknowledgement ordering and dependency-failure non-acknowledgement are additionally asserted at the
consumer boundary.

## Rollback and remaining risks

The consumer can be disabled while Kafka preserves a replayable backlog; accepted history must not
be deleted or rewritten. A previous application may be redeployed only when it remains V87-compatible.
V87 must never be edited or removed; policy removal or schema correction requires a separately
reviewed forward migration.

The governed Timescale image remains the mutable `timescale/timescaledb:latest-pg16` reference and
requires a separately authorized immutable pin. Flyway 9.22.3 continues to warn that PostgreSQL 16
is newer than its tested maximum. Physical device/provider acceptance for US-48 remains independent.
MVP accounting remains 73/87 accepted with 14 stories remaining.
