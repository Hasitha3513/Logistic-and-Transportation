# TENANT-FOUNDATION-DECISION-002 — Minimum Multi-Tenancy Contract

**Date:** 2026-08-27  
**Decision status:** **ARCHITECTURE APPROVED — LEGACY DATA MAPPING PENDING**  
**Implementation status:** Read-only analysis complete; no production code, authentication, migration, or data changes made.  
**US-29 status:** **BLOCKED_BY_TENANT_FOUNDATION**

## 1. Executive decision

The repository cannot safely unblock US-29 with a freight-only tenant filter. The central policy requires tenant identity at every tenant-owned boundary, while the current application has no `tenant_id`, `tenantId`, or tenant execution context. JWTs contain user, roles, and permissions only; reports and normal repositories perform unscoped reads; scheduled scanners have no tenant input; offline idempotency is global; and all V1–V41 data has unknown tenant ownership.

The recommended minimum contract is:

1. A first-class `Tenant` aggregate is the platform system of record; business modules reference its UUID but do not own or mutate tenant lifecycle.
2. Identity owns user-to-tenant membership and resolves a verified tenant at authentication. For the present ERP, the recommended minimum is exactly one active tenant membership per active user.
3. Username and email remain globally unique for compatibility with the current login contract.
4. An access token contains immutable `tenant_id`; every inbound use case receives a typed execution context containing `tenantId`, `actorId`, and correlation/request identity.
5. Every tenant-owned row—including child, history, inbox, delivery, and execution rows—stores `tenant_id` directly. Every repository operation, report, export, scheduled job, cache key, event, and idempotency key is tenant-scoped.
6. The minimum product has no interactive cross-tenant global administrator. Tenant lifecycle and emergency operations use separately controlled platform operations. Any future impersonation requires an explicit, audited contract.
7. Existing records must not be assigned to a bootstrap tenant until an authorized owner supplies and approves the mapping.

`TENANT-BUSINESS-AUTHORITY-001` approved the dedicated `tenancy` bounded context, exactly-one-active-membership MVP model, global identity uniqueness, global role templates, tenant-scoped assignments, notification classification, and no interactive global administrator. Historical ownership still cannot be derived from source code. Architecture is frozen, but backfill and implementation remain blocked until the legacy mapping is certified.

## 2. Verified baseline

- Branch: `feat/mvp-1.2-fuel-closure`
- Baseline commit: `14cfe1bacacaa1380207f59ceda21131cd76d52a`
- Flyway: V1 through V41; the next version would currently be V42 and must be rechecked at implementation time.
- No production `tenant_id`, `tenantId`, or `TenantContext` exists.
- `app_user.username` and `app_user.email` are globally unique.
- `app_user_role` has no tenant dimension. `refresh_token` is linked only to a user.
- `TokenClaims` and `JwtAccessTokenService` do not carry a tenant. `JwtAuthenticationFilter` reloads by username only.
- Reporting reads Trip and Fleet sources without tenant predicates. Representative production paths use `findAll()`.
- Fleet and notification schedulers execute without an explicit tenant iteration contract.
- `offline_sync_operation.operation_id` and request replay checks have no tenant dimension.
- The working tree contained known documentation/governance changes; no production overlap prevented this read-only audit.

## 3. Central-policy compliance matrix

