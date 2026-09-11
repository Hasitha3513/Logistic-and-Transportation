# US-38 Handle Fuel Exceptions Product Decisions

**Task:** `US-38-FUEL-EXCEPTIONS-PRODUCT-DECISIONS-001`  
**Decision:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`  
**Date:** 2026-09-05  
**Owner:** Fuel  
**Current Flyway head:** V65; implementation selects the next free forward version  
**Program accounting:** unchanged at 69 / 87 complete and 18 / 87 remaining  
**Next task:** `US-38-FUEL-EXCEPTIONS-IMPLEMENTATION-001`

## Source intent and ownership

The authoritative actor is the Fuel Manager. The source requires control of theft, incorrect readings, sudden price changes, emergency refueling, Fuel Card misuse, and negative balances so exceptional events do not corrupt inventory or cost records. It models those six cases as specializations of one Fuel exception capability, with investigation always included and correction, approval, blocking, reconciliation, and escalation used only when applicable.

Fuel owns the meaning, classification, evidence, local review, correction request and approval state, owner-command orchestration, immutable history, and Operations handoff record. Operations US-78 retains central assignment, severity confirmation, SLA, escalation levels, corrective-action tracking, RCA, resolution validation, closure, and reopen. US-38 does not create another generic incident, workflow, approval, Notification, Document, or Operations engine.

## Frozen taxonomy and language

The exact Fuel exception categories are:

- `SUSPECTED_FUEL_LOSS`: unexplained stock or consumption evidence requiring review; never a theft finding.
- `INCORRECT_READING`: an odometer, engine-hour, Bunker dip, or Fuel quantity reading suspected to be wrong.
- `SUDDEN_PRICE_CHANGE`: a reported price change requiring a new effective-dated Fuel Price and preservation of historical prices and transactions.
- `EMERGENCY_REFUEL`: an exceptional refuel identified by reason and operational attribution.
- `FUEL_CARD_POLICY_DEVIATION`: a US-35 review indicator requiring investigation; never a fraud finding.
- `NEGATIVE_BUNKER_BALANCE`: an attempted Bunker stock mutation that would make authoritative Tank stock negative and must be rejected rather than committed.

UI, API, audit, events, and notifications use `suspected`, `unexplained`, `policy deviation`, or `review required`. `THEFT_CONFIRMED`, `FRAUD_CONFIRMED`, `DRIVER_GUILTY`, criminal culpability, and automatic discipline are forbidden. The source phrase “theft” is represented by `SUSPECTED_FUEL_LOSS` until human review resolves the operational discrepancy; US-38 never decides criminal liability.

## Creation, deduplication, and source inputs

Cases may be opened manually by an authorized Fuel Manager from a verified same-Tenant source reference. Deterministic automatic creation is frozen only for a rejected `NEGATIVE_BUNKER_BALANCE` stock command because the accepted source explicitly requires a negative balance to generate an exception rather than remain valid. Other signals suggest a case but do not create one automatically.

The active-case dedupe key is `(tenantId, category, sourceType, sourceId)` for states other than `RESOLVED`. Signal-driven creation additionally uses a stable source-event identity. A later distinct episode may open only after the earlier case is resolved and must carry a new source-event identity.

- US-37 `EFFICIENCY_DEVIATION` and `POSSIBLE_LEAKAGE_INDICATOR` may be attached to a manually opened `SUSPECTED_FUEL_LOSS` case. They never automatically create a case or prove theft.
- All seven accepted US-35 review indicators may seed a manually opened `FUEL_CARD_POLICY_DEVIATION` case. US-38 reuses their result and never reimplements card detection. Provider facts remain immutable; provider correction is represented by a new provider reversal/correction fact.
- US-31 issue facts may support `INCORRECT_READING` or `SUSPECTED_FUEL_LOSS`, but US-38 does not redefine issue validity, limits, authorization, or lifecycle.
- US-32 purchase facts may support `SUDDEN_PRICE_CHANGE`, `EMERGENCY_REFUEL`, or evidence conflict review. The purchase remains the economic source of truth.
- US-33/Fleet remains authoritative for odometer and engine-hour validation and correction.
- US-34 Bunker remains authoritative for Tank stock, dip observations, `ledgerSequence`, and compensating stock adjustment.

No percentage, amount, frequency, “unusual use,” or price-variance threshold is invented. Until a separately approved policy supplies a threshold, suspected loss and sudden price change are explicitly reported/manual classifications grounded in recorded source evidence.

## Fuel-local lifecycle and outcomes

The exact aggregate lifecycle is `OPEN -> UNDER_REVIEW -> CORRECTION_PENDING -> AWAITING_APPROVAL -> RESOLVED`. `OPEN -> UNDER_REVIEW -> RESOLVED` is valid for no-action resolution. A rejected correction returns `AWAITING_APPROVAL -> UNDER_REVIEW`; a failed owner command returns `CORRECTION_PENDING` with a visible failure record and retryable action. There is no generic status patch, delete, cancellation, arbitrary category mutation, or Fuel-local reopen. An unresolved escalated case remains `UNDER_REVIEW`; Operations handoff is an orthogonal state, not another Fuel lifecycle.

Exact resolution outcomes are `NO_ACTION_REQUIRED`, `CORRECTION_APPLIED`, `RECONCILED`, `EMERGENCY_REFUEL_ACCEPTED`, and `REFERRED_TO_OPERATIONS`. Every resolution requires a reason, expected version, actor, time, and supporting evidence or correction/handoff reference as applicable.

Fuel-local impact is `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL` solely as an initial Operations severity candidate. Operations confirms central severity after handoff; Fuel impact never controls US-78 SLA directly.

## Emergency refuel

An emergency refuel is a manually recorded exception with a required bounded reason and same-Tenant Vehicle plus at least one same-Tenant Trip or Driver reference. It may reference an existing Fuel Issue or Fuel Purchase and a logical US-83 document, but US-38 does not invent or persist a receipt/document requirement absent from the source. Identification alone requires no approval. Any later command that creates or changes a purchase, issue, price, reconciliation, or stock fact follows the correction and independent-approval rules below. Retroactive recording is allowed only with the actual occurrence time, reason, actor, and immutable audit history; it never rewrites a historical transaction.

## Correction and approval

Raw accepted source history is never edited. US-38 stores a requested owner command and safe before-snapshot/hash, obtains approval when required, invokes only the owning module’s public command, and records the resulting reference or failure.

- Fuel Issue: use its accepted cancel/replacement or other explicitly available Fuel command; never update an issued historical row through US-38.
- Fuel Purchase and Fuel Price: use accepted lifecycle/reconciliation commands and a new effective-dated price; never retroactively replace transaction prices.
- Incorrect odometer/engine-hour reading: call the published Fleet reading correction/reset contract. No Fleet table access or distributed transaction is allowed.
- Bunker discrepancy: use the canonical Fuel Bunker adjustment command, producing a compensating movement with the next Tank `ledgerSequence`; never write Tank stock or movement history directly.
- Fuel Card: use accepted match/unmatch/reject/reversal-disposition commands or record a provider-correction request/outcome; never edit an imported provider transaction.

Independent approval is required before every correction that can change inventory, cost, effective price, reconciliation, or a source lifecycle fact. Pure classification, evidence, notes, review, `NO_ACTION_REQUIRED`, and Operations handoff require no correction approval. The correction requester must differ from the approver. The US-35 importer-versus-reconciler rule remains mandatory, and an importer cannot gain reconciliation authority through US-38. Approval automatically makes the correction eligible for owner-command execution; there is no generic approval engine and US-80 is not used.

## Operations handoff and P1-01

A Fuel case stays local while Fuel can investigate and correct it without central operational coordination. Handoff is mandatory for `CRITICAL` impact and permitted, with reason, for `HIGH` impact involving safety/compliance, cross-module operational coordination, repeated unresolved evidence, or a correction that remains unresolved after rejection/failure. LOW and MEDIUM cases are not handed off merely because they exist. `FUEL_EXCEPTION_ESCALATE` never mutates source data.

The handoff is `DURABLE_INTERNAL_REQUIRED`. Fuel publishes the existing Operations-owned `OperationalExceptionFactV1` through the shared P1-01 `DurableEventPublisher` in the same transaction that records the handoff. Implementation extends the contract allow-list with source module `FUEL`, the six exact source types above, summary code `FUEL_EXCEPTION_ESCALATED`, and safe metadata keys `fuelExceptionId`, `sourceType`, and `sourceId`. The event `sourceId` is the Fuel exception ID; its stable `eventId` is the immutable handoff ID. Category candidates map as follows: suspected loss and negative balance `OPERATIONAL`, incorrect reading `TECHNICAL`, sudden price and emergency refuel `FINANCIAL`, and card policy deviation `SECURITY`.

P1-01 provides atomic outbox persistence, at-least-once retry, and no global ordering or exactly-once claim. Operations deduplicates by `(tenantId, sourceEventId)`. Fuel stores `NOT_REQUIRED`, `PENDING`, `PUBLISHED`, `ACCEPTED`, or `FAILED` handoff status with attempt/result history. Retry reuses the same event ID. Operations may expose its case reference/status but cannot command Fuel data mutation. Failed handoff stays visible and retryable; a successful Fuel correction and failed handoff are both recorded, never hidden or rolled back across modules.

Notification is not called directly by Fuel exception domain code. US-78 emits its accepted escalation fact and Notification retains recipients, templates, channels, suppression, quiet hours, delivery retry, and history. Fuel may show in-app case state from its own query model; it creates no second Notification engine.

## Evidence, notes, audit, and privacy

Evidence is append-only and contains a logical source type/ID plus a minimal immutable safe snapshot needed to explain the case: quantity, unit price, currency, reading value/type, variance, indicator code, occurred time, and canonical source hash where applicable. It does not duplicate a foreign aggregate or raw provider body. Allowed logical references include Fuel Issue, Fuel Purchase, Fuel Card transaction/indicator, Fuel Performance indicator context, Bunker Tank/movement/dip, Vehicle, Driver, Trip, and US-83 document ID. US-83 owns binary content.

Notes are append-only, Tenant-owned, at most 2,000 characters, and retain actor/time. Evidence, notes, corrections, approvals/rejections, owner-command results, handoffs, classification, lifecycle, and resolution append immutable history. No delete endpoint or invented retention duration exists; retention is `RETENTION_POLICY_EXTERNAL_TO_US38`.

Driver/customer PII, PAN/CVV/PIN, opaque provider card reference, provider credentials, full GPS tracks, medical data, raw documents, and raw provider files are forbidden. Responses use logical IDs and display-safe labels only. Existing Audit read capability remains authoritative; no `FUEL_EXCEPTION_AUDIT_VIEW` is added.

## Tenant, RBAC, API, and errors

Tenant comes only from `CurrentTenant` or trusted worker/event context and is immutable on every case, evidence, correction, note, history, and handoff. Every referenced fact is resolved through its owner’s published same-Tenant contract; foreign-Tenant IDs return safe not-found/rejection. No payload accepts `tenantId`.

The exact new permissions are `FUEL_EXCEPTION_VIEW`, `FUEL_EXCEPTION_MANAGE`, `FUEL_EXCEPTION_CORRECT`, `FUEL_EXCEPTION_APPROVE`, and `FUEL_EXCEPTION_ESCALATE`. Frontend visibility never substitutes for backend enforcement.

The frozen API is:

- `GET /api/v1/fuel/exceptions` and `GET /api/v1/fuel/exceptions/{id}`;
- `POST /api/v1/fuel/exceptions` for explicit manual creation;
- `POST /api/v1/fuel/exceptions/{id}/review`;
- `GET|POST /api/v1/fuel/exceptions/{id}/evidence`;
- `POST /api/v1/fuel/exceptions/{id}/notes`;
- `POST /api/v1/fuel/exceptions/{id}/corrections`;
- `POST /api/v1/fuel/exceptions/{id}/corrections/{correctionId}/approve`;
- `POST /api/v1/fuel/exceptions/{id}/corrections/{correctionId}/reject`;
- `POST /api/v1/fuel/exceptions/{id}/resolve`;
- `POST /api/v1/fuel/exceptions/{id}/escalate`;
- `GET /api/v1/fuel/exceptions/{id}/history`.

No generic PATCH, category-edit, reopen, delete, raw-source edit, manual handoff-status mutation, payment, provider block, fraud verdict, or public/customer route exists. List pagination is default 20 and maximum 100. Allow-listed filters are category, lifecycle, source type, Vehicle, Driver, card, Tank, occurred-date range, review-required, and handoff status. Allow-listed sorts are created time, occurred time, updated time, category, lifecycle, and impact; default is created time descending.

Stable errors are `FUEL_EXCEPTION_NOT_FOUND`, `FUEL_EXCEPTION_CONFLICT`, `FUEL_EXCEPTION_INVALID_STATE`, `FUEL_EXCEPTION_SOURCE_NOT_FOUND`, `FUEL_EXCEPTION_CORRECTION_INVALID`, `FUEL_EXCEPTION_APPROVAL_REQUIRED`, `FUEL_EXCEPTION_SOD_VIOLATION`, and `FUEL_EXCEPTION_ALREADY_ESCALATED`. Invalid input is 400, unauthorized/forbidden is 401/403, safe not-found is 404, and stale version/dedupe/state races are 409 through the existing global error contract.

## Persistence, concurrency, and migration expectation

Implementation is expected to add Fuel-owned Tenant tables `fuel_exception_case`, `fuel_exception_evidence`, `fuel_exception_correction`, `fuel_exception_note`, `fuel_exception_history`, and `fuel_exception_operations_handoff`. Approval is immutable correction/history data rather than a separate generic approval table. Same-module relationships use Tenant-consistent composite foreign keys; Vehicle, Driver, Trip, Operations, Organization, and Document references remain logical UUIDs without physical cross-module foreign keys.

Mutable case/correction/handoff state uses optimistic versions. Append-only evidence, notes, approvals, correction results, and history are never updated or deleted. Unique active-source and handoff-event constraints prevent duplicate cases and Operations intake. Tenant-leading indexes cover category/lifecycle/created time, source identity, Vehicle/Driver/card/Tank filters, review state, history time, and handoff state. Queries are bounded and perform no arbitrary property traversal, unbounded scan, N+1 foreign lookup, or cross-module SQL.

The current head is V65. No migration version is reserved by this decision task; implementation must select the next free forward version and must not edit V1-V65.

## Frontend and acceptance contract

The UI lives under existing AppLayout and Fuel navigation with a list and case detail. It displays safe category/impact/lifecycle, evidence summary, append-only notes/history, correction request and permission-aware approval/rejection, resolution, and Operations handoff status. It does not duplicate application chrome or expose sensitive Driver/card/provider data. Labels remain non-punitive.

Implementation must add deterministic domain/application tests for taxonomy, lifecycle, dedupe, evidence, owner-command correction, approval/SoD, resolution, handoff retry/idempotency, privacy, optimistic-lock races, and Tenant isolation. PostgreSQL acceptance uses only `transport_logistics_acceptance` and proves clean Flyway, Tenant constraints, dedupe, append-only behavior, correction integrity, SoD, and durable handoff idempotency.

The real PostgreSQL-backed Chromium suite must cover all six categories: suspected-loss safe wording, incorrect-reading correction, effective-dated price preservation, identifiable emergency refuel, US-35 card-indicator review, rejected negative Bunker balance, correction approval/owner-result linkage, Operations handoff, history/evidence, Tenant B denial, and SoD denial. It must prove source immutability and preserve US-35 import/provider-fact/reconciliation/SoD, US-37 analytics semantics, Bunker canonical ledger ordering, and accepted US-78/P1-01 behavior.

## Scope containment

Payment/settlement, provider authorization or block, named provider integration, generic fraud or rules engine, opaque ML, punitive Driver action, automatic case creation except rejected negative Bunker balance, arbitrary thresholds, generic workflow/approval engine, duplicate Operations lifecycle, direct Notification/Documents implementation, binary storage, foreign persistence/SQL, distributed transaction, new outbox/inbox, raw-source rewrite, historical migration edit, and application implementation are all absent.

All actor, intent, taxonomy, safe-language, ownership, input, creation/dedupe, lifecycle, resolution, correction, approval/SoD, Operations/P1-01 handoff, Notification, evidence/privacy, Tenant/RBAC, API/error, persistence/concurrency, migration, frontend, test/E2E, regression, and scope-containment decisions are frozen. Scope leakage is **NONE**.
