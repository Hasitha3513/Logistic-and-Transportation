# MVP-GAP-008I — US-77 Closure

**Story:** US-77 — Manage Notification Rules
**Date:** August 22, 2026
**Decision:** COMPLETE

## 1. Executive Summary

All 26 frozen US-77 acceptance criteria pass. Backend clean test and verify, 15 architecture checks, application startup, Flyway V1-V28, frontend lint/unit/build, 45 notification browser executions, and the complete 156-case Playwright regression are green. No unresolved P0/P1 notification defect or required MVP capability remains.

The initial four-worker full regression exposed load-sensitive, non-reproducible failures in different cases. Default concurrency was bounded to three workers without adding retries, sleeps, timeout increases, skips, or weaker assertions. The unchanged 156-case suite then passed completely.

## 2. Frozen Scope

MVP includes controlled rule management, IN_APP and EMAIL, system-managed versioned plain-text templates, USER/ROLE/EMAIL_ADDRESS recipients, quiet hours, suppression, durable three-attempt EMAIL delivery, one-level terminal-failure escalation, diagnostics/audit, exactly eight production event types, RBAC, operator UI, and browser acceptance.

## 3. Completed Slices 008A-008I

| Slice | Result | Evidence |
|---|---|---|
| 008A | COMPLETE | frozen contract |
| 008B | COMPLETE | catalogue, templates, renderer, recipients, V26 |
| 008C | COMPLETE | quiet hours, suppression, execution audit, V27 |
| 008D | COMPLETE | durable attempts, retry, failure, escalation, V28 |
| 008E | COMPLETE | eight production producers |
| 008F | COMPLETE | real SMTP production transport |
| 008G | COMPLETE | complete operator frontend |
| 008H | COMPLETE | E2E-NOT-001..015 across three browsers |
| 008I | COMPLETE | full regression, traceability, and closure |

## 4. Acceptance Criteria Matrix

| Criterion | Status | Evidence | Backend test | Frontend test | Playwright | Notes |
|---|---|---|---|---|---|---|
| AC-77-01 | PASS | exact eight-row `NotificationEventCatalogue` | `NotificationEventCatalogueTest` | rules page catalogue tests | NOT-001/002 | deferred events rejected |
| AC-77-02 | PASS | rule use case/service/controller/persistence | rule service/controller/persistence tests | rules page tests | NOT-002..004 | CRUD and toggle persist |
| AC-77-03 | PASS | authoritative SecurityConfig mappings | `NotificationSecurityIntegrationTest` | permission-hidden controls | NOT-005 | 401/403 before mutation |
| AC-77-04 | PASS | exact type/threshold engine match | `NotificationRuleEngineTest` | indirect state rendering | NOT-006 | stable event evaluated once |
| AC-77-05 | PASS | disabled/type/severity no-match paths | rule/domain/engine tests | toggle tests | NOT-004 | no recipient notification |
| AC-77-06 | PASS | allow-list renderer and snapshots | renderer/engine/persistence tests | template preview tests | NOT-002/011 | ID/version/text retained |
| AC-77-07 | PASS | `TEMPLATE_DATA_MISSING` execution audit | renderer/engine tests | backend error rendering | indirect | source operation isolated |
| AC-77-08 | PASS | USER resolution for both channels | `NotificationRecipientResolverTest` | conditional recipient form | NOT-002/011 | canonical user/email |
| AC-77-09 | PASS | distinct active ROLE member resolution | recipient resolver/engine tests | role selector tests | NOT-002 | duplicates removed |
| AC-77-10 | PASS | durable `NO_RECIPIENT` outcome | resolver/engine/escalation tests | diagnostics rendering | indirect | no delivery created |
| AC-77-11 | PASS | recipient/channel validation | resolver/service/controller tests | field-error mapping | NOT-011 | invalid combinations rejected |
| AC-77-12 | PASS | immediate persisted IN_APP delivery | notification/engine/persistence tests | center tests | NOT-006..008 | unread count affected |
| AC-77-13 | PASS | recipient-owned idempotent read/read-all | notification service/security tests | center tests | NOT-009/010 | cross-user mutation denied |
| AC-77-14 | PASS | deterministic quiet evaluator and queue | quiet-hours/engine tests | policy form/status tests | NOT-013 | IN_APP remains immediate |
| AC-77-15 | PASS | CRITICAL quiet-hours bypass | quiet-hours/engine tests | severity policy UI | NOT-013 support | first EMAIL due immediately |
| AC-77-16 | PASS | stable-event uniqueness/idempotency | producer/engine/persistence tests | indirect | NOT-006 | no duplicate attempt |
| AC-77-17 | PASS | separate non-sliding suppression audit | suppression/key/concurrency tests | policy summary tests | NOT-014 | CRITICAL bypass retained |
| AC-77-18 | PASS | provider-accepted SMTP -> SENT | SMTP adapter/worker integration | delivery diagnostics tests | NOT-015 | provider message ID persisted |
| AC-77-19 | PASS | disabled/rejected cannot become SENT | non-production/SMTP/worker tests | failed-state tests | NOT-012 | no logging-only success |
| AC-77-20 | PASS | durable 3 attempts at +1/+2 minutes | retry/claim/worker/persistence tests | attempt history tests | NOT-015 | restart/concurrency safe |
| AC-77-21 | PASS | terminal FAILED and cleared schedule | attempt/claim/SMTP tests | diagnostics tests | NOT-012 | errors sanitized |
| AC-77-22 | PASS | linked exactly-once level-1 IN_APP fallback | `NotificationEscalationServiceTest` | fallback form tests | NOT-012 support | USER/ROLE only |
| AC-77-23 | PASS | eight stable public owning-module producers | Trip/Fleet producer/scanner tests | existing operation UI | NOT-006 | no Notification internals imported |
| AC-77-24 | PASS | execution/delivery/attempt diagnostics | persistence/security/diagnostic tests | diagnostics tests | NOT-012..015 | secrets excluded/masked |
| AC-77-25 | PASS | all applicable fields and backend errors | controller validation tests | 22 focused notification tests | NOT-002/003/011 | no Phase 2 fields |
| AC-77-26 | PASS | all release gates green | 647 tests; verify PASS | lint; 106/106; build | 45/45 and 156/156 | three browsers, no skips |

