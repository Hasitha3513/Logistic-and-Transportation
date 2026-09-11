# US-50 Monitor Speed CS02 — V81 Persistence

**Task:** `US-50-MONITOR-SPEED-CS02-V81-PERSISTENCE-001`  
**Result:** COMPLETE  
**US-50:** `IMPLEMENTATION_IN_PROGRESS`  
**Accounting:** unchanged at 73 / 87 complete; 14 / 87 remaining  
**Flyway:** V81; V82 absent  
**Database evidence:** `transport_logistics_acceptance` only

## Implemented boundary

V81 creates exactly four Tracking-owned, Tenant-scoped tables: `tracking_speed_rule`,
`tracking_speed_state`, `tracking_speed_episode`, and `tracking_speed_evaluation_job`. There are no
cross-module foreign keys; Trip, Driver, route and Vehicle references remain logical identifiers. The only
physical foreign key is the same-module, Tenant-consistent evaluation-job reference to `tracking_position`.

The schema enforces decimal thresholds greater than zero and no greater than 400 km/h, coherent Tenant and
route-version scopes, exact rule lifecycles, one active Tenant fallback, one active route/version rule, and one
current state per Tenant and Vehicle. Episode IDs remain deterministic domain UUIDs. A narrow database trigger
prevents deletion, mutation of identity evidence, backwards progress, and every update to a closed episode.

JDBC adapters implement the CS01 rule, state, episode and evaluation-job ports with Tenant-qualified SQL,
optimistic rule/state versions, row/advisory locking, idempotent episode/job writes, bounded repeat/history
queries, and exact `BigDecimal`/`Instant` mapping. The global job claim uses deterministic ordering and
`FOR UPDATE SKIP LOCKED`; renew, completion, retry/release and expired-lease recovery are owner-qualified and
reject stale owners.

## Verification

- V1→V81 and V80→V81: PASS; aggregate V1–V80 hash remained
  `871f0d89b12f5ffdf3106673aa19a749a980f1c593270c315e06be5efdff2301`.
- Focused V81/domain/ownership: 32 tests, 0 failures, 0 errors, 0 skipped.
- Stale current-head regression group: 35/35 PASS after updating V80 expectations to V81.
- Complete Tracking: 212/212 PASS; complete Trip: 101/101 PASS.
- Architecture/Modulith: 52/52 PASS.
- Complete Maven verify: 1,624 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS in 11:44.
- Checkstyle: 0 violations; PMD: PASS; SpotBugs: 0 findings.
- 5,000-row PostgreSQL plans used the named global-job, route-rule, state-PK, episode-repeat and
  episode-history indexes, with measured execution times from 0.027 ms to 0.042 ms.
- Job idempotency, two-worker skip-locked claiming, lease recovery/stale-owner denial, state first-write race,
  concurrent episode idempotency, closed immutability and cross-Tenant denial: PASS.
- `git diff --check`: PASS.

Accepted database-backed runs pinned both guarded PostgreSQL acceptance tests and ordinary Spring integration
contexts to `jdbc:postgresql://127.0.0.1:5433/transport_logistics_acceptance`. The development database was not
authoritative evidence and was not used by the accepted runs.

## Explicit exclusions

CS02 adds no API, permission seed, security/controller wiring, frontend, outbox/audit table, Notification
catalogue or consumer, event publisher adapter, scheduler/worker execution, V82 migration, story-accounting
change, or US-48 acceptance inheritance.

## Next controlled change set

`US-50-MONITOR-SPEED-CS03-EVALUATION-EPISODES-001`
