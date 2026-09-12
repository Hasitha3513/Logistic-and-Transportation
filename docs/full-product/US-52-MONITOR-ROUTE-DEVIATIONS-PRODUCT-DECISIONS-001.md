# US-52 Monitor Route Deviations Product Decisions

**Task:** `US-52-MONITOR-ROUTE-DEVIATIONS-PRODUCT-DECISIONS-001`  
**Decision:** `PRODUCT_DECISIONS_FROZEN / READY_FOR_IMPLEMENTATION`  
**Date:** 2026-09-12  
**Owner:** Tracking  
**Flyway head:** V83; V84 does not exist  
**Accounting:** unchanged at 73/87 complete and 14/87 remaining  
**Next:** `US-52-MONITOR-ROUTE-DEVIATIONS-CS01-DOMAIN-PORTS-001`

## Executive decision and source traceability

The requirements define a Tracking / Control Room Operator capability that compares the planned and actual
route, calculates severity, records the deviation, supports approval and escalates a significant unapproved
deviation. The mind map confirms planned-versus-actual path, fuel-waste tracking, severity and approval. The
UML is a behavioral cross-check only: its apparent direct Deviation-to-Planned-Route repository access is
replaced by published module contracts to preserve the modular boundary. Fuel-waste estimation is optional in
the source and is excluded from Phase 1 because no accepted fuel-impact contract or deterministic formula
exists; it requires a later product decision and cannot block core deviation monitoring.

Tracking owns comparison, current state, candidates, immutable deviation episodes, operational review and
publication. Routing owns route definitions, revisions, ordered route geometry, stops, disruptions and
authorized route-plan changes. Trip owns which route revision applies to a Vehicle/Trip at source time. US-48
owns accepted positions and trust/order/freshness facts. Notification owns recipients, templates, channels,
preferences, retries and delivery history. Workflow may later orchestrate review definitions but never owns
route or deviation state. No repository, entity, SQL join or physical foreign key crosses these owners.

Rejected alternatives are Routing-owned detection, Trip-owned evidence, a generic exception engine, direct
foreign-table reads, automatic rerouting, and treating every known alternate as authorized. They either move
telemetry-derived behavior away from Tracking or erase explicit source-time authority.

## Major decision register

| Decision | Rationale and alternatives rejected | Owner | Security, data, API, event and acceptance consequence |
| :--- | :--- | :--- | :--- |
| Tracking owns detection and review evidence | Comparison is telemetry-derived; Routing/Trip ownership or a generic exception engine would cross boundaries | Tracking | Tenant-scoped evidence/APIs; technical and physical acceptance remain US-52-owned |
| Trip source-time lookup plus Routing geometry lookup | Preserves assignment and route authority; direct SQL, ambient Tenant and last-route fallback rejected | Trip / Routing | Explicit Tenant inputs, logical references only, provider contract tests required |
| Immutable `REVISION:n` route snapshot | Later edits cannot rewrite evidence; mutable current-route geometry rejected | Routing | Episode snapshots version; route-change race and history tests mandatory |
| Per-route-revision tolerance, 10–5,000 m | Routes differ operationally; hidden default and generic policy engine rejected | Tracking | Managed/audited API, one active version, missing rule is NOT_EVALUATED |
| Local tangent-plane point-to-polyline distance | Bounded accuracy without paid/PostGIS dependency; endpoint chord rejected | Tracking | Complete 2–2,000-point geometry and numerical boundary tests required |
| Effective tolerance adds known accuracy | Avoids false alerts from declared GPS uncertainty; ignoring/clamping/guessing accuracy rejected | Tracking | Missing or >1,000 m accuracy cannot mutate state or alert |
| Two outside to confirm, one inside to clear | Suppresses jitter while restoring truth promptly; single outside and two-point clearance rejected | Tracking | Deterministic state/race tests; equality remains ON_ROUTE |
| Episode evidence with deterministic identity | Prevents packet floods and duplicate facts; per-packet violation rows rejected | Tracking | One episode/event under replay/concurrency; bounded history API |
| Distance-only WARNING/HIGH | Deterministic and source-aligned; subjective/duration scoring rejected | Tracking | HIGH review and one-way escalation behavior are testable |
| Approval annotates; route change stays external | Source approval is preserved without hidden rerouting; silent suppression rejected | Tracking review / Routing plan | Separate APPROVE permission, immutable reviews, no foreign mutation |
| Notification-only durable events | Source needs alert/escalation; automatic US-78 case creation rejected | Tracking / Notification | Shared outbox, minimized payload, idempotent replay, no coordinates |
| Physical final evidence required | Real coordinate/accuracy fidelity determines lateral comparison; fixture-only final acceptance rejected | Acceptance governance | Safe controlled route mandatory; no acceptance inheritance |

