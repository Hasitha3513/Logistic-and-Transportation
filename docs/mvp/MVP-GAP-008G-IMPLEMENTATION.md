# MVP-GAP-008G Implementation

## Scope

This slice completes the US-77 operator-facing rule administration and delivery visibility UI without adding backend behavior, migrations, provider settings, or notification-specific Playwright cases. US-77 remains PARTIAL pending 008H and 008I.

## UI architecture and API integration

The existing `NotificationRulesPage`, `NotificationRuleModal`, `NotificationCenter`, feature types, and TanStack Query hooks were extended. `NotificationDeliveryDiagnostics` provides the bounded administration surface. It consumes only the frozen APIs: event catalogue, filtered active templates, rule CRUD/status, bounded deliveries, and per-delivery attempts.

The event Select is catalogue-backed and exposes only returned events. Channel choices are limited to `IN_APP` and `EMAIL`. Template queries are keyed by event and channel, incompatible selections are cleared, and subject/body are displayed read-only with system-placeholder guidance.

## Conditional rule behavior

The form is divided into Basic, System Template, Recipient, Delivery Policy, and Failure Escalation sections. It supports USER/ROLE recipients for both channels and EMAIL_ADDRESS only for EMAIL. Role and username entry use canonical text values because the existing identity list APIs require separate identity administration authority; no role is hard-coded and no new identity endpoint was added.

Severity communicates at-or-above threshold semantics. EMAIL exposes quiet hours, ISO quiet days, suppression 0..1440, and the backend-required active fallback. Overnight quiet ranges are preserved while equal start/end is rejected. Escalation supports delay 0..60 and USER/ROLE fallback only. Switching to IN_APP clears and omits quiet/escalation state. Create and edit share exact DTO construction, and edit hydrates every backend policy field including enabled state.

Stable backend business codes and both supported field-error shapes map to the applicable Ant Design form control; general failures use the Ant Design App message context.

## Rule and delivery presentation

The rules table now summarizes template, severity, recipient, suppression, quiet schedule, fallback, enabled state, and actions without exposing internal policy identifiers. The diagnostics tab supports backend status/event/from/to filters with a 100-record bound. It presents masked recipients, PENDING/SENT/FAILED/READ states, attempts, next retry, terminal failure, sent time, and one-level parent/escalation context. Rule managers may open sanitized attempt history; manual retry is intentionally absent.

The personal Notification Center retains unread count, list, mark-one-read, mark-all-read, empty/error behavior, and now explicitly labels the channel while remaining separate from administration diagnostics.

## Permissions

- `NOTIFICATION_RULE_VIEW`: rules and delivery diagnostics.
- `NOTIFICATION_RULE_MANAGE`: create, edit, delete, enable/disable, and attempt-history inspection.
- `NOTIFICATION_VIEW`: personal Notification Center.

Frontend hiding complements the unchanged backend authority checks.

## Tests and verification

- Frontend lint: PASS, zero ESLint warnings/errors.
- Frontend unit/component: PASS, 106/106 (previously 94/94; 12 net new tests). Notification-focused tests cover catalogue/template-backed creation, payload trimming, channel-recipient compatibility, suppression zero, hidden-state omission, overnight quiet hours, escalation boundaries, policy summaries, delivery states, terminal failure, no manual retry, and sanitized attempt history.
- Frontend build: PASS. Vite reports its pre-existing bundle-size advisory only.
- Backend clean test: PASS, 644 run, 623 passed, 21 skipped.
- Backend verify: PASS, 644 run, 623 passed, 21 skipped; executable JAR packaged.
- Architecture: PASS, 15/15; Spring Modulith verification PASS.
- Spring context/JPA: PASS; context started and 44 JPA repositories were discovered.
- Flyway: PASS, V1-V28; no V29.
- Existing Playwright regression: PASS, 111/111 across Chromium, Firefox, and WebKit; no notification-specific cases were added.
- Visual inspection: PASS for rules, catalogue/template modal, expanded EMAIL quiet/fallback controls, diagnostics, and Notification Center at the standard desktop viewport. Wide tables remain contained with horizontal scrolling.

## Deferred work

- 008H: notification-specific Playwright cases.
- 008I: final regression, traceability, and US-77 closure.
- SMS, PUSH, WEBHOOK, template editing/creation, SMTP/provider UI, manual retry, analytics, campaigns, and multi-level escalation remain outside frozen MVP scope.

No production backend file or database migration was added by 008G.
