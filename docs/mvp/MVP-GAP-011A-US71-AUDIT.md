# MVP-GAP-011A — US-71 Current-State Audit

**Parent:** MVP-GAP-011-US71
**Story:** US-71 — Support Offline Data Synchronization
**Mode:** Analysis / contract freeze / gap audit
**Date:** August 22, 2026
**Result:** 011A COMPLETE; US-71 remains NOT IMPLEMENTED

## 1. Executive Summary

The actual repository confirms the documented status. There is no genuine offline synchronization implementation: no IndexedDB store, durable browser operation queue, operation envelope, connectivity coordinator, reconnect processing, backend synchronization endpoint, general server inbox, partial batch result contract, offline status UI, or offline Playwright test.

The repository does contain useful but isolated infrastructure: manual Vehicle-reading idempotency and chronology locking, Trip operational-event append behavior, JWT refresh, current RBAC, TanStack Query, public health, normal Trip-to-Notification events, notification retry infrastructure, and a self-starting three-browser Playwright harness. None of these captures a command durably while disconnected and later synchronizes it, so US-71 remains NOT IMPLEMENTED rather than PARTIAL.

The smallest complete MVP is the queue-first IndexedDB and server-inbox design frozen in `US71-MVP-OFFLINE-SYNC-CONTRACT.md`, limited to manual Vehicle readings and Trip checkpoints/delays/incidents against existing aggregates.

## 2. Change-Control Baseline

| Item | Value before 011A |
|---|---|
| Branch | `feature/mvp-gap-008i-us77-closure` |
| HEAD | `44f1cb8a18205cad4820b1e01fa85aead91bdf10` |
| Tracked diff | None |
| Pre-existing documentation change | Untracked `docs/mvp/US71-MVP-OFFLINE-SYNC-CONTRACT.md` from the parent contract-freeze turn |
| Other pre-existing untracked files | Four `hs_err_pid*.log` and two `replay_pid*.log` JVM diagnostics |

No production source, test, dependency, migration, or runtime configuration is changed by 011A.

## 3. Source and Authority Audit

### 3.1 Sources Located and Read

- `docs/mvp/MVP-CURRENT-STATUS-COMPARE-002.md`
- `docs/mvp/MVP_V2_CURRENT_STATUS_AUDIT.md`
- `docs/status/MVP-IMPLEMENTATION-AUDIT.md`
- `docs/mvp/MVP-CURRENT-STATUS-REASSESSMENT-001.md`
- `docs/mvp/MVP-traceability-matrix.md`
- `docs/mvp/MVP-current-status.md`
- `docs/full-capability-gap-audit.md`
- `docs/openapi-contract-inventory.md`
- `docs/phase-1-api-contract.md`
- `docs/architecture/CURRENT-PROJECT-STATUS.md`
- `docs/development/adapter-convention.md`
- `docs/adr/ADR-vehicle-reading-authority.md`
- `docs/qa/PLAYWRIGHT-MVP-COVERAGE-MATRIX.md`
- `docs/qa/PLAYWRIGHT-MVP-TEST-CASES.md`
- `docs/qa/PLAYWRIGHT-DISCOVERED-DEFECTS.md`
- the current MVP-GAP-011 and MVP-GAP-011A task specifications

No repository file named or containing the referenced “Mind-Map-Transportation-and-Logistic” documentation was found. No separate source requirements document provides an Actor or canonical “As a …” story sentence for US-71. The audit records those fields as unavailable rather than inventing them.

### 3.2 Authoritative Story

| Attribute | Actual evidence |
|---|---|
| ID | US-71 |
| Title | Support Offline Data Synchronization (`MVP-CURRENT-STATUS-COMPARE-002`); shortened to Support Offline Data Sync in the V2 inventory |
| Actor | Not specified in available repository sources |
| Canonical user-story sentence | Not specified in available repository sources |
| Source acceptance capabilities | Offline transaction queue; client-side store-and-forward; transactional sync conflict resolution |
| Current status | NOT IMPLEMENTED |
| Current priority | Final mandatory selected-MVP gap and release blocker |
| Historical priority | P2 / Phase 3 mobile, explicitly deferred by older audits |
| Current dependency decision | Existing React field UI, stable Fleet/Trip behavior, auth/RBAC, and green E2E harness; no separate mobile app required |

