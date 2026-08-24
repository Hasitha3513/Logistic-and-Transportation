# Full Capability Gap Audit

Audit date: 2026-08-16  
Audited branch/commit: `agent/docker-compose-full-stack` at `dccfbd5`, including the current working tree  
Scope source: the capability catalogue supplied for this audit, normalized to remove repeated labels, plus the historical `docs/mvp-gap-analysis.md` baseline  
Change boundary: audit only; no production code was modified

## 1. Executive summary

The repository is a functioning modular monolith, not a prototype. Phase 1's core operational path—identity, fleet/driver masters, compliance records, availability, route definition, trip order, audited assignment, dispatch, start, completion, close and cancellation—is materially implemented. Phase 2 US-31 Fuel Issue and US-32 Fuel Purchases are also present end to end in the current working tree.

The broad catalogue is much larger than the approved product scope. Freight, delivery, GPS, mobile, offline synchronization, advanced routing, notifications and most external integrations have no approved contract and are correctly classified as deferred or out of scope rather than release defects. The principal current gaps are real reporting data, trip/running operational logs, persisted maintenance scheduling, organization-specific business permissions, and automated PostgreSQL concurrency coverage.

| Status | Count | Share of 84 normalized capabilities |
|---|---:|---:|
| IMPLEMENTED | 23 | 27.4% |
| PARTIAL | 20 | 23.8% |
| MISSING | 10 | 11.9% |
| DEFERRED | 29 | 34.5% |
| OUT_OF_SCOPE | 2 | 2.4% |
| NOT_APPLICABLE | 0 | 0.0% |

The catalogue-wide implementation percentage is **27.4%**. This number is intentionally low because 31 capabilities are deferred/out of scope. Among the 53 non-deferred assessed capabilities, 43.4% are fully implemented and another 37.7% are partial. These are unweighted counts; they must not be read as effort estimates.

## 2. Repository baseline

- Backend: Java release 21, Spring Boot 3.2.12, Spring Modulith 1.2.12, Spring Data JPA, Spring Security/JWT, Flyway, H2 and PostgreSQL.
- Frontend: React 19, TypeScript, Vite, Ant Design, TanStack Query, React Hook Form, Zod, Axios, Vitest, Testing Library and MSW.
- Runtime base path is `/api`, despite `AGENTS.md` still stating `/api/v1`.
- Current application modules are `identity`, `organization`, `fleet`, `routing`, `trip`, `fuel`, `reporting` and `system`; `shared` supplies named domain/web interfaces.
- Latest migration is `src/main/resources/db/migration/V12__fuel_purchases.sql`; Phase 1 ends at V10, US-31 is V11 and US-32 is V12.
- The working tree was already dirty before this audit. US-32 source, migration, frontend and tests are untracked or modified and therefore are not represented by commit `dccfbd5` alone. This audit evaluates the complete working tree because that is the runnable baseline.
- The local Compose stack was healthy during audit: PostgreSQL, backend and frontend were all running; backend `/api/health` returned `UP`.

## 3. Current module map

| Module | Current responsibility | Principal public boundary |
|---|---|---|
| `identity` | Users, roles, permissions, login, JWT and refresh-token lifecycle | `AuthenticatedUserLookup`, `IdentityUseCase` |
| `organization` | Customer, department, location, project and vendor master | `VendorLookup`; other masters currently expose use cases only |
| `fleet` | Vehicles, categories/types, documents, drivers, licences and eligibility | `VehicleAssignmentEligibility`, `VehicleAllocationAvailability`, `DriverAssignmentEligibility`, `DriverAssignmentAvailability`, `VehicleFuelContextLookup` |
| `routing` | Route CRUD, ordered stops and route assignment lookup | `RouteAssignmentLookup` |
| `trip` | Trip order, route/resource assignment, lifecycle, dispatch and status history | `VehicleAllocationLookup`, `DriverAssignmentLookup`, `TripFuelContextLookup` |
| `fuel` | US-31 issue, station/limits, US-32 purchase/price/reconciliation and events | Application use cases plus immutable Spring events |
| `reporting` | Placeholder dashboard and report HTTP contracts | None beyond its controller |
| `system` | Health, local sample bootstrap and composition adapters | No business ownership |

`AGENTS.md` names nonexistent `masterdata`, `driver`, `route` and `notification` modules; the source annotations above are authoritative.

## 4. Current backend architecture map

The dominant implementation follows controller → input port → application service/domain policy → output port → persistence adapter → JPA repository. Examples include `TripController` → `TripUseCase` → `TripService`/`TripLifecyclePolicy` → `TripRepository` → `TripPersistenceAdapter` → `TripJpaRepository`, and the equivalent US-31/US-32 Fuel paths.

Cross-module dependencies are adapter-based and do not import another module's JPA repository/entity:

- Trip adapters consume Fleet's assignment-eligibility interfaces and Routing's `RouteAssignmentLookup`.
- Fuel adapters consume Fleet's `VehicleFuelContextLookup`, Trip's `TripFuelContextLookup`, Identity's `AuthenticatedUserLookup`, and Organization's `VendorLookup`.
- System adapters connect Fleet availability to Trip-owned overlap lookups.

The largest architectural exception is `ReportingController`, which has no application/domain/persistence layers and returns placeholders. Organization endpoints also lack explicit business permission rules.

## 5. Current frontend architecture map

- `frontend/src/App.tsx` provides protected routes for dashboard, fleet masters, drivers, routes, trips, Fuel Issues, Fuel Purchases, Fuel Prices and identity administration.
- `frontend/src/navigation/navigation.tsx` filters navigation using backend permissions.
- Generic `ResourceListPage` covers Phase 1 masters; trip and fuel workflows have dedicated pages, action components, hooks and tests.
- Status presentation is centralized under `frontend/src/components/status/StatusTags.tsx`.
- Trip list pagination remains a documented client-side fallback because `GET /trips` is unpaged.
- Dashboard cards honestly show “Not supplied by reporting API” rather than inventing values.
- There are no report, maintenance, running-log, tracking, freight, delivery, notification or mobile pages.

