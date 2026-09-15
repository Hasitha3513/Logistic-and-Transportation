# US-53 Replay Journeys Product Decisions

**Decision:** `APPROVED / FROZEN`

**Date:** 2026-09-15

**Repository baseline:** `6629bb0ab8ef2e60044114a1baa1a5ae9ef65d45`

**Flyway baseline:** V92; V93 is free

**Accounting:** 73/87 complete; 14/87 remaining

## Purpose and authority

US-53 gives a Tracking or Control Room Operator a bounded chronological reconstruction of retained Vehicle
movement, deterministic stop analysis and optional incident context. Original DOCX/mind-map/UML intent,
approved Tracking contracts and current implementation were reconciled. This decision introduces no code,
schema, API, permission or UI by itself and does not inherit physical acceptance from US-48, US-50 or US-52.

## Phase 1 scope and non-goals

| Capability | Decision |
| --- | --- |
| Vehicle and bounded date-range replay | IN |
| Trip-scoped replay | IN; exact source-time Trip attribution is retained |
| Assigned immutable route-revision context | IN when published attribution and geometry exist |
| Deterministic stop detection and details | IN |
| Incident overlays | IN only for the eligible matrix below |
| Playback, pause, resume, seek | IN; entirely client-side over retrieved chunks |
| Playback speeds | `0.5x`, `1x`, `2x`, `4x`, `8x`; default `1x` |
| Export/evidence package/download | OUT; forensic review means controlled on-screen investigation in Phase 1 |
| Engine-on versus movement | OUT while authoritative engine state is unavailable; display `UNKNOWN / UNAVAILABLE`, never infer it |
| Historical mutation, smoothing or map matching | OUT |
| Live tracking/dashboard/heat map | OUT; US-48/US-54 ownership |
| New map provider, road snapping or geocoding | OUT |

## Source of truth and retention

The sole movement source is Tracking-owned TimescaleDB hypertable `tracking_position_history`. It is queried
Tenant-first and ordered by immutable source time. Redis is replaceable live state and is prohibited as replay
evidence. Legacy `tracking_position` is not copied or joined into replay. No replay projection is required in
Phase 1; stop analysis is a deterministic application calculation over bounded history, and incident overlays
reuse Tracking-owned ports rather than duplicate producer tables.

Permitted enrichment is limited to published source-time Trip attribution, published Routing exact-revision
geometry, and eligible Tracking-owned incident queries. No private cross-module repository or SQL access is
allowed. Raw history retention remains 180 days. After expiry the API returns the retained portion with
`coverage=PARTIAL_RETENTION`, requested/available bounds and explicit missing intervals; when none remains it
returns an empty successful result with `coverage=NO_DATA`. Replay never reconstructs deleted raw points from
Redis, events or derived episodes.

V93 is expected only to seed the two permissions approved below. No replay table, projection, materialized
view, retention change or incident-copy table is authorized. If implementation proves an index is required,
stop and obtain a separately reviewed forward-migration decision; V93 must not absorb it.

## Selection, bounds and cursor

| Rule | Frozen value |
| --- | --- |
| Selector | Exactly one of `vehicleId` or `tripId` |
| Default range | Last 6 hours ending at request time for Vehicle selection; Trip bounds for Trip selection |
| Maximum requested range | 7 consecutive days; a longer Trip is replayed in explicit slices |
| Maximum Trip duration supported in one request | 7 days |
| Point page default / maximum | 1,000 / 2,000 |
| Stop page default / maximum | 100 / 500 |
| Incident page default / maximum | 100 / 500 |
| Maximum points across one browser replay | 20,000; crossing it returns `REPLAY_POINT_LIMIT_EXCEEDED` with narrower-range guidance |
| Range semantics | `[from,to)` in UTC; `from < to`; future `to` is clamped only when omitted, otherwise rejected |
| Cursor | Opaque, authenticated, filter- and Tenant-bound keyset; 15-minute expiry |
| Cursor order | `source_timestamp ASC, id ASC`; cursor resumes strictly after both values |
| Empty result | HTTP 200 with empty items, `coverage=NO_DATA`, no invented path |
| Retention crossing | HTTP 200 retained subset with `coverage=PARTIAL_RETENTION` and missing intervals |
| Invalid/reversed/oversized range | HTTP 400 with standard error contract |

