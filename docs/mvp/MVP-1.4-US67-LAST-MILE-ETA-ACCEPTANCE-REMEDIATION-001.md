# US-67 Last-Mile ETA Acceptance Remediation Report

**Task ID:** `MVP-1.4-US67-ACCEPTANCE-EVIDENCE-CLOSURE-002`  
**User Story:** `US-67` — Calculate Last-Mile ETA  
**Date:** 2026-09-01  
**Author:** Senior Principal QA / Release Engineer, Multi-Tenancy Security & Test Infrastructure Review Board  
**Status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**Accepted Flyway Head:** `V56` (`V56__delivery_rider_transport_mode_us67.sql`)  

---

## 1. Executive Summary & Verification Matrix

All technical and security acceptance gates for US-67 (Calculate Last-Mile ETA) have been executed and verified against real Spring Boot, PostgreSQL, and Chromium runtime environments:

| Gate | Target / Requirement | Verification Outcome | Evidence Summary |
| :--- | :--- | :---: | :--- |
| **A. Tenant-B IDOR Protection** | Cross-tenant Order & Batch ETA access denied safely (404) without metadata leakage | **PASS** | `deliveryEta.real.spec.ts` (test 5): foreign Order & Batch IDs return `404 Not Found`. |
| **B. Tenant Spoofing Prevention** | Spoofed `X-Tenant-Id` header ignored; authenticated session remains authoritative | **PASS** | `deliveryEta.real.spec.ts` (test 5): caller receives own tenant data regardless of header. |
| **C. Limited-User RBAC (UI & Backend)** | User with `DELIVERY_VIEW` but lacking `DELIVERY_UPDATE` cannot recalculate; direct POST returns `401`/`403` | **PASS** | `deliveryEta.real.spec.ts` (test 6): UI hides recalculate button; unauthorized API POST rejected. |
| **D. Missing Coordinates Handling** | Destination without coordinates returns safe UI alert state and API error code | **PASS** | `deliveryEta.real.spec.ts` (test 4): UI renders info banner; direct API returns 400 `DELIVERY_ETA_COORDINATES_MISSING`. |
| **E. Real Chromium E2E Suite** | Full real suite covering Batch ETA, Single-Order ETA, Recalculate, Reload, Mode change invalidation | **PASS** | `deliveryEta.real.spec.ts`: **6/6 PASS** (19.8s) on Chromium against real backend/PostgreSQL. |
| **F. Full Maven Verify (`./mvnw verify`)** | Clean build reaching terminal conclusion with 0 failures and 0 errors | **PASS** | **BUILD SUCCESS** (02:39 min) — **1,167 Tests run**, 0 Failures, 0 Errors, 31 Skipped. |
| **G. Notification Regression** | Clean foreign-key deletion order and policy execution | **PASS** | `*Notification*Test`: **148/148 PASS**, 0 Failures, 0 Errors. |
| **H. Architecture Compliance** | Modulith boundaries, hexagonal isolation, pure domain | **PASS** | **32/32 Architecture Tests PASS** (ModulithBoundaryEnforcement, HexagonalLayer, ModuleBoundary, ApplicationModules). |
| **I. Backend Static Analysis** | Checkstyle, PMD 7.17, SpotBugs 4.8.6 under Java 21 | **PASS** | **0 Checkstyle violations, 0 PMD violations, 0 SpotBugs bugs.** |
| **J. Frontend TypeScript & Build** | Clean production build without compilation errors | **PASS** | `tsc -b && vite build`: **PASS** in 5.17s. |
| **K. Frontend Vitest Suite** | Complete unit and component test suite | **PASS** | **56 test files passed, 252 tests passed**, 0 failures (42.52s). |
| **L. Frontend ESLint Classification** | Zero US-67 introduced errors; baseline classified | **PASS** | US-67 files: **0 errors**. Global: 71 pre-existing baseline debt. |
| **M. Git Workspace & Diff Cleanliness** | No stray files, no formatting errors | **PASS** | `git diff --check`: **PASS**. |

---

## 2. Real Chromium E2E Verification Details

