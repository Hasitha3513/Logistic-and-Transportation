# US-53 Trip Published Replay Query Contracts

## Verdict

`COMPLETE`. Trip now publishes the minimum framework-neutral, Tenant-qualified query surface required by
Journey Replay. Flyway remains V92; V93 remains unused and reserved for US-53 permissions.

## Gap and ownership

The previous `VehicleTripAssignmentLookup.findAt` contract could require one cross-module query per replay
point. The new `TripReplayQuery` remains owned and implemented by Trip. Tracking can resolve one replay scope
and obtain all assignment intervals for one bounded Vehicle/range without accessing Trip entities, repositories
or tables. There is no Trip-to-Tracking dependency and no Routing latest-revision lookup.

## Published contracts

`findReplayScope(tenantId, tripId)` returns only Trip ID, Vehicle ID, actual start/end, lifecycle status, route
ID and exact stored immutable route version. Foreign-Tenant, nonexistent, unstarted or unassigned Trips return
the same safe absence.

`findAssignmentsOverlapping(tenantId, vehicleId, rangeStart, rangeEnd)` uses the half-open requested range and
returns every authoritative interval satisfying `actual_start_time < rangeEnd` and
`actual_end_time IS NULL OR actual_end_time > rangeStart`. Cancelled and rejected Trips are excluded according
to the existing lifecycle contract. Results are ordered by effective start ASC and Trip ID ASC; overlaps remain
unresolved for Tracking to classify.

## Boundedness and safety

- The requested range must be nonempty and no longer than seven days.
- One parameterized database query retrieves assignment intervals with `LIMIT 2001`.
- Up to 2,000 intervals are accepted; row 2,001 produces
  `TRIP_ASSIGNMENT_RESULT_LIMIT_EXCEEDED` with no partial result or fallback.
- Published values exclude Driver/Customer data, geometry, coordinates and operational notes.
- Ordinary errors contain no Tenant, Vehicle, Trip, route or timestamp values.

## PostgreSQL evidence

The acceptance fixture migrated cleanly through V92 and verified scope resolution, nullable active end,
Tenant/Vehicle isolation, lifecycle filtering, half-open boundaries, overlap preservation and stable tie order.
After representative statistics were collected, `EXPLAIN (ANALYZE, BUFFERS)` selected
`idx_trip_tenant_vehicle_source_assignment`; the asserted plan contained no `Seq Scan on trip`. The existing
V92 partial covering index therefore supports the query and no migration is required.

## Verification

- Service/contract boundary: 5 tests PASS, including exactly 2,000 and 2,001 overflow.
- PostgreSQL acceptance: 4 tests PASS in 29.34 seconds; Maven BUILD SUCCESS in 46.386 seconds.
- Affected Trip/Tracking/Routing and architecture selection: 88 tests PASS in 45.316 seconds.
- Complete clean Maven: 1,777 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS in 10:55.
- Checkstyle: 0 violations. PMD: BUILD SUCCESS after correcting two task-local import findings.
- SpotBugs: 0 findings. Dependency analysis: BUILD SUCCESS with the repository's existing aggregate
  starter/transitive-dependency warnings and no task-added dependency.
- Docker Compose configuration and `git diff --check`: PASS.

## Rollback

Rollback is application-only: remove the published interface, service, repository port, JDBC adapter and bean
wiring. No schema or data rollback is necessary. V1-V92 remain immutable.
