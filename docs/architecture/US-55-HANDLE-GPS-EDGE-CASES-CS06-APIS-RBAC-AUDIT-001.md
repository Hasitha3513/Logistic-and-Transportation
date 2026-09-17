# US-55 CS06 — APIs, RBAC and Audit

**Status:** `PASS — IMPLEMENTATION_IN_PROGRESS / CS06_COMPLETE`  
**Flyway head:** `V100`  
**Accounting:** `73 / 87 COMPLETE`; `14 / 87 REMAINING`

## Delivered contract

- V99 introduces `GPS_EXCEPTION_VIEW` and `GPS_EXCEPTION_REVIEW` without overwriting an existing definition,
  creating roles or provisioning future tenants. Existing active `ADMIN`, `LOCAL_MVP_ADMIN` and `DISPATCHER`
  roles receive both permissions.
- V100 creates the Tracking-owned `tracking_gps_exception_acknowledgement_command` table. Its same-Tenant
  episode foreign key, Tenant/key uniqueness, fingerprint, key-length, version and JSON-object checks retain only
  completed successful commands. There is no automatic expiry; retention remains coupled to the episode.
- Literal `/api/v1/tracking/gps-exceptions` list/detail/evidence routes require VIEW. Acknowledgement requires
  REVIEW. Authentication and permission checks execute before any replay lookup.
- List queries use a required UTC `[from,to)` range no larger than seven days, limit 1–500, deterministic
  `last_observed_at DESC, id DESC` order and an authenticated Tenant/filter-bound cursor. Evidence uses
  `assessed_at DESC, id DESC`. Responses use `Cache-Control: no-store`.
- Acknowledgement uses a deterministic versioned fingerprint, Tenant-qualified transaction advisory lock,
  row-locked episode update, immutable allow-listed response snapshot and minimized success audit in one
  transaction. Audit records only the fixed outcome and sanitized correlation ID; reason is never copied.
- The replay response contains only episode ID, status, severity, version and acknowledgement timestamp. It
  excludes reason, evidence, coordinates, telemetry, provider/device references, diagnostics and personal data.

## Verification evidence

- Focused service, literal API/security and V99/V100 PostgreSQL selection: **12/12 PASS**.
- Real PostgreSQL proves V1→V100, V98→V100, V99→V100 and zero-work repeated startup; V1–V98 remain unchanged.
- Concurrent identical retries converge on one command, one episode mutation and one success audit. Different
  Tenant reuse succeeds independently; different actor/content reuse, stale versions and invalid lifecycle fail
  closed. Replay after a later resolution returns the original stored snapshot.
- A forced audit-trigger failure rolls back the episode mutation and command row. Same-Tenant FK enforcement,
  evidence immutability, unchanged recovery counters, and absence of Notification/Operations side effects pass.
- Literal HTTP acceptance proves unauthenticated denial, VIEW/REVIEW separation, `no-store`, safe foreign-Tenant
  absence and successful durable replay.
- Architecture/Modulith: **59/59 PASS**.
- Complete backend: **1,890 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS** (`12:37`).
- Checkstyle: **0 violations**. PMD: **BUILD SUCCESS**. SpotBugs: **BUILD SUCCESS, 0 findings**.
  Dependency analysis: **BUILD SUCCESS** with the repository's existing declared/used warning inventory.
- Frontend and Chromium: not applicable to CS06; CS07 owns the new operator UI and retained-surface warnings.
- Docker Compose configuration and `git diff --check`: PASS.

## Rollback and residual risk

V99 and V100 are forward-only after deployment. Application rollback may retain both safely. Removing either
permission or the command table requires a separately reviewed forward migration. Cursor/signing secrets remain
deployment-managed. Physical provider/device acceptance and unavailable tamper/battery fidelity remain separate
and are not inferred from this technical change set.

## Exact next queue

`US-55-HANDLE-GPS-EDGE-CASES-CS07-FRONTEND-001`
