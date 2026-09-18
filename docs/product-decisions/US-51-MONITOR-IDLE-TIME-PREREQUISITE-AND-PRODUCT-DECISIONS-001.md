# US-51 Monitor Idle Time — Prerequisite and Product Decisions

**Identifier:** `US-51-MONITOR-IDLE-TIME-PREREQUISITE-AND-PRODUCT-DECISIONS-001`

**Identifier status:** NEWLY CREATED by the explicit US-51 workstream selection; not a prior approved task

**Decision status:** APPROVED 2026-09-18 / IMPLEMENTATION AUTHORIZED AS BOUNDED CHANGE SETS

**Flyway:** V100; no migration is authorized by this decision package

**Accounting:** 73/87 COMPLETE

## Authorized readiness boundary

Software specification, canonical-contract implementation and controlled technical testing may
proceed without a physical device after D1-D11 receive product-owner approval. Production
`ENGINE_RUNNING` activation remains a separate gate: every production provider/device mapping must
stay unavailable until its native engine-running source, unit/type, device model/protocol and real
capture are verified and approved. Controlled fixtures may advertise the capability only in test
profiles. Physical acceptance remains a third, independent operator-sign-off gate.

## Purpose and authoritative intent

US-51 measures idle duration by comparing engine-on time with movement, records idle events, shows
history and optionally estimates fuel waste when a governed input exists. The original UML requires
insufficient telemetry to produce `Unknown / Unconfirmed`; it does not authorize treating missing
data as zero idle or synthesizing engine truth.

This package reviews the full dependency chain and freezes one implementable contract. D1-D11 were
explicitly approved on 2026-09-18. Their former **PROPOSED** labels record the reviewed state; the
documented choices are now authoritative for bounded CS01-CS07 delivery.

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

### D1 — Operational definition and supported vehicles — APPROVED

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
Both observations must report horizontal accuracy between 0 and **100 m**. Calculate WGS84
haversine centre distance, then subtract both reported accuracies and clamp the result at zero.
The movement condition passes only when adjusted displacement is at most
50 m. Missing, negative, over-100 m or invalid accuracy makes the observation `UNKNOWN`; speed
alone cannot confirm stationary state.

### D2 — Signals, provenance and capability eligibility — APPROVED

Introduce separate canonical `ignitionState` and `engineRunningState` values, each
`ON/OFF/UNKNOWN` or `RUNNING/NOT_RUNNING/UNKNOWN` as appropriate. Preserve the current legacy
`engineState` field as ignition semantics for V1/V2 compatibility; never reinterpret stored rows.

`ENGINE_RUNNING` is supported only from an approved device-native engine/CAN/RPM/running field
whose provider mapping and real device capture are recorded. A derived Traccar computed attribute
is eligible only if its expression consumes that approved native engine fact—not voltage, speed,
motion or connectivity. Provenance must retain provider type, device binding, source time,
capability interval and canonical event identity without storing raw payloads or credentials.

### D3 — Freshness, ordering, gaps and unknown state — APPROVED

- Evaluate source-time order from immutable Timescale history, not Redis arrival order.
- Require trusted, non-suspect telemetry with `ENGINE_RUNNING=SUPPORTED` at source time.
- Maximum gap between qualifying observations: **2 minutes**.
- Missing, explicit `UNKNOWN`, stale, out-of-order-only or unsupported evidence yields
  `UNKNOWN/UNCONFIRMED`, never false, normal or zero idle.
- Late observations remain in history but cannot rewrite a closed episode, change credited duration
  or regress current state. Phase 1 has no reconciliation mutation.
- A gap stops credited duration at the last qualifying source timestamp and closes a confirmed
  episode with `EVIDENCE_GAP`; the gap duration is not counted.

### D4 — Confirmation and recovery — APPROVED

- Candidate requires at least two eligible observations.
- Confirm after **5 continuous minutes** of qualifying evidence.
- Episode start remains the first qualifying source timestamp, so confirmed duration includes the
  candidate interval.
- Credited duration is the sum of consecutive qualifying source-time deltas, each no greater than
  two minutes; it is never calculated as an unchecked wall-clock `end-start` span.
- Before confirmation, a non-qualifying or unknown observation discards the candidate without
  creating an episode. After confirmation, unknown evidence ends the episode at the last eligible
  source timestamp with `EVIDENCE_GAP` and does not add the unknown interval.
- Explicit `engineRunningState=NOT_RUNNING` closes immediately at that source timestamp.
- Movement recovery requires two eligible observations above 3.0 km/h or outside the stationary
  accuracy-adjusted radius, at least 30 seconds apart; close at the first recovery observation.