Cursor expiry returns `400 REPLAY_CURSOR_EXPIRED`; cursor/filter or Tenant mismatch returns
`400 REPLAY_CURSOR_INVALID`. New late history may appear only after the snapshot upper bound captured in the
first response. Every cursor carries that immutable `snapshotRecordedAt`, so pagination is stable for the
request lifetime.

## Ordering and replay identity

Movement order is `sourceTimestamp ASC, historyId ASC`. `historyId` is the stable tie-breaker, including equal
timestamps from distinct provider events. Exact dedupe replay exists once because ingestion uniqueness remains
authoritative. Out-of-order and late points remain at their source-time position and retain their ordering
classification. They are never rewritten, interpolated or reordered by receipt time. `receivedAt` is returned
separately for latency/quality interpretation but does not drive playback.

A range may span multiple Trips or route revisions. Each point carries nullable source-time attribution and
an attribution status. Context changes appear as timeline boundaries; the UI never blends two Trips or route
revisions. Provider-neutral provenance is limited to provider alias plus quality/trust/order classifications;
message IDs, device references and raw metadata are excluded.

## Timeline point and data-quality contract

Each point contains: `historyId`, `vehicleId`, `sourceTimestamp`, `receivedAt`, WGS84 `latitude`/`longitude`,
nullable `accuracyMeters`, nullable `speedKph`, nullable `headingDegrees`, `trust`, `quality`,
`orderingClassification`, nullable `tripId`, nullable `routeId`, nullable `routeVersion`, `attributionStatus`,
and zero or more `qualityFlags`.

Allowed quality flags are `UNTRUSTED`, `ACCURACY_UNKNOWN`, `ACCURACY_LOW`, `STALE_AT_RECEIPT`, `TIME_GAP`,
`LARGE_JUMP`, `OUT_OF_ORDER`, `LATE`, `CLOCK_SKEW`, `ATTRIBUTION_UNKNOWN`, `GEOMETRY_UNAVAILABLE` and
`PARTIAL_RETENTION`. A time gap is more than two minutes between adjacent source points. A large jump is an
implied ground speed above 200 km/h when both endpoints are otherwise calculable; it is a warning, not proof
of tampering. Missing coordinates cannot exist in canonical history; a malformed historical row fails the
request safely and is never rendered as `(0,0)`. Uncertain data is shown as observed evidence, never continuous
verified movement. Client lines break at gaps, untrusted points and partial-retention boundaries.

## Deterministic stop-analysis contract

| Rule | Frozen value |
| --- | --- |
| Eligible point | `trust=TRUSTED`, valid coordinate, accuracy known and `<=100 m` |
| Stationary speed | Known `speedKph <= 3.0` |
| Missing speed | Permitted only through spatial evidence; never treated as zero |
| Spatial radius | All candidate points remain within 50 m of the accuracy-weighted candidate centroid |
| Minimum dwell | 5 minutes from first to last qualifying source timestamp |
| Maximum internal telemetry gap | 2 minutes |
| Duplicates | Exact ingestion duplicates absent; repeated coordinates remain distinct time evidence |
| Out-of-order/late points | Included at source-time position only when otherwise eligible; classification is retained |
| Stop start/end | First and last qualifying source timestamps |
| Stop location | Accuracy-weighted centroid; no address/geocode; rounded for display, full authorized coordinates in API |
| Adjacent merge | Merge when gap `<=2 minutes` and centroids are `<=50 m`; recompute and revalidate the combined candidate |
| Range boundary | Qualifying candidate touching `from` or `to` is returned with `startTruncated`/`endTruncated`; it is a confirmed stop only if the visible evidence itself reaches five minutes |

