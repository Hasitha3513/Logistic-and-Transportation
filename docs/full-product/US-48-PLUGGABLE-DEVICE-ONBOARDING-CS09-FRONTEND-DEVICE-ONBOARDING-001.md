# US-48 CS09 Frontend Device Onboarding

## Final acceptance result

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS09-FRONTEND-DEVICE-ONBOARDING-001-FINAL-RERUN` passes. CS09 frontend device onboarding is COMPLETE at Flyway V76. This closes the supported-adapter onboarding user interface; it does not complete US-48, change story accounting or waive physical FMC130/Flespi final evidence.

US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`. Accounting remains 72/87 complete with 15 remaining, and US-49 remains blocked.

## Accepted workflow

The real PostgreSQL-backed browser journey proved the complete server-authoritative sequence:

1. Create a provider-neutral Device in DRAFT without implicit activation.
2. Bind it to an ACTIVE same-Tenant Provider Connection. FLESPI uses manual external-reference entry because its adapter does not advertise discovery.
3. Associate a same-Tenant Vehicle while the Device remains DRAFT.
4. Reload the page and reconstruct the current provider binding, binding version, Vehicle association, DRAFT lifecycle and activation readiness from backend detail.
5. Explicitly activate only after both prerequisites exist.
6. Disable and reactivate while preserving the prepared authority and historical Tracking data.
7. Reload detail and rebind using the backend-returned current binding version. Stale-version rejection safely reloads current detail and never silently overwrites state.
8. Retire permanently. RETIRED exposes no activate, bind, rebind, associate, disable, provider-mutation or hard-delete action.

Activation remains unavailable without either an ACTIVE provider binding or current Vehicle association. Provider health and device connectivity are rendered as distinct facts; provider health never fabricates CONNECTED telemetry state.

## Security, tenancy and layout

`TRACKING_DEVICE_MANAGE` exposes management actions. `TRACKING_VIEW` receives approved masked read state only, and `TRACKING_HISTORY_VIEW` grants no management authority. The real journey proved Tenant-B identifiers cannot read or mutate Tenant-A Device, Provider Connection, Vehicle or binding state. External device identity remains masked for read-only use.

Neither UI nor safe network responses expose `credentialReference`, token, secret or authorization material. Credentials are not placed in local storage, URLs, rendered state, notifications or console output. The feature remains inside the existing Tracking views and AppLayout; it adds no duplicate global breadcrumb/title, floating action bar or competing shell.

## Verification evidence

- Focused CS09 frontend: 9/9 PASS across 2 files in 4.49s.
- Full Vitest: 290/290 PASS across 67 files in 48.29s.
- TypeScript: PASS.
- Production build: PASS in 4.79s; the existing bundle-size advisory is non-blocking.
- Tracking/CS09 changed-file ESLint: 0 findings.
- Global ESLint baseline: 71 pre-existing Delivery-module errors in eight files; no Tracking/CS09 finding and no unrelated Delivery change.
- Complete real PostgreSQL-backed Tracking Chromium: 24/24 PASS in 59.2s.
  - CS09 onboarding: 3/3 PASS.
  - Functional Tracking: 10/10 PASS.
  - CS08 Provider Connections: 10/10 PASS.
  - Unchanged throughput case: 1/1 PASS at 671.2 msg/s sustained and 1,228.0 msg/s burst.
- Complete Tracking Java package: 112/112 PASS in 02:23.
- Complete Maven `verify`: 1,513 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS in 08:36.
- Architecture: 49/49 PASS.
- Checkstyle: 0 violations; PMD: BUILD SUCCESS; SpotBugs: 0 findings.
- Flyway source/runtime head: V76. V77 does not exist.
- Accepted PostgreSQL evidence used only `transport_logistics_acceptance`; development database authoritative evidence is NO.
- `git diff --check`: required after documentation synchronization.

## Scope exclusions

No production implementation defect required remediation in this final rerun. There is no backend API, database, migration, permission, event, outbox, dependency, product-contract, throughput-threshold or story-accounting change. Arbitrary unsupported hardware, bulk onboarding, QR/mobile onboarding and US-49 remain outside scope.

## Next task

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS10-POSTGRES-CONCURRENCY-SCALE-ACCEPTANCE-001`
