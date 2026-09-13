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

## High-throughput topology

- Kafka is the durable ingestion backbone. Topic `tracking.telemetry.ingested.v1` is keyed by
  `{tenantId}:{vehicleId}` with idempotent production and `acks=all`. Independent consumer groups
  drive Redis, TimescaleDB and later approved Tracking detectors.
- Redis owns only the replaceable hot projection at
  `tracking:live:{tenantId}:{vehicleId}` with a sliding 24-hour TTL. Redis Streams are superseded
  and must not become a second durable telemetry backbone.
- TimescaleDB owns append-only normalized historical telemetry and retention/chunk policy.
- PostgreSQL continues to own provider/device configuration, nonce/dedupe authority, audit,
  evaluation state and other transactional Tracking records.
- Ingress returns `202 Accepted` only after Kafka acknowledges the normalized event. A dedicated
  Kafka consumer updates Redis, avoiding unsafe controller dual writes and enabling replay.
- The TimescaleDB consumer uses batches up to 500 and commits offsets only after its JDBC batch
  commits. Delivery is at-least-once and inserts are idempotent by Tenant and event identity.
- Every Kafka key/header/payload, Redis key/value, history row and query enforces `tenant_id`.

TimescaleDB and Kafka are required production capabilities. Tests may use profile-scoped
compatibility adapters, but production readiness fails when Kafka or TimescaleDB is unavailable.
Redis unavailability degrades live-map readiness without rejecting already durable telemetry or
bringing down Trip, Billing or Delivery transactions.

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

V86 remains the immutable hybrid foundation with one-day chunks. A forward V87 hardening
migration will configure 7-day chunks, Tenant/Vehicle segmented compression after 7 days and
180-day raw retention. US-52 route-geometry persistence moves to V88 and must recheck the head.

Implementation is divided into atomic change sets:

1. `HYBRID-TELEMETRY-TS01-V86-INFRASTRUCTURE`
2. `HYBRID-TELEMETRY-TS02-KAFKA-CONTRACT-AND-SECURE-INGRESS`
3. `HYBRID-TELEMETRY-TS03-KAFKA-REDIS-LIVE-PROJECTOR`
4. `HYBRID-TELEMETRY-TS04-V87-TIMESCALE-CONSUMER-AND-POLICIES`
5. `HYBRID-TELEMETRY-TS05-GATEWAY-SETTINGS-UI`
6. `HYBRID-TELEMETRY-TS06-LIVE-FLEET-MAP`
7. `HYBRID-TELEMETRY-TECHNICAL-CLOSURE`

Story accounting remains 73/87. TS01 is complete; the next executable task is TS02.

## Consequences and rollback

This approves Kafka, Spring Kafka, Redis, TimescaleDB, Spring Data Redis, Leaflet and
React-Leaflet. Deployment must monitor Kafka durability, Redis eviction/readiness and TimescaleDB.
Rollback pauses producers/consumers while preserving offsets and history; it never drops accepted
history or rewrites a historical migration.
