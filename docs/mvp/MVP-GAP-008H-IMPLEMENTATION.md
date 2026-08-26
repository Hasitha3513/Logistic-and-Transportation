# MVP-GAP-008H — Notification Playwright Coverage

**Story:** US-77 — Manage Notification Rules
**Status:** COMPLETE
**Date:** August 22, 2026

## Delivered coverage

The retained notification suite implements `E2E-NOT-001` through `E2E-NOT-015` and exercises the real Spring Boot H2 application and Vite UI. It covers rule access, create/edit/enable/disable, RBAC, operational Trip events, the notification center, unread/read/read-all behavior, EMAIL policy configuration, terminal failure, quiet hours, suppression audit, and deterministic retry success.

| Spec | Cases |
|---|---|
| `frontend/e2e/tests/notifications/notification-rules.spec.ts` | NOT-001..005 |
| `frontend/e2e/tests/notifications/notification-center.spec.ts` | NOT-006..010 |
| `frontend/e2e/tests/notifications/notification-policies.spec.ts` | NOT-011, NOT-013, NOT-014 |
| `frontend/e2e/tests/notifications/notification-delivery.spec.ts` | NOT-012, NOT-015 |

Notification-only verification passed 15/15 in each of Chromium, Firefox, and WebKit: 45 passed, 0 failed, 0 skipped.

## Deterministic E2E infrastructure

- The Playwright backend starts with `h2,e2e`; the normal production and local profiles do not expose the controls.
- `E2eAdjustableClock` permits deterministic time advancement for retry and quiet-hour boundaries.
- `E2eDeterministicEmailSender` provides explicit terminal-failure, transient-failure, and accepted-delivery outcomes without an external provider.
- `/e2e/notifications/**` is profile-restricted and requires `NOTIFICATION_RULE_MANAGE`.
- The EMAIL worker schedule is delayed in the E2E profile so tests invoke processing deterministically.
- Test actors, roles, rules, operational trips, event IDs, and recipients are unique per browser/test. Rule cleanup deletes unused rules and disables historically referenced rules so audit history is retained.
- No production endpoint, migration, credential, or provider behavior was added. Flyway remains V1-V28.

## Defect corrected

Cross-browser execution exposed a real modal initialization race: a catalogue refresh could rerun the create-form initialization effect and restore `IN_APP` after an operator selected `EMAIL`. `NotificationRuleModal` now initializes once per modal opening, after the catalogue is available, and does not overwrite subsequent input. The Ant Design page object also waits for catalogue/template readiness and uses keyboard-driven select interaction consistently across engines.

## Verification checkpoint

- Notification Playwright: 45/45 across Chromium, Firefox, and WebKit.
- Existing Playwright baseline retained: 111/111; the combined suite passes 156/156 with zero failures or skips.
- Frontend lint, 106/106 unit/component tests, and production build pass.
- Spring Modulith and hexagonal architecture focused verification: 12/12 pass (15/15 architecture checks represented by 2 module, 7 layer, 3 boundary tests in the full suite).
- H2 startup applies Flyway V1-V28 and discovers 44 JPA repositories.
- Backend `clean test` and `verify` each pass 647 tests (626 passed, 21 skipped); verify packages the executable JAR.

US-77 remains **PARTIAL** at this checkpoint. MVP-GAP-008I owns final full regression evidence, traceability closure, and the decision to mark US-77 COMPLETE.

No commit or push was performed.
