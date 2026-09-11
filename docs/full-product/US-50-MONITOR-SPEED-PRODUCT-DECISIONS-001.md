# US-50 Monitor Speed — Product Decisions

**Task:** `US-50-MONITOR-SPEED-PRODUCT-DECISIONS-001`  
**Decision:** `PRODUCT_DECISIONS_FROZEN / READY_FOR_IMPLEMENTATION`  
**Date:** 2026-09-11  
**Owner:** Tracking  
**Flyway head:** V80; V81 does not exist  
**Accounting:** unchanged at 73/87 complete and 14/87 remaining  
**Next:** `US-50-MONITOR-SPEED-CS01-DOMAIN-PORTS-001`

## Source traceability and boundary

The requirements DOCX defines US-50 as a Tracking / Control Room Operator capability that compares Vehicle
speed with applicable thresholds, records repeat violations, and alerts with Vehicle/time/location where
available. The UML names configured thresholds, road-specific rules, speed evaluation, over-speed
notification, repeat tracking and a recorded speed-violation event. US-48 supplies the optional normalized
`speedKph`, trusted Vehicle association and immutable source time. US-49 establishes the Tracking-owned
durable evaluator/outbox conventions. P1-01 supplies the only durable cross-module transport.

US-50 may use the technically frozen US-48 contract but inherits no US-48 acceptance. Tracking detects and
owns speed-monitoring evidence. Trip owns Trip/Driver/route assignment facts, Routing owns routes and legal
road-rule facts, Driver owns violations, discipline and performance, and Notification owns recipients,
channels, templates, preferences, retry and delivery history. US-50 creates no punishment, payroll, licence,
insurance, police, camera, vehicle-control, route-deviation, idle, GPS-exception or dashboard behavior.

## Signal, units and eligibility

- The canonical signal is the accepted Tracking `PositionEvent.speedKph`; internal persistence, thresholds,
  comparison and API values use decimal kilometres per hour. Provider adapters normalize other source units
  at ingress and preserve truthful absence. UI labels every value `km/h`; silent mph conversion is forbidden.
- A position is eligible only when it is nonduplicate, `TRUSTED`, Vehicle-associated at source time,
  `IN_ORDER`, has a valid immutable source timestamp, contains valid speed, and is no more than five minutes
  old when evaluated. Clock-skew, future, late, stale, out-of-order, untrusted and duplicate positions cannot
  change current speed state or generate alerts.
- Accepted historical observations remain queryable under US-48 retention but do not create retrospective
  episodes or notifications. Missing speed is `UNKNOWN / NOT_EVALUATED`, never zero or interpolated.
- Negative, non-finite, non-numeric, malformed-unit or greater-than-400 km/h speed is rejected at the US-48
  normalization boundary; it is never clamped. Monitoring ingestion remains available if evaluation fails.

## Threshold and road-rule decision

Phase 1 uses `ROUTE_CONFIG_ONLY_WITH_TENANT_FALLBACK`: Tracking owns explicit operational rules, not legal
road-law data. Precedence is (1) an ACTIVE same-Tenant rule for the source-time Trip's logical route and
route version, then (2) the ACTIVE Tenant-wide rule. There is no Vehicle-class or Vehicle-specific level.
Each decision snapshots rule ID, version, source and threshold.

No approved external road-limit provider, segment map matcher or published dynamic road-rule contract exists.
Consequently Phase 1 does **not** claim authoritative or live road-specific legal limits. A route rule is an
operator-configured operational threshold for a logical route/version. If route attribution or its rule is
absent, the explicit Tenant fallback applies; if neither rule exists, state is
`UNKNOWN / CONFIGURATION_UNAVAILABLE` and no event is produced. This limitation must be visible in UI,
manual and acceptance evidence.

Tolerance is exactly `0 km/h`. Equality is normal; only `observedSpeedKph > effectiveThresholdKph` is above
threshold. There is no shipped default threshold. Monitoring begins only after an authorized operator
configures and activates a positive threshold not exceeding 400 km/h.

