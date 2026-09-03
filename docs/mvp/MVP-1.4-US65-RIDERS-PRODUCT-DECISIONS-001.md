# Product Decisions and Domain Contract: US-65 — Manage Riders

## 1. Metadata & Status
- **Task ID**: `MVP-1.4-US65-RIDERS-PRODUCT-DECISIONS-001`
- **Story ID**: `US-65`
- **Story Title**: `Manage Riders`
- **Milestone / Release Band**: `MVP 1.4 — Last-Mile Delivery & Customer Experience`
- **Product Decision Status**: **`FROZEN`**
- **Implementation Status**: **`NOT_STARTED`**
- **Authoritative Baseline**:
  - `US-63` (Manage Delivery Zones): **`COMPLETE`**
  - `US-64` (Manage Delivery Slots): **`COMPLETE`**
  - `MVP 1.4`: **`2 / 8 COMPLETE`**
  - `Overall Accounting`: **`59 / 87 COMPLETE`**, **`28 / 87 DEFERRED`**, **`87 TOTAL`**
  - Current Accepted Flyway Head: `V53`
  - Target Migration for US-65: `V54__delivery_riders_us65.sql` (to be created during implementation)

---

## 2. Precondition Gate
- `docs/mvp/MVP-1.4-US64-DELIVERY-SLOTS-FINAL-ACCEPTANCE-001.md` verified: **`US-64 COMPLETE`**, `MVP 1.4: 2 / 8 COMPLETE`, `Overall: 59 / 87 COMPLETE`, `Flyway: V53 PASS`.
- Precondition check passed.

---

## 3. Source Authority & Requirements Extraction
- **Primary Source**: `docs/requirements/US-61-US-70-UseCase-Activity-Sequence-Diagrams.md` (US-65 — Manage Riders).
- **Primary Actor**: `Last-Mile Planner` (also accessible to `Delivery Dispatcher` / `Delivery Manager`).
- **Goal**: Onboard and manage gig or full-time riders with identity, shifts, and availability so valid riders can be assigned to last-mile deliveries.
- **Authority Order**:
  1. Original Business Requirements & Use Case Diagrams
  2. Accepted Architecture & Multi-Tenancy ADRs (`00_CORE_ARCHITECTURE/`)
  3. Verified Domain Contracts (`Driver`, `DeliveryZone`, `DeliverySlot`, `DeliveryOrder`)
  4. Roadmap & Milestone prose.

---

## 4. Rider Definition & Driver Domain Boundary
- **Rider Concept**: A **Rider** is a delivery-specific operational workforce entity (`DeliveryRider`) residing in the `delivery` bounded context, representing an active courier resource capable of servicing last-mile delivery orders within defined delivery zones and shifts.
- **Relationship to Driver Profile (US-39..US-45)**:
  - `Driver` (in `fleet` module) owns the person/employee core profile (name, phone, email, licensing classes, medical fitness, drug tests, infractions).
  - `DeliveryRider` (in `delivery` module) **references** `driverId` (logical UUID) and owns last-mile delivery operational attributes: rider type (`FULL_TIME`, `GIG`, `CONTRACTOR`), operational status (`ACTIVE`, `INACTIVE`, `SUSPENDED`), primary delivery zone (`delivery_zone_id`), secondary eligible zones, shift/duty availability, and max concurrent workload capacity.
  - **No duplication of PII or compliance masters**: Name, contact details, and license verification are queried on-demand via the public `DriverEligibilityPort`.

---

## 5. Domain Ownership & Modulith Architecture
- **Module Owner**: `com.transportlogistics.app.delivery`.
- **Pure Domain Aggregate**: `DeliveryRider` (Aggregate Root) and `DeliveryRiderShift` (Entity/Value Object) located in `com.transportlogistics.app.delivery.domain.model`.
- **Cross-Module Ports**:
  - `DriverEligibilityPort`: Public port consumed by `delivery` to verify driver existence, active status, license compatibility, and medical/suspension clearance.
  - `DeliveryZoneLookupPort`: Existing accepted port consumed to validate zone existence, active status, and serviceability.
  - `DeliverySlotAvailabilityPort`: Public port consumed to validate slot binding if shift is slot-bound.

---

## 6. Rider Identity, Types & Lifecycle
- **Identifier**: `UUID id` (Primary Key, tenant-scoped).
- **Rider Code**: `String riderCode` (Human-readable unique code, e.g. `RDR-000101`, unique per tenant).
- **Driver Link**: `UUID driverId` (Unique per tenant for active riders: 1 active rider profile per driver per tenant).
- **Rider Type (`DeliveryRiderType`)**:
  - `FULL_TIME` — Company employed permanent last-mile courier.
  - `GIG` — On-demand / flex gig-economy courier.
  - `CONTRACTOR` — Third-party logistics partner courier.