## 6. Flyway and schema baseline

V1–V12 create identity, organization, fleet, route, trip, compliance, assignment/dispatch audit, permissions, Fuel Issue and Fuel Purchase structures. V10 adds important foreign keys and lookup indexes missing from V1. V11/V12 add fuel audit histories, constraints and permissions.

Strengths:

- UUID primary keys and database uniqueness exist for core business references.
- Trip/fleet/route foreign keys are enforced from V10.
- Purchase number and voucher sequences are database backed.
- Fuel calculations use numeric columns; purchase money uses `NUMERIC(19,2)` and quantities use four decimal places.
- Append-only trip/fuel histories have foreign keys and chronological indexes.

Remaining database findings:

- No `@Version` optimistic locking and no PostgreSQL exclusion constraint protects overlapping allocations; application transactions and pessimistic locks carry the invariant.
- Automated repository/concurrency tests use H2. The runtime PostgreSQL stack applies V1–V12, but there is no Testcontainers PostgreSQL test suite.
- Maintenance, vehicle running log, trip operational log and reporting projection tables do not exist.
- Vehicle/driver operational statuses remain free-form strings rather than database-constrained enums.
- Active-price overlap is application-enforced; the database lookup index does not itself prevent overlaps.
- Vendor/invoice uniqueness is database-enforced but case sensitivity follows the database collation while application checks are case-insensitive.

## 7. Security baseline

Public endpoints are `/health`, `/auth/login`, `/auth/refresh`, Swagger/OpenAPI and `/error`. All other routes require authentication. Identity administration requires `IDENTITY_MANAGE`; fleet, driver, routing, trip, Fuel Issue, Fuel Purchase, price, dashboard and report routes have explicit permission mappings with deny-all fallbacks.

The current bootstrap administrator receives 49 permissions. Authentication uses BCrypt cost 12, signed expiring access tokens, hashed rotating/revocable refresh tokens, stateless Spring Security, disabled-user checks and fresh role/permission lookup. Security errors use the shared `ApiError` shape and correlation IDs.

Security gaps:

- Customer, department, location and project endpoints fall through to authenticated-only access; mutations lack organization-specific permissions.
- Browser tokens are stored in `localStorage`, increasing exposure if an XSS flaw occurs.
- Swagger is public in every profile; production exposure is a deployment acceptance item.
- ABAC, SSO, MFA, device authorization, segregation of duties and privileged-access monitoring are not approved/current features.

## 8. Capability coverage summary by group

| Group | Assessment |
|---|---|
| Dashboard/reporting | UI is ready for real data; backend remains placeholder-only. |
| Fleet/compliance | Core master, documents and eligibility exist; rich asset/rental, maintenance and running-log behavior does not. |
| Trip | Phase 1 order, route/resource assignment and strict lifecycle are implemented; operational logs and interruption remain gaps. |
| Routing | Basic ordered routes are complete; constraints, optimization and analytics are future work. |
| Driver | Basic master, licences and coherent multi-licence eligibility exist; medical/performance/payroll are future. |
| Fuel | US-31 and US-32 are implemented; stock/bunker, mileage enhancement and analytics remain deferred. |
| Security/audit | JWT/RBAC and operational histories are strong; organization permissions and broad activity/regulatory audit are incomplete. |
| Freight/delivery/GPS/mobile/integrations | No approved implementation; deferred/out of scope. |

## 9. Detailed capability matrix

Paths are repository-relative. “None” means no material production artifact was found.

