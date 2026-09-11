# US-49 Manage Geofences CS04A — V78 Permission Seed

**Task:** `US-49-MANAGE-GEOFENCES-CS04A-V78-PERMISSION-SEED-001`  
**Result:** COMPLETE  
**US-49:** `IMPLEMENTATION_IN_PROGRESS`  
**Flyway:** V78; no V79  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`

## Why V78 was required

CS04 could not implement its REST authorization until the three frozen US-49 permission codes existed in
the authoritative Identity RBAC catalogue. V78 is a forward-only security-metadata migration; V1–V77 remain
unchanged.

## Exact permission seed and grants

V78 inserts and activates exactly:

- `GEOFENCE_VIEW` — read same-Tenant geofence definitions and current memberships;
- `GEOFENCE_MANAGE` — create, update, activate, disable and retire same-Tenant geofences;
- `GEOFENCE_EVENT_VIEW` — read same-Tenant geofence transition history.

Following the established administrative grant convention, V78 grants these permissions only when the
existing role is named `ADMIN` or `LOCAL_MVP_ADMIN`. It does not create roles or grant Drivers, Dispatchers,
Customers, Riders, read-only Tracking roles or other non-administrative roles. The local Identity bootstrap
catalogue contains the same three codes and its canonical permission count is 181.

## Migration and verification evidence

- Clean V1→V78 and explicit V77→V78 migration: 2/2 PASS on
  `transport_logistics_acceptance`; exactly three active codes, administrative grants and absence of
  accidental non-administrative grants verified.
- Focused Identity/RBAC/bootstrap and Tracking security compatibility: 46/46 PASS.
- Complete Tracking Java regression: 154/154 PASS.
- Complete Maven verification: 1,560 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 09:34.
- Architecture and Modulith: 52/52 PASS.
- Checkstyle: 0 configured violations. PMD: BUILD SUCCESS. SpotBugs: 0 findings and 0 errors.
- Flyway head: V78. V79: NONE.
- Development database authoritative evidence: NO.
- `git diff --check`: PASS.

## Scope exclusions

No API, controller, security annotation, frontend, event, Notification consumer, domain behavior,
geofence persistence model, public contract, dependency or story-accounting change was made.

## Next controlled change set

`US-49-MANAGE-GEOFENCES-CS04-APIS-RBAC-AUDIT-001-RERUN`