## Planned route and attribution contracts

Tracking reuses `VehicleTripAssignmentLookup.findAt(tenantId, vehicleId, sourceTimestamp)`. Its existing
optional result remains `tripId`, nullable `driverId`, nullable `routeId` and nullable `routeVersion`; the Trip
provider must now populate `routeVersion` for assigned routed Trips. The canonical format is
`REVISION:<positive-integer>`, bounded to 120 characters. The value identifies the Routing revision effective
for that Trip assignment at the observation source time. Absence yields `ROUTE_UNAVAILABLE / NOT_EVALUATED`;
Tracking never falls back to the Vehicle's last Trip or route.

Routing publishes one additive root contract:

```java
interface PlannedRouteGeometryLookup {
    Optional<PlannedRouteGeometry> find(UUID tenantId, UUID routeId, String routeVersion);
}

record PlannedRouteGeometry(UUID routeId, String routeVersion,
                            List<Wgs84Point> orderedPoints) {}
record Wgs84Point(BigDecimal longitude, BigDecimal latitude) {}
```

The result is a provider-neutral immutable revision snapshot, not a Routing entity. It contains the complete
ordered path from origin through all authorized intermediate geometry points/stops to destination. WGS84 order
is always `(longitude, latitude)`, decimal precision is at least 6 fractional digits, and the list contains
2–2,000 points with no adjacent duplicate. Stops remain Routing-owned metadata and are not separately copied
unless they are path vertices. An origin-to-destination straight line is forbidden for multi-stop or shaped
routes. A revision is monitorable only when Routing can return the complete immutable snapshot.

Current Routing revisions preserve ordered location IDs but not immutable path coordinates, and the current
Trip adapter returns a null route version. CS01 must add the two published contract surfaces/provider mapping;
CS02 may add the smallest Routing-owned immutable revision-geometry persistence needed alongside
Tracking-owned US-52 persistence. This is an additive implementation prerequisite, not permission for
Tracking to derive geometry from Organization locations or read Routing/Trip persistence. Missing/malformed
geometry or a transient provider failure yields `CONFIGURATION_UNAVAILABLE`, is retryable, and never asserts a
deviation.

A later authorized route change creates a new Routing revision and a new source-time Trip attribution fact.
It never rewrites an earlier episode. An active old-revision episode closes as `SUPERSEDED` at the effective
change time; the first eligible position attributed to the new revision initializes a fresh baseline. Only the
explicitly assigned revision is authoritative. US-22 remains owner of disruptions/detours; an approved
disruption may be referenced by logical UUID only after Trip/Routing assigns the new revision.

## Corridor, geometry and GPS accuracy

Each route revision has one Tracking-owned operational tolerance rule. There is no Tenant fallback: route
geometry and operational context vary, and silently applying a broad default could hide a real deviation.
Tolerance is decimal metres, minimum 10 and maximum 5,000. No default is shipped. A route revision without one
ACTIVE rule is `CONFIGURATION_UNAVAILABLE / NOT_EVALUATED`.

Rules use `DRAFT -> ACTIVE <-> DISABLED -> RETIRED`; RETIRED is terminal. Only DRAFT or DISABLED rules are
editable. Activation increments an immutable rule version/effective fact and permits exactly one ACTIVE rule
per Tenant/route/revision. Optimistic version, persistent Tenant-scoped idempotency and safe audit apply.
Changes affect future observations only and never reprocess history.

Distance is the minimum point-to-segment distance across the full ordered polyline. Phase 1 uses a bounded
local tangent-plane projection centred on the observation, with longitude scaled by cosine(latitude), and
Euclidean clamped segment projection. This is WGS84-safe for the approved per-segment maximum of 25 km;
Routing must densify longer segments before publication. Antimeridian-crossing geometry is not supported and
fails validation. No PostGIS, paid map or navigation engine is approved.

Known horizontal accuracy is incorporated conservatively:

