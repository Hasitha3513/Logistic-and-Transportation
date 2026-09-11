# US-49 Manage Geofences Product Decisions

**Task:** `US-49-MANAGE-GEOFENCES-PRODUCT-DECISIONS-001`  
**Decision:** APPROVED / FROZEN  
**Owner:** Tracking bounded context  
**Current Flyway:** V76; no migration created  
**Migration likely:** YES; V77 is reserved for implementation  
**Accounting:** unchanged at 72 / 87 complete

## Source scope and ownership

US-49 lets a Tracking/Control Room Operator define depot, customer-site and unauthorized-zone
geofences, reject invalid definitions, and record meaningful Vehicle entry/exit transitions with
configured alerts. Tracking owns the `Geofence` definition, point evaluation, current Vehicle/geofence
state, transition history and unauthorized-entry classification.

Organization continues to own locations/sites. A geofence may hold one optional logical Organization
location UUID and validate it through a published same-Tenant active-location lookup; it never copies a
site/customer/depot record, queries Organization persistence or creates a cross-module foreign key. The
existing `LocationLookup` must evolve during implementation to take explicit `tenantId` plus `locationId`
because background work cannot depend on an ambient web Tenant. The geofence type expresses the Tracking
purpose; Organization need not acquire a duplicated geofence classification.

US-63 Delivery Zones remain Delivery-owned serviceability, workload, priority, override and last-mile
capacity objects. US-49 geofences are telemetry boundaries and transition detectors. Their tables, APIs,
lifecycles and events are separate. Delivery persistence and domain geometry types are not imported.
A small pure WGS84 polygon algorithm may be independently implemented inside Tracking; extracting a
shared cross-cutting geometry utility is optional only if it preserves both domain contracts and does not
turn `shared` into geospatial business ownership.

## Geofence definition

The Phase 1 aggregate root is `Geofence` with UUID identity, explicit Tenant, Tenant-unique trimmed name,
type, polygon, optional logical Organization location ID, alert configuration, lifecycle, audit timestamps
and actors, and optimistic version.

Supported types are exactly:

- `DEPOT` — requires one active same-Tenant Organization location reference.
- `CUSTOMER_SITE` — requires one active same-Tenant Organization location reference.
- `UNAUTHORIZED_ZONE` — must be free-standing; entering creates a high-severity unauthorized transition.

`AUTHORIZED_ZONE` and `CUSTOM` are not source-required and are excluded. A Tenant may define multiple
geofences for one logical location when operational boundaries differ; names remain unique. Every
geofence applies to all eligible Vehicles in its Tenant. Vehicle/category/route/trip selectors and a
generic rule-expression engine are excluded.

### Geometry

Phase 1 supports polygon only. Circle, ellipse, multipolygon, holes and generic GeoJSON are excluded.
Coordinates are WGS84 decimal `(longitude, latitude)` pairs. The API accepts an open ring of 3–100
distinct vertices; storage uses a canonical closed ring by appending the first vertex. The client must
not submit the closing vertex. Latitude is `[-90,90]`, longitude is `[-180,180]`; values must be finite.
Consecutive duplicate vertices, fewer than three distinct vertices, zero-area rings and self-intersection
are rejected. Orientation is semantically irrelevant and is preserved. A point exactly on an edge or
vertex is INSIDE.

The serialized polygon is capped at 16 KiB. PostgreSQL stores the validated canonical vertices as bounded
JSONB plus numeric min/max latitude/longitude columns. Bounding-box filtering precedes pure Java
point-in-polygon evaluation. PostGIS is not approved: existing US-48 and US-63 use ordinary PostgreSQL and
application geometry, the bounded 100-vertex/500-active-geofence Phase 1 model does not justify a new
deployment dependency, and correctness/performance must be measured before escalation.

### Lifecycle and versioning

Lifecycle is `DRAFT -> ACTIVE <-> DISABLED -> RETIRED`; RETIRED is terminal. Create produces DRAFT.
Activation requires valid geometry, valid required same-Tenant active location reference and complete
alert policy. Geometry, type, site link and alert policy may be edited only while DRAFT or DISABLED.
Disable stops new evaluation/publication while retaining definition, current-state snapshot and history.
Reactivation starts a new definition version and silently reinitializes membership from the next eligible
position. Retirement permanently stops evaluation and preserves all evidence.

Separate effective dates are not included. Lifecycle/audit timestamps plus the definition version stored
on each transition provide the required temporal evidence without speculative scheduled activation or
full definition-version tables. Management audit records the before/after version and safe reason.

