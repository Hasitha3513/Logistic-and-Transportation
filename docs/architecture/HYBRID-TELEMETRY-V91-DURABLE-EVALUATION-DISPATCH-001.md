# Hybrid Telemetry V91 — Durable Evaluation Dispatch

**Status:** `IMPLEMENTATION_COMPLETE`

**Date:** 2026-09-14

**Flyway head:** V91

## Decision

Tracking now persists each retained immutable telemetry history fact and its GEOFENCE, SPEED and
ROUTE_DEVIATION evaluation intents in one PostgreSQL transaction. Kafka acknowledgement remains
after that transaction returns successfully. A history or dispatch failure rolls back the whole
batch and permits Kafka redelivery; evaluator execution is asynchronous and cannot remove or roll
back accepted history. Redis remains an independent Kafka consumer.

`tracking_telemetry_evaluation_dispatch` is the single Tracking-owned queue. Tenant, immutable
history ID/source time, canonical dedupe identity, Vehicle, evaluator, state, attempts, due time,
lease, completion, bounded error code and version are persisted. Two Tenant-scoped unique keys
make replay idempotent. PostgreSQL `FOR UPDATE SKIP LOCKED`, bounded claims, expiring leases and
source-time ordering provide safe recovery and prevent simultaneous execution of one dispatch.

## Timescale relationship

The dispatch stores the complete immutable history identity
`(tenant_id, source_timestamp, history_id)` and insertion selects that exact history row inside the
same transaction. A direct normal-table foreign key is not used because TimescaleDB uniqueness on
a hypertable must include its partition column and the repository's history identity constraints
cannot be reshaped without a broader migration. Database uniqueness plus atomic insertion is the
strongest compatible relationship. No raw payload, credential, signature, Driver PII or Customer
PII is copied into dispatch.

V91 replaces the legacy-only geofence-transition position foreign key with a same-Tenant insert
constraint trigger accepting a confirming position from either legacy `tracking_position` or
immutable `tracking_position_history`. Legacy ingestion and job tables remain intact.

## Execution

The worker loads the exact Tenant-qualified history fact and invokes the existing geofence, speed
or route-deviation application port. It does not duplicate business algorithms. Failures retain a
retryable durable row with exponential bounded scheduling and a safe classification; the tenth
failed attempt is represented as FAILED and remains available for deliberate recovery. IDLE is not
an allowed evaluator because US-51 still lacks authoritative engine-state evidence.

## Verification

- Focused dispatch/history/Kafka/Timescale tests: PASS, including three-intent atomicity, replay
  idempotency, rollback/non-acknowledgement, claims, leases, retry/failure and exact-history routing.
- Real PostgreSQL/Kafka/Redis browser journeys: US-49 6/6, US-50 10/10 and US-52 CS06 10/10 PASS.
- Flyway V1→V91 and current-head migrations: PASS.
- Architecture/Spring Modulith: 59/59 PASS.
- Complete Maven: 1,750 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS in 10:18.
- Frontend: 77 files and 319 tests PASS; TypeScript and production build PASS.
- Checkstyle, PMD and SpotBugs: PASS; Docker Compose and `git diff --check`: PASS.

## Operations and rollback

Workers may be disabled while dispatch rows remain durable. V91 must not be edited or removed after
deployment. Any schema rollback or correction requires a separately reviewed forward migration.
The Compose Timescale service explicitly preloads the extension required by the governed image.
