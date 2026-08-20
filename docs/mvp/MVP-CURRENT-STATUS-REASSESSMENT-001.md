# MVP-CURRENT-STATUS-REASSESSMENT-001

## Engineering Health Update — MVP-FE-QUALITY-001 (2026-08-21)

This update supersedes the frontend lint result in the reassessment below. Feature completion remains unchanged at **37 COMPLETE, 1 PARTIAL, and 1 NOT IMPLEMENTED (94.87%)**.

| Gate | Before | After |
|---|---:|---:|
| Frontend lint | FAIL — 67 errors, 0 warnings | PASS — 0 errors, 0 warnings |
| `@typescript-eslint/no-explicit-any` | 48 errors | 0 errors |
| `@typescript-eslint/no-unused-vars` | 17 errors | 0 errors |
| `prefer-const` | 2 errors | 0 errors |
| Frontend unit/component tests | PASS — 94/94 | PASS — 94/94 |
| Frontend production build | PASS | PASS |
| Backend regression (`mvn -B test`) | PASS | PASS — 524 run, 503 passed, 21 skipped |
| Architecture/Modulith | PASS | PASS — 15/15 architecture tests |

The cleanup affected 25 existing frontend source, test, E2E, and legacy support files. It replaced unbounded `any` declarations with request/form/record types or `unknown` plus explicit narrowing, removed unused declarations, and corrected immutable bindings. No feature behavior, API contract, dependency, migration, backend source, ESLint rule, or MVP story classification changed.

Overall release readiness remains **NOT READY** solely because the Playwright release gate is not self-starting: its package command does not launch the frontend/backend services, so the previously recorded 108 connection-refused results did not reach functional assertions. The next task is **MVP-E2E-HARNESS-001**. US-71 and further US-77 work remain out of scope.

## Engineering Health Update — MVP-REGRESSION-FIX-001 (2026-08-21)

This update supersedes the backend and architecture results recorded in the original reassessment below while preserving that assessment as the historical before-state. Feature completion is unchanged at **37 COMPLETE, 1 PARTIAL, and 1 NOT IMPLEMENTED (94.87%)**.

| Gate | Before | After |
|---|---|---|
| `mvn -B clean test` | 520 run; 381 passed; 0 failures; 118 errors; 21 skipped; **FAILURE** | 524 run; 503 passed; 0 failures; 0 errors; 21 skipped; **SUCCESS** |
| `mvn -B verify` | Not run because tests failed | 524 run; 503 passed; 0 failures; 0 errors; 21 skipped; **SUCCESS** |
| Spring context | Missing production `FuelActorPort` bean | **PASS**; packaged application starts normally |
| Spring Modulith | Trip imported notification-internal `NotificationSeverity` | **PASS**; public event-owned severity maps to the internal notification severity inside notification |
| Architecture | Modulith failed; other 13 rules passed | `ApplicationModulesTest` 2/2, hexagonal 7/7, module boundary 3/3, Lombok 3/3; **15/15 PASS** |
| Flyway / JPA / Security | Flyway V1–V25 and JPA reached before context failure | V1–V25 applied; 40 JPA repositories and JWT security filter chain initialized; **PASS** |

Implemented corrections:

1. Added a fuel infrastructure adapter from `FuelActorPort` to the existing public identity `AuthenticatedUserLookup`; no fake or fallback actor was added.
2. Added a fuel infrastructure adapter from `FuelVendorPort` to the existing public organization `VendorLookup`, exposed after the actor correction allowed context initialization to advance.
3. Made notification severity part of `OperationalNotificationEvent`'s public integration contract and mapped it to the notification-internal severity in `NotificationRuleEngine`. The trip delay → public event → notification listener → rule engine flow remains intact.
4. Marked the production `TripController` constructor for Spring injection after the newly added test convenience constructor made constructor selection ambiguous.
5. Corrected four new persistence slices to use the repository H2 datasource configuration and supplied required foreign-key fixtures. Existing Flyway migrations were not changed.