| Rule | Requirement | Current system | Gap | Required foundation change |
|---|---|---|---|---|
| Tenant identity | Immutable UUID for every tenant | No Tenant model | No system of record | Platform-owned Tenant aggregate/table |
| Domain ownership | Tenant ID on every tenant-owned aggregate/value flow | Domain models have none | Ownership is implicit | Add explicit tenant identity to commands, queries, DTOs, aggregates, events |
| Persistence | `tenant_id NOT NULL`, indexed first; scoped uniqueness | No tenant columns | All rows can mix | Expand/backfill/contract migrations and composite indexes |
| Repository safety | Every read/write includes tenant predicate; no bare ID lookup | Many `findAll`/ID reads | Cross-tenant exposure | Tenant-aware ports and adapter queries; reject mismatched relations |
| Authentication | Tenant comes from verified identity, never arbitrary payload | JWT has no tenant | Tenant cannot be trusted | Resolve active membership and issue verified tenant claim |
| Authorization | Permission check plus membership in requested tenant | RBAC is global | Permission alone crosses tenants | Validate tenant status/membership before use case invocation |
| APIs | Inbound adapter passes typed tenant context explicitly | Username/IDs passed ad hoc | No isolation boundary | `TenantExecutionContext` on tenant-owned use cases; no client tenant override |
| Events | Tenant envelope, ordering, and idempotency scoped by tenant | Events lack tenant | Consumers cannot isolate | Add tenant envelope and `(tenant_id,event_id)` deduplication |
| Reports/exports | Predicate, totals, pages, files, and filenames scoped | Sources are unscoped | Leakage through aggregates/exports | Tenant-aware read ports and export jobs |
| Background work | Enumerate tenants explicitly and isolate failures | Global scheduled scans | Ambient/no ownership | Platform tenant enumeration plus one transaction/work item per tenant |
| Cache/idempotency | Tenant included in keys | Offline operation ID is global | Collision/leak risk | Composite tenant keys and explicit context |
| Tests | Same-tenant success and cross-tenant denial at all layers | No tenant tests | Policy unverified | Mandatory isolation, migration, job, event, and report suites |

## 4. Ownership and bounded contexts

### Approved model

`Tenant` is a platform aggregate with the minimum central contract: `tenantId`, `tenantCode`, `tenantName/displayName`, `status`, `defaultCurrency`, `defaultTimeZone`, `createdAt`, `updatedAt`, and optimistic `version`. Tenant lifecycle is not customer/master-data lifecycle.

The repository placement `core/organization` is **not approved**: the existing Organization module owns operational master data (`Customer`, `Department`, `Location`, `Project`, `Vendor`), while Tenant is a platform root. Identity owns credentials, membership, authentication, and resolution, but not the Tenant aggregate.

**Approved:** create a dedicated top-level platform `tenancy` bounded context. Do not place Tenant in `organization`, `identity`, or `shared`. `shared` remains technical-only.

Business modules store `tenantId` as a UUID logical reference. They may query a provider-neutral tenant-status port where necessary but may not mutate Tenant or join its table.

## 5. Identity, membership, and RBAC contract

### Membership

Approved MVP model: exactly one active tenant membership per active user. Represent it separately rather than using only `app_user.tenant_id`. Enforce at most one active membership per user and require exactly one before tenant-scoped login succeeds.

Multi-membership and tenant switching are deferred beyond the MVP.

### Uniqueness

- `username`: globally unique; keep the existing login and constraint.
- `email`: globally unique; keep the existing identity/recovery assumption.
- Roles: permissions and role definitions are global platform templates for MVP; membership/role assignment is tenant-scoped. Tenant-custom roles are deferred.
- Business identifiers: tenant-owned codes/numbers should ultimately use `(tenant_id, identifier)` uniqueness. Existing globally unique generators may remain temporarily as a stronger compatibility constraint during expansion, but must not substitute for tenant predicates.

### Tenant resolution

1. Login receives username/password only; tenant IDs from headers or payloads are not trusted.
2. Identity verifies credentials, active user, exactly one active membership, and active Tenant.
3. Access token includes `tenant_id`; refresh-token state is bound to the same tenant/membership.
4. The request filter validates signature/expiry and tenant claim, then confirms the user/membership/Tenant are still active according to the chosen revocation policy.
5. The adapter creates `TenantExecutionContext(tenantId, actorId, username, correlationId)` and passes it explicitly to inbound ports.
6. Domain/application code does not depend on Spring Security or a thread-local. An adapter-local scoped holder may bridge legacy wiring only temporarily, must be cleared in `finally`, and is never the sole repository safeguard.

Cross-tenant IDs return not-found semantics unless a public contract explicitly requires forbidden, avoiding record-existence disclosure.

### Global administration

