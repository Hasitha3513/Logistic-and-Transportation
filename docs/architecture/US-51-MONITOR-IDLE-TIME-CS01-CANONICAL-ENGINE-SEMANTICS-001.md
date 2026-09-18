# US-51 Monitor Idle Time — CS01 Canonical Engine Semantics

**Task:** `US-51-MONITOR-IDLE-TIME-CS01-CANONICAL-ENGINE-SEMANTICS-001`

**Status:** IMPLEMENTED / TECHNICALLY VERIFIED

**Decision authority:** D1-D11 approved 2026-09-18

**Flyway:** V100 unchanged

**Accounting:** 73/87 COMPLETE

## Implemented boundary

CS01 adds additive canonical `TrackingTelemetryIngestedV3` while leaving every production producer
and durable consumer on V1/V2. V3 retains the existing event type, Tenant/Vehicle partition key,
event identity and canonical deduplication identity. It adds nullable `ignitionState`, nullable
`engineRunningState`, and nullable `engineRunningSource` with the exact approved vocabularies.

Absence means not reported. A reported engine-running state, including explicit `UNKNOWN`, requires
a source. A source without a state is invalid. When both V3 ignition and retained legacy
`engineState` are present, they must agree because legacy `engineState` remains ignition semantics.
Nothing in CS01 infers running state from ignition, speed, movement, voltage, charging or
connectivity.

The governed names are `tracking.telemetry.ingested.v3` and
`tracking.telemetry.ingested.v3.dlt`. They are configuration/contract names only in CS01. Topic
creation, producer routing and consumers are deliberately not activated. The production publisher
rejects V3 before contacting Kafka, so an observation cannot be acknowledged while V100 history
cannot durably store it. V1/V2 publication and consumers are unchanged.

## Production-source and fixture isolation

Flespi, Traccar and Generic retain their existing capabilities and V2 mappings. Neither
`ProviderCapability` nor effective `TelemetrySignalCapability` advertises `ENGINE_RUNNING` at
V100. Controlled test fixtures construct V3 events in test sources only; no fixture bean or source
mapping is included in the production artifact.

`PROVIDER_VERIFIED_DERIVATION` records provenance only. It does not authorize a provider or device
mapping, and it cannot make ignition or another proxy signal authoritative.

## Verification scope

- V3 JSON round-trip and exact enums.
- Missing versus explicit unknown evidence and state/source consistency.
- Legacy ignition consistency and V1/V2 unchanged compatibility.
- Governed topic/version/key/header/Tenant discrimination.
- Stable canonical retry identity and distinct-observation identity.
- Explicit production publication rejection before any Kafka interaction.
- No production `ENGINE_RUNNING` provider/effective capability activation.
- Retained provider, ingestion, V2 consumer, architecture and full-backend regression gates.

## Verification results

- Focused canonical/provider regression: 39 tests, 0 failures, 0 errors, 0 skipped.
- Architecture and Spring Modulith: 59 tests, 0 failures, 0 errors, 0 skipped.
- Complete backend against `transport_logistics_acceptance`: 1,916 tests, 0 failures, 0 errors,
  0 skipped; `BUILD SUCCESS` in 13:04.
- Flyway during the accepted full run: all 100 migrations validated; schema current at V100.
- Checkstyle: 0 violations. PMD: pass. SpotBugs: 0 findings.
- Dependency analysis: build success with the repository's existing transitive-dependency warnings;
  CS01 adds no dependency.
- `git diff --check`: pass.

An initial full-suite invocation used the default unavailable port 5432 and produced one connection
refusal plus downstream ApplicationContext cascades. It did not connect to or mutate the development
database. The authoritative rerun explicitly used port 5433 and database
`transport_logistics_acceptance`.

## Remaining activation prerequisites

CS02 must implement V101 immutable V3 history and effective-dated `ENGINE_RUNNING` capability,
then add V3 consumers and explicitly enable producer routing only when durable processing is ready.
Production source activation remains separate and requires verified device/protocol semantics plus
genuine capture. Final US-51 acceptance still requires the frozen physical journey and operator
sign-off.

**Exact next task:** `US-51-MONITOR-IDLE-TIME-CS02-V101-HISTORY-CAPABILITY-001`
