# US-71 MVP Offline Data Synchronization Contract

**Task:** MVP-GAP-011-US71
**Slice:** MVP-GAP-011I
**Status:** CONTRACT FROZEN — IMPLEMENTED / COMPLETE
**Date:** August 23, 2026

## 1. Decision

US-71 will provide durable store-and-forward synchronization for two existing field workflows:

1. manual vehicle odometer/engine-hour readings; and
2. Trip operational events: checkpoints, delays, and incidents.

Both are append-style operations against existing server aggregates. No aggregate is created offline. The React application will persist each supported command in IndexedDB before reporting it as queued. The server will accept bounded batches, process every item independently through the owning module's application behavior, and durably record idempotency results.

This is a web-client operational queue, not a general distributed synchronization platform, mobile application, PWA, or offline app-shell implementation.

## 2. Authority and Repository Evidence

The selected 39-story inventory defines US-71 as offline transaction queueing, client-side store-and-forward, and conflict recovery. The current task makes it the last mandatory MVP story, superseding older audit recommendations to defer it with a future mobile application.

The frozen scope is grounded in these existing contracts:

- `VehicleReadingController`, `VehicleReadingUseCase`, and `VehicleReadingService` already support manual `ODOMETER` and `ENGINE_HOURS` readings, chronology validation, row locking, an idempotency key, and Fleet events.
- `VehicleReadingSection` and `useVehicleReadings` already expose the manual-reading field workflow.
- `TripController`, `TripOperationalEventUseCase`, and `TripOperationalEventService` already support `CHECKPOINT`, `DELAY`, and `INCIDENT`, append Trip history, and publish normal Trip notification events.
- `TripOperationalEventsSection` and `useTripOperationalEvents` already expose those commands.
- `SecurityConfig` currently maps manual readings to `VEHICLE_READING_CREATE` and Trip events to any of `TRIP_DISPATCH`, `TRIP_LOG_MANAGE`, or `TRIP_UPDATE`.
- The frontend has no IndexedDB dependency. The MVP will therefore use a small native IndexedDB adapter and add no offline framework dependency.
- The deployed application base path is `/api`; the controller path frozen below is `/offline-sync/operations`, externally `/api/offline-sync/operations`.

### 2.1 Authoritative User Story

| Attribute | Frozen value | Source decision |
|---|---|---|
| ID | US-71 | Consistent across the 39-story inventory and current status comparison |
| Title | Support Offline Data Synchronization | Current task and status comparison; the inventory shortens this to "Support Offline Data Sync" |
| Actor | Not specified in repository source material | No actor field or canonical story sentence was found; this contract does not invent one |
| User-story sentence | Not specified in repository source material | The repository supplies a title and missing acceptance capabilities, not an “As a … I want …” statement |
| Related feature | Cross-cutting operational continuity | `MVP_V2_CURRENT_STATUS_AUDIT.md` groups US-71 with cross-cutting MVP stories |
| Related sub-features | Offline transaction queue, client-side store-and-forward, transactional conflict resolution | Exact missing criteria in `MVP_V2_CURRENT_STATUS_AUDIT.md` |
| Current priority | Mandatory release blocker and next implementation task | `MVP-CURRENT-STATUS-COMPARE-002.md` and the current MVP-GAP-011 task |
| Historical priority | P2 / Phase 3 mobile architecture | Older V2/status audits; superseded for the selected 39-story release by the current task |
| Dependencies | Existing React field workflows, current authentication, stable Fleet/Trip application behavior, green E2E harness | Current repository and task authority |

The repository does not contain the referenced “Mind-Map-Transportation-and-Logistic” source or a separate requirements artifact with a fuller US-71 actor/story sentence. The oldest available audits explicitly deferred mobile offline synchronization and mentioned service-worker/mobile dependencies. The newer current-status comparison and the present task explicitly retain US-71 as the last mandatory MVP gap and permit a bounded React/IndexedDB implementation without a service worker. The newer, task-specific authority governs this contract; the historical conflict remains recorded rather than silently rewritten.

### 2.2 Source Acceptance Criteria

The exact repository acceptance capabilities are:

1. offline transaction queue;
2. client-side store-and-forward synchronization; and
3. transactional synchronization conflict resolution.

The current task elaborates those three capabilities into the following testable MVP criteria without expanding the business scope:

1. supported field commands are durably stored before the UI reports them queued;
2. queued commands survive reload/browser restart storage lifecycle;
3. supported commands synchronize automatically after backend reachability returns;
4. synchronization requires current authentication and current business authorization;
5. the server applies a repeated operation at most once and rejects changed payload reuse;
6. a batch returns one independent result for every accepted envelope;
7. permanent validation/authorization failures, business conflicts, and transient failures remain distinguishable;
8. transient failures use persisted bounded retry and recover after reconnect or re-authentication;
9. users can see global and per-operation pending/syncing/synced/failed/conflict state;
10. users can manually sync, open/refresh an affected record, retry where meaningful, and explicitly discard terminal local work;
11. synced Trip delay/incident commands use the normal Trip-to-Notification path; and
12. retained acceptance scenarios pass in Chromium, Firefox, and WebKit without weakening the existing 156 executions.

### 2.3 MVP Offline Definition

