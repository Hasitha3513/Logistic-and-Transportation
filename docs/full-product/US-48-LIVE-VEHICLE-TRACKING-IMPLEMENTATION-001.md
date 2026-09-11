# US-48 Live Vehicle Tracking — Implementation Evidence

**Status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**Story migration:** V73; **current repository head:** V74
**Final acceptance dependency:** `REAL_DEVICE_REAL_PROVIDER`  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining

## Delivered contract

- Dedicated provider-neutral `tracking` bounded context with framework-free domain and ports.
- Signed HMAC-SHA256 HTTP JSON ingress, persistent nonce replay protection, 1 MiB request and 500-position batch limits, and bounded Tenant/provider admission control.
- Tenant-scoped device registry and effective-dated, database-enforced one-active device/Vehicle association history.
- Immutable normalized PostgreSQL position history, deterministic dedupe/conflict handling, source-time association, ordering/trust classification, latest-received/latest-trusted projections, and bounded keyset history.
- External-policy retention metadata with no automatic purge while a legal policy is unconfigured.
- Exact human permissions: `TRACKING_VIEW`, `TRACKING_HISTORY_VIEW`, and `TRACKING_DEVICE_MANAGE`; literal `/api/v1/...` security coverage; masked device references for view-only users.
- Minimal accessible current/last-known operator UI with truthful freshness, connectivity, accuracy, timestamp/age, 15-second visible polling, history, and device-management controls.
- No per-packet P1-01/Spring event and no US-49–55 detector or dashboard scope.

## Verification evidence

- Complete Maven: **1,411 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS**, 07:10.
- PostgreSQL tracking focus: **13/13 PASS**, including the exact **9/9 concurrency matrix** plus immutability/overlap and literal security coverage.
- Flyway: **V1 through V73 PASS**; US-48 migration is **V73**.
- Architecture: **46/46 PASS**.
- Checkstyle: **0 violations** (repository warnings are informational); PMD: **BUILD SUCCESS** from a clean Java 21 compile; SpotBugs: **0 findings**.
- TypeScript: PASS; Vitest: **265/265 PASS**; production build: PASS; changed-file tracking lint: PASS.
- Real PostgreSQL-backed Chromium: **10/10 PASS**.
- Signed HTTP performance: **446.2 accepted msg/s sustained** (200 in 448.2 ms); **1,156.5 msg/s burst** (1,000 in 864.7 ms).
- PostgreSQL sampled p95: latest single Vehicle **66.160 ms**; 24-hour history page **10.008 ms**.
- Query plans: latest uses tenant-leading `tracking_vehicle_latest_pkey`; history uses tenant-leading `idx_tracking_vehicle_time`; execution times were 0.061 ms and 0.102 ms respectively.
- Measured `tracking_position` footprint: 1,078.61 bytes/row at 2,400 rows; projected frozen-target growth is about **15.53 GB/day (14.47 GiB/day)** before retention/storage compression policy.
- `git diff --check`: PASS.
- All accepted database-backed evidence used only `transport_logistics_acceptance`; the development database was not contacted or changed.

## Acceptance boundary

Implementation and controlled-provider technical evidence are complete. Story completion remains pending independent technical closure and final evidence from a physical device and real provider payload. Do not advance accounting or US-49.

## Authorized V74 technical remediation

`US-48-LIVE-VEHICLE-TRACKING-TECHNICAL-REMEDIATION-001` is complete. V74 adds the Tracking-owned globally unique opaque provider-key binding, binding-scoped nonce table, and one-current-policy-per-Tenant retention table. Telemetry Tenant authority now comes exclusively from an ACTIVE binding; the signed canonical value is `epoch + "\n" + nonce + "\n" + providerKeyId + "\n" + providerAlias + "\n" + rawBody`. Secrets are resolved only through the published `IntegrationSecretResolver` and are never persisted.

Configured retention rejects only timestamps strictly before `receivedAt - duration`; equality is accepted and policy/version/retain-until are retained. No policy preserves LATE handling without age-only rejection. An internal transactional, idempotent per-Tenant/Vehicle rebuild restores latest-received/latest-trusted projections from immutable retained history. Safe counters/timers, stale/offline/latest-ingest health facts, provider/binding/retention audit and denied-management audit are present without packet audit storms or high-cardinality/sensitive metric labels.

Fresh remediation evidence: provider security plus boundary tests **17/17 PASS**; PostgreSQL V74/remediation/concurrency **15/15 PASS** (exact races **9/9**); complete Maven **1,430 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS**, 07:08; architecture **49/49**; Checkstyle zero violations; PMD and SpotBugs pass with zero task findings; TypeScript/build pass; Vitest **265/265**; changed-file ESLint zero; real Chromium **10/10**. Signed ingress measured **483.5 msg/s sustained** and **1,087.1 msg/s burst**; latest p95 **2.222 ms** and 24-hour history p95 **0.989 ms**. All authoritative database evidence used `transport_logistics_acceptance` only.

**Next task:** `US-48-LIVE-VEHICLE-TRACKING-TECHNICAL-CLOSURE-001-RERUN`
