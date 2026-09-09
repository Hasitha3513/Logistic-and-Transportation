# US-48 Pluggable Device Onboarding Architecture

**Task:** `US-48-PLUGGABLE-DEVICE-ONBOARDING-ARCHITECTURE-001`  
**Decision:** APPROVED  
**Product capability:** `PLUG_AND_PLAY_FOR_SUPPORTED_ADAPTERS`  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining  
**Current Flyway head:** V74  
**Forward migration:** V75 AUTHORIZED for the implementation change sets; not implemented by this decision

## Decision and limits

Tracking will support runtime onboarding of many devices across many provider connections and Tenants
without rebuilding or restarting the application, provided the provider/protocol has an installed adapter.
Adding a new protocol adapter remains a reviewed source-code release and deployment. Runtime JAR upload,
script injection, arbitrary class loading, and claims of universal hardware compatibility are prohibited.

This decision remains inside US-48. It does not start US-49, advance story accounting, waive the physical
FMC130/real-provider acceptance gate, or change normalized position, history, trust, freshness,
connectivity, retention, Vehicle association, or public position-query semantics.

## Source findings

V73 currently stores provider alias and external device identity directly on `tracking_device`; devices
have only ACTIVE/DISABLED lifecycle. V74 adds a Tracking-owned `tracking_provider_binding` containing the
trusted Tenant, opaque provider key, alias, credential reference, lifecycle, audit facts, and version.
The existing Flespi adapter is a singleton scheduler driven by one property set and one device ID. Those
facts are safe for a one-device pilot but cannot provide hot multi-device/multi-connection onboarding.

The existing effective-dated `tracking_vehicle_device_assignment`, normalized `tracking_position`,
`tracking_vehicle_latest`, signed external ingress, provider-binding Tenant authority, and
`IntegrationSecretResolver` boundary are retained.

## Provider adapter SPI

The provider-neutral outbound SPI will live under Tracking ports, with provider-neutral application types:

```java
interface TrackingProviderAdapter {
    ProviderType providerType();
    ProviderCapabilities capabilities();
    ConfigurationValidation validateConfiguration(ProviderConnectionConfiguration configuration);
    ConnectionTestResult testConnection(ProviderConnectionExecution connection, char[] secret);
    FetchResult fetchPositions(ProviderFetchRequest request, char[] secret);
    default DiscoveryResult discoverDevices(ProviderDiscoveryRequest request, char[] secret);
    ProviderHealth health(ProviderConnectionExecution connection);
    default void close(ProviderConnectionId connectionId);
}
```

`ProviderFetchRequest` contains connection identity, a bounded list of provider device references,
provider-neutral source-time/message watermarks, page and response limits, and a deadline. `FetchResult`
contains only provider-neutral `NormalizedPositionCandidate` values and the next safe watermarks.
Candidates carry the external device reference, source timestamp, WGS84 position and optional normalized
accuracy/speed/heading/engine/meter/message identity/sequence facts. Provider DTOs, SDK types, raw JSON,
tokens, Tenant claims, and HTTP types never cross the adapter package boundary.

Adapters own parsing and normalization. The SPI does not expose a separate public `normalize` operation
because doing so would leak provider representations. `discoverDevices` is unsupported unless the adapter
declares `DISCOVERY`. `close` releases connection-scoped transport resources; it does not delete data.

## Provider types, capabilities, and registry

`ProviderType` is a bounded uppercase identifier (`[A-Z][A-Z0-9_]{0,63}`), not a database enum or giant
switch. Initially only `FLESPI` is supported. `TELTONIKA_HTTP`, `TRACCAR`, `WIALON`, `GEOTAB`, and
`SAMSARA` may be displayed as `NOT_IMPLEMENTED` only when deliberately registered as catalogue metadata;
they must not appear operational.

`TrackingProviderAdapterRegistry` receives all Spring-managed SPI implementations, rejects duplicate
provider types at startup, and performs lookup by `ProviderType`. Adding a supported provider requires an
adapter implementation, its private client/DTO/mapper, registration, tests, and deployment; it requires no
Tracking domain-position modification.

