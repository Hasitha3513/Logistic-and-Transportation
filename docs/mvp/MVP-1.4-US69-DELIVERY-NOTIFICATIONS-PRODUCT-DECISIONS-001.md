# US-69 Delivery Notifications — Frozen Product Decisions

**Status:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`

**Authority:** Original US-69 requirement, US-61–US-70 use-case/activity/sequence diagrams, mind map, accepted US-56–US-68 behavior, and implemented US-77 Notification source.

**Primary actor:** Customer / recipient.

**Decision date:** 2026-09-02.

## Authoritative Requirement and Acceptance Boundary

US-69 requires a customer or recipient to receive delivery notifications by SMS, application, or email, including conditional ETA, delay, and OTP information, so they know when to expect a delivery. The supporting diagrams require Delivery facts to trigger preference resolution, channel and template selection, send attempt recording, and retry/escalation where configured. Status and arrival information must be visible.

The frozen MVP satisfies the requirement through customer-facing operational Email and SMS, a Delivery-order notification timeline for authorized operators, and the event set below. Customer `IN_APP` delivery is not implementable safely until an authenticated customer-to-user association exists and is therefore deferred to US-70. OTP transport is deferred because the accepted Delivery source contains OTP-mismatch handling but no approved OTP issuer, verification lifecycle, expiry, attempt limit, or secret store. These are explicit limitations, not permission to synthesize an identity or OTP system inside US-69.

## US-69 / US-77 Boundary and Ownership

- US-77 remains the only Notification rules, templates, channel policy, quiet-hours, suppression, execution-audit, delivery-attempt, provider-adapter, and retry engine. US-69 adds Delivery event definitions, customer-recipient resolution, SMS capability, and customer-facing Delivery rules/templates to that engine; it creates no parallel rules engine.
- Delivery owns the business transaction and facts: out-for-delivery, calculated ETA/SLA state, completion, failed attempt, and redelivery schedule. It publishes identifiers and safe operational projections after commit. It never selects a channel/template, evaluates consent, retries a message, or calls SMTP/SMS/provider code.
- Notification owns customer preference records, recipient resolution through a public Organization port, rendering, channel selection through enabled US-77 rules, destination snapshots, attempts, provider abstraction, retry, notification/execution status, diagnostics, and masking.
- Organization remains the canonical owner of Customer master data, including phone and email. Notification may consume only a new tenant-aware public `CustomerNotificationContactLookup` projection containing customer ID, active flag, display name, phone, and email. It must not access Organization repositories, entities, or tables.
- US-68 Planner is read-only and emits no notification. Actual US-59/60/57 operations and US-66/67 facts are the triggers.

This boundary passes ARB P0-01 through P0-07: provider-neutral ports only, separate table ownership, public events/ports only, tenant isolation throughout, RBAC on diagnostics/preferences, no cross-module JPA association, and no provider work in a Delivery transaction.

## Frozen Trigger Events

Only these five events enter the US-77 catalogue for US-69:

| Notification event type | Delivery producer and trigger | Severity | Template variables beyond `eventTime` and `severity` | Suppression |
| :--- | :--- | :---: | :--- | :--- |
| `DELIVERY_OUT_FOR_DELIVERY` | US-66 batch dispatch; one event per active member order when the batch commits as `DISPATCHED` | `INFO` | `deliveryNumber`, `status` | One execution per event; 0-minute business suppression |
| `DELIVERY_ETA_RISK_CHANGED` | US-67 order ETA calculation when the prior current cache entry is absent/different and the new SLA state is `AT_RISK` or `LATE`; no batch ETA and no ETA recalculation by Notification | `WARNING` | `deliveryNumber`, `estimatedArrivalAt`, `slaStatus` | `catalogueMilestone=slaStatus`; 1,440-minute suppression for the same delivery/status/channel/recipient. `AT_RISK` to `LATE` is a distinct milestone, while persisted suppression covers a restart with an empty cache. |
| `DELIVERY_COMPLETED` | US-57 successful POD finalization and Delivery transition to `DELIVERED` | `INFO` | `deliveryNumber`, `status`, `completedAt` | One execution per event; 0-minute business suppression |
| `DELIVERY_FAILED_ATTEMPT_RECORDED` | US-59 committed failed attempt | `WARNING` | `deliveryNumber`, `status`, `failureDisposition` | One execution per event; 0-minute business suppression |
| `DELIVERY_REDELIVERY_SCHEDULED` | US-60 committed schedule or reschedule | `INFO` | `deliveryNumber`, `status`, `scheduledWindowStart`, `scheduledWindowEnd` | `catalogueMilestone=scheduleId`; duplicates of the same schedule milestone are suppressed |

No notification is triggered by order creation, readiness validation, Rider assignment alone, Planner display, exception display, destination edit alone, or batch planning alone. “Approaching” notifications require durable scheduling/location facts that do not exist and are deferred. ETA uses the accepted US-67 result and never polls or recreates its engine.

Each new event is a Delivery-published version-1 local contract using the narrowly adopted standard envelope below. This does not retrofit unrelated event families:

```json
{
  "eventId": "UUID",
  "eventType": "DELIVERY_...",
  "tenantId": "UUID",
  "occurredAt": "OffsetDateTime",
  "version": 1,
  "aggregateType": "DELIVERY_ORDER",
  "aggregateId": "UUID",
  "payload": {
    "customerId": "UUID",
    "deliveryNumber": "String",
    "actor": "String"
  }
}
```

The ETA event additionally carries `estimatedArrivalAt` and `slaStatus` (`AT_RISK` or `LATE`); completion carries `status=DELIVERED` and `completedAt`; failure carries `status=FAILED_ATTEMPT` and `failureDisposition` (`REDELIVERY_ELIGIBLE`, `RETURN_TO_BASE_REQUIRED`, or `ESCALATED`); redelivery carries `status=CONFIRMED`, `scheduleId`, `scheduledWindowStart`, and `scheduledWindowEnd`. Out-for-delivery carries `status=OUT_FOR_DELIVERY` as notification vocabulary without adding a DeliveryOrder lifecycle state. Events contain no phone, email, message body, instructions, free-text failure notes, precise live coordinates, Rider details, OTP, access code, entire aggregate, provider data, or credentials. Tenant and event identity are mandatory and server-derived. Publication is local and after the owning transaction commits.

## Recipient, Preference, Consent, and Snapshot Policy

The rule recipient type is extended with `EVENT_CUSTOMER`; its fixed recipient expression is the event field `customerId`. Notification resolves the customer through the Organization public lookup under the event Tenant. Inactive, missing, cross-tenant, malformed, or channel-ineligible contacts fail closed.

Notification owns a minimal `customer_notification_preference` profile containing only Tenant, logical Customer reference, Email/SMS enabled flags, audit timestamps, and optimistic version. It stores no phone/email. Preference semantics are deterministic:

- With no preference rows, operational Email is enabled when a syntactically valid customer email exists; SMS is disabled and therefore requires explicit opt-in.
- Once any preference row exists for a customer, the complete stored profile is authoritative and every omitted/disabled channel is suppressed.
- Preferences apply only to transactional Delivery communications. No marketing purpose, campaign, profiling, or marketing-consent inference is permitted.
- An enabled preference with no usable destination records `NO_RECIPIENT`. A disabled channel records the existing `SUPPRESSED` outcome with safe failure code `CUSTOMER_CHANNEL_DISABLED`; no new execution outcome is introduced.
- A preference/contact change affects future event evaluations. An already accepted notification retains its resolved destination and rendered content so a retry remains auditable and deterministic.

The `notification.recipient` and `notification_rule_execution.resolved_recipient` values are the normalized destination snapshots required for retry and audit: lower-case valid email or E.164 SMS number. They are never accepted from the Delivery event or client, never returned unmasked by diagnostics APIs, and never logged. Provider request bodies/responses are not persisted. The attempt stores only a provider message ID plus sanitized category, code, and message.

## Channels and Provider Strategy

- **EMAIL — MVP:** reuse the implemented SMTP adapter and provider-neutral `EmailNotificationSenderPort`. Deterministic test/E2E adapters remain profile-restricted.
- **SMS — MVP:** add `SMS` to the existing channel catalogue and a Notification-owned provider-neutral `SmsNotificationSenderPort`. The MVP adapter is deterministic/local and must not log recipient or body. No vendor SDK is approved. A production profile without an explicitly configured real adapter fails closed rather than reporting fictitious delivery.
- **Application / IN_APP — deferred to US-70:** the current IN_APP channel targets Identity usernames/roles, while Customer has no authenticated user association. US-69 must not guess one or expose customer notices to operators as the customer.
- **Push, WhatsApp, voice, and webhook channels — deferred.**

Provider credentials remain external configuration/secrets. They are prohibited from database rows, frontend payloads, event metadata, error text, and logs. Provider SDK types may appear only in a future outbound adapter; they may not leak into Delivery, Notification domain/application contracts, or public events.

## Templates, Rules, Quiet Hours, and Localization

Existing versioned, channel-specific `notification_template` records remain authoritative. The implementation adds controlled version-1 Email/SMS templates for the five catalogue events and corresponding tenant rules using `EVENT_CUSTOMER`; it creates no Delivery template table. Current templates are global system defaults rather than tenant-owned overrides. Tenant rules are tenant-owned and may select only compatible catalogue templates. Tenant template overrides and a template-edit API remain out of scope.

Allowed template variables are exactly those in the trigger table. `customerDisplayName` may be added at recipient-resolution time from the canonical Organization projection. Raw UUIDs, contact destinations, instructions, free-text failure notes, access/gate codes, OTPs, medical/drug information, provider identifiers, secrets, and Rider private data are forbidden template variables. Rendering retains US-77 token validation and text limits.

MVP localization is one configured system language; no recipient-locale model or translation framework is introduced. Existing US-77 suppression is reused. Quiet hours are extended from Email to SMS for `INFO` and `WARNING`; `CRITICAL` retains the current bypass, although no frozen US-69 event is critical. US-69 customer rules do not enable US-77 internal escalation. Per-recipient/per-tenant rate limiting beyond execution dedupe, milestone suppression, and quiet hours is deferred.

## Delivery Semantics, Idempotency, Retry, and Failure Classification

The end-to-end MVP guarantee is **BEST_EFFORT**. Delivery publishes a local after-commit event; that handoff is not durable across process failure and must not be described as at-least-once or exactly-once. Once Notification has persisted a notification and attempt, provider delivery uses bounded at-least-once retry. A database outbox/inbox and platform-wide event-envelope modernization remain P1-01 work and are not part of US-69.

Replay safety uses the implemented tenant-qualified execution uniqueness and stable key derived from `tenantId + eventId + ruleId + channel + normalized recipient`. Business duplicate suppression additionally uses rule/event/delivery/recipient/channel/milestone. Producers must preserve `eventId` when replaying the same committed fact.

Email and SMS share the existing durable attempt table and retry contract:

- Maximum three attempts total.
- Retry after one minute following attempt 1 and two minutes following attempt 2.
- Retryable: connection, timeout, interruption, HTTP 408/429, throttling, and provider 5xx.
- Non-retryable: invalid recipient, authentication/configuration, template or recipient validation, opt-out, and permanent provider 4xx.
- A Notification remains `PENDING` while another attempt is due, becomes `SENT` when the provider accepts it, and becomes terminal `FAILED` after a permanent failure or exhausted third attempt. `SENT` means provider/local-adapter acceptance—not device delivery.
- Attempt rows use existing `PENDING`, `IN_PROGRESS`, `SUCCEEDED`, and `FAILED`; rule executions use existing `ACCEPTED`, `SUPPRESSED`, `NO_RECIPIENT`, `TEMPLATE_DATA_MISSING`, and `FAILED`. No `DELIVERED`, `FAILED_RETRYABLE`, or consent-specific status is added.
- Manual send, resend, failed-message retry, and operator-triggered provider calls are deferred. Existing scheduled worker/claim locking is reused; no second scheduler is created.

Delivery receipts, callbacks, and webhooks are deferred. Therefore callback races and webhook signing are not MVP concerns. A provider timeout can still result in an external duplicate if the provider accepted before timing out; the port must pass the stable notification/attempt idempotency key where the provider supports it, but the product makes no exactly-once claim.

## Persistence and Migration Expectation

Existing `notification`, `notification_rule`, `notification_template`, `notification_rule_policy`, `notification_rule_execution`, and `notification_delivery_attempt` remain the core model. One new Notification-owned table is frozen:

| Column | Type / rule |
| :--- | :--- |
| `id` | UUID primary key |
| `tenant_id` | UUID, not null, trusted Tenant scope |
| `customer_id` | UUID, not null, logical Organization reference; no physical cross-module FK |
| `email_enabled` | BOOLEAN, not null |
| `sms_enabled` | BOOLEAN, not null |
| `created_at`, `updated_at` | TIMESTAMPTZ, not null |
| `version` | BIGINT, not null, optimistic locking |

Required constraints/indexes are unique `(tenant_id, customer_id)`, Delivery history on `notification_rule_execution(tenant_id, aggregate_type, aggregate_id, created_at DESC)`, existing Tenant/status/created-time and retry-due indexes, and existing tenant-qualified execution-key uniqueness. Same-Tenant Customer integrity is enforced through the tenant-aware Organization lookup because cross-module physical FKs are forbidden.

A forward migration is required to create that table; add `SMS` and `EVENT_CUSTOMER` to existing check constraints; expand `notification.recipient` from 128 to 320 characters to match execution storage; add the Delivery-history index; and seed controlled templates/rules without modifying V25–V28/V44/V57. If and only if V58 is still free when implementation starts, the expected filename is `V58__delivery_notifications_us69.sql`; otherwise the implementation must use the then-next free version. No migration is created by this decision task.

## Tenant, Security, Privacy, and Audit

- Tenant identity comes from the trusted event/execution context. Any client Tenant value is non-authoritative. Rules, preferences, execution rows, notification rows, attempts, recipient lookup, history, and retry are Tenant-scoped.
- Global system templates contain no tenant/customer data. Tenant rule/template compatibility is checked before rendering. A Tenant A event can never resolve Tenant B customer/contact/rule/preference/notification/attempt.
- Existing `NOTIFICATION_RULE_VIEW` protects operator history and preference reads; `NOTIFICATION_RULE_MANAGE` protects preference writes. `NOTIFICATION_VIEW` remains the signed-in Identity user's own IN_APP inbox permission. No `DELIVERY_NOTIFICATION_SEND`, retry, or manual-send permission is introduced.
- Template input is catalogue allow-listed and escaped/rendered as plain text. Header/control injection, arbitrary template variables, client-selected recipients, recipient spoofing, Tenant spoofing, replay spam, cross-Tenant template/rule selection, link leakage, credential leakage, and PII logging must have negative tests.
- US-69 sends no customer-facing link. Messages use the public delivery number, never raw internal UUIDs. Tracking tokens/authenticated portal links are US-70 decisions.
- Message bodies contain only minimized operational data. Phone/email destination snapshots and rendered bodies follow the existing Notification record lifecycle and platform backup/retention policy; US-69 adds no indefinite provider payload or separate archive and no new purge scheduler. Diagnostics and frontend always mask destinations. Failure notes are categorical/sanitized.
- Audit retains source event/type, aggregate identity through rule execution, rule/template ID and version, channel, masked destination on output, timestamps, attempt count/state, safe failure classification, and provider message ID. There is no manual actor because resend is deferred.

## API and Frontend UX

Notification remains API owner. The existing external route is extended, rather than adding Delivery-owned notification CRUD:

- `GET /api/v1/notification-deliveries?aggregateType=DELIVERY_ORDER&aggregateId={deliveryId}&limit={1..200}` — tenant-scoped read-only timeline; `NOTIFICATION_RULE_VIEW`.
- `GET /api/v1/notification-deliveries/{notificationId}/attempts` — existing masked attempt history; `NOTIFICATION_RULE_VIEW`.
- `GET /api/v1/notification-customer-preferences/{customerId}` — effective operational channel profile; `NOTIFICATION_RULE_VIEW`. Response includes `customerId`, `explicitProfile`, `emailEnabled`, `smsEnabled`, `maskedEmail`, `maskedPhone`, and nullable `version`.
- `PUT /api/v1/notification-customer-preferences/{customerId}` — replace the complete Email/SMS profile; `NOTIFICATION_RULE_MANAGE`. Body is exactly `emailEnabled: boolean`, `smsEnabled: boolean`, and nullable `version: long` (`null` only for first creation); response is the GET representation with the new version.

The existing status/event/time filters remain compatible. There is no send/resend endpoint and no `GET /deliveries/{id}/notifications` controller in Delivery.

The minimal frontend is a **Notifications** timeline in the existing Delivery Order detail page. It is shown only with `NOTIFICATION_RULE_VIEW` and displays event/template label, channel, status, masked destination, created/sent time, attempt count, and safe failure category; it exposes no provider payload or manual action. US-77 retains its existing administration/diagnostics page. US-69 adds no customer portal, login, rescheduling, preference-editing UI, push client, or other US-70 surface.

## Concurrency and Failure Isolation

- Duplicate delivery of the same event loses to the tenant-qualified execution key. Simultaneous retry workers use the existing durable claim/locking and one attempt number per Notification.
- A rule/template change after enqueue does not rewrite the captured template ID/version/body. A contact/preference change after enqueue does not mutate the destination snapshot; it controls future events.
- Delivery changes while an older notice is queued do not rewrite it. A later committed fact has a new event ID and may create a later notice according to milestone suppression.
- Provider failure and Notification listener failure occur after Delivery commit and never roll back or change `DeliveryOrder`, batch, POD, attempt, redelivery, or ETA state. There is no `NOTIFICATION_FAILED` Delivery lifecycle state.
- Notification DB persistence and external provider calls do not execute inside the Delivery owning transaction.

## Mandatory Implementation Verification

Implementation acceptance must include:

- Domain/application tests for catalogue validation, customer preference defaults/overrides, destination normalization/masking, template allow-listing, status transitions, retry categories/backoff, and ETA milestone suppression.
- Event producer/consumer contract tests for all five events, after-commit behavior, provider failure isolation, stable event replay, source transaction rollback, and absence of PII.
- PostgreSQL tests for Tenant isolation, cross-Tenant customer rejection, unique preference/execution keys, duplicate replay, concurrent worker claims, history filtering, attempts, and the complete Flyway head.
- Literal `/api/v1/...` SecurityConfig/RBAC tests for history, attempts, and preference GET/PUT, including unauthenticated 401, insufficient 403, same-Tenant success, and cross-Tenant 404.
- Frontend Vitest plus real PostgreSQL-backed Chromium proving a real Delivery operation produces a Notification execution/record/status visible in the Delivery detail timeline without mocking the internal REST/business path. The deterministic SMS/Email adapter is acceptable; no real SMS vendor is required.
- Relevant US-56–US-68 and US-77 regressions, architecture/Modulith verification, full Maven verify, Checkstyle, PMD, SpotBugs, TypeScript, Vitest, production build, changed-file lint, and `git diff --check`.
- Every destructive PostgreSQL acceptance path must use only `transport_logistics_acceptance`; the development database is never acceptance evidence.

## Deferred Scope

Customer IN_APP/push identity linkage and preference UI (US-70), OTP issue/verify/expiry/secret transport, approach/geofence/live-location notifications, marketing consent/campaigns, real SMS vendor choice/SDK, callbacks/webhooks/device-delivery receipts, manual send/resend/retry, WhatsApp/voice, tenant template overrides, recipient localization, broad rate limiting, durable outbox/inbox, and platform-wide event-envelope modernization are not approved by US-69.

## Frozen Program State

- US-69 product decisions: **FROZEN**
- US-69 implementation: **NOT STARTED**
- Current Flyway head: **V57**
- MVP 1.4: **6 / 8 COMPLETE**
- Overall: **63 / 87 COMPLETE**
- Deferred: **24 / 87**
- Next task: `MVP-1.4-US69-DELIVERY-NOTIFICATIONS-IMPLEMENTATION-001`
