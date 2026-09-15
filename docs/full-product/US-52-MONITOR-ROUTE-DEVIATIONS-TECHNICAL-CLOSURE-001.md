# US-52 Monitor Route Deviations Technical Closure

**Verdict:** `TECHNICALLY_COMPLETE`

**Acceptance:** `ACCEPTANCE_PENDING`

**Date:** 2026-09-15

**Application baseline:** `6d36b556181f72f7146a7ba06459ecf8328c94b6`

**Flyway head:** V92
**Accounting:** 73/87 complete; 14/87 remaining

## Executive summary

US-52 satisfies its technical contract across the Trip route-revision prerequisite and CS01 through CS07. The implementation compares eligible trusted positions with the exact immutable Routing revision assigned by Trip at source time, maintains deterministic Tenant-qualified state and episodes, exposes permission-separated APIs and frontend workflows, and publishes privacy-minimized durable detection and escalation events to Notification. Consolidated backend, PostgreSQL, architecture, frontend and Chromium gates pass.

The story is not finally accepted. Its frozen decisions require a US-52-specific physical provider/device journey proving lateral-coordinate and accuracy fidelity in a safe controlled route, plus operational recipient and user sign-off. Evidence from US-48, US-49 or US-50 is not inherited.

## Authoritative source inventory

| Source | Result |
| --- | --- |
| `docs/requirements/Traspotation & logistic.docx` | Located and inspected; identifies US-52 Route Deviations for the Tracking Operator and the `RouteDeviation` concept. |
| `docs/requirements/Mind-Map-Trasportation-and-Logistic.txt` | Located; planned-versus-actual path, alerts, severity and approval intent confirmed. |
| `docs/requirements/US-51-US-60-UseCase-Activity-Sequence-Diagrams.md` | Located; comparison, severity, recording, optional fuel estimate, approval and escalation flow inspected. |
| Product decisions | `US-52-MONITOR-ROUTE-DEVIATIONS-PRODUCT-DECISIONS-001.md`; frozen and internally consistent with implemented CS01–CS07. |
| Prerequisite and CS evidence | Trip prerequisite plus CS01, CS02/V88, CS03, CS04/V89, CS05/V90, CS06/V91 and CS07/V92 documents located and reconciled. |
| Roadmaps and governance | Both roadmaps and root `AGENTS.md` inspected. |

No required source is missing. The UML's apparent direct planned-route repository access is superseded by the approved published Routing contract. Optional fuel-waste estimation is explicitly excluded from Phase 1 because no approved deterministic Fuel contract exists; it does not block core US-52.

## Prerequisite and change-set reconciliation

| Slice | Technical result | Principal evidence |
| --- | --- | --- |
| Trip prerequisite/V85 | SATISFIED | Trip snapshots `route_id` and immutable nullable `route_version`; source-time lookup is Tenant-qualified; historical null remains non-evaluable. |
| CS01 | SATISFIED | Pure route-deviation domain policies and provider-neutral `PlannedRouteGeometryLookup` contract. |
| CS02/V88 | SATISFIED | Routing revision geometry and Tracking rule, state, episode and review persistence with Tenant-qualified constraints. |
| CS03 | SATISFIED | Eligibility, accuracy-aware corridor, two-point confirmation, monotonic severity, closure and route-context behavior. |
| CS04/V89 | SATISFIED | Explicit APIs, four permissions, safe Tenant absence, audit and append-only review/SoD behavior. |
| CS05/V90 | SATISFIED | Durable detected/escalated events, exact IN_APP catalogue, same-Tenant Dispatcher delivery, idempotency and privacy minimization. |
| CS06/V91 | SATISFIED | Permission-aware frontend and durable history-backed detector dispatch with leases, retry and `SKIP LOCKED`. |
| CS07/V92 | SATISFIED | Deterministic PostgreSQL concurrency and Tenant-leading episode/Trip query indexes with result equivalence. |

## Original requirement traceability

Each original US-52 acceptance condition is represented once below. Approved decisions refine implementation without redefining the original capability.

