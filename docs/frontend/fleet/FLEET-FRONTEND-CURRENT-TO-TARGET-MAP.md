# Fleet Frontend Current-to-Target Map

## Baseline

- Task: `FLEET-FRONTEND-REF-001`
- Implemented slice for this run: `FLEET-FE-001 — Vehicle Master`
- Frontend: React, TypeScript, Vite, Ant Design, React Router, TanStack Query, React Hook Form, Zod, Vitest, Testing Library, MSW, Playwright
- Existing public Vehicle route: `/fleet/vehicles`
- Backend contracts: unchanged
- Backend Vehicle status values observed: `AVAILABLE`, `ALLOCATED`, `MAINTENANCE`, `OUT_OF_SERVICE`, `BROKEN_DOWN`
- Backend Vehicle ownership values observed: `COMPANY_OWNED`, `LEASED`
- Vehicle retirement representation: deactivation through `DELETE /vehicles/{id}`; no separate `RETIRED` enum
- Vehicle QR API/UI: not present

## Current Fleet-Owned Files

| Current File | Current Path | Feature | Current Responsibility | Target Path | Move? | Rename? | Shared? | Risk |
|---|---|---|---|---|---|---|---|---|
| `types.ts` | `frontend/src/fleet/types.ts` | Running Log | Vehicle reading, mileage, reset, offline payload types | `frontend/src/features/fleet/runningLog/types/` | Deferred | TBD | No | High: US-71 offline contracts |
| `useVehicleReadings.ts` | `frontend/src/fleet/useVehicleReadings.ts` | Running Log | Reading queries, mutations, cache invalidation | `frontend/src/features/fleet/runningLog/hooks/` | Deferred | No | No | High: offline replay integration |
| `useVehicleReadings.test.tsx` | `frontend/src/fleet/useVehicleReadings.test.tsx` | Running Log | Reading hook regression | Matching running-log test path | Deferred | No | No | High |
| `VehicleReadingsSection.tsx` | `frontend/src/fleet/VehicleReadingsSection.tsx` | Running Log | Readings, mileage, resets, offline queue UX | `frontend/src/features/fleet/runningLog/components/` | Deferred | No | No | High |
| `VehicleReadingsSection.test.tsx` | `frontend/src/fleet/VehicleReadingsSection.test.tsx` | Running Log | Running-log component tests | Matching running-log test path | Deferred | No | No | High |
| `useVehicleMaintenance.ts` | `frontend/src/fleet/useVehicleMaintenance.ts` | Maintenance | Maintenance queries and commands | `frontend/src/features/fleet/maintenance/hooks/` | Deferred | No | No | High: availability linkage |
| `VehicleMaintenanceSection.tsx` | `frontend/src/fleet/VehicleMaintenanceSection.tsx` | Maintenance | Schedule, reschedule, complete, cancel UI | `frontend/src/features/fleet/maintenance/components/` | Deferred | No | No | High |
| `VehicleMaintenanceSection.test.tsx` | `frontend/src/fleet/VehicleMaintenanceSection.test.tsx` | Maintenance | Maintenance UI tests | Matching maintenance test path | Deferred | No | No | Medium |
| `useVehicleLubricantLogs.ts` | `frontend/src/fleet/useVehicleLubricantLogs.ts` | Fuel/Lubricant | Vehicle lubricant log queries/commands | `frontend/src/features/fleet/fuelLubricant/hooks/` | Deferred | No | No | Medium |
| `VehicleLubricantSection.tsx` | `frontend/src/fleet/VehicleLubricantSection.tsx` | Fuel/Lubricant | Vehicle fluid/lubricant history and entry UI | `frontend/src/features/fleet/fuelLubricant/components/` | Deferred | No | No | Medium: do not duplicate Fuel module |
| `VehicleLubricantSection.test.tsx` | `frontend/src/fleet/VehicleLubricantSection.test.tsx` | Fuel/Lubricant | Lubricant UI tests | Matching fuel/lubricant test path | Deferred | No | No | Medium |
| `useDriverDrugTests.ts` | `frontend/src/fleet/useDriverDrugTests.ts` | Driver Compliance | Driver drug-test server state | Current path pending Driver target decision | No | No | No | Protected; outside this task |
| `DriverDrugTestSection.tsx` | `frontend/src/fleet/DriverDrugTestSection.tsx` | Driver Compliance | Driver drug-test UX | Current path pending Driver target decision | No | No | No | Protected |
| `DriverDrugTestSection.test.tsx` | `frontend/src/fleet/DriverDrugTestSection.test.tsx` | Driver Compliance | Drug-test UI tests | Current path | No | No | No | Protected |
| `useDriverExceptions.ts` | `frontend/src/fleet/useDriverExceptions.ts` | Driver Availability | Driver exception server state | Current path pending Driver target decision | No | No | No | Protected |
| `DriverExceptionSection.tsx` | `frontend/src/fleet/DriverExceptionSection.tsx` | Driver Availability | Driver leave/exception workflows | Current path pending Driver target decision | No | No | No | Protected |
| `DriverExceptionSection.test.tsx` | `frontend/src/fleet/DriverExceptionSection.test.tsx` | Driver Availability | Exception UI tests | Current path | No | No | No | Protected |
| `useDriverMedicalRecords.ts` | `frontend/src/fleet/useDriverMedicalRecords.ts` | Driver Compliance | Medical-record server state | Current path pending Driver target decision | No | No | No | Protected |
| `DriverMedicalSection.tsx` | `frontend/src/fleet/DriverMedicalSection.tsx` | Driver Compliance | Driver medical UX | Current path pending Driver target decision | No | No | No | Protected |
| `DriverMedicalSection.test.tsx` | `frontend/src/fleet/DriverMedicalSection.test.tsx` | Driver Compliance | Medical UI tests | Current path | No | No | No | Protected |
| `useDriverPerformance.ts` | `frontend/src/fleet/useDriverPerformance.ts` | Driver Performance | Performance query | Current path pending Driver target decision | No | No | No | Protected |
| `DriverPerformanceSection.tsx` | `frontend/src/fleet/DriverPerformanceSection.tsx` | Driver Performance | Driver scorecard | Current path pending Driver target decision | No | No | No | Protected |
| `DriverPerformanceSection.test.tsx` | `frontend/src/fleet/DriverPerformanceSection.test.tsx` | Driver Performance | Performance UI tests | Current path | No | No | No | Protected |
| `useDriverViolations.ts` | `frontend/src/fleet/useDriverViolations.ts` | Driver Compliance | Violation server state | Current path pending Driver target decision | No | No | No | Protected |
| `DriverViolationsSection.tsx` | `frontend/src/fleet/DriverViolationsSection.tsx` | Driver Compliance | Violation and fine workflows | Current path pending Driver target decision | No | No | No | Protected |
| `DriverViolationsSection.test.tsx` | `frontend/src/fleet/DriverViolationsSection.test.tsx` | Driver Compliance | Violation UI tests | Current path | No | No | No | Protected |

