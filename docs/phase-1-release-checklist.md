# Phase 1 Release Checklist

## Build and architecture

- [x] `mvn clean verify` passes: 43 suites and 140/140 tests.
- [x] Spring context and packaged-JAR startup pass.
- [x] Spring Modulith boundaries pass.
- [x] JPA schema validation and repository integration tests pass.
- [x] `npm run lint` passes with zero warnings.
- [x] Frontend tests pass: 29/29 in 7 files.
- [x] Frontend production build passes.
- [ ] Release build is reproduced with Java 21 rather than host JDK 26.

## Database and Flyway

- [x] Unique forward-only Flyway versions V1–V10 exist; V1 was not edited for release integrity changes.
- [x] Fresh H2 V1–V10 migration and application startup succeed.
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
- [x] Phase 2 backlog, module/frontend strategy, slices, and entry criteria exist without Phase 2 production code.
- [ ] Authoritative consolidated US-01–US-87 source is supplied and Phase 2 items are mapped verbatim.
- [ ] PostgreSQL release evidence is attached.
- [ ] Product, security, operations, and release-owner sign-offs are complete.
- [ ] Working tree is intentionally reviewed and committed; current user-owned workspace is not clean.
- [ ] Annotated `v1.0.0-mvp` tag is created only after all gates pass.

Current authorization: **NOT READY — PostgreSQL and sign-off gates remain open.**
