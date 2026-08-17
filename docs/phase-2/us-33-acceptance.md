# US-33 Mileage & KM Tracking — Acceptance & Completion Document

**Status**: COMPLETED & VERIFIED  
**Architecture Style**: Spring Modulith / Hexagonal Architecture / PostgreSQL Append-Only Ledger  
**Authoritative Owner**: `fleet` Module  

---

## 1. Executive Summary

US-33 establishes authoritative odometer and engine-hour tracking across the Transport & Logistics platform. All reading facts are stored in an append-only ledger owned by the `fleet` module with strict chronology validation, meter epoch isolation, and physical meter replacement workflows.

Cross-module operations (Trip execution, Fuel Issue issuance) record facts directly into the Fleet ledger synchronously within their transaction boundaries via clean hexagonal output ports and adapters.

---

## 2. Acceptance Criteria Verification

| ID | Requirement | Verification Method | Status |
|---|---|---|---|
| **AC-1** | Fleet-Owned Append-Only Ledger | `VehicleReading` domain aggregate, Flyway V14 `vehicle_reading`, immutable entity | **PASS** |
| **AC-2** | Monotonic Chronology & Neighbor Rules | `VehicleReadingChronologyPolicy`, `VehicleReadingServiceTest` (21/21 tests) | **PASS** |
| **AC-3** | Trip Start/End Integration | `FleetVehicleReadingAdapter`, `TripService` synchronous recording with rollback safety | **PASS** |
| **AC-4** | Fuel Issue Integration at `ISSUED` | `FleetFuelReadingAdapter`, atomic odometer + engine hours recording | **PASS** |
| **AC-5** | Controlled Append-Only Correction | `CorrectReadingCommand`, audit reason required, superseded link | **PASS** |
| **AC-6** | Meter Replacement & Epoch Increment | Flyway V15 `vehicle_meter_reset`, `ResetMeterCommand`, epoch isolation | **PASS** |
| **AC-7** | Public Query Boundary (`VehicleMileageQuery`) | Period mileage summary & Trip distance calculation across epochs | **PASS** |
| **AC-8** | Multi-Epoch Distance Calculation | Correct arithmetic across reset epochs (`calculateMetricAcrossEpochs`) | **PASS** |
| **AC-9** | Coverage Determination (`CoverageStatus`) | `COMPLETE`, `PARTIAL`, `NO_DATA` status with operational diagnostics | **PASS** |
| **AC-10** | REST Endpoints & Permissions | `GET /vehicles/{id}/mileage-summary`, `GET /trips/{id}/distance` | **PASS** |
| **AC-11** | Frontend UI & Testing | `VehicleReadingsSection.tsx` with Period Mileage tab, Vitest (59/59 tests) | **PASS** |
| **AC-12** | Database & Persistence Invariants | `VehicleReadingRepositoryIntegrationTest` & `PostgreSqlProductionInvariantIntegrationTest` | **PASS** |

---

## 3. Public API Contract

### 3.1. REST Endpoints

1. **Get Vehicle Mileage & Utilization Summary**:
   - `GET /api/v1/vehicles/{vehicleId}/mileage-summary?from={from}&to={to}&includeSourceBreakdown={bool}`
   - Permission: `VEHICLE_READING_VIEW`
   - Response: `VehicleMileageSummary`

2. **Get Authoritative Trip Distance**:
   - `GET /api/v1/trips/{tripId}/distance`
   - Permission: `TRIP_VIEW`
   - Response: `TripDistanceSummary`

3. **Get Latest Vehicle Readings**:
   - `GET /api/v1/vehicles/{vehicleId}/readings/latest`
   - Permission: `VEHICLE_READING_VIEW`

4. **Record Manual Reading**:
   - `POST /api/v1/vehicles/{vehicleId}/readings`
   - Permission: `VEHICLE_READING_CREATE`

5. **Submit Correction**:
   - `POST /api/v1/vehicles/{vehicleId}/readings/{readingId}/correct`
   - Permission: `VEHICLE_READING_CORRECT`

6. **Record Physical Meter Replacement**:
   - `POST /api/v1/vehicles/{vehicleId}/meter-resets`
   - Permission: `VEHICLE_READING_RESET_METER`

---

## 4. Test Suite Summary

- **Backend Unit Tests**:
  - `VehicleMileageServiceTest`: 10/10 tests **PASSED**
  - `VehicleReadingServiceTest`: 21/21 tests **PASSED**
- **Persistence & Integration Tests**:
  - `VehicleReadingRepositoryIntegrationTest`: 1/1 test **PASSED**
  - `PostgreSqlProductionInvariantIntegrationTest`: multi-epoch scenario verified.
- **Frontend Vitest Tests**:
  - 10 test suites, 59/59 tests **PASSED**