| ID | Authoritative requirement | Implementation and evidence | Status | Gap |
| --- | --- | --- | --- | --- |
| US52-AC-01 | Compare planned and actual routes | Trip source-time assignment, Routing immutable geometry lookup, `RouteDeviationEvaluator`, TS02–TS04 and evaluator tests | SATISFIED | None technical |
| US52-AC-02 | Calculate/assign deviation severity | Accuracy-expanded tolerance with deterministic WARNING/HIGH and no downgrade; domain/PostgreSQL tests | SATISFIED | None technical |
| US52-AC-03 | Record deviations and query them historically | V88 episodes/reviews, bounded keyset API, V92 index, frontend history and Chromium scenario 8 | SATISFIED | None technical |
| US52-AC-04 | Support approval of significant deviations | HIGH-only approve/reject/correct-review commands, `ROUTE_DEVIATION_APPROVE`, optimistic review version, Chromium scenarios 4–7 | SATISFIED | None technical |
| US52-AC-05 | Approved deviations remain auditable | Append-only review records and audit entries; correction compensates rather than overwrites | SATISFIED | None technical |
| US52-AC-06 | Escalate significant rejected deviations | Deterministic `VehicleRouteDeviationEscalatedV1`, V90 Notification bridge/catalogue and replay-idempotency tests | SATISFIED | None technical |
| US52-AC-07 | Estimate fuel waste when data is available | Optional UML extension, explicitly excluded by frozen Phase-1 decision pending a deterministic Fuel contract | NOT_APPLICABLE | Deferred product decision; not part of approved Phase 1 |

Traceability totals: 6 `SATISFIED`, 1 `NOT_APPLICABLE`, 0 partial, unsatisfied or contradicted.

## Domain and persistence

The stable state is `UNKNOWN`, `ON_ROUTE` or `DEVIATING`. Eligible observations are trusted, associated, in-order, fresh WGS84 positions with known accuracy. The first outside point retains its coordinate, accuracy, source time and attribution; a second distinct consecutive outside point under the same Trip, route revision and rule version confirms an episode. Equality remains inside. Missing attribution, nullable historical route version, unavailable geometry/rule, stale, duplicate, untrusted and out-of-order telemetry cannot fabricate state.

Severity is WARNING above effective tolerance through twice tolerance and HIGH strictly above twice tolerance. It may progress WARNING to HIGH but never downgrade. Direct HIGH confirmation emits only HIGH detection; the first later WARNING-to-HIGH transition emits one `DISTANCE_HIGH` escalation. Rejected HIGH emits one `REVIEW_REJECTED` escalation. Continued HIGH, closure, approval and correction do not flood notifications.

V88 owns Tenant-qualified Tracking rule, state, episode and immutable review tables. It enforces one active rule per Tenant/route/revision and one open episode per Tenant/Vehicle. Same-module composite foreign keys, optimistic versions, deterministic identities and Tenant-leading indexes are present. Routing owns immutable revision geometry and Trip owns assignment snapshots; Tracking uses published adapters and does not query foreign persistence.

## Telemetry, API and frontend

Signed provider ingress produces the canonical Kafka telemetry event. Redis projects current live state, TimescaleDB retains immutable history, and V91 atomically records Tenant-qualified evaluation intent from accepted history. The worker uses bounded claims, `FOR UPDATE SKIP LOCKED`, lease expiry/recovery and terminal completion; evaluation failure does not roll back telemetry.

`/api/v1/tracking/route-deviations` exposes bounded rule management, current state, keyset episode history/detail and explicit approve, reject and compensating-correction commands. It accepts no client Tenant authority and normal responses exclude coordinates and geometry. The React/TypeScript frontend provides permission-aware rule, state, episode and review surfaces, guarded deep links, truthful UNKNOWN presentation, responsive tablet behavior and backend error handling.

## Events, Notification, security and privacy

`VehicleRouteDeviationDetectedV1` and `VehicleRouteDeviationEscalatedV1` use the P1-01 outbox with Tenant envelopes and at-least-once, consumer-idempotent delivery. V90 seeds only the two IN_APP catalogue entries. Active same-Tenant Dispatcher members are recipients. Tracking WARNING maps to Notification WARNING; Tracking HIGH and both escalation reasons map to the platform's existing CRITICAL notification severity. No email, SMS or Operations/US-78 integration exists.

