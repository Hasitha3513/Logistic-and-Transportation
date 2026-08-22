# MVP Current Status Compare 002

## 1. Executive Summary

The current repository has 38 of 39 selected MVP stories complete. US-77 is complete after MVP-GAP-008I; US-71 remains not implemented. Verified completion and weighted functional coverage are both 97.44%. No previously complete story regressed.

**MVP-GAP-008I checkpoint:** COMPLETE. All 26 frozen US-77 criteria pass, E2E-NOT-001 through E2E-NOT-015 cover the complete notification vertical, and the complete regression is green in Chromium, Firefox, and WebKit.

The 008I Maven clean test and verify gates each report 647 tests (626 passed, 21 skipped); all 15 architecture tests pass, the Spring context starts, JPA discovers 44 repositories, Flyway applies V1-V28, frontend lint/build plus all 106 unit tests pass, the notification Playwright gate passes 45/45, and the complete regression passes 156/156.

The release is **NOT READY** only because US-71 remains absent from the approved 39-story inventory. The exact next task is **MVP-GAP-011-US71**.

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
| Backend clean test | `mvn -B clean test` | PASS — 647 run, 626 passed, 0 failures, 0 errors, 21 skipped |
| Backend verify | `mvn -B verify` | PASS — 647 run, 626 passed, 0 failures, 0 errors, 21 skipped; JAR packaged |
| Spring startup | `ContextSmokeTest` and the Playwright-managed Spring server | PASS |
| Flyway | H2 startup logs during clean verification | PASS — V1 through V28 applied |
| JPA | repository/context suites inside Maven verification | PASS |
| Frontend lint | `npm run lint` | PASS — zero warnings permitted |
| Frontend unit | `npm test` | PASS — 23 files, 106 tests |
| Frontend build | `npm run build` | PASS — TypeScript and Vite build; non-blocking chunk-size warning |
| Playwright notification gate | `npx playwright test e2e/tests/notifications` | PASS — 45/45; 15/15 in Chromium, Firefox, and WebKit |
| Playwright full regression | `npm run test:e2e` | PASS — 156/156 across Chromium, Firefox, and WebKit (111 retained + 45 notification) |

The prior retained non-notification baseline remains 111/111. MVP-GAP-008H adds 45/45 notification executions; the standard combined command passes 156/156 with zero failures or skips.

Non-blocking frontend test output includes Ant Design deprecation/context warnings and a Vite large-chunk warning. These do not fail the configured quality gates.

## 4. Current Architecture Health

| Suite | Tests | Result |
|---|---:|---|
| `ApplicationModulesTest` | 2 | PASS |
| `HexagonalLayerArchitectureTest` | 7 | PASS |
| `ModuleBoundaryArchitectureTest` | 3 | PASS |
| `LombokUsageArchitectureTest` | 3 | PASS |
| **Total** | **15** | **PASS** |

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
| US-71 | Support Offline Data Synchronization | NOT IMPLEMENTED | NOT IMPLEMENTED | no sync API/coordinator; isolated reading idempotency only | no durable store/queue/status UI | no offline/reconnect tests | Entire slice absent; mandatory-scope release blocker |
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
| Cross-cutting (US-71, 74, 75, 77, 79, 80, 81, 83) | 7 | 0 | 1 | Notification complete; offline absent |
| **Total** | **38** | **0** | **1** | **97.44% verified completion** |

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
| Complete | 37 | 38 |
| Partial | 1 | 0 |
| Not implemented | 1 | 1 |
| Regressed | 0 | 0 |
| Blocked | 0 | 0 |
| Verified completion | 94.87% | 97.44% |
| Functional coverage | 96.15% | 97.44% |

- Newly completed: US-77.
- Still partial: none.
- Still not implemented: US-71.
- Regressed: none.
- Newly resolved gap detail: all frozen US-77 capabilities and acceptance criteria are complete, including real SMTP, three-attempt retry, one-level escalation, eight production producers, complete UI, and three-browser E2E.
- Resolved engineering issues confirmed current: the 118 ApplicationContext errors, missing `FuelActorPort`, Trip -> Notification boundary violation, 67 lint errors, and 108 Playwright connection failures remain resolved.

## 10. Remaining MVP Gap Queue

Dependency order is:

1. **MVP-GAP-011-US71 — Implement offline data synchronization**, unless the product authority explicitly defers US-71 out of the 39-story MVP.
2. **MVP-RELEASE-CANDIDATE-001 — final candidate validation** after all mandatory stories close.

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

**Status: NOT READY.**

Engineering health is release-grade and US-77 is closed. Functional scope is not closed because US-71 is still included in the approved 39-story inventory and remains not implemented; absent an explicit product/governance deferral, it is the remaining release blocker. No unresolved P0/P1 defect remains.

## 12. Recommended Next Task

- **Task ID:** MVP-GAP-011-US71
- **Story:** US-71 — Support Offline Data Synchronization
- **Title:** Implement Offline Data Synchronization
- **Reason:** US-77 is complete; US-71 is the only remaining mandatory story in the current 39-story MVP.

MVP-GAP-008A through MVP-GAP-008I are complete. The next recommended task is **MVP-GAP-011-US71 — Implement Offline Data Synchronization**. US-71 was not implemented by 008I.

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