Backend and architecture are now green. Overall release readiness remains **NOT READY** because the previously audited frontend lint and E2E gates remain unresolved. The next task is **MVP-FE-QUALITY-001**.

# Executive Summary

Total MVP Stories: 39  
Complete: 37  
Partial: 1  
Not Implemented: 1  
Blocked: 0

Functional Completion: 94.87%  
Release Readiness: NOT READY

The current tree contains the 37 historically complete MVP stories. US-77 has advanced because its previously missing administration page, notification center, hooks, permission-gated navigation, and frontend unit tests now exist. It remains PARTIAL because the implemented rule model is limited to event type, severity, channel, and recipient: there is no repository evidence for reusable templates, quiet hours/suppression windows, escalation chains, durable delivery retries, or broad event production. US-71 remains NOT IMPLEMENTED; vehicle-reading idempotency is a useful isolated server-side control, but it is not an offline synchronization solution.

The release is not ready. `mvn -B clean test` still reports the historical 118 errors, but they are not 118 independent defects. The first roots are (1) a missing `FuelActorPort` Spring bean, which prevents the application context from starting and cascades through integration/security tests, and (2) a Spring Modulith violation caused by `trip` using the non-exposed `notification.domain.model.NotificationSeverity`. Frontend production compilation and all Vitest tests pass, but lint and Playwright gates fail.

# 1. Build and Test Verification

## Backend

| Check | Result | Evidence |
|---|---|---|
| Command | FAIL | `mvn -B clean test`, Java 21.0.7, Maven 3.9.10 |
| Test totals | 520 run; 381 passed; 0 failures; 118 errors; 21 skipped | Aggregated `target/surefire-reports/TEST-*.xml` (125 suites) |
| Spring context | FAIL | `ContextSmokeTest`: `BunkerTankService` constructor parameter 5 has no bean of type `com.transportlogistics.app.fuel.application.ports.out.FuelActorPort` |
| First cascading root | FAIL | `FuelActorPort.java` exists and is consumed by `FuelConfig`, `FuelIssueService`, `FuelPurchaseService`, and `BunkerTankService`, but `rg` finds no production implementation or bean |
| Spring Modulith | FAIL | `ApplicationModulesTest.verifiesModuleBoundaries`: `TripOperationalEventService` accesses non-exposed `NotificationSeverity.INFO` from the notification module |
| Other architecture tests | PASS | `HexagonalLayerArchitectureTest` 7/7, `LombokUsageArchitectureTest` 3/3, `ModuleBoundaryArchitectureTest` 3/3 |
| Flyway/H2 | PASS before context wiring | Flyway validated and applied all 25 migrations, V1 through V25, then bean creation failed |

Root-cause separation:

1. `src/main/java/com/transportlogistics/app/fuel/application/ports/out/FuelActorPort.java` has no production adapter/bean. `src/main/java/com/transportlogistics/app/fuel/infrastructure/config/FuelConfig.java` requires it for fuel issue and purchase services, and component-scanned `BunkerTankService` requires it directly. This causes the Spring context failure and the large majority of the 118 errors; later messages stating that the context failure threshold was exceeded are cascades.
2. `src/main/java/com/transportlogistics/app/trip/application/service/TripOperationalEventService.java` imports `com.transportlogistics.app.notification.domain.model.NotificationSeverity`. The notification module does not expose that domain type through a named interface, so `src/test/java/com/transportlogistics/app/ApplicationModulesTest.java` independently errors.

There was no evidence of a Flyway SQL failure: all 25 migrations applied successfully in the first H2 context attempt. The test profile selected the default `h2` profile correctly.

## Frontend

