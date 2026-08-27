# Phase 1 MVP Gap Analysis

Audit date: 2026-08-14

Audited branch: `agent/trip-dispatch` at `eb334da`

Scope: the Fleet, Trip, Driver, Route, and cross-cutting MVP requirements stated for this repository.

This is a code-based audit. It covers the production source, application configuration, Flyway migrations, documentation, and the current 100-test suite. No production code was changed as part of the audit.

Path abbreviations used below:

- `app/` = `src/main/java/com/transportlogistics/app/`
- `test/` = `src/test/java/com/transportlogistics/app/`
- `db/` = `src/main/resources/db/migration/`

## Status definitions

- **IMPLEMENTED**: the stated MVP behavior is present end to end with meaningful validation and tests.
- **PARTIAL**: a usable implementation exists, but one or more material MVP behaviors, invariants, integrations, audit details, or tests are absent.
- **MISSING**: no meaningful business implementation exists; a route stub or a related field alone does not count.
- **DEFERRED**: intentionally outside the stated MVP. No audited requirement is currently classified this way.

## Executive assessment

| Status | Count |
|---|---:|
| IMPLEMENTED | 7 |
| PARTIAL | 18 |
| MISSING | 3 |
| DEFERRED | 0 |
| **Total** | **28** |

The codebase is beyond a skeleton: security, document/licence persistence, availability decisions, ordered route stops, conflict-aware assignment, concurrent allocation protection, dispatch revalidation, and dispatch audit are real implementations. The principal MVP risk is the remainder of the trip lifecycle. Submit, approve, reject, start, complete, close, and cancel currently change status without transition guards, transaction/audit consistency, actor capture, or adequate business validation. Running logs, trip logs, and reporting are not implemented, and maintenance is represented only by vehicle status strings.

The counts above are not a weighted completion percentage. Several PARTIAL items, especially trip lifecycle integrity, authorization, and audit, are foundational and affect multiple user stories.

## MVP requirement matrix

### Fleet

