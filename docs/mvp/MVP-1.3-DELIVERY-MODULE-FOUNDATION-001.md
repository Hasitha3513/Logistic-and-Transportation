# MVP 1.3 Delivery Module Foundation

**Task:** `MVP-1.3-DELIVERY-MODULE-FOUNDATION-001`  
**Date:** 2026-08-28  
**Status:** COMPLETE  
**Story implementation status:** US-56 through US-62 remain `IMPLEMENTATION_PENDING`

## 1. Source Authority

The primary product authority is `docs/requirements/Traspotation & logistic.docx`. The implementation boundary is frozen by `docs/mvp/MVP-1.3-DELIVERY-OPERATIONS-CONTRACT-001.md`. Repository architecture tests, `AGENTS.md`, the central knowledge base, `MVP_ROADMAP.md`, `docs/mvp/MVP-current-status.md`, and `docs/mvp/MVP-story-traceability-current.md` provide secondary architecture and traceability authority.

## 2. Existing Worktree Baseline

The foundation was reconciled on branch `feat/tenant-scoped-freight-reporting` at `3c7ee09 Freight & cargo implementation P2-01-07`. Delivery source, Delivery tests, three architecture-test changes, the frozen contract, requirements evidence, and roadmap/status updates were already uncommitted. No reset, clean, rebase, checkout, commit, or application push was performed. Unrelated work was preserved.

The database baseline was `V45__freight_reporting_permissions.sql`. No Delivery migration, entity, repository, controller, REST DTO, frontend feature, or permission seed existed.

## 3. Existing Delivery Artifacts Discovered

| Classification | Artifacts | Decision |
| :--- | :--- | :--- |
| `VALID_FOUNDATION_WORK` | `delivery/package-info.java`; `DeliveryId`; `DeliveryNumber`; `DeliveryStatus`; `DeliveryTenantContextPort`; `CurrentTenantDeliveryContextAdapter`; Delivery module/domain/boundary tests | Retained and verified. |
| `VALID_FUTURE_US56_WORK` | `DeliveryLookupPort`; `DeliveryReference`; `DeliverySummary`; customer, location, freight-order and trip lookup ports | Preserved as `PRE-EXISTING_UNCOMMITTED_FUTURE_SLICE`; not extended or claimed as implemented. |
| `VALID_FUTURE_US57_62_WORK` | evidence-storage, notification, offline-sync and slot-availability ports | Preserved as `PRE-EXISTING_UNCOMMITTED_FUTURE_SLICE`; adapters and workflows remain deferred. |
| `ARCHITECTURE_VIOLATION` | `DeliveryFoundationUseCase`, `DeliveryFoundationService`, `DeliveryFoundationConfig`, and tests exposing a synthetic status/permission-catalogue bean | Removed because they provided no source-backed business use case and violated the prohibition on placeholder beans. |
| `INCOMPLETE` | Future ports have no adapters; public lookup contract has no implementation; no Delivery persistence or REST API exists | Intentionally deferred to the owning story task. |
| `DUPLICATE` | None found | No action. |
| `UNRELATED_CHANGE` | All non-Delivery pre-existing worktree changes | Preserved and not attributed to this foundation task. |

## 4. Reconciliation Decisions

Delivery is established as a dedicated Spring Modulith module without implementing a production workflow. Source-backed future contracts were preserved because deleting them would discard useful uncommitted work, but they are not treated as complete capabilities. The artificial foundation-status use case was removed; permission names remain governance metadata only and are not exposed by a runtime bean.

The architecture tests were retained where they verify module discovery, framework independence, and forbidden cross-module internals. A temporary rule stating that all Delivery adapters must not depend on a future Delivery persistence-adapter package was removed because it would incorrectly prevent legitimate adapter composition once an approved persistence slice exists.

## 5. Delivery Bounded-Context Ownership

Delivery owns Delivery Orders, Delivery Attempts, Proof of Delivery, Delivery-specific failure/re-delivery handling, Delivery exceptions, and Delivery analytics defined by US-56 through US-62. It does not own Freight Orders, Trips, Routes, customers, locations, notifications, offline synchronization infrastructure, identity, or Tenant lifecycle.

Cross-module facts must enter through provider-neutral Delivery-owned outbound ports or approved public module/event contracts. Delivery must never inject or query another module's repository, JPA entity, table, internal application service, or adapter.

## 6. Package Structure

