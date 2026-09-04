# 🚚 Transport & Logistics — Enterprise Modular Monolith Roadmap

<div align="center">

![Total Stories](https://img.shields.io/badge/Total%20Stories-87-0969da.svg?style=for-the-badge&logo=target)
![Completed](https://img.shields.io/badge/Completed-66%20%2F%2087-2da44e.svg?style=for-the-badge&logo=checkmarx)
![Progress](https://img.shields.io/badge/Progress-75.9%25-brightgreen.svg?style=for-the-badge&logo=speedtest)
![MVP 1.4 Closed](https://img.shields.io/badge/MVP%201.4%20Last--Mile-8%20%2F%208-2da44e.svg?style=for-the-badge&logo=pinboard)
![Database](https://img.shields.io/badge/PostgreSQL%20%2F%20Flyway-V62-8a63d2.svg?style=for-the-badge&logo=postgresql)

</div>

---

> [!IMPORTANT]
> **Authoritative Baseline & Current State:**
> - **Last Reconciled:** `2026-09-04`
> - **Authority Order:** Original Requirements (`Traspotation & logistic.docx`), Frozen Architecture/Contracts, Verified Production Code/Tests, then Roadmap.
> - **MVP 1.3 Delivery Operations:** 7 / 7 COMPLETE (100%) - CLOSED
> - **MVP 1.4 Last-Mile Delivery:** 8 / 8 COMPLETE (US-63 through US-70 Accepted & Closed)
> - **Overall Release Band:** 67 / 87 COMPLETE (20 REPRIORITIZED / 87 TOTAL)
> - **Current Milestone:** MVP 1.4 Last-Mile Delivery — 8 / 8 COMPLETE, 100%, CLOSED.
> - **Active Focus:** Wave A is 2 / 2 COMPLETE and closed after independent US-78 acceptance.
> - **Immediate Next Action:** Execute `US-37-FUEL-PERFORMANCE-IMPLEMENTATION-001`.

---

## 📊 1. Executive Metrics & Milestone Progress

```
Overall Progress: [████████████████████████████████████████████░░░░░░] 67 / 87 Stories Complete (77.0%)
MVP 1.3 Band:     [██████████████████████████████████████████████████] 7 / 7 Complete (100.0%) - CLOSED
MVP 1.4 Band:     [██████████████████████████████████████████████████] 8 / 8 Complete (100.0%) - CLOSED
```

| Metric | Target | Current Count | Percentage | Status Indicator |
| :--- | :---: | :---: | :---: | :--- |
| **Completed Stories (Accepted)** | 87 | **66** | `75.9%` | 🟢 `ON TRACK / VERIFIED` |
| **Next Active / Acceptance Required** | — | **0** | `0.0%` | 🟢 `NONE` |
| **Not Started (MVP 1.4 Active Scope)** | 1 | **0** | `0.0%` | 🟢 `NONE` |
| **Reprioritized Remaining Stories** | 21 | **21** | `24.1%` | 🟡 `PLANNED IN GOVERNED WAVES` |
| **Total Registered User Stories** | **87** | **87** | **`100%`** | 🔒 `FROZEN REGISTER (US-01..US-87)` |

> [!NOTE]
> Stories `US-88`, `US-89`, and `US-90` are strictly undefined. The story accounting register is bounded exactly from `US-01` through `US-87` (`66 COMPLETE + 21 REMAINING = 87`).

---

## 🧭 2. MVP Phase Breakdown & Status Matrix

| Phase | Milestone Scope | Scope Definition | Stories | Progress | Status |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **Phase 0** | **Foundation** | Spring Modulith, Multi-Tenancy, Hexagonal Architecture, Flyway Baseline | Enabler | 100% | 🟢 `COMPLETE` |
| **MVP 1.0** | **Core Operations** | Fleet (US-01–08), Trips (US-09–16), Basic Route (US-17–19), Drivers (US-39–45), Core Enablers | 34 | 34 / 34 | 🟢 `COMPLETE (100%)` |
| **MVP 1.1A** | **Advanced Route** | Dynamic Route Optimization, Analytics, and Disruption Handling (US-20–23) | 4 | 4 / 4 | 🟢 `COMPLETE (100%)` |
| **MVP 1.1B** | **Freight & Cargo** | Freight Orders, Manifests, Load Planning, Weight/Volume Engine, Claims, Exceptions (US-24–30) | 7 | 7 / 7 | 🟢 `COMPLETE (100%)` |
| **MVP 1.2** | **Fuel Management** | Fuel Issues, Purchases, Mileage, Bunkers, Fuel Cards, Analytics, Exceptions (US-31–38) | 8 | 5 / 8 | 🟡 `MVP CLOSED; 3 STORIES REPRIORITIZED IN WAVE B` |
| **MVP 1.3** | **Delivery Operations** | Delivery Orders, Online POD, Offline POD, Failed Deliveries, Redelivery, Analytics, Exceptions (US-56–62) | 7 | 7 / 7 | 🟢 `COMPLETE (100%) - CLOSED` |
| **MVP 1.4** | **Last-Mile Delivery** | Delivery Zones, Delivery Slots, Riders, Batch Orders, ETA, Exceptions, Notifications, Self-Service (US-63–70) | 8 | 8 / 8 | 🟢 `COMPLETE (100%) - CLOSED` |
| **Full Product** | **Waves A–E** | Integration/Exceptions; Fuel/Finance; GPS; Compliance/Mobile; Analytics/Integrity/Resilience/Risk | 22 | 1 / 22 | 🟡 `IN PROGRESS — US-73 ACCEPTED` |

### Phase 0 Architecture Remediation

| Batch | Scope | Status | Verification Evidence |
| :--- | :--- | :---: | :--- |
| `P0-01` | Enforce Spring Modulith boundaries, acyclic modules, repository/entity ownership, hexagonal direction, Reporting isolation, and baseline dependency edges | ✅ `COMPLETE` | Architecture suite 32/32 PASS on Java 21; full suite reached 1,164 tests with one unrelated identity-bootstrap permission-count failure |
| `P0-02` | Establish explicit database/table ownership and prevent new cross-module repository, JPA mapping, or direct-SQL access | ✅ `COMPLETE` | Ownership tests 3/3, architecture/bootstrap tests 36/36, full Java 21 suite 1,167 PASS with 31 existing skips; two legacy SQL paths reserved for P0-03 |
| `P0-03` | Remove illegal cross-module implementation dependencies and enforce published-contract-only integration | ✅ `COMPLETE` | Freight consumes Fleet capacity through `FleetReportingQuery`; System probes sample-data readiness through `CustomerDataReadiness`; full Java 21 suite 1,168 PASS with 31 existing skips |
| `P0-04` | Make Tenant isolation mandatory across persistence, client input, tenant-local keys, object/list/report access, updates, and deletes | ✅ `COMPLETE` | PostgreSQL V57 applied; focused tenant/security/architecture gate 24/24 PASS; full Java 21 suite 1,173 PASS with 31 existing skips |
| `P0-05` | Harden Tenant-aware RBAC, contextual identity administration, privilege ceilings, and fail-closed HTTP authorization | ✅ `COMPLETE` | Focused authorization/architecture gate 72/72 PASS; Delivery PostgreSQL fixtures aligned with the canonical Tenant context; full Java 21 suite 1,180 PASS with 31 existing skips |
| `P0-06` | Correct aggregate-root identity/reference invariants and prevent unreviewed JPA aggregate graphs or cascades | ✅ `COMPLETE` | Focused domain/application/architecture gate 52/52 PASS; full Java 21 suite 1,185 PASS with 31 existing skips; no API/schema/event change |
| `P0-07` | Harden owning-module ACID transactions, after-commit local events, consumer failure isolation, Tenant event identity, and durable-integration decisions | ✅ `COMPLETE` | Focused consistency tests 46/46 and architecture/Modulith tests 42/42 PASS; full Java 21 suite 1,191 PASS with 31 existing conditional skips; no REST/schema/broker change |
| `P1-01` | Classify actual event usage, standardize consumed envelopes, and add one Tenant-scoped durable internal outbox boundary only where required | ✅ `COMPLETE` | 32 events classified; focused 52/52, PostgreSQL 7/7, architecture 45/45, full Java 21 suite 1,250 PASS with 15 conditional skips; V60 shared outbox; no broker/API/frontend change |

Development startup now consistently provisions the idempotent PostgreSQL sample fixture after Flyway when `run.sh` launches either the Docker stack or local PostgreSQL mode. H2 test startup continues to use the H2-specific fixture; this operational correction does not add a remediation batch or alter story accounting.

---

## 📋 3. Complete User Story Register (US-01 through US-87)

### 🚛 Module 1: Fleet Management (US-01 to US-08)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :---: | :--- |
| `US-01` | Manage Vehicle Master | Vehicle registry, categories, fuel types, lifecycle | 🟢 `COMPLETE` | `VehicleMasterTest`, V1–V3 |
| `US-02` | Manage Fleet Categories | Fleet classifications, capacity constraints | 🟢 `COMPLETE` | `FleetCategoryTest`, V2 |
| `US-03` | Manage Vehicle Documents | Registrations, insurance, roadworthiness | 🟢 `COMPLETE` | `VehicleDocumentTest`, V4 |
| `US-04` | Allocate Vehicles | Vehicle allocation state machine & scheduling | 🟢 `COMPLETE` | `VehicleAllocationTest`, V5 |
| `US-05` | Maintain Fuel & Lubricant Logs | Consumption tracking, fluid top-ups | 🟢 `COMPLETE` | `LubricantLogTest`, V6 |
| `US-06` | Maintain Running Logs | Odometer, engine hours, daily vehicle logs | 🟢 `COMPLETE` | `RunningLogTest`, V7 |
| `US-07` | Link Maintenance to Availability | Maintenance blocking availability schedules | 🟢 `COMPLETE` | `MaintenanceAvailabilityTest`, V8 |
| `US-08` | Handle Fleet Exceptions | Breakdown, document/registration, allocation, maintenance-status and rental-contract edge cases | 🟢 `COMPLETE` | Accepted current register; excluded from the exact 22-story remainder |

---

### 🛣️ Module 2: Trip Management (US-09 to US-16)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :---: | :--- |
| `US-09` | Create and Schedule Trips | Trip lifecycle, origin/destination, cargo link | 🟢 `COMPLETE` | `TripServiceTest`, V9 |
| `US-10` | Assign Driver and Vehicle to Trip | Joint allocation integrity validation | 🟢 `COMPLETE` | `TripAssignmentTest`, V10 |
| `US-11` | Dispatch and Track Trip Progress | Status transitions, departure, arrival timestamps | 🟢 `COMPLETE` | `TripDispatchTest`, V11 |
| `US-12` | Record Trip Checkpoints | Waypoint arrival/departure events | 🟢 `COMPLETE` | `TripCheckpointTest`, V12 |
| `US-13` | Log Delays and Incidents | Incident logging, delay duration, cause tracking | 🟢 `COMPLETE` | `TripIncidentTest`, V13 |
| `US-14` | Handle Trip Cancellations | Controlled cancellation with state reversal | 🟢 `COMPLETE` | `TripCancellationTest`, V14 |
| `US-15` | Complete Trip and Reconcile Logs | Final odometer, fuel reconciliation, closure | 🟢 `COMPLETE` | `TripCompletionTest`, V15 |
| `US-16` | Generate Trip Reports | Post-trip summary, variance analysis | 🟢 `COMPLETE` | `TripReportTest`, V16 |

---

### 🗺️ Module 3: Route Management (US-17 to US-23)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :---: | :--- |
| `US-17` | Define Standard Routes | Master routes, waypoint sequences, standard distance | 🟢 `COMPLETE` | `RouteDefinitionTest`, V17 |
| `US-18` | Manage Multi-Stop Routes | Intermediate stops, loading/unloading sequences | 🟢 `COMPLETE` | `MultiStopRouteTest`, V18 |
| `US-19` | Plan Alternate Routes | Detour definitions, hazard avoidance routes | 🟢 `COMPLETE` | `AlternateRouteTest`, V19 |
| `US-20` | Dynamic Route Optimization | Real-time traffic, shortest path, cost optimization | 🟢 `COMPLETE` | `RouteOptimizationTest`, V20 |
| `US-21` | Track Route Performance | Planned vs actual distance/duration variance | 🟢 `COMPLETE` | `RoutePerformanceTest`, V21 |
| `US-22` | Handle Route Disruptions | Blockage alerts, dynamic rerouting workflow | 🟢 `COMPLETE` | `RouteDisruptionTest`, V22 |
| `US-23` | Maintain Route Revision History | Versioned route modifications, audit trail | 🟢 `COMPLETE` | `RouteRevisionTest`, V23 |

---

### 📦 Module 4: Freight & Cargo Operations (US-24 to US-30)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :---: | :--- |
| `US-24` | Book Freight Orders | Shipper/consignee, goods classification, pricing | 🟢 `COMPLETE` | `FreightBookingTest`, V24 |
| `US-25` | Generate Cargo Manifests | Waybills, itemized package manifests, QR codes | 🟢 `COMPLETE` | `CargoManifestTest`, V25 |
| `US-26` | Plan Load and Space Utilization | Volume/weight optimization, axle load safety | 🟢 `COMPLETE` | `LoadPlanTest`, V26 |
| `US-27` | Weight and Volume Validation | Physical vs manifest weight reconciliation | 🟢 `COMPLETE` | `WeightValidationTest`, V27 |
| `US-28` | Track Cargo Status | End-to-end cargo milestone tracking | 🟢 `COMPLETE` | `CargoTrackingTest`, V28 |
| `US-29` | Process Cargo Claims | Loss/damage claims, settlement workflows | 🟢 `COMPLETE` | `CargoClaimTest`, V29 |
| `US-30` | Manage Cargo Exceptions | Damaged goods, discrepancies, quarantined items | 🟢 `COMPLETE` | `CargoExceptionTest`, V30 |

---

### ⛽ Module 5: Fuel & Energy Management (US-31 to US-38)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :---: | :--- |
| `US-31` | Record Fuel Issues | Internal pump issues, vehicle/driver linking | 🟢 `COMPLETE` | `FuelIssueTest`, V31 |
| `US-32` | Record External Fuel Purchases | Fuel card / receipt purchase logging | 🟢 `COMPLETE` | `FuelPurchaseTest`, V32 |
| `US-33` | Track Fuel Consumption and Mileage | Km/L calculation, baseline variance alerts | 🟢 `COMPLETE` | `FuelMileageTest`, V33 |
| `US-34` | Manage Bunker Fuel Stock | Internal tank dip readings, stock replenishment | 🟢 `COMPLETE` | `BunkerStockTest`, V34 |
| `US-35` | Manage Fuel Cards | Card issuance, restrictions, imported transactions, reconciliation and misuse control | 🟡 `WAVE B / READY_FOR_PRODUCT_DECISIONS` | Full-product roadmap |
| `US-36` | Calculate Trip Fuel Costs | Trip-level fuel expense aggregation | 🟢 `COMPLETE` | `TripFuelCostTest`, V35 |
| `US-37` | Analyze Fuel Performance | Vehicle/driver efficiency, anomaly and leakage analysis without changing raw fuel data | 🟡 `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED` | `US-37-FUEL-PERFORMANCE-PRODUCT-DECISIONS-001.md` |
| `US-38` | Handle Fuel Exceptions | Theft, wrong readings, price swings, emergency refuel, card misuse and negative balances | 🟡 `WAVE B / BLOCKED_BY_DEPENDENCY` | Requires US-35 and US-78 contracts |

---

### 👨‍✈️ Module 6: Driver Management (US-39 to US-46)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :---: | :--- |
| `US-39` | Manage Driver Profiles | Contact, license categories, emergency contacts | 🟢 `COMPLETE` | `DriverProfileTest`, V36 |
| `US-40` | Track Driver Availability | Duty status, leave management, shift schedules | 🟢 `COMPLETE` | `DriverAvailabilityTest`, V37 |
| `US-41` | Track Driver Performance | On-time delivery rate, safety score, violations | 🟢 `COMPLETE` | `DriverPerformanceTest`, V38 |
| `US-42` | Manage Driver Licenses | Expiration alerts, renewal workflows | 🟢 `COMPLETE` | `DriverLicenseTest`, V39 |
| `US-43` | Monitor Driver Hours of Service | Drive time limits, mandatory rest enforcement | 🟢 `COMPLETE` | `DriverHosTest`, V40 |
| `US-44` | Manage Driver Violations | Speeding, route deviation, safety incidents | 🟢 `COMPLETE` | `DriverViolationTest`, V41 |
| `US-45` | Manage Driver Medicals & Drug Tests | Periodic fitness-to-drive certifications | 🟢 `COMPLETE` | `DriverMedicalTest`, V42 |
| `US-46` | Process Driver Payroll Link | Traceable trip earnings, allowances, overtime and deductions exported to Payroll/HRMS | 🟡 `WAVE B / BLOCKED_BY_EXTERNAL_SYSTEM` | Payroll link only; not a payroll engine |

---

### 📡 Module 7: GPS & Telematics (US-47 to US-55)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :---: | :--- |
| `US-47` | Manage Transport Billing | Trip/freight costing, charges, adjustments, cost centres and billing finalization | 🟡 `WAVE B / READY_FOR_PRODUCT_DECISIONS` | Billing boundary requires ARB ratification |
| `US-48` | Track Vehicles Live | Provider-neutral telemetry ingestion, live/last-known state, connectivity, accuracy and freshness | 🟡 `WAVE C / READY_FOR_PRODUCT_DECISIONS` | GPS provider/device required for real acceptance |
| `US-49` | Manage Geofences | Depot, customer-site and unauthorized-zone entry/exit detection | 🟡 `WAVE C / BLOCKED_BY_DEPENDENCY` | Requires US-48 |
| `US-50` | Monitor Speed | Threshold, road-rule and repeat-speed monitoring | 🟡 `WAVE C / BLOCKED_BY_DEPENDENCY` | Requires US-48 |
| `US-51` | Monitor Idle Time | Engine-on versus movement duration and qualified fuel-waste estimates | 🟡 `WAVE C / BLOCKED_BY_DEPENDENCY` | Requires US-48 engine-state telemetry |
| `US-52` | Monitor Route Deviations | Planned-versus-actual comparison, severity and audited approval | 🟡 `WAVE C / BLOCKED_BY_DEPENDENCY` | Requires US-48 and Routing contract |
| `US-53` | Replay Journeys | Historical journey replay, stop analysis and incident forensics | 🟡 `WAVE C / BLOCKED_BY_DEPENDENCY` | Requires US-48 history; overlays US-49–52 |
| `US-54` | View Tracking Dashboard | Fleet overview, exceptions, heat maps, alerts and stale-state visibility | 🟡 `WAVE C / BLOCKED_BY_DEPENDENCY` | Must follow US-48–53 producers |
| `US-55` | Handle GPS Edge Cases | Signal loss, spoofing, tampering, delayed packets, battery drain and trusted-state protection | 🟡 `WAVE C / BLOCKED_BY_DEPENDENCY` | Requires US-48; integrates US-78 |

---

### 📦 Module 8 & 9: Delivery Operations & POD (US-56 to US-62)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :---: | :--- |
| `US-56` | Manage Delivery Orders | Order creation, item catalog, destination address | 🟢 `COMPLETE` | `DeliveryOrderServiceTest`, V43–V45 |
| `US-57` | Capture Proof of Delivery | Digital signatures, recipient verification, photos | 🟢 `COMPLETE` | `ProofOfDeliveryServiceTest`, V46 |
| `US-58` | Support Offline Proof of Delivery | Local POD queue, photo buffering, auto-sync | 🟢 `COMPLETE` | `OfflinePodSyncTest`, `offlinePod.spec.ts`, V47 |
| `US-59` | Manage Failed Deliveries | Return-to-base, non-delivery reasons, parcel holds | 🟢 `COMPLETE` | `FailedDeliveryServiceTest`, `failedDelivery.spec.ts`, V48 |
| `US-60` | Schedule Re-Delivery | Re-attempt scheduling, alternative windows | 🟢 `COMPLETE` | `RedeliveryPersistencePostgreSqlAcceptanceTest`, `redelivery.spec.ts`, V49 |
| `US-61` | Analyze Delivery Performance | On-time delivery metrics, driver delivery SLA | 🟢 `COMPLETE` | `DeliveryAnalyticsPersistencePostgreSqlAcceptanceTest`, `DeliveryAnalyticsServiceTest`, V50 |
| `US-62` | Handle Delivery Exceptions | Damaged at doorstep, recipient refusal, disputes | 🟢 `COMPLETE` | `DeliveryExceptionPersistencePostgreSqlAcceptanceTest`, `DeliveryExceptionConcurrencyPostgreSqlAcceptanceTest`, V51 |

---

### 🚲 Module 10: Last-Mile Delivery (US-63 to US-70)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :---: | :--- |
| `US-63` | Manage Delivery Zones | GeoJSON polygon zones, ray-casting PiP, priority overlap resolution, capacity, serviceability | 🟢 `COMPLETE` | `DeliveryZoneTest`, `DeliveryZoneServiceTest`, `DeliveryZoneControllerTest`, `DeliveryZonePersistencePostgreSqlAcceptanceTest`, `DeliveryZoneConcurrencyPostgreSqlAcceptanceTest`, `deliveryZones.spec.ts`, V52 |
| `US-64` | Manage Delivery Slots | Time-window capacity, cutoff enforcement, manager override, concurrent reservation | 🟢 `COMPLETE` | `DeliverySlotTest`, `DeliverySlotControllerTest`, `DeliverySlotPostgreSqlAcceptanceTest`, `DeliverySlotConcurrencyPostgreSqlAcceptanceTest`, `deliverySlots.spec.ts`, V53 |
| `US-65` | Manage Riders | Courier rider registry, zone eligibility, shift scheduling, concurrency-safe assignment & override | 🟢 `COMPLETE` | `DeliveryRiderTest`, `DeliveryRiderControllerTest`, `DeliveryRiderPostgreSqlAcceptanceTest`, `DeliveryRiderConcurrencyPostgreSqlAcceptanceTest`, `deliveryRiders.spec.ts`, V54 |
| `US-66` | Batch Delivery Orders | Automated order clustering for urban routes, manual batching, zone & capacity constraints, rider assignment | 🟢 `COMPLETE` | `DeliveryBatchTest`, `DeliveryBatchServiceTest`, `DeliveryBatchControllerTest`, `DeliveryBatchPostgreSqlAcceptanceTest`, `DeliveryBatchConcurrencyPostgreSqlAcceptanceTest`, `deliveryBatches.spec.ts`, `DeliveryBatchListPage.test.tsx`, V55 |
| `US-67` | Calculate Last-Mile ETA | Multi-modal last-mile routing ETA, SLA risk detection, stop service buffers, cache & event publication | 🟢 `COMPLETE` | `HEURISTIC_ONLY`; V56 US-67 migration, V57 current head; final acceptance passed: Maven 1,195/0/0/15, architecture 40/40, real Chromium 6/6. |
| `US-68` | Handle Last-Mile Exceptions | Planner orchestration of rider no-show, attempts, address/access, contactless and cash-dispute outcomes through existing Delivery capabilities | 🟢 `COMPLETE` | Final acceptance passed: read-only Planner, Maven 1,200/0/0/15, architecture 45/45, real Chromium 3/3, retained US-67 6/6; no migration or duplicate exception model. |
| `US-69` | Receive Delivery Notifications | Delivery-event-driven customer Email/SMS with preferences, durable attempts, and operator history | 🟢 `COMPLETE` | Final acceptance rerun passed after trigger remediation: READY=0, committed DISPATCHED=one per active member, removed/rollback=0; Maven 1,223/0/0/15, architecture 42/42, Chromium 7/7, and all security/privacy/Notification gates PASS. |
| `US-70` | Use Customer Self-Service | Token-scoped tracking, preferences, issue/feedback submission, and non-binding redelivery requests | 🟢 `COMPLETE` | Final acceptance passed: V59/V1→V59; Maven 1,238/0/0/15 (4:56); focused backend/security 28/28; PostgreSQL 4/4; architecture 42/42; frontend Vitest 259/259; real PostgreSQL-backed Chromium 9/9; introduced lint errors 0. No direct scheduling, slot booking, operator shell, raw-token persistence, Rider data, or POD evidence exposure. |

---

### ⚙️ Module 11: Cross-Cutting Platform & Security (US-71 to US-78)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :---: | :--- |
| `US-71` | Support Offline Data Sync | Generic IndexedDB sync framework, idempotency | 🟢 `COMPLETE` | `OfflineSyncCoordinatorTest`, V29 |
| `US-72` | Enforce Compliance | Vehicle, driver, cargo/hazmat, tax, regional and retention decisions | 🟡 `WAVE D / BLOCKED_BY_DEPENDENCY` | Requires typed domain facts and US-47 billing facts |
| `US-73` | Manage External Integrations | Provider-neutral endpoint/configuration, mapping, exchange, status, retry and error handling | 🟢 `COMPLETE` | Independent final acceptance PASS: V61, focused 24/24, regressions 40/40, Maven 1,276/0/0/15, architecture 44/44, real Chromium 6/6; controlled outbound JSON-file capability only |
| `US-74` | Manage Security | JWT authentication, multi-tenant RBAC | 🟢 `COMPLETE` | `SecurityAccessTest`, `TenantFilterTest` |
| `US-75` | Maintain Audit and Reports | Comprehensive audit log trail, CSV exports | 🟢 `COMPLETE` | `AuditLogTest`, `AuditReportTest` |
| `US-76` | Support Mobile Operations | Driver, dispatcher and delivery mobile workflows with offline, camera/signature, push and device health | 🟡 `WAVE D / BLOCKED_BY_DEPENDENCY` | PWA/native and device/provider decisions required |
| `US-77` | Manage Notification Rules | Rule-based alerts, multi-channel dispatch | 🟢 `COMPLETE` | `NotificationRuleTest`, V28 |
| `US-78` | Manage Operational Exceptions | Cross-domain classification, assignment, severity, SLA, escalation, corrective action, RCA and closure | 🟢 `COMPLETE` | Final acceptance PASS: V62; focused 41/41; concurrency 6/6; PostgreSQL 3/3; regressions 84/84; Maven 1,296/0/0/15 in 05:06; architecture 46/46; Chromium 6/6; `docs/full-product/US-78-OPERATIONAL-EXCEPTIONS-FINAL-ACCEPTANCE-001.md` |

---

### 🧩 Module 12: Supporting Architecture & Resilience (US-79 to US-87)
| ID | User Story Title | Scope / Feature | Status | Verification Evidence |
| :---: | :--- | :--- | :--- | :--- |
| `US-79` | Manage Master Data | Locations, customer records, branches | 🟢 `COMPLETE` | `MasterDataTest`, V1 |
| `US-80` | Configure Workflows | Multi-step approval state machine definitions | 🟢 `COMPLETE` | `WorkflowEngineTest` |
| `US-81` | Manage Scheduling | Universal operational cron schedule engine | 🟢 `COMPLETE` | `ScheduleCoordinatorTest` |
| `US-82` | Use Operational Analytics | Governed dashboards/KPIs, predictive maintenance, forecasts, risk and recommendations | 🟡 `WAVE E / LATER_WAVE` | Actual and predictive results remain distinct |
| `US-83` | Manage Documents | Attachment storage, MIME type validation | 🟢 `COMPLETE` | `DocumentStorageTest` |
| `US-84` | Handle Global System Failures | Outage, replication lag, third-party downtime, queue backlog, clock drift, degraded mode and verified recovery | 🟡 `WAVE E / LATER_WAVE` | Code + infrastructure + runbook evidence required |
| `US-85` | Protect Data Integrity | Duplicate/orphan/mismatch/master-data detection, quarantine and audited owner correction | 🟡 `WAVE E / BLOCKED_BY_DEPENDENCY` | Requires US-48 for complete GPS/trip mismatch scope |
| `US-86` | Handle Operational Disruptions | Disaster/restriction/strike/border/demand constraints and coordinated replanning | 🟡 `WAVE E / BLOCKED_BY_DEPENDENCY` | Requires US-78 and tracking/routing/scheduling contracts |
| `US-87` | Detect User Risk | Explainable override, missing-field, fraud, shared-login and delayed-reporting controls | 🟡 `WAVE E / LATER_WAVE` | No opaque ML or automatic punitive decisions |

---

## 🎯 4. Immediate Execution Queue

```
Current Status: 67 / 87 COMPLETE; 20 stories remain across Waves B–E
Queue Head:     US-37-FUEL-PERFORMANCE-IMPLEMENTATION-001
```

1. **Wave A — Integration and exception-control foundations:** 2 / 2 COMPLETE / CLOSED (US-73 and US-78).
2. **Wave B — Fuel control and financial links:** US-37, US-35, US-38, US-46, US-47.
3. **Wave C — GPS and telematics:** US-48, then US-49/50/51/52/55, then US-53, then US-54.
4. **Wave D — Compliance and field mobility:** US-72, US-76.
5. **Wave E — Analytics, integrity, resilience, disruption and user risk:** US-85, US-84, US-87, US-82, US-86.
6. After 87/87: `FULL-SOURCE-PARITY-AUDIT-001`, then `FULL-PLATFORM-END-TO-END-ACCEPTANCE-001` after authorized parity disposition.

Detailed dependencies, readiness/done gates, scores, rollback plans, source-parity links and exact 65→87 acceptance order are governed by `docs/roadmap/FULL-PRODUCT-87-STORY-COMPLETION-ROADMAP.md`.

---

## 📚 5. Authoritative Traceability Index

- **Requirements Baseline:** `docs/requirements/Traspotation & logistic.docx`
- **UML Specifications:** `docs/requirements/US-01-US-10-UseCase-Activity-Sequence-Diagrams.md` through `US-81-US-90`
- **Current MVP Status:** [`docs/mvp/MVP-current-status.md`](file:///home/hasitha-wijerathna/Documents/transport-logistics-modulith/transport-logistics-modulith/docs/mvp/MVP-current-status.md)
- **Delivery Contracts:** `docs/mvp/MVP-1.3-DELIVERY-OPERATIONS-CONTRACT-001.md`
- **Accepted Acceptance Reports:**
  - `US-56`: `docs/mvp/MVP-1.3-US56-DELIVERY-ORDERS-FINAL-ACCEPTANCE-001.md`
  - `US-57`: `docs/mvp/MVP-1.3-US57-PROOF-OF-DELIVERY-FINAL-ACCEPTANCE-001.md`
  - `US-58`: [`docs/mvp/MVP-1.3-US58-OFFLINE-POD-FINAL-ACCEPTANCE-001.md`](file:///home/hasitha-wijerathna/Documents/transport-logistics-modulith/transport-logistics-modulith/docs/mvp/MVP-1.3-US58-OFFLINE-POD-FINAL-ACCEPTANCE-001.md)
  - `US-59`: `docs/mvp/MVP-1.3-US59-FAILED-DELIVERIES-FINAL-ACCEPTANCE-001.md`
  - `US-60`: `docs/mvp/MVP-1.3-US60-REDELIVERY-FINAL-ACCEPTANCE-001.md`
  - `US-61`: `docs/mvp/MVP-1.3-US61-ANALYTICS-FINAL-ACCEPTANCE-001.md`
  - `US-62`: `docs/mvp/MVP-1.3-US62-DELIVERY-EXCEPTIONS-FINAL-ACCEPTANCE-001.md`
  - `US-63`: `docs/mvp/MVP-1.4-US63-DELIVERY-ZONES-FINAL-ACCEPTANCE-001.md`
  - `US-64`: `docs/mvp/MVP-1.4-US64-DELIVERY-SLOTS-FINAL-ACCEPTANCE-001.md`
  - `US-65`: `docs/mvp/MVP-1.4-US65-RIDERS-FINAL-ACCEPTANCE-001.md`
  - `US-66`: `docs/mvp/MVP-1.4-US66-BATCH-DELIVERY-ORDERS-FINAL-ACCEPTANCE-001.md`
  - `US-67`: `docs/mvp/MVP-1.4-US67-LAST-MILE-ETA-FINAL-ACCEPTANCE-001.md`
  - `US-68`: `docs/mvp/MVP-1.4-US68-LAST-MILE-EXCEPTIONS-FINAL-ACCEPTANCE-001.md`
  - `US-69`: `docs/mvp/MVP-1.4-US69-DELIVERY-NOTIFICATIONS-FINAL-ACCEPTANCE-001.md`
  - `US-70`: `docs/mvp/MVP-1.4-US70-CUSTOMER-SELF-SERVICE-FINAL-ACCEPTANCE-001.md`
  - `US-73`: `docs/full-product/US-73-EXTERNAL-INTEGRATIONS-FINAL-ACCEPTANCE-001.md`
  - `US-78`: `docs/full-product/US-78-OPERATIONAL-EXCEPTIONS-FINAL-ACCEPTANCE-001.md`
- **Frozen Product Decisions:**
  - `US-78`: `docs/full-product/US-78-OPERATIONAL-EXCEPTIONS-PRODUCT-DECISIONS-001.md`
- **Implementation Reports:**
  - `US-59`: `docs/mvp/MVP-1.3-US59-FAILED-DELIVERIES-IMPLEMENTATION-001.md`
  - `US-60`: `docs/mvp/MVP-1.3-US60-REDELIVERY-IMPLEMENTATION-001.md`
  - `US-62`: `docs/mvp/MVP-1.3-US62-DELIVERY-EXCEPTIONS-IMPLEMENTATION-001.md`
  - `US-63`: `docs/mvp/MVP-1.4-US63-DELIVERY-ZONES-IMPLEMENTATION-001.md`
  - `US-64`: `docs/mvp/MVP-1.4-US64-DELIVERY-SLOTS-IMPLEMENTATION-001.md`
  - `US-65`: `docs/mvp/MVP-1.4-US65-RIDERS-IMPLEMENTATION-001.md`
  - `US-66`: `docs/mvp/MVP-1.4-US66-BATCH-DELIVERY-ORDERS-IMPLEMENTATION-001.md`
  - `US-67`: `docs/mvp/MVP-1.4-US67-LAST-MILE-ETA-IMPLEMENTATION-001.md`
  - `US-68`: `docs/mvp/MVP-1.4-US68-LAST-MILE-EXCEPTIONS-IMPLEMENTATION-001.md`
  - `US-69`: `docs/mvp/MVP-1.4-US69-DELIVERY-NOTIFICATIONS-IMPLEMENTATION-001.md`
  - `US-69 remediation`: `docs/mvp/MVP-1.4-US69-DELIVERY-NOTIFICATIONS-ACCEPTANCE-REMEDIATION-001.md`
- **Central Knowledge Base:** US-73 implementation and technical-closure state is synchronized through commit `4582ba4`; final-acceptance facts are synchronized in the US-73 acceptance cycle.