| Check | Result | Evidence |
|---|---|---|
| Install | PASS | `npm install`: up to date |
| Production build | PASS | `npm run build`; TypeScript and Vite succeeded; output JS 1,824.50 kB (551.75 kB gzip) |
| Lint | FAIL | `npm run lint`: 67 errors, including explicit `any`, unused imports/variables, and two `prefer-const` cases across `src`, `e2e`, and legacy `test` |
| Unit/component tests | PASS | `npm test`: 22 files, 94 tests passed |
| Notification UI tests | PASS | `NotificationRulesPage.test.tsx` 5/5 and `NotificationCenter.test.tsx` 5/5 |
| E2E | FAIL (environment/harness) | `npm run test:e2e`: 108/108 failed; each generated result directory. First inspected error is `net::ERR_CONNECTION_REFUSED` for `http://localhost:5173/login` |

Warnings include an oversized Vite chunk, deprecated Ant Design props, static Ant Design message API usage outside app context, and unmatched MSW requests in two component tests. `frontend/playwright.config.ts` configures three browsers and a base URL but has no `webServer` block or global service bootstrap, so the E2E command starts neither frontend nor backend.

# 2. Current Module Status

| Module | Backend | Frontend | Tests | Overall | Evidence |
|---|---|---|---|---|---|
| Identity | Implemented | Implemented | Unit/controller tests pass; security integration is context-blocked | FEATURE COMPLETE / BUILD BLOCKED | `identity/application/service/IdentityService.java`; `identity/infrastructure/security/SecurityConfig.java`; `frontend/src/auth` |
| Master Data | Implemented | Implemented | Focused tests exist; integration verification context-blocked | COMPLETE | `OrganizationController.java`; organization ports/adapters; `ResourceListPage.tsx`; V1/V12/V13 |
| Fleet | Implemented | Implemented | Domain/service/controller tests pass; persistence/security tests context-blocked | COMPLETE | Fleet models/services/controllers/adapters; V1/V3/V4/V14-V16/V19-V23; `frontend/src/fleet` |
| Driver | Implemented | Implemented | Focused unit/component tests pass; integration tests context-blocked | COMPLETE | Driver profile, licence, availability, performance, violation, medical, drug-test, exception slices |
| Route | Implemented | Implemented | Focused tests pass; repository integration context-blocked | COMPLETE | `RouteService.java`, `RouteController.java`, route persistence, V1/V5, generic resource UI |
| Trip | Implemented | Implemented | Unit/controller/component tests pass; integration context-blocked | COMPLETE WITH ARCHITECTURE REGRESSION | Trip lifecycle/assignment/history/events; V1/V6-V8/V24; `frontend/src/trips` |
| Fuel | Implemented | Implemented | Unit/component tests pass; application integration cannot start | FEATURE COMPLETE / BUILD BLOCKED | Fuel issues, purchases, readings/cost, bunkers; V11/V12/V14-V18; `frontend/src/fuel` |
| Notification | Implemented core, incomplete policy depth | Implemented admin and center | Backend unit + 10 UI tests pass; security/persistence integration context-blocked; no E2E spec | PARTIAL | `notification` module, V25, `frontend/src/notifications` |
| Reporting | Implemented report queries and dashboard readiness endpoint | Dashboard implemented; no dedicated report screens | Focused tests exist; security integration context-blocked | COMPLETE under MVP interpretation | `reporting/application/service`; `ReportingController.java`; `DashboardPage.tsx` |
| Shared Infrastructure | Correlation/API errors present; context and Modulith gates fail | API/auth/query infrastructure present | Architecture 13/13 outside Modulith; Modulith fails | NOT RELEASE READY | `shared/web`; `SecurityConfig`; `ApplicationModulesTest` |
| Offline Synchronization | Absent | Absent | No tests | NOT IMPLEMENTED | No durable client store, queue, sync engine, conflict handler, or sync UI found |

# 3. Full MVP Story Status

The exact 39-story inventory is the one documented in `docs/mvp/MVP_V2_CURRENT_STATUS_AUDIT.md`. “Previous” below uses the supplied historical baseline: every story was COMPLETE except US-77 (PARTIAL) and US-71 (NOT IMPLEMENTED).