| Offline capability | MVP decision | Meaning |
|---|---|---|
| A. Application shell available without network | DEFERRED | No service worker or app-shell cache is required. The application must already be loaded to capture while disconnected. |
| B. Previously loaded data viewable offline | INCIDENTAL, NOT AUTHORITATIVE | Existing in-memory/TanStack data may remain visible, but US-71 creates no offline read replica and never labels it current. |
| C. New operational data captured offline | REQUIRED | Only the four frozen append-style operation types. |
| D. Mutations durably queued locally | REQUIRED | IndexedDB write precedes “Pending sync” feedback. |
| E. Automatic synchronization after reconnect | REQUIRED | While the application is open, with actual backend reachability and usable authentication. |
| F. Conflict detection | REQUIRED | Idempotency mismatch and owning-module business conflicts. |
| G. Conflict resolution | REQUIRED, BOUNDED | View/open, refresh, retry when safe, or explicitly discard; no generic merge. |
| H. Retry/recovery | REQUIRED | Persisted bounded backoff, startup recovery, auth pause/resume, and manual sync. |
| I. Cross-device synchronization | NOT REQUIRED | The server protects duplicate submissions, but clients do not exchange local queues. |
| J. Full offline-first application | DEFERRED | Administration, reads, lifecycle orchestration, PWA, and arbitrary offline CRUD remain outside MVP. |

## 3. Operation Scope

### 3.1 OFFLINE_REQUIRED

| Operation type | Owning module | Existing behavior reused | Conflict class | Required current authority |
|---|---|---|---|---|
| `VEHICLE_READING_RECORD` version 1 | Fleet | manual vehicle reading record path | `BUSINESS_CONFLICT` | `VEHICLE_READING_CREATE` |
| `TRIP_CHECKPOINT_RECORD` version 1 | Trip | record checkpoint | `BUSINESS_CONFLICT` | any current Trip-event authority: `TRIP_DISPATCH`, `TRIP_LOG_MANAGE`, or `TRIP_UPDATE` |
| `TRIP_DELAY_RECORD` version 1 | Trip | record delay and normal notification publication | `BUSINESS_CONFLICT` | any current Trip-event authority: `TRIP_DISPATCH`, `TRIP_LOG_MANAGE`, or `TRIP_UPDATE` |
| `TRIP_INCIDENT_RECORD` version 1 | Trip | record incident and normal notification publication | `BUSINESS_CONFLICT` | any current Trip-event authority: `TRIP_DISPATCH`, `TRIP_LOG_MANAGE`, or `TRIP_UPDATE` |

All four operations target an existing known aggregate. The client must have loaded the relevant Vehicle or Trip before losing backend connectivity.

### 3.2 ONLINE_ONLY

| Workflow | Reason |
|---|---|
| Authentication, refresh, logout, identity, roles, and permissions | Security state must be server-authoritative. No credentials are queued. |
| Customer, department, location, project, Fleet master data, route, and notification-rule administration | Administrative mutation has low field value and broad conflict semantics. |
| Vehicle-reading correction and meter reset | These alter interpretation of existing readings and require current chronology/epoch state. |
| Vehicle/driver allocation, assignment, availability, dispatch, approval, rejection, authorization, and cancellation | They are conflict-sensitive orchestration commands and must use current server state. |
| Trip submit, approve, reject, assign, dispatch, start, complete, close, and cancel | Lifecycle mutation is version/state sensitive. Trip completion may also create authoritative end readings. |
| Fuel issue, authorization, purchase, bunker, and reconciliation commands | Financial/stock authorization and current balances are server-authoritative. |
| Driver licences, medical records, drug tests, violations, and exception administration | Sensitive data and/or scheduling eligibility must not be stored offline for this story. |
| Reporting, dashboards, and all reads | US-71 queues operational writes; it does not create an offline read replica. Existing cached UI data is not represented as current server truth. |
| Destructive operations | MVP conflict resolution does not support offline delete/deactivation. |

### 3.3 DEFERRED

| Workflow | Reason |
|---|---|
| Creating Trips, Vehicles, Drivers, or other aggregates offline | Requires temporary IDs, relationship reconciliation, and wider conflict rules. |
| Proof-of-delivery, file/photo capture, signatures, and attachments | No selected MVP server workflow currently defines the required proof contract or safe blob storage. |
| Telematics ingestion and general driver/vehicle observations | Current source types/workflows are reserved or not selected for the MVP field UI. |
| Service worker, app-shell caching, installable PWA, and Browser Background Sync | Not required for correctness of the open-application queue. May be evaluated after MVP. |
| Generic merge UI, CRDTs, cross-device collaboration, and automatic business-conflict resolution | Outside the bounded append-operation contract. |

## 4. Client Operation Envelope

### 4.1 Field Classification

| Field | Classification | Correctness purpose |
|---|---|---|
| `operationId` | REQUIRED | Stable client-generated UUID and server deduplication identity |
| `operationVersion` | REQUIRED | Selects the supported typed payload schema; MVP value is 1 |
| `operationType` | REQUIRED | Routes to one typed owning-module handler |
| `aggregateType` | REQUIRED | Audit/routing guard and local record grouping |
| `aggregateId` | REQUIRED | Identifies the existing Vehicle or Trip |
| `payload` | REQUIRED | Contains only the typed business facts needed by the command |
| `clientCreatedAt` | REQUIRED | Preserves original queue/capture evidence |
| `clientUpdatedAt` | REQUIRED locally and in request | Records the latest client-side state change without replacing capture time |
| `clientInstanceId` | REQUIRED | Non-secret diagnostic identity for a browser database |
| `idempotencyKey` | REQUIRED | Equal to `operationId`; integrates the generic inbox and Fleet safeguard |
| `baseVersion` | OPTIONAL, null in MVP | Reserved contract slot; none of the frozen append operations uses aggregate versioning |
| `ownerUserId` | REQUIRED locally, NOT SENT as authority | Prevents cross-user queue exposure/synchronization; server derives actor from JWT |
| `status` | REQUIRED locally, NOT SENT | Drives durable local queue state |
| `attemptCount` | REQUIRED locally, NOT SENT | Persists retry budget |
| `lastAttemptAt` | OPTIONAL locally, NOT SENT | Retry diagnostics |
| `nextAttemptAt` | OPTIONAL locally, NOT SENT | Durable retry scheduling |
| `lastErrorCode` | OPTIONAL locally, NOT SENT | Stable terminal/retry reason |
| `lastErrorMessage` | OPTIONAL locally, NOT SENT | Sanitized operator context |
| `createdAt` | REQUIRED locally, NOT SENT separately | IndexedDB record audit; initially equal to local persistence time |
| `updatedAt` | REQUIRED locally, NOT SENT separately | IndexedDB state-transition audit |
| Device hardware identity, geolocation, user-agent, arbitrary headers | NOT REQUIRED | No correctness need; would increase fingerprinting/data exposure |