### 3.3 Conflict Resolution

Older audits treated offline sync as a future mobile/PWA capability, suggested separate `/api/v1/sync/*` endpoints, and referenced a service worker/mobile architecture. The latest current-status comparison reports 38/39 and makes US-71 the remaining release blocker. The current task explicitly requires a bounded React IndexedDB implementation and says a service worker is optional only if independently necessary.

This is a material historical scope change, but it is resolved by newer and task-specific authority. The contract adopts the current mandatory web-client slice and preserves mobile/PWA/full offline-first support as deferred.

## 4. Repository Search Results

Searches covered `offline`, `sync`, `synchronization`, `IndexedDB`, `Dexie`, `idb`, `localForage`, `serviceWorker`, `navigator.onLine`, browser online/offline events, queue, pending operation, retry, idempotency, client operation ID, conflict, `@Version`, optimistic locking, reconnect, background sync, and sync status.

| Concept | Result | Exact evidence |
|---|---|---|
| IndexedDB/Dexie/idb/localForage | MISSING | No production/frontend matches; no dependency in `frontend/package.json` |
| Service worker/PWA/background sync | MISSING | No worker registration, manifest, or Background Sync code |
| Connectivity listener/coordinator | MISSING | No `navigator.onLine` or browser online/offline listener |
| Durable operational queue/envelope | MISSING | No feature types/store/coordinator/hooks |
| Sync endpoint | MISSING | No Offline Sync controller/use case/DTO/module |
| General operation idempotency | MISSING | No cross-operation inbox or processed-operation table |
| Vehicle-reading idempotency | EXISTS, REUSABLE | `VehicleReadingService`, repository, `idempotency_key`, V14 unique index |
| Bunker movement uniqueness | EXISTS, NOT A SYNC PIPELINE | V18 unique movement identity is owning-workflow protection |
| Notification retry/idempotency | EXISTS, NOT REUSABLE AS QUEUE | Notification delivery attempts/worker are server email delivery infrastructure, not client mutation sync |
| Optimistic aggregate versions | LIMITED | `@Version` appears on Notification rule policy only; Vehicle/Trip operations do not expose a generic version contract |
| TanStack Query retry/cache | EXISTS, NOT DURABLE | Query defaults in `frontend/src/main.tsx`: one retry and 60-second stale time; cache is memory-only |
| Auth token persistence/refresh | EXISTS | `frontend/src/api/client.ts`, `AuthContext.tsx`; tokens in localStorage, one refresh retry |
| Sync status UI/manual sync | MISSING | `AppLayout` has system/notification status but no offline status/actions |
| Offline Playwright | MISSING | No offline/network/IndexedDB scenario in `frontend/e2e` |

## 5. Frontend Current-State Audit

| Area | Status | Evidence and assessment |
|---|---|---|
| React | EXISTS | React 19.1.1 in `frontend/package.json` |
| Routing | EXISTS | React Router routes in `frontend/src/App.tsx` |
| Server state | EXISTS | TanStack Query 5; query retry 1, memory cache, mutation hooks per feature |
| API client | EXISTS | Axios client at `/api`, JWT request interceptor and single refresh/replay in `frontend/src/api/client.ts` |
| Authentication state | EXISTS | `AuthContext`, `/auth/me`, permission helper, login/logout |
| Credential storage | EXISTS WITH EXISTING RISK | Access and refresh tokens use localStorage; US-71 must not copy either into IndexedDB |
| Global app state | LIMITED | React context plus TanStack Query; no general durable/global state store |
| Component system | EXISTS | Ant Design shell/components and centralized theme |
| Error handling | EXISTS FOR ONLINE REQUESTS | Axios/ApiError consumption and component messages; no offline classification |
| Existing persistence | AUTH ONLY | localStorage contains tokens; no business operation persistence |
| Connectivity | MISSING | No actual reachability state or browser connectivity events |
| Durable queue | MISSING | No IndexedDB abstraction or operation state model |
| Existing mutation path | ONLINE ONLY | `useVehicleReadings.ts` and `useTripOperationalEvents.ts` post directly through Axios |
| Global/per-record sync UX | MISSING | No pending/syncing/failed/conflict status |