RBAC remains separated into `ROUTE_DEVIATION_VIEW`, `ROUTE_DEVIATION_MANAGE`, `ROUTE_DEVIATION_EVENT_VIEW` and `ROUTE_DEVIATION_APPROVE`, enforced at HTTP and use-case boundaries. Tenant, actor and correlation context are derived from trusted authentication. Foreign-Tenant resources return safe absence/denial. Advisory locks, dispatch identities, persistence keys, event envelopes and recipient resolution are Tenant-qualified. Same-reviewer reversal is prohibited; a different authorized reviewer may append a correction.

Events, notifications, API responses and audit details exclude raw telemetry, coordinates, geometry, provider/device details, credentials, signatures, review notes from messages, and Driver/Customer PII. Notification failure cannot invalidate Tracking evidence.

## Flyway, concurrency and performance

Flyway clean replay and current-head validation pass through V92. V1–V91 remain immutable. V92 is the only CS07 migration and creates exactly two ordinary transactional indexes: `idx_tracking_route_deviation_episode_keyset` and `idx_trip_tenant_vehicle_source_assignment`. Both are Tenant-leading, ready and valid. Deployment requires the documented write-drained maintenance window; after successful deployment V92 is immutable and removal requires a reviewed forward migration.

CS07 proves same-Vehicle convergence, one open episode, duplicate/event idempotency, stale-state protection, monotonic severity, append-only concurrent review behavior, Tenant-qualified advisory locking, distinct `SKIP LOCKED` claims, lease recovery, terminal dispatch and deadlock absence. Representative 10,000-row plans changed episode pagination from sequential scan/top-N sort to ordered index-only access and Trip assignment from broad filtered/sorted lookup to its partial covering index. Local timings are environment evidence, not production SLO certification.

## Automated verification

| Gate | Result |
| --- | --- |
| Consolidated US-48–US-52/telemetry/Trip/Routing/Notification/PostgreSQL selection | 488/488 PASS |
| Architecture and Spring Modulith | 59/59 PASS |
| Complete Maven `clean test` | 1,754/1,754 PASS; 0 failures/errors/skips; BUILD SUCCESS in 10:36 |
| Focused US-52 Vitest | 10/10 PASS |
| Complete Vitest | 319/319 PASS across 77 files |
| TypeScript | PASS |
| Production frontend build | PASS; existing bundle-size advisory only |
| Real PostgreSQL Chromium | US-49 6/6, US-50 10/10, US-52 10/10; total 26/26 PASS |
| Checkstyle | PASS; zero configured violations |
| PMD | PASS when run independently; the first concurrent static run raced on Maven output and was discarded |
| SpotBugs | PASS; zero findings |
| Dependency analysis | BUILD SUCCESS; established broad-declaration warnings remain |
| Docker Compose/listener verification | PASS; internal and external bidirectional Kafka messages delivered |
| `git diff --check` | PASS |

All accepted database and Chromium evidence used `transport_logistics_acceptance`. No destructive acceptance operation used the development database.

## Physical and operational acceptance boundary

Technical closure does not prove real-world coordinate/accuracy fidelity. Final acceptance requires one real GPS/telematics device and provider path, a safe controlled field route with on-route and deviation points, preservation of genuine source time/WGS84 accuracy, recovery/clearance, a real same-Tenant operational recipient, limited-role/Tenant denial, privacy checks and operator sign-off. A real mobile device is required only if chosen as the physical telemetry source; no separate native mobile application is required. Production deployment itself is not required, but production-representative notification recipient behavior must be witnessed.

## Remaining gaps, rollback and classification

No known production technical defect or source contradiction remains. Residual risks are physical GPS accuracy/provider behavior, production-scale capacity beyond local evidence, and maintenance-window duration. Rollback is by application rollback while retaining immutable V85–V92; any schema reversal requires a separately governed forward migration. Events and evidence are append-only and must not be rewritten.

Final classification is `TECHNICALLY_COMPLETE / ACCEPTANCE_PENDING`. Accounting remains 73/87. The exact next governed task is:

`US-52-MONITOR-ROUTE-DEVIATIONS-FINAL-ACCEPTANCE-001`
