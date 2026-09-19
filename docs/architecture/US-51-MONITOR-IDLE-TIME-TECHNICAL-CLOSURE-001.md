# US-51 Monitor Idle Time — Technical Closure

**Task:** `US-51-MONITOR-IDLE-TIME-TECHNICAL-CLOSURE-001`
**Verdict:** PASS — `TECHNICALLY_COMPLETE / PRODUCTION_SOURCE_ACTIVATION_PENDING`
**Tested application commit:** `f3c59e3f9c02d9d537ff6e05678d3a399880b240`
**Flyway head:** V105
**Accounting:** 73 / 87 COMPLETE; 14 remaining

## Acceptance boundary

This report closes the software implementation boundary permitted by D11. Controlled V3 and
powertrain fixtures prove technical behavior only. Production activation remains pending because
Fleet has no authoritative effective-dated powertrain classification and no production provider or
device has an approved native engine-running mapping. Physical acceptance separately requires
genuine provider/device evidence, authorized Tenant/Vehicle binding, a safe field procedure,
privacy review and operator sign-off.

## Requirement-to-evidence traceability

| Approved requirement | Implementation / migration | Verification and tested commit | Remaining prerequisite |
| --- | --- | --- | --- |
| Canonical V3 separates ignition and engine-running while preserving V1/V2 | `TrackingTelemetryIngestedV3`, governed V3 topic/DLT and validators; V101 accepts/stores V3 without rewriting legacy `engine_state` | CS01 `2990587`; closure: contract, validator, consumer and activation-gate tests at `f3c59e3` | Production source mapping remains disabled |
| Durable history and source-time capability | Timescale history persists nullable ignition/running/source; effective-dated `ENGINE_RUNNING` capability; V101 | CS02 `6755031`; closure Timescale acceptance at `f3c59e3` | Real device capability interval and native capture |
| Conservative Fleet eligibility | Published Tenant/Vehicle/source-time query; production adapter returns `UNKNOWN` | CS04 `fa2ad36`; closure eligibility and evaluator tests at `f3c59e3` | Authoritative effective-dated Fleet data/workflow is absent and not invented |
| Restart-safe candidate without premature episode | V103 candidate identity/reference/recovery state and append-only candidate evidence | Prerequisite `9d2e424`; populated upgrade, restart, discard and retention tests retained; closure rerun at `f3c59e3` | None for software; production still ineligible |
| Discard without episode; immutable evidence; atomic promotion | `JdbcIdleCandidatePersistenceAdapter`; stable candidate identity; 180-day retention | CS04 PostgreSQL tests and closure rerun at `f3c59e3` | None for software |
| Frozen thresholds and continuity | `IdleEvaluationService`: 3 km/h, ≤100 m accuracy, adjusted ≤50 m, two samples, 300 credited seconds, ≤120-second gaps | CS04 `fa2ad36`; boundary, hybrid, unknown, conflict, ordering, reassignment and recovery tests rerun at `f3c59e3` | Genuine native engine evidence |
| Contradiction, late data, engine-stop and movement recovery | Source-time state machine; conflicting equal timestamps remain unknown; immediate engine-stop; two-point/30-second movement recovery | CS04 deterministic unit and PostgreSQL evidence; closure rerun at `f3c59e3` | Physical field verification |
| Durable dispatch and idempotency | V102/V103 IDLE dispatch, Tenant/Vehicle locking, leased claims and committed effects before completion | CS03 `ebcd73b`, CS04 `fa2ad36`; closure concurrency/worker tests at `f3c59e3` | None for software |
| Rollback, restart and stale-owner protection | Transactional persistence, retry identity, lease expiry/reclaim and owner-qualified completion | CS07 focused recovery evidence plus closure dispatch tests at `f3c59e3` | None for software |
| Read-only API and exact permissions | Four no-store routes; `IDLE_MONITOR_VIEW`, `IDLE_EVENT_VIEW`; V104 | CS05 `7ebf25c`; literal `/api/v1/...`, use-case, Tenant A/B, range/cursor and minimized audit tests rerun at `f3c59e3` | None for software |
| Privacy, pagination and audit | Tenant/filter-bound HMAC cursor, 31-day range, 50/100 limits, safe absence and minimized output/audit | CS05 tests and closure query/security selection at `f3c59e3` | Operator privacy review remains a field-acceptance input |
| Frontend workflow and session isolation | Permission-aware state/history/detail/evidence UI, explicit unavailable states and session-qualified query cache | CS06 `d4fa861`; Vitest 346/346 and real Chromium 4/4 retained at final implementation commit | Genuine field data remains unavailable |
| PostgreSQL plans and measured behavior | Exactly three Tenant-aligned state, episode and IDLE-dispatch indexes; V105 | CS07 `f3c59e3`: clean/upgrade/validity/plans, focused 26/26, local API p95 and recovery evidence | Maintenance-window write blocking; local figures are not production SLOs |
| No Phase-1 effects or estimate | No US-51 Notification, Operations publication or Fuel estimate; API/UI report fuel estimate unavailable | Source inspection and retained CS04-CS06 tests at `f3c59e3` | Any future effect/rate requires separate product authority |

## End-to-end software path

The final 53-test selection exercised the governed chain at the final commit:

`canonical V3 Kafka contract/consumer → durable Timescale history and IDLE dispatch → leased evaluator → restart-safe candidate/confirmed episode and immutable evidence → Tenant-authorized API`.

The real PostgreSQL-backed Chromium suite retained from CS07 exercises the authorized API through
the frontend. Controlled classification fixtures can qualify combustion/hybrid cases, while the
production Fleet adapter returns `UNKNOWN` and the same evaluator creates no candidate or episode.
Fixture implementations live in test scope; no production configuration can select them.

## Final verification

Fresh at `f3c59e3`:

- Cross-layer US-51 closure selection: **53 tests, 0 failures, 0 errors, 0 skipped**.
- Clean Flyway application through V105 occurred inside isolated acceptance containers.
- `VehicleMasterConfig.java` remained byte-identical to `HEAD`; its pre-existing
  assume-unchanged flag remains disclosed.

Reused from CS07 at the same tested commit:

- Complete Maven: **1,952/1,952 PASS**.
- Architecture/Modulith: **59/59 PASS**.
- Checkstyle, PMD and SpotBugs: PASS; dependency analysis completed with existing warnings.
- Vitest: **346/346 PASS**; TypeScript, production build and changed-file ESLint PASS.
- Real PostgreSQL-backed Chromium idle journey: **4/4 PASS**.
- Docker Compose validation and `git diff --check`: PASS.
- V105 state/episode/dispatch plan improvement and local API p95 evidence remain applicable.

Known non-blocking warnings: the existing MapStruct unmapped `TripMapper.tenantId` compiler warning,
deprecated/unchecked test-source notices, Flyway's PostgreSQL 16 support recommendation, Redis
repository-identification startup noise, local absent-broker connection noise in Spring context
tests, and the existing frontend bundle-size warning. None altered US-51 results.

## Explicit exclusions and next governance state

No new code, migration, API, permission, dependency, event, Notification, Operations fact, fuel
estimate or production mapping was introduced by technical closure. Flyway remains V105 and story
accounting remains 73/87.

There is no existing separately authorized US-51 activation or final-acceptance task identifier in
the authoritative roadmaps. Therefore no identifier is fabricated. The next US-51 action is a
product-governed production-source activation decision after authoritative Fleet classification
and verified device-native engine-running evidence exist; physical acceptance follows activation.
