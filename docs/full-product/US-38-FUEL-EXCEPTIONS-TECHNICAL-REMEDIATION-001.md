# US-38 Fuel Exceptions Technical Remediation

Status: `COMPLETE`
Story status: `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`
Migration: `V67__us38_technical_remediation.sql`
Accounting: 69 / 87 complete; 18 / 87 remaining (unchanged)

## Remediation result

V67 retains V66 and all earlier migrations unchanged while adding governed owner/request evidence, correction execution state and idempotency identity, append-only correction attempts, stable negative-balance source-event identity, and durable Operations handoff attempt state. Correction failures persist a safe error and immutable failed attempt; the same correction can retry, while retry after success is an auditable no-op. CRITICAL resolution requires a published or accepted handoff. Notes accept exactly 2,000 characters and reject 2,001.

The test-only `e2e` adapter can fail the first marked owner command and handoff. Neither adapter is active outside the `e2e` profile. Read-only acceptance observability is Node-side, rejects databases other than `transport_logistics_acceptance`, validates identifiers before fixed SELECTs, and is unavailable to browser code and public APIs.

## Deterministic PostgreSQL concurrency

Nine barrier/latch races pass on PostgreSQL: duplicate manual case, duplicate automatic negative-balance case, two reviewers, approve versus reject, double approve, resolve versus correction request, double escalation, handoff retry, and owner-command retry. Each race proves one state-changing winner and stable database uniqueness/optimistic-version behavior without sleeps.

## Real owner and immutability evidence

- Fuel Issue: a real submitted issue is cancelled only through the approved Fuel owner command; voucher, fuel type, quantity, unit price, total and issue time remain unchanged.
- Fuel Purchase and Price: historical P1 and its purchase monetary facts remain unchanged; approval creates a later P2 row rather than overwriting P1.
- Fleet reading: the original ODOMETER fact remains unchanged and Fleet creates a new compensating reading linked by `correctionOfReadingId`.
- Bunker: rejection leaves Tank stock, movement count and ledger tail at N; the next valid adjustment creates exactly one movement at N+1 and the Tank matches the ledger tail.
- Fuel Card: a case references a persisted US-35 indicator ID; provider transaction ID, canonical hash and local status remain unchanged.
- Handoff: failed publication persists; retry reuses the handoff row and event ID and creates exactly one Operations case.
- Owner execution: immutable attempts are `FAILED`, `SUCCESS`, then `NOOP_REPLAY`, with one idempotency key and exactly one effective success.

PostgreSQL sample seeding now advances `fuel_voucher_sequence` beyond seeded voucher business keys so ordinary post-startup Fuel Issue creation is deterministic and rerunnable.

## Final verification

- PostgreSQL focused concurrency/regression: 16/16 PASS; targeted cascade reproduction: 31/31 PASS; Flyway V1→V67 PASS.
- Full Maven: 1,356 tests, 0 failures, 0 errors, 15 skipped — `BUILD SUCCESS` in 05:34.
- Architecture: 46/46 PASS.
- Checkstyle: 0 violations; PMD: PASS; SpotBugs: 0 findings/0 errors.
- TypeScript and production build: PASS; Vitest: 263/263 PASS; changed-file ESLint: PASS.
- Strengthened real PostgreSQL-backed Chromium: 6/6 PASS in 35.7 seconds.
- `git diff --check`: PASS.

No public API, route, permission, product semantic, second outbox, dependency, or migration beyond V67 was added. The development database supplied no authoritative evidence. One discarded mixed-profile Maven invocation contacted it and V65 failed closed; accepted evidence is exclusively from `transport_logistics_acceptance`.

Next task: `US-38-FUEL-EXCEPTIONS-TECHNICAL-CLOSURE-001-RERUN`.
