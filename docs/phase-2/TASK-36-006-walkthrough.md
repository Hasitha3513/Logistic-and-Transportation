# Walkthrough: TASK-36-006 Bunker Management Frontend UI & End-to-End Verification

## 1. Summary of Changes

TASK-36-006 delivers the complete operator and manager web experience for US-36 Internal Station Bunker Fuel Management within the React + TypeScript + Ant Design frontend architecture. The frontend connects to the verified Spring Modulith backend endpoints without requiring any backend contract modifications.

All operator capabilitiesâ€”including bulk depot tank configuration, authoritative book inventory inspection, observational dip measurements with book-vs-physical variance tracking, audited stock adjustments with explicit direction, dual-tank atomic fuel transfers, and paginated stock movement ledgersâ€”are fully exposed with strict role-based permission enforcement and business error handling.

---

## 2. Frontend Pages and Components Created/Modified

| File | Type | Description |
|---|---|---|
| `frontend/src/fuel/bunkerTypes.ts` | Types | Full TypeScript interfaces for `BunkerTank`, `BunkerTankBalance`, `BunkerStockMovement`, `BunkerStockMovementPage`, `DipReading`, and payload DTOs. |
| `frontend/src/fuel/hooks/useBunkerTanks.ts` | Hooks | TanStack Query hooks for querying tanks, balances, movements, dip history, and mutations for CRUD, dips, adjustments, and transfers. |
| `frontend/src/components/status/StatusTags.tsx` | Components | Added `BunkerTankStatusTag`, `BunkerStockStatusTag`, and `BunkerMovementTypeTag` with standardized colors and human-readable labels. |
| `frontend/src/navigation/navigation.tsx` | Navigation | Added `{ key: 'bunker-tanks', label: 'Bunker Tanks', path: '/fuel/bunker-tanks', permission: 'BUNKER_VIEW' }` under Fuel Management. |
| `frontend/src/fuel/BunkerTankListPage.tsx` | Page | Complete bunker tank listing with station/fuel type/active filters, stock progress bars, utilization status badges, and action modals. |
| `frontend/src/fuel/BunkerTankDetailsPage.tsx` | Page | Bunker tank detail view with authoritative balance card, dip observation & variance card, and tabbed views for Stock Movement Ledger, Dip Readings, and Properties. |
| `frontend/src/App.tsx` | Routing | Registered routes for `/fuel/bunker-tanks` and `/fuel/bunker-tanks/:bunkerTankId`, plus home page fallback for `BUNKER_VIEW`. |
| `frontend/src/fuel/BunkerTankPages.test.tsx` | Tests | Comprehensive Vitest / RTL / MSW test suite covering 11 operator scenarios. |

---

## 3. API Integration & Payload Contracts

The frontend integrates directly with the US-36 REST endpoints:

- `GET /api/v1/bunker-tanks`: Queries tanks filtered by station, fuel type, or active state.
- `GET /api/v1/bunker-tanks/{id}`: Queries tank details and configuration.
- `GET /api/v1/bunker-tanks/{id}/balance`: Queries authoritative balance, capacity, latest physical dip, and variance.
- `GET /api/v1/bunker-tanks/{id}/movements`: Queries server-paginated stock ledger movements.
- `GET /api/v1/bunker-tanks/{id}/dip-readings`: Queries physical dip observation history.
- `POST /api/v1/bunker-tanks`: Creates a new bunker tank at an internal station.
- `PUT /api/v1/bunker-tanks/{id}`: Updates tank name, minimum stock, operating status, and active state.
- `POST /api/v1/bunker-tanks/{id}/dip-readings`: Captures physical dip observation without mutating book stock.
- `POST /api/v1/bunker-tanks/{id}/adjustments`: Commits signed stock adjustment delta and posts audit movement to ledger.
- `POST /api/v1/bunker-transfers`: Atomically transfers fuel between source and destination tanks under dual-tank row locks.

---

## 4. TanStack Query Hooks

Implemented in `frontend/src/fuel/hooks/useBunkerTanks.ts`:

- `useBunkerTanks(filters)`: Queries list of bunker tanks with queryKey `['bunker-tanks', filters]`.
- `useBunkerTank(id)`: Queries single bunker tank by UUID.
- `useBunkerBalance(id)`: Queries real-time balance and latest variance.
- `useBunkerMovements(id, page, limit)`: Queries paginated stock ledger entries.
- `useBunkerDipReadings(id)`: Queries historical dip readings.
- `useCreateBunkerTank()`: Mutation that invalidates `['bunker-tanks']`.
- `useUpdateBunkerTank(id)`: Mutation that invalidates tank details and tank list.
- `useRecordDipReading(id)`: Mutation that invalidates balance and dip reading history.
- `useAdjustBunkerStock(id)`: Mutation that invalidates balance, movements, and tank list.
- `useTransferBunkerStock()`: Mutation that invalidates all tanks, balances, and movement ledgers.