| Capability ID | Capability | Status | Delivery phase | Module | Backend evidence | Frontend evidence | Database evidence | Test evidence | Missing behavior | Risk | Recommended action |
|---|---|---|---|---|---|---|---|---|---|---|---|
| DASH-001 | Operations dashboard metrics | PARTIAL | REQUIRES_PRODUCT_DECISION | reporting | `reporting/.../ReportingController.java` returns date/status only | `pages/DashboardPage.tsx` renders supplied fields honestly | None | `DashboardPage.test.tsx` | Real vehicle, driver, trip and completion aggregates | Medium | Define reporting read ports after product accepts metric definitions. |
| DASH-002 | Dashboard alerts and exception widgets | MISSING | REQUIRES_PRODUCT_DECISION | reporting | None | Empty-capable alert tables exist in `DashboardPage.tsx` | None | Placeholder rendering test only | Alert source, severity policy and queries | Medium | Specify alert ownership and projection contract. |
| DASH-003 | Live tracking map/heat maps | DEFERRED | FUTURE_ROADMAP | future tracking | None | None | None | None | Entire tracking foundation | Low now | Defer until tracking ingestion is approved. |
| FLEET-001 | Vehicle master CRUD | PARTIAL | PHASE_1_COMPLETE | fleet | `Vehicle`, `VehicleUseCase`, `VehicleService`, `FleetController`, persistence stack | `ResourceListPage` vehicle configuration | V1 `vehicle`; V10 FKs/index | No focused vehicle CRUD suite | Typed invariants, duplicate error normalization and focused tests | Medium | Harden in a master-data quality slice, not US-33. |
| FLEET-002 | Ownership/rental/lease details | PARTIAL | PHASE_2_CANDIDATE | fleet | `Vehicle.ownershipType` only | Ownership field in vehicle form | V1 `ownership_type` | Indirect UI coverage | Contract dates, lessor, rates and expiry rules | Medium | Obtain rental-fleet contract before schema work. |
| FLEET-003 | Vehicle operational/status lifecycle | PARTIAL | PHASE_1_COMPLETE | fleet | Free-form `operationalStatus`; deactivate endpoint | Vehicle status display/edit | V1 status/active index | Availability status tests | Explicit allowed statuses/transitions and retired semantics | Medium | Centralize typed status policy in future hardening. |
| FLEET-004 | Categories, types, capacity and usage classes | PARTIAL | PHASE_1_COMPLETE | fleet | Category/type CRUD and type-category relationship | Category/type pages | V1 tables; V10 FKs | No focused master CRUD tests | Usage-category and reference-deactivation rules | Medium | Add reference-safe deactivation and tests. |
| FLEET-005 | Asset tags and QR codes | MISSING | PHASE_2_CANDIDATE | fleet | None | None | None | None | Asset/QR identity, generation and lookup | Low | Require a concrete scanning/use contract first. |
| FLEET-006 | Special-equipment vehicle attributes | DEFERRED | FUTURE_ROADMAP | fleet | None | None | None | None | Equipment taxonomy and matching rules | Low | Defer to cargo/route constraint scope. |
| DOC-001 | Vehicle document CRUD and soft retirement | IMPLEMENTED | PHASE_1_COMPLETE | fleet | `VehicleDocument`, use case/service, controller and persistence stack | Vehicle compliance detail in resource UI | V3 `vehicle_document` | Domain, service, controller and repository tests | None material for file-reference MVP | Low | Preserve. |
| DOC-002 | Expiry/missing mandatory-document dispatch blocking | IMPLEMENTED | PHASE_1_COMPLETE | fleet/trip | `VehicleAvailabilityService`; dispatch revalidation in `TripService` | Eligibility reasons shown in assignment UI | V3 dispatch index | Vehicle availability and dispatch rejection tests | None material | Low | Preserve and add PostgreSQL coverage later. |
| DOC-003 | Document version history | PARTIAL | REQUIRES_PRODUCT_DECISION | fleet | Audit metadata and soft delete, but no version aggregate | Current version only | V3 single-row records | Update/delete tests | Immutable versions and retrieval API | Low | Confirm retention requirement before extending. |
| DOC-004 | Renewal alerts | MISSING | PHASE_2_CANDIDATE | fleet/notification | None | Dashboard has no supplied alerts | None | None | Scheduler/query and notification policy | Medium | Couple to an approved notification slice. |
| DOC-005 | Lease documents and contract expiry | PARTIAL | PHASE_2_CANDIDATE | fleet | Generic document type can store references | Generic document display | V3 generic document table | Generic document tests | Rental-specific mandatory/expiry semantics | Medium | Define rental contract rules before implementation. |
| ALLOC-001 | Structured vehicle eligibility | IMPLEMENTED | PHASE_1_COMPLETE | fleet | `VehicleAvailabilityUseCase/Service`, structured reasons | Assignment drawer renders backend reasons | Vehicle/document tables | Every rejection reason is unit-tested | None material | Low | Preserve backend authority. |
| ALLOC-002 | Vehicle allocation and overbooking prevention | IMPLEMENTED | PHASE_1_COMPLETE | trip/fleet/system | Transactional assignment, overlap lookup, locks and audit | Trip assignment drawer | V6/V10 history/indexes | Service/controller/concurrent H2 tests | None for functional MVP | Medium | Add PostgreSQL concurrency automation. |
| ALLOC-003 | Reservation calendar | PARTIAL | PHASE_2_CANDIDATE | trip | Allocated periods are queryable only through trip records | No calendar UI | Trip period/index | Overlap tests only | Calendar projection/API/UI | Low | Add only with approved planner workflow. |
| ALLOC-004 | Priority allocation, approval and replacement suggestions | MISSING | REQUIRES_PRODUCT_DECISION | trip/fleet | Trip priority exists but is not allocation policy | Priority shown on trips | V1 priority field | None | Ranking, approval and suggestion rules | Medium | Product must define deterministic policy. |
| ALLOC-005 | Allocation edge cases and production concurrency | PARTIAL | PHASE_1_COMPLETE | trip/fleet | Assignment-affecting edits blocked; pessimistic locks | Conflict feedback | V10 indexes, no exclusion constraint | H2 concurrent vehicle/driver tests | PostgreSQL automated proof and DB-level overlap invariant | High | Add PostgreSQL integration/concurrency gate. |
| RUN-001 | Append-only vehicle running logs | MISSING | REQUIRES_PRODUCT_DECISION | fleet | Snapshot fields only | None | No running-log table | None | Timestamped odometer/engine-hour provenance | Medium | Resolve whether accepted Phase 1 limitation or next fleet slice. |
| RUN-002 | Odometer/engine-hour capture and validation | PARTIAL | PHASE_2_CANDIDATE | fleet/trip/fuel | Trip start/end and Fuel Issue readings validate nonnegative values | Trip/fuel forms | Scalar fields in V1/V11 | Lifecycle and fuel-policy tests | Cross-record monotonic history/tamper rules | High for analytics | Use one fleet-owned reading boundary before US-33. |
| RUN-003 | Trip-wise usage, lubricant and idle records | MISSING | FUTURE_ROADMAP | fleet/fuel | None beyond fuel issues | None | None | None | Logs, linkage and calculation rules | Low now | Defer pending source definitions. |
| MAINT-001 | Maintenance blocks availability | PARTIAL | PHASE_1_COMPLETE | fleet | `VehicleAvailabilityService` blocks maintenance-like status strings | Reason displayed | No maintenance table | Status rejection tests | Persisted work order/period overlap | Medium | Define minimal maintenance-owned blocking record. |
| MAINT-002 | Breakdown records and service scheduling | MISSING | PHASE_2_CANDIDATE | fleet | None | None | None | None | Work orders, breakdown lifecycle and schedules | Medium | Separate approved maintenance slice. |
| TRIP-001 | Trip order create/edit | IMPLEMENTED | PHASE_1_COMPLETE | trip | `TripUseCase`, `TripService`, `TripLifecyclePolicy`, controller/persistence | `TripEditorPage.tsx` | V1/V10 trip schema/FKs | Lifecycle/service/controller tests; limited create test depth | No material basic-order gap | Low | Preserve; add server pagination separately. |
| TRIP-002 | Bulk, template and recurring trip creation | DEFERRED | FUTURE_ROADMAP | trip | None | None | None | None | Complete feature set | Low | Await approved story. |
| TRIP-003 | Priority and customer instructions | IMPLEMENTED | PHASE_1_COMPLETE | trip | Fields persisted and validated for required priority | Trip form/details/list | V1 trip fields | Frontend trip tests | No advanced SLA semantics | Low | Preserve basic behavior. |
| TRIP-004 | Submit/approve/reject/resubmit authorization | IMPLEMENTED | PHASE_1_COMPLETE | trip/identity | `TripLifecyclePolicy`, transactional `TripService.transition`, permissions | `LifecycleActions.tsx` | V6 history; V9 permissions | Domain/service/controller/security/persistence tests | Multi-level approval intentionally excluded | Low | Preserve. |
| TRIP-005 | Multi-level approval/escalation matrix | DEFERRED | FUTURE_ROADMAP | future workflow | None | None | None | None | Workflow/rule engine | Low | Require explicit governance story. |
| TRIP-006 | Driver and vehicle assignment | IMPLEMENTED | PHASE_1_COMPLETE | trip/fleet/system | Eligibility ports, transactional assignment, overlap locks and history | Assignment drawers | V6/V7/V10 | Service/controller/concurrency tests | None material | Low | Preserve. |
| TRIP-007 | Route assignment | IMPLEMENTED | PHASE_1_COMPLETE | trip/routing | `RouteEligibilityPort`, `RouteEligibilityAdapter`, explicit endpoint/history | Trip details route action | Trip route FK/index | Route-assignment service/UI tests | None material | Low | Preserve. |
| TRIP-008 | Skill, fatigue and substitute assignment | PARTIAL | REQUIRES_PRODUCT_DECISION | fleet/trip | Licence-class matching exists | Licence class supplied in drawer | Driver licence schema | Mixed-licence/assignment tests | Endorsements, fatigue/hours and suggestions | Medium | Define driver-hours source and endorsement semantics. |
| TRIP-009 | Dispatch, start, complete and close | IMPLEMENTED | PHASE_1_COMPLETE | trip | Revalidation, locks, timestamps, odometers, audit and explicit transitions | Lifecycle modals/details | V6/V8 histories | Extensive domain/service/controller/persistence tests | None material | Low | Preserve. |
| TRIP-010 | Operational trip/checkpoint log | MISSING | REQUIRES_PRODUCT_DECISION | trip | Status history is not an operational log | Future section only | None | None | Checkpoints, delays, notes and provenance | Medium | Decide accepted deferral before implementing. |
| TRIP-011 | Exceptions, interruption and recovery | PARTIAL | PHASE_2_CANDIDATE | trip | Reject/cancel with reasons; no `INTERRUPTED` state | Reject/cancel modals | History supports reasons | Lifecycle tests | Interruption, resume, no-show and breakdown recovery | Medium | Define exception lifecycle as its own story. |
| TRIP-012 | Offline trip execution/synchronization | DEFERRED | FUTURE_ROADMAP | future mobile/sync | None | None | None | None | Idempotency, queue/conflict model | High future | Defer until mobile architecture is approved. |
| TRIP-013 | Completion reports and customer acknowledgment | MISSING | REQUIRES_PRODUCT_DECISION | trip/reporting | Completion fields exist; no report/ack aggregate | Details only | Trip fields only | Lifecycle tests | Report projection, fuel/incident summary and acknowledgment | Medium | Define reporting/ack contract. |
| ROUTE-001 | Basic route definition, distance and duration | IMPLEMENTED | PHASE_1_COMPLETE | routing | `Route`, use case/service/controller/persistence | Route resource page | V1 route; V10 FKs | Domain/service/controller/repository tests | None material | Low | Preserve. |
| ROUTE-002 | Ordered multi-stop routes | IMPLEMENTED | PHASE_1_COMPLETE | routing | Ordered `stopLocationIds` | Route editor/list | V5 `route_stop` with order | Route tests | None material for MVP | Low | Preserve. |
| ROUTE-003 | Route restrictions and time windows | PARTIAL | PHASE_2_CANDIDATE | routing | Endpoint/type compatibility only | No constraint editor | None | Route assignment tests | Vehicle, regional, hazmat and time-window constraints | Medium | Require a typed constraints contract. |
| ROUTE-004 | Dynamic/alternate route planning | DEFERRED | FUTURE_ROADMAP | routing | None | None | None | None | Planning/revision model | Low | Defer. |
| ROUTE-005 | Optimization, traffic and route analytics | DEFERRED | FUTURE_ROADMAP | routing/reporting/tracking | None | None | None | None | Data sources and algorithms | Low | Depend on tracking and reporting foundations. |
| DRIVER-001 | Basic driver profile/status | PARTIAL | PHASE_1_COMPLETE | fleet | `Driver`, CRUD use case/service/controller/persistence | Driver resource page | V1 driver | No focused profile CRUD tests | Typed status, richer identity/employment and normalized conflicts | Medium | Harden separately. |
| DRIVER-002 | Driver licences/classes/expiry/suspension-like state | IMPLEMENTED | PHASE_1_COMPLETE | fleet | `DriverLicense` lifecycle and persistence | Compliance detail | V4 licence table/indexes | Domain/service/controller/repository tests | Explicit suspended/revoked enum not modeled, but inactive blocks use | Low | Preserve unless regulatory vocabulary is approved. |
| DRIVER-003 | Driver availability and coherent multi-licence eligibility | IMPLEMENTED | PHASE_1_COMPLETE | fleet/trip | `DriverAvailabilityService` qualifies one coherent licence and checks overlap | Assignment reasons | V4 and trip assignment index | Mixed-licence matrix plus assignment tests | Endorsements/fatigue excluded | Low | Preserve. |
| DRIVER-004 | Medical fitness/testing | DEFERRED | FUTURE_ROADMAP | future driver | None | None | None | None | Entire regulated workflow | Low now | Require jurisdictional rules. |
| DRIVER-005 | Performance, violations, payroll and incentives | DEFERRED | FUTURE_ROADMAP | future driver/reporting | None | None | None | None | Entire feature family | Low now | Defer. |
| FUEL-001 | US-31 Fuel Issue lifecycle | IMPLEMENTED | PHASE_2_ACTIVE | fuel | `FuelIssue`, policy/service, controller, ports/adapters/events | Fuel issue list/editor/details | V11 issue/history | Domain/service/controller/repository and UI tests | None material in US-31 scope | Low | Preserve. |
| FUEL-002 | Fuel stations and issue limits | IMPLEMENTED | PHASE_2_ACTIVE | fuel | `FuelStation`, `FuelLimitPolicy`, services/repos | Station selection in Fuel Issue | V11 station/limit | Fuel Issue tests | No stock ledger by design | Low | Preserve. |
| FUEL-003 | US-32 Fuel Purchase lifecycle/reconciliation | IMPLEMENTED | PHASE_2_ACTIVE | fuel | `FuelPurchase`, policy/service, controller, histories/events | Purchase list/editor/details | V12 purchase/history/sequence | Domain/service/controller/repository/UI/security tests | No accounting/stock movement by scope | Low | Commit and review the current working-tree slice. |
| FUEL-004 | Vendor and fuel price catalogue | IMPLEMENTED | PHASE_2_ACTIVE | organization/fuel | `VendorLookup`, vendor stack, `FuelPriceService` | Fuel Price page | V12 vendor/price | Purchase policy/repository/UI tests | Organization vendor UI is coupled to price permission intentionally | Low | Preserve module boundary. |
| FUEL-005 | Bunker/tank stock ledger | DEFERRED | FUTURE_ROADMAP | future fuel | None | None | None | None | Stock inward/outward, balances and dip readings | High future | Do not infer from FuelStation; define ledger first. |
| FUEL-006 | US-33 mileage/KM enhancement | DEFERRED | PHASE_2_CANDIDATE | fleet/fuel/trip | Odometer snapshots are prerequisite only | Existing trip/fuel reading fields | No reading ledger | Existing validation only | Authoritative reading ownership, monotonicity and correction model | High | Exact next candidate only after contract approval. |
| FUEL-007 | Cost per trip, cards, analytics and fraud | DEFERRED | FUTURE_ROADMAP | fuel/reporting | None beyond issue/purchase facts | None | None | None | Projections, cards, tolerances and investigation workflows | Low now | Sequence after mileage and stock foundations. |
| FREIGHT-001 | Freight orders, cargo items and manifests | DEFERRED | FUTURE_ROADMAP | future freight | None | None | None | None | Entire bounded context | Low now | Require authoritative story IDs. |
| FREIGHT-002 | Load planning, weight/volume and hazmat constraints | DEFERRED | FUTURE_ROADMAP | future freight | None | None | None | None | Entire capability | Low now | Depend on freight order foundation. |
| FREIGHT-003 | Cargo insurance and claims | DEFERRED | FUTURE_ROADMAP | future freight | None | None | None | None | Entire capability | Low now | Defer. |
| DELIVERY-001 | Delivery orders and proof of delivery | DEFERRED | FUTURE_ROADMAP | future delivery | None | None | None | None | Entire bounded context | Low now | Depend on freight assignment. |
| DELIVERY-002 | Failed delivery/redelivery | DEFERRED | FUTURE_ROADMAP | future delivery | None | None | None | None | Entire workflow | Low now | Defer. |
| DELIVERY-003 | Last-mile zones, riders, batching and ETA | DEFERRED | FUTURE_ROADMAP | future delivery/tracking | None | None | None | None | Entire capability family | Low now | Defer. |
| GPS-001 | Device binding, position ingestion and live map | DEFERRED | FUTURE_ROADMAP | future tracking | None | None | None | None | Entire tracking foundation | High future | Define authenticated ingestion/retention first. |
| GPS-002 | Geofencing, speed, idle and deviation alerts | DEFERRED | FUTURE_ROADMAP | future tracking | None | None | None | None | Position-derived rules | Low now | Depend on GPS-001. |
| GPS-003 | Replay, forensic history and tamper/spoof detection | DEFERRED | FUTURE_ROADMAP | future tracking | None | None | None | None | Retention and detection policy | Low now | Depend on stable telemetry. |
| OFFLINE-001 | Offline capture, queues and conflict recovery | DEFERRED | FUTURE_ROADMAP | future sync/mobile | None | None | None | None | Idempotent command and synchronization architecture | High future | Product/architecture decision required. |
| SECURITY-001 | JWT login and disabled-user prevention | IMPLEMENTED | PHASE_1_COMPLETE | identity | `IdentityService`, BCrypt, JWT filter/service | Login/protected routing | V1/V2 identity tables | Identity/security tests | None material | Low | Preserve. |
| SECURITY-002 | Refresh rotation, expiry, revocation and logout | IMPLEMENTED | PHASE_1_COMPLETE | identity | Refresh store/service endpoints | Axios refresh coordination | V2 `refresh_token` | Identity and API-client tests | None material | Low | Preserve. |
| SECURITY-003 | RBAC and operation permissions | IMPLEMENTED | PHASE_1_COMPLETE | identity/all | 49 permission bootstrap; explicit security matchers | Permission-aware navigation/actions | V9/V11/V12 permissions | 401/403/success integration tests | Organization gap tracked separately | Low | Preserve deny-all fallback. |
| SECURITY-004 | Organization business permissions | PARTIAL | REQUIRES_PRODUCT_DECISION | identity/organization | Customer/department/location/project routes are authenticated-only | No organization navigation | V1 masters | No authorization tests | Least-privilege permission set | High | Define and enforce organization permissions before exposing UI broadly. |
| SECURITY-005 | ABAC, SSO, MFA, device auth and segregation of duties | DEFERRED | FUTURE_ROADMAP | identity | None | None | None | None | Entire advanced-security scope | Low now | Do not implement without explicit approval. |
| SECURITY-006 | Browser session/token hardening | PARTIAL | REQUIRES_PRODUCT_DECISION | frontend/identity | Stateless backend is strong | Tokens reside in `localStorage` | Refresh hashes persisted | API-client tests | XSS-resilient storage/CSRF decision | High | Make a deployment/security architecture decision. |
| AUDIT-001 | Trip/fuel lifecycle transaction audit | IMPLEMENTED | PHASE_1_COMPLETE | trip/fuel | Append-only histories, dispatch record and immutable fuel events | Timelines | V6–V8, V11–V12 | Lifecycle/history tests | None material | Low | Preserve. |
| AUDIT-002 | Master-data audit/change history | PARTIAL | REQUIRES_PRODUCT_DECISION | fleet/organization/identity | Compliance records have metadata; most masters do not | Current-state views | Mixed audit columns | Sparse master tests | Actor/time/version history for master changes | Medium | Define retention/audit policy. |
| AUDIT-003 | User activity, regulatory reports and retention policy | DEFERRED | FUTURE_ROADMAP | identity/reporting | Correlation IDs only | None | None | None | Central activity/audit reporting | Medium future | Require compliance requirements. |
| REPORT-001 | Operational trip/driver/vehicle reports | MISSING | REQUIRES_PRODUCT_DECISION | reporting | `ReportingController` returns empty collections | No report pages | No projections | No reporting tests | Queries, ports, DTOs and pagination | Medium | Implement only after metric/report definitions are approved. |
| REPORT-002 | Scheduled/ad-hoc/fuel/freight analytics | DEFERRED | FUTURE_ROADMAP | reporting | None | None | None | None | Entire analytics platform | Low now | Depend on reliable source facts. |
| MASTER-001 | Customer, department, location, project masters | PARTIAL | PHASE_1_COMPLETE | organization | Domain/use cases/services/controllers/persistence exist | Reference lookups in operational forms; no dedicated nav | V1/V10 | No organization tests | Explicit permissions, fuller CRUD consistency and audit | High | Security and test hardening before feature expansion. |
| MASTER-002 | Vendor master | IMPLEMENTED | PHASE_2_ACTIVE | organization | `Vendor`, `VendorUseCase`, `VendorLookup` and persistence | Used by Fuel Price/Purchase UI | V12 vendor | Indirect purchase/security/runtime smoke evidence | No dedicated vendor UI test suite | Low | Preserve public lookup boundary. |
| MASTER-003 | Company/branch/depot/product masters, calendars and workflow engine | DEFERRED | FUTURE_ROADMAP | future master/workflow | None | None | None | None | Entire capability family | Low now | Require product decomposition. |
| NOTIFY-001 | Email/SMS/push/in-app/webhook notifications and escalation | DEFERRED | FUTURE_ROADMAP | future notification | None | None | None | None | Module, providers, templates and policy | Low now | Do not create a notification module without an approved story. |
| DOCUMENT-001 | Generic upload, OCR, versioning and retention | DEFERRED | FUTURE_ROADMAP | future document | Current records hold file reference strings only | No uploader | No blob/document-version tables | None | Storage/security/retention architecture | Medium future | Require document-management decision. |
| INTEGRATION-001 | ERP/accounting/CRM/HRMS/payment/telematics/API gateway | OUT_OF_SCOPE | OUT_OF_SCOPE | external | None | None | None | None | All external contracts | Low now | Keep outside current repository scope. |
| MOBILE-001 | Driver/dispatcher/delivery mobile apps | DEFERRED | FUTURE_ROADMAP | future mobile | None | None | None | None | Applications and offline architecture | Low now | Depend on approved mobile/offline program. |
| RESILIENCE-001 | Replication/message-queue/integration failure recovery | OUT_OF_SCOPE | OUT_OF_SCOPE | platform | No message queue or replication owned here | None | None | None | Platform architecture | Low now | Handle in deployment/platform program. |
| RESILIENCE-002 | Duplicate/orphan/data-integrity edge cases | PARTIAL | REQUIRES_PRODUCT_DECISION | all | Unique constraints, validation, locks and conflict errors exist | API error feedback | Strong core FKs/uniques; no universal idempotency | Concurrency/constraint tests mainly H2 | Idempotency keys, orphan reconciliation and PostgreSQL automation | High | Establish production DB/idempotency test strategy. |

