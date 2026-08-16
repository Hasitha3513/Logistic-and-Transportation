# Decision

Fleet owns the authoritative, append-only history of vehicle odometer and engine-hour readings. Trip and Fuel record operational readings synchronously through a narrow Fleet-owned application interface. Neither module may maintain a competing current-reading calculation or access Fleet persistence directly.

One `VehicleReading` row represents one reading type. Corrections append a new row and supersede an earlier row; they never update or delete historical facts. A separate, append-only `VehicleMeterReset` record establishes a new meter epoch when a physical meter is replaced or rolls over.

# Context

The repository currently stores mutable `Double` snapshots in `Vehicle.currentOdometerKm` and `Vehicle.engineHours`, trip start/end odometers in `Trip`, and optional `BigDecimal` odometer/engine-hour values in `FuelIssue`. Fuel validates readings against the Fleet snapshot, but no component owns chronological history or correction semantics. This makes delayed events, concurrent submissions, corrections, and physical meter replacement ambiguous.

The existing modular monolith already uses public module interfaces, module-local ports/adapters, one PostgreSQL transaction manager, `TransactionTemplate`, and pessimistic row locks for allocation and lifecycle consistency. US-33 should extend those patterns rather than introduce a service, message broker, or second transaction model.

# Options Considered

1. **Trip owns mileage.** Rejected because readings also originate from Fuel and manual Fleet activity, and engine hours are vehicle facts rather than trip facts.
2. **Fuel owns mileage.** Rejected because trips and manual inspections can produce readings without a fuel issue.
3. **Each source owns its readings and Reporting merges them.** Rejected because validation, correction, idempotency, and latest-value semantics would remain duplicated and inconsistent.
4. **Fleet owns a single append-only ledger.** Chosen because Fleet owns the vehicle and its operational state, and can expose a narrow contract without sharing entities or repositories.

# Chosen Ownership

Fleet owns:

- `VehicleReading`, `VehicleMeterReset`, chronology policy, correction policy, and canonical units;
- latest-reading and paged-history queries;
- the public `VehicleReadingRecorder` module interface used by Trip and Fuel;
- manual entry, correction, and reset use cases;
- persistence, locking, idempotency, permissions, and reading events.

Trip and Fuel retain their source fields as immutable operational evidence and compatibility fields, but those fields are not authoritative for cross-module latest-reading queries. They must not import Fleet JPA entities or repositories.

# Domain Model

`VehicleReading` is one fact per row:

| Field | Contract |
|---|---|
| `readingId` | UUID identity |
| `vehicleId` | Existing Fleet vehicle |
| `readingType` | `ODOMETER` or `ENGINE_HOURS` |
| `value` | Non-negative `BigDecimal`, scale 3 |
| `unit` | Derived canonical unit: `KILOMETER` or `HOUR` |
| `meterEpoch` | Zero-based meter lineage; incremented only by an approved reset |
| `sourceType` | Operational provenance |
| `sourceReferenceId` | Trip/fuel/reset identifier when applicable |
| `recordedAt` | When the physical observation occurred |
| `receivedAt` | When the application accepted the fact |
| `createdAt` | Persistence audit time; initially equal to `receivedAt` |
| `createdBy` | Authenticated application user UUID |
| `correctionOfReadingId` | Directly superseded reading, if this is a correction |
| `correctionReason` | Required for corrections |
| `idempotencyKey` | Required for manual commands; generated deterministically for system sources |
| `notes` | Optional human context, maximum 1,000 characters |

Active source types are `MANUAL`, `TRIP_START`, `TRIP_END`, `FUEL_ISSUE`, `BASELINE`, and `METER_RESET`. `TELEMATICS` and `MAINTENANCE` are reserved enum values but have no active ingestion path in US-33. `CORRECTION` is not a source type; a correction retains the superseded fact's provenance and is identified by `correctionOfReadingId`.

# Chronology Rules

