# US33-D: Controlled VehicleReading Correction and Meter Reset / Replacement Architecture Document

## 1. Executive Summary

US33-D completes the lifecycle management of authoritative `VehicleReading` records in the Fleet module of the Modular Monolith by implementing:
1. **Controlled Append-Only Correction Workflow**: Correction of erroneous historical readings (e.g. human typos) without in-place mutation, preserving full immutable ledger integrity while maintaining monotonic chronology with effective temporal neighbors.
2. **Physical Meter Reset / Replacement Workflow**: Explicit business lifecycle for physical odometer/hour-meter cluster replacement or hardware rollover, transitioning the vehicle into a newly incremented `meterEpoch`, establishing an initial baseline in the new epoch, and permitting lower starting meter values (e.g. 245,000 km $\rightarrow$ 0 km) while preserving historical epoch monotonicity.

---

## 2. Architectural Design & Invariants

### 2.1 Append-Only Immutability & Correction Lineage

* **No In-Place Mutation**: Existing `VehicleReading` rows are never updated or deleted.
* **Correction Pointer**: A correction creates a new `VehicleReading` row with `correctionOfReadingId` pointing to the target reading it supersedes.
* **Unique Correction Constraint**: A database unique index `uq_vehicle_reading_one_correction` on `(correction_of_reading_id)` ensures that a reading cannot be superseded by more than one correction concurrently.
* **Self-Correction Check Constraint**: Database check constraint `chk_vehicle_reading_no_self_correction` guarantees `correction_of_reading_id <> reading_id`.
* **Inheritance & Fact Preservation**: A correction retains the target's original `readingType`, `recordedAt`, `meterEpoch`, `sourceType`, `sourceReferenceId`, and `unit`, but records a newly corrected `value`, an explicit mandatory non-blank `correctionReason`, the actor ID in `createdBy`, and the current timestamp in `createdAt` / `receivedAt`.

### 2.2 Effective Reading Definition & Chronology Policy

* **Effective Reading**: A reading $R$ is effective if and only if:
  $$\neg \exists C \in \text{vehicle\_reading} \text{ such that } C.\text{correction\_of\_reading\_id} = R.\text{id}$$
* **Chronological Neighborhood**: When validating a candidate correction at time $T$, effective previous and next readings are queried excluding the superseded target.
* **Bounded Monotonicity**: The candidate's corrected value must satisfy:
  $$\text{previous.value} \le \text{candidate.value} \le \text{next.value}$$
  (within the same meter epoch).

### 2.3 Physical Meter Replacement & Meter Epochs

* **Separation of Concerns**: A meter reset is distinct from an error correction. It models a physical event (odometer cluster replacement under warranty or at end-of-life).
* **Epoch Increment**: Each meter reset increments the vehicle's `meterEpoch` ($E \rightarrow E + 1$) for that reading type.
* **Initial Epoch Reading**: The reset creates a new `VehicleReading` with `sourceType = METER_RESET`, `sourceReferenceId = reset_id`, `meterEpoch = newEpoch`, and the new starting meter value (e.g. `0.000 km`).
* **Audit Record**: A dedicated `VehicleMeterReset` record is persisted in `vehicle_meter_reset`, linking `previousReadingId`, `previousMeterValue`, `newReadingId`, `newMeterValue`, `effectiveAt`, `reason`, `createdBy`, and `approvedBy`.
* **Monotonicity Scope**: Monotonicity is strictly enforced **per vehicle, per reading type, per epoch**. A reading in epoch 1 with value `0 km` or `50 km` is fully valid following epoch 0 with value `245,000 km`.

---

## 3. Database Schema (Flyway `V15`)

