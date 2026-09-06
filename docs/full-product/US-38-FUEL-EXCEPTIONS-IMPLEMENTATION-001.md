# US-38 Fuel Exceptions Implementation

Status: `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
Migration: `V66__fuel_exceptions_us38.sql`  
Accounting: 69 / 87 complete; 18 / 87 remaining (unchanged)

## Implemented contract

Fuel owns the six-category case model, local lifecycle (`OPEN`, `UNDER_REVIEW`, `AWAITING_APPROVAL`, `CORRECTION_PENDING`, `RESOLVED`), immutable evidence, append-only notes/history, correction requests and Operations-handoff record. Operations retains assignment, SLA, escalation, RCA and closure ownership.

The exact taxonomy is `SUSPECTED_FUEL_LOSS`, `INCORRECT_READING`, `SUDDEN_PRICE_CHANGE`, `EMERGENCY_REFUEL`, `FUEL_CARD_POLICY_DEVIATION`, and `NEGATIVE_BUNKER_BALANCE`. User-facing text remains review-oriented and non-punitive.

Manual creation validates a same-Tenant source. Rejected negative Bunker adjustments create or deduplicate an automatic case in an independent transaction while leaving Tank stock, movements and ledger sequence unchanged. Resolved cases are terminal locally; a later episode requires new source identity.

Evidence and notes are append-only and contain bounded safe metadata. Raw Fuel, card-provider and Fleet-reading facts are never rewritten by the case service. Every financial, inventory, price, reconciliation or lifecycle correction requires independent approval. The requester cannot approve; US-35 importer/reconciler separation remains authoritative. Approved corrections invoke existing owner commands for Bunker adjustments, Fuel Issue cancellation, Fuel Purchase reconciliation/cancellation, effective-dated Fuel Price creation, Fleet reading correction, and Fuel Card match/unmatch/reject. Owner failures remain visible in `CORRECTION_PENDING` and are explicitly retryable.

Authorized HIGH/CRITICAL escalation publishes the minimized existing `OperationalExceptionFactV1` through the P1-01 shared durable outbox. The immutable handoff event ID supplies producer and Operations consumer deduplication. Fuel sends only exception ID, source type and source ID metadata.

## Security, API and persistence

Tenant identity is server-derived and all case, child, source, filter and handoff operations are Tenant-scoped. V66 seeds exactly `FUEL_EXCEPTION_VIEW`, `FUEL_EXCEPTION_MANAGE`, `FUEL_EXCEPTION_CORRECT`, `FUEL_EXCEPTION_APPROVE`, and `FUEL_EXCEPTION_ESCALATE`.

The API implements list/create/detail, review, evidence, notes, correction request/approve/reject, resolve, escalate and history below `/api/v1/fuel/exceptions`. Generic status mutation, delete, raw-source editing, verdict, public and arbitrary Operations-create routes are absent.

V66 creates `fuel_exception_case`, `fuel_exception_evidence`, `fuel_exception_note`, `fuel_exception_correction`, `fuel_exception_history`, and `fuel_exception_operations_handoff`, with Tenant-leading constraints/indexes, optimistic versions, active-case uniqueness and handoff/event uniqueness. It extends the accepted Operations source-module constraint with `FUEL`.

## Frontend

Fuel Exceptions is integrated under the existing Fuel navigation and AppLayout. The page provides permission-aware queue/detail views, safe-language guidance, source/review/handoff state, append-only notes/evidence/history, correction approval and Operations escalation. Backend authorization remains authoritative.

## Verification evidence

- Complete Maven: 1,339 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS (05:18).
- Architecture: 46/46 PASS.
- PostgreSQL migration and structural acceptance: V1 through V66 PASS using only `transport_logistics_acceptance`.
- Checkstyle: zero violations; PMD: PASS; SpotBugs: zero findings.
- TypeScript: PASS; Vitest: 263/263 PASS; production build: PASS; changed-file lint: PASS.
- Real PostgreSQL-backed Chromium: 6/6 PASS across all categories and required safety, correction, handoff, tenancy and RBAC outcomes.
- `git diff --check`: PASS.

## Scope exclusions

No generic incident/workflow/fraud engine, raw-source rewrite, distributed transaction, second outbox, payment/provider authority, punitive conclusion, automated discipline, cross-module repository access, arbitrary Operations creation, or development-database acceptance path was introduced.

Next task: `US-38-FUEL-EXCEPTIONS-TECHNICAL-CLOSURE-001`.
