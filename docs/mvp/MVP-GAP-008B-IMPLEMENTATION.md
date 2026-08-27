# MVP-GAP-008B Implementation Record

## Status

**MVP-GAP-008B: COMPLETE**

**US-77: PARTIAL**. This slice implements only the controlled catalogue, system templates, rendering, recipient validation/directory boundary, template-backed rule configuration, query APIs, and V26. Quiet hours, suppression, retry, escalation, real EMAIL delivery, remaining event producers, full frontend completion, notification E2E, and final story closure remain deferred.

## Scope Implemented

- Exactly eight controlled MVP notification event definitions with owning module, default severity, supported channels, stable template code, and required/optional variable allow-lists.
- System-managed, plain-text, versioned `NotificationTemplate` records for IN_APP and EMAIL.
- A deterministic `{{variableName}}` renderer with no expression engine or reflection.
- Active compatible template selection by stable rule `templateCode`.
- Rendered title/body plus `templateId` and `templateVersion` snapshot on each new notification.
- USER, ROLE, and EMAIL_ADDRESS validation/resolution through a public Identity module boundary.
- Deterministic zero-member ROLE behavior that creates no fake notification and does not fail the source operation.
- Read-only catalogue/template APIs and additive rule request/response fields.
- V26 schema and deterministic seeding of 16 version-1 templates.

## Controlled MVP Catalogue

| Event | Module | Default severity | Channels | Template code |
|---|---|---|---|---|
| `TRIP_DELAY_RECORDED` | trip | WARNING | IN_APP, EMAIL | `TRIP_DELAY` |
| `TRIP_INCIDENT_RECORDED` | trip | WARNING | IN_APP, EMAIL | `TRIP_INCIDENT` |
| `VEHICLE_MAINTENANCE_DUE` | fleet | WARNING | IN_APP, EMAIL | `VEHICLE_MAINTENANCE_DUE` |
| `VEHICLE_DOCUMENT_EXPIRING` | fleet | WARNING | IN_APP, EMAIL | `VEHICLE_DOCUMENT_EXPIRING` |
| `DRIVER_EXCEPTION_RECORDED` | fleet | WARNING | IN_APP, EMAIL | `DRIVER_EXCEPTION` |
| `DRIVER_MEDICAL_EXPIRING` | fleet | WARNING | IN_APP, EMAIL | `DRIVER_MEDICAL_EXPIRING` |
| `DRIVER_DRUG_TEST_FAILED` | fleet | CRITICAL | IN_APP, EMAIL | `DRIVER_DRUG_TEST_FAILED` |
| `DRIVER_LICENSE_EXPIRING` | fleet | WARNING | IN_APP, EMAIL | `DRIVER_LICENSE_EXPIRING` |

`DRIVER_DRUG_TEST_EXPIRING`, `FUEL_LIMIT_EXCEEDED`, `FUEL_EXCEPTION`, and all other arbitrary event strings are absent and rejected for rule creation/update.

## Domain and Application Changes

- `NotificationEventCatalogue` is the single static source for event compatibility and exact variable allow-lists.
- `NotificationTemplate` validates identifiers, lengths, positive version, controlled event/code/channel compatibility, plain text, token syntax, and unsafe controls.
- `NotificationTemplateRenderer` accepts only simple allow-listed tokens. It rejects unknown or malformed tokens; missing required data returns `TEMPLATE_DATA_MISSING`; missing optional data renders empty; line endings become LF; output is limited to 255/4,000 characters.
- `NotificationRule` adds `templateCode`. New and updated rules cannot use free-form events or incompatible templates/recipients. Legacy unsupported rows may still be read while disabled and cannot be re-enabled without correction.
- `Notification` adds the template ID/version snapshot as a consistent nullable pair with positive-version validation.
- `NotificationRuleEngine` resolves one active compatible version, augments event variables with event time/severity, renders, resolves distinct recipients, snapshots template metadata/content, and then invokes existing channel delivery. Failures remain isolated per rule and from the source operation.

## Recipient Directory Boundary

Notification depends on `NotificationRecipientDirectoryPort`. The adapter imports only the public root Identity interface `com.transportlogistics.app.identity.NotificationRecipientDirectory`; it does not import Identity domain classes, repositories, entities, or internal services.

Resolution rules:

- USER/IN_APP: active user's canonical username.
- USER/EMAIL: active user's validated email; missing email is rejected.
- ROLE: existing active role, resolved at execution to distinct active usernames or validated emails.
- EMAIL_ADDRESS: normalized validated email for EMAIL only; IN_APP is rejected.
- Empty ROLE membership: no notification is persisted; `NO_RECIPIENT` is logged deterministically pending V27 execution audit.

