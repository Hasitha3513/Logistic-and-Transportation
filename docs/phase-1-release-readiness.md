# Phase 1 MVP Release Readiness

## Decision

**NOT READY for production sign-off.** All code-owned CRITICAL/HIGH findings identified by the release audit are closed. The remaining release gate is a clean PostgreSQL V1–V10 migration, application startup, and concurrency run. The local PostgreSQL 18 service is reachable on port 5433 but no usable audit credentials were available; Docker Desktop's service is stopped and cannot be started with the current OS privileges. H2 evidence is not presented as a substitute for PostgreSQL evidence.

The recommended release tag remains `v1.0.0-mvp` after that gate is closed and the supported Java 21 build is reproduced.

## Final verification evidence

| Area | Command/check | Result | Evidence |
|---|---|---|---|
| Backend build | `mvn clean verify` | PASS | 43 suites; 140 tests; 0 failures, errors, or skips. |
| Spring context | Context/integration suites | PASS | Packaged application also started successfully. |
| Spring Modulith | `ApplicationModulesTest` | PASS | Module-boundary verification passed. Host JDK 26 generated ArchUnit fallback warnings; project targets Java 21. |
| Flyway/H2 | Packaged JAR against empty in-memory H2 | PASS | V1–V10 validated and applied from an empty schema. |
| JPA | Repository/concurrency suites and `ddl-auto=validate` startup | PASS | Entity/schema validation and persistence integrations passed. |
| PostgreSQL | Fresh V1–V10 and concurrency | BLOCKED | PostgreSQL 18 listens on 5433; credentials unavailable. Docker service unavailable. |
| OpenAPI | Generated `/api/v3/api-docs` | PASS | Contract loads and includes `POST /trips/{id}/assign-route`. |
| Frontend lint | `npm run lint` | PASS | ESLint completed with zero warnings. |
| Frontend tests | `npm run test` | PASS | 7 files; 29/29 tests, including route assignment. |
| Frontend build | `npm run build` | PASS | 5,055 modules; bundle-size warning only. |

## Packaged-JAR smoke result

A fresh H2 run used the opt-in local identity and sample-data bootstraps. The actual HTTP flow created vehicle category/type, vehicle, mandatory document, driver, licence, and route; created and submitted a trip; approved it; assigned vehicle, driver, and route; dispatched, started, completed, and closed it.

- Health: `UP`
- Authenticated actor: `release.admin` with the local MVP-admin permission set
- Audit trip: `97f840ed-aaec-4ce9-8539-cae33593ad08`
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

The first smoke-client attempt serialized location UUIDs as JSON arrays due to a PowerShell wrapping error. Spring rejected the malformed requests with 400 before trip creation. The corrected script used terminating HTTP errors and completed the entire flow.

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
- The frontend production chunk is approximately 1.63 MB (507 kB gzip) and triggers Vite's size warning.

## Final release gate

Provision a disposable PostgreSQL database and credentials, then:

1. start from an empty schema and apply Flyway V1–V10;
2. start the packaged application with the PostgreSQL profile and `ddl-auto=validate`;
3. run the full test suite plus concurrent vehicle, driver, and lifecycle mutation scenarios on PostgreSQL;
4. retain migration/schema/history evidence and validate V10 against production-like data;
5. reproduce the build with Java 21 and complete operational/security/product sign-offs.

When those checks pass, update this document to READY, create the release commit, and create the annotated `v1.0.0-mvp` tag. Do not begin Phase 2 implementation before the entry criteria and authoritative US-01–US-87 mapping are approved.
