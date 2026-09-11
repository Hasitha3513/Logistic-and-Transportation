# US-38 Fuel Exceptions Technical Closure

**Task:** `US-38-FUEL-EXCEPTIONS-TECHNICAL-CLOSURE-001-RERUN`
**Decision:** `PASS / TECHNICAL_CLOSURE_COMPLETE`
**Date:** 2026-09-06
**Owner:** Fuel
**Story status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`
**Migration:** V67; Flyway head V67
**Program accounting:** 69 / 87 complete; 18 / 87 remaining (unchanged)
**Next task:** `US-38-FUEL-EXCEPTIONS-FINAL-ACCEPTANCE-001`

## Independent closure decision

The frozen six-category Fuel exception contract and authorized V67 remediation pass independent source, PostgreSQL, concurrency, security, architecture, frontend, and real-browser review. The exact taxonomy, safe review language, same-Tenant manual source validation, negative-Bunker-only automatic creation, active-case dedupe, five-state lifecycle, five resolution outcomes, independent approval, US-35 importer/reconciler separation, and bounded append-only evidence/history remain intact.

V67 supplies the authorized correction owner/request/before-state evidence, stable idempotency identity, immutable `FAILED`/`SUCCESS`/`NOOP_REPLAY` attempts, stable negative source-event identity, durable handoff failure/retry evidence, and CRITICAL handoff precondition. Database triggers reject update/delete of correction-attempt and case-history rows. V1–V66 and the public API remain unchanged; no migration beyond V67 exists.

## Fresh hard-gate evidence

- Replayed rejected negative Bunker commands retain one stable `sourceEventId` and one active case. Rejection leaves Tank stock, movement count, and ledger tail unchanged; the next valid adjustment is exactly N+1 and restores the Tank/tail equality invariant.
- Controlled owner execution persists `FAILED`, retries to `SUCCESS`, and records post-success `NOOP_REPLAY` under one idempotency key with one effective owner action. The case remains `CORRECTION_PENDING` after failure and the source fact remains unchanged.
- CRITICAL resolution is denied before handoff and after failed handoff; published/accepted handoff satisfies the frozen precondition. Retry retains the same handoff row, event ID, and source-event identity and creates exactly one Operations case.
- Real owner paths preserve the original Fuel Issue economic facts, historical P1 Fuel Price and purchase monetary facts, original Fleet ODOMETER reading, Bunker movement history, and US-35 provider transaction identity/hash/status. Price correction creates later effective-dated P2; Fleet creates a compensating correction fact; Bunker uses its canonical adjustment command.
- Emergency refuel requires a real same-Tenant Vehicle and Trip or Driver; nonexistent and foreign-Tenant identities are rejected. Tenant B case/source/retry/history access and limited-user mutation are denied.
- Notes accept 2,000 characters and reject 2,001. Exactly the five frozen Fuel-exception permissions exist. Test-only observability rejects every database except `transport_logistics_acceptance`, uses fixed read-only SQL from Node, carries no hardcoded/logged credential, and is not browser-bundled or exposed through a public route.
- Fuel reuses `OperationalExceptionFactV1`, the shared `DurableEventPublisher`, and `integration_outbox_event` with at-least-once semantics. Category mapping and minimized metadata are unchanged; there is no exactly-once/global-ordering claim, direct Notification dispatch, foreign persistence access, second outbox, or inbox.

`HISTORICAL_SOURCE_MUTATED = NO`.

## Fresh verification evidence

- Focused US-38: 21 tests, 0 failures, 0 errors, 0 skipped; includes PostgreSQL structural acceptance and 9/9 deterministic races. Clean restoration reached V1→V67.
- US-35, US-37, Bunker, US-78, P1-01, US-38 concurrency, and Notification cascade regression matrix: 112 tests, 0 failures, 0 errors, 0 skipped.
- Complete Maven: 1,356 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 05:37.
- Architecture and Spring Modulith: 46/46 PASS.
- Checkstyle: 0 violations; PMD: PASS; SpotBugs: 0 findings and 0 errors.
- Frontend: TypeScript PASS; production build PASS; 63 Vitest files / 263 tests PASS; US-38 changed-file ESLint PASS. The existing Vite large-chunk warning is global non-blocking debt.
- Fresh real PostgreSQL-backed Chromium: 6/6 PASS in 35.2 seconds using current source and supplemental read-only acceptance observations.
- `git diff --check`: PASS.

One discarded focused invocation used an incorrect local acceptance-database password and failed authentication before any assertion; its ApplicationContext and static-initializer errors were downstream cascades. It did not contact the development database and is excluded. A historical discarded mixed-profile remediation invocation did contact the development datasource, where V65 failed closed and rolled back. `DEVELOPMENT_DATABASE_AUTHORITATIVE_EVIDENCE = NO`; every fresh authoritative closure result used only `transport_logistics_acceptance`.

## Scope containment

No new public API, route, permission, product semantic, payment/settlement capability, provider authority, fraud/workflow/retry engine, Notification engine, binary storage, foreign repository/SQL/FK, distributed transaction, source rewrite, second outbox/inbox, or migration beyond V67 was introduced. Technical blockers are zero. US-38 is not accepted by this technical closure and story accounting does not advance.