## 10. Architectural findings

1. **Reporting is a contract stub, not a hexagonal module.** `ReportingController` directly constructs placeholder maps/lists and has no input port, service, output port or adapter.
2. **Core cross-module boundaries are otherwise clean.** No production Trip/Fuel code imports another module's JPA repository or persistence entity.
3. **Dead/duplicate dispatch wiring remains.** `VehicleDispatchEligibility` and `VehicleEligibilityPort.assertEligibleForDispatch` are wired, but production dispatch calls assignment eligibility and performs Trip-owned overlap checks. `FleetController.AvailabilityResponse` is unused.
4. **Test-only no-op route eligibility constructors are public production paths.** Two `TripService` constructors silently install `noOpRouteEligibility`; Spring uses the full constructor, but future manual wiring could bypass route validation.
5. **Several master domains remain anemic.** Vehicle, Driver and organization records rely heavily on controller validation/database constraints rather than domain policies.
6. **Status strings are duplicated.** Trip has a centralized policy, but Vehicle/Driver operational states and multiple UI/backend labels remain free-form strings.
7. No production `TODO`/`FIXME` markers were found.

## 11. Cross-module coupling findings

- Approved coupling is through public root interfaces and adapters, and Spring Modulith verification passes.
- `system` is used as a composition module for allocation overlap adapters; this is acceptable but should remain orchestration-only.
- Fuel's Organization, Identity, Fleet and Trip dependencies request narrow projections rather than persistence objects.
- Reporting must not solve its gap by importing all module JPA repositories. It should consume read ports/events or reporting-owned projections.
- Organization reference checks for trip/route largely rely on database FKs rather than a consistent public lookup interface.