| Requirement ID | Feature | Status | Module | Current classes/files | Missing behavior | Recommended next action |
|---|---|---|---|---|---|---|
| US-01 | Manage Vehicle Master | PARTIAL | fleet | `fleet/domain/model/Vehicle.java`; `VehicleUseCase`; `VehicleService`; `FleetController`; `VehicleRepository`; `VehicleEntity`; `VehicleJpaRepository`; `VehiclePersistenceAdapter`; `VehicleConfig`; `db/V1__baseline.sql` | CRUD and soft deactivation exist, but the domain record has no invariants; category/type existence and compatibility are not checked; updates do not require the target to exist; duplicate/constraint errors are not normalized; no vehicle CRUD tests exist. | Add domain/application validation and repository/controller tests without redesigning `Vehicle`; validate category/type through fleet-owned ports. |
| US-02 | Manage Fleet Categories and Types | PARTIAL | fleet | `VehicleCategory`, `VehicleType`; their use cases, services, repository ports, entities, JPA repositories, persistence adapters, configs; category/type endpoints in `FleetController`; `db/V1__baseline.sql` | Basic CRUD/deactivation exists, but type-to-category existence is not validated, updates can upsert nonexistent IDs, referenced categories/types can be deactivated without impact checks, and there are no tests. | Enforce fleet-owned reference rules and add domain/application/persistence/controller coverage. |
| US-03 | Manage Vehicle Documents | IMPLEMENTED | fleet | `VehicleDocument`, `VehicleDocumentStatus`; `VehicleDocumentUseCase`; `VehicleDocumentService`; `VehicleDocumentRepository`; `VehicleDocumentEntity`; `VehicleDocumentJpaRepository`; `VehicleDocumentPersistenceAdapter`; `VehicleDocumentConfig`; document endpoints in `FleetController`; `db/V3__vehicle_documents.sql`; vehicle document domain/service/repository/controller tests | No binary object-storage/upload workflow, but the stated MVP is satisfied by the persisted file reference/URL. | Keep as-is for MVP; add PostgreSQL/Testcontainers coverage when database hardening is scheduled. |
| US-04 | Allocate Vehicles | IMPLEMENTED | trip + fleet + system | `TripUseCase.assignVehicle/unassignVehicle`; `TripService`; `TripRepository`; `VehicleEligibilityPort`; `VehicleEligibilityAdapter`; `VehicleAssignmentEligibility`; `VehicleAvailabilityService`; `VehicleAllocationLookup`; `TripVehicleAllocationAdapter`; `SpringTripTransaction`; `TripJpaRepository`; `TripHistoryRepository`; assignment controller/service/lookup/concurrency tests; `db/V6__trip_vehicle_assignment_audit.sql` | The isolated allocation operation is implemented. Broader state semantics and schedule mutation issues are tracked under US-08 and US-10. | Preserve the module boundary; address state semantics in the lifecycle slice. |
| US-06 | Maintain Running Logs | MISSING | fleet / trip | `Vehicle.currentOdometerKm`, `Vehicle.engineHours`, `Trip.startOdometerKm`, and `Trip.endOdometerKm` are only scalar snapshot fields in `db/V1__baseline.sql` | No append-only running-log model, use case, repository, table, API, timestamped odometer/engine-hour entries, provenance, or tests. Trip completion does not update a vehicle running history. | Implement an append-only vehicle running log owned by fleet and invoked through a module boundary from validated trip start/completion. |
| US-07 | Link Maintenance to Availability | PARTIAL | fleet | `VehicleAvailabilityService.addOperationalReason`; `VehicleAvailability.Code.MAINTENANCE_BLOCKED`; `Vehicle.operationalStatus`; availability tests for `MAINTENANCE`, `UNDER_MAINTENANCE`, and `MAINTENANCE_DUE` | There is no maintenance domain model, work order/schedule, persistence, port, or date-overlap query. Availability relies on manually setting a status string and cannot derive a block from maintenance records. | Introduce only the minimum fleet-owned maintenance availability record/port required by MVP, then include period overlap in `VehicleAvailabilityService`. |
| US-08 | Handle Fleet Allocation Edge Cases | PARTIAL | trip + fleet + system | Structured `VehicleAvailability`; document/type/capacity/status checks; half-open overlap queries in `TripJpaRepository`; pessimistic vehicle lock in `VehicleJpaRepository`; `SpringTripTransaction`; concurrent vehicle assignment test | Trip schedule/type/capacity can be edited after assignment without revalidation; `ASSIGNED` can be set when only one resource is present; vehicle unassignment always returns to `APPROVED` even if a driver remains; no database exclusion constraint; concurrency is tested on H2 only. | Correct assignment-state semantics and prohibit or revalidate assignment-affecting trip edits; add PostgreSQL Testcontainers concurrency coverage. |

### Trip

