# P2-04 — Cargo Manifest & Consignment Verification (US-25)

**Contract:** `US25-CARGO-MANIFEST-CONTRACT-001`  
**Status:** COMPLETE — source-aligned Cargo Manifest scope only

## Scope and aggregate

US-25 adds a feature-first `manifest` slice inside the existing `freight` Spring Modulith module. `CargoManifest` is a separate aggregate associated with a saved US-24 Freight Order and owns ordered `CargoManifestItem` children. A manifest has a UUID and stable `CM-YYYY-NNNNNN` operator reference, `UNFINALIZED`/`FINALIZED` condition, audit metadata, finalization metadata and optimistic version.

Manifest items retain a Freight Order line reference, traceable description, positive quantity, packing information, provider-neutral commodity classification, and explicit conditional customs and hazardous-goods information. The repository contains no authoritative catalogues for those values; consequently P2-04 stores normalized supplied codes/text and does not invent package, commodity, customs or hazmat enums.

## US-24 / US-25 boundary

US-24 shipment lines remain commercial demand. US-25 manifest items are execution-grade records and never mutate or replace `FreightOrderLine`. Manifest application code resolves the order through the focused framework-neutral `FreightOrderLookup`; it does not access Freight Order JPA repositories/entities. Trip, Vehicle, load placement, capacity/weight, insurance, reports, cargo-exception correction and billing remain outside this slice.

## Validation and finalization

Readiness is returned as typed failures with a stable code, field and actionable message. It checks at least one item, Freight Order line traceability, exact quantity coverage without unit conversion, positive quantities, mandatory packing/classification, conditional customs information and conditional hazardous classification/details. Missing expected quantity is reported as `UNMANIFESTED_CARGO`; excess or unknown line references are also blockers.

`POST /v1/freight/manifests/{id}/finalize` is the only finalization operation. It re-loads and revalidates the complete aggregate inside the transaction, verifies the expected version, and records finalizer/time atomically. Ordinary manifest and item mutation after finalization returns `409 CARGO_MANIFEST_FINALIZED`. Stale mutation/finalization returns `409 CARGO_MANIFEST_CONCURRENT_UPDATE`.

## Database and API

Forward migration `V32__cargo_manifest_foundation.sql` creates `cargo_manifest_number_sequence`, `cargo_manifest`, aggregate-owned `cargo_manifest_item`, foreign keys, indexes, positive-quantity/order/finalization checks and the three permissions. V1–V31 remain unchanged.

- `GET /api/v1/freight/manifests` — paginated search/filter
- `POST /api/v1/freight/manifests` — create from a saved Freight Order
- `GET /api/v1/freight/manifests/{id}` — details
- `PATCH /api/v1/freight/manifests/{id}` — concurrency-checked unfinalized update
- `POST /api/v1/freight/manifests/{id}/items` — add item
- `PATCH /api/v1/freight/manifests/{id}/items/{itemId}` — update item
- `GET /api/v1/freight/manifests/{id}/readiness` — structured completeness result
- `POST /api/v1/freight/manifests/{id}/finalize` — explicit finalization

There is no DELETE endpoint.

## RBAC and frontend

- `CARGO_MANIFEST_VIEW`: list, details and readiness
- `CARGO_MANIFEST_MANAGE`: create, manifest update and item mutation
- `CARGO_MANIFEST_FINALIZE`: explicit finalization

The local opt-in administrator receives the permissions; no business role was created. Backend authorization precedes controller/application mutation.

`frontend/src/features/freight/manifests` provides Ant Design list/create/details, item editor with React Hook Form and Zod, conditional customs/hazmat fields, readiness failures, deliberate finalization, read-only finalized presentation, TanStack Query invalidation and permission-aware actions/navigation.

## Tests and E2E

Coverage includes domain invariants/readiness/finalization/immutability, application lookup/orchestration/concurrency, H2 aggregate round-trip/paging/version/audit, controller validation/error mapping, 401/403/permitted authorization, frontend list/details/conditional fields/finalized/RBAC behavior, and logical scenarios `E2E-P2-MAN-001` through `007` across Chromium, Firefox and WebKit.

## Known limitations and deferred capabilities

- Reconciliation is intentionally line-reference plus numeric quantity equality because US-24 defines no unit model; no unit conversion is attempted.
- Classification values are supplied provider-neutral codes, not invented master data.
- Post-finalization correction and retained exception resolution belong to US-30.
- Load placement/stacking belongs to US-26; weight/volume/capacity belongs to US-27; insurance, reporting and billing remain deferred to their owning stories.

## Verification evidence

The final regression gates completed successfully:

- Maven `clean test` and `verify`: 785 tests, 0 failures, 0 errors, 22 skipped.
- Architecture rules: 23 tests, 0 failures, 0 errors, 0 skipped.
- Spring Modulith verification: 2 tests, 0 failures, 0 errors, 0 skipped.
- Frontend Vitest: 204 tests across 42 files, 0 failures, 0 skipped.
- Frontend lint: passed.
- Frontend production build: passed; Vite retained the existing large-chunk advisory warning.
- Full Playwright regression: 267 tests, 0 failures — 89 Chromium, 89 Firefox and 89 WebKit tests. The seven US-25 scenarios passed in all three browsers (21 executions).
