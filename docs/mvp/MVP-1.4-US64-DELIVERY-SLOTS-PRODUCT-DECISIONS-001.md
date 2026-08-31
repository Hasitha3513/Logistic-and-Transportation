# Product Decisions & Domain Contract: US-64 Manage Delivery Slots

**Task ID:** `MVP-1.4-US64-DELIVERY-SLOTS-PRODUCT-DECISIONS-001`  
**Milestone:** MVP 1.4 Last-Mile Delivery & Customer Experience (US-63..US-70)  
**Story ID:** `US-64`  
**Story Title:** Manage Delivery Slots  
**Status:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`  
**Authoritative Preconditions:** US-63 Accepted and Closed (`MVP-1.4-US63-DELIVERY-ZONES-FINAL-ACCEPTANCE-001.md`), PostgreSQL Flyway baseline V52.

---

## 1. Authoritative Requirement Extraction

From `Traspotation & logistic.docx` and `docs/requirements/US-61-US-70-UseCase-Activity-Sequence-Diagrams.md`:
- **Primary Actor:** Last-Mile Planner / Dispatcher / Delivery Manager.
- **Goal:** Plan slot capacity, peak hours, cutoffs, and operational buffers so that last-mile delivery promises and vehicle/courier workloads remain feasible and over-capacity booking is systematically prevented.
- **Core Use Cases:**
  1. `Manage Delivery Slots`: Configure operational delivery time-bands bound to specific delivery zones and calendar dates.
  2. `Plan Slot Capacity`: Set maximum order quota (integer capacity) per slot instance.
  3. `Define Peak Hours & Service Types`: Tag slots (e.g. STANDARD, EXPRESS, SAME_DAY, PEAK) for specialized scheduling.
  4. `Define Cutoff Time & Operational Buffers`: Enforce booking cutoff lead times (e.g., must book $N$ minutes before slot start) and operational buffer windows.
  5. `Validate Slot Availability & Assignment`: Calculate remaining capacity and allow assigning eligible `DeliveryOrder` records.
  6. `Prevent Over-Capacity Booking`: Enforce deterministic concurrency checks preventing reservations exceeding configured capacity without explicit manager override.

---

## 2. Domain & Hexagonal Model

### 2.1 Delivery Slot Definition
A **Delivery Slot** (`DeliverySlot`) is a **dated, zone-scoped operational capacity window** representing an operational time interval on a specific calendar date in a tenant's time zone, bound to a specific active `DeliveryZone`, with an allocated order-count capacity, booking cutoff, and operational status.

### 2.2 Aggregate & Value Objects (`com.transportlogistics.app.delivery.domain.model`)
- **`DeliverySlot` (Aggregate Root)**:
  - `id`: UUID (Primary Key)
  - `tenantId`: UUID (Multi-tenant partition)
  - `deliveryZoneId`: UUID (FK to `DeliveryZone`, mandatory, same-tenant)
  - `slotDate`: `LocalDate` (Operating date)
  - `startTime`: `LocalTime` (Slot start boundary, inclusive)
  - `endTime`: `LocalTime` (Slot end boundary, exclusive, `[start, end)`)
  - `slotType`: `DeliverySlotType` (`STANDARD`, `EXPRESS`, `SAME_DAY`, `PEAK_WINDOW`)
  - `maxCapacity`: `int` (Hard maximum delivery order capacity $> 0$)
  - `reservedCapacity`: `int` (Persistent count of currently reserved active deliveries)
  - `cutoffTime`: `OffsetDateTime` (Timestamp after which no standard bookings are allowed)
  - `bufferMinutes`: `int` (Operational turnaround buffer in minutes $\ge 0$)
  - `status`: `DeliverySlotStatus` (`ACTIVE`, `INACTIVE`, `CLOSED`)
  - `version`: `long` (Optimistic locking version)
  - `audit`: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`

- **`DeliverySlotReservation` (Entity/Snapshot within Delivery Module)**:
  - `id`: UUID
  - `tenantId`: UUID
  - `deliverySlotId`: UUID (FK to `DeliverySlot`)
  - `deliveryOrderId`: UUID (FK to `DeliveryOrder`, unique for active reservations)
  - `status`: `ReservationStatus` (`ACTIVE`, `RELEASED`, `CANCELLED`)
  - `reservedAt`: `OffsetDateTime`
  - `reservedBy`: `String`
  - `releasedAt`: `OffsetDateTime` (nullable)
  - `releasedBy`: `String` (nullable)
  - `isOverride`: `boolean` (True if overbooking override was exercised)
  - `overrideReason`: `String` (nullable, required if `isOverride` is true)

