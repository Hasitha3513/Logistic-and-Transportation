# Playwright Discovered Defects Register

**Document ID:** QA-AUTO-DEFECTS-001  
**Target Baseline:** Corrected MVP Baseline v2  
**Date:** August 19, 2026  
**Status:** OPEN REGISTER  

---

## 1. Defect Summary

| Defect ID | User Story | Severity | Screen / Component | Brief Description | Status |
|---|---|---|---|---|---|
| *DEF-001* | US-74 | Minor | `LoginPage.tsx` | Mismatched user error message expectation on sign-in failure | Resolved in Test Expectation |
| *DEF-002* | US-39 / US-01 | Minor | `ResourceEditorModal.tsx` | Select fields require explicit user interaction when no default value is defined | Addressed in Automation Helper |

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
