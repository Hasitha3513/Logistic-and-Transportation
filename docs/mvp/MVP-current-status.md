# Transport & Logistics — Current Verified MVP Baseline

**Audit:** `MVP-CURRENT-STATUS-COMPARE-AND-RESUME-001`

**Date:** 2026-08-29

**Branch:** `feat/us56-delivery-orders-acceptance-hardening`

**Accepted application commit:** `40eb120ac64cce44716598d267c68901127dd44a` (`fix(delivery): harden US-56 acceptance behavior`), remotely verified.

**Worktree:** closure documentation in progress; pre-existing untracked `docs/requirements/` preserved.

## Release dashboard

| Release band | Complete | Partial | Blocked | Deferred | Missing | Status |
|---|---:|---:|---:|---:|---:|---|
| MVP 1.0 Core | 34 / 34 | 0 | 0 | 0 | 0 | COMPLETE |
| MVP 1.1 Advanced Route | 4 / 4 | 0 | 0 | 0 | 0 | COMPLETE |
| MVP 1.1 Freight | 7 / 7 | 0 | 0 | 0 | 0 | COMPLETE |
| MVP 1.2 Fuel | 5 / 8 | 0 | 0 | 3 / 8 | 0 | CLOSED_WITH_APPROVED_DEFERMENTS |
| MVP 1.3 Delivery Operations | 4 / 7 | 0 | 0 | 0 | 3 / 7 | IN_PROGRESS; US-56, US-57, US-58, and US-59 COMPLETE |

US-56, US-57, US-58, and US-59 are accepted and complete with tenant-scoped domain, persistence, V46/V47/V48 migrations, API/RBAC, offline IndexedDB sync, failure tracking, and React workflows. US-60 through US-62 remain queued in MVP 1.3.

## Repository baseline

- Backend modules: `delivery`, `fleet`, `freight`, `fuel`, `identity`, `notification`, `offlinesync`, `organization`, `reporting`, `routing`, `shared`, `system`, `tenancy`, `trip`.
- Driver is a Fleet sub-feature. Maintenance is a Fleet scheduling/availability sub-feature. Work orders, job cards, parts inventory, inspections, tyres, batteries and tracking are not implemented product capabilities. Delivery implements US-56 only; US-57 through US-62 are not implemented.
- Java 21; Spring Boot 3.2.12; Spring Modulith 1.2.12; Maven 3.9.9; Spring Security/JWT; JPA; Flyway; PostgreSQL; JUnit 5, Mockito and Testcontainers.
- React 19.1.1; TypeScript 5.8.3; Vite 7.3.6; Ant Design 5.27.1; TanStack Query 5.85.5; React Hook Form 7.62.0; Zod 4.1.5; Axios 1.11.0; Vitest 3.2.7; Playwright 1.62.1.
- Flyway: 46 migrations, `V1__baseline.sql` through `V46__delivery_order_us56.sql`; no gaps, duplicate versions or out-of-order versions found.
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

The conditional conclusion in `MVP-1.1-FREIGHT-CLOSURE-001` is a preserved historical record and is superseded by `MVP-1.1-FREIGHT-FINAL-CLOSURE-001`. MVP 1.1 Route & Freight is **COMPLETE**.

## MVP 1.2 Fuel

- COMPLETE: US-31, US-32, US-33, US-34, US-36.
- DEFERRED: US-35, US-37, US-38.
- `MVP-1.2-FUEL-CLOSURE-001` remains supported: **CLOSED_WITH_APPROVED_DEFERMENTS**.

## Post-MVP and additional scope

US-46, US-47, US-48–55, US-63–70 and remaining non-MVP platform stories are DEFERRED. US-56 through US-62 are selected for MVP 1.3 Delivery Operations and contract-frozen in `MVP-1.3-DELIVERY-OPERATIONS-CONTRACT-001.md`; US-56 is implemented and US-57 through US-62 remain not started. Fleet maintenance schedules are only the MVP availability linkage. A Maintenance/Work Order/Inventory/Inspection product is not implemented or promoted into an authoritative numbered release band. The former direct “MVP 1.3” maintenance recommendation is withdrawn.

## Verification

