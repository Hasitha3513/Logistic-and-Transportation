# US-54 Dashboard — V95 Speed Query Index Hardening

## Verdict

`PASS — V95_APPLIED_AND_VERIFIED`

V95 adds exactly one Tenant-leading B-tree index matching the production recent-speed-episode dashboard query:

```sql
CREATE INDEX idx_tracking_speed_episode_dashboard_recent
    ON tracking_speed_episode (
        tenant_id,
        confirmation_source_timestamp DESC,
        id DESC
    );
```

No predicate or INCLUDE columns are required: `confirmation_source_timestamp` is non-null and the query reads the episode projection from the heap. V1–V94 checksums remain unchanged.

## PostgreSQL acceptance

- Clean V1→V95 and explicit V94→V95 migration: PASS.
- Transactional failure rollback and safe retry: PASS; no partial index or V95 history row remains after failure.
- Flyway records V95 exactly once.
- `indisready = true`, `indisvalid = true`; zero invalid indexes.
- Existing data and Tenant isolation are preserved.
- Acceptance database: `transport_logistics_acceptance` only.

The representative workload contained 12,000 incidents (10,000 Tenant A, 2,000 Tenant B), distributed across 50 and 20 vehicles. The production query returned the same ordered 20 rows before and after indexing, including the `confirmation_source_timestamp DESC, id DESC` tie-breaker.

| Evidence | Before | After |
| --- | ---: | ---: |
| Plan | sequential scan + top-N sort | index scan |
| Rows scanned/returned | 12,000 / 20 | 20 / 20 |
| Rows removed | 5,081 | 0 |
| Buffer hits | 343 | 3 |
| Execution time | 2.303 ms | 0.032 ms |

Timings describe this technical environment and are not a production SLO.

## Deployment and rollback

Apply V95 in a controlled maintenance window: announce maintenance; drain speed-evaluation writers while retaining Kafka backlog; verify no long-running conflicting transaction and adequate disk; deploy through normal Flyway startup; monitor locks, CPU, I/O and disk; require V95 plus a ready/valid index; run a Tenant-scoped smoke query; restore traffic; monitor latency, errors and backlog recovery.

Ordinary `CREATE INDEX` can block affected writes during construction. Reads follow normal PostgreSQL MVCC behavior. Before commit, migration failure rolls back the index and Flyway history atomically. After successful deployment, V95 is immutable; removal requires a separately reviewed forward migration dropping only this index.
