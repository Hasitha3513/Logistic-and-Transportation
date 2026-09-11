# US-35 Fuel Cards — Implementation Evidence

**Task:** `US-35-FUEL-CARDS-IMPLEMENTATION-001`  
**State:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**Migration:** V64  
**Owner:** Fuel

## Implemented boundary

Fuel owns the local fuel-card master, local lifecycle, one current Vehicle-or-Driver binding with immutable history, restrictions, controlled import batches, immutable provider transaction facts, reconciliation history, retrospective review indicators, and privacy-minimized audit history. Organization remains authoritative for the provider reference; Fleet and Driver remain authoritative for binding targets; Trip remains authoritative for trip references; the existing US-32 Fuel Purchase remains authoritative for purchase facts.

The implementation does not claim provider authorization, blocking, activation, balances, settlement, or financial-ledger authority. It adds no payment processing, issuing, banking, provider credentials, inbound US-73 adapter, US-38 investigation workflow, US-78 case creation, or P1-01 event family.

## Sensitive-data model

Stored card identity is limited to an internal UUID, tenant-scoped provider UUID, alias, opaque provider resolution reference, masked display identifier, optional last four digits, and expiry month/year. API responses, UI, audit rows, errors, and browser evidence omit the opaque reference. PAN, CVV, PIN, magnetic-stripe data, authorization secrets, credentials, balances, and raw provider payloads are absent.

## Lifecycle, binding, and restrictions

Cards start in `DRAFT`. Explicit command endpoints implement activation, suspension, resume, local block, and cancellation; expiry is derived in the tenant timezone and cannot be reversed. Activation requires both an active binding and restrictions. A partial unique index permits exactly one current binding, while closed binding rows remain append-only history. Vehicle and Driver validation uses their public lookup contracts.

Restrictions cover ISO currency, per-transaction amount, daily amount, monthly amount, daily litres, allowed fuel types, and optional station references. Imported facts are retained and evaluated retrospectively; failures create non-fraud review indicators rather than rejecting provider facts.

## Controlled canonical import and evidence tier

The sole inbound evidence tier is `CONTROLLED_PROVIDER_FIXTURE`: one strict UTF-8 JSON multipart file using schema `FUEL_CARD_TRANSACTIONS_V1`. The parser rejects BOMs, malformed/trailing JSON, unknown properties, invalid enumerations, invalid numbers/currency, files over 1 MiB, more than 1,000 transactions, incorrect MIME type, and multipart requests containing anything other than the single `file` part.

Idempotency is enforced independently by tenant/provider batch ID, tenant/provider SHA-256 file hash, and tenant/provider transaction ID plus canonical transaction hash. Exact replay returns the prior batch; a same identity with different content fails as a conflict.

## Transactions, reversals, reconciliation, and indicators

Provider transaction source facts are immutable. Reversals are separate facts referencing the original provider transaction; the original remains stored. Local linkage supports explicit match, unmatch, and reject commands against an existing US-32 Fuel Purchase. It never creates or mutates a Fuel Purchase. Importer/reconciler separation is enforced even if one actor has both endpoint permissions, and optimistic versions serialize concurrent decisions.

Indicators are retrospective operational review signals: `BINDING_MISMATCH`, `FUEL_TYPE_NOT_ALLOWED`, `STATION_NOT_ALLOWED`, `LIMIT_EXCEEDED`, `CARD_INACTIVE`, `TRANSACTION_INTEGRITY_CONFLICT`, and `REVERSAL_REVIEW_REQUIRED`. They are not fraud findings and do not create US-78 cases.

## Tenant, RBAC, audit, and API

Every new table, repository operation, query, command context, child reference, audit row, and filter is tenant-scoped. Cross-tenant direct reads return not found. Five permissions are seeded idempotently: `FUEL_CARD_VIEW`, `FUEL_CARD_MANAGE`, `FUEL_CARD_BLOCK`, `FUEL_CARD_IMPORT`, and `FUEL_CARD_RECONCILE`. Literal `/api/v1/...` security regression coverage proves dedicated permission enforcement.

The frozen card, import, and transaction routes are implemented with page defaults of 20 and a maximum of 100. Card and transaction lists support the frozen safe filters and allowlisted sorts with stable UUID tie-breaking. Generic status mutation, delete, raw edit, provider action/retry, payment, secret, and raw-payload routes are absent.

Audit records cover card creation/update/lifecycle, binding and restriction changes, transaction import outcome, and reconciliation commands without provider references or raw payloads.

## Database

Forward-only migration `V64__fuel_cards_us35.sql` creates exactly eight Fuel-owned tables:

- `fuel_card`
- `fuel_card_binding_history`
- `fuel_card_restriction`
- `fuel_card_import_batch`
- `fuel_card_transaction`
- `fuel_card_reconciliation_history`
- `fuel_card_transaction_indicator`
- `fuel_card_audit_event`

Composite tenant/card foreign keys enforce same-tenant child ownership. Vehicle, Driver, Trip, Organization provider, user/actor, and Fuel Purchase identifiers are logical UUID references without cross-module physical foreign keys. JPA optimistic versions protect card, restriction, and transaction decisions.

## Frontend

`/fuel/cards` is integrated into the existing AppLayout and permission-aware navigation. It provides masked card listing, draft creation, detail, lifecycle actions, Vehicle/Driver binding, binding history, restrictions, audit history, canonical JSON upload/results/history, immutable transaction detail, indicators, reversal relationship, and match/unmatch/reject controls. It continuously displays “Provider synchronization not configured” and does not claim external action success.

## Verification evidence

- Technical closure: `US-35-FUEL-CARDS-TECHNICAL-CLOSURE-001` passed after repairing sample-data test baseline isolation; the fixture test passed 1/1 and the complete focused group passed 23/23.
- Focused domain, parser, application service, literal security, sample-data idempotency, and PostgreSQL tests: 23 passed.
- PostgreSQL V1→V64 schema/constraint/concurrency acceptance: 7 passed against `transport_logistics_acceptance` only; the development database was not used.
- Real PostgreSQL-backed Chromium suite: 6/6 passed, including real multipart parsing/persistence, replay, reconciliation SoD, review indicators, reversal, source immutability, tenant isolation, RBAC, filters, and forbidden routes.
- Frontend Vitest: 63 files / 263 tests passed.
- TypeScript and production Vite build passed; changed-file ESLint introduced zero errors.
- Checkstyle: zero violations; PMD and SpotBugs passed.
- Complete Maven: 1,332 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 04:57.
- Architecture: 46 tests, 0 failures, 0 errors, 0 skipped.
- Global ESLint retains 71 unrelated pre-existing Delivery errors; US-35 changed-file lint has zero errors.
- `git diff --check`: passed.

## Regression and scope containment

The complete Maven suite covers existing US-31/32/34/36/37, Fleet, Driver, Trip, Organization, Identity/RBAC, Tenancy, Audit, US-73, and US-78 behavior. The US-35 implementation adds no inbound Integration capability and no Operations case side effect. Existing Fuel Purchase and Fuel Performance facts remain unchanged by card imports and reconciliation.

Independent final acceptance has not started; the next task is `US-35-FUEL-CARDS-FINAL-ACCEPTANCE-001`.
