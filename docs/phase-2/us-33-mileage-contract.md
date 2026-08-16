# US-33 — Mileage & KM Tracking Contract

Status: **US33-A foundation implemented; US33-B through US33-G remain planned**  
Authority: [ADR-vehicle-reading-authority.md](../adr/ADR-vehicle-reading-authority.md)

US33-A implements the Fleet domain model, chronology policy, application/output ports, future public recorder boundary,
append-only persistence, latest/history queries, vehicle-lock concurrency, snapshot projection compatibility,
`VehicleReadingRecorded`, Flyway V14, and H2/PostgreSQL verification. Per the approved slice plan, it does not expose
REST endpoints or permissions and does not integrate Trip, Fuel, corrections, resets, or frontend UI.

## 1. User Story

As a Fuel/Fleet Manager, I want odometer and engine-hour readings captured consistently across operational events so vehicle utilization, fuel efficiency, and future cost-per-trip calculations can use trustworthy historical readings.

US-33 establishes authoritative facts. It does not calculate consumption, efficiency, or cost.

## 2. Scope

Included:

- a Fleet-owned append-only `VehicleReading` ledger;
- odometer and engine-hour facts in canonical units;
- chronological validation, including valid backdated insertion;
- latest-effective-reading queries;
- manual Fleet entry;
- synchronous Trip Start and Trip End integration;
- synchronous Fuel Issue integration at `ISSUED`;
- append-only corrections;
- explicit meter replacement/rollover;
- audit identity and timestamps;
- source idempotency and concurrency control;
- permissions, REST contracts, events, frontend history, and tests.

The implementation must preserve the existing controller → input port → application service → domain → output port → persistence adapter → JPA repository flow. It must not share JPA entities or repositories across modules.

## 3. Terminology

- **Reading fact:** One observed value for one vehicle and one reading type.
- **Recorded time:** When the physical meter was observed.
- **Received time:** When the application accepted the fact.
- **Effective reading:** A reading that has not been superseded by a correction.
- **Meter epoch:** The monotonic sequence belonging to one physical meter or one uninterrupted meter range.
- **Source aggregate:** The Trip, Fuel Issue, manual command, baseline migration, or meter reset that produced the reading.
- **Latest reading:** The chronologically greatest effective fact in the current meter epoch, not the most recently inserted row.
- **Correction chain:** The immutable original followed by one or more immutable rows that directly supersede one another.

## 4. Authoritative Data Owner

The Fleet module is authoritative because the reading describes a vehicle, can originate outside a trip or fuel issue, and participates in vehicle availability and future maintenance decisions.

Current repository observations supporting this decision:

- `Vehicle` and `VehicleEntity` hold mutable `Double` odometer/engine-hour snapshots.
- `Trip` holds start/end odometer `Double` values but no engine hours.
- `FuelIssue` holds optional `BigDecimal` odometer and engine hours and validates them against Fleet snapshots.
- no append-only chronological ledger or correction model exists.
- public module interfaces such as `VehicleFuelContextLookup` already provide narrow cross-module access.

Trip and Fuel keep their local reading fields as operational source evidence and response compatibility fields. They are not independent truths. Reporting and future calculations must query Fleet's effective ledger.

The public Fleet interface should be named `VehicleReadingRecorder` (or an equivalent root-package module contract). Trip and Fuel each define their own output port and adapter to it, preserving dependency inversion.

## 5. Reading Types

| Type | Canonical unit | Java value | Database value | Active in US-33 |
|---|---|---|---|---|
| `ODOMETER` | `KILOMETER` | `BigDecimal` | `NUMERIC(19,3)` | Yes |
| `ENGINE_HOURS` | `HOUR` | `BigDecimal` | `NUMERIC(19,3)` | Yes |

Values must be non-negative and normalized to scale 3 using an explicit rounding policy (`HALF_UP`) only at the input boundary. Domain and persistence code must never use `double` for new reading facts. Existing `Double` source fields are converted with `BigDecimal.valueOf`, never `new BigDecimal(double)`.

Miles are not accepted in US-33. A future adapter may convert external miles to canonical kilometers before invoking the domain; stored authoritative values remain canonical.

