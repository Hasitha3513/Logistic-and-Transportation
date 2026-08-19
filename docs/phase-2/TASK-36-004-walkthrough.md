# TASK-36-004 Walkthrough: Physical Dip Reading, Stock Variance & Adjustment API Completion and Verification

## Summary
In **TASK-36-004**, we completed, audited, and verified the API, security authorization, and database persistence layers for the **Physical Dip Reading**, **Stock Variance**, and **Stock Adjustment** capabilities within **US-36 Bunker Management**.

---

## Existing Functionality Reused
The existing foundation established in prior tasks was verified and reused without unnecessary redesign:
1. `BunkerTankService`:
   - `recordDipReading(...)`: Observation capture with exact decimal variance calculation (`physicalQuantity - bookQuantityAtMeasurement`).
   - `adjustStock(...)`: Explicit stock mutation with `PESSIMISTIC_WRITE` row locking, boundary validation (`resultingBalance >= 0`, `resultingBalance <= capacity`), and ledger posting.
   - `transfer(...)`: Dual-tank ordered locking (`TRANSFER_OUT` & `TRANSFER_IN` movements).
2. `BunkerTankController`:
   - `POST /bunker-tanks/{id}/dip-readings`: Observation entrypoint.
   - `GET /bunker-tanks/{id}/dip-readings`: History endpoint.
   - `POST /bunker-tanks/{id}/adjustments`: Explicit adjustment entrypoint.
   - `GET /bunker-tanks/{id}/balance`: Balance and latest dip observation summary.
   - `POST /bunker-transfers`: Inter-tank transfer entrypoint.
3. Security & Schema:
   - 7 canonical bunker permissions in `V18__bunker_management.sql`.

---

## Files Changed