If known speed exceeds 3.0 km/h, the candidate ends. With missing speed, every point must satisfy the spatial
radius. Unknown/greater-than-100 m accuracy, untrusted points or gaps over two minutes end the candidate and
create an explicit evidence gap. Stop identity is deterministic SHA-256 over Tenant, Vehicle, request snapshot,
start history ID and end history ID; it is not persisted and conveys no lifecycle authority.

## Incident-overlay eligibility

| Producer | Phase 1 | Source and time | Label | Producer acceptance treatment |
| --- | --- | --- | --- | --- |
| US-49 geofence | YES | Tracking-owned transition query; transition source time | `Geofence transition` with type/severity | `ACCEPTED`; normal evidence label |
| US-50 speed | YES, opt-in | Tracking-owned episode query; confirmation/start/end source times | `Speed-monitoring episode — technical evidence` | Must display `FIELD_FIDELITY_PENDING`; replay does not upgrade acceptance |
| US-51 idle | NO | None | `Engine/idle evidence unavailable` | Engine state is unresolved; never inferred |
| US-52 route deviation | YES, opt-in | Tracking-owned episode/review query; episode/review source times | `Route-deviation episode — technical evidence` | Must display `FIELD_ACCEPTANCE_PENDING`; replay does not upgrade acceptance |

No evidence returns an empty overlay collection plus producer status; it does not fabricate an incident.
Overlay payloads contain logical IDs, type, severity/status, source-time bounds and producer evidence status,
not polygons, route geometry, raw telemetry, review notes, credentials or PII. The UI defaults accepted US-49
on and technical-only US-50/US-52 overlays off with a persistent warning when enabled.

## Trip and route attribution

Trip selection uses the published Tenant-aware Trip query to resolve the selected Trip, Vehicle and exact
bounded actual time. Per-point attribution uses the existing source-time assignment contract. No match is
`UNATTRIBUTED`; overlapping authoritative assignments are `AMBIGUOUS` and no Trip is guessed. This condition
is logged safely and shown as a gap requiring data correction.

The planned route is an optional overlay resolved only by exact `(tenantId,routeId,routeVersion)` through the
published Routing geometry contract. Missing revision or geometry yields `GEOMETRY_UNAVAILABLE`; there is no
latest-revision fallback or chord synthesis. Multiple contexts are returned as immutable timeline segments.

### Frozen assignment-range ceiling

Decision `US-53-TRIP-ASSIGNMENT-RANGE-CEILING-001` authorizes at most 2,000 Trip assignment intervals for one
Tenant, Vehicle and range of no more than seven days. Trip executes one ordered query with `LIMIT 2001`;
the extra row detects overflow. Zero through 2,000 rows are complete results. A 2,001st row fails with
`TRIP_ASSIGNMENT_RESULT_LIMIT_EXCEEDED`, returns no partial attribution, performs no additional page or
per-point fallback, and asks the caller to narrow the range without exposing identifiers or timestamps.
Ordering is actual/effective start ascending and Trip identity ascending. Tracking retains responsibility for
classifying no applicable interval as `UNATTRIBUTED` and multiple applicable intervals as `AMBIGUOUS`.

## Frozen API contract

All routes are semantically read-only under `/api/v1/tracking/journey-replays`. POST bodies are required so
sensitive selectors and time ranges do not enter URLs or browser history:

| Method and path | Permission | Purpose |
| --- | --- | --- |
| `POST /points/query` | `JOURNEY_REPLAY_VIEW` | Bounded chronological point chunks plus coverage/context segments |
| `POST /stops/query` | `JOURNEY_REPLAY_VIEW` | Deterministic stop chunks for the identical selector/range/snapshot |
| `POST /incidents/query` | `JOURNEY_REPLAY_VIEW` and `JOURNEY_REPLAY_INCIDENT_VIEW` | Eligible producer overlays with acceptance labels |

