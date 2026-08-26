# Phase 1 manual integration checklist

Status recorded on 2026-08-15. Checked items were actually executed against the live H2 backend. “API” means command-line calls to the running application; “UI” means browser execution through the Vite frontend.

## Startup and security

- [x] Backend starts on `8080` with context path `/api`.
- [x] Flyway applies all current migrations V1–V11 to a fresh H2 database; the Phase 1 boundary is V10 and V11 is the already-present US-31 slice.
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
- [x] API/UI: nine lifecycle and assignment history entries are returned and rendered, including explicit route assignment.
- [x] API: blank rejection reason returns 400; valid rejection reaches `REJECTED`.
- [x] API: blank cancellation reason returns 400; valid cancellation reaches `CANCELLED`.
- [x] API: duplicate/invalid lifecycle transition returns 409 with a business code.
- [x] UI: dashboard renders the real reporting placeholder response honestly.
- [x] UI: trip list, central status tags, trip details, assignments, route summary, and history render live backend data.

## Manual/remaining UI work

- [x] UI vehicle/category/type CRUD and vehicle document management.
- [x] UI driver CRUD and licence management.
- [x] UI route CRUD and ordered-stop editing.
- [x] UI trip creation/editing.
- [x] Dedicated route assignment action through `/trips/{id}/assign-route`.
- [ ] Trip operational log — backend endpoint/model is absent.
- [ ] Trip exception log/interruption — backend feature is absent beyond reject/cancel history.
- [ ] Real reporting totals and alerts — reporting endpoints currently return READY/empty placeholder data.
- [ ] Explicit access-token expiry in a browser — not waited for during this run; frontend refresh behavior is covered by automated concurrent-401 tests and backend refresh was verified live.
- [x] Fresh browser tab has no console warnings or errors after the Ant Design React 19 compatibility patch.
- [x] Backend automated verification passes.
- [x] Frontend tests and production build pass.
