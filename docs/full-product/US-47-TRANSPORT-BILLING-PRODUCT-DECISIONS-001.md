# US-47 Manage Transport Billing Product Decisions

**Task:** `US-47-TRANSPORT-BILLING-PRODUCT-DECISIONS-001`  
**Decision:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`  
**Date:** 2026-09-07  
**Owner:** dedicated top-level `billing` bounded context  
**Current Flyway head:** V71; implementation must select the next free forward version  
**Program accounting:** unchanged at 71 / 87 complete and 16 / 87 remaining  
**Next task:** `US-47-TRANSPORT-BILLING-IMPLEMENTATION-001`

## Exact source-supported intent

The authoritative actor is the **Billing Officer**. The canonical goal is: calculate trip and freight billing with surcharges, penalties, and cost-center allocation so transport activities are financially accounted for.

The exact source-supported use cases are `Calculate Trip Cost`, `Generate Freight Billing`, `Apply Surcharges`, `Apply Penalties`, `Allocate Cost Center`, `Validate Billing Data`, and `Finalize Billing Record`. The activity starts from a **Completed Trip / Freight Activity**, loads cost and commercial data, calculates the base cost, applies applicable surcharges and penalties, allocates a cost centre, validates, calculates the final amount, finalizes, and records audit information. Invalid data produces a validation error.

The three canonical acceptance criteria are:

1. Trip/freight billing calculates applicable cost and surcharge.
2. Penalties and cost-center allocation are separately visible.
3. Final billing changes are audited.

The mind map additionally names invoice reconciliation, tax handling, and tax-and-billing compliance. The source model names a `BillingRecord` with `billingId`, `billingType`, `baseAmount`, `surcharge`, `penalty`, `totalAmount`, and `calculateTotal()`, related logically to Trip Orders and Freight Orders. It supplies no charge rate, surcharge or penalty category, tax jurisdiction or formula, invoice-number authority, customer billing cadence, discount rule, FX rule, accounting acknowledgement, payment behavior, retention term, or named ERP. Those omissions constrain Phase 1; they do not authorize invention.

The source UML's repository participants are conceptual data sources, not permission for cross-module repository access. Implementation must replace them with published provider-neutral source-fact contracts.

## ARB bounded-context and ownership decision

ARB ratifies a dedicated top-level `billing` bounded context. The reason is its independent monetary aggregate and lifecycle: source claim, calculation, validation, approval, finalization, immutable history, reversal, reconciliation evidence, and optional Finance handoff. Billing is not placed in Trip, Freight, Delivery, Reporting, Integration, Organization, or the proposed Finance blueprint.

Billing owns transport billable snapshots, charge composition, authorized adjustments, cost-centre allocation, validation, approval, operational billing finalization, reversals/corrections, append-only history, and the business meaning of a controlled external export request.

Trip owns Trip execution and lifecycle. Freight owns Freight Order/cargo facts and completion authority. Delivery owns delivery/POD completion facts but is not a direct Phase 1 billable source. Fuel owns fuel costs and operational fuel facts; no Fuel repository or automatic fuel-surcharge calculation is approved. Organization owns Customer identity/master data. Integration owns configuration, mapping, credentials, exchange attempts, technical retry, delivery evidence, and health. US-72 Compliance will consume minimized finalized tax/compliance facts and owns compliance decisions. Finance/external accounting owns tax invoices, general ledger, accounts receivable, official posting, fiscal periods, payment, banking, settlement, tax remittance, cash application, credit control, collections, and customer balances.

No GL, AR/AP, payment gateway, banking, cash receipt, bank reconciliation, tax engine, chart of accounts, credit/collections, customer-account balance, or duplicated Customer master is approved.

## Billable sources and eligibility

Phase 1 source types are exactly `TRIP` and `FREIGHT_ORDER`. A billable source fact is a provider-owned immutable projection, not an event copied from a foreign table. It includes Tenant, source type/ID/business number, terminal lifecycle, completion time, Customer logical ID, authorized commercial basis, quantities/units needed by the chosen line, source currency where present, source version, and a canonical SHA-256 snapshot hash.

- A Trip is eligible only in `CLOSED`. `COMPLETED` remains operationally mutable because the accepted Trip lifecycle permits a later close; selecting `CLOSED` prevents premature billing.
- A Freight Order is eligible only after Freight publishes an explicit `COMPLETED` or `CLOSED` billable fact. The current Freight Order implementation has no accepted terminal lifecycle, so implementation must add/freeze that owner-side completion contract before Freight can be billed. Billing may not infer completion from creation, load readiness, Trip state, Delivery state, dates, or direct database reads.
- Delivery `DELIVERED`, Fuel costs, operational exceptions, Driver payroll inputs, and Customer records do not independently create a Phase 1 bill. A later approved source contract may add a type without changing ownership.

All sources must resolve inside the current Tenant. Cancelled, rejected, draft, active, dispatched, merely completed Trip, non-terminal Freight, missing Customer, inactive Customer, incomplete commercial basis, stale source snapshot, and already-claimed source facts fail closed. Billing never mutates a source record.

## Aggregate, commercial model, and deterministic money rules

The aggregate is `TransportBillingRecord`, matching the source term without claiming tax-invoice ownership. A regular record contains one or more source claims for one Customer and one currency, charge lines, cost-centre allocations, validation snapshot/hash, approval/finalization facts, reversal links, export identity/projection, optimistic version, and append-only history.

Phase 1 is source-per-record by default: one `TRIP` or one `FREIGHT_ORDER` per regular record. Customer-period batching, mixed Trip/Freight consolidation, monthly cadence, contract grouping, and multi-source invoice runs are not source-defined and are deferred. `billingPeriodStart` and `billingPeriodEndExclusive` are optional trace fields copied from the source activity; they do not create a Finance fiscal period.

Commercial rates are explicit authorized inputs captured on each draft line with a bounded `reasonCode`, quantity, unit, unit rate, provenance, and source snapshot. US-47 owns their use and immutable snapshot, not a universal pricing/rate-card/contract engine. No arbitrary free-form amount without provenance is accepted.

Line categories are exactly:

- `BASE_CHARGE`: one required positive line representing the Trip cost or Freight billing basis;
- `SURCHARGE`: zero or more non-negative additions;
- `PENALTY`: zero or more non-negative customer-facing debit adjustments, shown separately;
- `CREDIT_ADJUSTMENT`: zero or more non-negative deductions used only with an approved bounded reason.

The source does not define surcharge/penalty subtypes, so implementation provides no universal catalogue such as fuel, toll, waiting, remote-area, or driver penalties. Bounded Tenant configuration or explicit inputs may use reason codes approved by the implementation contract; none may automatically arise from US-35/37/38 or US-46. Discounts are not source-supported as a separate Phase 1 feature; an approved commercial reduction uses `CREDIT_ADJUSTMENT`, independent approval, and audit.

Each record uses one ISO-4217 currency. The initial currency must equal the trusted Tenant default and every source/line must match; FX and mixed currency are forbidden. All money uses `BigDecimal`, database `NUMERIC(19,2)`, arithmetic precision 19, scale 2, and `RoundingMode.HALF_UP`. Quantity/rate may retain `NUMERIC(19,4)` during multiplication; each line is rounded once to scale 2 and totals sum rounded lines.

The deterministic Phase 1 formula is:

`subtotal = BASE_CHARGE + SURCHARGE + PENALTY - CREDIT_ADJUSTMENT`

`taxAmount = supplied external tax fact or 0.00`

`totalAmount = subtotal + taxAmount`

Negative totals are invalid. Tax is never inferred. A correction/reversal may carry signed compensating effects through its record type while retaining non-negative component fields.

## Tax, cost centre, and compliance boundary

US-47 does not calculate jurisdictional tax. It stores an externally supplied, independently authorized tax fact consisting of tax category, jurisdiction reference, taxable amount, tax rate where supplied, tax amount, exemption reference where applicable, provenance, and snapshot hash. Missing tax data is represented as `NOT_SUPPLIED`, never as a claim that the transaction is exempt or zero-rated. The tax amount must reconcile mathematically with the supplied fact before finalization; Billing does not decide whether a rate is legally correct.

At finalization Billing publishes the minimized facts needed by US-72: Billing record ID/number, Customer logical ID, source type/ID, currency, finalized time, subtotal, tax category, jurisdiction reference, taxable amount, supplied tax rate/amount, exemption reference, total, and immutable snapshot hash. US-72 evaluates tax-and-billing compliance; Billing neither embeds a generic compliance engine nor claims statutory compliance before that decision exists. Phase 1 validation may finalize only when the applicable US-72 decision contract is available or the record explicitly carries `COMPLIANCE_NOT_REQUIRED` from an authorized policy; it may not self-declare compliance.

A cost centre is an operational allocation reference, not a ledger account or chart-of-accounts node. It is a required bounded code plus description/source on every regular record, with allocations summing exactly 100.0000% of subtotal before tax. Until an external cost-centre owner is accepted, the code is an authorized Billing input snapshotted and audited. It is immutable after finalization and is exported only as a reference.

## Lifecycle, approval, finalization, reconciliation, and reversal

The exact local lifecycle is:

`DRAFT -> VALIDATED -> APPROVED -> FINALIZED -> EXPORT_REQUESTED -> EXPORTED`

Validation failure leaves the record `DRAFT`. Editing a validated draft returns it to `DRAFT`. Approval is mandatory, and the preparer must differ from the approver. Finalization may be performed by the approver or another actor with finalization authority but rechecks the unchanged validation hash and optimistic version. No generic workflow engine dependency is introduced.

`FINALIZED` means the commercial calculation, Customer/source references, source snapshot, currency, lines, tax facts, allocations, totals, approval, and operational billing number are locked and ready for Finance handoff. It does **not** mean tax invoice issued, posted, booked, reconciled, settled, or paid. Billing owns operational bill finalization only; formal tax invoice issuance remains Finance/external accounting authority.

Draft records may be reasoned-cancelled before approval as `CANCELLED`; cancelled records cannot be revived or exported. There is no delete. Approved records cannot be cancelled or edited. After finalization correction uses a new `REVERSAL` record referencing exactly one finalized original, copying its immutable facts and applying the exact negative commercial effect. A reversed original becomes `REVERSED` only when the reversal is finalized. A corrected replacement, if needed, is a new `REGULAR` record linked to the reversal chain. One effective reversal per original is allowed; history remains append-only.

Invoice reconciliation is Phase 1 **delivery evidence only**: Billing displays the Integration exchange ID, state, attempt summary, delivered file hash, and delivered time. No inbound accounting acknowledgement exists, so no `POSTED`, `BOOKED`, `ACCEPTED_BY_FINANCE`, `SETTLED`, `PAID`, or accounting-reconciled state is approved.

## Duplicate prevention, idempotency, and concurrency

An active/finalized regular source claim is unique by `(tenant_id, source_type, source_id)`. A source is released for rebilling only through a finalized reversal chain; the replacement explicitly references the reversed record. Charge category is not part of source uniqueness because splitting lines must not permit a second bill.

`Idempotency-Key` is mandatory for create, cancel, approve, finalize, reversal creation/finalization, and export request. Keys are Tenant- and command-scoped; replay returns the original result and a conflicting payload returns `409`. Mutable records use optimistic versioning. Deterministic races cover duplicate source claims, duplicate keys, edit versus approve, double approve, double finalize, finalize versus cancel/reversal, double reversal, and double export. Exactly-once external delivery is not claimed.

## External accounting, US-73, P1-01, and event contracts

Phase 1 external mode is `CONTROLLED_FILE_EXCHANGE`. Acceptance uses the already accepted US-73 `FILE_EXCHANGE / FILE_JSON_V1 / OUTBOUND` controlled filesystem adapter. No named ERP/accounting vendor, network endpoint, inbound acknowledgement, bidirectional synchronization, webhook, payment provider, or live posting is approved.

Billing owns the business family `TRANSPORT_BILLING_V1`, event `TransportBillingExportRequestedV1`, aggregate type `TRANSPORT_BILLING_RECORD`, classification `FINANCIAL_CONFIDENTIAL`, and stable export identity. The approved handoff is `DURABLE_INTERNAL_REQUIRED`: finalization/export request persists the canonical event through the shared P1-01 `DurableEventPublisher`; Integration consumes it as `integration-outbound-exchange`. No second outbox or distributed transaction is permitted. Delivery is at-least-once and unordered; retries reuse the stable event identity.

The exact canonical business payload is:

```json
{
  "schemaVersion": 1,
  "billingRecordId": "uuid",
  "billingNumber": "TB-YYYY-NNNNNN",
  "recordType": "REGULAR|REVERSAL",
  "customerId": "uuid",
  "currency": "ISO-4217",
  "source": {
    "type": "TRIP|FREIGHT_ORDER",
    "id": "uuid",
    "businessNumber": "string",
    "snapshotHash": "sha256 hex"
  },
  "amounts": {
    "baseCharge": "decimal scale 2",
    "surcharges": "decimal scale 2",
    "penalties": "decimal scale 2",
    "creditAdjustments": "decimal scale 2",
    "subtotal": "decimal scale 2",
    "taxAmount": "decimal scale 2",
    "totalAmount": "decimal scale 2"
  },
  "tax": {
    "status": "SUPPLIED|NOT_SUPPLIED",
    "category": "bounded string|null",
    "jurisdictionReference": "bounded string|null",
    "taxableAmount": "decimal scale 2|null",
    "rate": "decimal scale 4|null",
    "exemptionReference": "bounded string|null"
  },
  "costCentres": [{"code": "string", "allocationPercent": "decimal scale 4"}],
  "originalBillingRecordId": "uuid|null",
  "finalizedAt": "RFC-3339 timestamp"
}
```

The P1-01 envelope supplies event/Tenant/type/version/aggregate/time/correlation/causation fields outside this payload. Arrays are deterministically ordered. Customer name/contact, addresses, cargo detail, notes, credentials, bank/payment data, account numbers, raw source payloads, and unrestricted metadata are forbidden. The payload remains within US-73's accepted 32-KiB limit and fails closed rather than truncating.

`EXPORTED` means the controlled JSON file exists with Integration-recorded durable hash evidence. It does not mean the accounting system imported, accepted, posted, settled, or paid it.

## RBAC, Tenant, privacy, and audit

The exact new permissions are:

- `BILLING_VIEW`: same-Tenant list/detail/history and safe export evidence;
- `BILLING_PREPARE`: create/edit/cancel drafts, select source, maintain authorized lines/tax facts/cost-centre allocation, and validate;
- `BILLING_APPROVE`: independently approve validated regular and reversal records;
- `BILLING_FINALIZE`: finalize approved regular and reversal records;
- `BILLING_EXPORT`: request controlled export of a finalized record.

No broad `FINANCE_ADMIN` is created. No Billing permission grants Customer mutation, source mutation, Integration configuration activation, accounting posting, invoice issuance, tax determination, payment, or cross-Tenant access.

Tenant comes only from authenticated `CurrentTenant` or trusted worker/event context, never payload/header authority. Every record, line, tax fact, cost-centre allocation, source claim, reversal link, history row, idempotency key, event, and safe export projection carries immutable Tenant identity. Every logical Customer/Trip/Freight/Integration reference is same-Tenant validated; cross-Tenant IDs return safe not-found.

Audit/history is append-only for create, source claim, line replacement/calculation, tax fact, allocation, validation, approval, cancellation, finalization, reversal, export request, delivery evidence, failed commands, actor, time, correlation, version, and before/after hashes. It excludes credentials, bank information, raw payloads, full Customer/contact data, addresses, and unrestricted descriptions. Retention is `EXTERNAL_POLICY`; no legal duration or purge endpoint is invented.

## Frozen API and errors

The Billing-owned API is:

- `GET|POST /api/v1/billing/records`;
- `GET /api/v1/billing/records/{id}`;
- `PUT /api/v1/billing/records/{id}/lines` for complete draft-line/tax/allocation replacement;
- `POST /api/v1/billing/records/{id}/validate`;
- `POST /api/v1/billing/records/{id}/approve`;
- `POST /api/v1/billing/records/{id}/cancel` for reasoned draft cancellation only;
- `POST /api/v1/billing/records/{id}/finalize`;
- `POST /api/v1/billing/records/{id}/reversals`;
- `POST /api/v1/billing/records/{id}/export`;
- `GET /api/v1/billing/records/{id}/history`.

There is no generic status PATCH, finalized edit/delete, mark-paid, posting, journal, tax filing, Customer balance, bank/payment, raw payload, manual external-success, or Integration retry route. Lists default to 20 and cap at 100. Filters are Customer, source type, source ID, lifecycle, record type, currency, cost-centre code, created range, and finalized range. Sorts are created, updated, finalized, Customer, total, and lifecycle; arbitrary property sorting is forbidden.

Stable errors are `BILLING_NOT_FOUND`, `BILLING_INVALID_STATE`, `BILLING_SOURCE_NOT_ELIGIBLE`, `BILLING_SOURCE_DUPLICATE`, `BILLING_CUSTOMER_INVALID`, `BILLING_CURRENCY_MISMATCH`, `BILLING_CALCULATION_INVALID`, `BILLING_TAX_FACT_INVALID`, `BILLING_COST_CENTRE_INVALID`, `BILLING_APPROVAL_REQUIRED`, `BILLING_SOD_VIOLATION`, `BILLING_STALE_VERSION`, `BILLING_ALREADY_FINALIZED`, `BILLING_REVERSAL_INVALID`, and `BILLING_EXPORT_CONFIGURATION_INVALID`. Validation is 400, authentication/authorization 401/403, safe absence 404, and state/version/idempotency conflicts 409 through the existing global error contract.

## Persistence and architecture expectation

Implementation is expected to add Billing-owned Tenant tables `transport_billing_record`, `transport_billing_line`, `transport_billing_cost_centre`, `transport_billing_tax_fact`, `transport_billing_source_claim`, and `transport_billing_history`. A minimal safe export projection may live on the record/history; external configuration, exchange, attempts, payload, and technical audit remain Integration-owned. No table is created by this decision task and no migration number is reserved.

Same-module relationships use Tenant-consistent composite foreign keys. Customer, Trip, Freight, Compliance, and Integration IDs are logical UUID references only. Business numbering is Tenant/year scoped as `TB-YYYY-NNNNNN`, gap-tolerant and never reused; it is expressly not called a tax invoice number. Required uniqueness covers Tenant/idempotency command, Tenant/billing number, Tenant/source claim, and Tenant/reversal original. Draft state uses optimistic locking; finalized facts and history are append-only.

The module follows domain-first hexagonal architecture and exposes focused root contracts only. No cross-module repository, JPA entity, SQL, join, physical FK, service injection, generic Finance package, duplicated outbox, or distributed transaction is approved.

## Frontend and acceptance freeze

The operator UI lives under the existing `AppLayout` as a Billing feature with list/filter, create-from-source, draft charge composition, separate surcharge/penalty/credit display, tax-fact and cost-centre entry, validation, permission-aware approval/finalization/export/reversal, immutable source trace, delivery evidence, and history. It uses TanStack Query, React Hook Form/Zod, the shared Axios client, accessible selectors, and centralized RBAC. It never labels a record Paid, Posted, Settled, Tax Filed, or Tax Invoice Issued and never displays raw Integration payloads or credentials.

Unit/application coverage must prove eligibility, exact formula, precision/rounding, tax supplied/not-supplied behavior, allocations, lifecycle, SoD, immutability, reversal, source uniqueness, idempotency, Tenant/privacy, source-version invalidation, durable event, and export semantics. PostgreSQL acceptance must prove clean current-head migration, Tenant constraints, money precision, business numbering, source uniqueness, reversal chain, append-only history, optimistic locking, idempotency, and deterministic races without touching a development database.

The real PostgreSQL-backed Chromium suite must be complete and must prove at least six scenarios: successful Closed Trip billing through controlled JSON export; Freight blocked until an owner-completed fact exists and then accepted when it does; invalid/stale/duplicate source denial; preparer self-approval denial followed by independent approval/finalization; immutable finalized record plus exact compensating reversal; and Tenant-B/limited-role denial with privacy-safe history. It must verify exact file/hash evidence and absence of posted/paid claims. Partial browser evidence is not acceptance.

Rollback before approval is draft replacement/revalidation or reasoned cancellation. After finalization it is reversal/correction only. Export retry preserves the same event/payload identity and follows Integration's accepted retry policy. Database rollback is forward-only. Source records and finalized Billing history are never rewritten or deleted.

## Hard decision gate result

All requested gates are frozen: source actor/goal/criteria, dedicated owner, Billing/Finance/Customer/source boundaries, eligible sources, aggregate, operational-finalization versus tax-invoice authority, explicit commercial inputs, charge/adjustment/tax/currency/money/cost-centre rules, per-source period model, lifecycle, approval/SoD, immutability/reversal, duplicate/idempotency/concurrency rules, Tenant/RBAC/privacy/audit, controlled external tier, US-73/P1-01 event family, APIs/errors, persistence, UI, PostgreSQL/E2E strategy, and rollback. Scope leakage is `NONE`.
