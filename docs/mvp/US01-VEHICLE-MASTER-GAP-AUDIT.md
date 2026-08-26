# US-01 Vehicle Master — Implementation Gap Audit

**Task ID:** US01-HARDEN-001  
**Story:** US-01 — Manage Vehicle Master  
**Actor:** Fleet Manager  
**Date:** August 23, 2026  
**Status:** AUDIT COMPLETED  

---

## 1. Executive Summary

This audit evaluates the current implementation of **US-01: Manage Vehicle Master** across the Modular Monolith backend, React frontend, Flyway database schema, domain invariants, validation rules, security configuration, and test suites against the US-01 core intent and release criteria.

While baseline CRUD endpoints exist for vehicles (`GET/POST /vehicles`, `GET/PUT/DELETE /vehicles/{id}`), significant gaps were identified:
1. **Domain Invariants & Entity Integrity:** `Vehicle` is a flat Java record without domain validation or business invariants.
2. **Duplicate Conflict Handling:** Registration number, chassis number, and engine number uniqueness checks are not handled at the application/domain layer, allowing raw database constraint violations instead of clean RFC 7807 / `ApiError` 409 Conflict responses.
3. **Master Data Dependency Validation:** `VehicleService` does not validate that `categoryId` exists/is active, that `typeId` exists/is active, or that `typeId` belongs to the specified `categoryId`.
4. **Odometer & Engine Hours Invariants:** Negative values and illicit backward modifications on update are not prevented in `VehicleService`.
5. **Manufacture Year Range:** No boundary check ensures manufacture year is realistic (e.g., 1900 to current year + 1).
6. **Status Transition Validation:** `VehicleService.update()` permits arbitrary operational status mutations without enforcing valid transition rules or preventing invalid states when dependencies/maintenance conflict.
7. **Testing Coverage:** Zero unit tests existed for `VehicleService`, zero domain tests for `Vehicle`, and zero controller tests for `/vehicles` CRUD endpoints.
8. **Frontend Dependent Dropdowns & Form UX:** Vehicle Type dropdown does not reactively filter based on selected Vehicle Category, and duplicate error codes from the backend are not mapped to specific field errors.

---

## 2. Comprehensive Capability Gap Matrix

