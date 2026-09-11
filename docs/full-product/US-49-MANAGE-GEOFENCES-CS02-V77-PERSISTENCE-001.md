# US-49 Manage Geofences CS02 — V77 Persistence

**Task:** `US-49-MANAGE-GEOFENCES-CS02-V77-PERSISTENCE-001`  
**Result:** COMPLETE  
**US-49:** `IMPLEMENTATION_IN_PROGRESS`  
**Flyway:** V77  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`

## V77 schema

`V77__us49_geofence_persistence.sql` creates exactly four Tracking-owned tables:

- `tracking_geofence` stores Tenant-scoped definitions, canonical closed-ring JSONB, domain-derived
  bounding boxes, optional logical Organization location UUID, alert flags, lifecycle, optimistic version
  and audit facts. It enforces Tenant/name and Tenant/ID uniqueness, frozen type/lifecycle/location rules,
  nonblank trimmed names, valid bounding-box ranges, mandatory unauthorized-entry alerts, and a 16 KiB
  JSONB polygon limit.
- `tracking_vehicle_geofence_state` stores one current membership state per
  Tenant/geofence/Vehicle, including pending confirmation, last evaluated source ordering and optimistic
  version. Its geofence relationship is a same-module composite Tenant foreign key; Vehicle and optional
  position references remain logical.
- `tracking_geofence_transition` stores immutable transition evidence with deterministic UUID identity,
  Tenant-leading uniqueness, definition version, source time and confirming Tracking position. It has no
  coordinate, geometry, provider, device, Driver, Customer or raw-payload column. A database trigger rejects
  update and delete operations.
- `tracking_geofence_evaluation_job` is an idempotent durable queue keyed by Tenant/position. It enforces
  exact statuses, nonnegative attempts and all-null/all-present lease pairs, with a same-module composite
  Tracking-position foreign key and a Tenant-leading due-job index.

No PostGIS extension, cross-module foreign key, permission, API, event/outbox row or evaluator wiring was
introduced. V1–V76 are unchanged.

## JDBC adapters

The Tracking outbound adapters implement all CS01 persistence ports using the existing `JdbcTemplate` and
`TransactionTemplate` conventions. Every entity-specific read/write uses Tenant-qualified predicates.
Definitions and current state use optimistic version predicates; definition/state row-lock operations are
available for CS03. The active-count primitive uses a Tenant-keyed transaction advisory lock so the later
500-active activation rule can be enforced atomically without a database business trigger.

Polygon persistence serializes only the domain-validated canonical closed ring, enforces the UTF-8 size
bound before writing, derives bounding boxes from the polygon, and removes exactly one closing vertex when
reconstituting the domain object. Transition insertion is idempotent through deterministic Tenant identity,
and history queries use deterministic source-time/UUID ordering with bounded pages.

Evaluation jobs support idempotent enqueue, bounded `FOR UPDATE SKIP LOCKED` claim, renewal, release, retry,
completion and expired-lease reclaim. All owner mutations use owner-safe predicates, so an old owner cannot
overwrite a reclaimed job. CS02 deliberately does not start a scheduler or enqueue from Tracking ingestion.

## Verification

- V77 PostgreSQL acceptance: 10 tests, 0 failures, 0 errors, 0 skipped.
- V1→V77 and V76→V77: PASS on `transport_logistics_acceptance`.
- Complete Tracking Java: 146 tests, 0 failures, 0 errors, 0 skipped.
- Former global-head regression group: 32 tests, 0 failures, 0 errors, 0 skipped.
- Complete Maven verification: 1,552 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 09:26.
- Architecture and Modulith: 52 tests, 0 failures, 0 errors, 0 skipped.
- Checkstyle: 0 violations.
- PMD: `BUILD SUCCESS`.
- SpotBugs: 0 bug instances and 0 errors.
- Development database authoritative evidence: NO.
- `git diff --check`: PASS.

The first full Maven run found only nine assertions that still named V76 as the global repository head.
Those assertions were advanced to V77 and the exact 32-test group passed before the complete rerun. No
historical migration was edited or repaired.

## Explicit exclusions

CS02 implements no evaluation orchestration, ingestion enqueue wiring, REST API, web DTO, security seed,
frontend, Notification consumer, durable event adapter, outbox publication, or US-50–55 behavior.

## Next controlled change set

`US-49-MANAGE-GEOFENCES-CS03-EVALUATION-TRANSITIONS-001`