The minimum foundation defines no cross-tenant UI/API administrator and no tenant impersonation. Existing local bootstrap/admin roles do not prove such authority. Platform provisioning and emergency repair must use separately authenticated, audited operational commands. A later global-admin capability requires approved permissions, reason capture, time-bound tenant selection, immutable audit events, and explicit UI/API contracts.

## 6. Data-classification inventory

Classification is by persisted ownership, not by whether a row is currently seeded.

| Classification | Tables | Required treatment |
|---|---|---|
| PLATFORM ROOT | `tenant` (new) | Owned only by approved platform tenancy context |
| GLOBAL CATALOGUE | `app_permission` | No tenant column; centrally governed codes only |
| IDENTITY / MEMBERSHIP | `app_user`, `tenant_membership` (new), `refresh_token`, `app_user_role`, `app_role`, `app_role_permission` | Preserve global user identity; bind membership, sessions and assignments to tenant. Role-definition ownership remains an IAM approval item |
| TENANT OWNED — organization | `customer`, `department`, `location`, `project`, `vendor` | Direct `tenant_id`; tenant-scoped codes and relationships |
| TENANT OWNED — fleet/driver | `driver`, `vehicle_category`, `vehicle_type`, `vehicle`, `vehicle_document`, `driver_license`, `vehicle_reading`, `vehicle_meter_reset`, `maintenance_schedule`, `driver_exception`, `driver_violation`, `driver_medical_record`, `driver_drug_test`, `lubricant_log` | Direct `tenant_id` on parent and child/history rows |
| TENANT OWNED — route/trip | `route`, `route_stop`, `route_revision`, `route_revision_stop`, `route_disruption`, `trip`, `trip_status_history`, `trip_dispatch`, `trip_operational_event` | Direct `tenant_id`; same-tenant relationship validation |
| TENANT OWNED — fuel | `fuel_station`, `fuel_limit_policy`, `fuel_issue`, `fuel_issue_history`, `fuel_price`, `fuel_purchase`, `fuel_purchase_history`, `bunker_tank`, `bunker_stock_movement`, `bunker_dip_reading`, `bunker_stock_adjustment` | Direct `tenant_id`; scope ledgers and actor resolution |
| TENANT OWNED — freight | `freight_order`, `freight_order_line`, `cargo_manifest`, `cargo_manifest_item`, `load_plan`, `load_plan_item_placement`, `freight_insurance_policy`, `freight_insurance_claim`, `freight_insurance_settlement`, `cargo_exception`, `cargo_exception_history` | Direct `tenant_id` on every row; composite same-tenant consistency |
| TENANT OWNED — notification | `notification_rule`, `notification`, `notification_rule_policy`, `notification_rule_quiet_day`, `notification_rule_execution`, `notification_delivery_attempt` | Direct tenant scope for rule evaluation, recipients, retries and deduplication |
| GLOBAL CATALOGUE | `notification_template` | System/default templates are global; tenant-customized copies are deferred |
| TENANT OWNED — offline | `offline_sync_operation` | Direct `tenant_id`; idempotency uniqueness includes tenant |

No existing business table is safely presumed global merely because the legacy deployment has one data set. Child rows must carry `tenant_id` directly; deriving it only through a parent is insufficient for query, job, and defense-in-depth guarantees.

## 7. Freight and reporting isolation

Every freight aggregate and subordinate row listed above requires direct tenant identity. Within Freight, composite foreign keys or equivalent database constraints should enforce that order lines, manifests/items, load plans/placements, policies/claims/settlements, and exceptions/history cannot point to a parent in another tenant. Cross-module IDs remain UUID logical references with application-level same-tenant validation and no physical cross-module foreign keys.

US-29 queries must accept `tenantId` through their inbound and outbound ports. The tenant predicate must be applied before filters, grouping, sorting, pagination, totals, and export generation. Counts and summary rows must use the identical predicate as detail pages. Export work items must persist tenant identity; filenames/object keys and download authorization must include or validate the tenant; generated artifacts must never be served by guessable global IDs alone.

