# US-54 View Tracking Dashboard — CS02 Bounded Source Aggregation

## Verdict

`PASS — IMPLEMENTATION_IN_PROGRESS / CS02_COMPLETE`

CS02 implements the bounded, Tenant-qualified dashboard aggregation service, Tracking-owned source adapters and one published bulk Trip context contract. It introduces no HTTP endpoint, permission, migration or frontend behavior.

## Baseline and scope

- Starting commit: `946959cf3f90193e8969ed1d51b0733999cb1249`
- Flyway head: V93
- Accounting: 73 / 87 COMPLETE
- Governing decision: `docs/product-decisions/US-54-VIEW-TRACKING-DASHBOARD-PRODUCT-DECISIONS-001.md`

## Implemented behavior

- `TrackingDashboardQueryService` coordinates one bounded live-state page, one bulk Trip lookup and at most one query per authorized Tracking incident producer.
- Permission disclosure omits coordinates/accuracy, producer sections and Journey Replay affordance independently; omitted producer permissions leak no counts.
- Producer acceptance labels remain fixed: US-49 accepted, US-50 field-fidelity pending, US-48/52/53 field-acceptance pending and US-51 unavailable.
- Motion uses trusted observed speed only; density uses trusted LIVE/RECENT coordinates only and is capped at 100 deterministic cells.
- Incident queries are bounded to the previous 24 hours, 20 rows per producer and 50 rows total.
- `TrackingDashboardLiveStateAdapter` uses Redis live projections for newest candidates and a Tenant-scoped PostgreSQL batch query for authoritative trusted enrichment. PostgreSQL supplies a bounded, explicitly `DEGRADED` fallback when Redis is unavailable.
- Untrusted newest evidence remains `latestReceived` and never replaces `latestTrusted` or its map truth.
- `TripDashboardQuery` is the published Trip module contract. It accepts at most 100 Vehicle IDs and performs one Tenant-qualified source-time bulk lookup returning only Trip ID, lifecycle and route identity/version.
- Tracking consumes Trip only through `TripTrackingDashboardAdapter`; no Trip repository, JPA type or table leaks into Tracking.

## Security and data minimization

- Tenant identity is explicit on every new inbound, source and published cross-module operation.
- All PostgreSQL predicates lead with `tenant_id`; foreign-Tenant lookups return safe absence.
- No Driver, Customer, cargo, billing, payroll, provider/device, credential, signature, nonce, raw payload or review-note data is returned.
- CS02 creates no cache key, audit record, permission, event or public API.

## Verification

| Gate | Result |
| --- | --- |
| Focused domain/application/architecture | PASS — 68 / 68 |
| Real PostgreSQL dashboard/Trip acceptance | PASS — 7 / 7 against `transport_logistics_acceptance` |
| Complete acceptance-only Maven | PASS — 1,827 tests; 0 failures; 0 errors; 0 skipped; BUILD SUCCESS; 18:22 |
| Checkstyle | PASS — 0 violations; pre-existing warning debt retained |
| PMD | PASS after removing two unused imports and suppressing only the JDBC mapper row-number parameter |
| SpotBugs | PASS — 0 bug instances; 0 errors |
| Dependency analysis | PASS — BUILD SUCCESS; existing starter/transitive warnings retained |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

The complete Maven invocation pinned every application and test datasource variable to
`transport_logistics_acceptance` and disabled development sample-data and identity bootstraps.
No accepted CS02 evidence used the development database.

Frontend and Chromium were not rerun because CS02 exposes no controller, public route, DTO or UI behavior.

## Database and API impact

- Migration: none; Flyway remains V93 and V94 remains reserved for CS03 permission seeding.
- Schema/index changes: none.
- Public HTTP API: none.
- New published Java module contract: `TripDashboardQuery` only.

## Residual work

CS02 does not make the dashboard callable. Cursor authentication, use-case wiring, V94 permission seed,
literal-path RBAC, audit, rate limiting, OpenAPI and HTTP behavior belong to CS03.

Exact next queue:

`US-54-VIEW-TRACKING-DASHBOARD-CS03-V94-API-RBAC-AUDIT-001`