- A single noisy movement observation does not close an episode.
- Thresholds are fixed Phase-1 product values, not Tenant-editable rules.
- For hybrids, an engine-off/traction-ready condition is `NOT_RUNNING` and cannot be idle. Engine
  restarts begin a new candidate.

### D5 — Episode identity, retries and restart safety — APPROVED

Tracking owns one open idle episode per `(tenant_id, vehicle_id)`. PostgreSQL advisory locking is
Tenant/Vehicle-qualified. Immutable evidence uses the canonical dedupe identity; duplicate Kafka
delivery or polling replay cannot increment duration, evidence count or effects. The existing V91
dispatch gains `IDLE`; acknowledgement occurs only after episode/evidence persistence commits.
Expired claims recover through the existing bounded lease model, and Redis is never authoritative.
The source-time Vehicle association owns attribution. Reassignment closes a confirmed old-Vehicle
episode at the association boundary with `DEVICE_REASSIGNED`; candidate state is discarded and no
duration transfers. A change from `ENGINE_RUNNING=SUPPORTED` closes at its effective boundary with
`CAPABILITY_CHANGED` and publishes no effect.

For equal source timestamps, exact canonical dedupe identity is a replay and produces no effect.
Two different identities with identical timestamps and contradictory engine-running/movement facts
produce immutable `CONFLICTING_EVIDENCE`, advance neither candidate nor recovery and expose current
state as `UNKNOWN`. Deterministic event UUID order is used only for storage order, never to choose
which contradictory fact is true.

### D6 — Canonical compatibility — APPROVED

Create additive canonical telemetry **V3** on a new governed topic/DLT pair. V3 preserves V1/V2
identity, Tenant, Vehicle, Device, position, time and dedupe semantics; it adds separated ignition
and engine-running observations plus minimized provenance code. No producer dual-publishes a
single observation. V1/V2 consumers remain during the retention/rollback window; US-51 evaluates
only V3 `engineRunningState` with supported source-time capability.

Exact additive V3 fields are:

| Field | Type | Semantics |
| --- | --- | --- |
| `ignitionState` | nullable enum `ON`, `OFF`, `UNKNOWN` | Absent means not reported; never engine-running truth |
| `engineRunningState` | nullable enum `RUNNING`, `NOT_RUNNING`, `UNKNOWN` | Authoritative observation only when capability is supported |
| `engineRunningSource` | nullable enum `DEVICE_NATIVE_CAN`, `DEVICE_NATIVE_RPM`, `DEVICE_NATIVE_STATUS`, `PROVIDER_VERIFIED_DERIVATION` | Required when `engineRunningState` is present; no raw field/expression |

The topic is proposed as `tracking.telemetry.ingested.v3` with DLT
`tracking.telemetry.ingested.v3.dlt`, the existing Tenant/Vehicle Kafka key and the existing event
type. Absence is distinct from explicit `UNKNOWN`. `eventVersion` remains excluded from dedupe
identity. V3 producers publish exactly one version per observation. V1/V2 fields and topics are
immutable; their legacy `engineState` is treated as ignition-compatible history and is never
eligible for US-51.

### D7 — Persistence and migration boundaries — APPROVED

These numbers describe reviewable boundaries only; they are not reserved or authorized:

- **Proposed V101:** allow event version 3; add nullable immutable-history `ignition_state`,
  `engine_running_state` and constrained provenance code; extend capability vocabulary with
  `ENGINE_RUNNING`; preserve legacy `engine_state` unchanged.
- **Proposed V102:** add Tenant-leading idle current-state, episode and append-only evidence tables;
  one-open-episode uniqueness; dedupe constraints; source-time/keyset indexes; extend the V91
  evaluator constraint with `IDLE`. Reuse the existing V91 due-work and Vehicle/source-time indexes;
  no redundant dispatch index is permitted.
- **Proposed V103:** seed exactly `IDLE_MONITOR_VIEW` and `IDLE_EVENT_VIEW` and assign them to the
  existing same-Tenant Dispatcher/Admin role policy. No Notification catalogue entry is proposed.

No migration may create engine facts, backfill legacy ignition as engine-running, mutate immutable
history, or change foreign module tables.

Exact proposed objects and permitted changes:

- V101 alters only `tracking_position_history` version/check constraints and adds the three V3
  columns; it replaces only the capability-name check on
  `tracking_device_telemetry_capability`. No row is backfilled or rewritten.
