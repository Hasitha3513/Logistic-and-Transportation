# P2-01: Route Revision History and Route Disruption Management

## 1. Overview & Context

- **Task ID**: `P2-01`
- **Phase**: Phase-2 — Route Intelligence & Commercial Freight Operations
- **User Stories Delivered**:
  - `US-21`: Maintain Route History (immutable version snapshots on creation, update, and deactivation)
  - `US-23`: Handle Route Disruptions (disruption lifecycle, detour assignment, active alert banners, resolution workflow)
- **Module Ownership**: `routing` (`com.transportlogistics.app.routing`)
- **Architecture**: Spring Modulith + Feature-First Hexagonal Architecture (`domain <- ports/application <- adapters`)

---

## 2. Backend Architecture & Domain Model

### 2.1 Pure Domain (`com.transportlogistics.app.routing.domain.model`)
- `RouteRevision`: Immutable snapshot of a route definition at a specific point in time (`id`, `routeId`, `revisionNumber`, `code`, `name`, `originLocationId`, `destinationLocationId`, `plannedDistanceKm`, `estimatedDurationMinutes`, `active`, `stopLocationIds`, `changedAt`, `changedBy`).
- `RouteDisruption`: Aggregate record for disruptions (`id`, `routeId`, `disruptionType`, `severity`, `description`, `effectiveFrom`, `effectiveUntil`, `detourRouteId`, `status`, `createdAt`, `createdBy`, `resolvedAt`, `resolvedBy`).
- `RouteDisruptionType`: `ROAD_CLOSURE`, `ACCIDENT`, `WEATHER`, `RESTRICTION`.
- `DisruptionSeverity`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
- `DisruptionStatus`: `ACTIVE`, `RESOLVED`.
- `RouteDisruptionCreatedEvent` & `RouteDisruptionResolvedEvent`: Provider-neutral domain events published when disruptions occur or resolve.

### 2.2 Application Ports & Service (`com.transportlogistics.app.routing.application`)
- Inbound Port: `RouteUseCase`
  - `List<RouteRevision> getRevisions(UUID routeId)`
  - `RouteRevision getRevision(UUID routeId, int revisionNumber)`
  - `RouteDisruption createDisruption(UUID routeId, ...)`
  - `RouteDisruption resolveDisruption(UUID routeId, UUID disruptionId, String actor)`
  - `List<RouteDisruption> getDisruptions(UUID routeId)`
  - `List<RouteDisruption> getActiveDisruptions()`
- Outbound Ports:
  - `RouteRevisionRepository`
  - `RouteDisruptionRepository`
  - `RouteEventPublisher`
- Application Service: `RouteService` orchestrates automatic sequential revision creation (v1 on creation, vN+1 on update or deactivation), disruption lifecycle validation (effective date validation, active detour validation), and event publishing.

### 2.3 Persistence Adapters (`com.transportlogistics.app.routing.infrastructure.adapters.out.persistence`)
- `RouteRevisionEntity` & `RouteRevisionJpaRepository`: JPA mapping for `route_revision` and collection table `route_revision_stop`.
- `RouteRevisionPersistenceAdapter`: Implements `RouteRevisionRepository`.
- `RouteDisruptionEntity` & `RouteDisruptionJpaRepository`: JPA mapping for `route_disruption`.
- `RouteDisruptionPersistenceAdapter`: Implements `RouteDisruptionRepository`.
- `SpringRouteEventPublisherAdapter`: Bridges application event publisher to Spring `ApplicationEventPublisher`.

### 2.4 Web Layer (`com.transportlogistics.app.routing.infrastructure.adapters.in.web`)
- Controller: `RouteController`
  - `GET /api/v1/routes/{id}/revisions` (Authority: `ROUTE_VIEW`)
  - `GET /api/v1/routes/{id}/revisions/{revisionNumber}` (Authority: `ROUTE_VIEW`)
  - `GET /api/v1/routes/{id}/disruptions` (Authority: `ROUTE_VIEW`)
  - `GET /api/v1/routes/disruptions/active` (Authority: `ROUTE_VIEW`)
  - `POST /api/v1/routes/{id}/disruptions` (Authority: `ROUTE_DISRUPTION_MANAGE`)
  - `POST /api/v1/routes/{id}/disruptions/{disruptionId}/resolve` (Authority: `ROUTE_DISRUPTION_MANAGE`)

---

## 3. Database Migration (`V30__route_history_and_disruptions.sql`)

- Implements table `route_revision` with foreign key cascade to `route`, positive revision constraint, and unique constraint `(route_id, revision_number)`.
- Implements table `route_revision_stop` with composite primary key `(route_revision_id, stop_order)`.
- Implements table `route_disruption` with foreign keys to `route` (disrupted route and detour route), check constraints for time window, status, severity, and type.
- Seeds `ROUTE_DISRUPTION_MANAGE` permission into `app_permission`.

---

## 4. Frontend Implementation (`frontend/src/features/routing`)

- **Types**: `types/route.ts` with complete type definitions.
- **API Client**: `api/routeApi.ts` using shared Axios instance.
- **Hooks**: `hooks/useRouteHistoryAndDisruptions.ts` TanStack Query hooks with targeted query invalidations.
- **Components**:
  - `RouteRevisionSection.tsx`: Timeline/cards displaying route revisions, version badges (`v1`, `v2`), and full parameter diffs.
  - `RouteDisruptionsSection.tsx`: List of active and resolved disruptions with severity tags, detour links, report modal, and resolve actions.
  - `RouteDisruptionModal.tsx`: Form for creating disruptions with validation and active detour route selector.
  - `ActiveDisruptionsBanner.tsx`: Global network disruption alert banner on the route library.
- **Integration**: Mounted inside `ResourceListPage.tsx` route view and route details drawer.

---

## 5. Verification Results

| Test Suite | Total Tests | Result | Notes |
|---|---|---|---|
| **Backend Unit & Integration Tests** | 735 | **PASS (0 failures, 0 errors)** | 17 new tests added for revisions, disruptions, events, persistence |
| **Spring Modulith & Architecture Tests** | 22 | **PASS (0 failures)** | Layer rules, module boundaries, Lombok rules verified |
| **Frontend Unit Tests (Vitest)** | 184 | **PASS (0 failures)** | 6 new tests added for revision section, disruption section, active banner |
| **Frontend Production Build & Lint** | — | **PASS (0 errors, 0 warnings)** | `npm run lint` & `npm run build` |
| **Playwright Cross-Browser E2E Suite** | 219 (73 x 3) | **PASS (0 failures)** | Chromium (73/73), Firefox (73/73), WebKit (73/73) |
