# US-49 CS07A — V80 Index Hardening

## Result

`COMPLETE`

V80 adds only the two authorized physical-design indexes and the candidate lookup was reshaped without changing its logical result, public API, lifecycle, tenancy, RBAC, audit, or event contracts. US-49 remains `IMPLEMENTATION_IN_PROGRESS`; story accounting remains 72 / 87 complete.

## Database change

- `idx_tracking_geofence_job_global_due` on `(next_attempt_at, tenant_id, position_id)` including `status` and `lease_until` supports the global ordered `FOR UPDATE SKIP LOCKED` claim.
- Partial `idx_tracking_geofence_active_bbox_upper` on `(tenant_id, max_longitude)` including the remaining bbox coordinates and `id`, for `lifecycle = 'ACTIVE'`, supports active candidate narrowing.
- The production bbox query casts JDBC coordinate parameters to PostgreSQL `numeric`, preventing casts of the indexed numeric columns, and unions bbox candidates with existing vehicle membership candidates before the bounded definition lookup.

## Query-plan evidence

At 5,000 geofences, the pre-V80 candidate plan performed a sequential scan, removed 4,990 rows by filter, and completed in 2.405 ms. The V80 plan uses an index-only scan on `idx_tracking_geofence_active_bbox_upper`, returns the 10 candidates, retains `idx_tracking_geofence_state_vehicle` for the membership branch, and completes in 0.118 ms.

At 5,000 due jobs, the pre-V80 global claim performed a sequential scan and sort and completed in 1.112 ms. The V80 plan performs an ordered index scan on `idx_tracking_geofence_job_global_due`, has no sequential scan or explicit sort, returns the bounded 16-row claim batch, and completes in 0.036 ms.

## Verification

- Focused PostgreSQL persistence/concurrency/migration group: 31 / 31 PASS.
- Flyway clean migration: V1 → V80 PASS; forward-only upgrade: V79 → V80 PASS; no V81 introduced.
- Complete Maven verification: 1,595 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS (11:03).
- Architecture: 52 / 52 PASS.
- Checkstyle: 0 violations. PMD: BUILD SUCCESS. SpotBugs: 0 findings.
- TypeScript: PASS. Vitest: 299 / 299 PASS. Production build: PASS.
- Global ESLint: 71 pre-existing errors in unrelated Delivery frontend files; no frontend file changed by CS07A, so changed-file lint has no applicable files and introduces zero lint debt.
- Real PostgreSQL-backed Chromium: US-49 journey 6 / 6 PASS; performance 1 / 1 PASS.
- Performance: sustained 1,083.4 msg/s; burst 1,423.4 msg/s; latest p95 16.2 ms; history p95 13.3 ms.
- `git diff --check`: PASS.
- Accepted PostgreSQL evidence used only `transport_logistics_acceptance`; the development database was not used.

## Next task

`US-49-MANAGE-GEOFENCES-CS07-POSTGRES-CONCURRENCY-PERFORMANCE-001-RERUN`
