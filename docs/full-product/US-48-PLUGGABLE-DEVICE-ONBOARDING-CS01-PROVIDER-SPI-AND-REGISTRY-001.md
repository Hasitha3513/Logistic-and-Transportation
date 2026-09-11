# US-48 Pluggable Device Onboarding CS01 — Provider SPI and Registry

**Task:** `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS01-PROVIDER-SPI-AND-REGISTRY-001`  
**Result:** COMPLETE  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining  
**Flyway:** V74 (no migration)

## Implemented boundary

The provider-neutral application model lives in
`com.transportlogistics.app.tracking.application.provider`. It defines strict `ProviderType` values using
`[A-Z][A-Z0-9_]{0,63}`, the exact approved immutable capability model, safe bounded non-secret
configuration, connection identity/execution data, bounded fetch/discovery requests, watermarks,
normalized position candidates, fetch/validation/connection-test/discovery/health results, and safe
provider descriptors.

`TrackingProviderAdapter` is an outbound Tracking port. Its contract exposes provider type and
capabilities, validates safe configuration, tests a connection with a transient `char[]` secret, fetches
normalized candidates, reports health, and optionally discovers devices or closes connection resources.
Unsupported discovery returns an explicit `UNSUPPORTED` result. Provider DTOs, raw payloads, SDK types,
HTTP types, Tenant claims, and credential values do not cross this boundary.

`TrackingProviderAdapterRegistry` is an immutable application registry populated from Spring-managed
adapter implementations by `TrackingConfiguration`. It rejects null registrations and duplicate
`ProviderType` values deterministically at startup, provides optional lookup, fails unsupported required
lookups with `TRACKING_PROVIDER_TYPE_UNSUPPORTED`, and exposes sorted immutable descriptors. No provider
switch or provider-specific dependency was added.

## Extensibility and containment proof

`TrackingProviderSpiTest` supplies two dummy adapters with different provider types and capabilities. Both
register and resolve without any Tracking domain change. The same suite proves strict provider-type
validation, immutable/defensively copied capability and configuration values, bounds, explicit unsupported
discovery, duplicate rejection, safe unsupported-provider failure, normalized UNKNOWN/absent telemetry,
and absence of secret material from provider-neutral data and diagnostics.

`HexagonalLayerArchitectureTest` now prevents the provider application model and Tracking outbound ports
from depending on Spring, JPA, Hibernate, Jackson, or Tracking adapter packages. Existing rules continue
to confine Flespi implementation types to its adapter package.

## Verification

- Focused CS01 SPI and architecture: 36 tests, 0 failures, 0 errors, 0 skipped.
- PostgreSQL-backed Tracking regression: 45 tests, 0 failures, 0 errors, 0 skipped, using only
  `transport_logistics_acceptance` and Flyway V74.
- Complete Maven verification: 1,460 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 07:38.
- Complete architecture/Modulith suite: 48 tests, 0 failures, 0 errors, 0 skipped.
- Checkstyle: 0 violations.
- PMD: `BUILD SUCCESS`, no current blocking findings.
- SpotBugs on Java 21: 0 bug instances and 0 errors.
- `git diff --check`: required as the final repository gate.

An initial non-authoritative Tracking regression invocation was denied local socket access and produced
only downstream Spring context errors; its isolated acceptance-database rerun passed. An initial SpotBugs
invocation used Java 25 and failed because the bundled ASM version cannot parse class-file version 69; the
required Java 21 rerun passed with zero findings. During full verification, an asynchronous scheduled job
briefly queried a table while a test fixture was restoring the schema. Maven still completed all tests
successfully; this did not constitute a test failure or a CS01 defect.

## Explicit exclusions

CS01 adds no database or migration, provider-connection persistence, device-provider binding,
coordinator/scheduler, management API, permission, event, frontend, internal ingestion port, Tracking
domain-position change, Flespi runtime change, or US-49 behavior. It does not complete the wider
plug-and-play program or waive the physical-device/provider acceptance requirement.

## Next controlled change set

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS02-V75-PROVIDER-CONNECTION-PERSISTENCE-001`
