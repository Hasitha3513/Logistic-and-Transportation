# US-77 MVP Notification Contract

**Task:** MVP-GAP-008A
**Story:** US-77 — Manage Notification Rules
**Contract status:** FROZEN FOR MVP IMPLEMENTATION
**US-77 implementation status:** COMPLETE
**Latest schema:** V28; V25-V28 are immutable

## 1. Scope

This document freezes the minimum, testable US-77 contract. It is the authority for MVP-GAP-008B through MVP-GAP-008I; those slices may implement this contract but must not silently expand it.

MVP provides rule-driven operational notifications through **IN_APP** and **EMAIL**. It includes controlled event types, system-managed versioned templates, validated recipients, deterministic quiet hours, repeated-event suppression, one-level failure escalation, durable EMAIL retry, auditable delivery state, operator configuration, and notification-specific E2E coverage.

This contract does not create a generic workflow, scripting, campaign, or messaging platform.

## 2. Current Implementation (008I closure)

The repository already contains:

- `notification.OperationalNotificationEvent`, the public module boundary;
- `NotificationRule`, `Notification`, `NotificationRuleEngine`, rule/notification services and ports;
- REST CRUD, enable/disable, notification list, unread count, read, and read-all;
- JPA persistence and V25 tables for rules and notifications;
- `INFO < WARNING < CRITICAL`, USER/ROLE/EMAIL_ADDRESS, and IN_APP/EMAIL domain values;
- event/rule/recipient idempotency, SENT/FAILED/READ state, failure reason, and history;
- V25 permissions and explicit `SecurityConfig` mappings;
- `NotificationRulesPage`, `NotificationRuleModal`, `NotificationCenter`, and hooks;
- exactly eight frozen operational producers through the public Trip/Fleet boundaries;
- controlled templates/rendering, recipient validation, quiet/suppression, durable EMAIL retry, terminal escalation, SMTP production delivery, and diagnostic audit;
- complete operator UI for channels, templates, recipients, policies, delivery health, and attempts;
- 15 retained notification Playwright cases passing in all three supported browsers (45/45 executions).

Slices 008A through 008I are complete. Final closure verification passed 647 backend tests, all 15 architecture checks, Spring/JPA/Security startup, Flyway V1-V28, 106 frontend tests, lint/build, 45/45 notification browser executions, and the complete 156/156 Playwright regression.

## 3. MVP-Required Capabilities

| Capability | Frozen MVP requirement |
|---|---|
| Rule management | CRUD and explicit enable/disable against the controlled catalogue |
| Matching | enabled rule, exact event type, event severity at or above rule threshold |
| Channels | IN_APP and production EMAIL |
| Templates | system-managed, versioned, plain-text templates selected by rule |
| Recipients | validated USER, ROLE, and EMAIL_ADDRESS with channel compatibility |
| Quiet hours | per EMAIL rule; deployment business timezone; CRITICAL bypass |
| Suppression | per rule/window for repeated equivalent events; separately audited from idempotency |
| Escalation | one IN_APP fallback after terminal EMAIL failure only |
| Retry | durable automatic EMAIL attempts with bounded backoff |
| State/history | PENDING, SENT, FAILED, READ plus rule-execution and delivery-attempt audit |
| Operations | the MVP_REQUIRED event catalogue below |
| Security/UI | existing permissions; complete rule fields and delivery visibility |
| E2E | notification-specific rule, trigger, center, policy, failure, and retry tests |

## 4. Deferred Capabilities

| Capability | Boundary |
|---|---|
| SMS | PHASE 2 |
| PUSH | PHASE 2 |
| WEBHOOK | PHASE 2 |
| Administrator template editor/designer | PHASE 2 |
| HTML/rich templates, arbitrary expressions, or scripting | PHASE 2 |
| Escalation on unread age or event age; multi-level trees | PHASE 2 |
| Manual redelivery, bulk campaigns, preferences, analytics | PHASE 2 |
| `DRIVER_DRUG_TEST_EXPIRING` | NOT REQUIRED until a drug-test validity/expiry model exists |
| Fuel threshold/exception notifications | PHASE 2; synchronous MVP fuel policy already blocks the operation |

## 5. Controlled Event Catalogue

Role names are not hard-coded. “Functional role” below means an administrator-selected existing ROLE appropriate to that operation. No enabled rule is silently seeded against an assumed production role.