## Evaluation and transition semantics

Definition rows never receive per-packet updates. Runtime state is a separate
`VehicleGeofenceState`; `GeofenceTransition` is immutable business evidence.

Only a non-duplicate US-48 observation that is `TRUSTED`, belongs to a Vehicle, contains valid WGS84
coordinates, is not older than five minutes at evaluation, and has ordering `IN_ORDER` is eligible to
change current geofence state. Untrusted, invalid, stale, late, future-skew and out-of-order observations
remain in US-48 history but create no current US-49 transition. Delayed facts never rewind state.
Evaluation ordering uses `sourceTimestamp`, with position UUID as the deterministic tie-breaker; receipt
time is operational metadata only.

The first eligible observation for each Vehicle/geofence/definition-version initializes INSIDE or
OUTSIDE silently. It does not emit a synthetic ENTERED/EXITED event. Thereafter:

- stable OUTSIDE plus two distinct consecutive eligible INSIDE observations produces one `ENTERED`;
- stable INSIDE plus two distinct consecutive eligible OUTSIDE observations produces one `EXITED`;
- an observation matching stable state clears the opposite pending candidate;
- repeated/duplicate positions do not increment confirmation count or emit events.

This two-distinct-position confirmation is the Phase 1 hysteresis rule. There is no distance buffer or
time dwell. Boundary points count as INSIDE. Dwell duration/events are not source-required and are out of
scope.

For `UNAUTHORIZED_ZONE`, the confirmed `ENTERED` transition is additionally classified
`UNAUTHORIZED_ZONE_ENTERED`, severity `HIGH`, and always alert-enabled. Exit records `EXITED` and clears
current membership but does not erase or close prior evidence. Tracking performs no Driver discipline,
Vehicle immobilization or automatic operational correction.

Overlapping geofences are allowed and evaluated independently. There is no priority or one-winner rule.
An unauthorized zone remains unauthorized even when it overlaps a depot or customer-site geofence; the
most restrictive outcome therefore arises from independent facts rather than mutable precedence.

## Reliability and concurrency

Accepted eligible positions enqueue a Tracking-owned durable evaluation job atomically with position
acceptance; no cross-module event is emitted per packet. Bounded Tracking workers claim jobs with
PostgreSQL locking, evaluate a bounded maximum of 500 ACTIVE geofences per Tenant, and update
Vehicle/geofence state transactionally. Jobs are idempotent by Tenant/position and may retry safely.
Definition lifecycle/version is locked and revalidated in the evaluation transaction, so disable or
retire wins before publication when committed first; evaluation committed first retains its valid
transition. Geometry changes require DISABLED state and a new version, preventing an active in-place race.

Transition identity is deterministic SHA-256 over Tenant, geofence ID, definition version, Vehicle ID,
from/to state and confirming position ID. A Tenant-leading unique constraint makes duplicate/concurrent
evaluation idempotent. State updates use a Tenant/geofence/Vehicle lock plus optimistic version. Two
overlapping geofences create independent states/transitions.

The future deterministic PostgreSQL race matrix must prove:

1. concurrent duplicate position evaluation;
2. enter confirmation versus disable;
3. enter confirmation versus retire;
4. delayed outside after newer inside;
5. definition update/activation versus evaluation;
6. duplicate unauthorized transition/publication;
7. two overlapping geofences for one Vehicle;
8. stale management version.

## Events and integrations

Tracking publishes no per-position Spring or durable integration event. Only a committed transition may
publish `VehicleGeofenceTransitionedV1` through the existing P1-01 durable publisher for consumer
`NOTIFICATION` when its alert policy is enabled; unauthorized entry is always enabled. Delivery is
at-least-once, consumer idempotency uses the transition UUID/event UUID, and there is no global ordering
promise.

The version-1 minimized payload is:

```json
{
  "geofenceId": "uuid",
  "vehicleId": "uuid",
  "locationId": "uuid-or-null",
  "geofenceType": "DEPOT|CUSTOMER_SITE|UNAUTHORIZED_ZONE",
  "transition": "ENTERED|EXITED|UNAUTHORIZED_ZONE_ENTERED",
  "severity": "NORMAL|HIGH",
  "sourceTimestamp": "UTC instant",
  "definitionVersion": 1
}
```

The canonical P1-01 envelope supplies `eventId` (the transition UUID), `tenantId`, event type/version,
producer `TRACKING`, occurred time equal to source timestamp, and consumer `NOTIFICATION`. It contains no
coordinates, device/provider identity, Driver/Customer identity, geometry, address or raw telemetry.
Notification owns recipient/rule/channel/template/delivery/retry history. Tracking never sends Email/SMS.

