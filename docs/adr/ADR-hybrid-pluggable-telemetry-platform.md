# ADR — Hybrid Pluggable Telemetry Platform

**Status:** ACCEPTED / MVP PROMOTED  
**Date:** 2026-09-16
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

## Supported-provider boundary

Plug-and-play means a supported source can be registered without changing Tracking domain or
telemetry-processing code. It does not claim universal proprietary-protocol compatibility. A
device is eligible when Flespi or Traccar supports its protocol, or when an authorized gateway
produces the governed Generic signed-HMAC contract. Other protocols require a reviewed adapter.

| Provider | Phase-1 mode | Authentication | Status |
| --- | --- | --- | --- |
| Flespi | production polling | environment-backed provider token | retained production capability |
| Traccar | production HTTPS polling | environment-backed opaque bearer token | authorized / implementation pending |
| Generic | governed inbound callback | signed HMAC, timestamp and nonce | retained integration path |
| Future | adapter extension | governed provider credential type | outside current implementation |

Flespi and Traccar are separate first-class adapters; neither is a fallback for the other.

## Trust and normalization boundary

The Generic external trust boundary remains `POST /api/integration/v1/tracking/positions`.
Flespi and the authorized Traccar path have separate outbound-polling trust boundaries inside their
registered adapters; all three converge only after server-side provider, Tenant and binding authority.
Flespi, Traccar and Generic JSON adapters normalize only after an ACTIVE provider key has
resolved the trusted Tenant, provider type and alias. A URL segment, request body or caller
Tenant header is never Tenant authority. HMAC, timestamp, nonce, size, rate, active connection,
active device binding and source-time Vehicle association validation remain mandatory.

Provider normalizers are adapters around provider-neutral application commands. Raw payloads,
credentials and vendor DTOs never enter the domain or persistence model. Traccar speed is
converted from knots to kilometres per hour using exactly `1.852`.

Traccar Phase 1 is HTTPS-only bounded `/api/positions` polling. Credentials are opaque
environment-resolved API tokens sent as Bearer authorization unless verified compatible API
evidence requires another token header. Username/password, Basic authentication, disabled TLS
verification, plaintext credential persistence and a direct unsigned callback are prohibited.
Timeouts, bounded pages, retry/backoff, rate-limit handling, circuit breaking and sanitized health
are mandatory. Traccar-origin callbacks may use Generic ingress only through an authorized signing
proxy that produces the existing HMAC contract.

## Provider-neutral topology and trust boundary

```text
Physical GPS devices
  ├─ Flespi Cloud ───────┐
  ├─ Traccar ────────────┼─> registered inbound adapters
  └─ custom gateway ─────┘      ├─ Flespi polling
                                ├─ Traccar polling
                                └─ Generic signed HMAC
                                      |
                        canonical V1/V2 compatibility boundary
                                      |
                  Kafka -> TimescaleDB history + Redis live projection
                                      |
                         US-48 and Tracking evaluators
```

```text
UNTRUSTED                              TRUSTED TRACKING BOUNDARY
device/provider -> TLS + credential/signature -> server Tenant + source-time binding
                                                -> normalization -> Kafka
No payload/frontend Tenant authority. No adapter writes Redis, TimescaleDB, legacy Tracking
tables or another module's persistence directly.
```

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

Story accounting remains 73/87. The current Flyway head is V95. Canonical V2 CS02 is complete;
the next executable task is `US-55-HANDLE-GPS-EDGE-CASES-CS03-PERSISTENCE-AUTHORIZATION-001`.

## Governed onboarding and operations

The administrator selects a provider, configures endpoint and opaque credential reference, tests
connectivity, discovers or identifies the external device, validates its IMEI/external identity,
binds it to the authenticated Tenant and exactly one authorized Vehicle for an effective interval,
validates a normalized sample, then explicitly activates ingestion. Safe health is redacted.
Disablement, rebinding and credential rotation preserve effective-dated audit history. One physical
device may have only one authoritative active provider binding per source-time interval; active-active
Flespi/Traccar ingestion is prohibited. Existing `TRACKING_DEVICE_MANAGE` is sufficient.

Provider polling uses restart-safe watermarks, bounded pagination, source-clock validation,
dedupe/out-of-order handling, bounded retry/backoff/rate limiting, circuit breaking and sanitized
last-success/health facts. Adapter disablement stops new polling without deleting history. Kafka
acknowledgement follows the governed durable boundary. Redis remains disposable; TimescaleDB
history is immutable. Dead-letter replay preserves Tenant and canonical identity. Observability
exposes neither secrets nor precise locations.