- Historical accepted backend baseline: 958 tests discovered, 0 failures, 0 errors, 22 skipped when run in a working agent-attachment environment.
- Fresh 2026-08-29 `./mvnw test`: `ENVIRONMENT_BLOCKED` — 958 discovered, 0 assertion failures, 654 Mockito/Byte Buddy agent-attachment setup errors, 22 skipped. Retrying with `-Djdk.attach.allowAttachSelf=true` produced the same environment failure; this is not classified as an application regression.
- Fresh focused verification: 30 / 30 PASS — architecture 28 / 28 (`ApplicationModulesTest` 3, `HexagonalLayerArchitectureTest` 16, `ModuleBoundaryArchitectureTest` 6, `LombokUsageArchitectureTest` 3) plus `DeliveryValueObjectTest` 2 / 2.
- Fresh frontend lint: PASS. Vitest: 47 files, 231 / 231 PASS. TypeScript and Vite production build: PASS with a non-blocking chunk-size warning.
- US-29 Chromium E2E: 1 / 1 PASS; source metrics, shipment row, INCOMPLETE state, and permission-gated export are covered.
- Playwright inventory: 43 specs and 118 logical tests. Chromium: 118 / 118 PASS. Firefox: 117 / 118 passed in the full run; `E2E-NOT-011` failed once because an Ant Select option was not observed, then passed on an isolated rerun (flaky evidence, no automatic retry). WebKit: environment-blocked before test execution because host library `libavif16` is missing; a three-test smoke attempt produced three launch errors.

## Requirement / implementation drift

1. Earlier Freight, tenant-foundation, and US-27 records retain their original point-in-time conclusions and now carry supersession notes where they otherwise conflict with the current release state.
2. Bunker transfer/concurrency and route heuristic capabilities exceed the smallest MVP need.
3. No deferred fuel-card, fuel-analytics, fuel-theft, GPS, delivery, or last-mile implementation was found.

## Release readiness

| Band | Developer Demo | Internal QA | UAT | Pilot | Production |
|---|---|---|---|---|---|
| MVP 1.0 | READY | READY | READY_WITH_CONDITIONS | READY_WITH_CONDITIONS | READY_WITH_CONDITIONS |
| MVP 1.1 | READY_WITH_CONDITIONS | NOT_READY | NOT_READY | NOT_READY | NOT_READY |
| MVP 1.2 Fuel Core | READY | READY | READY_WITH_CONDITIONS | READY_WITH_CONDITIONS | READY_WITH_CONDITIONS |

Conditions include environment-specific PostgreSQL, security, operations and tenant-model review; production deployment was not validated.

## Current development position and exact next task

- Release band: MVP 1.3 Delivery Operations
- Domain: Delivery
- Scope: US-56 through US-62
- Last completed acceptance task: `MVP-1.3-US57-PROOF-OF-DELIVERY-FINAL-ACCEPTANCE-001`
- Status: IN_PROGRESS; US-56 & US-57 `COMPLETE`; 2 / 7 accepted production stories

US-56 implements the frozen priority/service catalogues, `NONE_IN_US56`, `NO_ASSIGNMENT_COLUMNS_IN_US56`, DRAFT-to-READY readiness validation and material-edit invalidation.
US-57 implements online Proof of Delivery capture (signatures, photos, barcodes, geolocation), immutability, RBAC, and Delivery completion.

Delivery numbering implements `MVP-1.3-US56-DELIVERY-NUMBER-POLICY-001`: immutable server-generated `DEL-YYYY-NNNNNN`, per-Tenant/per-tenant-local-year atomic allocation, gaps allowed, no explicit US-56 idempotency key, and tenant-scoped uniqueness.

**Current blocker:** `NONE`.

**Final acceptance evidence:** Checkstyle 0, PMD 0, SpotBugs 0; frontend lint PASS, 49 files and 237/237 PASS, and production build PASS; Chromium Delivery & POD E2E 6/6 PASS; full backend `mvn test` 987 tests with 0 failures and 0 errors (26 skipped); PostgreSQL 16 Flyway V1–V47 PASS.

US-57 production implementation and acceptance is `COMPLETE`. MVP 1.3 is 2 / 7 complete and overall completion is 52 / 87.

**Exact next task:** `MVP-1.3-US58-OFFLINE-POD-PRODUCT-DECISIONS-001`.
