# US-37 Analyze Fuel Performance — Technical Closure

**Task:** `US-37-FUEL-PERFORMANCE-TECHNICAL-CLOSURE-001`  
**Result:** `COMPLETE`  
**Story status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**Owner:** Fuel  
**Migration/current head:** V63  
**Accounting:** 67 / 87 accepted; 20 / 87 remaining

## Independent findings and remediation

Source-first review confirmed Fuel owns metric definitions, calculation, quality, baseline, peers, attribution, deterministic indicators, API semantics, and `FuelPerformanceQuery`. Reporting has no Fuel persistence access. Fuel consumes only bulk, Tenant-scoped Fleet/Trip root contracts and trusted Tenancy context. Raw Fuel, Fleet, Trip, Driver, and Reporting facts are never mutated.

Closure found and corrected three technical gaps without changing the frozen product contract:

- trend threshold decisions now use unrounded `BigDecimal` source precision, so 29.99% remains below and 30.00% meets the boundary;
- empty Tenant-calendar trend buckets are returned as explicit `INSUFFICIENT` null gaps, never omitted or represented as zero;
- the Fuel source-window query is database-capped at 100,000 rows and fails closed above that limit, preventing unbounded whole-fleet memory loading.

The acceptance fixture was strengthened to include 101 Vehicle comparison rows, more than 600 issued/invalid source facts, both modes, two fuel types, three attributed Drivers, a prior baseline, consecutive anomalous buckets, missing/reset-unsafe facts, and a second Tenant. Source snapshots are read through every page before and after all six analytics routes.

## Fresh verification

- Clean PostgreSQL/Flyway: only `transport_logistics_acceptance`; empty schema migrated V1 through V63; 63 migrations validated/applied; development database excluded.
- Focused Fuel analytics/security: 14 tests, 0 failures, 0 errors, 0 skipped in 18.647 seconds.
- Thresholds: 19.99% no deviation; 20.00% deviation/review; 29.99% no leakage; 30.00% qualifies; only two consecutive qualifying buckets emit possible leakage/review.
- Source immutability: summary, Vehicle list/detail, Driver list/detail, and trends leave exact source facts unchanged.
- Affected bunker concurrency diagnosis: first complete run had one suite-timing failure; isolated test 1/1 PASS and full affected class 7/7 PASS. No Fuel defect or production change was involved.
- Complete Maven rerun: 1,310 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS`; 05:14.
- Architecture: 46 tests, 0 failures/errors/skips; Spring Modulith, hexagonal, module/table ownership, P0-01..P0-07, and P1-01 PASS.
- Static analysis: Checkstyle, PMD, and SpotBugs PASS.
- Frontend: TypeScript PASS; Vitest 262/262 across 62 files; production build PASS; changed-file ESLint PASS.
- Global ESLint: 71 errors remain exclusively in unchanged Delivery files; pre-existing and outside US-37 scope. US-37 introduced lint errors: 0.
- Real Chromium/PostgreSQL: 6/6 PASS in 23.1 seconds, including the 101-row pagination fixture, both modes, UI/period, details/privacy, deterministic indicators, insufficient state, Tenant-B denial, and exact paginated source immutability.
- `git diff --check`: PASS.

## Closure boundary

Raw source mutation, foreign persistence/SQL, N+1 calls, mode mixing, ranking, punitive language, ML, US-38 behavior, payroll/billing, analytics tables, cache, scheduled refresh, event/outbox/P1-01 production, export, write routes, threshold configuration, and historical migration edits are absent.

US-37 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`. No story count advances. The next task is `US-37-FUEL-PERFORMANCE-FINAL-ACCEPTANCE-001`.