The stable operation envelope sent to the server is:

```text
operationId: UUID
operationType: VEHICLE_READING_RECORD | TRIP_CHECKPOINT_RECORD |
               TRIP_DELAY_RECORD | TRIP_INCIDENT_RECORD
operationVersion: 1
aggregateType: VEHICLE | TRIP
aggregateId: UUID
payload: typed JSON object
clientCreatedAt: ISO-8601 offset timestamp
clientUpdatedAt: ISO-8601 offset timestamp
clientInstanceId: UUID
idempotencyKey: string (exactly operationId in canonical UUID form)
baseVersion: null
```

`operationId` is generated once with `crypto.randomUUID()` before the first IndexedDB write and is never regenerated for retry. `clientInstanceId` is a non-secret UUID persisted as IndexedDB metadata. It is diagnostic evidence, not an identity or authorization credential.

All selected operations are append-style. `baseVersion` is therefore null in version 1. Business conflicts are still possible because server chronology and Trip lifecycle state can change while the client is offline.

The server ignores client-supplied actor identity. It derives the actor from the current JWT.

## 5. Typed Payloads

### 5.1 `VEHICLE_READING_RECORD` version 1

```text
readingType: ODOMETER | ENGINE_HOURS
value: non-negative decimal, maximum 3 decimal places
recordedAt: required ISO-8601 offset timestamp
notes: optional string, maximum 1000 characters
```

The handler forces `sourceType=MANUAL`, `sourceReferenceId=null`, and passes the envelope idempotency key into the existing Fleet application behavior. The aggregate ID is the Vehicle ID. Client time is evidence; Fleet chronology and the five-minute future tolerance remain server-authoritative.

### 5.2 `TRIP_CHECKPOINT_RECORD` version 1

```text
checkpointType: DEPARTURE | ARRIVAL | PICKUP | DELIVERY | REST_STOP | CUSTOM
occurredAt: required ISO-8601 offset timestamp
locationId: optional UUID
locationDescription: optional string, maximum 255 characters
remarks: optional string, maximum 2000 characters
```

### 5.3 `TRIP_DELAY_RECORD` version 1

```text
delayMinutes: integer >= 1
reason: required non-blank string, maximum 500 characters
occurredAt: required ISO-8601 offset timestamp
locationId: optional UUID
locationDescription: optional string, maximum 255 characters
remarks: optional string, maximum 2000 characters
```

### 5.4 `TRIP_INCIDENT_RECORD` version 1

```text
incidentSeverity: LOW | MEDIUM | HIGH | CRITICAL
description: required non-blank string, maximum 500 characters
occurredAt: required ISO-8601 offset timestamp
locationId: optional UUID
locationDescription: optional string, maximum 255 characters
remarks: optional string, maximum 2000 characters
```

For all Trip operations, the aggregate ID is the Trip ID. `occurredAt` is made mandatory for offline capture so replay never substitutes server receipt time for the field event time.

## 6. Local IndexedDB Model

### 6.1 Local Status Model

The complete local state set is `PENDING`, `SYNCING`, `SYNCED`, `FAILED`, and `CONFLICT`.

`RETRY_WAIT` is not required. A retryable item remains `PENDING` with a future `nextAttemptAt`; this preserves the useful operator state while avoiding a sixth state that adds no behavior. `SYNCING` is a leased claim, not a server success indication. Expired claims recover to `PENDING`.

Database name: `transport-logistics-offline`
Schema version: `1`
Operation store: `operations`
Metadata store: `metadata`

The operation record contains the complete envelope plus:

```text
ownerUserId: UUID
status: PENDING | SYNCING | SYNCED | FAILED | CONFLICT
attemptCount: non-negative integer
lastAttemptAt: optional ISO-8601 timestamp
nextAttemptAt: optional ISO-8601 timestamp
lastErrorCode: optional string
lastErrorMessage: optional sanitized string
serverProcessedAt: optional ISO-8601 timestamp
serverAggregateId: optional UUID
serverResultStatus: optional result status
createdAt: ISO-8601 timestamp
updatedAt: ISO-8601 timestamp
discardedAt: optional ISO-8601 timestamp (local audit before removal)
```

Indexes are required for `ownerUserId`, `status`, `nextAttemptAt`, `aggregateType+aggregateId`, and `updatedAt`.

Pages and hooks access storage only through a narrow feature-owned interface:

```text
enqueue, getPending, claimForSync, markSynced, markFailed,
markConflict, releaseForRetry, remove, countByStatus, purgeSynced
```

Raw IndexedDB calls do not appear in Fleet or Trip pages.

## 7. Queue-First Mutation Strategy

Supported operations use one queue-first path both online and offline:

