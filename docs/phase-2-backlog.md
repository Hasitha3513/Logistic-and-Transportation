# Phase 2 Backlog and Delivery Strategy

## Requirements traceability constraint

The current Phase 2 requests authoritatively map Fuel Issue to **US-31** and Manage Fuel Purchases to **US-32**. Later story identifiers remain deferred unless separately authorized.

US-31 and US-32 are explicit user-authorized vertical slices. Remaining Phase 2 work still follows the entry gates in [phase-2-entry-criteria.md](phase-2-entry-criteria.md).

## Epic backlog

| Epic | User Story ID | Actor | Feature | Priority | Dependencies | Backend module | REST API area | Principal database entities | Frontend page | Delivery order |
|---|---|---|---|---|---|---|---|---|---:|
| EP-201 Fuel Management | UNMAPPED — source unavailable | Fleet administrator | Fuel types, vendors and bunker master data | Must | Phase 1 release | `fuel` | `/fuel/types`, `/fuel/vendors`, `/fuel/bunkers` | fuel_type, fuel_vendor, bunker | Fuel configuration | 1 |
| EP-201 Fuel Management | US-32 — IMPLEMENTED | Fuel manager | Vendor prices, purchase invoices, receipt variance and reconciliation | Must | US-31 fuel foundation; organization vendor boundary | `fuel`, `organization` | `/fuel-purchases`, `/fuel-prices`, `/vendors` | vendor, fuel_price, fuel_purchase, fuel_purchase_history | Fuel purchases and prices | Delivered |
| EP-201 Fuel Management | US-31 — IMPLEMENTED | Fleet operator | Issue fuel against vehicle, driver and trip | Must | Phase 1 trip/fleet public interfaces | `fuel` | `/fuel-issues`, `/fuel-stations` | fuel_issue, fuel_issue_history, fuel_station, fuel_limit_policy | Fuel issues | Delivered |
| EP-201 Fuel Management | UNMAPPED — source unavailable | Fleet manager | Fuel cards and controlled allocations | Should | Fuel issue workflow | `fuel` | `/fuel/cards`, `/fuel/allocations` | fuel_card, fuel_card_assignment, fuel_allocation | Fuel cards | 4 |
| EP-201 Fuel Management | UNMAPPED — source unavailable | Auditor | Reconciliation, consumption and variance reporting | Should | Purchases and issues | `fuel`, `reporting` | `/reports/fuel/**` | reporting projections; fuel ledgers | Fuel reports | 5 |
| EP-202 Freight and Cargo | UNMAPPED — source unavailable | Operations planner | Freight order and cargo-item capture | Must | Phase 1 organization/location | `freight` | `/freight/orders` | freight_order, cargo_item | Freight orders | 6 |
| EP-202 Freight and Cargo | UNMAPPED — source unavailable | Operations planner | Cargo constraints, weight, volume and handling attributes | Must | Freight orders | `freight` | `/freight/orders/{id}/cargo` | cargo_item, cargo_requirement | Cargo editor | 7 |
| EP-202 Freight and Cargo | UNMAPPED — source unavailable | Dispatcher | Link freight orders to trips without transferring ownership | Must | Freight orders; trip public interface | `freight` | `/freight/orders/{id}/trip-assignment` | freight_trip_assignment | Freight assignment | 8 |
| EP-202 Freight and Cargo | UNMAPPED — source unavailable | Warehouse operator | Loading and unloading confirmation | Should | Freight-trip assignment | `freight` | `/freight/orders/{id}/handling-events` | cargo_handling_event | Cargo handling | 9 |
| EP-202 Freight and Cargo | UNMAPPED — source unavailable | Operations manager | Freight status and exception visibility | Should | Handling events | `freight`, `reporting` | `/freight/orders`, `/reports/freight/**` | freight status projection | Freight overview | 10 |
| EP-203 GPS and Real-Time Tracking | UNMAPPED — source unavailable | Integration service | Register tracking devices and vehicle bindings | Must | Phase 1 fleet public interface | `tracking` | `/tracking/devices`, `/tracking/bindings` | tracking_device, vehicle_device_binding | Tracking devices | 11 |
| EP-203 GPS and Real-Time Tracking | UNMAPPED — source unavailable | Telematics provider | Ingest authenticated position observations | Must | Device bindings | `tracking` | `/tracking/positions` | position_observation | No operator form; integration endpoint | 12 |
| EP-203 GPS and Real-Time Tracking | UNMAPPED — source unavailable | Dispatcher | View current trip/vehicle location | Must | Position ingestion; trip public interface | `tracking` | `/tracking/vehicles/{id}/latest`, `/tracking/trips/{id}` | latest_position_projection | Live operations map | 13 |
| EP-203 GPS and Real-Time Tracking | UNMAPPED — source unavailable | Operations manager | Detect stale telemetry and route deviation alerts | Should | Current location; route public interface | `tracking` | `/tracking/alerts` | tracking_alert | Tracking alerts | 14 |
| EP-203 GPS and Real-Time Tracking | UNMAPPED — source unavailable | Auditor | Position history with retention controls | Could | Stable ingestion | `tracking` | `/tracking/vehicles/{id}/history` | position_observation partitions/archive | Tracking history | 15 |
| EP-204 Delivery Management | UNMAPPED — source unavailable | Dispatcher | Create delivery obligations from assigned freight | Must | EP-202 freight-trip assignment | `delivery` | `/deliveries` | delivery, delivery_item | Deliveries | 16 |
| EP-204 Delivery Management | UNMAPPED — source unavailable | Driver/operator | Record arrival and delivery outcome | Must | Delivery obligation; trip lifecycle interface | `delivery` | `/deliveries/{id}/arrive`, `/deliveries/{id}/complete`, `/deliveries/{id}/fail` | delivery_event, delivery_status_history | Delivery execution | 17 |
| EP-204 Delivery Management | UNMAPPED — source unavailable | Driver/operator | Capture proof-of-delivery metadata and file reference | Must | Delivery completion | `delivery` | `/deliveries/{id}/proof` | proof_of_delivery | Proof of delivery | 18 |
| EP-204 Delivery Management | UNMAPPED — source unavailable | Customer-service user | Delivery exception and redelivery workflow | Should | Delivery outcomes | `delivery` | `/deliveries/{id}/exceptions`, `/deliveries/{id}/reschedule` | delivery_exception, redelivery_plan | Delivery exceptions | 19 |
| EP-204 Delivery Management | UNMAPPED — source unavailable | Operations manager | Delivery performance reporting | Should | Delivery events | `delivery`, `reporting` | `/reports/delivery/**` | delivery reporting projection | Delivery reports | 20 |
| EP-205 Advanced Routes | UNMAPPED — source unavailable | Route planner | Route templates and reusable ordered stop plans | Must | Phase 1 routing | `routing` | `/routes/templates` | route_template, route_template_stop | Route templates | 21 |
| EP-205 Advanced Routes | UNMAPPED — source unavailable | Route planner | Stop windows, service durations and constraints | Must | Route templates | `routing` | `/routes/{id}/constraints` | route_stop_constraint | Route constraints | 22 |
| EP-205 Advanced Routes | UNMAPPED — source unavailable | Dispatcher | Generate a trip route plan from a template | Must | Templates; trip route-assignment boundary | `routing` | `/routes/plans` | route_plan, route_plan_stop | Route planning | 23 |
| EP-205 Advanced Routes | UNMAPPED — source unavailable | Dispatcher | Re-sequence stops and record route-plan revisions | Should | Route plans | `routing` | `/routes/plans/{id}/revisions` | route_plan_revision | Route-plan editor | 24 |
| EP-205 Advanced Routes | UNMAPPED — source unavailable | Operations manager | Planned-versus-actual route analytics | Should | EP-203 tracking; route plans | `routing`, `tracking`, `reporting` | `/reports/routes/**` | route performance projection | Route analytics | 25 |