```sql
INSERT INTO app_permission (code, description, active) VALUES
    ('VEHICLE_READING_VIEW', 'View vehicle odometer and engine-hour readings', TRUE),
    ('VEHICLE_READING_CREATE', 'Create manual vehicle readings', TRUE),
    ('VEHICLE_READING_CORRECT', 'Correct recorded vehicle readings', TRUE),
    ('VEHICLE_READING_RESET_METER', 'Record physical vehicle meter replacement or reset', TRUE);

CREATE TABLE vehicle_meter_reset (
    reset_id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    reading_type VARCHAR(30) NOT NULL,
    previous_reading_id UUID,
    previous_meter_value NUMERIC(19,3) NOT NULL,
    new_reading_id UUID NOT NULL,
    new_meter_value NUMERIC(19,3) NOT NULL,
    effective_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason TEXT NOT NULL,
    created_by UUID NOT NULL,
    approved_by UUID,
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_vehicle_meter_reset_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    CONSTRAINT fk_vehicle_meter_reset_created_by FOREIGN KEY (created_by) REFERENCES app_user(id),
    CONSTRAINT fk_vehicle_meter_reset_approved_by FOREIGN KEY (approved_by) REFERENCES app_user(id),
    CONSTRAINT fk_vehicle_meter_reset_previous FOREIGN KEY (previous_reading_id) REFERENCES vehicle_reading(reading_id),
    CONSTRAINT fk_vehicle_meter_reset_new FOREIGN KEY (new_reading_id) REFERENCES vehicle_reading(reading_id),
    CONSTRAINT chk_vehicle_meter_reset_type CHECK (reading_type IN ('ODOMETER', 'ENGINE_HOURS')),
    CONSTRAINT chk_vehicle_meter_reset_prev_value CHECK (previous_meter_value >= 0),
    CONSTRAINT chk_vehicle_meter_reset_new_value CHECK (new_meter_value >= 0),
    CONSTRAINT chk_vehicle_meter_reset_reason CHECK (LENGTH(TRIM(reason)) > 0)
);

CREATE INDEX idx_vehicle_meter_reset_vehicle
    ON vehicle_meter_reset(vehicle_id, reading_type, effective_at DESC);

CREATE INDEX idx_vehicle_meter_reset_new_reading
    ON vehicle_meter_reset(new_reading_id);

ALTER TABLE vehicle_reading
    ADD CONSTRAINT chk_vehicle_reading_no_self_correction
    CHECK (correction_of_reading_id IS NULL OR reading_id <> correction_of_reading_id);
```

---

## 4. REST API & Authorization Endpoints

| Endpoint | Method | Required Authority | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/vehicles/{vehicleId}/readings` | `GET` | `VEHICLE_READING_VIEW` | Paginated search of all readings with derived `status` |
| `/api/v1/vehicles/{vehicleId}/readings/latest` | `GET` | `VEHICLE_READING_VIEW` | Effective latest Odometer and Engine Hours |
| `/api/v1/vehicles/{vehicleId}/readings` | `POST` | `VEHICLE_READING_CREATE` | Record manual reading |
| `/api/v1/vehicles/{vehicleId}/readings/{readingId}/correct` | `POST` | `VEHICLE_READING_CORRECT` | Submit append-only correction for reading |
| `/api/v1/vehicles/{vehicleId}/meter-resets` | `POST` | `VEHICLE_READING_RESET_METER` | Record physical meter replacement / reset |
| `/api/v1/vehicles/{vehicleId}/meter-resets` | `GET` | `VEHICLE_READING_VIEW` | List meter replacement history |

---

## 5. Frontend UI Implementation

* **Component**: `VehicleReadingsSection.tsx` embedded in `ResourceListPage.tsx` vehicle drawer.
* **Metrics Banner**: Authoritative Odometer (km), Authoritative Engine Hours (hrs), and active Meter Epoch badge.
* **Ledger Table**: Full chronological display with derived status badges:
  * `[Active]` (Green)
  * `[Corrected]` (Default strikethrough)
  * `[Correction]` (Processing blue)
* **Modals**:
  * "Add Reading" modal (manual entry with client & backend validation)
  * "Correct Reading" modal (shows target facts, enforces mandatory non-blank audit reason)
  * "Record Meter Replacement" modal (warns user of epoch transition, collects new starting meter value and mandatory reason)
* **Meter Replacement Tab**: Historical log of all meter replacement events for the vehicle.

---

## 6. Verification Results

* **Domain & Application Unit Tests**: 21/21 tests in `VehicleReadingServiceTest` passing.
* **Security & Authorization Tests**: `BusinessAuthorizationIntegrationTest` and `LocalIdentityBootstrapIntegrationTest` passing.
* **PostgreSQL Testcontainers Tests**: 21/21 production invariant tests in `PostgreSqlProductionInvariantIntegrationTest` passing.
* **Full Backend Verification**: `mvn clean verify` $\rightarrow$ **225 tests passing, 0 failures, 0 errors**.
* **Frontend Verification**: `npm test` $\rightarrow$ **57 tests passing, 0 failures, 0 errors**; ESLint clean (0 warnings), Vite production build clean.
