# Phase 1 manual integration checklist

Status recorded on 2026-08-15. Checked items were actually executed against the live H2 backend. “API” means command-line calls to the running application; “UI” means browser execution through the Vite frontend.

## Startup and security

- [x] Backend starts on `8080` with context path `/api`.
- [x] Flyway applies V1–V9 to a fresh H2 database.
- [x] `/api/health` returns 200.
- [x] `/api/v3/api-docs` returns 200.
- [x] Frontend starts on `5173` and proxies `/api` correctly.
- [x] Valid UI login succeeds and reaches the permitted landing page.
- [x] Invalid UI login displays a clean error without navigation or console errors.
- [x] `/auth/me` returns the current user and no password hash.
- [x] Browser reload restores the authenticated session.
- [x] Backend refresh rotation succeeds; replay of the old refresh token returns 401.
- [x] Frontend refresh handling retries once and shares one refresh across concurrent 401s (automated test).
- [x] UI logout revokes the refresh token and returns to `/login`.
- [x] Unauthenticated and invalid-token requests return 401.
- [x] A restricted actor receives 403 before a trip mutation.
- [x] Restricted UI navigation exposes only permitted modules and lands on Trips rather than an unauthorized dashboard.

## Phase 1 business flow

- [x] API: create vehicle category and vehicle type.
- [x] API: create and retrieve a vehicle.
- [x] API: add an active mandatory vehicle document.
- [x] API: vehicle is eligible for a non-overlapping future period.
- [x] API: create and retrieve a driver.
- [x] API: add an active valid driver licence.
- [x] API: driver is eligible for the requested class and a non-overlapping future period.
- [x] API: create a route with origin, destination, distance, duration, and ordered intermediate stop.
- [x] API: create a trip with `routeId`; initial status is `DRAFT`.
- [x] API: `DRAFT -> SUBMITTED`.
- [x] API: `SUBMITTED -> APPROVED`.
- [x] API: vehicle-only assignment leaves the trip `APPROVED`.
- [x] API: driver assignment after vehicle assignment changes the trip to `ASSIGNED`.
- [x] API: `ASSIGNED -> DISPATCHED` with revalidation.
- [x] API: `DISPATCHED -> IN_PROGRESS` with start odometer.
- [x] API: `IN_PROGRESS -> COMPLETED` with end odometer and remarks.
- [x] API: `COMPLETED -> CLOSED`.
- [x] API/UI: eight lifecycle and assignment history entries are returned and rendered.
- [x] API: blank rejection reason returns 400; valid rejection reaches `REJECTED`.
- [x] API: blank cancellation reason returns 400; valid cancellation reaches `CANCELLED`.
- [x] API: duplicate/invalid lifecycle transition returns 409 with a business code.
- [x] UI: dashboard renders the real reporting placeholder response honestly.
- [x] UI: trip list, central status tags, trip details, assignments, route summary, and history render live backend data.

## Manual/remaining UI work

- [ ] UI vehicle/category/type CRUD — blocked: current routes render `ModulePage` placeholders.
- [ ] UI vehicle document CRUD — blocked: no fleet management screen is implemented.
- [ ] UI driver CRUD and licence CRUD — blocked: current driver route is a placeholder.
- [ ] UI route CRUD/ordered-stop editing — blocked: current route page is a placeholder.
- [ ] UI trip creation/editing — blocked: only list and details pages exist.
- [ ] Dedicated route assignment action — backend has no `/trips/{id}/assign-route`; route is supplied on create/update.
- [ ] Trip operational log — backend endpoint/model is absent.
- [ ] Trip exception log/interruption — backend feature is absent beyond reject/cancel history.
- [ ] Real reporting totals and alerts — reporting endpoints currently return READY/empty placeholder data.
- [ ] Explicit access-token expiry in a browser — not waited for during this run; frontend refresh behavior is covered by automated concurrent-401 tests and backend refresh was verified live.
- [x] Fresh browser tab has no console warnings or errors after the Ant Design React 19 compatibility patch.
- [x] Backend automated verification passes.
- [x] Frontend tests and production build pass.
