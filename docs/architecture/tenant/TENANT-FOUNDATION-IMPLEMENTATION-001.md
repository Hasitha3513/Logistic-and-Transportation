# TENANT-FOUNDATION-IMPLEMENTATION-001

## Status

- Tenant architecture: `IMPLEMENTED_FOUNDATION`
- Tenant operational data: `NOT_YET_SCOPED`
- Tenant isolation: `PARTIAL`
- US-29: `BLOCKED_BY_TENANT_FOUNDATION`
- Next task: `TENANT-OPERATIONAL-DATA-RETROFIT-001`

## Canonical Tenant

| Field | Value |
|---|---|
| Tenant ID | `4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a` |
| Code | `CLTS-LK` |
| Name | Ceylon Logistics & Transport Solutions (Pvt) Ltd |
| Currency | `LKR` |
| Time zone | `Asia/Colombo` |
| Status | `ACTIVE` |

Flyway migration `V43__tenant_foundation.sql` inserts this record deterministically. No environment override or generated identifier is used.

## Domain and Module Ownership

The top-level Spring Modulith module `tenancy` owns the pure `Tenant` aggregate, tenant status, canonical identity, tenant directory port, current-tenant contract, and persistence adapter. The aggregate carries tenant ID, code, name, currency, time zone, status, audit fields, and optimistic version.

Identity continues to own globally unique credentials in `app_user`, global RBAC templates, explicit `TenantMembership`, and authenticated membership resolution. No `tenant_id` was added to `app_user`, global roles, permissions, role-permission mappings, or system notification templates.

## Membership Model

`tenant_membership` records membership ID, tenant ID, user ID, `ACTIVE`/`INACTIVE` status, audit fields, and optimistic version. The database enforces one membership record per user with `UNIQUE(user_id)`, which is the frozen single-operational-membership MVP model. Foreign keys require an existing tenant and user. Ordinary lookup is focused on the authenticated user; there is no operational `findAll` membership path.

Users created through the Identity use case and the opt-in local bootstrap receive a deterministic active membership to CLTS-LK. Direct database inserts and service identities receive no implicit or wildcard membership.

## Trusted Resolution and Tenant Context

The request flow is:

1. Verify the bearer token and load the active global user.
2. Resolve that user's membership server-side.
3. Reject missing/inactive membership or missing/inactive tenant.
4. Establish `TenantExecutionContext` with tenant ID, user ID, username, and correlation ID.
5. Run the downstream filter chain.
6. Remove the `ThreadLocal` context in `finally`, including downstream exception paths.

Client payloads, query parameters, local storage, and `X-Tenant-ID` are not tenant authorities. The current MVP has one membership and no tenant-selection header or tenant switcher. The public `CurrentTenant` port lets future application and repository adapters require the server-resolved tenant without depending on security/JPA types.

HTTP request context must never be inherited by asynchronous or scheduled work. Future tenant-scoped workers must accept tenant identity explicitly and establish/clear their own bounded execution context.

## JWT and Authorization Decision

JWT structure and refresh-token rotation are unchanged. Login, refresh, and every authenticated bearer request revalidate active membership and tenant state against the database, avoiding stale trusted tenant claims. Tenant membership controls scope; existing global RBAC controls capability. A tenant resolution failure uses the existing authentication error architecture and never falls back to global access.

Supported denial reasons are `TENANT_MEMBERSHIP_NOT_FOUND`, `TENANT_MEMBERSHIP_INACTIVE`, `TENANT_NOT_FOUND`, and `TENANT_INACTIVE`.

## Database Schema

### `tenant`

- UUID primary key `tenant_id`
- unique, nonblank `tenant_code`
- nonblank `tenant_name`, `default_currency`, and `default_time_zone`
- checked status: `ACTIVE` or `INACTIVE`
- `created_at`, `created_by`, `updated_at`, `updated_by`
- non-null optimistic `version`

### `tenant_membership`

- UUID primary key `membership_id`
- `tenant_id` FK to `tenant(tenant_id)` and indexed
- `user_id` FK to `app_user(id)` with cascade delete
- unique `user_id` enforcing one MVP membership per user
- checked status: `ACTIVE` or `INACTIVE`
- audit fields and optimistic `version`

The previous latest migration was V42. V43 is forward-only; no historical migration was edited and no legacy backfill was created.

## Verification Evidence

- H2 clean initialization applied V1 through V43 and validated the JPA schema.
- PostgreSQL 16.15 clean initialization applied V1 through V43; read-only verification found exactly one tenant with the approved values, zero users, and zero memberships when bootstrap/sample data were disabled.
- Domain and service tests cover validation, valid resolution, missing/inactive membership, and missing/inactive tenant.
- Database tests cover canonical values, clean singleton seed, tenant-code uniqueness, membership cardinality, both foreign keys, and initial optimistic versions.
- Context tests cover sequential Tenant A/Tenant B requests and exception-path cleanup.
- Existing login, refresh rotation, logout, RBAC, bootstrap, security, and architecture regressions remain part of the full Maven suite.

## Known Limitations and Next Sequence

This foundation does not make operational data tenant-safe. No tenant column, tenant predicate, cache partition, event envelope, offline-sync partition, reporting-source constraint, or scheduled-job fan-out was added to the existing business tables. No frontend change or current-tenant endpoint was needed for this slice.

`TENANT-OPERATIONAL-DATA-RETROFIT-001` must scope operational tables module by module, starting with parent aggregates and then deterministic children, followed by repository/query enforcement and isolation acceptance. Freight and Reporting sources must be tenant-owned and isolation-tested before US-29 may resume.

Legacy backfill remains not applicable because no recoverable legacy database was found and clean initialization was authorized.
