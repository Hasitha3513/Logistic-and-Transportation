# US-37 Analyze Fuel Performance — Final Acceptance

**Task:** `US-37-FUEL-PERFORMANCE-FINAL-ACCEPTANCE-001`

**Decision:** `PASS / COMPLETE`

**Date:** 2026-09-04

**Migration:** V63 permission seed; current Flyway head V63

**Program accounting:** 68 / 87 COMPLETE; 19 / 87 remaining

**Wave B:** OPEN

**Next task:** `US-35-FUEL-CARDS-PRODUCT-DECISIONS-001`

## Independent acceptance decision

US-37 satisfies the frozen source intent. Authorized users can analyze Tenant-scoped Fuel performance by summary, Vehicle, Driver, and trend using real committed Fuel facts without changing source history. The implementation remains a Fuel-owned, read-only, on-demand projection and exposes only the provider-neutral root `FuelPerformanceQuery` contract to Reporting.

The accepted calculations support 7/30/90-day and custom windows of at most 365 Tenant calendar days, distance and engine-hour measurement modes, same-Vehicle prior-period baselines, compatible peer comparisons, deterministic 20% deviation and repeated 30% possible-leakage indicators, explicit data quality, and non-punitive wording. Empty trend buckets are explicit `INSUFFICIENT` null gaps. Threshold decisions retain unrounded `BigDecimal` precision, and source processing fails closed above 100,000 rows.

## Authorization, tenancy, privacy, and scope

All six routes are read-only under `/api/v1/fuel/performance` and require the independently seeded V63 permission `FUEL_PERFORMANCE_VIEW`. Tenant authority is server-derived; foreign-Tenant Vehicle and Driver identifiers are safe not-found. Driver output is privacy-minimized and does not rank, discipline, score, export, or automatically create US-38/US-78 actions.

Source and architecture inspection confirmed no foreign repository/entity/table/SQL access, no physical cross-module foreign key, no N+1 foreign lookup, no cache, no persisted analytics projection, no event publication, and no raw Fuel mutation. The six-scenario browser fixture proves exact source immutability, including a 101-row pageable Vehicle comparison.

## Fresh acceptance evidence

- Isolated PostgreSQL preparation used only `transport_logistics_acceptance`; its `public` schema was recreated before acceptance. No development database was contacted.
- Focused Fuel performance and literal-route security group: 14 tests, 0 failures, 0 errors, 0 skipped; clean Flyway V1→V63; `BUILD SUCCESS` in 19.291 seconds.
- Complete Maven verification: 1,310 tests, 0 failures, 0 errors, 15 skipped; terminal `BUILD SUCCESS` in 05:15 under Java 21; Flyway reached V63.
- Architecture and Spring Modulith: 46 tests, 0 failures, 0 errors, 0 skipped.
- Checkstyle: 0 violations; PMD: PASS; SpotBugs: 0 findings/errors.
- Frontend: TypeScript PASS; Vitest 62 files / 262 tests PASS; production build PASS; US-37 changed-file ESLint PASS.
- Repository-wide ESLint retains 71 pre-existing errors and 0 warnings in eight Delivery files; US-37 introduced lint errors are zero.
- Fresh real PostgreSQL-backed Chromium: 6/6 PASS in 21.7 seconds, covering summary/timezone controls, both measurement modes, pageable Vehicle detail, privacy-minimized Driver analysis, deterministic indicators/insufficient data, Tenant denial, and exact source immutability.
- Historical migration inspection confirms V1–V62 are unchanged and US-37 adds only `V63__fuel_performance_permission_us37.sql`.
- `git diff --check`: PASS after closure-document synchronization.

An initial Maven invocation inside the restricted execution sandbox is excluded: the sandbox denied JVM self-attachment and local PostgreSQL sockets with `Operation not permitted`, producing setup cascades rather than test assertions. The identical unrestricted rerun above is the authoritative acceptance result; no code, fixture, configuration, or database remediation occurred between invocations.

## Final disposition

`US-37 = COMPLETE`. Program accounting advances exactly once to 68 / 87 complete and 19 / 87 remaining (`68 + 19 = 87`). Wave B remains open. The next authorized task is `US-35-FUEL-CARDS-PRODUCT-DECISIONS-001`.
