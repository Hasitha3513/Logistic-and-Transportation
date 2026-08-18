# US-36 Bunker Management: Architecture Audit & Current Status Report

**Task ID**: `US-36-GAP-CHECK-01`  
**Audit Role**: Senior Software Architect & Technical Auditor  
**Date**: 2026-08-18  
**Scope**: Verification of Bunker Purchase Receipt Integration, Task Sequence, Terminology, Invariants, and Concurrency.

---

## 1. Executive Summary & Overall Status

| Metric / Dimension | Assessment | Notes |
|---|---|---|
| **Overall US-36 Status** | **IN PROGRESS (Core Foundation & Issue Deduction Complete)** | Foundation and Issue Deduction are 100% verified. |
| **Tank Foundation & Schema** | **COMPLETE** | Flyway `V18__bunker_management.sql`, JPA entities, repositories, 7 permissions. |
| **Stock Ledger & Audit Trail** | **COMPLETE** | Append-only ledger with pessimistic row locking and idempotency tracking. |
| **Fuel Issue Stock Deduction** | **COMPLETE** | Atomic deduction on `INTERNAL` station issues with pessimistic lock & external bypass. |
| **Purchase Receipt Integration** | **MISSING** | `FuelPurchaseService.receive` does NOT yet credit bunker stock or append ledger movements. |
| **Capacity Invariant Protection** | **PARTIAL** | Enforced in `BunkerTankPolicy` and `BunkerTankService`; not yet connected to `FuelPurchaseService`. |
| **Idempotency Protection** | **COMPLETE** | Verified in DB constraint (`uq_bunker_movement_reference`) and ledger repository. |
| **PostgreSQL Concurrency Proof** | **GAP IDENTIFIED** | No multi-threaded concurrency integration test exists yet for bunker tanks. |

---

## 2. Verification of Current Bunker Foundation

