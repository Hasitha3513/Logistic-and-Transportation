# US-37 Analyze Fuel Performance — Implementation

**Status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**Owner:** Fuel  
**Migration:** V63 (permission seed only)  
**Current Flyway head:** V63

## Implemented boundary

US-37 is a read-only, bounded, on-demand Fuel projection over committed `ISSUED` Fuel Issues. It does not mutate Fuel Issues, purchases, prices, bunker facts, Trip fuel costs, Fleet readings, Vehicles, Trips, Drivers, or history. It creates no analytics table, cache, scheduled refresh, event, outbox record, export, threshold configuration, US-38 case, or punitive action.

The public Fuel-root `FuelPerformanceQuery` publishes summary, Vehicle, Driver, trend, period, baseline, metric, quality, and indicator values. Fuel obtains minimal Vehicle/Driver and Trip attribution context through bulk public module-root contracts. No foreign repository, entity, table, SQL, or per-row lookup is used. Tenant identity, timezone, and ISO-4217 currency are resolved from trusted Tenancy contracts.

## Analytics

- Modes: `DISTANCE` and `ENGINE_HOURS`; they are never combined.
- Distance output: litres, km, L/km, L/100km, km/L, total cost, cost/km, counts, quantities, baseline, variance, and quality.
- Engine-hour output: litres, hours, L/hour, total cost, cost/hour, counts, quantities, baseline, variance, and quality.
- Arithmetic uses `BigDecimal`, source precision during aggregation, `HALF_UP`, and the frozen output scales.
- Denominators are calculated per Vehicle and then summed, preventing cross-Vehicle meter subtraction.
- Quality is exactly `COMPLETE`, `PARTIAL`, `INSUFFICIENT`, or `INVALID_SOURCE_DATA`; missing values remain null/`N/A`.
- Presets are 7/30/90 Tenant-calendar days; custom periods are inclusive and capped at 365 days. Trends are daily, weekly, or monthly according to the frozen window boundaries, including explicit `INSUFFICIENT` null gaps for empty buckets.
- Historical baseline is the immediately preceding equal-length same-Vehicle/fuel/mode period with at least three valid samples and a positive denominator.
- Compatible peer comparison is separate and requires at least three active same-Tenant Vehicles of the same type, fuel, and mode.
- Driver attribution exposes only ID, operational label, and aggregate/sample facts. A Trip-linked issue is attributed only when Trip, Driver, and Vehicle agree.
- `EFFICIENCY_DEVIATION` begins at 20.00% adverse variance. `POSSIBLE_LEAKAGE_INDICATOR` requires two consecutive valid trend buckets at or above 30.00%. Language remains non-punitive.

## API, RBAC, and frontend

`FUEL_PERFORMANCE_VIEW` exclusively authorizes these six GET routes:

- `/api/v1/fuel/performance/summary`
- `/api/v1/fuel/performance/vehicles`
- `/api/v1/fuel/performance/vehicles/{vehicleId}`
- `/api/v1/fuel/performance/drivers`
- `/api/v1/fuel/performance/drivers/{driverId}`
- `/api/v1/fuel/performance/trends`

Raw Fuel permissions and `REPORT_VIEW` do not grant access. Cross-Tenant direct IDs are safe not-found. No write or export route exists. V63 seeds only the permission and creates no analytics schema.

The `/fuel/performance` page remains inside `AppLayout`, uses TanStack Query and the shared API client, and provides Tenant-period/mode controls, summary cards, actual-versus-baseline trends, Vehicle and privacy-minimized Driver comparisons, quality/exclusion explanations, review indicators, and `N/A` gaps without ranking or punitive wording.

## Verification evidence

- Focused analytics/security: 14/14 PASS, including literal `/api/v1/...` authorization, exact 19.99/20.00 and 29.99/30.00 boundaries, consecutive leakage, explicit trend gaps, and 101-row bounded pagination.
- Architecture: 46/46 PASS; approved `fuel -> tenancy` is limited to published root contracts.
- PostgreSQL/Flyway: isolated `transport_logistics_acceptance`; V1–V63 PASS; V63 permission present; development database excluded.
- Complete Maven: 1,310 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS`; 05:14. The first run exposed one suite-level bunker concurrency timing failure; its isolated test passed 1/1, its affected class passed 7/7, and the required complete rerun passed.
- Static analysis: Checkstyle, PMD, SpotBugs PASS.
- Frontend: TypeScript PASS; Vitest 262/262 PASS; production build PASS; changed-file ESLint PASS with 0 introduced errors.
- Global ESLint: 71 errors remain in unchanged Delivery files and are classified as pre-existing out-of-scope debt; no US-37 file is implicated.
- Real Chromium: 6/6 PASS in 23.1 seconds against PostgreSQL. It covers 101 Vehicle comparison rows, real committed Fuel facts, both modes, period/UI, Vehicle detail/paging, Driver privacy, deterministic deviation/leakage, insufficient data, Tenant-B Vehicle/Driver denial, and paginated exact before/after source-response equality.
- `git diff --check`: PASS.

## Scope exclusions

US-35 cards, US-38 exceptions/corrections, US-46 payroll, US-47 billing, US-82 platform analytics, ML/predictive AI, automatic discipline/cases, event/P1-01 publication, cache, snapshot/table persistence, export, and configurable thresholds remain absent.

## Next gate

`US-37-FUEL-PERFORMANCE-FINAL-ACCEPTANCE-001`. Technical closure passed; story accounting remains 67/87 accepted and 20/87 remaining until independent acceptance.