## V26 Schema

Migration: `V26__notification_catalogue_templates.sql`.

- Adds `notification_template` with UUID primary key, code, name, event type, channel, subject, body, positive version, active flag, and audit timestamps.
- Adds channel, positive-version, non-empty/maximum-length, and unique `(code, channel, version)` constraints.
- Adds `idx_notif_template_event_channel_active`.
- Seeds 16 deterministic records: one active version for each of eight templates in both channels.
- Adds nullable `notification_rule.template_code` and `idx_notif_rule_template_code`.
- Deterministically backfills supported V25 events. Unsupported legacy rules receive no unrelated template and are explicitly disabled.
- Adds nullable `notification.template_id` and `notification.template_version`, a template foreign key, positive-version check, and `idx_notification_template_id`.
- V1-V25 are unchanged. No V27/V28 behavior is included.

## API Changes

| Method/path | Permission | Behavior |
|---|---|---|
| `GET /notification-event-catalogue` | `NOTIFICATION_RULE_VIEW` | Returns only the eight controlled events and configuration metadata. |
| `GET /notification-templates` | `NOTIFICATION_RULE_VIEW` | Returns active templates; optional `eventType` and `channel` filters. |
| `GET /notification-templates/{id}` | `NOTIFICATION_RULE_VIEW` | Returns one active system template or 404. |
| `POST /notification-rules` | existing `NOTIFICATION_RULE_MANAGE` | Additively accepts optional `templateCode`; controlled default is used when omitted. |
| `PUT /notification-rules/{id}` | existing `NOTIFICATION_RULE_MANAGE` | Additively accepts and validates `templateCode`. |

Rule responses include `templateCode`; notification responses include `templateId` and `templateVersion`. No template mutation endpoint exists. Existing error envelopes are preserved with stable business codes.

## Security

The new catalogue and template GET paths require `NOTIFICATION_RULE_VIEW` in `SecurityConfig` and are also covered by the notification deny-all fallback. Existing rule-management permissions are unchanged. Integration tests cover unauthenticated 401, insufficient-authority 403, and authorized catalogue/template success.

## Files Created

### Production and migration

- `src/main/java/com/transportlogistics/app/identity/NotificationRecipientDirectory.java`
- `src/main/java/com/transportlogistics/app/notification/application/ports/in/NotificationConfigurationUseCase.java`
- `src/main/java/com/transportlogistics/app/notification/application/ports/out/NotificationRecipientDirectoryPort.java`
- `src/main/java/com/transportlogistics/app/notification/application/ports/out/NotificationTemplateRepository.java`
- `src/main/java/com/transportlogistics/app/notification/application/service/NotificationConfigurationService.java`
- `src/main/java/com/transportlogistics/app/notification/application/service/NotificationRecipientResolver.java`
- `src/main/java/com/transportlogistics/app/notification/domain/model/NotificationEventCatalogue.java`
- `src/main/java/com/transportlogistics/app/notification/domain/model/NotificationEventDefinition.java`
- `src/main/java/com/transportlogistics/app/notification/domain/model/NotificationTemplate.java`
- `src/main/java/com/transportlogistics/app/notification/domain/model/NotificationTemplateRenderer.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/in/web/controllers/NotificationConfigurationController.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/in/web/dto/response/NotificationEventCatalogueResponse.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/in/web/dto/response/NotificationTemplateResponse.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/out/identity/IdentityNotificationRecipientDirectoryAdapter.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/out/persistence/NotificationTemplateEntity.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/out/persistence/NotificationTemplateJpaRepository.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/out/persistence/NotificationTemplatePersistenceAdapter.java`
- `src/main/resources/db/migration/V26__notification_catalogue_templates.sql`

### Tests and documentation

- `src/test/java/com/transportlogistics/app/notification/application/service/NotificationRecipientResolverTest.java`
- `src/test/java/com/transportlogistics/app/notification/domain/model/NotificationEventCatalogueTest.java`
- `src/test/java/com/transportlogistics/app/notification/domain/model/NotificationTemplateRendererTest.java`
- `docs/mvp/MVP-GAP-008B-IMPLEMENTATION.md`

## Files Modified by 008B

