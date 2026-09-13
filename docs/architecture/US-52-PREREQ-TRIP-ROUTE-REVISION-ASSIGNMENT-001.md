# US-52 Trip Route-Revision Assignment Prerequisite

**Task:** `US-52-PREREQ-TRIP-ROUTE-REVISION-ASSIGNMENT-001`
**Status:** COMPLETE
**Migration:** `V85__trip_assigned_route_revision.sql`

## Blocker and ownership

Trip previously persisted only `route_id`, so its source-time assignment provider could return no immutable
route revision without guessing. Routing remains owner of routes and revisions. Trip now owns and persists the
assignment snapshot. Tracking consumes only the published Trip result and receives no persistence access.

## Write and read paths

The existing client API continues to submit only `routeId`. Routing's published `RouteAssignmentLookup`
provider obtains the current authoritative `RouteRevision` and publishes canonical
`REVISION:<positive-integer>`. The Trip adapter validates route eligibility and returns that version to
`TripService`, which snapshots it on creation or route reassignment. A same-route retry returns the existing
Trip without another save or audit entry.

`VehicleTripAssignmentLookup.findAt(tenantId,vehicleId,sourceTimestamp)` reads only the persisted Trip-owned
snapshot. It neither queries Routing nor falls back to a newer revision. Explicit Tenant filtering preserves
safe foreign-Tenant absence.

## Schema and historical data

V85 adds nullable `trip.route_version VARCHAR(120)` and `chk_trip_route_version_assignment`. A non-null value
requires a non-null route and exact `REVISION:<positive-integer>` syntax. Historical nulls remain null and
readable; the migration performs no backfill, no Routing join and no destructive rewrite. V1–V84 are unchanged.

## Retained dormant US-52 baseline

The types introduced by commit `12cd870` remain dormant and unchanged as authorized. Later governed change
sets must reconcile these findings:

- detected/escalated root records expose Tracking-internal `DistanceMeters` and episode severity types;
- the evaluator currently requests escalation for an initially HIGH confirmation, while the frozen decision
  reserves escalation for a later WARNING-to-HIGH transition or rejected HIGH review;
- first-candidate reconstruction uses the confirming position's coordinate/accuracy because the candidate
  snapshot does not retain its own point and accuracy;
- repository/use-case/event surfaces exist but have no active persistence or publication adapters.

## Verification

- Focused Trip/Routing/domain tests: 43 tests, zero failures/errors/skips.
- Explicit V84→V85 PostgreSQL migration: PASS.
- Clean V1→V85, schema validation, constraint, lookup and Tenant tests: PASS.
- US-48/US-49/US-50 Tracking regression: 203 tests, zero failures/errors/skips.
- Architecture and Spring Modulith: 55 tests, zero failures/errors/skips.
- Complete Maven: 1,677 tests, zero failures/errors/skips; BUILD SUCCESS in 14:10.
- `git diff --check`: PASS.

## Impact, rollback and next step

There is no REST payload, frontend, permission, Tracking persistence or event-publication change. Application
rollback may deploy the prior version while leaving the nullable column and check in place. Removing V85 would
require a separately reviewed forward migration. Resume
`US-52-MONITOR-ROUTE-DEVIATIONS-CS01-DOMAIN-PORTS-001`; V86 is provisionally reserved for later Tracking
persistence after the mandatory head recheck.
