# US-48 flespi Level-1 Inbound Adapter Implementation

**Task:** `US-48-FLESPI-LEVEL1-INBOUND-ADAPTER-IMPLEMENTATION-001`  
**Result:** `IMPLEMENTATION_COMPLETE / REAL_CAPTURE_PENDING`  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`  
**Flyway:** V74; no V75  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining

## Implemented boundary

The provider-specific implementation is contained in
`com.transportlogistics.app.tracking.adapters.inbound.flespi`. It adds bounded HTTPS REST polling,
provider-response mapping, a private signed loopback bridge, bounded adapter state, Micrometer metrics,
and an actuator health contributor. No flespi type crosses into Tracking domain/application contracts,
ports, persistence, Fleet, Trip, Reporting, or Integration exchange processing.

No database migration, table, public endpoint, permission, event family, outbox path, or packet audit was
added. The existing V74 provider binding, provider-neutral Tracking ingress, dedupe, Tenant isolation,
source-time association, trust, retention, and projection behavior remain authoritative.

## Transport, authority, and secrets

- Transport is HTTPS REST polling only; MQTT, webhook, Kafka, TCP, SSE, and WebSocket are absent.
- Polling defaults to five seconds and normalizes values below five seconds to the frozen minimum.
- Each request is scoped to the configured flespi device and bounded to at most 500 messages and a
  1 MiB provider response.
- The default cold-start overlap is five minutes and cannot exceed five minutes.
- The V74 `providerKeyId` binding must be ACTIVE and its provider alias must match configuration.
- Tenant authority comes only from that binding. Provider account/customer/Tenant payload data is ignored.
- The adapter resolves the opaque binding `credentialReference` through `IntegrationSecretResolver`.
  No token or secret value is committed or persisted, and the transient character buffer is cleared after use.

## Mapping and delivery

The controlled documentation-aligned mapping uses `ident`, `timestamp`, `position.latitude`, and
`position.longitude`. Valid `position.accuracy`, `position.speed`, and `position.direction` values are
mapped when present. Missing optional values remain absent. Ignition, odometer, engine hours, provider
message identity, and provider sequence are not synthesized.

The adapter preserves provider `timestamp` as `sourceTimestamp`, validates the configured identity and
WGS84 bounds before bridging, and emits only allowlisted `source=flespi-rest` metadata. Unrestricted raw
provider JSON is transient and is not persisted.

Delivery is at least once. The in-memory watermark advances only after the unchanged local ingress accepts
the mapped batch. Overlap and retry duplicates remain subject to existing Tracking dedupe. No second
business dedupe store or cursor table exists.

## Private HMAC bridge

The bridge invokes the literal existing route
`POST /api/integration/v1/tracking/positions` on a loopback-only HTTP(S) address. It creates a fresh
32-byte cryptographically random nonce and signs the exact frozen canonical value:

`epoch + "\n" + nonce + "\n" + providerKeyId + "\n" + providerAlias + "\n" + rawBody`

with HMAC-SHA256 and the existing resolved secret. The existing controller remains the sole authority for
binding lookup, lifecycle, Tenant derivation, alias validation, secret resolution, HMAC and nonce checks,
admission limits, device lookup, association, dedupe, and ingestion.

## Resilience, backpressure, metrics, and health

Transient network, timeout, HTTP 429, and 5xx failures use bounded 5/15/30/60-second retry delays plus
jitter. Authentication, malformed-response, oversize-response, and unsupported-mapping failures stop
unbounded retry. A single-flight guard prevents overlapping polls. The adapter retains no more than one
provider page and its current outbound batch; downstream failure leaves the watermark unchanged.

Bounded Micrometer counters/timers cover received, mapped, rejected, provider/mapping errors, retries,
poll latency, and ingress latency. Labels contain only bounded provider/result/category values. The
`trackingFlespi` health contributor reports configured/reachable state, last successful poll/message, and a
sanitized failure category. Provider failure degrades the adapter without making Tracking or its last-known
queries unavailable. The adapter is disabled by default and incomplete configuration cannot start polling.

## Verification evidence

- Controlled fixture: `CONTROLLED_FIXTURE_NOT_REAL_PROVIDER_EVIDENCE`.
- Focused adapter tests: 14 / 14 PASS.
- Focused Tracking/PostgreSQL selection: 46 / 46 PASS, including binding/security, status,
  remediation, persistence, and deterministic concurrency coverage.
- Existing deterministic Tracking concurrency matrix: 9 / 9 PASS.
- Full Maven: 1,445 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 07:26.
- Architecture: 47 / 47 PASS, including adapter-only flespi dependency containment.
- Checkstyle: 0 violations. PMD: BUILD SUCCESS. SpotBugs: 0 findings.
- Controlled-provider real PostgreSQL-backed Chromium: 11 / 11 PASS. Sustained ingestion measured
  418.4 msg/s and the 1,000-message burst measured 1,218.1 msg/s.
- Authoritative persistent evidence used only `transport_logistics_acceptance`; the development database
  was not authoritative evidence.
- `git diff --check`: PASS.

Frontend source/config/public contracts were not changed, so the task-authorized conditional frontend
TypeScript/Vitest/build/lint gate was not applicable.

## Real-capture gate and exclusions

The fixture is not physical-provider evidence. A real FMC130-generated flespi capture must still confirm
the actual timestamp and coordinate representation, accuracy availability, speed units, direction,
ignition field, immutable message identity/sequence, and provider replay/history/error behavior. Mapping-only
corrections may remain inside this adapter; any need for V75 or a domain, application-port, public-API,
permission, event, trust, Tenant-authority, or persistence change is a mandatory stop.

US-48 remains externally blocked and US-49 must not start.

**Next task:** `US-48-FMC130-FLESPI-EXTERNAL-CAPTURE-001`