- `src/main/java/com/transportlogistics/app/identity/infrastructure/config/IdentityConfig.java`
- `src/main/java/com/transportlogistics/app/identity/infrastructure/security/SecurityConfig.java`
- `src/main/java/com/transportlogistics/app/notification/application/ports/in/NotificationRuleUseCase.java`
- `src/main/java/com/transportlogistics/app/notification/application/service/NotificationRuleEngine.java`
- `src/main/java/com/transportlogistics/app/notification/application/service/NotificationRuleService.java`
- `src/main/java/com/transportlogistics/app/notification/domain/model/Notification.java`
- `src/main/java/com/transportlogistics/app/notification/domain/model/NotificationRule.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/in/web/dto/request/CreateNotificationRuleRequest.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/in/web/dto/request/UpdateNotificationRuleRequest.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/in/web/dto/response/NotificationResponse.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/in/web/dto/response/NotificationRuleResponse.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/in/web/mappers/NotificationWebMapper.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/out/persistence/NotificationEntity.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/adapters/out/persistence/NotificationRuleEntity.java`
- `src/main/java/com/transportlogistics/app/notification/infrastructure/config/NotificationConfig.java`
- `src/test/java/com/transportlogistics/app/notification/application/service/NotificationRuleEngineTest.java`
- `src/test/java/com/transportlogistics/app/notification/application/service/NotificationRuleServiceTest.java`
- `src/test/java/com/transportlogistics/app/notification/domain/model/NotificationRuleTest.java`
- `src/test/java/com/transportlogistics/app/notification/infrastructure/adapters/in/web/NotificationControllerTest.java`
- `src/test/java/com/transportlogistics/app/notification/infrastructure/adapters/in/web/NotificationSecurityIntegrationTest.java`
- `src/test/java/com/transportlogistics/app/notification/infrastructure/adapters/out/persistence/NotificationPersistenceIntegrationTest.java`
- `docs/mvp/US77-MVP-NOTIFICATION-CONTRACT.md`
- `docs/mvp/MVP-CURRENT-STATUS-COMPARE-002.md`

No frontend or Playwright source file was modified by 008B.

## Automated Evidence

- Catalogue: exact eight events, deferred events absent, channels and variable allow-lists.
- Template/domain: version, compatibility, safe controls, malformed/unknown tokens, required/optional data, length limits, and line normalization.
- Recipient: USER, ROLE, direct email, inactive/unknown representation, missing email, zero ROLE members, deduplication, invalid address, and channel compatibility.
- Rule/service: valid create/update, defaults, unsupported event/channel, incompatible template, and enable/disable.
- Engine: rendered rather than source title/message, snapshots, actual resolved recipients, delivery failure behavior, missing-data isolation, and zero-recipient handling.
- Persistence: V26 load, all 16 seeds, active compatible lookup, rule template persistence, notification snapshot persistence, and H2 schema constraints.
- Web/security: catalogue, templates/filter/detail, additive rule template field, 401, 403, and authorized success.

## Verification Results

| Gate | Result |
|---|---|
| `mvn -B clean test` | PASS — 547 run, 526 passed, 0 failed, 0 errors, 21 skipped |
| `mvn -B verify` | PASS — 547 run, 526 passed, 0 failed, 0 errors, 21 skipped; executable jar built |
| Architecture | PASS — 15/15 (`ApplicationModulesTest`, hexagonal, module boundary, Lombok) |
| Spring context | PASS — `ContextSmokeTest` |
| Flyway | PASS — V1-V26 validated/applied on H2; V26 uses H2/PostgreSQL-compatible SQL |
| Frontend lint | PASS |
| Frontend unit | PASS — 94/94 |
| Frontend build | PASS |
| Playwright | 110/111 on the full three-browser run; the pre-existing Firefox E2E-TRIP-008 severity-select interaction failed again in a one-test rerun before the incident request was submitted. Chromium and WebKit passed the same flow. No 008B frontend/E2E code changed. |

## Architecture Verification

Spring Modulith and ArchUnit verification remain green. Notification uses Identity only through the public root directory contract. Notification domain classes have no adapter/framework dependency. Trip/Fleet notification publishing boundaries were not changed.

## Deferred Work

- 008C: quiet hours, repeated-event suppression, and V27 execution audit.
- 008D: durable attempts, bounded retry, terminal failure, and escalation.
- 008E: remaining controlled production event producers.
- 008F: production EMAIL transport and provider-confirmed outcomes.
- 008G: full rule/template/policy frontend completion.
- 008H: notification-specific Playwright coverage.
- 008I: final regression, traceability, and US-77 closure.

No 008C-or-later schema, policy, transport, producer, UI, or E2E behavior was added.