| Capability | Required | Current Backend | Current Frontend | Database | Validation | Security | Tests | Gap | Severity | Action |
|---|---|---|---|---|---|---|---|---|---|---|
| **Vehicle Creation** | Create valid vehicle master record with 201 Created | `POST /vehicles` in `FleetController`, `VehicleService.create()` | `ResourceListPage` / `ResourceEditorModal` | `vehicle` table (`V1__baseline.sql`) | Incomplete DTO validation, no domain checks | `VEHICLE_CREATE` | Missing unit/controller tests | Missing domain validation & conflict mapping | **P1** | Add domain invariants, master-data checks, and unit tests |
| **Vehicle Retrieval** | Retrieve single vehicle by UUID | `GET /vehicles/{id}` in `FleetController`, `VehicleService.get()` | `ResourceListPage` drawer details | Primary key index | UUID path parsing | `VEHICLE_VIEW` | No direct controller test | No automated test | **P2** | Add controller test |
| **Vehicle Update** | Update mutable vehicle specifications | `PUT /vehicles/{id}` in `FleetController`, `VehicleService.update()` | `ResourceEditorModal` (method: `put`) | Row update on `vehicle` | Missing entity existence check & invariant enforcement | `VEHICLE_UPDATE` | No direct unit/controller tests | Blind save without pre-existence check or hierarchy validation | **P1** | Validate target existence, duplicate exclusions, hierarchy, and status |
| **Vehicle Listing** | List all vehicles with operational status | `GET /vehicles` in `FleetController`, `VehicleService.list()` | `ResourceListPage` table | Full table scan | None | `VEHICLE_VIEW` | Covered in Journey tests | Unbounded list (to be hardened under RC) | **P2** | Maintain endpoint stability |
| **Vehicle Search / Filter** | Filter by registration, status, category | Client-side table search & filter | Ant Design Table search/filter | None | Client-side | `VEHICLE_VIEW` | E2E journey tests | None | **P3** | Preserve UI filtering |
| **Vehicle Registration** | Trimmed, normalized, unique registration number | String field, no normalization or domain trim | Text input field | `VARCHAR(80) UNIQUE NOT NULL` | `@NotBlank` on DTO only | `VEHICLE_CREATE/UPDATE` | E2E tests | Case/trim normalization missing; duplicate throws 500 | **P1** | Add trimming/normalization and 409 Conflict duplicate check |
| **Chassis Number (VIN)** | Optional unique chassis identifier | String field, no uniqueness check in service | Text input field | `VARCHAR(120)` | None | `VEHICLE_CREATE/UPDATE` | None | Service does not enforce duplicate checks for non-null chassis | **P2** | Add uniqueness validation in domain/service |
| **Engine Number** | Optional unique engine identifier | String field, no uniqueness check in service | Text input field | `VARCHAR(120)` | None | `VEHICLE_CREATE/UPDATE` | None | Service does not enforce duplicate checks for non-null engine | **P2** | Add uniqueness validation in domain/service |
| **Vehicle Category** | Valid active reference to `vehicle_category` | `UUID categoryId` field | Category select dropdown | `category_id UUID NOT NULL`, FK in V10 | `@NotNull` on DTO only | `VEHICLE_CREATE/UPDATE` | None | No verification that category exists and is active | **P1** | Validate category existence via `VehicleCategoryRepository` |
| **Vehicle Type** | Valid active type belonging to selected category | `UUID typeId` field | Type select dropdown | `type_id UUID NOT NULL`, FK in V10 | `@NotNull` on DTO only | `VEHICLE_CREATE/UPDATE` | None | No verification that type belongs to category | **P1** | Validate type existence and category hierarchy match |
| **Manufacturer & Model** | Optional text metadata | String fields | Text inputs | `VARCHAR(120)` | None | `VEHICLE_CREATE/UPDATE` | None | None | **P3** | Maintain trimmed strings |
| **Manufacture Year** | Reasonable year (1900..current+1) | `Integer manufactureYear` | Number input | `INT` | None on backend | `VEHICLE_CREATE/UPDATE` | None | No range validation on backend | **P2** | Validate range (1900 to Clock.year + 1) |
| **Ownership Classification** | Categorized ownership (`COMPANY_OWNED`, `LEASED`, `RENTED`, `CONTRACTED`) | String field with default `COMPANY_OWNED` | Select dropdown | `VARCHAR(40) NOT NULL` | Controller fallback | `VEHICLE_CREATE/UPDATE` | E2E tests | Enum normalization not validated | **P2** | Enforce valid ownership types |
| **Operational Status** | Explicit operational state (`AVAILABLE`, `ALLOCATED`, `MAINTENANCE`, `OUT_OF_SERVICE`, `BROKEN_DOWN`) | String field with default `AVAILABLE` | Select dropdown & status tags | `VARCHAR(40) NOT NULL` | String fallback | `VEHICLE_CREATE/UPDATE` | Journey tests | Transitions not validated on update | **P1** | Enforce valid operational status transitions |
| **Lifecycle Status (Active/Inactive/Retired)** | Soft lifecycle state separating operational availability from retirement | `boolean active` flag; `deactivate()` method | State tag & Deactivate action | `active BOOLEAN NOT NULL` | None | `VEHICLE_STATUS_UPDATE` | None | Deactivation does not check active trip allocations | **P1** | Guard deactivation against active allocation |
| **Odometer & Engine Hours** | Non-negative readings; initial values on create; non-decreasing | `Double currentOdometerKm`, `Double engineHours` | Number inputs | `DOUBLE PRECISION` | Frontend `positive: true` | `VEHICLE_CREATE/UPDATE` | None | Negative values accepted by backend; update can corrupt readings | **P1** | Enforce non-negative invariants; protect against retrograde edits |
| **Capacity (kg)** | Non-negative carrying capacity | `Double capacityKg` | Number input | `DOUBLE PRECISION` | Frontend `positive: true` | `VEHICLE_CREATE/UPDATE` | E2E tests | Negative values accepted on backend | **P2** | Enforce capacity >= 0 |
| **QR Code / Asset Identification** | Stable unique identification for physical tagging / scanning | Handled via UUID / registration number | Rendered in detail descriptions | UUID primary key & registration number | Built-in | `VEHICLE_VIEW` | None | No separate QR contract documentation | **P2** | Document QR contract and verify stability |
| **Duplicate Conflict Detection** | Centralized duplicate error responses | None (throws raw JPA constraint exceptions) | Raw toast alert on error | Unique DB constraints | Missing | N/A | None | Raw 500 on duplicate registration | **P0** | Implement service duplicate checks throwing `ConflictException` |
| **Concurrent Updates** | Prevent lost updates | No `@Version` column in baseline schema | None | Baseline V1 has no version column | None | `VEHICLE_UPDATE` | None | Concurrency handled via pessimistic lock on allocation; record updates unversioned | **P2** | Document baseline V1 constraint and add existence checks |
| **Deletion Semantics** | Non-destructive soft deactivation / lifecycle retirement | `DELETE /vehicles/{id}` sets `active=false` | Deactivate action button | Preserves all rows and FK references | None | `VEHICLE_STATUS_UPDATE` | E2E test | Endpoint retains historical audit safely | **P3** | Preserve soft deactivation semantics |
| **Security & RBAC** | Method-level & path-level permission checks | `SecurityConfig` enforces `VEHICLE_VIEW`, `VEHICLE_CREATE`, `VEHICLE_UPDATE`, `VEHICLE_STATUS_UPDATE` | UI hides buttons for unauthorized roles | N/A | Spring Security | Fully configured | RBAC integration tests | Complete | **P3** | Ensure all new methods adhere to RBAC |
| **Backend Testing** | Comprehensive unit, domain, service, and controller tests | Missing `VehicleServiceTest`, `FleetControllerTest` vehicle tests | Component tests exist | H2 / Postgres tests | N/A | N/A | Missing | 0 dedicated unit tests for US-01 | **P0** | Create full suite of domain, service, and controller tests |
| **Frontend Dependent Dropdowns** | Filter Vehicle Type by selected Category | None (shows all types) | Static select options | N/A | None | N/A | None | Incompatible type/category selectable | **P2** | Add dependent category filtering in UI |
| **Frontend Error Mapping** | Display field-specific validation and duplicate errors | Global toast error only | Generic form error handler | N/A | Client-side zod | N/A | Playwright tests | Duplicate registration not mapped to field | **P2** | Enhance error mapping for duplicate conflicts |

