# US-01 Vehicle Master Management — Hardening Specification & Verification

**Task ID:** US01-HARDEN-001  
**Story:** US-01 — Manage Vehicle Master  
**Actor:** Fleet Manager  
**Date:** August 23, 2026  
**Status:** COMPLETE  

---

## 1. Executive Summary
This document specifies the comprehensive audit, correction, completion, and hardening performed on the **US-01: Manage Vehicle Master** vertical. The implementation ensures that vehicle records adhere to strict domain invariants, unique identity constraints, master data hierarchy validation, monotonic meter protections, centralized state transition policies, robust RBAC security, and automated verification across backend and frontend layers.

---

## 2. Original US-01 Scope
The original US-01 requirements called for the Fleet Manager to create, view, update, classify, track, and manage Vehicle master records and their operational/lifecycle status while preserving data validity, uniqueness, and Fleet business rules.

---

## 3. Problems Found
1. **Unchecked Domain Invariants:** `Vehicle` record accepted negative meters, negative capacity, and invalid manufacture years without domain errors.
2. **Raw Database Constraint Exceptions on Duplicates:** Duplication of registration, chassis, or engine numbers caused unhandled 500 exceptions instead of standard 409 Conflict.
3. **Master Data Dependency Bypass:** Vehicle Type and Category relationships were not validated at the service layer, allowing mismatched category/type pairs.
4. **Unvalidated Status Transitions:** `VehicleService.update()` allowed arbitrary status jumps (e.g. `MAINTENANCE` -> `ALLOCATED`) bypassing operational rules.
5. **No Dedicated Backend Tests:** Zero unit tests existed for `VehicleService` or `FleetController` vehicle CRUD endpoints.
6. **Frontend Dropdown Inconsistencies:** Vehicle Type selector did not filter options based on the chosen Category.

---

## 4. Current-System Gap Matrix
A full gap matrix was documented in `docs/mvp/US01-VEHICLE-MASTER-GAP-AUDIT.md`. Key items resolved include:
- Centralized duplicate detection (`VEHICLE_REGISTRATION_DUPLICATE`, `VEHICLE_CHASSIS_DUPLICATE`, `VEHICLE_ENGINE_DUPLICATE`).
- Master data reference checks (`VEHICLE_MASTER_REFERENCE_INVALID`).
- Status transition validation (`VEHICLE_STATUS_TRANSITION_INVALID`).
- Non-retrograde meter checks (`VEHICLE_DATA_INVALID`).

---

## 5. Corrected Domain Model
The `Vehicle` domain model enforces:
- `id`: Non-null UUID.
- `registrationNumber`: Trimmed, uppercase, required string.
- `chassisNumber`: Optional trimmed unique string.
- `engineNumber`: Optional trimmed unique string.
- `categoryId`: Non-null UUID referencing active `VehicleCategory`.
- `typeId`: Non-null UUID referencing active `VehicleType` belonging to `categoryId`.
- `ownershipType`: Normalized string (`COMPANY_OWNED`, `LEASED`, `RENTED`, `CONTRACTED`). Default: `COMPANY_OWNED`.
- `operationalStatus`: Normalized string (`AVAILABLE`, `ALLOCATED`, `MAINTENANCE`, `UNDER_MAINTENANCE`, `OUT_OF_SERVICE`, `BROKEN_DOWN`). Default: `AVAILABLE`.
- `capacityKg`: Non-negative floating-point number.
- `currentOdometerKm`: Non-negative floating-point number.
- `engineHours`: Non-negative floating-point number.
- `manufactureYear`: Integer bounded between 1900 and current year + 1.
- `active`: Boolean flag separating active operational state from lifecycle retirement/deactivation.

---

## 6. Identity Rules
- **Registration Number:** Authoritative primary operational identifier. Unique across all active and inactive records. Trimming and uppercase normalization applied.
- **Chassis Number / VIN:** Optional hardware identifier. When present, uniqueness is enforced across vehicles. Supports standard 17-char VINs and non-standard equipment serials.
- **Engine Number:** Optional motor identifier. When present, uniqueness is enforced across vehicles.
- **QR / Asset Tag:** Handled via unique UUID primary key and registration number without requiring breaking schema modifications.

---

## 7. Ownership Rules
Standardized ownership types supported:
- `COMPANY_OWNED` (default)
- `LEASED`
- `RENTED`
- `CONTRACTED`

---

## 8. Classification Rules
Hierarchical classification:
1. `VehicleCategory` (e.g. Trucks, Vans, Prime Movers).
2. `VehicleType` (e.g. Box Truck, Flatbed, Reefer) must have `category_id` equal to the vehicle's `categoryId`.

---

## 9. Status Model
The system cleanly separates two distinct lifecycle dimensions:
1. **Operational Status (`operationalStatus`):** Current real-time operational availability (`AVAILABLE`, `ALLOCATED`, `MAINTENANCE`, `UNDER_MAINTENANCE`, `OUT_OF_SERVICE`, `BROKEN_DOWN`).
2. **Lifecycle State (`active`):** Master record lifecycle status (`active = true` for active fleet assets, `active = false` for deactivated/retired assets).

---

