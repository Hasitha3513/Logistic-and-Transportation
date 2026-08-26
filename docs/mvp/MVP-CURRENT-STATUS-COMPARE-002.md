# MVP Current Status Compare 002

## 1. Executive Summary

The current repository has 39 of 39 selected MVP stories complete. MVP-GAP-011I reconciles all 12 frozen US-71 acceptance criteria against current code and executable evidence: E2E-OFF-001..015 pass in Chromium, Firefox, and WebKit (45/45), and the full retained Playwright suite passes 201/201. Verified completion and weighted functional coverage are both 100.00%. No previously complete story regressed.

**MVP-GAP-008I checkpoint:** COMPLETE. All 26 frozen US-77 criteria pass, E2E-NOT-001 through E2E-NOT-015 cover the complete notification vertical, and the complete regression is green in Chromium, Firefox, and WebKit.

The current backend baseline reports 681 tests (659 passed, 22 Docker-conditional skips); all 16 architecture tests pass, the Spring context starts, JPA discovers 45 repositories, and Flyway applies V1-V29. Frontend lint/build, all 170 unit/component tests, the offline 45/45 matrix, and the full 201/201 Playwright regression pass.

The functional MVP is **COMPLETE** and **READY FOR RELEASE-CANDIDATE VALIDATION**. It is not yet declared production ready; deployment, configuration, security hardening, performance, observability, backup/recovery, runbooks, load testing, and environment validation remain release-candidate concerns.

## 2. Previous Baseline

| Metric | Previous verified value |
|---|---:|
| Total stories | 39 |
| Complete | 37 |
| Partial | 1 |
| Not implemented | 1 |
| Verified completion | 94.87% |
| Functional coverage | 96.15% |

At that baseline US-77 was PARTIAL and US-71 was NOT IMPLEMENTED. The accepted MVP interpretation treats deterministic Trip/Fuel state machines as sufficient for US-80 and requested windows, availability, blackout checks, and overlap prevention as sufficient for US-81.

## 3. Current Build/Test Health

| Gate | Command/evidence | Current result |
|---|---|---|
| Backend clean test | `./mvnw -B clean test` | PASS — 681 run, 659 passed, 0 failures, 0 errors, 22 Docker-conditional skips |
| Backend verify | `mvn -B verify` | PASS — full suite green; JAR packaged |
| Spring startup | `ContextSmokeTest` and the Playwright-managed Spring server | PASS |
| Flyway | H2 startup logs during clean verification | PASS — V1 through V29 applied |
| JPA | repository/context suites inside Maven verification | PASS — 45 repositories |
| Frontend lint | `npm run lint` | PASS — zero warnings permitted |
| Frontend unit | `npm test` | PASS — 33 files, 170 tests |
| Frontend build | `npm run build` | PASS — TypeScript and Vite build; non-blocking chunk-size warning |
| Playwright notification gate | `npx playwright test e2e/tests/notifications` | PASS — 45/45; 15/15 in Chromium, Firefox, and WebKit |
| Playwright offline gate | `npx playwright test e2e/tests/offlineSync` | PASS — 45/45; 15/15 in Chromium, Firefox, and WebKit |
| Playwright full regression | `npm run test:e2e` | PASS — 201/201 across Chromium, Firefox, and WebKit (156 retained + 45 offline) |

The retained pre-011H baseline remains 156/156. MVP-GAP-011H adds exactly 45 offline executions; the standard combined command passes 201/201 with zero failures or skips.

Non-blocking frontend test output includes Ant Design deprecation/context warnings and a Vite large-chunk warning. These do not fail the configured quality gates.

## 4. Current Architecture Health

| Suite | Tests | Result |
|---|---:|---|
| `ApplicationModulesTest` | 2 | PASS |
| `HexagonalLayerArchitectureTest` | 7 | PASS |
| `ModuleBoundaryArchitectureTest` | 4 | PASS |
| `LombokUsageArchitectureTest` | 3 | PASS |
| **Total** | **16** | **PASS** |

The public `notification.OperationalNotificationEvent` is the Trip-to-Notification boundary, while Fleet uses its own output port and a Spring adapter to that same public contract. Trip and Fleet do not import Notification internals. Fuel actor attribution uses an application boundary and the Spring context resolves it. The historical Trip -> Notification and Fuel -> Identity/Security regressions are not present.

## 5. Full 39-Story MVP Matrix

All paths are relative to the repository root. Evidence is representative rather than an exhaustive class listing.

