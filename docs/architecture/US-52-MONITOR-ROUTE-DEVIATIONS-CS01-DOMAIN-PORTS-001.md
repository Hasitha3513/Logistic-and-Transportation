# US-52 Route-Deviation CS01 Domain and Published Contracts

**Task:** `US-52-MONITOR-ROUTE-DEVIATIONS-CS01-DOMAIN-PORTS-001`
**Status:** COMPLETE
**Accounting:** unchanged at 73/87 complete
**Flyway head:** V85; no migration in CS01

## Frozen-decision traceability

Routing owns immutable route definitions, revisions and geometry; Trip owns the assigned revision at source
time; Tracking owns comparison, stable state, candidates, episodes and review evidence. Every lookup is
Tenant-explicit. Tracking has no Routing repository, entity, SQL, Organization-location or latest-revision
access. No REST, frontend, permission, event activation or persistence change is part of CS01.

## Retained and corrected baseline

The dormant `12cd870` baseline supplied the root `PlannedRouteGeometryLookup`, `PlannedRouteGeometry` and
`Wgs84Point` records, Tracking route-deviation values, deterministic local tangent-plane distance calculator,
evaluator, state/episode/review models, and future ports. CS01 retains those types and keeps the future ports
inactive.

CS01 completes published geometry validation with a defensive immutable 2–2,000 point list, exact canonical
`REVISION:<positive-integer>` identity, longitude/latitude ordering, decimal preservation, WGS84 ranges,
adjacent-point uniqueness, antimeridian rejection and a 25 km maximum segment. Tracking's route version now
accepts the same bounded arbitrary-size positive integer syntax as Trip rather than imposing an incompatible
32-bit limit. Truthful availability distinguishes missing Trip, route, revision, geometry/rule, unknown or
excessive accuracy, stale/out-of-order/untrusted/invalid positions, and provider/configuration failure.

## Published Routing provider

`PlannedRouteGeometryLookup.find(tenantId, routeId, routeVersion)` validates explicit Tenant, route and exact
revision inputs. Current `RouteRevision` persistence contains origin/destination and ordered stop location IDs,
but no immutable revision coordinate snapshot. The CS01 provider therefore returns empty for every valid exact
revision. It never queries Organization, synthesizes an endpoint chord, or substitutes the latest revision.
CS02 must add Routing-owned immutable geometry persistence before authoritative geometry can be returned.

## Tracking domain policies

The retained framework-neutral foundation enforces tolerance 10–5,000 metres; eligible accuracy 0–1,000
metres; exact configured-tolerance-plus-accuracy calculation; inclusive inside boundary; WARNING above the
boundary through twice tolerance; HIGH strictly above twice tolerance; and non-downgrading open severity.
`RouteDistanceCalculator` deterministically calculates the minimum clamped point-to-segment distance across
the complete polyline using the approved bounded local tangent-plane projection.

Historical Trip rows with null route revision remain `UNKNOWN` with `NO_ROUTE_REVISION`; they cannot be
reported as on-route. No evaluator workflow or adapter was activated in this change set.

## Architecture evidence and deferred findings

Architecture enforcement proves the Tracking route-deviation domain/ports are framework-neutral, Tracking
imports no Routing internal package, and Routing's root-published types are not persistence entities.

The following retained findings remain assigned to later governed change sets:

- root detected/escalated records expose Tracking-internal distance/severity types (event integration CS05);
- evaluator workflow activation and rejected-review escalation remain CS03/CS05 work;
- first-candidate coordinate/accuracy reconstruction remains a CS03 correction before activation;
- repositories, use cases and event publisher ports remain dormant until their owning persistence/application
  change sets.

## Verification

- CS01 domain/contract/architecture focus: 21/21 PASS in 19.163 seconds.
- Trip/Routing and US-48, US-49 and US-50 relevant regression selection: 250/250 substantive PASS;
  228 passed together and the 22-test Tracking security class passed separately against its configured
  PostgreSQL port after the initial mixed command incorrectly targeted inactive port 5432.
- Architecture and Spring Modulith suite: 58/58 PASS in 26.715 seconds.
- Complete Java 21 Maven suite: 1,686 tests, zero failures/errors/skips; BUILD SUCCESS in 09:01.
- `git diff --check`: PASS.

## Impact and rollback

Flyway impact is none; V1–V85 are unchanged and V86 remains provisionally reserved for CS02 after a fresh
head check. REST API, frontend, permissions, dependencies and public HTTP coordinate exposure are unchanged.
Rollback removes the additive provider bean/validation corrections and focused tests/documentation; no data
rollback is required.

## Next change set

`US-52-MONITOR-ROUTE-DEVIATIONS-CS02-V86-PERSISTENCE-001` must add the smallest Routing-owned immutable
revision geometry and Tracking-owned route-deviation persistence, only after reconfirming V86 is free.