Capabilities are immutable adapter metadata: `POLLING`, `WEBHOOK`, `MQTT`, `DISCOVERY`,
`SOURCE_TIMESTAMP`, `ACCURACY`, `SPEED`, `HEADING`, `IGNITION`, `ODOMETER`, `ENGINE_HOURS`,
`MESSAGE_ID`, `SEQUENCE`, `HISTORY`, and `REPLAY`. Missing device observations remain UNKNOWN/absent.
Capability metadata guides validation and UI; it never fabricates telemetry.

## Runtime model

### Provider connection

The existing V74 `tracking_provider_binding` becomes the persistence authority for the API concept
`TrackingProviderConnection`; a competing connection table is not authorized. V75 extends it with:

- `provider_type VARCHAR(64) NOT NULL`
- `display_name VARCHAR(120) NOT NULL`
- `endpoint_uri VARCHAR(500) NULL`
- `safe_configuration JSONB NOT NULL DEFAULT '{}'`
- `poll_interval_seconds INTEGER NOT NULL DEFAULT 5`
- `page_size INTEGER NOT NULL DEFAULT 500`
- `test_status VARCHAR(24) NOT NULL DEFAULT 'NOT_TESTED'`
- `last_tested_at`, `last_successful_poll_at`, `last_provider_message_at` as nullable `TIMESTAMPTZ`
- `last_error_category VARCHAR(40) NULL`
- `next_poll_at TIMESTAMPTZ NULL`
- `lease_owner VARCHAR(120) NULL` and `lease_until TIMESTAMPTZ NULL`

Existing `tenant_id`, globally unique opaque `provider_key_id`, `provider_alias`,
`credential_reference`, lifecycle, actors/timestamps, and optimistic `version` remain. Lifecycle becomes
`DRAFT -> ACTIVE <-> DISABLED -> RETIRED`; RETIRED is terminal. Test status is separate:
`NOT_TESTED`, `PASS`, `AUTH_FAILED`, `UNREACHABLE`, or `INVALID_CONFIGURATION`.

Constraints require unique `(tenant_id, display_name)` and `(tenant_id, provider_alias)`, poll interval
between 5 and 86,400 seconds, page size between 1 and 500, safe configuration no larger than 8 KiB,
and lease owner/until both null or both present. Endpoint schemes and adapter-owned configuration keys are
validated before persistence. `safe_configuration` is bounded and schema-validated by the selected
adapter; secret-like keys are rejected. No secret value is stored.

### Tracking device

`tracking_device` remains the provider-neutral business device registry. V75 expands lifecycle to
`DRAFT -> ACTIVE <-> DISABLED -> RETIRED`; RETIRED is terminal. Device creation defaults to DRAFT so
connection binding, validation and Vehicle association can be completed before activation.

The legacy `external_device_reference` and `provider_alias` columns remain during compatibility migration
but become read-only compatibility projections from the active device-provider binding. They are never
polling or Tenant authority after cutover. They may be removed only in a separately authorized later
migration after all clients and data are proven migrated.

### Device-provider binding

V75 creates Tracking-owned `tracking_device_provider_binding`:

| Column | Type | Null | Rule |
| :--- | :--- | :---: | :--- |
| `id` | UUID | NO | Primary key; unique with Tenant |
| `tenant_id` | UUID | NO | Tenant authority; tenant-leading indexes |
| `tracking_device_id` | UUID | NO | Same-Tenant FK to `tracking_device` |
| `provider_binding_id` | UUID | NO | Same-Tenant FK to `tracking_provider_binding` |
| `external_device_reference` | VARCHAR(160) | NO | Provider-side device identity |
| `safe_configuration` | JSONB | NO | Default `{}`, adapter schema, maximum 4 KiB, no secrets |
| `lifecycle` | VARCHAR(16) | NO | `DRAFT`, `ACTIVE`, `DISABLED`, `RETIRED` |
| `watermark_source_timestamp` | TIMESTAMPTZ | YES | Last accepted source-time cursor |
| `watermark_message_identity` | VARCHAR(160) | YES | Stable provider cursor only when confirmed |
| `next_poll_at` | TIMESTAMPTZ | YES | Fair scheduling cursor |
| `created_at`, `updated_at` | TIMESTAMPTZ | NO | Audit timestamps |
| `created_by`, `updated_by` | UUID | NO | Logical actor references |
| `version` | BIGINT | NO | Default 0, optimistic concurrency |

