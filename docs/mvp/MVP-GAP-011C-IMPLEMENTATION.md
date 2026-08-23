# MVP-GAP-011C Backend Batch and Durable Idempotency Implementation

**Task:** MVP-GAP-011C  
**Story:** US-71 — Support Offline Data Synchronization  
**Slice status:** COMPLETE  
**US-71 status:** NOT IMPLEMENTED under the current audit convention; no real Vehicle or Trip offline-to-server business vertical exists yet

## Scope

This slice adds only the server-side synchronization foundation: a focused `offlinesync` Spring Modulith module, authenticated bounded batch API, independent item transactions, handler registry, deterministic request hashing, durable idempotency inbox, replay and mismatch handling, current-authority checks, and V29. It does not add the coordinator, connectivity behavior, real Fleet/Trip handlers, offline UI, service worker, or Playwright coverage.

## Module Architecture

The module follows the repository's ports-and-adapters flow:

`OfflineSyncController` -> `OfflineSyncUseCase` -> `OfflineSyncBatchService` -> `OfflineSyncItemTransaction` -> `OfflineSyncItemProcessor` -> `OfflineOperationHandler` and `OfflineSyncOperationRepository` -> persistence adapter -> JPA repository.

`IdentityOfflineSyncActorAdapter` resolves the authenticated username through the public `AuthenticatedUserLookup` boundary. The module does not import Fleet, Trip, Identity, or Notification repositories/entities/internals. `ModuleBoundaryArchitectureTest` now enforces this restriction.

## API and DTO Contract

- Method/path: `POST /offline-sync/operations` (external `/api/offline-sync/operations`)
- Security: bearer JWT/authentication required; unauthenticated requests return 401
- Body: `{ "operations": [...] }`, with 1 through 50 operations
- Oversize code: `OFFLINE_SYNC_BATCH_TOO_LARGE`
- Envelope: the exact frozen server fields from `US71-MVP-OFFLINE-SYNC-CONTRACT.md`; client actor/local queue fields are not accepted
- Response: server timestamp plus one ordered result per input item
- Result states: `APPLIED`, `ALREADY_APPLIED`, `REJECTED`, `CONFLICT`, `RETRYABLE_ERROR`
- Append-style `currentVersion` remains null

Normal authorization, validation, conflict, and retryable outcomes are item results, so one failure does not roll back other operations.

## Transactions and Crash Safety

`SpringOfflineSyncItemTransaction` uses `TransactionTemplate`; the batch itself has no encompassing transaction. Each item atomically claims its inbox identity, invokes its handler, stores its terminal result, and commits. A retryable exception rolls back both the test business mutation and inbox claim/result, returning `RETRYABLE_ERROR/OFFLINE_SYNC_RETRYABLE`, so the same operation ID may be retried. The structure provides the transaction seam required for future owning-module handlers without self-invoked `@Transactional` behavior.

## Handler Registry and Authorization

`OfflineOperationHandler` exposes operation type, version, required authorities, and a typed internal outcome. `OfflineOperationHandlerRegistry` indexes type/version registrations and fails startup for duplicates. No production handler silently accepts the four frozen operations in 011C; an absent handler returns `REJECTED/OFFLINE_SYNC_OPERATION_UNSUPPORTED`, and version mismatch returns `REJECTED/OFFLINE_SYNC_PAYLOAD_VERSION_UNSUPPORTED`.

The endpoint requires authentication, while the selected handler's authorities are evaluated per item from the current request principal. A forbidden item returns `REJECTED/OFFLINE_SYNC_FORBIDDEN`; a mixed batch continues. Forbidden results are not allowed to poison the durable inbox, so restored permission can be evaluated on a later attempt.

## Canonical Hash and Idempotency

`Sha256OfflineRequestHasher` computes SHA-256 over deterministic canonical JSON containing only operation type/version, aggregate type/ID, and typed payload. Object keys are sorted; UUIDs, timestamps, numbers, arrays, and optional null/absent values are normalized. Actor, operation ID, client instance, and queue timestamps are excluded.

