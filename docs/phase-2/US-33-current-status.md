# US-33: Track Mileage â€” Status Audit & Functional Baseline

## 1. Feature Identification

- **Task ID**: `US-33-COMPLETE`
- **User Story**: `US-33 â€” Track Mileage`
- **Actor**: `Fuel Manager`
- **Goal**: As a Fuel Manager, capture odometer/engine hours, calculate mileage across meter resets/replacements, and detect abnormal readings/tampering to ensure consumption and trip information is trustworthy.
- **Status**: **COMPLETE (100% Implemented & Verified)**

---

## 2. Architecture & Authority Invariants

- **Single Ledger Principle**: Fleet module owns `VehicleReading` and `VehicleMeterReset`. No competing mileage tables exist in Trip, Fuel, or Reporting.
- **Cross-Module Communication**:
  - `Trip` communicates with `Fleet` via `TripVehicleReadingPort` implemented by `TripFleetVehicleReadingAdapter`.
  - `Fuel` communicates with `Fleet` via `FuelVehicleReadingPort` implemented by `FleetFuelVehicleReadingAdapter`.
  - `Reporting` & other consumers query mileage summaries via `VehicleMileageQuery`.
- **Chronology & Immutability**:
  - `VehicleReading` records are strictly immutable.
  - Corrections preserve original records in the audit trail and establish a linked superseding entry.
  - Meter resets advance `meterEpoch` (e.g., E0 -> E1), allowing meter restarts while preserving total cumulative distance.
  - Decreasing readings within the same epoch are rejected (`409 Conflict`).
  - Anomaly detection flags abnormal speed spikes or jumps (>150 km/h average between consecutive timestamps).

---

## 3. Slice Breakdown & Verification Status

| Slice ID | Scope / Capabilities | Status | Verification |
|---|---|---|---|
| **US33-A** | REST API & Security (`/vehicles/{id}/readings`, `/latest`, permissions) | **COMPLETE** | `VehicleReadingControllerTest`, `VehicleReadingSecurityIntegrationTest` |
| **US33-B** | Trip lifecycle integration (`TRIP_START`, `TRIP_END` recordings) | **COMPLETE** | `TripVehicleReadingIntegrationTest` |
| **US33-C** | Fuel issue integration (`FUEL_ISSUE` reading recording) | **COMPLETE** | `FuelIssueServiceTest` |
| **US33-D** | Auditable corrections & physical meter replacement/reset | **COMPLETE** | `VehicleReadingServiceTest`, `VehicleReadingApiIntegrationTest` |
| **US33-E** | Mileage & trip distance engine, multi-epoch calculation, abnormal detection | **COMPLETE** | `VehicleReadingServiceTest`, `VehicleReadingApiIntegrationTest` |
| **US33-F** | React frontend components, forms, modals, tables, and statistics | **COMPLETE** | `VehicleReadingsSection.test.tsx`, `vitest run`, `vite build` |
| **US33-G** | Final acceptance, regression testing, and completion audit | **COMPLETE** | `mvn test` (212/212 passed), `npm run test` (55/55 passed) |

---

## 4. Database Migrations

- `V15__vehicle_reading_permissions.sql`: Added `VEHICLE_READING_VIEW`, `VEHICLE_READING_CREATE`, `VEHICLE_READING_CORRECT`, `VEHICLE_READING_RESET_METER`.
- `V16__vehicle_meter_reset.sql`: Created `vehicle_meter_reset` table with unique epoch constraints and FK to `vehicle`.