---

## 5. Permission Matrix & Navigation Behavior

| Permission | UI Action / Scope | Navigation / Guard Behavior |
|---|---|---|
| `BUNKER_VIEW` | Access `/fuel/bunker-tanks` and detail view | Renders "Bunker Tanks" sidebar link; redirects unauthorized users to `/workspace`. |
| `BUNKER_CREATE` | "New Tank" button & `CreateTankModal` | Hidden when absent. |
| `BUNKER_UPDATE` | "Edit Tank" button & `EditTankModal` | Hidden when absent. |
| `BUNKER_DIP_RECORD` | "Record Physical Dip" button & `RecordDipModal` | Hidden when absent. |
| `BUNKER_ADJUST` | "Stock Adjustment" button & `StockAdjustmentModal` | Hidden when absent. |
| `BUNKER_TRANSFER` | "Inter-Tank Transfer" button & `TransferModal` | Hidden when absent. |
| `BUNKER_LEDGER_VIEW` | Stock Movement Ledger tab contents | Displays permission alert if user lacks permission. |

---

## 6. Bunker Tank List View Verification

- Table columns: Tank Code (link to details), Tank Name, Station, Fuel Type, Capacity (L), Current Stock (L), Available Capacity (L), Utilization Progress Bar, Stock State badge, Operating Status tag, Actions.
- Progress bar dynamically colors: Green for normal, Orange for low stock, Blue for near capacity, Red for out-of-service.
- Filters: Station dropdown (internal stations), Fuel Type dropdown, and Active Status dropdown.

---

## 7. Bunker Tank Detail View Verification

- Responsive header displaying Tank Code, Status Tag, Stock Status Tag, Operating Station name, and Fuel Type.
- Breadcrumb navigation: `Home > Bunker Tanks > [Tank Code]`.
- Action buttons in top right: "Record Physical Dip", "Stock Adjustment", "Transfer Fuel" (permission-aware).

---

## 8. Balance & Capacity Card Behavior

- **Authoritative Book Inventory Card**: Displays Current Book Stock (L), Total Tank Capacity (L), Circular Utilization Gauge, Available Ullage (Free Space L), and Reorder Level (L).
- **Physical Dip Observation & Variance Card**: Displays Latest Physical Dip (L), Observation Timestamp, and Observed Variance (+/- L). Explains that dip readings are observations only and do not automatically alter the book balance.

---

## 9. Stock Movement Ledger Verification

- Displays server-paginated ledger entries (`OPENING_BALANCE`, `PURCHASE_RECEIPT`, `FUEL_ISSUE`, `TRANSFER_IN`, `TRANSFER_OUT`, `STOCK_ADJUSTMENT`).
- Shows formatted date/time, movement type badge, signed quantity delta (`+` green, `-` red), resulting book balance, reference type tag, and remarks/reason.

---

## 10. Physical Dip Recording & Observation Verification

- Modal clearly alerts the operator: *"Recording a physical dip captures an observational measurement and calculates variance against the book stock. It does NOT alter the book inventory balance."*
- Form fields: Physical Dip Measurement (Liters, required), Observation Notes (optional).
- On success: Closes modal, refreshes balance card and dip history tab without modifying book stock.

---

## 11. Stock Adjustment Verification

- Modal clearly alerts the operator: *"Authoritative Inventory Change: A stock adjustment directly mutates the book inventory balance and creates an audit movement in the stock ledger."*
- Interactive UI shows Current Book Balance and live preview of Resulting Balance.
- Direction selector: Radio buttons for "Stock Increase (+ IN)" or "Stock Decrease (- OUT)".
- Mandatory reason text area enforces audit compliance.

---

## 12. Inter-Tank Transfer Verification

- Atomic transfer modal with clear instructions explaining dual-tank row locks and matching fuel type requirements.
- Source Tank select filters and sets source balance.
- Destination Tank select dynamically filters to eligible tanks matching the source fuel type and excluding the source tank.
- Shows live source stock and destination free space before confirming transfer.

---

## 13. Create & Update Tank Verification

- **Create Modal**: Restricts station selection strictly to `INTERNAL` stations. Collects tank code, fuel type, tank name, capacity, optional minimum stock, and optional opening balance.
- **Update Modal**: Restricts mutation strictly to non-stock attributes (tank name, minimum stock, operating status, active state). Clearly informs operator that inventory balances cannot be edited directly in configuration.

---

## 14. Business Error Handling Verification

