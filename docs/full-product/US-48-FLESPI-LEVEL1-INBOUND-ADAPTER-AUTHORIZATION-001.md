# US-48 flespi Level-1 Inbound Adapter Authorization

**Task:** `US-48-FLESPI-LEVEL1-INBOUND-ADAPTER-AUTHORIZATION-001`  
**Decision:** APPROVED  
**Adapter:** Level 1 / implementation complete / real capture pending  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`  
**Flyway:** V74; V75 not authorized  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining

## Authorized boundary

One provider-specific adapter is authorized under `com.transportlogistics.app.tracking.adapters.inbound.flespi`. flespi DTOs, transport responses, cursors and mapping helpers must remain private to that adapter package. They may not appear in Tracking domain models, application contracts, ports, persistence, human responses, Fleet, Trip, Routing, Reporting, or US-73 exchange machinery.

The adapter translates only external provider authentication and representation. It must produce the existing provider-neutral position JSON and invoke the unchanged `POST /api/integration/v1/tracking/positions` endpoint. The existing controller remains the sole authority for provider-key binding lookup, ACTIVE lifecycle, trusted Tenant/provider alias, `IntegrationSecretResolver`, HMAC verification, binding-scoped nonce reservation, admission limits, same-Tenant device resolution, source-time Vehicle association, dedupe and ingestion.

No domain, schema, V74, public human API, permission, event, outbox, retention, trust, freshness, connectivity, dedupe, association or Tenant-authority change is authorized.

## Initial transport: bounded REST polling

Phase 1 selects **flespi REST polling**, not MQTT and not webhook. This is the minimum surface that works before a real payload is captured:

- HTTPS only, using a narrowly scoped flespi ACL token.
- Default poll interval: 5 seconds, configurable with a safe lower bound of 5 seconds.
- Query only the configured flespi device and a bounded incremental message window.
- Page size at most 500, matching the existing Tracking batch limit; never request unbounded history.
- Maintain an in-memory high-water mark using the documented provider timestamp plus confirmed immutable message identity when available.
- On cold start or cursor loss, read a bounded overlap window of at most five minutes. Existing Tracking dedupe, not the adapter cursor, is authoritative.
- Retry transient network/429/5xx failures with bounded exponential backoff of 5, 15, 30 and 60 seconds plus jitter; reset after a successful poll.
- Do not retry invalid mapping/authentication failures indefinitely. Expose sanitized degraded health and bounded failure metrics.
- Buffer no more than one 500-message page plus the current outbound batch. When downstream is unavailable, stop advancing the watermark and retry; never create an unbounded memory queue.

MQTT/webhook may be reconsidered only through a later authorization if real acceptance evidence proves REST polling cannot meet the frozen behavior. Kafka is not authorized.

## Provider authentication and secret handling

The existing V74 provider binding remains authoritative:

`providerKeyId -> trusted Tenant -> providerAlias -> credentialReference -> IntegrationSecretResolver`.

The opaque credential reference resolves a least-privilege flespi token. The adapter uses that token over TLS to read only the selected channel/device. For the local bridge, the same high-entropy resolved secret is the HMAC-SHA256 key used by the existing ingress. The adapter constructs a fresh epoch and cryptographically random nonce for each local request and signs exactly:

`epoch + "\n" + nonce + "\n" + providerKeyId + "\n" + providerAlias + "\n" + rawBody`.

The token must exist only in approved runtime secret infrastructure and transient zeroed memory where practical. It must never be committed, persisted in Tracking or Integration tables, embedded in configuration files, returned, logged, audited, exposed as a metric label, or included in test reports/screenshots. Rotation replaces the value behind the opaque reference and is verified with the binding's existing credential-update process.

The adapter must never accept Tenant authority from flespi customer/account metadata, device metadata, payload/query values or caller headers. The configured provider key selects the existing binding; the binding alone selects Tenant.

## Invocation decision

**Choice B is frozen:** invoke the existing HTTP ingestion endpoint over local/private application routing. Direct invocation of `TrackingUseCase` is not authorized because it would bypass the controller's concrete HMAC, nonce, provider-binding and admission chain or require duplicating that security logic.

Loopback/private routing must not expose an additional public endpoint. It must retain the endpoint's existing 1 MiB and 500-position limits and process its normal response/error contract. A successful flespi fetch is not a successful Tracking ingest; advance the adapter watermark only after the existing endpoint accepts or safely identifies an idempotent duplicate.

## Field mapping status

Field names must be confirmed from the actual FMC130-generated flespi capture before final acceptance. Documentation-aligned fixtures are allowed for implementation tests but are not real evidence.

| Normalized field | Candidate flespi field | Status | Rule |
| :--- | :--- | :--- | :--- |
| Device identity | `ident` | `CONFIRMED_FROM_PROVIDER_DOCS / PENDING_REAL_CAPTURE` | Map through configured Tracking device UUID; never expose full IMEI |
| Source timestamp | `timestamp` | `CONFIRMED_FROM_PROVIDER_DOCS / PENDING_REAL_CAPTURE` | Device/provider observation time; never adapter poll/receipt time |
| Latitude | `position.latitude` | `CONFIRMED_FROM_PROVIDER_DOCS / PENDING_REAL_CAPTURE` | WGS84; existing validation remains authoritative |
| Longitude | `position.longitude` | `CONFIRMED_FROM_PROVIDER_DOCS / PENDING_REAL_CAPTURE` | WGS84; existing validation remains authoritative |
| Horizontal accuracy | Candidate `position.accuracy` | `PENDING_REAL_CAPTURE` | Omit and preserve UNKNOWN if absent |
| Speed | Candidate `position.speed` | `PENDING_REAL_CAPTURE` | Map units explicitly to km/h; omit if uncertain |
| Heading | Candidate `position.direction` | `PENDING_REAL_CAPTURE` | Map only confirmed degrees `[0,360)` |
| Engine state | Candidate ignition I/O parameter | `PENDING_REAL_CAPTURE` | Map only a confirmed FMC130 ignition signal; otherwise UNKNOWN |
| Odometer | Device/can mileage parameter | `PENDING_REAL_CAPTURE` | Observation only; never calculate or mutate Fleet |
| Engine hours | Device engine-hours parameter | `PENDING_REAL_CAPTURE` | Observation only; never synthesize |
| Provider message ID | flespi message identity, if immutable | `PENDING_REAL_CAPTURE` | Never use arrival array index |
| Provider sequence | Teltonika/provider sequence, if stable | `PENDING_REAL_CAPTURE` | Otherwise null |

Provider alias is the configured/binding alias, not payload authority. Safe metadata must be allowlisted and bounded; unrestricted raw flespi JSON is never persisted.

## Delivery, dedupe and failure semantics

- Delivery is at least once. No exactly-once claim.
- Transport overlap/retry may repeat provider events; existing Tracking dedupe is authoritative.
- The adapter must not implement a second business dedupe store or database table.
- Mapping errors reject only the affected provider message, increment a bounded counter and record no raw/coordinate-rich ordinary log.
- Provider unavailability degrades only adapter/provider health. Tracking remains running and serves last-known state.
- Respect existing Tenant/provider admission limits; no adapter bypass.
- Ordinary logs exclude token, credential reference, HMAC/signature/nonce, full IMEI/serial, raw payload and coordinates.
- Metrics may include received, mapped, rejected, provider error, mapping error, retry count and bounded latency, labeled only with a bounded provider alias/result.
- Health may expose configured/reachable, last successful provider poll/message and sanitized last mapping-error category. One stale device must not imply provider-wide outage.
- Configuration changes may be audited; telemetry packets must not create per-packet audit or P1-01/outbox records.

## Required verification

Implementation tests must cover token-authenticated provider fetch, scoped device query, payload parsing, missing mandatory fields, source-time mapping, WGS84 validation, optional accuracy/speed/heading/ignition/meter facts, missing optional values, immutable message identity, duplicate overlap/retry, invalid/oversize responses, Tenant-authority rejection, exact local HMAC canonicalization, nonce uniqueness, secret/log/metric privacy, 429/5xx retry/backoff, bounded pagination/buffer, downstream failure without watermark advancement, recovery and provider failure isolation.

Controlled documentation-aligned fixtures are authorized for implementation only. After hardware arrives, actual FMC130-generated flespi evidence must confirm timestamp, coordinate, accuracy, speed, direction, ignition, message identity/sequence, retry/history and error behavior. Mapping-only corrections inside the adapter are permitted; any need for V75, domain/application-port/public-API/permission/event/Tenant/trust changes is a mandatory stop.

## Rollback

Disable the adapter scheduler/configuration and revoke/rotate the flespi token. Preserve the existing provider-neutral HTTP ingress, V74 data/history/projections and all Tracking behavior. Do not delete data, roll back schema, weaken authentication or alter the domain.

## Closure

Authorization is limited to the Level-1 adapter described above. Hardware, flespi account and real telemetry remain unavailable; `REAL_DEVICE_REAL_PROVIDER` final acceptance remains blocked. US-49 must not start.

## Implementation cross-reference

The authorized boundary is implemented and verified in
`US-48-FLESPI-LEVEL1-INBOUND-ADAPTER-IMPLEMENTATION-001.md`. This cross-reference does not alter the
frozen authorization or constitute real-device/real-provider evidence.

**Next task:** `US-48-FMC130-FLESPI-EXTERNAL-CAPTURE-001`
