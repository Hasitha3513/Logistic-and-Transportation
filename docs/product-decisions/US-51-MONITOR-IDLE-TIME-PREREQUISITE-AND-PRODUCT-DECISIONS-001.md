# US-51 Monitor Idle Time — Prerequisite and Product Decisions

**Identifier:** `US-51-MONITOR-IDLE-TIME-PREREQUISITE-AND-PRODUCT-DECISIONS-001`

**Identifier status:** NEWLY CREATED by the explicit US-51 workstream selection; not a prior approved task

**Decision status:** PROPOSED — IMPLEMENTATION AUTHORIZATION REQUIRED

**Flyway:** V100; no migration is authorized by this decision package

**Accounting:** 73/87 COMPLETE

## Purpose and authoritative intent

US-51 measures idle duration by comparing engine-on time with movement, records idle events, shows
history and optionally estimates fuel waste when a governed input exists. The original UML requires
insufficient telemetry to produce `Unknown / Unconfirmed`; it does not authorize treating missing
data as zero idle or synthesizing engine truth.

This package reviews the full dependency chain and proposes one implementable contract. Every item
marked **PROPOSED** requires one consolidated approval before implementation.

## Verified current implementation

| Layer | Existing implementation | Demonstrated gap |
| --- | --- | --- |
| Canonical telemetry | V1/V2 contain `engineState` (`ON/OFF/UNKNOWN`), source time, speed, position, trust and dedupe identity | Existing adapters use ignition as `engineState`; the name overstates the evidence |
| Flespi | Production polling, canonical Kafka publication and history; provider capability excludes `IGNITION` | Mapper always emits `UNKNOWN`; real device mapping/capture is absent |
| Traccar 6.15.3 target | Production polling maps boolean `attributes.ignition`; advertises `IGNITION` | Attribute is device-reported ignition, not proof of combustion/traction engine running |
| Generic signed ingress | Optional `ignitionOn` maps to current `engineState` | Contract likewise represents ignition only and has no governed engine-running claim |
| Capability registry | Effective-dated, Tenant/device/source-time lookup; `IGNITION` vocabulary exists | No distinct `ENGINE_RUNNING` capability |
| Immutable history | Timescale history retains source-time engine state, speed, trust and ordering | No separated ignition/running values; legacy semantics must remain readable |
| Durable evaluation | V91 dispatch supports geofence, speed and route-deviation evaluators | `IDLE` evaluator type and durable dispatch path do not exist |
| Idle domain | None | No policy, state, episode, evidence, recovery, dedupe or restart behavior |
| Effects/API/UI | Existing Tracking Notification/Operations, RBAC/API/UI patterns are reusable | No US-51 effect contract, permission, endpoint or page exists |
| Fuel estimate | Fuel exposes bounded performance metrics, not an effective-dated idle burn rate | No approved input can calculate defensible per-episode litres |

## Provider semantics and capability truth