Common parameters are exactly one of `vehicleId`/`tripId`, optional `from`/`to` under the default rules,
optional opaque `cursor`, and bounded `limit`. Incident requests additionally accept repeated `types` from
`GEOFENCE`, `SPEED`, `ROUTE_DEVIATION`; default is `GEOFENCE`. Responses include `items`, `nextCursor`,
`snapshotRecordedAt`, `requestedRange`, `availableRange`, `coverage`, `missingIntervals` and producer status.
Stop responses use `stopId`, source-time bounds, duration seconds, centroid, radius, point count, accuracy
range, truncation flags and quality flags. Incident responses use producer/type/evidence ID, source-time bounds,
severity/status, nullable Trip/route context and `evidenceStatus`.

No optimistic concurrency or idempotency key applies because all routes are reads. `404` is safe absence for
foreign or nonexistent Vehicle/Trip. `403` is used for authenticated permission denial without confirming
resource existence. OpenAPI must document bounds, coordinate sensitivity, cursor errors, partial retention,
quality flags and producer-status warnings. No endpoint mutates or exports evidence.

## RBAC and audit

V93 may seed exactly:

- `JOURNEY_REPLAY_VIEW`: view same-Tenant bounded movement, stops and exact route context.
- `JOURNEY_REPLAY_INCIDENT_VIEW`: view same-Tenant eligible incident overlays; it does not imply replay view.

Both are granted only to existing `ADMIN`, `LOCAL_MVP_ADMIN` and `DISPATCHER` roles when those roles exist.
No role is created. Both permissions are independently enforced at literal HTTP paths and use-case boundaries;
the incident endpoint requires incident permission and replay view because coordinates/time context remain
sensitive. `TRACKING_VIEW` and `TRACKING_HISTORY_VIEW` do not imply either new capability.

Every successful initial points/stops/incidents request and every denied authenticated request records a
privacy-safe audit event. Pagination within the same authenticated snapshot is summarized rather than logged
per page. Metadata is limited to Tenant, actor, action, UTC time, correlation ID, selector type, hashed selector
ID, requested duration, result count, coverage and overlay types. Coordinates, exact timestamps in URL query
logs, Driver/Customer identity, provider/device facts and cursor contents are prohibited. Export audit is
not applicable because export is out of scope.

## Privacy and security

Tenant and actor come only from authenticated server context. Every query, cursor, cache entry and audit fact
is Tenant-qualified. Vehicle/Trip/route logical IDs and precise coordinates are returned only to authorized
users for this purpose. Driver and Customer PII, provider credentials, device references, provider message IDs,
raw payloads, signatures and unrestricted metadata are prohibited.

Selectors and timestamps use the frozen POST bodies for browser-facing query execution to prevent sensitive
identifiers and time ranges entering URL/browser history. These calls perform no mutation. Responses set `Cache-Control: no-store`
and `Referrer-Policy: no-referrer`. Cursors stay in memory/session state, not URLs or persistent browser storage.
Foreign-Tenant identifiers return safe `404`. Rate/bounds are enforced server-side before history access.

## Frontend contract

Navigation is **Tracking → Journey Replay**, visible only with `JOURNEY_REPLAY_VIEW`. The page uses existing
AppLayout, Ant Design, TanStack Query, React Hook Form/Zod, Axios and Tracking map conventions. It provides a
Vehicle-or-Trip selector, UTC-localized range controls, Load action, map above/alongside a synchronized timeline,
play/pause/resume, seek and the five frozen speeds. Stops use accessible markers and a details list. Incident
toggles are permission- and producer-status-aware.

Gaps break the path and display reason/duration. Partial retention, low/unknown accuracy, technical-only
overlays and uncertain attribution remain visible warnings. Loading, empty, forbidden, expired-cursor and
failure states are distinct. At tablet width the map precedes the timeline; at phone width controls stack and
the timeline remains usable without hover. All controls have programmatic labels, keyboard focus/order and
visible focus; play/pause uses a live-region state announcement and does not auto-play. Coordinates are shown
only in an explicitly expanded authorized detail and are not copied automatically.

