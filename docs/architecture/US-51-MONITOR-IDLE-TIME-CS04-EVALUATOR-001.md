# US-51 Monitor Idle Time — CS04 Evaluator Closure

**Task:** `US-51-MONITOR-IDLE-TIME-CS04-EVALUATOR-001`

**Status:** `IMPLEMENTATION_IN_PROGRESS / CS04_COMPLETE`

**Flyway head:** V103
**Accounting:** 73/87 complete

## Implemented contract

The Tracking-owned evaluator consumes only canonical V3 history with trusted, in-order,
authoritative engine-running evidence. It queries the published Tenant/source-time Fleet
powertrain contract and the effective-dated `ENGINE_RUNNING` capability. Production Fleet
classification remains conservatively `UNKNOWN`, so this software readiness does not activate
production idle detection.

Eligible controlled evidence is limited to combustion/hybrid Vehicles, `RUNNING`, speed at or
below 3 km/h, horizontal accuracy at or below 100 m and WGS84 haversine displacement adjusted by
both observations' accuracy at or below 50 m. Confirmation requires two or more observations and
300 continuously credited seconds; a gap above 120 seconds discards an unconfirmed candidate.
Candidates remain restart-safe and create no episode until atomic promotion.

Confirmed episodes close immediately on authoritative engine stop. Movement requires two
non-qualifying observations at least 30 seconds apart and closes at the first movement time.
Capability loss and device reassignment close conservatively. Late/out-of-order evidence cannot
regress state; equal-source-time contradictory identities create minimized conflicting evidence
without UUID-based truth selection. Duplicate identities remain idempotent.

## Durable dispatch

The existing leased dispatch worker now claims `IDLE` work only because a functioning evaluator is
registered. History identity is revalidated before evaluation; evaluator effects commit before the
lease is completed. Failed effects are retried with bounded backoff, expired leases remain
recoverable, and stale owners cannot complete another worker's claim. IDLE replay applies its own
source-time continuity rules instead of the unrelated five-minute live-detector guard.

Existing GEOFENCE, SPEED and ROUTE_DEVIATION dispatch behavior is unchanged. V1/V2 history remains
ineligible for idle evaluation.

## Verification

Verification used Java 21 and isolated Docker-backed PostgreSQL/TimescaleDB with the database name
guarded as `transport_logistics_acceptance`. Focused evaluator, persistence, migration and dispatch
tests cover threshold/gap confirmation, powertrain UNKNOWN, device reassignment, V103 candidate
promotion, append-only evidence, concurrent claims, lease expiry and existing evaluator behavior.

- final evaluator/worker/candidate/dispatch selection: 17/17 PASS;
- architecture/Modulith: 59/59 PASS;
- complete Maven: 1,941/1,941 PASS, zero failures/errors/skips;
- Checkstyle: zero violations;
- PMD: BUILD SUCCESS, no current finding;
- SpotBugs: zero findings after correcting the candidate-count null guard and dispatch SQL formatting;
- dependency analysis: BUILD SUCCESS with the repository's existing declared/used warnings;
- Docker Compose validation and `git diff --check`: PASS.

No frontend or public API changed, so frontend and Chromium gates are not applicable to CS04.

## Restrictions and next work

No migration was added; V104 remains reserved for the approved CS05 permissions. Production
Flespi, Traccar and Generic engine-running mappings remain disabled. Production powertrain remains
`UNKNOWN`; a future authoritative effective-dated Fleet classification source is still required.
Controlled fixtures are technical evidence only and do not satisfy physical acceptance.

Exact next approved roadmap label: `CS05 V104 APIs/RBAC/audit`.
