# US-54 View Tracking Dashboard — CS04 Frontend

## Verdict

`PASS — IMPLEMENTATION_IN_PROGRESS / CS04_COMPLETE`

CS04 delivers the permission-gated, responsive Tracking dashboard at `/tracking/dashboard`. It consumes the secured CS03 body-query API, keeps selectors out of URLs and browser storage, and provides an accessible operational table with an SVG map/heat-density enhancement. The table remains authoritative when the map cannot render.

## Baseline and scope

- Branch: `feat/us67-acceptance-evidence-closure`
- Starting HEAD: `fe0ec361614efc661cb33cc685a407b84ed97323`
- Flyway head: V94 (unchanged)
- Accounting: 73 / 87 COMPLETE (unchanged)
- Governing decision: `docs/product-decisions/US-54-VIEW-TRACKING-DASHBOARD-PRODUCT-DECISIONS-001.md`

## Implemented behaviour

- Feature-flagged navigation and route access require `TRACKING_DASHBOARD_VIEW`.
- The query is a POST body; selectors are not placed in the URL or persisted in browser storage.
- Polling runs every 15 seconds only while the document is visible and online; failures use bounded 30/60-second retry delays.
- Fleet rows expose truthful live/recent/stale/degraded/offline state, observed motion, current Trip/route context, incidents and replay availability without exposing device/provider credentials or PII.
- The SVG map provides a density layer and selected-vehicle focus while the responsive table remains usable independently.
- Producer limitations are labelled instead of being presented as inherited acceptance.
- Notification unread count is requested only when the authenticated user also has `NOTIFICATION_VIEW`.

## Runtime defect and minimal correction

The first real-browser run proved that the existing dashboard audit adapter could serialize a `safe_detail` value longer than the existing `VARCHAR(300)` column, causing an HTTP 500 after the otherwise successful query. The adapter now records compact deterministic fact keys and sorted comma-delimited values. The approved facts and audit semantics are preserved, the result is bounded to the existing schema, and no migration, API or product-contract change was required. A direct regression test proves the 300-character bound and retained facts.

## Verification evidence

| Gate | Result |
| --- | --- |
| Focused frontend tests | 5 / 5 PASS |
| Complete Vitest | 330 / 330 PASS across 83 files |
| TypeScript | PASS |
| Production build | PASS; existing bundle-size advisory only |
| Changed-file ESLint | PASS |
| Architecture / Modulith | 59 / 59 PASS |
| Real PostgreSQL-backed Chromium | 6 / 6 PASS in 45.0 seconds |
| Complete Maven | 1,834 tests; 0 failures; 0 errors; 0 skipped; BUILD SUCCESS in 11:57 |
| Checkstyle | PASS; 0 violations (existing warnings remain repository-wide debt) |
| PMD | BUILD SUCCESS |
| SpotBugs | 0 findings |
| Dependency analysis | BUILD SUCCESS; existing repository-wide declaration warnings only |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

All accepted PostgreSQL-backed evidence used `transport_logistics_acceptance`. No development database was used as acceptance evidence.

## Security and privacy

Backend authorization, Tenant binding, conjunctive disclosure, rate limits and no-store semantics remain authoritative. The frontend neither invents permissions nor treats hidden navigation as authorization. Coordinates appear only when the API authorizes their disclosure. Provider credentials, signatures, device details, Driver PII and Customer PII are not rendered or stored.

## Residual scope and next queue

CS04 does not certify physical telemetry, operator sign-off, performance capacity or operational recovery. Those remain independent gates.

Exact next queue:

`US-54-VIEW-TRACKING-DASHBOARD-CS05-POSTGRES-REDIS-PERFORMANCE-OPERATIONS-001`
