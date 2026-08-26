# Reconstructed OpenAPI Contract Inventory

The uploaded ZIP contains generated Spring interfaces, but not the source OpenAPI YAML. The following groups were recovered from those interfaces: Auth, Customers, Dashboard, Departments, Drivers, Health, Locations, Projects, Reports, Roles, Routes, Trips, Users, Vehicle Categories, Vehicle Types, Vehicles.

This project preserves those route families. For exact schema-by-schema fidelity, place the original OpenAPI YAML under `src/main/resources/openapi/` and use it as the contract source of truth.

## Phase 2 generated contract

Springdoc now generates the implemented US-31 and US-32 contracts directly from `FuelController` and `FuelPurchaseController`. US-32 adds `/fuel-purchases`, its explicit submit/approve/receive/reconcile/cancel/history operations, `/fuel-prices`, and organization-owned `/vendors`. Request DTOs exclude lifecycle status, responses expose authoritative calculated totals and variances, list parameters expose server pagination/filtering, and Jakarta validation/shared `ApiError` behavior remains the runtime contract. See [phase-2/fuel-purchases.md](phase-2/fuel-purchases.md) for the permission and schema inventory.
