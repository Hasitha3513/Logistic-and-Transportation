# Product Decisions and Domain Contract: US-66 — Batch Delivery Orders

## 1. Metadata & Status
- **Task ID**: `MVP-1.4-US66-BATCH-DELIVERY-ORDERS-PRODUCT-DECISIONS-001`
- **Story ID**: `US-66`
- **Story Title**: `Batch Delivery Orders`
- **Milestone / Release Band**: `MVP 1.4 — Last-Mile Delivery & Customer Experience`
- **Product Decision Status**: **`FROZEN`**
- **Implementation Status**: **`NOT_STARTED`**
- **Authoritative Baseline**:
  - `US-63` (Manage Delivery Zones): **`COMPLETE`**
  - `US-64` (Manage Delivery Slots): **`COMPLETE`**
  - `US-65` (Manage Riders): **`COMPLETE`**
  - `MVP 1.4`: **`3 / 8 COMPLETE`**
  - `Overall Accounting`: **`60 / 87 COMPLETE`**, **`27 / 87 DEFERRED`**, **`87 TOTAL`**
  - Current Accepted Flyway Head: `V54`
  - Target Migration for US-66: `V55__delivery_batches_us66.sql` (to be created during implementation)

---

## 2. Precondition Gate
- `docs/mvp/MVP-1.4-US65-RIDERS-FINAL-ACCEPTANCE-001.md` verified: **`US-65 COMPLETE`**, `MVP 1.4: 3 / 8 COMPLETE`, `Overall: 60 / 87 COMPLETE`, `Flyway: V54 PASS`.
- Precondition gate passed.

---

## 3. Source Authority & Requirements Extraction
- **Primary Source**: `docs/requirements/US-61-US-70-UseCase-Activity-Sequence-Diagrams.md` (US-66 — Batch Delivery Orders).
- **Primary Actor**: `Last-Mile Planner` (also accessible to `Delivery Dispatcher` / `Delivery Manager`).
- **Goal**: Cluster deliveries by proximity (delivery zone), delivery slot, priority, and rider capacity so rider workloads and urban dispatches are efficient.
- **Authority Order**:
  1. Original Business Requirements & Use Case Diagrams
  2. Accepted Architecture & Multi-Tenancy ADRs (`00_CORE_ARCHITECTURE/`)
  3. Verified Domain Contracts (`DeliveryOrder`, `DeliveryZone`, `DeliverySlot`, `DeliveryRider`)
  4. Roadmap & Milestone prose.

---

## 4. Definition of "Delivery Batch"
- A **Delivery Batch** (`DeliveryBatch`) is a coherent operational grouping of ready `DeliveryOrder`s designated for single-courier / single-run execution within a specific `DeliveryZone` and `DeliverySlot`.
- **Domain Scope**:
  - A batch decides **which** orders belong together based on shared operational constraints (same zone, same slot/window, compatible service type, capacity bounds).
  - A batch links zero or one assigned `DeliveryRider`.
  - A batch is **not** a route optimization engine (stop order sequencing / traveling salesperson optimization is owned by the routing module / US-20 / US-67).

---

## 5. Domain Ownership & Modulith Boundary
- **Module Owner**: `com.transportlogistics.app.delivery`.
- **Pure Domain Aggregates**:
  - `DeliveryBatch` (Aggregate Root) in `com.transportlogistics.app.delivery.domain.model`.
  - `DeliveryBatchOrder` (Entity / Value Object) in `com.transportlogistics.app.delivery.domain.model`.
- **Route & Trip Boundaries**:
  - `delivery` owns batch composition, lifecycle, order membership, and rider binding.
  - US-66 does **not** create or mutate `Trip` or `Route` entities. It prepares dispatches within the last-mile delivery context.
  - Routing optimization (US-20/US-67), GPS tracking (US-48..US-55), and dynamic ETAs (US-67) remain strictly out of scope.

---

## 6. Batch Identity & Lifecycle
- **Identifier**: `UUID id` (Primary Key, tenant-scoped).
- **Batch Code**: `String batchCode` (Unique human-readable reference, e.g. `BAT-YYYY-NNNNNN`, regex `^BAT-[0-9]{4}-[0-9]{6}$`, generated atomically per tenant/year).
- **Zone Binding**: `UUID deliveryZoneId` (Mandatory, foreign key to `delivery_zone(id, tenant_id)`).
- **Slot Binding**: `UUID deliverySlotId` (Optional / Recommended, foreign key to `delivery_slot(id, tenant_id)`).
- **Rider Binding**: `UUID riderId` (Optional during creation, assignable before dispatch).
- **Batch Lifecycle Status (`DeliveryBatchStatus`)**:
  - `DRAFT` — Initial cluster under construction; orders can be added or removed.
  - `READY` — Finalized cluster ready for rider assignment / dispatch; orders locked.
  - `ASSIGNED` — Bound to an active, eligible `DeliveryRider`.
  - `DISPATCHED` — Rider has departed with the batch for last-mile delivery execution.
  - `COMPLETED` — All member delivery orders have reached terminal delivery states (`DELIVERED`, `FAILED_ATTEMPT`, `RETURN_TO_BASE`).
  - `CANCELLED` — Batch disbanded; member orders return to unbatched `READY_FOR_ASSIGNMENT` status.

