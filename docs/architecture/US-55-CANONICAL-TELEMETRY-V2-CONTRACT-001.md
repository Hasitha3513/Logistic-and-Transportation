# US-55 Canonical Telemetry V2 Contract

**Decision:** `US-55-CANONICAL-TELEMETRY-V2-CONTRACT-001`

**Status:** APPROVED / FROZEN
**Flyway:** V95; no database migration

## Versioning and rollout

V1 is immutable on `tracking.telemetry.ingested.v1` with DLT
`tracking.telemetry.ingested.v1.dlt`. V2 is additive on
`tracking.telemetry.ingested.v2` with DLT `tracking.telemetry.ingested.v2.dlt`.
Both topic and envelope version are validated. Each inbound message publishes exactly one version;
dual publication is prohibited. Consumers retain V1 and V2 listeners under their existing logical groups and map
both records to `CanonicalTelemetryEvent`. V1 retirement requires no active producer, no backlog, elapsed retention
and a closed rollback window.

V2 preserves the V1 event type, key, Tenant, Vehicle, Device, source/received time, position and dedupe semantics.
`eventVersion` is not part of the dedupe identity. The Timescale uniqueness and Redis ordering rules therefore
prevent cross-version duplication or regression.

## Additive observations

All fields are optional. Absence means `NOT_REPORTED_IN_THIS_EVENT`, never false, clear, zero, disconnected,
unsupported or failed.

| Field | Type and validation |
| --- | --- |
| `tamperState` | `DETECTED`, `CLEAR`, `UNKNOWN` |
| `batteryLevelPercent` | decimal 0..100, maximum scale 3; zero valid |
| `batteryVoltageVolts` | decimal 0..1000, maximum scale 6; zero valid when reported |
| `externalPowerState` | `CONNECTED`, `DISCONNECTED`, `UNKNOWN` |
| `batteryChargingState` | `CHARGING`, `NOT_CHARGING`, `UNKNOWN` |

Invalid ranges, precision and enum values fail V2 validation and follow the V2 DLT path. Optional-signal failure
never rewrites otherwise immutable V1 history. No raw provider value enters the canonical event.

## Provider mappings

| Adapter | Source | Unit/type | Canonical target | Missing / invalid behavior |
| --- | --- | --- | --- | --- |
| Flespi | `device.tampering.status` | normalized text/boolean text | `tamperState` | absent / `UNKNOWN` |
| Flespi | `battery.level` | percent decimal | `batteryLevelPercent` | absent / reject range or scale |
| Flespi | `battery.voltage` | volts decimal | `batteryVoltageVolts` | absent / reject range or scale |
| Flespi | `external.power.status` | normalized text/boolean text | `externalPowerState` | absent / `UNKNOWN` |
| Flespi | `battery.charging.status` | normalized text/boolean text | `batteryChargingState` | absent / `UNKNOWN` |
| Traccar | `position.attributes.alarm` = `tampering` | text | `tamperState=DETECTED` | absent / other value `UNKNOWN` |
| Traccar | `position.attributes.batteryLevel` | percent decimal | `batteryLevelPercent` | absent / reject range or scale |
| Traccar | `position.attributes.battery` | volts decimal | `batteryVoltageVolts` | absent / reject range or scale |
| Traccar | `position.attributes.power` | boolean | `externalPowerState` | absent / non-boolean `UNKNOWN` |
| Traccar | `position.attributes.charge` | boolean | `batteryChargingState` | absent / non-boolean `UNKNOWN` |

The Generic adapter does not accept these claims until a mapping is explicitly registered. No voltage-to-percent,
silence, GPS-loss, movement or coordinate-based inference is performed.

## Capability resolution

`TelemetryCapabilityLookupPort` is Tenant-qualified and effective at source time. It resolves `POSITION`, `SPEED`,
`ACCURACY`, `HEADING`, `IGNITION`, `TAMPER`, `BATTERY_LEVEL`, `BATTERY_VOLTAGE`, `EXTERNAL_POWER` and
`BATTERY_CHARGING` to `SUPPORTED`, `UNSUPPORTED` or `UNKNOWN`. CS02 defines the framework-neutral contract only;
persistence is reserved for the separately authorized US-55 persistence slice. Consumers never call providers
synchronously while interpreting Kafka records.

## Security, rollback and examples

V2 excludes raw payloads, credentials, signatures, tokens, unrestricted external identifiers, arbitrary attributes,
capability snapshots and person/customer data. DLT diagnostics remain minimized and metrics remain low-cardinality.
Rollback moves producers back to V1 while the dual consumers remain deployed; it never republishes or rewrites
history.

Canonical examples are covered by contract tests for: V1 without V2 fields; V2 with no observations; Flespi and
Traccar tamper/battery facts; zero percentage; `CLEAR`; explicit `UNKNOWN`; invalid percentage/voltage/enum;
topic-version mismatch; and cross-version identity reuse. Test identifiers and coordinates are fictional.