No criterion is PARTIAL, PLANNED, UNKNOWN, or NOT APPLICABLE.

## 5. Backend Verification

- `.\mvnw.cmd -B clean test`: BUILD SUCCESS; 647 run, 626 passed, 0 failures, 0 errors, 21 skipped.
- `.\mvnw.cmd -B verify`: BUILD SUCCESS; same test totals; executable JAR packaged.
- Spring context, Security, Notification, Trip, and Fleet startup: PASS through `ContextSmokeTest` and full integration/E2E startup.
- JPA: PASS; 44 repositories discovered.

## 6. Architecture Verification

| Suite | Result |
|---|---:|
| `ApplicationModulesTest` | 2/2 PASS |
| `HexagonalLayerArchitectureTest` | 7/7 PASS |
| `ModuleBoundaryArchitectureTest` | 3/3 PASS |
| `LombokUsageArchitectureTest` | 3/3 PASS |
| **Total** | **15/15 PASS** |

Notification owns its internals. Trip publishes the public `OperationalNotificationEvent`; Fleet uses its output port and Spring adapter. Identity recipient lookup remains behind the public `NotificationRecipientDirectory` boundary.

## 7. Database/Flyway Verification

- H2 validated and applied exactly 28 migrations on a clean database, ending at V28.
- V25, V26, V27, and V28 were not edited by 008I.
- No V29 exists or is required.

## 8. Frontend Verification

- `npm run lint`: PASS, 0 errors, 0 warnings.
- `npm test -- --run`: 23 files, 106/106 PASS.
- `npm run build`: PASS.
- Non-blocking output: existing Ant Design deprecation/context notices, unmatched MSW notices in passing tests, and Vite chunk-size advisory.

## 9. Playwright Verification

- Notification: 45/45 — Chromium 15/15, Firefox 15/15, WebKit 15/15.
- Existing retained: 111/111.
- Complete: 156/156, 0 failures, 0 skips, using the repository-standard `npm run test:e2e` command.
- Harness stabilization: default workers 4 -> 3; retries remain 0 and all expectations/timeouts are unchanged.

## 10. Security Verification

- Permissions: `NOTIFICATION_RULE_VIEW`, `NOTIFICATION_RULE_MANAGE`, `NOTIFICATION_VIEW`.
- Backend mappings are authoritative and security integration tests prove unauthenticated 401 and insufficient-authority 403.
- SMTP password is configuration-only and redacted by `toString`; it is not returned by an endpoint or logged.
- Delivery and execution diagnostics mask recipients and expose sanitized typed failures, not raw provider stacks.
- No notification endpoint exposes provider credentials or secrets.

## 11. E2E Test Infrastructure Safety

`E2eNotificationTestConfiguration` and `E2eNotificationTestController` are both restricted by `@Profile("e2e")`; a safety test asserts this. `/e2e/**` additionally requires `NOTIFICATION_RULE_MANAGE`. The adjustable clock and deterministic sender are not active in production/local profiles and cannot weaken production authentication.

## 12. Deferred Scope

The following are Phase 2 and are not US-77 blockers: SMS, PUSH, WEBHOOK, template editor, HTML/rich templates, manual retry, bulk campaigns, notification preferences, notification analytics, unread-age/event-age escalation, multi-level escalation, and provider UI. `DRIVER_DRUG_TEST_EXPIRING`, `FUEL_LIMIT_EXCEEDED`, and `FUEL_EXCEPTION` remain outside the exact MVP catalogue.

## 13. Known Non-Blocking Warnings

- Flyway recommends a newer version for H2 2.2.224, but V1-V28 validate and apply successfully.
- Java reports future-facing dynamic-agent/Unsafe warnings during tests.
- Frontend output contains the existing Ant Design/MSW notices and Vite large-chunk advisory.
- SMTP has documented at-least-once uncertainty if remote acceptance occurs before the local success response is observed.

## 14. Final US-77 Decision

**US-77 is COMPLETE.** No frozen capability is missing, no P0/P1 defect remains, module boundaries are green, and test-only controls are not exposed in production.

## 15. MVP Status Change

| Metric | Before | After |
|---|---:|---:|
| Total | 39 | 39 |
| Complete | 37 | 38 |
| Partial | 1 | 0 |
| Not implemented | 1 | 1 |
| Verified completion | 94.87% | 97.44% |
| Functional coverage | 96.15% | 97.44% |

Release readiness remains **NOT READY** because US-71 is mandatory and not implemented.

## 16. Next Task Recommendation

**MVP-GAP-011-US71 — Implement Offline Data Synchronization.** US-71 was not implemented or otherwise changed in 008I.
