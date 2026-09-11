# US-48 CS05 Flespi SPI Migration — Technical Evidence

## Result

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS05-FLESPI-SPI-MIGRATION-001` is complete at Flyway V76. US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`; accounting remains 72/87 complete with 15 remaining.

## Runtime cutover

The former singleton `FlespiPollingAdapter` runtime, device-specific `FlespiAdapterProperties`, in-memory business watermark and `TrackingIngressBridge` self-HTTP/HMAC loopback were removed. Flespi now has exactly one Spring-managed `TrackingProviderAdapter`, registered under `ProviderType("FLESPI")` and executed only through `TrackingProviderAdapterRegistry -> ProviderPollingCoordinator -> TrackingProviderIngestionPort` when the coordinator feature flag is enabled. The adapter owns no scheduler, device thread, job retry schedule or persistent cursor.

The unchanged signed `POST /api/integration/v1/tracking/positions` endpoint remains authoritative for external push providers. Coordinator-driven Flespi polling never invokes it.

## SPI behavior and capabilities

`FlespiTrackingProviderAdapter` declares only `POLLING`, `SOURCE_TIMESTAMP`, `ACCURACY`, `SPEED`, `HEADING` and `HISTORY`. Discovery, replay, message identity/sequence, ignition, odometer and engine hours are not advertised. Documentation-aligned mappings remain pending confirmation from a physical FMC130/flespi capture.

The refactored adapter-private mapper produces `NormalizedPositionCandidate` directly from `ident`, provider `timestamp`, WGS84 latitude/longitude and present validated accuracy/speed/heading values. It does not synthesize optional facts or expose Flespi DTOs outside the adapter package.

## Multi-device and connection execution

Every fetch receives persisted `ProviderConnectionExecution` and bounded `ProviderDeviceCursor` values. Connection-specific endpoint, alias, safe configuration, page/response bounds and deadline are honored. The client performs bounded sequential per-device requests inside one connection job, with a global result/page cap and no persistent per-device runtime object. One hundred devices were fetched in 100 bounded calls with zero per-device threads.

The adapter is stateless across accounts. Tenant A/A1, Tenant A/A2 and Tenant B/B1 style executions use their own runtime endpoint, token and device references through the same registered implementation. Hot add/disable, provider disable and rebinding remain governed by CS04 persistence reloads, so the next coordinator page reflects current V76 authority without restart.

## Secrets, cursors and failures

The coordinator remains the sole secret resolver: `credentialReference -> IntegrationSecretResolver -> transient char[]`. The adapter accepts only that transient buffer, stores no reference/value and does not read token environment variables. Credential rotation is observed on the next coordinator execution. The JDK HTTP API necessarily materializes the authorization header while constructing the outbound request; it is neither retained nor logged.

V76 per-binding source/message watermarks are the sole business cursor. Each device query derives a bounded overlap of at most five minutes from its supplied watermark; cold start uses the same maximum. `FetchResult` returns provider-neutral next-watermark proposals, while CS04 advances persistence only after ACCEPTED/DUPLICATE ingestion. Duplicate overlap remains at least once and Tracking dedupe stays authoritative.

The adapter performs no nested job retry. HTTP 429/5xx/network/timeout map to safe transient categories for coordinator scheduling; 401/403 map to authentication failure; malformed, rejected and oversize responses fail safely. One malformed message is rejected without discarding valid siblings. Metrics use only `provider=FLESPI` and bounded result/category values. Provider-connection health is isolated from aggregate coordinator/Tracking health and exposes no secret, Tenant, connection, device, external identity, coordinate or raw response.

## Legacy class disposition and rollback

- `FlespiPollingAdapter`: DELETE — scheduled singleton runtime retired.
- `FlespiAdapterProperties`: DELETE — device/property runtime authority retired.
- `TrackingIngressBridge`: DELETE — coordinator path uses the internal ingestion port.
- `FlespiProviderClient`: REFACTOR/RETAIN_AS_CLIENT_SUPPORT — bounded runtime endpoint/device fetches and safe connection probe.
- `FlespiMessageMapper`: REFACTOR/RETAIN_AS_CLIENT_SUPPORT — direct provider-neutral candidate mapping.
- `FlespiAdapterState` and `FlespiAdapterHealthIndicator`: REFACTOR — safe connection-scoped/aggregate adapter health only.

Operational rollback is to disable the provider coordinator and disable affected FLESPI runtime connections while retaining V76 connection/binding/history data. The deleted singleton path must not be reactivated; rollback never enables two polling architectures.

## Verification

- Focused Flespi SPI suite: 15/15 PASS.
- Focused Flespi/registry/coordinator/PostgreSQL selection: 36/36 PASS.
- Complete CS01–CS05 Tracking regression: 89/89 PASS.
- Existing Tracking concurrency: 9/9 PASS.
- Binding concurrency: 4/4 PASS.
- Coordinator concurrency: 8/8 PASS.
- Flespi cutover conditions: 6/6 PASS (mutual exclusion, credential rotation, binding/device disable authority, provider disable authority, rebind authority and duplicate overlap).
- 100-device Flespi execution and retained 10K CS04 design proof: PASS; no per-device threads.
- Full Maven: 1,493 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS in 08:42.
- Architecture/Modulith: 49/49 PASS.
- Checkstyle: 0 violations. PMD: BUILD SUCCESS with zero reported violations. SpotBugs: 0 findings.
- Controlled PostgreSQL-backed Chromium: 11/11 PASS in 38.7 seconds; 382.9 msg/s sustained and 1,020.6 msg/s burst.
- `git diff --check`: PASS.
- All accepted database evidence used only `transport_logistics_acceptance`; the development database was not authoritative evidence.

The first combined focused attempt used Testcontainers and failed before tests because its Docker client API default was too old. A later Tracking selection initially omitted the application datasource environment variables, causing only the Spring security context to target unavailable localhost:5432. Both were infrastructure invocation errors; corrected complete reruns using both repository datasource conventions and `transport_logistics_acceptance` passed.

## Scope exclusions and remaining external gate

No migration/V77, public API, frontend, permission, event, outbox, packet audit, Integration telemetry routing, domain field, Tenant-authority change or US-49 work was introduced. Controlled fixtures are technical evidence only. A physical FMC130 and genuine flespi-generated capture remain mandatory for US-48 final acceptance.

## Next task

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS06-MANAGEMENT-APIS-001`
