# US-54 View Tracking Dashboard — CS01 Domain and Query Contracts

## Verdict

`PASS — IMPLEMENTATION_IN_PROGRESS / CS01_COMPLETE`

CS01 defines the framework-neutral Tracking dashboard domain vocabulary, deterministic policy rules, inbound query contract and narrow outbound source contracts required by the approved US-54 product decision. It does not expose an HTTP endpoint or change runtime behaviour.

## Governed source

- Product decision: `docs/product-decisions/US-54-VIEW-TRACKING-DASHBOARD-PRODUCT-DECISIONS-001.md`
- Starting application commit: `6e05f792569a3e5cf9aefc916f6914542a3989e8`
- Starting Flyway head: V93
- Story accounting: 73 / 87 COMPLETE

## Implemented contracts

- `TrackingDashboardModels` defines Tenant-scoped queries, filters, disclosure, observations, live state, Trip context, incident evidence, vehicle rows, summary, heat cells, source status and the bounded dashboard page.
- `TrackingDashboardPolicy` enforces the 100-Vehicle and 100-row bounds, classifies motion only from trusted observed speed, excludes untrusted/stale positions from heat density and deterministically bins eligible coordinates into 0.01-degree cells.
- `TrackingDashboardQueryUseCase` is the provider-neutral inbound query boundary.
- `TrackingDashboardLiveStatePort`, `TrackingDashboardIncidentPort`, `TrackingDashboardTripContextPort` and `TrackingDashboardCursorPort` isolate source aggregation and cursor integrity behind narrow outbound boundaries.
- Collection-valued contracts defensively copy inputs and outputs.
- Every source query carries an explicit `tenantId`; CS01 introduces no unscoped persistence access.

## Explicit non-scope

- No Flyway migration; head remains V93 and V94 remains reserved for the later permission-only change set.
- No REST controller or public API is exposed.
- No application service, adapter, Redis query, PostgreSQL query, Trip integration or Notification integration is wired.
- No frontend or Chromium behaviour changes.
- No new permission, event, topic, dependency or table is introduced.
- No producer acceptance status is inherited by US-54.

## Verification evidence

Java 21 was used throughout.

| Gate | Result |
| --- | --- |
| Focused `TrackingDashboardPolicyTest` | PASS — 5 / 5 |
| Focused architecture + CS01 clean run | PASS — 64 / 64 |
| Complete acceptance-only `./mvnw -B clean test` | PASS — 1,820 tests; 0 failures; 0 errors; 0 skipped; BUILD SUCCESS; 18:00 |
| Checkstyle | PASS — 0 violations (repository warning-level formatting debt retained) |
| PMD | PASS — BUILD SUCCESS |
| SpotBugs | PASS — 0 bug instances; 0 errors |
| Dependency analysis | PASS — BUILD SUCCESS; existing starter/transitive classification warnings retained |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

The accepted full-suite invocation pinned `DB_URL`, `POSTGRES_DB`, `TRANSPORT_TEST_DB_URL` and both credential families to `transport_logistics_acceptance`, selected local acceptance mode and disabled development sample-data and identity bootstraps. A discarded earlier invocation contacted the development datasource before it was stopped; no evidence from that invocation is accepted here.

Scheduled-task SQL errors and Kafka connection warnings occurred only while test-owned contexts were resetting schemas or shutting down brokers. They did not produce a Maven test failure; the terminal aggregate above is authoritative.

Frontend and Chromium were not rerun because CS01 has no frontend, HTTP or runtime adapter change. Their previously accepted baseline is neither promoted nor inherited by this change set.

## Security and architecture result

- Tracking remains the owning bounded context.
- Domain and port code is framework-neutral.
- Tenant identity is explicit at every query/source boundary.
- Coordinates remain subject to the disclosure contract; no provider/device facts, credentials, signatures, Driver PII or Customer PII were added.
- Cross-module Trip data is represented only through the published port contract; no foreign repository or table access exists.

## Residual work and queue

CS01 is not story acceptance. The next governed task is:

`US-54-VIEW-TRACKING-DASHBOARD-CS02-BOUNDED-SOURCE-AGGREGATION-001`

Accounting remains 73 / 87 COMPLETE and Flyway remains V93.