Official Flespi material names the field `engine.ignition.status`, shows that message parameters
depend on protocol, device type, configuration and plugins, and separately exposes movement/speed.
See [Flespi message semantics](https://flespi.com/kb/messages-basic-information-units) and
[Flespi expressions](https://flespi.com/kb/expressions).

The official Traccar API defines Position `attributes` as provider/protocol-specific custom data;
Traccar's computed-attribute documentation says the available position attributes depend on what
the device reports. See [Traccar API](https://www.traccar.org/api-reference) and
[Traccar computed attributes](https://www.traccar.org/computed-attributes/).

Therefore provider support and device support are separate:

- Flespi may normalize `engine.ignition.status` when the bound device actually reports it.
- Traccar may expose boolean `attributes.ignition` when the protocol/device reports it.
- Neither value alone means an engine or EV traction system is running.
- A provider adapter capability describes a possible normalized field; the effective-dated device
  capability remains authoritative for a particular Tenant/device/source time.

## Consolidated proposed decisions

### D1 — Operational definition and supported vehicles — PROPOSED

Phase 1 supports internal-combustion and hybrid Vehicles only when the bound telemetry device has
an effective `ENGINE_RUNNING=SUPPORTED` capability and reports explicit engine-running truth.
Battery-electric, fuel-cell, stationary plant and auxiliary/PTO idling are excluded until their
energy/ready-state semantics are separately approved.

An eligible idle interval is continuous trustworthy evidence that:

1. `engineRunningState=RUNNING`;
2. speed is present and at most **3.0 km/h**; and
3. successive trustworthy positions do not show more than **50 m** displacement after expanding
   the comparison by reported horizontal accuracy.

Ignition, zero speed, external power, charging and connectivity are never substitutes for item 1.

### D2 — Signals, provenance and capability eligibility — PROPOSED

Introduce separate canonical `ignitionState` and `engineRunningState` values, each
`ON/OFF/UNKNOWN` or `RUNNING/NOT_RUNNING/UNKNOWN` as appropriate. Preserve the current legacy
`engineState` field as ignition semantics for V1/V2 compatibility; never reinterpret stored rows.

`ENGINE_RUNNING` is supported only from an approved device-native engine/CAN/RPM/running field
whose provider mapping and real device capture are recorded. A derived Traccar computed attribute
is eligible only if its expression consumes that approved native engine fact—not voltage, speed,
motion or connectivity. Provenance must retain provider type, device binding, source time,
capability interval and canonical event identity without storing raw payloads or credentials.

### D3 — Freshness, ordering, gaps and unknown state — PROPOSED

- Evaluate source-time order from immutable Timescale history, not Redis arrival order.
- Require trusted, non-suspect telemetry with `ENGINE_RUNNING=SUPPORTED` at source time.
- Maximum gap between qualifying observations: **2 minutes**.
- Missing, explicit `UNKNOWN`, stale, out-of-order-only or unsupported evidence yields
  `UNKNOWN/UNCONFIRMED`, never false, normal or zero idle.
- Late observations remain in history but cannot rewrite a closed episode or regress current state;
  a bounded reconciliation command may create append-only correction evidence if later approved.
- A gap stops credited duration at the last qualifying source timestamp and closes a confirmed
  episode with `EVIDENCE_GAP`; the gap duration is not counted.

### D4 — Confirmation and recovery — PROPOSED

- Candidate requires at least two eligible observations.
- Confirm after **5 continuous minutes** of qualifying evidence.
- Episode start remains the first qualifying source timestamp, so confirmed duration includes the
  candidate interval.
- Explicit `engineRunningState=NOT_RUNNING` closes immediately at that source timestamp.
- Movement recovery requires two eligible observations above 3.0 km/h or outside the stationary
  accuracy-adjusted radius, at least 30 seconds apart; close at the first recovery observation.
- A single noisy movement observation does not close an episode.
- Thresholds are fixed Phase-1 product values, not Tenant-editable rules.

### D5 — Episode identity, retries and restart safety — PROPOSED

Tracking owns one open idle episode per `(tenant_id, vehicle_id)`. PostgreSQL advisory locking is
Tenant/Vehicle-qualified. Immutable evidence uses the canonical dedupe identity; duplicate Kafka
delivery or polling replay cannot increment duration, evidence count or effects. The existing V91
dispatch gains `IDLE`; acknowledgement occurs only after episode/evidence persistence commits.
Expired claims recover through the existing bounded lease model, and Redis is never authoritative.

### D6 — Canonical compatibility — PROPOSED

Create additive canonical telemetry **V3** on a new governed topic/DLT pair. V3 preserves V1/V2
identity, Tenant, Vehicle, Device, position, time and dedupe semantics; it adds separated ignition
and engine-running observations plus minimized provenance code. No producer dual-publishes a
single observation. V1/V2 consumers remain during the retention/rollback window; US-51 evaluates
only V3 `engineRunningState` with supported source-time capability.

### D7 — Persistence and proposed migration boundaries — PROPOSED

These numbers describe reviewable boundaries only; they are not reserved or authorized:

- **Proposed V101:** allow event version 3; add nullable immutable-history `ignition_state`,
  `engine_running_state` and constrained provenance code; extend capability vocabulary with
  `ENGINE_RUNNING`; preserve legacy `engine_state` unchanged.
- **Proposed V102:** add Tenant-leading idle current-state, episode and append-only evidence tables;
  one-open-episode uniqueness; dedupe constraints; source-time/keyset indexes; extend the V91
  evaluator constraint with `IDLE` and add only its necessary claim index.
- **Proposed V103:** seed exactly `IDLE_MONITOR_VIEW` and `IDLE_EVENT_VIEW` and assign them to the
  existing same-Tenant Dispatcher/Admin role policy. No Notification catalogue entry is proposed.

No migration may create engine facts, backfill legacy ignition as engine-running, mutate immutable
history, or change foreign module tables.

### D8 — Notification and Operations — PROPOSED

Phase 1 creates **no Notification and no Operations fact**. The authoritative story requires
monitoring, recording and history, not escalation. This avoids inventing severity, recipients or
operational policy. Future effects require a separate product decision and catalogue authorization.

### D9 — API, RBAC, audit and UI — PROPOSED

- `IDLE_MONITOR_VIEW`: same-Tenant current state and availability.
- `IDLE_EVENT_VIEW`: bounded episode/evidence history and detail.
- Read-only, no-store endpoints under `/api/v1/tracking/idle-monitoring` for paginated state,
  paginated episodes and episode detail/evidence.
- Tenant-bound opaque keyset cursors; bounded explicit source-time range; no generic status mutation.
- Safe output: Vehicle label/ID, state, episode timestamps, confirmed idle duration, evidence
  quality/capability, optional estimate availability and source label. No coordinates, raw provider
  payload, device external reference, credentials, Driver/Customer PII or engine/CAN raw values.
- Audit protected history queries using the existing minimized read-audit convention; there is no
  review/acknowledgement mutation in Phase 1.
- Permission-aware responsive operator pages for current state, history and detail, explicitly
  rendering `UNKNOWN`, `UNSUPPORTED`, `STALE` and `EVIDENCE_GAP`.

### D10 — Fuel estimate — PROPOSED

Fuel waste remains `UNAVAILABLE` in Phase 1 because no approved effective-dated idle burn-rate
contract exists. Do not derive it from general litres/engine-hour performance or a global constant.
A later Fuel-published estimate must be labelled non-authoritative, versioned and effective-dated;
Tracking may store the applied rate/version snapshot but must not query Fuel persistence.

### D11 — Acceptance separation — PROPOSED

Technical acceptance uses controlled canonical events to prove logic, PostgreSQL/Kafka recovery,
Tenant isolation and UI behavior. Final acceptance additionally requires a supported physical
combustion/hybrid Vehicle or safe bench rig, device-native engine-running evidence, real provider
transport, safe idle/movement procedure, privacy review and authorized operator sign-off. Fixtures,
ignition-only capture and simulated messages cannot satisfy that field gate.

## Proposed bounded implementation change sets

| Change set | Scope | Dependencies | Completion criteria |
| --- | --- | --- | --- |
| CS01 canonical engine semantics | V3 contract, provider-neutral types, Flespi/Traccar/Generic mappings guarded by device capability | Approval of D1-D6 and real source-field mapping | Contract/compatibility/provider tests; no inference; V1/V2 unchanged |
| CS02 V101 history/capability | Immutable V3 storage and effective capability | CS01 | Clean V1→head and V100→head; exact constraints; append-only and Tenant tests |
| CS03 V102 idle persistence/dispatch | State, episode, evidence, idempotency, lease/claim support | CS02 | Deterministic concurrency, replay, restart, gap and ordering tests |
| CS04 evaluator | D1-D5 policy and optional estimate-unavailable result | CS03 | Threshold/recovery/property tests and retained detector regressions |
| CS05 V103 APIs/RBAC/audit | Two permissions and read-only bounded endpoints | CS03-CS04 | Literal HTTP allow/deny, Tenant A/B, cursor/privacy/audit tests |
| CS06 frontend | State/history/detail pages | CS05 | Component, accessibility, session-clearing, TypeScript/build and Chromium tests |
| CS07 PostgreSQL/Kafka performance/recovery | Concurrency, leases, dedupe, query plans and bounded workload | CS01-CS06 | No deadlocks/duplicates/leaks; controlled measurements, not production SLOs |
| Technical closure | Consolidated mandatory gates and evidence | CS01-CS07 | Full backend, architecture/static, frontend and Chromium gates pass |
| Final acceptance | Genuine engine-running field journey | Technical closure plus external prerequisites | Independent physical/provider/operator matrix passes |

## Consolidated implementation authorization request

Approve D1-D11 and authorize CS01-CS07 as separate governed commits, including additive canonical
V3 and proposed forward migrations V101-V103 **only if those versions remain free at each preflight**.
Authorization must identify at least one approved device-native engine-running source and mapping.
Without that source, CS01 may implement contract scaffolding and fixtures but US-51 production
evaluation must remain disabled and final acceptance blocked.