| eventType | Class | Owner / trigger | Severity | Default recipient intent | Channels | Template code | Deduplication key | Suppression | Escalation / retry | Audit |
|---|---|---|---|---|---|---|---|---|---|---|
| `TRIP_DELAY_RECORDED` | MVP_REQUIRED | Trip; accepted delay record | WARNING | Trip operations functional role | IN_APP, EMAIL | `TRIP_DELAY` | stable event ID from trip operational-event ID | trip + recipient + channel, 15 min | EMAIL terminal failure -> configured IN_APP fallback; standard EMAIL retry | event, match, render, suppression and delivery |
| `TRIP_INCIDENT_RECORDED` | MVP_REQUIRED | Trip; accepted incident record | LOW=INFO, MEDIUM/HIGH=WARNING, CRITICAL=CRITICAL | Trip operations/safety functional role | IN_APP, EMAIL | `TRIP_INCIDENT` | trip operational-event ID | incident ID; no time suppression beyond idempotency | same; CRITICAL bypasses quiet hours | all outcomes |
| `VEHICLE_MAINTENANCE_DUE` | MVP_REQUIRED | Fleet; active scheduled maintenance reaches 24-hour lead point | WARNING | Fleet operations functional role | IN_APP, EMAIL | `VEHICLE_MAINTENANCE_DUE` | schedule ID + scheduled start + lead bucket | schedule + recipient + channel, 24 h | standard | all outcomes |
| `VEHICLE_DOCUMENT_EXPIRING` | MVP_REQUIRED | Fleet; active mandatory document reaches 30-day lead or expiry milestone | WARNING; CRITICAL on/after expiry | Fleet compliance functional role | IN_APP, EMAIL | `VEHICLE_DOCUMENT_EXPIRING` | document ID + expiry date + milestone | document + milestone + recipient + channel, 24 h | standard; CRITICAL bypass | all outcomes |
| `DRIVER_EXCEPTION_RECORDED` | MVP_REQUIRED | Fleet; blocking exception is created/activated | WARNING; disciplinary suspension or medical emergency=CRITICAL | Driver operations functional role | IN_APP, EMAIL | `DRIVER_EXCEPTION` | exception ID + status transition | exception ID; no time suppression beyond idempotency | standard; CRITICAL bypass | all outcomes |
| `DRIVER_MEDICAL_EXPIRING` | MVP_REQUIRED | Fleet; active fit medical record reaches 30-day lead or expiry milestone | WARNING; CRITICAL on/after expiry | Driver compliance functional role | IN_APP, EMAIL | `DRIVER_MEDICAL_EXPIRING` | medical-record ID + valid-until + milestone | record + milestone + recipient + channel, 24 h | standard; CRITICAL bypass | all outcomes |
| `DRIVER_DRUG_TEST_FAILED` | MVP_REQUIRED | Fleet; active positive result becomes blocking | CRITICAL | Driver compliance/safety functional role | IN_APP, EMAIL | `DRIVER_DRUG_TEST_FAILED` | drug-test ID + blocking transition | test ID; no time suppression beyond idempotency | immediate EMAIL plus standard retry/fallback | all outcomes |
| `DRIVER_LICENSE_EXPIRING` | MVP_REQUIRED | Fleet; active licence reaches 30-day lead or expiry milestone | WARNING; CRITICAL on/after expiry | Driver compliance functional role | IN_APP, EMAIL | `DRIVER_LICENSE_EXPIRING` | licence ID + expiry date + milestone | licence + milestone + recipient + channel, 24 h | standard; CRITICAL bypass | all outcomes |
| `DRIVER_DRUG_TEST_EXPIRING` | NOT_REQUIRED | No current expiry/valid-until concept exists | n/a | n/a | n/a | n/a | n/a | n/a | n/a | catalogue decision only |
| `FUEL_LIMIT_EXCEEDED` | PHASE_2 | Rejected fuel-limit attempt | WARNING | Fuel control functional role | future decision | future | future | future | future | existing rejection/audit remains authoritative |
| `FUEL_EXCEPTION` | PHASE_2 | Undefined generic candidate | n/a | n/a | n/a | n/a | n/a | n/a | n/a | must be split into concrete events before promotion |

The three compliance scans run at least daily in the configured business timezone. The maintenance scan runs at least hourly. A producer must reuse a stable `eventId` when republishing the same catalogue milestone. The current free-form UI event list must be replaced by this catalogue; unlisted event types are rejected.

### Event variable allow-list

All templates may use `{{eventTime}}` and `{{severity}}`. Additional variables are:

| Template code | Allowed variables (required unless marked optional) |
|---|---|
| `TRIP_DELAY` | `tripId`, `tripNumber`, `delayMinutes`, `reason`, `locationDescription?` |
| `TRIP_INCIDENT` | `tripId`, `tripNumber`, `incidentSeverity`, `description`, `locationDescription?` |
| `VEHICLE_MAINTENANCE_DUE` | `vehicleId`, `vehicleRegistration`, `maintenanceType`, `scheduledStart`, `scheduledEnd` |
| `VEHICLE_DOCUMENT_EXPIRING` | `vehicleId`, `vehicleRegistration`, `documentId`, `documentType`, `documentNumber`, `expiryDate` |
| `DRIVER_EXCEPTION` | `driverId`, `driverName`, `exceptionId`, `exceptionType`, `startTime`, `endTime`, `reason?` |
| `DRIVER_MEDICAL_EXPIRING` | `driverId`, `driverName`, `medicalRecordId`, `validUntil`, `fitnessStatus` |
| `DRIVER_DRUG_TEST_FAILED` | `driverId`, `driverName`, `drugTestId`, `resultDate`, `testType` |
| `DRIVER_LICENSE_EXPIRING` | `driverId`, `driverName`, `licenseId`, `licenseNumber`, `licenseClass`, `expiryDate` |

## 6. Template Contract

`NotificationTemplate` contains: `id`, stable `code`, `name`, `eventType`, `channel`, `subject`, `body`, positive `version`, `active`, `createdAt`, and `updatedAt`.

- Templates are **system-managed for MVP** and installed/versioned by forward migration. Administrators select them but cannot edit their content.
- A rule references a stable template code compatible with its event type and channel. Processing resolves exactly one active version and snapshots template ID/version plus rendered subject/body on the notification.
- Subject is required for both channels and becomes the IN_APP title. Maximum subject length is 255 characters; body is required and limited to 4,000 characters after rendering.
- Templates are UTF-8 plain text. HTML, code execution, expressions, loops, conditions, reflection, and nested property traversal are forbidden. Output is treated as text and escaped by the eventual presentation/transport layer.
- Only exact `{{variableName}}` tokens from the catalogue allow-list are valid. Unknown tokens prevent activation/migration validation. Unknown event metadata not referenced by a template is ignored.
- Missing required data causes that rule execution to fail with `TEMPLATE_DATA_MISSING`; no delivery occurs and the failure is audited. Missing optional data renders as an empty string.
- Line endings are normalized. Control characters other than tab/newline are rejected.
- Updating content inserts a higher version and atomically switches the active version; historical notifications keep their rendered snapshot and template version. Exactly one active version per code/event/channel is allowed.

## 7. Recipient Contract

The existing recipient types are sufficient.

| Type | Validation | IN_APP resolution | EMAIL resolution |
|---|---|---|---|
| USER | exact existing active username | canonical username | active user's validated email; fail if absent |
| ROLE | exact existing role; at execution resolve active members | one notification per distinct canonical username | one delivery per distinct validated member email |
| EMAIL_ADDRESS | syntactically valid normalized address | **invalid combination** | normalized address |

