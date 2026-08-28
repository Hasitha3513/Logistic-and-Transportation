# Transport & Logistics — Current Verified MVP Baseline

**Audit:** `MVP-CONTINUE-REBASELINE-003`

**Date:** 2026-08-27

**Branch:** `feat/mvp-1.2-fuel-closure`

**Commit:** `14cfe1bacacaa1380207f59ceda21131cd76d52a`

**Initial worktree:** Clean

## Release dashboard

| Release band | Complete | Partial | Blocked | Deferred | Missing | Status |
|---|---:|---:|---:|---:|---:|---|
| MVP 1.0 Core | 34 / 34 | 0 | 0 | 0 | 0 | COMPLETE |
| MVP 1.1 Advanced Route | 4 / 4 | 0 | 0 | 0 | 0 | COMPLETE |
| MVP 1.1 Freight | 7 / 7 | 0 | 0 | 0 | 0 | COMPLETE |
| MVP 1.2 Fuel | 5 / 8 | 0 | 0 | 3 / 8 | 0 | CLOSED_WITH_APPROVED_DEFERMENTS |

## Repository baseline

- Backend modules: `fleet`, `freight`, `fuel`, `identity`, `notification`, `offlinesync`, `organization`, `reporting`, `routing`, `shared`, `system`, `tenancy`, `trip`.
- Driver is a Fleet sub-feature. Maintenance is a Fleet scheduling/availability sub-feature. Work orders, job cards, parts inventory, inspections, tyres, batteries, tracking and delivery are not implemented product capabilities.
- Java 21; Spring Boot 3.2.12; Spring Modulith 1.2.12; Maven 3.9.9; Spring Security/JWT; JPA; Flyway; PostgreSQL; JUnit 5, Mockito and Testcontainers.
- React 19.1.1; TypeScript 5.8.3; Vite 7.3.6; Ant Design 5.27.1; TanStack Query 5.85.5; React Hook Form 7.62.0; Zod 4.1.5; Axios 1.11.0; Vitest 3.2.7; Playwright 1.62.1.
- Flyway: 45 migrations, `V1__baseline.sql` through `V45__freight_reporting_permissions.sql`; no gaps, duplicate versions or out-of-order versions found.
- First-class tenant foundation and current-scope operational isolation are implemented: canonical CLTS-LK, tenant-membership-scoped roles, server-side resolution, request-bounded context, operational tenant discriminators, and tenant-scoped reporting sources.

## MVP 1.0 reconfirmation

| Area | Stories | Result |
|---|---|---:|
| Fleet | US-01–08 | 8 / 8 COMPLETE |
| Trip | US-09–16 | 8 / 8 COMPLETE |
| Basic Route | US-17–19 | 3 / 3 COMPLETE |
| Driver | US-39–45 | 7 / 7 COMPLETE |
| Enablers | US-71, 74, 75, 77, 79, 80, 81, 83 | 8 / 8 COMPLETE |

Production, migrations, API/UI/RBAC assets, focused tests and fresh full regression preserve the accepted 34-story core. Later Freight gaps do not downgrade this release.

## MVP 1.1 Advanced Route

US-20, US-21, US-22 and US-23 remain COMPLETE. Current routing domain/application/adapters, V30, UI components, RBAC and `routeIntelligence.spec.ts` remain present; fresh regressions passed.

## MVP 1.1 Freight

| Story | Status | Current evidence and gap |
|---|---|---|
| US-24 Freight Orders | COMPLETE | Domain/application/persistence/web/UI/RBAC, V31 and dedicated tests/E2E exist. |
| US-25 Cargo Manifest | COMPLETE | V32/V37/V42, nullable tri-state classifications, physical measurements, UNKNOWN finalization rule, explicit UI selection and dedicated tests/E2E exist. |
| US-26 Load Planning | COMPLETE | V34/V38, DRAFT/STRUCTURALLY_READY, ready command, material invalidation, notes preservation, structured rules, optimistic 409 behavior and 8 logical E2E cases exist. |
| US-27 Validate Weight and Volume | COMPLETE | V39/V42, pure calculation engine integrated with Cargo Manifest measurements, supporting verified PASS, FAIL (payload, volume, GVW), and INCOMPLETE diagnostics across unit, integration, and Playwright suites. |
| US-28 Freight Insurance | COMPLETE | Policy, claim, settlement and dispute workflows, V35/V36, UI/RBAC and dedicated tests/E2E exist. |
| US-29 Freight Reports | COMPLETE | Tenant-scoped summary, shipment/capacity utilization, insurance/claim/settlement/exception aggregation, honest INCOMPLETE semantics, bounded CSV, dedicated RBAC, React page, and tenant-isolation regression coverage. |
| US-30 Cargo Exceptions | COMPLETE | Aggregate, six types (DAMAGE, PARTIAL_SHIPMENT, WEIGHT_DISCREPANCY, HAZARDOUS_MATERIAL, UNMANIFESTED_CARGO, SEAL_TAMPERING), V40/V41, command-driven lifecycle (OPEN/HELD/ESCALATED/RESOLVED/REJECTED), immutable history, optimistic concurrency, API (8 endpoints), RBAC, React frontend, 40 backend tests and 8 Playwright E2E all PASS. Closed by P2-CARGO-EXCEPTION-001. |

