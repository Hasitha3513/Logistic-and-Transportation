# US-55 Traccar Production Polling Adapter

## Status

`IMPLEMENTATION_COMPLETE / VERIFIED`

This bounded Tracking change set implements the approved Traccar polling adapter. It does not close US-55
physical acceptance, add a migration, create a permission, or claim that a real Traccar server/device has been
accepted.

## Qualified provider contract

The official Traccar OpenAPI inspected on 2026-09-18 identifies version 6.15.3. This adapter qualifies that
contract: `ApiKey` HTTP bearer authentication and historical `GET /api/positions` retrieval with `deviceId`,
`from` and `to`. A no-parameter positions request is latest-position retrieval and is not used as journey
history. Other Traccar versions require compatibility verification.

## Implementation

- Registers one production `TRACCAR` `TrackingProviderAdapter` with truthful polling/history/signal capabilities.
- Uses an opaque runtime bearer token resolved by the existing coordinator; Basic authentication, URL secrets
  and credential persistence are absent.
- Retrieves bounded time ranges per configured numeric Traccar device ID, sorts by `fixTime` then immutable
  position ID, preserves the existing overlap watermark and advances only through the common all-ack canonical
  Kafka ingestion boundary.
- Rejects malformed, byte-oversized or record-overflow responses as a whole. It never silently truncates and
  therefore cannot advance a successful watermark for an incomplete response.
- Maps knots to km/h by exactly 1.852 and preserves approved ignition, odometer, tamper, battery, voltage,
  external-power and charging observations without inventing absent values.
- Uses public HTTPS/443 by default. Private HTTPS destinations require the deployment-owned exact `host:port`
  allowlist `TRACKING_TRACCAR_PRIVATE_ENDPOINT_ALLOWLIST`; Tenant safe configuration cannot expand it.
- Resolves and validates every destination immediately before each request, always rejects loopback, link-local,
  multicast and metadata addresses, rejects unapproved private/ULA addresses, and never follows redirects.
- Uses the JVM verified TLS trust chain. A private deployment may install an explicitly governed trusted CA in
  the runtime trust store; TLS verification is never disabled.

## Verification

- Traccar/provider SPI: 19/19 PASS.
- Architecture and Spring Modulith selection: 78/78 PASS.
- Complete backend: 1,904 tests, zero failures/errors/skips — BUILD SUCCESS in 12:57.
- PMD, Checkstyle and SpotBugs: PASS; SpotBugs reports zero findings.
- Docker/Frontend continuity was already proven by the immediately preceding canonical polling remediation;
  this change set modifies no frontend source or public API.
- `git diff --check`: PASS.

The complete backend run used `transport_logistics_acceptance` for generic contexts and guarded PostgreSQL
Testcontainers for destructive database tests. No development database or external Traccar system was contacted.

## Limitations and rollback

Real-provider connectivity, a real Traccar device, operator workflow and physical telemetry remain unproven and
cannot be inferred from fixtures. Guided Traccar onboarding UI evidence remains a separate queue. Rollback disables
or removes the registered adapter while retaining provider configuration, Kafka facts and Tracking history.
Flyway remains V100; story accounting remains 73/87.

Exact next independent queue: `US-55-PROVIDER-UI`.
