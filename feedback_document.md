# Fleet Feature-First Hexagonal Refactor Feedback

## Task

- Task ID: `FLEET-ARCH-REF-001`
- Implemented slice: `FLEET-HEX-001 — Vehicle Master`
- Status: complete for the Vehicle Master slice
- Baseline branch: `feature/mvp-gap-011i-us71-closure`
- Baseline commit: `361707f9258d7d875020e4629e528b9cf709096c`
- Commit/push performed: no

## Actual Fleet Scope Found

The pre-refactor inventory contains 285 Fleet Java files: 230 production files and 55 tests. The inventory and current-to-target disposition are recorded in `docs/architecture/fleet/FLEET-CURRENT-TO-TARGET-MAP.md`.

Vehicle Master currently owns registration identity, manufacturer/model/year, VIN/chassis/engine identifiers, vehicle type, capacity, ownership classification, operational status, active state, odometer, CRUD, status transitions, and deactivation. Existing behavior does not contain a Vehicle-owned QR-code capability or a Vehicle-owned dashboard metric. Those capabilities were not invented. Deactivation remains the existing retirement representation; no new `RETIRED` status was added.

## Architecture Before and After

Before this slice, Vehicle Master was distributed through the Fleet-wide `domain`, `application`, and `infrastructure` layers, and Vehicle CRUD shared the mixed `FleetController`.

After this slice, Vehicle Master is isolated under:

```text
fleet/vehiclemaster/
  domain/
  ports/inbound/
  ports/outbound/
  application/
  adapters/inbound/web/
  adapters/outbound/persistence/
  adapters/configuration/
```

The root `AGENTS.md` now defines feature-first hexagonal governance, dependency direction, provider-neutral ports, pure domain/application rules, adapter ownership, transaction placement, persistence mapping, event boundaries, API/database compatibility, test expectations, Git safety, and stop conditions.

## Files Moved or Created

Moved into the Vehicle Master feature:

- `Vehicle` domain aggregate
- composed `VehicleUseCase` inbound boundary
- `VehicleRepository` outbound boundary
- `VehicleService`
- `VehicleRequest` and `VehicleResponse`
- `VehicleEntity`, `VehicleJpaRepository`, and `VehiclePersistenceAdapter`
- Vehicle domain, service, controller, and JPA integration tests

Created for explicit boundaries and adapter separation:

- `CreateVehicleUseCase`
- `GetVehicleUseCase`
- `ListVehiclesUseCase`
- `UpdateVehicleUseCase`
- `DeactivateVehicleUseCase`
- `InvalidVehicleStatusTransitionException`
- `VehicleController`
- `VehicleWebMapper`
- `VehicleOperationResponse`
- `VehiclePersistenceMapper`
- `VehicleMasterConfig`
- `VehiclePersistenceMapperTest`

Existing Fleet and cross-module files were modified only where required to import the moved pure Vehicle aggregate or Vehicle repository port. The legacy mixed `FleetController` retains deferred Fleet capabilities but no longer owns Vehicle CRUD.

## Domain and Port Purity

Automated architecture checks report the following forbidden dependency counts for Vehicle Master:

| Layer | Spring | JPA | Hibernate | Jackson | Spring Data |
|---|---:|---:|---:|---:|---:|
| Domain | 0 | 0 | 0 | 0 | 0 |
| Inbound/outbound ports | 0 | 0 | 0 | 0 | 0 |
| Application | 0 | 0 | 0 | 0 | 0 |

The domain now raises a pure Java `InvalidVehicleStatusTransitionException`. The application service translates it to the existing shared conflict contract, preserving error code `VEHICLE_STATUS_TRANSITION_INVALID` and HTTP behavior without coupling the aggregate to a web-oriented exception.

## Application and Adapters

- The application service implements the focused inbound use cases and depends only on the outbound `VehicleRepository` port.
- Transaction boundaries remain defined by the use-case need and applied by Spring infrastructure configuration.
- `VehicleController` owns the existing `/vehicles` CRUD endpoints and permission annotations.
- `VehicleWebMapper` owns request/domain/response translation.
- `VehiclePersistenceMapper` explicitly maps pure domain objects to and from `VehicleEntity`.
- `VehiclePersistenceAdapter` implements the outbound port and is the only Vehicle Master application-facing persistence implementation.
- `FleetReportingAdapter` now consumes the Vehicle repository port instead of importing the Vehicle JPA repository/entity.
- No production module outside Fleet imports Vehicle Master adapters.