| Story | Feature | Previous | Current | Backend evidence | Frontend evidence | Test evidence | Current gap / release impact |
|---|---|---|---|---|---|---|---|
| US-01 | Manage Vehicle Master | COMPLETE | COMPLETE | `fleet/domain/model/Vehicle.java`, `VehicleService`, `FleetController`, V1 | `ResourceListPage`, `ResourceEditorModal` | vehicle service/controller; `vehicles.spec.ts` | None |
| US-02 | Manage Fleet Categories | COMPLETE | COMPLETE | `VehicleCategory`, `VehicleType`, ports/adapters, V1 | resource list/editor | category/type tests; vehicle E2E | None |
| US-03 | Manage Vehicle Documents | COMPLETE | COMPLETE | `VehicleDocument`, service/persistence, V3 | vehicle compliance details | domain/service/repository; `vehicleDocs.spec.ts` | None |
| US-04 | Allocate Vehicles | COMPLETE | COMPLETE | `VehicleAvailabilityService`, Trip/Fleet boundary, locked assignment | `AssignmentDrawers` | availability/concurrency; assignment E2E | None |
| US-05 | Maintain Fuel & Lubricant Logs | COMPLETE | COMPLETE | Fuel Issue slice; `LubricantLog`, V11/V23 | fuel pages; `VehicleLubricantSection` | backend/UI; lubricant and fuel E2E | None |
| US-06 | Maintain Running Logs | COMPLETE | COMPLETE | `VehicleReading`, meter reset, V14-V16 | `VehicleReadingsSection` | reading tests; `runningLogs.spec.ts` | None |
| US-07 | Link Maintenance to Availability | COMPLETE | COMPLETE | `MaintenanceSchedule`, availability integration, V19 | `VehicleMaintenanceSection` | maintenance/availability; E2E | None |
| US-08 | Handle Fleet Allocation Edge Cases | COMPLETE | COMPLETE | status/compliance/period checks, half-open overlap, locks | server eligibility reasons | rejection/concurrency suites | None |
| US-09 | Create Trip Orders | COMPLETE | COMPLETE | `Trip`, `TripService.createTrip`, controller/persistence | `TripListPage`, `TripEditorPage` | service/controller/UI; E2E | None |
| US-10 | Assign Driver and Vehicle | COMPLETE | COMPLETE | `TripService.assignVehicle/assignDriver`, Fleet ports | assignment drawers | eligibility/concurrency; E2E | None |
| US-11 | Assign Route | COMPLETE | COMPLETE | `TripService.assignRoute`, `RouteEligibilityAdapter`, V8 | Trip details assignment UI | service/UI; assignment E2E | None |
| US-12 | Start and End Trip | COMPLETE | COMPLETE | lifecycle transitions and synchronous readings | `LifecycleActions` | lifecycle/reading; E2E | None |
| US-13 | Maintain Trip Log | COMPLETE | COMPLETE | `TripOperationalEvent`, service/controller/persistence, V24 | `TripOperationalEventsSection` | backend/UI; positive/negative E2E | None |
| US-14 | Complete Trip | COMPLETE | COMPLETE | completion/close invariants and odometer validation | completion modal/actions | lifecycle/UI/E2E | None |
| US-15 | Handle Trip Exceptions | COMPLETE | COMPLETE | reject/cancel reasons and conflict release | lifecycle actions | lifecycle/UI/E2E | None |
| US-16 | Authorize Trip | COMPLETE | COMPLETE | submit/approve/reject; permission enforcement | permission-gated actions | authorization/lifecycle/E2E | None |
| US-17 | Define Routes | COMPLETE | COMPLETE | `Route`, use case/service/controller/persistence | route list/editor | domain/service/controller; E2E | None |
| US-18 | Calculate Distance and ETA | COMPLETE | COMPLETE | planned distance and estimated duration persisted | route fields | route tests/E2E | predictive traffic deferred |
| US-19 | Plan Multi-Stop Routes | COMPLETE | COMPLETE | ordered stop IDs and `@OrderColumn`, V5 | ordered stop editor | domain/repository; route E2E | optimization beyond MVP deferred |
| US-31 | Issue Fuel | COMPLETE | COMPLETE | Fuel Issue aggregate/policy/service/controller, V11 | issue list/editor/details | backend, 12 UI tests, E2E | None |
| US-32 | Manage Fuel Purchases | COMPLETE | COMPLETE | purchase/price/vendor lifecycle, V12 | purchase/price pages | backend, 10 UI tests, E2E | None |
| US-33 | Track Mileage | COMPLETE | COMPLETE | Fleet readings/meter reset, V14-V16 | vehicle readings | reading suites/E2E | None |
| US-34 | Allocate Fuel Cost | COMPLETE | COMPLETE | `TripFuelCostService`, controller, Fleet distance port, V17 | `TripFuelCostSection` | backend, UI, E2E | None |
| US-36 | Manage Fuel Bunkers | COMPLETE | COMPLETE | tank/ledger/dip/transfer services, V18 | bunker list/details | backend, 11 UI tests, E2E | None |
| US-39 | Manage Driver Profiles | COMPLETE | COMPLETE | Driver aggregate/service/controller/persistence | driver UI | service/controller/E2E | None |
| US-40 | Manage Driver Licensing | COMPLETE | COMPLETE | licence aggregate/service/persistence and coherent eligibility, V4 | driver details/assignment indicators | licence/availability/assignment tests | None |
| US-41 | Assess Driver Performance | COMPLETE | COMPLETE | `DriverPerformanceService`, summaries/metrics | `DriverPerformanceSection` | service/UI/E2E | None |
| US-42 | Manage Violations | COMPLETE | COMPLETE | violation aggregate/service/controller/persistence, V21 | `DriverViolationsSection` | domain/service/persistence/UI/E2E | None |
| US-43 | Manage Driver Medical Fitness | COMPLETE | COMPLETE | medical records and availability integration, V22 | `DriverMedicalSection` | domain/service/availability/UI/E2E | None |
| US-44 | Manage Drug Tests | COMPLETE | COMPLETE | drug-test lifecycle and eligibility integration, V22 | `DriverDrugTestSection` | domain/service/UI/E2E | None |
| US-45 | Handle Driver Exceptions | COMPLETE | COMPLETE | exception service/persistence and blackout integration, V20 | `DriverExceptionSection` | unit/UI/assignment/E2E | None |
| US-71 | Support Offline Data Synchronization | NOT IMPLEMENTED | COMPLETE | authenticated batch API, V29 durable inbox, canonical hashing, per-item transactions/RBAC/replay, and all four frozen Fleet/Trip handlers/public boundaries complete | native IndexedDB v1 queue/coordinator, queue-first Vehicle readings and Trip events, plus owner-scoped global/per-record status and terminal actions complete | 681 backend tests, 170 frontend tests, E2E-OFF-001..015 at 45/45, and full 201/201 browser regression green | None for frozen MVP scope; deferred PWA/background and broader offline capabilities remain non-blocking |
| US-74 | Manage Security | COMPLETE | COMPLETE | JWT, rotating refresh, BCrypt, permissions, `SecurityConfig` | auth context and permission navigation | identity/security and RBAC E2E | None |
| US-75 | Maintain Audit and Reports | COMPLETE | COMPLETE | histories and report services/controllers | dashboard and detail histories | reporting/history/UI/E2E | advanced analytics deferred |
| US-77 | Manage Notification Rules | PARTIAL | COMPLETE | rule engine, controlled catalogue/templates, recipient validation, quiet/suppression, durable scheduling, exact retry, terminal failure, one-level escalation, real SMTP, eight producers, RBAC, V25-V28 | complete rule modal/page, policy summaries, delivery/attempt diagnostics, notification center/hooks | backend policy/provider/concurrency/security tests; 22 notification-focused frontend tests; 106/106 total frontend; E2E-NOT-001..015 pass 45/45; full regression 156/156 | None for frozen MVP scope; US-77 blocker resolved |
| US-79 | Manage Master Data | COMPLETE | COMPLETE | customer/department/project/location/vendor/station services | resource pages | organization/security tests; journey coverage | None |
| US-80 | Configure Workflows | COMPLETE | COMPLETE | deterministic Trip/Fuel state machines and guarded transitions | lifecycle controls | lifecycle/authorization/E2E | Complete within approved MVP boundary |
| US-81 | Manage Scheduling | COMPLETE | COMPLETE | requested windows, availability, blackout and overlap queries | assignment drawers | availability/concurrency/E2E | Complete within approved MVP boundary |
| US-83 | Manage Documents | COMPLETE | COMPLETE | vehicle documents and driver licences with validity/file refs | fleet/driver details | document/licence tests/E2E | OCR/versioning deferred |

