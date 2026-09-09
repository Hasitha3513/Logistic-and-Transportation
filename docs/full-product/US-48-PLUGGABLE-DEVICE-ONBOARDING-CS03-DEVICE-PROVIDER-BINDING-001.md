# US-48 CS03 Device–Provider Binding — Technical Evidence

## Result

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS03-DEVICE-PROVIDER-BINDING-001-RERUN` is complete. CS03 is implemented at Flyway V76. US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`; accounting remains 72/87 complete with 15 remaining.

## V76 persistence

V76 creates the single Tracking-owned `tracking_device_provider_binding` table. It has same-Tenant composite foreign keys to `tracking_device` and `tracking_provider_binding`, Tenant/connection/external-reference uniqueness, and a partial unique index allowing at most one ACTIVE binding per device. Deletes are restricted. The table stores the four-state lifecycle, bounded non-secret JSON configuration, source/message watermarks, next-poll time, audit facts and optimistic version.

The migration deterministically backfills each legacy device through exactly one same-Tenant provider-alias match. The binding is ACTIVE only when both device and provider connection are ACTIVE; otherwise it is DISABLED. Zero, ambiguous and cross-Tenant-only matches fail closed. Legacy device provider columns remain unchanged as a compatibility projection. V1–V75 were not modified and no V77 was created.

## Runtime semantics

`TrackingDeviceProviderBindingStore` is the provider-neutral outbound port. Its JDBC adapter provides Tenant-scoped create/read/list operations, lifecycle changes, transactional rebind, optimistic watermark and next-poll updates. ACTIVE creation/rebind validates ACTIVE device and provider parents. Rebind disables the previous active row, creates the replacement, and updates legacy compatibility fields atomically while preserving binding and Vehicle-association history. RETIRED is terminal.

Safe configuration is a JSON object of at most 4,096 serialized bytes; application validation rejects secret-like keys. No credential value is persisted.

## Verification

- Focused CS02+CS03: 19/19 PASS. Corrected first-failure rerun: 11/11 PASS.
- Clean V1→V76 and realistic V75→V76: PASS; measured V75→V76 migration 32 ms.
- Backfill pass, zero-match, ambiguous-match and cross-Tenant fail-closed cases: PASS.
- PostgreSQL constraints, Tenant isolation, lifecycle, safe configuration, watermarks, next poll, optimistic version, compatibility projection and index-backed plans at 10,000 bindings: PASS.
- Existing Tracking concurrency: 9/9 PASS.
- New binding races (double-active, duplicate external reference, rebind/disable and stale watermark/version): 4/4 PASS.
- Complete Tracking regression: 81/81 PASS.
- Full Maven: 1,481 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS in 08:15.
- Architecture/Modulith/table ownership/provider containment: 51/51 PASS.
- Checkstyle: 0 violations. SpotBugs: 0 findings after consolidating identical lifecycle switch clauses.
- PMD: no CS03 finding; direct repository-wide goal reports 95 pre-existing findings outside the CS03 change set.
- Controlled PostgreSQL-backed Chromium: 10/10 PASS in 35.7 seconds. This is not real-provider evidence.
- `git diff --check`: PASS.
- All accepted PostgreSQL evidence used only `transport_logistics_acceptance`; the development database was not used.

## Scope exclusions

No coordinator, polling-runtime cutover, Flespi SPI migration, management API, frontend, permission, event family, cross-module foreign key or US-49 work was introduced.

## Next task

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS04-PROVIDER-EXECUTION-COORDINATOR-001`
