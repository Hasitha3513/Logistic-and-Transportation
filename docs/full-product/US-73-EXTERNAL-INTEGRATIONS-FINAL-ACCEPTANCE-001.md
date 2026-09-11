# US-73 Manage External Integrations — Independent Final Acceptance

**Task:** `US-73-EXTERNAL-INTEGRATIONS-FINAL-ACCEPTANCE-001`  
**Decision:** `COMPLETE`  
**Accepted capability:** `FILE_EXCHANGE / FILE_JSON_V1 / OUTBOUND`  
**Adapter:** `GOVERNED_OUTBOUND_JSON_FILE_EXCHANGE`  
**Evidence tier:** `CONTROLLED_SANDBOX`  
**Migration:** V61  
**Acceptance date:** 2026-09-03

## Independent decision

US-73 satisfies all three authoritative criteria: a configured supported capability exchanges its registered data; failed delivery is never represented as synchronized; and bounded retry plus safe error status and immutable attempt history are recorded. The dedicated `integration` bounded context owns only configuration, declarative mapping, health, exchange lifecycle, attempts, external retry, and Integration audit. It does not own downstream business meaning or access another module's repositories, entities, tables, or application internals.

The accepted runtime surface is deliberately narrow. Only the governed outbound JSON-file adapter is complete. This acceptance does not claim an ERP, accounting, CRM, HRMS, fuel-card, telematics, payment, insurance, DMS/OCR, REST, webhook, inbound-file, vendor-SDK, or other ecosystem connection.

## Architecture, security, and reliability

- Domain, ports, and application code are framework-neutral; filesystem, JPA, Spring transaction, secret resolution, and REST concerns remain in adapters.
- Tenant authority comes from `CurrentTenant` or trusted background execution. Tenant A/B object, list, mutation, test, and history isolation passes with safe not-found behavior.
- Only opaque environment credential references may be persisted. The accepted file adapter requires no credential, and API/UI/audit/history exclude secret values, full references, raw paths, canonical payloads, provider bodies, authorization material, and stack traces.
- Mapping versions are Tenant-scoped, immutable, declarative, SHA-256 hashed, limited to 100 fields and depth 10, and retain exact version/hash history. No scripting or lookup engine exists.
- Only `US73_PLATFORM_PROBE_V1` with `INTERNAL_OPERATIONAL_NON_SENSITIVE` data is registered. The canonical payload is minimized and capped at 32 KiB.
- P1-01's `DurableEventPublisher`, `DurableEventEnvelope`, and `integration_outbox_event` are reused through `integration-outbound-exchange`. There is no second outbox, inbox, broker, exactly-once, or global-ordering claim. Shared outbox acknowledgement occurs after durable Integration acceptance, before external filesystem completion.
- External delivery is truthfully at-least-once and idempotent on Tenant, configuration, source event, and mapping version. Status is exactly `PENDING`, `IN_PROGRESS`, `RETRY_SCHEDULED`, `SUCCEEDED`, or `FAILED`.
- A five-minute lease, maximum 50-row Tenant claim, and five total attempts—immediate, 30 seconds, 2 minutes, 10 minutes, and 30 minutes—bound processing. Permanent and exhausted failures become `FAILED` with sanitized codes; reconciliation remains read-only.
- The file adapter accepts only the server-side `CONTROLLED_SANDBOX` alias, generates `<exchange-uuid>.json`, enforces normalized containment and symlink protection, stages in a same-directory `.part`, forces the write, atomically renames without overwrite, and uses same-hash success/different-hash terminal integrity behavior. POSIX-capable execution applies directory `0750` and file `0640`.
- The safe connection test performs real write/delete I/O, is limited to five tests per actor/configuration/minute, and activation requires a successful current-version test within 15 minutes. Lifecycle remains `DRAFT -> ACTIVE -> DISABLED`; health is independent.
- All six Integration permissions are present. `INTEGRATION_RECONCILE` remains reserved and exposes no mutation route. The future financial/restricted SoD rule remains outside the accepted non-sensitive probe path.

## API, UI, database, and performance

Production exposes exactly eight authenticated operator endpoints under `/api/v1/integrations`: list, create, detail, update, test, enable, disable, and read-only exchange history. There is no delete, arbitrary send, manual retry, mark-success, reconciliation mutation, secret/raw-payload read, or inbound/public Integration route. E2E fixture endpoints are restricted to the `e2e` Spring profile. Pagination defaults to 20 and caps at 100; request DTOs provide no Tenant, lifecycle, health, attempt, secret-value, raw-path, filename, or arbitrary-header authority.

The feature-first React UI contains only list, create/edit, declarative mapping, test, enable/disable, health/status, and safe history workflows. It uses the existing shared API/query/form stack and exposes none of the excluded business ecosystems or sensitive fields.

V61 creates exactly five Tenant-owned Integration tables: `integration_configuration`, `integration_mapping`, `integration_exchange`, `integration_exchange_attempt`, and `integration_audit_event`. It adds the six permissions, Tenant-leading indexes, Tenant-consistent same-module foreign keys, immutable-version and idempotency uniqueness, bounded attempt constraints, and due/lease indexes. V1-V60 remain unchanged, and the shared P1-01 outbox remains owned by `shared`.

## Fresh acceptance evidence

| Gate | Result |
| :--- | :--- |
| Focused Integration | 24 tests, 0 failures, 0 errors, 0 skipped; clean PostgreSQL V1-V61 exercised |
| P1-01 plus US-69/70 regression | 40 tests, 0 failures, 0 errors, 0 skipped |
| Full Maven | 1,276 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 05:04 under Java 21 |
| Architecture / Spring Modulith | 44/44 PASS |
| Static analysis | Checkstyle 0 violations; PMD PASS; SpotBugs 0 findings/errors |
| Frontend | TypeScript PASS; Vitest 60 files/260 tests PASS in 45.66s; production build PASS in 4.75s; changed-file ESLint PASS |
| Global ESLint classification | 71 pre-existing errors in eight unchanged Delivery files; `US73_INTRODUCED_ERRORS = 0` |
| Real Chromium | 6/6 PASS in 1.3 minutes against fresh REST/React servers, PostgreSQL, shared outbox, and real isolated filesystem I/O |
| Git whitespace | `git diff --check` PASS |

All authoritative PostgreSQL evidence used only `jdbc:postgresql://127.0.0.1:5433/transport_logistics_acceptance` on PostgreSQL 16.15. The focused database suite repeatedly rebuilt a clean V1-V61 schema and proved Tenant uniqueness, mapping versions, idempotency, attempt ordering, Tenant-consistent foreign keys, lease recovery, 50-row claiming, terminal failure, and shared-outbox reuse.

During final-acceptance setup, one discarded focused invocation supplied only `POSTGRES_PORT`; generic Spring tests therefore connected to the development database and reported applying V61 from V60, while the destructive PostgreSQL class failed before execution because sandboxed Docker discovery was unavailable. That invocation is excluded from evidence. No query, cleanup, repair, rollback, or further mutation of the development database was performed by acceptance after discovery. Every accepted rerun pinned both Spring and destructive-test datasource variables to `transport_logistics_acceptance`.

## Final state

US-73 is `COMPLETE`. Program accounting advances from 65 to **66 / 87 COMPLETE**, leaving **21 / 87**. Wave A remains open because US-78 is not accepted. The next authorized task is `US-78-OPERATIONAL-EXCEPTIONS-PRODUCT-DECISIONS-001`.
