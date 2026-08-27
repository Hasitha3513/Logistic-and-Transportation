# Authoritative MVP Story Traceability Matrix (US-01 through US-87)

**Audit Date:** August 27, 2026  
**Auditor:** Senior Principal Enterprise Architect & QA Lead  
**Baseline Commit:** `ebe722f4f276db1b60a204e74f2368aafb011c85` (`feat/load-plan-structural-readiness`)  
**Scope:** Authoritative US-01 through US-87 Requirements Traceability

---

## 1. MVP 1.0 — Core Release (34 Stories)

| Story | Feature Title | Requirement Summary | Backend | DB | API | Frontend | RBAC | Unit/Int Tests | E2E Tests | Status | Gap / Evidence | Next Action |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **US-01** | Manage Vehicle Master | Vehicle CRUD, specs, status lifecycle, fuel type, capacity | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `Vehicle`, `FleetController`, `V1`/`V10`, `VehicleListPage`, `vehicles.spec.ts` | None |
| **US-02** | Manage Fleet Categories | Hierarchy of categories, capacity classes, types | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `VehicleCategory`, `VehicleType`, `V1`/`V10`, `ResourceListPage`, `vehicles.spec.ts` | None |
| **US-03** | Manage Vehicle Documents | Compliance docs, expiry, renewal, dispatch blocking | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `VehicleDocument`, `V3`, `vehicleDocs.spec.ts` | None |
| **US-04** | Allocate Vehicles | Availability lookup, overlap prevention, row locks | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `VehicleAvailabilityService`, `V6`/`V10`, `AssignmentDrawers`, `tripAssignments.spec.ts` | None |
| **US-05** | Maintain Fuel & Lubricant Logs | Lubricant entry, consumption, vehicle log integration | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `LubricantLog`, `V23`, `VehicleLubricantSection`, `lubricants.spec.ts` | None |
| **US-06** | Maintain Running Logs | Append-only odometer/engine-hour readings, resets | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `VehicleReading`, `V14-V16`, `VehicleReadingsSection`, `runningLogs.spec.ts` | None |
| **US-07** | Link Maintenance to Availability | Maintenance schedules block vehicle allocation | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `MaintenanceSchedule`, `V19`, `VehicleMaintenanceSection`, `maintenance.spec.ts` | None |
| **US-08** | Fleet Allocation Edge Cases | Dynamic deallocation, concurrency conflicts | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `VehicleAvailabilityService`, `tripAssignments.spec.ts` | None |
| **US-09** | Create Trip Orders | Origin/dest, times, cargo/passenger specs | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `Trip`, `V1`/`V8`/`V10`, `TripListPage`, `TripEditorPage`, `tripCreation.spec.ts` | None |
| **US-10** | Assign Driver and Vehicle | Resource assignment with eligibility checks | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `TripService.assign*`, `V6`/`V7`, `AssignmentDrawers`, `tripAssignments.spec.ts` | None |
| **US-11** | Assign Route | Route binding to trip order with validation | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `TripService.assignRoute`, `V1`/`V8`, `tripAssignments.spec.ts` | None |
| **US-12** | Start and End Trip | Trip start/end with odometer capture | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `TripService.startTrip/completeTrip`, `LifecycleActions`, `tripLifecycle.spec.ts` | None |
| **US-13** | Maintain Trip Log | Checkpoints, delays, incidents, status history | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `TripStatusHistory`, `TripOperationalEvent`, `V8`/`V24`, `tripHistory.spec.ts` | None |
| **US-14** | Complete Trip | Final completion, mileage reconciliations, close | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `TripService.completeTrip/closeTrip`, `tripLifecycle.spec.ts` | None |
| **US-15** | Handle Trip Exceptions | Cancellation and rejection with mandatory reasons | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `TripService.cancelTrip/rejectTrip`, `tripLifecycle.spec.ts` | None |
| **US-16** | Authorize Trip | Submit and approval workflow transitions | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `TripService.submitTrip/approveTrip`, `tripLifecycle.spec.ts` | None |
| **US-17** | Define Routes | Route definition, start/end points, active status | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `Route`, `V1`/`V10`, `routes.spec.ts` | None |
| **US-18** | Calculate Distance and ETA | Distance (km) and estimated travel duration | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `Route`, `V1`/`V5`, `routes.spec.ts` | None |
| **US-19** | Plan Multi-Stop Routes | Ordered intermediate stops with sequence numbers | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `RouteStop`, `V5`, `routes.spec.ts` | None |
| **US-39** | Manage Driver Profiles | Driver master CRUD, contact info, status | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `Driver`, `V1`/`V10`, `drivers.spec.ts` | None |
| **US-40** | Manage Driver Licensing | Multi-license classes, expiry date verification | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `DriverLicense`, `V4`/`V10`, `drivers.spec.ts` | None |
| **US-41** | Assess Driver Performance | Safety score, incident metrics, performance tab | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `DriverPerformance`, `V21`, `DriverPerformanceSection`, `performance.spec.ts` | None |
| **US-42** | Manage Violations | Traffic/safety violations, demerit points, blocking | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `DriverViolation`, `V21`, `DriverViolationsSection`, `violations.spec.ts` | None |
| **US-43** | Manage Medical Fitness | Medical fitness certs, validity checks, blocking | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `DriverMedicalRecord`, `V22`, `DriverMedicalSection`, `medical.spec.ts` | None |
| **US-44** | Manage Drug Tests | Drug/alcohol tests, positive result suspension | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `DriverDrugTest`, `V22`, `DriverDrugTestSection`, `drugTests.spec.ts` | None |
| **US-45** | Handle Driver Exceptions | Driver leave/suspension periods blocking allocation | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `DriverException`, `V20`, `DriverExceptionSection`, `exceptions.spec.ts` | None |
| **US-71** | Offline Synchronization | Offline queue, idempotency, sha256, sync outcomes | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `offlinesync` module, `V29`, `OfflineSyncCenter`, 15 offline Playwright tests | None |
| **US-74** | Security | JWT, BCrypt, RBAC, 70+ granular business permissions | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `identity` module, `V2`/`V9`/`V13`+, `auth.spec.ts`, `rbac.spec.ts` | None |
| **US-75** | Audit and Reports | Append-only histories, operational dashboard queries | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `reporting` module, audit tables, `DashboardPage`, `reporting.spec.ts` | None |
| **US-77** | Notification Rules | Dynamic rule engine, templates, email/in-app channels | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `notification` module, `V25-V28`, `NotificationRulesPage`, 4 spec suites | None |
| **US-79** | Master Data | Customers, departments, projects, locations, vendors | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `organization` module, `V1`/`V12`/`V13`, `app.smoke.spec.ts` | None |
| **US-80** | Workflows | Multi-state transactional lifecycles across domains | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | Lifecycle engines in Trip, Fuel, Freight, Insurance | None |
| **US-81** | Scheduling | Background compliance/maintenance/expiry scanners | YES | YES | N/A | N/A | N/A | YES | N/A | **COMPLETE** | Schedulers and scanner tests | None |
| **US-83** | Documents | Compliance documents and file reference metadata | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | Document repositories across Fleet, Driver, Insurance | None |