- Notification defines a minimal `NotificationRecipientDirectoryPort`; an adapter may use an Identity public interface. Notification must not import Identity domain, repositories, JPA entities, or application services.
- Rule create/update rejects an invalid recipient/channel combination, missing user/role, inactive USER, or malformed email with the existing `ApiError` format.
- A role may temporarily resolve to zero active recipients. The event is not failed globally; the rule execution records `NO_RECIPIENT`, creates no notification, and exposes the diagnostic to rule managers.
- Resolution de-duplicates canonical usernames/email addresses within a rule. Separate rules remain separate intentional policies; ordinary idempotency and each rule's suppression key still apply.
- EMAIL_ADDRESS never creates an IN_APP notification because it has no authenticated identity.

## 8. Severity Contract

The sole ordering is the existing `INFO < WARNING < CRITICAL`. No duplicate severity enum is introduced internally; the public boundary's values map one-to-one to the domain enum.

- A rule matches when `eventSeverity >= severityThreshold`.
- CRITICAL EMAIL bypasses quiet hours and is attempted immediately.
- Severity does not alter the three-attempt retry count.
- Escalation occurs only after terminal EMAIL failure; for CRITICAL it is created immediately after the final failed attempt.
- IN_APP is persisted immediately for every matched, non-suppressed event regardless of quiet hours.

## 9. Quiet-Hours Contract

- Quiet hours are optional per EMAIL rule and use one deployment business timezone configured by required/validated IANA zone property `app.notification.time-zone`, defaulting to `UTC` when not supplied. Database instants remain UTC.
- A policy contains start time, end time, and one or more ISO days of week. Start equal to end is invalid. Start before end is same-day; start after end is an overnight interval attributed to its start day.
- When a non-CRITICAL EMAIL falls in the interval, the rendered notification is persisted as PENDING with `nextDeliveryAt` equal to the first instant after the active interval. It is queued, not suppressed.
- CRITICAL EMAIL bypasses the interval. IN_APP is always persisted/delivered immediately.
- The event severity and a snapshot of the matched policy govern a queued notification even if the rule is later edited or disabled. Disabling a rule prevents new matches but does not erase an already-audited delivery obligation.
- Invalid or ambiguous local times use the timezone library's next valid instant; overlap chooses the earlier offset. Tests must cover overnight and daylight-saving behavior even though UTC has none.

## 10. Suppression Contract

Idempotency prevents reprocessing the same event. Suppression prevents repeated equivalent events with different event IDs.

- Suppression key: `ruleId + eventType + aggregateType + aggregateId + resolvedRecipient + channel + catalogue milestone (when present)`.
- Window is configured per rule from 0 to 1,440 minutes. Zero disables time suppression. Catalogue defaults are shown above.
- The window begins at the first accepted notification's `createdAt`; it is not extended by suppressed repeats.
- Suppression is per recipient and channel. A suppressed EMAIL does not suppress a separately configured IN_APP rule.
- CRITICAL events are not time-suppressed, but stable-event idempotency still applies.
- A suppressed match creates no delivery notification; `notification_rule_execution` records `SUPPRESSED`, the key, the controlling notification, and timestamps.

## 11. Escalation Contract

MVP escalation means **only escalation after terminal EMAIL delivery failure**.

- An EMAIL rule configures zero or one fallback recipient of type USER or ROLE and a delay from 0 to 60 minutes. EMAIL_ADDRESS is invalid for fallback because escalation is IN_APP.
- An EMAIL rule capable of matching CRITICAL events must have an active fallback. Other EMAIL rules may opt in.
- After the third failed attempt, the original EMAIL becomes FAILED. One child IN_APP notification is created for each resolved fallback recipient, linked to the failed notification with escalation level 1.
- Maximum level is fixed at 1. Delivery success before terminal failure cancels escalation. Persisting the fallback IN_APP notification terminates escalation.
- Unread age, event age, acknowledgement workflows, multi-level trees, and generic approvals are deferred.

## 12. Retry Contract

- EMAIL uses at most three total attempts: attempt 1 immediately (or after quiet hours), attempt 2 after 1 minute, and attempt 3 after a further 2 minutes. Maximum configured delay is 15 minutes; MVP values are system policy, not per-rule inputs.
- Retryable: connection/timeout/interruption, HTTP 408/429, provider throttling, and provider/server 5xx. Non-retryable: invalid recipient, authentication/configuration failure, template/recipient validation, and other provider 4xx.
- Each attempt is durably inserted before calling the sender and completed with timestamps, outcome, sanitized error category/code/message, and provider message ID when available.
- A retryable failure leaves the notification PENDING and sets `nextDeliveryAt`. A non-retryable failure, or retryable failure on attempt 3, sets FAILED and clears `nextDeliveryAt`.
- SENT means the real adapter/provider accepted the message and returned success. Logging, disabled delivery, mock mode, or merely invoking an adapter can never produce SENT in production mode.
- READ applies only to an IN_APP notification after SENT. No additional notification status is required: quiet/retry work remains PENDING; suppression/no-recipient outcomes belong to rule-execution audit.
- Automatic recovery after a process restart is required. Manual retry is deferred; terminal failures remain visible and immutable.

## 13. Email Contract

Notification owns an `EmailNotificationSenderPort` whose request contains notification ID, idempotency key, from, to, subject, plain-text body, and timeout; its result contains accepted/rejected status and provider message ID/error classification. `NotificationDeliveryPort` orchestration may delegate to this narrower sender port.

The infrastructure adapter may later use SMTP or a provider API; this contract does not select a vendor. Configuration follows current environment-driven conventions:

- `app.notification.email.enabled` (production must explicitly enable);
- `app.notification.email.mode` = `production` or `test`; no implicit fallback;
- `app.notification.email.provider` = adapter selector such as `smtp`;
- `app.notification.email.from` and optional reply-to;
- connect/read timeout with bounded values;
- endpoint/host/port and TLS settings;
- credentials only through environment/secret injection, never repository defaults or logs.

Production startup fails fast when EMAIL is enabled but provider, sender, or credentials are invalid. Test mode records a deterministic test outcome and cannot be enabled by the production profile. Disabled delivery returns a non-success result and must not mark SENT. Logs redact credentials and minimize recipient/message content.

## 14. Module Boundaries

```text
Owning module
  -> publishes notification.OperationalNotificationEvent
  -> Notification listener/application service
  -> rule/template/policy engine
  -> Notification-owned output ports
  -> persistence and channel adapters
```