## 6. Module-by-Module Status

| Area | Complete | Partial | Not implemented | Assessment |
|---|---:|---:|---:|---|
| Fleet (US-01-US-08) | 8 | 0 | 0 | Complete |
| Trip (US-09-US-16) | 8 | 0 | 0 | Complete |
| Routing (US-17-US-19) | 3 | 0 | 0 | Complete |
| Fuel (US-31, 32, 33, 34, 36) | 5 | 0 | 0 | Complete |
| Driver (US-39-US-45) | 7 | 0 | 0 | Complete |
| Cross-cutting (US-71, 74, 75, 77, 79, 80, 81, 83) | 8 | 0 | 0 | Complete; US-71 frozen store-and-forward scope closed by 011I |
| **Total** | **39** | **0** | **0** | **100.00% verified completion; 100.00% weighted functional coverage** |

Identity/security and reporting evidence is included in cross-cutting US-74 and US-75. Spring Security enforcement, actor attribution, and append-only operational histories remain green in the full suite.

## 7. US-77 Deep Gap Analysis

All frozen US-77 MVP capabilities are complete. The exact catalogue contains eight production events with stable producer identities; templates and recipient resolution are validated and snapshotted; quiet hours, suppression, durable retry, terminal failure, and one-level escalation follow the frozen contract; SMTP is the real production EMAIL transport; and the operator UI exposes every applicable rule and diagnostic field.