---

## 2. MVP 1.1 — Expanded Route & Freight (11 Stories)

| Story | Feature Title | Requirement Summary | Backend | DB | API | Frontend | RBAC | Unit/Int Tests | E2E Tests | Status | Gap / Evidence | Next Action |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **US-20** | Optimize Routes | Heuristic multi-stop route optimization and preview | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `RouteOptimizationService`, `V30`, `RouteOptimizerModal`, `routeIntelligence.spec.ts` | None |
| **US-21** | Route History | Immutable versioning and revision history snapshots | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `RouteRevision`, `V30`, `RouteRevisionSection`, `routeIntelligence.spec.ts` | None |
| **US-22** | Route Performance | Planned vs actual distance/duration variance metrics | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `RoutePerformance`, `V30`, `RoutePerformanceSection`, `routeIntelligence.spec.ts` | None |
| **US-23** | Route Disruptions | Real-time road/weather disruption alerts & detours | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `RouteDisruption`, `V30`, `RouteDisruptionsSection`, `routeIntelligence.spec.ts` | None |
| **US-24** | Freight Orders | Freight order lifecycle, customer, origin/destination | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `FreightOrder`, `V31`, `FreightOrderListPage`, `freightOrders.spec.ts` | None |
| **US-25** | Cargo Manifest | Manifest items, structured tri-state fragile/temperature | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `CargoManifest`, `V32`/`V37`, `CargoManifestEditorPage`, `cargoManifests.spec.ts` | None |
| **US-26** | Load Planning | Structural readiness, fragile/temp rules, free-text non-authority, optimistic concurrency | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `LoadPlan`, `V34`/`V38`, `LoadPlanDetailsPage`, `loadPlans.spec.ts` (8 cross-browser scenarios) | Formal acceptance decision in P2-LOAD-ACCEPTANCE-001 |
| **US-27** | Weight & Volume | Total cargo weight/volume vs vehicle payload/volume/GVW limits | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `WeightVolumeCalculationEngine`, `V39`, `LoadPlanDetailsPage`, `weightVolumeValidation.spec.ts` | None |
| **US-28** | Freight Insurance | Policies, claims, multi-tranche settlements, disputes | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `InsurancePolicy`, `InsuranceClaim`, `V36`, `PolicyListPage`, `freightInsurance.spec.ts` | None |
| **US-29** | Freight Reports | Tenant-scoped freight and load analytics | NO | NO | NO | NO | NO | NO | NO | **BLOCKED** | Blocked by paused tenant foundation | Do not implement while tenant work is paused |
| **US-30** | Cargo Exceptions | Cargo damage/shortage/hazardous/seal/unmanifested exception workflow | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `CargoException`, `V40`/`V41`, `CargoExceptionListPage`, `CargoExceptionDetailsPage`, `cargoExceptions.spec.ts` (8 scenarios) | None |

