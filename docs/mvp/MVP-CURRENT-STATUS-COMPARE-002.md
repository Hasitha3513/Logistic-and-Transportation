# MVP Current Status Compare 002

## 1. Executive Summary

The current repository preserves the latest verified functional baseline: 37 of 39 selected MVP stories are complete, US-77 is partial, and US-71 is not implemented. Verified completion is 94.87%; weighted functional coverage is 96.15%. No previously complete story regressed.

**MVP-GAP-008B checkpoint:** COMPLETE. The controlled catalogue, versioned system templates, renderer, recipient validation/directory boundary, rule template selection, V26, and read-only catalogue/template APIs are implemented. US-77 remains PARTIAL; the next implementation slice is MVP-GAP-008C.

All current engineering gates pass: Maven clean test and verify each report 524 tests (503 passed, 21 skipped), all 15 architecture tests pass, the Spring context starts, Flyway applies V1-V25, frontend lint/build and all 94 unit tests pass, and the self-starting Playwright harness passes 111 tests across Chromium, Firefox, and WebKit.

The release is **NOT READY** because US-77 still lacks MVP notification-policy and delivery behavior and US-71 remains absent from the approved 39-story inventory. The exact next task is **MVP-GAP-008-US77**. This document is an audit only; it makes no production, frontend, test, migration, or dependency changes.

## 2. Previous Baseline

| Metric | Previous verified value |
|---|---:|
| Total stories | 39 |
| Complete | 37 |
| Partial | 1 |
| Not implemented | 1 |
| Verified completion | 94.87% |
| Functional coverage | 96.15% |

Remaining stories were US-77 (PARTIAL) and US-71 (NOT IMPLEMENTED). The accepted MVP interpretation treats deterministic Trip/Fuel state machines as sufficient for US-80 and requested windows, availability, blackout checks, and overlap prevention as sufficient for US-81.

## 3. Current Build/Test Health

| Gate | Command/evidence | Current result |
|---|---|---|
| Backend clean test | `mvn -B clean test` | PASS — 524 run, 503 passed, 0 failures, 0 errors, 21 skipped |
| Backend verify | `mvn -B verify` | PASS — 524 run, 503 passed, 0 failures, 0 errors, 21 skipped; JAR packaged |
| Spring startup | `ContextSmokeTest` and the Playwright-managed Spring server | PASS |
| Flyway | H2 startup logs during clean verification | PASS — V1 through V25 applied |
| JPA | repository/context suites inside Maven verification | PASS |
| Frontend lint | `npm run lint` | PASS — zero warnings permitted |
| Frontend unit | `npm test` | PASS — 22 files, 94 tests |
| Frontend build | `npm run build` | PASS — TypeScript and Vite build; non-blocking chunk-size warning |
| Playwright | `npm run test:e2e` | PASS — 111 passed, 0 failed, 0 skipped, 4.2 minutes, three browsers |

The first sandboxed Playwright attempt could not resolve Maven Central because network access was denied. Re-running the unchanged standard command with permitted dependency/local-server access passed 111/111; this is an execution-environment restriction, not a repository defect.

Non-blocking frontend test output includes Ant Design deprecation/context warnings and a Vite large-chunk warning. These do not fail the configured quality gates.

## 4. Current Architecture Health

| Suite | Tests | Result |
|---|---:|---|
| `ApplicationModulesTest` | 2 | PASS |
| `HexagonalLayerArchitectureTest` | 7 | PASS |
| `ModuleBoundaryArchitectureTest` | 3 | PASS |
| `LombokUsageArchitectureTest` | 3 | PASS |
| **Total** | **15** | **PASS** |

