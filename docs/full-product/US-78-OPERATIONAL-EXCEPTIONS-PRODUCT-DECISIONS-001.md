# US-78 Operational Exceptions — Frozen Product Decisions

**Task ID:** `US-78-OPERATIONAL-EXCEPTIONS-PRODUCT-DECISIONS-001`
**Date:** 2026-09-04
**Status:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`
**Story accounting:** 66 / 87 COMPLETE; 21 / 87 REMAINING (unchanged)
**Current Flyway head at decision time:** V61; no US-78 version is reserved

## 1. Authoritative source intent

The primary actor is the **Operations Manager**. The source goal is to classify, assign, prioritize, escalate, and close operational exceptions with SLA, corrective-action, and root-cause tracking so operational failures are resolved systematically.

The source requires exception classification, auto-assignment, severity, an escalation matrix, SLA tracking, investigation, corrective action, conditional RCA, resolution validation, closure, retained evidence/history, and reopening when a prior resolution proves ineffective or an issue recurs. The common write-story acceptance baseline also requires permission checks, mandatory-field validation, meaningful business errors, persistence, and auditability. US-78-specific acceptance requires classification and severity, an applied SLA/escalation workflow, and closure only after resolution/corrective action, with RCA where required.

The source does **not** authorize an enterprise incident platform, arbitrary operator-created incidents, a configurable taxonomy language, a second workflow/scheduler/notification/document engine, source-domain mutation, or copies of foreign evidence.

## 2. Owner and model

US-78 establishes a top-level `operations` bounded context. Its model is a **hybrid lifecycle aggregate with logical source references**:

- Operations owns the `OperationalExceptionCase`, its classification, confirmed severity, assignment, SLA clocks, escalation facts, corrective actions, RCA, resolution/closure validation, reopen behavior, and immutable case history.
- The originating domain owns detection, source state, business meaning, source evidence, and every corrective command that changes the originating aggregate.
- Operations stores only `sourceModule`, `sourceType`, `sourceId`, and source-event identity. It never imports or queries a foreign repository, JPA entity, table, or adapter and never creates a physical foreign key to a source table.
- US-68 remains the accepted read-only Delivery Last-Mile Planner. It does not create US-78 cases and is not replaced by the Operations queue.

Operations does not own Trip, Route, Fuel, Delivery, Tracking, Driver, Cargo, Integration, Notification, Workflow, or Scheduling semantics. A source correction is executed through the owning domain's authorized use case; Operations may record only the request/reference and safe result.

## 3. Detector ownership and rollout

Every domain retains its detector and publishes a minimized typed fact only after it has decided that a source exception exists.

| Source | Domain-owned fact | US-78 treatment |
| :--- | :--- | :--- |
| Trip | Persisted Trip `INCIDENT` operational event (current accepted implementation belongs to US-13; the roadmap's “US-15 Trip Exceptions” dependency label is stale) | Future typed intake; no Trip-row access |
| Routing | `RouteDisruptionCreatedEvent` (current accepted implementation belongs to US-22; US-23 owns revision history) | **Minimum acceptance source 1** |
| Freight | `CargoException` lifecycle | Future typed intake; no Freight-row access |
| Driver/Fleet | `DriverException` and Fleet exception facts | Future typed intake; medical/disciplinary detail excluded |
| Delivery | `DeliveryExceptionCase` creation under US-62 | **Minimum acceptance source 2** |
| US-68 | Read-only Last-Mile Planner projection | No intake publication |
| Fuel US-38 | Fuel-owned theft/reading/price/emergency/card/balance detection | Future publisher through the same intake contract |
| Tracking US-55 | Tracking-owned signal/tamper/spoof/battery/delay detection and trusted-state policy | Future publisher through the same intake contract |
| US-86 | Operations-owned real-world disruption facts and coordination | May open a case without owning replanning; no disaster/replanning implementation in US-78 |

US-78 implementation and acceptance integrate **Routing and Delivery** first. That proves one Operations lifecycle across two independently owned source domains without making unfinished Fuel or Tracking stories prerequisites.

## 4. Typed intake, durability, and idempotency

The public Operations-root intake contract is `OperationalExceptionFactV1`:

```text
eventId: UUID
tenantId: UUID
sourceModule: SourceModule
sourceType: String (registered allow-list, max 80)
sourceId: UUID
occurredAt: OffsetDateTime
severityCandidate: LOW | MEDIUM | HIGH | CRITICAL
categoryCandidate: OperationalExceptionCategory
summaryCode: String (registered, max 80)
safeMetadata: Map<String,String> (allow-listed keys only; <= 20 entries, key <= 64,
              value <= 256, canonical payload <= 4 KiB)
