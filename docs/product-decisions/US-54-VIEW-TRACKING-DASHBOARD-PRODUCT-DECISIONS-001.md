# US-54 View Tracking Dashboard Product Decisions

**Decision:** `APPROVED / FROZEN`

**Date:** 2026-09-15

**Repository baseline:** `9b98040`

**Flyway baseline:** V93; V94 is reserved only for the permission seed approved below

**Accounting:** 73/87 complete; 14/87 remaining

## Purpose and authority

US-54 gives a Dispatcher or authorized Tracking/Control Room operator a bounded, current operational risk view across the same Tenant's fleet. The original DOCX, mind map and US-51–US-60 diagrams were reconciled with implemented US-48–US-53 contracts, Notification, audit, Tenant/RBAC and frontend architecture. This decision changes no production code, schema, API, permission or UI by itself.

US-54 is a read-only consumer. It never detects, confirms, escalates, reviews or mutates producer evidence. Displaying technically complete or accepted producer facts does not upgrade any producer's acceptance status.

## Phase 1 purpose and non-goals

Primary actors are existing `DISPATCHER`, `ADMIN` and `LOCAL_MVP_ADMIN` members who receive the narrow dashboard permission. The dashboard supports deciding which Vehicle needs immediate inspection because telemetry is stale/offline or an eligible Tracking incident is active/recent, and provides governed navigation to the owning workflow.

Phase 1 includes fleet status counts, a bounded latest-trusted-position map/table, freshness/connectivity and motion classification, active Trip context, accepted/technical-labelled incident summaries, a current fleet-density heat layer, Notification unread status via Notification's own API, filters/sorting and Journey Replay links.

Explicit non-goals are engine-on/idle monitoring, fuel-waste inference, new detection rules, incident mutation/review, dispatch optimization, predictive analytics, Driver ranking, Customer tracking, route replay, evidence export, geocoding, road snapping, a new map provider, WebSockets/server-sent events, a new Kafka topic, cross-Tenant benchmarking and a persisted dashboard/reporting warehouse.

The Live Vehicles page remains the detailed current/last-known Vehicle and device workflow. Journey Replay remains the bounded historical movement, stop and incident-forensics workflow. The dashboard is a permission-filtered summary and navigation surface; it does not replace either.

## Widget and authoritative-source matrix

