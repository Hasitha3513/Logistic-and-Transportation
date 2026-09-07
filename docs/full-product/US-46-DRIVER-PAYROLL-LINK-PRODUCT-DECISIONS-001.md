# US-46 Process Driver Payroll Link Product Decisions

**Task:** `US-46-DRIVER-PAYROLL-LINK-PRODUCT-DECISIONS-001`  
**Decision:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`  
**Date:** 2026-09-07  
**Owner:** Driver within the existing Fleet top-level module  
**Current Flyway head:** V67; implementation selects the next free forward version  
**Program accounting:** unchanged at 70 / 87 complete and 17 / 87 remaining  
**Next task:** `US-46-DRIVER-PAYROLL-LINK-IMPLEMENTATION-001`

## Source intent and authoritative boundary

The authoritative actor is the Finance Officer. The source requires trip earnings, allowances, overtime, and deductions to be linked to Drivers so payroll-related settlement is accurate. Its workflow selects a Driver and payroll period, loads Completed Trips, validates Driver/Trip inputs, calculates the four payroll-input categories and a net payroll input, creates an export/entry, records source Trip references, and appends audit evidence.

The exact source-supported subfeatures are `Calculate Trip Earnings`, `Calculate Allowances`, `Calculate Overtime`, `Apply Payroll Deductions`, `Validate Driver/Trip Inputs`, and `Create Payroll Export/Entry`. The acceptance flow succeeds only when Driver/Trip inputs validate and an audited, source-traceable export/entry is created; invalid inputs produce a validation error. The mind map additionally groups trip-based earnings, allowance mapping, payment tracking, overtime linkage, and deduction inputs under the Driver performance/payroll-link area. “Payment tracking” is treated only as export/delivery trace because the same authoritative boundary assigns final salary processing to Payroll/HRMS.

The sources provide no named allowance/deduction subtype, rate, overtime threshold, cadence, jurisdiction, statutory rule, file schema, vendor, acknowledgement, rejection protocol, or live-system behavior. Those omissions are explicit constraints, not permission to invent payroll policy.

US-46 is the boundary `Transport Operations -> Payroll/HRMS`. Driver owns traceable operational payroll-input preparation, approval, immutable release snapshots, correction batches, and export requests. Trip owns Trip lifecycle and assignment facts. Scheduling remains the authority for any future accepted duty/roster facts. Integration owns external configuration, declarative mapping, exchange attempts, file delivery, health, and Integration audit. Payroll/HRMS owns employee master, pay calendars, salary runs, gross/net salary, tax, pension/statutory deductions, benefits, final settlement, payslips, bank/payment execution, and acknowledgement of payroll posting. Finance owns ledger, tax accounting, bank, payment, and financial posting outcomes.

No payroll engine, HRMS, employee master, time-and-attendance engine, bank/payment system, general ledger, tax engine, pension engine, statutory-compliance interpretation, or salary authority is created.

## Eligible source facts and period semantics

Only same-Tenant Trips in `COMPLETED` or `CLOSED` state, assigned to the selected Driver, with a non-null actual end time inside the payroll period and at or before the batch cutoff are eligible. The existing Trip `DriverAssignmentReportItem` is the minimum published source; implementation may extend that provider-neutral Trip projection only with source facts actually required by the frozen contract. Driver resolves through the published Fleet `DriverLookup`. There is no foreign repository, JPA entity, table, SQL, or physical foreign key access.

A payroll period is a caller-supplied Tenant-local calendar range `[periodStart, periodEndExclusive)` plus immutable `cutoffAt`. No weekly, fortnightly, monthly, country, union, or company cadence is invented. A Trip may contribute once to one non-superseded released batch for the same Driver and payroll-input category. Trips completed after cutoff or corrected after release enter a later correction batch; released history is never edited.

Cancelled, rejected, draft, approved, assigned, dispatched, and in-progress Trips are ineligible. Missing Driver assignment, missing actual end time, cross-Tenant references, duplicate source use, unsupported currency, missing external payroll worker reference, and incomplete monetary evidence fail validation rather than being estimated.

## Payroll-input categories and calculations

The exact line categories are `TRIP_EARNING`, `ALLOWANCE`, `OVERTIME`, and `DEDUCTION`. The source defines no allowance subtype, deduction formula, overtime threshold, rate table, minimum guarantee, bonus, tax rule, pension rule, or jurisdiction. US-46 must not invent them.

Phase 1 therefore uses explicit, authorized, batch-scoped calculation inputs. Each line records the Driver, one source Trip, category, reason code, description, quantity, unit, rate, signed amount, ISO-4217 currency, provenance, and immutable input snapshot. `TRIP_EARNING`, `ALLOWANCE`, and `OVERTIME` amounts are non-negative additions; `DEDUCTION` is stored as a non-negative amount and subtracted exactly once. A line amount equals `quantity * rate`, rounded to currency scale 2 with `HALF_UP`, or an explicitly supplied amount when the reason code declares `FIXED_AMOUNT`. A batch has exactly one currency and performs no FX conversion.

The provisional operational net input is `sum(TRIP_EARNING + ALLOWANCE + OVERTIME) - sum(DEDUCTION)`. It may be negative and is never labelled take-home pay, net salary, amount payable, or settlement. Payroll/HRMS independently validates and calculates final payroll. Rate schedules and statutory formulas remain external; US-46 snapshots only the authorized calculation facts used for traceability. Free-form lines without a bounded reason code and source Trip are forbidden.

Operational overtime is an explicit duration/quantity input tied to a Completed Trip. The repository has no accepted duty-threshold or statutory-overtime contract, so US-46 does not infer overtime from elapsed Trip time. If Scheduling later publishes an approved duty baseline, a new governed contract may supply the quantity without moving rate or statutory authority into Driver.

## Aggregate, lifecycle, approval, corrections, and idempotency

The local aggregate is `DriverPayrollInputBatch`, containing one or more Driver sections and immutable source-backed lines. Its lifecycle is:

`DRAFT -> VALIDATED -> APPROVED -> EXPORT_REQUESTED -> EXPORTED`

Validation failure leaves the batch `DRAFT` with deterministic errors. External failure leaves it `EXPORT_REQUESTED` with Integration-owned attempt status visible; it does not revert approval or create a new source batch. Retry is owned by the accepted Integration delivery policy. `SUPERSEDED` is permitted only after a later correction batch is exported and references the prior batch. There is no generic status patch, delete, reopen, mark-paid, mark-posted, or mark-acknowledged command.

Draft lines may be replaced and revalidated with optimistic version checks. After approval, the batch, lines, source snapshots, currency, cutoff, and totals are immutable. Corrections use a new `CORRECTION` batch whose lines reference the original batch/line and carry the compensating delta and reason. Payroll-input history is append-only.

The prepare actor must differ from the approve actor. Approval verifies validation hash, source eligibility, totals, external worker references, and export configuration. The export actor may equal the approver, but cannot alter approved content. Integration's financial/restricted activation segregation remains separately enforced.

Client commands require an `Idempotency-Key`. Create uniqueness is `(tenant_id, idempotency_key)`; released source-line uniqueness prevents the same `(tenant_id, driver_id, trip_id, category, original_line_id)` from contributing twice outside an explicit correction chain. Concurrent validate/approve/export commands use optimistic locking and deterministic `409` conflicts. Exactly-once external delivery is not claimed.

## External worker reference, US-73 export, and P1-01

Driver `employeeNumber` is an operational display identifier, not automatically an HRMS employee identity. Each exported Driver section requires a provider-neutral external payroll worker reference supplied through a Tenant-owned, approved mapping. The mapping stores only Driver UUID, external-system alias, opaque external worker reference, validity, version, and audit metadata; it does not import an employee master or HR profile. The reference is snapshotted at approval. No email, phone, medical, licence, drug-test, bank, tax, pension, national-ID, or salary data is exported.

Phase 1 acceptance uses only US-73's real `FILE_EXCHANGE / FILE_JSON_V1 / OUTBOUND` capability and a controlled filesystem destination. This decision approves one new business family, `DRIVER_PAYROLL_INPUT_V1`, classification `FINANCIAL_CONFIDENTIAL`, source aggregate `DRIVER_PAYROLL_INPUT_BATCH`, and durable consumer `integration-outbound-exchange`. No live HRMS vendor, API, webhook, inbound acknowledgement, bidirectional synchronization, SFTP, email, or manual file upload is approved.

Approval/release persists a canonical `DriverPayrollInputExportRequestedV1` envelope through the shared P1-01 `DurableEventPublisher` in the same Driver transaction. Integration must extend its explicit allow-list for this event, configuration classification, mapping schema, and size-safe payload; it creates no second outbox. Delivery is at-least-once and globally unordered. Driver reuses a stable event ID for the same released batch; Integration deduplicates with its accepted `(tenantId, configurationId, sourceEventId, mappingVersionId)` key.

The canonical payload contains envelope identity/version/time/Tenant, batch ID/type, period dates, cutoff, currency, provisional totals, and bounded Driver sections containing Driver UUID, external worker reference, and lines with source Trip UUID/number, category, reason code, quantity/unit/rate/amount, original-line reference, and source snapshot hash. It excludes names, contact data, raw notes, credentials, bank/tax/pension data, and unrestricted metadata. The full canonical schema and 32-KiB behavior must be registered during implementation before code is accepted.

The exact canonical UTF-8 JSON property contract is:

```json
{
  "schemaVersion": 1,
  "batchId": "uuid",
  "batchType": "REGULAR|CORRECTION",
  "periodStart": "YYYY-MM-DD",
  "periodEndExclusive": "YYYY-MM-DD",
  "cutoffAt": "RFC-3339 timestamp",
  "generatedAt": "RFC-3339 timestamp",
  "currency": "ISO-4217",
  "totals": {
    "tripEarnings": "decimal scale 2",
    "allowances": "decimal scale 2",
    "overtime": "decimal scale 2",
    "deductions": "decimal scale 2",
    "provisionalNetInput": "decimal scale 2"
  },
  "drivers": [{
    "driverId": "uuid",
    "externalWorkerReference": "opaque string",
    "lines": [{
      "lineId": "uuid",
      "tripId": "uuid",
      "tripNumber": "string",
      "category": "TRIP_EARNING|ALLOWANCE|OVERTIME|DEDUCTION",
      "reasonCode": "bounded string",
      "quantity": "decimal",
      "unit": "TRIP|HOUR|FIXED",
      "rate": "decimal scale 2",
      "amount": "decimal scale 2",
      "originalLineId": "uuid|null",
      "sourceSnapshotHash": "sha256 hex"
    }]
  }]
}
```

The P1-01 envelope supplies `eventId`, `tenantId`, event type/version, aggregate type/ID, occurred time, producer/consumer, correlation, and causation identity outside this business payload. `drivers` and `lines` have deterministic Driver UUID/line UUID ordering. The whole canonical payload remains subject to the accepted 32-KiB limit; oversize batches fail validation and must be split into new explicitly approved batches, never silently truncated.

`EXPORTED` means Integration has durably delivered the approved JSON file and recorded its hash; it does not mean imported, reconciled, accepted, posted, settled, or paid by Payroll/HRMS. Because Phase 1 has no inbound acknowledgement, those states and `INTEGRATION_RECONCILE` mutation remain unavailable. Operators use read-only Integration exchange/attempt evidence for delivery reconciliation. A correction is a separately approved compensating export, never a rewrite or overwrite.

## Tenant, security, privacy, and audit

Tenant identity comes only from authenticated `CurrentTenant` or trusted worker/event context. Every batch, line, mapping, correction link, history record, idempotency key, and durable event carries Tenant identity. All Driver, Trip, configuration, and original-line references are resolved within the same Tenant. Cross-Tenant IDs return safe not-found and no request accepts `tenantId`.

The exact new permissions are:

- `DRIVER_PAYROLL_VIEW`: view same-Tenant batches, safe lines, totals, history, and delivery status;
- `DRIVER_PAYROLL_PREPARE`: create and change drafts, maintain external worker mappings, and validate;
- `DRIVER_PAYROLL_APPROVE`: independently approve validated batches and correction batches;
- `DRIVER_PAYROLL_EXPORT`: request export of an approved batch.

No permission grants salary processing, payment, posting, tax/pension administration, employee-master management, source Trip mutation, Integration configuration activation, or cross-Tenant access. Backend authorization is authoritative; navigation/action hiding is supplementary.

Create/update/validate/approve/export/correction, source selection, mapping changes, validation hash, totals, failed commands, Integration event identity, delivery result/hash reference, actor, timestamp, correlation, and optimistic version are auditable. Audit/history stores no credentials or prohibited Driver personal data. Retention is `RETENTION_POLICY_EXTERNAL_TO_US46`; no deletion endpoint is approved.

## Frozen API and stable errors

The Driver-owned API is:

- `GET|POST /api/v1/drivers/payroll-input-batches`;
- `GET /api/v1/drivers/payroll-input-batches/{batchId}`;
- `PUT /api/v1/drivers/payroll-input-batches/{batchId}/lines` for complete draft-line replacement;
- `POST /api/v1/drivers/payroll-input-batches/{batchId}/validate`;
- `POST /api/v1/drivers/payroll-input-batches/{batchId}/approve`;
- `POST /api/v1/drivers/payroll-input-batches/{batchId}/export`;
- `POST /api/v1/drivers/payroll-input-batches/{batchId}/corrections`;
- `GET /api/v1/drivers/payroll-input-batches/{batchId}/history`;
- `GET|PUT /api/v1/drivers/{driverId}/payroll-worker-mapping`.

There is no generic PATCH/status mutation, delete, payment, salary-run, payslip, employee, bank, tax, pension, raw-payload, manual-retry, acknowledgement, or public route. Batch list pagination defaults to 20 and is capped at 100. Allow-listed filters are period overlap, Driver, lifecycle, batch type, currency, and export state. Allow-listed sorts are created time, period start, updated time, lifecycle, and total; default is created time descending.

Stable business errors are `DRIVER_PAYROLL_BATCH_NOT_FOUND`, `DRIVER_PAYROLL_INVALID_STATE`, `DRIVER_PAYROLL_SOURCE_NOT_ELIGIBLE`, `DRIVER_PAYROLL_SOURCE_DUPLICATE`, `DRIVER_PAYROLL_WORKER_MAPPING_REQUIRED`, `DRIVER_PAYROLL_CURRENCY_MISMATCH`, `DRIVER_PAYROLL_VALIDATION_FAILED`, `DRIVER_PAYROLL_SOD_VIOLATION`, `DRIVER_PAYROLL_STALE_VERSION`, `DRIVER_PAYROLL_ALREADY_RELEASED`, and `DRIVER_PAYROLL_EXPORT_CONFIGURATION_INVALID`. Invalid input is 400, unauthenticated/forbidden is 401/403, safe not-found is 404, and stale/dedupe/state conflicts are 409 through the existing global error contract.

## Persistence, architecture, frontend, and acceptance expectation

Implementation is expected to add Driver-owned Tenant tables `driver_payroll_input_batch`, `driver_payroll_input_line`, `driver_payroll_worker_mapping`, and `driver_payroll_input_history`. Export attempts and external configuration remain Integration-owned; Driver stores only stable event/configuration/exchange references and latest safe delivery projection. Same-module relationships use Tenant-consistent composite foreign keys. Trip and Integration references are logical UUIDs with no physical cross-module foreign key.

Every table has Tenant-leading indexes and Tenant-scoped uniqueness. Batch and mapping state use optimistic versions. Released lines/history are append-only. Queries are bounded, Tenant-qualified, and contain no cross-module SQL, arbitrary property traversal, or N+1 foreign lookup. No migration version is reserved by this decision task and V1-V67 remain immutable.

Backend code remains inside the existing Fleet top-level module under a cohesive Driver payroll-link feature, following domain-first ports and adapters. Trip and Integration access occurs only through published contracts/events. No new top-level module, generic finance package, cross-module service/repository injection, or distributed transaction is approved.

The frontend lives under existing AppLayout and Driver navigation with a payroll-input batch list, create/edit/validate flow, detail/history, mapping state, permission-aware approval/export/correction actions, and safe Integration delivery status. It never exposes raw payloads, credentials, prohibited personal data, or application chrome duplication.

Implementation verification must cover calculation determinism, Completed/Closed eligibility, cutoff/late facts, all four categories, negative provisional net input, currency/rounding, mapping version snapshots, lifecycle, immutability, correction deltas, SoD, idempotency, optimistic races, Tenant/IDOR denial, privacy, canonical event/payload, outbox atomicity, Integration dedupe/retry/file hash, and no acknowledgement/payment claim. PostgreSQL acceptance must use only `transport_logistics_acceptance`, prove clean Flyway and Tenant constraints, and leave source Trips unchanged.

The real PostgreSQL-backed Chromium suite must prove a successful four-category batch and file export, deterministic totals and source traceability, ineligible Trip rejection, mapping-required validation, prepare/approve SoD denial then independent approval, duplicate/concurrent export idempotency, a compensating correction export, Tenant B denial, safe delivery evidence, and exact JSON file contents without prohibited data. It must preserve Driver/Trip, US-73, P1-01, Tenant/RBAC, and existing accepted story regressions.

## Related-feature boundaries, rollback, and risk disposition

Driver violations, medical/drug-test results, Driver exceptions, US-38 Fuel exceptions, US-35 Fuel Card indicators, and performance indicators never create deductions automatically. A deduction exists only as an explicitly prepared, source-referenced line in a batch and receives the same independent batch approval; Payroll/HRMS remains free to reject or transform it under its own authority. No punitive financial automation is approved. US-47 customer billing is separate and contributes no Driver payroll line.

US-46 owns operational input trace only; general financial and settlement reporting remains outside it. No payroll Notification engine or new operator alert is approved. If a later requirement needs notifications, it must reuse the Notification boundary under a separately registered safe event.

Rollback before approval is draft replacement and revalidation. Rollback after approval/export is a new compensating correction batch; source/released history is never deleted or rewritten. External retry reuses the same stable event identity and follows Integration's accepted policy. Database change rollback is forward-only.

Architecture risks are closed as follows: payroll, HRMS, Finance, and Billing scope leakage are excluded by ownership; foreign repository/SQL and physical foreign FKs are forbidden; payload minimization and dedicated view/export permissions constrain Driver PII and financial data; `BigDecimal`, one currency, scale 2, and `HALF_UP` constrain money precision; stable release identity and Integration dedupe constrain double export; approval immutability plus correction batches prevent silent mutation; explicit approved deductions prevent punitive automation; server Tenant authority and Tenant-leading keys constrain Tenant leakage; Integration alone resolves credentials; and the fixed lifecycle/SoD avoids a generic workflow or ABAC engine.

## Scope containment

Final salary, gross/net pay, statutory or voluntary deduction policy, taxes, pensions, benefits, payslips, payment, bank details, general-ledger posting, employee master, attendance, roster invention, jurisdictional rules, named HRMS/vendor, live API, webhook, inbound acknowledgement, SFTP, email delivery, manual retry, reconciliation mutation, arbitrary scripts, unrestricted mapping, new broker/outbox/inbox, exactly-once/global ordering, foreign persistence, source Trip mutation, historical migration edits, application implementation, and story-accounting advancement are absent.

All source-intent, ownership, eligibility, period/cutoff, category/calculation, lifecycle, approval/SoD, correction, idempotency, external mapping, US-73/P1-01, payload, acknowledgement, Tenant/RBAC/privacy/audit, API/error, persistence/concurrency, architecture, frontend, test/E2E, and scope-containment decisions are frozen. Scope leakage is **NONE**.