Canonical V1 remains immutable. V2 uses `tracking.telemetry.ingested.v2` and `.v2.dlt`, separate
serialization paths, consumer-first rollout, topic/envelope agreement and common cross-version/
cross-provider idempotency. Missing signal evidence remains absent, never inferred. Capabilities
are effective-dated registry facts rather than mutable claims repeated in each event.

## Remaining governed change sets

| Identifier / objective | Dependencies and boundary | Migration/API/frontend/security impact | Tests, stop, rollback and Definition of Done |
| --- | --- | --- | --- |
| `US-55-CS02` canonical V2 | V1 contract, Kafka and existing consumers; canonical boundary only | COMPLETE; no migration/API/UI/permission change | Contract/serializer/consumer/idempotency gates passed; rollback producer to V1 with dual consumers retained; DoD recorded in CS02 evidence |
| `US-55-CS03` persistence authorization | CS02 plus Tracking-owned provider/device tables | Decision-only until exact schema/index ownership is frozen; expected next-free forward migration; no API/UI/permission change | Stop before unapproved schema or cross-module persistence; rollback is no migration; DoD is explicit authorized DDL and test/rollback plan |
| `US-55-TRACCAR-ADAPTER` production polling | CS02/CS03, provider SPI, secret resolver and Traccar API evidence | No migration expected unless CS03 authorizes metadata; additive safe config only if proven; no new permission; UI remains truthful to installed types | Contract, TLS/auth, pagination, watermark, retry/rate-limit and normalization tests; stop on incompatible auth/API; rollback disables adapter; DoD is bounded production polling with redacted health |
| `US-55-PROVIDER-CAPABILITIES` effective-dated registry | CS03 and provider/device identity | Governed forward migration expected; provider-neutral read model may be additive; UI consumes server facts; Tenant and secret-redaction mandatory | PostgreSQL effective-time/Tenant tests; stop on mutable event claims or foreign ownership; rollback disables reads while retaining rows; DoD is auditable capabilities without inferred evidence |
| `US-55-ONBOARDING-BINDING` executable lifecycle | provider registry, Fleet Vehicle lookup and audit | Reuse management routes and `TRACKING_DEVICE_MANAGE`; migration only if separately authorized; UI not required in this slice | API/security/PostgreSQL race/audit tests; stop on active-active or frontend Tenant authority; rollback disables activation and preserves history; DoD is DRAFT-to-ACTIVE with one authoritative interval |
| `US-55-PROVIDER-UI` guided Flespi/Traccar/Generic setup | onboarding API and installed-provider metadata | No migration; additive feature UI; no secrets or new authorization semantics | TypeScript, Vitest, accessibility and Chromium RBAC tests; stop if backend contract is insufficient; rollback hides feature; DoD is truthful permission-filtered onboarding for installed adapters |
| `US-55-HEALTH-RECOVERY` operational behavior | adapters, Kafka/DLT, Redis and Timescale consumers | Metadata migration only if already authorized; additive safe health API/UI may require separate contract approval; no secret/location telemetry | Restart, stale, retry, rate-limit, circuit-breaker and DLT replay tests; stop on evidence loss; rollback disables automation; DoD is recoverable polling and redacted last-success/health |
| `US-55-IDEMPOTENCY-CLOSURE` convergence | V1/V2 identities and all provider adapters | No API/UI/permission change expected; migration only for an authorized uniqueness constraint/index | Kafka/PostgreSQL/Redis concurrency and replay tests; stop on legitimate-observation collapse; rollback to prior producer with dual consumers; DoD is one business acceptance across provider/version retries |
| `US-55-TECHNICAL-CLOSURE` full verification | all preceding implementation slices | No new behavior; documentation/evidence only | Full backend, PostgreSQL/Timescale, Kafka, Redis, architecture, static, frontend and Chromium gates; stop on independent defect; rollback offending slice; DoD is clean synchronized evidence with external hold preserved |
| `US-55-FIELD-ACCEPTANCE` genuine device journey | technically closed supported adapter and authorized operator/environment | No implementation or migration; acceptance evidence only | Genuine provider/device signal, outage/recovery/privacy/Tenant journey; stop if physical evidence is unavailable; no software rollback; DoD is independent sign-off—fixtures never qualify |

Every slice must record dependencies, architecture boundaries, migration/API/frontend/security
impact, tests, stop conditions, rollback and Definition of Done in its closure evidence.

## Consequences and rollback

This approves Kafka, Spring Kafka, Redis, TimescaleDB, Spring Data Redis, Leaflet and
React-Leaflet. Deployment must monitor Kafka durability, Redis eviction/readiness and TimescaleDB.
Rollback pauses producers/consumers while preserving offsets and history; it never drops accepted
history or rewrites a historical migration.