| Requirement ID | Feature | Status | Module | Current classes/files | Missing behavior | Recommended next action |
|---|---|---|---|---|---|---|
| US-09 | Create Trip Orders | PARTIAL | trip | `Trip`; `TripUseCase.create/get/list/update`; `TripService`; `TripController.TripRequest`; `TripRepository`; `TripEntity`; `TripJpaRepository`; `TripMapper`; `TripPersistenceAdapter`; `TripConfig`; `db/V1__baseline.sql` | The web layer supplies ID, trip number, timestamps, defaults, and initial status. There is no domain validation for date order, origin/destination difference, nonnegative capacity/passengers/odometers, valid priority/status, or referenced customer/department/project/route/location. Updates are allowed in every status and can upsert inconsistent data. There are no create/update tests. | Move trip-order invariants into domain/application code, validate references through module interfaces, and restrict updates by lifecycle state. |
| US-10 | Assign Driver and Vehicle | PARTIAL | trip + fleet + system | `TripService.assignVehicle/assignDriver/unassignVehicle/unassignDriver`; `VehicleEligibilityPort`; `DriverEligibilityPort`; fleet eligibility interfaces/adapters; overlap queries; `TripHistoryEntry`; assignment tests; `db/V6__...` and `db/V7__...` | Both assignment workflows are transactional and conflict-aware, but either individual assignment sets trip status to `ASSIGNED`; actor is not accepted for unassignment; the required licence class is recovered from history rather than assignment state; changing requested time/type/capacity after assignment bypasses eligibility. | Define assignment completeness explicitly, preserve both partial assignments without overstating state, persist assignment requirements directly, and revalidate assignment-affecting edits. |
| US-11 | Assign Route | PARTIAL | trip + routing | `Trip.routeId`; `TripController.TripRequest`; `Route`, `RouteUseCase`, `RouteService`, `RouteRepository`, routing persistence; `db/V1__baseline.sql`, `db/V5__route_stops.sql` | A route ID can be supplied on create/update, but there is no explicit assignment use case, routing module boundary, route existence/active validation, endpoint compatibility check, audit record, lifecycle restriction, or test. The database has no route foreign key. | Add a trip-owned route-assignment command that queries a minimal public routing interface and records history. |
| US-12 | Start and End Trip | PARTIAL | trip | `TripCommand.Start/Complete`; `/trips/{id}/start`; `/trips/{id}/complete`; actual time/odometer fields in `Trip`, `TripEntity`, and `db/V1__baseline.sql` | Start and complete have no allowed-from-state checks, assignment/dispatch preconditions, actor capture, transaction boundary, history record, required odometer validation, monotonic odometer rule, duplicate-command protection, or tests. Any trip can be started or completed. | Implement explicit transactional start and complete use cases with lifecycle guards, actor/time/odometer validation, and history. This belongs in the first next slice. |
| US-13 | Maintain Trip Log | MISSING | trip | `TripHistoryEntry` and `/trips/{id}/status-history` currently capture only vehicle assignment, driver assignment, unassignment, and dispatch | No operational trip-log model/table/API for timestamped notes, events, delays, checkpoints, mileage, or exception entries. Lifecycle events other than assignment/dispatch are absent from status history. | First complete lifecycle history; then add the minimum append-only trip operational log required by MVP. |
| US-14 | Complete Trip | PARTIAL | trip | `TripCommand.Complete`; `TripService.transition`; `/trips/{id}/complete`; actual end, end odometer, and completion remarks fields | Completion is a direct status mutation with no state guard, transaction, audit actor/history, odometer validation, required remarks policy, vehicle running-log update, or tests. | Include completion hardening in the first lifecycle slice, then publish/update running-log data through a fleet boundary. |
| US-15 | Handle Trip Exceptions | PARTIAL | trip | `TripCommand.Reject/Cancel`; reject and cancel endpoints; `REJECTED`/`CANCELLED` overlap exclusions in `TripJpaRepository` | Reasons are optional, transitions are unrestricted and unaudited, and there is no `INTERRUPTED` path, breakdown/interruption record, resume/recovery, in-progress cancellation policy, or safe reassignment behavior. | After lifecycle rules exist, add reason-required reject/cancel/interruption commands and append-only exception history. |
| US-16 | Authorize Trip | PARTIAL | trip + identity | submit/approve/reject endpoints and `TripCommand`; JWT authentication and authorities in identity | Submit/approve/reject ignore current state, actor, role/permission, approval metadata, history, and transaction boundaries. Any authenticated user can approve or reject. No authorization/lifecycle tests exist. | Add explicit trip permissions and enforce them at the HTTP/application boundary; implement audited DRAFT→SUBMITTED→APPROVED/REJECTED rules. |

### Driver

| Requirement ID | Feature | Status | Module | Current classes/files | Missing behavior | Recommended next action |
|---|---|---|---|---|---|---|
| DR-01 | Basic Driver Profile | PARTIAL | fleet | `Driver`; `DriverUseCase`; `DriverService`; driver endpoints in `FleetController`; `DriverRepository`; `DriverEntity`; `DriverJpaRepository`; `DriverPersistenceAdapter`; `DriverConfig`; `db/V1__baseline.sql` | CRUD/deactivation exists, but the domain has no invariants or typed status, updates need not load the target, duplicate persistence failures are not normalized, and no driver CRUD tests exist. | Add profile/status validation and CRUD tests while keeping Driver in fleet. |
| DR-02 | Driver Licensing | IMPLEMENTED | fleet | `DriverLicense`, `DriverLicenseStatus`; `DriverLicenseUseCase`; `DriverLicenseService`; `DriverLicenseRepository`; entity/JPA/persistence/config; licence endpoints; `db/V4__driver_licenses.sql`; domain/service/repository/controller tests | The global uniqueness rule, soft deletion, audit fields, date rules, and class validation are implemented. | Keep as-is for MVP; add PostgreSQL integration coverage later. |
| DR-03 | Driver Availability and Eligibility | PARTIAL | fleet + trip + system | `DriverAvailability`, `DriverAvailabilityUseCase`, `DriverAvailabilityService`; `DriverAssignmentAvailability`; `DriverAssignmentEligibility`; `TripDriverAssignmentAdapter`; structured availability endpoints; assignment and concurrent tests | The service incorrectly adds expiry/not-yet-valid reasons when *any* active licence is invalid even if another active licence fully satisfies the required class and period. Status values are untyped. No PostgreSQL concurrency test exists. | Evaluate validity against qualifying licences rather than unrelated active licences; add mixed-licence tests and PostgreSQL concurrency coverage. |

