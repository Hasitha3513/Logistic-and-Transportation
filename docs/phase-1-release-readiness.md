# Phase 1 MVP Release Readiness

## Decision

**NOT READY for production sign-off.** All code-owned CRITICAL/HIGH functional findings identified by the release audit are closed. No PostgreSQL listener is available on ports 5432 or 5433, no database credentials are exported, and the local Docker service is stopped. The mandatory clean PostgreSQL V1–V10 migration, startup, happy-path, and concurrency run therefore remains blocked. H2 evidence is not presented as a substitute for PostgreSQL evidence.

Current branch `agent/fuel-issue-us31` at `1ea7ae5` already contains the separately requested post-Phase-1 US-31 Fuel Issue implementation and V11. It must not be tagged as the pure Phase 1 artifact. A dedicated Phase 1 release branch/commit containing the Phase 1 closure work through V10 is required before `v1.0.0-mvp` is created.

The recommended release tag remains `v1.0.0-mvp` after that gate is closed and the supported Java 21 build is reproduced.

## Final verification evidence

| Area | Command/check | Result | Evidence |
|---|---|---|---|
| Backend build | `mvn clean verify` | PASS | 47 suites; 160 tests; 0 failures, errors, or skips. |
| Spring context | Context/integration suites | PASS | Packaged application also started successfully. |
| Spring Modulith | `ApplicationModulesTest` | PASS | Module-boundary verification passed. Host JDK 26 generated ArchUnit fallback warnings; project targets Java 21. |
| Flyway/H2 | Packaged JAR against empty in-memory H2 | PASS | Current HEAD validated and applied V1–V11 from an empty schema; Phase 1 is V1–V10 and V11 is US-31. |
| JPA | Repository/concurrency suites and `ddl-auto=validate` startup | PASS | Entity/schema validation and persistence integrations passed. |
| PostgreSQL | Fresh V1–V10 and concurrency | BLOCKED | No listener on 5432/5433, no exported credentials, and Docker service is stopped. |
| OpenAPI | Generated `/api/v3/api-docs` | PASS | Contract loads with 62 current-HEAD paths and includes `POST /trips/{id}/assign-route`. |
| Frontend lint | `npm run lint` | PASS | ESLint completed with zero warnings. |
| Frontend tests | `npm run test` | PASS | 8 files; 39/39 tests. Explicit bounded async/test timeouts stabilize Ant Design/jsdom integration tests on slower hosts. |
| Frontend build | `npm run build` | PASS | 5,059 modules; 1,646.93 kB JS (511.25 kB gzip); bundle-size warning only. |

## Outstanding findings by severity

| Severity | Finding | Release effect | Required action |
|---|---|---|---|
| CRITICAL | None identified. | — | — |
| HIGH | Fresh PostgreSQL migration/startup/happy-path/concurrency evidence is unavailable. | Blocks release authorization and the Phase 2 entry gate. | Provision a disposable PostgreSQL instance and execute the final gate below. |
| HIGH | Current HEAD includes post-Phase-1 US-31/V11 in the same commit as Phase 1 closure changes. | A `v1.0.0-mvp` tag on current HEAD would contain out-of-scope Phase 2 behavior. | Curate and review a Phase-1-only release commit through V10 before tagging. |
| MEDIUM | Verification ran on host Java 26 while compiling with `--release 21`; ArchUnit logged Java-26 fallback warnings. | Supported runtime reproduction is incomplete. | Repeat the release build and startup on Java 21. |
| MEDIUM | Reporting placeholders, missing trip/running logs, and the absence of a maintenance aggregate require formal product acceptance or deferral. | Product scope sign-off remains open. | Record explicit release-owner decisions. |
| MEDIUM | Token storage, CORS/same-origin, Swagger exposure, secrets, backup/restore, monitoring, and rollback remain deployment decisions. | Operations/security sign-off remains open. | Complete the deployment checklist in the target environment. |
| LOW | The frontend production chunk exceeds Vite's 500 kB warning threshold. | Performance warning only; build succeeds. | Accept for MVP or schedule code splitting. |

## Packaged-JAR smoke result

A fresh H2 run used the opt-in local identity and sample-data bootstraps. The actual HTTP flow found a valid vehicle with mandatory documents, a driver with a compatible licence, and a route; it then submitted the sample draft trip, approved it, assigned vehicle, driver, and route, dispatched, started, completed, and closed it.

- Health: `UP`
- Authenticated actor: `release.admin` with the local MVP-admin permission set
- Audit trip: `60000000-0000-0000-0000-000000000001` (`TRIP-DEMO-001` on the fresh in-memory run)
- Final state: `CLOSED`
- Route, vehicle, and driver assignments: persisted
- Status/history entries: 9
- Generated OpenAPI route-assignment operation: present

| Smoke step | Result |
|---|---|
| Login | PASS |
| Create/find valid vehicle and mandatory document | PASS |
| Create/find valid driver and compatible licence | PASS |
| Create/find route | PASS |
| Create trip | PASS |
| Submit | PASS |
| Approve | PASS |
| Assign vehicle | PASS |
| Assign driver | PASS |
| Assign route | PASS |
| Dispatch | PASS |
| Start | PASS |
| Complete | PASS |
| Close | PASS |
| Verify history | PASS — 9 entries |

