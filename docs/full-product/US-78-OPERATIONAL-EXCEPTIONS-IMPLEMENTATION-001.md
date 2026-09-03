# US-78 Manage Operational Exceptions — Implementation Evidence

**Task:** `US-78-OPERATIONAL-EXCEPTIONS-IMPLEMENTATION-001`
**Status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`
**Program accounting:** 66 / 87 COMPLETE; 21 / 87 remaining
**Migration:** V62; current Flyway head V62
**Next task:** `US-78-OPERATIONAL-EXCEPTIONS-TECHNICAL-CLOSURE-001`

## Owner and model

US-78 introduces the top-level `operations` bounded context and its hybrid `OperationalExceptionCase` lifecycle aggregate. Operations owns confirmed classification and severity, assignment, SLA facts, escalation, corrective actions, RCA, resolution, closure/reopen, and append-only history. Routing and Delivery retain detection, source meaning, evidence, state, and corrections. Operations persists only logical source UUIDs and does not access a foreign repository, entity, table, JPA relationship, or physical foreign key.

## Durable Routing and Delivery intake

Routing US-22 authoritative disruption creation and Delivery US-62 authoritative exception creation publish the allow-listed `OperationalExceptionFactV1` contract atomically with their existing source transactions through the shared P1-01 `DurableEventPublisher`. The Operations durable handler uses consumer `operations-exception-intake`, at-least-once delivery, and `UNIQUE (tenant_id, source_event_id)` case dedupe. Replaying a source event reuses its case; a distinct event for the same source aggregate remains distinct. No second outbox, inbox, broker, or workflow engine was introduced.

The contract accepts only Routing types `ROAD_CLOSURE`, `ACCIDENT`, `WEATHER`, and `RESTRICTION`, and Delivery types `DAMAGED_DELIVERY`, `WRONG_ADDRESS`, `PARTIAL_DELIVERY`, `OTP_MISMATCH`, and `RECIPIENT_REFUSAL`. Summary codes and safe metadata keys are source-specific allow lists. Metadata is capped at 20 entries, keys at 64 characters, values at 256 characters, and canonical metadata at 4 KiB.

## Lifecycle, SLA, assignment, and escalation

- Lifecycle is `OPEN -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED -> CLOSED`, with explicit start from open, resolution rejection to in-progress, and reasoned reopen from closed to in-progress. Skipped close/resolve transitions are forbidden.
- Severity is `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`; classification is one of the seven frozen categories. Reclassification requires reason and expected version.
- Server-side 24x7 response/resolution targets are 8h/72h, 4h/24h, 1h/8h, and 15m/2h respectively. At-risk begins at 75% of the resolution window. Critical intake escalates immediately.
- The one bounded scheduler adapter reuses `TenantJobExecutor`, the server clock, a Tenant-scoped pessimistic due scan, and a maximum of 50 cases per Tenant. Resolved/closed cases and L3 cases cannot receive stale escalation.
- Assignment is only to a validated same-Tenant eligible user or one of the seeded role queues. Critical cases cannot be unassigned. Assignment changes append actor, time, before/after target, and mandatory reason.
- Escalation is a monotonic `L0..L3` fact, never a lifecycle state and never an implicit severity mutation. Critical intake, SLA scans, and authorized manual commands publish the safe `OPERATIONAL_EXCEPTION_ESCALATED_V1` fact through P1-01.

Notification consumes only the safe escalation fact. Notification continues to own recipient resolution, templates, channels, quiet hours, suppression, attempts, provider retry, and delivery history.

## Corrective actions, RCA, closure, and history

Corrective actions are `CORRECTIVE` or `PREVENTIVE`, owned by `USER` or `ROLE_QUEUE`, and transition through `OPEN`, `IN_PROGRESS`, `COMPLETED`, or reasoned `CANCELLED`. They store only bounded plain text and a logical evidence/result reference; they never mutate the source domain.

RCA uses the frozen cause catalogue. High and critical cases require an independently approved RCA before closure; the approver must differ from the author. Resolution requires in-progress state and all required actions complete. Closure requires resolved state, validated resolution, completed actions, required approved RCA, and for high/critical cases a closer different from the resolver. Case, action, and RCA commands use expected versions and stale writes return `OPERATIONAL_EXCEPTION_CONFLICT`.

Every intake, classification, assignment, lifecycle, escalation, action, and RCA fact appends immutable Tenant-scoped history. No delete/purge API exists; retention remains `RETENTION_POLICY_EXTERNAL_TO_US78`.

## Security and privacy

V62 seeds `OPERATIONAL_EXCEPTION_VIEW`, `OPERATIONAL_EXCEPTION_MANAGE`, `OPERATIONAL_EXCEPTION_ASSIGN`, `OPERATIONAL_EXCEPTION_ESCALATE`, `OPERATIONAL_EXCEPTION_RCA`, `OPERATIONAL_EXCEPTION_CLOSE`, and `OPERATIONAL_EXCEPTION_AUDIT_VIEW`. All 17 exact command/query routes are protected at both literal external `/api/v1/...` and effective servlet-context paths. Tenant authority is derived from authenticated server context, and cross-Tenant identifiers fail as not found.

Operations excludes POD media/signatures, medical records, GPS tracks, financial documents, credentials, OTPs, addresses/contact destinations, provider bodies, and whole source objects from persistence, APIs, and events. US-83 remains the owner of document bytes, access, versioning, and retention.

## Persistence and API

Forward migration `V62__operational_exceptions_us78.sql` creates five Operations-owned tables: `operational_exception_case`, `operational_exception_assignment_history`, `operational_exception_corrective_action`, `operational_exception_rca`, and `operational_exception_history`. They enforce Tenant-consistent child foreign keys, Tenant-leading query/due/history indexes, reference and source-event uniqueness, enum/check constraints, immutable logical source facts, and optimistic versions for mutable records.

The implemented `/api/v1/operational-exceptions` surface contains pageable list, detail, and history plus explicit `classify`, `acknowledge`, `assign`, `start`, `escalate`, corrective-action create/start/complete, RCA record/approve, `resolve`, `close`, `reject-resolution`, and `reopen` commands. There is no manual create, generic status patch, delete, source correction, raw evidence, export, or customer API.

## Operator UI

The feature-first React implementation lives under `features/operations/operationalExceptions` and adds `/operations/exceptions` within the existing `AppLayout`. The queue exposes safe source summary, category, severity, status, assignment, due times, and SLA state. Detail actions, corrective actions, RCA, and audit history are permission-gated. It uses the shared API client, TanStack Query, React Hook Form, Zod, and Ant Design, and adds no analytics dashboard or customer-facing workflow.

## Verification evidence

- Focused Operations/Routing/Delivery domain and producer tests: 26/26 pass.
- Deterministic concurrency/interleaving tests: 6/6 pass without sleeps, covering duplicate intake, competing assignment, close versus escalation, action completion versus resolution, RCA approval versus closure, SLA versus close/reopen, and escalation replay.
- PostgreSQL acceptance: 3/3 pass against only `transport_logistics_acceptance`, including clean V1-V62 migration, five tables/indexes, source replay, Tenant consistency, immutable history, stale-version rejection, lifecycle/action/RCA consistency, and the SLA worker.
- Literal and effective security plus E2E-profile safety: 3/3 pass. Opt-in local identity bootstrap regression: 1/1 pass with the complete 146-permission set.
- Complete Maven verification: 1,296 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 05:14 under Java 21.
- Architecture and Spring Modulith: 49/49 pass, including module dependencies, hexagonal layers, P0/P1 rules, and ownership of all five Operations tables.
- Static analysis: Checkstyle 0 violations; PMD pass; SpotBugs 0 findings/errors.
- Frontend: TypeScript pass; Vitest 61 files/261 tests pass; production build pass; changed-file ESLint pass. Repository-wide ESLint retains 71 pre-existing errors in eight unchanged Delivery files; US-78 introduced errors are zero.
- Real PostgreSQL-backed Chromium: 6/6 pass in 19.1 seconds with fresh backend/authenticated sessions, real Routing and Delivery producers, shared durable processing, the common Operations lifecycle, safe Notification fact, separate RCA approver/closer, source immutability, replay dedupe, RBAC, and cross-Tenant non-inference.
- `git diff --check` passes with no whitespace errors.

One early non-authoritative security-test invocation inherited the development datasource and advanced its Flyway schema from V61 to V62. It did not run destructive cleanup or mutate business rows and is not acceptance evidence. Every authoritative PostgreSQL acceptance, complete Maven gate, and real browser suite explicitly used `transport_logistics_acceptance` on port 5433.

## Deferred detectors and scope containment

Fuel US-38, Tracking US-55, Trip/Cargo/Driver/Fleet/Compliance/Integration detectors, and US-86 disruption replanning remain deferred until their own frozen producer contracts. US-78 adds no generic case engine, foreign write, cross-module FK, arbitrary manual case creation, second outbox/inbox, second scheduler, second workflow, document store, analytics dashboard, or customer exception UI.

## Closure state

The frozen US-78 implementation is technically complete with zero unresolved technical blockers. It is not product-accepted or counted complete. Status is `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`; program accounting remains 66 / 87 complete with 21 remaining. The next task is `US-78-OPERATIONAL-EXCEPTIONS-TECHNICAL-CLOSURE-001`.