One row stores one type. If an operational action supplies both values, the application records two facts atomically with the same source reference and recorded time.

## 6. Source Types

| Source | Status | Reference | Recorded time |
|---|---|---|---|
| `MANUAL` | Active | None | Request value |
| `TRIP_START` | Active | Trip UUID | Trip actual start |
| `TRIP_END` | Active | Trip UUID | Trip actual completion |
| `FUEL_ISSUE` | Active | Fuel Issue UUID | Fuel issue date/time |
| `BASELINE` | Migration only | Vehicle UUID or deterministic migration reference | Evidence timestamp or controlled migration timestamp |
| `METER_RESET` | Active through reset command | Meter Reset UUID | Reset effective time |
| `TELEMATICS` | Reserved | Future device/event ID | Not implemented |
| `MAINTENANCE` | Reserved | Future maintenance event ID | Not implemented |

Reserved values may exist in the enum/check constraint so a later forward migration is not required merely to recognize them, but US-33 exposes no ingestion endpoint for them. Corrections retain the original source; `CORRECTION` is not a source type.

## 7. Validation Rules

All commands validate:

1. vehicle exists and is active enough to receive history; inactive vehicles remain readable and may be corrected/reset by authorized operators, but cannot receive ordinary new operational/manual readings;
2. reading type is supported;
3. value is present, non-negative, scale-compatible, and within `NUMERIC(19,3)`;
4. unit is derived from type and cannot be client-selected;
5. `recordedAt` is present and includes an offset;
6. `recordedAt` is no more than five minutes after the server clock for manual commands;
7. source reference is present for Trip, Fuel, Baseline, and Meter Reset sources and absent for manual source;
8. actor resolves to an authenticated application user;
9. notes are at most 1,000 characters;
10. source idempotency and chronological neighbor rules pass.

System-generated commands use the source aggregate's accepted operational timestamp and do not apply the five-minute manual tolerance. They still reject impossible chronology.

Stable errors:

| Code | HTTP | Meaning |
|---|---:|---|
| `VEHICLE_READING_NOT_FOUND` | 404 | Reading or vehicle-scoped target does not exist |
| `INVALID_VEHICLE_READING` | 400 | Missing/negative/invalid precision, type, timestamp, or source data |
| `VEHICLE_READING_DECREASE` | 409 | New latest reading is below its predecessor |
| `VEHICLE_READING_CHRONOLOGY_CONFLICT` | 409 | Backdated/same-time/corrected value violates a neighbor |
| `DUPLICATE_VEHICLE_READING` | 409 | Manual fingerprint or idempotency key already exists |
| `VEHICLE_READING_NOT_CORRECTABLE` | 409 | Target is superseded or outside the correctable epoch |
| `CORRECTION_REASON_REQUIRED` | 400 | Correction reason is blank |
| `METER_RESET_REQUIRED` | 409 | A lower value needs an explicit approved reset |
| `VEHICLE_READING_CONCURRENT_CHANGE` | 409 | Lock/concurrent persistence failure; caller may retry |

Errors use the existing shared `ApiError` and correlation ID. Field-shape failures populate `fieldErrors`; business chronology failures use stable top-level codes.

## 8. Chronological Insertion Rules

Chronology partitions by `(vehicleId, readingType, meterEpoch)`. It considers effective readings only.

For insertion at `T`:

```text
P = greatest effective reading where recordedAt < T
N = least effective reading where recordedAt > T

if P exists: P.value <= candidate.value
if N exists: candidate.value <= N.value
```

Examples:

| Existing facts | Candidate | Result |
|---|---|---|
| Aug 10: 10,000; Aug 12: 10,200 | Aug 11: 10,100 | Accept |
| Aug 10: 10,000; Aug 12: 10,200 | Aug 11: 10,500 | Reject: above next |
| Aug 10: 10,000; Aug 12: 10,200 | Aug 11: 9,900 | Reject: below previous |
| Latest: 10,200 | Later: 10,200 | Accept: stationary reading |
| Latest: 10,200 | Later: 9,999 | Reject with `METER_RESET_REQUIRED` |