## 12. Database findings

- Latest schema: V12, validated on H2 during `mvn clean verify` and applied successfully to the running PostgreSQL 16 Compose database.
- JPA reports 25 repositories; `ddl-auto=validate` is used for PostgreSQL.
- High risk: automated concurrency proof is H2-only despite production reliance on pessimistic locks.
- Medium risk: no overlap exclusion constraint, no optimistic versions, and free-form operational statuses.
- Medium risk: no persistence for reporting, maintenance, running logs or operational trip logs.
- Low/medium risk: V12 price overlap and case-insensitive invoice behavior are application-level conventions rather than identical database invariants.

## 13. Security findings

- 401, 403 and permitted-success tests exist for critical trip/fleet/route/report/fuel mutations.
- Authorization rules precede controller mutation and deny unmatched business routes.
- Organization CRUD is the main least-privilege hole: authentication is sufficient for customer, department, location and project mutations.
- Local bootstrap is opt-in and secret-driven; the Docker profile is deliberately local, not production provisioning.
- `localStorage` token persistence and public Swagger require explicit deployment acceptance.

## 14. Frontend findings

- Ant Design is the sole principal component system; no MUI use was found.
- Permission-aware navigation and action visibility align with backend permissions for implemented modules.
- Trip and Fuel workflows use backend decisions for eligibility/lifecycle; React does not recreate those rules.
- Fuel Purchase uses server pagination. Trip and generic master lists cannot because their backend contracts return unpaged collections.
- Dashboard truthfully displays absent metrics, but there is no reporting UI.
- Error handling is strongest in trip/fuel pages. Generic master forms have less comprehensive backend field-error/test coverage.
- Production JavaScript is about 1.67 MB (514.77 kB gzip), producing Vite's chunk-size warning.

