# US-46 Driver Payroll Link Technical Remediation

**Task:** `US-46-DRIVER-PAYROLL-LINK-TECHNICAL-REMEDIATION-001`  
**Result:** `COMPLETE`  
**Story status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**Accounting:** 70 / 87 complete; 17 / 87 remaining (unchanged)  
**Next task:** `US-46-DRIVER-PAYROLL-LINK-TECHNICAL-CLOSURE-001-RERUN`

## Remediation delivered

The authorized V71-only remediation is complete. Durable worker-mapping commands are tenant-scoped, append-only, and replay-safe. PostgreSQL transaction advisory locks serialize batch creation and released-source approval claims. Correction creation is atomic and idempotent; correction lines must reference an original line belonging to the same Tenant and predecessor batch. Released-source exclusion includes superseded releases. Integration restores deterministic canonical JSON from PostgreSQL JSONB before controlled-file delivery so the exact UTF-8 bytes match the recorded SHA-256 hash.

No new public API, route, permission, outbox, top-level module, external acknowledgement, payroll calculation, or migration beyond V71 was introduced. Historical migrations remain unchanged.

## Final verification evidence

- Flyway V1 through V71: pass on `transport_logistics_acceptance`.
- PostgreSQL V71 structure, append-only, idempotency, atomicity, Tenant isolation, and source immutability: 18 / 18 pass.
- Deterministic PostgreSQL concurrency matrix: 9 / 9 pass.
- Full Maven: 1,380 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 06:20.
- Architecture and Modulith: 46 / 46 pass.
- Checkstyle: 0 violations.
- PMD: build success, no current reported findings.
- SpotBugs: 0 findings.
- TypeScript: pass.
- Vitest: 263 / 263 pass across 63 files.
- Production build: pass; only the pre-existing bundle-size advisory remains.
- Changed-file ESLint: pass.
- Real PostgreSQL-backed Chromium: 7 / 7 pass in 38.3 seconds, including exact file/hash/canonical JSON evidence, correction traceability, literal API RBAC/Tenant isolation, and browser-level rejection of a canonical payload over 32 KiB.
- `git diff --check`: pass.

One later restricted-runner diagnostic invocation was excluded: the sandbox denied Byte Buddy self-attachment and localhost PostgreSQL connectivity. It is not application evidence and did not contact the development database. All authoritative PostgreSQL evidence used only `transport_logistics_acceptance`.

`DEVELOPMENT_DATABASE_AUTHORITATIVE_EVIDENCE = NO`. An earlier excluded invocation contacted `transport_logistics` only for Flyway discovery/repair checking; no accepted result came from it and no development data was changed.

## Outcome

Technical remediation is complete. US-46 is not accepted or complete yet and story accounting does not change. Proceed only to `US-46-DRIVER-PAYROLL-LINK-TECHNICAL-CLOSURE-001-RERUN`.