| Capability group | Status | Current evidence |
|---|---|---|
| Rule/catalogue/template/recipient | COMPLETE | `NotificationEventCatalogue`, `NotificationTemplateRenderer`, `NotificationRecipientResolver`, rule service/controller, V26, UI and NOT-001..005/011 |
| IN_APP/read state | COMPLETE | notification service/persistence/center and NOT-006..010 |
| Quiet hours/suppression/audit | COMPLETE | policy/evaluator/execution services, V27, diagnostics and NOT-013/014 |
| EMAIL/retry/escalation | COMPLETE | SMTP sender, durable worker/attempts, V28, diagnostics and NOT-012/015 |
| Production producers | COMPLETE | Trip delay/incident; Fleet maintenance/document/exception/medical/drug-test/licence producers and scanner tests |
| RBAC/security | COMPLETE | `NOTIFICATION_RULE_VIEW`, `NOTIFICATION_RULE_MANAGE`, `NOTIFICATION_VIEW`; backend 401/403 integration coverage |
| Frontend operability | COMPLETE | rule page/modal, delivery diagnostics, notification center, 22 focused component tests |
| Browser acceptance | COMPLETE | 15 logical cases, 45/45 notification executions, 156/156 complete regression |

SMS, PUSH, WEBHOOK, template editing, HTML templates, manual retry, campaigns, preferences, analytics, unread-age escalation, multi-level escalation, and provider UI are Phase 2 and do not reduce US-77 MVP completion.

## 8. US-71 Deep Gap Analysis

The repository now contains the frozen native IndexedDB store/queue, authenticated backend batch/idempotency foundation, authenticated reconnect/retry coordinator, both accepted queue-first owning-business verticals, owner-scoped global/per-record status and conflict actions, and complete offline/reconnect browser acceptance. A service worker/background synchronization remains outside the frozen MVP scope.

| Vertical slice element | Status | Evidence |
|---|---|---|
| Frontend durable operational store | IMPLEMENTED FOUNDATION | native IndexedDB v1, owner isolation, stable client ID, capacity/retention primitives |
| Frontend operation queue | IMPLEMENTED FOUNDATION | frozen typed envelope, guarded transitions, claims/recovery, retry metadata |
| Frontend sync coordinator | IMPLEMENTED FOUNDATION | startup/login/reconnect/manual triggers, 50-item batches, one-run/tab guard, cross-tab claims, exact retry timer, 401 pause/same-user resume, owner isolation, recovery/purge |
| Backend sync endpoint | IMPLEMENTED FOUNDATION | authenticated `POST /offline-sync/operations`, exact DTOs, 1..50 bounds |
| Backend idempotent processing | IMPLEMENTED FOUNDATION | V29 durable inbox, canonical SHA-256, terminal replay, actor/payload mismatch, concurrent claim |
| Backend conflict handling | IMPLEMENTED | stable per-item mismatch/business conflict results with safe owning-record open/refresh/discard UX |
| Backend partial batch result handling | IMPLEMENTED FOUNDATION | ordered independent item transactions and all five result states |
| Manual Vehicle-reading vertical | IMPLEMENTED | typed v1 handler, Fleet public boundary, current RBAC/actor attribution, existing Fleet validation/locking/idempotency/events, queue-first UI, owner-scoped local states, and post-apply query reconciliation |
| Trip operational-event vertical | IMPLEMENTED | typed v1 checkpoint/delay/incident handlers, Trip public boundary, current any-of RBAC, existing lifecycle/history/notification behavior, queue-first UI, owner-scoped local timeline, and post-apply reconciliation |
| Pending/syncing/failed/conflict UI | IMPLEMENTED | header indicator/drawer and Vehicle/Trip surfaces share centralized state presentation and action policy; counts and operations are owner-scoped and reactive |
| Offline/reconnect E2E | IMPLEMENTED | E2E-OFF-001..015 pass 15/15 in each of Chromium, Firefox, and WebKit; 45/45 offline and 201/201 full suite |

