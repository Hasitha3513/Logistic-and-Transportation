# US-35 Fuel Cards — Acceptance Remediation 002

**Task:** `US-35-FUEL-CARDS-ACCEPTANCE-REMEDIATION-002`  
**Authorization:** `BUNKER-LEDGER-ORDERING-AUTHORIZATION-001` applied  
**Result:** `COMPLETE`  
**Story status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED`

## Canonical Bunker ledger order

V65 adds the internal, server-controlled `ledger_sequence BIGINT` to `bunker_stock_movement`. The sequence is monotonic within Tenant + Tank, begins at 1, and is allocated as the bounded Tank-scoped `MAX(ledger_sequence) + 1` only after the existing Tank write lock is held. Opening balance, adjustment, purchase receipt, Fuel Issue, and both independently sequenced transfer sides use the allocator in the same transaction as the balance mutation and movement insert. Transfers retain stable two-Tank locking.

`occurred_at` remains business/source time and `created_at` remains audit time. Latest-first ledger queries now use `ledger_sequence DESC`; neither timestamp claims serialized mutation order. No Fuel Card API, lifecycle, RBAC, import, reconciliation, UI, event, or idempotency behavior changed.

## Migration and legacy canonicalization

V65 backfills each legacy Tenant/Tank ledger contiguously from 1 using `occurred_at ASC, created_at ASC, id ASC`. This is a deterministic migration canonicalization, not a claim about original commit order. The migration fails closed for orphan/cross-Tenant movements, invalid movement facts, logical duplicates, non-zero Tanks without history, invalid/non-contiguous sequences, and canonical tail/balance mismatch. It then enforces `NOT NULL`, uniqueness on `(tenant_id, tank_id, ledger_sequence)`, and the latest-first index `(tenant_id, tank_id, ledger_sequence DESC)`.

The clean PostgreSQL path used only `transport_logistics_acceptance`. Flyway V1→V65 passed. Schema inspection confirmed V65, non-null `BIGINT`, the unique constraint, and the Tenant-leading index. PostgreSQL `EXPLAIN` selected an Index Only Scan for the Tenant/Tank maximum lookup.

## Verification evidence

- Focused real PostgreSQL Bunker concurrency/ordering: 10 tests, 0 failures, 0 errors, 0 skipped. This includes two permitted issues, two permitted receipts, receipt-versus-issue conservation, rollback/no-gap behavior, equal/backdated `occurredAt`, monotonic latest-first sequences, per-Tank independence, transfer sequencing, and duplicate rejection.
- Fuel/Bunker/US-35/US-37 focused regression: 81 tests, 0 failures, 0 errors, 0 skipped.
- Complete Maven `verify`: 1,335 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS`; 05:03.
- Architecture: 46 tests, 0 failures/errors/skips (Modulith, hexagonal layers, module boundaries, table ownership, aggregate boundaries, and Lombok policy).
- Checkstyle: zero violations. SpotBugs: zero findings. PMD found zero remediation-introduced findings; `pmd:check` continues to report 83 pre-existing repository-wide wildcard-import findings, including unchanged imports in legacy Fuel services. No unrelated cleanup was performed.
- Frontend: TypeScript passed; Vitest passed 263/263 across 63 files; production build passed; US-35 changed-file ESLint passed.
- Real PostgreSQL-backed Chromium US-35 suite: 6/6 passed.
- `git diff --check`: passed.

One non-authoritative command accidentally contacted the development datasource before the acceptance URL was made explicit. V65 failed closed on unreconciled legacy ledger data. No clean, repair, manual data mutation, or authoritative evidence was taken from development; all reported database evidence is from `transport_logistics_acceptance`.

## Scope and next task

Story accounting remains 68 / 87 complete and 19 / 87 remaining. Remediation does not accept US-35. The next task is `US-35-FUEL-CARDS-FINAL-ACCEPTANCE-001-RERUN`.
