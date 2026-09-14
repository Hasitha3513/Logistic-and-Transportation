# US-52 CS07 V92 Maintenance-Window Index Hardening

## Verdict

PASS. V92 is a normal transactional Flyway migration for a controlled maintenance window. It does not use `CONCURRENTLY`, has no sidecar, changes no data or constraints, and creates only the two approved B-tree indexes.

## Exact DDL

```sql
CREATE INDEX idx_tracking_route_deviation_episode_keyset
    ON tracking_route_deviation_episode (
        tenant_id,
        vehicle_id,
        start_source_timestamp DESC,
        id DESC
    );

CREATE INDEX idx_trip_tenant_vehicle_source_assignment
    ON trip (
        tenant_id,
        vehicle_id,
        actual_start_time DESC,
        id DESC
    )
    INCLUDE (
        actual_end_time,
        status,
        driver_id,
        route_id,
        route_version
    )
    WHERE actual_start_time IS NOT NULL
      AND status NOT IN ('CANCELLED', 'REJECTED');
```

The first index matches Tenant/Vehicle keyset history ordering. The second matches the Trip-owned source-time assignment lookup, preserves immutable route/version attribution, and covers its returned projection. Both lead with `tenant_id`; neither duplicates an existing physical design.

## Migration acceptance

- Clean V1 to V92: PASS through the normal Flyway/application path.
- V91 to V92: PASS.
- V92 history row: exactly one successful row.
- V1-V91: unchanged.
- Index metadata: exact names, keys, descending order, INCLUDE list and partial predicate verified.
- `indisready=true` and `indisvalid=true` for both indexes.
- Transactional failure probe: an intentional second-statement failure rolled back the preceding index creation, left no invalid index, and left migration state retryable.
- JPA/schema validation: PASS.
- Post-run activity: zero idle-in-transaction sessions and zero lock waiters.

## Query-plan evidence

Acceptance data used 10,000 rows with Tenant/Vehicle selectivity and transactionally rolled-back probes. `enable_seqscan` was not disabled.

| Query | Before V92 | After V92 |
| --- | --- | --- |
| Episode keyset page | sequential scan of 10,000 rows, top-N sort, 371 buffer hits, 1.854 ms execution | index-only scan on `idx_tracking_route_deviation_episode_keyset`, no sequential scan, no sort, 6 buffer hits, 0.108 ms planning, 0.125 ms execution |
| Trip source-time assignment | 10,002 Vehicle-index entries examined, 9,940 removed, candidate sort, 395 buffer hits, 2.417 ms execution | index-only scan on `idx_trip_tenant_vehicle_source_assignment`, one result, no explicit sort, 4 buffer hits, 0.168 ms planning, 0.077 ms execution |

Results were equivalent before and after indexing. Tenant predicates remained mandatory; foreign-Tenant lookups returned safe absence. Timings describe the local acceptance environment and are not production SLOs.

## Locking and deployment

Ordinary `CREATE INDEX` takes a `SHARE` lock on its table. Reads remain available under normal PostgreSQL MVCC behavior, while conflicting writes wait until each build commits. This blocking is explicitly accepted only inside the controlled maintenance window.

Deployment sequence:

1. Announce maintenance and stop or drain writers for `tracking_route_deviation_episode` and `trip`.
2. Retain Kafka backlog safely; do not discard telemetry.
3. Confirm no long-running transaction or conflicting snapshot/lock remains.
4. Confirm disk headroom for both builds and WAL.
5. Deploy through normal application/Flyway startup.
6. Monitor `pg_stat_activity`, locks, CPU, I/O and disk.
7. Confirm Flyway V92 exactly once and both indexes ready/valid.
8. Run Tenant-qualified post-migration smoke queries.
9. Restore traffic and monitor latency, errors and Kafka backlog recovery.

The abandoned concurrent-index strategy must not be retried through this Flyway startup path: its non-transactional lifecycle and virtual-transaction waits were incompatible with the governed startup execution. That experiment was removed before V92 and left no history row, index, invalid index or probe data.

Before V92 commits, PostgreSQL rollback removes both builds and Flyway remains V91. After a successful deployment, V92 is immutable and may safely remain during an application rollback. Removing either index requires a separately reviewed forward migration.

## Residual risk

Maintenance duration depends on production cardinality, disk, WAL and concurrent workload. Local timings do not certify production duration. Operators must preserve the drain, capacity and lock checks above.
