# US-51 CS04 V103 Candidate-Persistence Prerequisite

**Parent task:** `US-51-MONITOR-IDLE-TIME-CS04-EVALUATOR-001`

**Status:** `PREREQUISITE_PERSISTENCE_COMPLETE / EVALUATOR_PENDING`

**Flyway head:** V103
**Accounting:** 73/87 complete

## Scope and boundary

This verified intermediate slice corrects the V102 pre-confirmation model without claiming CS04
completion. A candidate can now survive restart without an idle episode. Normal `IDLE` dispatch
claiming remains disabled until the approved evaluator exists. V104 remains reserved for the
unchanged CS05 permission seed.

Fleet publishes `VehiclePowertrainEligibilityQuery`. Its production adapter deliberately returns
`UNKNOWN`, because Fleet has no authoritative effective-dated powertrain classification. Controlled
tests may supply explicit source-time classifications; no production classification is inferred or
seeded.

## V103 persistence

V103 adds candidate identity, reference-history and recovery timestamps to `tracking_idle_state`,
links confirmed episodes to their stable candidate identity, and creates
`tracking_idle_candidate_evidence`. Evidence is Tenant-qualified, append-only for 180 days,
deterministically deduplicated and contains only minimized evaluation facts. Retention cleanup may
delete rows only after `retain_until`.

V102 candidate rows are upgraded losslessly: their existing provisional episode identity becomes
the stable candidate identity and the original rows remain intact. No candidate is deleted,
reinterpreted as confirmed or fabricated during migration. New candidates use no episode until
atomic promotion creates one confirmed episode and updates state in the same transaction.

## Verification

- Fleet production lookup is fail-closed and Tenant/source-time explicit.
- Fresh V1→V103 and populated V102→V103 migration paths pass.
- Candidate save/reload survives adapter recreation.
- Discard removes candidate state while retaining immutable evidence and creates no episode.
- Promotion creates one confirmed episode linked to the stable candidate identity.
- Duplicate evidence is idempotent; retained evidence rejects update/delete.
- Expired evidence can be purged in bounded batches.
- Existing V102 idle persistence and evaluator-dispatch lease tests remain green.
- Current-head assertions were advanced to V103; historical V102 fixtures remain pinned to V102.

Verified on 2026-09-18 with Java 21 against the explicitly confirmed
`transport_logistics_acceptance` database and isolated Docker Testcontainers:

- prerequisite-focused PostgreSQL/dispatch/Fleet: 13/13 PASS;
- PostgreSQL production invariants: 16/16 PASS;
- architecture/Modulith: 59/59 PASS;
- complete Maven test: 1,935/1,935 PASS, zero failures/errors/skips;
- Checkstyle: zero violations; SpotBugs: zero findings;
- dependency analysis: BUILD SUCCESS with the repository's existing declared/used warnings;
- PMD: the global gate reports 32 pre-existing unrelated `UnnecessaryImport` findings and no
  finding in a changed production file;
- Docker Compose validation and `git diff --check`: PASS.

The host JDK required the existing Byte Buddy agent to be preloaded for Mockito inline mocking.
The initial runs without it, and without explicit acceptance-database/Docker access, were discarded
as environment failures; no development database was reset or modified.

## Remaining CS04 work

Implement the approved D1-D5 evaluator, controlled powertrain fixtures, deterministic continuity,
conflict and recovery logic, then enable normal IDLE claiming only alongside that functioning
evaluator. Production powertrain eligibility remains `UNKNOWN`, production engine-running mappings
remain disabled and physical acceptance remains pending.
