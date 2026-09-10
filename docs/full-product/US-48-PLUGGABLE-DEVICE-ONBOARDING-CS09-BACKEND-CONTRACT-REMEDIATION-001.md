# US-48 CS09 Backend Contract Remediation — Technical Evidence

## Result

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS09-BACKEND-CONTRACT-REMEDIATION-001` is complete. The frozen onboarding order is now executable and reconstructable from backend state: **DRAFT Device → active provider binding → effective Vehicle association → explicit activation**. US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`; accounting remains 72/87 complete with 15 remaining and Flyway remains V76. CS09 frontend implementation and real FMC130/Flespi evidence are still pending; US-49 has not started.

## Original blocker and correction

The backend created devices in DRAFT but previously required an ACTIVE device before either provider activation or Vehicle association. This circular precondition made the frozen onboarding order impossible. Association and active binding now accept a same-Tenant DRAFT device, while DISABLED/RETIRED devices remain ineligible and RETIRED remains terminal. Device activation atomically fails closed unless the device has both a current ACTIVE binding to an ACTIVE provider connection and an active Vehicle association.

The existing device list/detail/create/update/lifecycle response is extended additively with `currentProviderBinding` and `currentVehicleAssociation`. For `TRACKING_DEVICE_MANAGE`, the provider summary exposes only binding ID, provider-connection ID, binding lifecycle/version, safe provider display/type/alias, connection lifecycle, masked external reference and bounded safe configuration. View-only callers receive no binding mutation version. No credential reference, secret, raw provider response or cross-Tenant fact is exposed. A deterministic current-binding query prefers ACTIVE, then DRAFT, then DISABLED, with stable recency ordering, so a refreshed client can reconstruct state and use the current optimistic binding version for rebind.

## Regression remediation outside Tracking

The first complete Maven run exposed a stale opt-in local bootstrap fixture: its integration test required 178 existing MVP permissions, but the source set contained 162 and omitted 16 already-migrated permissions for Delivery slots/analytics, freight reporting, Driver payroll and Tracking. The existing permissions were restored to `LOCAL_MVP_ADMIN`; no permission, migration or product contract was created or changed. The isolated regression test then passed 1/1.

## Verification

- Focused CS09 PostgreSQL contract: 5/5 PASS, including DRAFT preparation, readiness denial, refresh/rebind/stale-version recovery, terminal retirement and three explicit transition races.
- Existing Tracking provider/concurrency remediation selection: 15/15 PASS.
- Complete Tracking package: 111/111 PASS before the final added race case; the final complete Maven run includes the resulting 112 Tracking tests.
- Full Maven: 1,513 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS in 08:23.
- Architecture: 49/49 PASS (Spring Modulith, hexagonal layers, table ownership and module boundaries).
- Checkstyle: 0 violations. Repository warnings are pre-existing formatting debt outside this remediation.
- PMD: BUILD SUCCESS, no new findings.
- SpotBugs: 0 findings.
- TypeScript: PASS.
- Vitest: 283/283 PASS across 66 files.
- Production build: PASS; the existing bundle-size advisory is unchanged and non-blocking.
- Tracking and changed-E2E-file ESLint: PASS.
- Real PostgreSQL-backed Chromium Tracking: 21/21 PASS against `transport_logistics_acceptance`.
- Throughput: 456.3 msg/s sustained and 1,034.3 msg/s burst — PASS.
- Flyway: actual current head V76 — PASS. No migration added.
- `git diff --check`: PASS.
- Development database authoritative evidence: NO. Accepted database evidence used only `transport_logistics_acceptance`.

## Acceptance checklist

- [x] DRAFT Device can bind provider
- [x] Device remains DRAFT until explicit activation
- [x] activation readiness enforced
- [x] activation with valid provider binding PASS
- [x] activation with Vehicle association PASS
- [x] missing prerequisite rejected
- [x] current binding recoverable from backend
- [x] current binding ID exposed safely
- [x] provider connection ID exposed safely
- [x] binding lifecycle exposed safely
- [x] binding version exposed safely
- [x] no credential data exposed
- [x] refresh reconstruction PASS
- [x] rebind after refresh PASS
- [x] stale binding version PASS
- [x] cross-Tenant binding summary denied
- [x] retired Device terminal
- [x] existing ACTIVE flow preserved
- [x] effective-dated Vehicle association preserved
- [x] provider/Vehicle concepts remain separate
- [x] no DB change
- [x] no permission change
- [x] no event
- [x] no outbox
- [x] no frontend implementation
- [x] focused tests PASS
- [x] Tracking package PASS
- [x] PostgreSQL PASS
- [x] existing concurrency PASS
- [x] remediation concurrency PASS
- [x] full Maven PASS
- [x] architecture PASS
- [x] static analysis PASS
- [x] Chromium regression PASS
- [x] development DB authoritative evidence NO
- [x] git diff --check PASS
- [x] evidence document created
- [x] roadmap updated
- [x] KB synchronization attempted
- [x] CS09 ready to rerun
- [x] US-49 not started

## Next task

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS09-FRONTEND-DEVICE-ONBOARDING-001-RERUN`

## Unresolved risks

`GOVERNANCE_REMOTE_SYNC = BLOCKED_GOVERNANCE_SYNC_AUTHENTICATION`. The task-scoped Knowledge Base changes are committed locally on `main` as `76a6528` (`docs(manuals): sync CS09 backend onboarding contract`), with divergence `1 ahead / 0 behind` from `origin/main`. Normal HTTPS push failed because the configured credential helper resolves to the missing `/tmp/gh_2.100.0_linux_amd64/bin/gh`; no `gh` executable or `GH_TOKEN`/`GITHUB_TOKEN` is available, and the read-only SSH check was denied (`publickey`). Minimum user action: restore an authenticated GitHub credential helper/session, then push the existing KB commit normally to `origin main`. Do not recreate the commit or rerun implementation/acceptance.