## Requested feature traceability

The following matrix makes every requested Phase 2 feature explicit. The slice references are planning groupings, not replacements for the missing authoritative user-story identifiers.

| Epic | User Story ID | Actor | Feature | Priority | Dependency | Backend module | Expected REST API area | Expected DB entities/projections | Frontend page | Implementation slice |
|---|---|---|---|---|---|---|---|---|---|---:|
| EP-201 | US-31 — IMPLEMENTED | Fuel officer | Fuel Issue | Must | Phase 1 fleet/trip/identity public interfaces | `fuel` | `/fuel-issues`, `/fuel-stations` | fuel_issue, fuel_issue_history, fuel_station, fuel_limit_policy | Fuel issues | Delivered |
| EP-201 | US-32 — IMPLEMENTED | Fuel manager | Fuel Purchase | Must | Vendor and fuel station references | `fuel`, `organization` | `/fuel-purchases`, `/fuel-prices`, `/vendors` | vendor, fuel_price, fuel_purchase, fuel_purchase_history | Fuel purchases and prices | Delivered |
| EP-201 | UNMAPPED — source unavailable | Fleet manager | Mileage & KM Tracking | Must | Trip/vehicle public events | `fuel` | `/fuel/consumption` | fuel_mileage_reading/projection | Fuel consumption | 3 |
| EP-201 | UNMAPPED — source unavailable | Fleet manager | Fuel Cost per Trip | Should | Fuel issues and trip linkage | `fuel`, `reporting` | `/reports/fuel/trip-costs` | trip_fuel_cost_projection | Fuel reports | 5 |
| EP-201 | UNMAPPED — source unavailable | Fleet administrator | Fuel Cards | Should | Fuel issue controls | `fuel` | `/fuel/cards` | fuel_card, fuel_card_assignment | Fuel cards | 4 |
| EP-201 | UNMAPPED — source unavailable | Fuel officer | Bunker Management | Must | Fuel reference data | `fuel` | `/fuel/bunkers/**` | bunker, bunker_stock_ledger | Bunker stock | 1–2 |
| EP-201 | UNMAPPED — source unavailable | Fleet manager | Fuel Analytics | Should | Stable purchase/issue data | `fuel`, `reporting` | `/reports/fuel/**` | fuel reporting projections | Fuel reports | 5 |
| EP-201 | UNMAPPED — source unavailable | Fuel officer | Fuel Exceptions | Should | Purchase/issue workflows | `fuel` | `/fuel/exceptions` | fuel_exception | Fuel exceptions | 5 |
| EP-202 | UNMAPPED — source unavailable | Operations planner | Freight Orders | Must | Organization/location interfaces | `freight` | `/freight/orders` | freight_order | Freight orders | 6 |
| EP-202 | UNMAPPED — source unavailable | Warehouse operator | Cargo Manifest | Must | Freight order/cargo items | `freight` | `/freight/orders/{id}/manifest` | cargo_manifest, cargo_manifest_item | Cargo manifest | 7 |
| EP-202 | UNMAPPED — source unavailable | Load planner | Load Planning | Must | Manifest and fleet capacity view | `freight` | `/freight/load-plans` | load_plan, load_plan_item | Load planning | 7–8 |
| EP-202 | UNMAPPED — source unavailable | Load planner | Weight & Volume | Must | Cargo items | `freight` | `/freight/orders/{id}/cargo` | cargo_item, cargo_requirement | Cargo editor | 7 |
| EP-202 | UNMAPPED — source unavailable | Freight administrator | Cargo Insurance | Should | Freight/cargo identity | `freight` | `/freight/orders/{id}/insurance` | cargo_insurance_reference | Freight compliance | 7 |
| EP-202 | UNMAPPED — source unavailable | Operations manager | Freight Reports | Should | Assignment/handling events | `freight`, `reporting` | `/reports/freight/**` | freight reporting projections | Freight reports | 10 |
| EP-202 | UNMAPPED — source unavailable | Operations planner | Freight Exceptions | Should | Freight lifecycle | `freight` | `/freight/orders/{id}/exceptions` | freight_exception | Freight exceptions | 10 |
| EP-203 | UNMAPPED — source unavailable | Dispatcher | Live Vehicle Tracking | Must | Position ingestion | `tracking` | `/tracking/vehicles/{id}/latest` | latest_position_projection | Live tracking | 13 |
| EP-203 | UNMAPPED — source unavailable | Operations manager | Geofencing | Should | Live positions and approved geofences | `tracking` | `/tracking/geofences` | geofence, geofence_event | Tracking configuration/map | 14 |
| EP-203 | UNMAPPED — source unavailable | Safety officer | Speed Alerts | Should | Position ingestion with speed | `tracking` | `/tracking/alerts?type=SPEED` | tracking_alert | Tracking alerts | 14 |
| EP-203 | UNMAPPED — source unavailable | Fleet manager | Idle Monitoring | Should | Position ingestion | `tracking` | `/tracking/idle-periods` | idle_period/projection | Tracking dashboard | 14 |
| EP-203 | UNMAPPED — source unavailable | Dispatcher | Route Deviations | Should | Route plan and live position | `tracking`, `routing` | `/tracking/alerts?type=ROUTE_DEVIATION` | tracking_alert | Tracking alerts/map | 14 |
| EP-203 | UNMAPPED — source unavailable | Auditor | Replay / History | Could | Stable ingestion and retention | `tracking` | `/tracking/vehicles/{id}/history` | position_observation archive | Tracking history | 15 |
| EP-203 | UNMAPPED — source unavailable | Dispatcher | Tracking Dashboard | Must | Latest positions/alerts | `tracking`, `reporting` | `/tracking/dashboard` | tracking dashboard projection | Tracking dashboard | 13–14 |
| EP-203 | UNMAPPED — source unavailable | Dispatcher | GPS Exceptions | Should | Alert detection | `tracking` | `/tracking/exceptions` | tracking_exception | GPS exceptions | 14 |
| EP-204 | UNMAPPED — source unavailable | Dispatcher | Delivery Orders | Must | Freight-trip assignment | `delivery` | `/deliveries` | delivery, delivery_item | Deliveries | 15 |
| EP-204 | UNMAPPED — source unavailable | Driver/operator | Proof of Delivery | Must | Delivery outcome | `delivery` | `/deliveries/{id}/proof` | proof_of_delivery | Proof capture | 17 |
| EP-204 | UNMAPPED — source unavailable | Driver/operator | Signature / Photo | Must | File-reference/security decision | `delivery` | `/deliveries/{id}/proof` | proof_attachment | Proof capture | 17 |
| EP-204 | UNMAPPED — source unavailable | Driver/operator | Failed Deliveries | Must | Delivery lifecycle | `delivery` | `/deliveries/{id}/fail` | delivery_event, delivery_exception | Delivery execution | 16–18 |
| EP-204 | UNMAPPED — source unavailable | Dispatcher | Re-Delivery | Should | Failed-delivery outcome | `delivery` | `/deliveries/{id}/reschedule` | redelivery_plan | Delivery exceptions | 18 |
| EP-204 | UNMAPPED — source unavailable | Operations manager | Delivery Analytics | Should | Delivery events | `delivery`, `reporting` | `/reports/delivery/**` | delivery reporting projection | Delivery reports | 18 |
| EP-204 | UNMAPPED — source unavailable | Customer-service user | Delivery Exceptions | Should | Delivery workflow | `delivery` | `/deliveries/{id}/exceptions` | delivery_exception | Delivery exceptions | 18 |
| EP-205 | UNMAPPED — source unavailable | Route planner | Multi-stop improvements | Must | Existing route stops | `routing` | `/routes/templates`, `/routes/plans` | route_template_stop, route_plan_stop | Route-plan editor | 19–20 |
| EP-205 | UNMAPPED — source unavailable | Route planner | Distance / ETA enhancements | Should | Route plans; approved provider/integration | `routing` | `/routes/plans/{id}/estimates` | route_estimate | Route planning | 20 |
| EP-205 | UNMAPPED — source unavailable | Auditor | Route History | Should | Route-plan revision workflow | `routing` | `/routes/plans/{id}/revisions` | route_plan_revision | Route history | 20 |
| EP-205 | UNMAPPED — source unavailable | Operations manager | Route Analytics | Should | Tracking plus route plans | `routing`, `tracking`, `reporting` | `/reports/routes/**` | route performance projection | Route analytics | 21 |
| EP-205 | UNMAPPED — source unavailable | Route planner | Route Restrictions | Must | Route constraint model | `routing` | `/routes/{id}/constraints` | route_stop_constraint, route_restriction | Route constraints | 19 |
| EP-205 | UNMAPPED — source unavailable | Dispatcher | Route disruption handling | Should | Route revisions and active trips | `routing` | `/routes/plans/{id}/disruptions` | route_disruption, route_plan_revision | Route-plan editor | 20 |