Constraints include unique `(tenant_id, provider_binding_id, external_device_reference)`, one ACTIVE
binding per `(tenant_id, tracking_device_id)`, and composite same-Tenant foreign keys. Rebinding disables
the prior active binding and creates a new versioned binding in one Tracking transaction; it never rewrites
position or Vehicle-association history. Provider and device must both be ACTIVE before polling.

## Tenant and credential authority

Every connection, device, binding, coordinator claim, management query, audit fact, and execution context
is Tenant-scoped. Human Tenant comes from `CurrentTenant`. Background work discovers the persisted
connection together with its immutable Tenant and carries that Tenant explicitly; no provider payload,
account metadata, external device reference, query parameter, or header selects Tenant.

Credential values continue to resolve only as
`credentialReference -> IntegrationSecretResolver -> transient char[]`. Connection APIs accept an opaque
reference matching the resolver contract and return only `credentialConfigured`, a safe reference type,
and optional masked label. A null credential on update preserves the current reference. Test Connection
resolves the secret transiently, validates authentication/reachability/provider identity, clears buffers,
and creates no telemetry state. Credential-reference replacement is audited and takes effect on the next
claim without restart.

Device-specific environment variables are prohibited. Environment variables may back credential
references for a provider account, but device identity/configuration is stored in the runtime connection
and device-binding records.

## Ingestion security decision

Internal polling adapters will use a new non-web `TrackingProviderIngestionPort`; they will not continue
HMAC-signing loopback HTTP after the controlled migration. External push/webhook providers continue to use
the unchanged signed `POST /api/integration/v1/tracking/positions` ingress.

The coordinator—not a provider adapter—invokes the internal port with an opaque connection ID and fetched
candidates. The application service reloads the ACTIVE connection and ACTIVE device binding, derives
Tenant/provider alias from persistence, resolves external reference to internal device UUID, applies batch
limits, and delegates to the identical existing normalized ingestion service. The adapter cannot construct
Tenant authority or bypass device/association/dedupe/trust rules.

Security equivalence is therefore explicit: signed external callers prove possession through V74
key/timestamp/nonce/HMAC; internal adapters prove authority through non-web Spring wiring plus a current
database lease, and the internal port independently reloads lifecycle/Tenant/bindings before ingestion.
Neither path accepts Tenant from a payload. Removing self-HTTP avoids treating a provider API token as an
internal HMAC secret and removes nonce/network failure modes without weakening external ingress.

The internal port is package-published only to Tracking application/coordinator code, has no controller,
is rejected from other modules/adapters by architecture tests, and emits no packet audit or event.

## Dynamic execution coordinator

One `ProviderPollingCoordinator` owns scheduling. A short fixed-delay tick claims due ACTIVE provider
connections using a bounded query ordered by `next_poll_at`, with `FOR UPDATE SKIP LOCKED` and a renewable
database lease. Claims are unique by connection across application instances. A global bounded worker pool
and per-provider concurrency/rate limiter execute connection jobs; there is no thread or scheduler bean per
device.

For each connection, the coordinator pages ACTIVE device bindings by `(tenant_id, provider_binding_id,
next_poll_at, id)`, partitions them according to adapter capabilities/provider quotas, and supplies bounded
requests. Bulk-capable adapters fetch batches. A single-device endpoint adapter may perform bounded
sequential or limited-parallel requests inside the connection job, but cannot create persistent per-device
threads. Fair `next_poll_at` cursors prevent one large connection starving others.