Trip, Fleet, and Fuel may import only the public `com.transportlogistics.app.notification.OperationalNotificationEvent` contract (or a later public notification publisher interface). They must not import `notification.domain`, `notification.application`, persistence, or adapters.

The stable public event requires: stable `eventId`, catalogue `eventType`, `aggregateType`, non-null `aggregateId`, mapped severity, `occurredAt`, and immutable string metadata containing the catalogue variables. Existing title/message remain transitional fallback fields; template rendering becomes authoritative. Publication occurs only after the owning operation is accepted. Notification processing failure is audited and must not roll back the owning business transaction.

Identity interaction occurs through a minimal public recipient-directory contract/adaptor, never direct Identity repository/service access. Architecture suites must continue to enforce both boundaries.

## 15. Persistence Plan

V25 is immutable. Proposed forward migrations are deliberately separated:

### V26 — templates and rule selection

- `notification_template`: UUID PK; code/name/event_type/channel/subject/body/version/active/audit timestamps; unique `(code, channel, version)`; index `(event_type, channel, active)`; constraint on positive version and lengths.
- Add `template_code` to `notification_rule`, backfill catalogue-compatible rules, then enforce compatibility in application validation.
- Add `template_id`, `template_version`, and rendered content snapshot metadata to `notification` while retaining current title/message columns.

### V27 — policy and rule-execution audit

- `notification_rule_policy`: one-to-one rule FK; quiet enabled/start/end; suppression minutes; escalation enabled/delay/recipient type/value; audit timestamps/version.
- `notification_rule_quiet_day`: policy FK plus ISO day, unique `(policy_id, day_of_week)`.
- `notification_rule_execution`: event/rule/outcome, suppression key, controlling notification, failure code/message, created/completed timestamps; unique event/rule execution and indexes on outcome/time and suppression key/time.

### V28 — durable delivery and escalation

- `notification_delivery_attempt`: notification FK, attempt number, state, due/started/completed timestamps, error category/code/message, provider message ID; unique `(notification_id, attempt_number)` and due-work index.
- Add `next_delivery_at`, `parent_notification_id`, and `escalation_level` to `notification`; indexes on `(status, next_delivery_at)` and parent.

Workers claim due PENDING rows transactionally using database-supported locking (`FOR UPDATE SKIP LOCKED` on PostgreSQL or an equivalent tested claim token), then rely on unique attempt numbers and provider idempotency keys. Rule execution/suppression insertion must be atomic. Optimistic versioning or a claim token prevents double completion. No migration may edit V25 or combine unrelated module schema.

## 16. API Plan

Existing paths remain and use the existing permissions and `ApiError` contract.

| Method/path | Permission | Contract change / response | Validation and errors |
|---|---|---|---|
| `GET /notification-event-catalogue` | `NOTIFICATION_RULE_VIEW` | controlled event types, variables, channels, default policy | no free-form event creation |
| `GET /notification-templates?eventType=&channel=` | `NOTIFICATION_RULE_VIEW` | active system templates for selection | event/channel required together when filtering |
| `GET /notification-templates/{id}` | `NOTIFICATION_RULE_VIEW` | template metadata/content | 404 unknown/inactive as appropriate |
| `POST /notification-rules` | `NOTIFICATION_RULE_MANAGE` | extend request/response with template and policy | catalogue/channel/template/recipient compatibility; 400 stable field errors |
| `PUT /notification-rules/{id}` | `NOTIFICATION_RULE_MANAGE` | same fields; preserve ID/audit | 404; 409 on stale update if versioning is exposed |
| existing list/get/enable/disable/delete | existing permissions | include template/policy summary; preserve behavior | disabling affects new matches only |
| `GET /notifications` | `NOTIFICATION_VIEW` | add template version, attempt count, next delivery, terminal failure and parent link when visible | still recipient-scoped and bounded |
| existing unread/read/read-all | `NOTIFICATION_VIEW` | IN_APP only; preserve paths | EMAIL read mutation returns 409 |
| `GET /notification-deliveries?status=&eventType=&from=&to=` | `NOTIFICATION_RULE_VIEW` | paged operational delivery diagnostics | redact external recipient details unless manager is authorized |
| `GET /notification-deliveries/{id}/attempts` | `NOTIFICATION_RULE_VIEW` | ordered sanitized attempts | 404/403; never return credentials/provider secrets |

No MVP template mutation or manual retry endpoint is added. Existing clients that omit new rule fields during rollout resolve the catalogue default template/policy; responses always return the resolved values. New UI submits them explicitly.

Stable errors include `NOTIFICATION_EVENT_UNSUPPORTED`, `NOTIFICATION_TEMPLATE_INCOMPATIBLE`, `NOTIFICATION_RECIPIENT_INVALID`, `NOTIFICATION_RECIPIENT_NOT_FOUND`, `NOTIFICATION_CHANNEL_RECIPIENT_INCOMPATIBLE`, `NOTIFICATION_POLICY_INVALID`, and `NOTIFICATION_NOT_READABLE`.

## 17. Frontend Plan

Use existing Ant Design and TanStack Query patterns; do not redesign the application shell.

- `NotificationRulesPage`: load catalogue/templates, show channel, template, threshold, quiet/suppression/escalation summary, enabled state, and last delivery health. Preserve permission-gated actions.
- `NotificationRuleModal`: add required IN_APP/EMAIL selection; catalogue-backed event selection; compatible system template selection/preview; recipient validation; EMAIL-only quiet days/start/end; suppression window; and single fallback USER/ROLE configuration. Hide irrelevant fields rather than posting conflicting values.
- `NotificationCenter`: preserve bell/list/read behavior; show severity, channel, sent/pending/failed state, failure reason appropriate to the recipient, and queued retry time. READ actions remain IN_APP only.
- Add a small delivery-diagnostics surface to the rule page or drawer for `NOTIFICATION_RULE_VIEW`; do not create a separate administration application.
- Backend validation remains authoritative. Map field errors to the form. Use `App.useApp()` feedback and no browser alert/confirm/prompt.

## 18. Final Acceptance Criteria