| Element | Owner and source | Semantics and bounds | Failure/acceptance treatment |
| --- | --- | --- | --- |
| Fleet status counts | Tracking live-state query port over Redis projection with existing bounded database fallback | Count only the current filtered page plus explicit `matchingVehicleCount`; LIVE/RECENT/STALE/UNKNOWN and CONNECTED/DEGRADED/OFFLINE/UNKNOWN are separate | Mark source `DEGRADED` when Redis is unavailable; never relabel fallback as live |
| Vehicle table | Tracking latest-received/latest-trusted state | At most 100 Vehicles, stable `vehicleId ASC`; identifiers, freshness, connectivity, trust, source/receipt time, accuracy and optional speed | US-48 is technically complete and externally blocked; label `FIELD_ACCEPTANCE_PENDING` |
| Last-known map | Tracking `latestTrusted`; coordinates returned only with `TRACKING_VIEW` | At most 100 markers; LIVE, RECENT and STALE may render with visually distinct labels; UNKNOWN has no marker | Stale state is explicitly `LAST KNOWN`; untrusted latest-received never replaces trusted map truth |
| Motion summary | Tracking latest-trusted speed | `MOVING` only when speed is known and `>3.0 km/h`; `STATIONARY` only when known and `<=3.0 km/h`; otherwise `UNKNOWN`; stale/offline remains separately visible | Motion is observed speed, never engine/idle evidence |
| Fleet-density heat layer | Tracking latest-trusted LIVE/RECENT positions | Optional map layer, same filtered set, maximum 100 positions, server-binned `0.01° × 0.01°` cells with count and cell centre | Requires `TRACKING_VIEW`; excludes STALE/UNKNOWN/untrusted; map failure falls back to table |
| Geofence incidents | Tracking-owned US-49 transitions through a dashboard query port | Latest 20 within the last 24 hours; safe type, severity, Vehicle and source time | Producer `COMPLETE_ACCEPTED`; requires `GEOFENCE_EVENT_VIEW` |
| Speed incidents | Tracking-owned US-50 episodes through a dashboard query port | Latest 20 within the last 24 hours; safe severity/status, Vehicle and source time | `FIELD_FIDELITY_PENDING`; requires `SPEED_EVENT_VIEW` |
| Route-deviation incidents | Tracking-owned US-52 episodes through a dashboard query port | Latest 20 within the last 24 hours; safe severity/status, Vehicle/Trip/route identity and source time | `FIELD_ACCEPTANCE_PENDING`; requires `ROUTE_DEVIATION_EVENT_VIEW` |
| Active Trip context | New published Tenant-qualified `TripDashboardQuery` implemented by Trip | One bulk lookup for up to 100 Vehicle IDs at dashboard `evaluatedAt`; Trip ID, lifecycle, route ID/version only | No Driver/Customer/freight/billing data; absent/ambiguous context is `UNKNOWN` |
| Notification status | Notification's existing authenticated unread-count API, called independently by the frontend | Current actor's unread count and link to Notification | Dashboard backend does not query Notification tables or copy notification state |
| Journey Replay link | Existing frontend route | Link only when `JOURNEY_REPLAY_VIEW` is present; Vehicle ID is placed in in-memory navigation state, not URL | US-53 remains externally blocked; the link does not imply field acceptance |

No dashboard query accesses another module's private table, repository or JPA type. Tracking-owned producer evidence may be read only through focused Tracking ports. Trip enrichment uses a published module contract and one bounded bulk operation, never per-row calls.

## Producer acceptance matrix

| Producer | Dashboard label | Governing status |
| --- | --- | --- |
| US-48 live state | `FIELD_ACCEPTANCE_PENDING` | `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM` |
| US-49 geofence | `ACCEPTED` | `COMPLETE / ACCEPTED` |
| US-50 speed | `FIELD_FIDELITY_PENDING` | `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM` |
| US-51 idle | `UNAVAILABLE` | Engine-state capability unresolved; no widget or inference |
| US-52 route deviation | `FIELD_ACCEPTANCE_PENDING` | `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM` |
| US-53 Journey Replay | `FIELD_ACCEPTANCE_PENDING` | `TECHNICALLY_COMPLETE / IMPLEMENTATION_COMPLETE_ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM` |

Labels are returned as response metadata and remain visible in the UI. US-54 acceptance never changes these values.

## Freshness, trust and time rules

Existing `TrackingStatusPolicy` is authoritative and unchanged:

- `LIVE`: latest trusted source time is no more than 60 seconds old and latest receipt is no more than 60 seconds old.
- `RECENT`: latest trusted source time is no more than five minutes old but LIVE conditions are not met.
- `STALE`: latest trusted source time is older than five minutes.
- `UNKNOWN`: no trusted observation exists.
- `CONNECTED`: latest receipt is no more than 60 seconds old; `DEGRADED`: over 60 seconds and no more than five minutes; `OFFLINE`: over five minutes; `UNKNOWN`: no receipt exists.

All ages use server UTC `evaluatedAt`. `sourceTimestamp` and `receivedAt` remain separate. Latest-received untrusted, late, future or out-of-order evidence may be shown as an explicitly untrusted diagnostic timestamp but cannot replace `latestTrusted`, change a trusted map marker, or make a Vehicle LIVE. Redis state may be displayed when stale only as `LAST KNOWN / STALE`; immutable Timescale history is not consulted during normal dashboard refresh. History is used only after navigation to Journey Replay. No state yields counts and an empty table/map with `NO_DATA`; unavailable sources remain `UNAVAILABLE` or `DEGRADED`, never zero or healthy.

