# MVP-GAP-008E Implementation

## Scope

MVP-GAP-008E implements production producers for exactly the eight `MVP_REQUIRED` events frozen by the US-77 catalogue. It adds no notification API, frontend behavior, production email transport, Phase 2 fuel/drug-expiry event, or database migration.

## Event producer architecture

Trip publishes accepted delay and incident facts through the existing public `OperationalNotificationEvent` contract. Fleet application services publish mutation-based driver facts through `FleetOperationalNotificationPublisher`; its Spring adapter alone depends on the public Notification contract. Fleet-owned scanners query domain-safe repository ports and publish time-based maintenance and compliance facts. Neither Trip nor Fleet imports Notification domain, application, persistence, or infrastructure packages, and Notification does not query Fleet internals.

## Event mappings

| Event | Trigger | Aggregate | Severity | Required metadata |
|---|---|---|---|---|
| `TRIP_DELAY_RECORDED` | accepted delay operational event | Trip / trip ID | WARNING | `tripId`, `tripNumber`, `delayMinutes`, `reason`, optional `locationDescription` |
| `TRIP_INCIDENT_RECORDED` | accepted incident operational event | Trip / trip ID | LOW=INFO; MEDIUM/HIGH=WARNING; CRITICAL=CRITICAL | `tripId`, `tripNumber`, `incidentSeverity`, `description`, optional `locationDescription` |
| `VEHICLE_MAINTENANCE_DUE` | SCHEDULED maintenance inside the 24-hour lead window | Vehicle / vehicle ID | WARNING | `vehicleId`, `vehicleRegistration`, `maintenanceType`, `scheduledStart`, `scheduledEnd`, `milestone` |
| `VEHICLE_DOCUMENT_EXPIRING` | active mandatory document reaches D30 or EXPIRED | Vehicle / vehicle ID | D30=WARNING; EXPIRED=CRITICAL | `vehicleId`, `vehicleRegistration`, `documentId`, `documentType`, `documentNumber`, `expiryDate`, `milestone` |
| `DRIVER_EXCEPTION_RECORDED` | blocking exception created or activated | Driver / driver ID | WARNING; disciplinary suspension/medical emergency=CRITICAL | `driverId`, `driverName`, `exceptionId`, `exceptionType`, `startTime`, `endTime`, optional `reason` |
| `DRIVER_MEDICAL_EXPIRING` | active FIT medical record reaches D30 or EXPIRED | Driver / driver ID | D30=WARNING; EXPIRED=CRITICAL | `driverId`, `driverName`, `medicalRecordId`, `validUntil`, `fitnessStatus`, `milestone` |
| `DRIVER_DRUG_TEST_FAILED` | active positive result becomes blocking | Driver / driver ID | CRITICAL | `driverId`, `driverName`, `drugTestId`, `resultDate`, `testType` |
| `DRIVER_LICENSE_EXPIRING` | active license reaches D30 or EXPIRED | Driver / driver ID | D30=WARNING; EXPIRED=CRITICAL | `driverId`, `driverName`, `licenseId`, `licenseNumber`, `licenseClass`, `expiryDate`, `milestone` |

`catalogueMilestone` is also emitted for the downstream suppression key while `milestone` remains the frozen template variable.

## Transactional producers

- `TripOperationalEventService` publishes delay/incident facts only after the operational event is saved.
- `DriverExceptionService` publishes only blocking create/activation transitions.
- `DriverDrugTestService` publishes only a blocking active positive result.

Publication failures are caught after the source operation succeeds, logged without business payload secrets, and do not roll back the owning operation.

## Scheduled producers

- `MaintenanceDueNotificationScanner` queries scheduled maintenance entering the restart-safe 24-hour lead window. `FleetNotificationScheduler` invokes it with a one-hour default fixed delay.
- `ComplianceNotificationScanner` evaluates active mandatory vehicle documents, active FIT medical records, and active licenses through the business date plus 30 days. It emits D30 or EXPIRED milestones and uses the validated `app.notification.time-zone`. The scheduler default is 24 hours.

Each candidate is isolated so one publication failure cannot abort later candidates or compliance families. Repeated scans intentionally republish the same stable event identity; downstream idempotency/suppression prevents duplicate execution.

## Stable event-ID and milestone strategy

`FleetOperationalNotificationEvents` derives name-based UUIDs from UTF-8 input containing event type, immutable source record ID, and milestone material. Maintenance additionally includes scheduled start; expiry events include the applicable expiry date; mutation events include the blocking transition. Thus the same business snapshot produces the same UUID, while a changed schedule, expiry, or milestone produces a distinct UUID. Trip events reuse the persisted operational-event UUID directly.

Stable milestone identifiers are `DUE_24H`, `D30`, `EXPIRED`, the driver-exception blocking transition, and `BLOCKING_POSITIVE`. No in-memory sent flags or source-table notification state is introduced.

## Repository and scheduler design

Focused query capabilities were added to the existing output ports and persistence adapters for scheduled maintenance, mandatory vehicle documents, active FIT medical records, and active licenses. Application scanners receive domain objects, never JPA entities. Existing V3/V4/V19/V22 indexes support these predicates, so no V29 is needed.

## Tests

Producer tests cover all eight event types, all incident severity mappings, catalogue-required metadata, exclusions, stable repeated identities, changed milestones, scheduler interval annotations, business timezone behavior, mutation/scanner failure isolation, and the explicit exactly-eight producer catalogue. Existing assignment and dispatch coverage remains part of the full suite.

Architecture verification covers `ApplicationModulesTest`, `HexagonalLayerArchitectureTest`, `ModuleBoundaryArchitectureTest`, and `LombokUsageArchitectureTest` (15 tests total), including the public Trip/Fleet-to-Notification boundary.

## Verification

- Backend `clean test`: PASS — 625 run, 604 passed, 0 failures, 0 errors, 21 skipped.
- Backend `verify`: PASS — 625 run, 604 passed, 0 failures, 0 errors, 21 skipped; executable JAR packaged.
- Architecture: PASS — 15/15.
- Spring context: PASS.
- Flyway: PASS — V1-V28 validated and applied.
- Frontend lint: PASS.
- Frontend unit: PASS — 94/94 on a bounded full rerun. One unchanged Fuel Issue test timed out during the earlier backend-contended run and passed both in isolation and in the full rerun.
- Frontend build: PASS.
- Playwright: PASS — 111/111 across Chromium, Firefox, and WebKit; no new reproducible regression.

## Deferred work

- 008F: production EMAIL delivery.
- 008G: frontend rule/delivery completion.
- 008H: notification-specific Playwright coverage.
- 008I: final regression and US-77 closure.

US-77 remains PARTIAL. No 008F-or-later behavior is implemented by this slice.
