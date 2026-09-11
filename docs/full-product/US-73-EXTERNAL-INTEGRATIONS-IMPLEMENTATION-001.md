# US-73 Manage External Integrations — Implementation Evidence

**Task:** `US-73-EXTERNAL-INTEGRATIONS-IMPLEMENTATION-001`
**Status:** `COMPLETE` after `US-73-EXTERNAL-INTEGRATIONS-FINAL-ACCEPTANCE-001`
**Program accounting:** 66 / 87 COMPLETE; 21 / 87 remaining
**Migration:** V61; current Flyway head V61
**Evidence tier:** `CONTROLLED_SANDBOX`

## Implemented boundary

US-73 introduces the dedicated top-level `integration` bounded context. It owns provider-neutral configuration, immutable mapping versions, health, external-exchange state, immutable attempts, and append-only safe audit facts. Domain and application code remain framework-neutral; persistence, filesystem, secrets, scheduling, durable-event handling, and REST are adapters. Integration does not access another module's repository, entity, or table.

The only executable capability is `FILE_EXCHANGE / FILE_JSON_V1 / OUTBOUND`, implemented by `GOVERNED_OUTBOUND_JSON_FILE_EXCHANGE`. ERP, accounting, CRM, HRMS, fuel-card, telematics, payment, insurance, DMS/OCR, REST, webhook, inbound file/API, brokers, and vendor SDKs remain deferred.

## Domain, mapping, and security

- Configuration lifecycle is `DRAFT -> ACTIVE -> DISABLED`; reactivation requires a successful test of the current version within 15 minutes. Active configurations cannot receive material edits, and credential-reference changes require a disabled configuration plus optimistic versioning.
- Health is independent: `UNKNOWN`, `HEALTHY`, `DEGRADED`, `UNAVAILABLE`, or `AUTH_FAILED`.
- Mappings are immutable, positive, versioned declarations. Supported operations are allow-listed selection/rename, approved non-secret defaults, scalar formatting, and omission of optional nulls. Scripting, reflection, SQL, network, and filesystem lookup transformations are absent.
- The current adapter accepts only `INTERNAL_OPERATIONAL_NON_SENSITIVE` data and the registered `US73_PLATFORM_PROBE_V1` source/target contract.
- Tenant authority comes only from authenticated `CurrentTenant`; DTOs expose no authoritative `tenantId`. Persistence keys, uniqueness, scans, claims, audit, and APIs are Tenant-scoped.
- V61 seeds `INTEGRATION_VIEW`, `INTEGRATION_MANAGE`, `INTEGRATION_TEST`, `INTEGRATION_ACTIVATE`, `INTEGRATION_AUDIT_VIEW`, and reserved `INTEGRATION_RECONCILE`. All eight literal `/api/v1/integrations...` routes and their servlet-context-relative forms have permission regressions.
- Only an opaque credential reference may be stored. `IntegrationSecretResolver` has an environment-backed adapter; API/UI/audit expose only configured/masked facts. The selected file adapter requires no credential.

## Durable delivery and file adapter

The Integration-owned acceptance coordinator publishes the minimized probe through the shared P1-01 `DurableEventPublisher`. Consumer `integration-outbound-exchange` durably accepts it into Integration exchange state before the shared outbox row is acknowledged. The existing `integration_outbox_event` remains the sole outbox; no broker, inbox, or second scheduler queue was introduced.

External delivery is at-least-once. Exchange identity is unique on Tenant, configuration, source event, and mapping version. Status is `PENDING`, `IN_PROGRESS`, `RETRY_SCHEDULED`, `SUCCEEDED`, or terminal `FAILED`. Claims are limited to 50 due rows per Tenant with a five-minute lease. `US73_BOUNDED_V1` permits five total attempts: immediate, then 30 seconds, 2 minutes, 10 minutes, and 30 minutes. Attempts and safe terminal errors are immutable.

The file adapter resolves only a server allow-listed endpoint alias. It normalizes and verifies root/target containment, refuses traversal and symlink following, generates `<exchange-uuid>.json`, writes UTF-8 JSON through a same-directory `.part` file, syncs, and atomically renames without overwrite. Existing same-hash content is idempotent success; different content is terminal `INTEGRATION_FILE_INTEGRITY_FAILURE`. Payloads over 32 KiB are rejected. POSIX-capable runtimes use directory `0750` and file `0640`.