US-49 does not publish `OperationalExceptionFactV1`. The authoritative traceability model maps US-49 to
`Geofence` and `TrackingAlert`, while US-55 owns GPS exception lifecycle and integration with US-78.
Unauthorized transitions remain immutable Tracking evidence and notifications; Operations integration
requires a later explicitly approved source story, preventing duplicate exception workflow here.

## Security, privacy, audit and retention

New permissions are exactly `GEOFENCE_VIEW`, `GEOFENCE_MANAGE` and `GEOFENCE_EVENT_VIEW`.
`GEOFENCE_MANAGE` guards create/update/lifecycle actions; `GEOFENCE_VIEW` guards definitions and current
memberships; `GEOFENCE_EVENT_VIEW` guards transition/unauthorized history. Backend permission checks are
authoritative. Tenant context comes only from authenticated active membership and is propagated through
definitions, state, jobs, transitions, repositories, events and caches. Cross-Tenant resources return the
existing not-found-shaped denial.

No new generic ABAC engine is approved. Tenant isolation is mandatory; branch/site scoping is deferred
until an authoritative membership scope exists. Precise transition coordinates are not exposed or copied
into US-49 events/history. Authorized users may follow the logical position ID through existing
`TRACKING_HISTORY_VIEW` when exact location is operationally necessary. APIs expose masked/minimized
Vehicle, geofence, transition, source-time and definition-version facts only.

Create, update, activate, disable, retire and alert-policy changes use the existing safe audit convention.
Telemetry evaluations are not audit events. Transition history is append-only business evidence and is
retained under external Tracking retention policy; no automatic duration is invented. Disabling or
retiring a geofence never deletes states or transitions. Current state is rebuildable from retained
eligible positions plus definition versions; no public rebuild endpoint is exposed.

## Proposed API

All routes are under `/api/v1/tracking/geofences`; implementation must use web DTOs/mappers and preserve
standard errors. UUIDs are internal logical IDs and physical/provider device identity is absent.

| Method and route | Permission | Contract |
| :--- | :--- | :--- |
| `POST /geofences` | `GEOFENCE_MANAGE` | Create DRAFT from name, type, optional locationId, polygon vertices and alert flags; 201 |
| `GET /geofences` | `GEOFENCE_VIEW` | Tenant page; filters type/lifecycle/locationId; page 0+, size 1–100 |
| `GET /geofences/{id}` | `GEOFENCE_VIEW` | Definition plus safe current counts and version |
| `PUT /geofences/{id}` | `GEOFENCE_MANAGE` | Replace editable DRAFT/DISABLED fields with expected version |
| `POST /geofences/{id}/activate` | `GEOFENCE_MANAGE` | Expected version; validate geometry/site/policy |
| `POST /geofences/{id}/disable` | `GEOFENCE_MANAGE` | Expected version and required reason |
| `POST /geofences/{id}/retire` | `GEOFENCE_MANAGE` | Expected version and required reason; terminal |
| `GET /geofences/memberships` | `GEOFENCE_VIEW` | Current memberships filtered by vehicleId/geofenceId; bounded page |
| `GET /geofences/transitions` | `GEOFENCE_EVENT_VIEW` | History filtered by geofenceId/vehicleId/type/source-time; cursor page up to 100 |
| `GET /geofences/unauthorized-transitions` | `GEOFENCE_EVENT_VIEW` | Minimized unauthorized-entry history; cursor page up to 100 |

Errors are `GEOFENCE_NOT_FOUND`, `GEOFENCE_INVALID_GEOMETRY`, `GEOFENCE_NAME_CONFLICT`,
`GEOFENCE_LOCATION_NOT_FOUND`, `GEOFENCE_LOCATION_REQUIRED`, `GEOFENCE_LIFECYCLE_INVALID`,
`GEOFENCE_STALE_VERSION` and standard validation/authorization errors. Cross-Tenant identity is never
distinguished from not found. Idempotency-Key is required for create and lifecycle commands; conflicting
reuse fails safely.

## Proposed persistence and indexes

V77 is likely and may create only Tracking-owned tables:

- `tracking_geofence`: definition, bounded polygon JSONB, bounding box, optional logical Organization
  location UUID, lifecycle/alerts/version/audit; unique `(tenant_id,name)` and `(tenant_id,id)`.
