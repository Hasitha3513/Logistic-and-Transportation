# Phase 2 Entry Criteria

Phase 2 production development must not begin until every mandatory criterion below has objective evidence.

## Mandatory Phase 1 gates

- [ ] Phase 1 backend `mvn clean verify` passes on Java 21 with no failed or skipped critical tests.
- [ ] Frontend `npm run lint`, `npm run test`, and `npm run build` pass.
- [ ] A fresh PostgreSQL database applies Flyway V1 through V10 without manual schema steps.
- [ ] The application starts on that PostgreSQL database and `/api/health` returns `UP`.
- [ ] The complete authenticated trip flow passes against PostgreSQL.
- [ ] PostgreSQL concurrent vehicle, driver, and lifecycle scenarios allow exactly one conflicting mutation.
- [ ] No CRITICAL Phase 1 defect remains.
- [ ] No HIGH data-integrity, security, lifecycle, API-contract, or operator-workflow defect remains.
- [ ] `docs/phase-1-api-contract.md` matches generated OpenAPI and frontend clients/types.
- [ ] Phase 1 release notes and deployment/rollback evidence exist.
- [ ] Product and operations explicitly accept the documented Phase 1 limitations.

## Current assessment

Backend/H2, frontend lint/build, API-contract, release-note, integrity-migration, route-assignment, and lifecycle-locking work is present. PostgreSQL clean-room and production concurrency evidence is not available, so the Phase 2 entry gate is currently **CLOSED**.

## Story-source gate

The consolidated US-01–US-87 requirements/user-story document must be added or linked before Phase 2 slices are accepted. The repository currently contains Phase 1 story mapping only through US-16. Phase 2 story identifiers must be copied from the source; they must not be guessed or renumbered.