US-71 is **COMPLETE**. All 12 frozen criteria pass, every slice from 011A through 011I is complete, and all backend, architecture, startup, migration, frontend, focused offline, and full browser gates are green. Deferred PWA/service-worker/background-sync and broader offline CRUD/orchestration remain outside the frozen MVP scope.

### MVP-GAP-011A Contract and Audit Checkpoint

MVP-GAP-011A is complete as documentation-only analysis. `US71-MVP-OFFLINE-SYNC-CONTRACT.md` freezes manual Vehicle odometer/engine-hour readings plus Trip checkpoints/delays/incidents as the only offline-required operations. `MVP-GAP-011A-US71-AUDIT.md` records the source conflict, repository evidence, frontend/backend gaps, reusable infrastructure, V29 decision, system-vs-required matrix, security/ownership model, risks, and slices 011B–011I.

The audit found no meaningful offline behavior and therefore does not change story counts or completion percentage. Native IndexedDB is selected without adding a dependency; a bounded `POST /offline-sync/operations` contract, durable V29 inbox, per-item transactions/results, current-RBAC enforcement, and three-browser E2E acceptance are designed but not implemented. V1–V28 and the current green engineering baseline remain untouched.

### MVP-GAP-011B Implementation Checkpoint

MVP-GAP-011B is complete. `frontend/src/features/offlineSync` now owns the frozen four-operation version-one envelope, native IndexedDB schema, stable client instance metadata, owner-scoped queue API, atomic claim leases, crash recovery, guarded state transitions, persisted retry metadata, seven-day synced retention primitive, and the 1,000 non-synced capacity guard. Eighteen deterministic tests pass, taking the frontend suite from 106 to 124 tests. Frontend lint/build and the 647-test backend regression pass. `fake-indexeddb` is development-only because jsdom has no IndexedDB implementation; production remains native. No backend, Flyway, UI, auth, API-client, connectivity, service-worker, or workflow behavior changed.

Story counts stay unchanged because project governance requires the first accepted offline-to-server vertical before US-71 becomes PARTIAL. The coordinator is now complete; that vertical still requires the 011E Vehicle workflow.

### MVP-GAP-011C Implementation Checkpoint

MVP-GAP-011C is complete. `com.transportlogistics.app.offlinesync` now provides the authenticated 1..50 batch endpoint, exact frozen request/result contract, current per-item owning authority evaluation, handler registry, deterministic canonical SHA-256, independent item transactions, atomic test mutation/inbox proof, stable terminal replay, actor/payload mismatch, concurrent duplicate protection, and transient rollback. V29 adds the durable inbox with actor FK, terminal-status/version checks, and both frozen indexes; V1–V28 are unchanged. No real Fleet/Trip handlers, coordinator, offline UI, service worker, or Playwright change was added.

The full gates pass with 664 backend tests (642 passed, 22 conditional skips), 16 architecture tests, Spring context, 45 JPA repositories, H2 V1–V29, Maven verify/JAR packaging, frontend lint, 124/124 unit tests, and production build. PostgreSQL invariant coverage is present and conditionally skipped because Docker was unavailable. Detailed evidence is in `MVP-GAP-011C-IMPLEMENTATION.md`.

### MVP-GAP-011D Implementation Checkpoint

MVP-GAP-011D is complete. `frontend/src/features/offlineSync` now includes an exact authenticated batch client, startup/post-login/reconnect/manual coordinator triggers, bounded 50-item processing, one in-flight run per tab, 30-second cross-tab IndexedDB claim leases, exact 5/15/30/60-second retry with a ten-attempt ceiling, one earliest-due timer, final-401 pause without attempt increment, same-user resume, owner isolation, startup recovery and seven-day purge, sanitized persisted failures, and a feature-owned post-apply invalidation extension point. `OfflineSyncProvider` integrates this lifecycle below `AuthProvider`; no Vehicle/Trip workflow or offline UI is connected.