1. **AC-77-01 Catalogue:** Given rule management is opened, when catalogue data is requested, then only the frozen MVP_REQUIRED event types and their compatible channels/variables are offered.
2. **AC-77-02 Rule CRUD:** Given an actor has `NOTIFICATION_RULE_MANAGE`, when a valid catalogue-backed rule is created, updated, enabled, disabled, or deleted, then the operation persists and is visible through the existing rule API.
3. **AC-77-03 Authorization:** Given an unauthenticated actor or an actor without the required notification permission, when a protected rule, notification, or delivery endpoint is called, then it returns 401 or 403 before mutation.
4. **AC-77-04 Match:** Given an enabled rule with an exact event type and threshold, when an event at or above that threshold is published, then the rule is evaluated exactly once for that stable event ID.
5. **AC-77-05 No match:** Given a disabled rule, different event type, or event below threshold, when the event is published, then no recipient notification is created and the decision is auditable where applicable.
6. **AC-77-06 Template:** Given a matched rule, when its active compatible template is rendered, then only allow-listed variables are substituted and the template ID/version and rendered text are snapshotted.
7. **AC-77-07 Template failure:** Given required template data is absent, when rendering occurs, then no delivery is attempted and `TEMPLATE_DATA_MISSING` is audited without rolling back the source operation.
8. **AC-77-08 USER recipient:** Given USER targets an active user, when the rule executes, then IN_APP uses the canonical username and EMAIL uses that user's validated email.
9. **AC-77-09 ROLE recipient:** Given ROLE resolves multiple active users, when the rule executes, then one notification is created per distinct resolved channel recipient.
10. **AC-77-10 Zero recipient:** Given a ROLE resolves no active eligible user, when the rule executes, then it records `NO_RECIPIENT`, creates no delivery, and does not fail the source operation.
11. **AC-77-11 Compatibility:** Given EMAIL_ADDRESS is paired with IN_APP or USER EMAIL has no email, when the rule is validated, then it is rejected with a stable field/business error.
12. **AC-77-12 IN_APP:** Given a matched, non-suppressed IN_APP rule, when processed, then the notification is persisted immediately, appears for the recipient, and affects unread count.
13. **AC-77-13 Read state:** Given a recipient owns an unread IN_APP notification, when read or read-all is invoked, then the state becomes READ once and another user cannot mutate it.
14. **AC-77-14 Quiet hours:** Given a non-CRITICAL EMAIL rule is inside its configured quiet interval, when processed, then it remains PENDING until the calculated end instant while IN_APP remains immediate.
15. **AC-77-15 Critical bypass:** Given a CRITICAL EMAIL event occurs during quiet hours, when processed, then its first EMAIL attempt is due immediately.
16. **AC-77-16 Idempotency:** Given the same stable event is republished, when the same rule/recipient is evaluated, then no duplicate notification or delivery attempt is created.
17. **AC-77-17 Suppression:** Given equivalent events with different IDs occur inside a rule's window, when the later event is evaluated, then it is recorded as SUPPRESSED and creates no notification; CRITICAL bypasses time suppression.
18. **AC-77-18 Email success:** Given production EMAIL is validly configured and the provider accepts delivery, when the sender returns success, then the notification becomes SENT with a successful attempt and provider message ID.
19. **AC-77-19 No false SENT:** Given EMAIL is disabled, misconfigured, in logging-only behavior, or rejected, when delivery is attempted, then it cannot become SENT.
20. **AC-77-20 Retry:** Given a retryable EMAIL failure, when attempts run, then at most three durable attempts occur at the frozen schedule and processing survives application restart.
21. **AC-77-21 Terminal failure:** Given a non-retryable failure or failed third attempt, when completion is recorded, then the notification becomes FAILED with sanitized reason and no next-delivery time.
22. **AC-77-22 Escalation:** Given an EMAIL rule has a fallback and delivery becomes terminally FAILED, when its configured delay elapses, then exactly one level of linked IN_APP fallback notifications is created.
23. **AC-77-23 Production events:** Given each MVP_REQUIRED owning-module trigger occurs, when its transaction is accepted, then it publishes one stable public event with catalogue metadata without importing notification internals.
24. **AC-77-24 Audit:** Given matching, suppression, no-recipient, render failure, attempt, retry, terminal failure, or escalation occurs, when queried by an authorized manager, then timestamps and sanitized outcomes are available without secrets.
25. **AC-77-25 UI:** Given an authorized administrator uses the rule UI, when configuring a rule, then all and only applicable MVP fields can be submitted and backend field errors appear on the form.
26. **AC-77-26 Regression:** Given US-77 implementation is complete, when all release gates run, then backend, architecture, frontend, and three-browser suites remain green and notification-specific E2E cases pass.

## 19. Test and Playwright Plan

Backend tests must cover domain validation/rendering, time-zone/overnight quiet calculations, suppression vs idempotency, recipient resolution, retry classification/scheduling/restart, concurrent worker claims, escalation uniqueness, real sender contract, controller validation/security, persistence constraints, scheduled producer idempotency, and Spring Modulith boundaries. Frontend tests cover conditional fields, catalogue/template loading, field-error mapping, permission controls, delivery state, and center behavior.

Retained MVP Playwright cases:

| ID | Scenario |
|---|---|
| E2E-NOT-001 | Authorized administrator opens Notification Rules |
| E2E-NOT-002 | Create IN_APP rule from catalogue/template |
| E2E-NOT-003 | Edit rule |
| E2E-NOT-004 | Enable and disable rule |
| E2E-NOT-005 | Unauthorized role cannot view/manage rules; mutation is absent |
| E2E-NOT-006 | MVP operational event triggers a resolved notification |
| E2E-NOT-007 | Unread badge increments |
| E2E-NOT-008 | Open Notification Center and inspect notification |
| E2E-NOT-009 | Mark one notification read |
| E2E-NOT-010 | Mark all notifications read |
| E2E-NOT-011 | Configure EMAIL rule including template/policies |
| E2E-NOT-012 | EMAIL delivery failure never reports false SENT |
| E2E-NOT-013 | Quiet-hours EMAIL is PENDING while IN_APP is immediate |
| E2E-NOT-014 | Suppression prevents repeated equivalent notification and is audited |
| E2E-NOT-015 | Transient EMAIL failure retries and succeeds |