| Component | Layer / Package | Implementation Status | Evidence / Verification |
|---|---|---|---|
| **`BunkerTank`** | `fuel.domain.model` | **COMPLETE** | Immutable Java record with `availableCapacity()`, `isLowStock()`, `withStock()`, `withStatus()`. |
| **`BunkerTankRepository`** | `fuel.application.ports.out` | **COMPLETE** | Inbound port with pessimistic lock queries (`findByIdForUpdate`, `findActiveByStationAndFuelTypeForUpdate`). |
| **`BunkerStockMovement`** | `fuel.domain.model` | **COMPLETE** | Immutable aggregate record representing append-only ledger entries. |
| **`BunkerStockLedgerRepository`** | `fuel.application.ports.out` | **COMPLETE** | Inbound port with pagination, counting, and `existsByTankIdAndReference` idempotency lookup. |
| **`BunkerMovementType`** | `fuel.domain.model` | **COMPLETE** | Enum values: `OPENING_BALANCE`, `PURCHASE_RECEIPT`, `FUEL_ISSUE`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`. |
| **`BunkerReferenceType`** | `fuel.domain.model` | **COMPLETE** | Enum values: `INITIAL_SETUP`, `FUEL_PURCHASE`, `FUEL_ISSUE`, `TANK_TRANSFER`, `MANUAL_ADJUSTMENT`. |
| **Flyway Schema Migration** | `db/migration` | **COMPLETE** | `V18__bunker_management.sql` creating 4 tables with foreign keys and unique constraints. |
| **Granular Permissions** | `app_permission` | **COMPLETE** | Seeded 7 permissions (`BUNKER_VIEW`, `BUNKER_CREATE`, `BUNKER_UPDATE`, `BUNKER_LEDGER_VIEW`, `BUNKER_DIP_RECORD`, `BUNKER_ADJUST`, `BUNKER_TRANSFER`). |
| **REST Controller** | `fuel.infrastructure.adapters.in.web` | **COMPLETE** | `BunkerTankController` exposing 10 endpoints for tank CRUD, ledger, dips, adjustments, and transfers. |
| **Application Service** | `fuel.application.service` | **COMPLETE** | `BunkerTankService` orchestrating deadlock-free transfers, dip recordings, and adjustments. |
| **Automated Tests** | `src/test/java` | **COMPLETE** | 270 total tests passing (0 failures, 0 errors, 0 modulith violations). |

---

## 3. Task Numbering Reconciliation

During the implementation of US-36, the **Internal Fuel Issue Stock Deduction** slice was implemented immediately after the Foundation slice. The sequence has been reconciled as follows:

- **`TASK-36-001`**: Bunker Tank & Schema Foundation (**COMPLETED**)
- **`TASK-36-002`**: Internal Fuel Issue Bunker Stock Deduction & Validation (**COMPLETED**)
- **`TASK-36-003`**: Bulk Fuel Purchase Receiving Bunker Stock Credit & Reconciliation (**NEXT TASK**)
- **`TASK-36-004`**: Physical Dip Readings, Variance & Stock Adjustment (**PENDING**)
- **`TASK-36-005`**: Multi-Threaded Concurrency Hardening & Verification (**PENDING**)
- **`TASK-36-006`**: Bunker Management Frontend UI & End-to-End Verification (**PENDING**)

---

## 4. Movement Type Terminology Audit

- **Actual Enum Value**: `BunkerMovementType.FUEL_ISSUE` (Generic fuel issue movement).
- **Audit Findings**: The generic `FUEL_ISSUE` value is used uniformly across `FuelIssueService`, `BunkerMovementType`, `BunkerStockMovementJpaRepository`, and unit/integration test assertions.
- **Architectural Conformance**: Complies with the recommendation to use generic `FUEL_ISSUE` rather than trip-specific semantics.

---

## 5. Bulk Purchase Receipt Integration Audit

- **Inspection Target**: `FuelPurchaseService.java` (`receive` and `reconcile` methods).
- **Current Behavior**:
  - `receive()` transitions purchase to `RECEIVED`, updates `receivedQuantity`, `destinationFuelStationId`, `deliveryNoteNumber`, `receivedAt`, and publishes `FuelPurchaseReceived`.
  - `receive()` does **NOT** query or lock `BunkerTankRepository`.
  - `receive()` does **NOT** increment tank stock or append `PURCHASE_RECEIPT` to `BunkerStockLedgerRepository`.
  - No event listener exists on `FuelPurchaseReceived` to perform stock credit.
- **Classification**: **MISSING**.

---

## 6. Purchase Destination Tank Identification Model

- **Current State**: `FuelPurchase` records `destinationFuelStationId` (or fallback `fuelStationId`) and `fuelType`.
- **Target Resolution Model**:
  1. When a purchase destination station is `INTERNAL`, auto-resolve the active destination tank via:
     `bunkerTanks.findActiveByStationAndFuelTypeForUpdate(destinationStationId, purchase.fuelType())`
  2. If no active tank exists: reject receipt with `NO_ACTIVE_BUNKER_TANK`.
  3. If destination station is `EXTERNAL`: bypass bunker stock credit (commercial purchase for direct vendor dispensing).

---

## 7. Capacity Invariant & Idempotency Audit

- **Capacity Invariant (`currentStock + receivedQuantity <= tank.capacity`)**:
  - Implemented in `BunkerTankPolicy.validateReceivable` (`BUNKER_CAPACITY_EXCEEDED`).
  - **Gap**: Must be executed under `PESSIMISTIC_WRITE` lock during `FuelPurchaseService.receive`.
- **Idempotency**:
  - `BunkerStockLedgerRepository.existsByTankIdAndReference(tankId, BunkerReferenceType.FUEL_PURCHASE, purchaseId)` prevents duplicate stock credit on retry.
  - Database unique index `uq_bunker_movement_reference` ensures physical constraint integrity.

---

## 8. Atomicity & Transaction Boundary

- **Requirement**: `FuelPurchaseStatus.RECEIVED` and `BunkerStockMovement(PURCHASE_RECEIPT)` must succeed together or rollback completely.
- **Implementation Design**: In `FuelPurchaseService.receive`, all bunker operations must be executed synchronously within the `transactions.execute(...)` lambda.

---

## 9. Completed Fuel Issue Deduction Verification

- **Station Type `INTERNAL`**:
  - Active bunker tank required (`NO_ACTIVE_BUNKER_TANK`).
  - Available stock sufficiency checked in draft validation and under lock (`INSUFFICIENT_BUNKER_STOCK`).
  - Pessimistic write lock acquired (`findActiveByStationAndFuelTypeForUpdate`).
  - Balance decremented atomically.
  - Append-only `FUEL_ISSUE` movement created.
  - Idempotency verified before writing.
- **Station Type `EXTERNAL`**:
  - Bunker operations safely bypassed.

---

## 10. Concurrency Testing Gap

- Current bunker tests verify single-threaded transactional logic against H2.
- No multi-threaded test (e.g. concurrent race condition where two simultaneous operations try to overdraw/overfill a tank) exists yet.
- **Classification**: Identified as a hardening gap to be addressed in **`TASK-36-005`**.

---

## 11. US-36 Status Matrix

| Capability | Implementation | Tests | Status | Evidence / Gap |
|---|---|---|---|---|
| **Tank Foundation & Schema** | `BunkerTankEntity`, `V18` | `BunkerTankApiIntegrationTest` | **COMPLETE** | V18 migration applied, 7 permissions. |
| **Tank CRUD API** | `BunkerTankController` | `BunkerTankApiIntegrationTest` | **COMPLETE** | `/api/v1/bunker-tanks` endpoints passing. |
| **Opening Balance Init** | `BunkerTankService` | `BunkerTankServiceTest` | **COMPLETE** | Ledger initialized with `OPENING_BALANCE`. |
| **Stock Ledger Queries** | `BunkerStockLedgerPersistenceAdapter` | `BunkerTankApiIntegrationTest` | **COMPLETE** | Paged movements API functioning. |
| **Fuel Issue Stock Deduction** | `FuelIssueService` | `FuelIssueBunkerIntegrationTest` | **COMPLETE** | Atomic deduction & `FUEL_ISSUE` movement. |
| **External Station Bypass** | `FuelIssueService` | `FuelIssueServiceTest` | **COMPLETE** | Verified in unit test. |
| **Purchase Receipt Credit** | `FuelPurchaseService` | None | **MISSING** | **Must be implemented in TASK-36-003.** |
| **Capacity Protection on Receipt** | `BunkerTankPolicy` | `BunkerTankPolicyTest` | **PARTIAL** | Policy exists; not wired into purchase receive. |
| **Receipt Idempotency** | `existsByTankIdAndReference` | None | **PARTIAL** | Method ready; needs purchase receipt test. |
| **Dip Reading Recording** | `BunkerTankService` | `BunkerTankServiceTest` | **COMPLETE** | Physical dip & variance calculation. |
| **Stock Adjustment** | `BunkerTankService` | `BunkerTankServiceTest` | **COMPLETE** | Variance adjustment with ledger posting. |
| **Inter-Tank Transfer** | `BunkerTankService` | `BunkerTankServiceTest` | **COMPLETE** | Dual-tank ordered locking implemented. |
| **PostgreSQL Concurrency Proof** | Not yet created | None | **GAP** | Multi-threaded test required. |
| **Bunker Frontend UI** | Not yet created | None | **PENDING** | UI pages and components to be built. |

---

## 12. Exact Next Implementation Task

According to the audit decision rules:
> **Purchase Receipt is MISSING $\rightarrow$ NEXT TASK = TASK-36-003: Bulk Purchase Receiving Bunker Stock Credit & Reconciliation**