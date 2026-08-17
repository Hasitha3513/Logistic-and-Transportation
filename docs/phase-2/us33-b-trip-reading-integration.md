# US33-B — Trip Reading Integration

## Overview

US33-B integrates Trip execution (`TRIP_START` and `TRIP_COMPLETE` transitions) with Fleet-owned `VehicleReading` facts.

Authority: [ADR-vehicle-reading-authority.md](../adr/ADR-vehicle-reading-authority.md)  
Master Contract: [us-33-mileage-contract.md](us-33-mileage-contract.md)

## Key Principles & Architectural Boundaries

1. **Fleet Ownership**:
   - Fleet owns the authoritative append-only `vehicle_reading` ledger, chronological validation, latest reading resolution, and vehicle snapshot projections.
   - Trip keeps local `startOdometerKm`, `endOdometerKm`, `actualStartTime`, `actualEndTime` strictly as operational source evidence and API response compatibility fields.

2. **Inverted Dependency Ports**:
   - Trip defines its output port `TripVehicleReadingPort` in `com.transportlogistics.app.trip.application.ports.out`.
   - Trip's infrastructure adapter `FleetVehicleReadingAdapter` in `com.transportlogistics.app.trip.infrastructure.adapters.out.fleet` translates Trip calls into `VehicleReadingRecorder.Command` and invokes `VehicleReadingRecorder`.
   - No Fleet JPA entities or repositories are imported into the Trip module.

3. **Actor Resolution**:
   - Trip defines `TripActorPort` in `com.transportlogistics.app.trip.application.ports.out`.
   - `IdentityTripActorAdapter` delegates to `AuthenticatedUserLookup` in the Identity module to resolve the principal's username to an authenticated `app_user.id` UUID.

4. **Synchronous Transactional Consistency**:
   - Trip transition (`TRIP_START`, `TRIP_COMPLETE`), Trip history write, and Fleet `VehicleReadingRecorder.record` participate in the same active transaction.
   - If a reading violates chronology, decreases below a previous value without a reset, or fails validation, `VehicleReadingService` throws `ConflictException` / `ValidationException`, causing the entire Trip transition, history entry, and reading to roll back atomically.

5. **Authoritative Vehicle & Timestamps**:
   - Vehicle ID is taken strictly from the authoritative Trip vehicle assignment, never from client request bodies.
   - `recordedAt` timestamps correspond directly to physical business times (`actualStartTime` for start, `actualEndTime` for completion).

6. **Optional Engine Hours Support**:
   - Start and Complete commands accept optional `engineHours` (`startEngineHours` / `endEngineHours`).
   - If provided, an `ENGINE_HOURS` fact is recorded atomically alongside `ODOMETER`. If omitted, only the `ODOMETER` fact is recorded.

7. **Frontend Invalidation & Error Handling**:
   - Upon successful Start/Complete mutations, the frontend invalidates Trip queries as well as vehicle reading queries (`['vehicles']`, `['vehicle', vehicleId, 'readings']`, `['vehicle', vehicleId, 'readings', 'latest']`).
   - Backend chronology conflicts and business rule rejections are displayed cleanly in the UI modal descriptions.

## Artifacts & Components Created / Modified

- `TripVehicleReadingPort.java`: Output port for recording start and complete readings.
- `TripActorPort.java`: Output port for resolving actor username to user UUID.
- `FleetVehicleReadingAdapter.java`: Adapter invoking Fleet's `VehicleReadingRecorder`.
- `IdentityTripActorAdapter.java`: Adapter invoking Identity's `AuthenticatedUserLookup`.
- `TripCommand.java`: Added optional `engineHours` to `Start` and `Complete` records.
- `TripLifecyclePolicy.java`: Validates optional engine hours format and non-negativity.
- `TripService.java`: Calls `TripVehicleReadingPort` within `transition(...)` transaction.
- `TripConfig.java`: Spring bean wiring for `TripVehicleReadingPort` and `TripActorPort`.
- `TripController.java`: Updated `StartRequest` and `CompleteRequest` to support optional engine hours.
- `LifecycleActions.tsx`: Added vehicle reading queries cache invalidation.
- `LifecycleActions.test.tsx`: Added tests for cache invalidation and backend error rendering.
- `TripServiceLifecycleTest.java`: Added unit tests for start/complete reading recording, rollback on conflict, and engine hours.
- `TripLifecycleIntegrationTest.java`: Verified end-to-end reading creation and snapshot update.
- `PostgreSqlProductionInvariantIntegrationTest.java`: Added PostgreSQL tests for rollback atomicity and multi-trip chronology validation.