| Story ID | Story Name | Previous Status | Current Status | Backend Evidence | Frontend Evidence | Test Evidence | Gap |
|---|---|---|---|---|---|---|---|
| US-01 | Manage Vehicle Master | COMPLETE | COMPLETE | `fleet/domain/model/Vehicle.java`; `VehicleService.java`; `FleetController.java`; V1 | `ResourceListPage.tsx`, `ResourceEditorModal.tsx` | Vehicle service/controller tests; `vehicles.spec.ts` configured | Release integration blocked globally |
| US-02 | Manage Fleet Categories | COMPLETE | COMPLETE | `VehicleCategory.java`, `VehicleType.java`; services/adapters; V1 | Generic resource list/editor | Category/type tests; vehicle E2E configured | None functional |
| US-03 | Manage Vehicle Documents | COMPLETE | COMPLETE | `VehicleDocument.java`; `VehicleDocumentService.java`; persistence; V3 | Vehicle compliance drawer in generic fleet UI | Domain/service/repository tests; `vehicleDocs.spec.ts` | None functional |
| US-04 | Allocate Vehicles | COMPLETE | COMPLETE | `VehicleAvailabilityService.java`; trip/fleet lookup boundary; pessimistic assignment | `AssignmentDrawers.tsx` | Availability and concurrent-assignment tests | Global integration gate |
| US-05 | Maintain Fuel & Lubricant Logs | COMPLETE | COMPLETE | Fuel issue slice plus `LubricantLog.java`/service/persistence; V11/V23 | Fuel issue pages; `VehicleLubricantSection.tsx` | Lubricant and fuel tests; `lubricants.spec.ts` | None functional |
| US-06 | Maintain Running Logs | COMPLETE | COMPLETE | `VehicleReading.java`, meter reset, service/controller; V14-V16 | `VehicleReadingsSection.tsx`, hooks | Reading tests; `runningLogs.spec.ts` | None functional |
| US-07 | Link Maintenance to Availability | COMPLETE | COMPLETE | `MaintenanceSchedule.java`; service/persistence; availability integration; V19 | `VehicleMaintenanceSection.tsx` | 14 availability unit tests plus maintenance suites | Global integration gate |
| US-08 | Handle Fleet Allocation Edge Cases | COMPLETE | COMPLETE | Status/compliance/period checks, half-open overlap, locking | Assignment drawers render server reasons | Availability and concurrency tests | None functional |
| US-09 | Create Trip Orders | COMPLETE | COMPLETE | `Trip.java`; `TripService.createTrip`; controller/persistence; V1 | `TripListPage.tsx`, `TripEditorPage.tsx` | Trip service/controller/UI tests; `tripCreation.spec.ts` | None functional |
| US-10 | Assign Driver and Vehicle | COMPLETE | COMPLETE | `TripService.assignVehicle/assignDriver`; module ports; V6/V7 | `AssignmentDrawers.tsx` | Eligibility/concurrency tests | Global integration gate |
| US-11 | Assign Route | COMPLETE | COMPLETE | `TripService.assignRoute`; `RouteEligibilityAdapter`; V8 | Assignment drawer/details page | Route-assignment and details tests | None functional |
| US-12 | Start and End Trip | COMPLETE | COMPLETE | Start/complete lifecycle plus synchronous reading capture | `LifecycleActions.tsx` | Lifecycle/reading tests and E2E spec | Global integration gate |
| US-13 | Maintain Trip Log | COMPLETE | COMPLETE | `TripOperationalEvent`, use case/service/persistence/controller; V24 | `TripOperationalEventsSection.tsx` | Domain/service/controller/UI tests; E2E spec | Trip-to-notification boundary violation must be fixed |
| US-14 | Complete Trip | COMPLETE | COMPLETE | Completion/close invariants and odometer validation | Lifecycle modal/actions | Lifecycle and component tests | None functional |
| US-15 | Handle Trip Exceptions | COMPLETE | COMPLETE | Reject/cancel plus reasons and conflict release | Lifecycle actions | Lifecycle tests | None functional |
| US-16 | Authorize Trip | COMPLETE | COMPLETE | Submit/approve/reject; permission mapping in V9/SecurityConfig | Lifecycle actions permission-gated | Business authorization/lifecycle tests | Security integration context-blocked |
| US-17 | Define Routes | COMPLETE | COMPLETE | `Route.java`; use case/service/controller/persistence; V1 | Generic route list/editor | Route domain/service/controller tests | None functional |
| US-18 | Calculate Distance and ETA | COMPLETE | COMPLETE | Route distance and estimated duration persisted and consumed | Route list/editor fields | Route tests | No predictive traffic scope required |
| US-19 | Plan Multi-Stop Routes | COMPLETE | COMPLETE | Ordered `stopLocationIds`; `@OrderColumn`; V5 | Route editor stop inputs | Route domain/repository tests | No optimization scope required |
| US-31 | Issue Fuel | COMPLETE | COMPLETE | Fuel issue aggregate/policy/service/controller; V11 | Fuel issue list/editor/details | Backend and 12 frontend tests; E2E spec | Fuel integration context-blocked by missing actor bean |
| US-32 | Manage Fuel Purchases | COMPLETE | COMPLETE | Purchase/price/vendor lifecycle; V12 | Purchase and price pages | Backend and 10 frontend tests; E2E spec | Fuel integration context-blocked |
| US-33 | Track Mileage | COMPLETE | COMPLETE | Fleet-owned readings/meter reset; V14-V16 | Vehicle readings section | Reading suites and E2E spec | None functional |
| US-34 | Allocate Fuel Cost | COMPLETE | COMPLETE | `TripFuelCostService`, controller, fleet distance adapter; V17 | `TripFuelCostSection.tsx` | Backend and 2 frontend tests; E2E spec | Fuel integration context-blocked |
| US-36 | Manage Fuel Bunkers | COMPLETE | COMPLETE | Tank/ledger/dip/transfer services and adapters; V18 | Bunker list/details | Unit and 11 frontend tests; E2E spec | Missing actor bean blocks runtime construction |
| US-39 | Manage Driver Profiles | COMPLETE | COMPLETE | Driver aggregate/service/controller/persistence; V1 | Generic driver UI | Service/controller tests; drivers E2E | None functional |
| US-40 | Manage Driver Licensing | COMPLETE | COMPLETE | Licence aggregate/service/persistence; V4; eligibility integration | Driver details/assignment indicators | Licence and availability tests | None functional |
| US-41 | Assess Driver Performance | COMPLETE | COMPLETE | `DriverPerformanceService.java`, summary/metrics | `DriverPerformanceSection.tsx`, hook | Service and component tests; `performance.spec.ts` | None functional |
| US-42 | Manage Violations | COMPLETE | COMPLETE | Violation aggregate/service/persistence/controller; V21 | `DriverViolationsSection.tsx`, hook | Domain/service/persistence/UI tests; E2E spec | Integration context-blocked |
| US-43 | Manage Driver Medical Fitness | COMPLETE | COMPLETE | Medical record service/persistence/availability; V22 | `DriverMedicalSection.tsx`, hook | Domain/service/availability/UI tests; E2E spec | Integration context-blocked |
| US-44 | Manage Drug Tests | COMPLETE | COMPLETE | Drug-test lifecycle/service/persistence/availability; V22 | `DriverDrugTestSection.tsx`, hook | Domain/service/UI tests; E2E spec | Integration context-blocked |
| US-45 | Handle Driver Exceptions | COMPLETE | COMPLETE | Exception aggregate/service/persistence/availability; V20 | `DriverExceptionSection.tsx`, hook | Unit/UI tests and assignment integrations | Integration context-blocked |
| US-71 | Support Offline Data Sync | NOT IMPLEMENTED | NOT IMPLEMENTED | No sync API/queue; only isolated vehicle-reading idempotency | No IndexedDB/Dexie/localForage/service worker/sync UI | No offline/sync tests | Entire durable offline vertical slice absent |
| US-74 | Manage Security | COMPLETE | COMPLETE | JWT, rotating refresh tokens, BCrypt, permissions, `SecurityConfig` | Login/AuthContext and permission navigation | Identity unit/controller tests; integration context-blocked | Runtime context must be restored |
| US-75 | Maintain Audit and Reports | COMPLETE | COMPLETE | Append-only histories plus report services/read ports/controller | Dashboard consumes operations API; histories in details pages | Reporting/history/unit/UI tests; reporting E2E configured | Dashboard endpoint remains a simple readiness payload; detailed reports are API-first |
| US-77 | Manage Notification Rules | PARTIAL | PARTIAL | Rule/history persistence, engine, event listener, APIs, RBAC, V25 | Rules page, modal, center, hooks, nav/header integration | Backend unit tests and 10 UI tests pass; integration context-blocked; no Playwright notification spec | Templates, quiet/suppression, escalation, durable retry, broad event coverage absent |
| US-79 | Manage Master Data | COMPLETE | COMPLETE | Customer/department/project/location/vendor/station services; V1/V11-V13 | Generic resource pages | Organization tests; E2E indirectly covered | None functional |
| US-80 | Configure Workflows | COMPLETE | COMPLETE | Deterministic Trip/Fuel state machines | Lifecycle controls | Lifecycle tests | Complete under agreed MVP interpretation; no visual designer required |
| US-81 | Manage Scheduling | COMPLETE | COMPLETE | Trip windows, availability, overlap/conflict queries | Assignment drawers | Availability/concurrency tests; assignment E2E | Complete under agreed MVP interpretation; no Gantt/shift calendars required |
| US-83 | Manage Documents | COMPLETE | COMPLETE | Vehicle documents and driver licences with validity/file references | Fleet/driver detail UI | Document/licence tests; vehicle document E2E | OCR/versioning is not required MVP scope |