The public `notification.OperationalNotificationEvent` is the Trip-to-Notification boundary; Trip does not import notification internals. Fuel actor attribution uses an application boundary and the Spring context resolves it. The historical Trip -> Notification and Fuel -> Identity/Security regressions are not present.

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
| US-71 | Support Offline Data Synchronization | NOT IMPLEMENTED | NOT IMPLEMENTED | no sync API/coordinator; isolated reading idempotency only | no durable store/queue/status UI | no offline/reconnect tests | Entire slice absent; mandatory-scope release blocker |
| US-74 | Manage Security | COMPLETE | COMPLETE | JWT, rotating refresh, BCrypt, permissions, `SecurityConfig` | auth context and permission navigation | identity/security and RBAC E2E | None |
| US-75 | Maintain Audit and Reports | COMPLETE | COMPLETE | histories and report services/controllers | dashboard and detail histories | reporting/history/UI/E2E | advanced analytics deferred |
| US-77 | Manage Notification Rules | PARTIAL | PARTIAL | rule engine, controlled catalogue, versioned system templates/rendering, recipient validation/directory boundary, persistence, APIs, RBAC, V25-V26; one producer | rules page/modal, notification center/hooks | 55 focused backend notification tests in the latest focused run; 10 frontend notification unit tests; no notification E2E | Quiet/suppression, durable retry/escalation, real email, remaining producers, full UI fields, diagnostics and notification E2E missing; blocker |
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
| Cross-cutting (US-71, 74, 75, 77, 79, 80, 81, 83) | 6 | 1 | 1 | Notification partial; offline absent |
| **Total** | **37** | **1** | **1** | **94.87% verified completion** |

Identity/security and reporting evidence is included in cross-cutting US-74 and US-75. Spring Security enforcement, actor attribution, and append-only operational histories remain green in the full suite.

## 7. US-77 Deep Gap Analysis

### 7.1 Acceptance and implementation

The latest accepted baseline explicitly carries templates, quiet/suppression, escalation, durable retry, operational coverage, and notification E2E as US-77 gaps. The exact historical gap title narrows production channels to IN_APP and EMAIL. Therefore SMS, PUSH, and WEBHOOK are deferred and are not artificial MVP blockers.

| Item | MVP classification | Current implementation | Evidence / gap |
|---|---|---|---|
| A. Rule CRUD | REQUIRED FOR MVP | COMPLETE | `NotificationRuleUseCase`, `NotificationRuleService`, `NotificationRuleController`, persistence |
| B. Enable/disable | REQUIRED FOR MVP | COMPLETE | domain operations, service/controller PATCH endpoints, UI toggles |
| C. Event matching | REQUIRED FOR MVP | COMPLETE | `NotificationRuleEngine` exact event-type matching and severity threshold |
| D. Recipient configuration | REQUIRED FOR MVP | COMPLETE | USER, ROLE, EMAIL_ADDRESS recipient types; request/domain/UI fields |
| E. Templates | REQUIRED FOR MVP | MISSING | no template model, variables, rendering, or versioning; event title/message are copied |
| F. IN_APP delivery | REQUIRED FOR MVP | COMPLETE | `InAppNotificationDeliveryAdapter`; persisted notification center/history |
| G. EMAIL delivery | REQUIRED FOR MVP | PARTIAL | enum/config/adapter exist, but adapter logs instead of sending; UI has no channel control |
| H. SMS delivery | DEFERRED | MISSING | no enum/config/adapter; not an MVP blocker |
| I. PUSH delivery | DEFERRED | MISSING | no enum/config/adapter; not an MVP blocker |
| J. WEBHOOK delivery | DEFERRED | MISSING | no enum/config/adapter; not an MVP blocker |
| K. Quiet hours | REQUIRED FOR MVP | MISSING | no policy fields or evaluator |
| L. Suppression | REQUIRED FOR MVP | MISSING | idempotency exists, but no suppression window/policy |
| M. Escalation | REQUIRED FOR MVP | MISSING | no escalation rule, schedule, or recipient progression |
| N. Delivery retry | REQUIRED FOR MVP | MISSING | no durable attempt record, retry/backoff, or recovery worker |
| O. Failure status | REQUIRED FOR MVP | COMPLETE | FAILED state and failure reason in domain/persistence/engine |
| P. Notification history | REQUIRED FOR MVP | COMPLETE | notification persistence/list endpoint/UI drawer |
| Q. Read/unread | REQUIRED FOR MVP | COMPLETE | unread count, read/read-all APIs and UI |
| R. Operational integration | REQUIRED FOR MVP | PARTIAL | only production producer is Trip delay; other advertised event types are not connected |
| S. RBAC | REQUIRED FOR MVP | COMPLETE | V25 permissions and `SecurityConfig` endpoint mappings |
| T. Operator-facing UI | REQUIRED FOR MVP | PARTIAL | CRUD/center work; channel and missing policy fields cannot be configured |
| U. Notification E2E | REQUIRED FOR MVP | MISSING | no notification-specific file or scenario under `frontend/e2e` |

### 7.2 Production event producers

| Producer | Module | Event | Trigger | Rule mapping | Test coverage |
|---|---|---|---|---|---|
| `TripOperationalEventService.recordDelay` | Trip | `TRIP_DELAY_RECORDED` | a delay operational event is recorded | exact event type through public `OperationalNotificationEvent` | `TripOperationalEventServiceTest` |

