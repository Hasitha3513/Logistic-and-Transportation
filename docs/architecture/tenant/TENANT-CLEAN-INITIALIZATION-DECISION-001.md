# TENANT-CLEAN-INITIALIZATION-DECISION-001

**Title:** Clean Tenant-Aware Initialization Decision for CLTS-LK  
**Decision date:** 2026-08-28  
**Status:** **APPROVED**  
**Implementation status:** **NOT STARTED**

## Decision

No recoverable legacy production database requires preservation. The two
discovered PostgreSQL databases are empty, contain no public application tables
or Flyway history, and are classified as new-environment initialization targets,
not legacy production sources.

The recovery search found no credible database dump, backup, alternate runtime,
older checkout, or alternate connection configuration. Flyway migrations define
the application schema but are not business-data evidence. PostgreSQL/H2 sample
SQL and automated-test fixtures are non-production data.

Therefore:

- legacy preservation is **NOT REQUIRED**;
- legacy runtime reconciliation is **NOT APPLICABLE** after this decision;
- legacy backfill is **LEGACY_BACKFILL_NOT_APPLICABLE**;
- clean tenant-aware initialization is **AUTHORIZED**;
- this decision does not itself initialize a database or implement the tenant
  foundation.

No historical record is represented as migrated, reconciled, or backfilled.

## Canonical Tenant

| Field | Approved value |
|---|---|
| Tenant UUID | `4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a` |
| Tenant code | `CLTS-LK` |
| Legal/business name | Ceylon Logistics & Transport Solutions (Pvt) Ltd |
| Default currency | `LKR` |
| Default time zone | `Asia/Colombo` |
| Initial status | `ACTIVE` |

## Clean Initialization Authorization

`TENANT-FOUNDATION-IMPLEMENTATION-001` may design and implement the approved
forward-only clean initialization, including:

- the first-class Tenant aggregate and tenancy-owned persistence;
- canonical `CLTS-LK` bootstrap creation;
- the explicit tenant-membership model;
- verified tenant resolution and typed execution context;
- tenant-aware session/JWT behavior under the frozen identity contract;
- direct `tenant_id` ownership for tenant-owned rows;
- tenant-scoped repositories, mutations, counts, aggregations, exports, events,
  background work, caches, and idempotency;
- isolation, cross-tenant denial, migration, and bootstrap tests.

The implementation must recheck the latest migration version, use forward-only
migrations, preserve historical migrations, and create no fabricated historical
business records.

## Data Classification

### Non-production data

`postgresql-sample-data.sql`, H2 sample data, demo fixtures, test fixtures, and
Playwright fixtures remain **NON_PRODUCTION**. Development seeding must label and
isolate them accordingly; it must not silently adopt them as canonical Tenant
production records.

### Approved global exceptions

The current approved global records are:

- `app_permission` — centrally governed permission catalogue;
- `app_role` — global role templates, not tenant-custom roles;
- `app_role_permission` — global template-to-permission mapping;
- `notification_template` — system/default templates only.

`app_user` is a global credential identity, not global operational access.
`tenant_membership` grants access to `CLTS-LK`; role/membership assignments,
sessions, and refresh-token behavior must preserve tenant scope under the
implementation contract.

Countries, units of measure, manufacturers, and other generic catalogues may be
global only if an actual model exists and is explicitly approved. This decision
does not create those models or classify operational data as global.

## User Model

- Username and email remain globally unique credential identifiers.
- Tenant identity must not be inferred from username, email, request payload, or
  an unverified header.
- Every approved bootstrap/local user created in a clean development database
  requires an explicit membership to `CLTS-LK`.
- System/service accounts require explicit classification and membership or
  trusted-service policy; permission alone never bypasses isolation.
- MVP permits exactly one active Tenant membership per active user. Tenant
  switching and interactive cross-tenant administration remain deferred.

## Isolation Acceptance Requirements

Before US-29 can resume, implementation and tests must prove:

- every tenant-owned row is created with immutable `tenant_id`;
- every tenant-owned repository operation receives and predicates on Tenant;
- cross-tenant reads and mutations are denied without existence disclosure;
- tenant predicates precede filtering, sorting, pagination, counting,
  aggregation, summaries, and export generation;
- internal parent/child relationships preserve same-tenant consistency;
- events, jobs, caches, audit records, and idempotency keys carry Tenant;
- RBAC and tenant isolation are independently enforced.

## US-29

US-29 remains `BLOCKED_BY_TENANT_FOUNDATION`. It may resume only after
`TENANT-FOUNDATION-IMPLEMENTATION-001`, clean initialization, and tenant-isolation
acceptance pass.

## Explicit Non-Actions

- Production Java/TypeScript changes: none
- Flyway migrations: none
- Database initialization: none
- Database rows changed: none
- Tenant backfill: not applicable and not executed
- US-29 implementation: not resumed
- Main-project commit/push: none

## Next Task

`TENANT-FOUNDATION-IMPLEMENTATION-001`

### Synchronized Knowledge Base Files:

None. Central synchronization is deferred because updating the external
knowledge base would require its mandatory commit and push, while this task
explicitly prohibits commits and pushes.
