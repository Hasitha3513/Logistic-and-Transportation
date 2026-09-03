# Product Decisions and Domain Contract: US-63 Manage Delivery Zones
**Task ID:** `MVP-1.4-US63-DELIVERY-ZONES-PRODUCT-DECISIONS-001`  
**Story:** `US-63 — Manage Delivery Zones`  
**Phase:** `MVP 1.4 — Last-Mile Delivery & Customer Experience`  
**Status:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`  
**Authoritative Baseline:** `57 COMPLETE / 30 DEFERRED / 87 TOTAL`  
**Flyway Baseline:** `V51 (Next Expected: V52)`  

---

## 1. Executive Summary & Precondition Verification
Following the formal closure of **MVP 1.3** (`MVP-1.3-US62-DELIVERY-EXCEPTIONS-FINAL-ACCEPTANCE-001.md`) and the completion of the roadmap rebaseline (`MVP-POST-1.3-ROADMAP-REBASELINE-001.md`), **MVP 1.4** is the active development band. `US-63` is the foundational first story of MVP 1.4.

This document freezes all product decisions, domain boundaries, multi-tenant rules, geospatial data models, RBAC permissions, and API interfaces for `US-63` prior to any production implementation.

---

## 2. Authoritative Requirement Extraction

**Source:** `docs/requirements/US-61-US-70-UseCase-Activity-Sequence-Diagrams.md` (Lines 182–266)  
**Primary Actor:** `Last-Mile Planner`  
**Business Goal:** Create dynamic delivery zones, manage capacity, overrides, and micro-hubs so last-mile coverage can adapt to demand.

### Canonical Use Cases
1. `Create Dynamic Delivery Zone`: Define zone geometry, code, name, operational type, and serviceability.
2. `Manage Zone Capacity`: Configure standard daily parcel limits / throughput constraints (consumed in US-64/US-66).
3. `Associate Micro-Hub`: Link zone to operational depot/branch master data location (`depotLocationId`).
4. `Override Zone Assignment`: Allow authorized dispatchers/managers to reassign a delivery order across zones.
5. `Validate Zone Geometry`: Ensure valid bounding polygon with closed vertices and longitude/latitude coordinate ranges.
6. `View Zone Coverage`: Render tabular and spatial boundary coverage with active service status.

---

## 3. Core Product & Domain Architecture Decisions

### 3.1 Definition of "Delivery Zone"
A **Delivery Zone** in MVP 1.4 is a **named, tenant-owned operational service territory defined by a bounding polygon geometry**, linked to an optional originating dispatch micro-hub/depot location, with defined active serviceability and parcel handling capacity.

### 3.2 Domain Ownership & Modulith Boundary
- **Owning Bounded Context:** `delivery` module (`com.transportlogistics.app.delivery`).
- **Sub-Package:** `delivery.domain.model` (alongside `DeliveryOrder`, `ProofOfDelivery`, `DeliveryExceptionCase`).
- **Routing Boundary:** `routing` module provides route and waypoint heuristics. It does **not** own delivery zones.
- **Organization / Master Data Boundary:** Locations (`destinationLocationId`, `depotLocationId`) remain owned by `masterdata`/`organization`. The `delivery` module references them as logical UUIDs validated via `DeliveryLocationLookupPort`.

### 3.3 Geospatial Geometry Model & PostGIS Strategy
- **PostGIS Decision:** `POSTGIS_NOT_REQUIRED` for MVP 1.4.
- **Rationale:** The application operates in standard PostgreSQL 16 without custom C-extension dependencies. Introducing PostGIS would break container portability, CI/CD lightweight PostgreSQL runners, and testcontainers.
- **MVP Spatial Representation:**
  - Standard JSONB column `boundary_geojson` storing standard RFC 7946 GeoJSON `Polygon` coordinates.
  - Coordinate containment algorithm implemented in pure Java domain service (Ray-Casting Algorithm / Point-in-Polygon) with standard EPSG:4326 (WGS 84) coordinate validation.
  - Optional bounding box columns (`min_lat`, `max_lat`, `min_lon`, `max_lon`) indexed via composite B-Tree indexes for fast pre-filtering before polygon ray-casting.

### 3.4 Zone Aggregate Root (`DeliveryZone`)
```java
public class DeliveryZone {
    private final UUID id;
    private final UUID tenantId;
    private String zoneCode;               // e.g. "ZONE-COL-01" (Upper case, alphanumeric + dash)
    private String zoneName;               // e.g. "Colombo Central Commercial"
    private String description;
    private DeliveryZoneType zoneType;     // URBAN_DENSE, SUBURBAN, RURAL, SPECIAL_SECURITY
    private DeliveryZoneStatus status;     // ACTIVE, INACTIVE
    private boolean serviceable;          // true = accepts new delivery orders
    private Integer dailyCapacity;         // max deliveries per day (null = unlimited)
    private UUID depotLocationId;          // Optional micro-hub location
    private ZoneBoundary boundary;         // GeoJSON Polygon value object
    private int priority;                  // Higher number = higher priority for overlaps (default: 0)
    private Long version;
    private OffsetDateTime createdAt;
    private String createdBy;
    private OffsetDateTime updatedAt;
    private String updatedBy;
}
```

### 3.5 Zone Identifier & Tenant Uniqueness
- **Primary Identifier:** `UUID id` (Primary Key).
- **Human-Readable Code:** `zoneCode` VARCHAR(30) NOT NULL.
- **Uniqueness Constraint:** `UNIQUE(tenant_id, zone_code)` (case-insensitive normalized upper-case).

### 3.6 Overlapping Zones & Priority Resolution Policy
- **Policy:** `OVERLAP_ALLOWED_WITH_PRIORITY`.
- **Deterministic Resolution:**
  1. When resolving a point, find all `ACTIVE` and `serviceable = true` zones whose polygon contains the coordinates.
  2. If multiple zones contain the point, select the zone with the **highest `priority` value**.
  3. If priority is tied, select the zone with the **smallest bounding polygon area** (most localized micro-zone).
  4. If area is identical, select the most recently updated zone (`updated_at DESC`).

### 3.7 Location Membership & Point-in-Polygon Resolution
- **Resolution Strategy:** Computed dynamically from destination `location.latitude` and `location.longitude`.
- **Locations Without Coordinates:** Returns `DELIVERY_ZONE_LOCATION_UNRESOLVED`. Delivery order can proceed with unassigned zone or manual dispatcher override.
- **Boundary Points:** A coordinate located exactly on the outer boundary segment is considered `INSIDE` (inclusive epsilon $10^{-7}$).

### 3.8 Delivery Order Integration & Historical Auditing
- **Delivery Order Snapshot:** `delivery_order` table will gain an optional logical reference `delivery_zone_id UUID`.
- **Immutability of Historical Delivery Facts:**
  - Zone resolution occurs during delivery order creation and redelivery scheduling.
  - Modifying or deactivating a `DeliveryZone` does **not** retroactively mutate historical `delivery_order.delivery_zone_id` snapshots on completed/delivered orders.
- **US-62 Wrong-Address Integration:** When an exception records a `correctedLocationId`, the redelivery scheduling workflow (`US-60`) re-resolves the `DeliveryZone` for the corrected coordinates.

---

## 4. Downstream Dependency Contracts (MVP 1.4 Preparation)

| Story ID | Consuming Feature | US-63 Provided Contract |
| :---: | :--- | :--- |
| **`US-64`** | Manage Delivery Slots | Consumes `DeliveryZone.id`, `status`, `serviceable`, and `dailyCapacity` to partition time-slot booking quotas per zone. |
| **`US-65`** | Manage Riders | Consumes `DeliveryZone.id` to assign rider primary operating coverage zones. |
| **`US-66`** | Batch Delivery Orders | Consumes `delivery_zone_id` on `DeliveryOrder` to cluster orders into discrete zone batches. |
| **`US-67`** | Calculate Last-Mile ETA | Consumes `ZoneBoundary` and zone classification (`URBAN_DENSE` vs `RURAL`) for parking/transit speed modifiers. |
| **`US-68`** | Last-Mile Exceptions | Consumes zone context for building/gate-access exception categorization. |
| **`US-69` / `US-70`** | Notifications & Customer Portal | Public boundary: customer portal queries serviceability without revealing internal zone topology or driver rosters. |
| **`US-49`** | GPS Geofences Boundary | **Strictly Separated:** US-49 owns real-time IoT telematics tracking triggers; US-63 owns operational delivery business zones. |

---

## 5. Security, RBAC & Multi-Tenancy Contract

### 5.1 Permissions Matrix
- `DELIVERY_ZONE_CREATE`: `TENANT_ADMIN`, `OPERATIONS_MANAGER`, `DISPATCHER`
- `DELIVERY_ZONE_VIEW`: `TENANT_ADMIN`, `OPERATIONS_MANAGER`, `DISPATCHER`, `VIEWER`
- `DELIVERY_ZONE_UPDATE`: `TENANT_ADMIN`, `OPERATIONS_MANAGER`, `DISPATCHER`
- `DELIVERY_ZONE_ACTIVATE`: `TENANT_ADMIN`, `OPERATIONS_MANAGER` (`DISPATCHER` denied)
- `DELIVERY_ZONE_OVERRIDE`: `TENANT_ADMIN`, `OPERATIONS_MANAGER`, `DISPATCHER`

### 5.2 Multi-Tenant Isolation
- All zone operations validate `tenant_id` from `TenantExecutionContext`.
- Cross-tenant zone access returns safe HTTP `404 Not Found`.
- Cross-tenant micro-hub `depotLocationId` validation returns safe HTTP `404 Not Found`.

---

## 6. REST API Contract

### Endpoints
```http
POST   /api/v1/delivery-zones                      # Create delivery zone
GET    /api/v1/delivery-zones                      # List delivery zones (paginated, filtered)
GET    /api/v1/delivery-zones/{id}                 # Get delivery zone details with GeoJSON
PUT    /api/v1/delivery-zones/{id}                 # Update delivery zone metadata/boundary
POST   /api/v1/delivery-zones/{id}/activate        # Activate delivery zone
POST   /api/v1/delivery-zones/{id}/deactivate      # Deactivate delivery zone
POST   /api/v1/delivery-zones/resolve              # Resolve zone for given coordinates or locationId
```

### Request Payload (`POST /api/v1/delivery-zones`)
```json
{
  "zoneCode": "ZONE-COL-01",
  "zoneName": "Colombo Central Commercial",
  "description": "High density business district deliveries",
  "zoneType": "URBAN_DENSE",
  "serviceable": true,
  "dailyCapacity": 250,
  "depotLocationId": "22222222-2222-2222-2222-222222222221",
  "priority": 10,
  "boundary": {
    "type": "Polygon",
    "coordinates": [
      [
        [79.8450, 6.9271],
        [79.8600, 6.9271],
        [79.8600, 6.9400],
        [79.8450, 6.9400],
        [79.8450, 6.9271]
      ]
    ]
  }
}
```

---

## 7. Database Schema Expectation (Flyway V52)

```sql
CREATE TABLE delivery_zone (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    zone_code VARCHAR(30) NOT NULL,
    zone_name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    zone_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    serviceable BOOLEAN NOT NULL DEFAULT TRUE,
    daily_capacity INT,
    depot_location_id UUID,
    min_latitude DOUBLE PRECISION NOT NULL,
    max_latitude DOUBLE PRECISION NOT NULL,
    min_longitude DOUBLE PRECISION NOT NULL,
    max_longitude DOUBLE PRECISION NOT NULL,
    boundary_geojson JSONB NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(80) NOT NULL,
    CONSTRAINT uk_delivery_zone_code UNIQUE (tenant_id, zone_code),
    CONSTRAINT ck_delivery_zone_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_delivery_zone_type CHECK (zone_type IN ('URBAN_DENSE', 'SUBURBAN', 'RURAL', 'SPECIAL_SECURITY'))
);

