# US-55 Provider Polling Canonical Kafka Remediation

## Status

`NEWLY_PLANNED_PREREQUISITE / READY_FOR_AUTHORIZATION`

This is a bounded technical prerequisite under the existing US-55/Tracking scope. It creates no story,
migration, permission, public API or new provider capability claim.

## Confirmed discrepancy

The accepted hybrid ADR requires every provider path to converge after server-side Tenant, Device and
source-time Vehicle authority at the canonical V1/V2 boundary, then publish durably to Kafka before Redis
and TimescaleDB projection. The current Generic signed ingress follows that path.

The internal provider polling coordinator instead invokes `JdbcTrackingProviderIngestionAdapter`, which
converts `NormalizedPositionCandidate` into the legacy `TrackingUseCase.PositionCommand` and writes
`tracking_position`/`tracking_vehicle_latest` directly. It therefore bypasses Kafka, TimescaleDB history,
Redis live projection and the V2 tamper/battery/power fields. Flespi currently uses this legacy path; adding
Traccar to it would violate the promoted architecture and make its advertised signals disappear.

## Bounded remediation proposal

1. Extend the internal provider-neutral polling candidate with the already-approved optional V2 signal
   fields, preserving compatibility constructors for current Flespi tests.
2. Replace the polling ingestion adapter's legacy write with server-authoritative source-time binding,
   deterministic cross-version identity and `TrackingTelemetryIngestedV2` durable Kafka publication.
3. Advance a binding watermark only after Kafka acknowledgement. Broker timeout/failure must fail the
   coordinator execution and retain the previous watermark for retry.
4. Preserve Tenant-qualified connection/device locking, one authoritative provider interval, existing
   coordinator leases/backoff and transient secret clearing.
5. Prove Flespi regression, V1/V2 idempotency, Kafka acknowledgement/failure behavior, Redis/Timescale
   projection, reassignment-at-source-time and Tenant isolation.

No adapter may write Redis, TimescaleDB, legacy position tables or another module's persistence directly.

## Traccar API decision bundle

Official Traccar documentation confirms bearer-token authentication and
`GET /api/positions?deviceId={id}&from={ISO-8601}&to={ISO-8601}`. It does not define a page-size, limit or
cursor parameter. Before `US-55-TRACCAR-ADAPTER` implementation, approve these two consolidated choices:

1. **Bounded retrieval:** use bounded per-device UTC windows, a one-MiB response cap, local ordering by
   `fixTime` then immutable position `id`, a maximum of 500 accepted candidates, and fail closed with
   sanitized degraded health if a window cannot be represented within those bounds. Do not silently drop
   overflow or advance its watermark.
2. **Endpoint trust:** choose either public-HTTPS-only resolution (reject IP literals plus loopback, private,
   link-local and metadata addresses before each request; recommended for Phase 1) or explicitly authorize
   private/self-hosted Traccar endpoints with a separately governed network allowlist. HTTPS certificate
   verification, port 443, no redirects, no URL credentials/query/fragment and opaque bearer-token resolution
   remain mandatory in both cases.

Username/password, Basic authentication, plaintext secrets, disabled TLS verification and unsigned callbacks
remain prohibited.

## Delivery order

1. Authorize and implement this canonical Kafka remediation.
2. Implement `US-55-TRACCAR-ADAPTER` using the approved bounded-retrieval and endpoint-trust choices.
3. Run provider SPI, Kafka/Redis/Timescale, PostgreSQL, architecture, security, frontend truthfulness and full
   regression gates.
4. Preserve the separate open physical acceptance queue.

Rollback disables provider polling and retains Kafka/history/configuration data. Flyway remains V100.