## Refresh model

Phase 1 uses client polling. The first query runs on page entry, then every 15 seconds while the tab is visible and online. Background/hidden or offline tabs pause polling. Manual refresh is available, resets the backoff and announces completion accessibly. After a transient failure, retries back off to 30 seconds then 60 seconds and remain at 60 seconds until success; success returns to 15 seconds. Existing displayed data remains visibly stale with its last successful `evaluatedAt` during failure.

The server admits at most 10 dashboard requests per actor per minute and 40 per Tenant per minute per application instance. A rejected request returns HTTP 429 and `Retry-After: 60`. Real-time push, WebSockets, SSE and new broker contracts are outside Phase 1.

## Frozen API contract

The only dashboard endpoint is semantically read-only:

`POST /api/v1/tracking/dashboard/query`

A POST body prevents operational selectors from entering browser history. The body contains optional `vehicleIds` (maximum 100), `freshness[]`, `connectivity[]`, `motion[]`, `incidentTypes[]`, `includeHeatMap` (default false), `includeIncidents` (default true), optional opaque `cursor`, and `pageSize` (default 50, maximum 100). Allowed motion values are `MOVING`, `STATIONARY`, `UNKNOWN`; incident types are `GEOFENCE`, `SPEED`, `ROUTE_DEVIATION`. Empty arrays mean no filter. Client-supplied Tenant, actor or `evaluatedAt` is prohibited.

The response contains:

- `evaluatedAt`, `sourceStatus`, `lastSuccessfulRefreshAt` and `producerStatuses`;
- `summary` with matching count and freshness/connectivity/motion counts for the returned bounded result;
- `vehicles[]` with Vehicle ID, freshness, connectivity, motion, latest-received/latest-trusted metadata, optional authorized coordinate/accuracy/speed, optional active Trip/route context and applicable incident counts;
- permission-filtered `incidents[]`, at most 20 per authorized producer and 50 total, ordered `sourceTimestamp DESC, evidenceId DESC`;
- optional `heatMapCells[]`, at most 100, containing grid centre and count;
- `nextCursor`, with stable `vehicleId ASC` pagination.

The cursor is opaque, authenticated, Tenant/filter-bound and expires after five minutes. New refresh starts a new snapshot; cursor continuation retains the original `evaluatedAt`. Unknown enum/filter, malformed body, too many Vehicle IDs or excessive page size returns the standard HTTP 400 error. A foreign-Tenant Vehicle produces safe absence and cannot reveal existence. Missing dashboard permission is HTTP 403. Source degradation returns HTTP 200 with explicit per-source status when a truthful partial result exists; complete Tracking live-source unavailability returns HTTP 503 with the standard error contract. Rate limiting is HTTP 429. Responses set `Cache-Control: no-store` and `Referrer-Policy: no-referrer`.

OpenAPI must describe every bound, enum, permission-dependent omission, coordinate sensitivity, cursor error, producer acceptance label, degraded state, 429 response and 503 behavior. There is no mutation, idempotency key or optimistic version.

## RBAC and audit

V94 is reserved to seed exactly one new active permission and its idempotent grants:

- `TRACKING_DASHBOARD_VIEW`: view the same-Tenant operational Tracking dashboard without automatically receiving precise coordinates or producer evidence.

Grant it only to existing `ADMIN`, `LOCAL_MVP_ADMIN` and `DISPATCHER` roles when present; create no role. The literal HTTP path and secured use-case boundary both require `TRACKING_DASHBOARD_VIEW`. Broad `TRACKING_VIEW`, history, management or producer permissions do not imply dashboard access.

Additional disclosure is conjunctive:

- coordinates and heat-map cells require `TRACKING_VIEW`;
- geofence incidents require `GEOFENCE_EVENT_VIEW`;
- speed incidents require `SPEED_EVENT_VIEW`;
- route-deviation incidents require `ROUTE_DEVIATION_EVENT_VIEW`;
- Journey Replay navigation requires `JOURNEY_REPLAY_VIEW`.

