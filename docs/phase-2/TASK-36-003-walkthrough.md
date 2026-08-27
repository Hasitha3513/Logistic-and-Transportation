# TASK-36-003 Walkthrough: Bulk Fuel Purchase Receiving â€“ Bunker Stock Credit, Capacity Validation & Transactional Reconciliation

## Summary
In **TASK-36-003**, we implemented the missing transactional integration between **US-32 (Fuel Purchases)** and **US-36 (Bunker Management)**. When a `FuelPurchase` transitions to `RECEIVED` at an `INTERNAL` fuel station:
1. The destination station is resolved and validated.
2. The active `BunkerTank` for that station and fuel type is locked under `PESSIMISTIC_WRITE`.
3. **Mandatory Architect Correction 1 (Idempotency Order)**: BEFORE mutating any stock, the system checks whether a `PURCHASE_RECEIPT` movement already exists for this purchase. If already recorded, stock mutation and movement creation are skipped, preserving lifecycle idempotency without creating divergent stock state.
4. If no movement exists, capacity is validated (`validateReceivable`), stock is credited, updated tank is saved, `PURCHASE_RECEIPT` movement is appended to the ledger, and purchase is marked `RECEIVED` in a single atomic transaction.
5. For `EXTERNAL` stations, bunker inventory operations are safely bypassed.

---

## Files Changed

### Production Code
- [`FuelPurchaseService.java`](file:///D:/transport-logistics-modulith/transport-logistics-modulith/src/main/java/com/transportlogistics/app/fuel/application/service/FuelPurchaseService.java):
  - Injected `BunkerTankRepository`, `BunkerStockLedgerRepository`, and `BunkerTankPolicy`.
  - In `receive()`: Resolves destination station. If `INTERNAL`, locks destination tank (`PESSIMISTIC_WRITE`), checks idempotency BEFORE modifying tank stock, validates capacity, calculates new stock, saves tank, and appends `PURCHASE_RECEIPT` movement.
  - If `EXTERNAL`, bypasses bunker inventory operations.
- [`FuelConfig.java`](file:///D:/transport-logistics-modulith/transport-logistics-modulith/src/main/java/com/transportlogistics/app/fuel/infrastructure/config/FuelConfig.java):
  - Added `@Bean BunkerTankPolicy bunkerTankPolicy()` and wired bunker dependencies into `fuelPurchaseUseCase`.

### Tests
- [`FuelPurchaseServiceTest.java`](file:///D:/transport-logistics-modulith/transport-logistics-modulith/src/test/java/com/transportlogistics/app/fuel/application/service/FuelPurchaseServiceTest.java):
  - Added unit tests covering internal stock credit, capacity overflow rejection, external station bypass, pre-mutation idempotency check, and atomic rollback on persistence failure.
- [`FuelPurchaseBunkerIntegrationTest.java`](file:///D:/transport-logistics-modulith/transport-logistics-modulith/src/test/java/com/transportlogistics/app/fuel/application/service/FuelPurchaseBunkerIntegrationTest.java):
  - End-to-end database integration tests verifying internal stock credit (`2000L + 4800L = 6800L`), capacity overflow rejection (`8000L + 3000L > 10000L`), and external station bypass.

---

## Transaction Flow & Atomicity

```
Client calls POST /api/v1/fuel/purchases/{id}/receive
      â”‚
      â–¼
FuelPurchaseService.receive(...) inside transactions.execute(...)
      â”‚
      â”œâ”€â–º Load FuelPurchase with findByIdForUpdate (PESSIMISTIC_WRITE lock)
      â”œâ”€â–º Validate lifecycle: policy.requireReceivable(current)
      â”œâ”€â–º Validate receivedQuantity > 0 and receivedAt <= now + 5 min
      â”œâ”€â–º Resolve destination fuel station
      â”‚
      â”œâ”€â–º IF FuelStation.isInternal():
      â”‚     â”œâ”€â–º Query bunkerTanks.findActiveByStationAndFuelTypeForUpdate(...)
      â”‚     â”œâ”€â–º If absent: throw BusinessRuleException("NO_ACTIVE_BUNKER_TANK")
      â”‚     â”œâ”€â–º CHECK bunkerMovements.existsByTankIdAndReference(tank.id, FUEL_PURCHASE, purchase.id)
      â”‚     â”‚     â”œâ”€â–º IF ALREADY EXISTS: Do NOT mutate tank stock, do NOT save duplicate movement
      â”‚     â”‚     â””â”€â–º IF NOT EXISTS:
      â”‚     â”‚           â”œâ”€â–º bunkerTankPolicy.validateReceivable(tank, receivedQuantity, fuelType)
      â”‚     â”‚           â”‚     â””â”€â–º If newStock > capacity: throw BusinessRuleException("BUNKER_CAPACITY_EXCEEDED")
      â”‚     â”‚           â”œâ”€â–º newStock = currentStock + receivedQuantity
      â”‚     â”‚           â”œâ”€â–º bunkerTanks.save(tank.withStock(newStock))
      â”‚     â”‚           â””â”€â–º bunkerMovements.save(new BunkerStockMovement(PURCHASE_RECEIPT, ...))
      â”‚
      â”œâ”€â–º IF FuelStation.isExternal():
      â”‚     â””â”€â–º Bypass bunker lookup & stock credit
      â”‚
      â”œâ”€â–º Save updated FuelPurchase with status = RECEIVED, quantityVariance
      â”œâ”€â–º Save FuelPurchaseHistory record ("RECEIVED")
      â””â”€â–º Publish FuelPurchaseReceived domain event
```

---

## Test Classification (Mandatory Correction 2)
- **H2 Integration Tests**: Classified strictly as **transactional / database integration tests**.
- **PostgreSQL Multi-Threaded Concurrency Proof**: Preserved as **PENDING** for execution in **TASK-36-005**.

---

## Verification Summary

### Automated Test Results
- **Backend Tests**: `mvn -B test` $\rightarrow$ **BUILD SUCCESS**
  - **Tests run**: **279**, Failures: **0**, Errors: **0**, Skipped: **14**
- **Spring Modulith Verification**: `ApplicationModulesTest.verify()` passed with 0 violations.
- **Hexagonal Architecture Rules**: `HexagonalLayerArchitectureTest` passed with all rules satisfied.
- **Frontend Lint**: `npm run lint` $\rightarrow$ Passed with **0 warnings / 0 errors**.
- **Frontend Regression Suite**: `npm test` $\rightarrow$ **11 test suites passed, 57 tests passed (0 failures)**.
- **Frontend Production Build**: `npm run build` $\rightarrow$ **Built cleanly with 0 TypeScript/Vite errors**.

---

## Remaining US-36 Tasks

- **TASK-36-004**: Physical Dip Readings, Variance & Stock Adjustment (Controller API & Verification).
- **TASK-36-005**: Multi-Threaded PostgreSQL Concurrency Hardening.
- **TASK-36-006**: Bunker Management Frontend UI (Tank list, detail cards, dip modal, adjustment modal, ledger table).