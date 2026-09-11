# US-49 Manage Geofences — CS05 Closure Remediation

**Task:** `US-49-MANAGE-GEOFENCES-CS05-CLOSURE-REMEDIATION-001`  
**Result:** COMPLETE  
**Date:** 2026-09-11  
**Database:** `transport_logistics_acceptance`  
**Flyway:** V1→V79; V80 absent

## Closure outcome

The two CS05 closure blockers are resolved. The configured repository-wide PMD gate originally reported 63 `UnnecessaryImport` findings. All 63 were removed without suppressing or weakening PMD. The cleanup affected 50 pre-existing production Java files across billing, delivery, fleet, freight, fuel, organization, routing, and trip; diff inspection with end-of-line differences ignored confirmed that these unrelated files contain import-only edits.

The missing clean PostgreSQL journey is implemented in `GeofenceEvaluationPostgreSqlAcceptanceTest`. It starts with a clean schema, migrates V1→V78, installs controlled same-Tenant and Tenant-B Dispatcher fixtures, migrates the actual V79 catalogue, and exercises production runtime components without post-migration substitute rule or template SQL.

## Durable journey evidence

- Silent initial outside membership: no transition and no outbox event.
- First inside observation: pending hysteresis only; no transition and no outbox event.
- Second distinct inside observation: one persisted `UNAUTHORIZED_ZONE_ENTERED` transition with `HIGH` severity.
- Event: `VehicleGeofenceTransitionedV1`, version 1, event ID equal to transition UUID, `occurredAt` equal to the transition source timestamp.
- Shared P1-01 outbox: exactly one logical event for `geofence-transition-notification-bridge`.
- Payload keys are exactly `geofenceId`, `vehicleId`, `locationId`, `geofenceType`, `transition`, `severity`, `sourceTimestamp`, and `definitionVersion`.
- Actual V79 Tenant rule resolves `ROLE / DISPATCHER`; actual V79 `IN_APP` version-1 template renders.
- Exactly one same-Tenant Dispatcher notification and one accepted rule execution are persisted; Tenant B receives none.
- Consumer replay, direct event redelivery, and producer reevaluation create no duplicate event or notification.
- Event and rendered notification assertions exclude coordinates, polygon, device/provider identifiers, IMEI, raw telemetry, credentials, Driver PII, and Customer PII.

Tenant-safe recipient resolution required the Identity notification directory to query users through the current Tenant administration context. The existing offline-sync notification fixture was updated to create canonical-Tenant memberships for its controlled users; its focused regression passes 4/4.

## Verification

| Gate | Actual result |
| --- | --- |
| Clean V1→V79 journey | 3 tests, 0 failures, 0 errors; 24.72 s |
| Tracking Java | 171/171 PASS; 180.239 s summed test time |
| Notification Java | 164/164 PASS; 37.432 s summed test time |
| Identity/RBAC and Tenant/privacy selection | 78/78 PASS; Maven 01:05 |
| Full `./mvnw verify` | 1,583 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS; 10:16 |
| Architecture/Modulith | 52/52 PASS; 23.614 s |
| Checkstyle | 0 violations; BUILD SUCCESS |
| PMD | 63 before, 0 failing findings after; BUILD SUCCESS |
| SpotBugs | 0 findings; BUILD SUCCESS |
| Sustained signed ingress | 590.1 msg/s (threshold 200) |
| Burst signed ingress | 1,417.7 msg/s (threshold 1,000) |
| Latest-query p95 | 17.2 ms (threshold 200 ms) |
| History-query p95 | 18.9 ms (threshold 500 ms) |
| Chromium performance journey | 1/1 PASS; 31.4 s |
| `git diff --check` | PASS |

No development-database evidence was accepted. No frontend, API, permission, dependency, schema, migration, event-family, Operations-integration, or product-contract change was made. Application code was not committed or pushed.

## State

- CS05: COMPLETE
- US-49: `IMPLEMENTATION_IN_PROGRESS`
- US-48: `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`
- Accounting: 72 / 87 COMPLETE
- Next task: `US-49-MANAGE-GEOFENCES-CS06-FRONTEND-001`