### 5.1 Storage Decision

Use native IndexedDB behind a narrow adapter. No existing IndexedDB library is present. Four operation schemas and two stores do not justify adding Dexie or a larger offline framework. Native IndexedDB has more ceremony, so 011B must isolate it behind a promise-based feature interface and test transaction completion/error paths. No dependency is installed in 011A.

## 6. Backend Current-State Audit

| Area | Status | Exact evidence and reuse decision |
|---|---|---|
| Generic sync API/batch | MISSING | No endpoint, request/result model, handler registry, or per-item orchestration |
| General durable idempotency | MISSING | V1–V28 have no client-operation inbox |
| Request hash/mismatch detection | PARTIAL, FLEET ONLY | Vehicle reading compares same-key facts; no canonical generic request hash |
| Partial result pattern | MISSING FOR SYNC | Existing APIs are single-command; no one-result-per-item batch contract |
| Conflict primitives | REUSABLE | Shared `ConflictException`, ApiError mapping, Fleet chronology codes, Trip `CANNOT_RECORD_EVENT` |
| Transaction boundaries | REUSABLE BUT NEED ORCHESTRATION | Vehicle readings use an explicit transaction/vehicle lock; Trip event service is transactional; batch still needs one transaction per item |
| Concurrency controls | REUSABLE LOCALLY | Fleet parent row lock and unique reading key; no unique offline operation identity |
| Aggregate versioning | NOT SUITABLE | No public Vehicle/Trip expected-version contract; selected append operations do not require it |
| Public Fleet boundary | PARTIAL | Root `VehicleReadingRecorder` exists for system sources but omits manual idempotency/notes; a minimum public offline recording contract/adapter is needed |
| Public Trip event boundary | MISSING | `TripOperationalEventUseCase` is internal; a minimum public root contract is needed |
| Authentication/RBAC | REUSABLE | Stateless JWT, refresh rotation, `/auth/me`, current authorities, shared 401/403 behavior |
| Health | REUSABLE | Public `GET /health` under external `/api/health` |
| Notification path | REUSABLE | Trip publishes public `OperationalNotificationEvent`; Offline Sync must not import Notification internals |

## 7. Vehicle Reading Workflow Audit

### 7.1 Current Implementation

| Concern | Evidence |
|---|---|
| Endpoint | `POST /vehicles/{vehicleId}/readings` in `VehicleReadingController` |
| Request | `RecordManualVehicleReadingRequest`: type, non-negative value, required recorded time, optional idempotency key/notes |
| Application boundary | `VehicleReadingUseCase.RecordCommand` |
| Service | `VehicleReadingService.record` / `recordLocked` |
| Domain | `VehicleReading`, `VehicleReadingChronologyPolicy` |
| Persistence | `VehicleReadingEntity`, JPA repository, persistence adapter, V14 `vehicle_reading` |
| Idempotency | unique `idempotency_key`; equivalent same-key replay returns existing reading, changed facts conflict |
| Concurrency | parent Vehicle pessimistic lock before chronology/replay checks |
| Business rules | active Vehicle, manual source, non-negative/max-three-decimal value, required key/time/actor, future tolerance, epoch chronology, duplicate reading prevention |
| Permission | `VEHICLE_READING_CREATE` |
| Side effects | updates Vehicle reading snapshot and publishes `VehicleReadingRecorded` after commit |
| Frontend | `VehicleReadingsSection.tsx`, `useRecordManualReading` |
| Tests | domain/service/controller/security/API/repository tests and `runningLogs.spec.ts` |

### 7.2 Suitability

