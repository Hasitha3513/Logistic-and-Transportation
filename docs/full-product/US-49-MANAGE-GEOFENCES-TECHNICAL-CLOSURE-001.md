# US-49 Manage Geofences — Technical Closure

## Executive result

`COMPLETE — IMPLEMENTATION_COMPLETE / READY_FOR_FINAL_ACCEPTANCE`

The frozen US-49 scope is implemented without a missing use case or silent expansion. CS01–CS07 and
CS07A are complete, V80 is the current Flyway head, no V81 exists, and independent final acceptance is the
only remaining US-49 gate. This closure does not accept the story or change the 72/87 accounting. US-48's
physical FMC130/Flespi acceptance hold remains separate and unchanged.

## Frozen scope and ownership

| Area | Closed contract | Result |
| --- | --- | --- |
| Owner | Tracking; Organization is reached only through Tenant-aware `LocationLookup` | PASS |
| Types | Exactly `DEPOT`, `CUSTOMER_SITE`, `UNAUTHORIZED_ZONE` | PASS |
| Geometry | Polygon-only WGS84, explicit longitude/latitude, open API ring, 3–100 distinct vertices, canonical closed persistence, 16 KiB bound, boundary INSIDE, pure Java; no PostGIS | PASS |
| Lifecycle | `DRAFT`, `ACTIVE`, `DISABLED`, terminal `RETIRED`; edits only in DRAFT/DISABLED; activation creates a new definition version and silent reinitialization; no delete | PASS |
| Location | DEPOT/CUSTOMER_SITE require a same-Tenant active Organization location; UNAUTHORIZED_ZONE is free-standing; Tracking stores only a logical UUID | PASS |
| Vehicle | All same-Tenant Vehicles are eligible; Tracking neither duplicates Vehicle ownership nor creates a cross-module FK | PASS |
| Position eligibility | Nonduplicate, TRUSTED, Vehicle-associated, valid WGS84, IN_ORDER and no more than five minutes old | PASS |
| Ordering | Source timestamp then position UUID; delayed/out-of-order data cannot rewind state | PASS |
| State | First observation initializes silently; two distinct consecutive observations confirm entry/exit; contradictory observations clear pending state; no dwell or distance buffer | PASS |
| Unauthorized | Confirmed entry is mandatory HIGH `UNAUTHORIZED_ZONE_ENTERED`; exit is normal EXITED; no discipline, Vehicle lock, Trip/payroll mutation or Operations case | PASS |
| Overlap | Independent evaluation, no priority suppression, unauthorized entry never suppressed | PASS |
| Transition identity | Deterministic SHA-256 identity over Tenant, geofence, definition version, Vehicle, from/to state and confirming position | PASS |

US-63 Delivery Zones remain a separate Delivery-owned bounded context and are not reused.

## CS01–CS07 traceability

| Change set | Technical result |
| --- | --- |
| CS01 | Pure geofence domain, inbound/outbound ports, eligibility, geometry and state-machine contracts complete |
| CS02 / V77 | Four Tracking-owned tables and Tenant-qualified JDBC persistence complete |
| CS03 | Atomic accepted-position job creation, evaluation, transitions and durable publication complete |
| CS04A / V78 | Exactly three permissions seeded with narrow administrative grants |
| CS04 | Bounded API family, dual-layer authorization, Tenant isolation, persistent idempotency and safe audit complete |
| CS05A / V79 | One active V1 IN_APP template and Tenant-scoped ROLE/DISPATCHER policy seeded |
| CS05 | Notification consumer, replay idempotency, failure isolation and minimized payload closure complete |
| CS06 | Approved existing-stack operator UI and real PostgreSQL-backed workflows complete |
| CS07A / V80 | Authorized due-job and active-bbox physical indexes plus numeric JDBC binding complete |
| CS07 | Three-run concurrency matrix, bounded worker/lease behavior, plans and performance complete |

## Persistence, evaluator and migrations

Tracking owns `tracking_geofence`, `tracking_vehicle_geofence_state`, `tracking_geofence_transition` and
`tracking_geofence_evaluation_job`. Repositories are Tenant-qualified; there are no foreign-module physical
FKs, cross-module SQL paths, or hard deletes. Transition UPDATE/DELETE protection preserves immutable
evidence. Eligible accepted positions and required jobs are transactionally coupled.

There is one feature-flagged coordinator, a bounded global claim, `FOR UPDATE SKIP LOCKED`, fixed bounded
workers, a bounded queue, owner-qualified leases and expired-work recovery. There is no scheduler or thread
per Tenant, Vehicle, Device, geofence or position. The advisory-lock-protected active limit is 500 per Tenant;
the 499+2 race ends at exactly 500.

- V77: geofence persistence.
- V78: permission seed.
- V79: Notification catalogue seed.
- V80: only `idx_tracking_geofence_job_global_due` and partial
  `idx_tracking_geofence_active_bbox_upper` index hardening.
- V81: absent.

Clean V1→V80 and V79→V80 pass. At about 5,000 definitions the bbox path uses an index-only scan and returns
10 candidates in 0.097–0.107 ms. At about 5,000 jobs the due claim uses its ordered index, returns the bounded
16 rows in 0.027–0.033 ms, and performs neither a full sequential scan nor a global explicit sort. Numeric
casts remain on bound parameters, not indexed bbox columns.

## API, security, idempotency and audit

The frozen `/api/v1/tracking/geofences` family provides create, bounded list/detail, update, activate,
disable/reactivate, retire, memberships, cursor-paged transitions and unauthorized transitions. There is no
DELETE. Definition/membership page sizes and transition limits are capped at 100.