Business chronology uses `recordedAt`, never insertion order. Within the current meter epoch, a new effective value at time `T` must satisfy:

```text
previous effective value at time < T <= new value <= next effective value at time > T
```

The lower/upper comparison applies independently per vehicle, reading type, and meter epoch. Equality is allowed because a stationary vehicle or engine can be observed repeatedly. A backdated in-range fact is valid; a fact above its chronological successor or below its predecessor is rejected with `VEHICLE_READING_CHRONOLOGY_CONFLICT`.

For the same vehicle, type, epoch, and exact `recordedAt`:

- the same value from a different legitimate source may be retained as corroborating evidence;
- a different value is rejected as a chronology conflict;
- a correction is the only operation allowed to supersede that effective value.

Effective queries ignore readings that have a correction child. When timestamps tie, deterministic presentation order is `recordedAt`, then `receivedAt`, then `createdAt`; UUID ordering is not a business rule. Manual readings more than five minutes in the future are rejected. Late readings are accepted only if both chronological neighbors permit them.

# Correction Rules

Readings are immutable. A correction:

1. locks the vehicle and loads the currently effective target;
2. rejects a target that is already superseded or belongs to a closed prior meter epoch;
3. appends a row at the target's `recordedAt`, with the same type, unit, epoch, source, and source reference;
4. references the directly superseded row and requires a non-blank reason and authenticated actor;
5. validates the corrected value against the previous and next effective neighbors, excluding the target;
6. publishes `VehicleReadingCorrected` after persistence.

Only the leaf of a correction chain is effective. The original and all intermediate corrections remain queryable for audit. Latest lookup, reports, future fuel-efficiency calculations, and subsequent chronology validation use only effective leaves. A unique constraint on `correction_of_reading_id` prevents two corrections from superseding the same row.

# Meter Replacement

An ordinary reading can never bypass monotonicity. Meter replacement or rollover uses the separately authorized `VehicleMeterReset` command and record:

- `resetId`, `vehicleId`, `readingType`, `previousReadingId`, `newReadingId`;
- `previousMeterValue`, `newMeterValue`, `effectiveAt`, `reason`;
- `approvedBy`, `createdAt`.

The operation locks the vehicle, requires `VEHICLE_READING_RESET_METER`, verifies the previous effective reading, increments `meterEpoch`, and atomically inserts both the reset record and an initial `METER_RESET` reading in the new epoch. The new value may be lower but must be non-negative. For US-33, a reset cannot be backdated before an already accepted later effective reading; historical re-segmentation is an administrative migration concern. Lifetime-distance reconstruction across epochs is deferred.

# Trip Integration

Trip defines a local output port and adapter to the Fleet public `VehicleReadingRecorder`; it does not import Fleet domain or persistence types. On a successful start, Trip records an `ODOMETER` fact with source `TRIP_START`, reference `tripId`, and `recordedAt=actualStartTime`. On successful completion, it records `TRIP_END` at `actualEndTime`. Odometer becomes required for these vehicle lifecycle operations once US-33 Trip integration is enabled; engine hours are optional until the Trip request contract exposes them.

The Trip lifecycle mutation, history insert, and Fleet reading insert execute synchronously in the existing transaction. Any reading validation or persistence failure rolls back the lifecycle mutation and history. Events are not used for this critical write because asynchronous/eventual recording could leave a completed trip without its authoritative reading.

# Fuel Integration

Fuel defines a local output port and adapter to the same Fleet boundary. Draft, submit, and authorize do not create authoritative readings because values remain editable or the issue may never occur. The transition to `ISSUED` records each supplied odometer/engine-hour value with source `FUEL_ISSUE`, reference `fuelIssueId`, and `recordedAt=issueDateTime`.

Fuel issue status/history and Fleet readings share the existing transaction. Fleet performs chronology and duplicate validation; Fuel does not reproduce it. A failure rolls back the `ISSUED` transition. Fuel retains its captured fields as source evidence.