Current reporting also reads Trip, Vehicle, Driver, documents, and exceptions. Therefore Freight-only remediation is insufficient. US-29 becomes eligible only after all report source data is tenant-scoped and the rest of the same deployment cannot expose unscoped tenant-owned APIs to tenant users. A tightly feature-gated deployment could reduce the rollout surface, but no such approved isolation boundary exists today.

## 8. Legacy ownership and migration strategy

### Non-negotiable approval gate

The repository does not prove that all V1–V41 rows and users belong to one legal tenant. Seed users, local admin roles, or a single deployment do not constitute data-governance evidence. An authorized owner must provide:

- the canonical bootstrap tenant UUID, code, name, currency, time zone, and status;
- a signed mapping rule for every legacy business row and user;
- treatment of orphaned, shared, test, and unknown records;
- membership assignments and role semantics;
- reconciliation counts and acceptance authority.

No default UUID/name and no automatic “all rows belong to tenant A” backfill is approved by this document.

### Forward-only expand/migrate/contract plan

1. Reconfirm latest migration; allocate new versions (currently V42+).
2. **Foundation expansion:** create Tenant and membership/session structures without changing existing behavior.
3. **Data expansion:** add nullable `tenant_id` columns and supporting indexes to tenant-owned tables. Deploy dual-aware adapters only under a controlled migration mode.
4. **Controlled mapping:** load the approved ownership map; backfill parents, then children; quarantine rather than guess ambiguous rows.
5. **Reconciliation:** prove row counts, null counts, orphan counts, cross-tenant relationship checks, identifier collisions, and user membership coverage.
6. **Application cutover:** require tenant context and tenant predicates for all exposed reads/writes, reports, jobs, events, notifications, and offline operations.
7. **Contract enforcement:** set `NOT NULL`, add tenant-first/composite indexes and same-module consistency constraints, then revise uniqueness only where approved.
8. **Optional defense in depth:** evaluate PostgreSQL RLS after application predicates are proven; RLS cannot replace explicit ports or tests.

Rollback is release rollback during expansion only. Once authoritative data is backfilled and contract constraints are active, reversal requires a new forward migration; historical migrations remain immutable.

## 9. Repository, async, and integration patterns

- Inbound ports use explicit `TenantExecutionContext`; outbound repository methods take `tenantId` or a tenant-owned aggregate that contains it.
- Replace `findById(id)` with `findByTenantIdAndId(tenantId,id)` equivalents. Lists, existence checks, locks, deletes, bulk updates, native SQL, and counts follow the same rule.
- Writes validate that every referenced tenant-owned aggregate belongs to the same tenant. Cross-module verification is through approved ports/events, never joins or foreign keys.
- Schedulers enumerate active tenants from the platform port and invoke one tenant-scoped unit of work at a time. A failure in one tenant must not change another tenant's transaction.
- Async messages/events carry the tenant envelope explicitly. Consumers establish context from that verified envelope, not from a missing request thread.
- Notification rules, recipients, executions, and delivery retries are tenant-scoped; a global template may be copied/read only under the approved catalogue rule.
- Offline replay uniqueness becomes at least `(tenant_id, operation_id)` and validates actor membership, client instance, aggregate ownership, and request hash inside the same tenant.
- Document metadata, expiry scans, download authorization, and storage keys are tenant-scoped.
- Cache keys begin with tenant identity; cache invalidation events carry it. No cached tenant-owned object is retrievable by entity ID alone.

## 10. Required verification

Implementation tasks must add, at minimum:

- Tenant aggregate/status and membership invariant unit tests.
- Login/refresh tests for active, inactive, missing, duplicate, and cross-tenant memberships.
- JWT claim/tamper tests and request-context cleanup tests.
- Repository integration tests proving same ID/code patterns in two tenants, scoped reads/writes/locks/deletes, and no unscoped native query.
- Flyway migration tests for approved backfill counts, null rejection, identifier collisions, composite constraints, and rollback-safe expansion.
- Controller tests showing a tenant cannot read, mutate, infer, export, or download another tenant's records.
- Freight relationship tests for every parent/child boundary.
- Report tests proving details, totals, pagination, filters, dashboards, and exports are tenant-identical and isolated.
- Scheduler tests for explicit tenant iteration, per-tenant transactions, retries, and failure isolation.
- Event/idempotency, notification, offline replay, document, and cache isolation tests.
- Architecture rules preventing tenant-owned repository ports without tenant identity and preventing domain/application dependencies on Spring Security or adapter context.
- Full `./mvnw test`, `./mvnw verify`, Modulith/ArchUnit, frontend unit/build/lint, and multi-tenant E2E acceptance before US-29 status changes.