- V102 creates `tracking_idle_state`, `tracking_idle_episode` and
  `tracking_idle_episode_evidence`; alters only
  `ck_tracking_telemetry_dispatch_evaluator`; creates one partial unique open-episode index,
  one Tenant/Vehicle episode keyset index, one Tenant/state current-state index and Tenant/episode
  evidence source-time/dedupe uniqueness. Tables contain only Tenant/Vehicle/Device UUIDs,
  canonical event/dedupe references, source timestamps, state/outcome, credited seconds,
  minimized speed/accuracy/adjusted-distance evidence, lifecycle/end reason, optimistic version and
  audit timestamps. Coordinates and raw provider facts are prohibited.
- V103 inserts only the two permissions and their exact existing `DISPATCHER` and `ADMIN` role
  grants using idempotent business keys. It creates no role, Notification rule/template or
  Operations catalogue entry.

### D8 — Notification and Operations — APPROVED

Phase 1 creates **no Notification and no Operations fact**. The authoritative story requires
monitoring, recording and history, not escalation. This avoids inventing severity, recipients or
operational policy. Future effects require a separate product decision and catalogue authorization.

### D9 — API, RBAC, audit and UI — APPROVED

- `IDLE_MONITOR_VIEW`: same-Tenant current state and availability.
- `IDLE_EVENT_VIEW`: bounded episode/evidence history and detail.
- Exact read-only, no-store routes:
  - `GET /api/v1/tracking/idle-monitoring/states?vehicleId=&state=&cursor=&limit=`;
  - `GET /api/v1/tracking/idle-monitoring/episodes?vehicleId=&from=&to=&endReason=&cursor=&limit=`;
  - `GET /api/v1/tracking/idle-monitoring/episodes/{episodeId}`;
  - `GET /api/v1/tracking/idle-monitoring/episodes/{episodeId}/evidence?cursor=&limit=`.
- `from` and `to` are mandatory for episode history, `from < to`, maximum range **31 days**.
  Default/max limit is **50/100**. Tenant-bound opaque keyset cursors order episodes by
  `(startSourceTimestamp DESC,id DESC)` and evidence by `(sourceTimestamp ASC,id ASC)`.
- Safe output: Vehicle label/ID, state, episode timestamps, confirmed idle duration, evidence
  quality/capability, optional estimate availability and source label. No coordinates, raw provider
  payload, device external reference, credentials, Driver/Customer PII or engine/CAN raw values.
- Audit protected history queries using the existing minimized read-audit convention; there is no
  review/acknowledgement mutation in Phase 1.
- Permission-aware responsive operator pages for current state, history and detail, explicitly
  rendering `UNKNOWN`, `UNSUPPORTED`, `STALE` and `EVIDENCE_GAP`.
- Safe errors use existing envelopes and proposed codes `IDLE_RANGE_INVALID`,
  `IDLE_CURSOR_INVALID`, `IDLE_EPISODE_NOT_FOUND` and `IDLE_CAPABILITY_UNAVAILABLE`; foreign-Tenant
  identities return the same safe absence as unknown identities.
- State and episode/evidence retain for **180 days**, aligned with raw Tracking history. Read-audit
  metadata retains the existing Audit-owned policy and stores only Tenant, actor, action, bounded
  filters, result count and correlation ID—never coordinates or engine/provider raw values.
- UI clears all cached state on authentication or Tenant change, never displays zero idle for
  unavailable/unknown evidence, and distinguishes `UNSUPPORTED`, `NOT_REPORTED`, `STALE`,
  `CONFLICTING_EVIDENCE`, `CANDIDATE`, `IDLE` and `NORMAL` with accessible text, not colour alone.

### D10 — Fuel estimate — APPROVED

Fuel waste remains `UNAVAILABLE` in Phase 1 because no approved effective-dated idle burn-rate
contract exists. Do not derive it from general litres/engine-hour performance or a global constant.
A later Fuel-published estimate must be labelled non-authoritative, versioned and effective-dated;
Tracking may store the applied rate/version snapshot but must not query Fuel persistence.

### D11 — Acceptance separation — APPROVED

Technical acceptance uses controlled canonical V3 events and a test-profile capability registry to prove logic, PostgreSQL/Kafka recovery,
Tenant isolation and UI behavior. Final acceptance additionally requires a supported physical
combustion/hybrid Vehicle or safe bench rig, device-native engine-running evidence, real provider
transport, safe idle/movement procedure, privacy review and authorized operator sign-off. Fixtures,
ignition-only capture and simulated messages cannot satisfy that field gate.

Technical closure may mark the software `TECHNICALLY_COMPLETE / PRODUCTION_SOURCE_ACTIVATION_PENDING`
when all gates pass with every production mapping disabled. Production activation requires a
separate mapping approval and real capture. Story completion still requires independent physical
acceptance and operator sign-off.

## D1-D11 decision matrix