Last-mile optimization, consumer-facing tracking, automated dispatch optimization, and external marketplace capabilities are **not confirmed Phase 2 scope**. Treat them as Phase 3 candidates until they appear in the authoritative requirements source.

## Dependency order

1. Establish fuel reference data and immutable stock ledgers before issuing or reconciling fuel.
2. Establish freight ownership and cargo constraints before linking freight to trips or confirming handling.
3. Establish trusted device identity and ingestion before maps, alerts, or history are meaningful.
4. Establish delivery obligations from freight before proof, exceptions, or performance reports.
5. Establish reusable route templates and constraints before generated plans, revisions, and planned-versus-actual analytics.
6. Add analytics only after the underlying transaction/event data and retention rules are stable.

Cross-module integration must use public application/module interfaces or events. No module may import another module's JPA entity, Spring Data repository, or persistence adapter.

## Backend module strategy

### `fuel`

- Owns: FuelType, FuelVendor, Bunker, FuelPurchase, FuelIssue, FuelCard, FuelAllocation and stock-ledger aggregates.
- Depends on: read-only fleet identity/eligibility and trip-reference interfaces; identity authorities.
- Publishes: `FuelPurchased`, `FuelIssued`, `FuelStockAdjusted`, `FuelVarianceDetected`.
- Consumes: relevant trip completion/cancellation and vehicle retirement events.
- Public interfaces: validate vehicle/trip references, query issue summaries, expose reporting projections.

