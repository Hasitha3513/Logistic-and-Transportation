# US-48 External Acceptance Hold and Wave C Dependency Disposition

**Task:** `US-48-EXTERNAL-ACCEPTANCE-HOLD-AND-DOWNSTREAM-DEPENDENCY-DISPOSITION-001`  
**Decision:** APPROVED  
**Date:** 2026-09-10  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`  
**Accounting:** 72 / 87 complete; 15 / 87 remaining  
**Flyway:** V76; no V77

## Decision A — external acceptance hold

US-48 physical external acceptance is `ON_HOLD_EXTERNAL_PREREQUISITE`. Two readiness attempts reached
the same hard blocker: no physical Teltonika FMC130 and no genuine FMC130-originated Flespi message were
available or verifiable. No production defect or product-decision gap was identified. Repeating readiness
cannot create the missing external system and must stop until a material external fact changes.

The restart trigger is the availability of the physical FMC130 with safe power, LTE/SIM/APN and GNSS,
plus verified real Flespi Teltonika channel/device access and genuine device-originated telemetry. Once
that trigger is satisfied, the required sequence remains readiness rerun, real capture, then independent
US-48 final acceptance. No `READINESS-002`, `READINESS-003` or equivalent task may be created for unchanged
hardware absence.

This hold is not completion, acceptance, waiver or deferred completion. Physical-provider evidence remains
mandatory. US-48 stays `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`; accounting stays
72/87.

## Decision B — downstream dependency classification

The source requirements deliberately separate US-48's normalized current-location responsibility from
US-49–55 interpretation and presentation. CS01–CS10 technically prove Tenant-scoped normalized positions,
source and receipt timestamps, WGS84 coordinates, optional quality facts, effective-dated Vehicle
association, immutable history, dedupe/order/trust, latest-received/latest-trusted projections, freshness,
connectivity and provider-neutral ingestion. Those contracts are stable implementation inputs; they do not
prove physical FMC130/Flespi fidelity.

| Story | Dependency classification | Disposition | Reason and final-evidence boundary |
| :--- | :--- | :--- | :--- |
| US-49 Manage Geofences | Technical Tracking contract | `TECHNICAL_DEPENDENCY_SATISFIED / READY_FOR_PRODUCT_DECISIONS` | Needs Vehicle, trusted WGS84 position and source time to evaluate configured boundaries. These contracts are technically accepted. US-49 acceptance must prove its own geometry, transition, Tenant and alert behavior; it does not inherit US-48 physical acceptance. |
| US-50 Monitor Speed | Technical Tracking contract for implementation; real field fidelity for final acceptance | `TECHNICAL_DEPENDENCY_SATISFIED / READY_FOR_PRODUCT_DECISIONS` | Normalized optional `speedKph`, Vehicle, source time, trust and quality exist and FLESPI advertises SPEED. Implementation may proceed provider-neutrally. Final acceptance must use a source with verified speed fidelity or remain externally blocked; missing speed is UNKNOWN, never zero. |
| US-51 Monitor Idle Time | Required telemetry capability | `BLOCKED_BY_REQUIRED_TELEMETRY_CAPABILITY` | Correct idling requires engine-on truth plus movement. Current FLESPI does not advertise IGNITION and no other accepted engine-state source is identified. Missing telemetry cannot be treated as zero idle. |
| US-52 Monitor Route Deviations | Technical Tracking plus accepted Routing contracts | `TECHNICAL_DEPENDENCY_SATISFIED / READY_FOR_PRODUCT_DECISIONS` | Trusted actual Vehicle position/source time and existing planned-route contracts are sufficient for implementation. US-52 owns severity and audited approval and must prove them independently. |
| US-53 Replay Journeys | Technical immutable-history contract | `TECHNICAL_DEPENDENCY_SATISFIED / READY_FOR_PRODUCT_DECISIONS_AFTER_EARLIER_WAVE_C_DECISIONS` | Immutable Vehicle history, source time, association, retention and access contracts are technically stable. Core replay need not wait for FMC130 acceptance, but optional US-49–52 overlays must consume accepted producer outputs and must not be recreated. |
| US-54 View Tracking Dashboard | Producer-story outputs | `BLOCKED_BY_US49_TO_US53_PRODUCERS` | The requirements make US-54 a consolidated consumer of US-48–53 states/events. It must not implement geofence, speed, idle, deviation or replay detection behind the dashboard. |
| US-55 Handle GPS Edge Cases | Mixed technical and unprovided telemetry/provider evidence | `BLOCKED_BY_REQUIRED_TELEMETRY_AND_PRODUCT_DECISIONS` | Signal loss, stale state and delayed-packet ordering have technical US-48 support, but the full story also requires spoofing, tampering and battery-drain identification. Current FLESPI capabilities and normalized model do not establish those signals. Partial support cannot be called story readiness. |

## Contract and sequencing guardrails

- Wave C work may consume only the frozen Tracking use cases/models or an explicitly approved minimized
  state-change event; it may not query Tracking tables from another module or activate per-packet events.
- `VehicleTrackingStateChangedV1` remains inactive until a concrete consumer and exact payload are approved.
- US-49/50/52/53 implementation and acceptance remain independent of US-48 completion accounting.
- US-51 and full US-55 require separate product/telemetry decisions before implementation.
- US-54 follows the required producers. Physical US-48 acceptance continues in parallel only when the
  external restart trigger occurs.

## Next task

The earliest source-ordered story whose implementation dependency is satisfied is:

`US-49-MANAGE-GEOFENCES-PRODUCT-DECISIONS-001`

Starting product decisions for US-49 does not change US-48's status or waive physical evidence. This
governance task changes no production code, API, permission, event, database schema, migration or story
count.

