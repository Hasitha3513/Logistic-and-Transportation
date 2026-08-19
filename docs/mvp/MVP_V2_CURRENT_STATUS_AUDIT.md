# MVP v2 Current Implementation Audit

**Audit Task:** MVP-STATUS-AUDIT-007 (Updated post MVP-GAP-005)  
**Audit Date:** August 19, 2026  
**Auditor:** Senior Solution Architect, Technical Lead, and QA Lead  
**Repository Branch:** `feature/mvp-gap-005`  
**Repository Working Tree:** Modified (Driver Medical Fitness & Drug Screening Implemented)  
**Backend Baseline:** Java 21 / Spring Boot 3.2.12 / Spring Modulith 1.2.12 / PostgreSQL & H2 (316 unit/arch tests passing)  
**Frontend Baseline:** React 19 / TypeScript 5.8 / Vite 7 / Ant Design 5.27 (77 unit tests passing, build clean)  
**Playwright E2E Baseline:** Complete (32 tests / 26 specs passing 100% on Chromium; multi-browser smoke passing across Chromium, Firefox, WebKit)  

---

## 1. Executive Summary

A comprehensive, evidence-based code and architectural audit of the **Transport & Logistics Management System** was conducted against the **Corrected MVP Baseline v2 (39 User Stories)** following the completion of **MVP-GAP-005** (US-43 Driver Medical Fitness and US-44 Driver Drug Tests).

Every story was inspected across backend domain aggregates, application use cases, ports, persistence entities, database migrations (V1 through V22), Spring Security RBAC rules (71 permissions), REST controllers, React views, Vitest suites, and Playwright E2E automation.

### 1.1 Story Classification Breakdown

| Classification | Story Count | Percentage of MVP Baseline |
|---|---:|---:|
| **COMPLETE** | **33** | **84.6%** |
| **PARTIAL** | **4** | **10.3%** |
| **NOT IMPLEMENTED** | **2** | **5.1%** |
| **BLOCKED** | **0** | **0.0%** |
| **NEEDS VERIFICATION** | **0** | **0.0%** |
| **TOTAL MVP STORIES** | **39** | **100.0%** |

### 1.2 Quantitative Metrics

1. **Verified Completion Percentage:**
   $$\text{Verified Completion} = \frac{33 \text{ COMPLETE}}{39 \text{ Stories}} \times 100\% = \mathbf{84.6\%}$$

2. **Functional Coverage Percentage:**
   $$\text{Functional Coverage} = \frac{33 + (0.5 \times 4)}{39} \times 100\% = \frac{35.0}{39} \times 100\% = \mathbf{89.7\%}$$

*(Note: Post-MVP Phase 2 stories US-20–23, US-35, US-37, US-38, and US-46 are tracked separately in Section 6 and strictly excluded from MVP metrics).*

---

## 2. Module Summary

| Module | Stories | Complete | Partial | Missing | Blocked | Verify | Coverage |
|---|---:|---:|---:|---:|---:|---:|---:|
| **Fleet Management** | 8 | 7 | 1 | 0 | 0 | 0 | **93.8%** |
| **Trip Management** | 8 | 7 | 1 | 0 | 0 | 0 | **93.8%** |
| **Basic Route Management** | 3 | 3 | 0 | 0 | 0 | 0 | **100.0%** |
| **Basic Fuel Management** | 5 | 5 | 0 | 0 | 0 | 0 | **100.0%** |
| **Driver Management** | 7 | 7 | 0 | 0 | 0 | 0 | **100.0%** |
| **Cross-Cutting / Supporting** | 8 | 4 | 2 | 2 | 0 | 0 | **62.5%** |
| **TOTAL** | **39** | **33** | **4** | **2** | **0** | **0** | **89.7%** |

---

## 3. Story-by-Story Audit

