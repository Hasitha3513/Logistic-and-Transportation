# US-46 Process Driver Payroll Link Implementation

**Task:** `US-46-DRIVER-PAYROLL-LINK-IMPLEMENTATION-001`  
**Result:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**Accounting:** 70 / 87 complete; 17 / 87 remaining (unchanged)  
**Next task:** `US-46-DRIVER-PAYROLL-LINK-TECHNICAL-CLOSURE-001-RERUN`

## Implemented boundary

Driver payroll input is implemented as a feature-first slice inside the existing Fleet module. Driver owns tenant-scoped regular and correction batches, completed/closed Trip source validation through the published Trip port, deterministic monetary calculation, worker-reference mapping, preparer/approver segregation, immutable release snapshots, local history, and a durable export request. Payroll/HRMS remains authoritative for employee master, salary, tax, pension, payslips, payment, settlement, and posting.

The implementation supports exactly `TRIP_EARNING`, `ALLOWANCE`, `OVERTIME`, and `DEDUCTION`; `DRAFT`, `VALIDATED`, `APPROVED`, `EXPORT_REQUESTED`, `EXPORTED`, and correction supersession semantics; ISO-4217 single-currency batches; scale-2 `HALF_UP` money; non-negative stored deductions subtracted once; caller-supplied period and cutoff; optimistic locking; tenant-scoped idempotency keys; and stable release event identities. Released lines are protected by a PostgreSQL immutability trigger. Draft duplicate preparation remains possible, while validation rejects duplicate source contributions already present in a non-superseded released batch.

## Integration and security

`DriverPayrollInputExportRequestedV1` is stored through the shared P1-01 durable outbox in the Driver transaction. Integration recognizes `DRIVER_PAYROLL_INPUT_V1` as `FINANCIAL_CONFIDENTIAL`, accepts only outbound `FILE_JSON_V1`, applies declarative mapping, and produces deterministic UTF-8 JSON file/hash evidence. The payload is capped at 32 KiB and excludes names, contact details, medical, licence, drug-test, bank, tax, pension, salary, credentials, and raw notes.

The four tenant-scoped permissions are `DRIVER_PAYROLL_VIEW`, `DRIVER_PAYROLL_PREPARE`, `DRIVER_PAYROLL_APPROVE`, and `DRIVER_PAYROLL_EXPORT`. The preparer cannot approve the same batch. Security rules cover `/drivers`, `/v1/drivers`, and the literal `/api/v1/drivers` deployment URL.

## Public API and UI

The API base is `/api/v1/drivers`. It provides list/create/get, replace lines, validate, approve, export, create correction, history, and get/update worker-mapping operations. Mutating create/mapping commands require `Idempotency-Key`; no generic status mutation exists.

The Driver Payroll page runs inside `AppLayout`, uses the shared API client and TanStack Query, exposes permission-aware actions, shows provisional operational totals and safe export status, and does not present payroll settlement claims.

## Database

- `V68__driver_payroll_input_us46.sql`: batches, lines, mappings, history, constraints, indexes, immutability trigger, permissions, and Integration classification.
- `V69__driver_payroll_admin_role_permissions.sql`: idempotent administrative role grants.
- `V70__driver_payroll_released_source_scope.sql`: released-source lookup indexes aligned with lifecycle-scoped duplicate validation.
- `V71__driver_payroll_mapping_idempotency.sql`: tenant-leading durable worker-mapping command idempotency and append-only enforcement.
- US-46 migrations: V68–V71. Current Flyway head: V71.

All authoritative PostgreSQL verification used only `transport_logistics_acceptance`; the development database was not used.

## Verification evidence

- Full Maven: 1,359 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 05:44.
- Flyway: V1 through V70 on `transport_logistics_acceptance`.
- Architecture and Modulith: 46 / 46 pass.
- Checkstyle: 0 violations.
- PMD: pass with no current findings.
- SpotBugs: 0 findings.
- TypeScript: pass.
- Vitest: 263 / 263 pass across 63 files.
- Production build: pass; only the pre-existing bundle-size advisory remains.
- Changed-file ESLint: pass.
- Real PostgreSQL-backed Chromium: 6 / 6 pass in 32.0 seconds, covering calculations/source facts, mapping/validation, approval SoD, stable export replay and real file evidence/privacy, immutable correction, and literal API RBAC/Tenant isolation.
- `git diff --check`: pass (line-ending conversion notices only).

## V71 technical remediation evidence

`US-46-DRIVER-PAYROLL-LINK-TECHNICAL-REMEDIATION-001` completed the authorized V71-only remediation without changing the public API, permissions, outbox, module boundary, or story accounting. PostgreSQL advisory transaction locks now serialize batch idempotency and released-source approval claims; correction creation is atomic and replay-safe; correction lines are Tenant/source validated; `SUPERSEDED` remains part of released-source exclusion; and controlled JSON delivery re-canonicalizes PostgreSQL JSONB before writing so the delivered UTF-8 bytes exactly match the stored SHA-256 evidence.

Fresh final evidence used only `transport_logistics_acceptance`: dedicated PostgreSQL structure/append-only/idempotency/atomicity/isolation tests passed 18/18, including the deterministic concurrency matrix 9/9; clean Flyway V1 through V71 passed; full Maven passed 1,380 tests with 0 failures, 0 errors, and 15 skipped in 06:20; architecture passed 46/46; Checkstyle reported 0 violations; PMD passed; SpotBugs reported 0 findings; TypeScript, production build, changed-file ESLint, and Vitest 263/263 passed; and real PostgreSQL-backed Chromium passed 7/7 in 38.3 seconds, including exact file hash/canonical JSON and browser-level rejection of a payload over 32 KiB. `git diff --check` passed.

US-46 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`. Accounting remains 70/87 complete and 17/87 remaining. The next task is `US-46-DRIVER-PAYROLL-LINK-TECHNICAL-CLOSURE-001-RERUN`.

No new dependency, top-level module, second outbox, live HRMS, acknowledgement flow, payroll engine, tax/pension engine, payment/banking, or general-ledger behavior was introduced. Application changes were not committed or pushed.
