# US-48 Pluggable Device Onboarding CS02 — V75 Provider-Connection Persistence

**Task:** `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS02-V75-PROVIDER-CONNECTION-PERSISTENCE-001`  
**Result:** COMPLETE  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining  
**Flyway head:** V75

## Persistence authority and migration

V75 evolves the existing Tracking-owned `tracking_provider_binding` table into the sole persistence
authority for the `TrackingProviderConnection` concept. No `tracking_provider_connection` or competing
table exists. V1 through V74 are unchanged.

`V75__tracking_pluggable_provider_connections.sql` adds provider type, display name, safe endpoint and
JSONB configuration, polling bounds, separate connection-test state, safe health timestamps/category,
next-poll state, and a complete lease pair. It retains the globally unique opaque `provider_key_id`,
Tenant, alias, opaque credential reference, audit columns, lifecycle, and optimistic version.

The migration first rejects unrecognized legacy aliases and duplicate `(tenant_id, provider_alias)` rows.
Repository-authoritative existing aliases are limited to `FLESPI` and controlled `FIXTURE`; these backfill
deterministically to the matching uppercase provider type and non-secret alias display name. Existing
ACTIVE/DISABLED lifecycle is preserved. A representative V74→V75 upgrade completed in 32 ms.

The V75 schema also widens only the existing `tracking_device` lifecycle constraint to reserve
`DRAFT`, `ACTIVE`, `DISABLED`, and `RETIRED` for CS03. The explicitly deferred
`tracking_device_provider_binding` table is not created, and legacy device provider columns are retained.

## Constraints and indexes

- `provider_type` matches `[A-Z][A-Z0-9_]{0,63}` without a PostgreSQL enum.
- Provider alias and display name are unique only inside a Tenant; provider key remains globally unique.
- Safe configuration must be a JSON object and its PostgreSQL text encoding cannot exceed 8,192 bytes.
- Poll interval is 5–86,400 seconds; page size is 1–500.
- Test status is exactly `NOT_TESTED`, `PASS`, `AUTH_FAILED`, `UNREACHABLE`, or
  `INVALID_CONFIGURATION`.
- Lifecycle is exactly `DRAFT`, `ACTIVE`, `DISABLED`, or `RETIRED`.
- Error category is an uppercase bounded code; lease owner/until must both be null or both present.
- Existing `(tenant_id,lifecycle,provider_alias,id)` and global provider-key indexes remain.
- New `(tenant_id,provider_type,lifecycle,id)` and partial `(next_poll_at,id)` ACTIVE/due indexes support
  provider filtering and the future CS04 claim access path.

EXPLAIN over 10,000 temporary connections across 100 Tenants selected
`uq_tracking_provider_display_name`, `uq_tracking_provider_alias`, `uq_tracking_provider_key_id`, and
`idx_tracking_provider_due` for the governed list, alias, key, and future due-scan queries respectively.

## Provider-neutral persistence model

The pure application model comprises `TrackingProviderConnection`, create/mutation records,
`ProviderConnectionLifecycle`, and `ProviderConnectionTestStatus`, reusing CS01 `ProviderType`,
`ProviderSafeConfiguration`, `ProviderConnectionConfiguration`, and `ProviderConnectionId`.
`TrackingProviderConnectionStore` is an outbound port implemented by the Tracking JDBC adapter.

All human persistence reads, lists, and optimistic updates predicate on both `tenant_id` and connection
UUID. Cross-Tenant reads are empty and cross-Tenant/stale writes fail with `TRACKING_STALE_VERSION`.
Updates increment `version`, preserve immutable provider key/alias/Tenant authority, support opaque
credential-reference replacement, and reject transitions out of terminal RETIRED state.

Only the opaque credential reference is stored. Application validation rejects secret-like safe-config
keys and credential-bearing URI userinfo/query parameters before JDBC. JSON serialization is defensively
copied and checked against the encoded 8 KiB database bound.

## Compatibility

The legacy signed ingress lookup by globally unique provider key remains unchanged and authenticates only
ACTIVE bindings. Explicit security regression tests prove DRAFT, DISABLED, and RETIRED all return
unauthorized. The legacy V74 maintenance path now supplies required provider type/display values, and the
PostgreSQL sample fixture remains idempotent with `FIXTURE` type/display. The singleton Flespi runtime has
not been cut over.

## Verification evidence

- CS01 SPI plus CS02 PostgreSQL persistence/migration: 23/23 PASS.
- Full Tracking regression: 71/71 PASS, including deterministic Tracking concurrency 9/9 and the new
  stale-version case.
- Current-head regression group: 32/32 PASS after advancing nine V74 head assertions to V75.
- V1→V75: PASS; V74→V75: PASS; measured upgrade migration: 32 ms.
- Architecture/Modulith/table ownership: 48/48 PASS.
- Full Maven: 1,471 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS in 07:45.
- Checkstyle: 0 violations; PMD: BUILD SUCCESS; SpotBugs on recompiled Java 21 classes: 0 findings.
- Controlled PostgreSQL-backed Chromium: 10/10 PASS in 36.8 seconds.
- `git diff --check`: required as the final repository gate.
- `DEVELOPMENT_DATABASE_AUTHORITATIVE_EVIDENCE = NO`; accepted evidence used only
  `transport_logistics_acceptance`.

## Scope exclusions and rollback

CS02 adds no device-provider binding table/behavior, coordinator, lease claim SQL, provider execution,
internal ingestion port, Flespi SPI migration, REST API, frontend, permission, event family, or US-49
work. Operational rollback retains V75 and the existing signed-ingress/history paths; a future coordinator
can remain disabled. No down migration is provided.

## Next controlled change set

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS03-DEVICE-PROVIDER-BINDING-001`
