# Phase 1 MVP

## Scope

Phase 1 establishes authenticated transport operations: fleet and driver compliance, route planning, trip authorization, resource assignment, dispatch, execution, completion, and audit history in one Spring Modulith application with an Ant Design operator frontend.

## Implemented Features

- Vehicle categories/types, vehicle master, documents, and period-based eligibility
- Driver profiles, multiple licences, licence-class eligibility, and assignment availability
- Route definitions with ordered stops, distance, duration, search, and active state
- Trip creation/editing and explicit route, vehicle, and driver assignment
- Submit, approve, reject, dispatch, start, complete, close, and cancel lifecycle commands
- Fresh dispatch eligibility validation, overlap conflict detection, and append-only status history
- React operator forms and permission-aware navigation/actions for the implemented Phase 1 masters and trip workflow

## Security

- Signed bearer JWT access tokens with expiration
- Rotating, revocable, hashed refresh tokens
- BCrypt password hashing at strength 12
- Disabled-user prevention and current-authority reload
- Permission-based endpoint authorization, including route assignment and actuator administration
- Consistent 401/403 behavior and correlation-aware `ApiError` responses

## Database

Flyway is authoritative. Migration V10 adds Phase 1 foreign keys and indexes required for referential integrity and allocation queries. H2 clean migration V1-V10 passes. A clean PostgreSQL run is still a release gate because usable audit credentials/container runtime were unavailable on the verification workstation.

## Frontend

The React/TypeScript/Vite frontend uses Ant Design, TanStack Query, React Hook Form, and Zod. It includes login, dashboard, fleet/driver/route management, compliance record management, trip create/edit/detail, route/resource assignment, lifecycle actions, history, centralized status tags, and permission guards.

## Tested Business Flow

The real API flow has passed on a freshly migrated H2 database:

Login → vehicle category/type → vehicle → mandatory document → driver → compatible licence → route → trip → submit → approve → assign vehicle → assign driver → assign route → dispatch → start → complete → close → verify history.

Negative tests cover expired compliance records, inactive/maintenance resources, overlap conflicts, unauthorized commands, invalid lifecycle transitions, reasons, and odometer validation. H2 concurrent vehicle, driver, and lifecycle tests allow exactly one conflicting mutation.

Final verification: backend `mvn clean verify` passed 160/160 tests across 47 suites; frontend lint passed, Vitest passed 39/39 tests across 8 files, and the production build passed. The fresh packaged-JAR smoke trip `TRIP-DEMO-001` reached `CLOSED` with 9 ordered history entries. These current-HEAD checks also exercise the already-present post-Phase-1 V11 migration; the Phase 1 release boundary itself remains V10.

## Known Limitations

- Clean PostgreSQL migration and concurrency evidence are pending.
- Reporting returns limited/placeholder aggregates.
- Operational list APIs are not yet server-paginated.
- Trip logs, running logs, and persisted maintenance workflows are not implemented.
- Browser refresh tokens remain in local storage; deployment must accept or harden this risk.

## Deferred Features

- Fuel Purchase, bunker/stock ledger, fuel cards, reconciliation, and analytics. US-31 Fuel Issue already exists on a post-Phase-1 feature branch and is not part of the Phase 1 release artifact.
- Freight and cargo management
- GPS and real-time tracking
- Delivery management and proof of delivery
- Advanced route analytics/disruption handling
- Advanced analytics and AI optimization
- Full last-mile delivery, which is not included in normal Phase 2 scope without requirements confirmation

## Deployment Notes

- Use Java 21 and Maven 3.9.x.
- Supply `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a production-strength `JWT_SECRET` through deployment secrets.
- Disable H2 bootstrap/sample flags in production.
- Run `mvn clean verify`, frontend lint/test/build, and a clean PostgreSQL V1-V10 migration before release authorization.
- Decide production exposure for Swagger/OpenAPI and configure same-origin routing or a narrow CORS policy.

## Migration Version

Latest Phase 1 migration: `V10__phase1_release_integrity.sql`.

## Release Readiness

Recommended identifier: `v1.0.0-mvp`.

Current state: **NOT READY** until the clean PostgreSQL migration and PostgreSQL concurrency gate pass. No tag has been created or pushed.

Do not tag the current `agent/fuel-issue-us31` HEAD as Phase 1 because it contains US-31/V11. Create the tag only from a reviewed Phase-1-only release commit through V10.

## Release Commands

After a reviewed Phase-1-only branch named `release/phase-1-mvp` exists, the release owner should run:

```powershell
git switch release/phase-1-mvp
git status --short
git log -1 --oneline
git tag --list v1.0.0-mvp
git tag -a v1.0.0-mvp -m "Phase 1 MVP"
git push origin release/phase-1-mvp
git push origin v1.0.0-mvp
```

`git status --short` must be empty, the inspected commit must contain migrations only through V10, and `git tag --list v1.0.0-mvp` must return no existing tag before the tag command is run. No tag or tag push was executed during this audit.