Provider behavior is controlled through an explicit E2E test adapter, not a real external email account. SMS/PUSH/WEBHOOK cases are excluded.

## 20. Traceability Matrix

`PLANNED` means not yet implemented.

| Acceptance | Domain component | Application service | Persistence | API | Frontend | Backend test | Frontend test | Playwright | Slice |
|---|---|---|---|---|---|---|---|---|---|
| AC-77-01,02,05 | catalogue + existing rule | rule service | rule/V26 | catalogue + existing rule APIs | rules/modal | rule/controller | rules page/modal | NOT-001..004 | 008B/G/H |
| AC-77-03 | existing permissions | security chain | V25 | all protected endpoints | permission gates | security integration | permission UI | NOT-005 | 008B-G/H |
| AC-77-04,16 | eight stable public operational events IMPLEMENTED | rule engine IMPLEMENTED | V27 execution audit IMPLEMENTED | execution diagnostics IMPLEMENTED | state display IMPLEMENTED | producer stability + engine idempotency PASS | indirect | NOT-006 PASS | 008B/C/E/G/H/I COMPLETE |
| AC-77-06,07 | template IMPLEMENTED | renderer IMPLEMENTED | V26 IMPLEMENTED | template API IMPLEMENTED | selection/preview IMPLEMENTED | renderer/integration PASS | modal PASS | NOT-002/011 PASS | 008B/G/H/I COMPLETE |
| AC-77-08..11 | recipient policy IMPLEMENTED | recipient resolver IMPLEMENTED | execution audit IMPLEMENTED | rule validation IMPLEMENTED | conditional recipient fields IMPLEMENTED | resolver/controller PASS | modal PASS | NOT-002/011 PASS | 008B/G/H/I COMPLETE |
| AC-77-12,13 | existing Notification | existing notification service | existing notification table | existing notifications API | existing center | service/integration PASS | center PASS | NOT-006..010 PASS | 008H/I COMPLETE |
| AC-77-14,15 | quiet policy IMPLEMENTED | deterministic evaluator IMPLEMENTED | V27 policy/quiet-day/next-delivery IMPLEMENTED | additive rule policy IMPLEMENTED | quiet fields IMPLEMENTED | clock/time-zone/DST PASS | modal/status PASS | NOT-013 PASS | 008C/D/G/H/I COMPLETE |
| AC-77-17 | suppression policy IMPLEMENTED | evaluator + policy-row locking IMPLEMENTED | V27 execution audit/index IMPLEMENTED | audit query IMPLEMENTED | policy fields IMPLEMENTED | concurrency/window/critical PASS | modal PASS | NOT-014 PASS | 008C/G/H/I COMPLETE |
| AC-77-18,19 | provider-neutral sender result IMPLEMENTED | configured real SMTP transport IMPLEMENTED | V28 provider evidence IMPLEMENTED | sanitized typed failures IMPLEMENTED | delivery status IMPLEMENTED | real local-SMTP/worker integration PASS | status UI PASS | NOT-012 PASS | 008F/G/H/I COMPLETE |
| AC-77-20,21 | attempt policy IMPLEMENTED | durable retry worker + real SMTP mapping IMPLEMENTED | V28 attempts IMPLEMENTED | delivery/attempt diagnostics IMPLEMENTED | retry/failure state IMPLEMENTED | timing/restart/concurrency/provider PASS | status UI PASS | NOT-015 PASS | 008D/F/G/H/I COMPLETE |
| AC-77-22 | escalation policy IMPLEMENTED | durable one-level escalation IMPLEMENTED | V27 policy/V28 linkage IMPLEMENTED | rule/diagnostics IMPLEMENTED | fallback fields IMPLEMENTED | uniqueness/resolution PASS | modal PASS | NOT-012 PASS | 008D/G/H/I COMPLETE |
| AC-77-23 | public contract + exactly eight producers IMPLEMENTED | Trip mutation producers + Fleet mutation/scanner producers IMPLEMENTED | no new schema after V28 | operational APIs unchanged | existing operation UI | producer/scanner/failure-isolation/module PASS | existing feature tests | NOT-006 PASS | 008E/H/I COMPLETE |
| AC-77-24 | rule execution + attempts + producer/provider facts IMPLEMENTED | rule/delivery/attempt diagnostics IMPLEMENTED | V27 execution/V28 attempts IMPLEMENTED | authorized audit APIs IMPLEMENTED | diagnostics IMPLEMENTED | persistence/security/producer/SMTP PASS | diagnostics PASS | NOT-012..015 PASS | 008C/D/E/F/G/H/I COMPLETE |
| AC-77-25 | n/a | existing/new APIs | n/a | field errors IMPLEMENTED | modal/page IMPLEMENTED | controller PASS | component PASS | NOT-002/011 PASS | 008G/H/I COMPLETE |
| AC-77-26 | all | all | V26-V28 | all | all | clean test/verify PASS | lint 0 warnings; 106/106; build PASS | 45/45 notification; 156/156 full | 008I COMPLETE |

## 21. Implementation Slices

### MVP-GAP-008B — Templates and rule configuration

- **Status:** COMPLETE (2026-08-21).
- **Objective:** controlled catalogue, system templates, renderer, recipient validation, compatible rule contract.
- **Likely files/modules:** notification domain/application/web/persistence/config; minimal Identity public directory boundary and adapter; notification frontend types/hooks only if API compilation requires, otherwise defer UI.
- **Database/API:** V26; catalogue/template GET; additive rule fields with compatibility defaults.
- **Tests:** template/variable/version, recipient/channel, controller, persistence, security, module boundary.
- **Dependencies:** this contract.
- **Done:** AC-77-01..11 pass without implementing quiet/retry/email transport UI.
- **Implementation evidence:** `NotificationEventCatalogue` exposes exactly eight controlled events; `NotificationTemplate` and `NotificationTemplateRenderer` enforce version, compatibility, allow-list, required/optional data, length, line-ending, and control-character rules; `NotificationRecipientResolver` uses the public Identity `NotificationRecipientDirectory` boundary; `NotificationRuleService` validates catalogue/template/recipient compatibility; `NotificationRuleEngine` resolves recipients, renders content, snapshots template ID/version and isolates failures; `V26__notification_catalogue_templates.sql` creates and seeds 16 system template versions and adds rule/notification linkage; catalogue and template query APIs are protected by `NOTIFICATION_RULE_VIEW`.
- **Verification evidence:** backend `clean test` and `verify` each ran 547 tests with 0 failures/errors and 21 skipped; architecture 15/15 PASS; Spring context PASS; H2 Flyway V1-V26 PASS; frontend lint, 94/94 unit tests, and build PASS. The existing Playwright suite ran 110/111; the same pre-existing Firefox operational-incident selector failure reproduced in isolation while Chromium/WebKit passed, and no E2E source was changed by 008B.