```text
com.transportlogistics.app.delivery
├── domain/model
├── ports/outbound
└── adapters/outbound/tenancy
```

The `application`, `ports/inbound`, and other adapter packages will be added only when a source-backed business use case requires them. Empty packages and placeholder beans are not part of the foundation.

## 7. Public Contracts

The root module currently contains the pre-existing read-only `DeliveryLookupPort`, `DeliveryReference`, and `DeliverySummary` contracts. They expose UUIDs and provider-neutral values only; no JPA, HTTP, Spring, or adapter types leak through them. They are reserved future-slice contracts and have no implementation in this task.

No public REST route, application service, repository, entity, event, or workflow contract is implemented by the foundation.

## 8. Outbound Ports

`DeliveryTenantContextPort` plus its `CurrentTenant` adapter is the only outbound port actively wired by the foundation. The pre-existing customer, location, freight-order, trip, evidence-storage, notification, offline-sync, and slot-availability ports are preserved for their frozen owning stories but remain unwired future-slice contracts. No Route or Actor port was invented without an immediate use case.

## 9. Tenant Posture

Delivery reuses the authoritative server-side `CurrentTenant` / `TenantExecutionContext` mechanism through `CurrentTenantDeliveryContextAdapter`. It does not accept Tenant authority from request data, query parameters, `X-Tenant-ID`, JWT-only claims, local storage, or frontend state.

All future Delivery aggregates, commands, queries, DTOs, tables, repositories, events, jobs, caches, analytics, and exports must carry or enforce `tenant_id`. The foundation creates no tenant-owned persistence.

## 10. RBAC Posture

The frozen Delivery permission catalogue remains documented in the contract and central RBAC governance. No permission is seeded, assigned, exposed by an artificial service, or enforced by a public endpoint in this task. The first story that exposes an action must seed and enforce the narrowest applicable permission through membership-scoped authorization.

## 11. Persistence Posture

No Delivery table, JPA entity, Spring Data repository, persistence adapter, schema sequence, or Flyway migration exists. Historical migrations V1 through V45 remain unchanged. Tenant-scoped Delivery persistence begins only in an approved US-56 implementation task.

## 12. Architecture Rules

- Dependency direction is `domain <- ports/application <- adapters`.
- Delivery domain, ports, and application code remain free of Spring, JPA/Hibernate, Jackson, HTTP, and adapter dependencies.
- Root-package public contracts expose provider-neutral values only.
- Delivery may depend on another module only through an approved public contract, outbound port adapter, or registered event.
- No cross-module SQL, JPA association, physical foreign key, repository injection, or internal service dependency is permitted.
- Spring Modulith must discover `delivery` and verify the full module graph without cycles.

## 13. Tests

Foundation coverage includes Delivery module discovery, Spring Modulith boundary verification, framework-free Delivery core rules, forbidden dependencies on Freight/Trip/Routing/Organization/Notification/Offline Sync internals, value-object validation, and direct verification that the tenant adapter delegates to `CurrentTenant` without inventing a Tenant when context is absent. The existing Lombok architecture rules apply unchanged.

- Focused foundation/architecture suite: 32 run, 0 failures, 0 errors, 0 skipped.
- Full `./mvnw test`: 958 run, 0 failures, 0 errors, 22 skipped (`BUILD SUCCESS`) when run with JVM agent attachment available. The first sandboxed attempt produced 654 Mockito infrastructure errors because Byte Buddy could not self-attach; the identical unrestricted rerun passed.
- Full `./mvnw verify`: 958 run, 0 failures, 0 errors, 22 skipped; executable JAR packaging succeeded.

## 14. Pre-existing Future US-56 Work

The root lookup/reference/summary contracts and the customer, location, freight-order, and trip lookup ports are preserved as `PRE-EXISTING_UNCOMMITTED_FUTURE_SLICE`. They do not constitute a Delivery Order aggregate, use case, adapter, API, persistence model, or completed acceptance criterion.

## 15. Deferred US-56–62 Behavior

Delivery Order CRUD/readiness/assignment, POD, offline evidence, failure handling, re-delivery, analytics, exceptions, public APIs, persistence, permission seeding, notification behavior, and frontend work remain deferred to their source-defined story tasks. US-56 remains `IMPLEMENTATION_PENDING`.

## 16. Exact Next Task

`MVP-1.3-US56-DELIVERY-ORDERS-001`