**Suitable as Offline Workflow #1.** It is append-style, already accepts client event time and idempotency, uses a stable business permission, and has deterministic chronology conflicts. The sync handler must force `MANUAL`, use the offline operation UUID as the reading idempotency key, and call Fleet application behavior. Reading correction and meter reset remain online-only.

## 8. Trip Operational Event Workflow Audit

### 8.1 Current Implementation

| Concern | Evidence |
|---|---|
| Endpoints | `POST /trips/{id}/checkpoints`, `/delays`, `/incidents`; reads under `/operational-events` in `TripController` |
| Requests | `TripCheckpointRequest`, `TripDelayRequest`, `TripIncidentRequest` with field/size validation |
| Application boundary | internal `TripOperationalEventUseCase` |
| Service | transactional `TripOperationalEventService` |
| Domain | `TripOperationalEvent`, type/checkpoint/severity enums |
| Persistence | `TripOperationalEventEntity`, repository/adapter, V24 table |
| Stable operation identity | MISSING; service generates a new server event UUID per call |
| Current idempotency | MISSING for repeated HTTP command |
| State conflict | Draft, Submitted, Rejected, Cancelled, and Closed reject with `CANNOT_RECORD_EVENT` |
| Permission | any of `TRIP_DISPATCH`, `TRIP_LOG_MANAGE`, `TRIP_UPDATE` |
| Side effects | Trip event row; auxiliary Trip history; delay/incident public `OperationalNotificationEvent` |
| Frontend | `TripOperationalEventsSection.tsx`, three direct Axios mutation hooks |
| Tests | domain/service/controller/persistence/component and `tripOperationalEvents.spec.ts` |

### 8.2 Suitability

**Suitable as Offline Workflow #2.** Checkpoint, delay, and incident are append operations with explicit field timestamps and contained state conflicts. V29 inbox identity must guard the call because the Trip service itself is not idempotent. Offline Sync invokes a new narrow Trip public contract; Trip retains persistence/history/notification behavior. A successfully applied delay/incident must exercise the normal notification path once. Offline Sync never calls Notification directly.

The current Trip service intentionally isolates notification/history publication failures from the accepted primary event. US-71 does not change that business decision; its exactly-once guarantee covers invoking the primary Trip command at most once, not upgrading auxiliary notification delivery guarantees.

## 9. Plausible Field-Operation Inventory

| Operation | Classification | Evidence/reason |
|---|---|---|
| Vehicle odometer reading | OFFLINE_REQUIRED | Existing manual append ledger with idempotency and strong field value |
| Vehicle engine-hour reading | OFFLINE_REQUIRED | Same existing manual append contract |
| Trip checkpoint | OFFLINE_REQUIRED | Existing append Trip-log workflow |
| Trip delay | OFFLINE_REQUIRED | Existing append event and normal notification path |
| Trip incident | OFFLINE_REQUIRED | Existing append event and normal notification path |
| Trip start | ONLINE_ONLY | Lifecycle transition, assignment/readiness validation, and authoritative start reading |
| Trip completion/close | ONLINE_ONLY | Lifecycle/version-like state, end odometer, audit, and business conflicts |
| Fuel issue/log | ONLINE_ONLY | Authorization, price/limit, station/bunker stock, current balance, and financial audit |
| Lubricant log | OFFLINE_OPTIONAL, DEFERRED FROM FROZEN MVP | Append-like but not required by source acceptance criteria; adding a third handler expands data/UX/test scope |
| Vehicle inspection | OUTSIDE_CURRENT_MVP | No Vehicle-inspection aggregate/API/UI found; maintenance descriptions are not an inspection workflow |
| Driver field event | ONLINE_ONLY | Existing exception/violation/medical/drug workflows affect eligibility and may contain sensitive data |
| Delivery checkpoint | OFFLINE_REQUIRED as Trip checkpoint only | `DELIVERY` checkpoint type exists; no proof artifact is implied |
| Proof of delivery/signature/photo | OUTSIDE_CURRENT MVP | Explicitly deferred in capability/release docs; no delivery/proof aggregate or API |
| Reading correction/meter reset | ONLINE_ONLY | Changes ledger interpretation/epoch and requires current server chronology |

