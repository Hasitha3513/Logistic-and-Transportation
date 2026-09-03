# US-73 Manage External Integrations — Frozen Product Decisions

**Task:** `US-73-EXTERNAL-INTEGRATIONS-PRODUCT-DECISIONS-001`
**Decision:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`
**Wave:** A
**Program accounting:** 65 / 87 COMPLETE; 22 / 87 remaining
**Architecture baseline:** P1-01 COMPLETE; Flyway head V60
**Decision date:** 2026-09-03

## 1. Authoritative requirement

**Actor:** System Administrator.

**Goal:** configure and monitor external-system integrations so supported business data can be exchanged reliably.

The source names ERP, Accounting, CRM, HRMS, fuel vendors, telematics, payment, insurance, DMS, API, webhook, and file integrations. Its three acceptance criteria are:

1. configured systems can exchange supported data;
2. a failed integration is never silently shown as synchronized;
3. retry and error status is recorded.

The source assigns connectivity and reliable exchange to Integration. External and domain systems retain business meaning. The explicit source failure case is failed exchange with retry/error status. The security/architecture cases required to make that safe are duplicate delivery, invalid mapping, disabled configuration, unavailable endpoint, unavailable/revoked credential, transient I/O, stale concurrent update, partial file write, unsafe path/URL, oversized payload, and cross-Tenant access.

## 2. Frozen scope

US-73 establishes a reusable, Tenant-owned Integration bounded context and proves it with exactly one real adapter:

`GOVERNED_OUTBOUND_JSON_FILE_EXCHANGE`

The adapter writes a canonical UTF-8 JSON document to a server-allowlisted filesystem endpoint through a logical endpoint alias. Final acceptance uses a real isolated filesystem share and real atomic file I/O. It is not a mocked adapter and does not claim a vendor connection.

The evidence tier is `CONTROLLED_SANDBOX`.

US-73 includes:

- configuration, declarative mapping, activation/disablement, safe connection testing, health, exchange status, bounded automatic retry, immutable attempt history, safe reconciliation data, and audit;
- a provider-neutral contract registry and outbound integration-fact consumption boundary;
- the selected outbound JSON-file adapter;
- admin UI and APIs for the selected capability;
- one non-sensitive `US73_PLATFORM_PROBE_V1` acceptance contract that exercises the same production mapping, durability, persistence, retry, and file adapter path without inventing a downstream business integration.

US-73 excludes:

- any ERP, Accounting, CRM, HRMS, fuel-card, telematics, payment, insurance, DMS, webhook, REST, API-gateway, or inbound-file connector;
- US-35/46/47/48/72/76/83 business contracts or rules;
- arbitrary payload submission by operators;
- generic scripting/plugins; provider SDKs; Kafka/RabbitMQ; another outbox/inbox; exactly-once or global ordering;
- manual retry, payload editing, deletion, or changing a historical failure into success;
- global/shared integration configurations.

## 3. Ownership and architecture

The new top-level `com.transportlogistics.app.integration` module is approved as a dedicated bounded context. It is justified by its own configuration, mapping-version, exchange, attempt, health, reconciliation, and credential-reference lifecycles.

Integration owns:

- endpoint capability/configuration metadata;
- logical credential and endpoint references, never secret values;
- declarative transport mappings and immutable mapping versions;
- exchange/idempotency state, attempts, external correlation, health projection, and Integration audit facts;
- external transport adapters and external retry orchestration.

Integration does not own Trip, Fleet, Fuel, Driver, Customer, Delivery, Notification, Document, Payroll, Billing, Compliance, or GPS semantics. It never reads their tables or imports their repositories/entities. Domain modules publish approved minimized facts or expose explicit root-package ports. Integration validates transport shape and mapping; the owning domain validates business meaning.

Required dependency direction is `integration/domain <- ports/application <- adapters`. Domain/application code has no Spring, JPA, Jackson, HTTP, filesystem, or vendor SDK dependency. A file adapter lives under `integration/adapters/outbound/file`; REST/UI are inbound adapters; persistence is an outbound adapter with explicit entity/domain mapping.

### Inbound and outbound boundaries

No external inbound endpoint is implemented for US-73.

Future inbound flow is frozen architecturally as:

`external payload → authenticated Integration adapter → transport/schema validation → immutable mapping version → explicit owning-domain public port → domain validation/result`.

It must never save directly through a domain repository.

The implemented outbound flow is:

`approved minimized fact → shared DurableEventPublisher/outbox → Integration durable handler → idempotent exchange creation → immutable mapping version → file adapter → exchange/attempt result`.

No external I/O occurs in the originating business transaction. The outbox row is acknowledged after Integration has durably accepted the exchange, not after the external file is written. Integration then owns external delivery attempts.

## 4. Integration-category classification

| Source reference | Classification for US-73 | Frozen meaning |
|---|---|---|
| File | `REQUIRED_FOR_US73_ACCEPTANCE` | One outbound UTF-8 JSON file adapter is implemented and accepted. |
| ERP | `FUTURE_CONSUMER_OF_US73_PLATFORM` | Requires an explicitly approved domain contract and real system/sandbox. |
| Accounting | `FUTURE_CONSUMER_OF_US73_PLATFORM` | US-47/Finance owns billing/posting meaning. |
| CRM | `FUTURE_CONSUMER_OF_US73_PLATFORM` | Organization/Sales ownership must be reconciled first. |
| HRMS | `FUTURE_CONSUMER_OF_US73_PLATFORM` | US-46/HRMS owns payroll-link semantics. |
| Fuel vendor | `FUTURE_CONSUMER_OF_US73_PLATFORM` | US-35 owns fuel-card/vendor business rules. |
| Telematics | `FUTURE_CONSUMER_OF_US73_PLATFORM` | US-48 owns device/location interpretation. |
| Payment | `FUTURE_CONSUMER_OF_US73_PLATFORM` | Finance/payment authority and stronger SoD are required. |
| Insurance | `FUTURE_CONSUMER_OF_US73_PLATFORM` | Freight/insurance domain owns policy/claim meaning. |
| DMS | `FUTURE_CONSUMER_OF_US73_PLATFORM` | US-83 owns document lifecycle, retention, permissions, and OCR association. |
| API | `FUTURE_CONSUMER_OF_US73_PLATFORM` | No generic outbound REST or API-gateway capability is claimed by US-73 acceptance. |
| Webhook | `FUTURE_CONSUMER_OF_US73_PLATFORM` | No public inbound webhook route is created by US-73 acceptance. |

There are no vendor requirements and no `SOURCE_EXAMPLE_ONLY` provider names. All named ecosystems are source-recognized future consumers, but only the file capability is currently supported. REST, webhook, inbound file, bidirectional adapters, and API-gateway behavior are `NOT_CURRENTLY_SUPPORTED`.

## 5. Direction and capability model

Provider-neutral direction values are `OUTBOUND`, `INBOUND`, and `BIDIRECTIONAL`. Direction is explicit per configuration and never inferred. `BIDIRECTIONAL` means both separately registered directions are supported; it is not a default.

The US-73 runtime capability registry contains only:

- type `FILE_EXCHANGE`;
- protocol `FILE_JSON_V1`;
- direction `OUTBOUND`.

An unsupported type/protocol/direction combination cannot be enabled and returns `INTEGRATION_CAPABILITY_UNSUPPORTED`. INBOUND and BIDIRECTIONAL are frozen model concepts for later adapters, not claims of current runtime support.

## 6. Configuration and lifecycle

`IntegrationConfiguration` is Tenant-owned and contains:

- `id: UUID`, internal trusted `tenantId: UUID`;
- `name: String` and Tenant-local unique normalized name;
- `type: FILE_EXCHANGE`, `protocol: FILE_JSON_V1`, `direction: OUTBOUND`;
- `endpointAlias: String` resolved against server configuration, never a path or URL from the API;
- optional `credentialReference: String` (unused by the accepted file adapter);
- `mappingVersionId: UUID`;
- `dataClassification: INTERNAL_OPERATIONAL_NON_SENSITIVE` for the accepted adapter;
- fixed `retryPolicy: US73_BOUNDED_V1`;
- lifecycle, health snapshot, last-tested and last-success metadata;
- optimistic `version`, created/updated time and actor.

No request DTO contains `tenantId`, status, health, attempt count, secret value, raw path, or arbitrary headers.

Configuration lifecycle is exactly `DRAFT → ACTIVE → DISABLED`; `DISABLED → ACTIVE` requires a fresh successful test. `ACTIVE` configurations cannot receive material edits. They must first be disabled. There is no delete operation.

Enabling requires:

- current capability supported;
- current mapping version valid/active;
- endpoint alias resolvable;
- a successful safe connection test against the current configuration version within 15 minutes;
- `INTEGRATION_ACTIVATE` and all applicable SoD checks.

Provider health is separate from lifecycle: `UNKNOWN`, `HEALTHY`, `DEGRADED`, `UNAVAILABLE`, `AUTH_FAILED`. Temporary failure never silently disables configuration and never makes an exchange successful.

## 7. Credential and secret model

Only an opaque `credentialReference` may be stored. A provider-neutral `IntegrationSecretResolver` outbound port resolves it at call time. The first implementation is environment-backed; an external secret-store adapter is future work. Database rows, APIs, UI, audit, logs, metrics, exceptions, and exchange payloads never contain a password, API key, client secret, private key, bearer token, or resolved secret.

The selected file adapter needs no credential. This avoids manufacturing a secret merely to populate a field.

Rotation rules:

- rotating the value behind an unchanged reference occurs outside application persistence, then requires a new successful connection test;
- changing a reference requires `DISABLED`, optimistic version, `INTEGRATION_MANAGE`, audit, and re-test before activation;
- revoked/missing/invalid credentials yield `AUTH_FAILED`, are permanent for that attempt, and are never echoed;
- REST/UI responses show only `credentialConfigured` plus a server-masked reference label, never the reference or value;
- a test connection uses a non-mutating/safe provider probe and returns only normalized health/error information.

No global configuration or cross-Tenant secret reference exists.

## 8. Mapping model

Integration owns transport mapping definitions; domains own semantic validation. Each `IntegrationMapping` row is one immutable version with Tenant, configuration, mapping key, positive version number, source contract/version, target schema/version, declarative rules, SHA-256 definition hash, lifecycle, timestamps, and actors.

The accepted mapping vocabulary is restricted to:

- select an allow-listed source field;
- rename it to a declared target field;
- supply an approved non-secret literal default;
- format ISO date/time, decimal, boolean, UUID, and declared enum values;
- omit an optional null field.

No JavaScript, Groovy, SpEL, template expression, arbitrary regex replacement, reflection, class name, SQL, network lookup, filesystem lookup, or plug-in code is allowed. Maximum mapping size is 100 target fields and nesting depth 10.

Before activation, mapping validation proves source/target contract registration, required fields, types, allowed transformations, duplicate target names, and output size. Active mapping versions are immutable. A change creates the next Tenant/configuration-local version. Every exchange stores the mapping-version ID and definition hash; historical meaning never changes silently.

## 9. Tenant, identity, and authorization

All configurations, mappings, exchanges, attempts, health, idempotency keys, scheduled claims, and audit records are Tenant-owned. Authenticated admin APIs derive Tenant from `CurrentTenant`; background work enumerates active Tenants through `TenantJobExecutor`, establishes a bounded context, and clears it. External correlation, endpoint alias, integration key, request fields, and event payload never establish Tenant authority.

Tenant A cannot list, read, test, enable, disable, update, or inspect Tenant B resources. Cross-Tenant and guessed-ID access is an indistinguishable 404 after authentication; manipulated `tenantId` input is rejected because no such field exists.

Permissions follow current uppercase convention:

- `INTEGRATION_VIEW` — list/detail/status;
- `INTEGRATION_MANAGE` — create/update DRAFT or DISABLED configuration/mapping;
- `INTEGRATION_TEST` — safe test connection;
- `INTEGRATION_ACTIVATE` — enable/disable;
- `INTEGRATION_AUDIT_VIEW` — exchange/attempt/audit history;
- `INTEGRATION_RECONCILE` — reserved for later approved manual reconciliation; it grants no action in the US-73 API.

Backend authorization is authoritative. UI visibility is only convenience.

For `FINANCIAL` or `RESTRICTED` future configurations, activation must be performed by a different active user from the last material configuration/mapping/credential-reference change, with `INTEGRATION_ACTIVATE` and a future context-specific permission. The selected non-sensitive file probe does not require dual control. ABAC is limited to this explicit data-classification/type check; no generic policy engine is introduced.

## 10. Durability, idempotency, retry, and reconciliation

### P1-01 reuse

The existing shared `DurableEventPublisher`, `DurableEventEnvelope`, `integration_outbox_event`, Tenant-aware worker, five-claim policy, 32-KiB envelope limit, and at-least-once semantics are reused. The new handler name is `integration-outbound-exchange`. There is no second outbox, inbox, broker, global order, or exactly-once claim.

P1-01 guarantees at-least-once delivery from an approved producer transaction to Integration. Integration's idempotent acceptance of that event is unique on `(tenant_id, integration_configuration_id, source_event_id, mapping_version_id)`. Replaying the same event creates no second exchange.

### External delivery state

External exchange status is exactly `PENDING`, `IN_PROGRESS`, `RETRY_SCHEDULED`, `SUCCEEDED`, or `FAILED`. A five-minute claim lease permits safe recovery of abandoned `IN_PROGRESS` work. The scheduler claims at most 50 due exchanges per Tenant. Integration exchange state is external-delivery lifecycle, not another outbox.

`US73_BOUNDED_V1` permits five total external attempts: one immediate attempt plus retries after 30 seconds, 2 minutes, 10 minutes, and 30 minutes. There is no infinite retry. A future HTTP adapter may honor a valid `Retry-After` bounded to 30 minutes without increasing the five-attempt total.

Retryable classifications are connection reset, timeout, 408, 429, 5xx, temporary DNS/network/provider outage, and temporary filesystem I/O. Permanent classifications are missing/revoked/invalid credential, invalid mapping, unsupported contract/version/capability, owning-domain validation rejection, malformed/oversized payload, disabled configuration, unsafe endpoint/path, endpoint misconfiguration including 404, and non-retryable 4xx.

Every attempt stores normalized outcome, safe code, start/end time, latency, attempt number, and correlation metadata. A failed attempt never sets `SUCCEEDED`. Exhaustion or permanent error sets `FAILED` with a sanitized terminal code.

### Reconciliation

US-73 provides read-only reconciliation evidence: exchange identity, source event ID/type, mapping version/hash, canonical payload hash, attempt chronology, safe result code, provider/external correlation ID if present, target filename for the file adapter, and timestamps. Operators cannot edit payload/history, retry manually, or mark an exchange successful. Manual retry/resolution and safe-error download are deferred. Any later retry must retain the same idempotency identity.

## 11. Payload, file, webhook, REST, and SSRF security

### Payload persistence

The durable producer envelope and canonical exchange payload are each capped at 32 KiB. Integration persists only the minimized canonical payload needed for retry, its SHA-256 hash, registered contract/version, mapping version/hash, safe status/correlation, and attempt metadata. It stores no whole aggregate or raw provider response/body.

The accepted platform probe is `INTERNAL_OPERATIONAL_NON_SENSITIVE`. The file adapter rejects PII, financial, medical, credential, authentication, precise-location, POD, or other restricted contracts. Each later domain integration must freeze minimization, retention, and a field/envelope-encryption and key-ownership strategy before registering sensitive payloads. Indiscriminate encryption without key governance is not approved.

Succeeded canonical payloads are eligible for purge after 30 days; failed payloads after 90 days. Safe exchange/attempt metadata and audit remain; no automatic purge is added until an approved Tenant-qualified maintenance action exists. This aligns with, but does not mutate, P1-01 retention.

### Selected file adapter

- Output only; one canonical UTF-8 `application/json` file per exchange.
- Endpoint is a logical alias mapped by server configuration to an allow-listed non-web-accessible root. API/UI never accepts or returns a raw path.
- Filename is server-generated as `<exchange-uuid>.json`; no external/user filename or directory segment is used.
- Root and target are normalized/canonicalized; symlinks are not followed; the target must remain beneath the configured root.
- Maximum file/payload size is 32 KiB; maximum nesting depth is 10; SHA-256 is stored.
- Write to a same-directory `.part` staging file, flush, then atomically rename without overwrite. On retry, an existing final file with the same hash means idempotent success; a different hash is terminal integrity failure.
- Recommended filesystem permissions are directory `0750` and file `0640`, subject to deployment identity policy.
- Outbound system-generated JSON requires no malware scan. Any future inbound/binary/file-upload adapter must make scanning capability and quarantine an explicit decision.

### Webhook and rate limiting

Webhook security is `NOT_APPLICABLE` to the selected adapter and no public route exists. A future inbound webhook requires TLS, a 256-bit opaque endpoint identity stored as a hash, HMAC or asymmetric signature, a five-minute timestamp window, Tenant/configuration-scoped event-ID replay protection, JSON content type, 32-KiB limit, bounded rate limiting, and constant-time verification. IP allow-listing may be defense in depth but never sole authentication.

### SSRF and outbound REST

SSRF is `NOT_APPLICABLE` to the selected adapter because operators cannot configure a URL. A future URL adapter must be HTTPS-only; allow only approved ports (default 443); reject userinfo and fragments; disable redirects; resolve and revalidate DNS for every connection; block loopback, link-local, private, multicast, reserved, and cloud metadata ranges for IPv4/IPv6; pin the validated address for the connection; bound connect/read timeouts, headers, request/response sizes; and never permit arbitrary authorization headers or certificate bypass. A separately approved allow-list is preferred.

Outbound REST certificate validation uses the platform trust store; insecure TLS, hostname bypass, and automatic redirects are forbidden. No dependency such as Resilience4j is approved by this decision. Timeouts, bounded retry, and a minimal persisted open/half-open health policy may be implemented without a new library if a future HTTP adapter needs it.

## 12. Health, observability, and audit

Lifecycle, provider health, and last successful exchange are separate fields. A connection test performs only an atomic write/delete probe containing no business data, secret, or arbitrary operator content. It does not create a domain transaction or declare the provider generally healthy forever. Tests are limited to five per user/configuration per minute.

Safe metrics are Tenant-partitioned exchange count, success/failure/retry counts, latency, last success, due backlog, oldest due age, and terminal failures. Payload, filename, endpoint path, credential reference/value, authorization header, and customer/driver/location identifiers are forbidden metric labels and logs.

Integration owns append-only audit facts for configuration create/change, enable/disable, credential-reference change, mapping-version creation/activation, test connection, durable fact acceptance, each exchange attempt, terminal failure, and reconciliation view. Audit stores trusted Tenant, actor/service identity, action, target IDs, old/new definition hashes where applicable, safe outcome/code, time, correlation ID, and request correlation ID—never a secret or payload. US-75 may consume a published safe audit projection; it never reads Integration tables.

## 13. Frozen API and UI

All operator routes are authenticated under `/api/v1/integrations`:

| Method and path | Purpose | Permission |
|---|---|---|
| `GET /api/v1/integrations` | Pageable/filterable list; default 20, maximum 100 | `INTEGRATION_VIEW` |
| `POST /api/v1/integrations` | Create DRAFT with nested initial mapping | `INTEGRATION_MANAGE` |
| `GET /api/v1/integrations/{id}` | Safe configuration/mapping/health detail | `INTEGRATION_VIEW` |
| `PUT /api/v1/integrations/{id}` | Full update of DRAFT/DISABLED config; mapping change creates immutable next version | `INTEGRATION_MANAGE` |
| `POST /api/v1/integrations/{id}/test` | Safe connection probe | `INTEGRATION_TEST` |
| `POST /api/v1/integrations/{id}/enable` | Activate validated/tested config | `INTEGRATION_ACTIVATE` |
| `POST /api/v1/integrations/{id}/disable` | Stop new exchange attempts/claims | `INTEGRATION_ACTIVATE` |
| `GET /api/v1/integrations/{id}/exchanges` | Pageable safe exchange/attempt history; default 20, maximum 100 | `INTEGRATION_AUDIT_VIEW` |

There is no DELETE, generic send, manual retry, reconcile mutation, secret read, raw payload read, or external/public API. Create/update request fields are `name`, `type`, `protocol`, `direction`, `endpointAlias`, optional `credentialReference`, `dataClassification`, nested declarative `mapping`, and `version` on update. Server-derived fields are omitted from requests.

Responses mask credential configuration and expose no raw path, canonical payload, provider body, secret, authorization header, or stack trace. The global API-error envelope remains authoritative. Frozen codes are `INTEGRATION_NOT_FOUND`, `INTEGRATION_DISABLED`, `INTEGRATION_CONFIGURATION_INVALID`, `INTEGRATION_CAPABILITY_UNSUPPORTED`, `INTEGRATION_AUTH_FAILED`, `INTEGRATION_MAPPING_INVALID`, `INTEGRATION_PAYLOAD_INVALID`, `INTEGRATION_DUPLICATE`, `INTEGRATION_RATE_LIMITED`, `INTEGRATION_PROVIDER_UNAVAILABLE`, `INTEGRATION_TERMINAL_FAILURE`, `INTEGRATION_CONFLICT`, and `INTEGRATION_FILE_INTEGRITY_FAILURE`.

The operator UI contains list, create/edit detail, declarative mapping, masked credential indicator, lifecycle/health/last-test/last-success, test, enable/disable, and paginated safe exchange history. It contains no downstream business-rule UI and never displays secrets, raw payloads, authorization headers, tokens, full paths, or provider response bodies.

## 14. Expected persistence

Implementation will likely require a forward Flyway migration selected only after rechecking the then-current head. V61 is not reserved.

Integration owns these expected Tenant-scoped tables:

| Table | Purpose and critical keys |
|---|---|
| `integration_configuration` | Config/lifecycle/health/endpoint and credential references; Tenant-local normalized name; optimistic version. |
| `integration_mapping` | Immutable mapping versions; unique `(tenant_id, configuration_id, mapping_key, mapping_version)`; definition hash. |
| `integration_exchange` | Minimized canonical payload/hash, source event, mapping snapshot, status, due/lease/correlation; unique `(tenant_id, configuration_id, source_event_id, mapping_version_id)`. |
| `integration_exchange_attempt` | Parent-owned immutable attempt history with Tenant-consistent FK and unique attempt number. |
| `integration_audit_event` | Append-only Integration action/outcome/hash audit; no payload or secret. |

All have `tenant_id UUID NOT NULL`, Tenant-leading indexes, audit timestamps, logical cross-module UUID references only, and no physical FK to another module. Same-module parent/child relationships use Tenant-consistent constraints. Exact columns/dictionaries are implementation work and must be synchronized with the eventual migration.

## 15. Test and acceptance contract

Implementation must add domain and application tests for lifecycle, mapping immutability/validation, classification, idempotency, retry classification/backoff/exhaustion, health separation, and terminal failure. Security tests cover every literal `/api/v1/integrations` route, permissions, SoD, no `tenantId` authority, Tenant A/B object/list/test/update/enable/disable/history denial, secret/payload/path redaction, test throttling, and malformed/oversized content.

PostgreSQL acceptance uses only `transport_logistics_acceptance` and proves Flyway-current constraints, Tenant-local uniqueness, optimistic concurrency, claim lease/recovery, event replay, attempt ordering, terminal status, and no duplicate exchange. Architecture checks prove:

- Integration imports no foreign repository/entity/persistence package;
- no domain module imports Integration adapters/application internals;
- only Integration owns its five tables;
- durable publication reuses shared P1-01 types/table and no other outbox/inbox/broker exists;
- public contracts are provider-neutral, Tenant-bearing, and published at the module root;
- domain/application layers contain no Spring/JPA/Jackson/HTTP/filesystem/vendor imports.

Static analysis, frontend TypeScript/Vitest/build/changed-file lint, and full Maven verify are mandatory. Performance acceptance covers a 50-exchange claim batch, 32-KiB rejection boundary, bounded paginated history, duplicate storms, lease recovery, and no unbounded list/query.

### Controlled-sandbox E2E

Final acceptance must execute this complete flow against a real isolated filesystem root:

1. authorized operator creates a DRAFT `FILE_JSON_V1` OUTBOUND configuration with a valid declarative mapping;
2. invalid mapping is rejected;
3. connection test performs the real safe filesystem probe and records audit without business data;
4. operator enables the current tested version;
5. a committed `US73_PLATFORM_PROBE_V1` fact is published through P1-01;
6. the shared outbox records and claims it;
7. Integration idempotently creates an exchange and snapshots mapping version/hash;
8. the real file adapter atomically writes `<exchange-id>.json` beneath the allowlisted sandbox root;
9. hash, target filename, safe correlation, and `SUCCEEDED` are recorded;
10. replay of the same event creates no second exchange or file;
11. a real transient filesystem fault yields `RETRY_SCHEDULED` and later succeeds within the frozen policy;
12. invalid/unsafe endpoint or permanent file-integrity failure becomes `FAILED`, never success;
13. UI history shows safe status/attempts without raw payload/path/secret;
14. disabled configuration performs no new exchange;
15. a limited user is denied mutations and Tenant B receives no existence or history signal for Tenant A.

Claimed evidence tier is only `CONTROLLED_SANDBOX`; this does not claim ERP/vendor interoperability.

## 16. Implementation slicing and downstream boundary

Recommended small change sets:

- `US-73-CS01`: pure domain/ports, lifecycle/mapping/idempotency/retry model, permissions and architecture baselines;
- `US-73-CS02`: Flyway-current Integration persistence and P1-01 durable handler;
- `US-73-CS03`: governed JSON-file adapter, scheduler, health, audit, and controlled-sandbox tests;
- `US-73-CS04`: operator API/UI, literal-route security, PostgreSQL/Chromium closure and full regression.

US-73 may later support:

- US-35 fuel-card imports;
- US-46 payroll/HRMS export;
- US-47 Accounting/ERP handoff;
- US-48 telematics ingestion;
- US-72 regulatory feeds;
- US-76 push/device services;
- US-83 DMS/OCR.

Each later story must register its own source/target contract, mapping, direction, data classification, security, provider/sandbox, durability, reconciliation, and acceptance evidence. None is implemented or accepted by US-73.

## 17. Definition of Ready

- [x] authoritative source, actor, goal, categories, acceptance criteria, and boundary reconciled;
- [x] dedicated Integration owner and provider-neutral architecture frozen;
- [x] one governed outbound JSON-file adapter and `CONTROLLED_SANDBOX` evidence selected;
- [x] current versus future integration categories and directions classified;
- [x] Tenant, credential-reference/environment resolver, rotation, and redaction model frozen;
- [x] declarative versioned mapping and no-script rule frozen;
- [x] P1-01 reuse, at-least-once, idempotency, retry/backoff, lease and terminal failure frozen;
- [x] read-only reconciliation and no manual retry/success rewriting frozen;
- [x] file security frozen; webhook/SSRF/REST requirements frozen for future adapters;
- [x] RBAC, SoD, audit, observability, limits, API/UI, expected persistence and error model frozen;
- [x] PostgreSQL/architecture/security/frontend/controlled-sandbox E2E strategy frozen;
- [x] downstream story and source-parity boundaries frozen.

US-73 is therefore `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`. Story accounting remains unchanged.

**Next task:** `US-73-EXTERNAL-INTEGRATIONS-IMPLEMENTATION-001`.