Late arrival changes insertion order, not business order. `receivedAt` and `createdAt` remain later than `recordedAt` and provide audit evidence.

### Same timestamp

For an exact timestamp already holding an effective value in the same partition:

- equal value is allowed for a different, idempotent operational source and can serve as corroborating evidence;
- different value is rejected;
- replay of the same source returns the existing result without a second insert;
- manual replay with the same idempotency key returns the original result; the same manual fact under a different key is rejected as duplicate.

Value selection therefore never depends on source priority or UUID ordering. List display ties use `receivedAt` then `createdAt`. Since all effective values at the same time must be equal, latest-value semantics remain deterministic.

## 9. Correction Rules

No update or delete endpoint exists for a reading. Correction is a command.

`CorrectVehicleReadingCommand` contains:

- vehicle ID and effective target reading ID;
- corrected `BigDecimal` value;
- mandatory reason;
- actor UUID;
- optional notes/idempotency key.

The service locks the vehicle, verifies the target is the effective leaf, and inserts a correction row with:

- the target's type, unit, epoch, source, source reference, and `recordedAt`;
- current `receivedAt`/`createdAt` and correcting actor;
- `correctionOfReadingId=target.id`;
- required correction reason.

It validates the corrected value against the effective previous/next chronology while excluding the target. A correction may itself be corrected, producing a chain. Only the leaf is effective. A row may have at most one direct successor.

History APIs return every row. `supersededBy` is derived from the correction relationship. Response semantics:

- `corrected=true` when this row is a correction of another row;
- `supersededBy` non-null when this row is no longer effective;
- `effective=true` may be included internally/UI-side as `supersededBy == null`.

Latest queries, validation, reports, and future calculations ignore superseded rows. Corrections cannot cross a meter epoch or change type, unit, source, source reference, or recorded time.

## 10. Meter Replacement

Meter replacement and rollover are one explicit command and one separate immutable aggregate, not a special negative exception in ordinary validation.

`VehicleMeterReset` fields:

- `resetId`, `vehicleId`, `readingType`;
- previous and new meter epochs;
- previous reading ID/value;
- new reading ID/value;
- `effectiveAt`, reason, approvedBy, createdAt.

Rules:

- permission `VEHICLE_READING_RESET_METER` is mandatory;
- reason is mandatory;
- previous reading must be the latest effective reading for the current epoch;
- new value must be non-negative but may be below the old value;
- the operation increments the epoch and inserts the reset plus the first `METER_RESET` reading atomically;
- reset `effectiveAt` cannot precede an already accepted later effective reading in US-33;
- ordinary create/correct commands cannot alter an epoch.

US-33 exposes current physical readings by current epoch. It preserves prior epochs but does not calculate lifetime accumulated kilometers/hours across replacements.

## 11. Idempotency

System keys are deterministic:

```text
TRIP_START:{tripId}:{readingType}
TRIP_END:{tripId}:{readingType}
FUEL_ISSUE:{fuelIssueId}:{readingType}
BASELINE:{vehicleId}:{readingType}:{migrationVersion}
METER_RESET:{resetId}:{readingType}
```

The database also enforces one original row for:

```text
(vehicle_id, reading_type, source_type, source_reference_id)
WHERE source_reference_id IS NOT NULL
  AND correction_of_reading_id IS NULL
```

Replaying an identical system command returns the existing reading. Replaying the same source key with a different vehicle, type, value, or recorded time returns `DUPLICATE_VEHICLE_READING`; it does not silently overwrite evidence.

Manual POST requires an `Idempotency-Key` UUID header. The key is unique and a matching replay returns the original response. A second key for the same `(vehicle, type, epoch, recordedAt, value)` is rejected as a semantic duplicate. Notes are not part of the fingerprint.

Corrections and resets also require idempotency keys at REST boundaries so network retries cannot append multiple audit commands.

## 12. Trip Integration

Proposed boundary:

```java
public interface VehicleReadingRecorder {
    RecordResult record(RecordCommand command);
}
```

The command carries `vehicleId`, type, value, Fleet source type, source reference UUID, recorded time, actor UUID, and deterministic idempotency key. The exact Java records belong to Fleet's public root package; Trip maps its concepts in an infrastructure adapter.