| Required negative | Result |
|---|---|
| Expired mandatory vehicle document rejected | PASS — automated availability/dispatch coverage |
| Expired driver licence rejected | PASS — automated availability/assignment/dispatch coverage |
| Overlapping vehicle rejected | PASS — 409 and concurrent-allocation coverage |
| Overlapping driver rejected | PASS — 409 and concurrent-assignment coverage |
| Unauthorized approval returns 403 before mutation | PASS — security integration coverage |
| Invalid lifecycle transition rejected | PASS — domain/service/controller integration coverage |

The first driver-assignment attempt requested class `C`; the driver owned an active `HEAVY` licence, so the backend returned 400 with `REQUIRED_LICENSE_CLASS_MISSING` and did not assign the driver. Repeating the command with the coherent `HEAVY` requirement completed the flow. Early dispatch/start/complete/close attempts returned 409 without advancing the trip, providing live invalid-transition evidence.

## Closed release blockers

| Former blocker | Resolution | Evidence |
|---|---|---|
| Missing core foreign keys and overlap indexes | Added forward-only `V10__phase1_release_integrity.sql`; V1 was not edited. | Clean H2 V1–V10 migration and repository tests pass. |
| Missing explicit route-assignment command/UI | Added trip input-port command, route eligibility boundary, endpoint, permission mapping, audit entry, tests, and frontend action. | OpenAPI and packaged-JAR smoke confirm the operation. |
| General trip-lifecycle race | Trip mutations now load through a pessimistic write-lock repository operation. | Concurrent lifecycle integration test permits one winning mutation and one conflict without duplicate audit effects. |
| Missing operator mutation UI | Added create/edit flows for vehicles, categories, types, drivers, routes, and trips; document/licence management and deactivation controls use existing APIs. | Frontend lint/tests/build pass. |
| Missing frontend lint gate | Added ESLint configuration and script. | `npm run lint` passes with zero warnings. |

## Security and negative behavior

- JWT login, `/auth/me`, expiration, refresh rotation/replay rejection, logout revocation, disabled-user prevention, BCrypt hashing, and password-response safety are covered by tests.
- Business permissions cover fleet, routing, trip lifecycle, reporting, dashboard, identity administration, and actuator access.
- Integration tests verify unauthenticated 401, authenticated-without-authority 403, and permitted success. Authorization is evaluated before domain mutation.
- Expired mandatory documents, inactive/maintenance vehicles, invalid/incompatible licences, unavailable drivers, and schedule overlaps return structured rejection/conflict behavior.
- Concurrent vehicle assignment, driver assignment, and lifecycle mutation are covered on H2. PostgreSQL locking semantics remain unverified until the release database gate is run.

## Database integrity

V10 adds core foreign keys for project, vehicle type, vehicle, route, route stop, and trip references, plus indexes supporting vehicle allocation, driver assignment, route lookup, and vehicle category/type queries. This is a forward-only migration.

Remaining database limitations:

- PostgreSQL V1–V10 and concurrency evidence is missing.
- The duplicate-active-document rule is application-enforced rather than expressed as a portable database invariant.
- Existing production-like data must be validated before V10 is applied outside a clean environment.

## Known limitations and accepted/deferred scope

- Reporting/dashboard endpoints still return honest placeholder/empty values rather than operational aggregates.
- Trip logs and fleet running logs are not implemented; product ownership must confirm whether they remain mandatory Phase 1 scope or are formally deferred.
- Maintenance blocks availability through vehicle operational status; a maintenance aggregate/workflow is not present.
- Trip and most master lists are unpaged/unbounded; the frontend cannot obtain true server-side pagination where the backend lacks it.
- Vehicle/driver availability list evaluation can produce N+1 queries.
- Driver licences model `ACTIVE`, `INACTIVE`, and `DELETED`, not distinct `SUSPENDED`/`REVOKED` states.
- Browser tokens use `localStorage`; production acceptance or hardening is required.
- Production CORS/same-origin, Swagger exposure, secrets, monitoring, backup, and rollback are deployment decisions still requiring sign-off.
- The frontend production chunk is approximately 1.65 MB (511 kB gzip) and triggers Vite's size warning.

## Final release gate

Provision a disposable PostgreSQL database and credentials, then:

1. start from an empty schema and apply Flyway V1–V10;
2. start the packaged application with the PostgreSQL profile and `ddl-auto=validate`;
3. run the full test suite plus concurrent vehicle, driver, and lifecycle mutation scenarios on PostgreSQL;
4. retain migration/schema/history evidence and validate V10 against production-like data;
5. reproduce the build with Java 21 and complete operational/security/product sign-offs.

When those checks pass, update this document to READY, curate a Phase-1-only release commit through V10, and create the annotated `v1.0.0-mvp` tag on that commit. US-31 already exists on the current feature branch as a prior explicit exception; no further Phase 2 slice should begin before the entry criteria and authoritative story mapping are approved.