No `OFFLINE_OPTIONAL` operation is included in implementation slices 011B–011I. The label records a plausible future candidate without inflating the frozen scope.

## 10. Reusable Infrastructure vs Actual US-71

### Reusable

- Fleet manual-reading validation, idempotency, unique index, vehicle lock, chronology conflict codes, snapshot update, and events.
- Trip checkpoint/delay/incident validation, state policy, transaction boundary, history, and public notification event.
- Shared `ApiError`, correlation ID, `BusinessRuleException`, `ConflictException`, and `NotFoundException` conventions.
- JWT authentication, refresh rotation, `/auth/me` actor ID/permissions, and business authorities.
- Public `/health` endpoint for low-frequency reachability checks.
- Axios client, TanStack Query invalidation, Ant Design App/layout, React Hook Form/Zod patterns.
- Playwright `webServer`, generated local administrator, H2 sample data, three-browser projects, and eventual UI helpers.
- H2/PostgreSQL Flyway and Testcontainers/invariant test conventions.

### Not US-71 and Not Directly Reusable as Its Queue

- TanStack Query memory cache does not durably store operations.
- JWT/refresh localStorage is credential persistence, not a business queue.
- Notification email delivery retry handles server-side provider delivery, not browser mutation replay.
- Vehicle-reading idempotency covers one endpoint only and cannot produce generic partial batch results.
- Bunker uniqueness and lifecycle locks protect their owning workflows but do not create a synchronization inbox.
- Trip operational events have no client operation ID or replay guard today.

## 11. System vs Required System Matrix

| Capability | Required? | Current implementation | Evidence | Reusable? | Gap | Target slice |
|---|---|---|---|---|---|---|
| IndexedDB | Yes | None | Negative frontend/dependency search | No | Durable store | 011B |
| Durable queue | Yes | None | Direct mutations only | No | Queue API/transitions | 011B |
| Operation envelope | Yes | None | No types/store | No | Frozen typed envelope | 011B |
| Stable operation ID | Yes | Reading key only | Vehicle request key; no generic UUID | Partial | Generate/persist once | 011B/011C |
| Payload version | Yes | None | No sync schema | No | `operationVersion=1` | 011B/011C |
| General idempotency | Yes | None | No inbox table | Reading only | Durable server inbox | 011C |
| Request hash mismatch | Yes | Reading fact comparison only | `requireEquivalentReplay` | Concept reusable | Canonical SHA-256 | 011C |
| Batch API | Yes | None | No endpoint | No | Bounded authenticated endpoint | 011C |
| Partial success | Yes | None | Single-command APIs | No | Per-item transaction/result | 011C |
| Handler routing | Yes | None | No registry | Existing module contracts pattern | Typed registry/contracts | 011C/011E/011F |
| Version conflicts | No for frozen operations | Limited `@Version` elsewhere | Notification policy only | No need | Keep `baseVersion=null` | Contract only |
| Business conflicts | Yes | Owning exceptions exist | Fleet chronology; Trip state | Yes | Map to sync result | 011E/011F |
| Persisted retry | Yes | Notification retry only | Server email worker | Policy idea only | Client retry metadata/timer | 011B/011D |
| Reconnect | Yes | None | No connectivity listeners | Health endpoint | Coordinator triggers | 011D |
| Backend reachability | Yes | Public health | `/health` | Yes | Bounded probe/client interpretation | 011D |
| Auth recovery | Yes | Axios refresh and session-expired redirect | API/Auth context | Yes | Pause queue/resume same user | 011D |
| Authorization re-check | Yes | Direct endpoints protected | SecurityConfig | Yes | Per-item authority evaluation | 011C/011E/011F |
| Multi-user isolation | Yes | None for business data | `/auth/me` has user ID | Partial | Owner-keyed IndexedDB queries | 011B/011D |
| Global sync status | Yes | None | App header has no sync state | Layout reusable | Indicator/panel | 011G |
| Per-record status | Yes | None | No local rows | Existing sections reusable | Local overlay/items | 011E/011F/011G |
| Manual sync | Yes | None | No coordinator | No | Header action | 011G |
| Retry/discard/open/refresh | Yes | None | No terminal queue UX | Ant components reusable | Safe actions | 011G |
| Retention/capacity | Yes | None | No business store | No | 7-day purge/1,000 cap | 011B |
| Vehicle reading offline | Yes | Online only | Fleet files/tests above | Strongly | Hook/handler integration | 011E |
| Trip event offline | Yes | Online only | Trip files/tests above | Strongly | Inbox + hook/handler integration | 011F |
| Notification side effect | Yes for delay/incident normal path | Exists online | `OperationalNotificationEvent` | Yes | Prove sync uses Trip path once | 011F/011H |
| Offline Playwright | Yes | None | No offline specs | Harness reusable | 15 logical cases x 3 | 011H |
| Cross-browser | Yes | Existing 156 pass | Playwright config/projects | Yes | Add offline cases | 011H |

