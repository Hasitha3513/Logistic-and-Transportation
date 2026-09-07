# US-47 Transport Billing Implementation

**Task:** `US-47-TRANSPORT-BILLING-IMPLEMENTATION-001`  
**Result:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**Story accounting:** 71 / 87 COMPLETE; 16 / 87 remaining

## Implemented boundary

The dedicated top-level `billing` bounded context owns `TransportBillingRecord`, immutable source snapshots and claims, explicit commercial lines, supplied tax facts, operational cost-centre allocations, validation, independent approval, operational finalization, exact reversal, durable idempotency, append-only history, and controlled export state. Finance/external accounting remains authoritative for tax invoices, GL, AR/AP, posting, periods, payment, banking, settlement, remittance, collections, and customer balances.

Published provider-neutral source contracts accept only a `CLOSED` Trip or an explicit Freight-owned `COMPLETED`/`CLOSED` fact. Organization remains Customer master; Billing stores only the same-Tenant logical Customer UUID. No foreign repository, SQL, JPA relationship, or physical foreign key was introduced.

## Commercial and lifecycle rules

- One regular record claims one `TRIP` or `FREIGHT_ORDER` source.
- Exactly one positive `BASE_CHARGE`; non-negative `SURCHARGE`, `PENALTY`, and bounded-reason `CREDIT_ADJUSTMENT` lines.
- `subtotal = base + surcharges + penalties - credits`; one Tenant currency, `BigDecimal`, precision 19, `HALF_UP`, scale 2 money and scale 4 rates/quantities.
- Tax is `SUPPLIED` or `NOT_SUPPLIED`; Billing reconciles supplied arithmetic but performs no jurisdictional tax calculation.
- Regular cost-centre allocations total exactly 100.0000% and are operational references, not GL accounts.
- `DRAFT → VALIDATED → APPROVED → FINALIZED → EXPORT_REQUESTED → EXPORTED`, with reasoned pre-approval cancellation and post-finalization exact reversal. Draft editing invalidates validation. Preparer and approver must differ.
- Tenant/year numbers use `TB-YYYY-NNNNNN`. Mutable commands use optimistic versions. Required commands persist Tenant/scope/key/request hashes and serialize identical-key races with PostgreSQL advisory transaction locks.

## API, RBAC, persistence, and integration

The exact `/api/v1/billing/records` list/create/detail/replace-lines/validate/approve/cancel/finalize/reversals/export/history family is implemented. Literal-route security tests cover the five permissions: `BILLING_VIEW`, `BILLING_PREPARE`, `BILLING_APPROVE`, `BILLING_FINALIZE`, and `BILLING_EXPORT`.

Flyway `V72__transport_billing_us47.sql` adds `freight_billing_fact` plus the six Tenant-owned Billing tables, Tenant-consistent composite keys, source/billing-number/reversal/idempotency uniqueness, monetary constraints, immutable finalized-child/history triggers, Tenant-leading indexes, and permission seeds. V1→V72 is clean. No prior migration changed.

`TransportBillingExportRequestedV1` uses the shared P1-01 outbox with aggregate `TRANSPORT_BILLING_RECORD`, family `TRANSPORT_BILLING_V1`, `FINANCIAL_CONFIDENTIAL`, and controlled US-73 `FILE_JSON_V1`. Canonical UTF-8 JSON is deterministic, sorted where repeated, private, and fails closed above 32 KiB. `EXPORTED` is set only from controlled delivery/hash evidence and means delivery only—not posting, importing, payment, or settlement.

## Frontend

The Billing route and navigation live under the existing `AppLayout`. The feature owns its API, TanStack Query hooks, types, React Hook Form/Zod create and draft-edit modals, permission-aware lifecycle actions, list, detailed line/tax/cost-centre/source/approval/delivery/history views, and explicit non-accounting terminology.

## Verification evidence

- Domain, PostgreSQL V72, literal security, integration mapping, and architecture focused selection: PASS.
- PostgreSQL: only `transport_logistics_acceptance`; repeated clean V1→V72; development database was not authoritative evidence.
- Full Maven: 1,389 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS`; 06:29.
- Architecture: 46 / 46 PASS in the final full suite.
- Checkstyle: 0 violations. PMD: PASS. SpotBugs: 0 findings.
- Frontend TypeScript: PASS. Vitest: 63 files and 263 / 263 tests PASS. Production build: PASS. US-47 changed-file ESLint: 0 errors.
- Real PostgreSQL-backed Chromium: 7 / 7 PASS in 33.3 seconds. Evidence includes CLOSED Trip and explicit Freight eligibility, all four categories, tax/cost centre, SoD, finalization/export/file/hash/EXPORTED, invalid/stale/duplicate rejection, exact immutable reversal, durable replay/conflict, Tenant-B/RBAC denial, canonical privacy, and UI.
- `git diff --check`: PASS.

## Scope exclusions

No GL, AR/AP, payment gateway, banking, tax engine/filing, customer balance/master duplication, pricing engine, automatic Fuel surcharge, automatic Driver penalty, live ERP claim, inbound acknowledgement, manual external-success/retry route, second outbox, foreign persistence, distributed transaction, or source mutation exists.

## Handoff

US-47 remains unaccepted. The next task is `US-47-TRANSPORT-BILLING-TECHNICAL-CLOSURE-001`; story accounting remains unchanged.
