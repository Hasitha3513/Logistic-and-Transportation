# US-50 Monitor Speed CS01 — Domain and Ports

**Task:** `US-50-MONITOR-SPEED-CS01-DOMAIN-PORTS-001`  
**Result:** COMPLETE  
**Domain owner:** Tracking  
**US-50:** `IMPLEMENTATION_IN_PROGRESS`  
**Accounting:** unchanged at 73 / 87 complete; 14 / 87 remaining  
**Flyway:** V80 (no migration; V81 remains reserved for CS02)  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`

## Implemented boundary

Tracking now owns a framework-free speed-monitoring domain using canonical decimal kilometres per hour.
`SpeedKph` accepts 0–400 km/h for observations and enforces a strictly positive threshold. `SpeedPosition`
models the frozen eligibility facts without persistence coupling: nonduplicate, trusted, Vehicle-associated,
in-order, immutable source time, known valid speed, and age no greater than five minutes.

`SpeedRule` implements Tenant and route-version scopes with `DRAFT`, `ACTIVE`, `DISABLED`, and terminal
`RETIRED` lifecycle states. Only draft or disabled rules are editable. Pure threshold resolution selects an
active same-Tenant route/version rule first, then the active Tenant fallback, and otherwise returns
configuration unavailable. The comparison has zero tolerance: only speed strictly greater than the
effective threshold is speeding.

`VehicleSpeedState` exposes only `UNKNOWN`, `NORMAL`, and `SPEEDING`. Pending confirmation remains internal.
The first eligible above-threshold position is silent; a second distinct consecutive eligible position under
the same effective rule ID/version confirms one episode. Continued speeding progresses that episode, while
one eligible position at or below threshold closes it. Ineligible, duplicate, and delayed positions cannot
rewind state. A rule-version change discards pending confirmation without synthesizing an episode.

`SpeedingEpisode` preserves nullable source-time Trip/Driver/route attribution, the first and confirming
position identities, effective threshold, maximum observed speed, counts, and episode chronology. A closed
same-Tenant/same-Vehicle/same-rule-version episode whose successor starts within the inclusive ten-minute
window produces `HIGH` severity and increments the repeat count; otherwise severity is `WARNING` and the
repeat count is zero. Episode identity is a deterministic SHA-256-derived UUID over Tenant, Vehicle, rule,
rule version, and first candidate position.

The published read-only Trip contract `VehicleTripAssignmentLookup.findAt(tenantId, vehicleId,
sourceTimestamp)` returns only Trip ID plus nullable Driver, route, and route-version attribution. Tracking
ports cover evaluation, rule management, queries, rule/state/episode/job persistence abstractions,
source-time attribution, and publication. `VehicleSpeedingDetectedV1` is only the minimized provider-neutral
publication-port payload in CS01; it excludes coordinates, provider/device credentials and person data.

## Verification

- Focused framework-free speed domain: 18 tests, 0 failures, 0 errors, 0 skipped.
- Complete Tracking Java suite: 201 tests, 0 failures, 0 errors, 0 skipped; PostgreSQL acceptance paths used
  `transport_logistics_acceptance` and migrated V1 through V80.
- Complete Trip Java suite: 101 tests, 0 failures, 0 errors, 0 skipped.
- Complete Maven verification: 1,613 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 11:08.
- Architecture and Modulith: 52 tests, 0 failures, 0 errors, 0 skipped.
- Checkstyle: 0 violations.
- PMD: `BUILD SUCCESS` after removing one unused CS01 helper parameter.
- SpotBugs: 0 bug instances and 0 errors.
- `git diff --check`: PASS.

The accepted database-backed evidence explicitly pinned both the test harness and Spring datasource to
`jdbc:postgresql://localhost:5433/transport_logistics_acceptance`. An earlier invalid environment attempt
resolved Spring to unavailable `localhost:5432`; it produced only ApplicationContext cascades and is not
accepted evidence. No development database was contacted by the accepted runs.

## Explicit exclusions

CS01 adds no V81 migration, table, JPA/JDBC adapter, REST API, security configuration, frontend, durable
event adapter, Notification consumer, scheduler, or worker. It makes no Driver mutation, changes no public
HTTP contract, does not advance story accounting, and does not alter US-48's external acceptance hold.

## Next controlled change set

`US-50-MONITOR-SPEED-CS02-V81-PERSISTENCE-001`
