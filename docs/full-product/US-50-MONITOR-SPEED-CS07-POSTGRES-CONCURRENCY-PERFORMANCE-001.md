# US-50 Monitor Speed — CS07 PostgreSQL Concurrency and Performance

**Result:** `COMPLETE`  
**Story:** `IMPLEMENTATION_IN_PROGRESS`  
**Database:** `transport_logistics_acceptance` only  
**Flyway:** V83; V84 absent  
**Accounting:** unchanged at 73/87 complete and 14/87 remaining

## Test-harness remediation

`SpeedEvaluationRuntimePostgreSqlAcceptanceTest` previously deleted immutable episode evidence during setup.
Its test-only setup now rebuilds the explicitly isolated schema with Flyway. The V81 trigger remains enabled
and UPDATE/DELETE rejection still passes; no production code, migration or episode semantics changed.

## Concurrency, plans and performance

The 41-test PostgreSQL/runtime/security matrix passed three consecutive complete repetitions (123/123).
It covers deterministic confirmation/state, source ordering, rule precedence and concurrency, repeat bounds,
immutable evidence, management idempotency, Tenant isolation, outbox/Notification replay, bounded workers,
idempotent enqueue, disjoint `FOR UPDATE SKIP LOCKED` claims, lease renewal/reclaim, stale-owner rejection and
transaction failure safety. No deadlock, lost job or per-packet notification was observed.

At 5,000 rows PostgreSQL used bounded index scans: `idx_tracking_speed_job_global_due` (16 rows, 0.034 ms),
`idx_tracking_speed_rule_route_lookup` (1 row, 0.013 ms), `tracking_speed_state_pkey` (1 row, 0.014 ms),
`idx_tracking_speed_episode_repeat` (1 row, 0.021 ms), and `idx_tracking_speed_episode_vehicle_history`
(10 rows, 0.021 ms). No V84 index is required.

Signed ingress with evaluator, outbox and Notification enabled measured 441.3 msg/s sustained (200 messages)
and 1,265.5 msg/s burst (two Vehicles, 1,000 messages). Latest p95 was 19.6 ms and 24-hour history p95 18.4 ms.
Real Chromium passed 11/11. Full Maven passed 1,656 tests with zero failures/errors and 15 skipped in 12:39.
Architecture passed 52/52; Vitest passed 309/309; TypeScript, production build, Checkstyle, PMD, SpotBugs and
`git diff --check` pass. The 71 unrelated Delivery ESLint findings remain unchanged and non-blocking.

Two discarded Maven invocations exposed datasource configuration only: ordinary Spring tests defaulted to
port 5432 while the explicit harness used 5433. The accepted run pinned both conventions to
`transport_logistics_acceptance`; development-database authoritative evidence is **NO**.

No production code, API, permission, event, Notification behavior, frontend feature, dependency, schema or
migration changed. US-48 remains externally blocked. Next: `US-50-MONITOR-SPEED-TECHNICAL-CLOSURE-001`.
