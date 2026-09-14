# US-52 CS04 Route-Deviation APIs, RBAC and Audit

## Result

`PASS` — US-52 is `IMPLEMENTATION_IN_PROGRESS / CS04_COMPLETE`. Story accounting remains
73 / 87 complete with 14 stories remaining. Flyway advances from V88 to V89 only to seed the four
approved route-deviation permissions. The next governed slice is
`US-52-MONITOR-ROUTE-DEVIATIONS-CS05-NOTIFICATION-INTEGRATION-001`.

## API and ownership

Tracking exposes the approved family at `/api/v1/tracking/route-deviations`:

- `POST /rules`, `GET /rules`, `GET /rules/{ruleId}`, `PUT /rules/{ruleId}`;
- `POST /rules/{ruleId}/activate`, `/disable`, and `/retire`;
- `GET /states` and `GET /states/{vehicleId}`;
- `GET /episodes`, `GET /episodes/{episodeId}`, and
  `GET /episodes/{episodeId}/reviews`;
- `POST /episodes/{episodeId}/approve`, `/reject`, and `/correct-review`.

Dedicated validated request/response DTOs and a web mapper prevent domain or persistence types from
leaking. Tenant and actor are always derived from `CurrentTenant`; client-supplied identity fields
are neither accepted nor serialized. Responses omit coordinates, raw telemetry, provider facts,
secrets and personal data. Precise tracking history remains governed separately by
`TRACKING_HISTORY_VIEW`.

Rules use offset pages with a default size of 20 and maximum 100. State pages use the same bounds.
Episode searches require a source-time range of at most 31 days, default to 100 and cap at 500, and
use descending deterministic keyset order `(start_source_timestamp, id)`. Review history is capped
at 100. Filters are Tenant-scoped and limited to approved lifecycle, Vehicle, Trip, route, severity,
open/closed and bounded source-time facts.

## Commands, concurrency and review lifecycle

Rule creation, mutation and lifecycle commands require `Idempotency-Key`, enforce the frozen
10–5,000 metre inclusive tolerance, and use expected versions for mutation. Duplicate retries
return the recorded result without another transition or success audit. Draft rules can be updated;
the established domain lifecycle governs activate, disable and retire.

Only HIGH episodes are reviewable. Approve and reject append immutable PENDING-to-terminal review
evidence. Corrections append a compensating APPROVED or REJECTED record rather than overwrite
history. The actor who made the current decision cannot reverse it; a different authorized actor
may correct it. `UNKNOWN` requires a 10–500 character note; the other frozen reasons accept an
optional note of at most 500 characters. Expected-version checks and row locking make concurrent
review attempts converge on one accepted result.

## Security, Tenant isolation and audit

| Permission | Authority |
|---|---|
| `ROUTE_DEVIATION_VIEW` | Read rules and current Vehicle deviation state |
| `ROUTE_DEVIATION_MANAGE` | Create, update, activate, disable and retire rules |
| `ROUTE_DEVIATION_EVENT_VIEW` | Read episodes and immutable review history |
| `ROUTE_DEVIATION_APPROVE` | Approve, reject or append a correction |

Permissions are enforced at both controller and secured use-case boundaries. Literal
`/api/v1/...` matchers are covered by regression tests. Unauthenticated access returns 401;
missing permission returns 403; same-Tenant lookup is mandatory, and foreign-Tenant identifiers
resolve through the same safe absence behavior without existence disclosure. Management does not
grant review authority, and event read does not grant rule management or precise history access.

Every successful rule command and review transition writes one immutable, typed Tracking audit
record in the same transaction. The record carries trusted Tenant, trusted actor, action, logical
resource, timestamp, correlation identifier, minimized before/after facts, governed reason and
resulting version. It excludes coordinates, provider payloads, credentials and PII. Rollback,
validation failure and denied commands cannot create a success audit; retries cannot duplicate one.
Read operations create no business audit noise.

## Migration and verification evidence

V89 (`V89__us52_route_deviation_permissions.sql`) idempotently inserts exactly the four approved
permissions. It grants them only to existing `ADMIN` and `LOCAL_MVP_ADMIN` roles; no operational or
new role is created. Clean V1→V89 and V88→V89 migration paths pass, and V88 geometry/deviation
evidence remains unchanged.

- Consolidated CS01–CS04/API/security/audit/PostgreSQL selection: 26 / 26 PASS.
- Repaired V89 current-head/bootstrap regression group: 19 / 19 PASS.
- Architecture, Spring Modulith and ownership: 58 / 58 PASS.
- Complete clean Maven suite: 1,738 / 1,738 PASS; 0 failures, 0 errors, 0 skipped; BUILD SUCCESS in
  10:25.
- Checkstyle: 0 violations. PMD: 0 findings. SpotBugs: 0 findings.
- Dependency analysis: BUILD SUCCESS with the repository's existing starter/transitive
  classification warnings only.
- Docker Compose configuration: valid; PostgreSQL and Redis healthy.
- `git diff --check`: PASS.
- Frontend verification was not run because CS04 changes no frontend file.

## Scope boundary and rollback

CS04 adds no external route-deviation event, Notification integration, Operations intake,
rejected-review escalation, Kafka consumer, scheduled evaluator, frontend, dashboard, replay,
physical-device acceptance or telemetry-pipeline change. CS05 owns durable event/Notification
integration; CS06 owns frontend; CS07 owns the dedicated concurrency/performance closure.

Rollback is an application-code rollback plus forward governance of the V89 permission catalogue;
the applied migration is immutable and must not be edited or reversed destructively. Existing rule,
episode, state, review and audit evidence remains authoritative.
