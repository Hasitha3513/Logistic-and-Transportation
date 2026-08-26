# US33-A: Vehicle Reading Web API & Permissions Foundation

## Scope

This task delivers the HTTP Web adapter and Security permissions foundation for the Fleet module's `VehicleReading` capability.

### In Scope
- REST controller [`VehicleReadingController`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/src/main/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/VehicleReadingController.java)
- Manual reading creation endpoint `POST /vehicles/{vehicleId}/readings`
- Filtered, paginated reading history query `GET /vehicles/{vehicleId}/readings`
- Latest readings query `GET /vehicles/{vehicleId}/readings/latest`
- Forward Flyway migration [`V15__vehicle_reading_permissions.sql`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/src/main/resources/db/migration/V15__vehicle_reading_permissions.sql) seeding `VEHICLE_READING_*` permissions
- Registration of reading permissions in [`LocalIdentityBootstrap`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/src/main/java/com/transportlogistics/app/identity/infrastructure/config/LocalIdentityBootstrap.java)
- Controller unit tests, security integration tests, and API integration tests

### Out of Scope (Deferred to subsequent slices)
- Trip Start/Complete integration (`US33-B`)
- Fuel Issue integration (`US33-C`)
- Reading Corrections workflow (`US33-D`)
- Meter Reset / Replacement epoch workflow (`US33-D`)
- Mileage calculation & Trip distance queries (`US33-E`)
- Frontend UI components (`US33-F`)

---

## API Endpoints

Base path: `/` (Spring MVC base with `/api/v1` or root according to repository mapping).

### 1. List Vehicle Readings
- **Method & Path**: `GET /vehicles/{vehicleId}/readings`
- **Required Authority**: `VEHICLE_READING_VIEW`
- **Query Parameters**:
  - `readingType` (optional, enum: `ODOMETER`, `ENGINE_HOURS`)
  - `sourceType` (optional, enum: `MANUAL`, `TRIP_START`, `TRIP_END`, `FUEL_ISSUE`, `BASELINE`, `METER_RESET`, `TELEMATICS`, `MAINTENANCE`)
  - `from` (optional, ISO-8601 `OffsetDateTime`)
  - `to` (optional, ISO-8601 `OffsetDateTime`)
  - `page` (optional, default `0`)
  - `limit` (optional, default `20`)
- **Response**: `200 OK` with JSON `PageResponse<VehicleReadingResponse>`

### 2. Get Latest Vehicle Readings
- **Method & Path**: `GET /vehicles/{vehicleId}/readings/latest`
- **Required Authority**: `VEHICLE_READING_VIEW`
- **Response**: `200 OK` with JSON `LatestVehicleReadingsResponse`

### 3. Record Manual Vehicle Reading
- **Method & Path**: `POST /vehicles/{vehicleId}/readings`
- **Required Authority**: `VEHICLE_READING_CREATE`
- **Request Body**: JSON `RecordManualVehicleReadingRequest`
- **Response**: `201 Created` with JSON `VehicleReadingResponse`
- **Enforcements**:
  - Server sets `sourceType = MANUAL`
  - Server sets `sourceReferenceId = null`
  - Server resolves `actorId` from authenticated JWT principal via `AuthenticatedUserLookup`
  - Server validates non-negative value, required fields, and <= 5 minutes future timestamp ceiling

---

## Request / Response DTOs

### `RecordManualVehicleReadingRequest`
```json
{
  "readingType": "ODOMETER",
  "value": 10500.000,
  "recordedAt": "2026-08-16T10:00:00Z",
  "idempotencyKey": "opt-client-key-123",
  "notes": "Regular shift start entry"
}
```

### `VehicleReadingResponse`
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "vehicleId": "7b0848ea-b452-4751-b844-3d120a16bfa8",
  "readingType": "ODOMETER",
  "value": 10500.000,
  "unit": "KILOMETER",
  "meterEpoch": 0,
  "sourceType": "MANUAL",
  "sourceReferenceId": null,
  "recordedAt": "2026-08-16T10:00:00Z",
  "receivedAt": "2026-08-16T10:00:02Z",
  "createdBy": "8b0848ea-b452-4751-b844-3d120a16bfa1",
  "correctionOfReadingId": null,
  "correctionReason": null,
  "idempotencyKey": "opt-client-key-123",
  "notes": "Regular shift start entry",
  "createdAt": "2026-08-16T10:00:02Z"
}
```

### `LatestVehicleReadingsResponse`
```json
{
  "vehicleId": "7b0848ea-b452-4751-b844-3d120a16bfa8",
  "odometer": {
    "readingId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "value": 10500.000,
    "unit": "KILOMETER",
    "meterEpoch": 0,
    "sourceType": "MANUAL",
    "sourceReferenceId": null,
    "recordedAt": "2026-08-16T10:00:00Z",
    "receivedAt": "2026-08-16T10:00:02Z"
  },
  "engineHours": null
}
```

---

## Permissions & Flyway Migration

### Seeded Permissions
- `VEHICLE_READING_VIEW`: View vehicle reading history and latest readings
- `VEHICLE_READING_CREATE`: Record manual vehicle readings
- `VEHICLE_READING_CORRECT`: Submit reading corrections
- `VEHICLE_READING_RESET_METER`: Execute physical meter reset or replacement

### Migration
Flyway migration: [`V15__vehicle_reading_permissions.sql`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/src/main/resources/db/migration/V15__vehicle_reading_permissions.sql)

---

## Security Behavior

| Endpoint | No Authentication | Without Required Permission | With Required Permission |
|---|:---:|:---:|:---:|
| `GET /vehicles/{id}/readings` | 401 Unauthorized | 403 Forbidden | 200 OK |
| `GET /vehicles/{id}/readings/latest` | 401 Unauthorized | 403 Forbidden | 200 OK |
| `POST /vehicles/{id}/readings` | 401 Unauthorized | 403 Forbidden | 201 Created |

---

## Error Handling

| Business Condition | HTTP Status | Error Code |
|---|:---:|:---:|
| Reading decrease below previous latest | 409 Conflict | `VEHICLE_READING_DECREASE` |
| Chronology conflict with chronological neighbors | 409 Conflict | `VEHICLE_READING_CHRONOLOGY_CONFLICT` |
| Duplicate source / conflicting replay | 409 Conflict | `DUPLICATE_VEHICLE_READING` |
| Negative value or missing required fields | 400 Bad Request | `VALIDATION_ERROR` |
| Inactive vehicle or reading in far future (> 5 min) | 422 Unprocessable Entity | `INVALID_VEHICLE_READING` |
| Target vehicle not found | 404 Not Found | `VEHICLE_NOT_FOUND` |

---

## Tests

1. [`VehicleReadingControllerTest`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/src/test/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/VehicleReadingControllerTest.java): 8 unit tests covering paged listing, filters, latest retrieval with present/absent types, manual creation enforcement, validation errors, conflict mapping, and not-found mapping.
2. [`VehicleReadingSecurityIntegrationTest`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/src/test/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/VehicleReadingSecurityIntegrationTest.java): 3 integration tests verifying 401/403/200/201 security behaviors.
3. [`VehicleReadingApiIntegrationTest`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/src/test/java/com/transportlogistics/app/fleet/infrastructure/adapters/in/web/VehicleReadingApiIntegrationTest.java): End-to-end multi-step scenario verifying login, manual reading creation (10,000 km), latest query, subsequent reading (10,100 km), paged search order, and chronology decrease rejection (9,900 km -> 409 Conflict).

---

## Status Update

Overall status: **US-33 PARTIAL**
Next slice: **US33-B (Trip Lifecycle Reading Integration)**