### Route

| Requirement ID | Feature | Status | Module | Current classes/files | Missing behavior | Recommended next action |
|---|---|---|---|---|---|---|
| RT-01 | Basic Route Definition | IMPLEMENTED | routing | `Route`; `RouteUseCase`; `RouteService`; `RouteController`; `RouteRepository`; `RouteEntity`; `RouteJpaRepository`; `RoutePersistenceAdapter`; `RouteConfig`; `db/V1__baseline.sql`; route domain/service/repository/controller tests | Location existence/active state is not validated and there are no database foreign keys, but the route definition itself is implemented. | Address location reference integrity with US-11 rather than rebuilding route CRUD. |
| RT-02 | Ordered Stops | IMPLEMENTED | routing | `Route.stopLocationIds`; validation in `Route`; `RouteEntity` `@ElementCollection`/`@OrderColumn`; `db/V5__route_stops.sql`; domain/controller/repository tests | Stop location existence is not checked. | Preserve ordering implementation; add location validation through a minimal organization boundary when route references are hardened. |
| RT-03 | Distance and Estimated Duration | IMPLEMENTED | routing | Positive distance/duration fields and validation in `Route` and `RouteController.RouteRequest`; persistence in `RouteEntity`; migration and tests | No material gap in the stated MVP. | Keep as-is. |
| RT-04 | Trip-Route Assignment | PARTIAL | trip + routing | `Trip.routeId`; route CRUD/persistence; trip create/update request | See US-11: only raw ID storage exists. | Implement together with US-11. |

### Cross-cutting

| Requirement ID | Feature | Status | Module | Current classes/files | Missing behavior | Recommended next action |
|---|---|---|---|---|---|---|
| XC-01 | Authentication | IMPLEMENTED | identity | `IdentityUseCase`; `IdentityService`; access/password/refresh ports; persistence adapters; `BCryptPasswordHasher`; `JwtAccessTokenService`; `JwtAuthenticationFilter`; `SecurityConfig`; identity endpoints; `db/V2__identity_security.sql`; identity domain/service/controller/security tests | No schema-level bootstrap administrator is supplied, so environments need an external provisioning path. This does not invalidate the authentication implementation itself. | Document provisioning and secret requirements; retain current token rotation and disabled-user checks. |
| XC-02 | Authorization | PARTIAL | identity + all business modules | JWT role/permission claims are refreshed against current database state; `/users/**` and `/roles/**` require `IDENTITY_MANAGE` | Fleet, route, trip, authorization, dispatch, reporting, and organization endpoints require authentication only. No business permissions are seeded or enforced. | Define the smallest MVP permission set and endpoint policy, prioritizing trip approval/dispatch and fleet mutation. |
| XC-03 | Audit and History | PARTIAL | trip + fleet + identity | Vehicle documents and driver licences have create/update actors/timestamps; assignments and dispatch write `TripHistoryEntry`; dispatch has `TripDispatchRecord`; correlation IDs are available | Core master records lack audit metadata; submit/approve/reject/start/complete/close/cancel and unassign actor details are incomplete; no immutable operational trip/running logs exist. | Complete lifecycle history first, then add append-only operational logs. |
| XC-04 | Business Validation | PARTIAL | all | Strong validation exists in `VehicleDocument`, `DriverLicense`, `Route`, and availability services; Jakarta validation covers selected requests | `Trip`, `Vehicle`, `Driver`, categories/types, identity records, and organization records are mostly anemic; reference validation and many database checks/FKs are missing; controllers own defaults/IDs/timestamps. | Start with trip lifecycle/order invariants and reference boundaries, then harden fleet master data. |
| XC-05 | Concurrency Protection | PARTIAL | trip + fleet | Transaction template; pessimistic vehicle/driver locks; overlap queries; concurrent assignment integration tests | Lifecycle transitions and schedule updates have no locking/versioning; no `@Version`; no database exclusion constraint; Testcontainers is declared but unused and concurrency runs only against H2. The transaction adapter's conflict message always says vehicle allocation, even for driver/dispatch conflicts. | Add optimistic/pessimistic protection where lifecycle commands mutate the same trip and verify PostgreSQL behavior with Testcontainers. |
| XC-06 | Basic Reporting and Dashboard | MISSING | reporting | `ReportingController` declares `/dashboard/operations`, `/reports/trips`, `/reports/driver-assignments`, and `/reports/vehicle-utilization` | Dashboard returns only `{date,status:READY}`; all reports return empty collections; there are no use cases, ports, repositories/adapters, DTOs, queries, validation, or tests. | Implement last, after lifecycle/history/log data is reliable; use read-only reporting ports/queries rather than importing module JPA repositories. |