1. perform Zod/client structural validation;
2. create the immutable operation identity;
3. persist the local record as `PENDING` in IndexedDB;
4. report **Pending sync**, never server-confirmed success;
5. if the backend is reachable and authentication is usable, request immediate synchronization;
6. map the item result to the local status and refresh normal server queries after `APPLIED` or `ALREADY_APPLIED`.

This avoids separate online and offline business paths. Existing direct mutation hooks are adapted only for the four supported operations. Backend validation remains authoritative.

## 8. Connectivity and Coordinator

One feature-owned sync coordinator is responsible for all supported operations. It:

- treats `navigator.onLine`, `online`, and `offline` as hints only;
- verifies backend reachability using the existing public `/health` endpoint and/or the sync call itself;
- selects due `PENDING` operations for the current user;
- atomically claims them as `SYNCING` in IndexedDB;
- sends at most 50 operations per batch;
- permits only one in-flight coordinator run per browser tab and uses a short IndexedDB lease to avoid duplicate tab claims;
- maps every server item result independently;
- returns abandoned/expired `SYNCING` leases to `PENDING` on startup;
- runs after login, application startup, reconnect, manual **Sync now**, and a due retry timer;
- never deletes queued work because a request timed out or the session expired.

Core correctness requires the application to be open. Browser Background Sync is not required.

## 9. Sync API

### 9.1 Endpoint

```text
POST /offline-sync/operations
External URL: /api/offline-sync/operations
Authentication: Bearer JWT required
Content-Type: application/json
```

Request:

```json
{
  "operations": ["operation envelope", "..."]
}
```

Response for an accepted batch:

```json
{
  "serverTimestamp": "ISO-8601 timestamp",
  "results": ["one result for every submitted operation"]
}
```

Batch size is 1 through 50. An empty, missing, malformed, or oversized batch is rejected before processing with the existing `ApiError` contract. Oversize uses `OFFLINE_SYNC_BATCH_TOO_LARGE`. A syntactically valid envelope with an invalid typed payload receives an item-level `REJECTED` result.

### 9.2 Item Result

```text
operationId: UUID
status: APPLIED | ALREADY_APPLIED | REJECTED | CONFLICT | RETRYABLE_ERROR
serverTimestamp: ISO-8601 timestamp
aggregateId: UUID
currentVersion: null for MVP append operations
errorCode: optional stable code
message: optional sanitized message
```

An accepted HTTP batch normally returns 200 even when individual items are rejected or conflicted. Failure of one item never rolls back another item.

### 9.3 Partial Batch Semantics

Each operation is processed in its own transaction. For example, a ten-item batch may return seven `APPLIED`, one `ALREADY_APPLIED`, one `CONFLICT`, and one `REJECTED`. The response preserves the input order and contains ten results.

Top-level HTTP failures are limited to authentication, malformed batch structure, batch-size validation, and inability to execute the batch endpoint itself. A transient failure isolated to one operation is `RETRYABLE_ERROR` for that item.

## 10. Backend Module Boundary

A focused `offlinesync` Spring Modulith module will own:

- the web controller and web DTOs;
- batch orchestration;
- handler registry;
- idempotency/audit domain model and persistence port;
- V29 persistence adapter and configuration.

It must not access Fleet, Trip, Identity, or Notification repositories/entities. It invokes public root-package application contracts owned by Fleet and Trip. The owning modules retain validation, locking, audit/history, aggregate mutation, and event publication.

Handlers are typed and selected by `supports(operationType, operationVersion)`. The controller contains no business switch. Fleet and Trip may expose the minimum public recording contracts needed for this orchestration; they do not expose JPA entities or repositories.

Trip delay/incident synchronization follows the normal Trip application path. Trip publishes its existing operational event, and Notification reacts normally. Offline Sync never calls Notification directly.

## 11. Server Idempotency Contract

V29 is required because no current table can atomically deduplicate all supported operation types or retain their result. The proposed immutable forward migration is `V29__create_offline_sync_operations.sql`. It adds one durable `offline_sync_operation` table. V1–V28 are immutable.

Minimum server record:

```text
operation_id UUID primary key
operation_type VARCHAR
operation_version INTEGER
actor_id UUID
client_instance_id UUID
aggregate_type VARCHAR
aggregate_id UUID
request_hash VARCHAR
result_status VARCHAR
result_code VARCHAR nullable
result_version BIGINT nullable
processed_at TIMESTAMP WITH TIME ZONE
created_at TIMESTAMP WITH TIME ZONE
```

Required constraints/indexes are:

- primary/unique identity on `operation_id`;
- foreign key from `actor_id` to `app_user(id)` where H2/PostgreSQL-compatible lifecycle semantics allow it;
- checks for positive `operation_version` and allowed stored result statuses;
- index on `(actor_id, processed_at)` for actor audit lookup;
- index on `(aggregate_type, aggregate_id, processed_at)` for record diagnostics; and
- no uniqueness on request hash because two legitimately distinct operation IDs may carry identical append facts and the owning module decides whether that is a business duplicate.

No server purge is frozen for MVP. Deleting idempotency rows would reopen old operations to duplicate application, so any future retention change requires a separate product/audit decision.

The request hash is SHA-256 over deterministic canonical JSON containing operation type, version, aggregate type, aggregate ID, and typed payload. Object keys are sorted, timestamps and UUIDs use canonical text, and absent optional values have one canonical representation.

Rules:

