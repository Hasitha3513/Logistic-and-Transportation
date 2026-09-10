# US-49 CS05A — V79 Notification Catalogue Seed

**Status:** COMPLETE  
**Flyway:** V79; V1–V78 unchanged; V80 absent  
**Story state:** US-49 `IMPLEMENTATION_IN_PROGRESS`  
**Accounting:** 72 / 87 COMPLETE; 15 / 87 remaining

## Blocker and resolution

CS05 could not implement the frozen `VehicleGeofenceTransitionedV1` path on a clean V78 database because Notification requires active compatible template and Tenant-scoped rule metadata. V79 removes only that catalogue blocker using existing Notification tables and lifecycle conventions.

V79 seeds one global active version-1 IN_APP template with code/event type `VEHICLE_GEOFENCE_TRANSITIONED_V1`. It also seeds one enabled Tenant-scoped `ROLE` rule per existing Tenant using the established `DISPATCHER` recipient convention, plus its existing policy row with no quiet hours, zero suppression, and no escalation. IDs are deterministic and existing uniqueness constraints remain authoritative.

The Java Notification event catalogue now exposes the same IN_APP-only definition so the existing renderer and rule engine can validate it. CS05A does not publish or consume the event and does not create a delivery attempt.

## Privacy and scope

The template uses only `vehicleId`, `geofenceId`, `geofenceType`, `transition`, and `sourceTimestamp`. `definitionVersion` remains required by the event definition but is not displayed. `locationId` is optional. Coordinates, polygon data, device/provider identity, IMEI, Driver/Customer PII, raw telemetry, and credentials are neither required nor rendered.

No table, permission, channel, recipient model, template API, event family, Tracking behavior, Operations integration, frontend, dependency, or public API was added. US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`.

## Verification

- Focused V79 catalogue/migration/render tests: 16/16 PASS.
- Notification module regression: 164/164 PASS.
- Notification/Tracking security and privacy regression: 52/52 PASS.
- Affected Flyway-head regression after V79 expectation reconciliation: 59/59 PASS.
- Clean Flyway V1→V79 and V78→V79: PASS; one template and one rule/policy per current Tenant; no unrelated catalogue rows.
- Full Maven `verify`: 1,574 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS in 10:29.
- Architecture/Modulith: 52/52 PASS.
- Checkstyle: BUILD SUCCESS; zero configured violations. Existing repository warning baseline is unchanged.
- PMD 7.17.0: BUILD SUCCESS.
- SpotBugs: zero findings and zero errors.
- `git diff --check`: PASS.
- Authoritative database evidence: `transport_logistics_acceptance`; development database evidence: NO.

## Next task

`US-49-MANAGE-GEOFENCES-CS05-NOTIFICATION-INTEGRATION-001-RERUN`
