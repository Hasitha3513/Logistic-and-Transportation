# TENANT-OPERATIONAL-DATA-RETROFIT-001

> **CURRENT-STATE NOTE (2026-08-28):** The readiness gate recorded here was consumed by `P2-FREIGHT-REPORTING-001`; US-29 is now COMPLETE and Freight is 7/7 COMPLETE.

Status: `IMPLEMENTED / ISOLATION ACCEPTED FOR CURRENT SCOPE`

Date: 2026-08-28

## Decision Result

| Gate | Result |
| :--- | :--- |
| Tenant Foundation | `IMPLEMENTED` |
| Role Assignment | `TENANT_MEMBERSHIP_SCOPED` |
| Operational Data | `TENANT_SCOPED` |
| Freight Isolation | `PASS` |
| Reporting Source Isolation | `PASS` |
| Background Processing | `ACTIVE_TENANT_CONTEXT_REQUIRED` |
| Tenant Isolation | `ACCEPTED_FOR_CURRENT_SCOPE` |
| US-29 | `READY_FOR_IMPLEMENTATION` |

## Forward Migration

`V44__tenant_scope_operational_data.sql` is the only schema migration introduced by this task. V1–V43 remain immutable.

V44 creates `tenant_membership_role`, migrates the clean-initialization role assignments from transitional `app_user_role`, and adds a non-null, indexed `tenant_id` discriminator to operational tables owned by Identity token persistence, Organization, Fleet/Driver, Routing/Trip, Fuel, Freight, Notification, and Offline Sync. Existing clean-initialization rows are assigned to canonical Tenant `CLTS-LK` (`4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a`).

Global definitions remain global: `app_user`, `app_role`, `app_permission`, `app_role_permission`, `tenant`, and `notification_template`. `app_user_role` is retained only as a legacy transitional table and is no longer the runtime authorization source.

## Runtime Enforcement

- `CurrentTenant` / `TenantExecutionContext` remains the authoritative server-side source.
- Tenant-owned JPA models inherit the shared `@TenantId` discriminator mapping; generated queries and inserts are tenant-qualified.
- The shared Spring Data base repository tenant-qualifies direct `findById`, `existsById`, `getReferenceById`, and `deleteById` operations, closing Hibernate's direct primary-key lookup gap.
- Scheduled Fleet compliance, maintenance, and notification-delivery work enumerates active Tenants and establishes a distinct execution context for every Tenant.
- Reporting adapters continue to query owning-module repositories/read models and therefore inherit the same tenant discriminator. No cross-module SQL was introduced.
- JWT structure is unchanged. Active membership and Tenant are revalidated server-side.

## Acceptance Evidence

`OperationalTenantIsolationIntegrationTest` creates Freight and Trip reporting facts for two Tenants and proves:

1. Tenant-qualified Freight collection reads;
2. denial of a cross-Tenant Freight primary-key lookup;
3. automatic Tenant ownership on new Freight rows; and
4. isolated Reporting source results for each Tenant.

`TenantJobExecutorTest` proves scheduled work receives a separate explicit Tenant context for every active Tenant. Identity/security regression fixtures now assign global role templates through `tenant_membership_role`.

## Compatibility

- No REST path, request/response schema, JWT claim, permission code, or domain lifecycle changed.
- No legacy data backfill is required because clean initialization was authorized.
- Multi-membership Tenant switching remains deferred.
- US-29 report functionality was not implemented by this task.
