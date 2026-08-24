# MVP-GAP-011H Implementation Evidence

**Story:** US-71 — Support Offline Data Synchronization  
**Slice:** MVP-GAP-011H — Complete Offline Playwright Acceptance Matrix  
**Status:** COMPLETE  
**Date:** August 23, 2026  
**US-71 status after this slice:** PARTIAL (formal closure remains MVP-GAP-011I)

## Scope Delivered

Exactly 15 frozen logical cases were added under `frontend/e2e/tests/offlineSync`. Every case executes in Chromium, Firefox, and WebKit, producing exactly 45 offline browser executions. The pre-existing 156 executions were retained unchanged, so the repository-standard suite now contains 201 executions.

No new offline business operation, service worker, app-shell cache, Browser Background Sync, PWA behavior, generic merge, retry policy, Flyway migration, or IndexedDB schema version was added.

## Test Structure

| Spec | Cases | Capability |
|---|---|---|
| `vehicle-offline.spec.ts` | E2E-OFF-001..004 | real offline capture, reload durability, reconnect, exactly-once Vehicle replay |
| `trip-offline.spec.ts` | E2E-OFF-005..007 | checkpoint/delay/incident capture, exactly-once Trip apply, normal Notification side effect |
| `offline-outcomes.spec.ts` | E2E-OFF-008..011 | independent batch outcomes, revoked permission, business conflict, transient retry |
| `offline-recovery-status.spec.ts` | E2E-OFF-012..015 | Manual Sync, confirmed discard, status counts/owner isolation, same-context new-page resume |
| `e2e/helpers/offlineSyncTestApi.ts` | shared | real fixture creation, read-only IndexedDB inspection, operation-ID tracking, server outcome checks, narrow deterministic controls |

## Network-Control Strategy

- Core disconnected capture uses `context.setOffline(true)` only after the application, target record, and offline storage are loaded.
- Reconnect uses `context.setOffline(false)` and bounded eventual assertions against both IndexedDB and server business state.
- Reload durability selectively aborts `/api/offline-sync/operations`; Vite/static assets remain reachable. The suite does not claim cold-start app-shell availability.
- Every changed offline state or route is restored. There are no arbitrary sleeps; asynchronous assertions use locator waits or `expect.poll`.

## IndexedDB Assertion Strategy

The helper inspects the real `transport-logistics-offline` database, schema version 1, and the `operations` store in read-only transactions. It checks database/store/metadata readiness before opening a transaction and never creates core-case operations directly. Assertions cover operation ID/type, aggregate, status, attempts, retry due time, and owner.

Core operations are created only through the real Vehicle or Trip UI. Narrow direct state timing is used only where the acceptance behavior is recovery rather than capture:

- make an already-created retry item due for E2E-OFF-012 and E2E-OFF-015;
- defer an already-created retry item to stabilize E2E-OFF-012 and E2E-OFF-014.

These actions preserve the captured operation ID, payload, owner, attempt history, and production retry policy.

## Data and Parallel Isolation

- Tests create unique Vehicles, roles/users, rules, and markers where practical.
- Backend forced outcomes are keyed by the captured operation ID, not global mode.
- IndexedDB is isolated by Playwright BrowserContext and production owner scoping.
- E2E-OFF-014 proves User B cannot see User A's global counts or operation list.
- Tests do not depend on execution order and do not globally delete a shared database.
- The existing three-worker bound and zero-retry policy are retained.

## Server-Control Strategy and Security

`E2eOfflineSyncControl`, `E2eOfflineSyncTestConfiguration`, and `E2eOfflineSyncTestController` provide operation-ID-scoped `APPLIED`, `REJECTED`, `CONFLICT`, `RETRYABLE`, and `BLOCK` outcomes plus safe release/inbox inspection.

The controls:

- are instantiated only under the `e2e` Spring profile;
- reuse the existing protected `/e2e/**` authority convention (`NOTIFICATION_RULE_MANAGE`);
- do not weaken JWT authentication or owning-handler authorization;
- delegate each handler's exact `isAuthorized` behavior;
- expose no credential or production bypass; and
- are covered by `E2eOfflineSyncProfileSafetyTest`, proving controller/configuration absence outside the `e2e` profile.

Production exposure is none.

## Acceptance Strategies

### Permission revocation

E2E-OFF-009 creates an authorized actor through the existing identity administration path, queues through the real UI, removes `VEHICLE_READING_CREATE` from that actor's role, refreshes its real JWT, then reconnects. The server rejects before domain mutation; IndexedDB records `FAILED` / `OFFLINE_SYNC_FORBIDDEN`, and blind Retry is absent.

### Retry and recovery

E2E-OFF-011 forces one `RETRYABLE_ERROR`, verifies the same operation remains `PENDING` with incremented `attemptCount` and `nextAttemptAt`, releases the control, and observes the same ID synchronize once. E2E-OFF-012 invokes the actual global **Sync now** UI, never the sync API directly.

### Exactly once

Vehicle and Trip cases capture the real operation ID from IndexedDB, verify one business mutation, replay the identical envelope through the approved server path, verify `ALREADY_APPLIED`, and recheck that the business count remains one.

### Notification side effect

E2E-OFF-007 creates a normal notification rule, queues a Trip delay offline, reconnects, and verifies one Trip operational event and one notification for that exact event. Replay creates neither a second Trip event nor a second notification. The architecture remains Offline Sync -> Trip -> `OperationalNotificationEvent` -> Notification.