### `freight`

- Owns: FreightOrder, CargoItem, CargoRequirement, FreightTripAssignment and CargoHandlingEvent.
- Depends on: organization customer/project and location public interfaces; trip reference interface.
- Publishes: `FreightAssignedToTrip`, `CargoLoaded`, `CargoUnloaded`, `FreightExceptionRaised`.
- Consumes: trip cancellation/completion events.
- Public interfaces: assignment readiness and freight summary for trip operations.

### `tracking`

- Owns: TrackingDevice, VehicleDeviceBinding, PositionObservation, LatestPosition and TrackingAlert.
- Depends on: fleet vehicle references, trip active-assignment view and routing plan view.
- Publishes: `PositionReceived`, `TelemetryBecameStale`, `RouteDeviationDetected`, `GeofenceEntered` when geofencing is approved.
- Consumes: vehicle assignment, dispatch, trip start/completion and device-retirement events.
- Public interfaces: latest position and bounded history queries; ingestion is isolated from operator APIs.

### `delivery`

- Owns: Delivery, DeliveryItem, DeliveryEvent, ProofOfDelivery, DeliveryException and RedeliveryPlan.
- Depends on: freight assignment and trip lifecycle public interfaces; shared file-reference abstraction.
- Publishes: `DeliveryArrived`, `DeliveryCompleted`, `DeliveryFailed`, `ProofOfDeliveryRecorded`.
- Consumes: cargo loaded, trip started/cancelled/completed events.
- Public interfaces: trip delivery-readiness and delivery summary queries.

