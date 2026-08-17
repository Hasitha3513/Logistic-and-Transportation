# US-34 â€” Fuel Cost Per Trip Documentation

## 1. Executive Summary

- **Task ID**: `US-34`
- **User Story**: As a Fuel Manager / Operations Manager, I want fuel cost allocated to trips and shared operational activity with traceability to the underlying fuel transactions, so that trip fuel cost and variance can be calculated accurately.
- **Module Ownership**:
  - `Fuel` owns authoritative fuel cost and consumption calculation and allocation.
  - `Fleet` owns odometer/distance truth (via `VehicleMileageQuery.calculateTripDistance(tripId)`).
  - `Trip` owns trip operational state and references.
  - Cross-module boundary is maintained using Hexagonal architecture ports (`TripDistancePort` -> `FleetFuelTripDistanceAdapter`).
- **Status**: **COMPLETE**

---

## 2. Implemented Architecture & Design

### 2.1 Backend Domain & Models (`com.transportlogistics.app.fuel`)

1. **`TripFuelCost`**: Aggregated domain read model representing total allocated cost, quantity, distance, unit efficiency metrics, and line breakdown.
2. **`TripFuelCostLine`**: Record representing an individual contributing fuel issue transaction, including voucher number, issue timestamp, fuel type, quantity (L), unit price, calculated line cost, and pricing source.
3. **`PricingSource`**: Enum indicating where price was resolved:
   - `EXPLICIT_ISSUE_PRICE`: Unit price captured on the `FuelIssue` record.
   - `PRICE_CATALOGUE`: Unit price looked up via the vendor catalogue active at issue time.
   - `UNPRICED`: Missing price requiring operational/management review.
4. **`TripFuelCostCalculationStatus`**: Enum indicating completeness of calculation:
   - `COMPLETE`: All contributing issues priced and authoritative distance available.
   - `PARTIAL`: Fuel issues exist with prices, but distance is pending or some issues lack pricing.
   - `UNAVAILABLE`: No fuel issues recorded for the trip.

### 2.2 Ports & Adapters

- **Inbound Port**: `TripFuelCostUseCase` (`getTripFuelCost(UUID tripId)`).
- **Application Service**: `TripFuelCostService` implementing `TripFuelCostUseCase`.
- **Outbound Port**: `TripDistancePort` declaring `getTripDistance(UUID tripId)`.
- **Fleet Adapter**: `FleetFuelTripDistanceAdapter` implementing `TripDistancePort` by calling `VehicleMileageQuery.calculateTripDistance(tripId)`.
- **Persistence Adapter**: `FuelIssuePersistenceAdapter.findByTripId(UUID tripId)`.
- **Web Adapter**: `TripFuelCostController` exposing `GET /trips/{tripId}/fuel-cost`.

### 2.3 Pricing & Consumption Calculation Rules

- Uses `BigDecimal` with `RoundingMode.HALF_UP` for all monetary and quantity calculations.
- Only operationally issued vouchers (`status = ISSUED`) are included; drafts, pending authorizations, and cancelled vouchers are excluded.
- Calculates `costPerKm = totalFuelCost / tripDistanceKm` when `tripDistanceKm > 0`.
- Calculates `litersPer100Km = (totalQuantityLiters * 100) / tripDistanceKm` when `tripDistanceKm > 0`.
- If distance is 0 or unavailable, `costPerKm` and `litersPer100Km` are set to `null` to prevent division by zero.

---

## 3. Database Migration & Security

- **Flyway Migration**: `V17__fuel_cost_permissions.sql`
  - Adds permission `FUEL_COST_VIEW`.
- **Security Configuration**:
  - `GET /trips/*/fuel-cost` secured with `hasAuthority('FUEL_COST_VIEW')`.
  - Roles granted access: `ADMIN`, `OPERATIONS`, `FUEL_MANAGER`.

---

## 4. Frontend Integration

1. **Hook**: `useTripFuelCost(tripId?: string)` using `@tanstack/react-query`.
2. **Component**: `TripFuelCostSection.tsx`
   - Summary statistics cards: Total Fuel Cost, Total Fuel Quantity, Trip Distance, Cost / KM, Consumption (L/100km).
   - Incomplete / Partial state alert banners.
   - Breakdown table of contributing fuel issues with pricing source tags (`Issue Price`, `Price Catalogue`, `Unpriced`).
   - Empty state when no issues are associated.
3. **Trip Details Page**: Added `Fuel Cost` tab to `TripDetailsPage.tsx`.

---

## 5. Verification Matrix

| Area | Scope | Result | Details |
|---|---|---|---|
| Domain / Service Tests | `TripFuelCostServiceTest` | **PASSED** (10/10) | Verified price hierarchy, explicit price, catalogue fallback, unpriced, distance integration, zero distance, and partial status |
| Web Controller Tests | `TripFuelCostControllerTest` | **PASSED** (3/3) | Verified status 200, 403, and JSON payload serialization |
| Security Tests | `TripFuelCostSecurityIntegrationTest` | **PASSED** (1/1) | Verified `FUEL_COST_VIEW` authorization vs unprivileged actor |
| Full API Tests | `TripFuelCostApiIntegrationTest` | **PASSED** (1/1) | Verified end-to-end integration of Trip + Fleet reading + Fuel issue -> `/trips/{id}/fuel-cost` |
| PostgreSQL Invariants | `PostgreSqlProductionInvariantIntegrationTest` | **PASSED** (14/14) | Real PostgreSQL 16 schema migration validation (17 migrations applied cleanly) |
| Total Backend Tests | `mvn clean verify` | **PASSED** (240/240) | 100% build & test pass |
| Frontend Unit Tests | `TripFuelCostSection.test.tsx` | **PASSED** (2/2) | Complete and partial cost calculation render |
| Total Frontend Tests | `vitest run` | **PASSED** (57/57) | 100% pass |
| Frontend Build & Lint | `npm run lint` & `npm run build` | **PASSED** | 0 ESLint errors/warnings; production bundle generated cleanly |