## 12. Frozen Architecture Recommendation

### Backend

Use a new focused `com.transportlogistics.app.offlinesync` Spring Modulith module because synchronization owns a distinct inbox/idempotency aggregate, batch contract, and orchestration lifecycle. Follow existing package names:

```text
offlinesync/
  domain/model/
  application/ports/in/
  application/ports/out/
  application/service/
  infrastructure/adapters/in/web/controllers/
  infrastructure/adapters/in/web/dto/request|response/
  infrastructure/adapters/out/persistence/
  infrastructure/config/
```

The module calls narrow public Fleet/Trip root contracts. It imports no owning-module application-internal package, JPA repository/entity, or Notification implementation. Batch orchestration is not placed in `shared` or a controller.

### Frontend

Use:

```text
frontend/src/features/offlineSync/
  types.ts
  storage.ts
  queue.ts
  syncClient.ts
  syncCoordinator.ts
  hooks.ts
  components/
  *.test.ts(x)
```

Fleet/Trip hooks call feature-owned offline mutation functions; they never use raw IndexedDB. `AppLayout` hosts only the compact global component/provider integration.

## 13. V29 Decision

**V29 is required.** Existing Vehicle idempotency cannot protect Trip events or return durable generic replay results, and reusing business tables would couple Offline Sync to owning persistence.

Proposed migration: `V29__create_offline_sync_operations.sql`.

The table/constraints/indexes are frozen in the contract: UUID operation primary key, operation type/version, actor/client instance, aggregate identity, SHA-256 request hash, terminal result/code/version, processed/created timestamps, actor/aggregate diagnostic indexes, and H2/PostgreSQL-compatible checks. V1–V28 remain untouched. Server rows are not purged in MVP because deletion weakens durable replay protection.

## 14. Security and Data Review

| Concern | Decision |
|---|---|
| Token expires offline | Keep operation PENDING; existing refresh may recover; otherwise require login and resume |
| Logout | Retain queue associated with user; do not silently delete field data |
| Different user signs in | Do not show/claim/sync previous user's operations |
| Server actor | Always derive from current JWT; never trust local owner/actor as authority |
| Permission revoked | Item `REJECTED / OFFLINE_SYNC_FORBIDDEN` before domain mutation |
| Credentials | Never store password/access/refresh token in IndexedDB |
| Payload sensitivity | Frozen Vehicle/Trip event payloads contain operational facts and free text but exclude medical, drug, identity, financial, and credential data |
| IndexedDB at rest | Origin-scoped, not application-encrypted; physical/browser-profile access risk must be documented |
| Free text | Existing size limits retained; users must not enter secrets; server messages sanitized |

## 15. Test and Playwright Recommendation

The detailed backend/frontend strategy and E2E-OFF-001 through E2E-OFF-015 are frozen in the contract. Key gates are per-item transaction/idempotency concurrency, native IndexedDB reopen/ownership, retry/auth recovery, normal Trip notification publication, and three-browser offline/reconnect behavior. Existing 156 Playwright executions remain mandatory and additive.