### Existing `routing` expansion

- Continues to own routes and stops; adds RouteTemplate, RoutePlan, constraints and revisions.
- Depends on: location public interfaces and optionally tracking projections for actual-performance comparison.
- Publishes: `RoutePlanCreated`, `RoutePlanRevised`.
- Consumes: trip route-assignment and position events only where needed for projections.
- Public interfaces: route eligibility, route-plan details and trip-compatible plan lookup.

`reporting` consumes module events or stable query projections. It must not become an ownership module or query other modules' repositories directly.

## Frontend strategy

Use feature-oriented folders while retaining the shared Ant Design application shell:

```text
frontend/src/
  fuel/{api,components,hooks,pages,schemas,types}
  freight/{api,components,hooks,pages,schemas,types}
  tracking/{api,components,hooks,pages,types}
  delivery/{api,components,hooks,pages,schemas,types}
  routing/{components,hooks,pages,schemas}
  reporting/{components,hooks,pages}
  shared/{api,auth,components,errors,forms,status}
```

Planned route groups are `/fuel/**`, `/freight/**`, `/tracking/**`, `/deliveries/**`, and `/routes/plans/**`. Navigation and actions remain permission-driven using backend authority names; proposed authorities must be approved with the story mapping rather than silently added. TanStack Query hooks own server state, React Hook Form plus Zod own client validation, and backend field errors remain authoritative. Shared dependencies should be limited to the existing API client, permission checks, paged-table patterns, file-reference controls, status tags and error mapping. Do not create a `last-mile` frontend area unless that scope is formally approved.

## Vertical implementation slices

Each slice includes domain behavior, ports, service, persistence, migration, REST contract, authorization, tests, frontend query/form/table flow, and documentation.