---

## 3. Key Product & Technical Decisions

### 3.1 Time & Interval Semantics
- **Interval Format:** Half-open interval `[startTime, endTime)`.
  - Example: `09:00–12:00` and `12:00–15:00` for the same zone on the same date are **non-overlapping** and valid.
- **Overlap Policy:** Two active slots for the **same zone, date, and slot type** must not overlap (`startA < endB AND startB < endA`). Overlap rejection code: `DELIVERY_SLOT_OVERLAP`.
- **Timezone Authority:** All local date/time inputs are interpreted in the tenant's operational time zone (`TenantContextPort.timeZone()`). Storage uses `LocalDate` + `LocalTime` for slot definition and UTC `TIMESTAMPTZ` for cutoff and audit timestamps.

### 3.2 Capacity & Overbooking Policy
- **Capacity Unit:** Number of `DeliveryOrder` assignments (integer count $> 0$).
- **US-63 Zone Daily Capacity Relationship:** Zone `dailyCapacity` (from US-63) represents an advisory daily ceiling for the zone. The sum of slot capacities on a given date may equal or exceed zone daily capacity, but zone total active bookings are constrained by zone daily capacity if configured.
- **Overbooking:** Strictly prohibited by default (`DELIVERY_SLOT_CAPACITY_EXCEEDED`).
- **Manager Override:** Authorized users with `DELIVERY_SLOT_OVERRIDE` may overbook a slot with mandatory `overrideReason`. Audit records capture `isOverride=true` and actor details.
- **Capacity Reduction Invariant:** A manager cannot reduce `maxCapacity` below the current active `reservedCapacity` unless existing reservations are first cancelled or reassigned. Error: `DELIVERY_SLOT_CAPACITY_REDUCTION_BELOW_RESERVED`.

### 3.3 DeliveryOrder Linkage & Lifecycle
- **Linkage:** `delivery_order.delivery_slot_id` (UUID logical & foreign key constraint to `delivery_slot`).
- **Reservation Timing:** Delivery order consumes slot capacity when dispatcher/planner assigns a slot (or during automated assignment).
- **Release/Cancellation:** Capacity is immediately decremented and reservation status set to `RELEASED` or `CANCELLED` when:
  - The delivery order is cancelled.
  - The delivery order is reassigned to another slot or zone.
  - US-62 wrong-address exception re-resolves the delivery to a different zone.
  - The delivery order transitions to terminal `RETURN_TO_BASE`.
- **Historical Immutability:** Completed deliveries (`DELIVERED`) retain their historical `delivery_slot_id` reference; deactivating or editing future slots does not mutate completed records.

### 3.4 Integration with US-60 Redelivery & US-56 Orders
- **US-60 Redelivery Integration:** Implement `DeliverySlotAvailabilityPort` (from US-60) backed by the real US-64 `DeliverySlotRepository` and `DeliveryZoneLookupPort`. When scheduling redelivery, US-60 can now resolve eligible slots with live capacity rather than static fallback availability.
- **US-56 Delivery Windows:** Slot `[startTime, endTime)` updates/populates `DeliveryOrder.scheduledDeliveryWindow` for dispatch execution.

---

## 4. Multi-Tenancy & Database Schema (Flyway V53)

### 4.1 Schema Expectations (PostgreSQL 16)
- **Table `delivery_slot`**:
  ```sql
  CREATE TABLE delivery_slot (
      id UUID PRIMARY KEY,
      tenant_id UUID NOT NULL,
      delivery_zone_id UUID NOT NULL,
      slot_date DATE NOT NULL,
      start_time TIME NOT NULL,
      end_time TIME NOT NULL,
      slot_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
      max_capacity INTEGER NOT NULL CHECK (max_capacity > 0),
      reserved_capacity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_capacity >= 0),
      cutoff_time TIMESTAMPTZ,
      buffer_minutes INTEGER NOT NULL DEFAULT 0 CHECK (buffer_minutes >= 0),
      status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
      version BIGINT NOT NULL DEFAULT 0,
      created_at TIMESTAMPTZ NOT NULL,
      updated_at TIMESTAMPTZ NOT NULL,
      created_by VARCHAR(255),
      updated_by VARCHAR(255),
      CONSTRAINT chk_slot_time_order CHECK (start_time < end_time),
      CONSTRAINT uk_delivery_slot_id_tenant UNIQUE (id, tenant_id),
      CONSTRAINT fk_delivery_slot_zone_tenant FOREIGN KEY (delivery_zone_id, tenant_id)
          REFERENCES delivery_zone (id, tenant_id) ON DELETE RESTRICT
  );

  CREATE INDEX idx_delivery_slot_zone_date ON delivery_slot (tenant_id, delivery_zone_id, slot_date, status);
  CREATE INDEX idx_delivery_slot_date_status ON delivery_slot (tenant_id, slot_date, status);
  ```