Eighteen new deterministic tests cover client envelopes, result mapping, retry/auth/reconnect, same-tab and real IndexedDB cross-tab duplicate suppression, missing/unknown results, HTTP policy, batching, timers, recovery, purge, logout, and owner isolation. The frontend suite is now 142/142. No backend production, Flyway, operational page, service-worker, or Playwright behavior changed. US-71 therefore remains NOT IMPLEMENTED at 38 complete, 0 partial, 1 not implemented (97.44%); detailed evidence is in `MVP-GAP-011D-IMPLEMENTATION.md`.

### MVP-GAP-011E Implementation Checkpoint

MVP-GAP-011E is complete. Offline Sync now owns a typed version-one `VEHICLE_READING_RECORD` handler that requires `VEHICLE_READING_CREATE` and calls a narrow public Fleet boundary. Fleet applies the operation through the existing Vehicle-reading service, preserving validation, pessimistic locking, idempotency, snapshots, and events while forcing server-authenticated actor attribution and `MANUAL` source semantics. The handler joins the inbox transaction so owning mutation and terminal result remain atomic.

The Vehicle-reading UI now queues both online and offline submissions before synchronization, never falls back to a direct manual-reading POST, exposes owner-scoped pending/syncing/conflict/failed state, hides reconciled synced items without deleting them, and invalidates normal Fleet queries after apply. Correction and reset operations remain direct and online-only. Backend clean test/verify pass with 673 tests (651 passed, 22 skipped); frontend lint, 148 unit tests, build, and all 156 Playwright executions pass. No Flyway or IndexedDB schema change was required. US-71 is now PARTIAL at 38 complete, 1 partial, and 0 not implemented: verified completion is 97.44% and weighted functional coverage is 98.72%. Detailed evidence is in `MVP-GAP-011E-IMPLEMENTATION.md`.

### MVP-GAP-011F Implementation Checkpoint

MVP-GAP-011F is complete. Offline Sync now owns strict version-one handlers for `TRIP_CHECKPOINT_RECORD`, `TRIP_DELAY_RECORD`, and `TRIP_INCIDENT_RECORD`, accepts any current `TRIP_DISPATCH`, `TRIP_LOG_MANAGE`, or `TRIP_UPDATE` authority, and calls the narrow public `TripOperationalEventRecorder` boundary. The existing Trip service remains authoritative for lifecycle validation, operational-event/history persistence, actor attribution, and normal `TRIP_DELAY_RECORDED` / `TRIP_INCIDENT_RECORDED` notification publication. Offline Sync imports neither Trip persistence internals nor Notification.

The Trip event UI now queues online and offline captures before synchronization, has no direct-POST fallback, shows owner-scoped pending/syncing/conflict/failed timeline entries, hides reconciled synced copies, and invalidates Trip event/history/detail/list queries after apply. Backend clean test/verify pass with 680 tests (658 passed, 22 skipped); frontend lint, 152 unit tests, build, and all retained 156 Playwright executions pass. Flyway remains V29 and IndexedDB remains v1. US-71 remains PARTIAL at 38 complete, 1 partial, and 0 not implemented; detailed evidence is in `MVP-GAP-011F-IMPLEMENTATION.md`.

### MVP-GAP-011G Implementation Checkpoint

MVP-GAP-011G is complete. The application header now exposes coordinator-derived connectivity, pending/syncing/conflict/failed counts, an owner-scoped status drawer, and coordinator-only manual synchronization. A centralized action policy prevents blind retries for forbidden, conflict, idempotency-mismatch, and invalid-payload outcomes; retry preserves operation identity, payload, owner, creation history, and cumulative attempts. Terminal operations support safe owning-record open/refresh and Ant Design-confirmed local discard, with Vehicle and Trip surfaces reusing the same presentation/actions.

Frontend lint, production build, and all 169 unit/component tests pass. Backend clean verify remains green with 680 tests (658 passed, 22 Docker-conditional skips), 16 architecture checks, Spring context, 45 JPA repositories, and Flyway V1-V29. The retained Playwright baseline remains 156/156 across Chromium, Firefox, and WebKit. No backend production code, Flyway migration, IndexedDB schema, or 011H offline E2E scenario changed. US-71 remains PARTIAL; the next slice is MVP-GAP-011H. Detailed evidence is in `MVP-GAP-011G-IMPLEMENTATION.md`.

