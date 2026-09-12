# US-50 Monitor Speed — CS04 APIs, RBAC and Audit

Status: **COMPLETE** on 2026-09-12. US-50 remains `IMPLEMENTATION_IN_PROGRESS`; accounting remains 73/87 complete. Flyway is V82 and V83 is absent.

## API contract

The exact base is `/api/v1/tracking/speed-monitoring`.

- Rules: `POST /rules`, `GET /rules`, `GET /rules/{ruleId}`, `PUT /rules/{ruleId}`, and explicit `POST /rules/{ruleId}/activate|disable|retire` commands. There is no DELETE or generic status mutation.
- States: `GET /states` and `GET /states/{vehicleId}`.
- Episodes: `GET /episodes` and `GET /episodes/{episodeId}`.
- Rule and state pages default to 20 and reject sizes above 100. Episode history requires an ordered UTC range no greater than 31 days, defaults to 100, rejects limits above 500, and uses stable descending `(startSourceTimestamp,id)` cursor ordering.
- DTOs expose configured `thresholdKph`/`thresholdSource`, rule/version evidence and nullable logical attribution. They exclude candidate/position identity, coordinates, raw telemetry, device/provider/IMEI facts, credentials and Driver/Customer PII. No authoritative legal-road-limit claim is made.

## Security, tenancy and permissions

V82 seeds exactly `SPEED_MONITOR_VIEW`, `SPEED_MONITOR_MANAGE` and `SPEED_EVENT_VIEW`, granting them only to existing `ADMIN` and `LOCAL_MVP_ADMIN` roles. HTTP matchers cover the literal `/api/v1/...` paths and the secured use-case decorator independently enforces the same matrix. The permissions do not imply one another.

Tenant identity is taken only from authenticated `CurrentTenant`; no API request accepts `tenantId`. Every repository lookup is Tenant-qualified and foreign-Tenant identifiers are shaped as safe not-found responses.

## Commands, concurrency, idempotency and audit

Create, activate, disable and retire use Tenant-scoped persistent idempotency claims in the existing `tracking_audit_event` mechanism. Same-key/same-request replay is safe, changed-request reuse conflicts, and another Tenant may independently reuse the key. PUT uses optimistic rule versioning. V81 database uniqueness remains authoritative for active Tenant and route/version rules, with constraint and stale-version failures translated to safe HTTP conflicts.

Successful create, update, activate, disable and retire operations emit one management audit containing only rule identity, scope, lifecycle/version transition, configured threshold and nullable route/version references. Failed validation/authorization and read/evaluation/telemetry operations do not emit misleading management audits.

## Verification

All authoritative PostgreSQL evidence used only `transport_logistics_acceptance`.

- Final focused API/RBAC/permission/idempotency/audit gate: 12/12 PASS.
- Focused CS04 plus V81 persistence and architecture gate: 72/72 PASS.
- Complete Tracking: 238/238 PASS.
- Complete Trip: 101/101 PASS.
- Architecture/Modulith: 52/52 PASS.
- Complete Maven: 1,650 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS in 12:30.
- Flyway V1→V82 and V81→V82: PASS. V82 contains permission/grant rows only; V83 is absent.
- Checkstyle: 0 configured violations. PMD: PASS. SpotBugs: 0 findings/errors.
- `git diff --check`: PASS.

## Scope exclusions and next task

No frontend, durable event adapter, Notification catalogue/consumer, Driver mutation, speed-evaluation semantic change, speed table/column/index, or dependency was added. Next: `US-50-MONITOR-SPEED-CS05-NOTIFICATION-INTEGRATION-001`.