## API, Security, and Database Compatibility

- Existing `/vehicles` HTTP paths, methods, request fields, response fields, success messages, validation, permissions, and conflict behavior were preserved.
- Vehicle mutation authorization remains enforced before the use case is invoked.
- No database schema was changed.
- No Flyway migration was created or edited.
- Flyway successfully validated and applied the existing 29 migrations in H2 test/application contexts.
- PostgreSQL production invariant tests remain part of the green backend suite; no provider-specific type leaked into the domain, ports, or application layer.

## Verification Evidence

### Backend

- Focused Vehicle Master, architecture, Modulith, and integration suite: 51 tests, 0 failures, 0 errors, 0 skipped.
- Business authorization integration: 4 tests, 0 failures, 0 errors.
- `mvn clean test`: 718 tests, 0 failures, 0 errors, 22 skipped; build successful.
- `mvn verify`: 718 tests, 0 failures, 0 errors, 22 skipped; packaged build successful.
- Spring Modulith verification passed through `ApplicationModulesTest`.
- Spring application contexts started successfully throughout the integration suite and direct `h2,e2e` startup validation.

### Frontend and Browser Contract

- `npm test`: 33 test files and 170 tests passed.
- `npm run build`: successful. Vite reported only the existing large-chunk advisory.
- Focused Playwright Vehicle Master regression on Chromium: 4 tests passed.
- `npm run lint`: failed on an unrelated pre-existing unused `PlusOutlined` import in `frontend/src/fuel/FuelPricePage.tsx:21`. Fuel Pricing is outside this task, so it was not modified.

## Problems Found and Resolution

- The original mixed controller combined Vehicle Master with deferred Fleet functions. Vehicle CRUD was extracted into its own inbound adapter while leaving deferred endpoints intact.
- Reporting imported Vehicle JPA types directly. It now uses the Vehicle outbound port.
- The domain status-transition exception depended on a shared application/web exception. A pure domain exception plus application translation removed that inward dependency without changing the public error contract.
- Moving the aggregate required mechanical import updates in existing Fleet consumers and integration fixtures. Focused and full tests confirm those consumers remain compatible.
- An initial broad import substitution also touched similarly named Vehicle classes. It was detected immediately, reversed, and verified by compilation and full regression tests before completion.
- The first E2E invocation passed a Windows path that Playwright did not match. Re-running with the repository-compatible forward-slash path started both configured web servers and passed all focused scenarios.

## Deferred Features

The following refactors remain deliberately deferred and protected from this slice:

- `FLEET-HEX-002` — Fleet Categories and Vehicle Types
- Vehicle Documents
- Vehicle Availability and Allocation collaboration
- Fuel/Lubricant records
- Running Logs and Vehicle Readings
- Maintenance and maintenance-driven availability
- Driver profile, licensing, availability, and compliance features

No production behavior for those capabilities was redesigned or moved into a new feature package.

## Next Task

`FLEET-HEX-002 — Fleet Categories and Vehicle Types`

Start from the inventory map, preserve the existing category/type API and schema, extract only the category/type aggregate and its ports/application/adapters, and keep Vehicle Master depending on provider-neutral contracts rather than category/type persistence.

# Fleet Frontend Feature-First Refactor Feedback

## Task

`FLEET-FRONTEND-REF-001`

## Before

Vehicle CRUD, documents, maintenance, readings, and lubricant presentation were coordinated by the generic `ResourceListPage` and `ResourceEditorModal`. Fleet navigation was flat, Vehicle-specific API calls and error handling lived in generic page files, category/type identifiers were rendered as UUIDs, and the generic page owned concerns that could not evolve independently by feature.

## After

Vehicle Master now owns its API, TanStack Query hooks, Ant Design components, page orchestration, types, and Zod validation under `frontend/src/features/fleet/vehicleMaster`. `AppLayout` remains the only owner of the sidebar, header, breadcrumb, and page title. The existing `/fleet/vehicles` route, backend endpoints, permissions, query compatibility, and deferred Fleet integrations remain intact.

## Vehicle Master

The list supports search, ownership and operational-status filters, sorting, pagination, explicit operational and lifecycle presentation, Company-owned and Leased/rental filtering from one Vehicle registry, responsive columns, and accessible row actions. Category and type reference APIs resolve user-facing classification names in the list and details drawer. The backend currently supports `AVAILABLE`, `ALLOCATED`, `MAINTENANCE`, `OUT_OF_SERVICE`, and `BROKEN_DOWN`; deactivation is the supported historical-preserving lifecycle operation. No unsupported `RETIRED` state or QR workflow was invented.