# 4. US-77 Deep Verification

The previously missing frontend has been implemented.

Exact frontend evidence:

- `frontend/src/notifications/NotificationRulesPage.tsx`: permission-gated rule table, enable/disable, create/edit/delete actions.
- `frontend/src/notifications/NotificationRuleModal.tsx`: event type, severity threshold, channel, recipient type/value, initial enable flag.
- `frontend/src/notifications/NotificationCenter.tsx`: header badge, drawer history, read/read-all actions, related-route navigation.
- `frontend/src/notifications/useNotificationRules.ts` and `useNotifications.ts`: TanStack Query API integration and polling.
- `frontend/src/App.tsx`: `/notification-rules` route.
- `frontend/src/navigation/navigation.tsx`: `NOTIFICATION_RULE_VIEW`-gated menu.
- `frontend/src/layout/AppLayout.tsx`: mounted notification center.

Backend and database evidence:

- Domain/application: `NotificationRule.java`, `Notification.java`, `NotificationRuleService.java`, `NotificationService.java`, `NotificationRuleEngine.java`.
- Events: `OperationalNotificationEvent.java`, `OperationalNotificationEventListener.java`; current production publishing was found in `TripOperationalEventService.recordDelay`.
- Persistence: rule/notification entities, JPA repositories and adapters under `notification/infrastructure/adapters/out/persistence`.
- Delivery: `InAppNotificationDeliveryAdapter.java`; `EmailNotificationDeliveryAdapter.java` (logs dispatch and explicitly comments where production SMTP logic belongs).
- Migration/RBAC: `V25__notification_rules.sql` creates `notification_rule` and `notification`, enforces event/rule/recipient idempotency, and seeds `NOTIFICATION_RULE_VIEW`, `NOTIFICATION_RULE_MANAGE`, `NOTIFICATION_VIEW`; `SecurityConfig.java` maps the endpoints.