## 15. Test coverage findings

Audit verification on 2026-08-16:

- Backend `mvn clean verify`: **173 tests passed**, zero failures/errors/skips.
- `ApplicationModulesTest`: two tests passed; no module-boundary violations.
- `ContextSmokeTest`: Spring context and Flyway V1–V12 passed.
- Frontend lint: passed.
- Frontend Vitest: **49 tests in 9 files passed**.
- Frontend production build: passed with only the chunk-size advisory.

Strong coverage: identity/security; documents; licences; coherent driver eligibility; vehicle availability; route domain/persistence; trip lifecycle, route/resource assignment, dispatch, audit and H2 concurrency; US-31; US-32; frontend shell, permissions and operational flows.

Weak/missing coverage: organization; basic vehicle/driver/category/type CRUD; reporting; maintenance; running/trip logs; PostgreSQL Testcontainers/concurrency; direct vendor CRUD; advanced frontend error cases.

The host runs Java 26.0.1 while compilation targets release 21. ArchUnit logs `Unsupported class file major version 70` fallback warnings; Modulith verification still passed. Release evidence should be reproduced on Java 21.

## 16. OpenAPI and contract findings

- Generated Springdoc was reachable and exposed 74 paths, including 7 Fuel Issue path groups, 8 Fuel Purchase path groups and 3 report paths.
- `docs/openapi-contract-inventory.md` is an inventory, not a maintained source OpenAPI specification. No authoritative YAML contract is present.
- Runtime request records prevent arbitrary lifecycle status mutation and use the shared error handler.
- Report endpoints appear in OpenAPI despite returning placeholders; path presence is not implementation proof.
- Organization permission behavior is not evident from generated schemas and must remain documented separately.

