# US-46 Driver Payroll Link Final Acceptance

**Task:** `US-46-DRIVER-PAYROLL-LINK-FINAL-ACCEPTANCE-001`  
**Decision:** `COMPLETE / FINAL_ACCEPTANCE_PASS`  
**Owner:** Driver within the existing Fleet module  
**Flyway:** US-46 V68–V71; current repository head V71  
**Accounting:** 71 / 87 complete; 16 / 87 remaining  
**Next task:** `US-47-TRANSPORT-BILLING-PRODUCT-DECISIONS-001`

## Independent decision

US-46 satisfies its frozen source intent as an operational Driver payroll-input preparation and controlled export link. Payroll/HRMS remains authoritative for employee master, final payroll calculation, salary runs, tax, pension, benefits, payslips, payment, settlement, and posting. No payroll engine, salary engine, automatic punitive deduction, bank/payment capability, general ledger, live HRMS, inbound acknowledgement, or attendance engine was introduced.

The accepted implementation preserves exactly four categories (`TRIP_EARNING`, `ALLOWANCE`, `OVERTIME`, `DEDUCTION`), caller-supplied period/cutoff, same-Tenant Completed/Closed Trip eligibility, `BigDecimal` scale-2 `HALF_UP` calculations, one ISO-4217 currency, non-negative deductions subtracted once, and the term `provisionalNetInput`. Regular and correction lifecycles, independent approval, immutable releases, durable worker-mapping idempotency, Tenant isolation, source immutability, P1-01 atomicity, truthful `EXPORTED` projection, and controlled US-73 file/hash evidence pass.

## Fresh final-acceptance evidence

- PostgreSQL closure suite: 18 / 18 PASS in 01:54, including clean Flyway V1→V71, V71 structure/append-only enforcement, released-line/history immutability, Tenant-B, source/config immutability, mapping replay/conflict/new-key behavior, outbox commit/rollback, and delivery replay.
- Deterministic PostgreSQL concurrency matrix: 9 / 9 PASS.
- Full Maven: 1,380 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 06:21.
- Architecture and Spring Modulith: 46 / 46 PASS in 25.339 seconds.
- Checkstyle: 0 violations.
- PMD: PASS with no current findings.
- SpotBugs: PASS with no findings.
- TypeScript: PASS.
- Vitest: 263 / 263 PASS across 63 files in 64.78 seconds.
- Production build: PASS in 6.49 seconds; only the existing bundle-size advisory remains.
- US-46 changed-file ESLint: PASS; `US46_INTRODUCED_LINT_ERRORS = 0`.
- Global ESLint debt remains 71 unrelated pre-existing Delivery errors.
- Fresh real PostgreSQL-backed Chromium: 7 / 7 PASS in 38.2 seconds, including real Driver/Trip/four-category totals, mapping requirement and snapshot, independent approval, actual canonical file/hash/privacy, `EXPORTED`, correction/supersession/original immutability, Tenant/RBAC denial, replay, and browser-level 32-KiB validation rejection.
- `git diff --check`: PASS.

`DEVELOPMENT_DATABASE_AUTHORITATIVE_EVIDENCE = NO`. The historical excluded invocation that contacted `transport_logistics` performed Flyway discovery/repair only. Every authoritative final-acceptance database result used `transport_logistics_acceptance`.

## Scope and state

Public API change: none. New permissions: none. Second outbox/inbox: none. Migration beyond V71: none. Foreign repository/SQL/FK: none. Distributed transaction: none. Scope leakage: none.

US-46 is complete. Wave B remains open with US-47 remaining. Accounting advances exactly once to 71 / 87 complete and 16 / 87 remaining; 71 + 16 = 87.