CREATE INDEX idx_delivery_zone_tenant_status ON delivery_zone(tenant_id, status);
CREATE INDEX idx_delivery_zone_bbox ON delivery_zone(tenant_id, min_latitude, max_latitude, min_longitude, max_longitude);
```

---

## 8. Frontend UX & Visualization Contract
- **Navigation:** Embedded under `Delivery Operations` $\rightarrow$ `Delivery Zones`.
- **List Page:** Ant Design table showing Zone Code, Name, Type, Status Tag, Serviceable Switch, Capacity, Priority, and Quick Actions.
- **Detail / Map Drawer:** Leaflet / OpenStreetMap lightweight polygon renderer with GeoJSON boundary coordinate preview and vertex summary.
- **Create / Edit Form:** Standard Ant Design form with coordinate input helper (GeoJSON paste + interactive polygon map preview without heavy external GIS dependencies).

---

## 9. Verification & Acceptance Matrix

- `VM63-01`: Create valid delivery zone with GeoJSON polygon and bounding box computation.
- `VM63-02`: Reject duplicate `zoneCode` within the same tenant (`409 Conflict`).
- `VM63-03`: Allow identical `zoneCode` across different tenants (multi-tenant isolation).
- `VM63-04`: Resolve coordinates inside polygon to correct zone.
- `VM63-05`: Overlapping polygon resolution selects highest priority zone.
- `VM63-06`: Coordinate outside all zones returns `ZONE_UNRESOLVED`.
- `VM63-07`: Deactivated zone is excluded from dynamic resolution.
- `VM63-08`: Cross-tenant zone access returns safe `404 Not Found`.
- `VM63-09`: Optimistic locking detects concurrent modifications (`409 Conflict`).
- `VM63-10`: Spring Security RBAC enforces `VIEW`, `CREATE`, `UPDATE`, `ACTIVATE` authorities.

---

## 10. Implementation Readiness Gate

All 34 implementation-critical product decisions are frozen. Production implementation may proceed in the next task:
👉 **`MVP-1.4-US63-DELIVERY-ZONES-IMPLEMENTATION-001`**