Flow:

```text
TripService
  -> Trip-local VehicleReadingPort
  -> Trip infrastructure adapter
  -> Fleet VehicleReadingRecorder
  -> Fleet application service/policy/repository
```

Trip Start records:

- required `ODOMETER`, source `TRIP_START`, reference Trip ID;
- optional `ENGINE_HOURS` once the request exposes it;
- `recordedAt=actualStartTime`.

Trip Complete records:

- required `ODOMETER`, source `TRIP_END`, reference Trip ID;
- optional `ENGINE_HOURS` once exposed;
- `recordedAt=actualEndTime`.

The existing Trip request uses `Double`; the Trip integration slice must introduce `BigDecimal` request/domain values for new reading input while preserving response compatibility deliberately. Conversion must occur at the boundary during rollout.

The lifecycle mutation, Trip history, and Fleet readings must share the existing synchronous transaction. Record readings before returning the transitioned aggregate. Any Fleet error rolls back Trip state and history. Do not use an eventual event listener for the authoritative write.

## 13. Fuel Integration

Fuel maps through a Fuel-local output port and Fleet adapter. It already resolves an authenticated user UUID through its actor port and can pass that UUID to Fleet.

Only `FuelIssueService.issue` records readings, after lifecycle/operational validation but inside the same transaction as the status and history writes:

```text
AUTHORIZED -> validate -> Fleet record supplied readings
           -> persist ISSUED + history -> publish existing FuelIssued
```

Each non-null value produces one fact:

- type `ODOMETER` or `ENGINE_HOURS`;
- source `FUEL_ISSUE`;
- reference Fuel Issue ID;
- `recordedAt=FuelIssue.issueDateTime`;
- actor is the user performing `issue`.

Draft, submitted, and authorized Fuel Issues do not create facts because their readings remain editable and fuel has not yet physically been issued. Cancellation before issue creates none. An `ISSUED` Fuel Issue is immutable under current rules, so its source reading remains stable. Fleet alone validates chronology; the current Fuel snapshot comparison must be retired from Fuel once the integration is authoritative, not duplicated alongside it.

## 14. Database Design

Design only; no migration is created by this contract task.

```sql
vehicle_reading (
  reading_id UUID PRIMARY KEY,
  vehicle_id UUID NOT NULL REFERENCES vehicle(id),
  reading_type VARCHAR(30) NOT NULL,
  value NUMERIC(19,3) NOT NULL,
  unit VARCHAR(30) NOT NULL,
  meter_epoch INTEGER NOT NULL DEFAULT 0,
  source_type VARCHAR(30) NOT NULL,
  source_reference_id UUID NULL,
  recorded_at TIMESTAMPTZ NOT NULL,
  received_at TIMESTAMPTZ NOT NULL,
  created_by UUID NOT NULL REFERENCES app_user(id),
  correction_of_reading_id UUID NULL REFERENCES vehicle_reading(reading_id),
  correction_reason TEXT NULL,
  idempotency_key VARCHAR(160) NULL,
  notes VARCHAR(1000) NULL,
  created_at TIMESTAMPTZ NOT NULL
)
```

Checks:

- `value >= 0` and `meter_epoch >= 0`;
- only allowed reading/source types;
- `ODOMETER` pairs only with `KILOMETER`;
- `ENGINE_HOURS` pairs only with `HOUR`;
- correction reference and non-blank reason appear together;
- original rows do not carry correction reasons.

Indexes/uniqueness:

```sql
CREATE INDEX idx_vehicle_reading_chronology
  ON vehicle_reading(vehicle_id, reading_type, meter_epoch, recorded_at DESC);

CREATE INDEX idx_vehicle_reading_source
  ON vehicle_reading(source_type, source_reference_id);

CREATE INDEX idx_vehicle_reading_correction
  ON vehicle_reading(correction_of_reading_id);

CREATE UNIQUE INDEX uq_vehicle_reading_source
  ON vehicle_reading(vehicle_id, reading_type, source_type, source_reference_id)
  WHERE source_reference_id IS NOT NULL AND correction_of_reading_id IS NULL;

CREATE UNIQUE INDEX uq_vehicle_reading_idempotency
  ON vehicle_reading(idempotency_key)
  WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX uq_vehicle_reading_one_correction
  ON vehicle_reading(correction_of_reading_id)
  WHERE correction_of_reading_id IS NOT NULL;
```

