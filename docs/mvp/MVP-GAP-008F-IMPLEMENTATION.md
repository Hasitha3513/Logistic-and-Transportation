# MVP-GAP-008F Implementation

## Provider decision

SMTP is the single production transport. It fits the existing Spring Boot and environment-driven Docker/PostgreSQL deployment conventions without selecting an arbitrary vendor. `spring-boot-starter-mail` provides Jakarta Mail integration; no provider SDK or provider type crosses the existing `EmailNotificationSenderPort` application boundary.

## Transport architecture

`NotificationEmailConfiguration` intentionally creates exactly one sender:

- disabled: `EmailNotificationDeliveryAdapter`, which returns `EMAIL_DISABLED` and never acceptance;
- test: `DeterministicTestEmailNotificationSenderAdapter`, which performs no network operation and never claims provider acceptance;
- production: `SmtpEmailNotificationSenderAdapter`, which sends one UTF-8 plain-text message to one recipient.

`NotificationEmailDeliveryWorker`, `NotificationEmailDeliveryClaimService`, `NotificationEmailRetryPolicy`, and the V28 durable attempt model remain authoritative for claims, retries, terminal failure, and escalation. No second worker was introduced.

## Configuration and mode selection

Strongly typed `app.notification.email` properties cover `enabled`, exact `mode` (`production` or `test`), provider, from, reply-to, bounded connect/read timeouts, and SMTP host, port, TLS mode, authentication flag, username, and password. The H2/E2E defaults are explicit disabled test mode. The PostgreSQL profile explicitly selects production/SMTP while remaining disabled unless deployment enables it. Compose passes credentials only from environment variables.

Enabled production configuration fails startup for an unsupported/missing provider, invalid/missing sender or host, invalid port/TLS/timeout, or missing required credentials. A production database profile rejects test mode. Unencrypted SMTP is permitted only for a loopback integration server.

## Success and failure semantics

`SENT` means the SMTP server completed the SMTP transaction and accepted responsibility for the message. It does not mean the recipient mailbox confirmed delivery. Jakarta Mail's message ID is persisted in V28 `notification_delivery_attempt.provider_message_id` after acceptance.

Typed mapping is:

| Condition | Category | Retry |
|---|---|---|
| refused connection or DNS/host failure | `CONNECTION` | yes |
| socket connect/read timeout | `TIMEOUT` | yes |
| interrupted thread | `INTERRUPTION` via worker | yes |
| SMTP authentication failure | `AUTHENTICATION` | no |
| invalid configuration/message preparation/TLS negotiation | `CONFIGURATION` | no |
| malformed/permanently rejected recipient | `INVALID_RECIPIENT` | no |
| SMTP transient 4xx | existing retryable `PROVIDER_5XX` category | yes |
| SMTP permanent 5xx sender/message rejection | existing non-retryable `PROVIDER_4XX` category | no |

The provider-neutral category names predate SMTP; SMTP numeric codes are retained in sanitized error codes so semantics remain precise without a schema or domain-contract change. Raw provider diagnostics are not persisted.

## Idempotency and uncertain completion

The stable attempt key remains `<notificationId>:<attemptNumber>` and is transmitted as a non-sensitive `X-Idempotency-Key` message header for traceability. SMTP has no provider-side idempotency guarantee. If a connection fails after server acceptance but before the client observes the response, retry may produce duplicate external delivery. This is an unavoidable at-least-once boundary and is not represented as exactly-once delivery.

## Secret and privacy safeguards

Credentials have no source defaults and `NotificationEmailProperties.toString()` redacts the password and only reports whether a username is configured. The adapter does not log credentials, recipient, subject, or body. Returned and persisted failure text is constant and sanitized; full provider exception messages are not exposed. Existing masked delivery diagnostics remain unchanged.

## Local integration tests

`LocalSmtpTestServer` is a loopback-only deterministic SMTP server supporting acceptance, temporary/permanent recipient rejection, sender rejection, authentication rejection, and greeting timeout. Tests exercise the real Jakarta Mail adapter and persistence-backed worker path: accepted delivery persists a succeeded attempt/message ID and marks `SENT`; transient rejection schedules the exact V28 retry and then succeeds; permanent/authentication failures become terminal and enter escalation orchestration; disabled mode cannot produce `SENT`.

## Verification

- Backend clean test: PASS — 644 run, 623 passed, 0 failures, 0 errors, 21 skipped.
- Backend verify: PASS — 644 run, 623 passed, 0 failures, 0 errors, 21 skipped; executable JAR packaged.
- Architecture: PASS — 15/15.
- Spring context: PASS.
- Flyway: PASS — V1-V28 validated and applied; no V29.
- Frontend lint: PASS.
- Frontend unit: PASS — 94/94.
- Frontend build: PASS.
- Playwright: PASS — 111/111 across Chromium, Firefox, and WebKit.

## Deferred work

- 008G: frontend rule and delivery completion.
- 008H: notification-specific Playwright coverage.
- 008I: regression and US-77 closure.

US-77 remains PARTIAL. No 008G-or-later behavior is implemented by this slice.