This is the only production publisher found. Test-only publishers are excluded. The rule UI advertises incident, maintenance, driver exception/medical/drug/licence, and fuel threshold events, but no matching production publisher exists for them. MVP-required producer scope must be frozen before implementation; at minimum the UI must not promise unmapped events.

### 7.3 Delivery channels

| Channel | Domain | Config | Production adapter | Failure/retry/status | Tests | MVP |
|---|---|---|---|---|---|---|
| IN_APP | yes | yes | yes; persistence is authoritative | failure state; no retry; SENT/FAILED/READ | rule-engine/service/UI | required, complete |
| EMAIL | yes | yes | **no real transport**; adapter logs in mock and enabled modes | exceptions can fail, no retry; otherwise misleading SENT | adapter path indirectly covered | required, partial |
| SMS | no | no | no | none | none | deferred |
| PUSH | no | no | no | none | none | deferred |
| WEBHOOK | no | no | no | none | none | deferred |

`EmailNotificationDeliveryAdapter` contains a production-delivery comment but no SMTP/provider call. An enum or bean registration is not delivery evidence.

### 7.4 Policy depth

| Capability | Result | Exact evidence |
|---|---|---|
| Template/render variables/versioning | IMPLEMENTED | `NotificationTemplate`, `NotificationTemplateRenderer`, `NotificationEventCatalogue`, `NotificationRuleEngine`, and V26 active-version lookup/snapshot columns |
| Recipient rules | IMPLEMENTED (basic) | `RecipientType`, `recipientValue`, engine resolution |
| Quiet hours | NOT IMPLEMENTED | no fields/evaluator in `NotificationRule`, V25, or engine |
| Suppression | NOT IMPLEMENTED | no suppression policy/window; unique event/rule/recipient is idempotency only |
| Escalation | NOT IMPLEMENTED | no escalation model/service/scheduler |
| Retry/backoff/attempts | NOT IMPLEMENTED | no attempt entity, backoff policy, worker, or scheduled retry |
| Failed delivery | IMPLEMENTED | `NotificationStatus.FAILED`, `failureReason`, engine exception handling |
| Dead-letter/recovery | NOT IMPLEMENTED | no recovery queue or operator retry action |

### 7.5 Frontend operability

`NotificationRulesPage`, `NotificationRuleModal`, `NotificationCenter`, `useNotificationRules`, and `useNotifications` provide list/create/edit/delete, enable/disable, unread badge, list, mark-read, and mark-all-read behavior with permission-aware navigation. `NotificationRuleModal` carries `channel` in types/default payload but exposes no channel form control, so an operator cannot select or change EMAIL. It also has no template, quiet-hour, suppression, escalation, or retry fields because the backend capability is absent.

### 7.6 Test coverage

| Criterion | Backend unit | Backend integration | Frontend unit | Playwright | Status |
|---|---|---|---|---|---|
| Rule CRUD/enable | yes | controller/security/persistence | yes | no | PARTIAL E2E confidence |
| Matching/threshold/recipient | yes | listener/engine paths | indirect | no | COMPLETE below E2E |
| Persistence/history/read state | yes | persistence/controller/security | yes | no | COMPLETE below E2E |
| IN_APP delivery | yes | engine/persistence | center tests | no | COMPLETE below E2E |
| EMAIL delivery | path only | no real transport integration | no channel selection | no | MISSING production proof |
| Templates/policies/retry/escalation | no | no | no | no | MISSING |
| Operational producer | Trip service unit | no notification journey | no | no | PARTIAL |
| Notification operator journey | n/a | n/a | 10 tests | none | MISSING |

Backend notification-specific suites contain 32 tests; frontend notification-specific suites contain 10 tests. The green 111-test Playwright suite has no notification-specific spec and is not counted as US-77 E2E coverage.

## 8. US-71 Deep Gap Analysis

Repository searches found no IndexedDB/Dexie/localForage operational store, service worker/background synchronization, durable queue, reconnect coordinator, `navigator.onLine` handling, sync endpoint, conflict resolver, batch partial-result contract, synchronization status UI, or offline/reconnect E2E scenario. TanStack Query cache and ordinary auth/browser storage are not operational offline synchronization.