## Existing MVP artifact inventory

### Identity

- Controller and DTOs: `IdentityController` with `LoginRequest`, `RefreshTokenRequest`, `AuthResponse`, `UserRequest`, `RoleRequest`, and password-free `UserResponse`.
- Input port/service: `IdentityUseCase`, `IdentityService`.
- Output ports: `IdentityRepository`, `AccessTokenService`, `PasswordHasher`, `RefreshTokenStore`.
- Domain: `User`, `Role`, `AuthTokens`, `IssuedRefreshToken`, `TokenClaims`, `AuthenticationFailedException`.
- Security adapters: `BCryptPasswordHasher`, `JwtAccessTokenService`, `JwtAuthenticationFilter`, `SecurityConfig`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, `SecurityErrorWriter`, `JwtProperties`.
- Persistence: `UserEntity`, `RoleEntity`, `RefreshTokenEntity`; `UserJpaRepository`, `RoleJpaRepository`, `RefreshTokenJpaRepository`; `IdentityPersistenceAdapter`, `RefreshTokenPersistenceAdapter`; `IdentityConfig`.
- Migration: `db/V1__baseline.sql`, `db/V2__identity_security.sql`.
- Tests: `identity/application/service/IdentityServiceTest.java`; `identity/domain/model/UserAuthorizationTest.java`; `identity/infrastructure/adapters/in/web/IdentityControllerTest.java`; `identity/infrastructure/security/IdentitySecurityIntegrationTest.java`; `JwtAccessTokenServiceTest.java`.

### Organization support used by trip/routing references

- Controller and DTOs: `OrganizationController` with customer, department, location, and project request records.
- Input ports/services: `CustomerUseCase`/`CustomerService`, `DepartmentUseCase`/`DepartmentService`, `LocationUseCase`/`LocationService`, `ProjectUseCase`/`ProjectService`.
- Output ports/persistence: corresponding repository ports, entities, JPA repositories, persistence adapters, and configuration classes for all four concepts.
- Domain: `Customer`, `Department`, `Location`, `Project`.
- Migration: `db/V1__baseline.sql`.
- Tests: none.
- Relevant limitation: trip and routing store organization/location IDs without querying a public organization interface or relying on foreign keys.

### Fleet and Driver

- Controller and DTOs: `FleetController`; `DriverRequest`, `DriverLicenseRequest`, `DriverLicensePatchRequest`, `VehicleRequest`, `CategoryRequest`, `TypeRequest`, `DocumentRequest`, and `DocumentPatchRequest`.
- Input ports/services: `DriverUseCase`/`DriverService`, `DriverLicenseUseCase`/`DriverLicenseService`, `DriverAvailabilityUseCase`/`DriverAvailabilityService`, `VehicleUseCase`/`VehicleService`, `VehicleDocumentUseCase`/`VehicleDocumentService`, `VehicleAvailabilityUseCase`/`VehicleAvailabilityService`, `VehicleCategoryUseCase`/`VehicleCategoryService`, `VehicleTypeUseCase`/`VehicleTypeService`.
- Output ports: `DriverRepository`, `DriverLicenseRepository`, `VehicleRepository`, `VehicleDocumentRepository`, `VehicleCategoryRepository`, `VehicleTypeRepository`.
- Public/module interfaces: `DriverAssignmentAvailability`, `DriverAssignmentEligibility`, `VehicleAllocationAvailability`, `VehicleAssignmentEligibility`, `VehicleDispatchEligibility`.
- Domain: `Driver`, `DriverLicense`, `DriverLicenseStatus`, `DriverAvailability`, `Vehicle`, `VehicleDocument`, `VehicleDocumentStatus`, `VehicleAvailability`, `VehicleCategory`, `VehicleType`.
- Persistence: six matching entities, JPA repositories, persistence adapters, and six configuration classes.
- Migrations: `db/V1__baseline.sql`, `db/V3__vehicle_documents.sql`, `db/V4__driver_licenses.sql`.
- Tests: 12 classes covering document/licence domain and service rules, document/licence persistence and controllers, and structured vehicle/driver availability. Vehicle, driver, category, and type CRUD have no tests.

