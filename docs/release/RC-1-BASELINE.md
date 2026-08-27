# Phase-1 Release Candidate 1 Baseline (RC-1-BASELINE)

**Release:** `RC-1`  
**Product:** Transport & Logistics Management System  
**Phase:** Phase-1 MVP  
**Generation Date:** August 24, 2026  
**Author:** Senior Principal Software Architect, Release Engineer & QA Lead  
**Source Branch:** `feature/mvp-gap-011i-us71-closure`  
**Source Commit:** `361707f9258d7d875020e4629e528b9cf709096c` (with pending baseline commit)  
**Release Decision:** **`READY_FOR_RC`**  

---

## 1. Executive Baseline Summary

| Baseline Dimension | Certified Outcome |
|---|---|
| **Phase-1 Functional MVP Scope** | **39 / 39 Stories Complete (100.0%)** |
| **UI Hardening Suite** | **`UI-HARDEN-001` through `UI-HARDEN-005` COMPLETE & CERTIFIED** |
| **Backend Framework & Target** | **Java 21 bytecode target, Spring Boot 3.2.12, Spring Modulith 1.2.12** |
| **Backend Test Suite** | **718 Tests Run, 0 Failures, 0 Errors, 22 Conditionals Skipped** |
| **Architecture Verification** | **16 / 16 ArchUnit Tests Pass (Hexagonal layers, Modulith boundaries, Lombok discipline)** |
| **Database Schema Baseline** | **PostgreSQL / Flyway V1 through V29 IMMUTABLE & VALIDATED** |
| **Frontend Framework & Build** | **React 19, TypeScript 5.8, Ant Design 5, Vite 7 (0 ESLint errors/warnings, Clean Production Build)** |
| **Frontend Unit & Integration Tests** | **35 Test Files, 178 / 178 Vitest Tests Pass** |
| **Browser Automation Matrix** | **210 / 210 Playwright Tests Pass (Chromium: 70/70, Firefox: 70/70, WebKit: 70/70)** |
| **Deterministic Session Logout (`RC-HARDEN-016`)** | **15 / 15 Pass across 5x Repeat Runs on Chromium, Firefox, WebKit** |

---

## 2. Release Candidate Content Breakdown

### 2.1 UI Hardening Verification
- **UI-HARDEN-001 (Global Layout Ownership):** Verified single ownership of breadcrumb, top header, and page title in `AppLayout.tsx`.
- **UI-HARDEN-002 (Page Padding Normalization):** Verified elimination of nested outer wrappers and redundant page margins.
- **UI-HARDEN-003 (Notification Rules Flattening):** Verified simplified hierarchy in `NotificationRulesPage.tsx` without wrapper card nesting.
- **UI-HARDEN-004 (Standardized Filter Bars):** Verified consistent filter bar density, responsive toolbar layouts, and unified search controls.
- **UI-HARDEN-005 (Standardized Form UX):** Verified single-action Save/Cancel ownership, clean validation error summaries, and elimination of duplicate drawer footers.

### 2.2 Cross-Browser Session Determinism
- **RC-HARDEN-016 (Deterministic Logout):** Fixed WebKit and Firefox dropdown hit-testing and asynchronous token evacuation timing races. Verified 100% pass rate in 15 repeat executions.

### 2.3 SMTP Worker Error Classification
- **SMTP Retry Policy:** Corrected `SmtpEmailNotificationSenderAdapter.java` so temporary 4xx status codes are prioritized as retryable `PROVIDER_5XX` prior to evaluating permanent failure heuristics.

---

## 3. Operational Backlog (Not Blocking RC-1)

| Item ID | Category | Severity | Status | Scope Description |
|---|---|---|---|---|
| **RC-R05** | Production Environmental Hardening | P1 | **OPEN / OPERATIONAL BACKLOG** | Multi-stage production container user hardening (non-root execution), CI/CD runner secrets injection, and staging PostgreSQL Testcontainers execution in continuous integration environments. |

---

## 4. Version and Tagging Strategy

- **Current Repository Version:** `1.0.0-SNAPSHOT` (`pom.xml`), `0.1.0` (`frontend/package.json`)
- **Recommended Release Candidate Version:** `1.0.0-RC1`
- **Recommended Tag:** `v1.0.0-rc.1`
- **Target Integration Branch:** `master` (or `main`)
- **Recommended Phase-2 Development Branch:** `feature/phase-2-freight-cargo`

---

## 5. Release Command Plan (For User Execution After Review)

```bash
# 1. Stage and commit the certified RC-1 baseline
git add docs/ frontend/ src/ AGENTS.md
git commit -m "chore(release): freeze and certify Phase-1 RC-1 baseline"

# 2. Integrate into master branch
git switch master
git merge --no-ff feature/mvp-gap-011i-us71-closure -m "merge: Phase-1 RC-1 release baseline"

# 3. Tag Release Candidate 1
git tag -a v1.0.0-rc.1 -m "Transport & Logistics Management System — Phase-1 RC-1"

# 4. Push to remote repository
git push origin master
git push origin v1.0.0-rc.1

# 5. Create clean Phase-2 development branch from the certified baseline
git checkout -b feature/phase-2-freight-cargo
```
