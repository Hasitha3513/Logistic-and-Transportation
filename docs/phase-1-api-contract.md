# Phase 1 API Contract

This document freezes the implemented Phase 1 HTTP surface as of migration V10. Paths are relative to the `/api` servlet context. JSON uses ISO-8601 strings for `LocalDate` and `OffsetDateTime`, UUID strings for identifiers, and the shared `ApiError` shape for errors. Current feature-branch OpenAPI also contains post-Phase-1 US-31/V11 fuel endpoints; they are intentionally excluded from this Phase 1 contract.

## Common behavior

- Protected requests use `Authorization: Bearer <access-token>`.
- Unauthenticated protected calls return 401; authenticated calls without the listed authority return 403.
- Validation/business errors return `ApiError { timestamp, status, error, code, message, path, correlationId, fieldErrors[] }`.
- Collection endpoints currently return arrays unless the response column explicitly states otherwise.
- DELETE operations on fleet masters deactivate records; compliance record deletes are audited soft deletes.

## System and authentication

| Method | Endpoint | Permission | Request DTO | Response DTO | Business purpose |
|---|---|---|---|---|---|
| GET | `/health` | Public | — | `{status,timestamp}` | Application health smoke check. |
| POST | `/auth/login` | Public | `LoginRequest {username,password}` | `AuthResponse {accessToken,refreshToken,tokenType,expiresIn}` | Authenticate an active user. |
| POST | `/auth/refresh` | Public | `RefreshTokenRequest {refreshToken}` | `AuthResponse` | Rotate a valid refresh token and issue a new pair. |
| POST | `/auth/logout` | Authenticated | `RefreshTokenRequest` | `MessageResponse` | Revoke the supplied refresh token. |
| GET | `/auth/me` | Authenticated | — | `UserResponse` | Return current actor, roles, and permissions. |

## Identity administration

All identity-administration endpoints require `IDENTITY_MANAGE`.

| Method | Endpoint | Request DTO | Response DTO | Business purpose |
|---|---|---|---|---|
| GET | `/users` | — | `UserResponse[]` | List users without password hashes. |
| POST | `/users` | `UserRequest {username,email,password,firstName,lastName,phone,active,roleIds[]}` | `UserResponse` (201) | Create user and role assignments. |
| GET | `/users/{id}` | — | `UserResponse` | Read a user. |
| PUT | `/users/{id}` | `UserRequest` | `UserResponse` | Update profile, optional password, state, and roles. |
| DELETE | `/users/{id}` | — | `MessageResponse` | Disable a user. |
| GET | `/roles` | — | `Role[]` | List roles and permissions. |
| POST | `/roles` | `RoleRequest {name,description,active,permissions[]}` | `Role` (201) | Create a role. |
| GET | `/roles/{id}` | — | `Role` | Read a role. |
| PUT | `/roles/{id}` | `RoleRequest` | `Role` | Update role and permissions. |
| DELETE | `/roles/{id}` | — | `MessageResponse` | Delete/deactivate a role according to identity rules. |

## Organization reference data

These existing reference endpoints require authentication; they do not currently have separate Phase 1 business authorities.

| Method | Endpoint | Request DTO | Response DTO | Business purpose |
|---|---|---|---|---|
| GET/POST | `/customers` | POST `CustomerRequest {code,name,contactPerson,phone,email,active}` | `Customer[]` / `Customer` (201) | List/create customers. |
| GET/PUT/DELETE | `/customers/{id}` | PUT `CustomerRequest` | `Customer` / `MessageResponse` | Read/update/deactivate customer. |
| GET/POST | `/departments` | POST `DepartmentRequest {code,name,description,active}` | `Department[]` / `Department` (201) | List/create departments. |
| GET/POST | `/locations` | POST `LocationRequest {code,name,address,latitude,longitude,active}` | `Location[]` / `Location` (201) | List/create locations. |
| GET/PUT/DELETE | `/locations/{id}` | PUT `LocationRequest` | `Location` / `MessageResponse` | Read/update/deactivate location. |
| GET/POST | `/projects` | POST `ProjectRequest {code,name,departmentId,active}` | `Project[]` / `Project` (201) | List/create projects. |

## Fleet masters and compliance

