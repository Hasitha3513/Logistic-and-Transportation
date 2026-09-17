# US-55 Provider Polling Canonical Kafka Remediation

## Status

`COMPLETE`

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

## Implemented remediation

1. The provider-neutral polling candidate carries the approved nullable V2 tamper, battery, voltage,
   external-power and charging observations while retaining its V1-compatible constructor.
2. The polling ingestion adapter now reloads the leased active connection, locks the same-Tenant binding,
   resolves server-authoritative Device and source-time Vehicle authority, creates deterministic cross-version
   identity and publishes `TrackingTelemetryIngestedV2` through `TelemetryStreamPublisherPort`.
3. A poll is accepted only after every required Kafka acknowledgement. Timeout, partial acknowledgement or
   publication failure fails the coordinator execution, retains the previous successful watermark and enters
   the existing bounded failure backoff. A retry republishes the same deterministic event and deduplication
   identities for canonical consumer idempotency.
4. Preserve Tenant-qualified connection/device locking, one authoritative provider interval, existing
   coordinator leases/backoff and transient secret clearing.
5. Prove Flespi regression, V1/V2 idempotency, Kafka acknowledgement/failure behavior, Redis/Timescale
   projection, reassignment-at-source-time and Tenant isolation.

No migrated polling adapter writes Redis, TimescaleDB, legacy position tables or another module's persistence
directly. Kafka acknowledgement is publication evidence only; downstream Timescale history, eligible Redis
live state and detector evaluation remain independently retryable consumers owned by Tracking.

## Traccar API decision bundle

The official OpenAPI contract inspected on 2026-09-18 identifies Traccar `6.15.3`; that exact contract is the
qualified Phase 1 adapter target, and other server versions require compatibility verification. It confirms
the `ApiKey` HTTP bearer scheme and
`GET /api/positions?deviceId={id}&from={ISO-8601}&to={ISO-8601}`. It does not define a page-size, limit or
cursor parameter. The approved choices for `US-55-TRACCAR-ADAPTER` are:

1. **Bounded retrieval:** use bounded per-device UTC windows, a one-MiB response cap, local ordering by
   `fixTime` then immutable position `id`, a maximum of 500 accepted candidates, and fail closed with
   sanitized degraded health if a window cannot be represented within those bounds. Do not silently drop
   overflow or advance its watermark.
2. **Endpoint trust:** public HTTPS is allowed by default. Private/self-hosted HTTPS requires a deployment-
   administrator-managed destination-and-port allowlist that Tenant users cannot expand. Resolve and validate
   every destination at connection time, reject loopback, link-local, metadata and all other unapproved
   addresses, and do not follow redirects across the policy boundary. Certificate verification is mandatory;
   private deployments may use an explicitly configured trusted CA. Authentication uses only the approved
   opaque credential reference and supported bearer token—never URL credentials or Basic fallback.

The technical limits are configurable and conservatively default to a 45-second execution deadline, two
concurrent provider executions, at most 100 devices per coordinator run, one MiB per response and 500 accepted
observations per device request. A response is validated completely before publication. Overflow is rejected
explicitly, emits minimized actionable health, does not advance the watermark and uses bounded backoff without
an immediate retry loop. Latest-position retrieval is not historical journey evidence; bounded time-ranged
`/api/positions` retrieval is the supported recovery path. Username/password, Basic authentication, plaintext
secrets, disabled TLS verification and unsigned callbacks remain prohibited.

## Delivery order

1. Canonical Kafka remediation — COMPLETE.
2. Implement `US-55-TRACCAR-ADAPTER` using the approved bounded-retrieval and endpoint-trust choices.
3. Run provider SPI, Kafka/Redis/Timescale, PostgreSQL, architecture, security, frontend truthfulness and full
   regression gates.
4. Preserve the separate open physical acceptance queue.

Rollback disables provider polling and retains Kafka/history/configuration data. Flyway remains V100.

## Verification evidence

- Focused canonical polling, Flespi mapping and provider SPI tests prove V1 compatibility, nullable V2 signal
  preservation, binding rejection, all-ack publication and deterministic retry identity.
- Isolated PostgreSQL acceptance proves the coordinator publishes canonical Kafka facts and performs zero
  legacy `tracking_position` writes; the verified database is `transport_logistics_acceptance`.
- Retained Kafka publisher, Timescale consumer, Redis projector and V2 consumer integration tests prove that
  established owners continue to consume the canonical event.
- Architecture/Modulith verification passes with the published Tracking ports; no module-private persistence
  access was introduced.
- The complete corrected backend suite passes 1,899/1,899 with zero failures, errors or skips. Generic Spring
  contexts used `transport_logistics_acceptance`; destructive PostgreSQL suites used their guarded isolated
  Testcontainer rather than sharing one database. PMD, SpotBugs, Checkstyle, dependency analysis, Docker Compose
  validation and `git diff --check` pass. Dependency analysis retains the repository's existing starter/transitive
  classification warnings.
- Frontend continuity passes 336/336 Vitest tests, TypeScript and the production build. No frontend file changed.
  Full-project ESLint still reports 71 pre-existing Delivery-module findings; changed-file lint is not applicable
  and the unrelated global debt was not absorbed into this Tracking remediation.
- No schema, migration, permission, public API, frontend contract or story accounting changed. Flyway remains
  V100 and US-55 physical acceptance remains independently blocked on external evidence.