- **Rider Lifecycle Status (`DeliveryRiderStatus`)**:
  - `ACTIVE` — Approved and operationally available for shift and order assignments.
  - `INACTIVE` — Off-boarded or temporarily deactivated. Cannot receive new shifts or order assignments.
  - `SUSPENDED` — Operationally blocked due to disciplinary, safety, or compliance reasons.
- **Computed Operational Availability (`DeliveryRiderAvailability`)**:
  - `AVAILABLE` — Active, currently on duty/shift, with remaining workload capacity.
  - `BUSY` — Active and on duty, but currently at maximum concurrent delivery capacity.
  - `OFF_DUTY` — Active, but currently not on an active shift / checked out.
  - `UNAVAILABLE` — Inactive or suspended.

---

## 7. Zone Eligibility Model
- **Primary Zone**: `UUID primaryZoneId` (Mandatory, foreign key to `delivery_zone(id, tenant_id)`).
- **Secondary Zones**: Stored in `delivery_rider_zone (tenant_id, rider_id, delivery_zone_id, created_at, created_by)` with composite tenant foreign keys.
- **Zone Serviceability Invariant**: When assigning a rider to a `DeliveryOrder`, the order's destination zone (`delivery_zone_id`) MUST match the rider's primary or secondary eligible zones, unless an authorized manager override (`DELIVERY_RIDER_OVERRIDE` permission + `overrideReason`) is provided.
- **Zone Inactivation Effect**: If a zone becomes inactive or non-serviceable, existing historical assignments remain intact for audit; future assignments to that zone are blocked.

---

## 8. Shifts, Availability & US-64 Slot Relationship
- **Shift Model**:
  - `delivery_rider_shift` table: `(id, tenant_id, rider_id, shift_date, start_time, end_time, delivery_slot_id [nullable], status, max_deliveries, created_at, updated_at)`.
  - Shift statuses: `SCHEDULED`, `ON_DUTY`, `COMPLETED`, `CANCELLED`.
  - A shift can optionally bind to a specific `delivery_slot_id` or represent a general working time window on a given date.
- **Duty State & Conflict Invariant**:
  - A rider cannot have overlapping `SCHEDULED` or `ON_DUTY` shifts on the same date.
- **Workload Capacity**:
  - `maxConcurrentDeliveries`: Configurable capacity limit per rider/shift (default: 5 concurrent orders).
  - During assignment, the system ensures `active_assigned_orders < maxConcurrentDeliveries`, preventing overload unless manager override is invoked.

---

## 9. DeliveryOrder Assignment & History Model
- **Assignment Ownership**: Assignment is executed as a delivery-centric command: `POST /api/v1/deliveries/{id}/assign-rider`.
- **Assignment History**:
  - `delivery_order_rider_assignment` table records full audit history: `(id, tenant_id, delivery_order_id, rider_id, assigned_at, assigned_by, unassigned_at, unassigned_by, status ['ACTIVE', 'COMPLETED', 'REASSIGNED', 'CANCELLED'], is_override, override_reason)`.
  - Partial unique index: `CREATE UNIQUE INDEX uk_active_delivery_order_rider ON delivery_order_rider_assignment (tenant_id, delivery_order_id) WHERE status = 'ACTIVE'`.
  - Foreign key on `delivery_order`: `ALTER TABLE delivery_order ADD COLUMN current_rider_id UUID;` with composite tenant foreign key `(current_rider_id, tenant_id) REFERENCES delivery_rider(id, tenant_id) ON DELETE SET NULL;`.
- **Reassignment Protocol**:
  - Reassigning an order from Rider A to Rider B atomically marks Rider A's assignment as `REASSIGNED` (recording `unassigned_at` and `unassigned_by`), decrements Rider A's active workload, creates an `ACTIVE` assignment for Rider B, updates `delivery_order.current_rider_id = Rider B.id`, and emits `DeliveryRiderReassignedEvent`.

---

## 10. Concurrency & Integrity Guarantees
- **Atomic Assignment Race**: Concurrency on the same `DeliveryOrder` is protected via pessimistic write locking on `delivery_order` and the partial unique index `uk_active_delivery_order_rider`. Exactly one thread succeeds; the other receives `409 Conflict (DELIVERY_ORDER_ALREADY_ASSIGNED)`.
- **Rider Overload Race**: Concurrency on rider workload capacity is protected via pessimistic write locking on `delivery_rider` (`findByIdAndTenantIdWithLock`).
- **Shift Overlap Race**: Shift creation validates non-overlapping intervals within an atomic transaction.

---

## 11. Multi-Tenancy & Security
- **Tenant Authority**: Strictly extracted from authenticated `SecurityContext` / JWT token (`TenantContextPort`). No client-supplied `tenant_id` accepted in request bodies or query params.
- **Composite Foreign Keys**: All tables (`delivery_rider`, `delivery_rider_zone`, `delivery_rider_shift`, `delivery_order_rider_assignment`) enforce composite `(id, tenant_id)` foreign keys.
- **Privacy & PII Projection**:
  - Delivery APIs only project safe operational driver data (`firstName`, `lastName`, `phone`, `employeeNumber`).
  - No medical records, drug test results, or sensitive personal identity numbers (NIC/SSN) are exposed in delivery responses.

