# US-49 CS07 — PostgreSQL Concurrency and Performance Closure

## Result

`COMPLETE`

The complete V80-aware CS07 matrix passed without production-code remediation. US-49 remains
`IMPLEMENTATION_IN_PROGRESS`, Flyway remains V80, no V81 exists, accounting remains 72 / 87 complete,
and the US-48 external acceptance hold is unchanged.

## Original block and remediation

The original V79 CS07 execution proved functional concurrency but blocked because the global due-job claim
used a sequential scan plus global sort and the active bbox lookup filtered nearly the entire Tenant
population. CS07A introduced only the authorized V80 claim and bbox indexes and numeric JDBC parameter
binding. This rerun verifies those physical changes under the complete concurrency and performance matrix.

## Repeated PostgreSQL race matrix

The unchanged 31-test PostgreSQL-focused matrix ran three consecutive times; all 93 executions passed.

| Repetition | Result | 500-active initialization | Bbox plan | Due-job plan |
| --- | --- | ---: | ---: | ---: |
| 1 | 31 / 31 PASS | 822 ms | 0.105 ms | 0.029 ms |
| 2 | 31 / 31 PASS | 2,581 ms | 0.097 ms | 0.033 ms |
| 3 | 31 / 31 PASS | 917 ms | 0.107 ms | 0.027 ms |

The matrix proves duplicate first-state and confirmation serialization, deterministic source-time ordering,
delayed rewind prevention, lifecycle/version serialization, stale optimistic management rejection, the
499+2 activation race with a final count of exactly 500, Tenant-independent limits, immutable transition
uniqueness, overlapping-geofence independence, management idempotency and safe audit, outbox/Notification
idempotency, and no stable-position per-packet transition event.

## Worker and lease safety

- The global scheduler has one geofence evaluator entry point and no per-Tenant, per-Vehicle, per-geofence,
  or per-position scheduled entry points.
- Concurrent `FOR UPDATE SKIP LOCKED` workers claimed distinct bounded batches with one owner per job.
- Expired work was reclaimed; every stale-owner renew, complete, retry and release mutation was rejected.
- The fixed worker pool and bounded queue released unstarted claimed work on saturation; no thread-per-job
  behavior or data loss was observed.
- Controlled transaction/publication failures preserve atomic state/transition/outbox behavior and safe job
  retry; consumer replay creates no duplicate logical final IN_APP notification.
- No deadlock, connection leak, pool exhaustion, transaction-timeout storm, or global isolation-level change
  was observed or introduced.

## V80 query plans

At approximately 5,000 ACTIVE definitions with 10 bbox candidates,
`idx_tracking_geofence_active_bbox_upper` is used as an index-only scan. Ten candidates are returned without
filtering approximately the full Tenant population. The existing state-vehicle index remains active for exit
detection. Execution across the three repetitions was 0.097–0.107 ms. Explicit numeric parameter binding
keeps casts on the parameters rather than the indexed bbox columns.

At approximately 5,000 due jobs, `idx_tracking_geofence_job_global_due` is used as an ordered index scan.
The bounded query returns 16 rows, performs no full due-job sequential scan and no global explicit sort.
Execution across the three repetitions was 0.027–0.033 ms.

Representative current-state and transition-history lookups remain tenant-leading and index-supported.
Clean V1→V80 and explicit V79→V80 migrations pass. V80 is the current head and V81 is absent.

## Performance and regression evidence

- Real signed PostgreSQL ingress with evaluator and durable integration enabled: 1,287.7 msg/s sustained;
  1,442.4 msg/s burst; latest p95 10.5 ms; history p95 14.8 ms.
- The 500-active workload always created exactly 500 silent initial states and zero transition/outbox events.
- Tracking Java: 183 / 183 PASS.
- Notification Java: 164 / 164 PASS.
- Security/Tenant/privacy/RBAC selection: 152 / 152 PASS.
- Full Maven: 1,595 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS in 10:44.
- Architecture/Modulith: 52 / 52 PASS.
- Checkstyle: zero configured violations. PMD: PASS. SpotBugs: zero findings/errors.
- TypeScript: PASS. Vitest: 299 / 299 PASS. Production build: PASS.
- Real Chromium: US-49 6 / 6 and performance 1 / 1 PASS.
- Global ESLint baseline remains 71 unrelated Delivery errors. No frontend file changed; CS07 introduced no
  lint debt, so changed-file lint is not applicable.
- `git diff --check`: PASS.
- Every authoritative PostgreSQL run used `transport_logistics_acceptance`; development database evidence is
  `NO`.

## Scope exclusions

No table, column, migration, permission, API, event contract, Notification semantic, scheduler semantic,
worker architecture, frontend feature, dependency, lifecycle, PostGIS capability, or downstream US-50–55
scope was added or changed.

## Next task

`US-49-MANAGE-GEOFENCES-TECHNICAL-CLOSURE-001`
