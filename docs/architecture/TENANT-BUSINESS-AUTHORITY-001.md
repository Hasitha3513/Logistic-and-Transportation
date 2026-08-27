# TENANT-BUSINESS-AUTHORITY-001 — MVP Multi-Tenancy Authority Freeze

**Date:** 2026-08-27  
**Status:** **APPROVED**  
**Implementation:** **PENDING**  
**Legacy ownership mapping:** **PENDING CERTIFICATION**  
**US-29:** **BLOCKED_BY_TENANT_FOUNDATION**

## Decision

The following decisions are authoritative for the MVP tenant foundation and supersede the corresponding open choices in `TENANT-FOUNDATION-DECISION-002`. They authorize later, separately scoped implementation. They do not assert that tenant isolation exists today and do not authorize legacy backfill.

## Approved contract

### Tenant system of record

- A dedicated top-level platform bounded context named `tenancy` owns Tenant lifecycle.
- Tenant is a first-class aggregate with `tenantId` (immutable UUID), `tenantCode`, `tenantName`, `status`, `defaultCurrency`, `defaultTimeZone`, `createdAt`, `updatedAt`, and `version`.
- Business modules reference `tenantId` but do not own Tenant lifecycle.
- Identity owns authentication, membership, and tenant resolution—not Tenant. Organization does not own Tenant. `shared` remains technical-only.

### Identity, membership, and RBAC

- An active user has exactly one active tenant membership for MVP. Membership is explicit; `app_user.tenant_id` alone is not the model.
- Tenant-scoped login requires valid credentials, an active user, exactly one active membership, and an active Tenant. Multi-membership/switching is deferred.
- Username and email remain globally unique.
- `app_permission` and role definitions are global platform templates. Membership/role assignments are tenant-scoped; tenant-custom roles are deferred.
- Authorization requires verified membership plus permission. A permission never bypasses isolation.

### Administration and tenant resolution

- No interactive cross-tenant global administrator or tenant impersonation exists in MVP. Existing admin-style roles do not bypass isolation.
- Provisioning/emergency operations require separately controlled, audited mechanisms. Future global administration requires a separate ADR.
- Normal login remains username/password; client tenant headers/payloads are not authoritative.
- Authentication resolves membership and Tenant, then issues a JWT with immutable `tenant_id`.
- Tenant-owned inbound use cases receive a typed context containing at least `tenantId`, `actorId`, and `correlationId`; HTTP/Spring Security types do not enter domain models.
- Authentication failure is 401, permission denial within the tenant follows current 403 behavior, and another tenant's normal resource returns 404 unless explicitly documented otherwise.

### Data, catalogue, and identifier ownership

- Every tenant-owned persisted row directly stores `tenant_id`, including child, history, execution, delivery, document, offline, and reporting/export rows.
- This covers Organization business data, Fleet, Driver, Route, Trip, Fuel, Freight, tenant-owned Notification data, Offline operations, Documents, and reporting artifacts. Parent-derived ownership alone is insufficient.
- System/default notification templates are global; rules, executions, and deliveries are tenant-owned. Customized template copies are deferred.
- Tenant-owned identifiers target `(tenant_id, business_identifier)` uniqueness. Existing global uniqueness may temporarily remain until collision/API analysis; removal is not authorized here.

### Jobs, offline processing, events, reports, and exports

- Schedulers enumerate active tenants through a tenancy port and isolate each tenant's transaction and failures.
- Offline idempotency is scoped by at least `(tenant_id, operation_id)` and validates tenant, actor membership, aggregate ownership, and request integrity.
- Tenant-owned events/messages carry explicit tenant identity; consumers use the verified envelope, never ambient request context.
- Tenant scope applies before report/export filtering, grouping, sorting, pagination, counting, aggregation, summaries, and artifact generation. Details, totals, and exports use the same boundary; artifacts are tenant-owned.

### Migration and security invariant

- Migration is forward-only: expand, migrate, reconcile, contract. V1–V41 remain immutable.
- V42 is only the expected next version; it must be rechecked and is not reserved here.
- Tenant isolation is mandatory at every tenant-owned boundary. RBAC, frontend visibility, or a reporting-only predicate is insufficient.

## Legacy-data authority gate

Historical ownership is not certified. No bootstrap tenant may be invented, and the legacy single-tenant deployment is not evidence that one company owns every record.

Before backfill, an authorized owner must approve `docs/architecture/tenant/TENANT-LEGACY-OWNERSHIP-MAPPING.md`, including canonical Tenant identity, user membership, business-row rules, orphan/test/unknown treatment, reconciliation counts, approver, and date. Multiple owners require deterministic per-record mapping.

**Backfill: NOT AUTHORIZED.**

## Current-state qualification and consequence

The current schema has no tenant columns/membership, authentication has no tenant-aware JWT/context, and repositories/reports/jobs remain unscoped. This decision is not evidence of runtime isolation.

- `TENANT-FOUNDATION-DECISION-002` advances to **ARCHITECTURE APPROVED — LEGACY DATA MAPPING PENDING**.
- US-29 remains blocked pending certified legacy ownership, tenant implementation, and isolation acceptance.
- Next task: `TENANT-LEGACY-OWNERSHIP-AUTHORITY-001` — Certify Bootstrap Tenant and Legacy V1–V41 Ownership Mapping.

## Approval checklist

- [x] Dedicated `tenancy` context and Tenant UUID model
- [x] Exactly one active membership per active user
- [x] Global username/email and global permission/role templates
- [x] Tenant-scoped assignments and no global-admin bypass
- [x] Membership → JWT `tenant_id` → typed execution context
- [x] Direct tenant ownership and notification/identifier classifications
- [x] Tenant-scoped jobs, offline processing, events, reporting, and exports
- [x] Legacy ownership retained as an approval gate
- [x] Forward-only migration policy
- [x] Production code, authentication, and V1–V41 unchanged

### Synchronized Knowledge Base Files:

- `00_CORE_ARCHITECTURE/multi_tenancy_standards.md`

The central document records **DECIDED**, **IMPLEMENTATION PENDING**, and **LEGACY MAPPING PENDING** separately. No commit or push was performed per this task's explicit constraints.
