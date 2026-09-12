# US-50 Monitor Speed — CS03 Evaluation and Episodes

Status: **COMPLETE** on 2026-09-12. US-50 remains `IMPLEMENTATION_IN_PROGRESS`; story accounting remains 73/87 complete. Flyway remains V81 and no V82 exists.

## Implemented runtime

- An accepted, trusted, in-order, recent Vehicle-associated position with a valid normalized `speedKph` atomically enqueues one idempotent `tracking_speed_evaluation_job`. Missing speed and ineligible positions do not enqueue pointless work.
- `SpeedEvaluationCoordinator` is the single global scheduled entry point. It uses bounded claims, a fixed worker pool, a bounded queue, owner-qualified lease renewal/release, bounded retry (maximum five claims), and privacy-safe error codes. `app.tracking.speed-evaluator.enabled` defaults to disabled and must be explicitly enabled.
- Jobs reuse the V81 `FOR UPDATE SKIP LOCKED` repository and global due-job index. Evaluation locks Tenant/Vehicle state in PostgreSQL; different Vehicles are not globally serialized.
- Trip attribution uses only `VehicleTripAssignmentLookup.findAt(tenantId, vehicleId, sourceTimestamp)`. The Trip-owned adapter resolves the applicable execution by source time and returns nullable Trip, Driver, route, and route-version facts. Empty or failed attribution retains Vehicle evaluation and uses the Tenant fallback.
- Matching ACTIVE route/version rules take precedence over the ACTIVE Tenant fallback. No effective rule produces `CONFIGURATION_UNAVAILABLE` without an episode or publication.
- Source ordering is `(sourceTimestamp, positionId)`. Duplicate and delayed work completes idempotently without rewinding state.

## Episode behavior

- At/below threshold initializes or returns to NORMAL. The first above-threshold sample creates only a silent candidate; the second distinct consecutive sample under the same rule version confirms one deterministic `SpeedingEpisode`.
- Continued above-threshold samples progress the same episode's maximum speed and evidence count without another publication. One eligible at/below-threshold sample closes it. Missing or ineligible data never fabricates clearance.
- Active episodes retain their frozen rule snapshot. Pending candidates reset when the effective rule changes.
- Episode identity is SHA-256-derived from Tenant, Vehicle, rule identity/version, and first candidate position. Concurrent confirmation converges on one row and one logical publication call.
- A same-rule episode starting within the inclusive ten-minute repeat window is HIGH with incremented repeat count; first, different-rule, and outside-window episodes are WARNING with repeat count zero.
- `SpeedingEpisodePublisherPort` is invoked only on new confirmation. Its source timestamp and observed speed are the confirming observation. The payload remains minimized and excludes coordinates, device/provider facts, position identity, and raw telemetry. CS03 supplies an explicit no-op default adapter; the shared durable P1-01 adapter remains deferred to CS05.
- No Driver, payroll, disciplinary, Notification, API, RBAC, audit-management, or frontend production behavior was introduced.

## Verification evidence

All accepted PostgreSQL evidence used only `transport_logistics_acceptance`; the development database was not authoritative evidence.

- Focused runtime/persistence/domain/architecture selection: 80 tests, 0 failures, 0 errors, 0 skipped.
- Complete Tracking regression: 227 tests, 0 failures, 0 errors, 0 skipped.
- Complete Trip regression: 101 tests, 0 failures, 0 errors, 0 skipped.
- Architecture/Modulith: 52 tests, 0 failures, 0 errors, 0 skipped. The only Tracking-to-Trip dependency is the published lookup contract/provider; no Tracking dependency on Trip implementations, Driver implementations, or Notification implementations was introduced.
- Full `./mvnw verify`: 1,639 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS in 11:58.
- Flyway clean replay: V1 through V81 applied successfully; V81 is current and V82 is absent.
- Checkstyle: 0 violations. PMD: PASS. SpotBugs: 0 findings.
- Signed Chromium ingress with the speed evaluator enabled and valid `speedKph`: 432.7 msg/s sustained for 200 messages; 1,563.8 msg/s burst for 1,000 messages; latest P95 20.0 ms; history P95 17.6 ms; 1/1 PASS.
- `git diff --check`: PASS.

## Scope boundary and next task

CS03 stops at `SpeedingEpisodePublisherPort`. It creates no migration, REST API, permissions, frontend, durable event adapter, Notification catalogue/consumer, or Driver mutation. Next: `US-50-MONITOR-SPEED-CS04-APIS-RBAC-AUDIT-001`.