- **Table `delivery_slot_reservation`**:
  ```sql
  CREATE TABLE delivery_slot_reservation (
      id UUID PRIMARY KEY,
      tenant_id UUID NOT NULL,
      delivery_slot_id UUID NOT NULL,
      delivery_order_id UUID NOT NULL,
      status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
      reserved_at TIMESTAMPTZ NOT NULL,
      reserved_by VARCHAR(255) NOT NULL,
      released_at TIMESTAMPTZ,
      released_by VARCHAR(255),
      is_override BOOLEAN NOT NULL DEFAULT FALSE,
      override_reason TEXT,
      version BIGINT NOT NULL DEFAULT 0,
      CONSTRAINT uk_slot_reservation_id_tenant UNIQUE (id, tenant_id),
      CONSTRAINT fk_slot_res_slot_tenant FOREIGN KEY (delivery_slot_id, tenant_id)
          REFERENCES delivery_slot (id, tenant_id) ON DELETE RESTRICT,
      CONSTRAINT fk_slot_res_order_tenant FOREIGN KEY (delivery_order_id, tenant_id)
          REFERENCES delivery_order (id, tenant_id) ON DELETE RESTRICT
  );

  CREATE UNIQUE INDEX uk_active_order_slot_reservation ON delivery_slot_reservation (tenant_id, delivery_order_id)
      WHERE status = 'ACTIVE';
  ```

- **Column addition to `delivery_order`**:
  ```sql
  ALTER TABLE delivery_order ADD COLUMN delivery_slot_id UUID;
  ALTER TABLE delivery_order ADD CONSTRAINT fk_delivery_order_slot_tenant
      FOREIGN KEY (delivery_slot_id, tenant_id) REFERENCES delivery_slot (id, tenant_id) ON DELETE SET NULL;
  ```

---

## 5. Security & RBAC Model

### 5.1 Permissions Seeded in V53
1. `DELIVERY_SLOT_VIEW` — View slots and capacity matrices.
2. `DELIVERY_SLOT_CREATE` — Create new delivery slots.
3. `DELIVERY_SLOT_UPDATE` — Modify slot window, capacity, cutoff, or buffer.
4. `DELIVERY_SLOT_ACTIVATE` — Activate, deactivate, or close slots.
5. `DELIVERY_SLOT_ASSIGN` — Assign delivery orders to slots / release reservations.
6. `DELIVERY_SLOT_OVERRIDE` — Overbook slots exceeding configured capacity ceiling.

### 5.2 Role Matrix
| Role | View | Create | Update | Activate | Assign | Override |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| **ADMIN** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **DELIVERY_MANAGER** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **DISPATCHER** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| **VIEWER** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **DRIVER** | ✅ (Assigned only) | ❌ | ❌ | ❌ | ❌ | ❌ |

---

## 6. REST API Contract

Base Path: `/api/v1/delivery-slots`