---

## 3. MVP 1.2 — Full Fuel Expansion (8 Stories)

| Story | Feature Title | Requirement Summary | Backend | DB | API | Frontend | RBAC | Unit/Int Tests | E2E Tests | Status | Gap / Evidence | Next Action |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **US-31** | Issue Fuel | Fuel voucher workflow, vehicle limit check, bunker debit | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `FuelIssue`, `V11`, `FuelIssueListPage`, `fuelIssue.spec.ts` | None |
| **US-32** | Fuel Purchases | Vendor catalogues, receiving, physical variance, bunker credit | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `FuelPurchase`, `V12`, `FuelPurchaseListPage`, `fuelPurchase.spec.ts` | None |
| **US-33** | Mileage Tracking | Authoritative monotonic VehicleReading ledger | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `VehicleReading`, `V14-V16`, `VehicleReadingsSection`, `runningLogs.spec.ts` | None |
| **US-34** | Fuel Cost Allocation | Trip fuel cost snapshotting, consumption metrics | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `TripFuelCostService`, `V17`, `TripFuelCostSection`, `tripFuelCost.spec.ts` | None |
| **US-35** | Fuel Cards | Card provider integration, card statement reconciliation | NO | NO | NO | NO | NO | NO | NO | **DEFERRED** | Post-MVP scope | Post-MVP roadmap |
| **US-36** | Fuel Bunkers | Internal bunker storage, book stock balance, dip variance, transfers, row locks | YES | YES | YES | YES | YES | YES | YES | **COMPLETE** | `BunkerTank`, `V18`, `BunkerTankListPage`, `bunkerManagement.spec.ts` | None |
| **US-37** | Fuel Analytics | Predictive fuel models and theft anomaly detection | NO | NO | NO | NO | NO | NO | NO | **DEFERRED** | Post-MVP scope | Post-MVP roadmap |
| **US-38** | Fuel Exceptions | Unauthorized off-network fill-up investigations | NO | NO | NO | NO | NO | NO | NO | **DEFERRED** | Post-MVP scope | Post-MVP roadmap |

---

## 4. Post-MVP & Product Roadmap (US-46 through US-70, US-72, US-73, US-76, US-78, US-82, US-84-87)

| Story Range | Feature Area | Description | Status |
|---|---|---|:---:|
| **US-46** | Driver Payroll | Driver compensation, trip allowance calculations, overtime | **DEFERRED** |
| **US-47** | Transport Billing | Invoicing, rate cards, customer billing schedules | **DEFERRED** |
| **US-48 through US-55** | GPS / Tracking | IoT telematics ingestion, live tracking map, geofencing, route replay | **DEFERRED** |
| **US-56 through US-62** | Delivery Operations | Delivery orders, electronic proof of delivery (ePOD), sign-on-glass | **DEFERRED** |
| **US-63 through US-70** | Last Mile Delivery | Dynamic dispatching, customer tracking portal, SMS alerts | **DEFERRED** |
| **US-72, US-73, US-76, US-78, US-82, US-84-87** | Advanced Platform | Multi-tenant SaaS billing, SSO/SAML, custom reports builder, external ERP sync | **DEFERRED** |