Missing optional permission omits that field/section without leaking counts. Each initial dashboard query, authenticated denial and rate-limit rejection writes one privacy-safe audit fact. Cursor continuation is summarized rather than audited per page. Metadata is limited to Tenant, actor, UTC time, correlation ID, filter categories, requested page size, result count, included section names and source-status labels. Coordinates, Vehicle/Trip IDs, incident IDs, exact selectors, cursors, Driver/Customer identity, provider/device facts and credentials are prohibited from audit metadata.

## Privacy and security

Tenant and actor come only from authenticated server context and are passed explicitly through every query, cursor, cache key, metric and audit operation. Vehicle and Trip identifiers are disclosed only under dashboard permission; precise coordinates and heat cells additionally require `TRACKING_VIEW`. Foreign-Tenant identifiers are silently absent.

Driver and Customer PII, names/contact details, cargo, billing/payroll facts, provider credentials or credential references, provider keys, device references/serials, message IDs, signatures, nonces, raw telemetry and review notes are prohibited. Metrics use bounded status/producer labels only and never Tenant, actor, Vehicle, Trip, coordinate, device/provider identity or cursor. Request bodies, responses and errors must not be logged. Browser state remains in memory; no selectors, coordinates, cursors or dashboard response enter URLs, persistent storage, analytics payloads or browser caches.

## Frontend contract

Add **Tracking → Dashboard** as the first Tracking child, visible only with `TRACKING_DASHBOARD_VIEW`; preserve all existing routes. The route is `/tracking/dashboard`. `AppLayout` alone owns the route title, breadcrumb and global spacing; the page must not duplicate them.

Desktop/tablet layout has a filter bar, compact accessible summary cards, a map with optional heat layer, a synchronized Vehicle table and a bounded recent-incidents panel. Selecting a marker selects its table row and vice versa. Incident indicators link to the owning detail route only when the corresponding permission exists. Journey Replay opens from an authorized row using in-memory selection state.

Loading uses skeletons without false zeroes. Empty, no-data, forbidden, partially degraded, stale, offline and map-provider-failed states are distinct. A map failure keeps filters, cards, table and incidents usable and offers retry; no new map-provider dependency is required. At phone width the table becomes accessible Vehicle cards and the map is collapsed behind an explicit control; summaries remain first. All status meaning is conveyed by text and icon, not colour alone. Keyboard navigation, visible focus, labelled controls, semantic headings, live refresh announcements, reduced-motion behavior and WCAG 2.1 AA contrast are mandatory. Existing React, TypeScript, TanStack Query, Ant Design, React Hook Form/Zod where forms apply, Axios and feature-first conventions remain authoritative.

## Performance and operations

- Maximum response page and rendered markers: 100 Vehicles.
- Maximum heat cells: 100; maximum incidents: 50 total and 20 per producer.
- Producer window: previous 24 hours from server `evaluatedAt`; no unbounded aggregate.
- Trip enrichment: one bulk call for at most 100 Vehicle IDs; no N+1.
- Controlled acceptance target: initial dashboard p95 at most 1.5 seconds and subsequent refresh p95 at most 1 second with 100 Vehicles, all authorized sections, 20 concurrent sessions and warmed dependencies.
- Query-plan acceptance requires Tenant-leading indexed access, bounded rows, no full unbounded producer-table scan, no per-Vehicle producer query and no explicit large-result sort where an existing index supplies order.
- Record request count, outcome, latency, returned Vehicle count, source status and included producer categories with bounded labels; record connection/Redis degradation and rate limits safely.
- Backend and frontend use coordinated `tracking.dashboard.enabled` / `VITE_TRACKING_DASHBOARD_ENABLED` defaults-on flags. Disabling hides navigation and returns controlled endpoint unavailability without mutating producer evidence.