`MVP-1.1-FREIGHT-CLOSURE-001` was executed, but its conclusion is superseded. MVP 1.1 is **PARTIAL**, not `CLOSED_WITH_BLOCKED_DEFERMENT`.

## MVP 1.2 Fuel

- COMPLETE: US-31, US-32, US-33, US-34, US-36.
- DEFERRED: US-35, US-37, US-38.
- `MVP-1.2-FUEL-CLOSURE-001` remains supported: **CLOSED_WITH_APPROVED_DEFERMENTS**.

## Post-MVP and additional scope

US-46, US-47, US-48–70 and remaining non-MVP platform stories are DEFERRED. Fleet maintenance schedules are only the MVP availability linkage. A Maintenance/Work Order/Inventory/Inspection product is not implemented or promoted into an authoritative numbered release band. The former direct “MVP 1.3” maintenance recommendation is withdrawn.

## Verification

- `mvn test` with Oracle JDK 21: PASS — 951 run, 0 failed, 0 errors, 22 skipped.
- `./mvnw verify` with full Oracle JDK 21: PASS — packaged successfully after the same suite.
- Architecture: 25 / 25 PASS (`ApplicationModulesTest` 2, `HexagonalLayerArchitectureTest` 15, `ModuleBoundaryArchitectureTest` 5, `LombokUsageArchitectureTest` 3).
- Frontend lint: PASS, 0 errors/warnings. Vitest: 46 files, 229 / 229 PASS (full suite plus US-29 focused suite). TypeScript/Vite build: PASS with a non-blocking chunk-size warning.
- US-29 Chromium E2E: 1 / 1 PASS; source metrics, shipment row, INCOMPLETE state, and permission-gated export are covered.
- Playwright inventory: 43 specs and 118 logical tests. Chromium: 118 / 118 PASS. Firefox: 117 / 118 passed in the full run; `E2E-NOT-011` failed once because an Ant Select option was not observed, then passed on an isolated rerun (flaky evidence, no automatic retry). WebKit: environment-blocked before test execution because host library `libavif16` is missing; a three-test smoke attempt produced three launch errors.

## Requirement / implementation drift

1. US-27’s acceptance and closure documents overstate completion: the engine is not fed production cargo measurements.
2. US-30’s local workflow exists, but the frozen US-27, correction/claim and Trip readiness branches are absent.
3. `MVP-1.1-FREIGHT-CLOSURE-001.md`, `US27-WEIGHT-VOLUME-ACCEPTANCE-003.md` and `PHASE2-SCOPE-MATRIX.md` contain stale COMPLETE conclusions.
4. Bunker transfer/concurrency and route heuristic capabilities exceed the smallest MVP need.
5. No deferred tenant, fuel-card, fuel-analytics, fuel-theft, GPS, delivery or last-mile implementation was found.

## Release readiness

| Band | Developer Demo | Internal QA | UAT | Pilot | Production |
|---|---|---|---|---|---|
| MVP 1.0 | READY | READY | READY_WITH_CONDITIONS | READY_WITH_CONDITIONS | READY_WITH_CONDITIONS |
| MVP 1.1 | READY_WITH_CONDITIONS | NOT_READY | NOT_READY | NOT_READY | NOT_READY |
| MVP 1.2 Fuel Core | READY | READY | READY_WITH_CONDITIONS | READY_WITH_CONDITIONS | READY_WITH_CONDITIONS |

Conditions include environment-specific PostgreSQL, security, operations and tenant-model review; production deployment was not validated.

## Current development position and exact next task

- Release band: MVP 1.1 (Route & Freight)
- Domain: Freight
- Scope: Phase 2B (US-24 to US-30)
- Last completed task: `TENANT-FOUNDATION-IMPLEMENTATION-001` (first-class tenant, membership, trusted resolution/context, and clean CLTS-LK initialization)
- Status: 7 / 7 Freight Stories COMPLETE; Overall Phase 2B = COMPLETE

**Exact next task:** MVP 1.3 expansion planning.
