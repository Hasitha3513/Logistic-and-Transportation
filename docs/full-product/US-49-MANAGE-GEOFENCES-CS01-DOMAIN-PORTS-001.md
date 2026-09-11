# US-49 Manage Geofences CS01 — Domain and Ports

**Task:** `US-49-MANAGE-GEOFENCES-CS01-DOMAIN-PORTS-001`  
**Result:** COMPLETE  
**Domain owner:** Tracking  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining  
**Flyway:** V76 (no migration; V77 remains reserved for CS02)  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`

## Implemented boundary

Tracking now owns a framework-free geofence domain model for immutable WGS84 polygons, lifecycle and
alert policy, position eligibility, per-Vehicle/per-Geofence runtime state, deterministic transitions,
and evaluation-job lifecycle. Supported types are `DEPOT`, `CUSTOMER_SITE`, and
`UNAUTHORIZED_ZONE`. Geometry enforces 3–100 distinct finite vertices, WGS84 ranges, no consecutive
duplicates, non-zero area, and no self-intersection. Boundary points are inside and point-in-polygon is
implemented with pure Java rather than PostGIS.

The aggregate permits definition changes only while `DRAFT` or `DISABLED`, enforces the frozen lifecycle,
makes `RETIRED` terminal, requires active same-Tenant Organization locations for depot/customer-site
activation, and makes unauthorized-zone entry alerts mandatory. Runtime state is separate from the
aggregate. The first eligible observation is silent; a transition requires two distinct confirming
positions; duplicates do not confirm; contradictory positions reset the pending candidate; and overlaps
are evaluated independently. Delayed or equal-source-time lower-UUID positions cannot rewind state.
Definition-version changes reset evaluation silently.

Transition identity is deterministically SHA-256-derived from the frozen canonical facts. Unauthorized
entry is `HIGH` and alert-required; exits remain normal. Provider-neutral inbound and outbound ports cover
management, querying, evaluation, persistence, Organization location lookup, durable evaluation jobs, and
transition publication. The publication contract is a port payload only—no Notification implementation or
durable event adapter was introduced.

Organization's published `LocationLookup` retains its existing consumer-compatible operation and adds an
explicit `(tenantId, locationId)` overload backed by a tenant-scoped repository query. Contract tests prove
the explicit path propagates the requested Tenant and cannot fall back to an unscoped identity lookup.

## Verification

- Focused remediation and US-49 domain/lookup tests: 37 tests, 0 failures, 0 errors, 0 skipped.
- Complete PostgreSQL-backed Tracking selection: 136 tests, 0 failures, 0 errors, 0 skipped; V1 through V76
  on `transport_logistics_acceptance` only.
- Complete Maven verification: 1,542 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 08:40.
- Architecture and Modulith: 52 tests, 0 failures, 0 errors, 0 skipped.
- Checkstyle: 0 violations.
- PMD: `BUILD SUCCESS`.
- SpotBugs: 0 bug instances and 0 errors.
- `git diff --check`: PASS.

The first complete Maven attempt exposed 10 compatibility errors after existing Location consumers were
made dependent on ambient Tenant context. The smallest remediation preserved the original operation and
added the explicit tenant-scoped overload. The exact failing freight group plus US-49 focused tests passed
37/37 before the complete 1,542-test rerun succeeded. Initial Tracking attempts affected only test
infrastructure (Docker API compatibility and an unpinned Spring datasource); accepted evidence uses only
`transport_logistics_acceptance`.

## Explicit exclusions

CS01 adds no database objects, V77 migration, persistence adapters, REST API, controller, frontend,
permission, Notification implementation, or durable transition-event adapter. It does not change US-48's
external acceptance hold, complete US-49, or change story accounting.

## Next controlled change set

`US-49-MANAGE-GEOFENCES-CS02-V77-PERSISTENCE-001`
