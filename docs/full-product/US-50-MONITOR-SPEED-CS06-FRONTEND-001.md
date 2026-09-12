# US-50 Monitor Speed — CS06 Frontend

Status: **COMPLETE** on 2026-09-12. US-50 remains `IMPLEMENTATION_IN_PROGRESS`; accounting remains 73/87 complete. Flyway remains V83 and V84 is absent.

## Delivered

- Added permission-aware Tracking navigation and routes for speed rules, current Vehicle states and speed episode history/detail.
- Added Tenant and route/version rule create, detail, edit, activate, disable, reactivate and permanent-retire workflows. There is no delete action.
- Preserved stable idempotency keys per create/lifecycle attempt and sent exact optimistic versions; stale conflicts provide reload/retry recovery.
- Added truthful `UNKNOWN`, `NORMAL` and `SPEEDING` state presentation. Configuration-unavailable and missing values remain explicit and are never rendered as zero.
- Added bounded cursor episode history with an enforced ordered UTC range of at most 31 days, exact Tracking `WARNING`/`HIGH` severity, repeat evidence and nullable attribution.
- Kept the UI privacy-minimized: no coordinates, raw telemetry, device/provider secrets, Customer data, inferred Driver identity, legal-limit claim, disciplinary action, map or US-54 dashboard.
- Enforced exact UI capabilities for `SPEED_MONITOR_VIEW`, `SPEED_MONITOR_MANAGE` and `SPEED_EVENT_VIEW`; backend authorization remains authoritative.

## Verification

- Focused frontend: 3 files, 10/10 tests PASS.
- Full frontend: 74 files, 309/309 tests PASS.
- TypeScript: PASS.
- Changed-file ESLint: PASS with zero US-50 findings.
- Global ESLint: 71 unrelated pre-existing Delivery findings; no US-50 file is involved.
- Production build: PASS, 5,246 modules; the existing >500 kB chunk advisory remains (2,399.03 kB JS, 694.64 kB gzip).
- Real Chromium against `transport_logistics_acceptance`: 10/10 PASS, including literal `/api/v1/tracking/speed-monitoring/rules` 403, Tenant isolation, state/episode behavior, privacy and the 31-day bound.
- Focused PostgreSQL API/security: 5/5 PASS via the explicit fail-closed local acceptance path.
- Architecture: 52/52 PASS.
- Flyway: 83 migrations validated, current head V83; no V84 migration exists.
- Complete Maven evidence is inherited from the fresh CS05 repository baseline because CS06 changed no backend file: 1,656 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS.
- `git diff --check`: PASS.

One broader 59-test diagnostic selection was excluded: the Testcontainers path negotiated obsolete Docker API 1.32, and the explicit-local rerun then exposed the already-existing `SpeedEvaluationRuntimePostgreSqlAcceptanceTest.clean` attempt to delete immutable speed-episode evidence. The required focused API/security gate passed independently; CS06 does not alter backend tests or production semantics.

## Scope and next task

No backend, public API, migration, permission, event, Notification, dependency, map, Driver-discipline or production dependency change was made. US-48 remains on external-prerequisite hold. Accounting remains 73/87.

Next: `US-50-MONITOR-SPEED-CS07-POSTGRES-CONCURRENCY-PERFORMANCE-001`.
