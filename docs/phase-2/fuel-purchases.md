# US-32 — Manage Fuel Purchases

## Purpose and ownership

US-32 records vendor fuel prices, purchase invoices, physical receipt variances, and financial reconciliation. The existing Spring Modulith `fuel` module owns `FuelPrice`, `FuelPurchase`, lifecycle history, calculations, persistence, REST, and immutable business events. The `organization` module owns the new minimal `Vendor` master and exposes only `VendorLookup`; Fuel does not import organization persistence types.

The existing `FuelStation` is reused as the optional purchase and receipt destination. No FuelTank, bunker ledger, accounting journal, payment processing, or supplier portal is introduced.

## Lifecycle

`DRAFT → SUBMITTED → APPROVED → RECEIVED → RECONCILED`

`DRAFT`, `SUBMITTED`, and `APPROVED` may transition to `CANCELLED`. `RECEIVED` and `RECONCILED` purchases cannot be cancelled or normally edited. Status is never accepted by create/update DTOs; all transitions use explicit command endpoints and append history.

## Rules and calculations

- Purchase numbers use database sequence format `FP-YYYY-NNNNNN` and are unique.
- Vendor must exist and be active; Fuel uses `VendorLookup` through an output adapter.
- Vendor invoice uniqueness is enforced by application logic and `(vendor_id, invoice_number)` database constraint.
- Invoice number and date are required before submission.
- Quantity and unit price must be positive. Tax rate and other charges cannot be negative.
- Active catalogue prices cannot overlap for the same vendor and fuel type.
- The invoice price is retained; catalogue price is only an expected-price reference.
- `subtotal = quantity × unitPrice`.
- `taxAmount = subtotal × taxRate ÷ 100`.
- `totalAmount = subtotal + taxAmount + otherCharges`.
- Monetary results use scale 2 and `HALF_UP`; quantity variance uses scale 4.
- Receipt variance is `receivedQuantity - invoiceQuantity`; price variance is `invoiceUnitPrice - expectedCataloguePrice`.

## Database

Flyway `V12__fuel_purchases.sql` creates `vendor`, `fuel_price`, `fuel_purchase`, `fuel_purchase_history`, indexes, constraints, purchase-number sequence, and US-32 permissions. Historical rows are retained; there is no purchase delete endpoint.

## REST API and permissions

| Method | Path | Permission |
|---|---|---|
| GET | `/fuel-purchases` | `FUEL_PURCHASE_VIEW` |
| POST | `/fuel-purchases` | `FUEL_PURCHASE_CREATE` |
| GET | `/fuel-purchases/{id}` | `FUEL_PURCHASE_VIEW` |
| PUT | `/fuel-purchases/{id}` | `FUEL_PURCHASE_UPDATE` |
| POST | `/fuel-purchases/{id}/submit` | `FUEL_PURCHASE_SUBMIT` |
| POST | `/fuel-purchases/{id}/approve` | `FUEL_PURCHASE_APPROVE` |
| POST | `/fuel-purchases/{id}/receive` | `FUEL_PURCHASE_RECEIVE` |
| POST | `/fuel-purchases/{id}/reconcile` | `FUEL_PURCHASE_RECONCILE` |
| POST | `/fuel-purchases/{id}/cancel` | `FUEL_PURCHASE_CANCEL` |
| GET | `/fuel-purchases/{id}/history` | `FUEL_PURCHASE_VIEW` |
| GET | `/fuel-prices` | `FUEL_PRICE_VIEW` |
| POST/PUT | `/fuel-prices[/{id}]` | `FUEL_PRICE_MANAGE` |
| GET | `/vendors[/{id}]` | `FUEL_PRICE_VIEW` |
| POST/PUT/DELETE | `/vendors[/{id}]` | `FUEL_PRICE_MANAGE` |

The list endpoint supports server-side `page`, `limit`, free-text search, purchase number, invoice number, vendor, fuel type, lifecycle/reconciliation status, and date filters. Errors use the shared `ApiError` shape and stable US-32 codes.

## Events

Immutable ID/fact records are published through the existing Spring application-event adapter: `FuelPurchaseApproved`, `FuelPurchaseReceived`, `FuelPurchaseReconciled`, and `FuelPurchaseCancelled`. `FuelPurchaseReceived` is the future integration point for a fuel-owned stock ledger; US-32 does not create bunker stock.

## Frontend

- `/fuel/purchases` — server-paginated purchase table and filters.
- `/fuel/purchases/new` and `/fuel/purchases/{id}/edit` — React Hook Form/Zod draft editor with non-authoritative total preview.
- `/fuel/purchases/{id}` — financial, receipt, variance, reconciliation, lifecycle actions, and history.
- `/fuel/prices` — vendor price catalogue management.

Navigation and actions are permission-aware. Ant Design modals handle receive, reconcile, cancel, submit, and approve operations. Backend calculations and lifecycle validation remain authoritative.

## Tests

Coverage includes monetary rules and rounding, invalid inputs, vendor/invoice validation, lifecycle and cancellation boundaries, receipt variance, append-only history, controller validation, 401/403/permitted approval, Flyway/JPA uniqueness and overlap behavior, frontend list/permission/receipt/reconciled-state behavior, and existing regression suites.

## Deferred

US-33 mileage enhancements, US-34 cost per trip, fuel cards, bunker stock management, analytics/fraud, accounting integration, and payments remain deferred. The next recommended slice is US-33 only after its exact domain and API contract is confirmed.
