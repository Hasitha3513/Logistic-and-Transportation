# US-54 View Tracking Dashboard — CS03 V94 API, RBAC and Audit

## Verdict

`PASS — IMPLEMENTATION_IN_PROGRESS / CS03_COMPLETE`

CS03 exposes the frozen bounded dashboard query through a Tenant-scoped, permission-gated body-query API. It adds the permission-only V94 migration, authenticated cursor, conjunctive disclosure, admission controls and privacy-safe audit without changing producer evidence or adding dashboard persistence.

## Baseline and scope

- Starting commit: `5befac0cbdb00a8f27fe0c436c115fc0ed6d5c4d`
- Flyway head: V94
- Accounting: 73 / 87 COMPLETE
- Governing decision: `docs/product-decisions/US-54-VIEW-TRACKING-DASHBOARD-PRODUCT-DECISIONS-001.md`

## Implemented behavior

- `POST /api/v1/tracking/dashboard/query` accepts the frozen bounded body filters, page size and opaque continuation cursor.
- V94 seeds only `TRACKING_DASHBOARD_VIEW` and grants it idempotently to existing `ADMIN`, `LOCAL_MVP_ADMIN` and `DISPATCHER` roles. It creates no role, table, index or unrelated permission.
- Both the literal `/api/v1/...` security route and the secured use-case boundary require `TRACKING_DASHBOARD_VIEW`; broad Tracking permissions do not imply access.
- Optional coordinate/heat-map, producer-incident and Journey Replay sections remain independently permission-gated and omitted without count leakage.
- The HMAC-SHA256 cursor is purpose-separated, five-minute expiring and bound to Tenant, filters, page size and snapshot position.
- Admission is bounded to 10 requests per actor and 40 per Tenant per minute per instance. Rejections return HTTP 429 with `Retry-After: 60`.
- Successful initial queries and rate-limit rejections record only privacy-safe audit categories and bounded status labels. Cursor continuations are not audited per page. The existing authenticated Tracking denial filter records literal-route denials.
- Responses use `Cache-Control: no-store` and `Referrer-Policy: no-referrer`. Malformed filters/cursors return the standard HTTP 400 contract; total live-source unavailability returns HTTP 503; truthful degraded results remain HTTP 200.
- `app.tracking.dashboard.enabled` controls the backend surface without mutating or deleting producer evidence.

## Security and data minimization

- Tenant and actor originate only from authenticated server context and are passed explicitly through query, cursor, rate and audit boundaries.
- Foreign-Tenant identifiers produce safe absence.
- Audit and metrics contain no selectors, IDs, coordinates, cursor values, raw telemetry, provider/device facts, credentials, signatures, Driver or Customer PII.
- The API never grants producer mutation, precise coordinates or producer evidence merely because dashboard access is present.

## Verification

| Gate | Result |
| --- | --- |
| Focused API/security/cursor/V94/PostgreSQL regression | PASS — 37 / 37 |
| Architecture and Spring Modulith | PASS — 59 / 59 |
| Complete acceptance-only Maven | PASS — 1,833 tests; 0 failures; 0 errors; 0 skipped; BUILD SUCCESS; 19:17 |
| Checkstyle | PASS — 0 violations; pre-existing warning debt retained |
| PMD | PASS — BUILD SUCCESS |
| SpotBugs | PASS — 0 findings |
| Dependency analysis | PASS — BUILD SUCCESS; established starter/transitive warning debt retained |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

All PostgreSQL evidence used only `transport_logistics_acceptance`; no development database evidence was accepted. Frontend and Chromium were not rerun because CS03 changes no frontend file, route or browser behavior; those gates belong to CS04.

## Database and contract impact

- Migration: V94 permission catalogue/grants only.
- Table/index changes: none.
- Public API: one read-only `POST /api/v1/tracking/dashboard/query` endpoint.
- Events/topics/dependencies: none.

## Residual work

CS03 does not add dashboard navigation or UI. Responsive table/map fallback, heat layer, polling/backoff, producer labels and browser accessibility belong to CS04.

Exact next queue:

`US-54-VIEW-TRACKING-DASHBOARD-CS04-FRONTEND-001`