Endpoints:

- `GET/POST /notification-rules`
- `GET/PUT/DELETE /notification-rules/{id}`
- `PATCH /notification-rules/{id}/enable`
- `PATCH /notification-rules/{id}/disable`
- `GET /notifications?limit=`
- `GET /notifications/unread-count`
- `PATCH /notifications/{id}/read`
- `PATCH /notifications/read-all`

Tests:

- Backend: `NotificationRuleTest`, `NotificationTest`, `NotificationRuleServiceTest`, `NotificationServiceTest`, `NotificationRuleEngineTest`, `NotificationControllerTest`, `NotificationPersistenceIntegrationTest`, `NotificationSecurityIntegrationTest`.
- Frontend: `NotificationRulesPage.test.tsx` 5/5 and `NotificationCenter.test.tsx` 5/5 passed.
- No notification-specific Playwright spec exists under `frontend/e2e/tests`.

Capability depth:

| Capability | Finding |
|---|---|
| Rule CRUD / enable-disable | Implemented |
| Channels | IN_APP implemented; EMAIL adapter is not a complete production SMTP delivery implementation |
| Recipients | USER, ROLE, EMAIL_ADDRESS modeled |
| Templates | Not modeled as reusable/versioned templates; event title/message are copied directly |
| Quiet hours / suppression windows | Not modeled |
| Escalation | Not modeled |
| Notification history / status | Implemented with PENDING/SENT/FAILED/READ, timestamps and failure reason |
| Delivery retry | No retry schedule, attempt ledger, backoff, or dead-letter behavior found |
| Idempotency | Implemented for event + rule + recipient |
| Event coverage | Only trip-delay production publication found despite a broader UI event catalog |
| Frontend/RBAC | Implemented |