- first submission with a new operation ID applies the owning-module command and persists its terminal result in the same item transaction;
- replay by the same actor with the same canonical request returns `ALREADY_APPLIED` when the stored result was `APPLIED`, otherwise it returns the stored terminal rejection/conflict without rerunning business behavior;
- reuse of an operation ID by another actor or with a different request hash returns `CONFLICT` with `OFFLINE_SYNC_IDEMPOTENCY_MISMATCH`;
- concurrent duplicate submissions are serialized by the unique operation ID; only one can execute business behavior;
- a transient exception rolls back the item transaction, including the idempotency claim, and returns `RETRYABLE_ERROR`;
- a process crash cannot commit the business mutation without its idempotency result because both participate in one database transaction.

The existing Vehicle-reading idempotency key remains a second owning-module safeguard. Trip event exactly-once behavior is supplied by the offline inbox transaction; notification/history side effects are reached only through the normal Trip operation.

## 12. Conflict and Rejection Rules

No selected operation uses optimistic aggregate versions in MVP.

### 12.1 Conflicts

| Condition | Item status | Stable sync code |
|---|---|---|
| Same operation ID with changed payload or actor | `CONFLICT` | `OFFLINE_SYNC_IDEMPOTENCY_MISMATCH` |
| Vehicle chronology decrease/order conflict or equivalent duplicate | `CONFLICT` | `OFFLINE_SYNC_CONFLICT` with sanitized owning error code/message |
| Trip is now Draft, Submitted, Rejected, Cancelled, or Closed | `CONFLICT` | `OFFLINE_SYNC_CONFLICT` |
| Other owning-module state conflict | `CONFLICT` | `OFFLINE_SYNC_CONFLICT` |

The server never overwrites current state or silently adjusts timestamps/readings.

### 12.2 Rejections

| Condition | Item status | Stable sync code |
|---|---|---|
| Unsupported operation type | `REJECTED` | `OFFLINE_SYNC_OPERATION_UNSUPPORTED` |
| Unsupported operation version | `REJECTED` | `OFFLINE_SYNC_PAYLOAD_VERSION_UNSUPPORTED` |
| Invalid typed payload | `REJECTED` | `OFFLINE_SYNC_PAYLOAD_INVALID` |
| Current actor lacks the owning operation's authority | `REJECTED` | `OFFLINE_SYNC_FORBIDDEN` |
| Aggregate not found or other permanent business rejection | `REJECTED` | sanitized stable owning code, otherwise `OFFLINE_SYNC_PAYLOAD_INVALID` |

Validation, authorization, and conflict results are not automatically retried.

## 13. Retry and Recovery

Only network errors, timeouts, backend unavailability, item `RETRYABLE_ERROR`, and HTTP 5xx responses are retried automatically.

The persisted delay sequence is:

```text
initial immediate attempt, then 5s, 15s, 30s, 60s, and 60s thereafter
maximum 10 automatic attempts per operation
```

After ten failed send attempts, the item becomes `FAILED`. **Retry** explicitly returns it to `PENDING` and requests a new immediate attempt while preserving its operation ID and cumulative audit metadata.

Being offline does not increment `attemptCount`. HTTP 401 or failed token refresh pauses synchronization, returns claimed items to `PENDING`, and prompts re-authentication without incrementing attempts. Synchronization resumes after successful login by the same user.

`REJECTED` maps to local `FAILED`; `CONFLICT` maps to local `CONFLICT`; `APPLIED` and `ALREADY_APPLIED` map to local `SYNCED`.

## 14. Authentication, Authorization, and Local Ownership

- Capture requires an already authenticated user loaded from `/auth/me`; anonymous forms cannot enqueue.
- Synchronization requires a currently valid access token and uses the existing refresh flow.
- No password, access token, refresh token, or raw credential is stored in the offline database.
- Local records are keyed to `ownerUserId`. Only that currently authenticated user can view, claim, sync, retry, or discard them.
- Logout and session expiry preserve queued records.
- A different user on the same browser cannot sync or inspect another user's payloads.
- The server derives actor identity and authorities from the current JWT for every operation. Capture-time permission is not accepted as authority.
- Permission revoked while offline yields item-level `REJECTED / OFFLINE_SYNC_FORBIDDEN` before domain mutation.
- Endpoint authentication failure is the existing 401 `ApiError`. Per-operation permission failure is an item result so mixed-authority batches retain partial success.

IndexedDB is origin-scoped but is not application-encrypted storage. Payloads are deliberately limited to the non-sensitive operational fields above. Browser-profile/device access remains a documented local exposure risk.

## 15. User-Visible Status and Actions

The existing application header receives a compact indicator with:

- **Online** or **Offline**;
- **Pending N**;
- **Syncing N**;
- **Conflicts N**;
- **Failed N**; and
- **Sync now** when online and due work exists.

The indicator opens a small status panel, not a separate administration application.

Vehicle readings and Trip events show local rows/items with `Pending sync`, `Syncing`, `Synced`, `Conflict`, or `Failed`. Local rows are visually distinct from server-confirmed records and use the operation ID as their temporary UI key.

For `FAILED` and `CONFLICT` items the user can:

- retry when retry is meaningful;
- refresh server state;
- open the affected Vehicle/Trip;
- discard the local item after an Ant Design confirmation modal.

No generic merge editor is included. Discard is never silent.

## 16. Retention and Capacity

- `SYNCED` local operations are retained for 7 days, then purged on startup and after successful sync.
- `PENDING`, `SYNCING`, `FAILED`, and `CONFLICT` operations are never automatically deleted.
- The store accepts at most 1,000 non-synced operations per browser profile. At the limit, capture is blocked with a clear storage-capacity error until the user synchronizes or explicitly discards terminal items.
- Server idempotency/audit rows are retained; server-side purge is deferred because they are required for durable replay protection and audit.