| ID | User Story | Status | Backend Evidence | Frontend Evidence | Missing Acceptance Criteria | Tests | Priority |
|---|---|:---:|---|---|---|---|:---:|
| **US-01** | Manage Vehicle Master | **COMPLETE** | `Vehicle.java`, `VehicleService.java`, `FleetController.java`, `V1__baseline.sql` | `ResourceListPage.tsx`, `ResourceEditorModal.tsx`, `fleet/types.ts` | None | `FleetControllerTest`, `VehicleServiceTest` (12 tests) | Baseline |
| **US-02** | Manage Fleet Categories | **COMPLETE** | `VehicleCategory.java`, `VehicleType.java`, `VehicleCategoryService.java`, `FleetController.java` | `ResourceListPage.tsx`, `ResourceEditorModal.tsx` | None | `VehicleCategoryServiceTest`, `VehicleTypeServiceTest` (8 tests) | Baseline |
| **US-03** | Manage Vehicle Documents | **COMPLETE** | `VehicleDocument.java`, `DocumentType`, `VehicleDocumentService.java`, `V3__vehicle_documents.sql` | `ResourceListPage.tsx`, Document drawer | Automated renewal alert notifications, document version history table | `VehicleDocumentTest`, `VehicleDocumentServiceTest` (8 tests) | Baseline |
| **US-04** | Allocate Vehicles | **COMPLETE** | `VehicleAvailabilityService.java`, `VehicleAllocationLookup.java`, `TripVehicleAllocationAdapter.java`, `V6` | `AssignmentDrawers.tsx` | Priority allocation ranking algorithm, replacement vehicle auto-suggestion | `VehicleAvailabilityServiceTest`, `ConcurrentVehicleAssignmentIntegrationTest` (12 tests) | Baseline |
| **US-05** | Maintain Fuel & Lubricant Logs | **PARTIAL** | `FuelIssue.java`, `FuelIssueService.java`, `VehicleReading.java`, `V11`, `V14` | `FuelIssueListPage.tsx`, `FuelIssueEditorPage.tsx`, `VehicleReadingsSection.tsx` | Lubricant, fluid, and engine oil consumption logging | `FuelIssueServiceTest`, `FuelIssueBunkerIntegrationTest` (26 tests) | P2 |
| **US-06** | Maintain Running Logs | **COMPLETE** | `VehicleReading.java`, `VehicleMeterReset.java`, `VehicleReadingService.java`, `VehicleReadingController.java`, `V14`, `V16` | `VehicleReadingsSection.tsx`, `fleet/useVehicleReadings.ts` | Idle time breakdown tracking | `VehicleReadingServiceTest`, `VehicleReadingSecurityIntegrationTest` (24 tests) | Baseline |
| **US-07** | Link Maintenance to Availability | **COMPLETE** | `MaintenanceSchedule.java`, `MaintenanceScheduleService.java`, `VehicleAvailabilityService.java`, `FleetController.java`, `V19__maintenance_schedules.sql` | `VehicleMaintenanceSection.tsx`, `useVehicleMaintenance.ts`, `ResourceListPage.tsx` | None | `MaintenanceScheduleTest`, `MaintenanceScheduleServiceTest`, `VehicleAvailabilityServiceTest`, `FleetControllerMaintenanceTest`, `FleetMaintenanceSecurityIntegrationTest`, `MaintenanceSchedulePersistenceIntegrationTest`, `TripVehicleMaintenanceAssignmentIntegrationTest` (41 tests) | Baseline |
| **US-08** | Handle Fleet Allocation Edge Cases | **COMPLETE** | `VehicleAvailabilityService.java`, `DriverAvailabilityService.java`, pessimistic row locking (`findByIdForUpdate`), half-open interval overlap query | `AssignmentDrawers.tsx` | None | `ConcurrentVehicleAssignmentIntegrationTest`, `ConcurrentDriverAssignmentIntegrationTest` (8 tests) | Baseline |
| **US-09** | Create Trip Orders | **COMPLETE** | `Trip.java`, `TripService.java`, `TripController.java`, `TripLifecyclePolicy.java`, `V1` | `TripListPage.tsx`, `TripEditorPage.tsx` | Bulk trip order creation, recurring trip templates | `TripServiceTest`, `TripControllerTest` (14 tests) | Baseline |
| **US-10** | Assign Driver and Vehicle | **COMPLETE** | `TripService.assignVehicle/Driver`, `DriverEligibilityAdapter.java`, `VehicleEligibilityAdapter.java`, `V6`, `V7` | `AssignmentDrawers.tsx` | Driver fatigue validation (driving hours counter), automatic substitute recommendation | `ConcurrentVehicleAssignmentIntegrationTest`, `ConcurrentDriverAssignmentIntegrationTest` (16 tests) | Baseline |
| **US-11** | Assign Route | **COMPLETE** | `TripService.assignRoute`, `RouteEligibilityAdapter.java`, `TripController.java`, `V8` | `AssignmentDrawers.tsx`, `TripDetailsPage.tsx` | Dynamic on-the-fly route geometry creation, automated alternate route generator | `TripLifecycleIntegrationTest`, `TripDetailsPage.test.tsx` (10 tests) | Baseline |
| **US-12** | Start and End Trip | **COMPLETE** | `TripService.startTrip`, `TripService.completeTrip`, `TripFleetVehicleReadingAdapter.java`, synchronous `VehicleReading` capture | `LifecycleActions.tsx` | Offline start/end transactional queue, GPS telemetry loss fallback | `TripLifecycleIntegrationTest`, `LifecycleActions.test.tsx` (14 tests) | Baseline |
| **US-13** | Maintain Trip Log | **PARTIAL** | `TripHistoryEntry.java`, `trip_history` table (`V6`, `V7`, `V8`), `VehicleReading.java` | `HistoryTimeline.tsx`, `TripDetailsPage.tsx` | En-route intermediate checkpoints, mid-trip delay reason logging | `TripLifecycleIntegrationTest` (8 tests) | P2 |
| **US-14** | Complete Trip | **COMPLETE** | `TripService.completeTrip`, `TripService.closeTrip`, invariant checks (`endOdometer >= startOdometer`), remarks | `LifecycleActions.tsx` | Actual vs planned duration/fuel variance report, customer sign-off signature | `TripLifecycleIntegrationTest`, `LifecycleActions.test.tsx` (12 tests) | Baseline |
| **US-15** | Handle Trip Exceptions | **COMPLETE** | `TripService.rejectTrip`, `TripService.cancelTrip`, mandatory cancellation reason validation, conflict release | `LifecycleActions.tsx` | Driver no-show event aggregate, breakdown incident recovery workflow | `TripLifecycleIntegrationTest`, `LifecycleActions.test.tsx` (12 tests) | Baseline |
| **US-16** | Authorize Trip | **COMPLETE** | `TripService.submitTrip`, `TripService.approveTrip`, `TripService.rejectTrip`, `V9__mvp_business_permissions.sql` | `LifecycleActions.tsx` | None | `TripLifecycleIntegrationTest`, `BusinessAuthorizationIntegrationTest` (10 tests) | Baseline |
| **US-17** | Define Routes | **COMPLETE** | `Route.java`, `RouteService.java`, `RouteController.java`, `RouteEntity.java`, `V1` | `ResourceListPage.tsx`, `ResourceEditorModal.tsx` | None | `RouteTest`, `RouteServiceTest`, `RouteControllerTest` (10 tests) | Baseline |
| **US-18** | Calculate Distance and ETA | **COMPLETE** | `Route.distanceKm`, `Route.estimatedDurationMinutes`, route persistence, trip distance recovery in fuel costing | `ResourceListPage.tsx` | Real-time traffic intelligence (advanced post-MVP) | `RouteTest`, `RouteServiceTest` (6 tests) | Baseline |
| **US-19** | Plan Multi-Stop Routes | **COMPLETE** | `Route.stopLocationIds`, `RouteEntity` `@OrderColumn(name = "stop_index")`, `route_stops` table (`V5`) | `ResourceEditorModal.tsx` | Dynamic waypoint re-ordering optimization | `RouteTest`, `RouteJpaRepositoryTest` (6 tests) | Baseline |
| **US-31** | Issue Fuel | **COMPLETE** | `FuelIssue.java`, `FuelLimitPolicy.java`, `FuelStation.java`, `FuelIssueService.java`, `FuelController.java`, `V11` | `FuelIssueListPage.tsx`, `FuelIssueDetailsPage.tsx`, `FuelIssueEditorPage.tsx` | None | `FuelIssueServiceTest`, `FuelIssueBunkerIntegrationTest`, `FuelIssuePages.test.tsx` (26 tests) | Baseline |
| **US-32** | Manage Fuel Purchases | **COMPLETE** | `Vendor.java`, `FuelPrice.java`, `FuelPurchase.java`, `FuelPurchaseService.java`, `FuelPurchaseController.java`, `V12` | `FuelPurchaseListPage.tsx`, `FuelPurchaseDetailsPage.tsx`, `FuelPurchaseEditorPage.tsx` | None | `FuelPurchaseServiceTest`, `FuelPurchasePages.test.tsx` (28 tests) | Baseline |
| **US-33** | Track Mileage | **COMPLETE** | `VehicleReading.java`, `VehicleMeterReset.java`, `VehicleReadingService.java`, `VehicleReadingController.java`, `V14`, `V16` | `VehicleReadingsSection.tsx` | None | `VehicleReadingServiceTest`, `VehicleReadingSecurityIntegrationTest`, `VehicleReadingsSection.test.tsx` (24 tests) | Baseline |
| **US-34** | Allocate Fuel Cost | **COMPLETE** | `TripFuelCostService.java`, `TripFuelCostController.java`, `FleetFuelTripDistanceAdapter.java`, `TripFuelCost.java`, `V17` | `TripFuelCostSection.tsx` | None | `TripFuelCostServiceTest`, `TripFuelCostSection.test.tsx` (18 tests) | Baseline |
| **US-36** | Manage Fuel Bunkers | **COMPLETE** | `BunkerTank.java`, `BunkerStockMovement.java`, `DipReading.java`, `BunkerTankService.java`, `BunkerTankController.java`, `V18` | `BunkerTankListPage.tsx`, `BunkerTankDetailsPage.tsx` | None | `BunkerTankServiceTest`, `BunkerTankApiIntegrationTest`, `BunkerPostgresConcurrencyIntegrationTest`, `BunkerTankPages.test.tsx` (36 tests) | Baseline |
| **US-39** | Manage Driver Profiles | **COMPLETE** | `Driver.java`, `DriverService.java`, `FleetController.java`, `V1__baseline.sql` | `ResourceListPage.tsx`, `ResourceEditorModal.tsx` | None | `DriverServiceTest`, `FleetControllerTest` (10 tests) | Baseline |
| **US-40** | Manage Driver Licensing | **COMPLETE** | `DriverLicense.java`, `LicenseClass.java`, `DriverLicenseService.java`, `FleetController.java`, `V4__driver_licenses.sql` | `ResourceListPage.tsx`, `AssignmentDrawers.tsx` | None | `DriverLicenseTest`, `DriverLicenseServiceTest`, `DriverLicenseRepositoryTest` (10 tests) | Baseline |
| **US-41** | Assess Driver Performance | **COMPLETE** | `DriverPerformanceService.java`, `DriverPerformanceSummary.java`, `FleetController.java`, `DriverPerformanceMetrics.java` | `DriverPerformanceSection.tsx`, `useDriverPerformance.ts` | None | `DriverPerformanceServiceTest`, `DriverPerformanceSection.test.tsx`, `performance.spec.ts` (14 tests) | Baseline |
| **US-42** | Manage Violations | **COMPLETE** | `DriverViolation.java`, `DriverViolationService.java`, `FleetController.java`, `V21__driver_violations.sql` | `DriverViolationsSection.tsx`, `useDriverViolations.ts` | None | `DriverViolationTest`, `DriverViolationServiceTest`, `DriverViolationPersistenceIntegrationTest`, `violations.spec.ts` (22 tests) | Baseline |
| **US-43** | Manage Driver Medical Fitness | **COMPLETE** | `DriverMedicalRecord.java`, `DriverMedicalStatus.java`, `VisionTestStatus.java`, `DriverMedicalRecordService.java`, `DriverAvailabilityService.java`, `FleetController.java`, `V22__driver_medical_and_drug_tests.sql` | `DriverMedicalSection.tsx`, `useDriverMedicalRecords.ts`, `ResourceListPage.tsx` | None | `DriverMedicalRecordTest`, `DriverMedicalRecordServiceTest`, `DriverAvailabilityServiceTest`, `FleetControllerMedicalTest`, `DriverMedicalPersistenceIntegrationTest`, `TripDriverMedicalAssignmentIntegrationTest`, `DriverMedicalSection.test.tsx`, `medical.spec.ts` (18 tests) | P1 |
| **US-44** | Manage Drug Tests | **COMPLETE** | `DriverDrugTest.java`, `DrugTestType.java`, `DrugTestResult.java`, `DrugTestStatus.java`, `DriverDrugTestService.java`, `DriverAvailabilityService.java`, `FleetController.java`, `V22__driver_medical_and_drug_tests.sql` | `DriverDrugTestSection.tsx`, `useDriverDrugTests.ts`, `ResourceListPage.tsx` | None | `DriverDrugTestTest`, `DriverDrugTestServiceTest`, `DriverAvailabilityServiceTest`, `FleetControllerDrugTestTest`, `DriverDrugTestPersistenceIntegrationTest`, `TripDriverMedicalAssignmentIntegrationTest`, `DriverDrugTestSection.test.tsx`, `drugTests.spec.ts` (19 tests) | P1 |
| **US-45** | Handle Driver Exceptions | **COMPLETE** | `DriverException.java`, `DriverExceptionService.java`, `FleetController.java`, `DriverAvailabilityService.java`, `V20__driver_exceptions.sql` | `DriverExceptionSection.tsx`, `useDriverExceptions.ts`, `ResourceListPage.tsx` | None | `DriverExceptionTest`, `DriverExceptionServiceTest`, `DriverAvailabilityServiceTest`, `FleetControllerDriverExceptionTest`, `FleetDriverExceptionSecurityIntegrationTest`, `DriverExceptionPersistenceIntegrationTest`, `TripDriverExceptionAssignmentIntegrationTest`, `DriverExceptionSection.test.tsx` (49 tests) | Baseline |
| **US-71** | Support Offline Data Sync | **NOT IMPLEMENTED** | None | None | Offline transaction queue, store-and-forward sync, conflict resolver | None (0 tests) | P2 / Phase 3 |
| **US-74** | Manage Security | **COMPLETE** | `IdentityService.java`, `JwtAccessTokenService.java`, `BCryptPasswordHasher.java`, `IssuedRefreshToken.java`, `SecurityConfig.java`, 66 permissions, `V2`, `V9`, `V13`, `V15`, `V17` | `LoginPage.tsx`, `AuthContext.tsx` | Advanced enterprise MFA / SSO / ABAC (post-MVP) | `IdentityServiceTest`, `IdentityControllerTest`, `BusinessAuthorizationIntegrationTest` (26 tests) | Baseline |
| **US-75** | Maintain Audit and Reports | **COMPLETE** | Audit history: `trip_history`, `fuel_issue_history`, `fuel_purchase_history`, `bunker_stock_movements`, `vehicle_readings` (COMPLETE). Operational Reporting: `TripReportService.java`, `DriverAssignmentService.java`, `VehicleUtilizationService.java`, `TripReportingAdapter.java`, `FleetReportingAdapter.java`, `ReportingController.java` (COMPLETE via public read ports). | `DashboardPage.tsx` | None | `ReportingControllerTest`, `ReportingSecurityIntegrationTest`, `TripReportServiceTest`, `DriverAssignmentServiceTest`, `VehicleUtilizationServiceTest`, `DashboardPage.test.tsx` (16 tests) | Baseline |
| **US-77** | Manage Notification Rules | **NOT IMPLEMENTED** | None | None | Notification rule configuration, email/SMS/push delivery, alert suppression | None (0 tests) | P2 |
| **US-79** | Manage Master Data | **COMPLETE** | `Customer`, `Department`, `Project`, `Location`, `Vendor`, `FuelStation`, `FuelLimitPolicy`, `OrganizationService`, `OrganizationController`, `V1`, `V11`, `V12` | `ResourceListPage.tsx`, `ResourceEditorModal.tsx` | None | `OrganizationControllerTest`, `OrganizationServiceTest` (14 tests) | Baseline |
| **US-80** | Configure Workflows | **PARTIAL** | Hardcoded strict domain state machines for Trip (10 states), Fuel Issue (5 states), Fuel Purchase (6 states) | `LifecycleActions.tsx`, `FuelIssueDetailsPage.tsx` | Dynamic user-defined workflow transition editor | `TripLifecycleIntegrationTest` (8 tests) | P2 |
| **US-81** | Manage Scheduling | **PARTIAL** | Trip time windows, calendar overlap query in `TripJpaRepository`, availability checks | `AssignmentDrawers.tsx` | Resource calendar entities, holiday calendars, shifts, automated dispatch optimizer | `VehicleAvailabilityServiceTest` (4 tests) | P2 |
| **US-83** | Manage Documents | **COMPLETE** | `VehicleDocument.java`, `DriverLicense.java`, document type enums, validity dates, file references, allocation validation, `V3`, `V4` | `ResourceListPage.tsx` | OCR scanning (explicitly non-MVP) | `VehicleDocumentTest`, `DriverLicenseTest` (18 tests) | Baseline |