### MVP-GAP-008C — Quiet hours and suppression

- **Status:** COMPLETE (2026-08-21).
- **Objective:** deterministic policy evaluation and auditable repeated-event suppression.
- **Likely files:** notification domain/application/persistence/web config.
- **Database/API:** V27 policy, quiet-day, rule-execution tables; additive rule policy fields.
- **Tests:** time zones, overnight/DST, severity bypass, window boundary, concurrency and audit.
- **Dependencies:** 008B.
- **Done:** AC-77-14,15,17 pass.
- **Implementation evidence:** `NotificationRulePolicy`, `NotificationQuietHoursEvaluator`, `NotificationSuppressionKey`, `NotificationSuppressionEvaluator`, and recipient-granular `NotificationRuleExecution`; `NotificationRuleEngine` policy-row locking, stable-event idempotency, quiet EMAIL queuing, critical bypass, and audit outcomes; additive rule policy fields and protected `GET /notification-rule-executions`; V27 policy, quiet-day, execution, and `notification.next_delivery_at` persistence. V28 will reuse the sequencing-adjusted next-delivery column.
- **Verification evidence:** backend clean test and verify each ran 579 tests with 0 failures/errors and 21 skipped; architecture 15/15 PASS; Spring context PASS; H2 Flyway V1-V27 PASS; frontend lint, 94/94 unit tests, and build PASS. The full Playwright run was 110/111 with a transient Firefox logout redirect timeout that passed 1/1 in isolation; no reproducible 008C regression was found.

### MVP-GAP-008D — Escalation and durable retry

- **Status:** COMPLETE (2026-08-22).
- **Objective:** durable attempt state, automatic recovery, bounded retry, one-level terminal-failure escalation.
- **Likely files:** notification domain/application scheduler/ports/persistence/config/web diagnostics.
- **Database/API:** V28 attempts and notification scheduling/link columns; diagnostics GET.
- **Tests:** error classification, timing, restart, concurrent claims, terminal state, escalation uniqueness.
- **Dependencies:** 008B/C.
- **Done:** AC-77-20..22 and attempt-audit portion of AC-77-24 pass with a deterministic fake sender.
- **Implementation evidence:** `NotificationDeliveryAttempt`, `NotificationEmailRetryPolicy`, typed `EmailDeliveryErrorCategory`, `EmailNotificationSenderPort`, short-transaction `NotificationEmailDeliveryClaimService`, scheduled `NotificationEmailDeliveryWorker`, and `NotificationEscalationService`; stable `<notificationId>:<attemptNumber>` idempotency, stale same-attempt recovery, pessimistic notification claims, exactly three attempts with +1/+2 minute backoff, terminal FAILED state, and linked level-1 IN_APP fallback. `V28__notification_email_delivery_attempts.sql` adds attempt audit and parent/level linkage while reusing V27 `next_delivery_at`. Protected delivery and attempt diagnostics provide bounded sanitized evidence.
- **Verification evidence:** backend clean test and verify each ran 610 tests with 0 failures/errors and 21 skipped; architecture 15/15 PASS; Spring context PASS; H2 Flyway V1-V28 PASS; frontend lint, 94/94 unit tests, and build PASS; Playwright 111/111 PASS across Chromium, Firefox, and WebKit.

### MVP-GAP-008E — Required operational event producers

- **Status:** COMPLETE (2026-08-22).
- **Objective:** publish all eight MVP_REQUIRED events with stable IDs and catalogue metadata.
- **Likely files:** Trip and Fleet application services/public event adapters; scheduled scan configuration; notification catalogue tests. Fuel is not changed for MVP.
- **Database/API/frontend:** no notification schema beyond prior slices; existing source APIs/UI remain.
- **Tests:** one producer test per trigger, scheduled scan idempotency, rollback isolation, module boundaries.
- **Dependencies:** 008B/C; scheduled events need suppression/execution audit.
- **Done:** AC-77-23 passes for every required catalogue row.
- **Implementation evidence:** `TripOperationalEventService` publishes stable accepted delay and incident events; `DriverExceptionService` and `DriverDrugTestService` publish blocking mutation facts; `MaintenanceDueNotificationScanner` and `ComplianceNotificationScanner` publish restart-safe time milestones through `FleetOperationalNotificationPublisher`. `FleetOperationalNotificationEvents` derives deterministic name-based UUIDs from immutable source and milestone inputs, emits frozen metadata/severity, and preserves `DUE_24H`, `D30`, `EXPIRED`, and blocking-transition identity. Focused repository ports keep JPA and Notification internals outside application scanners. No V29, API, or frontend change was introduced.
- **Verification evidence:** backend clean test and verify each passed with 625 tests (604 passed, 21 skipped); architecture 15/15 PASS; Spring context PASS; H2 Flyway V1-V28 PASS; frontend lint, 94/94 unit tests, and build PASS; Playwright 111/111 PASS across Chromium, Firefox, and WebKit.

### MVP-GAP-008F — Production EMAIL delivery

