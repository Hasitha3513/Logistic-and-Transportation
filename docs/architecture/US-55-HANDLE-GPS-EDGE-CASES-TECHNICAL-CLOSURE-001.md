# US-55 Handle GPS Edge Cases — Technical Closure

## Verdict

`TECHNICALLY_COMPLETE / ACCEPTANCE_PENDING`

This closure verifies the implementation through CS08 without claiming physical-device or
real-provider acceptance. Flyway remains V100 and MVP accounting remains 73/87.

## Baseline

- Application baseline: `884a977c71f1e35a969ede045ae7777701be3aa7`
- Knowledge-base baseline: `8f8b7c164f5cab5c24d4f2e23863560798c51af1`
- Branch: `feat/us67-acceptance-evidence-closure`
- Starting state: both repositories clean, remotely contained, divergence `0 0`
- Database used by destructive/integration verification: `transport_logistics_acceptance`
- Migration head: V100

## Requirement-to-evidence matrix

| Requirement | Classification | Evidence |
| --- | --- | --- |
| Canonical V1/V2 signals and explicit supported/unknown capability semantics | PROVEN | CS01, CS02 and canonical V2 contract; V1/V2 compatibility and consumer tests |
| Immutable telemetry history and effective-dated device capability resolution | PROVEN | V96, PostgreSQL acceptance, source-time capability tests |
| GPS-exception episode lifecycle and immutable evidence | PROVEN | V97, domain/service and PostgreSQL acceptance tests |
| Concurrent creation, idempotency and restart-safe recovery | PROVEN | CS08 deterministic PostgreSQL concurrency/recovery suite |
| Signal-loss scanning and two-point recovery | PROVEN | CS04 evaluator/scanner tests and retained CS08 recovery evidence |
| Redis ordering, Tenant-qualified keys and trusted-state guards | PROVEN | CS04 and CS08 Redis integration/concurrency tests |
| Geofence, speed and route-deviation eligibility | PROVEN | retained producer eligibility/regression suites; suspect/untrusted observations are excluded |
| First-open Notification and one HIGH Operations fact | PROVEN | CS05 bridge, durable event, duplicate-delivery and Tenant-recipient tests |
| API RBAC, Tenant isolation, bounded pagination, no-store and privacy | PROVEN | CS06 literal HTTP/security/PostgreSQL tests and contract registry |
| Durable acknowledgement replay and atomic audit | PROVEN | V100, acknowledgement replay, idempotency and transaction/audit tests |
| Frontend permissions and uncertain-request retry behavior | PROVEN | CS07 Vitest and real Chromium acceptance |
| PostgreSQL/Kafka/Redis concurrency, bounded waits and recovery | PROVEN | CS08 focused infrastructure gates and controlled performance evidence |
| Physical loss, burst, reassignment, tamper and battery behavior | EXPLICITLY_DEFERRED | Requires genuine device/provider evidence; fixtures do not satisfy physical acceptance |
| Automatic Notification catalogue provisioning for future Tenants | EXPLICITLY_DEFERRED | Frozen governance decision; existing seeded Tenants only |
| Traccar production polling/onboarding adapter | EXPLICITLY_DEFERRED | Canonical normalizer/fixtures exist; production adapter remains allocated to the US-48 provider roadmap |

## CS08 behavior review

### Episode concurrency

The advisory-lock identity includes Tenant and the episode identity, preventing duplicate open
episodes while allowing independent Tenants and Vehicles to progress. PostgreSQL concurrency
tests prove one open episode, append-only evidence, monotonic severity and restart-safe recovery.

### Kafka publication

Ingress waits for all required publication acknowledgements within the configured bounded
deadline. Partial publication and timeouts fail the request without weakening the existing
canonical deduplication identity; retries remain idempotent. Consumer acknowledgement still
occurs only after successful durable or live projection.

### Hybrid `/latest` source selection

Technical closure found and corrected one discrepancy: the CS08 implementation returned any
PostgreSQL state before consulting Redis, so an older persisted point could mask a newer eligible
live projection. The final implementation reads both Tenant-qualified sources and selects the
newest eligible trusted source timestamp. An older Redis value cannot regress PostgreSQL state;
Redis failures are propagated rather than converted to "no data"; no foreign-Tenant key can be
queried. Only `latestTrustedEligible` projections are eligible for the live path, so uncertain
telemetry is never promoted to trusted state.

## Provider capability disposition

| Provider | Production adapter/onboarding | Fixture tests | Real-provider evidence | Disposition |
| --- | --- | --- | --- | --- |
| Flespi | Polling adapter and governed provider binding implemented | Yes | No | Technical implementation proven; external capture pending |
| Generic signed ingress | Secure inbound adapter and onboarding implemented | Yes | No | Technical implementation proven; physical/provider acceptance pending |
| Traccar | Canonical payload normalization only | Yes | No | Production adapter/onboarding remains an explicit US-48 dependency |

An enum, alias or normalizer is not treated as proof of a provider integration. No provider has
been credited with physical acceptance in this closure.

## Verification results

- Focused US-55/PostgreSQL/Redis/security/V1-V2 suite: 62/62 PASS.
- Hybrid source-selection regression: 3/3 PASS.
- Complete Maven: 1,895 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS in 12:32.
- Architecture and Spring Modulith: 59/59 PASS.
- Frontend Vitest: 336/336 PASS; TypeScript and production build PASS.
- Real PostgreSQL-backed GPS-exception Chromium: 6/6 PASS.
- Hybrid performance Chromium: 1/1 PASS.
- Checkstyle: zero violations; PMD BUILD SUCCESS; SpotBugs zero findings.
- Dependency analysis: BUILD SUCCESS with the unchanged declared/used dependency warning baseline.
- Packaged Java 21 startup: PASS against acceptance PostgreSQL, Kafka and Redis; V100 current.
- Docker Compose validation and `git diff --check`: PASS.

## Performance and operational evidence

The closure rerun used the isolated PostgreSQL acceptance database, Kafka and Redis with synthetic
canonical telemetry. It measured 576.6 events/s sustained and 4,693.7 events/s burst; `/latest` p95
was 16.6 ms and history p95 was 19.2 ms. These are local
technical measurements, not production SLOs or capacity claims. Recovery evidence covers bounded
Kafka acknowledgement, Redis restart/order protection, PostgreSQL episode convergence and
durable acknowledgement replay.

## Security and privacy

- Tenant identity scopes persistence, advisory locks, Redis keys, cursors and recipient selection.
- Untrusted or suspect observations cannot become latest trusted state.
- API responses and integration payloads exclude credentials, signatures, raw provider payloads,
  precise coordinates in notifications, processing exceptions and Driver/Customer PII.
- Protected endpoints retain backend permission enforcement; frontend visibility is not relied on
  as authorization.

## Limitations and remaining acceptance prerequisites

- Genuine provider/device evidence is still required for physical signal loss, burst behavior,
  reassignment, tamper and battery/power scenarios.
- Flespi external capture is pending.
- Traccar production adapter/onboarding is not implemented and remains governed by US-48.
- Future-Tenant Notification defaults remain deferred.
- This closure does not advance story accounting or assert final product acceptance.

## Next governed action

`US-55-HANDLE-GPS-EDGE-CASES-FINAL-ACCEPTANCE-001`

Final acceptance must stop with an external-prerequisite disposition if genuine evidence remains
unavailable; it must not substitute fixtures for physical evidence.