011A writes no tests because its Definition of Done is documentation and evidence only.

## 16. Risk Assessment

| Risk | Likelihood/impact | Mitigation |
|---|---|---|
| Cross-module transaction/inbox could commit separately from business mutation | Medium / Critical | One item-level Spring transaction; handler joins it; integration test rollback and concurrent duplicate |
| Trip event has no native idempotency | High / High | V29 inbox unique operation identity before invoking Trip; same transaction |
| Native IndexedDB ceremony/error handling | Medium / Medium | Narrow promise adapter, deterministic tests, no raw page access |
| React StrictMode/multiple tabs start duplicate coordinators | High / Medium | In-memory single-flight plus IndexedDB claim lease/expiry |
| Same browser used by multiple users | Medium / High | Required owner user ID and owner-filtered queries/actions; same-user resume only |
| Existing localStorage refresh token risk | Existing / High | Do not expand it into IndexedDB; US-71 does not redesign authentication |
| Device clock inaccurate | Medium / Medium | Client time is evidence; server receipt time authoritative for processing/security; owning validation remains |
| App cannot reload while fully network-disconnected without PWA | Expected / Low | Explicitly outside MVP; queue survives storage lifecycle and resumes when assets/app become available |
| Browser offline/reload timing differs | Medium / Medium | Eventual assertions, asset/backend failure separation, all three browsers |
| Free-text operational payload exposed in browser profile | Medium / Medium | Minimum fields, no sensitive workflows, user isolation, explicit discard, capacity/retention |
| Notification publication is intentionally failure-isolated | Existing / Medium | Test normal successful path exactly once; do not claim stronger delivery semantics than Trip currently owns |
| V29 purge could re-enable old operations | High / High | No server purge in MVP |

No stop condition remains unresolved. The authority conflict is explicitly resolved by the newer task, sensitive workflows are excluded, no aggregate creation/temp IDs are required, native IndexedDB is supported by the retained browsers, and the current transaction/module patterns can support the proposed inbox through explicit public contracts.

## 17. Implementation Slices

| Slice | Objective | Dependency | Acceptance/DoD summary |
|---|---|---|---|
| 011B | Native IndexedDB envelope/queue | 011A | Durable reopen, ownership, lease, retry metadata, retention/capacity; frontend gates |
| 011C | Backend batch + V29 idempotency | 011A | Per-item results/transactions, replay/mismatch/concurrency, security, architecture, V1–V29 |
| 011D | Coordinator/reconnect/retry/auth | 011B/011C | Single-flight, reachability, persisted retry, 401 pause and same-user resume |
| 011E | Manual Vehicle readings | 011B–011D | Offline capture/reload/reconnect/apply-once/conflict through Fleet |
| 011F | Trip checkpoint/delay/incident | 011B–011E | Apply once through Trip with normal history/notification path |
| 011G | Conflict/status UX | 011E/011F | Header/per-record state, manual sync, safe retry/open/refresh/discard |
| 011H | Offline Playwright | 011G | Frozen 15 logical cases across Chromium/Firefox/WebKit plus prior 156 |
| 011I | Regression/closure | 011B–011H | All quality/startup/Flyway/E2E gates; only then 39/39 and 100% |

Each slice's backend/frontend/database/test details and Definition of Done are specified in section 20 of the contract.

## 18. Final Classification and Recommendation

| Metric | After 011A |
|---|---:|
| Total selected MVP stories | 39 |
| Complete | 38 |
| Partial | 0 |
| Not implemented | 1 |
| Verified completion | 97.44% |
| Functional coverage | 97.44% |

US-71 remains **NOT IMPLEMENTED**. Contract/audit documentation and reusable server idempotency are not a meaningful offline vertical implementation.

Release status remains **NOT READY** only because US-71 is the final mandatory functional gap. The exact next task is **MVP-GAP-011B — IndexedDB durable queue and operation envelope**. Do not begin 011B as part of this audit.
