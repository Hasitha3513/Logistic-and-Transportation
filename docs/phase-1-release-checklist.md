# Phase 1 Release Checklist

## Build and architecture

- [x] `mvn clean verify` passes: 47 suites and 160/160 tests.
- [x] Spring context and packaged-JAR startup pass.
- [x] Spring Modulith boundaries pass.
- [x] JPA schema validation and repository integration tests pass.
- [x] `npm run lint` passes with zero warnings.
- [x] Frontend tests pass: 39/39 in 8 files.
- [x] Frontend production build passes.
- [ ] Release build is reproduced with Java 21 rather than host JDK 26.

## Database and Flyway

- [x] Unique forward-only Flyway versions V1–V10 exist; V1 was not edited for release integrity changes.
- [x] Fresh H2 migration and application startup succeed: V1–V10 Phase 1 plus the already-present post-Phase-1 V11 US-31 migration.
- [x] V10 adds core reference foreign keys and allocation/query indexes.
- [ ] Disposable PostgreSQL database and credentials are provisioned.
- [ ] Fresh PostgreSQL V1–V10 migration succeeds.
- [ ] Application starts with PostgreSQL and `ddl-auto=validate` without manual schema changes.
- [ ] V10 is checked against production-like data before deployment.
- [ ] PostgreSQL backup, restore, migration-failure, and rollback procedures are approved.

## Security and configuration

- [x] Login, `/auth/me`, access expiration, refresh rotation/replay rejection, and logout are verified.
- [x] Disabled users are rejected and BCrypt hashes never appear in REST responses.
- [x] Unauthenticated protected calls return 401.
- [x] Authenticated actors without permission receive 403 before mutation.
- [x] Fleet, route, trip lifecycle, reporting/dashboard, identity, and actuator permissions are covered.
- [x] PostgreSQL URL/user/password have no committed operational defaults.
- [ ] Deployment supplies database and high-entropy JWT secrets.
- [ ] H2 identity/sample-data bootstrap flags are disabled in production.
- [ ] Browser token-storage risk is accepted or hardened.
- [ ] CORS/same-origin and Swagger/OpenAPI exposure are explicitly approved.

## Phase 1 business flow

- [x] Vehicle category/type and vehicle create/edit/deactivate UI/API paths exist.
- [x] Vehicle document management and eligibility checks exist.
- [x] Driver create/edit/deactivate and licence management exist.
- [x] Route create/edit/deactivate and ordered stops exist.
- [x] Trip create/edit and explicit route assignment exist.
- [x] Submit, approve/reject, vehicle/driver assignment, dispatch, start, complete, close, and cancel exist.
- [x] Packaged-JAR HTTP path reaches CLOSED with route, vehicle, driver, and 9 history records.
- [x] Generated OpenAPI includes the route-assignment operation.
- [ ] Product formally accepts placeholder reporting/dashboard values.
- [ ] Product formally defers or accepts missing trip logs/running logs and full maintenance workflow.

## Negative and concurrency checks

- [x] Invalid vehicle/document and driver/licence eligibility reasons are tested.
- [x] Vehicle and driver overlap conflicts return 409.
- [x] Invalid lifecycle transitions and invalid reason/odometer inputs are rejected.
- [x] H2 concurrent vehicle and driver assignments permit exactly one winner.
- [x] H2 concurrent lifecycle mutation permits exactly one winner without duplicate history.
- [ ] PostgreSQL vehicle, driver, and lifecycle concurrency scenarios pass.
- [ ] Distinct suspended/revoked licence states are accepted as out of Phase 1 or modeled later.

## Frontend and contract

- [x] Permission-aware navigation, details, mutation forms, assignment drawers, and lifecycle modals are present.
- [x] Backend field errors map into React Hook Form; Ant Design is the primary UI system.
- [x] Status presentation and notification handling are centralized.
- [x] API contract inventory is frozen in `phase-1-api-contract.md` and generated OpenAPI is live.
- [ ] Product accepts list endpoints without true server-side pagination.
- [ ] Bundle-size warning is accepted for MVP or reduced before deployment.

## Release and Phase 2 gates

- [x] Phase 1 release notes exist.
- [x] Phase 2 backlog, module/frontend strategy, slices, and entry criteria exist; this task added no Phase 2 production code and records the already-present US-31 exception.
- [ ] Authoritative consolidated US-01–US-87 source is supplied and Phase 2 items are mapped verbatim.
- [ ] PostgreSQL release evidence is attached.
- [ ] Product, security, operations, and release-owner sign-offs are complete.
- [ ] Release changes are intentionally reviewed and committed; this audit leaves only the documented release/test-harness edits uncommitted.
- [ ] A Phase-1-only release commit through V10 is curated; current HEAD also includes US-31/V11 and is not a valid pure Phase 1 tag target.
- [ ] Annotated `v1.0.0-mvp` tag is created only after all gates pass.

Current authorization: **NOT READY — PostgreSQL and sign-off gates remain open.**