## 17. Error Codes

The MVP sync layer defines:

- `OFFLINE_SYNC_OPERATION_UNSUPPORTED`
- `OFFLINE_SYNC_PAYLOAD_INVALID`
- `OFFLINE_SYNC_PAYLOAD_VERSION_UNSUPPORTED`
- `OFFLINE_SYNC_IDEMPOTENCY_MISMATCH`
- `OFFLINE_SYNC_CONFLICT`
- `OFFLINE_SYNC_FORBIDDEN`
- `OFFLINE_SYNC_RETRYABLE`
- `OFFLINE_SYNC_BATCH_TOO_LARGE`

Messages are sanitized. Existing correlation IDs remain available for top-level `ApiError` responses and server logs.

## 18. E2E Acceptance Contract

The offline Playwright slice must add real UI/API/IndexedDB coverage without weakening the existing 156 cases. All retained cases run in Chromium, Firefox, and WebKit with eventual assertions and no arbitrary sleeps.

| ID | Acceptance case |
|---|---|
| E2E-OFF-001 | Capture a manual vehicle reading while the browser context is offline; UI shows Pending sync and no server row exists. |
| E2E-OFF-002 | A queued reading remains in IndexedDB after a page/application reload; the test restores asset reachability without bypassing the storage abstraction. |
| E2E-OFF-003 | Reconnect automatically applies the reading and changes status to Synced. |
| E2E-OFF-004 | Resubmitting the same operation returns already applied and creates one server reading. |
| E2E-OFF-005 | Capture each supported Trip operational event family while offline. |
| E2E-OFF-006 | Reconnect applies a Trip event exactly once. |
| E2E-OFF-007 | A synced delay/incident follows normal Trip publication and produces its expected notification side effect once. |
| E2E-OFF-008 | A mixed batch preserves applied, rejected, conflict, and retryable local outcomes independently. |
| E2E-OFF-009 | Permission revoked before synchronization produces Failed/Forbidden and no domain mutation. |
| E2E-OFF-010 | Vehicle chronology or Trip lifecycle business conflict is visible with open/refresh/discard actions. |
| E2E-OFF-011 | A deterministic transient failure remains queued and later succeeds with the same operation ID. |
| E2E-OFF-012 | Manual Sync now sends due work. |
| E2E-OFF-013 | Discard requires explicit confirmation and removes only the selected terminal local item. |
| E2E-OFF-014 | Global counts reflect pending, syncing, conflict, and failed states. |
| E2E-OFF-015 | Durable queue survives a new page/session using the same browser storage profile and resumes for the same user. |

`context.setOffline(true)` is used for core capture/reconnect cases after the application is loaded. Reload-persistence cases keep the frontend asset server reachable while deterministically making the backend unavailable; a service worker is not part of this contract.

Any server failure/auth/clock controls used by E2E must be enabled only in the `e2e` profile, unavailable in production, and protected consistently with the existing test-control convention.

## 19. Required Verification for Closure

### 19.1 Backend Test Strategy

Backend tests must cover empty/valid/max/oversized batches; mixed partial results; unsupported type/version; invalid typed payload; current 401 and per-item forbidden behavior; permission revocation before application; first apply and same-payload replay; changed-payload/actor mismatch; concurrent duplicate submission; transaction rollback/restart-safe persistence; handler routing; Vehicle chronology conflicts; Trip lifecycle conflicts; and normal Trip notification publication without an Offline Sync-to-Notification dependency. V29 repository integration must run on clean H2, while PostgreSQL invariant/Testcontainers coverage validates the unique operation identity, request hash, indexes, and concurrent duplicate behavior.

Architecture verification must keep `ApplicationModulesTest`, `HexagonalLayerArchitectureTest`, `ModuleBoundaryArchitectureTest`, and `LombokUsageArchitectureTest` at 15/15 or higher with no repository/entity import from Offline Sync.

### 19.2 Frontend Test Strategy

Frontend unit/component tests must use an isolated real IndexedDB test database or a standards-compatible test implementation and cover enqueue durability; new storage instance/reload reads; all status transitions; atomic claim/expired lease recovery; persisted retry metadata; owner filtering; seven-day synced purge; capacity guard; offline no-send; reachability/reconnect; partial results; retryable/permanent/conflict results; auth expiry and same-user resume; different-user isolation; duplicate coordinator suppression; manual sync; per-record pending state; global counts; and confirmed discard.

TanStack Query cache assertions do not substitute for IndexedDB persistence tests.

### 19.3 Playwright Strategy

The E2E-OFF-001 through E2E-OFF-015 contract in section 18 is additive to the retained 156 executions. Every retained logical case runs in Chromium, Firefox, and WebKit. Network and IndexedDB assertions use eventual polling. Core capture/reconnect uses Playwright network controls after initial app load; reload persistence keeps assets reachable while the backend is unavailable. No arbitrary sleep, direct local-queue insertion for core flows, production test endpoint, or browser-policy reduction is allowed.

US-71 cannot be marked complete until slices 011B–011I implement and verify:

- native IndexedDB storage and queue transitions;
- backend batch API, V29, request hash, concurrent idempotency, and partial success;
- coordinator, reachability, retry, authentication pause/resume, and duplicate-run prevention;
- Vehicle-reading and Trip-event UI integration;
- global and per-record status/conflict UX;
- backend unit, security, persistence, concurrency, module, and context tests;
- frontend storage, coordinator, hook, and component tests;
- all offline Playwright cases supported above across three browsers;
- `./mvnw -B clean test`, `./mvnw -B verify`, `npm run lint`, `npm test`, and `npm run build`;
- clean H2 V1–V29 migration and PostgreSQL-compatible V29 SQL;
- all pre-existing 156 Playwright cases.

