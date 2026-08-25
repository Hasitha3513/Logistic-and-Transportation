# US24-FREIGHT-ORDER-CONTRACT-001

**Story:** US-24 — Manage Freight Orders  
**Decision status:** Reconciled and authoritative for P2-03  
**Revision:** P2-03-R1, August 25, 2026

This contract supersedes planning assumptions that conflict with the source-aligned US-24 scope. It defines a commercial shipment request, not an operational lifecycle or cargo manifest.

## Owned capability

US-24 owns:

- a `FreightOrder` identified by UUID and a stable business order number;
- required customer, origin, and destination references;
- requested pickup and delivery timestamps as the timing/SLA requirement;
- a required constrained service-level code and priority code;
- optional special-handling instructions;
- one or more minimal shipment lines;
- create, paginated list/search, detail, and update operations;
- validation, actor/timestamp audit metadata, optimistic concurrency, and permission enforcement.

Each minimal shipment line contains only:

- `lineId`;
- `description`;
- positive `quantity`.

No authoritative unit model exists in the current repository, so US-24 does not add a unit field or invent unit values. A shipment line captures commercial demand and is not a Cargo Manifest item.

## Explicitly excluded capability

US-24 does not own or define:

- a status or lifecycle state machine;
- `DRAFT`, `CONFIRMED`, `ASSIGNED`, `IN_TRANSIT`, `DELIVERED`, or `CANCELLED` states;
- confirm, cancel, assign, dispatch, in-transit, or delivery commands;
- Cargo Manifest, commodity classification, customs, hazardous-material, packaging, or seal data;
- load planning, capacity/GVW, weight/volume, insurance, rating, charges, or reporting.

US-25 begins from a saved Freight Order and owns its own manifest aggregate and manifest-item representation. It must not mutate a US-24 shipment line into a manifest record.

## Validation contract

- Customer, origin, and destination are required and must resolve through organization-owned public lookup contracts.
- Customer and locations must be active where the authoritative organization model exposes active state.
- Origin and destination must differ.
- Requested pickup and delivery are required; delivery must not precede pickup.
- Service level and priority are required constrained codes. No business values are prescribed by the source and no SLA engine is introduced.
- Special-handling instructions are optional and length-limited.
- At least one shipment line is required.
- Line description is required and length-limited.
- Line quantity must be greater than zero.
- Hazmat, customs, manifest, capacity, GVW, insurance, and rating validation are forbidden in this slice.

## API contract

With the application context path, the complete US-24 API is:

- `GET /api/v1/freight/orders` — paginated list/search;
- `POST /api/v1/freight/orders` — create;
- `GET /api/v1/freight/orders/{id}` — detail;
- `PATCH /api/v1/freight/orders/{id}` — partial update using the expected version.

There are no confirm, cancel, assign, dispatch, or delivery endpoints.

## Concurrency, audit, and security

- Updates require the current optimistic version.
- A stale update returns HTTP `409` with `FREIGHT_ORDER_CONCURRENT_UPDATE` using the existing `ApiError` contract.
- Creation and update preserve actor and timestamp metadata and publish provider-neutral events through an outbound port.
- `FREIGHT_ORDER_VIEW` authorizes list/detail.
- `FREIGHT_ORDER_MANAGE` authorizes create/update.
- No business role, ABAC policy, or authentication redesign is introduced.

## Architecture and persistence

The owning Spring Modulith module is `com.transportlogistics.app.freight`. The `order` feature follows feature-first hexagonal architecture. Domain, ports, and application code are framework-free. Spring/JPA/web/event/transaction concerns remain in adapters.

Freight does not access Customer, Location, Trip, or Fleet repositories or entities. Organization references are validated through focused public lookup contracts.

The forward-only schema is `V31__freight_order_foundation.sql`, following the actual V30 route migration. It creates only the Freight Order sequence, `freight_order`, `freight_order_line`, indexes/constraints, and the two permissions. Migrations V1–V30 remain immutable.

## Historical reconciliation note

Earlier Phase-2 planning described a Freight Order lifecycle (`DRAFT -> CONFIRMED -> ASSIGNED -> IN_TRANSIT -> DELIVERED -> CANCELLED`), confirm/cancel endpoints, manifest-grade cargo fields, and V30 for Freight. Those were planning assumptions rather than source requirements. P2-03-R1 removes them from the active US-24 contract: route work already owns V30, Freight uses V31, and lifecycle/manifest behavior remains deferred to an explicitly sourced later story.

## Story-number reconciliation

At P2-03-R1 completion, `PHASE2-SCOPE-RECONCILIATION-REQUIRED` recorded an unresolved mismatch for US-29/US-30. `PHASE2-SCOPE-RECON-001` subsequently verified and froze the authoritative sequence: US-29 is **Generate Freight Reports** and US-30 is **Handle Cargo Exceptions**. See `PHASE2-SCOPE-RECONCILIATION-001.md`; the US-24 boundary above is unchanged.
