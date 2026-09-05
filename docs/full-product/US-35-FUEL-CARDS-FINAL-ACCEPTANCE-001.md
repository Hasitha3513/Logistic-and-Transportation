# US-35 Manage Fuel Cards — Final Acceptance

**Task:** `US-35-FUEL-CARDS-FINAL-ACCEPTANCE-001-RERUN-2`  
**Decision:** `COMPLETE / FINAL_ACCEPTANCE_PASS`  
**Accepted:** 2026-09-05  
**Owner:** Fuel  
**US-35 migration:** V64  
**Current Flyway head:** V65  
**Program accounting:** 69 / 87 COMPLETE; 18 / 87 remaining  
**Next task:** `US-38-FUEL-EXCEPTIONS-PRODUCT-DECISIONS-001`

## Decision

Independent hostile acceptance passes after `BUNKER-LEDGER-ORDERING-AUTHORIZATION-001`, `US-35-FUEL-CARDS-ACCEPTANCE-REMEDIATION-002`, and `US-35-FUEL-CARDS-ACCEPTANCE-REMEDIATION-003`. The frozen Fuel Card contract is satisfied without reopening product decisions or changing production logic during this acceptance run.

Fuel owns the masked local card reference, lifecycle, restrictions, one active Driver-or-Vehicle binding and immutable history, controlled canonical import, immutable normalized provider facts, deterministic review indicators, and reconciliation to an existing US-32 Fuel Purchase. The external provider retains card-account, authorization, settlement, provider-ledger, provider-transaction-identity, and merchant-fact authority. Payment processing, provider synchronization/blocking, PAN/CVV/PIN, provider credentials or balances, raw-file retention, definitive fraud findings, US-38 investigation, US-78 case creation, and P1-01 events remain absent.

## Fresh acceptance evidence

- PostgreSQL/H2 sample provisioning: 2/2 PASS. Repeated PostgreSQL provisioning remained idempotent; H2 ledgered samples remained consistent and its history-free Tank remained at zero.
- Focused US-35: 22/22 PASS with zero failures, errors, or skips.
- Bunker PostgreSQL concurrency: 10/10 PASS, including concurrent receipt versus issue, two receipts, two issues, same/backdated timestamps, rollback, Tenant/Tank independence, and duplicate-sequence rejection.
- Complete Fuel/Bunker/US-35/US-37 selection: 163/163 PASS with zero failures, errors, or skips.
- Complete Maven verification: 1,335 tests, 0 failures, 0 errors, 15 skipped; terminal `BUILD SUCCESS` in 05:09.
- Architecture: 46/46 PASS.
- Static analysis: Checkstyle reported 0 violations; PMD `BUILD SUCCESS`; SpotBugs reported 0 findings and 0 errors under Java 21.
- Frontend: TypeScript PASS; Vitest 263/263 across 63 files PASS; production build PASS; US-35 feature ESLint PASS. Existing warning/debt output is outside US-35 and does not fail the repository acceptance policy.
- Real PostgreSQL-backed Chromium: 6/6 PASS using the actual upload/parser/hash/persistence/dedupe/reconciliation path and only `transport_logistics_acceptance`.
- Post-Chromium read-only database proof: Flyway V65 successful; nonzero Tanks without movements = 0; latest-ledger-tail mismatches = 0; seeded nonzero Tank stock range = 24,920.000 to 24,920.000 litres.
- `git diff --check`: PASS. No migration beyond V65 and no application production change was made by final acceptance.

## Contract and containment findings

The exact `DRAFT`, `ACTIVE`, `SUSPENDED`, `BLOCKED`, `EXPIRED`, and `CANCELLED` lifecycle; terminal-state rules; Tenant-timezone expiry; binding/restriction invariants; BigDecimal/currency rules; strict `FUEL_CARD_TRANSACTIONS_V1` limits and validation; batch/file/transaction idempotency; immutable reversal and provider facts; importer/reconciler separation; safe review-only language; five permissions; Tenant A/B denial; masked API/UI/audit surface; bounded pagination/filter/sort; and frozen explicit routes all pass source and test inspection.

V64 remains the intact US-35 migration. V65 remains the authorized Bunker-only canonical ledger-order migration, with per-Tenant/Tank `ledger_sequence`, deterministic backfill and validation, NOT NULL, uniqueness, and Tenant-leading ordering index. Runtime allocation is `MAX(ledger_sequence) + 1` under the existing Tank write lock, and latest-first reads use sequence rather than business or audit time.

`DEVELOPMENT_DATABASE_AUTHORITATIVE_EVIDENCE = NO`. Historical discarded invocations contacted the development database; every accepted PostgreSQL result in this rerun used only `transport_logistics_acceptance`.

## Release state

`US-35 = COMPLETE`. Program accounting advances exactly once to 69 / 87 complete and 18 / 87 remaining (`69 + 18 = 87`). Wave B remains open with US-35 and US-37 complete; US-38, US-46, and US-47 remain. The next authorized task is `US-38-FUEL-EXCEPTIONS-PRODUCT-DECISIONS-001`.