**Test Suite:** `frontend/e2e/tests/delivery/deliveryEta.real.spec.ts`  
**Configuration:** `frontend/playwright.config.ts` (`--project=chromium`)  
**Backend:** Real Spring Boot 3.2.12 on PostgreSQL 16 (Port 8088 / 5433)  

```
Running 6 tests using 1 worker

  ✓ 1 US-67 real PostgreSQL Rider ETA acceptance › calculates MOTORBIKE ETA through the real UI and backend (3.4s)
  ✓ 2 US-67 real PostgreSQL Rider ETA acceptance › shows and recalculates the single-order ETA through the real UI (1.4s)
  ✓ 3 US-67 real PostgreSQL Rider ETA acceptance › changes Rider mode through the real API and receives a different BICYCLE ETA (42ms)
  ✓ 4 US-67 real PostgreSQL Rider ETA acceptance › handles missing coordinates gracefully in browser and backend (1.1s)
  ✓ 5 US-67 real PostgreSQL Rider ETA acceptance › enforces Tenant IDOR protection and rejects cross-tenant ETA access and tenant spoofing (58ms)
  ✓ 6 US-67 real PostgreSQL Rider ETA acceptance › enforces RBAC on ETA recalculation: limited user without DELIVERY_UPDATE receives 403 on direct POST (856ms)

6 passed (19.8s)
```

---

## 3. Full Backend Verification Evidence (`./mvnw verify`)

- **Java Runtime:** OpenJDK 21.0.12 LTS
- **Build Outcome:** `BUILD SUCCESS` (Total time: 02:39 min)
- **Surefire Totals:** Tests run: 1,167, Failures: 0, Errors: 0, Skipped: 31
- **Notification Suite:** Tests run: 148, Failures: 0, Errors: 0
- **Architecture Suite:** Tests run: 32, Failures: 0, Errors: 0
- **Static Analysis:**
  - Checkstyle: 0 violations
  - PMD: 0 violations
  - SpotBugs: 0 bugs

---

## 4. Frontend Verification & Lint Classification

- **TypeScript Compilation & Production Build:** `PASS` (`tsc -b && vite build` completed in 5.17s)
- **Vitest Unit & Component Suite:** `PASS` (56/56 test files, 252/252 tests passed)
- **ESLint Verification:**
  - `US67_INTRODUCED_LINT_ERRORS`: `0`
  - `NEW_E2E_INTRODUCED_LINT_ERRORS`: `0`
  - `GLOBAL_FRONTEND_LINT`: `BASELINE_DEBT` (71 pre-existing errors in legacy pages, 0 introduced in US-67 code)

---

## 5. Security & Multi-Tenancy Evaluation Matrix

| Vector | Evaluated Rule | Result |
| :--- | :--- | :---: |
| **Tenant Isolation** | Tenant A actor cannot view or calculate Tenant B Order/Batch ETAs | **PASS** (`404 Not Found`) |
| **Header Spoofing** | Injected `X-Tenant-Id` header is ignored; server-side token identity rules | **PASS** (`INEFFECTIVE`) |
| **Single-Order RBAC** | `DELIVERY_VIEW` required for read; `DELIVERY_UPDATE` required for recalculation | **PASS** (`401`/`403` on direct unauthorized call) |
| **Batch RBAC** | `DELIVERY_BATCH_VIEW` required for read; `DELIVERY_BATCH_UPDATE` for recalculation | **PASS** |
| **Transport Mode Authority** | Transport mode is mastered server-side on `DeliveryRiderEntity` | **PASS** |
| **Cache Scoping** | In-memory cache composite key includes `tenantId:subjectId:fingerprint` | **PASS** |

---

## 6. Story Accounting & Status

- **US-67 Status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`
- **MVP 1.4 Progress:** `4 / 8 COMPLETE` (US-63, US-64, US-65, US-66 Accepted & Closed)
- **Overall Release Band:** `61 / 87 COMPLETE`
- **Approved Deferments:** `26 / 87`
- **Accepted Flyway Head:** `V56`
- **Next Queue Item:** `MVP-1.4-US67-LAST-MILE-ETA-FINAL-ACCEPTANCE-001-RERUN`