Exactly `GEOFENCE_VIEW`, `GEOFENCE_MANAGE` and `GEOFENCE_EVENT_VIEW` exist. HTTP security and secured
use-case adapters both enforce these codes without role-name shortcuts. Tenant comes only from the trusted
context and is propagated into Tenant-qualified operations; cross-Tenant access is not-found-shaped and the
client cannot edit a tenant ID. Create, activate, disable and retire use persistent idempotency: equal replay
is safe, changed reuse conflicts, and the same key is independent across Tenants. Successful create, update,
activate, disable, retire and alert-policy mutations are audited without geometry/telemetry dumping; routine
evaluation does not create audit spam.

Safe domain mappings cover `GEOFENCE_NOT_FOUND`, `GEOFENCE_INVALID_GEOMETRY`, `GEOFENCE_NAME_CONFLICT`,
`GEOFENCE_LOCATION_NOT_FOUND`, `GEOFENCE_LOCATION_REQUIRED`, `GEOFENCE_LIFECYCLE_INVALID` and
`GEOFENCE_STALE_VERSION`, plus standard authentication, validation and idempotency errors. SQL/JDBC details
are not exposed.

## Event and Notification freeze

`VehicleGeofenceTransitionedV1`, version 1, is produced by Tracking and consumed only by Notification.
`eventId` is the transition UUID and `occurredAt` is source time. The payload is exactly `geofenceId`,
`vehicleId`, nullable `locationId`, `geofenceType`, `transition`, `severity`, `sourceTimestamp` and
`definitionVersion`; it excludes coordinates, polygon, Device/provider facts, Driver/Customer facts, raw
telemetry and credentials.

Configured normal ENTERED/EXITED transitions publish; unauthorized entry always publishes. Initialization,
pending hysteresis and stable packets do not publish. The existing P1-01 `DurableEventPublisher` and shared
outbox provide at-least-once delivery with producer and consumer idempotency; no feature outbox exists.
Notification alone owns rules, recipients, preferences, templates, delivery attempts and retry/history.
Tracking never mutates Notification persistence. No Operations or Driver-discipline integration exists.

## Frontend closure

The operator UI uses only React, React Router, Ant Design, TanStack Query, React Hook Form/Zod, Axios and
AuthContext. No Refine, Ant Design Pro Components, map/paid SDK or new frontend dependency was introduced.
It provides list/detail/create/edit/activate/disable/reactivate/retire, memberships, transition history and
unauthorized history. Exact permissions control affordances while the backend remains authoritative; direct
forbidden mutation has literal-URL 403 evidence.

Polygon editing is keyboard-operable for add/edit/remove/reorder, uses explicit longitude/latitude and an
open 3–100-vertex ring, and renders a local SVG preview. History exposes no coordinates, Device/provider
facts, Driver PII or Customer PII. No US-54 dashboard, heat/live map, US-78 action or disciplinary workflow
was added.

## Concurrency and performance evidence

The unchanged PostgreSQL matrix passed 31/31 three consecutive times, 93/93 combined. It covers duplicate
evaluation, first-state/confirmation races, delayed and equal-time UUID ordering, disable/retire both commit
orders, definition-version and stale-management races, the 499+2 limit, overlap, job claims, lease recovery,
stale-owner rejection, saturation/backlog recovery and Tenant concurrency. No flaky exception is accepted.

- Sustained: 1,287.7 msg/s (minimum 200).
- Burst: 1,442.4 msg/s (minimum 1,000).
- Latest p95: 10.5 ms (maximum 200 ms).
- History p95: 14.8 ms (maximum 500 ms).
- 500-active initialization: 822 ms, 2,581 ms and 917 ms; 500 states and zero transition/outbox events each.

## Quality and independent closure rerun

Latest complete evidence remains: Tracking 183/183, Notification 164/164, security/Tenant/privacy/RBAC
152/152, Maven 1,595/0/0/15 in 10:44, architecture 52/52, Vitest 299/299, Chromium 7/7, TypeScript/build,
Checkstyle, PMD and SpotBugs all PASS. Global ESLint remains 71 unrelated pre-existing Delivery errors;
US-49 added zero.

This closure independently reran the six focused PostgreSQL/API/security/Notification classes: 40/40 PASS
against only `transport_logistics_acceptance`, including clean V1→V80. Architecture/Modulith reran 52/52
PASS. Focused geofence Vitest reran 8/8 PASS. Two excluded setup invocations failed before test logic: one
could not use the sandboxed Docker socket, and one used the wrong local PostgreSQL role. Neither contacted
the development database and neither is product evidence. The corrected repository-defined local acceptance
path passed without application changes.

## Remaining-gap classification and acceptance readiness

| Classification | Result |
| --- | --- |
| Implementation defect | NONE |
| Product decision required | NONE |
| External dependency | NONE for US-49 |
| Documentation only | Closed by this package and synchronized records |
| Final-acceptance evidence only | Independent hostile acceptance remains |

`FINAL_ACCEPTANCE_READINESS = READY_FOR_INDEPENDENT_FINAL_ACCEPTANCE`. US-49 does not inherit US-48
acceptance; it relies on the frozen technical Tracking contracts while US-48 remains externally held.

Final acceptance must independently inspect and execute representative source traceability, lifecycle,
PostgreSQL persistence, signed position ingestion, silent initialization, two-position confirmation,
unauthorized HIGH publication, durable Notification, Tenant/RBAC denial, operator UI/privacy, concurrency,
performance, V80 migration state and absence of US-48 acceptance inheritance. It must not merely restate this
closure report.

## Next task

`US-49-MANAGE-GEOFENCES-FINAL-ACCEPTANCE-001`