- **Status:** COMPLETE (2026-08-22).
- **Objective:** provider-neutral sender port plus one real, configured infrastructure adapter with fail-fast production behavior.
- **Likely files:** notification output ports, delivery orchestration/adapter/config, application profiles.
- **Database/API/frontend:** uses V28 attempts; no vendor-specific public API.
- **Tests:** adapter contract, timeout/error mapping, disabled/misconfigured behavior, secret redaction, integration with a local test server.
- **Dependencies:** 008D.
- **Done:** AC-77-18,19 pass and no logging path can report SENT.
- **Implementation evidence:** `SmtpEmailNotificationSenderAdapter` provides one UTF-8 plain-text SMTP transport behind the preserved `EmailNotificationSenderPort`; `NotificationEmailConfiguration` selects exactly one disabled, test, or production sender; `NotificationEmailProperties` validates mode/profile/provider/from/host/port/TLS/credentials/bounded timeouts and redacts secrets. SMTP acceptance alone permits `SENT` and its Jakarta Mail message ID is persisted through V28. Typed SMTP/connection/timeout/authentication/recipient/TLS failures feed the existing 008D retry and escalation lifecycle. SMTP's at-least-once uncertain-completion risk is documented in `MVP-GAP-008F-IMPLEMENTATION.md`.
- **Verification evidence:** backend clean test and verify each passed with 644 tests (623 passed, 21 skipped); architecture 15/15 PASS; Spring context PASS; H2 Flyway V1-V28 PASS; frontend lint, 94/94 unit tests, and build PASS; Playwright 111/111 PASS across Chromium, Firefox, and WebKit.

### MVP-GAP-008G — Frontend completion

- **Status:** COMPLETE.
- **Objective:** configure every applicable rule field and display delivery health using existing Ant Design UX.
- **Likely files:** notification page/modal/center, hooks/types, route-adjacent component tests.
- **Database/API:** none beyond B-F.
- **Tests:** conditional fields, catalogue/templates, validation mapping, permissions, delivery state.
- **Dependencies:** 008B-F APIs stable.
- **Done:** AC-77-25 and frontend portions of all traceability rows pass; lint/test/build green.
- **Implementation evidence:** the existing rule page/modal and query layer now consume the controlled catalogue and filtered templates, expose IN_APP/EMAIL, USER/ROLE/EMAIL_ADDRESS compatibility, severity, read-only previews, quiet days/hours, suppression, and mandatory EMAIL USER/ROLE fallback while excluding hidden stale fields. `NotificationDeliveryDiagnostics` presents bounded filtered delivery health and sanitized attempt history without manual retry. The Notification Center remains recipient-scoped and permission-gated.
- **Verification evidence:** frontend lint/build and 106/106 unit/component tests PASS; backend clean test and verify each PASS with 644 tests (623 passed, 21 skipped); architecture 15/15, Spring context, JPA, and Flyway V1-V28 PASS; existing Playwright regression 111/111 PASS across Chromium, Firefox, and WebKit. Visual inspection covered the rules page, full modal, expanded EMAIL policies, diagnostics, and center. See `MVP-GAP-008G-IMPLEMENTATION.md`.

### MVP-GAP-008H — Notification-specific Playwright

- **Status:** COMPLETE (2026-08-22).
- **Objective:** implement E2E-NOT-001 through E2E-NOT-015 with deterministic test delivery/time controls.
- **Likely files:** `frontend/e2e/tests/notifications/**` and narrowly scoped harness fixtures/config.
- **Database/API/frontend:** no production expansion.
- **Tests:** all retained notification cases in Chromium, Firefox, and WebKit.
- **Dependencies:** 008B-G.
- **Done:** all notification cases pass through the repository-standard self-starting command.
- **Implementation evidence:** four notification spec files plus page objects and API fixtures cover rule CRUD/toggle/RBAC, a real Trip operational event, recipient-scoped center/read state, EMAIL configuration, terminal failure, quiet hours, suppression audit, and transient retry. An `e2e`-only adjustable clock, deterministic sender, worker trigger, and security-protected test controller provide bounded deterministic controls with no production-profile exposure. The modal initialization race discovered by WebKit was fixed by initializing once per open.
- **Verification evidence:** notification-only Playwright passed 45/45: Chromium 15/15, Firefox 15/15, WebKit 15/15. Frontend lint, 106/106 unit/component tests, and build pass; focused module/layer/harness verification passes 12/12. See `MVP-GAP-008H-IMPLEMENTATION.md`.

### MVP-GAP-008I — Regression and US-77 closure

- **Status:** COMPLETE (2026-08-22).
- **Objective:** full story traceability and release regression verification.
- **Likely files:** MVP/QA documentation only unless an in-scope defect is found and separately approved.
- **Database/API/frontend:** none planned.
- **Tests:** Maven clean verify, 15 architecture tests, startup/Flyway, frontend lint/unit/build, full three-browser Playwright.
- **Dependencies:** 008B-H.
- **Done:** AC-77-26 passes, no unresolved required criterion remains, and only then US-77 becomes COMPLETE.
- **Verification evidence:** backend clean test and verify each pass 647 tests (626 passed, 21 skipped, 0 failures/errors); architecture 15/15; Spring context, Security, Notification, Trip, Fleet, JPA (44 repositories), and H2 Flyway V1-V28 pass; frontend lint, 106/106 tests, and build pass; notification Playwright passes 45/45 and the complete three-browser regression passes 156/156. Default E2E concurrency was bounded from four to three after reproducible full-suite contention; retries, timeouts, sleeps, and assertions remain unchanged.

## 22. Definition of Done

US-77 is complete only when:

1. every AC-77 criterion is implemented and traceable to passing automated evidence;
2. only the eight MVP_REQUIRED catalogue events are exposed for MVP and each has a production producer;
3. IN_APP remains correct and EMAIL has a real provider-confirmed success path with no false SENT state;
4. templates, recipient validation, quiet hours, suppression, retry, terminal-failure escalation, and audit follow this frozen contract;
5. V25 remains unchanged and all new schema uses reviewed V26+ forward migrations on H2 and PostgreSQL;
6. module boundaries remain green and no owning module imports notification internals;
7. notification UI and E2E-NOT-001..015 are complete;
8. full backend, architecture, startup/Flyway, frontend, and three-browser gates pass;
9. human reviewers approve any provider selection, schema, public boundary expansion, and final diff.

MVP-GAP-008A completed only the contract freeze; MVP-GAP-008I supplied the final evidence that changes US-77 to COMPLETE.
