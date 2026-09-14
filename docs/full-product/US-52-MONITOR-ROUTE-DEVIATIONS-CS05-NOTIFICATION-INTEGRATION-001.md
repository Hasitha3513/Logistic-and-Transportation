# US-52 Monitor Route Deviations — CS05 Notification Integration

Status: **COMPLETE** on 2026-09-14. US-52 remains `IMPLEMENTATION_IN_PROGRESS`; accounting remains 73/87 complete and 14/87 remaining. Flyway head is V90.

## Approved contract and durable publication

Tracking publishes minimized `VehicleRouteDeviationDetectedV1` and `VehicleRouteDeviationEscalatedV1` envelopes through the existing P1-01 transactional outbox. Delivery is at least once and Notification consumption is idempotent. Tracking evidence is committed independently of Notification processing, so a Notification failure cannot invalidate the route-deviation episode or review evidence.

Detection is emitted once at confirmation. WARNING maps to Notification `WARNING`; domain HIGH maps explicitly to the existing Notification `CRITICAL` value. A directly confirmed HIGH episode emits only its HIGH detection. The first later WARNING-to-HIGH transition emits one `DISTANCE_HIGH` escalation. The first rejected HIGH review emits one `REVIEW_REJECTED` escalation. Repeated HIGH telemetry, approval, closure, progress, non-evaluable telemetry and review correction remain silent.

The payload and rendered content preserve Tenant, episode, vehicle, planned-route revision, rounded observed distance and UTC source time. They prohibit coordinates, geometry, review notes, raw telemetry, provider/device details, credentials/signatures, driver/customer PII and driver identity in the message. Active same-Tenant Dispatcher members receive IN_APP notifications only; no email, SMS or Operations/US-78 integration was introduced.

## V90 catalogue seed

V90 adds exactly two active IN_APP templates and their Tenant-scoped ROLE/DISPATCHER rules and required default policy associations:

- `TRACKING_ROUTE_DEVIATION_DETECTED_V1`
- `TRACKING_ROUTE_DEVIATION_ESCALATED_V1`

It creates no role, permission, severity value, schema object or unrelated template. The platform severity enum remains `INFO`, `WARNING`, `CRITICAL`.

## Verification

All accepted direct PostgreSQL evidence used only `transport_logistics_acceptance`.

- Focused route-deviation domain, bridge and architecture verification: 19/19 PASS.
- Notification bridge mapping and privacy verification: 3/3 PASS.
- V90 catalogue and route-deviation management PostgreSQL verification: 5/5 PASS.
- PostgreSQL outbox-to-Notification journey, Tenant isolation and duplicate replay: 1/1 PASS.
- Notification catalogue/controller/producer coverage: 14/14 PASS.
- Corrected V90 production-invariant and catalogue-count regression group: 29/29 PASS.
- Architecture and Spring Modulith verification: 59/59 PASS.
- Complete Maven `clean verify`: 1,745 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS in 16:15.
- Flyway clean V1→V90 and current-head verification: PASS.
- Checkstyle: 0 configured violations. PMD: PASS. SpotBugs: 0 findings/errors.
- Maven dependency analysis: BUILD SUCCESS; repository-wide undeclared/unused dependency advisories remain pre-existing debt and were not expanded by CS05.
- `git diff --check`: PASS.

## Scope and next task

No public API, permission, Notification severity enum/schema, Operations workflow, email/SMS channel, frontend, dependency or story accounting change was made. CS05 is closed; US-52 remains implementation-in-progress pending later change sets.

Next: `US-52-MONITOR-ROUTE-DEVIATIONS-CS06-FRONTEND-001`.
