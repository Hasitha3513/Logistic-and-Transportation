# Phase 2 Entry Criteria

Phase 2 production development must not begin until every mandatory criterion below has objective evidence.

## Mandatory Phase 1 gates

- [ ] Phase 1 backend `mvn clean verify` passes on Java 21 with no failed or skipped critical tests.
- [x] Frontend `npm run lint`, `npm run test`, and `npm run build` pass (39/39 tests in 8 files).
- [ ] A fresh PostgreSQL database applies Flyway V1 through V10 without manual schema steps.
- [ ] The application starts on that PostgreSQL database and `/api/health` returns `UP`.
- [ ] The complete authenticated trip flow passes against PostgreSQL.
- [ ] PostgreSQL concurrent vehicle, driver, and lifecycle scenarios allow exactly one conflicting mutation.
- [x] No CRITICAL Phase 1 defect remains.
- [x] No HIGH data-integrity, security, lifecycle, API-contract, or operator-workflow defect remains.
- [x] `docs/phase-1-api-contract.md` matches the V10 Phase 1 controllers and frontend clients/types; post-Phase-1 V11 paths are explicitly out of scope.
- [ ] Phase 1 release notes and deployment/rollback evidence exist.
- [ ] Product and operations explicitly accept the documented Phase 1 limitations.

## Current assessment

Backend/H2, frontend lint/test/build, API-contract, release-note, integrity-migration, route-assignment, and lifecycle-locking evidence is present. The backend passed 160/160 tests on the Java 26 host while targeting Java 21; the required Java 21 reproduction remains unchecked. PostgreSQL clean-room, PostgreSQL happy-path, and production concurrency evidence are not available, so the Phase 2 entry gate is currently **CLOSED**.

US-31 Fuel Issue was implemented earlier under an explicit user request before these gates were closed. Treat it as a recorded exception, not evidence that the gate is open. No subsequent Phase 2 production slice should start until all mandatory criteria pass.

## Story-source gate

The consolidated US-01–US-87 requirements/user-story document must be added or linked before Phase 2 slices are accepted. The repository currently contains Phase 1 story mapping only through US-16. Phase 2 story identifiers must be copied from the source; they must not be guessed or renumbered.
