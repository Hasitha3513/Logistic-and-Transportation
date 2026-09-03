# P0-04 Multi-Tenant Isolation Hardening

Status: COMPLETE

## Authoritative model

The Transportation MVP uses `SERVER_SIDE_MEMBERSHIP_RESOLUTION`. A signed bearer token establishes identity, while every authenticated request resolves the user's current active `TenantMembership` and active Tenant from server persistence. `CurrentTenant` / `TenantExecutionContext` is the runtime authority. Request payloads, query parameters, arbitrary headers, browser state, and unvalidated JWT Tenant claims are not Tenant authority.

The MVP permits one operational Tenant membership per user. `tenant` is the Platform-owned isolation root; customer, organization, department, and company records are Tenant-owned business data and are not alternate Tenant concepts.

## Data classification

- `TENANT-SCOPED`: memberships and membership-role assignment; customers, departments, locations, projects, vendors; Fleet vehicles, documents, readings, maintenance, drivers and compliance; routes; trips, assignment/dispatch/history/events; Fuel; Freight; Delivery; operational notifications; offline operations; reports, exports, caches, and evidence storage derived from those aggregates.
- `GLOBAL REFERENCE`: `app_user` credentials, global role templates, permission catalogue, role-permission templates, and notification templates.
- `PLATFORM INTERNAL`: Tenant lifecycle and the canonical Tenant directory.
- `UNCLEAR / GOVERNED DEBT`: legacy internal Fleet reading, Route disruption, and operational-notification event payloads that predate the standard Tenant event envelope; legacy physical cross-module foreign keys pending retirement planning.

## Corrections

1. All tenant-owned JPA entities, including the 14 Delivery entities introduced after V44, now use `TenantScopedEntity`. Hibernate discriminator queries and the tenant-aware repository base therefore protect inherited reads, lists, updates, and deletes.
2. Architecture tests reject any new tenant-owned JPA entity that omits the centralized discriminator and any inbound request DTO declaring a client-controlled `tenantId` authority field.
3. V57 replaces global operational business-key uniqueness with Tenant-local uniqueness for Organization, Fleet, Route, Trip, Fuel, Freight, notification execution, and bunker idempotency keys. Identity and role-template uniqueness remains global.
4. Cross-tenant PostgreSQL tests prove same-Tenant access and denial for guessed identifiers, Vehicle read/update/delete/list, Driver read, Trip read/modify/report, Route read, Vehicle Document read, Freight reporting, and a manipulated Delivery request `tenantId`.

## Existing protections verified

- JWT validation re-resolves active membership and active Tenant on every bearer request.
- Scheduled jobs iterate active Tenants through `TenantJobExecutor` and establish/clear explicit bounded context.
- Freight and Delivery reporting predicates source SQL by Tenant; exports reuse those scoped queries.
- ETA caches include Tenant in keys and fingerprints.
- POD evidence storage validates Tenant-prefixed paths and metadata ownership.
- No public REST path, method, response schema, or authorization permission changed.

## Remaining governed work

The legacy internal event payloads listed above still require an explicitly versioned Tenant-envelope migration before they can become external contracts. P0-05 must combine resource-scope isolation with permission checks; no role, including administrative templates, may bypass Tenant ownership. Retirement of legacy physical cross-module foreign keys remains a separate forward-only database-governance batch.