### MVP-GAP-011H Implementation Checkpoint

MVP-GAP-011H is complete. Exactly 15 logical offline cases now use real operational UI capture and real IndexedDB, with core disconnected flows using `context.setOffline(true)`. Reload durability selectively interrupts only the synchronization endpoint so Vite assets remain available. Operation IDs are read from IndexedDB and preserved through retry, replay, reload, discard, and server-inbox verification. E2E-profile-only, authority-protected operation-ID controls make applied, rejected, conflict, retryable, and blocked outcomes deterministic; a profile safety test proves they are absent outside `e2e`.

The focused offline matrix passes 45/45: Chromium 15/15, Firefox 15/15, and WebKit 15/15. The full retained Playwright suite passes 201/201 with zero failures or skips. Frontend lint, production build, and all 170 unit/component tests pass. Backend clean test and verify pass with 681 tests (659 passed, 22 Docker-conditional skips), 16 architecture checks, Spring context, 45 JPA repositories, Flyway V1-V29, and a packaged JAR. No service worker, V30 migration, retry-policy change, or IndexedDB version change was introduced. US-71 remains PARTIAL by governance; the next slice is MVP-GAP-011I. Detailed evidence is in `MVP-GAP-011H-IMPLEMENTATION.md`.

### MVP-GAP-011I Closure Checkpoint

MVP-GAP-011I is complete. All 12 frozen US-71 criteria are PASS. Backend clean test and verify each pass with 681 tests (659 passed, 22 Docker/Testcontainers-conditional skips), all 16 architecture checks pass, Spring context/security and 45 JPA repositories start, and clean H2 applies V1-V29. Frontend lint, all 170 unit/component tests, and the production build pass. The focused offline suite passes 45/45 and the full suite passes 201/201 with zero failures or skips across Chromium, Firefox, and WebKit. E2E controls remain authenticated, authority-protected, and `e2e`-profile-only; owner isolation and absence of credentials in IndexedDB are verified. US-71 is COMPLETE. Detailed evidence is in `MVP-GAP-011I-US71-CLOSURE.md`.

## 9. Previous vs Current Comparison

| Metric | Previous verified | Current verified |
|---|---:|---:|
| Total | 39 | 39 |
| Complete | 37 | 39 |
| Partial | 1 | 0 |
| Not implemented | 1 | 0 |
| Regressed | 0 | 0 |
| Blocked | 0 | 0 |
| Verified completion | 94.87% | 100.00% |
| Functional coverage | 96.15% | 100.00% |

- Newly completed: US-77 and US-71.
- Still partial: none.
- Still not implemented: none.
- Regressed: none.
- Newly resolved gap detail: all frozen US-77 capabilities and acceptance criteria are complete, including real SMTP, three-attempt retry, one-level escalation, eight production producers, complete UI, and three-browser E2E.
- Resolved engineering issues confirmed current: the 118 ApplicationContext errors, missing `FuelActorPort`, Trip -> Notification boundary violation, 67 lint errors, and 108 Playwright connection failures remain resolved.

## 10. Remaining MVP Gap Queue

Dependency order is:

1. **MVP-RELEASE-CANDIDATE-001 — MVP Release Candidate Validation and Production Readiness Audit.**

Required US-77 slices:

1. **MVP-GAP-008A — Acceptance/domain-policy freeze: COMPLETE.** The controlled event catalogue, system-template contract, quiet/suppression semantics, terminal-failure escalation, durable EMAIL retry, provider-neutral email contract, and Phase 2 channel boundary are frozen.
2. **MVP-GAP-008B — Templates and rule configuration: COMPLETE.** Controlled events, system templates, safe rendering, recipient validation, template-backed rules, V26, APIs, and security are implemented.
3. **MVP-GAP-008C — Suppression and quiet hours: COMPLETE.** Deterministic timezone/DST evaluation, durable quiet scheduling, concurrent non-sliding suppression, rule-execution audit/query, and V27 are implemented.
4. **MVP-GAP-008D — Escalation and durable retry: COMPLETE.** Durable attempts, exact bounded backoff, restart/concurrency safety, terminal failure, one-level escalation, V28, and sanitized diagnostics are implemented.
5. **MVP-GAP-008E — Required production event producers: COMPLETE.** Eight frozen events, stable source/milestone identity, public Trip/Fleet publishing boundaries, hourly/daily scanners, and failure isolation are implemented.
6. **MVP-GAP-008F — Production EMAIL adapter: COMPLETE.** Real SMTP delivery, explicit mode selection, fail-fast production configuration, typed error mapping, message evidence, local integration tests, and 008D retry/escalation integration are implemented.
7. **MVP-GAP-008G — Frontend completion: COMPLETE.** Catalogue/template-driven rule configuration, conditional channel/recipient/policy UX, field-error mapping, rule summaries, bounded delivery health, and sanitized attempt diagnostics are implemented and visually verified.
8. **MVP-GAP-008H — Notification Playwright coverage: COMPLETE.** Rule configuration, event trigger, center/unread/read, RBAC, EMAIL failure, quiet/suppression, and retry pass across all supported browsers.
9. **MVP-GAP-008I — Regression and story closure: COMPLETE.** All 26 criteria, backend/architecture/startup/Flyway/frontend gates, notification 45/45, and complete Playwright 156/156 pass.

