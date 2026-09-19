# US-87 CS01 — Domain and Signal Contracts

**Task:** `US-87-DETECT-USER-RISK-CS01-DOMAIN-SIGNAL-CONTRACTS-001`

**Status:** `COMPLETE`

**Flyway:** V105 unchanged

**Accounting:** 73 / 87 complete

## Approval provenance

On 2026-09-19, the project user approved the exact first-wave package at application commit
`c8e811f6c55ec91859680048c51b8e08b1ac4b35` and stated that they hold or have delegated Product,
Security, Privacy/records, Identity source-owner, and Architecture/data authority. This is a single user
approval statement; no separate reviewers or signatures are inferred.

The authorization is limited to the four Identity permission-ceiling actions, their single reason code,
three distinct facts in 15 minutes, MEDIUM review priority, five-minute lateness, advisory-only effect,
the minimized payload, duplicate/conflict behavior, reviewer separation, 180-day retention, and one
internal appeal within 30 days.

## Implemented scope

- Framework-neutral Identity-owned first-wave vocabulary and constants.
- Tenant-qualified effective-dated rule version with half-open time semantics.
- Exact minimized permission-ceiling denial fact with deterministic retry identity.
- Explicit duplicate, conflict, stale, unknown, and insufficient evidence semantics.
- Immutable MEDIUM advisory finding, review disposition, and internal appeal value objects.
- Narrow internal denied-signal output port without an adapter or runtime producer.

The signal excludes requested permission names, credentials, usernames, IP addresses, user agents, request
bodies, exception messages, and free-form allegations. It proves only that Identity rejected a requested
permission grant above the actor's current ceiling; it does not prove malicious intent, fraud, compromise,
misconduct, or successful privilege escalation.

## Verification

- `UserRiskFirstWaveDomainTest`: 8 / 8 pass.
- `UserRiskArchitectureTest`: 2 / 2 pass.
- Focused compilation and tests: 10 / 10 pass.
- Identity/security/architecture/Modulith regression: 92 / 92 pass against
  `transport_logistics_acceptance` at Flyway V105.
- Checkstyle: 0 violations; PMD: success; SpotBugs: 0 findings.
- Dependency analysis: build success with the repository's pre-existing starter/transitive dependency debt;
  CS01 adds no dependency.

Two attempted complete-suite runs exercised broad PostgreSQL/Kafka paths but did not produce a terminal
Maven summary: the first inherited the wrong Kafka endpoint and the corrected rerun stopped making progress
after unrelated migration-clean/scheduled-worker interference. Neither attempt reported a CS01 assertion
failure. CS01 closure relies on the passing focused and mandatory affected regression gates above and does
not claim a complete-suite pass.

## Explicit exclusions and next authorization

There is no persistence, migration, adapter, producer instrumentation, evaluation worker, API, permission,
frontend, Operations integration, or enforcement. Existing permission-ceiling behavior is unchanged.

The next proposed task is `US-87-DETECT-USER-RISK-CS02-PERSISTENCE-001`. It requires separate approval of
the exact Tenant-leading DDL, append-only and idempotency constraints, indexes, 180-day disposition
mechanism, concurrency behavior, rollback plan, and the next free Flyway migration number. No migration
number is reserved by CS01.