## 20. Slice Status

### 20.1 MVP-GAP-011B — Durable Local Queue

- **Objective:** Implement the frozen envelope, native IndexedDB adapter, queue transitions, ownership, leases, retention, and capacity guard without wiring business forms.
- **Backend changes:** None.
- **Frontend changes:** Add feature-owned `features/offlineSync` types, storage/queue interfaces and implementations, plus deterministic clock/UUID seams for tests.
- **Database changes:** Browser IndexedDB schema version 1 only; no Flyway change.
- **Tests:** Storage persistence/reopen, transitions, lease recovery, retry metadata, user isolation, purge, and capacity.
- **Acceptance:** A supported typed envelope survives a new storage instance and cannot be claimed by another user.
- **Dependencies:** Frozen 011A contract and current `/auth/me` user ID.
- **Definition of Done:** Frontend lint/unit/build pass; no page, endpoint, service worker, or production workflow changed.

### 20.2 MVP-GAP-011C — Backend Batch and Durable Idempotency

- **Objective:** Add the bounded API, typed handler registry, per-item transaction orchestration, canonical request hashing, and durable replay results.
- **Backend changes:** Add `offlinesync` hexagonal module, controller/DTOs, application service, domain result model, handler port, persistence port/adapter, security mapping, and public owning-module adapter seams without implementing the two business handlers yet.
- **Frontend changes:** Add only typed sync request/response client if needed for contract verification; no coordinator.
- **Database changes:** Add `V29__create_offline_sync_operations.sql` exactly as frozen.
- **Tests:** Batch bounds/partial behavior, security, unsupported/invalid results, first/replay/mismatch/concurrent duplicate, rollback recovery, persistence, H2/PostgreSQL, and architecture.
- **Acceptance:** Fifty independent no-op/test handlers can return ordered partial results; replay cannot execute a handler twice.
- **Dependencies:** 011B types may inform JSON names, but backend correctness does not depend on browser storage.
- **Definition of Done:** Clean backend test/verify and V1–V29 startup pass with no module/repository leak.

### 20.3 MVP-GAP-011D — Coordinator and Recovery

**Implementation status: COMPLETE (2026-08-23).** Evidence is recorded in `MVP-GAP-011D-IMPLEMENTATION.md`.

- **Objective:** Send due operations safely after startup/login/reconnect/manual trigger and persist retry/auth recovery.
- **Backend changes:** No business handler; deterministic e2e-profile reachability/failure control only if later E2E requires it, not in production.
- **Frontend changes:** Add sync client/coordinator, single-run and cross-tab lease protection, reachability probe, timers, auth pause/resume hooks, and query invalidation callbacks.
- **Database changes:** None beyond IndexedDB version 1.
- **Tests:** Online success, offline no-send, reconnect, partial batch mapping, retry schedule/max, 401 pause, login resume, and duplicate-run prevention.
- **Acceptance:** A generic pending operation reaches the batch API once, survives transient failure, and resumes with the same ID.
- **Dependencies:** 011B and 011C.
- **Definition of Done:** Coordinator unit tests plus frontend gates pass; no operational form is offline-enabled yet.

### 20.4 MVP-GAP-011E — Vehicle Reading Workflow

**Status:** COMPLETE — implementation evidence is recorded in `MVP-GAP-011E-IMPLEMENTATION.md`.

- **Objective:** Enable queue-first manual odometer/engine-hour capture against existing Vehicles.
- **Backend changes:** Add the typed Fleet handler/public application boundary and reuse `VehicleReadingService` locking, validation, idempotency, snapshot, and event behavior.
- **Frontend changes:** Adapt only `useRecordManualReading`/`VehicleReadingsSection`; render local reading state and invalidate normal Fleet queries after apply.
- **Database changes:** No migration beyond V29.
- **Tests:** Typed handler, permissions, replay/mismatch, chronology conflict, UI pending/synced/conflict, reload and online-immediate behavior.
- **Acceptance:** Offline reading capture is durable and applies once through Fleet; correction/reset remain online-only.
- **Dependencies:** 011B–011D and existing `VEHICLE_READING_CREATE` behavior.
- **Definition of Done:** First complete offline vertical passes backend/frontend integration without changing Vehicle-reading business rules.

### 20.5 MVP-GAP-011F — Trip Operational Event Workflow

**Status:** COMPLETE — implementation evidence is recorded in `MVP-GAP-011F-IMPLEMENTATION.md`.

- **Objective:** Enable queue-first checkpoint, delay, and incident capture against existing Trips.
- **Backend changes:** Add the typed Trip handler/public application boundary and invoke `TripOperationalEventService`; keep history and `OperationalNotificationEvent` on the normal Trip path.
- **Frontend changes:** Adapt the three Trip event hooks/forms and render local timeline items.
- **Database changes:** No migration beyond V29; no duplicate Trip-event idempotency table.
- **Tests:** All event payloads, invalid Trip state, permission, replay/concurrency, history behavior, and one successful notification side effect for one applied delay/incident.
- **Acceptance:** Reconnect creates one Trip event and never calls Notification from Offline Sync.
- **Dependencies:** 011B–011E and the public Notification event boundary.
- **Definition of Done:** Both frozen workflows operate through the same queue/coordinator and module verification remains green.

### 20.6 MVP-GAP-011G — Status and Conflict UX