| Method | Endpoint | Permission | Request DTO | Response DTO | Business purpose |
|---|---|---|---|---|---|
| GET | `/vehicles` | `VEHICLE_VIEW` | — | `Vehicle[]` | List vehicles. |
| POST | `/vehicles` | `VEHICLE_CREATE` | `VehicleRequest` | `Vehicle` (201) | Create vehicle master. |
| GET | `/vehicles/{id}` | `VEHICLE_VIEW` | — | `Vehicle` | Read vehicle. |
| PUT | `/vehicles/{id}` | `VEHICLE_UPDATE` | `VehicleRequest` | `Vehicle` | Update vehicle. |
| DELETE | `/vehicles/{id}` | `VEHICLE_STATUS_UPDATE` | — | `MessageResponse` | Deactivate vehicle. |
| GET | `/vehicles/{id}/availability` | `VEHICLE_AVAILABILITY_VIEW` | Query `from,to,requiredVehicleTypeId?,requiredCapacityKg?,excludeTripId?` | `VehicleAvailability` | Explain eligibility for a period. |
| GET | `/vehicles/available` | `VEHICLE_AVAILABILITY_VIEW` | Query `from,to,vehicleTypeId?,categoryId?,minimumCapacityKg?,excludeTripId?` | `Vehicle[]` | List eligible vehicles. |
| GET | `/vehicles/{vehicleId}/documents` | `VEHICLE_VIEW` | — | `VehicleDocument[]` | List visible documents. |
| POST | `/vehicles/{vehicleId}/documents` | `VEHICLE_DOCUMENT_MANAGE` | `DocumentRequest` | `VehicleDocument` (201) | Add document. |
| PATCH | `/vehicles/{vehicleId}/documents/{documentId}` | `VEHICLE_DOCUMENT_MANAGE` | `DocumentPatchRequest` | `VehicleDocument` | Partially update document. |
| DELETE | `/vehicles/{vehicleId}/documents/{documentId}` | `VEHICLE_DOCUMENT_MANAGE` | — | 204 | Soft-delete document while retaining audit. |
| GET | `/vehicle-categories` | `VEHICLE_VIEW` | — | `VehicleCategory[]` | List categories. |
| POST | `/vehicle-categories` | `VEHICLE_CREATE` | `CategoryRequest` | `VehicleCategory` (201) | Create category. |
| GET/PUT/DELETE | `/vehicle-categories/{id}` | `VEHICLE_VIEW` / `VEHICLE_UPDATE` / `VEHICLE_STATUS_UPDATE` | PUT `CategoryRequest` | `VehicleCategory` / `MessageResponse` | Read/update/deactivate category. |
| GET | `/vehicle-types` | `VEHICLE_VIEW` | — | `VehicleType[]` | List types. |
| POST | `/vehicle-types` | `VEHICLE_CREATE` | `TypeRequest` | `VehicleType` (201) | Create type. |
| GET/PUT/DELETE | `/vehicle-types/{id}` | `VEHICLE_VIEW` / `VEHICLE_UPDATE` / `VEHICLE_STATUS_UPDATE` | PUT `TypeRequest` | `VehicleType` / `MessageResponse` | Read/update/deactivate type. |
| GET | `/drivers` | `DRIVER_VIEW` | — | `Driver[]` | List drivers. |
| POST | `/drivers` | `DRIVER_CREATE` | `DriverRequest` | `Driver` (201) | Create driver. |
| GET/PUT/DELETE | `/drivers/{id}` | `DRIVER_VIEW` / `DRIVER_UPDATE` / `DRIVER_UPDATE` | PUT `DriverRequest` | `Driver` / `MessageResponse` | Read/update/deactivate driver. |
| GET | `/drivers/{id}/availability` | `DRIVER_AVAILABILITY_VIEW` | Query `from,to,requiredLicenseClass?,excludeTripId?` | `DriverAvailability` | Explain driver eligibility. |
| GET | `/drivers/available` | `DRIVER_AVAILABILITY_VIEW` | Query `from,to,requiredLicenseClass?` | `Driver[]` | List eligible drivers. |
| GET | `/drivers/{driverId}/licenses` | `DRIVER_VIEW` | — | `DriverLicense[]` | List licences. |
| POST | `/drivers/{driverId}/licenses` | `DRIVER_LICENSE_MANAGE` | `DriverLicenseRequest` | `DriverLicense` (201) | Add licence. |
| PATCH | `/drivers/{driverId}/licenses/{licenseId}` | `DRIVER_LICENSE_MANAGE` | `DriverLicensePatchRequest` | `DriverLicense` | Update licence. |
| DELETE | `/drivers/{driverId}/licenses/{licenseId}` | `DRIVER_LICENSE_MANAGE` | — | 204 | Soft-delete licence. |

`VehicleRequest` contains registration/chassis/engine numbers, category/type IDs, manufacturer/model/year, ownership/operational state, odometer, engine hours, capacity, and active state. Document/licence DTOs mirror their persisted Phase 1 fields.

## Routing

