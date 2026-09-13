# ADR — Hybrid Pluggable Telemetry Platform

**Status:** ACCEPTED / MVP PROMOTED  
**Date:** 2026-09-13  
**Scope:** US-48 and Wave C Tracking platform enabler

## Decision

Promote the provider-neutral multi-gateway ingestion pipeline, Redis live-state projection,
TimescaleDB historical position store, gateway administration experience, and live Fleet map
into the current 87-story MVP as technical scope of US-48 and shared infrastructure for
US-49 through US-55. This promotion does not create a new story ID and does not mark US-48
accepted without its existing physical-device evidence.

The implementation extends the V75/V76 provider-connection and device-binding aggregates.
It must not introduce a singleton Tenant gateway configuration or persist webhook secrets or
API tokens. Credentials remain opaque references resolved transiently through
`IntegrationSecretResolver`.

## Trust and normalization boundary

The canonical external trust boundary remains `POST /api/integration/v1/tracking/positions`.
Flespi, Traccar and Generic JSON adapters normalize only after an ACTIVE provider key has
resolved the trusted Tenant, provider type and alias. A URL segment, request body or caller
Tenant header is never Tenant authority. HMAC, timestamp, nonce, size, rate, active connection,
active device binding and source-time Vehicle association validation remain mandatory.

Provider normalizers are adapters around provider-neutral application commands. Raw payloads,
credentials and vendor DTOs never enter the domain or persistence model. Traccar speed is
converted from knots to kilometres per hour using exactly `1.852`.

## Hybrid storage

- Redis owns the replaceable hot projection and Tenant-qualified ingestion stream. Keys use
  `tracking:live:{tenantId}:{vehicleId}` and `tracking:stream:{tenantId}`.
- TimescaleDB owns append-only normalized historical telemetry and retention/chunk policy.
- PostgreSQL continues to own provider/device configuration, nonce/dedupe authority, audit,
  evaluation state and other transactional Tracking records.
- Accepted ingress updates the Redis live projection before acknowledging and appends a bounded
  stream item for micro-batch persistence. Stream acknowledgement occurs only after the
  historical transaction commits.
- Redis is not a source of durable truth. On outage, ingress fails closed with an observable
  service-unavailable result; it must not silently claim a live update. Historical reads never
  depend on Redis. Recovery replays unacknowledged stream entries idempotently.
- Every key, stream item, history row and query contains and enforces `tenant_id`.

TimescaleDB is a required production capability for this promoted path. Local Compose and
PostgreSQL acceptance environments use a TimescaleDB PostgreSQL 16 image. Tests that do not
exercise the hybrid adapter may use a profile-scoped compatibility implementation; production
must fail readiness when Redis or TimescaleDB is unavailable.

## APIs and UI

Existing provider-connection APIs remain the management authority and are enhanced for the
three installed types: `FLESPI`, `TRACCAR` and `GENERIC`. Test Connection remains non-ingesting.
The operator UI exposes safe provider-specific fields, a copyable ingress URL and connection
test action without returning credential references or values.

`GET /api/v1/tracking/vehicles/live` is the Redis-backed, same-Tenant live-map read. It requires
`TRACKING_VIEW`, uses bounded pagination/filtering, and exposes no Driver or Customer PII.
The map uses OpenStreetMap tiles through React-Leaflet, polls every ten seconds only while
visible and online, and labels its status policy explicitly. Trip replay remains a link to the
subsequent replay capability rather than fabricated history behavior.

## Migration allocation and delivery sequence

V86 is allocated to the hybrid telemetry foundation because it is the next free Tracking
migration. US-52 route-geometry persistence moves to V87 and must recheck the head before
implementation. Historical migrations remain immutable.

Implementation is divided into atomic change sets:

1. `HYBRID-TELEMETRY-TS01-V86-INFRASTRUCTURE`
2. `HYBRID-TELEMETRY-TS02-NORMALIZERS-AND-SECURE-INGRESS`
3. `HYBRID-TELEMETRY-TS03-REDIS-HOT-PATH-AND-MICROBATCH`
4. `HYBRID-TELEMETRY-TS04-GATEWAY-SETTINGS-UI`
5. `HYBRID-TELEMETRY-TS05-LIVE-FLEET-MAP`
6. `HYBRID-TELEMETRY-TECHNICAL-CLOSURE`

Story accounting remains 73/87. The next executable task is TS01.

## Consequences and rollback

This adds Redis, TimescaleDB, Spring Data Redis, Leaflet and React-Leaflet as approved runtime
dependencies. Deployment must provide monitored Redis durability/eviction settings and a
Timescale-capable PostgreSQL service. Rollback disables the hybrid ingress/read adapters and
returns to the existing signed PostgreSQL path; it never drops accepted history or rewrites a
historical migration.
