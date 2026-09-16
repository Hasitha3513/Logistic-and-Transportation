# US-55 CS05 — Operations and Notification Integration

**Status:** `COMPLETE`

**Story state:** `IMPLEMENTATION_IN_PROGRESS / CS05_COMPLETE`

**Flyway head:** `V98`

**Accounting:** `73 / 87 COMPLETE`

## Delivered contract

Tracking publishes a minimized durable first-open fact for each GPS exception episode. Notification
delivers one IN_APP alert to active same-Tenant Dispatcher members. WARNING is stored and rendered as
WARNING. HIGH uses the Notification platform's existing CRITICAL storage severity but preserves HIGH
in the approved template variable. A direct HIGH opening also publishes one deterministic
`OperationalExceptionFactV1`; a WARNING-to-HIGH transition publishes that Operations fact without a
second Notification. Recovery, repeated evidence and ordinary lifecycle progress are silent.

The Operations contract now accepts source module `TRACKING`, the ten canonical GPS exception source
types, summary `TRACKING_GPS_EXCEPTION_HIGH`, and four bounded Tracking categories. Technical/data
quality/device-health facts route to `OPERATIONS_TECHNICAL_QUEUE`; device-security facts route to
`OPERATIONS_SECURITY_QUEUE`. Metadata is limited to episode/type/severity/device/optional Vehicle and
opening/last-observed timestamps. It contains no coordinates, payload, provider detail, credential,
signature, error message, stack trace or personal data.

V98 transactionally extends the two Operations check constraints and seeds the exact active template,
Dispatcher IN_APP rule and disabled escalation/quiet-hour policy for tenants present during migration.
Future-Tenant automatic default-rule provisioning is
`DEFERRED_PENDING_GOVERNED_TENANT_CREATION_WORKFLOW`; V98 adds no trigger or speculative workflow.

## Verification evidence

- Pure event/catalogue/bridge/contract tests: 13/13 PASS.
- Mockito evaluator and Notification engine plus V97→V98 PostgreSQL acceptance: 12/12 PASS.
- Clean application startup migration: V1→V98 PASS against `transport_logistics_acceptance`.
- Exact V98 template, existing-Tenant rule, Operations source/category constraints: PASS.
- Architecture and Spring Modulith: 64/64 PASS.
- Complete Maven post-quality-fix regression: 1,878 tests, 0 failures, 0 errors, 0 skipped;
  BUILD SUCCESS in 12:23.
- Checkstyle: PASS, 0 violations. PMD: BUILD SUCCESS. SpotBugs: PASS, 0 findings.
- Dependency analysis: BUILD SUCCESS with the repository's existing aggregate dependency warnings;
  no dependency changed.
- Docker Compose validation and `git diff --check`: PASS.
- Frontend and Chromium: not applicable; CS05 changes no public API or frontend behavior.

## Security and rollback

Tenant identity remains explicit in both durable envelopes. Notification recipient resolution remains
same-Tenant and role-qualified, and Operations intake remains Tenant-qualified and idempotent. Failed
downstream handling cannot invalidate Tracking evidence because delivery uses the existing P1-01
outbox. V98 is forward-only after successful deployment; rollback disables the CS05 consumers/rules
without deleting durable Tracking evidence.

## Exact next queue

`US-55-HANDLE-GPS-EDGE-CASES-CS06-APIS-RBAC-AUDIT-001`