Watermarks advance transactionally only for candidates accepted or identified as exact duplicates by the
existing ingestion service. Provider/downstream failure leaves cursors unchanged and schedules bounded
retry. Disable/retire is observed before each claim and batch, so hot disable stops new work; in-flight work
may finish but the ingestion port revalidates lifecycle and fails closed. Activation sets `next_poll_at`
and becomes eligible on the next tick without restart.

The initial global limits are configuration of infrastructure capacity, not per-device variables: bounded
connection claims, maximum workers, maximum devices per job, deadlines, and provider quota policies.
Metrics use provider type/result and bounded status labels; UUIDs and external device references are not
metric labels. Actuator exposes aggregate adapter health, while safe management APIs expose connection-level
last test/poll/error state. Device connectivity remains derived solely from accepted Tracking receipts.

This model stores 10,000 device bindings without 10,000 beans or threads and preserves the existing
approximately 167 positions/second nominal and 5x burst design. Implementation acceptance must still prove
100-device single-connection hot operation, multi-connection/multi-Tenant isolation, coordinator lease
races, bounded memory/threads, quota behavior, and the existing 1,000-message/second burst gate. Provider
quotas remain an external capacity constraint and cannot be inferred from architecture alone.

## Authorized management APIs

All paths are under the existing `/api/v1/tracking` public base and require authenticated active membership.
Provider configuration operations reuse `TRACKING_DEVICE_MANAGE`; no new permission is authorized.

- `GET /provider-types`
- `GET|POST /provider-connections`
- `GET|PUT /provider-connections/{connectionId}`
- `POST /provider-connections/{connectionId}/test`
- `POST /provider-connections/{connectionId}/activate`
- `POST /provider-connections/{connectionId}/disable`
- `POST /provider-connections/{connectionId}/retire`
- `GET /provider-connections/{connectionId}/devices/discover` only when `DISCOVERY` is supported
- `POST /devices/{deviceId}/provider-bindings` for reasoned bind/rebind

Existing device list/get/create/update/activate/disable and Vehicle association paths remain. V75 adds
`POST /devices/{deviceId}/retire`. New device creation accepts `providerConnectionId`, external device
reference, optional safe hardware reference and optional bounded safe device configuration. During one
compatibility release, the current `providerAlias`-only create request remains accepted only when exactly
one eligible same-Tenant connection has that alias; zero or ambiguity fails closed. Responses add provider
connection summary/capabilities without exposing secrets.

Lists default to 20 and cap at 100. Mutations use optimistic `version`; create and rebinding use a
Tenant/operation-scoped `Idempotency-Key`. Cross-Tenant resources return the existing not-found-shaped
denial. Test/discovery never creates positions. Discovery is bounded and paginated by provider capability.
Bulk onboarding is deferred to a later US-48 change set after single-device APIs are accepted; when added,
it will reuse the same commands rather than create a parallel model.

Authorized error codes are `TRACKING_PROVIDER_TYPE_UNSUPPORTED`,
`TRACKING_PROVIDER_CONNECTION_NOT_FOUND`, `TRACKING_PROVIDER_CONNECTION_INVALID`,
`TRACKING_PROVIDER_CONNECTION_DISABLED`, `TRACKING_PROVIDER_AUTH_FAILED`,
`TRACKING_PROVIDER_UNREACHABLE`, `TRACKING_DEVICE_PROVIDER_DUPLICATE`, and
`TRACKING_DEVICE_EXTERNAL_ID_CONFLICT`, plus existing validation/not-found/concurrency errors where exact.

## RBAC, audit, and privacy

`TRACKING_DEVICE_MANAGE` governs provider types, connection CRUD/test/lifecycle/discovery, device binding,
device lifecycle, and Vehicle association. `TRACKING_VIEW` continues to govern safe device/connection
summaries and live state; `TRACKING_HISTORY_VIEW` remains history-only. Backend checks remain authoritative.

Audit records cover connection create/update, credential-reference replacement, test result category,
activate/disable/retire, device create/bind/rebind/activate/disable/retire, association changes, and denied
privileged attempts. Safe details contain IDs/status/reason codes only—never credentials, raw configuration,
external identities, coordinates, nonce/signature, or payload. No telemetry packet creates audit/outbox data.

