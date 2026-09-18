# US-51 CS03 — V102 Idle Persistence and Dispatch Closure

## Verdict

`US-51-MONITOR-IDLE-TIME-CS03-V102-IDLE-PERSISTENCE-DISPATCH-001` is complete on the
tested application tree derived from `675503198a9e10a0d3a28790370bbec87dcf42bb`.
Flyway V102 is the verified current head. Story accounting remains 73/87.

## Migration boundary

`V102__create_us51_idle_persistence_and_dispatch.sql` creates only Tracking-owned
`tracking_idle_state`, `tracking_idle_episode`, and `tracking_idle_episode_evidence`, together
with their approved constraints and query indexes. It also extends the existing
`ck_tracking_telemetry_dispatch_evaluator` vocabulary with `IDLE` while retaining `GEOFENCE`,
`SPEED`, and `ROUTE_DEVIATION`.

- `tracking_idle_state` is keyed by Tenant and Vehicle, records the effective capability and
  minimized evaluator state, and uses optimistic versioning.
- `tracking_idle_episode` enforces at most one non-closed episode for each Tenant/Vehicle and
  supports deterministic `(start_source_timestamp DESC, id DESC)` keyset reads.
- `tracking_idle_episode_evidence` is append-only and deduplicated by Tenant, episode, source
  timestamp, and canonical identity. It stores no coordinates, raw provider payload, credential,
  signature, Driver, or Customer data.
- Device and episode relationships are composite Tenant-qualified internal Tracking references.
  Vehicle remains a logical UUID reference owned outside Tracking.

V1/V2 history and ignition semantics are unchanged. V102 does not seed or activate any production
`ENGINE_RUNNING` capability.

## Persistence and transaction behavior

`IdlePersistencePort` is framework-neutral. `JdbcIdlePersistenceAdapter` serializes a Tenant/Vehicle
mutation with the established Tenant-qualified advisory lock and commits episode, evidence, and
state changes in one transaction. Database uniqueness is the second line of defense for concurrent
open-episode creation. Evidence replay reports a duplicate without changing counters, while state
and episode updates use governed optimistic versions.

## Staged durable dispatch

Canonical V3 ingestion creates `IDLE` dispatch work only when the observation contains a consistent
engine-running state/source pair and the device has source-time `ENGINE_RUNNING=SUPPORTED`.
V1/V2 and absent/unknown/unsupported capabilities remain ineligible. History and dispatch intent
are committed together before Kafka acknowledgement.

The existing worker claim deliberately excludes `IDLE` until CS04 installs the real evaluator.
`claimIdle` exposes the existing bounded lease/reclaim mechanism to that future evaluator. No no-op
consumer exists, and staged IDLE work cannot be silently completed. Existing evaluator claims,
completion, retries, expiry recovery, and `FOR UPDATE SKIP LOCKED` behavior are unchanged.

## Verification evidence

All destructive database verification used disposable Testcontainers or the explicitly isolated
`transport_logistics_acceptance` database. The development database was not reset or modified.

- Focused CS03/V101/history/dispatch/worker selection: 26 tests, 0 failures, 0 errors.
- Clean V1→V102 and compressed V101→V102 paths, metadata, constraints, append-only evidence,
  Tenant isolation, duplicate insertion, rollback, optimistic conflicts, and one-open concurrency:
  PASS.
- IDLE dispatch eligibility, V1/V2 exclusion, atomic history/intent, lease ownership, expiry
  recovery, and retained evaluator behavior: PASS.
- Architecture/Modulith/table-ownership suite: PASS.
- Complete Maven `clean test`: PASS.
- Checkstyle: 0 violations; PMD: PASS; SpotBugs: PASS; dependency analysis: PASS.
- Docker Compose validation and `git diff --check`: PASS.

The exact terminal totals and tested commit are retained in the task close-out and repository test
reports. An initial complete run exposed one stale factual V101 assertion in a V58 acceptance test;
the assertion was corrected to V102 and its focused test passed before the complete rerun.

## Limitations and next task

CS03 provides persistence and staged dispatch only. It does not implement qualification,
confirmation, recovery, APIs, permissions, UI, Notification, Operations, fuel estimation, or any
production device mapping. Production engine-running source activation and genuine physical
acceptance remain pending.

Next: `US-51-MONITOR-IDLE-TIME-CS04-EVALUATOR-001`.
