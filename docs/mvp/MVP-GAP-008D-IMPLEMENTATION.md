# MVP-GAP-008D Implementation

## Status and scope

**Status:** COMPLETE (2026-08-22)
**Story:** US-77 — Manage Notification Rules
**Implemented:** durable EMAIL attempts, bounded retry, restart recovery, typed failure classification, terminal failure, one-level IN_APP escalation, delivery diagnostics, attempt diagnostics, and V28.
**Not implemented:** real EMAIL transport, remaining event producers, notification UI completion, notification Playwright cases, manual retry, or post-MVP channels.

## Domain changes

- `NotificationDeliveryAttempt` records attempt identity, number, state, timing, sanitized failure evidence, and provider message ID.
- `NotificationDeliveryAttemptState` distinguishes `PENDING`, `IN_PROGRESS`, `SUCCEEDED`, and `FAILED`.
- `EmailDeliveryErrorCategory` provides typed retryable and non-retryable outcomes without parsing provider text.
- `NotificationEmailRetryPolicy` caps EMAIL at three total attempts and fixes delays at one minute then two minutes.
- `Notification` additively records `parentNotificationId` and `escalationLevel`, and owns valid status/timestamp transitions without worker orchestration.
- `NotificationRulePolicy` activates the existing V27 fallback fields and validates enabled USER/ROLE fallback configuration with a delay from 0 through 60 minutes.

## Attempt lifecycle and retry timing

An immediate EMAIL starts as durable `PENDING` and attempt 1 is immediately eligible. A quiet-hours EMAIL retains the V27 `next_delivery_at`; the worker does not recalculate quiet hours. Each claimed attempt is inserted as `IN_PROGRESS` before the sender is called. An accepted result completes it as `SUCCEEDED`, records the provider message ID, marks the notification `SENT`, and clears `nextDeliveryAt`.

A retryable attempt-1 failure leaves the notification `PENDING` and schedules attempt 2 at completion plus one minute. A retryable attempt-2 failure schedules attempt 3 at completion plus two minutes. Any attempt-3 failure, or any non-retryable failure, marks the notification `FAILED` and clears `nextDeliveryAt`. Attempt 4 cannot be constructed or claimed.

## Failure classification

Retryable categories are connection, timeout, interruption, HTTP 408, HTTP 429, throttling, and provider/server 5xx. Non-retryable categories are invalid recipient, authentication, configuration, template validation, recipient validation, and provider 4xx other than 408/429. Codes and messages are normalized and length-limited before persistence; credentials and raw provider payloads are never accepted by the attempt model.

## Sender boundary and idempotency

`EmailNotificationSenderPort` is the narrow Notification-owned boundary. Its request carries notification ID, stable idempotency key, sender, recipient, subject, plain text body, and timeout. Its result is either accepted with optional provider ID or rejected with a typed error category and sanitized diagnostics. The deterministic key is `<notificationId>:<attemptNumber>`. Recovering the same uncertain attempt reuses its row and key; a scheduled new attempt receives the next deterministic key.

`EmailNotificationDeliveryAdapter` deliberately returns a typed configuration failure until 008F provides real transport. It never treats logging or bean invocation as delivery and cannot produce a false `SENT` state.

## Worker and claim/concurrency strategy

`NotificationEmailDeliveryWorker` queries a bounded set of due, durable PENDING EMAIL notifications. `NotificationEmailDeliveryClaimService` opens a short `REQUIRES_NEW` transaction, locks the notification row pessimistically, determines the next attempt, flushes the `IN_PROGRESS` attempt, and commits before the external sender call. Sender completion runs in a second short transaction under the same notification-row lock.

The notification lock serializes competing claims on H2 and PostgreSQL. `UNIQUE(notification_id, attempt_number)` is the database backstop. The external call is never made while holding the claim transaction open. A claim younger than five minutes is not reclaimed; a stale claim restarts the same attempt and therefore reuses its stable idempotency key. SENT and terminal FAILED notifications cannot be reclaimed.

## Restart recovery

There are no in-memory delays or queues. The scheduler repeatedly discovers persisted PENDING EMAIL notifications from `next_delivery_at`. Retry schedules therefore survive process recreation. Stale `IN_PROGRESS` attempts are recovered deterministically as the same attempt. Failed originals remain queryable for delayed fallback, whose due time is derived from the final attempt completion plus the persisted V27 escalation delay.

## Terminal failure and escalation

Escalation runs only for a terminally FAILED, level-0 EMAIL with an enabled V27 policy. It resolves USER or ROLE recipients through `NotificationRecipientDirectoryPort`; ROLE results are canonicalized and de-duplicated. Each fallback is a distinct linked IN_APP `SENT` notification with `parentNotificationId` set to the failed EMAIL and `escalationLevel` set to 1. No level above 1 is allowed.

The original notification is locked while escalation is evaluated, and the V28 unique parent/recipient/level constraint prevents duplicates after concurrency or restart. A zero-recipient or resolution failure records a sanitized durable diagnostic on the original failure and does not resurrect EMAIL retry or affect the source business transaction.

## V28

`V28__notification_email_delivery_attempts.sql` creates `notification_delivery_attempt`, adds `notification.parent_notification_id` and `notification.escalation_level`, and adds history, due/completion, parent, uniqueness, state, level, and FK constraints/indexes. It reuses `next_delivery_at` from V27. V1-V27 were not changed by this slice.

## Diagnostics APIs and security

- `GET /notification-deliveries` supports bounded `status`, `eventType`, `from`, and `to` filters and returns masked recipient, status, attempt count, scheduling, terminal failure, parent/level, and timestamps.
- `GET /notification-deliveries/{id}/attempts` returns ordered sanitized attempt history and `404` for an unknown delivery.
- Both endpoints require `NOTIFICATION_RULE_VIEW`; the security integration suite covers 401, 403, authorized access, filters, bounds, sanitization, and 404.
- The existing notification response is extended additively with retry/escalation fields, preserving notification-center compatibility.

## Tests

Coverage includes attempts 1-3, exact retry timing, success and terminal paths, no fourth attempt, typed classifications, interruption state, stable idempotency, stale recovery, due-query restart behavior, provider evidence, sanitization, database uniqueness, concurrent claims, escalation delay and recipient behavior, duplicate prevention, diagnostics, API security, H2 V28 application, and architecture boundaries. `DeterministicEmailNotificationSender` supports success, retryable/non-retryable failure, fail-once, fail-twice, and always-retryable scenarios without becoming production transport.

## Verification

Backend `clean test` and `verify` each passed with 610 run, 589 passed, 0 failures, 0 errors, and 21 skipped; verify packaged the JAR. Architecture passed 15/15, the Spring context started, and H2 Flyway validated/applied V1-V28. Frontend lint, all 94 unit tests, and the production build passed. The standard self-starting Playwright regression suite passed 111/111 across Chromium, Firefox, and WebKit, improving on the previous transient 110/111 full-run baseline.

## Deferred work

- 008E: remaining required operational event producers.
- 008F: real configured EMAIL provider adapter.
- 008G: frontend rule/delivery completion.
- 008H: notification-specific Playwright coverage.
- 008I: final regression and US-77 closure.