correlationId: String? (max 128)
```

The contract is mapped to the P1-01 canonical version-1 envelope. Tenant identity comes from the trusted envelope/current Tenant context, never a request body, header, source entity serialization, or arbitrary metadata entry. `sourceModule`, `sourceType`, `summaryCode`, and metadata keys are registered per producer; unknown values fail closed.

Accepted operational-exception intake is business-critical and durable. Routing and Delivery publish their approved facts atomically through the existing shared `DurableEventPublisher` and `integration_outbox_event`. Operations consumes at-least-once, with no global ordering or exactly-once claim. There is no `operations_outbox`, `operations_inbox`, Kafka, or RabbitMQ.

The case table is the consumer dedupe boundary. `UNIQUE (tenant_id, source_event_id)` means replay of one source publication creates one case. A distinct source event/occurrence creates a distinct case even when it references the same source aggregate. Correlation IDs are trace attributes only: v1 has no merge, parent/child, or many-to-many incident graph.

## 5. Case identity, classification, and severity

- Internal identity is UUID.
- The operator-facing reference is a non-sequential random identifier in the form `OEX-` plus 12 Crockford Base32 characters, unique within a Tenant. It conveys no volume or cross-Tenant sequence information.
- Initial classification is one of `OPERATIONAL`, `SAFETY`, `COMPLIANCE`, `CUSTOMER`, `FINANCIAL`, `TECHNICAL`, or `SECURITY`. This is a small Operations triage taxonomy, not a replacement for source-domain types.
- The producer proposes `categoryCandidate` and `severityCandidate`. A registered Operations mapping confirms them on intake. An authorized manager may reclassify or change severity only with a non-empty reason; the before/after values are appended to history.

Severity semantics are fixed:

| Severity | Meaning |
| :--- | :--- |
| `LOW` | Contained issue with no immediate safety, compliance, customer-service, or continuity threat. |
| `MEDIUM` | Material operational degradation requiring action within the current operating period. |
| `HIGH` | Serious impact or likely SLA, customer, safety, compliance, or continuity failure requiring prompt management. |
| `CRITICAL` | Immediate safety, legal, security, or material continuity risk; it escalates at intake. |

## 6. Lifecycle and transitions

Case states are exactly `OPEN`, `ACKNOWLEDGED`, `IN_PROGRESS`, `RESOLVED`, and `CLOSED`. Escalation is a level/flag and immutable history action, not a competing lifecycle state.

```text
OPEN -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED -> CLOSED
OPEN -> ACKNOWLEDGED -> IN_PROGRESS
RESOLVED -> IN_PROGRESS       (resolution validation rejected; reason required)
CLOSED -> IN_PROGRESS         (authorized reopen; recurrence/ineffective resolution reason required)
```

`OPEN -> CLOSED`, `OPEN -> RESOLVED`, and `ACKNOWLEDGED -> CLOSED` are forbidden. Reopen uses `OPERATIONAL_EXCEPTION_CLOSE`, requires a reason, increments the case version, restarts resolution SLA from the applicable policy, and preserves the prior closed history. Deletion and arbitrary status PATCH are not exposed.

## 7. SLA and scheduling

The server calculates all SLA instants. Browser timestamps are display-only. V1 uses continuous elapsed time (24x7); Tenant timezone controls display and date interpretation but holidays/business-hour pauses are not claimed. Future business-calendar behavior must use the US-81 Scheduling boundary rather than add an Operations calendar.

| Severity | Response due | Resolution due | At-risk threshold |
| :--- | :---: | :---: | :---: |
| `LOW` | 8 hours | 72 hours | 75% of resolution window |
| `MEDIUM` | 4 hours | 24 hours | 75% of resolution window |
| `HIGH` | 1 hour | 8 hours | 75% of resolution window |
| `CRITICAL` | 15 minutes | 2 hours | Immediate escalation plus 75% warning |

The case stores `responseDueAt`, `resolutionDueAt`, `nextEscalationAt`, response/resolution completion instants, and derived `ON_TRACK`, `AT_RISK`, `BREACHED`, or `MET` indicators. Acknowledgement stops the response clock; resolution stops the resolution clock. Closure validation time is not a third SLA.

US-81's accepted scheduling capability/pattern is reused for Tenant-aware bounded due-work execution. Implementation must use one bounded scanner adapter, `TenantJobExecutor`, server clock, 50-case Tenant batches, and idempotent escalation-level uniqueness. It must not create a second cron engine. The US-80 boundary remains the authority for reusable workflow concepts; US-78 v1 owns only its fixed aggregate transitions and must not introduce another configurable workflow engine.

## 8. Assignment and escalation

Assignment targets are `ROLE_QUEUE` or `USER`:

- Auto-assignment maps category/severity to a same-Tenant role queue. It does not silently choose a person or load-balance from private identity data.
- A named user must be active in the same Tenant and hold the required view/manage authority. Role codes are validated against active role templates; arbitrary strings and external/customer assignees are rejected.
- `OPERATIONAL_EXCEPTION_ASSIGN` permits assign, reassign, and reasoned unassign. An eligible operator with `OPERATIONAL_EXCEPTION_MANAGE` may self-assign from a role queue. A critical case cannot be left unassigned.
- Assignment history is immutable and records from/to target, actor, time, and reason.

Escalation triggers are: critical intake, SLA at-risk/breach, authorized manual escalation, and an explicitly published repeat occurrence. V1 has four monotonic levels (`L0` through `L3`) mapped to role queues, never hard-coded usernames. The same case/level cannot be emitted twice. Manual escalation requires `OPERATIONAL_EXCEPTION_ESCALATE`; severity cannot be manipulated through an escalation request.

Operations publishes one real-consumer event, `OPERATIONAL_EXCEPTION_ESCALATED_V1`, through P1-01 for Notification. It contains only case/source references, category, severity, escalation level, SLA status, occurred time, and correlation ID. US-77/Notification owns recipients, channels, templates, quiet hours, suppression, delivery attempts, provider retry, and delivery history. Operations sends no Email, SMS, push, or webhook itself.

## 9. Corrective action, RCA, resolution, and closure

A corrective action records UUID, `CORRECTIVE` or `PREVENTIVE` type, bounded description, `USER`/`ROLE_QUEUE` owner, server-controlled due/completion times, `OPEN`/`IN_PROGRESS`/`COMPLETED`/`CANCELLED` status, optional logical evidence/document reference, and version. Cancellation requires a reason. It never invokes a foreign mutation automatically.

RCA records a controlled cause category (`PEOPLE`, `PROCESS`, `EQUIPMENT`, `EXTERNAL`, `SYSTEM_DATA`, `ENVIRONMENT`, `UNKNOWN`), bounded root-cause code, summary, contributing factors, author, approval actor/time, and version. RCA is mandatory before closing `HIGH` or `CRITICAL` cases and optional for `LOW`/`MEDIUM` unless an explicit sensitivity policy requires it.

Resolution requires a resolution note and no incomplete required corrective action. It records the source correction/result reference if one exists but does not assert that a source aggregate changed. Closure additionally requires:

- the case is `RESOLVED`;
- every required corrective action is complete;
- required RCA exists and is approved;
- resolution validation has succeeded; and
- the closer holds `OPERATIONAL_EXCEPTION_CLOSE`.

For `HIGH`/`CRITICAL`, the closer must differ from the resolver, and an RCA approver must differ from the RCA author. Low/medium cases require no second-person approval. This is the complete v1 SoD rule; financial/security-specific SoD is deferred until a corresponding detector contract exists.

## 10. Audit, notes, evidence, privacy, and retention

Creation, classification, severity, acknowledgement, assignment, status, SLA policy/recalculation, escalation, corrective-action lifecycle, RCA lifecycle, resolution, closure, and reopen append immutable history. History records actor/system identity, server timestamp, action, before/after safe values, reason, correlation ID, and resulting version. Current state may change; history is never overwritten.

Operational notes are plain text, escaped on output, mandatory only where a command requires a reason, and limited to 2,000 characters. They are Tenant-scoped and audited. Operations stores minimized facts only and must not copy POD photos/signatures, medical certificates, full GPS tracks, financial documents, credentials, OTPs, addresses, contact destinations, provider payloads, or whole source incident logs.

Evidence is a logical source evidence reference or a US-83 Document UUID. US-83 owns upload, binary content, versioning, access, and retention. Operations performs authorization before returning any reference and never provides a file store. RCA and sensitive category details require `OPERATIONAL_EXCEPTION_RCA`; ordinary view does not automatically disclose them.

No authoritative duration is supplied. `RETENTION_POLICY_EXTERNAL_TO_US78` is frozen: US-78 exposes no delete/purge API and hard-codes no arbitrary 30-day or seven-year rule. Cases, history, actions, and RCA remain retained until a separately approved Tenant-qualified retention process preserves required audit/legal holds. P1-01 transport rows retain their own 30/90-day minimums; source evidence follows its owning domain/US-83 policy.

## 11. Tenant, RBAC, ABAC, and security errors

Every case, assignment-history row, corrective action, RCA, history row, query, idempotency key, event, worker claim, and cache key is Tenant-owned. Tenant A cannot view, search, export, assign, update, escalate, resolve, close, reopen, or infer a Tenant B case. Guessed/cross-Tenant IDs return the established safe not-found response.

Frozen permissions:

| Permission | Authority |
| :--- | :--- |
| `OPERATIONAL_EXCEPTION_VIEW` | View queue and non-sensitive detail. |
| `OPERATIONAL_EXCEPTION_MANAGE` | Acknowledge, start, classify, change severity with reason, manage notes/actions, and resolve. |
| `OPERATIONAL_EXCEPTION_ASSIGN` | Assign, reassign, or unassign. |
| `OPERATIONAL_EXCEPTION_ESCALATE` | Manual escalation. |
| `OPERATIONAL_EXCEPTION_RCA` | View, author, and approve RCA subject to SoD. |
| `OPERATIONAL_EXCEPTION_CLOSE` | Validate, close, reject resolution, or reopen. |
| `OPERATIONAL_EXCEPTION_AUDIT_VIEW` | View immutable full history. |

Contextual checks use Tenant, category/sensitivity, severity, assigned queue/user, state, and SoD actors. They are explicit predicates, not a generic ABAC policy engine. Request DTOs cannot set Tenant, source identity, case reference, lifecycle state, SLA status/due times, audit actors/times, escalation level, or approval identity.

Stable business errors include `OPERATIONAL_EXCEPTION_NOT_FOUND`, `OPERATIONAL_EXCEPTION_CONFLICT`, `OPERATIONAL_EXCEPTION_INVALID_TRANSITION`, `OPERATIONAL_EXCEPTION_ASSIGNMENT_INVALID`, `OPERATIONAL_EXCEPTION_SLA_INVALID`, `OPERATIONAL_EXCEPTION_RCA_REQUIRED`, `OPERATIONAL_EXCEPTION_RCA_APPROVAL_REQUIRED`, and `OPERATIONAL_EXCEPTION_CLOSE_NOT_ALLOWED`, wrapped in the standard error envelope.

## 12. Frozen REST and query contract

All routes are authenticated Operations-owned routes under `/api/v1/operational-exceptions`:

- `GET /api/v1/operational-exceptions`
- `GET /api/v1/operational-exceptions/{id}`
- `GET /api/v1/operational-exceptions/{id}/history`
- `POST /api/v1/operational-exceptions/{id}/classify`
- `POST /api/v1/operational-exceptions/{id}/acknowledge`
- `POST /api/v1/operational-exceptions/{id}/assign`
- `POST /api/v1/operational-exceptions/{id}/start`
- `POST /api/v1/operational-exceptions/{id}/escalate`
- `POST /api/v1/operational-exceptions/{id}/corrective-actions`
- `POST /api/v1/operational-exceptions/{id}/corrective-actions/{actionId}/start`
- `POST /api/v1/operational-exceptions/{id}/corrective-actions/{actionId}/complete`
- `POST /api/v1/operational-exceptions/{id}/rca`
- `POST /api/v1/operational-exceptions/{id}/rca/approve`
- `POST /api/v1/operational-exceptions/{id}/resolve`
- `POST /api/v1/operational-exceptions/{id}/close`
- `POST /api/v1/operational-exceptions/{id}/reject-resolution`
- `POST /api/v1/operational-exceptions/{id}/reopen`

There is no public/manual create, generic PATCH/status endpoint, delete, source-correction, arbitrary send, raw evidence, customer, or export endpoint in US-78.

List filters are `status`, `severity`, `category`, `sourceModule`, `assignedUserId`, `assignedRoleCode`, `slaStatus`, `openedFrom`, and `openedTo`. Search is limited to exact/prefix case reference, registered summary code, or exact source UUID; notes/RCA are never wildcard-searched. Pagination defaults to 20 and caps at 100. Safe sort keys are `openedAt`, `updatedAt`, `severity`, `responseDueAt`, `resolutionDueAt`, and `status`. History defaults to 50 and caps at 200. All mutation commands carry the expected case/action/RCA version as applicable; stale writes return HTTP 409.

## 13. Frontend scope

The feature lives at `frontend/src/features/operations/operationalExceptions` and exposes `/operations/exceptions` inside the existing operator `AppLayout`. The queue shows reference, safe source summary, category, severity/status badges, assignment, response/resolution due indicators, and `ON_TRACK`/`AT_RISK`/`BREACHED`/`MET` state. The detail page/drawer provides permitted lifecycle actions, corrective actions, gated RCA, source/evidence links, and a pageable immutable timeline.

The global layout owns shell, sidebar, header, breadcrumb, title, and spacing. US-78 may show queue count chips but is not the US-82 analytics dashboard. It has no customer-portal surface and exposes no scheduler internals.

## 14. Persistence and performance expectation

Implementation is expected to create Operations-owned normalized tables equivalent to:

- `operational_exception_case`
- `operational_exception_assignment_history`
- `operational_exception_corrective_action`
- `operational_exception_rca`
- `operational_exception_history`

Every table has `tenant_id UUID NOT NULL`, Tenant-consistent same-module foreign keys, server timestamps, and optimistic versioning where mutable. Source/domain/document/user identifiers are logical references with no cross-module physical FK. Required indexes lead with Tenant and cover source-event dedupe, case reference, status/open time, status/severity, assignment/status, response due, resolution due, escalation due, case history time, and action status/due time. Worker scans are Tenant-qualified and bounded to 50; list/history queries are pageable and cannot accept arbitrary database-field sorting.

US-78 likely requires a forward Flyway migration. The implementation must inspect the then-current head and must not assume or reserve V62. Historical migrations remain immutable.

## 15. Concurrency and failure handling

- Optimistic case/action/RCA versions make two assignees or conflicting lifecycle commands produce one winner and one 409.
- Duplicate intake is stopped by the database uniqueness key and returns the existing case idempotently.
- SLA work re-reads locked/current state; resolution/closure prevents a stale worker from adding a new escalation. A unique case/level fact prevents retry duplication.
- Close versus escalation, resolve versus action completion, and RCA approval versus close all revalidate state/preconditions in the winning transaction.
- Listener/scheduler failure leaves case state valid. At-least-once retry is idempotent and cannot create duplicate cases, history transitions, escalation levels, or Notification facts.

## 16. Implementation and acceptance gates

Architecture tests must prove that Operations has no foreign repository/entity/adapter/table access, domains do not import Operations persistence/adapters, only an Operations-root typed intake contract crosses the boundary, P1-01 is reused, Operations owns only its tables, and no workflow/scheduler/outbox/file/notification engine is cloned.

Security tests must cover Tenant A/B object/list/search/history/action denial, literal `/api/v1/...` route enforcement, all seven permissions, high/critical SoD, stale versions, mass assignment, source/Tenant spoofing, severity manipulation, assignment/escalation/closure/RCA privileges, note escaping/limits, and evidence/RCA privacy.

PostgreSQL acceptance must use only `transport_logistics_acceptance` and prove current Flyway head, constraints/indexes, idempotency, optimistic races, SLA-worker/manual-command races, append-only history, and Tenant-qualified worker batches. The development database is not evidence.

One real PostgreSQL-backed Chromium journey must:

1. create a real Routing disruption and a real Delivery exception through their owning APIs;
2. observe exactly one Operations case per typed fact through the same durable intake contract;
3. prove replay of the same source event creates no duplicate;
4. show both source domains in the Operations queue;
5. acknowledge, assign, start, create/complete corrective action, and escalate a high-severity case;
6. prove a safe durable escalation fact reaches Notification while Notification retains channel/template/delivery ownership;
7. record and independently approve RCA, resolve, independently close, and retain the complete timeline;
8. prove the source Route/Delivery records were not mutated by Operations; and
9. prove Tenant B cannot list, read, search, act on, or infer either case.

Acceptance requires both Routing and Delivery cases to traverse the common lifecycle and all real scenarios to pass; one source domain or a partial browser result cannot support a cross-domain claim.

## 17. Deferred and forbidden scope

Deferred: all other detector integrations; Fuel/GPS semantics; configurable business calendars; automatic person load-balancing; case merge/parent-child graphs; analytics dashboards; customer visibility; generic manual case creation; external/bidirectional APIs; automated retention purge; arbitrary attachments; and US-86 disruption replanning.

Forbidden: foreign table/repository access, physical foreign-domain FKs, entity serialization, source-domain mutation, duplicate evidence, a generic case/rules engine, a second workflow/scheduler/notification/document/outbox/inbox system, and any claim that unfinished US-38/55/86 behavior exists.

## 18. Decision gate and next task

The actor/goal, Operations ownership, hybrid aggregate model, detector boundary, typed durable intake, logical source reference, idempotency, taxonomy, severity, lifecycle, SLA, scheduling/workflow reuse, assignment, escalation, Notification boundary, corrective action, RCA, closure, append-only audit, Tenant/RBAC/SoD/privacy, Document boundary, API/UI, persistence/indexing/concurrency, two-domain minimum, future US-38/55/86 boundaries, and real acceptance journey are frozen. Scope leakage is **NONE**.

US-78 remains unimplemented and unaccepted. The next task is:

`US-78-OPERATIONAL-EXCEPTIONS-IMPLEMENTATION-001`