- `tracking_vehicle_geofence_state`: current stable/pending state per Tenant/geofence/Vehicle and last
  evaluated position/source/version; unique `(tenant_id,geofence_id,vehicle_id)`.
- `tracking_geofence_transition`: append-only definition/version/Vehicle/from/to/classification/severity,
  source time, logical Tracking position ID and deterministic identity; no copied coordinates.
- `tracking_geofence_evaluation_job`: transient durable work keyed uniquely by Tenant/position ID with
  status/attempt/lease facts; same-module Tenant-consistent position reference.

Only same-module geofence/position relationships may use composite Tenant foreign keys. Organization
location and Fleet Vehicle UUIDs remain logical references. Tenant-leading indexes cover lifecycle and
bounding box, logical location, Vehicle/geofence current state, transition history by geofence and Vehicle,
unauthorized source-time history, deterministic identity, and due evaluation jobs. Definition count is
limited to 500 ACTIVE geofences per Tenant for the Phase 1 evaluation budget.

## Frontend and accessibility

Tracking gains **Geofences** navigation within the existing AppLayout. The feature owns a bounded list,
details, DRAFT create/edit, activate/disable/retire actions, type/site/status filters, current memberships
and transition history. It is not the US-54 dashboard.

No paid/external map provider is approved. The current frontend has no established map SDK; the initial
implementation uses a provider-neutral local SVG/canvas preview over entered WGS84 vertices. The form must
remain fully operable without the visual preview through an ordered, keyboard-editable vertex table with
latitude/longitude inputs, add/remove/reorder controls and inline errors. React Hook Form/Zod/Ant Design
provide client UX; backend geometry validation remains authoritative. Map SDK adoption requires separate
licensing/privacy/network decisions.

## Acceptance, performance and rollback

Future acceptance must prove valid create/activation, every invalid geometry rule, silent initial state,
two-confirmation ENTERED/EXITED, duplicate-inside suppression, boundary jitter, delayed/out-of-order and
stale suppression, disable/retire races, unauthorized HIGH alert, overlap independence, Tenant B and
limited-role denial, minimized durable Notification delivery/replay, PostgreSQL constraints/index plans,
and accessible Chromium management/history flows. H2 alone is insufficient.

US-49 must retain US-48 gates: sustained >=200 accepted positions/s, burst >=1,000/s, latest p95 <=200 ms
and history p95 <=500 ms while geofence evaluation is enabled at the approved test cardinality. Measure
evaluation backlog, transition latency and bounded worker/lease recovery. No real FMC130 evidence is
inherited; US-49 proves its own controlled and real-source journey when available.

Rollback disables an affected geofence or the bounded evaluator/transition publication, preserves
definition and immutable transition history, and leaves US-48 ingestion/latest/history available.
Database correction is forward-only; never edit V77 after application.

## Source parity and exclusions

The DOCX/UML source explicitly supplies creation, geometry validation, depot/customer/unauthorized types,
entry/exit detection and configured alerts. Polygon-only storage, lifecycle, two-point hysteresis,
initialization, state/jobs, idempotency, RBAC and durable delivery are necessary implementation controls.
They narrow generic `Geometry` to the smallest source-complete model and do not remove source behavior.
Exclusions are circles/multipolygons/holes, dwell, selected-Vehicle/category/route/trip rules, map-platform
ownership, Delivery Zone serviceability/capacity, site/customer master data, exact-coordinate events,
Driver discipline, Operations cases, analytics/dashboard/replay and historical re-evaluation.

## Controlled implementation sequence

1. `US-49-MANAGE-GEOFENCES-CS01-DOMAIN-PORTS-001`
2. `US-49-MANAGE-GEOFENCES-CS02-V77-PERSISTENCE-001`
3. `US-49-MANAGE-GEOFENCES-CS03-EVALUATION-TRANSITIONS-001`
4. `US-49-MANAGE-GEOFENCES-CS04-APIS-RBAC-AUDIT-001`
5. `US-49-MANAGE-GEOFENCES-CS05-NOTIFICATION-INTEGRATION-001`
6. `US-49-MANAGE-GEOFENCES-CS06-FRONTEND-001`
7. `US-49-MANAGE-GEOFENCES-CS07-POSTGRES-CONCURRENCY-PERFORMANCE-001`
8. `US-49-MANAGE-GEOFENCES-TECHNICAL-CLOSURE-001`
9. `US-49-MANAGE-GEOFENCES-FINAL-ACCEPTANCE-001`

US-48 remains on external-prerequisite hold and accounting remains 72/87. No implementation or V77 was
created by this decision.