Final US-77 status: PARTIAL

# 5. US-71 Deep Verification

Durable offline synchronization does not exist.

Repository-wide searches found no IndexedDB, Dexie, localForage, service worker, offline transaction queue, background synchronization, online/offline listener, sync-state UI, conflict resolver, or partial-recovery processor. The only related production control is the vehicle-reading `idempotencyKey` in `VehicleReading`, `VehicleReadingService`, persistence, API DTOs, and frontend types. That protects one server operation from duplicate submission; it does not capture operations offline or synchronize them later.

| Required mechanism | Current evidence |
|---|---|
| Durable local storage | None |
| Offline operation queue | None |
| Connectivity-restored synchronization | None |
| Retry/backoff | None |
| Source timestamps | Vehicle readings have timestamps, but no offline envelope |
| Local operation identifiers | No general local transaction ID; reading idempotency key only |
| Idempotency | One vehicle-reading slice only; no general sync protocol |
| Synchronization status UI | None |
| Conflict detection/resolution | No offline conflict protocol |
| Partial failure recovery | None |
| Store-and-forward / low-bandwidth behavior | None |
| Tests | None |

Final US-71 status: NOT IMPLEMENTED

# 6. Regression Analysis

| Metric | Previous | Current |
|---|---:|---:|
| Complete | 37 | 37 |
| Partial | 1 | 1 |
| Not Implemented | 1 | 1 |
| Blocked | 0 | 0 |
| Functional Completion | 94.87% | 94.87% |

NEWLY COMPLETED:

- No whole story changed to COMPLETE.
- Within US-77, the previously missing rule-administration page, notification center, hooks, route/navigation integration, and 10 frontend tests are now implemented and passing.

REGRESSED:

- The backend verification baseline remains red: missing `FuelActorPort` wiring causes context failure and 117 directly/cascading context errors; Spring Modulith adds a separate module-boundary error.
- Frontend lint is now a failing release gate with 67 errors.
- The configured E2E suite is not self-starting and all 108 cases fail with connection refused when invoked by its documented package script.

STILL MISSING:

- US-71 durable offline synchronization.
- US-77 template/suppression/escalation/retry depth and notification E2E coverage.

NEW GAPS DISCOVERED:

- The current notification event contract leaks a notification-internal enum into trip.
- Notification rule UI advertises multiple event types, while current production publication evidence covers only trip delay.
- Email delivery is a logging adapter with a production SMTP placeholder comment.

# 7. Release Readiness

Result: NOT READY