Rules use `DRAFT -> ACTIVE <-> DISABLED -> RETIRED`; RETIRED is terminal. Configuration is editable only in
DRAFT or DISABLED. Activation creates an immutable incremented version/effective fact, enforces at most one
ACTIVE Tenant fallback and one ACTIVE rule per Tenant/route/version, and resets affected candidate/current
evaluation state without rewriting evidence. Disable/retire stops new evaluation. Management uses optimistic
versioning, persistent idempotency and audit; telemetry samples are not management-audited.

## Detection, episode and repeat semantics

Current state is `UNKNOWN | NORMAL | SPEEDING`. The first eligible sample establishes NORMAL or the first
above-threshold candidate silently. Two distinct consecutive eligible samples above the same effective rule
version confirm an episode. The episode starts at the first candidate's source time, records the confirming
position, and publishes once. One eligible sample at or below the same effective threshold clears SPEEDING
and closes the episode at that source time. An ineligible or missing-speed sample changes no episode and
exposes monitoring availability as UNKNOWN without fabricating a clearance.

Tracking persists a `SpeedingEpisode`, not one violation row per packet. It snapshots Tenant, Vehicle,
optional Trip/Driver/route/version attribution, rule ID/version/source, effective threshold, start,
confirmation and nullable end source times, maximum observed speed, eligible above-threshold sample count,
severity and repeat count. Further above-threshold samples update max/sample count but create no new event.
History is immutable after closure and follows the existing external Tracking retention policy.

A repeat is a newly confirmed episode for the same Tenant and Vehicle under the same rule ID/version within
ten minutes after the prior episode ended. It is a separate evidence episode with incremented rolling repeat
count, but publication remains exactly one logical notification per episode. There is no punitive escalation.
Severity is deterministic: first episode is `WARNING`; a repeat episode is `HIGH`. Duration and excess amount
are retained facts but do not create a hidden scoring model.

The deterministic episode identity is SHA-256 over Tenant ID, Vehicle ID, effective rule ID/version and the
first candidate position ID. Duplicate/concurrent evaluation therefore converges on one state, episode and
event. Same-Vehicle evaluation serializes; different Vehicles remain parallel.

## Attribution

At confirmation, Tracking may call a new published, read-only Trip contract
`VehicleTripAssignmentLookup.findAt(tenantId, vehicleId, sourceTimestamp)`. The minimized optional result is
`tripId`, nullable `driverId`, nullable `routeId` and nullable `routeVersion`. The provider must resolve only
the Trip whose execution/assignment covers source time and Tenant. Tracking stores UUID/string snapshots as
logical references with no foreign keys, joins or foreign repository access.

Missing or failed attribution never drops Vehicle speed evidence. Driver, Trip and route remain UNKNOWN.
There is no fallback to last-known assignment. A route rule can apply only when the returned route/version
matches; otherwise the explicit Tenant rule is used. US-50 does not mutate Driver violation, performance,
payroll or licence state and publishes no Driver-owned disciplinary command/event.

## Persistence, concurrency and failure behavior

V81 is likely and is reserved only after implementation rechecks the head. Expected Tracking-owned tables are
`tracking_speed_rule`, `tracking_speed_state`, `tracking_speed_episode` and
`tracking_speed_evaluation_job`. No generic policy engine or foreign physical key is approved. Every table,
index, uniqueness rule, query, idempotency identity and worker claim is Tenant-scoped and Tenant-leading.
Rules and events use bounded/indexable access at the US-48 scale.

An accepted eligible position atomically creates at most one bounded evaluation job. Evaluation is at least
once, leased and retryable; failure never rolls back Tracking position ingestion. Threshold lookup failure
produces `CONFIGURATION_UNAVAILABLE` and no event. Driver/Trip lookup failure leaves nullable attribution.
The required PostgreSQL concurrency matrix covers duplicate sample, simultaneous same-Vehicle evaluation,
rule activation/disable versus evaluation, attribution change versus source-time evaluation, out-of-order
sample, deterministic episode/event identity and Notification replay. Configuration that commits first is
authoritative; an evaluation that locked and committed first remains valid immutable evidence.

