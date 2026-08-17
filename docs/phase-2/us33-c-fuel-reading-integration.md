# US33-C — Fuel Issue to Fleet VehicleReading Integration

## 1. Scope

US33-C integrates Fuel Issue execution with Fleet-owned `VehicleReading` facts. Fuel does not independently maintain authoritative vehicle mileage or engine-hours truth. When an authorized Fuel Issue transitions to the `ISSUED` operational status, any supplied odometer and/or engine-hour readings are recorded as authoritative `VehicleReading` facts in Fleet via Fleet's public `VehicleReadingRecorder` boundary.

## 2. Trigger Lifecycle

- **Point of Execution:** Recorded strictly when a Fuel Issue reaches status `ISSUED`.
- **Pre-issue States:** `DRAFT`, `PENDING_AUTHORIZATION`, and `AUTHORIZED` do not generate authoritative Fleet readings because planned fueling may change or never physically occur.
- **Idempotency:** Re-issuing an already issued record is prevented by Fuel lifecycle rules. If an issue operation is re-attempted, Fleet's source-reference idempotency keys (`FUEL_ISSUE:{fuelIssueId}:{readingType}`) protect against duplicate facts.

## 3. Fleet Boundary & Hexagonal Ports

- **Fuel Output Port:** `FuelVehicleReadingPort` in `com.transportlogistics.app.fuel.application.ports.out`
  ```java
  void recordIssue(UUID vehicleId, UUID fuelIssueId, BigDecimal odometerKm, BigDecimal engineHours,
                   OffsetDateTime issueDateTime, UUID actorId);
  ```
- **Fuel Infrastructure Adapter:** `FleetFuelVehicleReadingAdapter` in `com.transportlogistics.app.fuel.infrastructure.adapters.out.fleet`
  - Injects and delegates to public `com.transportlogistics.app.fleet.VehicleReadingRecorder`.
  - Translates non-null `odometerKm` to `ReadingType.ODOMETER` with `SourceType.FUEL_ISSUE`.
  - Translates non-null `engineHours` to `ReadingType.ENGINE_HOURS` with `SourceType.FUEL_ISSUE`.
- **Zero Leaked Persistence:** No Fleet JPA entities, tables, or repositories are imported into Fuel.

## 4. Odometer & Engine-Hour Behavior

- **Authoritative Vehicle ID:** Authoritative `vehicleId` resolved from the `FuelIssue` aggregate.
- **Optional Values:**
  - If odometer is supplied, an `ODOMETER` fact is recorded.
  - If engine hours are supplied, an `ENGINE_HOURS` fact is recorded.
  - If both are supplied, two facts are recorded atomically.
  - If neither is supplied, no reading is recorded, and issuance proceeds normally.
- **Precision:** `BigDecimal` values normalized to `NUMERIC(19,3)`.

## 5. Timestamp Mapping

- `recordedAt` is set to `FuelIssue.issueDateTime`, representing the physical moment of fueling.
- Fleet maintains server `receivedAt` and `createdAt`.

## 6. Chronology Validation & Deduplication

- Fleet owns chronological monotonicity:
  - Readings must be $\ge$ previous effective reading before `issueDateTime`.
  - Readings must be $\le$ next effective reading after `issueDateTime`.
  - Backdated insertions between existing readings are accepted if within the interval, and rejected if violating monotonicity.
- Duplicate reading checks against older mutable vehicle snapshots in `FuelIssueService` were removed. Payload validation (non-negative format) remains in `FuelIssuePolicy`.

## 7. Transaction Atomicity

- Fuel Issue status update to `ISSUED`, `fuel_issue_history` entry, and Fleet `VehicleReadingRecorder.record(...)` participate in the same database transaction.
- If a Fleet chronology conflict (`VEHICLE_READING_DECREASE`, `VEHICLE_READING_CHRONOLOGY_CONFLICT`) or validation failure occurs:
  - Fuel Issue transaction rolls back completely.
  - Status remains `AUTHORIZED`.
  - No `ISSUED` audit history is written.
  - No `FuelIssued` domain event is published.
  - No partial `vehicle_reading` is written.

## 8. Trip-Linked Fuel Behavior

- When a Fuel Issue is linked to a Trip (`tripId != null`), the reading source type remains `FUEL_ISSUE` with `sourceReferenceId = fuelIssueId`.
- Operational validation confirms trip eligibility and vehicle matching without altering reading source semantics.

## 9. Domain Events & Security

- **Fuel Module:** Publishes `FuelIssued` on successful transaction completion.
- **Fleet Module:** Publishes `VehicleReadingRecorded` for each recorded fact.
- **Security:** Fuel operator holding `FUEL_ISSUE_ISSUE` permission triggers reading creation internally without requiring additional manual reading permissions.

## 10. Frontend Cache Invalidation & UX

- Upon successful Fuel Issue `issue` mutation:
  - Invalidates `['fuel-issues']` query cache.
  - Invalidates `['vehicles']`, `['vehicle', vehicleId, 'readings']`, and `['vehicle', vehicleId, 'readings', 'latest']` query caches.
- Displays backend chronology errors (`VEHICLE_READING_DECREASE`, `VEHICLE_READING_CHRONOLOGY_CONFLICT`) in error alerts.
- Displays confirmation note on `ISSUED` records when readings are recorded.

## 11. Automated Verification Summary

- **Backend Unit & Slice Tests:** 195 tests pass across all business modules.
- **PostgreSQL Testcontainers:** 19 production invariant tests pass, including rollback atomicity, backdated chronology, and the shared Trip + Fuel single ledger sequence (`TRIP_START 10,000` $\rightarrow$ `FUEL_ISSUE 10,050` $\rightarrow$ `TRIP_END 10,100`).
- **Maven Clean Verify:** `mvn clean verify` passes all 214 tests and Spring Modulith verifications.
- **Frontend Verification:** `eslint`, `vitest` (54 tests across 9 suites), and `vite build` all pass with 0 errors.

## 12. Remaining US-33 Roadmap

- **US33-D:** Manual Reading API and permissions (paged queries, manual command, controller DTOs).
- **US33-E:** Meter corrections and meter reset/replacement workflows.
- **US33-F:** Fleet Vehicle Details Readings UI tab with manual/correction/reset actions.
- **US33-G:** Final release verification gate.
