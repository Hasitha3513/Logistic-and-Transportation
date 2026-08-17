# Example Agent Task Scope

## Task
Implement a Fuel module bug‑fix.

## Allowed

- `src/main/java/com/transportlogistics/app/fuel/**`
- `src/test/java/com/transportlogistics/app/fuel/**`
- `docs/phase-2/**`

## Forbidden

- `src/main/java/com/transportlogistics/app/identity/**`
- `src/main/java/com/transportlogistics/app/trip/**`
- `src/main/java/com/transportlogistics/app/fleet/**` (unless explicitly approved)
- `frontend/**`
- `pom.xml`
- Existing Flyway migrations

## Deletion Allowed
NO

## Refactoring
Only inside Fuel module where required by the fix.

## Public API changes
NO

## Database changes
NO

## Notes
This is an example only. Do not modify production Fuel code during this governance task.
