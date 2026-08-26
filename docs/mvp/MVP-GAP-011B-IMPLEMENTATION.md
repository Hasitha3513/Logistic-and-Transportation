# MVP-GAP-011B — Native IndexedDB Durable Queue

**Story:** US-71 — Support Offline Data Synchronization  
**Mode:** Implementation  
**Slice status:** COMPLETE  
**US-71 status:** NOT IMPLEMENTED under the current audit convention; no accepted offline-to-server vertical exists yet

## Scope

This slice implements only the durable browser-storage foundation frozen by MVP-GAP-011A. It adds no page integration, sync API client, coordinator, connectivity listener, service worker, authentication change, backend code, or Flyway migration.

## Native IndexedDB Architecture

`frontend/src/features/offlineSync/storage.ts` is the only production adapter that uses raw IndexedDB. It exposes the feature-owned `OfflineOperationStorage` interface and resolves persistence methods only after their IndexedDB transaction completes. The production database name is `transport-logistics-offline`, schema version is `1`, and test database names are injected and isolated.

The feature uses native IndexedDB without Dexie, `idb`, localForage, or a persistence framework. `fake-indexeddb` is a development-only dependency because jsdom provides no IndexedDB implementation; it is used only by Vitest and is never part of the production adapter.

## Frozen Operation Envelope

The exact supported operation types are:

- `VEHICLE_READING_RECORD`
- `TRIP_CHECKPOINT_RECORD`
- `TRIP_DELAY_RECORD`
- `TRIP_INCIDENT_RECORD`

`operationVersion` is always `1`, `idempotencyKey` is the canonical generated `operationId`, and `baseVersion` is always `null`. Callers cannot supply any of those fields. The server-bound envelope contains `operationId`, `operationVersion`, `operationType`, `aggregateType`, `aggregateId`, typed `payload`, client timestamps, `clientInstanceId`, `idempotencyKey`, and `baseVersion`.

Local-only metadata contains the owner, status, attempt/retry fields, sanitized error fields, server result fields, local audit timestamps, optional discard timestamp, and the lease fields `syncLeaseId` and `syncLeaseExpiresAt`. The lease fields are deliberately local and prevent retry scheduling from being overloaded as claim state.

## Payload Types

The feature owns narrow compatible primitive unions rather than importing Fleet or Trip feature types and creating cross-feature coupling:

- Vehicle reading: `ODOMETER | ENGINE_HOURS`, non-negative numeric `value` with at most three decimals, offset timestamp, optional notes.
- Trip checkpoint: frozen checkpoint union, offset timestamp, optional location and remarks.
- Trip delay: positive integer minutes, required reason, offset timestamp, optional location and remarks.
- Trip incident: frozen severity union, required description, offset timestamp, optional location and remarks.

Structural validation enforces UUIDs, required values, offset timestamps, frozen enum values, and contract length limits before persistence. Backend validation remains authoritative in later workflow slices.

## Database Schema

| Store | Key path | Purpose |
|---|---|---|
| `operations` | `operationId` | Durable envelopes and local state |
| `metadata` | `key` | Stable non-secret `clientInstanceId` |

The `operations` indexes are `ownerUserId`, `status`, `nextAttemptAt`, `updatedAt`, and compound `[aggregateType, aggregateId]`. No credentials, tokens, passwords, or permission snapshots are stored.

## Storage API

The adapter implements initialization/client identity, enqueue/read/pending selection, atomic claim, expired-claim recovery, synced/failed/conflict transitions, explicit retry release, terminal removal, owner-scoped status counts, synced retention purge, and owner-scoped non-synced counts. A concrete `close()` method exists only to support lifecycle cleanup and durability tests; raw `IDBDatabase` is never exposed.

## Status Transitions

Legal transitions are guarded explicitly:

```text
PENDING  -> SYNCING
SYNCING  -> PENDING | SYNCED | FAILED | CONFLICT
FAILED   -> PENDING
CONFLICT -> PENDING
SYNCED   -> retained terminal record
```

`FAILED`, `CONFLICT`, and `SYNCED` may be removed by the storage API for later confirmed UX. Pending or syncing records cannot be removed.

## Claim and Lease Strategy

Claim selection and mutation run in one IndexedDB read/write transaction. Only requested, owner-matching, due `PENDING` operations are changed to `SYNCING`. IndexedDB write-transaction serialization means two adapter instances cannot both return the same claimed operation. Each claim records a generated local lease ID and caller-provided lease expiry. Expired `SYNCING` claims recover to `PENDING` without changing the operation ID, payload, attempt count, or saved error context; a live lease remains untouched.

## Retry Metadata

`releaseForRetry` accepts caller-calculated `attemptCount`, `lastAttemptAt`, `nextAttemptAt`, and sanitized error data, persists them, returns the operation to `PENDING`, and clears its claim lease. Retry schedule calculation remains deferred to 011D.

## Ownership Isolation

Every query and mutation that addresses user data requires `ownerUserId`. Cross-user reads and list/count results reveal no payload; cross-user claims and removals are no-ops. Direct cross-owner transitions fail with a feature ownership error whose message does not include record contents.

## Capacity and Retention

The enqueue capacity guard counts `PENDING`, `SYNCING`, `FAILED`, and `CONFLICT` across the whole browser database inside the same serialized write transaction as insertion. Operation 1,000 is accepted; operation 1,001 is rejected with `OFFLINE_SYNC_LOCAL_CAPACITY_EXCEEDED`. `SYNCED` records do not consume capacity.

`purgeSynced(ownerUserId, olderThan)` removes only owner-scoped `SYNCED` records older than the supplied seven-day threshold. It never removes pending, syncing, failed, or conflict work. Timed/automatic purge orchestration remains deferred to 011D.

## Tests and Verification

The 18 new tests cover all four envelopes, unsupported/invalid construction, schema/stores/indexes, stable and database-specific client IDs, enqueue/reopen durability, due selection, retry metadata, atomic concurrent claims, expired/live leases, all state transitions, owner isolation, removal, counts, seven-day retention behavior, and the 999/1,000/1,001 capacity boundary including synced exclusion.

Verification on 2026-08-22:

| Gate | Result |
|---|---|
| `npm run lint` | PASS |
| `npm test` | PASS — 124/124 after this slice |
| `npm run build` | PASS |
| `.\mvnw.cmd -B test` | PASS — 647 run, 0 failures, 0 errors, 21 skipped |
| Playwright | Not run; feature remains unreachable from production UI and the task permits retaining the 156/156 baseline |

## Deferred Work

011C owns the authenticated backend batch API, V29 inbox, request hashing, and server idempotency. 011D owns the coordinator, connectivity/retry policy, startup recovery invocation, and auth pause/resume. 011E and 011F own Vehicle/Trip workflow integration; 011G owns status/conflict UX; 011H owns offline Playwright; 011I owns full closure. No part of those slices is implemented here.
