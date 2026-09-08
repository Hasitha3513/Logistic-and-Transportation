# US-48 Track Vehicles Live Product Decisions

**Task:** `US-48-LIVE-VEHICLE-TRACKING-PRODUCT-DECISIONS-001`
**Decision:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`
**Date:** 2026-09-08
**Owner:** dedicated top-level `tracking` bounded context
**Current Flyway head:** V72; implementation must discover the next free forward version
**Program accounting:** unchanged at 72 / 87 complete and 15 / 87 remaining
**Next task:** `US-48-LIVE-VEHICLE-TRACKING-IMPLEMENTATION-001`

## Source intent

The exact source actor is **Tracking / Control Room Operator**, shortened in the canonical story to **Tracking Operator**. The exact story is: **“As a Tracking Operator, I want to view live location, movement, last location, connectivity and GPS accuracy, so that fleet movement can be monitored.”** Priority is High and the related feature is `Live Tracking`.

The exact acceptance criteria are:

1. Current location/status is shown when live data exists.
2. Connectivity loss displays last-known location and timestamp.
3. Accuracy/staleness is visible.

The named source use cases are `View Vehicle on Map`, `Track Vehicle Status`, `Detect Connectivity Loss`, `Display Last Known Location`, `Display GPS Accuracy`, and `Show Data Freshness / Timestamp`. The activity selects a Fleet/Vehicle, loads latest tracking data, shows current position/status/accuracy/source timestamp when fresh, otherwise shows connectivity loss plus the last-known position/timestamp, and refreshes. Source entities are `TrackingDevice` and `PositionEvent`.

The source supports location, movement, Vehicle status, device state/last-seen, position events, source timestamp, accuracy, freshness/staleness, connectivity loss, last-known fallback, a map view, and refreshed display. It does **not** specify a GPS vendor/device, device serial schema, ingestion protocol, credential mechanism, coordinate reference system, numerical freshness/clock/accuracy thresholds, packet identity or sequence, retention period, scale, throughput/latency target, raw-payload policy, streaming transport, map provider, assignment cardinality, temporary reassignment, purge API, downstream event contract, or storage extension. The numerical and technical choices below are explicit ARB Phase-1 assumptions, not claimed source facts.

## Bounded context and ownership

ARB ratifies a dedicated top-level `tracking` bounded context. Its distinct high-write telemetry lifecycle, effective-dated device association, immutable ordered history, deduplication, trusted-current projection, privacy boundary, retention metadata, bounded time-series reads, and foundational dependency role for US-49 through US-55 justify independent ownership. It remains one module in the existing Spring Modulith, not a microservice or generic IoT platform.

Tracking owns provider-neutral ingestion normalization, its local operational device-reference registry, device-to-Vehicle association history, accepted normalized position history, dedupe/conflict identity, order/late classification, trust/quality classification, last-received and last-trusted projections, freshness/connectivity derivation, retention metadata, safe operator queries, and adapter health observations.

- Fleet owns Vehicle identity/master, registration, lifecycle/status, ownership and operational availability. Tracking stores a same-Tenant logical Vehicle UUID only and must use a published Fleet projection; no Vehicle copy, repository access, join, or physical foreign key.
- Trip owns lifecycle, Driver/Vehicle assignment and execution. Tracking may consume a minimized active-assignment fact through a published contract but US-48 neither persists Driver identity in position history nor requires Trip assignment to ingest. No Trip repository access.
- Routing owns planned routes, versions, stops, geometry and constraints. US-48 exposes actual accepted position facts only; US-52 later owns comparison/deviation.
- Organization owns depots, branches, customer sites and location/site master. Tracking does not duplicate sites.
- Integration US-73 owns governed provider configuration and opaque credential-reference lifecycle when extended for telematics. It does not transport high-rate packets, own Tracking business state, retry every telemetry message, or become an ingestion bottleneck.

US-49 owns geofence definitions/evaluation/entry-exit alerts; US-50 owns speed limits and violations; US-51 owns idle detection/duration/fuel-waste interpretation; US-52 owns planned-versus-actual route deviation; US-53 owns replay/stop analysis/forensics UI; US-54 owns the consolidated dashboard; US-55 owns signal-loss exception lifecycle, spoof/tamper/battery/delayed-packet detectors and trust overrides. US-48 supplies only normalized foundational facts and does not implement those detectors.

## Device and Vehicle association

Tracking owns a narrow local operational device-reference registry, not telecom inventory, SIM lifecycle, firmware management, procurement, warranty, billing, or remote device control. `TrackingDevice` has: `id`, `tenantId`, required normalized `externalDeviceReference`, required `providerAlias`, optional bounded `hardwareSerialReference`, lifecycle `ACTIVE|DISABLED`, `registeredAt`, `registeredBy`, optional `lastSeenAt`, optimistic `version`, and audit timestamps. The external reference and optional serial are sensitive operational identifiers and are masked from ordinary views.

An effective-dated association contains `id`, `tenantId`, `trackingDeviceId`, logical `vehicleId`, `effectiveFrom`, nullable `effectiveTo`, `createdAt`, and `createdBy`. Exactly one active association per device and exactly one active tracking device per Vehicle are allowed in Phase 1. Temporary reassignment is supported only by explicitly closing the old interval and opening a new interval atomically; intervals for the same device or Vehicle cannot overlap. History is immutable and never re-parented.

An ingested packet resolves the association covering its `sourceTimestamp`, not merely the current association. No covering association returns `TRACKING_DEVICE_NOT_ASSOCIATED`; overlapping ambiguous data fails closed. Association change and ingestion serialize on the Tenant/device boundary so a packet cannot be assigned nondeterministically.

## Provider, protocol and trust boundary

Core strategy is `PROVIDER_NEUTRAL`; no vendor SDK or vendor-shaped domain type is approved. Phase-1 internal ingestion is a provider-neutral application port supporting single and bounded batch normalized messages. The first external adapter contract is authenticated **HTTP JSON over TLS** at `POST /api/integration/v1/tracking/positions`; a batch contains at most 500 messages and 1 MiB. MQTT, raw TCP device protocols, polling, file import and direct browser ingestion are deferred.

External ingress terminates in a Tracking-owned adapter. US-73 may govern provider alias/configuration and an opaque credential reference after its inbound/telematics capability is explicitly extended, but actual high-rate telemetry bypasses US-73 exchanges and P1-01. Tenant authority is resolved from the authenticated provider/device configuration and association; payload `tenantId` is absent or, if a provider mapping supplies it internally, non-authoritative and must match trusted context.

Device/provider authentication is a signed provider request using a server-side secret resolved from an opaque credential reference, with timestamp and nonce replay protection. TLS is mandatory. Plaintext API keys/secrets are never stored in Tracking tables, returned, logged, or audited. mTLS may be added by infrastructure later but is not a Phase-1 prerequisite. Human JWT/RBAC is never accepted for device ingress.

Acceptance tiers are frozen as follows:

- implementation and deterministic tests: `CONTROLLED_PROTOCOL_FIXTURE`;
- technical closure: controlled fixture/simulator plus real PostgreSQL load, concurrency and browser evidence;
- final acceptance: `REAL_DEVICE_REAL_PROVIDER`, using at least one physical GPS device and its real provider-generated payloads through the chosen adapter.

A provider sandbox may supplement but not replace physical-device evidence. If no real provider/device is available, final acceptance is `BLOCKED_BY_EXTERNAL_SYSTEM`; fixture evidence must never be relabelled as real-source acceptance.

## Normalized telemetry contract

Each normalized position contains required `tenantId` (trusted context, not payload authority), `deviceId`, resolved `vehicleId`, `sourceTimestamp`, server `receivedAt`, WGS84 `latitude` and `longitude`, `providerAlias`, and dedupe identity; optional facts are `providerMessageId`, `providerSequence`, `horizontalAccuracyMeters`, `speedKph`, `headingDegrees`, `altitudeMeters`, `engineState`, `odometerKm`, `engineHours`, and bounded safe provider metadata. Raw credentials, unrestricted metadata and Driver/Customer identity are forbidden.

- Coordinates use WGS84 decimal degrees. Latitude is finite `[-90,90]`; longitude is finite `[-180,180]`. Missing, NaN, infinite or out-of-range values are rejected.
- `sourceTimestamp` is the device/provider observation instant and is immutable. `receivedAt` is the server acceptance instant. Both are stored as UTC instants; source time is never replaced by receipt time.
- `horizontalAccuracyMeters` is optional, finite and `>= 0`. Absence means `UNKNOWN`, never zero. Values above 1,000 m are accepted as `UNTRUSTED/POOR_ACCURACY` history but cannot advance trusted state.
- Optional speed is finite `0..400 km/h`; heading is finite `[0,360)`; engine state is `ON|OFF|UNKNOWN`. Odometer and engine-hours are non-negative observations only and never overwrite Fleet readings.
- Impossible movement/spoof inference is deferred to US-55. US-48 records enough immutable time/coordinate/accuracy facts for later evaluation but makes no accusation.

## Freshness, connectivity and trusted state

The following thresholds are ARB operational assumptions and configurable server policy with these defaults; responses include the evaluated policy version:

- `LIVE`: trusted source age `<= 60 seconds` and the latest successful provider receipt age `<= 60 seconds`;
- `RECENT`: trusted source age `> 60 seconds and <= 5 minutes`;
- `STALE`: a trusted position exists but source age is `> 5 minutes`;
- `UNKNOWN`: no trusted position exists.

Negative source age within 120 seconds is tolerated as clock skew but marked `CLOCK_SKEW`; more than 120 seconds in the future is retained as untrusted and does not advance projections. A packet older than the current trusted source time is `OUT_OF_ORDER`; it remains append-only history if within the active retention window and valid. A packet older than 24 hours at receipt is `LATE`; it may remain history but cannot advance current/trusted projections. A packet older than the configured retained-history boundary is rejected `TOO_OLD`. Bad clocks are never silently rewritten.

Connectivity is `CONNECTED|DEGRADED|OFFLINE|UNKNOWN`: CONNECTED when last successful provider receipt is within 60 seconds; DEGRADED when over 60 seconds through 5 minutes; OFFLINE after 5 minutes; UNKNOWN before any accepted receipt. This is observed ingestion connectivity, not proof of provider-wide outage. `LIVE` additionally requires a trusted point; connected but poor-quality telemetry is not live.

`latestReceived` is the most recently received valid normalized packet even if untrusted. `latestTrusted` is the greatest deterministically ordered trusted observation. `lastKnownPosition` shown to operators is `latestTrusted`; untrusted, duplicate, conflicting, future or older packets never replace it. Equal source times order by provider sequence when present, then stable event identity; two different payload hashes for the same canonical identity are an integrity conflict and neither may nondeterministically win.

Trust is `TRUSTED|UNTRUSTED|UNKNOWN`. Valid coordinates, an effective association, acceptable clock/age and accuracy `<= 1,000 m` are required for TRUSTED. Missing accuracy yields `UNKNOWN` quality but may advance a distinct last-known observation only when explicitly labelled `ACCURACY_UNKNOWN`; it is not labelled precise. Manual trust override belongs to US-55 and is absent in US-48.

## Dedupe, ordering and atomicity

Primary identity is `(tenantId, providerAlias, providerMessageId)` when the provider supplies a stable message ID. Otherwise it is SHA-256 over canonical `(tenantId, deviceId, sourceTimestamp, coordinates, providerSequence-or-null)`. Provider sequence improves ordering but is optional. Exact replay produces one accepted history row, one projection effect and no duplicate downstream effect. Same identity with a different canonical payload returns `TRACKING_POSITION_CONFLICT` and preserves the original.

Within one Tracking-owned PostgreSQL transaction, ingestion authenticates/resolves association, reserves dedupe identity, appends immutable history, and conditionally advances latest-received/latest-trusted using compare-and-set ordering. A rollback leaves none of those effects. Accepted position rows are append-only; corrections are new classified facts, never updates to original source data.

## Persistence, retention and scale

Phase 1 uses ordinary relational PostgreSQL. PostGIS, TimescaleDB, Kafka, Redis and table partitioning are not required for US-48 and are deferred pending measured need; US-49 must separately decide PostGIS. Tracking owns expected tables `tracking_device`, `tracking_vehicle_device_assignment`, `tracking_position`, `tracking_vehicle_latest`, and `tracking_ingest_identity`. The final design may merge ingest identity into position only if all dedupe/conflict invariants remain database-enforced. No migration is created or numbered by this decision task.

All tables are Tenant-owned with same-module Tenant-consistent keys. Vehicle, Trip, Routing, Organization, Identity and Integration identifiers are logical references without physical foreign keys. Required indexes are Tenant-leading: `(tenant_id, vehicle_id, source_timestamp DESC, id DESC)`, `(tenant_id, device_id, source_timestamp DESC, id DESC)`, latest lookup by `(tenant_id, vehicle_id)`, active association uniqueness, and the chosen provider/canonical dedupe uniqueness.

Retention duration is `EXTERNAL_POLICY`: the source/legal policy supplies no period. Implementation must persist policy identifier/version and `retainUntil` metadata on history; until configured, automatic purge is disabled and capacity monitoring is mandatory. Last-known projection is retained while the device/Vehicle reference remains operational and can be rebuilt from retained immutable history. There is no public purge API. A future authorized Tenant-qualified maintenance job may purge only expired history in bounded batches and audit policy/action; it cannot rewrite remaining facts.

ARB initial load target—not source fact—is 10,000 active vehicles, one message per vehicle per minute sustained (about 167 messages/s and 14.4 million rows/day), with a 5x burst for 15 minutes. Technical closure must demonstrate at least 200 accepted messages/s sustained and 1,000 messages/s burst on the acceptance environment, p95 single latest lookup <= 200 ms, p95 24-hour single-Vehicle history page <= 500 ms, no N+1 foreign lookups, and index-backed plans. Because retention duration is external, capacity must be reported per retained day; partitioning is revisited from measured plans and maintenance impact.

## Events and P1-01

Raw telemetry is `LOCAL_STATE_ONLY`: no durable P1-01 event or Spring event is emitted per packet. This prevents an outbox/event storm. US-48 freezes one optional coalesced contract, `VehicleTrackingStateChangedV1`, but it remains `NOT_ACTIVATED_NO_CURRENT_CONSUMER` until US-49..55 registers a consumer. When activated, it may emit only on Vehicle change of freshness/connectivity/trust state or at most once per Vehicle per minute for a materially newer trusted position; it uses the P1-01 envelope and `DurableEventPublisher`, at-least-once delivery, Tenant/event dedupe, no global ordering, and no Driver/Customer/device secret/raw payload.

No second outbox is permitted. Future detector implementations may consume an in-process Tracking position port or an approved coalesced/durable family based on their failure semantics; they may not query Tracking tables.

## APIs and live updates

Human operator API:

- `GET /api/v1/tracking/vehicles` — bounded fleet tracking state list;
- `GET /api/v1/tracking/vehicles/{vehicleId}/latest` — latest-received/trusted, last-known, freshness/connectivity/accuracy;
- `GET /api/v1/tracking/vehicles/{vehicleId}/positions?from=&to=&cursor=&limit=` — immutable bounded history;
- `GET /api/v1/tracking/devices` and `GET /api/v1/tracking/devices/{deviceId}`;
- `POST /api/v1/tracking/devices`, `PUT /api/v1/tracking/devices/{deviceId}`, and explicit `activate|disable` commands;
- `POST /api/v1/tracking/devices/{deviceId}/associations` and `POST .../associations/{associationId}/end`.

There is no generic status PATCH, public purge/export/raw-payload/credential route, arbitrary provider-success route, detector route, or customer route. Lists default to 20 and cap at 100. History requires `from` and `to`, caps a request at 24 hours, uses opaque keyset cursor `(sourceTimestamp,id)`, defaults to 100 and caps at 500, and orders `sourceTimestamp DESC, id DESC`. No offset pagination or unbounded history read.

US-48 live UI transport is **polling**, not SSE/WebSocket. It requests latest state every 15 seconds while visible, backs off to 30 then 60 seconds after failures, pauses while hidden/offline, and refreshes immediately on focus/reconnect. US-54 may separately approve streaming after measured fanout needs. This choice adds no browser connection-state or backpressure infrastructure prematurely.

Stable errors are `TRACKING_VEHICLE_NOT_FOUND`, `TRACKING_DEVICE_NOT_FOUND`, `TRACKING_DEVICE_ALREADY_ASSOCIATED`, `TRACKING_VEHICLE_ALREADY_ASSOCIATED`, `TRACKING_DEVICE_NOT_ASSOCIATED`, `TRACKING_ASSOCIATION_CONFLICT`, `TRACKING_POSITION_INVALID`, `TRACKING_POSITION_DUPLICATE`, `TRACKING_POSITION_CONFLICT`, `TRACKING_POSITION_TOO_OLD`, `TRACKING_TENANT_MISMATCH`, `TRACKING_PROVIDER_UNAUTHORIZED`, `TRACKING_RATE_LIMITED`, and `TRACKING_STALE_VERSION`. Exact duplicate ingress is an idempotent success with original result; invalid input is 400, authentication/authorization 401/403, safe absence 404, conflict/stale version 409, and rate limit 429 through the platform error envelope.

## RBAC, tenancy, privacy, audit and operations

Exact human permissions are `TRACKING_VIEW`, `TRACKING_HISTORY_VIEW`, and `TRACKING_DEVICE_MANAGE`. `TRACKING_INGEST` is a service capability, not a human role permission; the external adapter uses provider authentication and an internal authority. No `GPS_ADMIN` exists.

Every registry, association, packet, identity, latest projection, audit fact, idempotency key and query is Tenant-scoped. Tests cover same external reference in Tenants A/B, device/association mutation, ingress resolution, latest/history, guessed Vehicle/device IDs, and any future stream. Cross-Tenant resources are safe not-found and never reveal existence.

Precise Vehicle location is sensitive operational data. Only same-Tenant users with `TRACKING_VIEW` receive latest coordinates; history additionally requires `TRACKING_HISTORY_VIEW`. Device hardware/external references are masked for viewers and visible only as necessary to device managers. APIs return no Driver name; a minimized logical active Driver ID may be exposed only by a later expressly approved Trip composition. No medical, performance, violation or personal data is exposed. US-48 never exposes fleet location to Customer Self-Service.

Audit is required for device create/update/enable/disable, association start/end/reassignment, provider-configuration reference changes, denied management commands, retention-policy changes and future administrative purge. Audit rows are not written per packet or ordinary latest query. History/export access audit is `EXTERNAL_POLICY`; security access logs retain safe actor/route/Tenant/correlation facts without coordinates.

Metrics include received, accepted, duplicate, conflict, invalid, late, out-of-order and untrusted counts; stale/offline devices; processing/DB latency; ingestion rate/backlog where applicable; provider authentication/failure counts; and latest successful ingest per Tenant/provider. Health reports adapter reachability/configured state, last successful ingest, ingestion lag and stale-device count. It never infers provider-wide outage from one stale device.

## Frontend

US-48 supplies a minimal operator tracking foundation: a Vehicle tracking list, selected Vehicle map point, latest/last-known facts, source time/age, explicit freshness and connectivity text, accuracy value or `UNKNOWN`, bounded history, and device-association management. It is not the US-54 fleet dashboard.

The map uses a provider-neutral frontend component contract; no tile/geocoding vendor is selected. Acceptance may use a deterministic local coordinate canvas/fixture while final real-source acceptance validates the physical-device position. If no approved map tile provider exists, the UI must still render coordinates and status truthfully rather than block tracking.

Freshness/connectivity/trust never rely on colour alone; text labels, icons and accessible names are required. Stale/offline state keeps the last trusted point visible with its source timestamp and age, clearly labelled `LAST KNOWN`, never `LIVE`. The frontend includes no Driver surveillance, Customer exposure, mobile tracker app, geofence/speed/idle/deviation/replay detectors, heat maps or alert dashboard.

## Verification and concurrency gates

Unit/application tests cover coordinate/optional observation validation, source-versus-receipt time, clock skew, accuracy, freshness/connectivity boundaries, trust, association-at-source-time, dedupe/conflict, out-of-order/late handling, immutable history, compare-and-set latest state, retention metadata, security/privacy and event suppression/coalescing.

The deterministic PostgreSQL race matrix is exactly:

1. exact duplicate packet;
2. same provider identity with different payload;
3. newer versus older arrival;
4. double active device association;
5. reassignment versus source-time ingest;
6. two competing latest-position updates;
7. same external device reference in different Tenants;
8. duplicate provider event identity in one batch/concurrent batches;
9. concurrent history append/latest projection transaction with forced rollback.

No sleep-based correctness is accepted. PostgreSQL evidence must prove clean migration, Tenant constraints, association uniqueness/non-overlap, dedupe uniqueness/conflict preservation, append-only history, association-at-source-time, trusted/latest correctness, rollback atomicity, out-of-order behavior, retention metadata, concurrency, index-backed plans and the frozen load target.

Final real E2E uses a physical provider-approved Device A associated to Vehicle A and proves a fresh provider point becomes latest; provider source time and actual accuracy are preserved; exact duplicate is idempotent; an older/delayed packet remains history without replacing latest; invalid coordinates fail; disconnect/staleness shows last-known with timestamp; reconnect restores state; reassignment preserves old Vehicle history and maps later source-time packets correctly; Tenant B and a limited role are denied; and provider credentials/raw payload are never exposed. All results must come from the isolated PostgreSQL acceptance database.

## Rollback and controlled implementation plan

The provider adapter is independently disableable. Bad provider configuration is isolated by provider/Tenant, untrusted facts remain marked and cannot advance trusted state, and latest projections can be rebuilt deterministically from retained immutable accepted history. Schema correction is forward-only; historical migrations and accepted source facts are never destructively rewritten.

Implementation is split into these independently verified changes:

1. `US-48-CS01`: Tracking module, device registry and effective-dated Vehicle association.
2. `US-48-CS02`: provider-neutral HTTP ingestion, validation, dedupe, ordering and trust.
3. `US-48-CS03`: append-only history, latest projections, freshness/connectivity and retention metadata.
4. `US-48-CS04`: RBAC, Tenant isolation, signed provider authentication, privacy and audit.
5. `US-48-CS05`: bounded query API, polling and minimal accessible operator frontend/map abstraction.
6. `US-48-CS06`: selected provider/device adapter and real-source acceptance harness.
7. `US-48-CS07`: deterministic concurrency, query-plan/load testing and operational hardening.

Implementation must re-check Fleet/Trip/Routing published contracts, US-73 capability extension and the next free migration before coding. This decision creates no production code, API, table, migration, permission or event.

## Decision gate result

Every required ownership, provider, device, association, telemetry, time, accuracy, freshness, connectivity, trust, dedupe, ordering, retention, persistence, performance, API, polling, RBAC, authentication, privacy, audit, observability, concurrency, PostgreSQL, real-provider, frontend, rollback and change-set decision is frozen. No vendor was invented and no US-49 through US-55 behavior leaked into US-48.