## Persistence and API

Forward migration `V61__external_integrations_us73.sql` creates exactly five Integration-owned Tenant tables: `integration_configuration`, `integration_mapping`, `integration_exchange`, `integration_exchange_attempt`, and `integration_audit_event`. It adds Tenant-leading indexes, same-module Tenant-consistent foreign keys, normalized-name/mapping-version/exchange-idempotency/attempt-order uniqueness, enum checks, optimistic versions, due/lease indexes, and no second outbox.

Implemented API surface:

- `GET/POST /api/v1/integrations`
- `GET/PUT /api/v1/integrations/{id}`
- `POST /api/v1/integrations/{id}/test`
- `POST /api/v1/integrations/{id}/enable`
- `POST /api/v1/integrations/{id}/disable`
- `GET /api/v1/integrations/{id}/exchanges`

Lists default to 20 and cap at 100. There is no delete, arbitrary send, manual retry, mark-success, reconciliation mutation, secret/raw-payload read, raw-path, public webhook, or inbound route.

## Operator UI

The feature-first React integration UI provides permission-gated list, create/detail/edit, mapping configuration, safe connection test, enable/disable, and read-only exchange history. It uses the shared API client, TanStack Query, React Hook Form, Zod, and Ant Design. It never renders a secret value, canonical payload, provider body, or raw filesystem path.

## Verification evidence

- Independent technical-closure focused Integration gate: `24` tests, `0` failures, `0` errors, `0` skipped, including clean PostgreSQL V1-V61 migration, security, domain, persistence, and real filesystem adapter tests.
- Independent P1-01 plus US-69/70 regression gate: `40` tests, `0` failures, `0` errors, `0` skipped.
- Complete Java/Maven closure gate: `1276` tests, `0` failures, `0` errors, `15` skipped; `BUILD SUCCESS` in `04:56` under Java 21.
- Architecture and Spring Modulith: `44/44` pass, including Integration layer/dependency/table-ownership rules.
- Static analysis: Checkstyle `0` violations, PMD pass, SpotBugs `0` findings/errors under Java 21.
- PostgreSQL acceptance used only `transport_logistics_acceptance` as authoritative destructive evidence and verified Flyway V1-V61, constraints, Tenant isolation, uniqueness, idempotency, ordered attempts, 50-row claim bound, lease recovery, terminal failure, and shared-outbox reuse.
- Controlled-sandbox Chromium closure journey: `6/6` pass in `1.3m` with fresh local servers, real PostgreSQL, REST, React, durable outbox handling, and real filesystem I/O. It covers create/invalid mapping/test/enable/success, duplicate replay, retry then success, permanent integrity failure, safe history, disablement, RBAC, and cross-Tenant isolation.
- Frontend: TypeScript pass; Vitest `60` files and `260` tests pass; production build pass; changed-file ESLint pass. Repository-wide ESLint retains `71` pre-existing errors in eight unchanged Delivery files; US-73 introduced errors are zero.
- `git diff --check` passes with no reported whitespace errors.

One non-authoritative setup invocation earlier inherited the local development datasource and applied the forward V61 migration there. No destructive acceptance cleanup or acceptance query used that database, and it is not cited as evidence. All authoritative PostgreSQL acceptance and the final Maven gate used `transport_logistics_acceptance` on port 5433.

## Deferred integrations

Every future business ecosystem must separately govern its owner, source fact, contract/version, mapping, classification, credential/provider model, durability, reconciliation, and real acceptance path. US-73 does not mark any downstream integration implemented or accepted.

## Closure state

`US-73-EXTERNAL-INTEGRATIONS-TECHNICAL-CLOSURE-001` passed independently with zero unresolved technical blockers. `US-73-EXTERNAL-INTEGRATIONS-FINAL-ACCEPTANCE-001` then independently reran the complete acceptance matrix and accepted only the frozen controlled outbound JSON-file capability. US-73 is `COMPLETE`; story accounting is 66 / 87 complete and 21 / 87 remaining. The next task is `US-78-OPERATIONAL-EXCEPTIONS-PRODUCT-DECISIONS-001`.
