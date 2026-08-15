# US-31 Fuel Issue

## Scope

US-31 is the first Phase 2 slice. It records a controlled fuel issue against a fleet vehicle and, optionally, an eligible assigned trip/driver. It does not implement purchasing, inventory/stock deduction, reconciliation, cards, analytics, vendor management, or US-32–US-38.

## Ownership and boundaries

The Spring Modulith `fuel` module owns `FuelIssue`, `FuelStation`, `FuelLimitPolicy`, append-only history, lifecycle policy, persistence, and REST. It queries fleet, trip, and identity through `VehicleFuelContextLookup`, `TripFuelContextLookup`, and `AuthenticatedUserLookup`. It imports no other module's JPA entity or repository.

Flow: controller → input port → application service → domain policy → output port → persistence adapter → JPA repository.

## Lifecycle

```text
DRAFT → PENDING_AUTHORIZATION → AUTHORIZED → ISSUED
  └──────── eligible non-final states ────────→ CANCELLED
```

- Only `DRAFT` can be edited or submitted.
- Authorization and issue each revalidate all operational rules.
- `ISSUED` and `CANCELLED` are final and read-only.
- Cancellation requires a reason except for a draft.
- Every mutation appends history. Authorization, issue, and cancellation publish module events.

## Business rules and stable errors

- Positive quantity (`INVALID_FUEL_QUANTITY`); non-negative unit price/readings.
- Existing active, operational vehicle (`FUEL_VEHICLE_NOT_FOUND`, `FUEL_VEHICLE_INELIGIBLE`).
- Existing active station (`FUEL_STATION_NOT_FOUND`, `FUEL_STATION_INACTIVE`).
- Readings at least as high as the vehicle's current readings (`INVALID_FUEL_ODOMETER`, `INVALID_FUEL_ENGINE_HOURS`).
- Linked trip in `ASSIGNED`, `DISPATCHED`, or `IN_PROGRESS`, with matching vehicle and optional driver (`FUEL_TRIP_NOT_FOUND`, `FUEL_TRIP_NOT_ELIGIBLE`, `FUEL_VEHICLE_TRIP_MISMATCH`, `FUEL_DRIVER_TRIP_MISMATCH`).
- Active vehicle-specific limit, falling back to active global limit (`FUEL_LIMIT_EXCEEDED`).
- Invalid lifecycle transitions return shared `ApiError` responses with stable codes and HTTP 409.

## REST and permissions

All endpoints require bearer authentication. Search page indexes are zero-based.

| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET | `/fuel-issues` | `FUEL_ISSUE_VIEW` | Server-paged search by vehicle, trip, status, date range, voucher |
| POST | `/fuel-issues` | `FUEL_ISSUE_CREATE` | Create draft with generated voucher |
| GET | `/fuel-issues/{id}` | `FUEL_ISSUE_VIEW` | Detail |
| PUT | `/fuel-issues/{id}` | `FUEL_ISSUE_UPDATE` | Replace editable draft fields |
| POST | `/fuel-issues/{id}/submit` | `FUEL_ISSUE_SUBMIT` | Submit |
| POST | `/fuel-issues/{id}/authorize` | `FUEL_ISSUE_AUTHORIZE` | Authorize; optional comment |
| POST | `/fuel-issues/{id}/issue` | `FUEL_ISSUE_ISSUE` | Record issued fuel |
| POST | `/fuel-issues/{id}/cancel` | `FUEL_ISSUE_CANCEL` | Cancel with reason |
| GET | `/fuel-issues/{id}/history` | `FUEL_ISSUE_VIEW` | Append-only history |
| GET | `/fuel-stations` | `FUEL_ISSUE_VIEW` | Station references; optional active filter |
| POST | `/fuel-stations` | `FUEL_ISSUE_CREATE` | Create station |
| GET | `/fuel-stations/{id}` | `FUEL_ISSUE_VIEW` | Station detail |
| PUT | `/fuel-stations/{id}` | `FUEL_ISSUE_UPDATE` | Update station |

Create/update fields are `vehicleId`, optional `tripId`/`driverId`, `fuelType`, `quantity`, optional `unitPrice`, `stationId`, optional `odometer`/`engineHours`, `issueDateTime`, and optional `notes`. Status and audit fields are never client-editable. Jakarta validation failures expose shared `ApiError.fieldErrors`; generated OpenAPI documents these controller contracts.

## Persistence and concurrency

Flyway `V11__fuel_issue.sql` creates the voucher sequence, `fuel_station`, `fuel_limit_policy`, `fuel_issue`, and `fuel_issue_history`, with checks, foreign keys, unique vouchers, and lookup indexes. Lifecycle mutations acquire a pessimistic row lock and run in one transaction with history. Database constraints remain the final integrity boundary.

## Frontend

Routes are `/fuel/issues`, `/fuel/issues/new`, `/fuel/issues/:fuelIssueId`, and `/fuel/issues/:fuelIssueId/edit`. Navigation and actions are permission-aware. The list uses backend pagination; the editor uses React Hook Form, Zod, and Ant Design; detail actions use modals and display backend business errors without calculating eligibility in React.