`vehicle_meter_reset` contains the fields in section 10 and foreign keys to the previous/new readings and approving user. It has uniqueness on `new_reading_id` and `(vehicle_id, reading_type, new_meter_epoch)`.

The effective view/query is defined by absence of a correction child. It is an application repository query initially; no materialized view is needed. PostgreSQL cannot enforce cross-row monotonicity with a normal check constraint, so vehicle locking plus neighbor validation is authoritative.

## 15. APIs

Paths follow current controllers. If the application later applies a global `/api/v1` prefix, it must do so consistently rather than special-case US-33.

### List history

```http
GET /vehicles/{vehicleId}/readings?readingType=ODOMETER&sourceType=TRIP_END&from=...&to=...&page=0&limit=20
```

Returns a server-paged object with `content`, `page`, `limit`, `totalElements`, and `totalPages`. Default sort is `recordedAt DESC, receivedAt DESC`; maximum limit is 100.

### Latest

```http
GET /vehicles/{vehicleId}/readings/latest
```

Returns nullable odometer and engine-hours sections independently.

### Manual entry

```http
POST /vehicles/{vehicleId}/readings
Idempotency-Key: <UUID>

{
  "readingType": "ODOMETER",
  "value": 12345.670,
  "recordedAt": "2026-08-16T08:30:00+05:30",
  "reason": "Depot inspection",
  "notes": "Optional context"
}
```

`reason` is required for manual provenance and is stored in notes/audit context; the unit is server-derived. Success is `201 Created`; an idempotent replay returns `200 OK` with the original reading.

### Correct

```http
POST /vehicles/{vehicleId}/readings/{readingId}/correct
Idempotency-Key: <UUID>

{
  "value": 12354.670,
  "reason": "Transposed digits confirmed from dashboard photo",
  "notes": "Optional"
}
```

Success returns `201 Created` with the correction row and linkage.

### Meter reset

```http
POST /vehicles/{vehicleId}/meter-resets
Idempotency-Key: <UUID>

{
  "readingType": "ODOMETER",
  "newValue": 0.000,
  "effectiveAt": "2026-08-16T09:00:00+05:30",
  "reason": "Instrument cluster replaced"
}
```

Success returns `201 Created` with reset and initial-reading identifiers.

### Response models

`VehicleReadingResponse`:

- `id`, `vehicleId`, `readingType`, `value`, `unit`, `meterEpoch`;
- `sourceType`, `sourceReferenceId`;
- `recordedAt`, `receivedAt`, `createdAt`, `createdBy`;
- `corrected`, `effective`, `supersededBy`, `correctionOfReadingId`;
- `correctionReason`, `notes`.

`LatestVehicleReadingResponse`:

```text
vehicleId
odometer: { readingId, value, unit, recordedAt, receivedAt, sourceType, sourceReferenceId, meterEpoch }
engineHours: { readingId, value, unit, recordedAt, receivedAt, sourceType, sourceReferenceId, meterEpoch }
```

Either latest section may be null. Responses never expose internal idempotency keys.

## 16. Permissions

| Permission | Endpoints/operations |
|---|---|
| `VEHICLE_READING_VIEW` | list and latest |
| `VEHICLE_READING_CREATE` | manual entry |
| `VEHICLE_READING_CORRECT` | correction |
| `VEHICLE_READING_RESET_METER` | replacement/rollover |

System-generated Trip and Fuel writes are authorized by their existing lifecycle permissions (`TRIP_START`, `TRIP_COMPLETE`, `FUEL_ISSUE_ISSUE`) and invoke an internal application boundary, not the manual REST endpoint. The Fleet service still records the actual actor. Permissions are seeded by a new Flyway migration and assigned to the bootstrap administration role following existing conventions. No hard-coded role names are permitted in endpoint rules.

## 17. Events

Fleet publishes immutable Spring application/Modulith events:

- `VehicleReadingRecorded(readingId, vehicleId, readingType, value, unit, sourceType, sourceReferenceId, recordedAt, receivedAt)`;
- `VehicleReadingCorrected(readingId, correctedReadingId, vehicleId, readingType, correctedValue, recordedAt, correctedAt)`;
- `VehicleMeterReset(resetId, vehicleId, readingType, previousValue, newValue, previousEpoch, newEpoch, effectiveAt)`.

No Kafka or external broker is introduced. Events support future reporting, maintenance, and analytics, but they are not used to complete the authoritative Trip/Fuel write. Publication occurs after the Fleet record is persisted in the caller's transaction; future consumers should use transaction-bound Modulith listeners where appropriate.

## 18. Frontend Requirements

Add a **Readings** tab to Vehicle Details using Ant Design.

History table columns:

- Date/Time (`recordedAt`), received time tooltip;
- Reading Type;
- Value and canonical unit;
- Source and linked source reference;
- Entered By;
- Meter Epoch (shown prominently only after a reset exists);
- Correction Status;
- Actions.

Use `Table` with server pagination/filtering, `Tag` for source/correction state, `Descriptions` for details, and permission-aware actions. `Modal` handles manual entry and correction. A guarded `Modal` or `Drawer` handles meter reset with a prominent warning. Use React Hook Form + Zod with Ant Design controls. Backend chronology is authoritative; React must display `ApiError` and field errors rather than calculate eligibility.

Visibility:

- tab/query requires `VEHICLE_READING_VIEW`;
- Add Manual Reading requires `VEHICLE_READING_CREATE`;
- Correct requires `VEHICLE_READING_CORRECT` and an effective row;
- Reset Meter requires `VEHICLE_READING_RESET_METER`.

Do not add fuel-efficiency charts, cost metrics, maintenance counters, GPS distance, or frontend-only current-reading calculations.

## 19. Test Matrix

### Domain

- increasing odometer and engine hours accepted;
- equal/stationary readings accepted;
- decreasing latest value rejected with reset guidance;
- backdated in-range accepted;
- backdated below previous or above next rejected;
- future manual timestamp tolerance enforced;
- same-time equal accepted and different rejected;
- correction leaves original immutable and effective leaf changes;
- invalid correction chronology rejected;
- correction chain and already-superseded target behavior;
- meter reset creates new epoch and permits lower new value;
- ordinary reading cannot simulate reset.

### Application

- manual entry and latest query;
- inactive/missing vehicle handling;
- Trip Start and End create correct source facts;
- Trip lifecycle rolls back when reading fails;
- Fuel Issue creates supplied facts only at `ISSUED`;
- Fuel issue/status/history roll back when reading fails;
- two readings from one source action are atomic;
- duplicate system source returns existing result;
- replay mismatch fails;
- manual idempotency and semantic duplicate behavior;
- list filters, ordering, correction visibility, and current epoch.

### Persistence/PostgreSQL

- empty database migration plus JPA validation;
- precision/check constraints and FK integrity;
- partial unique source and idempotency indexes;
- one-correction-per-target uniqueness;
- latest/effective neighbor queries;
- concurrent 10,100/10,200 inserts serialize and preserve chronology;
- concurrent correction/create and reset/create races;
- rollback leaves no reading, reset, projection, or source lifecycle partial state.

### Security/API

- unauthenticated → 401;
- authenticated without each permission → 403;
- permitted actor → success;
- authorization failure precedes mutation;
- stable `ApiError`, correlation ID, and field-error mapping;
- server pagination and maximum limit.

### Existing regression

- Trip start/complete and lifecycle concurrency tests;
- Fuel Issue lifecycle and PostgreSQL tests;
- vehicle availability and Fleet CRUD;
- Spring Modulith verification;
- full Java 21 `mvn clean verify`.

### Frontend

- history list/loading/empty/error/pagination/filter states with MSW;
- latest values;
- manual entry, correction, and reset forms;
- backend field/business errors;
- permission-driven visibility;
- idempotent retry behavior.

## 20. Migration Strategy

Do not fabricate chronology. Implement a forward Flyway migration after repository data profiling.

