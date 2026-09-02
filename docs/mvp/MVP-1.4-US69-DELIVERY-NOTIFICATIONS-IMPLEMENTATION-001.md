# US-69 Delivery Notifications — Technical Implementation Closure

**Task ID:** `MVP-1.4-US69-DELIVERY-NOTIFICATIONS-IMPLEMENTATION-001`  
**Date:** 2026-09-03  
**Status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**US-69 migration:** `V58__delivery_notifications_us69.sql`  
**Current Flyway head:** `V58__delivery_notifications_us69.sql`

## Implemented ownership and event model

Delivery publishes exactly five version-1, Tenant-scoped, after-commit business facts with aggregate type `DELIVERY_ORDER`: `DELIVERY_OUT_FOR_DELIVERY`, `DELIVERY_ETA_RISK_CHANGED`, `DELIVERY_COMPLETED`, `DELIVERY_FAILED_ATTEMPT_RECORDED`, and `DELIVERY_REDELIVERY_SCHEDULED`. Payloads contain only the frozen safe business fields. Delivery does not select channels, resolve destinations or templates, persist notification records, invoke providers, or perform retries.

The System integration bridge maps those public Delivery facts to the existing US-77 `OperationalNotificationEvent` contract without introducing a Delivery-to-Notification implementation dependency. Notification owns rule evaluation, `EVENT_CUSTOMER` recipient resolution, channel preferences, destination normalization and masking, templates, delivery records, execution audit, attempts, retry, history, and provider adapters. Organization exposes only the Tenant-aware public `CustomerNotificationContactLookup` projection; Notification does not access Organization persistence.

The shared after-commit publisher now dispatches a nested event immediately while an outer after-commit callback is executing. This prevents a bridged event from being registered against a transaction whose commit phase has already finished. Consumer/provider failures remain isolated from the committed Delivery operation.

## Preferences, channels, delivery, and privacy

`customer_notification_preference` is Notification-owned and Tenant-scoped, with optimistic versioning and uniqueness on `(tenant_id, customer_id)`. With no explicit row, a valid active customer email is enabled and SMS is disabled. A stored row is a complete replacement profile. `PUT` accepts only `emailEnabled`, `smsEnabled`, and nullable-on-create `version`; stale updates use the existing conflict semantics.

Email reuses the existing provider-neutral email boundary. SMS adds a provider-neutral sender port and deterministic local/E2E adapter; production without a configured sender fails closed. `SENT` means provider/local-adapter acceptance, not device delivery. Existing bounded retry remains three total attempts with one- and two-minute backoff. Retryable and permanent failure classification is shared across Email/SMS, and retries use the persisted normalized destination snapshot.

Customer email is lower-case normalized and validated; SMS uses validated E.164 form. Raw destinations are confined to internal delivery boundaries. APIs and UI return masked destinations and no message body, credentials, provider payload, free-text notes, Rider private data, OTP, or access codes. Template rendering remains catalogue/allow-list controlled.

Stable event IDs and the existing execution key prevent replay spam. ETA suppression uses `slaStatus` and a 1,440-minute window; redelivery suppression uses `scheduleId`. The existing execution and claim uniqueness constraints protect concurrent event processing and retry workers.

## Migration and APIs

Migration V58 creates `customer_notification_preference`, expands the existing channel and recipient-type constraints for `SMS` and `EVENT_CUSTOMER`, expands recipient snapshots to 320 characters, adds the Delivery history index, and seeds ten controlled version-1 Email/SMS templates and ten Tenant-scoped rules for the five frozen events. It creates no Delivery-owned notification table and no physical Organization foreign key.

Notification exposes these Tenant-scoped, RBAC-protected APIs under the configured `/api` context:

- `GET /api/v1/notification-deliveries?aggregateType=DELIVERY_ORDER&aggregateId={deliveryId}&limit={1..200}` — `NOTIFICATION_RULE_VIEW`.
- `GET /api/v1/notification-deliveries/{notificationId}/attempts` — `NOTIFICATION_RULE_VIEW`.
- `GET /api/v1/notification-customer-preferences/{customerId}` — `NOTIFICATION_RULE_VIEW`.
- `PUT /api/v1/notification-customer-preferences/{customerId}` — `NOTIFICATION_RULE_MANAGE`.

Literal `/api/v1/...` and context-relative security tests cover the history, attempt-history, and preference routes. Aggregate history filtering uses dynamically composed JPA criteria so absent timestamp filters do not produce untyped PostgreSQL parameters.

## Frontend

The existing Delivery Order details page includes a read-only Customer notification timeline for actors with `NOTIFICATION_RULE_VIEW`. It shows event/template title, channel, status, masked destination, timestamps, attempt count, and safe failure category. US-69 adds no send, resend, retry, provider, or preference-management action.

## Verification evidence

| Gate | Actual result |
| :--- | :--- |
| Focused literal-URL remediation regression | 32 tests, 0 failures, 0 errors — PASS (22 security, 7 controller, 3 event-contract) |
| Complete `./mvnw verify` | 1,220 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 4:09 |
| Flyway | V1–V58 validated/applied against `transport_logistics_acceptance`; current head V58 |
| Architecture and Spring Modulith | 42/42 PASS |
| Checkstyle | PASS; 0 violations |
| PMD | PASS |
| SpotBugs | PASS; 0 bugs/errors |
| TypeScript | PASS |
| Vitest | 57 files / 254 tests PASS |
| Production build | PASS; known non-failing bundle-size warning |
| Changed-file ESLint | PASS; US-69 introduced errors: 0 |
| Global ESLint | `BASELINE_DEBT`: 71 errors, 0 warnings in untouched Delivery analytics/batches/exceptions/riders/slots/zones files |
| Real PostgreSQL-backed Chromium | 6/6 PASS in 17.3s |
| `git diff --check` | PASS |

The real browser suite starts fresh backend/frontend sessions and proves the Delivery completion event, Notification consumption, Organization customer lookup, default Email behavior, explicit deterministic SMS behavior, persisted history, masked timeline, Tenant-B isolation, and literal-URL RBAC denial. It uses `jdbc:postgresql://127.0.0.1:5433/transport_logistics_acceptance` and does not mock the internal REST/business flow.

## Development database safety note

All destructive PostgreSQL verification and all accepted technical evidence used only `transport_logistics_acceptance`. During an earlier focused diagnostic run, a process that inherited the default datasource applied V58 to the development schema before the datasource was corrected. It did not clean, truncate, or delete development data. The development database was not used as evidence, and this task deliberately performs no rollback or other mutation of it.

## Deferred scope

Customer IN_APP, portal/login, rescheduling, OTP, push, WhatsApp, voice, callbacks/webhooks, manual send/resend/retry, vendor SMS SDKs, and a platform outbox/inbox remain deferred. No US-70 capability or platform-wide event redesign was introduced.

## Status and next task

US-69 technical implementation is complete but has not received final acceptance.

- US-69: **`IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`**
- MVP 1.4: **6 / 8 COMPLETE**
- Overall: **63 / 87 COMPLETE**
- Deferred: **24 / 87**
- Next task: `MVP-1.4-US69-DELIVERY-NOTIFICATIONS-FINAL-ACCEPTANCE-001`
