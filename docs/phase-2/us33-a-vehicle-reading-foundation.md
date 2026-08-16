# US33-A — Vehicle Reading Foundation

## Status

Implemented. This slice is the Fleet-owned domain and persistence foundation approved in the US-33 contract. It deliberately contains no REST controller, permission mapping, Trip/Fuel integration, correction/reset workflow, or frontend UI.

## Implemented Domain

Fleet now owns one immutable `VehicleReading` fact per type. The domain includes:

- `VehicleReading`, `VehicleReadingType`, `VehicleReadingUnit`, and `VehicleReadingSourceType`;
- `ODOMETER` in canonical `KILOMETER` and `ENGINE_HOURS` in canonical `HOUR`;
- `BigDecimal` values normalized at the application boundary to scale 3;
- source/reference, meter epoch, recorded/received/created timestamps, actor, correction representation, idempotency key, and notes;
- intrinsic non-negative, precision, canonical-unit, audit, and correction-representation validation;
- `VehicleReadingChronologyPolicy` for previous/next/same-time validation.

The application rejects inactive vehicles for ordinary readings. `BASELINE` is allowed for controlled historical reconciliation. Vehicle existence and operational eligibility remain distinct concepts.

## Schema

Flyway `V14__vehicle_reading_foundation.sql` creates `vehicle_reading` with:

- UUID primary key and vehicle/application-user/self foreign keys;
- `NUMERIC(19,3)` value, `TIMESTAMPTZ` chronology/audit columns, and meter epoch;
- checks for allowed types/sources, non-negative values/epochs, canonical type/unit pairing, source-reference consistency, and correction reason pairing;
- chronology, source, correction, idempotency, and source-identity indexes.

PostgreSQL receives the approved partial unique source-identity index:

```text
(vehicle_id, reading_type, source_type, source_reference_id)
WHERE source_reference_id IS NOT NULL
  AND correction_of_reading_id IS NULL
```

H2 does not support partial indexes. The H2 profile substitutes a non-unique lookup index while the application idempotency policy remains active. PostgreSQL is the release invariant for database-enforced source uniqueness.

## Invariants

For an ordinary candidate at `recordedAt=T`, within vehicle/type/epoch:

```text
previous.value <= candidate.value <= next.value
```

Validation queries only the nearest previous/next effective rows and exact timestamp rows. It never loads complete history. Backdated in-range readings are accepted. A later decrease returns `VEHICLE_READING_DECREASE`; a backdated or same-time conflict returns `VEHICLE_READING_CHRONOLOGY_CONFLICT`.

Equal facts from different legitimate system sources at the same timestamp are allowed. Different values at the same timestamp are rejected without UUID/source-priority ordering.

## Concurrency

Recording runs in one transaction and first obtains the existing pessimistic write lock on the parent vehicle. After the lock, idempotency and chronological neighbors are re-read. This serializes reading writes across application instances and also protects the compatibility snapshot update.

Concurrency failures map to `VEHICLE_READING_CONCURRENT_CHANGE`. PostgreSQL tests race two incompatible same-vehicle submissions and prove exactly one survives with valid chronology.

## Application and Module Boundaries

`VehicleReadingUseCase` provides the foundation record, get, paged history, and latest-per-type operations. It is not exposed over HTTP in US33-A.

`VehicleReadingRecorder` is a Fleet root-package public module boundary for future Trip/Fuel adapters. Its immutable command/result types expose no JPA entity. No Trip or Fuel wiring exists yet.

`VehicleReadingRepository` provides save, ID, previous, next, same-time, latest, current-epoch, source/idempotency, and server-page queries. `VehicleReadingTransaction` and `VehicleReadingEventPublisher` preserve application/infrastructure separation.

## Events

`VehicleReadingRecorded` contains immutable reading facts. The Spring adapter registers publication after transaction commit, so a rolled-back reading cannot emit a successful-record event. No external broker is introduced.

## Vehicle Snapshot Compatibility

Existing `Vehicle.currentOdometerKm` and `Vehicle.engineHours` fields remain. After each accepted reading, the latest effective ledger value is projected to the matching snapshot in the same transaction. A valid backdated insertion does not replace a later snapshot.

The ledger is authoritative. Snapshot fields remain compatibility projections and still use legacy `Double`, so consumers requiring exact precision must use `VehicleReading`.

## Data Migration and Backfill

V14 does not fabricate readings. Existing Trip values lack a reliably resolvable application-user UUID, and Vehicle snapshots lack physical observation timestamps. Automatically assigning migration time or an arbitrary actor would violate the approved provenance contract.

The schema is ready for deterministic `TRIP_START`, `TRIP_END`, `FUEL_ISSUE`, and `BASELINE` reconciliation. Controlled backfill remains part of the later release/reconciliation slice after legacy facts are profiled, conflicts are reported, and a migration actor/provenance policy is approved.

## Tests

The US33-A suite covers:

- domain unit/precision/correction representation;
- first, increasing, equal, decreasing, negative, backdated, same-time, vehicle state, idempotency, latest, pagination, and filtering behavior;
- H2 Flyway V1–V14, JPA validation, persistence, chronology queries, and pagination;
- PostgreSQL V1–V14, JPA validation, index presence, partial source uniqueness, real concurrent writes, and transaction rollback of both ledger and snapshot.

The PostgreSQL test exposed and drove removal of an H2-only query assumption: optional filters now use conditional JPA Specifications rather than untyped-null JPQL parameters.

## Remaining US-33 Slices

- **US33-B:** manual REST API, actor resolution, permissions, security/controller/OpenAPI tests;
- **US33-C:** transactional Trip Start/End integration;
- **US33-D:** transactional Fuel Issue integration at `ISSUED`;
- **US33-E:** append-only correction and meter-reset workflows;
- **US33-F:** Vehicle Details Readings UI and frontend tests;
- **US33-G:** controlled legacy backfill, expanded PostgreSQL/end-to-end release verification.

The exact next slice is US33-B. It should expose the existing use case safely rather than duplicate chronology or persistence logic.

## Verification Result

Verified on 2026-08-16 with Java 21.0.7:

- backend `clean verify`: 203 tests across 55 suites, 0 failures, 0 errors, 0 skipped;
- PostgreSQL 16.4/Testcontainers: 14 tests, including V1–V14, JPA validation, source uniqueness, vehicle-reading concurrency, and rollback;
- Spring Modulith: 2 verification tests passed;
- frontend regression only: ESLint passed, 49 Vitest tests passed, and the Vite production build passed.

No US33-A frontend feature was added; the frontend commands verify that the existing application remains green.