## 11. Release Readiness

**Status: FUNCTIONAL MVP COMPLETE — READY FOR RELEASE-CANDIDATE VALIDATION.**

All 39 selected MVP stories are complete and no unresolved P0/P1 defect remains. This is not a production-readiness declaration; release-candidate validation remains responsible for deployment, configuration, security hardening, performance, observability, backup/recovery, runbooks, load testing, and environment validation.

## 12. Recommended Next Task

- **Task ID:** MVP-RELEASE-CANDIDATE-001
- **Title:** MVP Release Candidate Validation and Production Readiness Audit
- **Reason:** Functional MVP scope is complete at 39/39; production-readiness concerns must be assessed separately.

MVP-GAP-008A through MVP-GAP-008I and MVP-GAP-011A through MVP-GAP-011I are complete. The next recommended task is **MVP-RELEASE-CANDIDATE-001**. No release-candidate work was performed as part of 011I.

## 13. Phase 2 Deferred Scope

- Visual/arbitrary workflow designers and enterprise approval composition beyond US-80's deterministic MVP state machines.
- Gantt scheduling, advanced shifts/calendars, AI/multi-trip optimization beyond US-81.
- Notification SMS, PUSH, and WEBHOOK channels unless product authority promotes them into MVP.
- Predictive traffic routing, advanced optimization, document OCR/versioning, and advanced analytics not required by the selected stories.

## 14. Phase 3 Deferred Scope

- Broader disconnected/mobile-first operation modes beyond the eventual minimum US-71 contract.
- Enterprise-grade distributed synchronization topologies and cross-device collaborative conflict resolution beyond MVP.
- Artillery/load testing is **POST-FUNCTIONAL-MVP HARDENING**. Future targets may include authentication, vehicle availability, trip creation/allocation, fuel operations, reports, and notification retrieval. No performance dependency was added.

## 15. Historical / Superseded Findings

The following are historical and must not be reported as current defects unless a later executable gate proves regression:

- 118 backend ApplicationContext errors.
- Missing `FuelActorPort` bean.
- Trip importing notification internals / Spring Modulith boundary violation.
- 67 frontend lint errors.
- 108 Playwright `ERR_CONNECTION_REFUSED` failures.

Current 008B evidence supersedes those findings: 547 backend tests and Maven verify pass, 15/15 architecture tests pass, lint and 94/94 frontend tests pass, and the production build succeeds. The three-browser Playwright run passed 110/111; Firefox alone reproducibly failed the pre-existing operational-incident select interaction before any request was submitted, while Chromium and WebKit passed that flow. No 008B frontend or E2E file was changed.

### Change control record

Before 008B, branch `feature/mvp-fe-quality-001` at `db56d0e1304ef54626f768720e75940e213a87d3` already contained seven modified harness/audit files, this untracked comparison document and notification contract, and six untracked JVM diagnostic logs. Those pre-existing changes were preserved. 008B changed only the notification/Identity boundary, notification tests, V26, and the required MVP documentation. No frontend source, E2E source, dependency, V1-V25, commit, or push was added by 008B.

Before 008C, the working tree already contained modifications to two MVP audit documents, three Playwright QA documents, `frontend/e2e/tests/smoke/app.smoke.spec.ts`, and `frontend/playwright.config.ts`, plus six JVM diagnostic logs. Those changes were preserved. 008C changed only notification policy/audit code, the single notification audit security mapping, notification tests, V27, and the required MVP documentation. 008D added only the notification delivery/retry/escalation/diagnostics slice, V28, its tests, and required documentation/security mapping. It did not modify frontend source/E2E/config files, dependencies, V1-V27, or create a commit/push.
