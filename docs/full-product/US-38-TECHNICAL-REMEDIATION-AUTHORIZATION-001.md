# US-38 Technical Remediation Authorization

Decision: `APPROVED`  
Classification: `NON-STORY TECHNICAL GOVERNANCE / REMEDIATION AUTHORIZATION`  
Current Flyway head: `V66`  
Authorized forward migration: `V67` (confirmed free)  
US-38: `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
Accounting: 69 / 87 complete; 18 / 87 remaining (unchanged)

## Authorization

V66 and V1–V65 remain immutable. One forward-only V67 migration is authorized solely to bring the existing US-38 implementation into conformance with its frozen contract. This authorization creates no product semantics, route, permission, owner, workflow, outbox, or story-accounting change.

V67 may extend `fuel_exception_correction` with mandatory owner module/type, requested command type, safe request evidence/reference, safe before-snapshot hash, request reason, request and execution state, and optimistic-version fields. It may add one Fuel-owned append-only correction-attempt table containing Tenant, correction, attempt number, stable idempotency key, attempt actor/time, `SUCCESS | FAILED | NOOP_REPLAY`, safe result reference/error code and optional result hash. Tenant-consistent foreign keys and non-redundant Tenant-leading indexes are authorized.

V67 may complete negative-balance `source_event_id` constraints and handoff failure/retry persistence, including stable immutable event identity, attempt/result timestamps and fields required to preserve `PENDING | PUBLISHED | ACCEPTED | FAILED`. Existing P1-01 durable publishing and Operations deduplication must be reused; no second outbox/inbox or generic retry engine is authorized.

## Frozen remediation behavior

- One correction has one stable owner-command idempotency identity. A successful correction is never executed twice; retry after success returns the prior safe result as `NOOP_REPLAY`.
- Every owner-command attempt is append-only. Failed attempts remain visible while the case stays `CORRECTION_PENDING`.
- A rejected negative-Bunker command receives a stable source-event identity; duplicate processing creates no second active case.
- Handoff failure persists `FAILED`; explicit retry reuses the same handoff/event/source-event identity, appends history and creates no duplicate Operations case.
- Emergency refuel requires a real same-Tenant Vehicle and at least one real same-Tenant Trip or Driver.
- Notes permit at most 2,000 characters.
- A CRITICAL case cannot resolve until handoff is `PUBLISHED` or `ACCEPTED`.
- Evidence, note, correction request/review/result, handoff create/fail/retry/accept and resolution actions append immutable history.

## Mandatory remediation verification

The remediation must replace false-positive evidence with deterministic PostgreSQL and Chromium proof for effective-dated Fuel Price preservation, actual US-35 indicator origin, real emergency attribution, source immutability, negative-ledger safety, correction/handoff idempotency and failure retry. Concurrency must cover duplicate manual/automatic creation, two reviewers, approve-versus-reject, double approve, resolve-versus-correction, double escalation, handoff retry and owner-command retry without sleep-based correctness.

PostgreSQL acceptance must use only `transport_logistics_acceptance` and cleanly migrate V1 through V67. No development database is authoritative evidence.

## Exclusions and rollback

No payment, settlement, provider authorization/blocking, generic fraud or workflow engine, punitive action, automatic case creation beyond negative balance, foreign persistence/SQL, distributed transaction, raw-source rewrite, historical migration edit, new public route, or second outbox is authorized. If remediation fails, retain existing rows, disable affected commands if necessary, and correct only with another explicitly authorized forward migration.

Next task: `US-38-FUEL-EXCEPTIONS-TECHNICAL-REMEDIATION-001`.
