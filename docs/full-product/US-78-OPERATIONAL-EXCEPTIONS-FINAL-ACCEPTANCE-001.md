# US-78 Manage Operational Exceptions — Final Acceptance

**Task:** `US-78-OPERATIONAL-EXCEPTIONS-FINAL-ACCEPTANCE-001`  
**Decision:** `PASS / COMPLETE`  
**Date:** 2026-09-04  
**Migration:** V62; current Flyway head V62  
**Program accounting:** 67 / 87 COMPLETE; 20 / 87 remaining  
**Wave A:** 2 / 2 COMPLETE / CLOSED  
**Next task:** `US-37-FUEL-PERFORMANCE-PRODUCT-DECISIONS-001`

## Independent acceptance decision

US-78 satisfies the frozen source intent. An authorized Operations Manager can classify and prioritize cases, confirm severity, assign work, track response and resolution SLA, escalate, investigate, manage corrective actions, record and independently approve required RCA, resolve, validate, close, reject a resolution, and reopen an ineffective or recurring case. No acceptance blocker remains.

The accepted owner is the top-level `operations` bounded context using a hybrid lifecycle aggregate with logical source references. Operations owns only triage and case lifecycle. Routing and Delivery retain detection, source meaning, evidence, state, and corrective mutation. Source inspection and architecture tests found no foreign repository/entity/table access or physical cross-module foreign key.

## Cross-domain intake and durability

Routing US-22 and Delivery US-62 both publish the same bounded `OperationalExceptionFactV1` contract through the shared P1-01 `DurableEventPublisher` and `integration_outbox_event`. The contract contains only event/source identity, trusted Tenant, occurrence time, severity/category candidates, registered summary code, bounded safe metadata, and optional correlation ID. Source types, summary codes, and metadata keys are allow-listed and unknown values fail closed; metadata is limited to 20 entries, 64-character keys, 256-character values, and a 4 KiB canonical payload.

Publication is atomic with the source transaction and provides at-least-once delivery with no exactly-once or global-ordering claim. `UNIQUE (tenant_id, source_event_id)` is the consumer dedupe boundary. There is no Operations-specific outbox/inbox, broker, second scheduler, second workflow engine, or second Notification engine.

## Lifecycle, authorization, and safety

- Classification is exactly `OPERATIONAL`, `SAFETY`, `COMPLIANCE`, `CUSTOMER`, `FINANCIAL`, `TECHNICAL`, or `SECURITY`; severity is exactly `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`.
- Lifecycle is exactly `OPEN`, `ACKNOWLEDGED`, `IN_PROGRESS`, `RESOLVED`, and `CLOSED`. Escalation remains separate and monotonic `L0..L3`; forbidden shortcuts are rejected.
- Reopen is the reasoned, versioned `CLOSED -> IN_PROGRESS` command and restarts resolution SLA while preserving history.
- Server-side 24x7 SLA is LOW 8h/72h, MEDIUM 4h/24h, HIGH 1h/8h, and CRITICAL 15m/2h; at-risk begins at 75%, and critical intake escalates immediately.
- The SLA scanner reuses `TenantJobExecutor`, server time, Tenant-qualified due queries, and a maximum of 50 cases per Tenant.
- Assignment supports only validated same-Tenant eligible `USER` or `ROLE_QUEUE` targets. Critical cases cannot remain unassigned.
- Notification consumes the minimized `OPERATIONAL_EXCEPTION_ESCALATED_V1` fact and retains ownership of recipients, channels, templates, quiet hours, suppression, attempts, provider retry, and delivery history.
- Corrective actions, RCA, resolution, closure, rejection, and reopen preserve optimistic versions and append-only history. High/critical RCA requires an approver different from its author, and the closer must differ from the resolver.

All routes are under `/api/v1/operational-exceptions` and use the seven frozen permissions: `OPERATIONAL_EXCEPTION_VIEW`, `OPERATIONAL_EXCEPTION_MANAGE`, `OPERATIONAL_EXCEPTION_ASSIGN`, `OPERATIONAL_EXCEPTION_ESCALATE`, `OPERATIONAL_EXCEPTION_RCA`, `OPERATIONAL_EXCEPTION_CLOSE`, and `OPERATIONAL_EXCEPTION_AUDIT_VIEW`. Tenant A cannot infer or mutate Tenant B cases. There is no manual-create, delete, generic status patch, source-correction, raw-evidence, export, public, or customer route.

Operations stores bounded plain text and logical evidence/document references only. It does not retain POD media/signatures, medical records, GPS tracks, financial documents, credentials, OTPs, addresses/contact destinations, provider bodies, or whole foreign exceptions. US-83 retains document ownership, and case retention remains `RETENTION_POLICY_EXTERNAL_TO_US78` with no delete/purge API or invented duration.

## Persistence and performance

Forward migration `V62__operational_exceptions_us78.sql` owns exactly five Tenant-scoped tables: case, assignment history, corrective action, RCA, and immutable history. V1-V61 remain unchanged. Tenant-consistent same-module foreign keys, enum/state/version checks, case-reference and source-event uniqueness, and Tenant-leading indexes cover case/source lookup, queue filters, due scans, assignment, history pagination, and corrective-action due work. Queue/history pagination and sort/search fields are bounded; Operations performs no foreign-table scan.

## Fresh acceptance evidence

- Focused Operations/Routing/Delivery/durable-publication group: 41 tests, 0 failures, 0 errors, 0 skipped.
- Deterministic concurrency: 6 tests, 0 failures, 0 errors, 0 skipped.
- PostgreSQL acceptance: 3 tests, 0 failures, 0 errors, 0 skipped; clean V1→V62 against only `transport_logistics_acceptance`.
- Cross-story regression: 84 tests, 0 failures, 0 errors, 0 skipped, covering Routing, Delivery/US-68, Notification/US-77, P1-01, US-73, Identity/RBAC, and Tenant scheduling.
- Complete Maven verification: 1,296 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 05:06 under Java 21.
- Architecture and Spring Modulith: 46 tests, 0 failures, 0 errors, 0 skipped.
- Checkstyle: 0 violations; PMD: PASS; SpotBugs: 0 findings/errors.
- Frontend: TypeScript PASS; Vitest 61 files / 261 tests PASS; production build PASS; changed-file ESLint PASS.
- Repository-wide ESLint retains 71 pre-existing errors in eight unchanged Delivery files; US-78 introduced lint errors are zero.
- Fresh real PostgreSQL-backed Chromium: 6/6 PASS in 19.3 seconds with real Routing and Delivery producers, replay dedupe, common lifecycle, safe Notification processing, independent RCA approval/closure, immutable source state, RBAC, and Tenant non-inference.
- `git diff --check`: PASS.

One discarded regression invocation inherited `jdbc:postgresql://localhost:5432/transport_logistics` for a security test and ran Flyway validation/repair. It did not clean the schema or mutate business rows. That result is excluded. The accepted regression rerun and every authoritative PostgreSQL/Maven/Chromium result explicitly used `jdbc:postgresql://localhost:5433/transport_logistics_acceptance`.

## Scope containment

US-78 adds no Fuel US-38 or Tracking US-55 detector, US-86 replanning, arbitrary incident framework, analytics dashboard, customer exception UI, foreign repository/table access, physical foreign FK, or duplicate outbox/inbox/scheduler/workflow/Notification/Document capability. US-68 remains an accepted read-only Planner.

## Final disposition

`US-78 = COMPLETE`. US-73 and US-78 make Wave A `2 / 2 COMPLETE / CLOSED`. Program accounting advances exactly once to 67 / 87 complete and 20 / 87 remaining (`67 + 20 = 87`).
