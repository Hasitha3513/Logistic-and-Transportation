# US-52 CS03 Route-Deviation Evaluation and Episodes

## Result

`PASS` — US-52 is `IMPLEMENTATION_IN_PROGRESS / CS03_COMPLETE`. Story accounting remains
73 / 87 complete. Flyway remains V88. The next governed slice is
`US-52-MONITOR-ROUTE-DEVIATIONS-CS04-APIS-RBAC-AUDIT-001`.

## Activated boundary and ownership

- CS03 exposes the existing Tracking-owned `RouteDeviationEvaluationUseCase` as the narrow
  internal activation boundary. It does not add a Kafka consumer, scheduler, REST endpoint or
  public contract; runtime ingestion wiring remains separately governed.
- Each accepted position supplies authoritative Tenant, Vehicle, position identity, source time,
  WGS84 point, accuracy, trust, ordering, acceptance and Vehicle-association facts.
- Tracking resolves source-time attribution only through Trip's published
  `VehicleTripAssignmentLookup`, and resolves exact immutable geometry only through Routing's
  published `PlannedRouteGeometryLookup`. There is no latest-revision fallback and no foreign
  entity, repository, SQL or physical foreign-key access.
- Cross-module lookups complete before the Tracking-owned database transaction. Tracking alone
  owns deviation rules, candidate/stable state and episode evidence.

## Deterministic evaluation

- Distance is the minimum clamped point-to-segment distance over the complete immutable route
  polyline using the CS01 bounded local tangent-plane calculation.
- The inclusive inside boundary is configured tolerance plus eligible accuracy. Accuracy from 0
  through 1,000 metres is eligible. `WARNING` applies above that effective boundary through the
  frozen upper boundary; `HIGH` begins strictly above it. Open-episode severity never downgrades.
- Two distinct, consecutive eligible outside points under the same Tenant, Vehicle, Trip, route,
  revision and rule confirm one episode. One eligible inside point resets a candidate or closes an
  open episode as `RETURNED_TO_ROUTE`.
- The first candidate's identity, source timestamp, coordinates, accuracy, calculated distance,
  severity and attribution are persisted and reused verbatim at confirmation. The confirming point
  cannot replace or reconstruct that evidence.
- Continued deviation updates the same episode, maximum distance and evidence count. Trip, route
  or revision changes close the old episode as `SUPERSEDED` before a fresh lifecycle begins.

## Non-evaluable truth and safety

The service distinguishes invalid coordinates, unknown or excessive accuracy, untrusted,
out-of-order, duplicate/ineligible and stale positions; no Trip, route or route revision; missing
exact geometry or rule; and Trip, Routing or configuration provider failures. These outcomes update
only truthful availability while preserving valid candidate/episode state. They never confirm,
clear or falsely classify a deviation as on-route.

Ordering is `(sourceTimestamp, positionId)`. Duplicate/replayed and older observations cannot
advance or rewind state. A Tenant/Vehicle advisory lock serializes lifecycle transitions, the CS02
optimistic lock version protects persisted state, and the V88 unique open-episode constraint is the
database backstop. State and episode changes share one Tracking transaction; an episode-write
failure rolls back the complete transition. Concurrent confirmation converges on one open episode.

All repository operations and cross-module lookups carry explicit Tenant authority. No provider
secret, signature, raw payload, Driver PII or Customer PII is persisted or logged by this flow.
Precise coordinates remain only in Tracking-owned deviation evidence.

## Verification evidence

- Focused evaluator/application/source-contract selection: 25 / 25 PASS.
- V88 PostgreSQL acceptance, including application lifecycle, concurrent confirmation, rollback,
  tenant isolation, advisory locking and V87-to-V88 upgrade: 9 / 9 PASS.
- Affected Trip, Routing, Tracking, Kafka, Redis, Timescale, US-49 and US-50 regression selection:
  231 / 231 PASS.
- Architecture, Modulith and table ownership: 58 / 58 PASS.
- Complete clean Maven suite: 1,726 / 1,726 PASS; 0 failures, 0 errors, 0 skipped; BUILD SUCCESS in
  09:41.
- Flyway clean replay: V1 through V88 PASS; V88 remains current and no migration was added.
- Checkstyle: 0 violations. PMD: 0 findings. SpotBugs: 0 findings.
- Dependency analysis: BUILD SUCCESS with only the repository's existing starter/transitive
  classification warnings.
- Docker Compose configuration: valid; PostgreSQL service healthy.
- `git diff --check`: PASS.
- Frontend verification was not run because CS03 changes no frontend file or contract.

## Scope boundary, rollback and remaining work

CS03 adds no migration, external event publication, operational-exception integration, review
command, rejected-review escalation, audit-management workflow, API, permission, notification,
scheduled scanner, dashboard or frontend behavior. CS04 owns management/query APIs, RBAC and audit.
CS05–CS07 retain notification/event integration, frontend, and PostgreSQL concurrency/performance
closure respectively, followed by technical closure and independent physical final acceptance.

Rollback is an application-code rollback: V88 is unchanged and remains forward-compatible.
Already persisted CS03 state/episodes remain valid evidence and must not be deleted or rewritten.