| Order | Epic | Vertical slice | Dependencies | Backend/API | Frontend | Database | Priority |
|---:|---|---|---|---|---|---|---|
| Delivered exception | EP-201 | US-31 trip-linked Fuel Issue | Phase 1 fleet/trip/identity interfaces | Fuel issue lifecycle and station lookup | Fuel issue list/create/detail | V11 fuel issue, history, station, policy | Delivered |
| Delivered | EP-201 | US-32 vendor pricing and Fuel Purchase | US-31 station; organization vendor boundary | Purchase lifecycle, pricing, receipt variance, reconciliation | Purchase list/editor/details and prices | V12 vendor, prices, purchases, history | Delivered |
| 1 | EP-201 | Fuel reference foundations | Phase 2 gates | Fuel type/vendor/bunker CRUD | Fuel configuration pages | V-next reference tables | Must |
| 2 | EP-201 | Purchase and bunker receipt ledger | Slice 1 | Purchase/receipt commands and stock query | Purchase entry and stock view | Purchase and immutable ledger | Must |
| 3 | EP-201 | Bunker-ledger integration for issued fuel | Slice 2; delivered US-31 issue workflow | Post issue movements to the immutable stock ledger without redefining US-31 | Stock impact on issue details | Issue-to-ledger reference and stock movement | Must |
| 4 | EP-201 | Fuel card controls | Slice 3 | Card assignment/allocation APIs | Card administration | Card and allocation tables | Should |
| 5 | EP-201 | Fuel reconciliation report | Slices 2–4 | Reporting projection/API | Consumption/variance report | Projection or optimized views | Should |
| 6 | EP-202 | Freight order and cargo capture | Phase 2 gates | Freight/cargo aggregate API | Freight list/editor | Freight and cargo tables | Must |
| 7 | EP-202 | Cargo constraints and validation | Slice 6 | Constraint commands/read model | Cargo requirements editor | Requirement table | Must |
| 8 | EP-202 | Freight-to-trip assignment | Slice 7; trip interface | Transactional assignment API | Assignment drawer | Assignment/history tables | Must |
| 9 | EP-202 | Load/unload handling | Slice 8 | Handling-event commands | Handling action modals/timeline | Handling event table | Should |
| 10 | EP-202 | Freight exceptions/reporting | Slice 9 | Exception and report APIs | Exceptions and overview | Status/report projections | Should |
| 11 | EP-203 | Device registry and binding | Phase 2 gates; fleet interface | Device/binding APIs | Device administration | Device/binding tables | Must |
| 12 | EP-203 | Secure position ingestion | Slice 11 | Idempotent ingestion API | Integration status only | Position storage and dedupe key | Must |
| 13 | EP-203 | Current-position operations view | Slice 12; trip interface | Latest-position queries | Live operations map/list | Latest-position projection | Must |
| 14 | EP-203 | Stale and deviation alerts | Slice 13; routing interface | Detection and alert APIs | Alert list/details | Alert table | Should |
| 15 | EP-204 | Delivery obligation creation | EP-202 slice 8 | Delivery aggregate/API | Delivery list/details | Delivery/item tables | Must |
| 16 | EP-204 | Arrival and outcome workflow | Slice 15; trip interface | Lifecycle commands/history | Execution modals/timeline | Event/history tables | Must |
| 17 | EP-204 | Proof of delivery | Slice 16; file-reference decision | Proof metadata API | Proof capture/view | Proof table/file references | Must |
| 18 | EP-204 | Exceptions and redelivery | Slice 17 | Exception/reschedule APIs | Exception workflow | Exception/redelivery tables | Should |
| 19 | EP-205 | Route templates and constraints | Phase 2 gates | Template/constraint APIs | Template editor | Template/stop/constraint tables | Must |
| 20 | EP-205 | Trip route plans and revisions | Slice 19; trip interface | Plan/generate/revise APIs | Plan builder/revision view | Plan/stop/revision tables | Must |
| 21 | EP-205 | Planned-versus-actual analytics | EP-203 slice 13; slice 20 | Route performance report | Route analytics | Performance projection | Should |

The first two Phase 2 slices, **US-31 Fuel Issue** and **US-32 Manage Fuel Purchases**, are implemented. US-32 deliberately stops at organization-owned vendor maintenance, expected-price catalogue, invoice/receipt variance, and reconciliation; it does not create a bunker ledger or accounting integration. The exact next recommended slice is **US-33 Mileage & KM Tracking enhancements**, but implementation must wait for an authoritative US-33 contract. US-34 through US-38 remain deferred.
