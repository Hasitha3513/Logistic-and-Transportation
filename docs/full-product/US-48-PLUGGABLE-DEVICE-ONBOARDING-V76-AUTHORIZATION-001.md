# US-48 Pluggable Device Onboarding — V76 Authorization

**Task:** `US-48-PLUGGABLE-DEVICE-ONBOARDING-V76-AUTHORIZATION-001`  
**Decision:** APPROVED  
**Reason:** `CS03_SCHEMA_PREREQUISITE`  
**Current Flyway head:** V75  
**Authorized next head:** V76  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining

## Decision

Authorize exactly one forward-only migration named
`V76__tracking_device_provider_binding.sql`. V1 through V75 remain immutable. V76 may create exactly one
Tracking-owned table, `tracking_device_provider_binding`, because the approved CS03 persistence model is
absent from the frozen V75 migration. No V77, down migration, checksum repair, migration-history rewrite,
or competing provider-connection table is authorized.

`tracking_provider_binding` remains the sole provider-connection authority. The new table is the sole
runtime authority for the relationship among a Tracking device, provider connection, and provider-side
device identity. Vehicle association remains separate in `tracking_vehicle_device_assignment`; no Fleet
foreign key or cross-module persistence access is permitted.

## Authorized DDL

V76 is authorized to create only these columns:

| Column | PostgreSQL type | Null | Default / rule |
| :--- | :--- | :---: | :--- |
| `id` | UUID | NO | Primary key |
| `tenant_id` | UUID | NO | Persisted Tenant authority |
| `tracking_device_id` | UUID | NO | Same-Tenant Tracking device reference |
| `provider_binding_id` | UUID | NO | Same-Tenant provider-connection reference |
| `external_device_reference` | VARCHAR(160) | NO | Trimmed, nonblank provider-side identity |
| `safe_configuration` | JSONB | NO | Default `{}`; JSON object; encoded text <=4,096 bytes |
| `lifecycle` | VARCHAR(16) | NO | `DRAFT`, `ACTIVE`, `DISABLED`, or `RETIRED` |
| `watermark_source_timestamp` | TIMESTAMPTZ | YES | Execution cursor only |
| `watermark_message_identity` | VARCHAR(160) | YES | Stable identity only; null allowed |
| `next_poll_at` | TIMESTAMPTZ | YES | Future CS04 scheduling cursor |
| `created_at`, `updated_at` | TIMESTAMPTZ | NO | Existing Tracking audit convention |
| `created_by`, `updated_by` | UUID | NO | Logical actor references |
| `version` | BIGINT | NO | Default `0`; non-negative optimistic version |

The migration must add unique `(tenant_id,id)` for same-Tenant referencing and composite foreign keys
from `(tracking_device_id,tenant_id)` to `tracking_device(id,tenant_id)` and from
`(provider_binding_id,tenant_id)` to `tracking_provider_binding(id,tenant_id)`, both `ON DELETE RESTRICT`.
The parent composite keys already exist. No cross-module key is authorized.

Required constraints are unique `(tenant_id,provider_binding_id,external_device_reference)`, a partial
unique index on `(tenant_id,tracking_device_id)` where lifecycle is ACTIVE, the exact lifecycle check,
trimmed/nonblank external identity, JSON-object/4 KiB safe-configuration bound, bounded optional message
identity, and non-negative version.

## Authorized indexes

In addition to the primary/unique indexes, V76 may create only:

- `(tenant_id,provider_binding_id,lifecycle,id)` for bounded connection binding lists;
- `(tenant_id,lifecycle,id)` for Tenant lifecycle management;
- partial `(next_poll_at,id)` where lifecycle is ACTIVE and `next_poll_at` is not null for the future CS04
  due scan.

The partial unique active-device index serves active binding lookup. The external-reference unique index
serves exact provider-connection/external-reference lookup. CS03 must prove representative query plans
with 10,000 temporary bindings and remove any demonstrably redundant candidate index before acceptance.

## Backfill decision

Automatic deterministic backfill is AUTHORIZED and REQUIRED for existing `tracking_device` rows. Each
device must match exactly one `tracking_provider_binding` in the same Tenant using the legacy
`provider_alias`. Zero matches, multiple matches, or cross-Tenant inconsistency must abort V76 with a safe
diagnostic that exposes no device identity or credential data.

For each deterministic match, V76 creates one binding preserving the device's external reference. Its
lifecycle is ACTIVE only when both the legacy device and matched provider connection are ACTIVE; otherwise
it is DISABLED. `safe_configuration` is `{}`; watermark fields and `next_poll_at` are null; `version` is
zero. Audit timestamps use the device's existing creation/update facts where available and actors use the
existing registration actor. No secret, credential value, watermark, message identity, or schedule state
is fabricated. Existing legacy columns remain unchanged as compatibility projections through CS05.

## CS03 acceptance gates

The resumed CS03 must prove V1→V76 and V75→V76, unchanged V1–V75 checksums, deterministic backfill and
fail-closed ambiguity, same-Tenant foreign keys and cross-Tenant rejection, both uniqueness rules, exact
lifecycle and safe-configuration constraints, watermark/next-poll round trips, optimistic concurrency,
10,000-row index plans, Tracking-only table ownership, and complete existing Tracking regression gates.
PostgreSQL evidence must use `transport_logistics_acceptance`; the development database is not
authoritative.

## Scope exclusions and rollback

This decision adds no production migration or Java code. It authorizes no coordinator, claim SQL,
worker/lease execution, internal ingestion port, Flespi cutover, REST API, frontend, permission, event,
position/history change, signed-ingress change, or US-49 work.

Operational rollback leaves V76 applied, keeps the compatibility path active, disables any future
coordinator, and preserves all binding and telemetry history. The table is not dropped and V75 is never
rewritten.

## Next task

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS03-DEVICE-PROVIDER-BINDING-001-RERUN`