If map tiles fail, the timeline, stop list, quality evidence and playback clock remain usable with a clear
`Map unavailable` notice. No alternate external map call or coordinate leak is attempted.

## Performance and operations

| Control | Frozen target |
| --- | --- |
| Initial 1,000-point page | p95 `<=2.0 s` in the acceptance workload |
| Subsequent page | p95 `<=1.0 s` in the acceptance workload |
| Concurrent sessions | 20 per application instance in controlled acceptance |
| Query plan | Tenant/Vehicle/source-time keyset index or Timescale chunk exclusion; no full retained-history scan or unbounded sort |
| Application memory | At most one 2,000-point page plus bounded stop-analysis state per request; no full-Tenant buffering |
| Cancellation | Client abort propagates; database statement timeout 5 seconds; no background replay job remains |
| Rate limit | 30 replay requests/minute/actor and 120/minute/Tenant; fail with 429 and `Retry-After` |
| Metrics | count, latency, result size, coverage, rejection reason and overlay type only |
| Sensitive labels prohibited | Tenant/actor/Vehicle/Trip IDs, coordinates, route, device/provider, cursor and exact timestamps |

Timescale retention/compression remains authoritative; queries must work on compressed chunks and must not
decompress or extend retention as a side effect. Operational rollback disables the Journey Replay navigation
and endpoints with a feature flag; history and producer evidence remain untouched. Metrics/logs must allow
slow-query, 429, cursor failure and partial-retention monitoring without sensitive labels.

## Acceptance boundary

Technical acceptance requires deterministic domain tests, real PostgreSQL/Timescale bounds/order/retention and
plan evidence, Tenant/RBAC/audit/privacy tests, exact producer-overlay contracts, frontend accessibility and real
Chromium replay. US-53 may become `TECHNICALLY_COMPLETE / ACCEPTANCE_PENDING` while external evidence is absent.

Final acceptance requires genuine retained provider/device telemetry, an authorized Vehicle/Trip range,
chronological playback, a real stop and gap where safely available, route context, eligible overlay provenance,
privacy review and operator sign-off. It may become `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`
without losing technical closure. Every overlay retains its producer's acceptance label; display never upgrades it.

## Risks and unresolved decisions

Known risks are high point density, compressed-chunk plan variance, map-tile availability, sensitive location
exposure, ambiguous Trip attribution and users mistaking technical-only overlays for field-accepted facts. The
frozen bounds, status labels, no-store responses, server-derived authority and plan gates mitigate them.

Implementation-blocking unresolved decisions: **NONE**. Export and engine-state comparison are explicit
Phase-1 non-goals, not unresolved items.

## Controlled implementation change sets

1. `US-53-REPLAY-JOURNEYS-CS01-DOMAIN-QUERY-CONTRACTS-001`
2. `US-53-REPLAY-JOURNEYS-CS02-TIMESCALE-QUERY-ADAPTERS-001`
3. `US-53-REPLAY-JOURNEYS-CS03-STOP-ANALYSIS-001`
4. `US-53-REPLAY-JOURNEYS-CS04-API-RBAC-AUDIT-001` — includes permission-only V93
5. `US-53-REPLAY-JOURNEYS-CS05-FRONTEND-001`
6. `US-53-REPLAY-JOURNEYS-CS06-INCIDENT-OVERLAYS-001`
7. `US-53-REPLAY-JOURNEYS-CS07-POSTGRES-PERFORMANCE-OPERATIONS-001`
8. `US-53-REPLAY-JOURNEYS-TECHNICAL-CLOSURE-001`
9. `US-53-REPLAY-JOURNEYS-FINAL-ACCEPTANCE-001`

The exact next queue is `US-53-REPLAY-JOURNEYS-FINAL-ACCEPTANCE-001`; run it only when the required genuine
retained provider/device journey evidence and operator sign-off are available.