---

## 7. Batch Creation & Grouping Modes
1. **Manual Batching**:
   - Dispatcher explicitly selects a set of pending `DeliveryOrder` IDs for a target `DeliveryZone` and optional `DeliverySlot`.
   - Atomic all-or-nothing validation: if any selected order is ineligible, the entire request is rejected with explicit reasons.
2. **Deterministic Auto-Batching**:
   - Groups unbatched orders in `READY_FOR_ASSIGNMENT` status filtered by `tenant_id`, `delivery_zone_id`, `delivery_slot_id`, and `service_type`.
   - Sorts candidate orders deterministically: `priority DESC (URGENT -> HIGH -> NORMAL -> LOW)`, then `created_at ASC`.
   - Partitions into batch chunks up to `maxBatchSize` (default: 5 orders, or matching target rider's remaining capacity).
   - Generates proposed `DRAFT` batches for planner review.

---

## 8. DeliveryOrder Eligibility & Single Active Membership
- **Eligibility Invariant**:
  - Only `DeliveryOrder`s in `READY_FOR_ASSIGNMENT` status may be added to a batch.
  - Terminal orders (`DELIVERED`, `RETURN_TO_BASE`) and draft/cancelled orders cannot enter a batch.
- **Single Active Membership Invariant**:
  - A `DeliveryOrder` can belong to at most **one** active (non-cancelled, non-disbanded) `DeliveryBatch` at any time.
  - Enforced in database via a partial unique index on `delivery_batch_order(tenant_id, delivery_order_id) WHERE status = 'ACTIVE'`.
- **Zone & Slot Homogeneity**:
  - All orders in a batch MUST have `delivery_zone_id` matching the batch's `deliveryZoneId`.
  - If the batch specifies a `deliverySlotId`, all member orders must have matching `delivery_slot_id` (unless explicitly override-authorized).

---

## 9. Rider Assignment & Workload Capacity Integration
- **Rider Assignment**:
  - `POST /api/v1/deliveries/batches/{id}/assign-rider` assigns a rider to the entire batch.
  - Reuses the accepted US-65 `DeliveryRiderService` and `DriverEligibilityPort` validations.
  - Validates that the rider is `ACTIVE`, on-duty for the batch's date/slot, and authorized for the batch's zone.
- **Capacity Guard**:
  - The number of active orders in the batch + the rider's existing active deliveries must NOT exceed `rider.maxConcurrentDeliveries` (unless `DELIVERY_RIDER_OVERRIDE` with `overrideReason` is supplied).
- **Order-Level Rider Link**:
  - Assigning a rider to a batch atomically updates `delivery_order.current_rider_id` and registers `delivery_order_rider_assignment` records for all contained orders to maintain unified assignment history.

---

## 10. Batch Membership Model (`delivery_batch_order`)
- **Structure**:
  - `id`: UUID (Primary Key)
  - `tenant_id`: UUID (NOT NULL)
  - `batch_id`: UUID (FK -> `delivery_batch(id, tenant_id)` ON DELETE CASCADE)
  - `delivery_order_id`: UUID (FK -> `delivery_order(id, tenant_id)`)
  - `sequence_hint`: INT (Optional display index 1..N, default insertion order)
  - `status`: VARCHAR(32) (`ACTIVE`, `REMOVED`, `COMPLETED`)
  - `added_at`: TIMESTAMPTZ (NOT NULL)
  - `added_by`: VARCHAR(128) (NOT NULL)
  - `removed_at`: TIMESTAMPTZ (Nullable)
  - `removed_by`: VARCHAR(128) (Nullable)
- **Add / Remove Semantics**:
  - Orders may be added or removed only while batch status is `DRAFT` or `READY`.
  - Removing an order marks its membership status `REMOVED`, clears any batch rider association on that order, and releases rider capacity.

---

## 11. Priority, Service Type & Vehicle Mode Compatibility
- **Priority Handling**:
  - High/Urgent priority orders within the same zone/slot are grouped first.
  - Mixed priorities within the same batch are permitted as long as zone and slot match.
- **Service Types**:
  - `EXPRESS` and `SAME_DAY` orders are clustered in dedicated batches where volume permits; fallback permits mixing with `STANDARD` if within the same slot window.
- **Vehicle Mode**:
  - Derived from fleet/rider profile (`MOTORBIKE`, `VAN`, `BICYCLE`); batch size default is bounded by the rider's operational capacity.

---

## 12. Concurrency & Transactional Integrity
- **Optimistic Locking**:
  - `DeliveryBatch` carries `@Version Long version`. Concurrent mutations return `409 Conflict`.
- **Atomic Bulk Insertion**:
  - Creating or adding orders to a batch executes inside a single transaction with row locking (`PESSIMISTIC_WRITE`) on the selected `DeliveryOrder` entities to prevent duplicate concurrent batching.
- **Double-Batching Race**:
  - Partial unique index `uk_active_batch_order (tenant_id, delivery_order_id) WHERE status = 'ACTIVE'` guarantees that parallel attempts to batch the same order fail safely with a unique constraint violation.

---

## 13. Multi-Tenancy & Same-Tenant Integrity
- `tenant_id` is derived exclusively from authenticated server-side context (`SecurityContext`).
- Direct composite foreign keys backed by PostgreSQL constraints:
  - `delivery_batch(id, tenant_id)` PRIMARY KEY
  - `delivery_batch(delivery_zone_id, tenant_id) -> delivery_zone(id, tenant_id)`
  - `delivery_batch(delivery_slot_id, tenant_id) -> delivery_slot(id, tenant_id)`
  - `delivery_batch(rider_id, tenant_id) -> delivery_rider(id, tenant_id)`
  - `delivery_batch_order(batch_id, tenant_id) -> delivery_batch(id, tenant_id)`
  - `delivery_batch_order(delivery_order_id, tenant_id) -> delivery_order(id, tenant_id)`

---

## 14. RBAC & Security Permissions
- `DELIVERY_BATCH_VIEW` — View batches, batch details, and batch order rosters.
- `DELIVERY_BATCH_CREATE` — Create manual batches or trigger auto-batching.
- `DELIVERY_BATCH_UPDATE` — Add/remove orders or update batch metadata in `DRAFT`/`READY` states.
- `DELIVERY_BATCH_ASSIGN` — Assign or reassign a rider to a batch.
- `DELIVERY_BATCH_DISPATCH` — Mark a batch as `DISPATCHED`.
- `DELIVERY_BATCH_CANCEL` — Disband or cancel a batch.
- **Role Assignments**:
  - `ADMIN`, `DELIVERY_MANAGER`: Full permissions (`VIEW`, `CREATE`, `UPDATE`, `ASSIGN`, `DISPATCH`, `CANCEL`).
  - `DISPATCHER`: (`VIEW`, `CREATE`, `UPDATE`, `ASSIGN`, `DISPATCH`).
  - `PLANNER`: (`VIEW`, `CREATE`, `UPDATE`).
  - `VIEWER`: (`VIEW`).
  - `RIDER`: (`VIEW` limited to assigned batches).

---

## 15. REST API Contract
- `POST /api/v1/deliveries/batches` — Create manual delivery batch.
- `POST /api/v1/deliveries/batches/auto-cluster` — Deterministic auto-clustering preview/create.
- `GET /api/v1/deliveries/batches` — List delivery batches with pagination and zone/slot/status/rider filters.
- `GET /api/v1/deliveries/batches/{id}` — Get batch details and member orders.
- `PUT /api/v1/deliveries/batches/{id}` — Update batch details (metadata).
- `POST /api/v1/deliveries/batches/{id}/orders` — Add orders to batch.
- `DELETE /api/v1/deliveries/batches/{id}/orders/{orderId}` — Remove order from batch.
- `POST /api/v1/deliveries/batches/{id}/ready` — Transition batch from `DRAFT` to `READY`.
- `POST /api/v1/deliveries/batches/{id}/assign-rider` — Assign rider to batch.
- `POST /api/v1/deliveries/batches/{id}/dispatch` — Dispatch batch.
- `POST /api/v1/deliveries/batches/{id}/cancel` — Cancel / disband batch.
- `GET /api/v1/deliveries/batches/{id}/orders` — Get active member orders for batch.

---

## 16. Error Contract
- `DELIVERY_BATCH_NOT_FOUND` (404)
- `DELIVERY_BATCH_INVALID_STATE` (400 / 409)
- `DELIVERY_BATCH_ORDER_NOT_ELIGIBLE` (400)
- `DELIVERY_BATCH_ORDER_ALREADY_BATCHED` (409)
- `DELIVERY_BATCH_ZONE_MISMATCH` (400)
- `DELIVERY_BATCH_SLOT_MISMATCH` (400)
- `DELIVERY_BATCH_RIDER_INELIGIBLE` (400)
- `DELIVERY_BATCH_CAPACITY_EXCEEDED` (400 / 409)
- `DELIVERY_BATCH_VERSION_CONFLICT` (409)

---

## 17. Domain Events
- `DeliveryBatchCreatedEvent`
- `DeliveryBatchUpdatedEvent`
- `DeliveryBatchOrderAddedEvent`
- `DeliveryBatchOrderRemovedEvent`
- `DeliveryBatchRiderAssignedEvent`
- `DeliveryBatchDispatchedEvent`
- `DeliveryBatchCancelledEvent`
- All events contain standard tenant envelope `(eventId, tenantId, timestamp, batchId, actor)` and zero sensitive customer PII or driver medical data.

---

## 18. Frontend Refine UX
- Navigation: `Delivery Operations > Batches` (`/deliveries/batches`).
- **Batch List View**: Data table displaying Batch Code, Status tag, Zone badge, Slot window, Assigned Rider, Order count, Priority indicators, and Action buttons.
- **Batch Create / Cluster Modal**:
  - Step 1: Select Zone and Delivery Slot.
  - Step 2: View eligible `READY_FOR_ASSIGNMENT` orders with proximity & priority sorting.
  - Step 3: Select orders (or Auto-Select up to max capacity).
  - Step 4: Optional Rider assignment and submission.
- **Batch Detail Drawer / Page**: Full member order roster, delivery address details, recipient contact, batch timeline, and lifecycle action buttons (`Mark Ready`, `Assign Rider`, `Dispatch`, `Cancel`).

---

## 19. Planned PostgreSQL Schema Baseline (`V55__delivery_batches_us66.sql`)
- Tables:
  1. `delivery_batch`
  2. `delivery_batch_order`
  3. `delivery_batch_counter` (for atomic `BAT-YYYY-NNNNNN` sequence generation)
- RBAC Seeds for `DELIVERY_BATCH_*` permissions.
- Indexes:
  - `idx_delivery_batch_tenant_status (tenant_id, status)`
  - `idx_delivery_batch_tenant_zone (tenant_id, delivery_zone_id)`
  - `idx_delivery_batch_tenant_slot (tenant_id, delivery_slot_id)`
  - `idx_delivery_batch_tenant_rider (tenant_id, rider_id)`
  - `uk_active_batch_order (tenant_id, delivery_order_id) WHERE status = 'ACTIVE'`
  - `idx_batch_order_lookup (tenant_id, batch_id, status)`

---

## 20. Verification & Acceptance Matrix (VM66)
- `VM66-01`: Create manual batch with valid ready orders in same zone & slot -> SUCCESS.
- `VM66-02`: Create batch with draft or delivered order -> REJECT (400).
- `VM66-03`: Create batch with orders from different zones -> REJECT (400).
- `VM66-04`: Create batch with orders from different slots -> REJECT (400).
- `VM66-05`: Add already active batched order to a second batch -> REJECT (409).
- `VM66-06`: Concurrent attempt to add same order into two batches -> 1 PASS, 1 409 CONFLICT.
- `VM66-07`: Add / remove order from `DRAFT` batch -> SUCCESS, capacity and counts updated.
- `VM66-08`: Modify orders on `DISPATCHED` or `COMPLETED` batch -> REJECT (409).
- `VM66-09`: Assign eligible active rider to batch -> SUCCESS, order-level `current_rider_id` synchronized.
- `VM66-10`: Assign rider when batch order count exceeds rider remaining capacity -> REJECT (400).
- `VM66-11`: Cancel batch -> SUCCESS, member orders return to unbatched pool, rider capacity released.
- `VM66-12`: Cross-tenant batch access (IDOR) -> 404 / 403, zero data leakage.
- `VM66-13`: RBAC enforcement across all endpoints (`VIEW`, `CREATE`, `UPDATE`, `ASSIGN`, `DISPATCH`, `CANCEL`).
- `VM66-14`: Modulith architecture & purity tests pass (no illegal module imports).
- `VM66-15`: Playwright E2E test `deliveryBatches.spec.ts` passes on Chromium.

---

## 21. Explicit Scope Exclusions
- **US-67 (Last-Mile Dynamic ETA)**: No traffic integration, no dynamic ETA calculation, no GPS feeds.
- **US-68 (Last-Mile Exceptions)**: No doorstep exception engine.
- **US-69 (Customer Delivery Notifications)**: No direct SMS / push notifications dispatched to end customers.
- **US-70 (Customer Self-Service)**: No external self-service rescheduling portal.
- **Route / Trip Optimization**: No TSP/VRP algorithmic route optimization inside US-66.

---

## 22. Status & Immediate Next Task
- **Product Decisions Status**: **`FROZEN`**
- **Implementation Status**: **`NOT_STARTED`**
- **Next Task**: `MVP-1.4-US66-BATCH-DELIVERY-ORDERS-IMPLEMENTATION-001`
