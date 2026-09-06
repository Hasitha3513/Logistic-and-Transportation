# US-38 Acceptance Observability Authorization

Decision: `APPROVED`  
Classification: `NON-STORY TECHNICAL GOVERNANCE / ACCEPTANCE OBSERVABILITY AUTHORIZATION`  
Observability model: `REAL CHROMIUM + READ-ONLY ACCEPTANCE DATABASE ASSERTION`  
Acceptance database: `transport_logistics_acceptance` only  
Current Flyway head: `V67`  
US-38: `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
Accounting: 69 / 87 complete; 18 / 87 remaining (unchanged)

## Decision

Real PostgreSQL-backed Chromium acceptance may supplement its normal user and API workflow with read-only queries against the same `transport_logistics_acceptance` database. This test-harness observability is authorized only for server-owned identities that intentionally remain absent from public contracts.

The harness may read the persisted negative-Bunker `source_event_id` before and after replay, assert that the active matching case count remains one, and verify unchanged Tank stock, movement count, and ledger sequence. It may similarly compare the immutable handoff event ID across failure and retry, count the resulting Operations case, and inspect correction-attempt rows for stable idempotency, one effective success, and subsequent `NOOP_REPLAY` behavior.

Chromium must still perform every business action through the existing application/API workflow. Database access is supplemental verification only and may not create, update, delete, repair, or invoke behavior. Test output should report match results or redacted hashes rather than unnecessarily disclose internal identifiers.

## Security and isolation

- Database access remains Node/test-harness-side and must never enter the browser bundle or production application code.
- Credentials come from the existing acceptance environment, are not hardcoded, and are not logged.
- The harness must fail closed unless the configured database is exactly `transport_logistics_acceptance`.
- Development and production databases are excluded.

## Contract preservation

The public API remains unchanged: `sourceEventId`, handoff event ID, and correction idempotency keys remain non-public. No public or diagnostic route, permission, migration, runtime frontend dependency, product semantic, event contract, outbox, or story-accounting change is authorized. V67 remains the Flyway head.

Next task: continue `US-38-FUEL-EXCEPTIONS-TECHNICAL-REMEDIATION-001`, then run `US-38-FUEL-EXCEPTIONS-TECHNICAL-CLOSURE-001-RERUN` after every remediation gate passes.