1. Create reading/reset tables, checks, indexes, and permissions.
2. Backfill Trip facts where `actual_start_time/start_odometer_km` or `actual_end_time/end_odometer_km` is non-null. Use Trip IDs and deterministic source keys.
3. Backfill only `ISSUED` Fuel Issues with non-null readings at `issue_date_time`.
4. Convert legacy `Double` values through decimal string/value-safe conversion and round explicitly to scale 3.
5. Validate all backfilled facts in chronological order per vehicle/type. Conflicts go to a migration audit report/table for explicit resolution; migration must not silently clamp, reorder, or delete facts.
6. Where no trustworthy timestamped fact establishes the current snapshot, create a `BASELINE` fact from the Vehicle value with migration provenance. If a snapshot contradicts a later trusted fact, report it instead of inserting it.
7. Initialize `meterEpoch=0`; legacy data does not imply a reset without evidence.
8. Keep Vehicle, Trip, and Fuel reading columns. Begin maintaining Vehicle snapshot columns as projections from latest effective readings.
9. Reconcile projections and ledger in tests/operations before any later schema deprecation.

The migration must be idempotent through deterministic keys and tested on PostgreSQL from V1 through the new latest version. H2 tests remain as fast coverage; production-sensitive migration/concurrency tests run in the release gate.

## 21. Deferred Capabilities

- fuel efficiency and consumption analytics;
- cost per trip or cost per kilometer;
- telematics ingestion and device identity;
- GPS distance reconciliation;
- maintenance schedules/counters;
- idle-time tracking;
- anomaly/fraud detection;
- lifetime accumulated distance across meter epochs;
- bulk import/export and approval workflows beyond meter reset;
- notifications.

## 22. Open Questions

The following are non-blocking implementation/product confirmations; defaults are defined so engineering can proceed:

1. **Trip engine hours:** current Trip commands expose only odometer. Default: add optional engine hours during US33-C; do not make it mandatory.
2. **Legacy conflict handling destination:** default: produce a migration reconciliation report and skip conflicting rows. Operations must identify who resolves that report before production rollout.
3. **Vehicle snapshot projection timing:** default: update the snapshot synchronously in the same Fleet transaction. Confirm whether any external integration writes snapshots directly; such writes must stop before cutover.
4. **Manual future tolerance:** default: five minutes. Product may reduce this to strict server time without changing the data model.
5. **Reset approval:** default: possession of `VEHICLE_READING_RESET_METER` is sufficient for Phase 2. Four-eyes approval/segregation of duties is deferred.

None changes ownership, append-only history, canonical units, or transaction requirements.

## 23. Implementation Slices

Dependency order:

### US33-A — VehicleReading domain and persistence

- domain types, chronology policy, ports, service, entity/repository/adapter;
- Flyway tables, indexes, baseline/backfill tooling, latest query;
- projection compatibility and domain/persistence/PostgreSQL tests;
- no REST integration yet.

### US33-B — Manual API and permissions

- paged list/latest, manual command, controller DTOs and `ApiError` mapping;
- permission migration/bootstrap/security rules;
- controller/security tests and OpenAPI documentation.

### US33-C — Trip integration

- Trip output port and Fleet adapter;
- `BigDecimal` start/end reading input and optional engine hours;
- same-transaction recording and rollback/regression tests.

### US33-D — Fuel Issue integration

- Fuel output port and Fleet adapter;
- record at `ISSUED`, retire duplicate snapshot chronology validation;
- atomic two-reading/idempotency/rollback tests.

### US33-E — Correction and meter reset

- correction chain, reset aggregate/new epoch, APIs/events/permissions;
- audit, concurrency, security, and PostgreSQL tests.

### US33-F — React Fleet reading history UI

- Vehicle Details Readings tab;
- list/latest/manual/correction/reset permission-aware UX;
- React Testing Library and MSW tests.

### US33-G — Release verification

- PostgreSQL race matrix and cross-module end-to-end scenarios;
- backfill/reconciliation verification;
- Spring Modulith, OpenAPI, documentation, and full Java 21 release gate.

Each slice must remain independently green. Do not start analytics or other deferred features as part of these slices.