### Routing

- Controller/DTO: `RouteController`, `RouteRequest`.
- Input port/service: `RouteUseCase`, `RouteService`.
- Output port/persistence: `RouteRepository`, `RouteEntity`, `RouteJpaRepository`, `RoutePersistenceAdapter`, `RouteConfig`.
- Domain: `Route` including ordered `stopLocationIds` and planning fields.
- Migrations: `db/V1__baseline.sql`, `db/V5__route_stops.sql`.
- Tests: `RouteTest`, `RouteServiceTest`, `RouteControllerTest`, `RouteRepositoryIntegrationTest`.

### Trip

- Controller and DTOs: `TripController`; `TripRequest`, `VehicleAssignmentRequest`, `DriverAssignmentRequest`, `ReasonRequest`, `StartRequest`, `CompleteRequest`, `DispatchRequest`.
- Input port/service: `TripUseCase`, `TripService`.
- Output ports: `TripRepository`, `TripHistoryRepository`, `TripDispatchRepository`, `TripTransaction`, `VehicleEligibilityPort`, `DriverEligibilityPort`.
- Public/module interfaces: `VehicleAllocationLookup`, `DriverAssignmentLookup`.
- Domain: `Trip`, `TripCommand`, `TripHistoryEntry`, `TripDispatchRecord`.
- Fleet adapters: `VehicleEligibilityAdapter`, `DriverEligibilityAdapter`.
- Persistence: `TripEntity`, `TripHistoryEntity`, `TripDispatchEntity`; `TripJpaRepository`, `TripHistoryJpaRepository`, `TripDispatchJpaRepository`; `TripPersistenceAdapter`, `TripHistoryPersistenceAdapter`, `TripDispatchPersistenceAdapter`; `TripMapper`; `TripConfig`; `SpringTripTransaction`.
- Cross-module composition: `system/TripVehicleAllocationAdapter`, `system/TripDriverAssignmentAdapter`.
- Migrations: `db/V1__baseline.sql`, `db/V6__trip_vehicle_assignment_audit.sql`, `db/V7__trip_driver_assignment_audit.sql`, `db/V8__trip_dispatch.sql`.
- Tests: 11 classes covering vehicle/driver assignment service and controller behavior, overlap lookups, H2 concurrency, dispatch rejection paths, controller actor propagation, and dispatch persistence. Trip creation, editing, submit, approve, reject, start, complete, close, cancel, and full history have no tests.

### Reporting, System, and Shared

- Reporting: only `ReportingController`; no domain/application/persistence layers or tests.
- System: `HealthController` plus the two composition adapters connecting fleet availability to trip-owned overlap lookups.
- Shared: `NotFoundException`, `ConflictException`, `ApiError`, `GlobalExceptionHandler`, and `CorrelationIdFilter`.
- Root tests: `ApplicationModulesTest` verifies Spring Modulith structure; `ContextSmokeTest` verifies application context startup.

## Architectural and implementation findings

### Consistent architecture already present

- Spring Modulith verification passes, and no trip code imports fleet JPA repositories/entities.
- Cross-module availability queries use public interfaces and system composition adapters rather than shared repositories.
- Most application services are framework-free and wired by module configuration.
- JPA entities remain inside persistence adapters and are not returned by REST controllers.
- Assignment and dispatch use application-owned transaction boundaries and fleet-owned eligibility rules.

### Inconsistencies and risks