| Vertical slice element | Status | Evidence |
|---|---|---|
| Frontend durable operational store | MISSING | no IndexedDB/Dexie/localForage implementation |
| Frontend operation queue | MISSING | no pending operational envelope/queue |
| Frontend sync coordinator | MISSING | no reconnect/background coordinator |
| Backend sync endpoint | MISSING | no batch synchronization API |
| Backend idempotent processing | PARTIAL | vehicle readings have a server-side `idempotency_key` (V14), but no general sync envelope/processor |
| Backend conflict handling | MISSING | no offline version/conflict contract |
| Backend partial batch result handling | MISSING | no batch endpoint/result model |
| Pending/syncing/failed/conflict UI | MISSING | no sync status surface |
| Offline/reconnect E2E | MISSING | no Playwright offline sync scenario |

US-71 remains **NOT IMPLEMENTED**. Isolated vehicle-reading idempotency is useful infrastructure but does not constitute an offline vertical slice.

## 9. Previous vs Current Comparison

| Metric | Previous verified | Current verified |
|---|---:|---:|
| Total | 39 | 39 |
| Complete | 37 | 37 |
| Partial | 1 | 1 |
| Not implemented | 1 | 1 |
| Regressed | 0 | 0 |
| Blocked | 0 | 0 |
| Verified completion | 94.87% | 94.87% |
| Functional coverage | 96.15% | 96.15% |

- Newly completed: none.
- Still partial: US-77.
- Still not implemented: US-71.
- Regressed: none.
- Newly discovered gap detail: EMAIL can be selected by backend contract but the current modal has no channel control; the enabled adapter still only logs and can mark a non-delivery SENT. This refines US-77 and does not create another story.
- Resolved engineering issues confirmed current: the 118 ApplicationContext errors, missing `FuelActorPort`, Trip -> Notification boundary violation, 67 lint errors, and 108 Playwright connection failures remain resolved.

## 10. Remaining MVP Gap Queue

Dependency order is:

1. **MVP-GAP-008-US77 — Complete Manage Notification Rules.**
2. **MVP-GAP-011-US71 — Implement offline data synchronization**, unless the product authority explicitly defers US-71 out of the 39-story MVP.
3. **MVP-RELEASE-CANDIDATE-001 — final candidate validation** after all mandatory stories close.

Required US-77 slices:

1. **MVP-GAP-008A — Acceptance/domain-policy freeze: COMPLETE.** The controlled event catalogue, system-template contract, quiet/suppression semantics, terminal-failure escalation, durable EMAIL retry, provider-neutral email contract, and Phase 2 channel boundary are frozen.
2. **MVP-GAP-008B — Templates and rule configuration: COMPLETE.** Controlled events, system templates, safe rendering, recipient validation, template-backed rules, V26, APIs, and security are implemented.
3. **MVP-GAP-008C — Suppression and quiet hours:** implement deterministic time-zone-aware evaluation and auditable suppression outcomes.
4. **MVP-GAP-008D — Escalation and durable retry:** persist delivery attempts, apply bounded backoff, expose failure/recovery state, and avoid false SENT statuses.
5. **MVP-GAP-008E — Required production event producers:** connect the frozen operational event catalogue through public module boundaries.
6. **MVP-GAP-008F — Production EMAIL adapter:** implement and integration-test real delivery behind configuration; preserve IN_APP behavior.
7. **MVP-GAP-008G — Frontend completion:** expose channel, template, and required policy fields and present delivery outcomes.
8. **MVP-GAP-008H — Notification Playwright coverage:** cover rule configuration, event trigger, center/unread/read, RBAC, and failure/retry where observable.
9. **MVP-GAP-008I — Regression and story closure:** run all backend, architecture, frontend, and three-browser gates and update traceability.

## 11. Release Readiness

**Status: NOT READY.**

Engineering health is release-grade at this checkpoint, but functional scope is not closed. US-77 is an agreed story and remains partial. US-71 is still included in the approved 39-story inventory and remains not implemented; absent an explicit product/governance deferral, it is also a release blocker. No P0/P1 regression in completed functionality was found.

## 12. Recommended Next Task

- **Task ID:** MVP-GAP-008C
- **Story:** US-77 — Manage Notification Rules
- **Title:** Quiet Hours and Suppression
- **Reason:** 008B supplies the controlled catalogue, compatible templates, renderer, validated recipients, and rule contract required before deterministic policy evaluation can be added.

MVP-GAP-008A and MVP-GAP-008B are complete. The next authorized implementation slice is **MVP-GAP-008C — Quiet hours and suppression**.

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
