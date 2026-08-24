# MVP-GAP-008C Implementation Record

## Status

**COMPLETE — 2026-08-21**

US-77 remains **PARTIAL**. This slice implements AC-77-14, AC-77-15, AC-77-17, and the rule-execution portion of AC-77-24. It does not implement retry, escalation execution, remaining producers, production email transport, complete frontend policy UX, or notification Playwright scenarios.

## Scope

Implemented notification rule policy persistence and evaluation for quiet hours and repeated-event suppression, durable quiet-hour email scheduling, recipient-granular execution audit, an authorized minimal audit query, and deterministic/concurrent test coverage. Authentication, existing permissions, event producers, immediate channel adapters, and V1-V26 were preserved.

## Domain and Application Changes

- `NotificationRulePolicy` owns quiet-hours configuration and the inclusive `0..1440` minute suppression window.
- `NotificationQuietHoursEvaluator` uses injected `Clock` and the deployment IANA timezone from `app.notification.time-zone` (default `UTC`).
- `NotificationSuppressionKey` creates a normalized SHA-256 key from rule, event type, aggregate, resolved recipient, channel, and explicit catalogue milestone/no-milestone data. Event ID and display text are excluded.
- `NotificationSuppressionEvaluator` keeps time suppression distinct from stable-event idempotency. CRITICAL bypasses time suppression; a zero-minute window disables it.
- `NotificationRuleExecution` records recipient-level evaluation outcomes and sanitized failures.
- `NotificationRuleEngine` runs in an isolated `REQUIRES_NEW` transaction, locks the one-to-one policy row, evaluates each resolved recipient, creates no notification for suppressed/no-recipient/render-failure outcomes, and preserves the existing immediate path otherwise.

## Quiet-Hours Semantics

- Quiet hours apply only to EMAIL; enabling them for IN_APP returns `NOTIFICATION_POLICY_INVALID`.
- Same-day intervals use start-inclusive/end-exclusive semantics.
- Overnight intervals belong to their start day.
- A DST gap resolves to the next valid instant; an overlap uses the earlier offset through Java timezone rules.
- Non-CRITICAL EMAIL inside quiet hours is rendered and persisted as PENDING with authoritative `nextDeliveryAt`; the email adapter is not invoked.
- CRITICAL EMAIL and all IN_APP notifications remain immediate.
- Later rule edits or disablement do not recalculate an already persisted `nextDeliveryAt`.

## Suppression Semantics

- The suppression window is based only on the latest ACCEPTED execution after `now - window`; SUPPRESSED executions never extend it.
- The exact boundary is accepted because the lookup is strictly `completedAt > boundary`.
- Recipient, channel, rule, aggregate, and catalogue milestone are independent key dimensions.
- Stable event replay is independently stopped by the recipient-granular execution key.
- CRITICAL events bypass time suppression but remain subject to stable-event idempotency.

## Audit Model and Query

Outcomes implemented: `ACCEPTED`, `SUPPRESSED`, `NO_RECIPIENT`, `TEMPLATE_DATA_MISSING`, and `FAILED`.

`GET /notification-rule-executions` accepts optional `ruleId`, `eventId`, and bounded `limit` parameters. It requires `NOTIFICATION_RULE_VIEW`, masks resolved recipients, omits suppression keys, and exposes sanitized failure diagnostics only.

## V27

`V27__notification_rule_policy_and_execution.sql` adds:

- `notification_rule_policy`, including frozen escalation columns defaulted safely without escalation behavior;
- `notification_rule_quiet_day`, with one ISO day per policy and a composite primary key;
- `notification_rule_execution`, with recipient-granular SHA-256 execution uniqueness and suppression/audit indexes;
- `notification.next_delivery_at` plus the due-notification index required for durable quiet-hour queuing.

Existing rules are backfilled with quiet hours disabled and frozen catalogue suppression defaults. V1-V26 were not modified. V28 must reuse `notification.next_delivery_at` rather than add it again.

## API Changes

Notification rule create/update/response models add:

- `quietHoursEnabled`
- `quietStartTime`
- `quietEndTime`
- `quietDays`
- `suppressionWindowMinutes`

Notification responses add `nextDeliveryAt`. Existing request constructors and omitted-policy behavior remain compatible.

## Concurrency Strategy

Every rule has one policy row. The engine obtains a pessimistic write lock on that row before suppression lookup and accepted-execution insert. Equivalent events for the same rule therefore serialize across database transactions; the second transaction observes the first ACCEPTED execution and records SUPPRESSED. The execution key unique constraint independently protects stable event replay at resolved-recipient granularity.

## Automated Evidence

Coverage includes policy validation, same-day/overnight boundaries, inactive days, critical and IN_APP bypass, fixed clock, America/New_York DST gap and overlap, deterministic keys and milestones, zero/inside/boundary suppression, critical suppression bypass, stable-event idempotency, accepted/suppressed/no-recipient/template-failure/failed audit, failure sanitization, controlling notification linkage, policy/quiet-day/next-delivery persistence, authorized audit queries, and a two-thread/two-transaction database lock test.

Verification:

| Gate | Result |
|---|---|
| `./mvnw -B clean test` | PASS — 579 run, 558 passed, 0 failures, 0 errors, 21 skipped |
| `./mvnw -B verify` | PASS — same test result; executable JAR packaged |
| Architecture | PASS — 15/15 |
| Spring context | PASS |
| Flyway H2 | PASS — V1-V27 |
| `npm run lint` | PASS |
| `npm test` | PASS — 22 files, 94 tests |
| `npm run build` | PASS; pre-existing non-blocking chunk warning |
| `npm run test:e2e` | 110/111; one transient Firefox logout redirect timeout |
| isolated Firefox logout rerun | PASS — 1/1 |

The documented baseline Firefox E2E-TRIP-008 selector failure did not reproduce in this run. The full run instead had a Firefox logout redirect timing failure, which passed immediately in isolation. No 008C production or frontend code touches authentication/logout or trip selectors, so there is no reproducible 008C Playwright regression.

## Deferred Work

- 008D: durable delivery attempts, retry/backoff, and terminal-failure escalation execution.
- 008E: remaining required operational event producers and milestone metadata.
- 008F: production EMAIL transport.
- 008G: complete frontend policy/diagnostics UX.
- 008H: notification-specific Playwright coverage.
- 008I: final regression and US-77 closure.