## 17. Documentation drift

1. `docs/mvp-gap-analysis.md` is stale by design: it audits commit `eb334da`, 100 tests and pre-hardening lifecycle/security. It must not be used as current status.
2. `README.md` says the current branch contains US-31 but omits US-32/V12, even though US-32 is present in the working tree.
3. `README.md` says H2 sample bootstraps are H2-only, while the administrator bootstrap also supports the explicit Docker profile; later text clarifies this.
4. `docs/local-development.md` still says current migrations are V1–V11 and uses `DEV_IDENTITY_*` examples, whereas Spring properties map through `APP_DEV_IDENTITY_BOOTSTRAP_*` in Compose. It also omits US-32/V12.
5. `docs/phase-1-release-readiness.md` and release checklist state PostgreSQL/Docker is unavailable and the release is not ready. A healthy PostgreSQL Compose stack and a V1–V12 US-32 smoke run now exist, but Phase-1-only tagging and Java 21/PostgreSQL concurrency evidence remain separate gates.
6. `AGENTS.md` has stale module names and `/api/v1` base path.
7. `docs/phase-2-backlog.md` contains duplicate/contradictory ordering: a legacy unmapped fuel-master row precedes implemented US-31/US-32 and later rows use unapproved story mapping.

## 18. Duplicate, dead and placeholder code