- Backend build/test: FAIL; application context cannot start.
- Spring Modulith: FAIL; module boundary violation.
- Flyway: PASS for V1-V25 on H2 before bean wiring failure.
- Frontend build: PASS.
- Frontend unit tests: PASS, 94/94.
- Frontend lint: FAIL, 67 errors.
- E2E: FAIL, 108/108 due to missing running test services; functional assertions were not reached.
- Security/RBAC: mappings are extensive and notification permissions are present, but security integration tests cannot load the context.
- Critical MVP gaps: US-71 is absent and US-77 is partial.

Feature evidence remains strong, but a release candidate cannot be declared while the application cannot construct its Spring context and the architectural verification fails.

# 8. Prioritized Remaining Work

| ID | Priority | User Story | Problem | Evidence | Backend impact | Frontend impact | Database impact | Tests required | Dependencies | Recommended order |
|---|---|---|---|---|---|---|---|---|---|---:|
| MVP-REGRESSION-FIX-001 | P0 | Cross-cutting | Missing `FuelActorPort` bean and notification boundary leak keep Maven red | Surefire roots above | Add the existing intended identity-to-fuel actor adapter/wiring; expose a stable notification event API without trip importing internal domain types | None | None | `mvn -B clean test`, context smoke, Modulith, security/integration suites | None | 1 |
| MVP-FE-QUALITY-001 | P0 | Cross-cutting | 67 lint errors fail frontend verification | ESLint output across `src`, `e2e`, `test` | None | Type unused/`any` cleanup without behavior change | None | build, lint, Vitest | None after P0 backend can run | 2 |
| MVP-E2E-HARNESS-001 | P0 | Cross-cutting | Playwright package script starts no services; 108 connection failures | `playwright.config.ts`; first error context | Test-service startup/seed readiness | Configure webServer/base URL lifecycle | None unless test seed correction is needed | smoke first, then Chromium and cross-browser | Runnable backend | 3 |
| MVP-GAP-008-FE | P1 | US-77 | Story is still partial despite completed UI: templates, quiet hours, suppression, escalation, retry and event breadth are absent | Notification domain/migration/engine and producer search | Extend agreed MVP notification policy and event integrations through module-safe contracts | Extend current UI only for agreed fields/status | Additive migration likely required | Domain/service/persistence/security/UI/E2E | Green baseline | 4 |
| MVP-GAP-011 | P1 | US-71 | No durable offline transaction pipeline | Negative repository search | Idempotent sync API/conflict protocol | IndexedDB queue, retry/status UI | Additive operation/inbox metadata likely | Offline, reconnect, duplicate, conflict, partial-sync E2E | Stable operation APIs and green E2E harness | 5 |
| MVP-FE-HARDEN-001 | P2 | Cross-cutting | Bundle/deprecation/MSW warnings | Build and Vitest output | None | Code splitting, Ant Design API cleanup, complete MSW handlers | None | build and component tests | P0 gates | 6 |

# 9. Recommended Next Task

MVP-REGRESSION-FIX-001

The selection rule mandates the regression task because `mvn -B clean test` fails due to real application regressions. It must restore the missing `FuelActorPort` production boundary and remove the direct trip dependency on notification-internal `NotificationSeverity`, then rerun the complete backend suite. US-77 and US-71 work must not begin until that baseline is green.

MVP STATUS:
Complete: 37/39
Partial: 1/39
Not Implemented: 1/39
Blocked: 0/39
Functional Completion: 94.87%

BUILD STATUS:
Backend: FAIL
Frontend: FAIL
E2E: FAIL

RELEASE READINESS:
NOT READY

HIGHEST PRIORITY GAP:
MVP-REGRESSION-FIX-001 — Restore Spring context and Modulith verification

NEXT TASK:
MVP-REGRESSION-FIX-001

NEXT ACTION:
Wire the intended production `FuelActorPort` adapter, replace the trip dependency on notification-internal severity with a module-safe public contract, and rerun `mvn -B clean test`.
