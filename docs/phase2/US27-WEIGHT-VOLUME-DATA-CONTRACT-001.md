# US-27 — Calculate Weight and Volume: Authoritative Vehicle Capacity Master Data Contract (US27-WEIGHT-VOLUME-DATA-CONTRACT-001)

**Date:** August 27, 2026  
**Status:** FROZEN AND IMPLEMENTED  
**Story:** US-27 — Calculate Weight and Volume (Validate Weight and Volume)  
**Task:** P2-WEIGHT-VOLUME-DATA-001  

---

## 1. Authoritative Requirements & Context

### 1.1 Source Definition
- **Primary Actor:** Freight Manager
- **Goal:** Provide authoritative physical measurement validation across manifested cargo loads, verifying net weight, gross weight, cubic volume, and vehicle capacity to prevent physical overloading or compliance breaches.
- **Included Use Cases (from Source Specification):**
  1. `Calculate Gross Weight`: Total cargo weight + vehicle tare weight / container tare weight.
  2. `Calculate Net Weight`: Sum of manifested cargo item weights.
  3. `Calculate Cubic Volume`: Sum of manifested cargo item cubic dimensions.
  4. `Prevent Overload`: Validate net weight <= vehicle payload capacity, and gross weight <= vehicle gross vehicle weight (GVW).
  5. `Validate Axle Load`: Evaluate axle load against vehicle axle load ratings when configuration exists.
  6. `Record Weight Discrepancy`: Surface missing measurements and structural capacity violations.

---

## 2. Bounded-Context Ownership

1. **Fleet Management (`fleet` module):**
   - Authoritative owner of physical vehicle specifications, tare weight, gross vehicle weight ratings (GVWR/GVW), usable cubic cargo volume capacity, axle count, and axle load limits.
   - Publishes read-only summaries via `FleetReportingQuery` and `FleetVehicleSummary`.
2. **Freight Operations (`freight` module):**
   - Authoritative owner of cargo manifest item measurements (quantities, net weight, dimensions, fragile/temperature classifications).
   - Authoritative owner of Load Plans and US-27 calculation & validation engines.
   - Consumes vehicle capacity master data strictly through the provider-neutral inbound lookup port `VehicleLoadSpaceLookupPort`.
3. **Trip Operations (`trip` module):**
   - Consumes load validation outcomes during dispatch readiness checks.

---

## 3. Physical Data Matrix & Reconciled Semantics

### 3.1 Cargo Data Facts (Freight Owned)
- **Net Weight:** Measured per item in kilograms (`kg`).
- **Dimensions:** Length, width, height measured in meters (`m`) or centimeters (`cm`), yielding cubic volume in cubic meters ($m^3$).
- **Quantity:** Unit count multiplier.

### 3.2 Vehicle Capacity Master Data (Fleet Owned)

| Field Name | DB Column | Type | Unit | Nullable / Default | Semantic Definition & Invariant |
|---|---|---|---|---|---|
| `capacityKg` | `capacity_kg` | `DOUBLE PRECISION` | `kg` | `NULL` | **Payload Capacity:** Maximum allowable weight of cargo and passengers the vehicle can legally transport. |
| `tareWeightKg` | `tare_weight_kg` | `DOUBLE PRECISION` | `kg` | `NULL` | **Tare / Kerb Weight:** Weight of the empty vehicle ready for service (fluids, fuel tank, standard equipment). |
| `grossVehicleWeightKg` | `gross_vehicle_weight_kg` | `DOUBLE PRECISION` | `kg` | `NULL` | **Gross Vehicle Weight (GVW/GVWR):** Maximum legal operating weight of the fully loaded vehicle. Invariant: `grossVehicleWeightKg >= tareWeightKg`. |
| `cargoVolumeCapacityM3` | `cargo_volume_capacity_m3` | `DOUBLE PRECISION` | $m^3$ | `NULL` | **Cargo Volume Capacity:** Usable cubic cargo space available inside the cargo body/trailer. |
| `axleCount` | `axle_count` | `INT` | integer | `NULL` | **Axle Count:** Total number of load-bearing axles on the vehicle (must be >= 1). |
| `maxAxleLoadKg` | `max_axle_load_kg` | `DOUBLE PRECISION` | `kg` | `NULL` | **Max Axle Load Rating:** Maximum allowable static load per axle. |

---

## 4. Reconciliation of Existing `capacity_kg`

