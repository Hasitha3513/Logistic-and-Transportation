# US-38 Handle Fuel Exceptions — Final Acceptance

**Task:** `US-38-FUEL-EXCEPTIONS-FINAL-ACCEPTANCE-001`
**Decision:** `PASS / COMPLETE`
**Accepted:** 2026-09-07
**Owner:** Fuel
**Migrations:** V66 and V67; current Flyway head V67
**Program accounting:** 70 / 87 COMPLETE; 17 / 87 remaining
**Wave B:** OPEN; US-35, US-37 and US-38 complete; US-46 and US-47 remain
**Next task:** `US-46-DRIVER-PAYROLL-LINK-PRODUCT-DECISIONS-001`

## Independent acceptance decision

US-38 satisfies the authoritative source intent and frozen product decisions. Fuel owns the exact six-category review capability, local lifecycle, evidence, notes, correction request and independent approval, owner-command orchestration, immutable history, and durable handoff record. Operations retains central assignment, severity confirmation, SLA, escalation level, RCA, closure and reopen. Language remains non-punitive and does not assert theft, fraud, Driver guilt, criminal culpability, or automatic discipline.

Manual cases require a verified same-Tenant source. Only a rejected `NEGATIVE_BUNKER_BALANCE` command automatically creates a case. Replaying the same rejected command retains one stable internal source-event identity and one active case, while Tank stock, movement count, and ledger sequence remain unchanged. A subsequent valid Bunker adjustment creates the canonical compensating movement at N+1 and preserves the ledger-tail invariant.

The local lifecycle is exactly `OPEN`, `UNDER_REVIEW`, `CORRECTION_PENDING`, `AWAITING_APPROVAL`, and `RESOLVED`; outcomes are exactly `NO_ACTION_REQUIRED`, `CORRECTION_APPLIED`, `RECONCILED`, `EMERGENCY_REFUEL_ACCEPTED`, and `REFERRED_TO_OPERATIONS`. CRITICAL resolution fails before handoff and after a failed handoff, and becomes eligible only after published/accepted handoff when other preconditions pass.

## Correction, immutability and handoff controls

V67 persists mandatory owner/request/before-state, approval, execution, idempotency and optimistic-version evidence. Correction attempts are database-enforced append-only and record `FAILED`, `SUCCESS`, and `NOOP_REPLAY`. Controlled failure remains retryable in `CORRECTION_PENDING`; retry performs exactly one effective owner action, and replay after success does not repeat it. Requester/approver separation and US-35 importer/reconciler separation remain enforced.

Real acceptance paths preserve historical Fuel Issue, Fuel Purchase, P1 Fuel Price, Fleet reading, Bunker movement and Fuel Card provider facts. Price correction creates a later effective-dated P2 without changing P1 or historical purchase monetary values. Fleet correction creates a compensating reading while retaining the original. The card-policy case originates from a persisted accepted US-35 indicator. `HISTORICAL_SOURCE_MUTATED = NO`.

The controlled handoff failure persists `FAILED`; retry reuses the same handoff row, immutable event ID and source-event identity and creates one Operations case. Fuel reuses `OperationalExceptionFactV1`, the shared `DurableEventPublisher`, and `integration_outbox_event` under at-least-once semantics. There is no exactly-once/global-ordering claim, direct Fuel Notification engine, second outbox/inbox, or duplicate Operations lifecycle.

## Security, API, migration and performance

Tenant authority is server-derived. Tenant B case/source/attempt/history/correction/handoff access and limited-user commands are denied. Exactly `FUEL_EXCEPTION_VIEW`, `FUEL_EXCEPTION_MANAGE`, `FUEL_EXCEPTION_CORRECT`, `FUEL_EXCEPTION_APPROVE`, and `FUEL_EXCEPTION_ESCALATE` are added. The frozen public route set remains unchanged and exposes no test-only source-event, handoff-event or idempotency identity.

The Node acceptance helper is test-only, fixed-query/read-only, rejects every database except `transport_logistics_acceptance`, carries no hardcoded or logged credential, and is not browser-bundled. V66 remains intact; V67 contains only the authorized remediation and append-only database enforcement. V1–V66 are unchanged, no migration beyond V67 exists, US-38 tables remain Fuel-owned, and there is no physical cross-module foreign key. Pagination and source/history/attempt lookups are bounded and Tenant-leading; there is no cross-module N+1 or unbounded foreign scan.

## Fresh final-acceptance evidence

- Focused US-38: 21/21 PASS, including clean PostgreSQL V1→V67 and nine deterministic concurrency races.
- US-35, US-37, Bunker, US-78, P1-01 and Notification regression matrix: 112/112 PASS.
- Full Maven: 1,356 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 05:34.
- Architecture and Spring Modulith: 46/46 PASS.
- Static analysis: Checkstyle 0 violations; PMD PASS; SpotBugs 0 findings/errors. One discarded sandboxed static invocation could not write a Maven dependency-tracking file; the unchanged unrestricted rerun is authoritative.
- Frontend: TypeScript PASS; production build PASS; 63 Vitest files / 263 tests PASS; US-38 changed-file ESLint PASS. The existing large-chunk warning remains non-blocking global debt.
- Fresh real PostgreSQL-backed Chromium: 6/6 PASS in 36.6 seconds, covering all six categories and the required real-source, immutability, idempotency, handoff, tenancy and RBAC evidence.
- `git diff --check`: PASS.

A historical discarded mixed-profile remediation invocation contacted the development datasource and V65 failed closed and rolled back. `DEVELOPMENT_DATABASE_AUTHORITATIVE_EVIDENCE = NO`. Every fresh authoritative final-acceptance database result used only `transport_logistics_acceptance`.

## Scope containment and disposition

No payment/settlement, provider authority, named provider integration, fraud/workflow/retry engine, punitive Driver action, arbitrary threshold, automatic case beyond negative balance, duplicate Operations/Notification/Document capability, binary storage, foreign repository/SQL/FK, distributed transaction, second outbox/inbox, raw source rewrite, new public API/route/permission, or migration beyond V67 exists.

All final-acceptance gates pass with zero blockers. US-38 is COMPLETE. Program accounting advances exactly once from 69 / 87 to 70 / 87 complete, leaving 17 / 87 (`70 + 17 = 87`). Wave B remains open for US-46 and US-47.