---

## 3. Prioritized Gap Summary

### P0 (Critical - Blockers for Correctness & Stability)
- **GAP-US01-001:** Missing duplicate conflict detection in `VehicleService` for `registrationNumber`, `chassisNumber`, and `engineNumber`, leading to raw 500 exceptions instead of standard 409 Conflict (`VEHICLE_REGISTRATION_DUPLICATE`, `VEHICLE_CHASSIS_DUPLICATE`, `VEHICLE_ENGINE_DUPLICATE`).
- **GAP-US01-002:** Total lack of dedicated backend unit tests for `Vehicle` domain, `VehicleService`, and `FleetController` vehicle CRUD endpoints.

### P1 (High - Business Invariants & Integrity)
- **GAP-US01-003:** Missing Master Data reference and hierarchy validation: `categoryId` existence, `typeId` existence, and `type.categoryId == categoryId` consistency.
- **GAP-US01-004:** Missing domain invariants for `manufactureYear` (1900..now+1), `capacityKg` (>= 0), `currentOdometerKm` (>= 0), and `engineHours` (>= 0).
- **GAP-US01-005:** Unvalidated operational status transitions on `Vehicle.update()` and unverified deactivation guard against active allocations.
- **GAP-US01-006:** `VehicleService.update()` blind overwrite without validating target entity existence.

### P2 (Medium - Usability, Contract & Robustness)
- **GAP-US01-007:** Frontend Vehicle Type selection does not reactively filter based on selected Vehicle Category.
- **GAP-US01-008:** Frontend duplicate error response mapping to specific form inputs.
- **GAP-US01-009:** Missing repository integration tests for Vehicle persistence and query methods.

### P3 (Low - Documentation & Minor Polish)
- **GAP-US01-010:** Formalize PlantUML use case, activity, and sequence diagrams for US-01.
