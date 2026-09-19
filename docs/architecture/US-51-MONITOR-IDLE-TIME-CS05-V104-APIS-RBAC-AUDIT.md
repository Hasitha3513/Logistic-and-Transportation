# US-51 CS05 — V104 APIs, RBAC and audit

## Status

`IMPLEMENTATION_IN_PROGRESS / CS05_COMPLETE`

CS05 adds only the approved read surface and permission catalogue. It does not change evaluator
state, activate a production engine-running source, estimate fuel, or satisfy physical acceptance.

## Endpoint contract matrix

| Route | Permission | Request and validation | Minimized response | Pagination and persistence query | Tenant and audit |
|---|---|---|---|---|---|
| `GET /api/v1/tracking/idle-monitoring/states` | `IDLE_MONITOR_VIEW` | Optional `vehicleId`, `state`, `cursor`; default 50, max 100 | Vehicle ID, monitoring/capability state, source-time state timestamps and counters; fuel estimate explicitly `UNAVAILABLE` | `latest_source_timestamp DESC, vehicle_id DESC`; HMAC Tenant/filter-bound cursor; `tracking_idle_state` | Server Tenant predicate; minimized `IDLE_MONITOR_STATES_VIEWED` audit |
| `GET /api/v1/tracking/idle-monitoring/episodes` | `IDLE_EVENT_VIEW` | Optional `vehicleId`, `endReason`; required `[from,to)` up to 31 days; default 50, max 100 | Confirmed/closed episode identity, Vehicle ID, lifecycle timestamps, reason and confirmed seconds; no candidate/internal/provider fields | `start_source_timestamp DESC, id DESC`; HMAC Tenant/query-bound cursor; `tracking_idle_episode` excludes `CANDIDATE` | Server Tenant predicate; minimized `IDLE_EPISODES_VIEWED` audit |
| `GET /api/v1/tracking/idle-monitoring/episodes/{episodeId}` | `IDLE_EVENT_VIEW` | UUID path identity | Same minimized episode contract | Tenant-qualified identity lookup excluding `CANDIDATE` | Foreign Tenant/candidate returns safe not-found; minimized detail audit |
| `GET /api/v1/tracking/idle-monitoring/episodes/{episodeId}/evidence` | `IDLE_EVENT_VIEW` | UUID path identity, optional cursor; default 50, max 100 | Evidence identity, source timestamp, quality outcome, credited delta, recorded time | `source_timestamp ASC, id ASC`; HMAC Tenant/episode-bound cursor; `tracking_idle_episode_evidence` | Parent ownership verified first; minimized `IDLE_EVIDENCE_VIEWED` audit |

Every response is `Cache-Control: no-store` with `Referrer-Policy: no-referrer`. Vehicle labels are
nullable because Tracking has no Tenant-qualified published Fleet label lookup; IDs remain truthful.
Coordinates, device/provider references, raw engine/CAN evidence, payloads, credentials, PII and
internal diagnostics are absent.

## V104

`V104__us51_idle_monitoring_permissions.sql` inserts exactly `IDLE_MONITOR_VIEW` and
`IDLE_EVENT_VIEW`, then grants them only to pre-existing `ADMIN` and `DISPATCHER` roles. It creates
no role and deliberately grants neither permission to `LOCAL_MVP_ADMIN`.

## Architecture and security

- Framework-neutral inbound/read/cursor/audit ports keep persistence and HTTP out of the application contract.
- Both `SecurityConfig` literal path rules and the secured use-case decorator enforce permissions.
- Every SQL operation begins with `tenant_id`; episode evidence requires a same-Tenant visible parent.
- Cursor signatures bind Tenant, operation, filters and the ordered tuple.
- Read auditing stores only correlation-safe text, filter presence, requested size and result count.
- Read paths issue no evaluator, dispatch, Notification or Operations mutation.

## Verification

- Focused unit, PostgreSQL migration and literal-path security: **9/9 PASS**.
- Clean V1→V104 and explicit V103→V104 permission migration: **PASS**.
- Exact catalogue: two permissions; exact existing-role grants: ADMIN and DISPATCHER; LOCAL_MVP_ADMIN: zero.
- Literal `/api/v1/...` authentication/authorization, direct use-case denial, no-store, Tenant not-found,
  31-day boundary, candidate exclusion, minimized audit and response minimization: **PASS**.
- Architecture/Modulith: **59/59 PASS**.
- Checkstyle: **0 violations** (repository warnings remain non-blocking).
- Complete Maven: **1,950 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**.
- PMD: **PASS** after documenting the JDBC row-mapper index parameter as intentionally unused.
- SpotBugs: **0 findings**.
- Dependency analysis: **BUILD SUCCESS**; the repository's existing declared/transitive dependency warnings
  remain unchanged and no dependency was added by CS05.
- Docker Compose configuration and `git diff --check`: **PASS**.

## Remaining boundaries

- Production Fleet powertrain classification remains `UNKNOWN`.
- Production Flespi, Traccar and Generic engine-running mappings remain disabled.
- Fuel estimation remains unavailable.
- Physical authoritative engine-running evidence and operator sign-off remain pending.
- Exact next queue: `CS06 frontend`.