## 10. Status Transition Rules
| From Status | Allowed Target Statuses | Notes |
|---|---|---|
| `AVAILABLE` | `ALLOCATED`, `MAINTENANCE`, `UNDER_MAINTENANCE`, `OUT_OF_SERVICE`, `BROKEN_DOWN` | Standard assignment, scheduled maintenance, or breakdown |
| `ALLOCATED` | `AVAILABLE`, `MAINTENANCE`, `UNDER_MAINTENANCE`, `OUT_OF_SERVICE`, `BROKEN_DOWN` | Trip completion, mid-trip breakdown, or out-of-service event |
| `MAINTENANCE` / `UNDER_MAINTENANCE` | `AVAILABLE`, `OUT_OF_SERVICE` | Cleared after service completion or decommissioned |
| `OUT_OF_SERVICE` / `BROKEN_DOWN` | `AVAILABLE`, `MAINTENANCE`, `UNDER_MAINTENANCE` | Recovered to available or towed into maintenance |

Any other transition throws `ConflictException("VEHICLE_STATUS_TRANSITION_INVALID")` (HTTP 409).

---

## 11. API Contract
- `POST /api/vehicles` — Creates vehicle (201 Created).
- `GET /api/vehicles` — Lists all vehicles (200 OK).
- `GET /api/vehicles/{id}` — Retrieves vehicle details (200 OK or 404 Not Found).
- `PUT /api/vehicles/{id}` — Updates vehicle specifications (200 OK, 400 Bad Request, 404 Not Found, 409 Conflict).
- `DELETE /api/vehicles/{id}` — Soft deactivates vehicle (`active = false`, 200 OK).

---

## 12. Validation Rules
- Mandatory: `registrationNumber`, `categoryId`, `typeId`.
- Range: `capacityKg >= 0`, `currentOdometerKm >= 0`, `engineHours >= 0`, `1900 <= manufactureYear <= currentYear + 1`.
- Monotonicity: `currentOdometerKm` and `engineHours` cannot be decreased via normal `PUT /vehicles/{id}` update.

---

## 13. Error Codes
- `VEHICLE_NOT_FOUND` (404)
- `VALIDATION_ERROR` (400)
- `VEHICLE_DATA_INVALID` (400)
- `VEHICLE_MASTER_REFERENCE_INVALID` (400)
- `VEHICLE_REGISTRATION_DUPLICATE` (409)
- `VEHICLE_CHASSIS_DUPLICATE` (409)
- `VEHICLE_ENGINE_DUPLICATE` (409)
- `VEHICLE_STATUS_TRANSITION_INVALID` (409)
- `VEHICLE_RETIREMENT_BLOCKED` (409)

---

## 14. Security / RBAC
- `VEHICLE_VIEW`: Permitted for `GET /vehicles`, `GET /vehicles/{id}`, availability and document lookups.
- `VEHICLE_CREATE`: Permitted for `POST /vehicles`.
- `VEHICLE_UPDATE`: Permitted for `PUT /vehicles/{id}`.
- `VEHICLE_STATUS_UPDATE`: Permitted for `DELETE /vehicles/{id}` (deactivation).

---

## 15. Database Integrity
- Primary key `id UUID PRIMARY KEY`.
- Unique constraint `registration_number VARCHAR(80) UNIQUE NOT NULL`.
- Foreign keys `fk_vehicle_category` (`category_id` -> `vehicle_category(id)`) and `fk_vehicle_type` (`type_id` -> `vehicle_type(id)`).
- Flyway migrations V1–V29 preserved intact without modification.

---

## 16. Frontend Behavior
- `ResourceListPage.tsx` and `ResourceEditorModal.tsx` filter Vehicle Types dynamically based on the selected Category.
- Duplicate conflict errors (`VEHICLE_REGISTRATION_DUPLICATE`, etc.) map cleanly to input fields with human-readable error messages.
- Protected actions (Create, Edit, Deactivate) respect user permissions via RBAC tags.

---

## 17. Auditability
- Vehicle deactivation preserves all historical operational logs (Fuel Issues, Readings, Trips, Lubricant Logs, Maintenance Schedules).
- Physical row deletion is forbidden; soft lifecycle deactivation is enforced.

---

## 18. Automated Tests
1. **Domain Tests:** `VehicleDomainTest` (invariants, defaults, normalization, status state transitions).
2. **Service Tests:** `VehicleServiceTest` (CRUD, duplicate detection, master data hierarchy checks, non-retrograde meters, active allocation guards).
3. **Controller Tests:** `FleetControllerVehicleCrudTest` (HTTP status codes, DTO validation, 409 conflict mappings, 404 responses).
4. **Persistence Integration Tests:** `VehicleJpaRepositoryIntegrationTest` (case-insensitive duplicate detection, custom query methods).
5. **E2E Playwright Tests:** `vehicles.spec.ts` (creation, validation, duplicate rejection, details viewing).

---

## 19. UML Corrections
- `docs/uml/US-01-use-case.puml` (Corrected PlantUML use case diagram with proper `<<include>>` relationships).
- `docs/uml/US-01-activity.puml` (Detailed operational flow with decision gates).
- `docs/uml/US-01-sequence.puml` (Multi-tier interaction sequence).

---

## 20. Deferred Scope
- Unbounded pagination across all fleet listing endpoints is scheduled under Release Candidate task `RC-HARDEN-007`.
- Hardware telemetry integration deferred to IoT Phase 3.

---

## 21. Verification Results
- All unit, integration, and architecture tests pass without regressions.
- Cross-browser Playwright tests execute cleanly.

---

## 22. Final US-01 Decision
**COMPLETE** — US-01 Manage Vehicle Master vertical is fully audited, corrected, completed, and hardened.
