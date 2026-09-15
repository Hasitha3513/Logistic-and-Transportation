# US-53 Replay Journeys CS06 — Incident Overlay Evidence

## Verdict

`PASS — IMPLEMENTATION_IN_PROGRESS / CS06_COMPLETE`

CS06 activates the frozen, privacy-minimized US-49/US-50/US-52 incident overlays through Tracking-owned
query adapters. Story accounting remains 73/87 and Flyway remains V93.

## Delivered contract

- Same-Tenant geofence transitions are available by default and labelled `ACCEPTED`.
- Speed episodes are opt-in and labelled `FIELD_FIDELITY_PENDING`.
- Route-deviation episodes and reviews are opt-in and labelled `FIELD_ACCEPTANCE_PENDING`.
- Results are chronological, bounded, and paged with the existing Tenant/query-bound opaque cursor.
- Trip selection resolves through the published Trip replay scope; no foreign persistence access was added.
- Responses expose only evidence identity, producer/type, source-time bounds, severity/status and nullable
  Trip/route revision context. Coordinates, geometry, raw telemetry, review notes, provider/device facts,
  credentials, signatures and PII remain prohibited.
- The operator UI defaults to geofence evidence, keeps technical-only warnings visible for opt-in producers,
  provides independent loading/error/retry behavior, and hides overlays without the exact permission.

## Independent baseline remediation

The first complete Maven run exposed one independent stale local-bootstrap defect: V93 granted the two replay
permissions to `LOCAL_MVP_ADMIN`, but `LocalIdentityBootstrap` subsequently replaced that role's permission
set without those keys. The minimal correction adds exactly `JOURNEY_REPLAY_VIEW` and
`JOURNEY_REPLAY_INCIDENT_VIEW` to the existing opt-in bootstrap set. The isolated failing test then passed;
no new permission, role, migration or production authorization policy was introduced.

## Verification

All PostgreSQL-backed evidence used only `transport_logistics_acceptance`.

| Gate | Result |
| --- | --- |
| Focused incident/domain/Timescale | 25/25 PASS |
| Focused API/security | 43/43 PASS |
| Isolated identity remediation | 1/1 PASS |
| Architecture/Modulith | 59/59 PASS |
| Complete Maven after remediation | 1,811/1,811 PASS; 0 failures, 0 errors, 0 skipped; BUILD SUCCESS (11:19) |
| Complete Vitest | 325/325 PASS |
| TypeScript and production build | PASS; existing bundle-size advisory only |
| Changed-file ESLint | PASS |
| Real PostgreSQL/Timescale Chromium | 6/6 PASS |
| Checkstyle | PASS, 0 violations |
| PMD | No unsuppressed CS06 findings; direct global goal retains 32 unrelated legacy wildcard-import findings |
| SpotBugs | PASS |
| Dependency analysis | PASS with the repository's existing advisory list |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

The first complete Maven run executed 1,811 tests and found only the local-bootstrap parity defect above.
After its focused repair, the required complete rerun passed. The first Chromium invocation was rejected by
the fixture guard before any scenario ran because `PGDATABASE`/`PGUSER` were absent; the authoritative fresh
rerun explicitly bound both fixture and application to `transport_logistics_acceptance` and passed 6/6.

## Residual scope and next queue

Physical US-48, US-50 and US-52 acceptance holds remain independent. CS06 does not infer idle/engine evidence,
change producer semantics, add a projection/schema, or change story accounting.

Exact next queue: `US-53-REPLAY-JOURNEYS-CS07-POSTGRES-PERFORMANCE-OPERATIONS-001`.