## Vehicle Master and Shared Integration Files

| Current File | Current Path | Feature | Current Responsibility | Target Path | Move? | Rename? | Shared? | Risk |
|---|---|---|---|---|---|---|---|---|
| `ResourceListPage.tsx` | `frontend/src/pages/ResourceListPage.tsx` | Vehicle Master + multiple domains | Vehicle list, details, CRUD configuration mixed with Driver, Route, Identity, Category and Type resources | Vehicle responsibilities split into `features/fleet/vehicleMaster/`; generic page remains | Split | No | Yes | High: route, RBAC and details composition |
| `ResourceEditorModal.tsx` | `frontend/src/pages/ResourceEditorModal.tsx` | Vehicle Master + multiple domains | Dynamic form plus Vehicle-specific backend error mapping | Vehicle form/error mapping moves to `features/fleet/vehicleMaster/components/` and `validation/` | Split | No | Yes | High: error/field mapping |
| `StatusTags.tsx` | `frontend/src/components/status/StatusTags.tsx` | Shared presentation | Central Vehicle and cross-domain status tags | Remains shared | No | No | Yes | Low |
| `StatusTags.test.tsx` | `frontend/src/components/status/StatusTags.test.tsx` | Shared presentation | Status presentation regression | Remains shared | No | No | Yes | Low |
| `App.tsx` | `frontend/src/App.tsx` | Routing | Registers `/fleet/vehicles` against generic resource page | Same path, Vehicle-owned page component | Modify | No | Yes | High: deep-link compatibility |
| `navigation.tsx` | `frontend/src/navigation/navigation.tsx` | Navigation/RBAC | Central flat Fleet child metadata and recursive permission filtering | Same centralized file with nested Fleet Management metadata | Modify | No | Yes | High: all navigation/RBAC |
| `AppLayout.tsx` | `frontend/src/layout/AppLayout.tsx` | Global Layout | Sidebar, title, breadcrumb and parent lookup | Same file; recursive ancestry lookup | Modify | No | Yes | Medium: nested menu context |
| `AppLayout.test.tsx` | `frontend/src/layout/AppLayout.test.tsx` | Global Layout | Navigation, single title, RBAC and Vehicle details regressions | Same file | Modify | No | Yes | Medium |
| `FleetVehiclesPage.ts` | `frontend/e2e/pages/FleetVehiclesPage.ts` | Vehicle Master E2E | Vehicle page object | Same E2E path | Modify only for accessible selectors | No | Test shared | Medium |
| `vehicles.spec.ts` | `frontend/e2e/tests/fleet/vehicles.spec.ts` | Vehicle Master E2E | Create, validation, duplicate and details scenarios | Same E2E path | Modify | No | No | Medium |
| `fleetToTripJourney.spec.ts` | `frontend/e2e/tests/e2e-journeys/fleetToTripJourney.spec.ts` | Cross-feature | Vehicle creation journey | Same path | No unless selector repair needed | No | Yes | High |
| `app.smoke.spec.ts` | `frontend/e2e/tests/smoke/app.smoke.spec.ts` | Cross-feature | Fleet smoke workflow | Same path | No unless selector repair needed | No | Yes | Medium |
| `navigation.spec.ts` | `frontend/e2e/tests/navigation/navigation.spec.ts` | Navigation | Fleet route/navigation visibility | Same path | Modify for nested navigation assertion | No | Yes | Medium |
| `offlineSyncTestApi.ts` | `frontend/e2e/helpers/offlineSyncTestApi.ts` | Running Log integration | Opens Vehicle detail through `vehicleId` query parameter | Same path; behavior preserved | No | No | Yes | High: US-71 |

