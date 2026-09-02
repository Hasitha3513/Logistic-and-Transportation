# US-68 Last-Mile Exceptions — Technical Implementation Closure

**Task ID:** `MVP-1.4-US68-LAST-MILE-EXCEPTIONS-IMPLEMENTATION-001`  
**Date:** 2026-09-02  
**Status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**US-68 migration:** None  
**Current Flyway head:** `V57__tenant_local_business_keys.sql`

## Implemented model

US-68 adds a read-only Last-Mile Planner projection to the Delivery Order details workflow. `LastMilePlannerService` composes existing Delivery-owned order, failed-attempt, escalation, and specialized-exception use cases. It returns counts and links the operator to the existing owning workflows; it does not execute those commands.

The Planner does not create a `DeliveryAttempt` or `DeliveryExceptionCase`, schedule redelivery, assign a Rider, mutate a Batch, calculate ETA, send notifications, or persist US-68 state. No new aggregate, table, repository, permission family, event, migration, or duplicate exception API family was introduced. US-69 notifications, US-70 self-service, and US-78 global exceptions remain out of scope.

The browser/API contract is `GET /api/v1/deliveries/{id}/last-mile-planner`. It accepts either `DELIVERY_FAIL_VIEW` or `DELIVERY_EXCEPTION_VIEW`. Tenant identity remains server-derived and the composed repository/use-case reads retain their tenant-scoped behavior.

## Security reconciliation

Spring Security matches the request path without the configured `/api` servlet context. The effective matcher `GET /v1/deliveries/*/last-mile-planner` therefore precedes the broad Delivery read matcher, while a regression matcher and tests retain the literal `/api/v1/...` URL contract. This prevents the broad `DELIVERY_VIEW` rule from overriding the Planner's frozen permission contract.

The shared Delivery security matrix passed **39/39** with zero failures or errors. It covers the literal Planner URL for denial without either Planner authority and passage through route security with each allowed authority. Real Chromium evidence confirms authenticated success, unauthenticated `401`, insufficient-permission `403`, Tenant-B safe `404`, and ineffective `X-Tenant-Id` spoofing.

## Verification evidence

| Gate | Actual result |
| :--- | :--- |
| Shared Delivery security regression | 39 tests, 0 failures, 0 errors — PASS |
| Real US-68 Chromium | 3/3 PASS — populated UI projection, real API response, and security/tenant isolation |
| Real US-67 Chromium regression | 6/6 PASS in 21.1s — selector scoped to `#delivery-eta`; no product behavior change |
| Complete `./mvnw verify` | 1,200 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 4:01 |
| Architecture and Spring Modulith | 45/45 PASS |
| Checkstyle | PASS; 0 violations |
| PMD | PASS |
| SpotBugs | PASS; 0 bugs/errors |
| TypeScript | PASS |
| Vitest | 57 files / 254 tests PASS |
| Production build | PASS |
| Changed-file ESLint | PASS; US-68 introduced errors: 0 |
| Global ESLint | `BASELINE_DEBT`: 71 errors, 0 warnings in eight pre-existing Delivery analytics/batches/exceptions/riders/slots/zones files |
| Flyway | V1–V57 validated/applied; no V58; US-68 migration is none |
| `git diff --check` | PASS |

Both real suites use Playwright-managed backend/frontend sessions and the isolated `transport_logistics_acceptance` datasource. They do not mock authentication, Rider, Delivery Order, Batch, ETA GET, or ETA calculate core APIs.

## PostgreSQL datasource safety inventory

All destructive PostgreSQL-capable acceptance classes were re-audited. The valid verification datasource was exclusively `jdbc:postgresql://127.0.0.1:5433/transport_logistics_acceptance`; the development datasource is excluded from valid evidence.

| Classification | Count |
| :--- | ---: |
| `SAFE_SHARED` — guarded local/Testcontainers resolver | 9 |
| `SAFE_EXPLICIT` | 0 |
| `TESTCONTAINERS_ONLY_EXCLUDED_IN_LOCAL_MODE` | 0 |
| `UNSAFE_DEVELOPMENT_DATASOURCE` | 0 |
| `UNKNOWN` | 0 |

The nine shared paths are Delivery analytics, number, order, and redelivery acceptance; bunker concurrency; offline-sync invariant; PostgreSQL production invariant; local sample-data bootstrap; and sample-data RBAC idempotency.

## Read-path and performance audit

The Planner executes four bounded calls: one Order lookup plus existing attempt-history, escalation, and exception-case reads. It performs no loop-driven downstream query, duplicate call, full-table query, heavy aggregate graph load, or ETA recalculation. The implementation is `READ_ONLY / PASS`.

## Status and next task

US-68 technical implementation is complete but has not received final acceptance.

- US-68: **`IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`**
- MVP 1.4: **5 / 8 COMPLETE**
- Overall: **62 / 87 COMPLETE**
- Deferred: **25 / 87**
- Next task: `MVP-1.4-US68-LAST-MILE-EXCEPTIONS-FINAL-ACCEPTANCE-001`
