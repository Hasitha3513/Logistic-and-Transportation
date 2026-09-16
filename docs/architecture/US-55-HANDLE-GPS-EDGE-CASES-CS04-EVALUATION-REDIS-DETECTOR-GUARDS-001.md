# US-55 CS04 — Evaluation, Redis and Detector Guards

**Status:** `COMPLETE`

**Story state:** `IMPLEMENTATION_IN_PROGRESS / CS04_COMPLETE`

**Flyway head:** `V97`

**Accounting:** `73 / 87 COMPLETE`

## Delivered contract

V97 transactionally creates Tracking-owned `tracking_gps_exception_episode` and
`tracking_gps_exception_evidence`. Episodes are Tenant/device/type scoped, use optimistic versions,
and allow only one active logical episode. Evidence is minimized, deterministic, Tenant-bound,
idempotent and database-enforced append-only. It stores no coordinates, provider payload,
credential, signature or personal data.

The Tracking evaluator now classifies canonical V1/V2 telemetry using effective-dated capability
facts, writes the episode transition and evidence in one transaction, rejects stale regression,
uses the frozen two-point recovery rule and reconstructs authoritative progress from PostgreSQL.
Unsupported optional V2 signals remain `UNKNOWN` and cannot create false incidents. A scheduled
freshness detector can open signal-loss evidence without fabricating a telemetry-history row.

The Kafka live projector evaluates reliability before applying the existing atomic Redis
compare-and-apply operation. Only trusted, in-order, eligible observations can advance live state.
Geofence, speed and route-deviation dispatch now require the same trusted, in-order, accurate,
non-null-island and fresh observation. Redis remains disposable and is not lifecycle authority.

No REST API, permission, event contract, frontend, provider onboarding or notification behavior
changed in CS04.

## Migration evidence

- Clean V1→V97: PASS.
- Explicit V96→V97: PASS.
- Forced transactional failure: PASS; Flyway remained V96 and no episode table remained.
- Normal retry and repeat startup at V97: PASS; one successful V97 history row.
- Exactly two authorized tables and three Tenant-leading operational indexes: PASS.
- Indexes ready and valid: PASS.
- Same-Tenant device/episode/evidence constraints: PASS.
- Active-episode uniqueness and deterministic evidence uniqueness: PASS.
- Evidence update/delete rejection: PASS.
- Acceptance database guard: `transport_logistics_acceptance` only.

## Verification evidence

- Focused evaluator/projector/dispatch: 16 tests, 0 failures, 0 errors.
- V97 PostgreSQL/Timescale migration and constraints: 3 tests, 0 failures, 0 errors.
- Architecture and Spring Modulith: 59 tests, 0 failures, 0 errors.
- Complete Maven `clean test`: 1,872 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS in 12:17.
- Checkstyle: PASS, 0 violations.
- PMD: PASS after removing all CS04-local findings.
- SpotBugs: PASS, 0 findings.
- Dependency analysis: PASS with the repository's existing aggregate dependency warnings; no dependency changed.
- Docker Compose validation: PASS.
- `git diff --check`: PASS.
- Frontend/Chromium: not applicable; CS04 changed no public API, frontend-consumed fixture or UI behavior.

The first unqualified complete-suite invocation attempted the default development port and failed
closed on connection refusal. It performed no destructive action and is excluded. All accepted
PostgreSQL evidence used only `transport_logistics_acceptance` on the isolated acceptance port.

## Security and residual risk

Every new persistence lookup, relationship and operational index is Tenant-qualified. Device and
episode composite foreign keys prevent cross-Tenant linking, and live-state keys retain Tenant
qualification. Evidence contains only hashes, classifications and minimized timestamps; it does not
contain secrets, exact coordinates, payloads or PII.

Physical provider fidelity remains outside this technical change set. Real loss, burst, tamper and
battery evidence is still required for final US-55 acceptance and cannot be inherited from another
story.

## Exact next queue

`US-55-HANDLE-GPS-EDGE-CASES-CS05-OPERATIONS-NOTIFICATION-INTEGRATION-001`