# Database Strategy

Future migration creates `vehicle_reading` and `vehicle_meter_reset` with UUID keys, `NUMERIC(19,3)` values, `TIMESTAMPTZ` timestamps, non-negative checks, canonical type/unit checks, and foreign keys to vehicle and application user. It includes:

- index `(vehicle_id, reading_type, meter_epoch, recorded_at DESC)`;
- index `(source_type, source_reference_id)`;
- index on `correction_of_reading_id`;
- partial unique index on `(vehicle_id, reading_type, source_type, source_reference_id)` for non-null source references and original rows;
- partial unique index on non-null `idempotency_key`;
- unique `correction_of_reading_id` for correction rows.

Chronological monotonicity is not representable as a simple row check and remains a transactional domain/application invariant. PostgreSQL constraints are the final boundary for source idempotency, correction fan-out, references, units, and non-negative values.

# Concurrency Strategy

Every create, correction, reset, and source-recording command runs in one transaction and first obtains the existing pessimistic write lock on the parent vehicle through `VehicleRepository.findByIdForUpdate`. After locking, it reloads effective previous/next neighbors and validates. This serializes all reading types for one vehicle, is consistent with current allocation locking, and is simpler than advisory locks or `SERIALIZABLE` retries at Phase 2 scale.

The lock order is source aggregate first (Trip or Fuel Issue), then vehicle, matching current cross-module orchestration. Manual Fleet operations lock only the vehicle. Implementations must not introduce a reverse vehicle-then-Trip/Fuel lock path.

# Migration Strategy

Migration is evidence-based:

1. backfill non-null Trip start/end readings using actual lifecycle timestamps and deterministic source keys;
2. backfill non-null readings only from Fuel Issues already in `ISSUED`, using `issueDateTime`;
3. validate each vehicle/type chronology and quarantine/report conflicting legacy facts rather than silently rewriting them;
4. add one `BASELINE` reading from a current Vehicle snapshot only when it supplies otherwise unavailable current provenance and does not contradict trusted timestamped facts;
5. retain existing Vehicle, Trip, and Fuel columns during compatibility rollout.

Vehicle snapshot fields become cached projections maintained from the latest effective reading after US-33 is stable. They are not removed in US-33. A later migration may remove them only after every reader uses the ledger and reconciliation proves the projections match.

# Permissions

- `VEHICLE_READING_VIEW`
- `VEHICLE_READING_CREATE`
- `VEHICLE_READING_CORRECT`
- `VEHICLE_READING_RESET_METER`

Permissions are assigned through migration/bootstrap using current authority conventions. No role name is embedded in Fleet code.

# API Contract

- `GET /vehicles/{vehicleId}/readings`
- `GET /vehicles/{vehicleId}/readings/latest`
- `POST /vehicles/{vehicleId}/readings`
- `POST /vehicles/{vehicleId}/readings/{readingId}/correct`
- `POST /vehicles/{vehicleId}/meter-resets`

History is server-paged and filterable by type, source, and recorded-time range. Requests use canonical units implicitly; responses include units and correction state. Errors use the existing `ApiError`, correlation ID, and stable business codes.

# Deferred Scope

Fuel efficiency, cost per trip, telematics ingestion, GPS reconciliation, fraud/anomaly detection, maintenance scheduling, idle-time analytics, lifetime-distance calculations across meter epochs, and data export are deferred.

# Consequences

- Fleet becomes the only source of current and historical reading truth.
- Trip and Fuel gain synchronous Fleet dependencies through explicit module interfaces but retain hexagonal boundaries.
- Operational commands can fail on chronology conflicts and roll back atomically.
- Historical facts become auditable and correctable without destructive updates.
- Vehicle snapshot fields temporarily duplicate latest values as projections and require reconciliation tests.
- Per-vehicle pessimistic locking favors consistency over maximum same-vehicle write throughput, which is appropriate for human and operational event rates.