## Navigation

The permission-aware navigation is recursive and now renders:

- Fleet Management
  - Vehicle Master
  - Fleet Categories
    - Vehicle Categories
    - Vehicle Types

Only routes that already exist are navigable. Recursive trail resolution keeps nested selections and the `Home / Fleet Management / Vehicle Master` breadcrumb correct for direct navigation.

## API

`vehicleApi.ts` owns the existing `/vehicles`, `/vehicle-categories`, `/vehicle-types`, and Vehicle document calls used by Vehicle Master. No backend path, payload, method, permission, or database contract changed.

## Hooks

`useVehicles.ts` centralizes list, detail, reference, document, create, update, and deactivate queries/mutations. Existing `vehicles-page` cache compatibility is preserved for deferred reading, trip, and offline-sync consumers.

## Components

- `VehicleTable` owns responsive Vehicle list presentation and accessible actions.
- `VehicleEditorModal` owns React Hook Form, Zod, Ant Design controls, reference selection, and stable backend field-error mapping.
- `VehicleDetailsDrawer` owns overview/classification presentation and composes the existing document, maintenance, reading, and lubricant sections without moving those deferred features.

## Pages

`VehicleListPage` owns Vehicle Master filters, query composition, permission-aware actions, direct details deep-link state through `vehicleId`, document editor coordination, and backend-authoritative lifecycle feedback. It does not duplicate the global title or breadcrumb.

## Validation

`vehicleSchema.ts` is the single client-side Vehicle form schema. Required identifiers and master references, manufacture year, odometer, engine hours, and capacity constraints are mapped into React Hook Form. Backend validation and conflict responses remain authoritative.

## RBAC

`VEHICLE_VIEW` controls route/navigation visibility. Create, edit, deactivate, and document actions use `VEHICLE_CREATE`, `VEHICLE_UPDATE`, `VEHICLE_STATUS_UPDATE`, and `VEHICLE_DOCUMENT_MANAGE` respectively. Read-only actors do not receive mutation controls; backend authorization remains authoritative.

## Routes

The canonical route remains `/fleet/vehicles`. Existing query-parameter details deep links remain compatible. No separate Company/Rental, status-action, QR, or unsupported `/fleet/vehicles/:id` route was introduced.

## UI Issues Fixed

- Removed duplicate Vehicle page title/breadcrumb ownership from the child page.
- Replaced raw category/type UUIDs with readable names.
- Added explicit `ALLOCATED` status presentation.
- Reduced the fixed actions column to compact, tooltip-backed accessible icon buttons.
- Made the Vehicle table content-driven and responsive at desktop, tablet, and mobile widths.
- Added semantic breadcrumb navigation.
- Preserved separate operational status and active/inactive lifecycle concepts.
- Removed the unrelated dead `PlusOutlined` Fuel Price import that blocked the repository lint gate; Fuel behavior was not changed.
- Visual review found an existing mojibake glyph in the deferred Vehicle Running Logs empty/epoch text. That belongs to `FLEET-FE-006` and was not changed here.

## Tests

- `npm run lint`: passed with zero warnings.
- `npm test`: 35 files, 178 tests passed.
- `npm run build`: passed; only the existing Vite large-chunk advisory remains.
- Focused Vehicle/Fleet Playwright: 30 tests passed across Chromium, Firefox, and WebKit.
- Full Playwright completion run: 210 tests passed across Chromium, Firefox, and WebKit. During an earlier confirmation run, unrelated Firefox `E2E-OFF-008` observed one timing-sensitive missing `nextAttemptAt` assertion; the exact scenario passed immediately on isolated rerun and the subsequent clean full run passed 210/210. No Fleet test failed.
- Visual review covered desktop, 768px tablet, and 390px mobile layouts, plus create modal and details drawer behavior.

## Deferred

- Fleet Categories (`FLEET-FE-002`)
- Vehicle Documents (`FLEET-FE-003`)
- Vehicle Allocation (`FLEET-FE-004`)
- Fuel & Lubricant (`FLEET-FE-005`)
- Running Logs (`FLEET-FE-006`)
- Maintenance (`FLEET-FE-007`)
- Fleet navigation/edge-case cleanup (`FLEET-FE-008`)

## Next Task

`FLEET-FE-002 — Fleet Categories`