1. **Trip lifecycle is not a state machine.** `TripService.transition` accepts typed commands but performs direct status copies with no allowed-from-state validation except dispatch. Start, complete, close, reject, approve, submit, and cancel can execute from any state.
2. **Lifecycle audit is incomplete.** Only assignment/unassignment and dispatch write `trip_status_history`; most lifecycle endpoints do not capture actor, reason, or timestamped history.
3. **Transaction boundaries are inconsistent.** Assignment and dispatch use `TripTransaction`; other lifecycle mutations and trip updates do not.
4. **Assignment status semantics conflict.** Either a vehicle-only or driver-only assignment sets `ASSIGNED`. Vehicle unassignment always sets `APPROVED`, whereas driver unassignment preserves `ASSIGNED` if a vehicle remains.
5. **Trip edits can bypass eligibility.** `PUT /trips/{id}` can alter requested time, required type/capacity, route, and endpoints after resources are assigned without conflict or eligibility revalidation.
6. **Controllers contain application decisions.** Controllers generate IDs, trip numbers, timestamps, default statuses, and default operational values, and construct/return domain records directly. This weakens the stated ports/adapters separation even though JPA entities are not exposed.
7. **Reference integrity is mostly implicit.** `V1__baseline.sql` omits foreign keys for vehicle category/type, vehicle type/category, project/department, route locations, and all trip references. Application services also do not validate most of these references.
8. **Business authorization is incomplete.** Security verifies authentication for all nonpublic routes, but only identity administration has an authority rule.
9. **Availability contains duplicated/ambiguous licence logic.** `DriverAvailabilityService` evaluates `anyMatch` across all active licences for expired/not-yet-valid reasons, which can reject a driver even when another licence satisfies the requested class and period.
10. **PostgreSQL behavior is untested.** Testcontainers dependencies are present, but there are no container declarations; all repository and concurrency integration tests use the default H2 profile.
11. **Error semantics are too broad.** Every `ConflictException` is serialized with code `ALLOCATION_CONFLICT`, and most domain rule failures share `BAD_REQUEST`; constraint violations outside handled exceptions may not produce `ApiError`.
12. **Time is not injected outside identity.** Fleet/trip/routing use direct `OffsetDateTime.now()` calls, making time-sensitive lifecycle/document behavior harder to test consistently.

## Duplicate logic, placeholders, dead code, and TODOs

- No explicit `TODO` or `FIXME` markers were found in production code.
- `ReportingController` is placeholder behavior: the dashboard says `READY`, while all report content is empty.
- `VehicleDispatchEligibility`, `VehicleEligibilityPort.assertEligibleForDispatch`, and the corresponding method/dependency in `VehicleEligibilityAdapter` are wired but not called by current production dispatch. Dispatch now uses intrinsic assignment eligibility plus trip-owned overlap checks. This is dead/duplicate dispatch eligibility wiring.
- `FleetController.AvailabilityResponse` is declared but unused; structured domain availability objects are returned instead.
- Trip transition logic repeatedly copies the entire `Trip` record and embeds string status literals in `TripService`; there is no single transition policy.
- Defaults are duplicated between controller mapping and loosely typed domain values (`AVAILABLE`, `COMPANY_OWNED`, `NORMAL`, status strings).
- The two overlap-query paths (fleet availability through system adapters and direct trip orchestration checks) are intentional for read APIs versus lock-aware assignment, but their responsibilities need documentation to avoid future duplication.

## Stale or misleading documentation

### `README.md`

- The statement that authentication endpoints are development contract stubs and do not provide production JWT security is stale. JWT signing/verification, BCrypt, access-token expiry, refresh rotation/revocation/expiry, disabled-user checks, current role/permission refresh, security errors, and tests are implemented.
- The statement that driver licences and vehicle documents need dedicated persistence tables is stale. `V3__vehicle_documents.sql` and `V4__driver_licenses.sql` plus full adapters and tests exist.
- “API surface preserved” is accurate only as route-family coverage. Reporting endpoints are placeholders, and several trip lifecycle routes do not yet enforce their business semantics.

### `AGENTS.md`

- Its module list (`masterdata`, separate `driver`, `route`, `notification`) does not match the actual modules (`identity`, `organization`, `fleet`, `routing`, `trip`, `reporting`, `system`, `shared`). Driver belongs to fleet and no notification module exists.
- It specifies `/api/v1`, while `application.yml` configures context path `/api` and controllers map routes directly beneath it.

### `docs/openapi-contract-inventory.md`