The canonical operation UUID is also the only valid idempotency key. A first terminal outcome stores `APPLIED`, `REJECTED`, or `CONFLICT`. Same actor and hash replay never invokes the handler: stored `APPLIED` is returned as `ALREADY_APPLIED`, while rejection/conflict is returned unchanged. Actor or hash mismatch returns `CONFLICT/OFFLINE_SYNC_IDEMPOTENCY_MISMATCH` without overwriting the original row. The database primary key plus flushed transactional claim and duplicate-claim replay path limits concurrent identical submissions to one handler execution.

## Database

`V29__create_offline_sync_operations.sql` creates `offline_sync_operation` with the frozen columns. It adds:

- primary key on `operation_id`
- `operation_version > 0` check
- terminal stored-status check limited to `APPLIED`, `REJECTED`, and `CONFLICT`
- `actor_id` foreign key to `app_user(id)`
- `idx_offline_sync_actor_processed (actor_id, processed_at)`
- `idx_offline_sync_aggregate_processed (aggregate_type, aggregate_id, processed_at)`

`ALREADY_APPLIED` is derived and `RETRYABLE_ERROR` is rolled back, so neither is persisted. V1–V28 were not edited and no purge behavior was added.

## Error Mapping

Stable codes covered by this slice include `OFFLINE_SYNC_BATCH_TOO_LARGE`, `OFFLINE_SYNC_OPERATION_UNSUPPORTED`, `OFFLINE_SYNC_PAYLOAD_VERSION_UNSUPPORTED`, `OFFLINE_SYNC_PAYLOAD_INVALID`, `OFFLINE_SYNC_FORBIDDEN`, `OFFLINE_SYNC_IDEMPOTENCY_MISMATCH`, `OFFLINE_SYNC_CONFLICT`, and `OFFLINE_SYNC_RETRYABLE`. Java class names and stack traces are not returned.

## Tests and Verification

- Hash tests cover key ordering, UUID/timestamp/number/null normalization and excluded/included fields.
- Registry tests prove duplicate type/version registration fails fast.
- `OfflineSyncIntegrationTest` covers 401, 1/50/51/empty bounds, ordered/partial results, current RBAC, unsupported type/version, idempotency-key validation, replay, mismatch, duplicate IDs within a batch, persisted rejection/conflict, transient rollback, atomic mutation/inbox behavior, and real concurrent duplicate execution.
- H2 migration/invariant checks cover V29 columns, indexes, constraints, stored fields, and 45-repository context loading.
- `OfflineSyncPostgreSqlInvariantIntegrationTest` covers PostgreSQL UUID/timestamptz, PK/FK/check constraints, and indexes when Docker is available. It was retained but skipped in this environment because Docker was unavailable.
- Architecture: 16/16 tests pass (2 Modulith, 7 hexagonal, 4 module-boundary, 3 Lombok).
- Backend clean test: 664 run, 642 passed, 0 failures, 0 errors, 22 skipped.
- Backend verify: BUILD SUCCESS; executable JAR packaged.
- Spring context: PASS.
- Flyway: clean H2 V1–V29 PASS.
- Frontend lint: PASS.
- Frontend unit: 25 files, 124/124 PASS.
- Frontend build: PASS with the existing non-blocking chunk-size advisory.
- Playwright: not executed because 011C changes no UI/E2E behavior; retained baseline remains 156/156.

The full backend run exposed two test-suite stability issues. Offline-sync test rows are now deleted after each integration test so the intentional actor FK does not contaminate later user-cleanup tests. The existing local SMTP integration worker timeout was increased from 250 ms to 2 seconds to remain deterministic under full-suite load; production notification behavior was not changed.

## Deferred Work

- 011D: coordinator, reconnect, retry, and auth recovery
- 011E: Vehicle reading owning-module handler/workflow
- 011F: Trip operational-event owning-module handlers/workflow
- 011G: synchronization state and conflict UX
- 011H: three-browser offline Playwright
- 011I: complete regression and US-71 closure decision

US-71 therefore remains NOT IMPLEMENTED with 38 complete, 0 partial, and 1 not implemented story (97.44%).
