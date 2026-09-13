# Hybrid Telemetry Timescale History Runbook

## Signals

Monitor `tracking-telemetry-persister-group` lag, batch persistence outcomes, PostgreSQL/Timescale
availability, compression/retention job state and the internal DLT volume. Metrics and health details
may expose availability and lag only. Never log or label coordinates, payloads, Tenant/Vehicle IDs,
provider secrets, signatures or credential references.

## Database outage and recovery

1. Confirm Kafka remains healthy and retains the uncommitted source backlog.
2. Confirm the affected consumer offsets did not advance after the database failure.
3. Disable the historical consumer with the governed hybrid-storage runtime switch if bounded retry
   is creating avoidable operational pressure.
4. Restore TimescaleDB and verify Flyway is at V87 and the hypertable plus both policies are healthy.
5. Re-enable the consumer and confirm lag drains without duplicate Tenant-scoped facts.
6. Verify representative bounded Tenant/Vehicle/time-range reads and the absence of partial batches.

Do not acknowledge failed batches, delete Kafka backlog, synthesize history from Redis, or use a
development database for acceptance recovery.

## Dead-letter replay

`tracking.telemetry.ingested.v1.dlt` is an internal, access-controlled topic. Diagnose with safe
event/header identities only. Correct the producer or configuration cause, then replay the original
canonical record through `tracking.telemetry.ingested.v1` under change control. Database uniqueness
makes exact replay safe; never edit Timescale rows to disguise a poison record.

## Timescale policy operations

V87 configures seven-day chunks, compression after seven days and raw retention after 180 days.
Compression segments by Tenant and Vehicle. Inspect Timescale job and hypertable catalogues before
changing operational state. Policy removal, interval changes, schema correction or immutable-image
pinning require a separately reviewed change and, where schema state changes, a new forward migration.

## Rollback

The consumer may be disabled without deleting accepted history. Redeploy the previous application
only if it is compatible with V87, keep the Kafka backlog replayable, and preserve every existing
historical fact. Never edit or delete V87 and never silently rewrite telemetry during rollback.