---

## 4. Detailed Gap Analysis

### US-05 — Maintain Fuel & Lubricant Logs
- **Current implementation:** Fuel vouchers, fuel quantities, internal/external stations, odometer readings, and unit prices are fully tracked via US-31 (`FuelIssue`) and US-33 (`VehicleReading`).
- **Implemented acceptance criteria:**
  - Fuel issuance logging per vehicle with approval workflow.
  - Odometer reading captured at time of fuel issuance.
  - Fuel vendor and station mapping.
- **Missing acceptance criteria:**
  - Lubricant / engine oil / fluid consumption logging.
- **Backend gaps:** Add `LubricantLog` aggregate in `fleet` or `fuel` module if non-fuel fluid tracking is required.
- **Frontend gaps:** Add lubricant entry form to vehicle running logs.
- **Database gaps:** Table `lubricant_logs` (not created).
- **Testing gaps:** Lubricant logging domain and controller tests.
- **Dependencies:** `Vehicle` aggregate.
- **Recommended next action:** Implement `LubricantLog` aggregate in `fleet` module as a P2 enhancement.

---

### US-07 — Link Maintenance to Availability
- **Current implementation:** Preventive maintenance scheduling and availability blackout reservation engine fully implemented.
  - Domain aggregate: `MaintenanceSchedule` with statuses (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`) and date invariants (`scheduledEnd > scheduledStart`).
  - Application service: `MaintenanceScheduleService` managing lifecycle, conflict validation, reschedule, and cancellation.
  - Availability integration: `VehicleAvailabilityService.evaluate()` checks active schedules (`SCHEDULED`, `IN_PROGRESS`) using half-open interval semantics `[scheduledStart, scheduledEnd)`. Overlapping allocation attempts are rejected with reason code `MAINTENANCE_BLOCKED`.
  - Persistence: Flyway migration `V19__maintenance_schedules.sql`, entity `MaintenanceScheduleEntity`, JPQL interval query.
  - Web & Security: REST endpoints under `/api/v1/vehicles/{vehicleId}/maintenance-schedules` guarded with `VEHICLE_VIEW` and `VEHICLE_MAINTENANCE_MANAGE`.
  - Frontend: `VehicleMaintenanceSection.tsx` embedded in Vehicle Details drawer with schedule listing, creation modal, rescheduling, cancel, and complete actions.
- **Implemented acceptance criteria:**
  - Preventive maintenance scheduling with half-open window `[scheduledStart, scheduledEnd)`.
  - Rejection of allocation requests when vehicle is currently in maintenance status or has an overlapping maintenance schedule.
  - Structured rejection reason code `MAINTENANCE_BLOCKED`.
  - Rescheduling, cancellation with remarks, and completion with remarks.
  - Non-blocking behavior for completed or cancelled maintenance windows.
- **Missing acceptance criteria:** None (fully satisfied).
- **Backend gaps:** None.
- **Frontend gaps:** None.
- **Database gaps:** None (`V19__maintenance_schedules.sql` applied).
- **Testing gaps:** None (41 tests covering domain invariants, service transitions, interval overlap edge cases, controller security, persistence queries, and trip assignment rejection).
- **Dependencies:** `Vehicle` aggregate.
- **Status:** **COMPLETE** (Resolved under task `MVP-GAP-002`).

---

### US-13 — Maintain Trip Log
- **Current implementation:** Complete append-only timeline of all trip state transitions, assignments, cancellations, odometers, timestamps, and actors in `trip_history` table (`TripHistoryEntry.java`).
- **Implemented acceptance criteria:**
  - Status transition chronology.
  - Resource assignment and re-assignment audit trail.
  - Start and end odometer and timestamp capture.
- **Missing acceptance criteria:**
  - En-route intermediate checkpoints and mid-trip delay/incident logging.
- **Backend gaps:** Create `TripCheckpoint` domain model and endpoint `POST /trips/{id}/checkpoints`.
- **Frontend gaps:** Checkpoint recording button in `TripDetailsPage.tsx`.
- **Database gaps:** Table `trip_checkpoints` (not created).
- **Testing gaps:** Checkpoint recording and chronology tests.
- **Dependencies:** `Trip` aggregate.
- **Recommended next action:** Add `TripCheckpoint` entity as a P2 enhancement.

---

### US-41 — Assess Driver Performance
- **Current implementation:** Not implemented.
- **Implemented acceptance criteria:** None.
- **Missing acceptance criteria:** Driver performance scorecard, safety ratings, fuel efficiency scores, on-time delivery ratings.
- **Backend gaps:** Create `DriverPerformance` aggregate in `fleet` or `driver` module.
- **Frontend gaps:** Driver scorecard tab in driver profile screen.
- **Database gaps:** Table `driver_performance_records`.
- **Testing gaps:** Performance score calculation tests.
- **Dependencies:** `Driver`, `Trip`, `VehicleReading`.
- **Recommended next action:** Implement driver scorecard aggregate as a P1 feature.

---

### US-42 — Manage Violations
- **Current implementation:** Not implemented.
- **Implemented acceptance criteria:** None.
- **Missing acceptance criteria:** Traffic violation recording, penalty points, fine amounts, fine payment tracking, repeat-offender flags.
- **Backend gaps:** Create `DriverViolation` aggregate with `fineAmount`, `paymentStatus`, `points` in `fleet` module.
- **Frontend gaps:** Violations list and recording drawer in driver profile.
- **Database gaps:** Table `driver_violations`.
- **Testing gaps:** Violation recording and fine payment tests.
- **Dependencies:** `Driver` aggregate.
- **Recommended next action:** Implement `DriverViolation` aggregate as a P1 feature.

---

### US-43 — Manage Driver Medical Fitness
- **Current implementation:** Complete dedicated aggregate `DriverMedicalRecord` with validity date windows, fitness status (`FIT`, `FIT_WITH_RESTRICTIONS`, `TEMPORARILY_UNFIT`, `UNFIT`), vision test results, and examiner/certificate tracking. Integrated with `DriverAvailabilityService` to block assignment when expired or unfit.
- **Implemented acceptance criteria:**
  - Record medical fitness assessments with examiner, clinic, certificate reference, and notes.
  - Vision test evaluation (`PASSED`, `PASSED_WITH_CORRECTIVE_LENSES`, `FAILED`, `NOT_TESTED`).
  - Strict date validity checks `[validFrom, validUntil]`.
  - Seamless trip allocation integration rejecting unfit or expired drivers with `MEDICALLY_UNFIT` or `MEDICAL_FITNESS_EXPIRED`.
- **Backend gaps:** None.
- **Frontend gaps:** None (`DriverMedicalSection.tsx` integrated in driver details drawer).
- **Database gaps:** None (`driver_medical_record` table in `V22__driver_medical_and_drug_tests.sql`).
- **Testing gaps:** None (covered by unit, persistence, assignment integration, and Playwright `E2E-DRV-007`).
- **Dependencies:** `Driver`, `DriverAvailabilityService`.
- **Status:** **COMPLETE** (Resolved under task `MVP-GAP-005`).

---

### US-44 — Manage Drug Tests
- **Current implementation:** Complete dedicated aggregate `DriverDrugTest` managing test types (`RANDOM`, `SCHEDULED`, `PRE_EMPLOYMENT`, `POST_INCIDENT`, `REASONABLE_SUSPICION`, `RETURN_TO_DUTY`), sample collection, lab results (`NEGATIVE`, `POSITIVE`, `INCONCLUSIVE`), and return-to-duty clearance workflows. Integrated with `DriverAvailabilityService`.
- **Implemented acceptance criteria:**
  - Schedule random/mandatory drug screenings with lab and chain of custody tracking.
  - Record test results and automatically flag positive results for return-to-duty blocks.
  - Return-to-duty clearance action recording SAP rehabilitation notes and unblocking driver.
  - Seamless trip allocation integration rejecting failed drug test drivers with `RETURN_TO_DUTY_CLEARANCE_REQUIRED` or `DRUG_TEST_FAILED`.
- **Backend gaps:** None.
- **Frontend gaps:** None (`DriverDrugTestSection.tsx` integrated in driver details drawer with scheduling, result recording, and RTD clearance modals).
- **Database gaps:** None (`driver_drug_test` table in `V22__driver_medical_and_drug_tests.sql`).
- **Testing gaps:** None (covered by unit, persistence, assignment integration, and Playwright `E2E-DRV-008`).
- **Dependencies:** `Driver`, `DriverAvailabilityService`.
- **Status:** **COMPLETE** (Resolved under task `MVP-GAP-005`).

---

### US-45 — Handle Driver Exceptions
- **Current implementation:** Fully implemented and integrated. `DriverException` domain model, application service `DriverExceptionService`, persistence adapter, and web controller endpoints under `/api/v1/drivers/{driverId}/exceptions`. `DriverAvailabilityService` verifies driver operational status, licence validity, overlapping active trip assignments, and blocking driver exceptions (`SCHEDULED`, `ACTIVE`) with rejection reason `DRIVER_EXCEPTION_BLOCKED`.
- **Implemented acceptance criteria:**
  - Block allocation when driver is suspended, on leave, or inactive.
  - Block allocation when driver lacks valid licence class or licence is expired.
  - CRUD and state transition machine for driver exceptions (`SCHEDULED`, `ACTIVE`, `COMPLETED`, `CANCELLED`).
  - Strict bidirectional conflict validation: Rule A (existing exception blocks overlapping trip assignment), Rule B (active trip blocks new overlapping exception), Rule C (active trip blocks rescheduling into trip window).
  - Half-open interval semantics $[startTime, endTime)$.
  - Frontend management interface in driver drawer (`DriverExceptionSection.tsx`).
- **Missing acceptance criteria:** None for MVP scope (medical records and drug testing are tracked in US-43/US-44).
- **Backend gaps:** None.
- **Frontend gaps:** None.
- **Database gaps:** None (`driver_exception` table created in `V20__driver_exceptions.sql`).
- **Testing gaps:** None (49 automated unit and integration tests passing).
- **Dependencies:** `DriverAvailabilityService`, `TripDriverAssignmentAdapter`.
- **Recommended next action:** Completed in MVP-GAP-003.

---

### US-71 — Support Offline Data Synchronization
- **Current implementation:** Not implemented.
- **Implemented acceptance criteria:** None (web-first architecture).
- **Missing acceptance criteria:** Offline transaction queue, client-side store-and-forward, transactional sync conflict resolution.
- **Backend gaps:** Idempotent sync ingest endpoints (`/api/v1/sync/trips`, `/api/v1/sync/readings`) with optimistic revision tracking.
- **Frontend gaps:** Service worker / IndexedDB local offline storage queue.
- **Database gaps:** Synchronization sequence tables.
- **Testing gaps:** Offline replay and conflict resolution tests.
- **Dependencies:** Mobile client app architecture.
- **Recommended next action:** Defer to Phase 3 Mobile Architecture.

---

### US-75 — Maintain Audit and Reports
- **Current implementation:**
  - **Audit:** Fully implemented via immutable append-only transaction tables (`trip_history`, `fuel_issue_history`, `fuel_purchase_history`, `bunker_stock_movements`, `vehicle_readings`).
  - **Operational Reporting:** Completed via `TripReportService`, `DriverAssignmentService`, and `VehicleUtilizationService` in `reporting` module, consuming public read contracts `TripReportingQuery` and `FleetReportingQuery`. Endpoints `/api/v1/reports/trips`, `/api/v1/reports/driver-assignments`, and `/api/v1/reports/vehicle-utilization` return typed DTOs (`PageResponse<TripReportResponse>`, `List<DriverAssignmentResponse>`, `List<VehicleUtilizationResponse>`).
  - **Modulith Boundary Resolution:** Fixed by deleting obsolete direct dependency `TripReportRepository` $\rightarrow$ `TripEntity` and establishing decoupled outbound read ports (`TripReportReadPort`, `FleetReportReadPort`).
- **Implemented acceptance criteria:**
  - Immutable audit trail capturing timestamp, actor, entity id, and action across all state transitions.
  - Operational health endpoint `/dashboard/operations` guarded with `DASHBOARD_VIEW`.
  - Database-driven JPA query projections with date range, status, customer, vehicle, and driver filtering guarded with `REPORT_VIEW`.
  - Vehicle utilization metrics (assigned trips, completed trips, distance travelled from odometers, allocated hours).
- **Missing acceptance criteria:** None (fully satisfied).
- **Backend gaps:** None.
- **Frontend gaps:** Optional downloadable report export viewer (post-MVP enhancement).
- **Database gaps:** None.
- **Testing gaps:** None (covered by `ReportingControllerTest`, `ReportingSecurityIntegrationTest`, `TripReportServiceTest`, `DriverAssignmentServiceTest`, `VehicleUtilizationServiceTest`, `ApplicationModulesTest`).
- **Dependencies:** `TripReportingQuery`, `FleetReportingQuery`.
- **Status:** **COMPLETE** (Resolved under task `MVP-GAP-001-FIX`).

---

### US-77 — Manage Notification Rules
- **Current implementation:** Not implemented.
- **Implemented acceptance criteria:** None.
- **Missing acceptance criteria:** Notification rule definitions, multi-channel alerting (email, SMS, push, in-app), quiet hours, escalation paths.
- **Backend gaps:** Create `NotificationRule` engine, Spring event listeners, and email/webhook dispatch adapters.
- **Frontend gaps:** Notification preferences and in-app notification center.
- **Database gaps:** Table `notification_rules`, `in_app_notifications`.
- **Testing gaps:** Notification dispatching tests.
- **Dependencies:** Spring Application Event infrastructure.
- **Recommended next action:** Implement notification engine as a P2 feature.

---

### US-80 — Configure Workflows
- **Current implementation:** Hardcoded, strict state machines in domain code for Trip (10 states), Fuel Issue (5 states), and Fuel Purchase (6 states) with role-gated transitions and invariants.
- **Implemented acceptance criteria:**
  - Enforce valid state transitions with guard conditions.
  - Role-gated transition execution (`TRIP_APPROVE`, `TRIP_DISPATCH`, etc.).
- **Missing acceptance criteria:**
  - Dynamic, user-defined workflow transition editor and configurable multi-level approval matrices.
- **Backend gaps:** Workflow schema definition engine.
- **Frontend gaps:** Visual workflow builder UI.
- **Dependencies:** None.
- **Recommended next action:** Treat dynamic workflow builder as a P2 / enterprise feature.

---

### US-81 — Manage Scheduling
- **Current implementation:** Trip time-window scheduling (`requestedStartTime`, `requestedEndTime`), half-open calendar overlap queries in `TripJpaRepository`, and conflict-aware availability checks.
- **Implemented acceptance criteria:**
  - Trip departure and arrival scheduling windows.
  - Concurrency-safe overlap conflict detection for vehicles and drivers.
- **Missing acceptance criteria:**
  - Resource calendar entities, organization holiday calendars, driver shift schedules, and automated multi-trip dispatch optimization.
- **Backend gaps:** Create `ShiftSchedule` and `HolidayCalendar` entities in `organization` module.
- **Frontend gaps:** Gantt / calendar resource view.
- **Database gaps:** Tables `shift_schedules`, `holiday_calendars`.
- **Testing gaps:** Shift overlap and holiday conflict validation tests.
- **Dependencies:** `Driver`, `Vehicle`.
- **Recommended next action:** Implement shift and holiday calendars as a P2 feature.

---

## 5. Previously Considered Complete But Now Partial

Comparing the current implementation against the expanded/corrected MVP acceptance scope reveals the following status reclassifications:

1. **US-05 (Maintain Fuel & Lubricant Logs):**
   - *Previous status:* COMPLETE (assumed satisfied by Fuel Issue vouchers).
   - *Current status:* **PARTIAL**
   - *Rationale:* The expanded scope explicitly requires lubricant and engine oil consumption tracking in addition to fuel vouchers. No lubricant aggregate or API currently exists.

2. **US-07 (Link Maintenance to Availability):**
   - *Previous status:* PARTIAL (only evaluated real-time operational status).
   - *Current status:* **COMPLETE** (Resolved under `MVP-GAP-002`).
   - *Rationale:* Implemented preventive maintenance scheduling (`MaintenanceSchedule`), interval overlap validation, half-open availability blocking in `VehicleAvailabilityService`, REST endpoints with `VEHICLE_MAINTENANCE_MANAGE`, and frontend maintenance management UI.

3. **US-13 (Maintain Trip Log):**
   - *Previous status:* COMPLETE (assumed satisfied by `trip_history` audit timeline).
   - *Current status:* **PARTIAL**
   - *Rationale:* The expanded scope requires mid-trip en-route checkpoint recording and operational delay reason logging. The system currently only records formal status transitions and start/end odometers.

4. **US-45 (Handle Driver Exceptions):**
   - *Previous status:* COMPLETE (assumed satisfied by `DriverAvailabilityService` suspension check).
   - *Current status:* **PARTIAL**
   - *Rationale:* The expanded scope requires a formal driver exception/leave management workflow. Currently, suspension is only a static operational status flag on the driver record.

5. **US-75 (Maintain Audit and Reports):**
   - *Previous status:* PARTIAL (audit trail complete, reporting endpoints were stubs with Modulith violation).
   - *Current status:* **COMPLETE** (Resolved under `MVP-GAP-001-FIX`).
   - *Rationale:* Implemented `TripReportingQuery`, `FleetReportingQuery`, application services, typed response DTOs, and full test suite. Modulith boundary violation eliminated and verified by `ApplicationModulesTest`.

6. **US-80 (Configure Workflows):**
   - *Previous status:* COMPLETE (assumed satisfied by 10-state Trip engine).
   - *Current status:* **PARTIAL**
   - *Rationale:* The expanded scope requires configurable approval workflows. Current workflows are strictly governed by hardcoded domain policies.

7. **US-81 (Manage Scheduling):**
   - *Previous status:* COMPLETE (assumed satisfied by trip time windows).
   - *Current status:* **PARTIAL**
   - *Rationale:* The expanded scope requires calendar management, shifts, and driver holiday blackout intervals.

---

## 6. Existing Post-MVP Functionality

The codebase contains substantial, production-ready functionality that extends beyond the 39-story MVP baseline. This code must remain intact and protected:

| Feature / Story | Implementation Evidence | Classification | Notes |
|---|---|:---:|---|
| **US-35 (Fuel Card Integration)** | Placeholder interfaces | `POST_MVP_DEFERRED` | Preserved for Phase 2 |
| **US-37 (Advanced Fuel Analytics)** | `costPerKm`, `litersPer100Km` efficiency metrics in `TripFuelCostService.java` | `EXISTING_POST_MVP` | Fully operational trip-level fuel efficiency metrics |
| **US-38 (Advanced Fuel Anomaly Detection)** | Negative balance rejection, capacity guards, dip reading variance in `BunkerTankService.java` | `EXISTING_POST_MVP` | Operational inventory guards and dip variance tracking |
| **US-20–23 (Advanced Route Management)** | Route stops ordered collection (`V5`) | `EXISTING_POST_MVP` | Route definition and multi-stop support in place |
| **US-46 (Driver Payroll Link)** | None | `POST_MVP_DEFERRED` | Preserved for Phase 3 |

---

## 7. Architecture Findings
 
### Finding 1: Spring Modulith Cross-Module Violation in Reporting [RESOLVED]
- **Location:** `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/persistence/TripReportRepository.java`
- **Previous Issue:** `TripReportRepository` directly extended `JpaRepository<TripEntity, UUID>`, referencing internal class `TripEntity` belonging to `trip.infrastructure`.
- **Resolution (MVP-GAP-001-FIX):** Deleted `TripReportRepository`. Created public root contract `TripReportingQuery` and adapter `TripReportingAdapter` in `trip` module. Reporting module consumes this contract through outbound port `TripReportReadPort` $\rightarrow$ `TripReportReadAdapter`. `ApplicationModulesTest` now passes with 0 violations.
 
### Finding 2: Direct Map Return in Reporting Controller [RESOLVED]
- **Location:** `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/web/controllers/ReportingController.java`
- **Previous Issue:** Controller methods returned raw `Map<String, Object>` and `List<Map<String, Object>>`.
- **Resolution (MVP-GAP-001-FIX):** Implemented `TripReportUseCase`, `DriverAssignmentUseCase`, and `VehicleUtilizationUseCase` with MapStruct web mapper `ReportingWebMapper` and typed DTO records (`PageResponse<TripReportResponse>`, `List<DriverAssignmentResponse>`, `List<VehicleUtilizationResponse>`).
 
### Finding 3: Hexagonal Directionality Compliance [PASS]
- All domain packages across `fleet`, `trip`, `routing`, `fuel`, `identity`, `reporting`, and `organization` are completely free of Spring MVC, JPA, Hibernate, or HTTP dependencies.
- `HexagonalLayerArchitectureTest` passes 100% (7/7 tests).
- `ModuleBoundaryArchitectureTest` passes 100% (3/3 tests).

### Finding 4: Concurrency & Transactional Isolation [PASS]
- Pessimistic write locking (`findByIdForUpdate`) combined with `entityManager.refresh()` is properly implemented across `VehicleAvailabilityService`, `DriverAvailabilityService`, `VehicleReadingService`, `MaintenanceScheduleService`, and `BunkerTankService`.

---

## 8. Test Coverage Findings

### 8.1 Test Execution Summary

| Test Category | Suite / Class | Count | Result |
|---|---|---:|:---:|
| **Backend Unit & Integration Tests** | `mvn test` | 358 tests (21 skipped) | **358 PASSED, 0 FAILURES, 0 ERRORS (100% pass)** |
| **Frontend Unit & Component Tests** | `vitest run` | 71 tests (13 test files) | **71 PASSED (0 failures)** |
| **Frontend Production Build** | `tsc -b && vite build` | 5,073 modules | **BUILD SUCCESS (25.36s)** |

### 8.2 Automated Test Distribution by Module

- **Fleet Management:** 75 tests (CRUD, categories, documents, availability, readings, meter resets, preventive maintenance scheduling, maintenance overlap intervals, security, concurrency).
- **Trip Management:** 77 tests (Order validation, 10-state lifecycle, vehicle/driver assignment, route validation, odometer recording, dispatch, approval, maintenance blackout rejection).
- **Basic Route Management:** 22 tests (Route CRUD, multi-stop ordering, distance and duration validation).
- **Basic Fuel Management:** 90 tests (Fuel issue vouchers, limit policies, purchases, receiving, pricing, trip cost allocation, bunker tank ledger, dip readings, inter-tank transfer, PostgreSQL concurrency).
- **Reporting Module:** 15 tests (Trip reporting, driver assignments, vehicle utilization, security authorization, parameter validation).
- **Driver Management:** 26 tests (Driver profile CRUD, licence class validation, availability evaluation).
- **Identity & Security:** 33 tests (JWT authentication, refresh tokens, BCrypt-12 password hashing, RBAC permission evaluation, maintenance security).
- **Organization Master Data:** 14 tests (Customer, Department, Project, Location, Vendor CRUD and uniqueness).
- **Architecture Verification:** 15 tests (`ApplicationModulesTest`, `HexagonalLayerArchitectureTest`, `ModuleBoundaryArchitectureTest`, `LombokUsageArchitectureTest`).

---

## 9. P0 / P1 / P2 Gap Queue

### Priority P0 — Critical for Operational Correctness & Architecture Cleanliness

*All Priority P0 gaps are currently **RESOLVED** (0 remaining).*

#### GAP-001: Implement Operational Reporting Projections & Fix Modulith Boundary Violation [COMPLETED]
- **Task ID:** `MVP-GAP-001-FIX`
- **User Story:** `US-75` (Maintain Audit and Reports)
- **Status:** **COMPLETED** (Verified by `ApplicationModulesTest` and 15 reporting test cases).
- **Resolution:** Implemented `TripReportingQuery`, `FleetReportingQuery`, `TripReportService`, `DriverAssignmentService`, `VehicleUtilizationService`, and typed REST responses in `ReportingController`. Direct dependency on `TripEntity` deleted.

---

### Priority P1 — Required for Full MVP Acceptance Coverage

#### GAP-002: Preventive Maintenance Scheduling Linkage [COMPLETED]
- **Task ID:** `MVP-GAP-002`
- **User Story:** `US-07` (Link Maintenance to Availability)
- **Status:** **COMPLETED** (Verified by 41 maintenance unit, security, and persistence integration tests).
- **Resolution:** Implemented `MaintenanceSchedule` aggregate, lifecycle state machine, `V19__maintenance_schedules.sql`, half-open interval overlap detection in `VehicleAvailabilityService`, `/vehicles/{id}/maintenance-schedules` REST endpoints, and `VehicleMaintenanceSection.tsx` React component.

#### GAP-003: Driver Leave & Exception Management
- **Task ID:** `MVP-GAP-003`
- **User Story:** `US-45` (Handle Driver Exceptions)
- **Exact Gap:** No formal driver leave or disciplinary exception booking to block driver availability during time-off windows.
- **Backend Work:** Create `DriverException` entity in `fleet` with date ranges; integrate into `DriverAvailabilityService.evaluate()`.
- **Frontend Work:** Driver exception drawer in driver profile.
- **Tests Required:** `DriverExceptionServiceTest`, `DriverAvailabilityIntegrationTest`.
- **Dependencies:** `Driver` aggregate.
- **Estimated Complexity:** **S**

#### GAP-004: Driver Scorecards & Violations Management
- **Task ID:** `MVP-GAP-004`
- **User Stories:** `US-41` (Driver Performance), `US-42` (Violations)
- **Exact Gap:** No driver violation recording, penalty points, or scorecard aggregate.
- **Backend Work:** Create `DriverViolation` entity with fine amounts and points; create `DriverScorecard` query service.
- **Frontend Work:** Driver violations and performance scorecard tab.
- **Tests Required:** `DriverViolationServiceTest`, `DriverScorecardTest`.
- **Dependencies:** `Driver`, `Trip`.
- **Estimated Complexity:** **L**

#### GAP-005: Driver Medical Fitness & Drug Screening
- **Task ID:** `MVP-GAP-005`
- **User Stories:** `US-43` (Medical Fitness), `US-44` (Drug Tests)
- **Exact Gap:** No structured medical certificate or substance screening entities.
- **Backend Work:** Create `DriverMedicalRecord` and `DriverDrugTest` entities in `fleet`; add availability blocking on failed/expired tests.
- **Frontend Work:** Medical & compliance tab in driver profile.
- **Tests Required:** `DriverMedicalServiceTest`, `DriverDrugTestTest`.
- **Dependencies:** `DriverAvailabilityService`.
- **Estimated Complexity:** **M**

---

### Priority P2 — Hardening & Usability Enhancements

#### GAP-006: Lubricant & Fluid Consumption Logs
- **Task ID:** `MVP-GAP-006`
- **User Story:** `US-05` (Maintain Fuel & Lubricant Logs)
- **Exact Gap:** Non-fuel fluid tracking (oil, coolant) is not modeled.
- **Backend Work:** Create `LubricantLog` entity in `fleet`.
- **Frontend Work:** Add lubricant logging section to vehicle readings.
- **Estimated Complexity:** **S**

#### GAP-007: En-Route Checkpoints & Delay Logging
- **Task ID:** `MVP-GAP-007`
- **User Story:** `US-13` (Maintain Trip Log)
- **Exact Gap:** Intermediate arrival checkpoints and mid-trip delay logs.
- **Backend Work:** Create `TripCheckpoint` entity in `trip`.
- **Frontend Work:** Add checkpoint recording action in `TripDetailsPage.tsx`.
- **Estimated Complexity:** **S**

#### GAP-008: In-App & Email Notification Rules Engine
- **Task ID:** `MVP-GAP-008`
- **User Story:** `US-77` (Manage Notification Rules)
- **Exact Gap:** No rule-based notification dispatching.
- **Backend Work:** Implement `NotificationRule` engine with Spring event listeners.
- **Frontend Work:** User notification preferences screen.
- **Estimated Complexity:** **L**

#### GAP-009: Dynamic Workflow Transition Engine
- **Task ID:** `MVP-GAP-009`
- **User Story:** `US-80` (Configure Workflows)
- **Exact Gap:** Dynamic, user-defined workflow transition editor.
- **Backend Work:** Configurable state transition engine.
- **Frontend Work:** Visual workflow graph editor.
- **Estimated Complexity:** **XL**

#### GAP-010: Resource & Shift Scheduling
- **Task ID:** `MVP-GAP-010`
- **User Story:** `US-81` (Manage Scheduling)
- **Exact Gap:** Resource calendars, driver shifts, holiday blackout schedules.
- **Backend Work:** Create `ShiftSchedule` and `HolidayCalendar` entities in `organization`.
- **Frontend Work:** Visual resource scheduling calendar.
- **Estimated Complexity:** **L**

#### GAP-011: Offline Synchronization Queue
- **Task ID:** `MVP-GAP-011`
- **User Story:** `US-71` (Support Offline Data Sync)
- **Exact Gap:** Mobile store-and-forward sync queue and conflict resolver.
- **Backend Work:** Idempotent batch sync endpoints.
- **Frontend Work:** IndexedDB sync queue / service worker.
- **Estimated Complexity:** **XL (Phase 3 Mobile)**

---

## 10. Recommended Next Development Task

### Selection Rationale:
Following the successful completion and verification of `MVP-GAP-002` (100% green test baseline with 358 passing backend tests, 71 passing frontend tests, and 0 Modulith/Architecture violations), the next development priority moves to the top of the **Priority P1** queue:

1. **Complete driver exception and leave management:** `US-45` (Handle Driver Exceptions) currently only checks static driver operational status. Adding time-off and leave management will allow the system to prevent scheduling drivers who are on approved leave or suspended for disciplinary reasons.
2. **High operational value & clean encapsulation:** `DriverException` resides strictly within the `fleet` module with zero cross-module circular dependencies.

```
NEXT TASK ID:       MVP-GAP-003
RELATED STORY:      US-45 (Handle Driver Exceptions)
TITLE:              Implement Driver Leave and Exception Management
PRIORITY:           P1
DEPENDENCIES:       Driver aggregate (exists).
EXPECTED RESULT:    DriverException entity created; DriverAvailabilityService rejects trip allocations overlapping active leave or exception windows with DRIVER_UNAVAILABLE.
```

---

## 11. Implementation Update: MVP-GAP-001-FIX (US-75 Operational Reporting & Boundary Resolution)

**Implementation Date:** August 19, 2026  
**Status:** **COMPLETED**  
**Story Reclassification:** **US-75: PARTIAL $\rightarrow$ COMPLETE**

### 11.1 Architecture & Defect Resolution
1. **Spring Modulith Boundary Defect Resolved:**
   - Deleted invalid cross-module persistence repository `com.transportlogistics.app.reporting.infrastructure.adapters.in.persistence.TripReportRepository` which directly imported and queried `trip.infrastructure.adapters.out.persistence.TripEntity`.
   - Established public read ports at module root packages:
     - `com.transportlogistics.app.trip.TripReportingQuery` (with public DTO records `TripReportItem`, `DriverAssignmentReportItem`, `VehicleTripReportItem`).
     - `com.transportlogistics.app.fleet.FleetReportingQuery` (with public DTO records `FleetVehicleSummary`, `FleetDriverSummary`).
   - Implemented owning module persistence adapters:
     - `com.transportlogistics.app.trip.infrastructure.adapters.out.persistence.TripReportingAdapter` implementing `TripReportingQuery` via optimized JPQL constructor queries on `TripJpaRepository`.
     - `com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence.FleetReportingAdapter` implementing `FleetReportingQuery` via `VehicleJpaRepository` and `DriverJpaRepository`.
   - Implemented reporting outbound ports and adapters:
     - `com.transportlogistics.app.reporting.application.ports.out.TripReportReadPort` $\rightarrow$ `TripReportReadAdapter` (in `reporting.infrastructure.adapters.out.trip`).
     - `com.transportlogistics.app.reporting.application.ports.out.FleetReportReadPort` $\rightarrow$ `FleetReportReadAdapter` (in `reporting.infrastructure.adapters.out.fleet`).

2. **Application Services & Typed Web DTOs:**
   - Implemented transactional read-only application services in `reporting.application.service`:
     - `TripReportService implements TripReportUseCase` (supporting date range validation, status filtering, customerId filtering, pagination, and vehicle/driver enrichment).
     - `DriverAssignmentService implements DriverAssignmentUseCase` (supporting date range validation, driverId filtering, and vehicle/driver enrichment).
     - `VehicleUtilizationService implements VehicleUtilizationUseCase` (supporting date range validation, vehicleId filtering, fleet-wide aggregation, completed trip counts, distance summation, and allocated duration hours).
   - Replaced raw untyped `Map<String, Object>` responses in `ReportingController` with typed DTOs:
     - `GET /api/v1/reports/trips` $\rightarrow$ `PageResponse<TripReportResponse>`
     - `GET /api/v1/reports/driver-assignments` $\rightarrow$ `List<DriverAssignmentResponse>`
     - `GET /api/v1/reports/vehicle-utilization` $\rightarrow$ `List<VehicleUtilizationResponse>`
   - Mapped models to response DTOs using MapStruct interface `ReportingWebMapper`.
   - Preserved RBAC security requiring `REPORT_VIEW` permission for `/reports/**` and `DASHBOARD_VIEW` for `/dashboard/operations`.

### 11.2 Files Changed, Created, and Deleted
- **Created Files:**
  - `src/main/java/com/transportlogistics/app/trip/TripReportItem.java`
  - `src/main/java/com/transportlogistics/app/trip/DriverAssignmentReportItem.java`
  - `src/main/java/com/transportlogistics/app/trip/VehicleTripReportItem.java`
  - `src/main/java/com/transportlogistics/app/trip/TripReportingQuery.java`
  - `src/main/java/com/transportlogistics/app/trip/infrastructure/adapters/out/persistence/TripReportingAdapter.java`
  - `src/main/java/com/transportlogistics/app/fleet/FleetVehicleSummary.java`
  - `src/main/java/com/transportlogistics/app/fleet/FleetDriverSummary.java`
  - `src/main/java/com/transportlogistics/app/fleet/FleetReportingQuery.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/out/persistence/FleetReportingAdapter.java`
  - `src/main/java/com/transportlogistics/app/reporting/application/model/TripReportRecord.java`
  - `src/main/java/com/transportlogistics/app/reporting/application/model/DriverAssignmentReportRecord.java`
  - `src/main/java/com/transportlogistics/app/reporting/application/model/VehicleUtilizationReportRecord.java`
  - `src/main/java/com/transportlogistics/app/reporting/application/ports/out/TripReportReadPort.java`
  - `src/main/java/com/transportlogistics/app/reporting/application/ports/out/FleetReportReadPort.java`
  - `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/out/trip/TripReportReadAdapter.java`
  - `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/out/fleet/FleetReportReadAdapter.java`
  - `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/web/dto/response/TripReportResponse.java`
  - `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/web/dto/response/DriverAssignmentResponse.java`
  - `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/web/dto/response/VehicleUtilizationResponse.java`
  - `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/web/dto/response/PageResponse.java`
  - `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/web/mappers/ReportingWebMapper.java`
  - `src/test/java/com/transportlogistics/app/reporting/application/service/TripReportServiceTest.java`
  - `src/test/java/com/transportlogistics/app/reporting/application/service/DriverAssignmentServiceTest.java`
  - `src/test/java/com/transportlogistics/app/reporting/application/service/VehicleUtilizationServiceTest.java`
  - `src/test/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/web/ReportingControllerTest.java`
  - `src/test/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/web/ReportingSecurityIntegrationTest.java`
- **Modified Files:**
  - `src/main/java/com/transportlogistics/app/trip/infrastructure/adapters/out/persistence/TripJpaRepository.java` (added constructor JPQL reporting queries)
  - `src/main/java/com/transportlogistics/app/reporting/application/ports/in/TripReportUseCase.java`
  - `src/main/java/com/transportlogistics/app/reporting/application/ports/in/DriverAssignmentUseCase.java`
  - `src/main/java/com/transportlogistics/app/reporting/application/ports/in/VehicleUtilizationUseCase.java`
  - `src/main/java/com/transportlogistics/app/reporting/application/service/TripReportService.java`
  - `src/main/java/com/transportlogistics/app/reporting/application/service/DriverAssignmentService.java`
  - `src/main/java/com/transportlogistics/app/reporting/application/service/VehicleUtilizationService.java`
  - `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/web/controllers/ReportingController.java`
- **Deleted Files:**
  - `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/persistence/TripReportRepository.java` (removed cross-module violation)
  - `src/main/java/com/transportlogistics/app/reporting/infrastructure/adapters/in/persistence/TripReportProjection.java`
  - `src/main/java/com/transportlogistics/app/reporting/web/dto/response/reporting/TripReportDto.java`
  - `src/main/java/com/transportlogistics/app/reporting/web/dto/response/reporting/DriverAssignmentDto.java`
  - `src/main/java/com/transportlogistics/app/reporting/web/dto/response/reporting/VehicleUtilizationDto.java`

### 11.3 Verification Results
- **Architecture & Modulith Boundaries:**
  - `ApplicationModulesTest`: **PASSED (2/2 tests, 0 failures, 0 violations)**
  - `HexagonalLayerArchitectureTest`: **PASSED (7/7 tests, 0 failures)**
  - `ModuleBoundaryArchitectureTest`: **PASSED (3/3 tests, 0 failures)**
- **Backend Test Suite:**
  - `mvn -B test`: **329 tests run, 0 failures, 0 errors, 21 skipped (100% pass)**
- **Frontend Test & Production Build:**
  - `vitest run`: **68 tests across 12 files passed (100% pass)**
  - `npm run build`: **5,071 modules transformed, 0 errors**

---

## 12. Implementation Update: MVP-GAP-002 (US-07 Preventive Maintenance Scheduling & Availability Linkage)

**Implementation Date:** August 19, 2026  
**Status:** **COMPLETED**  
**Story Reclassification:** **US-07: PARTIAL $\rightarrow$ COMPLETE**

### 12.1 Architecture & Implementation Overview
1. **Domain Model & Invariants:**
   - Defined `MaintenanceStatus` enum (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`).
   - Implemented `MaintenanceSchedule` domain aggregate record with validation invariants (`scheduledEnd > scheduledStart`, `cost >= 0`, `maintenanceType` not blank) and `hasOverlap(from, to)` helper using half-open interval semantics `[scheduledStart, scheduledEnd)`.

2. **Application Layer & Availability Linkage:**
   - Inbound Port: `MaintenanceScheduleUseCase` defining CRUD, rescheduling, cancellation, completion, and overlap lookup.
   - Outbound Port: `MaintenanceScheduleRepository`.
   - Application Service: `MaintenanceScheduleService` implementing transactional lifecycle transitions with conflict validation and audit logging.
   - Availability Integration: Updated `VehicleAvailabilityService` to check both real-time operational status and active maintenance schedules (`SCHEDULED`, `IN_PROGRESS`). Overlapping vehicle allocations are strictly rejected with structured reason code `MAINTENANCE_BLOCKED`.
   - Boundary Check: Confirmed that boundary matches (`allocation.end == maintenance.start` or `allocation.start == maintenance.end`) are valid and allowed per half-open interval specification.

3. **Persistence & Database Migration:**
   - Flyway Migration: `V19__maintenance_schedules.sql` creating `maintenance_schedule` table with foreign key `fk_maintenance_schedule_vehicle`, check constraint `chk_maintenance_schedule_dates`, performance indexes, and registering `VEHICLE_MAINTENANCE_MANAGE` permission.
   - Entity: `MaintenanceScheduleEntity` mapping all domain fields with audit timestamps.
   - Adapter: `MaintenanceSchedulePersistenceAdapter` implementing `MaintenanceScheduleRepository` with JPQL overlap query `m.scheduledStart < :to AND m.scheduledEnd > :from`.

4. **Web Layer & Security:**
   - DTOs: `MaintenanceScheduleRequest`, `MaintenanceSchedulePatchRequest`, `MaintenanceActionRequest`, `MaintenanceScheduleResponse`.
   - Mapper: `FleetWebMapper` MapStruct interface.
   - REST Controller: Endpoints in `FleetController`:
     - `GET /api/v1/vehicles/{vehicleId}/maintenance-schedules` (`VEHICLE_VIEW`)
     - `POST /api/v1/vehicles/{vehicleId}/maintenance-schedules` (`VEHICLE_MAINTENANCE_MANAGE`)
     - `GET /api/v1/vehicles/{vehicleId}/maintenance-schedules/{scheduleId}` (`VEHICLE_VIEW`)
     - `PUT /api/v1/vehicles/{vehicleId}/maintenance-schedules/{scheduleId}` (`VEHICLE_MAINTENANCE_MANAGE`)
     - `PATCH /api/v1/vehicles/{vehicleId}/maintenance-schedules/{scheduleId}` (`VEHICLE_MAINTENANCE_MANAGE`)
     - `POST /api/v1/vehicles/{vehicleId}/maintenance-schedules/{scheduleId}/cancel` (`VEHICLE_MAINTENANCE_MANAGE`)
     - `POST /api/v1/vehicles/{vehicleId}/maintenance-schedules/{scheduleId}/complete` (`VEHICLE_MAINTENANCE_MANAGE`)

5. **Frontend Management Component:**
   - Types: Updated `frontend/src/fleet/types.ts` with `MaintenanceStatus` and `MaintenanceSchedule`.
   - Hooks: Created `useVehicleMaintenance.ts` with query and mutation hooks invalidating vehicle and schedule caches.
   - UI Component: Created `VehicleMaintenanceSection.tsx` embedded in `ResourceListPage.tsx` vehicle drawer with interactive schedule table, status badges, date-time pickers, cancellation modals, and completion modals.

### 12.2 Files Changed and Created
- **Database Migration:**
  - `src/main/resources/db/migration/V19__maintenance_schedules.sql`
- **Domain Layer:**
  - `src/main/java/com/transportlogistics/app/fleet/domain/model/MaintenanceStatus.java`
  - `src/main/java/com/transportlogistics/app/fleet/domain/model/MaintenanceSchedule.java`
- **Application Layer:**
  - `src/main/java/com/transportlogistics/app/fleet/application/ports/in/MaintenanceScheduleUseCase.java`
  - `src/main/java/com/transportlogistics/app/fleet/application/ports/out/MaintenanceScheduleRepository.java`
  - `src/main/java/com/transportlogistics/app/fleet/application/service/MaintenanceScheduleService.java`
  - `src/main/java/com/transportlogistics/app/fleet/application/service/VehicleAvailabilityService.java` (updated)
- **Infrastructure Layer:**
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/config/MaintenanceScheduleConfig.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/config/VehicleConfig.java` (updated)
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/out/persistence/MaintenanceScheduleEntity.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/out/persistence/MaintenanceScheduleJpaRepository.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/out/persistence/MaintenanceSchedulePersistenceAdapter.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/dto/request/MaintenanceScheduleRequest.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/dto/request/MaintenanceSchedulePatchRequest.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/dto/request/MaintenanceActionRequest.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/dto/response/MaintenanceScheduleResponse.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/mappers/FleetWebMapper.java` (updated)
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/controllers/FleetController.java` (updated)
  - `src/main/java/com/transportlogistics/app/identity/infrastructure/security/SecurityConfig.java` (updated)
- **Frontend Layer:**
  - `frontend/src/fleet/types.ts` (updated)
  - `frontend/src/fleet/useVehicleMaintenance.ts`
  - `frontend/src/fleet/VehicleMaintenanceSection.tsx`
  - `frontend/src/pages/ResourceListPage.tsx` (updated)
- **Automated Tests:**
  - `src/test/java/com/transportlogistics/app/fleet/domain/model/MaintenanceScheduleTest.java` (4 tests)
  - `src/test/java/com/transportlogistics/app/fleet/application/service/MaintenanceScheduleServiceTest.java` (9 tests)
  - `src/test/java/com/transportlogistics/app/fleet/application/service/VehicleAvailabilityServiceTest.java` (14 tests)
  - `src/test/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/controllers/FleetControllerMaintenanceTest.java` (8 tests)
  - `src/test/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/FleetMaintenanceSecurityIntegrationTest.java` (3 tests)
  - `src/test/java/com/transportlogistics/app/fleet/infrastructure/adapters/out/persistence/MaintenanceSchedulePersistenceIntegrationTest.java` (3 tests)
  - `src/test/java/com/transportlogistics/app/trip/infrastructure/adapters/out/persistence/TripVehicleMaintenanceAssignmentIntegrationTest.java` (3 tests)
  - `frontend/src/fleet/VehicleMaintenanceSection.test.tsx` (3 tests)

### 12.3 Verification Results
- **Architecture & Modulith Boundaries:**
  - `ApplicationModulesTest`: **PASSED (2/2 tests, 0 failures, 0 violations)**
  - `HexagonalLayerArchitectureTest`: **PASSED (7/7 tests, 0 failures)**
  - `ModuleBoundaryArchitectureTest`: **PASSED (3/3 tests, 0 failures)**
  - `LombokUsageArchitectureTest`: **PASSED (3/3 tests, 0 failures)**
- **Backend Test Suite:**
  - `mvn -B test`: **358 tests run, 0 failures, 0 errors, 21 skipped (100% pass)**
- **Frontend Test & Production Build:**
  - `vitest run`: **71 tests across 13 files passed (100% pass)**
  - `npm run build`: **5,073 modules transformed, 0 errors (25.36s)**

---

## 13. Implementation Update: MVP-GAP-003 (US-45 Driver Leave & Exception Management)

**Implementation Date:** August 19, 2026  
**Status:** **COMPLETED**  
**Story Reclassification:** **US-45: PARTIAL $\rightarrow$ COMPLETE**

### 13.1 Architecture & Implementation Summary
1. **Database Schema & Forward Migration (`V20__driver_exceptions.sql`):**
   - Created table `driver_exception` with primary key UUID, foreign key to `driver(id)` ON DELETE CASCADE, check constraint `chk_driver_exception_dates (end_time > start_time)`, and composite indexes `(driver_id, status)` and `(start_time, end_time)`.
   - Seeded permission `DRIVER_EXCEPTION_MANAGE` into `app_permission`.

2. **Fleet Domain Model & Invariants (`com.transportlogistics.app.fleet.domain.model`):**
   - Implemented `DriverExceptionType` (`LEAVE`, `DISCIPLINARY_SUSPENSION`, `MEDICAL_EMERGENCY`, `OTHER`).
   - Implemented `DriverExceptionStatus` (`SCHEDULED`, `ACTIVE`, `COMPLETED`, `CANCELLED`) with `isBlocking()` query method.
   - Implemented `DriverException` domain model with strict half-open interval overlap detection $[startTime, endTime)$.
   - Added `DriverAvailability.Code.DRIVER_EXCEPTION_BLOCKED` to driver availability rejection taxonomy.

3. **Application Layer & Bidirectional Conflict Engine (`com.transportlogistics.app.fleet.application`):**
   - Implemented inbound port `DriverExceptionUseCase` and outbound repository port `DriverExceptionRepository`.
   - Implemented `DriverExceptionService` with pessimistic database row locking (`drivers.findByIdForUpdate(driverId)`), legal lifecycle state machine, and bidirectional trip conflict enforcement:
     - **Rule A (Exception $\rightarrow$ Trip):** Existing blocking driver exceptions prevent overlapping trip driver assignments with `DRIVER_EXCEPTION_BLOCKED`.
     - **Rule B (Trip $\rightarrow$ Exception):** Existing active trip assignments block new overlapping driver exception creation with `BusinessRuleException` (`TRIP_CONFLICT`).
     - **Rule C (Trip $\rightarrow$ Rescheduled Exception):** Existing active trip assignments block rescheduling exceptions into trip windows with `BusinessRuleException` (`TRIP_CONFLICT`).
   - Updated `DriverAvailabilityService` to check active overlapping exceptions (`SCHEDULED`, `ACTIVE`) and return `DRIVER_EXCEPTION_BLOCKED`.
   - Registered `DriverExceptionConfig` and updated `DriverConfig`.

4. **Infrastructure Persistence & Web Adapters:**
   - Implemented `DriverExceptionEntity`, `DriverExceptionJpaRepository`, and `DriverExceptionPersistenceAdapter`.
   - Implemented request/response DTOs: `DriverExceptionRequest`, `DriverExceptionPatchRequest`, `DriverExceptionActionRequest`, `DriverExceptionResponse`.
   - Added MapStruct translation methods in `FleetWebMapper`.
   - Added REST endpoints in `FleetController` under `/api/v1/drivers/{driverId}/exceptions/**`.
   - Secured endpoints in `SecurityConfig` with `DRIVER_VIEW` and `DRIVER_EXCEPTION_MANAGE`.

5. **Frontend Management Drawer (`frontend/src/fleet`):**
   - Added TypeScript interfaces in `frontend/src/fleet/types.ts`.
   - Created React Query hooks in `frontend/src/fleet/useDriverExceptions.ts`.
   - Built full Ant Design management UI in `frontend/src/fleet/DriverExceptionSection.tsx` supporting creation, rescheduling, cancellation, and completion with remarks.
   - Integrated into driver drawer in `frontend/src/pages/ResourceListPage.tsx`.

### 13.2 Files Created / Modified
- **Database Layer:**
  - `src/main/resources/db/migration/V20__driver_exceptions.sql`
- **Domain Layer:**
  - `src/main/java/com/transportlogistics/app/fleet/domain/model/DriverExceptionType.java`
  - `src/main/java/com/transportlogistics/app/fleet/domain/model/DriverExceptionStatus.java`
  - `src/main/java/com/transportlogistics/app/fleet/domain/model/DriverException.java`
  - `src/main/java/com/transportlogistics/app/fleet/domain/model/DriverAvailability.java` (updated)
- **Application Layer:**
  - `src/main/java/com/transportlogistics/app/fleet/application/ports/in/DriverExceptionUseCase.java`
  - `src/main/java/com/transportlogistics/app/fleet/application/ports/out/DriverExceptionRepository.java`
  - `src/main/java/com/transportlogistics/app/fleet/application/service/DriverExceptionService.java`
  - `src/main/java/com/transportlogistics/app/fleet/application/service/DriverAvailabilityService.java` (updated)
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/config/DriverConfig.java` (updated)
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/config/DriverExceptionConfig.java`
- **Infrastructure Persistence Layer:**
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/out/persistence/DriverExceptionEntity.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/out/persistence/DriverExceptionJpaRepository.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/out/persistence/DriverExceptionPersistenceAdapter.java`
- **Infrastructure Web Layer:**
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/dto/request/DriverExceptionRequest.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/dto/request/DriverExceptionPatchRequest.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/dto/request/DriverExceptionActionRequest.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/dto/response/DriverExceptionResponse.java`
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/mappers/FleetWebMapper.java` (updated)
  - `src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/controllers/FleetController.java` (updated)
  - `src/main/java/com/transportlogistics/app/identity/infrastructure/security/SecurityConfig.java` (updated)
- **Frontend Layer:**
  - `frontend/src/fleet/types.ts` (updated)
  - `frontend/src/fleet/useDriverExceptions.ts`
  - `frontend/src/fleet/DriverExceptionSection.tsx`
  - `frontend/src/pages/ResourceListPage.tsx` (updated)
- **Automated Tests:**
  - `src/test/java/com/transportlogistics/app/fleet/domain/model/DriverExceptionTest.java` (3 tests)
  - `src/test/java/com/transportlogistics/app/fleet/application/service/DriverExceptionServiceTest.java` (11 tests)
  - `src/test/java/com/transportlogistics/app/fleet/application/service/DriverAvailabilityServiceTest.java` (16 tests)
  - `src/test/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/controllers/FleetControllerDriverExceptionTest.java` (5 tests)
  - `src/test/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/FleetDriverExceptionSecurityIntegrationTest.java` (3 tests)
  - `src/test/java/com/transportlogistics/app/fleet/infrastructure/adapters/out/persistence/DriverExceptionPersistenceIntegrationTest.java` (2 tests)
  - `src/test/java/com/transportlogistics/app/trip/infrastructure/adapters/out/persistence/TripDriverExceptionAssignmentIntegrationTest.java` (7 tests)
  - `frontend/src/fleet/DriverExceptionSection.test.tsx` (2 tests)

### 13.3 Verification Results
---

## 14. Implementation Update: MVP-GAP-004 & QA-AUTO-001 Verification

**Implementation Date:** August 19, 2026  
**Status:** **COMPLETED**  
**Story Reclassifications:**
- **US-41: NOT IMPLEMENTED $\rightarrow$ COMPLETE**
- **US-42: NOT IMPLEMENTED $\rightarrow$ COMPLETE**

### 14.1 Architecture & Implementation Summary
1. **US-41 (Assess Driver Performance):**
   - Implemented `DriverPerformanceService`, calculating safety score (0–100), overall rating (`EXCELLENT`, `GOOD`, `SATISFACTORY`, `NEEDS_IMPROVEMENT`, `AT_RISK`), trip reliability KPI (completion rate %), repeat-offender penalty calculations, and infraction statistics.
   - Built frontend `DriverPerformanceSection.tsx` with dashboard gauges and stats cards.
   - Unit & UI tested with 100% pass rate.

2. **US-42 (Manage Violations):**
   - Created `V21__driver_violations.sql` forward migration with check constraints and index optimizations.
   - Implemented `DriverViolation` aggregate, `DriverViolationService`, severity scales, penalty points, fine amounts, and payment lifecycle (`UNPAID`, `PAID`, `WAIVED`, `DISPUTED`).
   - Built frontend `DriverViolationsSection.tsx` with recording modal and payment drawers.
   - Secured with `DRIVER_VIOLATION_MANAGE` permission.

3. **QA-AUTO-001 (Playwright UI Automation Framework):**
   - Engineered full E2E framework under `frontend/e2e/` with 11 Page Object Models, custom role fixtures, deterministic data factories, and Ant Design interaction helpers.
   - Delivered 3 QA artifacts: `docs/qa/PLAYWRIGHT-MVP-COVERAGE-MATRIX.md`, `docs/qa/PLAYWRIGHT-MVP-TEST-CASES.md`, `docs/qa/PLAYWRIGHT-DISCOVERED-DEFECTS.md`.
   - Verified 30 tests across 24 specs (100% passing on Chromium, 6/6 passing across Chromium, Firefox, WebKit in smoke suite).

### 14.2 Previous State Metrics
- **Complete Stories:** 31 / 39 (79.5%)
- **Functional Coverage:** 84.6%
- **Architecture Tests:** 15/15 passing (100%)
- **Frontend Tests:** 75/75 passing (100%)
- **Playwright E2E:** 30/30 passing (100%)

---

## 15. Implementation Update: MVP-GAP-005 Verification

**Implementation Date:** August 19, 2026  
**Status:** **COMPLETED**  
**Story Reclassifications:**
- **US-43: NOT IMPLEMENTED $\rightarrow$ COMPLETE**
- **US-44: NOT IMPLEMENTED $\rightarrow$ COMPLETE**

### 15.1 Architecture & Implementation Summary
1. **US-43 (Manage Driver Medical Fitness):**
   - Created `V22__driver_medical_and_drug_tests.sql` migration creating table `driver_medical_record` and seeding permissions `DRIVER_MEDICAL_VIEW` and `DRIVER_MEDICAL_MANAGE`.
   - Implemented domain aggregates `DriverMedicalRecord`, `DriverMedicalStatus`, `VisionTestStatus`.
   - Implemented `DriverMedicalRecordService` with transactional boundaries, pessimistic row locking, and `DriverMedicalRecordPersistenceAdapter`.
   - Integrated with `DriverAvailabilityService` to reject unfit drivers (`MEDICALLY_UNFIT`) and drivers with expired certificates (`MEDICAL_FITNESS_EXPIRED`).
   - Delivered frontend `DriverMedicalSection.tsx` and `useDriverMedicalRecords.ts` with modal recording form.
   - Added automated Playwright test `E2E-DRV-007`.

2. **US-44 (Manage Drug Tests):**
   - Created table `driver_drug_test` in `V22__driver_medical_and_drug_tests.sql` and seeded permissions `DRIVER_DRUG_TEST_VIEW` and `DRIVER_DRUG_TEST_MANAGE`.
   - Implemented domain aggregate `DriverDrugTest`, `DrugTestType`, `DrugTestResult`, `DrugTestStatus`.
   - Implemented `DriverDrugTestService` managing scheduling, sample collection timestamps, result recording, and return-to-duty clearance workflows.
   - Integrated with `DriverAvailabilityService` to reject positive/uncleared drivers (`RETURN_TO_DUTY_CLEARANCE_REQUIRED` / `DRUG_TEST_FAILED`).
   - Delivered frontend `DriverDrugTestSection.tsx` and `useDriverDrugTests.ts` with scheduling, result recording, and RTD clearance actions.
   - Added automated Playwright test `E2E-DRV-008`.

### 15.2 Current State Metrics Post MVP-GAP-005
- **Total MVP Stories:** 39
- **Complete Stories:** **33** / 39 (**84.6%**)
- **Partial Stories:** **4** / 39 (**10.3%**)
- **Not Implemented Stories:** **2** / 39 (**5.1%**)
- **Functional Coverage:** **89.7%**
- **Driver Management Module Coverage:** **100.0%** (7 of 7 stories COMPLETE)
- **Backend Tests:** 316 unit & architecture tests passing (0 failures, 0 errors)
- **Modulith & Architecture Rules:** 15/15 passing (100%)
- **Frontend Vitest Tests:** 77/77 passing (100%)
- **Frontend Build:** Clean (`tsc -b && vite build` succeeded)
- **Playwright E2E Tests:** 32/32 passing (100% across Chromium)
- **Remaining P1 Gaps:** None. All P1 Driver Management stories are complete.





