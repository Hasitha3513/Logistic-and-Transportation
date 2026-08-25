# US25-CARGO-MANIFEST-CONTRACT-001

**Story:** US-25 — Manage Cargo Manifest  
**Actor:** Freight Manager  
**Priority:** High  
**Related feature:** Manifest  
**Decision status:** FROZEN for P2-04-R1  
**Source:** `D:\Traspotation & logistic.docx`, Freight & Cargo Manager / US-25.

## User story

As a Freight Manager, I want to maintain manifest items, classifications, customs and hazmat information, so that cargo is traceable and compliant.

## Purpose

Provide the execution-grade record of what cargo is actually included for a saved Freight Order and ensure its item, classification, conditional customs/hazmat and completeness data is valid before finalization.

## Acceptance criteria

- A manifest contains identifiable cargo line items.
- Commodity, customs and hazmat information can be recorded.
- Unmanifested cargo is flagged before controlled dispatch.

The source use-case model also requires manifest-line and completeness validation before finalization. Invalid commodity classification, missing applicable customs data, missing mandatory line data, or known unmanifested cargo prevents normal finalization.

### Given/When/Then traceability

| AC ID | Given | When | Then |
|---|---|---|---|
| US25-AC1 | A manifest is created for a Freight Order | Cargo is recorded | The manifest contains identifiable cargo line items. |
| US25-AC2 | Cargo requires classification, customs or hazmat data | The cargo item is maintained | Applicable commodity, customs and hazmat information can be recorded. |
| US25-AC3 | Known cargo is absent from the manifest | Controlled-dispatch readiness is evaluated | The unmanifested cargo is flagged. |

## Aggregate boundary

US-25 owns a separate `CargoManifest` aggregate associated with a saved US-24 Freight Order. It owns:

- manifest identity and stable manifest reference;
- Freight Order reference;
- audit metadata and optimistic version;
- cargo items with their own stable item identity;
- cargo-item description/identity needed for traceability;
- quantity and packing information supplied for the manifested item;
- commodity classification;
- customs information when applicable;
- hazardous-goods classification and details when the item is hazardous;
- validation state/results required to determine finalization readiness;
- finalization metadata and edit restrictions.

The implementation contract must not invent a package-type, commodity, customs or hazardous-goods value catalogue. P2-04-R1 must first reuse an authoritative existing model if one exists; otherwise it must use constrained provider-neutral values whose semantics come from supplied source data.

## Data fields

**ARCHITECTURAL INTERPRETATION:** The source defines the business information below but not physical column/DTO names.

The source-supported conceptual fields are:

- manifest ID and stable manifest number/reference;
- Freight Order ID;
- manifest-level audit metadata and optimistic version;
- finalization actor/time when finalized;
- one or more cargo items;
- per item: item ID, traceable description/identity, positive quantity, packing information, commodity classification, conditional customs information, and conditional hazardous-goods classification/details.

Exact field names, lengths and value representations must follow repository conventions and a source-backed catalogue review during P2-04-R1. No Trip ID, Vehicle ID, load position, axle value, insurance value or exception state is owned by this aggregate.

## References

- **Freight Order:** required. The manifest represents the actual cargo associated with an existing order; it does not mutate US-24 minimal shipment lines into manifest items.
- **Trip:** not required or owned by the authoritative US-25 source. A later dispatch-readiness integration may consume finalized-manifest status through a focused boundary without adding Trip orchestration to this aggregate.
- **Vehicle:** not required or owned by US-25. Vehicle/load-space selection belongs to US-26.

## Operations

US-25 supports:

- create a Cargo Manifest from a saved Freight Order;
- paginated list and detail retrieval;
- update an unfinalized manifest;
- add and update cargo items on an unfinalized manifest;
- validate cargo items and complete manifest lines;
- finalize a valid and complete manifest.

The source does not define deletion, cancellation, dispatch, load placement, insurance, reporting or exception-resolution commands for US-25.

## Lifecycle, finalization and edit restrictions

**ARCHITECTURAL INTERPRETATION:** The source explicitly defines finalization and its validation failure path but does not prescribe a status enum.

The source defines only two relevant business conditions: **unfinalized** and **finalized**. This contract does not authorize a broader status state machine.

- Finalization is an explicit business command, not an arbitrary status patch.
- The application must revalidate the complete aggregate transactionally immediately before finalization.
- Mandatory cargo-item data, applicable customs data, hazardous classification/details and manifest completeness must pass.
- Known unmanifested cargo blocks finalization and controlled dispatch.
- A finalized manifest and its cargo items are immutable to ordinary update operations.
- Post-finalization correction is an authorized outcome of US-30 Cargo Exceptions and must preserve the original audit trail; P2-04 does not implement that correction workflow.

Ordinary create/edit operations apply only while unfinalized. The implementation may choose a repository-aligned representation for these two conditions, but may not add approval, dispatch, cancellation, loading or delivery states.

