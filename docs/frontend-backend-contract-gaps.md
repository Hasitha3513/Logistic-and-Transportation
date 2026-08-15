# Frontend/backend contract gaps

## Integration fixes applied

| Feature | Frontend expectation | Actual backend contract | Problem | Fix applied | Remaining action |
|---|---|---|---|---|---|
| API base path | Relative API calls such as `/auth/login` | Backend context path is `/api` | Requests were sent to Vite or to backend paths without `/api` | Axios defaults to `/api`; Vite proxies `/api` to port 8080 | Keep deployment routing aligned |
| Vite configuration | `vite.config.ts` is authoritative | Stale generated `vite.config.js` sat beside it | Vite loaded the old proxy and returned frontend HTML for API calls | Removed/ignored generated config artifacts | None |
| Login | Protected dashboard requires JWT tokens | `POST /auth/login`, then `GET /auth/me` | No login route/form existed | Added RHF/Zod/Ant Design login page and protected routing | None for current contract |
| Token renewal | A 401 caused by access-token expiry should be recoverable | `POST /auth/refresh` rotates access and refresh tokens | No response interceptor or concurrency control existed | Added one shared refresh promise, one retry, token rotation, and failed-refresh logout; added tests | Browser time-based expiry remains a manual long-running check |
| Fresh H2 identity | Local login needs an existing user | Identity administration is protected; migrations seed permissions only | A fresh database had no usable first user | Added disabled-by-default H2 bootstrap using environment-only credentials | Production/PostgreSQL still needs an approved provisioning process |
| Identity persistence | Role/user creation writes parent then join rows | JPA parent entities plus JDBC join tables | JDBC ran before JPA flush and violated foreign keys | Use `saveAndFlush` before join-table replacement | Covered by local bootstrap integration test |
| Current-user display | `roles` is normally an array | `/auth/me` returns role names and permissions | A stale/malformed session response crashed on `roles[0]` | Defensive role display; permissions remain backend-authoritative | None |
| Least-privilege landing | User should land on an authorized module | Dashboard requires `DASHBOARD_VIEW` | Restricted users were sent to `/` and immediately received a dashboard 403 | Home route selects the first permitted module | None |
| Ant Design runtime | No console warnings | React 19 with Ant Design 5 needs the official compatibility patch | AntD emitted a compatibility warning | Installed/imported `@ant-design/v5-patch-for-react-19` | Consider Ant Design 6 only as a separately planned upgrade |

## Remaining contract/product gaps

| Feature | Frontend expectation | Actual backend endpoint/request/response | Problem | Fix applied | Remaining action |
|---|---|---|---|---|---|
| Fleet management UI | Vehicle/category/type/document forms and tables | CRUD exists under `/vehicles`, `/vehicle-categories`, `/vehicle-types`, and `/vehicles/{id}/documents` | Frontend routes are presentation-only placeholders | None; not hidden with mock data | Implement the Phase 1 fleet screens against these real contracts |
| Driver management UI | Driver/licence forms, list, and availability | CRUD/availability exists under `/drivers` and `/drivers/{id}/licenses` | Frontend driver route is a placeholder | None | Implement the Phase 1 driver screen |
| Route management UI | Route form with ordered stops | `POST/PUT /routes` accepts `stops`; responses expose `stopLocationIds` | Frontend route page is a placeholder; request/response field names differ | Live API persistence verified; no fake UI added | Map form `stops` to response `stopLocationIds` in the route screen |
| Trip creation/editing | Create/edit trip in UI | `POST/PUT /trips` accepts `TripRequest` | Frontend has list/details only | None | Implement Phase 1 trip order form |
| Route assignment command | Operational “assign route” action | No `/trips/{id}/assign-route`; `routeId` is part of create/update | No dedicated audited assignment transition | None | Decide whether existing create/update semantics satisfy MVP; otherwise add a trip-owned command in a future backend slice |
| Trip list pagination | Server-side page, sort, and filter | `GET /trips` returns an unpaged list and has no filters | Frontend must paginate/filter client-side | UI labels the fallback honestly and avoids duplicate full fetches | Add backend pagination/filtering, then switch the query contract |
| Lifecycle timestamps | Start/complete modals may collect actual times | Start accepts only `startOdometerKm`; complete accepts `endOdometerKm` and remarks; server supplies timestamps | Operators cannot submit explicit actual start/end timestamps | Frontend follows the real backend contract | Add timestamp fields only if MVP business rules require operator-entered values |
| Dashboard metrics | Counts, progress, documents, and exception alerts | `GET /dashboard/operations` returns `{date,status}` only | No operational totals are available | UI displays “Not supplied by reporting API” rather than inventing metrics | Implement reporting queries in the reporting module |
| Reports | Trip/driver/vehicle report data | `/reports/trips`, `/reports/driver-assignments`, `/reports/vehicle-utilization` return empty placeholders | No frontend report page and no real report data | None | Implement reporting data first, then UI |
| Trip operational logs | Chronological checkpoints/notes/delays | No endpoint/model/table | Tabs are marked Future | None | Implement the remaining MVP trip-log slice |
| Exceptions/interruption | Operational exception history | Reject/cancel are supported; no interruption/log API | Exceptions tab is marked Future | None | Implement only the defined remaining MVP exception behavior |
| Cross-origin deployment | Absolute frontend API origin | Backend has no CORS configuration; Vite proxy makes local calls same-origin | A separately hosted frontend cannot call the API directly | Local development uses narrow same-origin proxy; security was not weakened | Add environment-configured exact origins only when deployment topology requires it |
| Error-to-form mapping | Backend `fieldErrors` map to RHF fields | `ApiError` supplies code/message/fieldErrors/correlationId | Existing trip action errors show message/correlation ID; missing CRUD forms cannot map field errors yet | Live 400/401/403/404/409 contracts verified | Map field errors as each missing CRUD form is implemented |
| Authorization references | Trip viewer sees related display names | Vehicle/driver reference endpoints require their own view permissions | `TRIP_VIEW`-only user sees abbreviated IDs, not vehicle/driver names | UI does not make unauthorized reference calls | Consider a trip-owned read projection if names must be visible without fleet permissions |

## Security endpoints still authenticated-only

The following organization endpoints fall through to `.anyRequest().authenticated()` and have no explicit business permission mapping: `/customers/**`, `/departments/**`, `/locations/**`, and `/projects/**`. This was not weakened or redesigned during integration. Define organization permissions in a separate authorized MVP security slice if these mutations must be exposed to non-administrative UI users.