- Placeholder: all four reporting endpoints.
- Dead/duplicate: `VehicleDispatchEligibility`, the dispatch method on `VehicleEligibilityPort`/adapter, and unused `FleetController.AvailabilityResponse`.
- Risky fallback: no-op route eligibility constructors in `TripService`.
- Intentional duplication: availability read paths and locked assignment checks serve different query/command concerns but should stay documented.
- No explicit production TODO/FIXME markers were found.

## 19. Risk assessment

| Priority | Risk | Consequence |
|---|---|---|
| P0 | US-32 exists only in the dirty working tree | Build/runtime evidence cannot be reproduced from commit `dccfbd5` alone. |
| P0 | Organization mutations are authenticated-only | Any signed-in user can mutate shared customer/location/project data. |
| P0 | No automated PostgreSQL concurrency suite | Production lock/overlap semantics differ from the H2 evidence. |
| P1 | No authoritative odometer/running-log ledger | US-33, cost-per-trip and tamper analysis lack trustworthy source facts. |
| P1 | Reporting endpoints are placeholders | Dashboard/report permissions protect data that does not yet exist. |
| P1 | No maintenance aggregate | Availability depends on manually maintained status strings. |
| P2 | Unpaged trip/master APIs | Performance and frontend scalability degrade with data volume. |
| P2 | Browser tokens in localStorage | XSS impact includes token theft. |
| P3 | Large frontend bundle | Slower initial load; not a correctness blocker. |

## 20. Dependency-ordered prioritized backlog

1. **P0 — Curate and review US-32 as a reproducible change set.** Commit only the intended purchase/vendor/frontend/migration/docs changes after reviewing the pre-existing dirty tree. This is release hygiene, not a new feature.
2. **P0 — Add organization business authorization.** Define minimum view/create/update/status permissions for customers, departments, locations and projects; add 401/403/success tests.
3. **P0 — Automate PostgreSQL migration and concurrency verification.** Run V1–V12, JPA validation, vehicle/driver/lifecycle races and fuel uniqueness against disposable PostgreSQL.
4. **P1 — Approve the US-33 contract before coding.** Decide authoritative odometer ownership, reading source, monotonic/correction rules, trip/fuel linkage, units and audit behavior.
5. **P1 — Implement an append-only fleet reading/running-log foundation only if approved by US-33.** Expose a narrow module boundary to Trip/Fuel; do not duplicate odometer truth.
6. **P1 — Define reporting metrics and replace placeholders.** Use read-only ports/projections and add backend/frontend tests.
7. **P1 — Define minimal maintenance availability records.** Add scheduled/active maintenance overlap only after ownership and status rules are approved.
8. **P2 — Resolve trip operational-log/exception scope.** Decide whether accepted Phase 1 limitations remain deferred.
9. **P2 — Harden and paginate master/trip APIs.** Add consistent conflicts, reference-safe deactivation and focused tests.
10. **HOLD — Freight, delivery, GPS, offline/mobile, notifications, ABAC/SSO/MFA and external integrations.** Do not begin without authoritative stories and architecture decisions.

## 21. Recommended next implementation slice

The exact next candidate is **US-33 — Mileage & KM Tracking contract and append-only vehicle-reading foundation**, but implementation must not start until the story contract answers:

- which module owns authoritative odometer/engine-hour readings;
- accepted sources (trip start/end, fuel issue, manual, telematics later);
- monotonicity, correction and duplicate/idempotency rules;
- unit/precision and timestamp requirements;
- how historical readings affect current vehicle odometer;
- required permissions, APIs, events and audit retention.

Likely modules/files after approval—not changed by this audit—are Fleet domain/application/persistence/controller, Trip/Fuel public adapters, a new forward-only Flyway migration, permission configuration, React Fuel/Fleet pages and corresponding domain/application/persistence/security/frontend tests. US-34 and later work must remain deferred.

## Final requested summary

1. **Overall implemented capability percentage:** 27.4% of the full 84-capability catalogue; 43.4% of non-deferred/non-out-of-scope capabilities.
2. **Phase 1 completion assessment:** Core operational MVP is implemented and green, with formally visible limitations in reporting, logs, maintenance persistence, organization authorization and PostgreSQL release evidence.
3. **Phase 2 completion assessment:** US-31 and US-32 are implemented in the current working tree; later stories are not implemented.
4. **Implemented capability count:** 23.
5. **Partial capability count:** 20.
6. **Missing active/product-decision capability count:** 10.
7. **Deferred capability count:** 29.
8. **Out-of-scope/product-decision count:** 2 out of scope; 13 implemented/partial/missing rows require a product decision before expansion.
9. **Highest-risk architectural issue:** reporting has no hexagonal application/data implementation, while US-33 lacks an authoritative reading owner.
10. **Highest-risk data-integrity issue:** production overlap/lifecycle locking has no automated PostgreSQL concurrency proof or database exclusion constraint.
11. **Highest-risk security issue:** organization master mutations require authentication but no explicit business permission.
12. **Highest-risk frontend/backend contract issue:** dashboard/report UI contracts exceed the placeholder data returned by reporting; trip/master pagination is also absent server-side.
13. **Documentation accuracy assessment:** mixed; architecture and current Phase 1 behavior are mostly accurate, but migration count, US-32 presence, old audit/release state, module names and environment-variable examples are stale.
14. **Test coverage assessment:** strong for identity, compliance, eligibility, routing, trip lifecycle/assignment/dispatch and fuel; weak for organization/master CRUD/reporting and PostgreSQL-specific behavior.
15. **Exact recommended next implementation slice:** approve the US-33 Mileage & KM Tracking contract, then implement one append-only Fleet-owned reading foundation—do not start coding before contract approval.
16. **Likely files/modules for that slice:** Fleet reading domain/use case/repository/service/controller/persistence/config; Trip/Fuel public adapters; one new migration; permission/security mappings; focused React page/hooks; domain/application/persistence/security/frontend tests.
