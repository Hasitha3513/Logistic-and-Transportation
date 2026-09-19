# US-51 Monitor Idle Time — CS07 PostgreSQL/Kafka Performance and Recovery

**Task:** `US-51-MONITOR-IDLE-TIME-CS07-POSTGRES-KAFKA-PERFORMANCE-RECOVERY-001`
**Verdict:** COMPLETE
**Flyway head:** V105
**Story accounting:** 73 / 87 COMPLETE (unchanged)

## Scope and safety

This closes the approved CS07 technical boundary. It does not activate production powertrain
classification or a production `ENGINE_RUNNING` mapping, and it does not provide physical-device
acceptance. Destructive verification used only `transport_logistics_acceptance`.
`VehicleMasterConfig.java` was compared byte-for-byte with `HEAD`; its existing assume-unchanged
flag remains present and no content was changed.

## V105 authorization and DDL

V105 is a normal transactional Flyway migration containing exactly the authorized indexes:

```sql
CREATE INDEX idx_tracking_idle_state_keyset
    ON tracking_idle_state (tenant_id, latest_source_timestamp DESC, vehicle_id DESC);
CREATE INDEX idx_tracking_idle_episode_tenant_keyset
    ON tracking_idle_episode (tenant_id, start_source_timestamp DESC, id DESC)
    WHERE lifecycle <> 'CANDIDATE';
CREATE INDEX idx_tracking_telemetry_dispatch_idle_due
    ON tracking_telemetry_evaluation_dispatch
        (next_attempt_at, tenant_id, vehicle_id, source_timestamp, dispatch_id)
    WHERE evaluator_type = 'IDLE'
      AND status IN ('PENDING', 'FAILED', 'PROCESSING');
```

The definitions match production Tenant predicates, keyset ordering and due-work predicates.
Existing indexes and constraints remain intact. Clean V1→V105 and populated V104→V105 paths pass;
all three indexes are ready and valid.

Ordinary `CREATE INDEX` follows repository convention and preserves atomic Flyway rollback. It may
block writes to the indexed tables, so deployment requires a bounded maintenance window, drained
affected writers, lock/transaction observation, disk headroom and validation before traffic resumes.

## Query-plan evidence

The controlled workload used 10,000 state rows, 10,000 non-candidate episode rows and a
representative mixed dispatch workload across multiple Tenants/Vehicles. Planner features were not
disabled and probe transactions were rolled back.

| Query | Before V105 | After V105 |
| --- | --- | --- |
| State first page | sequential scan + top-N sort; 10,000 rows; 2.663 ms | keyset index scan; 0.099 ms |
| State subsequent page | full scan/sort | keyset index scan; 0.083 ms |
| Episode first page | sequential scan + top-N sort; 10,000 rows; 2.735 ms | keyset index scan; 0.059 ms |
| Episode subsequent page | full scan/sort | keyset index scan; 0.082 ms |
| IDLE due dispatch | sequential scan + top-N sort; 4.270 ms | bitmap index path, no full-table sequential scan; 0.453 ms |

Dispatch data included due work, future retries, completed work, expired processing leases and active
leases. Existing evidence and retention paths were already indexed (0.047 ms and 0.262 ms), so no
additional index was added.

## Concurrency, recovery and performance

Focused real PostgreSQL/Kafka verification proves Tenant-qualified claims, concurrent candidate
evaluation, one-open-episode convergence, immutable duplicate-safe evidence, atomic promotion,
transaction rollback, Kafka redelivery, expired-lease recovery, active-lease protection, stale-owner
rejection and replay after effects commit but before dispatch completion. Duplicate observations do
not double-credit duration or recovery. V1/V2 remain ineligible.

The focused selection passed **26/26**. No deadlock, leak, duplicate logical outcome, probe row, idle
transaction or unexpected waiting lock remained.

Fresh local acceptance-stack API measurements used 30 warmed same-Tenant authorized samples per
route: state-page p95 **12.676 ms** and episode-history p95 **13.255 ms**. The unchanged canonical
Kafka→Timescale pipeline retains its previously recorded technical baseline of 576.6 durable events/s
sustained and 4,693.7 events/s burst. That throughput is reused regression evidence; CS07 reran its
publication, persistence, redelivery and dispatch behavior but not a new capacity certification.
These local figures are neither production guarantees nor physical-device evidence.

## Verification

- Focused V105, idle concurrency/recovery and Kafka: **26/26 PASS**.
- Complete backend: **1,952 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**.
- Architecture/Modulith: **59/59 PASS**.
- Checkstyle, PMD, SpotBugs: PASS; dependency analysis completed with existing warnings.
- Frontend Vitest: **87 files / 346 tests PASS**.
- TypeScript, production build, changed-file ESLint: PASS; existing bundle warning remains.
- Real PostgreSQL-backed Chromium idle continuity: **4/4 PASS**.
- Docker Compose validation and `git diff --check`: PASS.

## Remaining limitations and next queue

Production Fleet eligibility remains `UNKNOWN`; production Flespi, Traccar and Generic
engine-running mappings remain disabled. Physical capture, privacy review and operator sign-off
remain mandatory. CS07 does not complete US-51.

Exact next queue: `US-51-MONITOR-IDLE-TIME-TECHNICAL-CLOSURE-001`.