## Validation boundary

US-25 validates identity, traceability, classification, conditional customs/hazmat data, packing data, positive quantity and manifest completeness. It does not calculate or enforce:

- physical placement, stacking, temperature placement or cargo compatibility (US-26);
- gross/net/cubic totals, payload, volume or axle limits (US-27);
- policy coverage, claims or settlements (US-28);
- report aggregation (US-29);
- cargo-exception lifecycle and corrective actions (US-30).

## Explicitly excluded capability

- mutation or conversion of US-24 `FreightOrderLine` records;
- physical loading verification, placement, stacking, loading/unloading sequence and vehicle selection;
- payload, gross/net/cubic, volume or axle calculations;
- insurance policy/claim workflow;
- reporting and cargo-exception resolution;
- deletion of finalized audit history;
- invented package, commodity, customs or hazardous-goods catalogues.

## RBAC

**ARCHITECTURAL INTERPRETATION:** The source requires controlled finalization but does not name technical permission codes. The following codes apply current repository naming and least-privilege conventions.

Use permission authorities, not role names:

- `CARGO_MANIFEST_VIEW` — list and detail;
- `CARGO_MANIFEST_MANAGE` — create and edit an unfinalized manifest and its items;
- `CARGO_MANIFEST_FINALIZE` — execute finalization.

Authorization must run before mutation. P2-04-R1 must add permissions through the next forward Flyway migration and integrate them with the existing administrator bootstrap without creating new business roles.

## Audit and concurrency

- Record creator/updater/finalizer actor and timestamps using repository conventions.
- Use the current optimistic-version policy for mutable operations.
- A stale update or stale finalization attempt returns HTTP 409 through the existing `ApiError` contract.
- Preserve finalized data and audit history; do not hard-delete finalized manifests or items.

## Architecture

The feature belongs inside the existing top-level `com.transportlogistics.app.freight` Spring Modulith module under a feature-first `manifest` slice. Domain, ports and application remain framework-free. Spring web, persistence, security, transaction and event behavior remain adapters.

Freight may resolve the Freight Order through a focused public application/module boundary. It must not access another feature’s JPA repository/entity directly. Trip and Vehicle repositories/entities are outside this contract.

## Planned API boundary

**ARCHITECTURAL INTERPRETATION:** The source defines business operations, not REST paths. These paths map them to current repository conventions without adding behavior.

P2-04-R1 may implement only repository-aligned equivalents of:

- `GET /api/v1/freight/manifests`
- `POST /api/v1/freight/manifests`
- `GET /api/v1/freight/manifests/{manifestId}`
- `PATCH /api/v1/freight/manifests/{manifestId}`
- `POST /api/v1/freight/manifests/{manifestId}/items`
- `PATCH /api/v1/freight/manifests/{manifestId}/items/{itemId}`
- `POST /api/v1/freight/manifests/{manifestId}/finalize`

No load-plan, weight/volume, insurance, report, cargo-exception, dispatch, cancel or freight-charge endpoint is part of P2-04-R1.

## Frontend contract

The feature path is `frontend/src/features/freight/manifests/`. It owns:

- permission-aware paginated manifest list;
- create and unfinalized edit forms;
- manifest details and cargo-item presentation;
- add/update cargo-item controls;
- conditional customs and hazmat fields driven by supplied classifications;
- structured validation/readiness feedback;
- explicit finalization confirmation and finalized edit restriction;
- backend field-error, conflict and authorization feedback.

Use Ant Design, React Hook Form, Zod, TanStack Query and the shared Axios client. Frontend visibility does not replace backend authorization.

## Persistence plan

- Current latest migration is `V31__freight_order_foundation.sql`.
- P2-04-R1 owns the next available forward migration after rechecking the chain at implementation time.
- The migration may create only manifest, manifest-item, validation/finalization audit data and manifest permission records required by this contract.
- Historical migrations remain immutable.

## Required P2-04-R1 verification

Cover domain validation/finalization/edit restrictions, application orchestration, persistence and optimistic concurrency, controller/API errors, 401/403/permitted authorization, architecture/Modulith rules, frontend list/form/items/details/finalization/error mapping, and the frozen US-25 cross-browser E2E matrix.

## Definition of done

- The separate Manifest aggregate and owned item representation preserve the US-24/US-25 boundary.
- Create/list/detail/update/item/finalize operations match the frozen API boundary.
- Conditional customs/hazmat and completeness rules prevent invalid finalization.
- Known unmanifested cargo is surfaced before controlled dispatch readiness.
- Finalized manifests reject ordinary edits and retain audit history.
- RBAC is enforced before mutation, and stale mutation/finalization returns the current 409 error contract.
- Backend, frontend, architecture/Modulith and all frozen Chromium/Firefox/WebKit scenarios pass.
- No US-26 through US-30 or US-47 behavior is implemented.