## Current Feature Ownership

| Target Feature | Current Implementation | This Run |
|---|---|---|
| `vehicleMaster` | Embedded in generic Resource pages, shared status presentation, App route and E2E page object | Extract |
| `category` | `resourcePages.categories` and `resourcePages.types` | Preserve/defer to `FLEET-FE-002` |
| `document` | Embedded inside Vehicle details via `/vehicles/{id}/documents` | Preserve/defer to `FLEET-FE-003` |
| `allocation` | Vehicle eligibility/assignment UX primarily owned by Trip assignment drawers | Preserve/defer to `FLEET-FE-004`; do not duplicate Trip orchestration |
| `fuelLubricant` | Vehicle lubricant section plus separate authoritative Fuel module | Preserve/defer to `FLEET-FE-005` |
| `runningLog` | Vehicle reading, mileage, meter reset and offline UX under `src/fleet` | Preserve/defer to `FLEET-FE-006` |
| `maintenance` | Vehicle maintenance section and hooks under `src/fleet` | Preserve/defer to `FLEET-FE-007` |

## Vehicle Master Target

```text
frontend/src/features/fleet/vehicleMaster/
├── api/
├── hooks/
├── components/
├── pages/
├── types/
├── validation/
└── utils/                 # only when a real Vehicle-owned utility is required
```

The existing `/fleet/vehicles` route remains authoritative. Company and rental/leased fleets remain one Vehicle list filtered by ownership. Operational status and active lifecycle state remain separate. No QR action, retired enum, dashboard, standalone document list, allocation calendar, or unsupported status transition is invented.