1. `POST /api/v1/delivery-slots` — Create delivery slot (requires `DELIVERY_SLOT_CREATE`)
2. `GET /api/v1/delivery-slots` — Query slots by zone, date range, type, and status (requires `DELIVERY_SLOT_VIEW`)
3. `GET /api/v1/delivery-slots/{id}` — Get slot details with reserved/remaining capacity (requires `DELIVERY_SLOT_VIEW`)
4. `PUT /api/v1/delivery-slots/{id}` — Update slot configuration with optimistic locking (requires `DELIVERY_SLOT_UPDATE`)
5. `POST /api/v1/delivery-slots/{id}/activate` — Activate slot (requires `DELIVERY_SLOT_ACTIVATE`)
6. `POST /api/v1/delivery-slots/{id}/deactivate` — Deactivate slot (requires `DELIVERY_SLOT_ACTIVATE`)
7. `POST /api/v1/delivery-slots/{id}/close` — Close slot to new bookings (requires `DELIVERY_SLOT_ACTIVATE`)
8. `GET /api/v1/delivery-slots/available` — Query available slots for a specific destination location or zone on a date (requires `DELIVERY_SLOT_VIEW`)
9. `POST /api/v1/delivery-slots/{id}/reservations` — Assign delivery order to slot (requires `DELIVERY_SLOT_ASSIGN`, or `DELIVERY_SLOT_OVERRIDE` if overbooking)
10. `POST /api/v1/delivery-slots/{id}/reservations/{deliveryId}/release` — Release delivery slot reservation (requires `DELIVERY_SLOT_ASSIGN`)

---

## 7. Scope Exclusions & Downstream Boundaries

- **US-65 (Riders):** No courier/rider shift assignment or bike/van fleet mapping.
- **US-66 (Batching):** No multi-order clustering algorithms or batch vehicle routing.
- **US-67 (Last-Mile ETA):** No dynamic traffic/telematics-based real-time ETA engine.
- **US-68 (Last-Mile Exceptions):** No address unreachable / gate code exception ticket workflows.
- **US-69 (Notifications):** No SMS/WhatsApp/Push customer notifications (domain events only).
- **US-70 (Customer Self-Service):** No customer-facing public rescheduling portal.

---

## 8. Verification & Acceptance Matrix (VM64-01 .. VM64-27)

1. `VM64-01`: Create valid delivery slot with zone, date, start/end time, capacity, and cutoff.
2. `VM64-02`: Reject invalid time window (`startTime >= endTime`).
3. `VM64-03`: Create same slot parameters across Tenant A and Tenant B successfully.
4. `VM64-04`: Reject creating slot with cross-tenant `deliveryZoneId`.
5. `VM64-05`: Prevent overlapping slots for same zone, date, and type.
6. `VM64-06`: Verify adjacent half-open boundary slots (`09:00-12:00` and `12:00-15:00`) succeed.
7. `VM64-07`: Query slots filtered by zone, date range, and status.
8. `VM64-08`: Available capacity calculation (`maxCapacity - reservedCapacity`).
9. `VM64-09`: Reserve eligible delivery order, decrementing available capacity.
10. `VM64-10`: Reject assigning delivery order whose destination resolves to Zone B into a Zone A slot.
11. `VM64-11`: Reject booking into an `INACTIVE` zone.
12. `VM64-12`: Reject booking into a non-serviceable zone (`serviceable=false`).
13. `VM64-13`: Reject booking when slot capacity is exhausted (`DELIVERY_SLOT_CAPACITY_EXCEEDED`).
14. `VM64-14`: Multithreaded PostgreSQL concurrency test: Two threads attempt last remaining capacity; exactly 1 succeeds and 1 gets 409.
15. `VM64-15`: Prevent double active slot reservation for the same `DeliveryOrder`.
16. `VM64-16`: Release slot reservation, restoring available capacity.
17. `VM64-17`: Reassign delivery to a different slot atomically.
18. `VM64-18`: Reject reducing `maxCapacity` below current `reservedCapacity`.
19. `VM64-19`: Optimistic locking conflict (409) on concurrent slot modification.
20. `VM64-20`: Cross-tenant direct slot access returns 404.
21. `VM64-21`: RBAC permission enforcement (403 for unauthorised roles).
22. `VM64-22`: Overbooking succeeds with `DELIVERY_SLOT_OVERRIDE` and mandatory reason.
23. `VM64-23`: Server-authoritative audit logging for slot creation, updates, and reservations.
24. `VM64-24`: Historical completed delivery retains assigned `delivery_slot_id` across slot edits.
25. `VM64-25`: US-60 redelivery scheduling resolves live US-64 slots through `DeliverySlotAvailabilityPort`.
26. `VM64-26`: US-62 corrected address changing delivery zone triggers slot cancellation.
27. `VM64-27`: Full regression pass across all delivery modules (US-56..US-63).

---

## 9. Implementation Readiness Declaration

All implementation-critical decisions for **US-64 Manage Delivery Slots** are **FROZEN**.

The next task is **`MVP-1.4-US64-DELIVERY-SLOTS-IMPLEMENTATION-001`**.