`effectiveToleranceMeters = configuredToleranceMeters + horizontalAccuracyMeters`.

Only finite accuracy from 0 through 1,000 metres is eligible, matching the US-48 trust ceiling. Missing
accuracy is truthful `ACCURACY_UNKNOWN / NOT_EVALUATED`; it cannot create or clear a deviation. Distance less
than or exactly equal to effective tolerance is inside. Deviation requires strictly greater distance.

## Eligibility, ordering and state

A position is eligible only when it is accepted and nonduplicate, `TRUSTED`, Vehicle-associated, `IN_ORDER`,
valid WGS84, has immutable source time, known eligible accuracy, and is no more than five minutes old at
evaluation. Future, clock-skew, late, stale, out-of-order, untrusted, invalid and duplicate observations do not
change the stable state, candidate or episode. They may make the monitoring availability shown to an operator
`UNKNOWN` but cannot fabricate ON_ROUTE or clearance. Ordering is `(sourceTimestamp, positionId)` ascending;
receipt time is operational metadata only.

Public state is exactly `UNKNOWN | ON_ROUTE | DEVIATING`. The first eligible observation initializes ON_ROUTE
or an outside candidate silently. Two distinct consecutive eligible outside observations under the same Trip,
route revision and rule version confirm DEVIATING. One eligible observation inside the same effective corridor
clears a candidate or closes an active episode; the inclusive boundary is therefore responsive without
weakening confirmation. A Vehicle beyond the destination after Trip completion is not evaluated. There is no
comparison with the last route, origin/destination chord or any unassigned alternate.

An accepted eligible position atomically enqueues at most one Tracking-owned durable evaluation job. Failure
does not roll back telemetry. Jobs are Tenant/position-idempotent, bounded, leased, retryable and claimed with
`FOR UPDATE SKIP LOCKED` by one scheduler and fixed workers. Same-Vehicle evaluation serializes; different
Vehicles remain parallel. Lease expiry permits reclaim and exhausted failures remain observable/retryable.

## Episode, severity and operational review

Tracking persists one `RouteDeviationEpisode`, not one row per packet. It snapshots: deterministic UUID,
Tenant, Vehicle, nullable Trip/Driver, route ID/version, rule ID/version, configured and effective tolerance at
confirmation, first-candidate and confirming position IDs, start/confirmation/end source timestamps, maximum
distance from route, eligible outside sample count, severity, review status, nullable reason code, reviewer,
review time, review version, terminal outcome, creation/update timestamps and optional logical disruption ID.
Raw route geometry, coordinate history, device/provider data and credentials are not copied.

Identity is a SHA-256-derived UUID over Tenant, Vehicle, Trip, route ID/version, rule ID/version and first
candidate position ID. Duplicate/concurrent evaluation converges on one state, episode and event. Closed
evidence is append-only; source positions, source times, attribution and route version are never corrected.

Severity is deterministic from the maximum observed lateral distance relative to the effective tolerance:
`WARNING` when distance is greater than tolerance and at most twice tolerance; `HIGH` when strictly greater
than twice tolerance. Duration is evidence, not a severity input. Severity may move only WARNING to HIGH while
open and never downgrade. Confirmation publishes one detection event; the first later HIGH transition may
publish one escalation event. Further packets do not notify. This bounded one-way escalation satisfies the
source without packet flooding.

Review status is `NOT_REQUIRED | PENDING | APPROVED | REJECTED`. WARNING starts NOT_REQUIRED. HIGH starts
PENDING and requires `ROUTE_DEVIATION_APPROVE`. Approval means the observed detour is operationally accepted
as evidence; it neither changes Routing, suppresses monitoring nor erases severity. Rejection preserves the
episode and requests Notification escalation. An authorized route change must be performed through Trip/
Routing and then follows the version-change rule above.

Approval/rejection requires expected review version and exactly one bounded reason code:
`AUTHORIZED_DETOUR | ROAD_CLOSURE | TRAFFIC_DIVERSION | OPERATIONAL_NECESSITY | UNKNOWN`. `UNKNOWN` requires a
bounded safe note of 10–500 characters; other notes are optional and bounded to 500. A subsequent correction
uses a compensating review record and audit entry; no destructive overwrite. Automated detection has no human
creator, so creator-versus-approver SoD is inapplicable. The same actor may not reverse their own prior review;
a different approver is required for review correction. There is no Driver discipline or punitive action.