## Frontend decision

The existing Tracking feature remains feature-first and gains three views: Live Vehicles, Devices, and
Provider Connections. AppLayout remains the sole global chrome owner. TanStack Query owns server state;
React Hook Form + Zod + Ant Design own forms and validation.

Provider Connections supports type selection, safe configuration fields generated from bounded adapter
metadata, opaque credential-reference entry/replacement, Test Connection, activate/disable/retire, device
count, and safe health. It never echoes a secret. Devices supports provider-connection selection, optional
bounded discovery or manual external reference, validation, DRAFT save, Vehicle association, activation,
disable/retire/rebind, capabilities, masked identity, last receipt, and connectivity. Unsupported types are
not selectable as operational connections.

## Flespi migration

The existing `tracking.adapters.inbound.flespi` client/DTO/mapper is refactored to implement the SPI and
retains TLS-only REST polling, five-second minimum, 500-message/1 MiB bounds, five-minute overlap,
at-least-once delivery, optional-field honesty, sanitized errors, metrics, health, and bounded retry.
Singleton `FlespiAdapterProperties`, `FlespiPollingAdapter`, and `TrackingIngressBridge` cease to be an
active competing execution path after coordinator cutover.

V75 backfills one device-provider binding for every current device only when exactly one same-Tenant V74
provider binding matches its alias. Duplicate aliases, missing matches, or cross-Tenant inconsistencies
abort migration with a safe diagnostic; secrets are never copied. Existing controlled fixture rows remain
valid. Cutover is feature-flagged: deploy schema and read compatibility, backfill/verify, enable coordinator,
disable singleton polling, then remove obsolete property execution in the same accepted release.

## Verification and architecture gates

Implementation must prove:

- SPI/registry duplicate-type and dummy-second-adapter extensibility without domain changes.
- V1→V75 migration, deterministic backfill, constraints, indexes, Tenant-leading query plans and rollback.
- Same-Tenant CRUD and complete cross-Tenant denial for connections, discovery, devices and bindings.
- Secret/reference/API/log/metric/audit privacy and credential rotation without restart.
- 100 dynamically active Flespi devices under one connection with no per-device beans/threads.
- Tenant A with two connections and Tenant B with one, fully isolated.
- Hot create/activate/disable/retire/rebind and provider disable while polling races.
- Connection lease claim/expiry/recovery and exactly one active execution per connection.
- Cursor advances only after accepted/idempotent ingestion; retry/overlap remains at least once.
- Test Connection and discovery create no telemetry state.
- Existing HMAC ingress security matrix and internal-port authority equivalence.
- Existing device/association, position/history, nine-race, architecture, full Maven, static/frontend,
  Chromium, sustained and burst regression gates.
- Physical FMC130 and real Flespi evidence remains mandatory for final acceptance.

## Rollback

Disable the coordinator feature flag and all provider connections. Preserve V75 schema, device/binding
configuration, existing history/latest projections, signed external ingress, and last-known reads. Do not
reverse migrations, delete telemetry, restore per-device environment configuration, weaken authentication,
or reactivate the old singleton and coordinator simultaneously.

## Controlled implementation sequence

1. `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS01-PROVIDER-SPI-AND-REGISTRY-001`
2. `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS02-V75-PROVIDER-CONNECTION-PERSISTENCE-001`
3. `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS03-DEVICE-PROVIDER-BINDING-001`
4. `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS04-PROVIDER-EXECUTION-COORDINATOR-001`
5. `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS05-FLESPI-SPI-MIGRATION-001`
6. `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS06-MANAGEMENT-APIS-001`
7. `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS07-RBAC-AUDIT-SECURITY-001`
8. `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS08-FRONTEND-PROVIDER-CONNECTIONS-001`
9. `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS09-FRONTEND-DEVICE-ONBOARDING-001`
10. `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS10-POSTGRES-CONCURRENCY-SCALE-ACCEPTANCE-001`

Each change set is independently reviewable. No implementation begins in this architecture task.

**Next task:** `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS01-PROVIDER-SPI-AND-REGISTRY-001`
