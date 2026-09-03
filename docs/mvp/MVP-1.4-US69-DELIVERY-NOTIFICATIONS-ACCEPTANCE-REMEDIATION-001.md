# US-69 Delivery Notifications — Acceptance Remediation

**Task ID:** `MVP-1.4-US69-OUT-FOR-DELIVERY-TRIGGER-REMEDIATION-001`  
**Date:** 2026-09-03  
**Status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**US-69 migration / current Flyway head:** `V58__delivery_notifications_us69.sql`

## Root cause and correction

`DeliveryBatchService.markReady` incorrectly published `DELIVERY_OUT_FOR_DELIVERY` while a Batch committed as `READY`. The frozen customer meaning is actual operational dispatch, so readiness must not produce that event. The existing active-member publication loop was moved without contract changes to `DeliveryBatchService.dispatchBatch`, after the Batch is saved as `DISPATCHED`.

The correction retains the existing Tenant-derived event envelope, safe payload, active-membership query, Spring-local after-commit publisher, and Notification bridge. It adds no Delivery Order lifecycle state, Notification behavior, provider, API, dependency, or migration.

## Deterministic lifecycle evidence

- Committed `markReady`: Batch is `READY`; `DELIVERY_OUT_FOR_DELIVERY` count is 0; the active-member lookup is not invoked.
- Committed `dispatchBatch` with two active members and one removed member: Batch is `DISPATCHED`; exactly two events are observed, one for each active Delivery Order; the removed order is never loaded.
- Controlled dispatch rollback after state work: the actual after-commit adapter observes no event. The test uses transaction synchronization callbacks and no sleeps.
- Whole-source duplicate audit: the only production producer of `DELIVERY_OUT_FOR_DELIVERY` is the canonical `dispatchBatch` path. No readiness/status-listener producer remains.
- All five frozen US-69 event types and their version-1 envelope/payload contract remain unchanged.

## Verification evidence

| Gate | Actual result |
| :--- | :--- |
| Focused Batch/event/after-commit tests | 20 tests, 0 failures, 0 errors, 0 skipped — PASS |
| Delivery, US-66/67/68, Notification, and PostgreSQL regressions | 193 tests, 0 failures, 0 errors, 0 skipped — PASS |
| Complete `./mvnw verify` | 1,223 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 4:22 |
| Flyway | V1–V58 applied/validated using `transport_logistics_acceptance`; current head V58 |
| Architecture and Spring Modulith | 42/42 PASS |
| Checkstyle | PASS; 0 violations |
| PMD | PASS |
| SpotBugs | PASS; 0 bugs/errors |
| TypeScript | PASS |
| Vitest | 57 files / 254 tests PASS |
| Production build | PASS in 4.67s; existing non-failing bundle-size warning |
| Changed-file ESLint | PASS; 0 errors/warnings |
| Real PostgreSQL-backed Chromium | 7/7 PASS in 18.5s, including READY=0 then DISPATCHED=exactly-one masked timeline record |
| `git diff --check` | PASS |

All destructive PostgreSQL verification used only `transport_logistics_acceptance` at port 5433. The development database was not used or touched. V58 was not edited, no V59 was created, and the prior excluded development diagnostic remains excluded from evidence.

No US-70 capability, OTP, vendor SMS SDK, callback/webhook, manual resend, or outbox/inbox work was introduced. The global frontend lint baseline was not rerun because frontend production code is unchanged; changed-file lint passed and the previously classified 71-error/0-warning debt remains confined to untouched files.

## Program state and next task

- US-69: `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`
- MVP 1.4: 6 / 8 COMPLETE
- Overall: 63 / 87 COMPLETE
- Deferred: 24 / 87
- Next: `MVP-1.4-US69-DELIVERY-NOTIFICATIONS-FINAL-ACCEPTANCE-001-RERUN`

This remediation does not constitute final product acceptance and does not start US-70.

## Governance synchronization

The four affected central knowledge-base files were committed on local `main` as `4ab58cb` (`docs(delivery): record US-69 trigger remediation`). A normal push to `origin/main` failed because HTTPS credentials are unavailable and `gh` is not installed; local `main` is one commit ahead of `origin/main` (`1 0`). This is `BLOCKED_GOVERNANCE_SYNC_AUTHENTICATION`, not a technical remediation failure or a reason to downgrade US-69 from acceptance pending. Minimum action: provide a secure authenticated Git credential for the configured GitHub HTTPS remote, then push the existing commit without recreating it.