If a Trip ends while an episode is open, Tracking closes it at the Trip end source time as
`TRIP_ENDED_UNRESOLVED`; HIGH remains reviewable. If route attribution changes, the old episode closes
`SUPERSEDED`. Normal inside clearance closes `RETURNED_TO_ROUTE`. No retrospective evaluation occurs.

## Persistence, API, security and audit

V84 is reserved, subject to head recheck. The minimum Tracking-owned tables are:

- `tracking_route_deviation_rule` for route-revision tolerance, lifecycle, versions and audit facts;
- `tracking_route_deviation_state` for stable/candidate state and source ordering per Vehicle;
- `tracking_route_deviation_episode` for append-only operational evidence and current review projection;
- `tracking_route_deviation_review` for immutable approval/rejection/correction history;
- `tracking_route_deviation_evaluation_job` for durable bounded work.

Logical Trip, Driver, Route, revision, disruption and Vehicle references have no cross-module physical foreign
keys. Same-module relationships may use Tenant-consistent composite keys. All uniqueness, indexes, commands,
jobs, caches and repository operations are Tenant-leading. There is no delete or public purge; retention uses
the existing external Tracking retention policy and policy metadata.

The API family is `/api/v1/tracking/route-deviations`. It provides bounded rule create/list/detail/update plus
explicit activate/disable/retire commands; current-state list/detail; episode history/detail; and explicit
`POST /episodes/{id}/approve`, `/reject`, and `/correct-review` commands. Generic status PATCH is forbidden.
Management commands require `Idempotency-Key`; optimistic commands require the current version.

Lists default to 20 and cap at 100. Episode history requires a UTC range no longer than 31 days, uses keyset
ordering `(startSourceTimestamp,id)` descending, defaults to 100 and caps at 500. Normal responses expose
logical Vehicle/Trip/Driver/Route identities, route/rule versions, distance/tolerance, state, severity, source
times and review facts. They do not expose coordinates or geometry. Existing `TRACKING_HISTORY_VIEW` is
required to follow a logical position reference to exact coordinates.

Permissions are exactly `ROUTE_DEVIATION_VIEW`, `ROUTE_DEVIATION_MANAGE`,
`ROUTE_DEVIATION_EVENT_VIEW`, and `ROUTE_DEVIATION_APPROVE`. VIEW governs rules/current state;
MANAGE governs rule configuration/lifecycle; EVENT_VIEW governs episodes and review history; APPROVE governs
approve/reject/correction commands and does not imply MANAGE. Tenant comes only from authenticated active
membership; no body/query tenant ID is accepted. Foreign-Tenant identifiers return safe not-found. No branch,
project or generic ABAC is invented.

Audit covers rule create/update/activate/disable/retire, review decisions/corrections, denied management/review
commands and policy changes. Telemetry evaluation and ordinary reads are not audit events. Immutable episode,
review and durable-event records are business evidence.

## Durable events and Notification

Tracking publishes `VehicleRouteDeviationDetectedV1` at confirmation and optionally
`VehicleRouteDeviationEscalatedV1` once on the first WARNING-to-HIGH transition or rejected HIGH review.
Both use the shared P1-01 outbox, canonical Tenant envelope, at-least-once delivery, no global ordering and
Tenant/event consumer idempotency. Detection event ID is the episode UUID; escalation ID is deterministic over
Tenant, episode and escalation reason. Notification is the only Phase-1 consumer and resolves same-Tenant
Dispatcher recipients. Operations/US-78 integration is NONE in Phase 1.

The minimized detection payload is `routeDeviationEpisodeId`, `vehicleId`, nullable `tripId`, nullable
`driverId`, `routeId`, `routeVersion`, `severity`, `observedDistanceMeters`, `effectiveToleranceMeters`,
`sourceTimestamp`, and `approvalRequired`. Escalation adds only `escalationReason` (`DISTANCE_HIGH` or
`REVIEW_REJECTED`). Exact coordinates, geometry, device/provider identity, credentials, raw telemetry, Driver
PII, Customer PII and notes are forbidden. Notification failure cannot invalidate persisted evidence; replay
must not create another logical delivery.

## Frontend, privacy and operations

