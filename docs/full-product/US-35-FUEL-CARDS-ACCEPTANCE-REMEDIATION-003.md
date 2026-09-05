# US-35 Fuel Cards — Acceptance Remediation 003

**Task:** `US-35-FUEL-CARDS-ACCEPTANCE-REMEDIATION-003`  
**Result:** `COMPLETE`  
**Story status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED`  
**Migration/current head:** V65 (unchanged)

## Root cause and correction

Normal PostgreSQL sample startup created ten Tanks at `25000.000` litres and one canonical `DISPENSE` movement per Tank for `80.000` litres whose resulting balance was `24920.000`. The Tank values were corrected to the derived ledger truth `25000.000 - 80.000 = 24920.000` without changing movements, sequences, or production behavior.

The H2 fixture's two ledgered Tanks were already consistent. Its third Tank held `4200.000` litres without any movement history. Because synthetic movements are prohibited, that history-free sample Tank was corrected to `0.000` litres. PostgreSQL and H2 provisioning tests now reject both a nonzero Tank without movements and any mismatch between current stock and the highest `ledger_sequence` result. PostgreSQL executes the normal fixture twice, retaining idempotent movement counts and zero mismatches.

## Verification

- PostgreSQL/H2 provisioning regression: 2/2 PASS against `transport_logistics_acceptance`; clean Flyway V1→V65 plus normal sample provisioning PASS.
- Complete Fuel/Bunker/US-35/US-37 selection: 163/163 PASS; Bunker concurrency 10/10 PASS, including receipt versus issue.
- Complete Maven `verify`: 1,335 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 05:02.
- Architecture/Modulith: 46/46 PASS.
- Checkstyle, PMD, and SpotBugs: PASS; zero actionable findings.
- Frontend: TypeScript PASS; Vitest 263/263 across 63 files; production build PASS; US-35 scoped ESLint PASS.
- Fresh real PostgreSQL-backed Chromium: 6/6 PASS.
- Immediate post-E2E query: zero nonzero Tanks without movements and zero ledger-tail mismatches; ten PostgreSQL sample Tanks all at `24920.000`.
- Provider-fact immutability remains covered by the Chromium match/reversal journey and complete Maven suite.
- `git diff --check`: PASS.

## Scope and next task

No production Fuel/Fuel Card/Bunker logic, API, migration, V65 content, permission, event, or dependency changed. Authoritative PostgreSQL evidence used only `transport_logistics_acceptance`. Story accounting remains 68/87 complete and 19/87 remaining.

Next task: `US-35-FUEL-CARDS-FINAL-ACCEPTANCE-001-RERUN-2`.