### Independent outcomes and conflict UX

E2E-OFF-008 queues four real UI operations in one uninterrupted offline interval and maps captured IDs independently: APPLIED -> SYNCED, REJECTED -> FAILED, CONFLICT -> CONFLICT, and RETRYABLE_ERROR -> PENDING. E2E-OFF-010 uses a real Vehicle chronology conflict and verifies Conflict, Open, Refresh, and Discard while Retry is absent.

## Defects Found and Fixed

| Classification | Defect | Minimal fix |
|---|---|---|
| PRODUCT DEFECT | TanStack Query paused offline mutations before the queue-first mutation function ran | `networkMode: 'always'` only for the frozen Vehicle-reading and Trip-event queue-first mutations |
| PRODUCT DEFECT | Frontend UUID regex rejected canonical UUID text accepted by Java and present in deterministic records | align validation to canonical hexadecimal 8-4-4-4-12 text; add unit coverage |
| HARNESS DEFECT | E2E handler decorator did not preserve Trip any-authority semantics | delegate `isAuthorized` to the wrapped production handler |
| HARNESS DEFECT | IndexedDB inspection could race initialization or open an absent database | require expected database/version/stores/metadata before read-only access |
| TEST / BROWSER TIMING | reconnect, retry scheduler, drawer overlay, and Ant Select timing differed by engine | uninterrupted offline batches, isolated manual retry lifecycle, stable semantic/eventual selectors; no business-rule change |

No unresolved P0/P1 defect remains.

## Cross-Browser Results

| Browser | Logical cases | Passed | Failed | Skipped |
|---|---:|---:|---:|---:|
| Chromium | 15 | 15 | 0 | 0 |
| Firefox | 15 | 15 | 0 | 0 |
| WebKit | 15 | 15 | 0 | 0 |
| **Total executions** | **45** | **45** | **0** | **0** |

Full retained gate: **201/201 passed** (156 prior executions + 45 offline), 0 failed, 0 skipped.

## Full Regression

| Gate | Result |
|---|---|
| `npm run lint` | PASS |
| `npm test` | PASS — 33 files, 170 tests |
| `npm run build` | PASS — non-blocking existing Vite chunk-size warning |
| focused offline Playwright | PASS — 45/45 |
| `npm run test:e2e` | PASS — 201/201 |
| `./mvnw -B clean test` | PASS — 681 run, 659 passed, 22 skipped, 0 failures/errors |
| `./mvnw -B verify` | PASS — same test result; executable JAR packaged |
| Spring Modulith architecture | PASS — 16/16 |
| Spring context | PASS |
| JPA | PASS — 45 repositories |
| Flyway | PASS — V1-V29 |

The 22 skipped backend cases are PostgreSQL/Testcontainers conditional tests; Docker was unavailable. This matches the governed baseline skip model.

## Database and Deferred Scope

- New migration: none; Flyway remains V1-V29.
- IndexedDB remains version 1.
- No service worker or offline cold-start claim.
- Administrative, lifecycle, allocation, Fuel, and all other frozen online-only commands remain online-only.
- MVP-GAP-011I remains responsible for formal US-71 closure and is not implemented here.

## Files Added by 011H

- `frontend/e2e/helpers/offlineSyncTestApi.ts`
- `frontend/e2e/tests/offlineSync/vehicle-offline.spec.ts`
- `frontend/e2e/tests/offlineSync/trip-offline.spec.ts`
- `frontend/e2e/tests/offlineSync/offline-outcomes.spec.ts`
- `frontend/e2e/tests/offlineSync/offline-recovery-status.spec.ts`
- `src/main/java/com/transportlogistics/app/offlinesync/infrastructure/testing/E2eOfflineSyncControl.java`
- `src/main/java/com/transportlogistics/app/offlinesync/infrastructure/testing/E2eOfflineSyncTestConfiguration.java`
- `src/main/java/com/transportlogistics/app/offlinesync/infrastructure/adapters/in/web/controllers/E2eOfflineSyncTestController.java`
- `src/test/java/com/transportlogistics/app/offlinesync/infrastructure/testing/E2eOfflineSyncProfileSafetyTest.java`
- `docs/mvp/MVP-GAP-011H-IMPLEMENTATION.md`

## Existing Files Modified by 011H

- `frontend/src/fleet/useVehicleReadings.ts`
- `frontend/src/trips/useTripOperationalEvents.ts`
- `frontend/src/features/offlineSync/queue.ts`
- `frontend/src/features/offlineSync/queue.test.ts`
- `docs/mvp/US71-MVP-OFFLINE-SYNC-CONTRACT.md`
- `docs/mvp/MVP-CURRENT-STATUS-COMPARE-002.md`
- `docs/qa/PLAYWRIGHT-MVP-COVERAGE-MATRIX.md`
- `docs/qa/PLAYWRIGHT-MVP-TEST-CASES.md`
- `docs/qa/PLAYWRIGHT-DISCOVERED-DEFECTS.md`

## Status Decision

MVP-GAP-011H is **COMPLETE**. US-71 intentionally remains **PARTIAL**. MVP counts remain 39 total, 38 complete, 1 partial, and 0 not implemented; verified completion remains 97.44% and weighted functional coverage remains 98.72%. The exact next task is MVP-GAP-011I.