These are controlled-environment acceptance targets, not production SLO guarantees. Production capacity must be revalidated against real Tenant distribution and infrastructure.

## Persistence and migration

No dashboard projection, table, materialized view, event, topic, retention change or new detector is approved. Current Redis live state, Tracking-owned indexed episode/transition storage and a published bulk Trip query are sufficient. V94 is permission-only. Existing indexes must first be proven against the exact bounded queries. If a missing index is demonstrated with PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)`, stop and obtain a separate forward-migration authorization; V94 must not absorb it.

## Acceptance boundary

Automated technical acceptance must prove domain aggregation, bounds, permission-filtered disclosure, Tenant isolation, safe degradation, status/acceptance labels, literal-path/use-case security, audit privacy, PostgreSQL plans, Redis failure behavior, frontend accessibility/responsiveness, polling/backoff, map fallback, Chromium journeys and complete regression.

Physical acceptance requires a genuine provider/device stream driving dashboard freshness, movement and recovery; an authorized same-Tenant operational session; operator confirmation that stale/offline and producer labels are truthful; privacy review; and field sign-off. Producer-specific physical evidence is not inherited. US-54 may be `TECHNICALLY_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM` when software gates pass but these field facts remain unavailable.

Degraded-mode acceptance is independently automatable: Redis unavailable/fallback, producer section unavailable, Notification unavailable, map failure, offline/hidden browser behavior, rate limiting and recovery must preserve truthful partial state without false zeroes.

## Risks and rollback

Primary risks are N+1 aggregation, disclosure through optional sections, false current-state claims from stale/untrusted telemetry, browser retention of sensitive selectors, map coupling and dashboard availability being mistaken for producer acceptance. The frozen bounds, bulk contracts, conjunctive permissions, no-store/body-query design, explicit source/acceptance metadata and table-first fallback mitigate them.

Rollback disables the coordinated dashboard flags and removes its navigation/API surface. It does not remove V94, rewrite audit, delete producer evidence, alter Redis/Timescale history or change producer acceptance. After deployment, V94 is immutable; permission removal requires a separately reviewed forward migration.

## Controlled implementation change sets

1. `US-54-VIEW-TRACKING-DASHBOARD-CS01-DOMAIN-QUERY-CONTRACTS-001` — add framework-neutral dashboard query/value contracts, aggregation policy tests and outbound port boundaries; no API, persistence or migration.
2. `US-54-VIEW-TRACKING-DASHBOARD-CS02-BOUNDED-SOURCE-AGGREGATION-001` — implement bounded Tracking source adapters, one published bulk Trip context contract, acceptance metadata and safe degradation; no foreign persistence.
3. `US-54-VIEW-TRACKING-DASHBOARD-CS03-V94-API-RBAC-AUDIT-001` — add permission-only V94, secured body-query API, OpenAPI, audit, rate limiting and literal-path/Tenant security.
4. `US-54-VIEW-TRACKING-DASHBOARD-CS04-FRONTEND-001` — add the permission-aware responsive dashboard, polling/backoff, map/table/heat layer, producer labels and degraded/accessibility behavior.
5. `US-54-VIEW-TRACKING-DASHBOARD-CS05-POSTGRES-REDIS-PERFORMANCE-OPERATIONS-001` — prove query plans, Redis fallback/recovery, 20-session bounds, safe metrics, flags and deployment/rollback behavior; request a separate migration only for a proven index gap.
6. `US-54-VIEW-TRACKING-DASHBOARD-TECHNICAL-CLOSURE-001` — independently rerun focused, PostgreSQL/Redis, architecture, complete backend, static, frontend and Chromium gates and reconcile technical evidence.
7. `US-54-VIEW-TRACKING-DASHBOARD-FINAL-ACCEPTANCE-001` — perform independent technical/degraded/operator/physical acceptance without producer inheritance.

The exact first implementation queue is:

`US-54-VIEW-TRACKING-DASHBOARD-CS01-DOMAIN-QUERY-CONTRACTS-001`