| Decision | Recommended choice | Approval effect |
| --- | --- | --- |
| D1 | Combustion/hybrid only; native engine-running + speed ≤3 km/h + accuracy-adjusted displacement ≤50 m; accuracy required and ≤100 m | Freezes eligible Vehicle/signal/movement semantics |
| D2 | Separate ignition and engine-running; source-time `ENGINE_RUNNING` capability; production mappings disabled until verified | Allows software work without false production activation |
| D3 | Source-time ordered; 2-minute maximum gap; missing/stale/unsupported/conflicting evidence is unknown and uncredited | Freezes continuity and uncertainty |
| D4 | Two samples, five minutes; summed capped deltas; explicit engine stop immediate; movement needs two samples/30 seconds | Freezes confirmation/recovery/duration |
| D5 | One Tenant/Vehicle open episode; canonical dedupe; advisory lock; durable leased `IDLE` dispatch; reassignment/capability boundaries close safely | Freezes idempotency/restart behavior |
| D6 | Additive V3 with exact separated fields/topic/DLT; V1/V2 immutable and ineligible for idle | Authorizes compatible event evolution |
| D7 | Proposed V101 history/capability, V102 idle/dispatch, V103 permissions only | Authorizes exact forward schema boundaries if versions remain free |
| D8 | No Phase-1 Notification or Operations fact | Prevents invented escalation scope |
| D9 | Four exact read-only routes, two permissions, 31-day range, 50/100 page limits, no-store/privacy/audit/UI rules | Freezes public/read security surface |
| D10 | Fuel estimate `UNAVAILABLE`; no generic rate or cross-module persistence query | Prevents misleading waste claims |
| D11 | Technical fixtures allowed; production mapping activation and physical acceptance remain separate gates | Separates readiness from external evidence truthfully |

## Proposed bounded implementation change sets

| Change set | Scope | Dependencies | Completion criteria |
| --- | --- | --- | --- |
| CS01 canonical engine semantics | V3 contract, provider-neutral types and test-profile fixture producer; production mappings remain disabled | Approval of D1-D11; physical source not required | Contract/compatibility tests; no inference; V1/V2 unchanged; rollback disables V3 producer while dual consumers remain |
| `US-51-MONITOR-IDLE-TIME-CS02-V101-HISTORY-CAPABILITY-001` — COMPLETE | Immutable V3 storage and effective capability | CS01 and migration authorization | Clean V1→V101 and compressed V100→V101, exact constraints, append-only/Tenant, rollback and durable acknowledgement tests pass |
| `US-51-MONITOR-IDLE-TIME-CS03-V102-IDLE-PERSISTENCE-DISPATCH-001` — COMPLETE | State, episode, evidence, idempotency, lease/claim support | CS02 and migration authorization | Deterministic concurrency, replay, restart, gap/order tests; application rollback retains additive schema |
| `US-51-MONITOR-IDLE-TIME-CS04-EVALUATOR-001` | D1-D5 policy and estimate-unavailable result | CS03 | Threshold/recovery/property tests and retained detector regressions; feature flag disables claims |
| CS05 V103 APIs/RBAC/audit | Two permissions and exact read-only bounded endpoints | CS03-CS04 and permission authorization | Literal HTTP allow/deny, Tenant A/B, cursor/privacy/audit tests; routes can be disabled without data loss |
| CS06 frontend | State/history/detail pages | CS05 | Component, accessibility, session-clearing, TypeScript/build and Chromium; hide navigation on rollback |
| CS07 PostgreSQL/Kafka performance/recovery | Concurrency, leases, dedupe, query plans and bounded workload | CS01-CS06 | No deadlocks/duplicates/leaks; controlled measurements, not production SLOs; production capability still unavailable |
| Technical closure | Consolidated mandatory gates and evidence | CS01-CS07 | Full backend, architecture/static, frontend and Chromium gates pass |
| Final acceptance | Genuine engine-running field journey | Technical closure plus external prerequisites | Independent physical/provider/operator matrix passes |

## Consolidated implementation authorization request

Approve D1-D11 and authorize CS01-CS07 as separate governed commits, including additive canonical
V3 and proposed forward migrations V101-V103 **only if those versions remain free at each preflight**.
The first executable task will be newly identified as
`US-51-MONITOR-IDLE-TIME-CS01-CANONICAL-ENGINE-SEMANTICS-001`. It may implement V3 contracts,
dual-consumer compatibility and test-profile fixtures without a physical source. All production
provider/device `ENGINE_RUNNING` mappings must remain unavailable until separately verified and
approved; final acceptance remains blocked until genuine evidence and operator sign-off exist.
