# P2-03 — Freight Order Foundation (US-24)

**R1 contract:** `US24-FREIGHT-ORDER-CONTRACT-001`  
**Status:** COMPLETE — source-aligned create/list/detail/update only

## Scope

P2-03 implements the frozen `US24-FREIGHT-ORDER-CONTRACT-001` commercial shipment request. It supports create, paginated list/search, details, and update. It intentionally has no status, confirmation, cancellation, assignment, dispatch, delivery, manifest, load-planning, insurance, rating, or reporting behavior.

## Module and architecture

The new top-level Spring Modulith package is `com.transportlogistics.app.freight`. Its `order` feature follows feature-first hexagonal architecture:

- pure domain: `FreightOrder`, `FreightOrderLine`, created/updated event records;
- framework-neutral inbound/outbound ports and `FreightOrderService`;
- inbound REST DTO/controller/MapStruct adapter;
- outbound JPA, organization lookup, Spring-event, number-generator, and transaction adapters.

The application layer defines use-case atomicity through `FreightOrderTransaction`; the infrastructure adapter implements it with Spring transactions. Events pass through `FreightOrderEventPublisher` and are mapped to Spring publication only in the adapter.

## Aggregate and invariants

`FreightOrder` owns its stable business number, customer, different active origin/destination locations, requested pickup/delivery schedule, constrained freight-owned service-level and priority codes, optional special-handling instructions, one or more minimal commercial shipment lines, audit metadata, and optimistic version.

Each `FreightOrderLine` contains only ID, description, and positive quantity. It is not a Cargo Manifest item. Delivery cannot precede pickup. The aggregate is always editable subject to RBAC, validation, and optimistic concurrency because the frozen contract defines no lifecycle.

## Cross-module lookups

- Customer validation uses the additive organization-owned `CustomerLookup` public contract.
- Location validation uses the existing organization-owned `LocationLookup` public contract.
- Freight never imports organization services, repositories, domain types, JPA entities, or adapters.

There is no new ABAC model in this slice. Existing authenticated permission enforcement remains authoritative.

## Database

`V31__freight_order_foundation.sql` adds:

- `freight_order_number_sequence`;
- `freight_order` with organization foreign keys, schedule/location checks, audit fields, and optimistic `version`;
- `freight_order_line` with ordering, positive quantity, and parent cascade-delete constraints;
- `FREIGHT_ORDER_VIEW` and `FREIGHT_ORDER_MANAGE` permission registrations.

Historical migrations V1–V30 are unchanged. Business numbers use `FO-YYYY-NNNNNN`.

## REST API and errors

With the application `/api` context path, the API is:

- `GET /api/v1/freight/orders` — paginated search/filter/sort;
- `POST /api/v1/freight/orders` — create, returning 201;
- `GET /api/v1/freight/orders/{id}` — details;
- `PATCH /api/v1/freight/orders/{id}` — validated partial update with required expected version.

Stable failures include missing/inactive customer or location, invalid order/line, not found, required version, and `FREIGHT_ORDER_CONCURRENT_UPDATE` (409). The existing global API error/correlation handling is reused.

## Security

- `FREIGHT_ORDER_VIEW`: list and details.
- `FREIGHT_ORDER_MANAGE`: create and update.

No role is created. The opt-in local administrator bootstrap receives both implemented permissions. Backend authorization runs before controller/application mutation.

## Frontend

`frontend/src/features/freight/orders` owns exact types, Axios API calls, TanStack Query keys/hooks, Zod validation, and Ant Design list/create/details/edit pages. Navigation is permission-filtered. Manage actions are hidden without `FREIGHT_ORDER_MANAGE`; backend RBAC remains authoritative. Customer/location names come from existing organization APIs and safely fall back to IDs.

## Tests and verification

Coverage includes aggregate rules, application orchestration/reference/concurrency behavior, JPA persistence/lines/paging/versioning, controller mapping/validation/409 behavior, 401/403/permitted security paths, explicit architecture/module boundaries, frontend list/details/edit/line management/validation/backend-error/RBAC behavior, and six cross-browser Playwright scenarios.

Verification evidence is recorded in the P2-03 completion report after all full gates run.

## Known limitations and deferred work

- Service-level and priority are constrained Freight-owned codes because no authoritative master or safely reusable provider-neutral value set exists.
- Customer/location selector calls retain their existing organization permissions; IDs remain authoritative in Freight.
- Immutable generic audit-history infrastructure was not invented; create/update actor and timestamp metadata are preserved on the aggregate.
- US-25 Cargo Manifest and all US-26–US-30 capabilities remain deferred.
- P2-03-R1 originally left `PHASE2-SCOPE-RECONCILIATION-REQUIRED` unresolved. The later `PHASE2-SCOPE-RECON-001` source audit froze US-29 as **Generate Freight Reports** and US-30 as **Handle Cargo Exceptions** without changing US-24.