- **Pre-Existing Semantics:** In the initial MVP schema (`V1__baseline.sql`), `vehicle.capacity_kg` was created without formal metadata.
- **Reconciliation Decision:** `capacity_kg` is formally defined as **Payload Capacity** (maximum cargo weight capacity in kilograms).
- **Backwards Compatibility:**
  - `capacity_kg` is preserved in table `vehicle` and domain models without destructive schema changes or column renames.
  - In frontend UI, it is clearly labeled as **Payload capacity (kg)**.

---

## 5. Axle Load Decision Gate

- **Gate Status:** `NOT_REQUIRED_IN_MVP_DATA_FOUNDATION / BLOCKED_PENDING_AXLE_DISTRIBUTION_CONTRACT` for dynamic distribution algorithms.
- **Rationale:** Master data fields `axle_count` and `max_axle_load_kg` are introduced additively on `Vehicle` to store authoritative manufacturer ratings. However, complex center-of-gravity, multi-axle bridge formula, and longitudinal weight distribution calculations are gated because truthful axle-load computation requires axle spacing geometry and 3D cargo placement coordinates not present in 2D load plans.
- A simplistic `totalWeight / axleCount` rule is explicitly forbidden.

---

## 6. Legacy Data & Unknown Handling

- All newly added database columns are `NULLABLE`.
- Historical vehicle records retain `NULL` for unrecorded capacity fields.
- **No False Defaulting:** Missing capacity data is preserved as `null`/UNKNOWN and is NEVER converted to `0.0`.
- During US-27 calculation (in `P2-WEIGHT-VOLUME-CALC-002`), missing vehicle capacity facts will generate explicit diagnostics (e.g., `VEHICLE_VOLUME_CAPACITY_UNAVAILABLE`, `VEHICLE_GVW_UNAVAILABLE`) rather than failing silently or assuming zero capacity.

---

## 7. Database Migration

- **Migration Version:** `V39__vehicle_capacity_master_data.sql`
- **Previous Version:** `V38__load_plan_readiness.sql`
- **Forward-Only SQL:**
  ```sql
  ALTER TABLE vehicle
      ADD COLUMN tare_weight_kg DOUBLE PRECISION,
      ADD COLUMN gross_vehicle_weight_kg DOUBLE PRECISION,
      ADD COLUMN cargo_volume_capacity_m3 DOUBLE PRECISION,
      ADD COLUMN axle_count INT,
      ADD COLUMN max_axle_load_kg DOUBLE PRECISION;

  ALTER TABLE vehicle
      ADD CONSTRAINT chk_vehicle_tare_weight CHECK (tare_weight_kg IS NULL OR tare_weight_kg >= 0),
      ADD CONSTRAINT chk_vehicle_gvw CHECK (gross_vehicle_weight_kg IS NULL OR gross_vehicle_weight_kg >= 0),
      ADD CONSTRAINT chk_vehicle_cargo_volume CHECK (cargo_volume_capacity_m3 IS NULL OR cargo_volume_capacity_m3 >= 0),
      ADD CONSTRAINT chk_vehicle_axle_count CHECK (axle_count IS NULL OR axle_count > 0),
      ADD CONSTRAINT chk_vehicle_max_axle_load CHECK (max_axle_load_kg IS NULL OR max_axle_load_kg >= 0),
      ADD CONSTRAINT chk_vehicle_gvw_gte_tare CHECK (
          gross_vehicle_weight_kg IS NULL OR tare_weight_kg IS NULL OR gross_vehicle_weight_kg >= tare_weight_kg
      );
  ```

---

## 8. Cross-Module Lookup Boundary

- **Public Fleet Query (`FleetReportingQuery`):** Exposes `FleetVehicleSummary` with all 6 capacity facts.
- **Inbound Freight Lookup Port (`VehicleLoadSpaceLookupPort`):** Exposes `VehiclePlanningView` with all 6 capacity facts to Freight/Load Planning.
- **Decoupling:** `Freight` does not reference `VehicleEntity`, JPA repositories, or internal Fleet classes.

---

## 9. RBAC & Security

- `VEHICLE_VIEW`: Permitted to view all vehicle master capacity facts.
- `VEHICLE_MANAGE`: Permitted to create, edit, and deactivate vehicle master records with capacity facts.
- No new permission tokens created; existing RBAC structure is maintained.

---

## 10. Status Traceability

- **US-26:** COMPLETE (Readiness & structural validation frozen in `P2-LOAD-ACCEPTANCE-001`).
- **US-27:** PARTIAL — Data Foundation COMPLETE (`P2-WEIGHT-VOLUME-DATA-001`).
- **Next Slice:** `P2-WEIGHT-VOLUME-CALC-002` (Implement authoritative weight/volume calculations and capacity validation engine).