Tracking gains Route Deviations pages for rule configuration, truthful current state, bounded episode history,
episode detail and permission-controlled review. It uses the existing React, TypeScript, React Router, Ant
Design, TanStack Query, React Hook Form, Zod, Axios and AuthContext stack. Terminology is Planned route, Actual
position, Deviation, Distance from route and Authorized detour/Approved deviation—not violation, offence or
misconduct.

Phase 1 adds no map SDK or paid provider. A bounded local SVG/canvas preview may render the immutable polyline
and selected position only in detail views for users holding both ROUTE_DEVIATION_EVENT_VIEW and
TRACKING_HISTORY_VIEW; all facts remain accessible without the visual. UNKNOWN states explicitly distinguish
stale telemetry, accuracy unknown, no Trip, no route revision, unavailable geometry and missing rule. The UI
never fabricates ON_ROUTE.

## Scale, concurrency and acceptance

The approved scale is 10,000 registered Vehicles, 2,000 simultaneously active Vehicles per Tenant, at most
2,000 points per route revision, 10,000 active route-revision rules per Tenant, 31-day request windows and
external-policy episode retention. Work and queries are bounded; no per-Vehicle thread or unbounded scan is
permitted. US-52 must preserve US-48 gates: sustained at least 200 positions/second, burst at least 1,000,
latest p95 at most 200 ms and 24-hour history p95 at most 500 ms. At 2,000 active Vehicles and 2,000-point
worst-case routes, eligible-position-to-state/episode projection p95 must be at most 2 seconds with backlog
draining after burst and no ingestion rollback.

PostgreSQL acceptance must cover duplicate positions, concurrent same-Vehicle evaluation, out-of-order facts,
route change versus evaluation, rule activation/update versus evaluation, confirmation and clearance races,
approval versus new evidence, deterministic episode/event identity, lease reclaim, event and Notification
replay, and cross-Tenant concurrency. Query plans must prove Tenant-leading indexes at representative volume.

Technical acceptance may use deterministic signed trusted position fixtures and immutable Routing/Trip test
providers. Final acceptance requires US-52's own physical provider/device journey because lateral-position and
accuracy fidelity materially determine deviation. It must use a safe private/controlled area or stationary
test route geometry designed around genuine points; unsafe or unlawful driving is forbidden. No US-48, US-49
or US-50 acceptance is inherited.

Mandatory scenarios are on-route, exact boundary, first outside candidate, confirmation, continued deviation,
WARNING-to-HIGH escalation, clearance, duplicate, out-of-order, stale, missing/low accuracy, no Trip, missing
geometry/rule, route revision change, Trip completion, review approve/reject/correction, Tenant and limited-role
denial, minimized Notification/replay, history paging, accessible frontend, PostgreSQL races and all performance
gates.

## Controlled implementation sequence and exclusions

1. `US-52-MONITOR-ROUTE-DEVIATIONS-CS01-DOMAIN-PORTS-001`
2. `US-52-MONITOR-ROUTE-DEVIATIONS-CS02-V84-PERSISTENCE-001`
3. `US-52-MONITOR-ROUTE-DEVIATIONS-CS03-EVALUATION-EPISODES-001`
4. `US-52-MONITOR-ROUTE-DEVIATIONS-CS04-APIS-RBAC-AUDIT-001`
5. `US-52-MONITOR-ROUTE-DEVIATIONS-CS05-NOTIFICATION-INTEGRATION-001`
6. `US-52-MONITOR-ROUTE-DEVIATIONS-CS06-FRONTEND-001`
7. `US-52-MONITOR-ROUTE-DEVIATIONS-CS07-POSTGRES-CONCURRENCY-PERFORMANCE-001`
8. `US-52-MONITOR-ROUTE-DEVIATIONS-TECHNICAL-CLOSURE-001`
9. `US-52-MONITOR-ROUTE-DEVIATIONS-FINAL-ACCEPTANCE-001`

Excluded are navigation, automatic rerouting, traffic/legal/compliance engines, Driver discipline, spoofing,
generic exceptions, US-54 dashboard, retrospective reprocessing, automatic Operations cases, raw-coordinate
events, uncontrolled reason taxonomy, fuel-waste estimates, PostGIS and paid map dependencies.

All ownership, route source/version/geometry, corridor, accuracy, eligibility, state, hysteresis, episode,
severity, review, event, persistence, API, security, frontend, performance and acceptance decisions are frozen.
Open product decisions are **NONE**. This task creates no production code, migration, API, permission, event,
Notification or frontend implementation.
