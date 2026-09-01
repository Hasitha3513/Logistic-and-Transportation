# US-67 Governance Reconciliation and Independent Final Acceptance

## 1. Decision

- **Task:** `MVP-1.4-US67-GOVERNANCE-RECONCILIATION-AND-FINAL-ACCEPTANCE-001`
- **Date:** 2026-09-01
- **Governance resolution:** `ACCEPT_EXISTING_US67_IMPLEMENTATION_AS_BASELINE`
- **Application branch:** `feat/us67-last-mile-eta-implementation`
- **Application HEAD:** `ab0b3965beffa14d3e78697946f1a5883b5bba27`
- **Final decision:** `FAILED — US-67 NOT COMPLETE`
- **Story state:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED`
- **Accounting:** MVP 1.4 remains `4 / 8`; overall remains `61 / 87`; deferred remains `26 / 87`.

The implementation was not reverted, removed, reset, or otherwise downgraded.
The product-decision document is reconciled from the stale sequencing label
`NOT_STARTED` to the verified source state.

## 2. Source Authority and Contract Reconciliation

The review used the original US-67 use-case/activity/sequence material, frozen
product decisions, production source at HEAD, implementation report, accepted
US-63 through US-66 artifacts, Route sources and tests, frontend sources and
tests, roadmap, and central knowledge-base history.

| Decision | Classification | Evidence |
| :--- | :--- | :--- |
| Delivery owns ETA orchestration | MATCH | ETA domain, application service, ports, and adapters are under `delivery`. |
| ETA consumes a fixed batch sequence | MATCH | Members are sorted by `sequenceHint`; no TSP/VRP optimization occurs. |
| Single-order and batch ETA projections | MATCH | Both models and REST operations exist. |
| Haversine with 1.3 circuity and zone/mode speeds | MATCH | `ZoneModeHeuristicRoutingAdapter` implements the model. |
| 15-minute operational cache, no persistence | MATCH | In-memory tenant/fingerprint keys and `staleAt`; no ETA migration/table. |
| `V55` remains Flyway head | MATCH | 55 migrations validated; database schema current at version 55. |
| Implementation state `NOT_STARTED` | DOCUMENTATION_STALE | HEAD already contains the implementation; reconciled non-destructively. |
| Rider/vehicle mode affects calculation | MATERIAL_CONFLICT | Service resolves every single and batch calculation as `MOTORBIKE`; rider lookup does not map a rider capability/mode. |
| Provider strategy | IMPLEMENTED_EQUIVALENT / RECONCILED | Product authority approved `HEURISTIC_ONLY` for MVP. External-provider and fallback gates are `NOT_APPLICABLE_EXTERNAL_PROVIDER_DEFERRED`; `LastMileRoutingPort` remains the extension seam. |
| Stale-write race protection | MATERIAL_CONFLICT | Cache writes are unconditional `ConcurrentHashMap.put` operations; there is no generation/version comparison or CAS protecting the same key. |
| Mutation/reassignment/location invalidation | MATERIAL_CONFLICT | Eviction methods exist on the cache port/adapter, but no production event listener or mutation service invokes them. |

## 3. Environment and Migration

- Java: OpenJDK `21.0.12`.
- PostgreSQL: repository PostgreSQL 16 container started and healthy.
- Flyway: validated `55` migrations; schema version `55`; no V56 ETA migration.
- Initial in-sandbox test attempt was invalidated by the sandbox blocking Mockito
  agent attachment. The focused suite was rerun outside that restriction.

## 4. Verified Passing Evidence

Command:

```text
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH \
./mvnw test -Dtest='EtaModelTest,ZoneModeHeuristicRoutingAdapterTest,DeliveryEtaServiceTest,DeliveryEtaControllerTest'
```

Result: `13` tests, `0` failures, `0` errors, `0` skipped; `BUILD SUCCESS`.

This proves the implemented model constructors/freshness check, basic single and
batch happy paths, missing-coordinate rejection, mocked not-found behavior,
controller happy paths, Haversine coordinate validation, and mode/zone heuristic
functions. It does not prove the mandatory concurrency and integration gates.

## 5. Failed Mandatory Gates

### 5.1 Rider mode and origin integration

`DeliveryEtaService` hardcodes `MOTORBIKE` and `URBAN_DENSE` for single orders.
For a batch it loads an assigned rider but still assigns `MOTORBIKE`. It does not
derive a canonical transport mode or current rider location. Therefore mode
application, rider reassignment semantics, and the accepted origin hierarchy are
not verified or correctly implemented.

### 5.2 Stale-write protection and concurrency

The input fingerprint separates changed input identities, but it does not prevent
an older request with the same fingerprint from overwriting a newer calculation.
Cache writes are unconditional. There are no tests for:

- concurrent calculation of the same subject;
- calculation racing batch membership mutation;
- calculation racing rider reassignment;
- an older provider response arriving after a newer response;
- invalidation racing a read; or
- identical subject UUIDs under different tenant contexts at the cache adapter.

Consequently the mandatory concurrency and stale-write gates fail.

### 5.3 Invalidation

`evictOrderEta` and `evictBatchEta` are implemented but have no production call
sites. Batch membership/status mutation, rider reassignment, and delivery-location
correction therefore rely on a changed fingerprint being observed on a later read;
they do not implement the frozen event/port invalidation contract, and obsolete
entries remain resident until process restart or global clear.

### 5.4 Provider strategy reconciliation

Product authority subsequently approved `MVP_PROVIDER_STRATEGY = HEURISTIC_ONLY`.
Production wiring correctly supplies the local zone/mode Haversine heuristic
through provider-neutral `LastMileRoutingPort`. External-provider timeout, 429,
5xx, malformed response, slow response, fallback, credentials, endpoint SSRF,
quota, and retry gates are now
`NOT_APPLICABLE_EXTERNAL_PROVIDER_DEFERRED`. `EtaSource.HEURISTIC` is the normal
MVP result; `HEURISTIC_FALLBACK` and `EXTERNAL_PROVIDER` remain reserved and must
not be emitted until a future upstream provider path actually exists.

### 5.5 Tenant, IDOR, RBAC, and security evidence

Controller annotations use the frozen existing permissions. Batch repository
lookups are explicitly tenant-scoped and cache keys include tenant ID. However,
the mandatory hostile ETA-specific evidence is incomplete: controller tests cover
only successful requests, and there is no real order/batch ETA integration suite
proving cross-tenant 404 behavior, permission denial, tenant spoofing resistance,
or cache collision isolation. No unresolved vulnerability is asserted here; the
gate fails because required evidence is absent.

### 5.6 Frontend and Chromium

The ETA UI is implemented in the batch drawer. The Playwright spec contains one
mocked batch happy path with mocked authentication, data, ETA read, and refresh.
It does not prove single-order ETA, mode effects, stale state, missing coordinates,
RBAC denial, tenant isolation, reload behavior, or the real React-to-REST path.
Therefore Chromium and real-application-path gates fail.

## 6. Route, Architecture, Events, Privacy, and Scope Review

- Route boundary: PASS by source inspection; ETA consumes `sequenceHint` and does
  not optimize, mutate Route aggregates, or implement TSP/VRP.
- Domain purity: no framework imports were found in the ETA domain models/ports.
- Events: provider-neutral, tenant-aware calculated event exists and contains no
  customer PII or provider secret.
- Telematics: no live GPS ingestion, breadcrumb, OBD, or polling implementation
  was introduced.
- US-68, US-69, US-70: no exception workflow, notification delivery, or customer
  self-service implementation was found in US-67.
- ETA remains cache-only; it is not represented as persisted history.

Because mandatory product gates already failed, architecture, static analysis,
Delivery/Route regression, full backend verification, complete frontend suite,
production build, performance acceptance, and real Chromium integration were not
misrepresented as passing and were not used to override the failure.

## 7. Smallest Safe Correction

Create `MVP-1.4-US67-LAST-MILE-ETA-ACCEPTANCE-REMEDIATION-001` scoped to:

1. derive transport mode and accepted origin from canonical Rider/Delivery data;
2. add generation/version-aware conditional cache publication and single-flight
   behavior sufficient to prevent stale overwrite;
3. integrate explicit tenant-aware invalidation for batch membership/status,
   rider reassignment, and corrected delivery location through approved ports/events;
4. add the six mandatory concurrency scenarios and ETA-specific tenant/RBAC/IDOR tests;
5. add real-path frontend/Chromium coverage;
6. rerun every remaining hard acceptance gate, including full backend and frontend suites.

US-68 must not start until US-67 acceptance passes.