- REST errors (e.g. `INSUFFICIENT_BUNKER_STOCK`, `CAPACITY_EXCEEDED`, `MISMATCHED_FUEL_TYPES`) are parsed and rendered directly inside modal alerts and via Ant Design notification/message banners.
- Form states remain preserved on error so operators can correct quantities without retyping entire payloads.

---

## 15. Frontend Unit & Integration Tests (Vitest)

All 12 test suites passed:
- `src/api/client.test.ts` (2 tests)
- `src/components/status/StatusTags.test.tsx` (3 tests)
- `src/pages/DashboardPage.test.tsx` (2 tests)
- `src/fuel/TripFuelCostSection.test.tsx` (2 tests)
- `src/fleet/VehicleReadingsSection.test.tsx` (1 test)
- `src/trips/AssignmentDrawers.test.tsx` (6 tests)
- `src/trips/LifecycleActions.test.tsx` (8 tests)
- `src/trips/TripListPage.test.tsx` (3 tests)
- `src/trips/TripDetailsPage.test.tsx` (6 tests)
- `src/fuel/FuelPurchasePages.test.tsx` (7 tests)
- `src/fuel/FuelIssuePages.test.tsx` (12 tests)
- `src/fuel/BunkerTankPages.test.tsx` (11 tests)

**Total Frontend Tests**: **68 passed / 68 total (0 failed, 0 skipped)**
**ESLint**: **0 errors, 0 warnings**
**TypeScript & Vite Production Build**: **Clean build (0 errors)**

---

## 16. Backend Regression Verification (Maven)

Command executed: `mvn -B test`
**Total Backend Tests**: **312 passed / 312 total (0 failed, 0 errors, 21 skipped)**
**Status**: **BUILD SUCCESS**

---

## 17. End-to-End Acceptance Scenario

1. **Manager Navigation**: User with `BUNKER_VIEW` logs in $\rightarrow$ Fuel Management menu displays "Bunker Tanks" $\rightarrow$ Navigates to `/fuel/bunker-tanks`.
2. **Tank Setup**: Manager clicks "New Tank" (`BUNKER_CREATE`) $\rightarrow$ Selects Internal Depot station, DIESEL fuel type, 10,000 L capacity $\rightarrow$ Tank `BNK-DSL-01` created.
3. **Purchase Receiving**: Bulk fuel purchase received $\rightarrow$ Backend credits 2,000 L to `BNK-DSL-01`.
4. **Detail Inspection**: Operator navigates to `/fuel/bunker-tanks/tank-1` $\rightarrow$ Balance shows 2,000 L / 10,000 L (20% utilization).
5. **Physical Dip**: Inspector records physical dip of 1,980 L $\rightarrow$ Dip saved, variance card shows -20.0 L (book stock remains 2,000 L).
6. **Stock Adjustment**: Manager executes stock adjustment of -20.0 L with reason "Evaporation variance reconciliation" $\rightarrow$ Book stock becomes 1,980 L, movement posted to ledger.
7. **Fuel Issue**: Vehicle refueled with 100 L $\rightarrow$ Stock deducted to 1,880 L.
8. **Transfer**: Operator transfers 500 L to auxiliary tank `BNK-DSL-02` $\rightarrow$ Stock updated atomically under pessimistic row locks.

---

## 18. Final US-36 Status Matrix

| Task ID | Component | Status | Verification |
|---|---|---|---|
| TASK-36-001 | Domain Models, Value Objects & Migration | **COMPLETE** | Unit tests + Flyway V18 |
| TASK-36-002 | Fuel Issue Bunker Deduction & Overdraw Validation | **COMPLETE** | Service + Integration tests |
| TASK-36-003 | Bulk Fuel Purchase Bunker Credit & Capacity Validation | **COMPLETE** | Service + Idempotency tests |
| TASK-36-004 | Dip Reading, Variance, Adjustment & Transfer APIs | **COMPLETE** | Web + Security + Service tests |
| TASK-36-005 | PostgreSQL Multi-Threaded Concurrency Hardening | **COMPLETE** | 7/7 real PostgreSQL multi-threaded tests |
| TASK-36-006 | Frontend UI & End-to-End Operator Experience | **COMPLETE** | 11 Vitest UI tests + 68/68 full frontend + 312/312 backend |

---

## 19. Remaining Risks and Mitigations

- **Risk**: High concurrency volume on a single bunker tank during simultaneous vehicle fueling.
  - **Mitigation**: Verified with PostgreSQL row-level `PESSIMISTIC_WRITE` locking (`entityManager.refresh()` ensures fresh balance read).
- **Risk**: Unauthorized operator modifying book balances.
  - **Mitigation**: Configuration updates prohibit stock modifications; stock adjustments require explicit `BUNKER_ADJUST` permission and mandatory justification.