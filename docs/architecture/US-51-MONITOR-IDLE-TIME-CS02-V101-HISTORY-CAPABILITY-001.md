# US-51 CS02 — V101 History and Capability Closure

## Verdict

`COMPLETE`. V101 is the verified Flyway head. Canonical V3 observations can be published to the
V3 topic, consumed durably, and retained losslessly in Tracking-owned Timescale history. No
production provider can advertise or emit authoritative `ENGINE_RUNNING` evidence.

## Implemented boundary

- V101 accepts history event versions 1, 2 and 3 without reinterpreting legacy `engine_state`.
- `tracking_position_history` adds nullable `ignition_state`, `engine_running_state` and
  `engine_running_source`, with enum, pair, version and ignition-consistency constraints.
- V3 retains the V2 optional signal vocabulary; V1 continues to require those fields to be null.
- `tracking_device_telemetry_capability` accepts effective-dated `ENGINE_RUNNING`; absence and
  foreign-Tenant lookup resolve conservatively to `UNKNOWN`. V101 seeds no capability rows.
- The V3 history consumer acknowledges only after the complete bounded batch commits. Failed
  writes remain unacknowledged for Kafka retry; canonical Tenant-qualified identity preserves
  cross-version deduplication.
- The live projector accepts V3 shared position facts, but Redis remains disposable and does not
  own the authoritative engine-running evidence.
- V1/V2 topics and consumers remain unchanged. Flespi, Traccar and Generic production mappings
  remain V2-only; fixture V3 construction remains test-only.

## Verification evidence

Environment: Java 21.0.12, Maven 3.9.9, Docker 29.7.2, PostgreSQL/Timescale Testcontainers named
`transport_logistics_acceptance`.

- Focused V1/V2/V3, consumer, migration, history and capability suite: 34 tests, 0 failures,
  0 errors, 0 skipped.
- V101 migration: clean V1→V101 and compressed-history V100→V101 pass; legacy records preserved;
  V101 recorded once.
- Persistence: V3 round-trip, constraints, append-only behavior, Tenant/effective-time boundaries,
  rollback, deduplication and acknowledgement ordering pass.
- Architecture/Modulith: 59 tests, 0 failures/errors/skips.
- Complete backend: 1,924 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS` in 13:07.
- Checkstyle: 0 violations. PMD: pass. SpotBugs: 0 findings. Dependency analysis: pass with the
  repository's existing declared/used dependency warnings.

## Limitations and next boundary

CS02 does not implement idle evaluation, idle state/episodes/evidence, REST/RBAC/UI, notification,
Operations, fuel estimation, or production engine-running source activation. Physical acceptance
still requires genuine approved device-native evidence and operator sign-off.

Next: `US-51-MONITOR-IDLE-TIME-CS03-V102-IDLE-PERSISTENCE-DISPATCH-001`.
