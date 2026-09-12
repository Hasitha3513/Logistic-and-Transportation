# US-50 Monitor Speed — CS05 Notification Integration

Status: **COMPLETE** on 2026-09-12. US-50 remains `IMPLEMENTATION_IN_PROGRESS`; accounting remains 73/87 complete. Flyway is V83 and V84 is absent.

## Durable contract and delivery

Tracking now publishes `VehicleSpeedingDetectedV1` through the shared P1-01 durable outbox when a `SpeedingEpisode` is confirmed. The canonical event and aggregate identity is the deterministic episode UUID, `occurredAt` is the confirmation source timestamp, producer is `TRACKING`, aggregate type is `SPEEDING_EPISODE`, delivery is at least once, and no global ordering is claimed.

The version-1 payload contains exactly `speedEpisodeId`, `vehicleId`, nullable `driverId`, nullable `tripId`, nullable `routeId`, nullable `routeVersion`, `observedSpeedKph`, `effectiveThresholdKph`, `thresholdSource`, `ruleId`, `ruleVersion`, `severity`, `sourceTimestamp`, and `repeatCount`. It excludes coordinates, position/device/provider identity, raw telemetry, credentials, Driver/Customer PII, and unrestricted metadata.

The Notification bridge fails malformed or non-version-1 events permanently, maps WARNING to Notification WARNING and HIGH to Notification CRITICAL, and delegates recipient, template, preference, channel, execution identity, and history behavior to the existing Notification engine. Producer and consumer replay converge on one logical notification.

## V83 catalogue seed

V83 is a narrow catalogue-only migration. It adds one active version-1 IN_APP template and one enabled Tenant-scoped `ROLE` / `DISPATCHER` rule and default policy per existing Tenant for `VEHICLE_SPEEDING_DETECTED_V1`. Quiet hours, suppression, and escalation are disabled. The template describes a configured operational speed threshold and makes no legal-limit, punitive, violation, payroll, or disciplinary claim. No schema, table, column, index, permission, or public API changes were made.

## Verification

All authoritative PostgreSQL evidence used only `transport_logistics_acceptance`.

- PostgreSQL end-to-end event journey: PASS for first WARNING episode, repeat HIGH episode, exact outbox payload, same-Tenant Dispatcher notification, Tenant-B exclusion, continued-packet non-flooding, and producer/consumer replay idempotency.
- Complete Tracking: 240/240 PASS.
- Complete Notification: 165/165 PASS.
- Architecture/Modulith: 52/52 PASS.
- Complete Maven: 1,656 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS in 12:47.
- Flyway clean V1→V83 and V82→V83: PASS. V83 contains catalogue data only; V84 is absent.
- Checkstyle: 0 configured violations. PMD: PASS. SpotBugs: 0 findings/errors.
- Signed PostgreSQL-backed Chromium ingestion/performance smoke: 1/1 PASS in 32.6 seconds; sustained 461.9 messages/second, burst 1,498.7 messages/second, latest p95 18.2 ms, history p95 17.9 ms.
- `git diff --check`: PASS.

## Scope exclusions and next task

No Driver violation, discipline, licence, payroll, Operations exception, frontend, public API, permission, speed-evaluation semantic, dependency, or Notification schema change was introduced. US-48 remains on external-prerequisite hold and US-50 final physical-source speed fidelity remains an acceptance gate. Next: `US-50-MONITOR-SPEED-CS06-FRONTEND-001`.
