# US-48 CS10 PostgreSQL, Concurrency and Scale Acceptance

## Result

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS10-POSTGRES-CONCURRENCY-SCALE-ACCEPTANCE-001` is COMPLETE. `PLUG_AND_PLAY_FOR_SUPPORTED_ADAPTERS` is technically complete at Flyway V76. US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`; accounting remains 72/87 complete with 15 remaining, physical FMC130/Flespi evidence remains mandatory, and US-49 has not started.

All authoritative database evidence used only `transport_logistics_acceptance`. Development database authoritative evidence is NO. V1 through V76 migrated successfully, the retained V75→V76 upgrade passed, V1–V75 were not changed, and V77 does not exist.

## Accepted PostgreSQL and concurrency evidence

- Focused PostgreSQL CS10 matrix: 47/47 PASS across provider connections, device-provider bindings, coordinator persistence, onboarding transitions, core Tracking races and remediation invariants.
- Provider connection constraints, global provider-key uniqueness, Tenant-local name/alias uniqueness, lifecycle, optimistic versioning, safe configuration and lease-pair integrity: PASS.
- Device binding same-Tenant foreign keys, external-reference uniqueness, one ACTIVE binding per Device, terminal retirement, 4 KiB safe configuration and non-negative optimistic version: PASS.
- Tenant isolation for provider, Device, binding, Vehicle association, reads, mutations and internal ingest authority: PASS.
- Tracking deterministic race matrix: 9/9 PASS; exact duplicate, conflicting duplicate, out-of-order, source-time association, rollback and immutable-history rules remain unchanged.
- Device-provider binding concurrency: 4/4 PASS.
- Coordinator concurrency: 8/8 PASS, comprising 7/7 PostgreSQL lease/watermark races and 1/1 bounded worker-saturation/requeue race.
- Controlled Flespi cutover races: 6/6 PASS. There is no legacy dual runtime or self-HTTP loopback.
- CS09 lifecycle transition races: 3/3 PASS.
- Hot add, disable, rebind, provider disable and credential rotation without restart: PASS. Persisted authority is revalidated, history is retained, and the next execution resolves the current secret reference.
- Same-Tenant multi-connection and multi-Tenant multi-connection execution: PASS without secret, cursor or Tenant crossover.
- One connection with 100 ACTIVE bindings, Device 101 hot-add, mid-set disable, fair bounded paging and persisted watermarks: PASS.
- At least 10,000 temporary bindings across provider/Tenant fixtures: PASS. Due scans and current/active/external-reference/provider-list lookups used existing indexes.
- Scheduler entry points: 1. Per-device schedulers: 0. Per-device long-lived threads: 0. Worker pool and queue: bounded.
- Lease expiry recovery, stale-owner rejection and backpressure: PASS.
- Retention policy, transaction-scoped device/Vehicle authority reuse, dedupe, ordering and latest projections: PASS.
- Latest lookup p95: 0.620 ms. History lookup p95: 0.347 ms, within the unchanged 200/500 ms targets.

Representative accepted indexes included `uq_tracking_provider_key_id`, `uq_tracking_provider_alias`, `uq_tracking_provider_display_name`, `idx_tracking_provider_due`, `uq_tracking_device_provider_active_device`, `idx_tracking_device_provider_connection_list` and `idx_tracking_device_provider_due`.

## Throughput stability remediation

The first pre-remediation CS10 runs measured 1,366.2 msg/s and then 969.5 msg/s burst; the second run correctly failed the unchanged 1,000 msg/s gate. The remaining repeated query was a per-message read of the same latest-trusted source timestamp while its Vehicle was already protected by a transaction-scoped advisory lock.

`JdbcTrackingStore` now caches only that source-time cursor for each already locked Vehicle inside the current batch transaction. It initializes the cursor from PostgreSQL once per Vehicle, updates it only after a trusted accepted packet, and creates a new cache for every transaction. Per-message source-time association, validation, dedupe/conflict detection, immutable insert, retention, ordering, trust and latest projection remain authoritative and unchanged.

Three independent unchanged post-fix runs all passed:

| Run | Sustained | Burst | Result |
| :--- | ---: | ---: | :--- |
| 1 | 774.9 msg/s | 1,601.5 msg/s | PASS |
| 2 | 649.9 msg/s | 1,752.8 msg/s | PASS |
| 3 | 546.2 msg/s | 1,516.2 msg/s | PASS |

The complete Tracking Chromium run subsequently measured 634.8 msg/s sustained and 1,935.8 msg/s burst. Thresholds, requests, concurrency, timer boundaries and assertions were not changed.

## Complete regression evidence

- Complete real PostgreSQL-backed Tracking Chromium: 24/24 PASS in 58.4 seconds, including CS09 onboarding, CS08 Provider Connections, signed ingress and the unchanged throughput case.
- Complete Tracking Java package: 112/112 PASS in 02:25.
- Signed external HMAC/timestamp/nonce ingress security: 22/22 PASS when run independently against the explicit acceptance datasource.
- Complete Maven `verify`: 1,513 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS in 08:49.
- Architecture: 49/49 PASS.
- Checkstyle: 0 violations. PMD: BUILD SUCCESS. SpotBugs: no errors or warnings.
- TypeScript: PASS. Vitest: 290/290 PASS across 67 files. Production build: PASS.
- Tracking changed-file ESLint: 0 findings.
- Repository-wide ESLint: 71 pre-existing errors in eight unrelated Delivery files; no Tracking finding and no Delivery change.
- `git diff --check`: required after final evidence synchronization.

There is no new migration, table, index, API, permission, event, outbox, dependency, scheduler or frontend feature. Packet audit remains NONE and metrics retain bounded labels without Tenant, connection, Device or external-reference identifiers. No real-provider or physical-device result is claimed.

## Next task

Physical hardware/provider access is not available in this workspace, so the next task is:

`US-48-LIVE-VEHICLE-TRACKING-EXTERNAL-ACCEPTANCE-PREPARATION-001`

That preparation does not waive `US-48-FMC130-FLESPI-EXTERNAL-CAPTURE-001` or physical final acceptance.
