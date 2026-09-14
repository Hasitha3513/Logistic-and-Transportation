# US-52 Monitor Route Deviations — CS06 Frontend and Hybrid Closure

Status: **COMPLETE** on 2026-09-14. US-52 remains `IMPLEMENTATION_IN_PROGRESS`; accounting remains
73/87 complete and 14/87 remaining. Flyway head is V91.

The permission-aware route-deviation frontend, API integration and corrected accessible Chromium
selectors are retained from the authorized baseline. V91 closes the structural hybrid-ingestion
gap: immutable Timescale history now creates durable, Tenant-qualified route-deviation evaluation
work without manufacturing legacy `tracking_position` rows. The same canonical dispatcher also
preserves US-49 geofence and US-50 speed evaluation behavior.

## Acceptance evidence

- Real PostgreSQL/Kafka/Redis US-49 journey: 6/6 PASS.
- Real PostgreSQL/Kafka/Redis US-50 journey: 10/10 PASS.
- Real PostgreSQL-backed Chromium CS06: 10/10 PASS; no partial result accepted.
- Frontend Vitest: 77 files, 319/319 tests PASS.
- TypeScript and production build: PASS.
- No frontend file changed; changed-file ESLint is empty/pass by scope.
- Global ESLint reports 71 pre-existing errors in unrelated Delivery files; V91/CS06 introduced
  none and this debt is not authorized for opportunistic remediation.
- Architecture/Spring Modulith: 59/59 PASS.
- Complete Maven: 1,750/1,750 PASS; BUILD SUCCESS in 10:18.
- Checkstyle: zero violations; PMD and SpotBugs: PASS.
- Packaged hybrid startup: PASS on Java 21 against `transport_logistics_acceptance`; OpenAPI and
  Swagger UI return 200. Unauthenticated health returns the expected protected 401 response.

No public API, permission, event contract, frontend route, dependency or story accounting changed.
CS06 is technically closed. Next: `US-52-MONITOR-ROUTE-DEVIATIONS-CS07-POSTGRES-CONCURRENCY-PERFORMANCE-001`.
