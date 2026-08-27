# US-27 — Calculate Weight and Volume: Calculation and Capacity Validation Engine (US27-WEIGHT-VOLUME-CALCULATION-002)

**Date:** August 27, 2026  
**Status:** IMPLEMENTATION COMPLETE, ACCEPTANCE PENDING  
**Story:** US-27 — Calculate Weight and Volume (Validate Weight and Volume)  
**Task:** P2-WEIGHT-VOLUME-CALC-002  

---

## 1. Domain Ownership & Bounded Contexts

1. **`fleet` Module:**
   - Authoritative owner of Vehicle capacity master facts:
     - `capacityKg` (Payload capacity in kg)
     - `tareWeightKg` (Unladen empty vehicle weight in kg)
     - `grossVehicleWeightKg` (Gross Vehicle Weight rating in kg; $GVW \ge Tare$)
     - `cargoVolumeCapacityM3` (Usable cubic cargo space in $m^3$)
     - `axleCount` (Axle count, integer $\ge 1$)
     - `maxAxleLoadKg` (Maximum allowable axle rating in kg)
2. **`freight` Module:**
   - Authoritative owner of cargo manifest measurements (item count, dimensions, weight) and load planning placements.
   - Authoritative owner of `WeightVolumeCalculationEngine` and `validateWeightAndVolume` orchestration.
   - Consumes vehicle capacity strictly through `VehicleLoadSpaceLookupPort`.

---

## 2. Calculation Formulas & Unit Normalization

### 2.1 Unit Conversions
- **Weight Units:**
  - `KG` $\to$ $\times 1$
  - `G` $\to$ $\times 0.001$
  - `TONNE` $\to$ $\times 1000$
- **Dimension Units:**
  - `M` $\to$ $\times 1$
  - `CM` $\to$ $\times 0.01$
  - `MM` $\to$ $\times 0.001$

### 2.2 Core Formulas
1. **Line Weight:** $\text{lineWeightKg} = \text{quantity} \times \text{unitWeightKg}$
2. **Total Cargo Weight (Net Weight):** $\text{totalCargoWeightKg} = \sum \text{lineWeightKg}$
3. **Line Volume:** $\text{lineVolumeM3} = \text{quantity} \times (\text{lengthM} \times \text{widthM} \times \text{heightM})$
4. **Total Cargo Volume (Cubic Volume):** $\text{totalCargoVolumeM3} = \sum \text{lineVolumeM3}$
5. **Projected Gross Weight:** $\text{projectedGrossWeightKg} = \text{tareWeightKg} + \text{totalCargoWeightKg}$
6. **Payload Utilization (%):** $\text{payloadUtilizationPercent} = \frac{\text{totalCargoWeightKg}}{\text{payloadCapacityKg}} \times 100$
7. **Volume Utilization (%):** $\text{volumeUtilizationPercent} = \frac{\text{totalCargoVolumeM3}}{\text{volumeCapacityM3}} \times 100$

All calculations use `BigDecimal` arithmetic with `RoundingMode.HALF_UP` (2 decimal places for weight/utilization, 3 decimal places for volume).

---

## 3. Validation Rules & Structured Codes

| Check | Condition | Outcome | Machine-Readable Violation Code |
|---|---|---|---|
| **Payload Capacity** | $\text{totalCargoWeightKg} \le \text{payloadCapacityKg}$ | `PASS` | None |
| **Payload Overload** | $\text{totalCargoWeightKg} > \text{payloadCapacityKg}$ | `FAIL` | `VEHICLE_PAYLOAD_EXCEEDED` |
| **Volume Capacity** | $\text{totalCargoVolumeM3} \le \text{volumeCapacityM3}$ | `PASS` | None |
| **Volume Overload** | $\text{totalCargoVolumeM3} > \text{volumeCapacityM3}$ | `FAIL` | `VEHICLE_VOLUME_CAPACITY_EXCEEDED` |
| **GVW Limit** | $\text{projectedGrossWeightKg} \le \text{grossWeightLimitKg}$ | `PASS` | None |
| **GVW Overload** | $\text{projectedGrossWeightKg} > \text{grossWeightLimitKg}$ | `FAIL` | `VEHICLE_GVW_EXCEEDED` |
| **Axle Load** | Gated pending 3D placement geometry | `INCOMPLETE` | `LOAD_AXLE_DATA_UNAVAILABLE` |

---

## 4. Unknown & Incomplete Data Handling

- Missing or `null` values are **NEVER defaulted to 0**.
- When input data is absent, the specific check evaluates to `INCOMPLETE` and registers explicit `missingData` items:
  - `CARGO_ITEM_WEIGHT_DATA_MISSING`
  - `CARGO_ITEM_DIMENSIONS_DATA_MISSING`
  - `VEHICLE_PAYLOAD_CAPACITY_MISSING`
  - `VEHICLE_VOLUME_CAPACITY_UNAVAILABLE`
  - `VEHICLE_GVW_DATA_MISSING`
  - `VEHICLE_AXLE_LIMITS_UNAVAILABLE`
- `overallOutcome` is `FAIL` if any rule fails, `INCOMPLETE` if any rule lacks required data, and `PASS` only when all required rules succeed.

---

## 5. API & REST Interface

- **Endpoint:** `POST /api/v1/freight/load-plans/{id}/validate-weight-volume`
- **RBAC Requirement:** `LOAD_PLAN_MANAGE` (or `LOAD_PLAN_VIEW` for inspection endpoints)
- **Response Structure (`LoadValidationResultResponse`):**
  ```json
  {
    "loadPlanId": "UUID",
    "validatedAt": "ISO-8601",
    "validatedBy": "username",
    "overallOutcome": "PASS | FAIL | INCOMPLETE",
    "cargoWeightKg": 4500.00,
    "payloadCapacityKg": 5000.00,
    "payloadUtilizationPercent": 90.00,
    "cargoVolumeM3": 20.000,
    "volumeCapacityM3": 25.000,
    "volumeUtilizationPercent": 80.00,
    "projectedGrossWeightKg": 7500.00,
    "grossWeightLimitKg": 8000.00,
    "tareWeightKg": 3000.00,
    "payloadResult": "PASS",
    "volumeResult": "PASS",
    "gvwResult": "PASS",
    "axleResult": "INCOMPLETE",
    "violations": [],
    "missingData": []
  }
  ```

---

## 6. Frontend Presentation (`LoadPlanDetailsPage.tsx`)

- Displays rich validation summary block with:
  - Overall status badge (`PASS`, `FAIL`, `INCOMPLETE`).
  - Individual status tags for Payload, Volume, GVW, and Axle checks.
  - Formatted metrics for Cargo Weight, Payload Capacity, Payload Utilization %, Cargo Volume, Volume Capacity, Volume Utilization %, Projected Gross Weight, GVW Limit, and Tare Weight.
  - Actionable alert banners for violations and missing data badges.

---

## 7. Status & Boundaries Traceability

- **US-26 (Plan Loads):** COMPLETE — UNCHANGED. `STRUCTURALLY_READY` semantics preserved independently from capacity validation.
- **US-27 (Calculate Weight and Volume):** `PARTIAL — IMPLEMENTATION COMPLETE, ACCEPTANCE PENDING`.
- **US-29 (Freight Reports):** `BLOCKED_BY_TENANT_FOUNDATION` (Tenant PAUSED).
- **US-30 (Cargo Exceptions):** `MISSING`.
- **Next Step:** `P2-WEIGHT-VOLUME-ACCEPTANCE-003` (Dedicated Acceptance and E2E Closure).