---

## 12. RBAC Permissions Matrix
- `DELIVERY_RIDER_VIEW`: View rider roster, zones, shifts, and availability.
- `DELIVERY_RIDER_CREATE`: Onboard new delivery riders and link drivers.
- `DELIVERY_RIDER_UPDATE`: Update rider profile, zones, and shift schedules.
- `DELIVERY_RIDER_ACTIVATE`: Activate, deactivate, or suspend riders.
- `DELIVERY_RIDER_ASSIGN`: Assign or reassign riders to delivery orders.
- `DELIVERY_RIDER_OVERRIDE`: Cross-zone or over-capacity manager assignment override.

| Role | VIEW | CREATE | UPDATE | ACTIVATE | ASSIGN | OVERRIDE |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **ADMIN** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **DELIVERY_MANAGER** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **DISPATCHER** | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ |
| **PLANNER** | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| **VIEWER** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **DRIVER/RIDER** | ✅ (Self) | ❌ | ❌ | ❌ | ❌ | ❌ |

---

## 13. REST API Surface

```http
# Rider Management
POST   /api/v1/delivery-riders                  # Onboard/link rider
GET    /api/v1/delivery-riders                  # List riders (paginated, filter by zone/status/type)
GET    /api/v1/delivery-riders/{id}             # Get rider detail
PUT    /api/v1/delivery-riders/{id}             # Update rider metadata & zones
POST   /api/v1/delivery-riders/{id}/activate    # Activate rider
POST   /api/v1/delivery-riders/{id}/deactivate  # Deactivate rider
POST   /api/v1/delivery-riders/{id}/suspend     # Suspend rider

# Shifts & Availability
POST   /api/v1/delivery-riders/{id}/shifts      # Create shift
GET    /api/v1/delivery-riders/{id}/shifts      # List shifts
POST   /api/v1/delivery-riders/{id}/duty-status # Clock-in / Clock-out / On-duty toggle
GET    /api/v1/delivery-riders/available        # Query available riders for a zone/date/slot

# Order Assignment Commands (Canonical Delivery-Centric)
POST   /api/v1/deliveries/{id}/assign-rider     # Assign rider to delivery order
POST   /api/v1/deliveries/{id}/reassign-rider   # Reassign to another rider
POST   /api/v1/deliveries/{id}/unassign-rider   # Unassign rider
GET    /api/v1/deliveries/{id}/rider-history    # Get assignment history
```

---

## 14. Domain Events
- `DeliveryRiderCreatedEvent`: `(tenantId, riderId, driverId, riderCode, primaryZoneId, riderType, createdAt, actor)`
- `DeliveryRiderStatusChangedEvent`: `(tenantId, riderId, previousStatus, newStatus, updatedAt, actor)`
- `DeliveryRiderAssignedEvent`: `(tenantId, deliveryOrderId, riderId, assignmentId, isOverride, assignedAt, actor)`
- `DeliveryRiderReassignedEvent`: `(tenantId, deliveryOrderId, previousRiderId, newRiderId, assignmentId, reassignedAt, actor)`
- `DeliveryRiderUnassignedEvent`: `(tenantId, deliveryOrderId, riderId, unassignedAt, actor)`

---

## 15. Scope Boundaries & Exclusions
- **US-66 (Batching)**: Clustering multiple orders into routes/batches is strictly deferred to US-66.
- **US-67 (Last-Mile ETA)**: Real-time dynamic GPS ETA calculation is deferred to US-67.
- **US-68 (Exceptions)**: In-flight delivery exception management is handled in US-62 / US-68.
- **US-69 (Notifications)**: Rider dispatch SMS/push notifications are published via domain events, handled by the notification module.
- **US-70 (Customer Portal)**: Customer live tracking is deferred to US-70.

---

## 16. Implementation Plan & Verification Strategy
- **Flyway Migration**: `V54__delivery_riders_us65.sql`.
- **Target Verification**:
  - Domain Unit Tests: `DeliveryRiderTest`, `DeliveryRiderShiftTest`.
  - Application Service Tests: `DeliveryRiderServiceTest`.
  - Controller Security Tests: `DeliveryRiderControllerTest`.
  - PostgreSQL Concurrency Acceptance: `DeliveryRiderConcurrencyPostgreSqlAcceptanceTest` (testing simultaneous assignment race, overload race, shift overlap race).
  - Frontend Vitest & Playwright E2E: `deliveryRiders.spec.ts`.
- **Next Step**: `MVP-1.4-US65-RIDERS-IMPLEMENTATION-001`.
