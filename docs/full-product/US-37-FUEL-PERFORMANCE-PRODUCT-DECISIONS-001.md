# US-37 Analyze Fuel Performance — Product Decisions

**Task:** `US-37-FUEL-PERFORMANCE-PRODUCT-DECISIONS-001`  
**Decision:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`  
**Date:** 2026-09-04  
**Owner:** Fuel  
**Current Flyway head:** V62  
**Migration expectation:** LIKELY — permission seed only; no analytics table or reserved version  
**Program accounting:** 67 / 87 complete; 20 / 87 remaining  
**Next task:** `US-37-FUEL-PERFORMANCE-IMPLEMENTATION-001`

## Source intent and acceptance boundary

The authoritative actor is the Fuel Manager. The goal is to analyze vehicle and driver fuel efficiency, historical trends, anomalies, and possible leakage indicators so fuel waste can be identified. The source requires vehicle comparison, driver comparison, trend viewing, anomaly/leakage flags, supporting evidence, and proof that analysis does not alter raw fueling data.

US-33 remains the owner of operational mileage and reading validation. US-37 owns broader historical Fuel interpretation. A leakage flag is an indicator for review, never proof of theft, fraud, misuse, or driver culpability. US-38 separately owns future investigation, classification, correction/reconciliation, and exception action. Predictive AI, opaque ML, manufacturer benchmarks, automatic enforcement, and automatic case creation are absent.

## Ownership and source facts

Fuel owns all US-37 calculations, thresholds, quality interpretation, API semantics, and the provider-neutral performance projection. Reporting may present or compose the published Fuel projection but may not redefine formulas or query Fuel persistence.

Only authoritative current facts participate:

- US-31: `ISSUED` Fuel Issue quantity, fuel type, price snapshot when present, issue time, vehicle, and validated optional Trip/Driver association. Draft, pending, authorized-but-not-issued, and cancelled records are excluded.
- US-32: accepted purchase/price facts provide traceable price context where an issued record has a valid price snapshot. Purchase volume is procurement/stock input and is not treated as vehicle consumption.
- US-33/Fleet: validated vehicle odometer and engine-hour readings, reset epochs, coverage, and abnormal-reading status through published Tenant-scoped contracts.
- US-34: Fuel-owned trip cost and consumption allocation, including priced/unpriced completeness and trip distance status.
- US-36: bunker stock movements, dip readings, adjustments, and reconciliation facts remain stock evidence. They are not assigned to a vehicle or driver without an actual Fuel Issue reference.
- Fleet owns vehicle identity/type and supported measurement context; Trip owns trip-to-vehicle/driver attribution; Driver/Fleet owns driver identity. Fuel receives only minimal Tenant-scoped projections through published root contracts.

Fuel must not query Fleet, Driver, Trip, Reporting, or Organization repositories, entities, or tables. Cross-module IDs remain logical references. Analysis is read-only and must never modify Fuel Issue, Purchase, Price, Bunker, Trip Fuel Cost, Vehicle Reading/Odometer, Driver, Trip, Vehicle, or any source history row.

## Measurement modes and metric catalogue

Every result has one compatible `measurementMode`: `DISTANCE` or `ENGINE_HOURS`. Modes are never combined into one efficiency value or one peer ranking.

Distance-mode metrics:

- consumed liters;
- valid distance in kilometres;
- litres per kilometre = litres / kilometres;
- litres per 100 km = litres × 100 / kilometres;
- kilometres per litre = kilometres / litres;
- cost per kilometre = same-currency priced cost / kilometres;
- Fuel-owned cost per Trip where the existing US-34 calculation is complete.

Engine-hour-mode metrics:

- consumed litres;
- valid engine hours used;
- litres per engine hour = litres / hours;
- cost per engine hour = same-currency priced cost / hours.

Common metrics are issue/sample count, priced/unpriced count, valid/excluded quantity, baseline consumption rate, adverse variance percentage, trend buckets, and quality status. Actual-versus-rated efficiency is deferred because the accepted Vehicle model has no authoritative rated-efficiency fact. There is no manufacturer benchmark or FX conversion.

All quantities, distances, hours, money, ratios, and percentages use `BigDecimal`, never binary floating point. Source precision is retained during aggregation. API display scales are litres 3, km/hours 3, money 2, rates 3, and percentages 2, using `HALF_UP`. Litres, kilometres, hours, `L/100km`, `km/L`, `L/hour`, and ISO-4217 currency are explicit. Cost metrics are emitted only for one complete source currency; mixed, missing, or inconsistent currency makes cost metrics `null` with incomplete quality.

## Denominator and data-quality rules

No metric divides by zero. A zero/missing distance, zero/missing engine-hours delta, negative delta, reset crossing without valid epoch-aware coverage, invalid chronology, abnormal/tampered reading, missing association, unpriced issue for a cost metric, or incompatible unit is not silently converted to zero.

The fixed quality catalogue is:

- `COMPLETE`: all required numerator, denominator, attribution, unit, and relevant price/currency facts are valid.
- `PARTIAL`: some valid samples exist but one or more samples or optional cost facts are excluded.
- `INSUFFICIENT`: no valid denominator or fewer than the minimum baseline samples exist for the requested comparison.
- `INVALID_SOURCE_DATA`: the requested scope contains contradictory chronology, negative/reset-unsafe deltas, incompatible units, or an authoritative invalid/abnormal source marker that prevents a trustworthy aggregate.

Every response exposes the period, Tenant timezone, measurement mode, valid sample count, excluded sample count and reason counts, quality, baseline type/sample count/window, currency when applicable, and server `calculatedAt`. Exclusion is analytical only; source records are retained unchanged. Later owner-approved corrections appear on the next query.

## Windows, trends, and baseline

Supported presets are 7, 30, and 90 Tenant-calendar days. The default is the trailing 30 Tenant-calendar days. Custom ranges are inclusive of the local start date and exclusive of the local day after the end date, converted server-side with the authoritative Tenant timezone. A range must be ordered and may not exceed 365 days. Future dates beyond the current Tenant day and unbounded lifetime queries are rejected.

Trend grain is deterministic: daily for 7/30-day windows, weekly Tenant-calendar buckets for windows of 31–90 days, and monthly Tenant-calendar buckets for 91–365 days. A bucket needs at least one valid issued sample and a positive compatible denominator; otherwise it is returned as insufficient, never as zero. Percent change is `(current rate - comparison rate) / comparison rate × 100`; a missing or zero comparison produces `null`.

The v1 baseline is the same vehicle's immediately preceding, equal-length period in the same fuel type and measurement mode. It uses the aggregate consumption rate from valid facts and requires at least three valid issued samples and a positive denominator. If unavailable, the result is `INSUFFICIENT`; a peer baseline may be displayed separately but must not silently replace the historical baseline. Rated efficiency and driver historical baseline are deferred until authoritative supporting data and semantics exist.

Peer comparison is allowed only within the same Tenant, vehicle type, fuel type, and measurement mode. It requires at least three eligible vehicles with complete compatible results. Operational-context, project, and location comparison are deferred because no accepted authoritative analytics dimension currently exists. Comparisons show values and deviation context, not ordinal “best/worst” rankings.

## Driver attribution and privacy

A Fuel Issue contributes to driver performance only when its Driver is authoritative and, when a Trip is present, the accepted Trip projection confirms that the same Driver and Vehicle were assigned to that Trip. Missing, mismatched, or ambiguous associations are excluded with reason `UNATTRIBUTED`; they remain eligible for vehicle analysis when vehicle facts are valid. Driver denominators are aggregated only from attributable Trip/usage facts and are not guessed from vehicle totals.

Driver analysis is internal operational-review data. It requires `FUEL_PERFORMANCE_VIEW`, returns the minimal logical Driver ID and operational display label supplied by the published Driver/Fleet contract, and exposes supporting aggregate/sample facts. It exposes no phone, email, address, medical, drug-test, licence, payroll, or disciplinary data. There is no public leaderboard, ordinal rank, automated discipline, incentive, payroll, or billing effect. V1 adds no ABAC engine; same-Tenant permission checks and authoritative attribution are mandatory.

## Deterministic anomaly and leakage indicators

The v1 anomaly model is transparent and fixed:

- compare actual consumption intensity (`L/km` or `L/hour`) with the same-vehicle historical baseline;
- `adverseVariancePercent = (actualRate - baselineRate) / baselineRate × 100`;
- emit `EFFICIENCY_DEVIATION / REVIEW_REQUIRED` when adverse variance is at least 20.00%;
- emit `POSSIBLE_LEAKAGE_INDICATOR / REVIEW_REQUIRED` only when at least two consecutive valid trend buckets each have adverse variance of at least 30.00% against a valid historical baseline;
- no flag is emitted when baseline or source quality is insufficient/invalid.

Thresholds are Fuel-owned version-1 technical constants. They are not Tenant-configurable and require no generic analytics/rules engine or persistence. The UI/API must say `Anomaly`, `Deviation`, `Review required`, or `Possible leakage indicator`; it must not say theft, fraud, misuse, guilt, or culpability. US-37 never opens a US-38 or US-78 case, changes a transaction, blocks issue, disciplines a Driver, or publishes an event.

## Read model, performance, and integration

US-37 uses bounded, on-demand Fuel-owned query computation. It reads Fuel-owned indexed facts and obtains bulk, Tenant-scoped usage/vehicle/driver/trip dimensions through published provider-neutral contracts. Per-row foreign calls and N+1 queries are forbidden. Reporting consumes only the public Fuel root `FuelPerformanceQuery` projection and must not independently calculate Fuel KPIs.

The public Fuel-root contract is `FuelPerformanceQuery`, returning provider-neutral `FuelPerformanceSummary`, `VehicleFuelPerformance`, `DriverFuelPerformance`, and `FuelPerformanceTrend` values. The implementation may add minimal bulk source projections at the Fleet and Trip module roots, preserving their ownership and Tenant scope. No Reporting-owned adapter may access Fuel persistence.

No projection table, snapshot, cache, event, P1-01 durable delivery, or scheduled refresh is approved. `calculatedAt` records server calculation time; results reflect committed source state at query time. No analytics schema is required, but implementation will likely need a forward migration solely to seed `FUEL_PERFORMANCE_VIEW`; no version is reserved in this decision task. If measured performance later requires persistence or caching, it requires a new decision with Tenant-aware keys, source fingerprint/freshness, rebuild semantics, and a separate forward migration.

Fuel queries must use Tenant-leading indexed filters, bounded ranges, database aggregation/paging, and bulk cross-module lookups. Vehicle/driver lists default to 20 and cap at 100. An implementation acceptance fixture must cover at least 3 compatible vehicles, 3 attributed drivers, 2 fuel types, both measurement modes, 35 daily source observations, a preceding baseline window, at least 2 anomalous consecutive buckets, invalid/missing/reset data, 101 comparison rows for pagination, and a second Tenant.

## API, errors, and security

The external read-only API is:

- `GET /api/v1/fuel/performance/summary`
- `GET /api/v1/fuel/performance/vehicles`
- `GET /api/v1/fuel/performance/vehicles/{vehicleId}`
- `GET /api/v1/fuel/performance/drivers`
- `GET /api/v1/fuel/performance/drivers/{driverId}`
- `GET /api/v1/fuel/performance/trends`

Allowed filters are `preset` or bounded `from`/`to`, optional `vehicleId`, `driverId`, `vehicleTypeId`, `fuelType`, and `measurementMode`. Vehicle and Driver filters must resolve inside the current Tenant; foreign IDs return safe not-found. Allowed list sort fields are `consumptionRate`, `adverseVariancePercent`, `fuelQuantity`, `distanceKm`, `engineHours`, `cost`, and `sampleCount`, with stable ID tie-break. Arbitrary fields, SQL-style filters, project/location filters, and mass ID assignment are absent.

The single new capability is `FUEL_PERFORMANCE_VIEW`. Existing raw Fuel permissions do not implicitly grant personnel-performance analytics, and `REPORT_VIEW` does not authorize the Fuel API. Tenant authority comes only from `CurrentTenant` or a trusted worker context, never a request payload/query parameter. Tenant A cannot query, compare, export, infer, or view Tenant B analytics.

Stable errors are `FUEL_PERFORMANCE_INVALID_RANGE`, `FUEL_PERFORMANCE_INSUFFICIENT_DATA`, `FUEL_PERFORMANCE_UNSUPPORTED_MEASUREMENT`, `FUEL_PERFORMANCE_SOURCE_INVALID`, and `FUEL_PERFORMANCE_NOT_FOUND`, mapped through the existing standard error envelope. Insufficient aggregate cells normally return explicit quality plus `null` metrics; malformed range/mode is 400, unauthorized is 403, and unknown/cross-Tenant direct IDs are safe 404.

There are no POST, PUT, PATCH, DELETE, correction, approval, fraud, discipline, threshold-management, export, or raw-data mutation endpoints. Export is deferred; a future requirement must reuse a governed Reporting/export boundary rather than duplicate a CSV engine.

## Frontend decision

The existing Fuel feature gains a `/fuel/performance` operator page under the shared `AppLayout`; it does not duplicate navigation chrome, header, breadcrumb, or route title. The page contains a Tenant-timezone date selector, summary cards, an actual-versus-historical trend chart, a pageable vehicle comparison table, a pageable privacy-controlled Driver comparison table, quality/exclusion explanations, and non-punitive anomaly/review indicators.

Charts distinguish actual, baseline, adverse variance, and insufficient data. Missing values are gaps/`N/A`, never zero. Tables do not display rank numbers or “worst Driver” language. TanStack Query owns server state with Fuel feature query keys; the shared API client, React Hook Form/Zod conventions where applicable, Ant Design, and accessible selectors remain mandatory.

## Acceptance contract

Implementation must prove deterministic unit tests for both measurement modes, every denominator failure, reset/chronology handling, quality states, baseline and threshold boundaries, consecutive leakage indication, compatible peer selection, Driver attribution, currency/rounding, Tenant-timezone date boundaries, pagination/sort allow-lists, and source correction reflection.

Security/API tests must prove literal `/api/v1/...` routes, permission denial, Tenant A/B isolation, cross-Tenant Vehicle/Driver safe not-found, Driver privacy, invalid ranges/modes, absence of mass assignment, and absence of write/export routes. Architecture tests must prove no foreign repositories/entities/tables/SQL and Reporting consumption only through `FuelPerformanceQuery`.

PostgreSQL acceptance uses only `transport_logistics_acceptance`. The real PostgreSQL-backed Chromium journey must create/use accepted Fuel Issues, mileage/usage, and valid Vehicle/Trip/Driver context; display summary and period filtering; inspect vehicle and Driver results; prove the deterministic anomaly and insufficient-data states; deny Tenant B; and compare source Fuel facts before/after to prove exact immutability.

## Explicit exclusions

US-35 Fuel Cards are not required. Card metrics may extend the model only after US-35 acceptance. US-38 exception handling, theft/fraud classification, transaction correction, and operational action are not implemented. US-46 payroll and US-47 billing are untouched. US-82 platform analytics, predictive models, configurable rules, forecasts, external providers, P1-01 events, exports, persisted snapshots, caches, and unbounded history are deferred.

## Hard-gate result

All owner, source, immutability, metric, measurement, denominator, quality, window, baseline, peer, attribution, anomaly, language, threshold, trend, outlier, read-model, persistence, Reporting, Tenant, RBAC/privacy, API, frontend, numeric/unit, lineage, performance, test, E2E, US-35 independence, and scope-containment decisions are frozen. Scope leakage: NONE.