Performance must preserve sustained >=200 positions/s, burst >=1,000 positions/s, latest p95 <=200 ms and
24-hour history p95 <=500 ms. No per-packet publication, unbounded query or per-Vehicle thread is allowed.

## Event, Notification and privacy

Confirmed episodes publish exactly one durable `VehicleSpeedingDetectedV1` through the shared P1-01 outbox.
Producer is Tracking; consumer is Notification only. Delivery is at least once, with Tenant/event consumer
dedupe and no global ordering. Event ID is the deterministic episode UUID. Exact payload:

```json
{
  "speedEpisodeId": "UUID",
  "vehicleId": "UUID",
  "driverId": "UUID|null",
  "tripId": "UUID|null",
  "routeId": "UUID|null",
  "routeVersion": "string|null",
  "observedSpeedKph": "decimal",
  "effectiveThresholdKph": "decimal",
  "thresholdSource": "ROUTE_CONFIG|TENANT_CONFIG",
  "ruleId": "UUID",
  "ruleVersion": "long",
  "severity": "WARNING|HIGH",
  "sourceTimestamp": "UTC instant",
  "repeatCount": "integer"
}
```

Coordinates, Position/device/provider identity, raw telemetry, credentials, Driver/Customer PII and arbitrary
metadata are prohibited. Tracking detail may retain an internal logical position reference, but alerts and
Notification never expose coordinates. Notification resolves same-Tenant recipients and owns suppression,
preferences, templates, channels, retry and history. Replay produces no second logical delivery.

## RBAC, API and frontend

The exact human permissions are `SPEED_MONITOR_VIEW`, `SPEED_MONITOR_MANAGE` and `SPEED_EVENT_VIEW`.
They grant no Driver violation authority and imply neither another. Tenant comes only from trusted context;
client bodies never accept `tenantId`, and cross-Tenant IDs are safe not-found.

The frozen family is `/api/v1/tracking/speed-monitoring`: bounded rule list/detail/create/update and explicit
activate/disable/retire commands; bounded current-state list/detail; and bounded episode history/detail.
Lists default to 20 and cap at 100. Episode history requires a bounded UTC range no greater than 31 days,
uses stable `(startSourceTime,id)` keyset ordering, defaults to 100 and caps at 500. There is no generic status
PATCH, raw telemetry endpoint or unbounded export.

The operator UI uses the existing React/React Router/Ant Design/TanStack Query/React Hook Form/Zod/Axios/
AuthContext stack. It supports rule configuration, active conditions and episode history with Vehicle and
permitted logical attribution, severity, observed/effective speed and source time. It labels configured
operational thresholds and UNKNOWN states explicitly. No Refine, Pro Components, map SDK or US-54 dashboard.

## Acceptance and delivery sequence

Technical acceptance may use deterministic signed provider-neutral fixtures carrying valid `speedKph`, real
PostgreSQL, literal-URL RBAC, event/Notification replay, concurrency and performance evidence. Final acceptance
of real-world speed fidelity requires a physical device/provider payload whose speed field and unit mapping are
verified. Until that exists, implementation and technical closure may pass but final US-50 status must be
`ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`; fixtures may not masquerade as physical evidence.

Required scenarios are below threshold, exactly threshold, first candidate, confirmed speeding, clearance,
repeat episode, duplicate, out-of-order, missing/invalid speed, rule update/disable, route-rule precedence and
Tenant fallback, configuration unavailable, cross-Tenant denial, Driver known/unknown, one Notification plus
replay, bounded queries and the frozen performance gates.

Controlled sequence: CS01 Domain + Ports; CS02 V81 persistence; CS03 evaluation/episodes; CS04 APIs/RBAC/audit;
CS05 Notification integration; CS06 frontend; CS07 PostgreSQL concurrency/performance; technical closure; then
independent final acceptance subject to the real-speed external gate.

## Decision gate

All required product, ownership, telemetry, threshold, road-rule limitation, tolerance, detection, clearance,
episode, repeat, severity, attribution, persistence, retention, event, Notification, privacy, RBAC, API,
frontend, invalid/missing data, concurrency, performance, migration and acceptance decisions are frozen.
This task creates no production code, migration, API implementation, permission seed or event implementation.