### Test Suites Extended & Created
- [`BunkerTankServiceTest.java`](file:///D:/transport-logistics-modulith/transport-logistics-modulith/src/test/java/com/transportlogistics/app/fuel/application/service/BunkerTankServiceTest.java):
  - Added unit test cases for positive variance dip recording, negative dip quantity rejection, positive stock adjustment (`ADJUSTMENT_IN`), negative stock rejection (`INSUFFICIENT_BUNKER_STOCK`), capacity overflow rejection (`BUNKER_CAPACITY_EXCEEDED`), zero delta rejection, blank reason rejection, and dual-tank transfer.
- [`BunkerTankApiIntegrationTest.java`](file:///D:/transport-logistics-modulith/transport-logistics-modulith/src/test/java/com/transportlogistics/app/fuel/infrastructure/adapters/in/web/BunkerTankApiIntegrationTest.java):
  - Added Web MVC integration tests for `POST /bunker-tanks/{id}/dip-readings`, `GET /bunker-tanks/{id}/dip-readings`, `POST /bunker-tanks/{id}/adjustments`, `GET /bunker-tanks/{id}/balance`, and `POST /bunker-transfers`.
- [`BunkerSecurityIntegrationTest.java`](file:///D:/transport-logistics-modulith/transport-logistics-modulith/src/test/java/com/transportlogistics/app/identity/infrastructure/security/BunkerSecurityIntegrationTest.java):
  - Added security authorization matrix verification: 401 unauthenticated, 403 unauthorized, and 200/201 authorized for `BUNKER_DIP_RECORD`, `BUNKER_ADJUST`, `BUNKER_VIEW`, and `BUNKER_TRANSFER`.
- [`BunkerTankAdjustmentIntegrationTest.java`](file:///D:/transport-logistics-modulith/transport-logistics-modulith/src/test/java/com/transportlogistics/app/fuel/application/service/BunkerTankAdjustmentIntegrationTest.java):
  - End-to-end database persistence integration test verifying physical dip recording without modifying book stock, explicit negative/positive adjustments producing `ADJUSTMENT_OUT` / `ADJUSTMENT_IN` ledger movements, boundary constraint enforcement, and atomic inter-tank transfer.

---

## Dip Reading Domain Rule
- **Observation Only**: A physical dip reading represents an observational measurement.
- **Invariant**: `recordDipReading` **DOES NOT** mutate `BunkerTank.currentStockLiters`.
- **Proof**: In `BunkerTankAdjustmentIntegrationTest.shouldRecordDipReadingWithoutChangingBookStock()`, a physical dip of `5,400 L` against a book stock of `5,500 L` produces a variance of `-100 L` while the tank book stock remains unchanged at `5,500 L`.

---

## Variance Calculation
- Formula: `variance = physicalQuantity - bookQuantityAtMeasurement`.
- Standard Examples Verified:
  - Book `5,500 L`, Physical `5,400 L` $\rightarrow$ Variance `-100.000 L`
  - Book `5,500 L`, Physical `5,600 L` $\rightarrow$ Variance `+100.000 L`
  - Book `5,500 L`, Physical `5,500 L` $\rightarrow$ Variance `0.000 L`
- All computations use `BigDecimal` with `RoundingMode.HALF_UP` and scale of 3 (`BunkerTankPolicy.QUANTITY_SCALE`).

---

## Stock Adjustment Domain Rules & Ledger
- Explicit adjustment requires mandatory `reason` and non-zero `quantityDeltaLiters`.
- Under `PESSIMISTIC_WRITE` lock:
  - If `resultingBalance < 0` $\rightarrow$ Throws `INSUFFICIENT_BUNKER_STOCK`.
  - If `resultingBalance > capacity` $\rightarrow$ Throws `BUNKER_CAPACITY_EXCEEDED`.
- Creates exactly one ledger movement:
  - Delta `< 0` $\rightarrow$ `BunkerMovementType.ADJUSTMENT_OUT` with positive quantity `delta.abs()`.
  - Delta `> 0` $\rightarrow$ `BunkerMovementType.ADJUSTMENT_IN` with positive quantity `delta`.
  - Movement references `BunkerReferenceType.MANUAL_ADJUSTMENT` with ID of `StockAdjustment`.
  - Captures `sourceDipReadingId` for audit traceability.

---

## Security Verification Matrix
| Endpoint | Method | Required Authority | Unauth | Forbidden | Authorized |
|---|---|---|---|---|---|
| `/bunker-tanks` | GET | `BUNKER_VIEW` | 401 | 403 | 200 |
| `/bunker-tanks` | POST | `BUNKER_CREATE` | 401 | 403 | 201 |
| `/bunker-tanks/{id}/movements` | GET | `BUNKER_LEDGER_VIEW` | 401 | 403 | 200 |
| `/bunker-tanks/{id}/dip-readings` | POST | `BUNKER_DIP_RECORD` | 401 | 403 | 201 |
| `/bunker-tanks/{id}/dip-readings` | GET | `BUNKER_VIEW` | 401 | 403 | 200 |
| `/bunker-tanks/{id}/adjustments` | POST | `BUNKER_ADJUST` | 401 | 403 | 201 |
| `/bunker-transfers` | POST | `BUNKER_TRANSFER` | 401 | 403 | 200 |

---

## Verification Summary

### Automated Test Results
- **Focused Bunker Tests**: **44 tests run, 0 failures, 0 errors**.
- **Full Backend Suite**: `mvn -B test` $\rightarrow$ **BUILD SUCCESS (305 tests run, 0 failures, 0 errors, 14 skipped)**.
- **Spring Modulith Verification**: `ApplicationModulesTest.verify()` passed with **0 violations**.
- **Hexagonal Architecture Rules**: `HexagonalLayerArchitectureTest` passed with **0 violations**.
- **Frontend Lint**: `npm run lint` $\rightarrow$ Passed with **0 warnings / 0 errors**.
- **Frontend Regression Suite**: `npm test` $\rightarrow$ **11 test suites passed, 57 tests passed (0 failures)**.
- **Frontend Production Build**: `npm run build` $\rightarrow$ **Built cleanly with 0 TypeScript/Vite errors**.

---

## PostgreSQL Concurrency Status
- **Status**: **PENDING**.
- The integration tests run in TASK-36-004 are accurately classified as **transactional / database integration tests**.
- The multi-threaded PostgreSQL/Testcontainers concurrency proof is isolated to **TASK-36-005**.

---

## Remaining US-36 Tasks
- **TASK-36-005**: Multi-Threaded PostgreSQL Concurrency Hardening.
- **TASK-36-006**: Bunker Management Frontend UI (Tank list, detail cards, dip modal, adjustment modal, ledger table).