# US-68 Last-Mile Exceptions — Final Acceptance

**Task ID:** `MVP-1.4-US68-LAST-MILE-EXCEPTIONS-FINAL-ACCEPTANCE-001`  
**Date:** 2026-09-02  
**Decision:** `PASS / COMPLETE`  
**Flyway head:** V57  
**US-68 migration:** None

## Independent decision

US-68 is accepted as a Delivery-owned, read-only Last-Mile Planner orchestration. The implementation projects existing operational facts and directs an operator to established owning workflows. It does not introduce or duplicate a last-mile exception aggregate, persistence model, permission family, command engine, or event family.

## Ownership and boundary findings

| Boundary | Acceptance finding |
| :--- | :--- |
| US-59 failed delivery | PASS — failed attempts, contact/payment/access outcomes, escalation, and return-to-base remain owned by the existing failed-delivery use case. |
| US-60 redelivery | PASS — scheduling and customer/slot preference rules remain in the existing redelivery use case. |
| US-62 specialized exceptions | PASS — wrong-address, damage, OTP mismatch, refusal, and partial-delivery investigation remain `DeliveryExceptionCase` behavior. |
| US-57 POD | PASS — the Planner neither writes nor finalizes proof. |
| US-65 Rider | PASS — the Planner does not assign/reassign or bypass Rider eligibility. |
| US-66 Batch | PASS — the Planner does not mutate membership, capacity, sequencing, or assignment. |
| US-67 ETA | PASS — the Planner contains no routing calculation, cache access, or invalidation behavior. |

`LastMilePlannerService` performs exactly four bounded Delivery-module reads: Order, attempt history, escalation history, and exception cases. Its focused test verifies those calls and no additional interactions. The frontend action buttons only scroll to an owning section or navigate to Rider/Batch pages.

## API, authorization, and tenancy

The only US-68 route is `GET /api/v1/deliveries/{id}/last-mile-planner`; no `/last-mile-exceptions/**` CRUD family exists. The controller and route configuration both require `DELIVERY_FAIL_VIEW` or `DELIVERY_EXCEPTION_VIEW`.

Because Spring Security matches without the `/api` servlet context, the effective `/v1/deliveries/*/last-mile-planner` matcher is placed before the broad Delivery matcher. Literal `/api/v1/...` regression tests prove that either approved authority passes route security and a user with neither receives `403`. Unauthenticated requests return `401`.

The Planner owns this external read contract and does not require the union of Rider, Batch, and ETA endpoint permissions. Its internal reads remain tenant-scoped. Real acceptance proves Tenant-A access, Tenant-B safe `404`, and ineffective client tenant-header spoofing. The response exposes only Delivery ID/status, aggregate counts, and action identifiers; it contains no access codes, credentials, medical/drug data, tenant secrets, live location, or unnecessary customer-sensitive data.

## Independent verification

| Gate | Result |
| :--- | :--- |
| Fresh real US-68 Chromium | 3/3 PASS in 16.3s against `transport_logistics_acceptance` |
| Planner service and literal-URL security tests | 5/5 PASS |
| Retained real US-67 Chromium regression | 6/6 PASS; ETA selector scoped to `#delivery-eta` only |
| Complete Maven evidence on the accepted source | 1,200 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 4:01 |
| Architecture / Spring Modulith refresh | 45/45 PASS |
| ARB P0-01 through P0-07 | PASS — module, table, dependency, tenant, authorization, aggregate, and transaction boundaries preserved |
| Checkstyle | PASS; 0 violations |
| PMD | PASS |
| SpotBugs | PASS; 0 bugs/errors |
| TypeScript | PASS |
| Vitest | 57 files / 254 tests PASS |
| Production build | PASS; existing bundle-size advisory only |
| Task and E2E ESLint | PASS; introduced errors 0 |
| Global frontend lint | `BASELINE_DEBT`: 71 pre-existing errors in unrelated unchanged Delivery files |
| `git diff --check` | PASS |

The Chromium suites use real React, REST, Spring Security, Delivery services, PostgreSQL, and Flyway. Authentication, Rider, Delivery Order, Batch, ETA reads, and ETA calculation are not mocked.

## Persistence, Flyway, and safety

- New aggregate/entity/repository/table/migration: **None**.
- New permission/event/API family: **None**.
- Flyway V1–V57: **PASS**; no V58 exists.
- Valid destructive acceptance datasource: `jdbc:postgresql://127.0.0.1:5433/transport_logistics_acceptance` only.
- PostgreSQL inventory: `SAFE_SHARED=9`, `SAFE_EXPLICIT=0`, `UNSAFE=0`, `UNKNOWN=0`.
- The development database is excluded from valid evidence.

## Performance and scope containment

The four bounded reads show no N+1 loop, duplicate downstream call, heavy aggregate graph, ETA calculation, or full-table query. No US-69 customer notification, US-70 self-service, US-78 global exception engine, route optimization, GPS, or telematics implementation leaked into US-68.

## Final state

- US-68: **COMPLETE**
- MVP 1.4: **6 / 8 COMPLETE**
- Overall: **63 / 87 COMPLETE**
- Deferred: **24 / 87**
- Next task: `MVP-1.4-US69-DELIVERY-NOTIFICATIONS-PRODUCT-DECISIONS-001`
