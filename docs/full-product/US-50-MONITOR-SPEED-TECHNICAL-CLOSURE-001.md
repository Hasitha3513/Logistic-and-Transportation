# US-50 Monitor Speed — Technical Closure

**Verdict:** `TECHNICAL_CLOSURE_COMPLETE`  
**US-50:** `IMPLEMENTATION_COMPLETE / FINAL_ACCEPTANCE_PENDING_EXTERNAL_SPEED_FIDELITY`  
**Flyway:** V83; V84 absent  
**Accounting:** unchanged at 73/87 complete and 14/87 remaining

## Executive conclusion and source traceability

The original requirements and US-50 UML require configured thresholds, road-specific rules, evaluation of
Vehicle speed, recorded repeat evidence and operator notification. Direct inspection of the frozen decisions,
CS01-CS07 implementation, migrations, security, frontend and tests finds the implementation aligned. There is
no implementation defect and no new product decision required. Final acceptance still requires genuine
physical device/provider telemetry whose speed field and km/h conversion are verified; that evidence is not
currently available and is an `EXTERNAL_DEPENDENCY / FINAL_ACCEPTANCE_EVIDENCE_ONLY` gap.

## Domain and runtime reconciliation

Tracking owns monitoring, state and episodes. Trip supplies only source-time assignment through
`VehicleTripAssignmentLookup.findAt(tenantId, vehicleId, sourceTimestamp)`; Routing owns route identity,
Driver owns disciplinary behavior, and Notification owns delivery. Tracking performs no Driver mutation.

The canonical input is `PositionEvent.speedKph` in km/h. Eligible input is nonduplicate, TRUSTED, IN_ORDER,
Vehicle-associated, valid at immutable source time, speed 0..400 and no more than five minutes old. Missing or
ineligible speed is unknown/not evaluated and never fabricated as zero or retrospectively alerted.

Resolution is ACTIVE route/version configuration, then ACTIVE Tenant fallback, otherwise
CONFIGURATION_UNAVAILABLE. These are configured operational thresholds, not authoritative legal limits.
Tolerance is zero: greater-than is speeding and equality is normal. State is exactly UNKNOWN, NORMAL or
SPEEDING. A first distinct above-threshold sample is silent; the second consecutive sample under the same rule
version confirms an episode. One eligible at/below sample closes it; rule changes do not bridge candidates or
rewrite frozen evidence.

Episode identity is SHA-256-derived from Tenant, Vehicle, rule ID/version and first position ID. Evidence
contains the frozen required identifiers, nullable source-time attribution, rule snapshot, timestamps,
maximum, count, WARNING/HIGH severity and repeat count. Same-rule episodes beginning within the inclusive
ten-minute boundary are HIGH repeats; later episodes are WARNING. Tracking never exposes CRITICAL.

## Persistence, API and security

V81 contains only the four Tenant-owned speed rule/state/episode/job tables, constraints, indexes and immutable
episode trigger. It enforces one ACTIVE Tenant rule, one ACTIVE Tenant/route/version rule, and one state per
Tenant/Vehicle. Closed evidence rejects UPDATE and DELETE. The job repository provides idempotent enqueue,
bounded global `FOR UPDATE SKIP LOCKED` claims, fixed bounded workers, lease renewal/retry/release/reclaim and
stale-owner rejection. Source ordering is `(sourceTimestamp, position UUID)`.

V82 seeds exactly `SPEED_MONITOR_VIEW`, `SPEED_MONITOR_MANAGE` and `SPEED_EVENT_VIEW` to existing ADMIN and
LOCAL_MVP_ADMIN roles. V83 contains only the Tenant-scoped Dispatcher IN_APP Notification catalogue for
`VehicleSpeedingDetectedV1`. V84 does not exist.

The `/api/v1/tracking/speed-monitoring` family provides bounded rule commands, current-state reads and stable
episode cursor history with a maximum 31-day UTC range. Rule/state pages default to 20 and cap at 100; episode
history defaults to 100 and caps at 500. Trusted authentication supplies Tenant; request bodies do not.
HTTP and use-case permission checks, safe foreign-Tenant not-found behavior, persistent Tenant-scoped command
idempotency, stale-version conflict and privacy-minimized management-only audit are verified.

## Event, Notification and frontend

Confirmation atomically publishes `VehicleSpeedingDetectedV1` version 1 through the shared P1-01 outbox.
Event and aggregate identity equal the episode ID; occurredAt is confirmation source time. Its payload has
exactly the frozen 14 fields and excludes coordinates, position/device/provider identity, raw telemetry,
credentials and personal data. Replay remains one logical outbox event and one same-Tenant Notification.
Notification maps Tracking WARNING to WARNING and HIGH to CRITICAL without changing Tracking evidence; the V83
text says configured threshold and makes no legal or disciplinary allegation. Continued packets do not flood.

The React/Router/Ant Design/TanStack Query/React Hook Form/Zod/Axios/AuthContext UI covers rule creation,
editing and lifecycle, current state, and episode list/detail without delete, map or US-54 dashboard scope.
It uses exact permission visibility, truthful UNKNOWN, textual WARNING/HIGH, labelled forms and keyboard/focus
capable Ant controls. It exposes none of the prohibited telemetry, credential or PII fields.

## Concurrency, plans, performance and quality

CS07 passed 41/41 concurrency cases three times (123/123), including duplicate/first-state/confirmation and
rule races, equal-time ordering, repeat boundary, claims, leases, failure rollback, replay and Tenant isolation.
Its test cleanup uses Flyway reset only in the isolated harness and leaves production immutability unchanged.
At 5,000 rows the intended global-due, route-rule, state-PK, repeat and vehicle-history indexes were used; no
V84 index is needed. Signed ingestion measured 441.3 msg/s sustained and 1,265.5 msg/s burst; latest p95 was
19.6 ms and history p95 18.4 ms, all within frozen gates and without claiming US-48 physical acceptance.

Fresh closure revalidation passes 73/73 representative PostgreSQL/runtime/API/security/architecture tests and
10/10 focused frontend tests. Fresh CS07 evidence remains: Maven 1,656/0/0/15, architecture 52/52, Vitest
309/309, Chromium 11/11, TypeScript/build/Checkstyle/PMD/SpotBugs/diff check PASS. The unchanged 71 Delivery
ESLint findings are unrelated pre-existing debt. Authoritative database evidence is
`transport_logistics_acceptance`; development-database authoritative evidence is NO.

## Gap classification and disposition

- Implementation defect: `NONE`
- Product decision required: `NONE`
- Documentation-only gap: `NONE`
- Remaining gap: `EXTERNAL_DEPENDENCY / FINAL_ACCEPTANCE_EVIDENCE_ONLY` — physical provider speed fidelity
- US-48 acceptance inheritance: `NONE`; US-48 remains externally blocked

Technical closure passes. If final acceptance ran now its expected disposition would be
`ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`. Next: `US-50-MONITOR-SPEED-FINAL-ACCEPTANCE-001`.