- It correctly states that the original OpenAPI source is absent and only route families were reconstructed.
- It should not be treated as proof of functional coverage: it inventories endpoint families, not implemented business behavior or schemas.

## Test coverage assessment

- Current suite: **100 tests, 0 failures, 0 errors, 0 skipped** in the most recent `mvn verify` run on this branch.
- Strong coverage: identity security; vehicle documents; driver licences; vehicle/driver availability rejection reasons; route validation/search/stops; trip vehicle/driver assignment conflicts and concurrency; dispatch revalidation/audit.
- Missing coverage: vehicle/driver/category/type CRUD; organization; trip order create/update; route assignment to trip; submit/approve/reject; start/complete/close/cancel; trip operational logs; maintenance; reporting; business-module authorization.
- Database caveat: repository and concurrency integration tests run with H2. The declared PostgreSQL Testcontainers dependencies are unused.

## Dependency-ordered remaining work

1. **Trip lifecycle integrity and complete status history.** Add explicit allowed transitions and validation for DRAFT→SUBMITTED→APPROVED/REJECTED→ASSIGNED→DISPATCHED→IN_PROGRESS→COMPLETED→CLOSED plus cancellation rules; make submit/approve/reject/start/complete/close/cancel transactional and audited with actors/reasons/odometers. Correct partial-assignment state semantics.
2. **MVP permissions for business commands.** Seed and enforce the minimum trip/fleet/report permissions, especially approve, reject, dispatch, start, complete, and master-data mutations. Add 403 integration tests.
3. **Trip order and route/reference integrity.** Validate periods, endpoints, quantities, lifecycle-edit restrictions, active organization references, and route assignment through minimal module interfaces. Revalidate assigned resources when assignment-affecting fields change.
4. **Driver mixed-licence correctness and PostgreSQL concurrency verification.** Correct qualifying-licence evaluation and run assignment/dispatch concurrency scenarios with Testcontainers PostgreSQL.
5. **Running log and trip operational log.** Add append-only logs after start/complete invariants and history are trustworthy; connect vehicle mileage through a fleet-owned boundary.
6. **Maintenance-to-availability linkage.** Add the smallest persisted maintenance blocking concept and period overlap needed by US-07; keep maintenance ownership in fleet.
7. **Trip exceptions.** Add audited, reason-required interruption/cancellation/recovery behavior on top of the lifecycle state machine and logs.
8. **Fleet/driver/category/type master-data hardening.** Add domain/reference validation, safe deactivation rules, consistent conflicts, and missing CRUD tests.
9. **Reporting/dashboard.** Replace controller stubs with read-only reporting use cases and adapters only after lifecycle, assignment, dispatch, and log data are reliable.
10. **Documentation/OpenAPI alignment.** Update README/module/base-path claims and establish a maintained OpenAPI contract after behavior stabilizes.

## Exact recommended next implementation slice

Implement **Trip lifecycle integrity and full lifecycle audit** inside the existing trip module:

- enforce valid current state for submit, approve, reject, start, complete, close, and cancel;
- require both assignments before the trip is considered fully `ASSIGNED` and before dispatch;
- preserve the existing hardened dispatch flow;
- make every lifecycle mutation transactional;
- capture authenticated actor, required reason/remarks, occurred-at time, and from/to status in `trip_status_history`;
- validate requested period, actual times, and nondecreasing start/end odometers;
- add domain/application/controller/persistence tests for every valid and rejected transition.

Likely files to touch in that slice:

- `app/trip/domain/model/Trip.java`
- `app/trip/domain/model/TripCommand.java`
- `app/trip/domain/model/TripHistoryEntry.java` (only if additional lifecycle detail is required)
- `app/trip/application/ports/in/TripUseCase.java`
- `app/trip/application/service/TripService.java`
- `app/trip/infrastructure/adapters/in/web/TripController.java`
- `app/trip/infrastructure/config/TripConfig.java` (only if a clock or new application dependency is introduced)
- existing trip history persistence classes; a new Flyway migration only if explicit authorization/start/completion metadata cannot be represented by current history fields
- new or expanded tests under `test/trip/domain/model/`, `test/trip/application/service/`, `test/trip/infrastructure/adapters/in/web/`, and `test/trip/infrastructure/adapters/out/persistence/`

This slice should precede running logs, exception workflows, and reporting because those capabilities depend on trustworthy lifecycle events and timestamps.