- **Objective:** Complete global/per-record state, manual sync, retry, refresh/open, and confirmed discard.
- **Backend changes:** Only sanitized result/current-state summary refinements within the frozen contract, if tests show they are necessary.
- **Frontend changes:** Add header indicator/panel and reusable status/actions; integrate with Vehicle and Trip surfaces using Ant Design App/modal APIs.
- **Database changes:** None.
- **Tests:** Counts/statuses, permission failure, conflict details, retry eligibility, owner isolation, manual sync, and explicit discard.
- **Acceptance:** Users never confuse local capture with server confirmation and can resolve every terminal local state without raw storage access.
- **Dependencies:** 011E and 011F.
- **Definition of Done:** Accessibility/component tests and all frontend gates pass with no generic merge UI.

### 20.7 MVP-GAP-011H — Offline Playwright

**Status:** COMPLETE (2026-08-23) — implementation evidence is recorded in `MVP-GAP-011H-IMPLEMENTATION.md`.

- **Objective:** Automate the frozen three-browser E2E-OFF acceptance contract.
- **Backend changes:** Add only `e2e`-profile deterministic failure/clock/permission fixtures that are impossible to expose in production.
- **Frontend changes:** Add page objects/helpers for real UI and read-only IndexedDB assertions where needed.
- **Database changes:** None.
- **Tests:** E2E-OFF-001 through E2E-OFF-015 as retained by section 18, three browsers, plus all existing 156.
- **Acceptance:** Offline/reconnect/reload/idempotency/conflict/auth/manual-sync behavior is deterministic without arbitrary waits.
- **Dependencies:** 011G and the self-starting Playwright harness.
- **Definition of Done:** New offline executions and the prior 156 all pass in Chromium, Firefox, and WebKit.

### 20.8 MVP-GAP-011I — Regression and Closure

**Status:** COMPLETE (2026-08-23) — closure evidence is recorded in `MVP-GAP-011I-US71-CLOSURE.md`.

- **Objective:** Run every backend/frontend/browser/startup/migration gate, reconcile documentation, and decide US-71 completion from evidence.
- **Backend changes:** Fix only US-71 regressions discovered by full verification; no release-candidate feature work.
- **Frontend changes:** Fix only US-71 regressions discovered by full verification.
- **Database changes:** Confirm clean H2 V1–V29 and PostgreSQL compatibility; never edit V1–V28.
- **Tests:** Clean test/verify, 15+ architecture checks, context/startup, frontend lint/unit/build, offline suite, and full Playwright regression.
- **Acceptance:** Every frozen criterion is evidenced with zero failures/errors and no unresolved P0/P1 US-71 defect.
- **Dependencies:** 011B–011H complete.
- **Definition of Done:** Only then update MVP to 39 complete, 0 partial, 0 not implemented, 100%, and recommend `MVP-RELEASE-CANDIDATE-001`.

### 20.9 Current Slice State

| Slice | Status | Scope |
|---|---|---|
| MVP-GAP-011A | COMPLETE | Contract and operation scope frozen in this document |
| MVP-GAP-011B | COMPLETE | Native IndexedDB v1 stores/indexes, typed envelope, stable client ID, guarded transitions, owner isolation, atomic leases, retry metadata, retention/capacity, and 18 deterministic tests |
| MVP-GAP-011C | COMPLETE | Authenticated bounded batch API, per-item transactions/current RBAC, handler registry, canonical SHA-256, durable V29 inbox, replay/mismatch/concurrency/rollback semantics, and backend verification |
| MVP-GAP-011D | COMPLETE | Authenticated typed client, startup/login/reconnect/manual coordination, 50-item batching, 30-second IndexedDB leases, exact bounded retry, auth pause/resume, owner isolation, recovery/purge, and post-apply extension point |
| MVP-GAP-011E | COMPLETE | Queue-first manual Vehicle readings, typed Fleet handler/public boundary, existing Fleet validation and idempotency, owner-scoped local status, and post-apply reconciliation |
| MVP-GAP-011F | COMPLETE | Queue-first Trip checkpoints/delays/incidents, typed handlers/public boundary, existing lifecycle/history/notification path, any-of Trip authority, idempotency/concurrency, local timeline, and post-apply reconciliation |
| MVP-GAP-011G | COMPLETE | Owner-scoped global/per-record status, manual sync, action matrix, safe terminal diagnostics, retry/refresh/open, confirmed discard, auth pause, and reactive counts |
| MVP-GAP-011H | COMPLETE | E2E-OFF-001..015 use real UI capture, real IndexedDB, real browser offline switching, deterministic e2e-profile controls, and pass 45/45 across Chromium, Firefox, and WebKit; full retained regression passes 201/201 |
| MVP-GAP-011I | COMPLETE | All 12 frozen criteria reconciled; clean backend/architecture/startup/Flyway/frontend gates, 45/45 offline acceptance, and 201/201 full regression pass |

US-71 is **COMPLETE** after 011I. All 12 frozen acceptance criteria pass, slices 011A–011I are complete, and the complete release-gate evidence is recorded in `MVP-GAP-011I-US71-CLOSURE.md`. The MVP is now 39 complete, 0 partial, and 0 not implemented, with 100.00% verified completion and 100.00% weighted functional coverage. The next task is `MVP-RELEASE-CANDIDATE-001`; this status does not declare the system production ready.

## 21. Full US-71 Definition of Done

US-71 is complete only when all frozen offline operations persist before acknowledgement, survive local lifecycle, synchronize through the authenticated/authorized bounded API, apply at most once, report independent partial results, expose actionable terminal state, and pass the complete backend/frontend/three-browser verification described above. Online-only and deferred scopes must remain unchanged. Completion cannot be inferred from reusable idempotency, documentation, or infrastructure alone.
