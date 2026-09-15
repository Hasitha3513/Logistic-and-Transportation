# US-53 Replay Journeys CS04 — API, RBAC and Audit Evidence

## Verdict

`PASS` — the bounded replay point, stop and incident query boundary is implemented under the literal
`/api/v1/tracking/journey-replays` family. US-53 remains implementation-in-progress; CS04 does not constitute
story acceptance.

## Implemented contract

- Semantically read-only `POST /points/query`, `POST /stops/query` and `POST /incidents/query` routes.
- Server-derived Tenant and actor context; Vehicle/Trip selectors remain body-only.
- `JOURNEY_REPLAY_VIEW` protects points and stops at HTTP and use-case boundaries.
- Incidents require both `JOURNEY_REPLAY_VIEW` and `JOURNEY_REPLAY_INCIDENT_VIEW` at both boundaries.
- V93 seeds exactly those two permissions and conditionally grants existing `ADMIN`, `LOCAL_MVP_ADMIN` and
  `DISPATCHER` roles; it creates no role.
- Responses and replay errors set `Cache-Control: no-store` and `Referrer-Policy: no-referrer`.
- Successful initial queries write Tenant/actor-qualified audit evidence with a SHA-256 selector identity,
  duration, result count, coverage and overlay types. Cursor contents, coordinates, provider/device facts,
  credentials and PII are excluded. Existing denied-request audit coverage includes the replay POST routes.
- Incident execution remains truthfully unavailable until governed CS06 producer adapters are implemented.

## Verification

- Focused replay/security: 38 tests, 0 failures, 0 errors.
- V93 PostgreSQL acceptance: 1 test, 0 failures, 0 errors; clean V1→V93 and one V93 history row.
- Timescale replay acceptance: 7 tests, 0 failures, 0 errors.
- Corrected current-head/Kafka/Timescale regression group: 44 tests, 0 failures, 0 errors.
- Architecture and Spring Modulith: 59 tests, 0 failures, 0 errors.
- Complete Maven: 1,807 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS` in 10:53.
- Checkstyle: 0 violations. PMD, SpotBugs and dependency analysis: `BUILD SUCCESS`.
- Frontend continuity: TypeScript PASS; Vitest 319/319; production build PASS.
- Repository-wide ESLint: 71 pre-existing errors in unchanged Delivery files. Changed-file frontend lint is
  not applicable because CS04 changes no frontend file.
- Docker Compose validation and `git diff --check`: PASS.

## Security and residual scope

Literal `/api/v1/...` authorization is regression-tested. `TRACKING_VIEW` and `TRACKING_HISTORY_VIEW` confer
neither replay permission. CS05 owns the operator frontend and CS06 owns eligible producer incident adapters;
physical/provider evidence remains a final-acceptance concern and is not inherited.

## Next governed queue

`US-53-REPLAY-JOURNEYS-CS05-FRONTEND-001`