## 11. Sequenced implementation plan

| Sequence | Proposed task | Deliverable | Exit criterion |
|---|---|---|---|
| 0 | `TENANT-BUSINESS-AUTHORITY-001` | Freeze owner, membership, IAM, admin, notification, isolation and migration policy | **COMPLETE**; legacy mapping is a separate gate |
| 1 | `TENANT-LEGACY-OWNERSHIP-AUTHORITY-001` | Certify bootstrap Tenant and deterministic V1–V41 ownership | Approved mapping artifact; backfill still not executed |
| 2 | `TENANT-FOUNDATION-IMPLEMENTATION-001` | Platform Tenant aggregate/ports plus identity membership schema | Foundation tests pass; no unowned records changed |
| 3 | `TENANT-AUTH-CONTEXT-001` | Login/refresh tenant resolution, JWT claim, typed execution context | Authentication and context isolation tests pass |
| 4 | `TENANT-LEGACY-EXPANSION-001` | Nullable columns, approved backfill tooling, reconciliation reports | Zero unexplained rows/collisions; human acceptance recorded |
| 5 | `TENANT-ACTIVE-MODULE-ISOLATION-001` | Scope organization, fleet, route, trip, fuel, notification, offline, documents, jobs/events/cache | No exposed tenant-owned path remains unscoped |
| 6 | `TENANT-FREIGHT-ISOLATION-001` | Scope all Freight aggregates, children, workflows, and identifiers | Freight cross-tenant suite passes |
| 7 | `TENANT-REPORTING-ISOLATION-001` | Scope reporting/dashboard/export source ports and artifacts | Detail/total/export isolation suite passes |
| 8 | `TENANT-CONTRACT-ENFORCEMENT-001` | `NOT NULL`, tenant-first indexes, composite constraints, security acceptance | Full regression/architecture/E2E pass and migration reconciliation approved |
| 9 | `US-29-FREIGHT-REPORTS-001` | Implement the freight reporting story | Only starts after tasks 0–8 are complete |

The next tenant task is `TENANT-LEGACY-OWNERSHIP-AUTHORITY-001`, a documentation/data-governance certification—not a migration or code change. US-27 remains the next independently executable MVP 1.1 implementation item.

## 12. Approved decisions and remaining stop condition

| Decision | Frozen outcome | Status |
|---|---|---|
| Tenant system of record | Dedicated platform `tenancy` bounded context | APPROVED |
| Membership cardinality | Exactly one active tenant per active user for MVP | APPROVED |
| Legacy ownership | Explicit approved mapping; never infer | **PENDING CERTIFICATION** |
| Role definitions | Global platform templates; tenant-scoped assignments | APPROVED |
| Notification templates | Global system/default catalogue; customization deferred | APPROVED |
| Global administrator | None in MVP; audited operational mechanisms only | APPROVED |

Until legacy ownership is certified, do not create a bootstrap tenant or backfill V1–V41. Tenant implementation and isolation acceptance also remain incomplete, so do not mark US-29 ready.

## 13. Final disposition

**TENANT-FOUNDATION-DECISION-002: ARCHITECTURE APPROVED — LEGACY DATA MAPPING PENDING.** `US-29` remains `BLOCKED_BY_TENANT_FOUNDATION` pending certified legacy ownership, tenant implementation, and isolation acceptance.

### Synchronized Knowledge Base Files:

- `00_CORE_ARCHITECTURE/multi_tenancy_standards.md`

The approved contract was synchronized as a decision only. Implementation and legacy mapping remain pending; no commit or push was performed per task constraints.
