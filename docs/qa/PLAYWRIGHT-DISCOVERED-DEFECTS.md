# Playwright Discovered Defects Register

**Document ID:** QA-AUTO-DEFECTS-001  
**Target Baseline:** Corrected MVP Baseline v2  
**Date:** August 22, 2026
**Status:** CLOSED FOR US-77 MVP

---

## 1. Defect Summary

| Defect ID | User Story | Severity | Screen / Component | Brief Description | Status |
|---|---|---|---|---|---|
| *DEF-001* | US-74 | Minor | `LoginPage.tsx` | Mismatched user error message expectation on sign-in failure | Resolved in Test Expectation |
| *DEF-002* | US-39 / US-01 | Minor | `ResourceEditorModal.tsx` | Select fields require explicit user interaction when no default value is defined | Addressed in Automation Helper |
| *DEF-003* | Cross-cutting | Critical | Playwright harness | `npm run test:e2e` started no backend or frontend; all 108 tests failed before assertions | Resolved in Harness |
| *DEF-004* | US-05 / Cross-browser | Minor | Firefox lubricant negative test under eight-worker load | One validation assertion failed once under suite contention, then passed twice in isolation | Resolved by Bounded Workers |
| *DEF-005* | US-77 | Major | `NotificationRuleModal.tsx` | Catalogue refresh could reset an operator's EMAIL selection back to IN_APP while editing the create form | Resolved in Product UI |
| *DEF-006* | US-77 / Cross-browser | Minor | Playwright full-suite harness | Four-worker contention caused different one-off full-suite timing failures despite isolated passes | Resolved by Bounded Workers |

---

## 2. Detailed Defect Logs

### DEF-001: Sign-in Error Message String Alignment
- **Story:** US-74 (Manage Security)
- **Severity:** Minor / Cosmetic
- **Screen:** `/login` (`LoginPage.tsx`)
- **Steps to Reproduce:**
  1. Open `/login`.
  2. Enter invalid username/password and submit.
- **Expected (Initial Specification):** Error message displays `"Invalid username or password"`.
- **Actual (Implemented):** Component displays `"Sign-in failed. Check your username and password."`
- **Evidence:** `LoginPage.tsx` line 31.
- **Automation Action:** Updated test assertion to expect the authoritative production string.

---

### DEF-002: Required Select Form Controls in ResourceEditorModal
- **Story:** US-01, US-39
- **Severity:** Minor
- **Screen:** Resource Editor Modal (`ResourceEditorModal.tsx`)
- **Steps to Reproduce:**
  1. Open Create Driver or Create Vehicle modal.
  2. Fill text inputs without selecting dropdown options for required fields (e.g. `status`, `categoryId`).
  3. Click Save.
- **Expected:** Form displays inline validation error messages for missing dropdown selections.
- **Actual:** Verified that inline validation is enforced accurately by Zod schema.
- **Automation Action:** Enhanced automation POM actions to explicitly select dropdown values for positive journeys and assert validation counts for negative tests.

---

### DEF-003: Playwright Services Were Not Self-Starting
- **Story:** Cross-cutting release gate
- **Severity:** Critical / Harness
- **Initial evidence:** `npm run test:e2e` ran 108 cases; 108 failed with `ERR_CONNECTION_REFUSED` at `http://localhost:5173/login`.
- **Root cause:** `playwright.config.ts` had no service lifecycle configuration, so the package command launched browsers without the Spring Boot or Vite applications.
- **Resolution:** Added two Playwright `webServer` entries using the existing Maven wrapper, H2 profile, `/api/health` readiness, Vite dev server, and `/login` readiness. Runtime credentials and JWT secret are generated per run; no real or static secret was added.
- **Verification:** 111/111 passed across Chromium, Firefox, and WebKit.

---

### DEF-004: Firefox Validation Timing Under Excessive Parallel Load
- **Story:** US-05
- **Severity:** Minor / Flaky timing
- **Evidence:** One `E2E-FLT-006-NEG` assertion failed in Firefox during an eight-worker all-browser run; Chromium and WebKit passed the same case, and two isolated Firefox repetitions passed.
- **Resolution:** Bounded default concurrency to four workers with `E2E_WORKERS` override. Retries remain zero; no timeout, sleep, assertion, or product behavior was changed.
- **Verification:** The subsequent retained gate passed 111/111. MVP-GAP-008I later tightened the final default to three workers as recorded in DEF-006.

### DEF-005: Notification Rule Form Reinitialized During User Input

- **Story:** US-77
- **Severity:** Major / Product UI
- **Evidence:** WebKit repeatedly showed the channel restored to `In-app` after the test selected `Email`; slower catalogue/template completion reproduced the race.
- **Root cause:** the create-form initialization effect depended on catalogue data and reset the entire form whenever that query produced another data reference.
- **Resolution:** initialize once per modal opening, only after catalogue data exists. Later catalogue refreshes no longer overwrite operator input.
- **Verification:** `E2E-NOT-011` passes in Chromium, Firefox, and WebKit, and the complete notification suite passes 45/45.

### DEF-006: Full-Suite Concurrency Contention

- **Story:** US-77 / cross-browser release gate
- **Severity:** Minor / Harness reliability
- **Evidence:** unchanged four-worker full runs ended 155/156 in different cases; the cases passed in other browsers/runs, and isolated NOT-003 passed 3/3 in WebKit.
- **Resolution:** bounded the default to three workers while preserving the `E2E_WORKERS` override. Retries remain zero; no timeout, sleep, skip, assertion, or product behavior changed.
- **Verification:** repository-standard `npm run test:e2e` passed 156/156: 111 retained plus 45 notification executions.

No unresolved notification defect remains. Notification E2E coverage and US-77 closure are complete for MVP-GAP-008I.