| Method | Endpoint | Permission | Request DTO | Response DTO | Business purpose |
|---|---|---|---|---|---|
| GET | `/routes` | `ROUTE_VIEW` | Query `query?,originLocationId?,destinationLocationId?,active?` | `Route[]` | Search/filter routes. |
| POST | `/routes` | `ROUTE_CREATE` | `RouteRequest {code,name,originLocationId,destinationLocationId,plannedDistanceKm,estimatedDurationMinutes,stops[],active}` | `Route` (201) | Create route with ordered stops. |
| GET | `/routes/{id}` | `ROUTE_VIEW` | — | `Route` | Read route. |
| PUT | `/routes/{id}` | `ROUTE_UPDATE` | `RouteRequest` | `Route` | Update route. |
| DELETE | `/routes/{id}` | `ROUTE_UPDATE` | — | `MessageResponse` | Deactivate route. |

## Trips

| Method | Endpoint | Permission | Request DTO | Response DTO | Business purpose |
|---|---|---|---|---|---|
| GET | `/trips` | `TRIP_VIEW` | — | `Trip[]` | List trips; no server pagination is implemented. |
| POST | `/trips` | `TRIP_CREATE` | `TripRequest` | `Trip` (201) | Create DRAFT trip. |
| GET | `/trips/{id}` | `TRIP_VIEW` | — | `Trip` | Read trip. |
| PUT | `/trips/{id}` | `TRIP_UPDATE` | `TripRequest` | `Trip` | Update permitted order fields. |
| POST | `/trips/{id}/submit` | `TRIP_SUBMIT` | — | `Trip` | DRAFT/REJECTED → SUBMITTED. |
| POST | `/trips/{id}/approve` | `TRIP_APPROVE` | Optional `{}` | `Trip` | SUBMITTED → APPROVED. |
| POST | `/trips/{id}/reject` | `TRIP_REJECT` | `ReasonRequest {reason}` | `Trip` | SUBMITTED → REJECTED. |
| POST | `/trips/{id}/assign-vehicle` | `TRIP_ASSIGN_VEHICLE` | `{vehicleId}` | `Trip` | Validate and assign vehicle. |
| POST | `/trips/{id}/unassign-vehicle` | `TRIP_ASSIGN_VEHICLE` | — | `Trip` | Remove pre-dispatch vehicle assignment. |
| POST | `/trips/{id}/assign-driver` | `TRIP_ASSIGN_DRIVER` | `{driverId,requiredLicenseClass}` | `Trip` | Validate and assign driver. |
| POST | `/trips/{id}/unassign-driver` | `TRIP_ASSIGN_DRIVER` | — | `Trip` | Remove pre-dispatch driver assignment. |
| POST | `/trips/{id}/assign-route` | `TRIP_ASSIGN_ROUTE` | `{routeId}` | `Trip` | Assign active endpoint-compatible route and audit action. |
| POST | `/trips/{id}/dispatch` | `TRIP_DISPATCH` | `DispatchRequest {remarks?}` | `Trip` | Revalidate both resources and dispatch. |
| POST | `/trips/{id}/start` | `TRIP_START` | `StartRequest {startOdometerKm}` | `Trip` | DISPATCHED → IN_PROGRESS. |
| POST | `/trips/{id}/complete` | `TRIP_COMPLETE` | `CompleteRequest {endOdometerKm,completionRemarks?}` | `Trip` | IN_PROGRESS → COMPLETED. |
| POST | `/trips/{id}/close` | `TRIP_CLOSE` | — | `Trip` | COMPLETED → CLOSED. |
| POST | `/trips/{id}/cancel` | `TRIP_CANCEL` | `ReasonRequest {reason}` | `Trip` | Cancel an allowed pre-completion state. |
| GET | `/trips/{id}/status-history` | `TRIP_VIEW` | — | `TripHistoryEntry[]` | Read ordered lifecycle/assignment audit. |

`TripRequest` contains customer/department/project/route references, priority, origin/destination, requested period, required vehicle type/capacity, cargo/passenger details, instructions, and notes. The backend owns IDs, trip numbers, lifecycle state, and audit timestamps.

## Reporting

| Method | Endpoint | Permission | Request DTO | Response DTO | Business purpose |
|---|---|---|---|---|---|
| GET | `/dashboard/operations` | `DASHBOARD_VIEW` | Query `date?` | Map `{date,status,...}` | Basic operations dashboard response. |
| GET | `/reports/trips` | `REPORT_VIEW` | Query `fromDate,toDate,page?,limit?,status?,customerId?` | Paged-shaped map with empty placeholder content | Basic trip report contract. |
| GET | `/reports/driver-assignments` | `REPORT_VIEW` | Query `fromDate,toDate,driverId?` | Map array | Driver assignment report contract. |
| GET | `/reports/vehicle-utilization` | `REPORT_VIEW` | Query `fromDate,toDate,vehicleId?` | Map array | Vehicle utilization report contract. |

Reporting aggregation is a known Phase 1 limitation; clients must not infer unavailable metrics.
