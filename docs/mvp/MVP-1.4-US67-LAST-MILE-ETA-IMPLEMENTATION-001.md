# MVP-1.4-US67-LAST-MILE-ETA-IMPLEMENTATION-001

## 1. Executive Summary

- **Task ID**: `MVP-1.4-US67-LAST-MILE-ETA-IMPLEMENTATION-001`
- **User Story**: `US-67` — Calculate Last-Mile ETA
- **Status**: `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`
- **Module**: `delivery`
- **Flyway Migrations**: `NO_FLYWAY_MIGRATION_REQUIRED` (Authoritative DB head remains `V55`)
- **Key Architecture Rules Enforced**:
  - Pure Domain & Ports isolation: no Spring, JPA, or external library imports in domain/application/ports packages.
  - Multi-modal ETA calculation: `BICYCLE`, `MOTORBIKE`, `VAN`, `CAR`, `WALKER` with configurable speeds per zone type.
  - Multi-stop batch cumulative ETA with doorstep (5 min) and dense apartment (10 min) service buffers.
  - SLA breach evaluation: `ON_TIME` ($> 15\text{m}$ before window end), `AT_RISK` ($\le 15\text{m}$ before window end), `LATE` ($> \text{window end}$).
  - In-memory cache with payload fingerprinting and 15-minute TTL freshness validation.
  - Decoupled domain event emission via `DeliveryEtaCalculatedEvent`.
  - Full multi-tenancy isolation on all queries and composite cache keys.

---

## 2. Deliverables & Code Changes

### A. Domain Models & Events
1. `DeliveryTransportMode.java`: Enum defining `BICYCLE`, `MOTORBIKE`, `VAN`, `CAR`, `WALKER`.
2. `EtaStatus.java`: Enum defining `ON_TIME`, `AT_RISK`, `LATE`.
3. `EtaSource.java`: Enum defining `HEURISTIC`, `HEURISTIC_FALLBACK`, `EXTERNAL_PROVIDER`.
4. `SingleOrderEtaEstimate.java`: Pure domain record representing single order ETA estimate with duration, distance, staleness checks, and SLA status.
5. `BatchEtaStopEstimate.java`: Pure domain record representing individual stop in a delivery batch with travel and service buffer durations.
6. `BatchEtaEstimate.java`: Pure domain record representing batch cumulative duration, distance, and completion ETA.
7. `DeliveryEtaCalculatedEvent.java`: Domain event for decoupled event notification across modules.

### B. Ports & Application Services
1. `LastMileRoutingPort.java`: Outbound provider-neutral routing port accepting origin/destination coordinates and transport mode.
2. `EtaCachePort.java`: Outbound port for order and batch ETA caching.
3. `DeliveryEtaEventPublisherPort.java`: Outbound event publication port.
4. `DeliveryEtaUseCase.java`: Inbound use case port (`getOrderEta`, `calculateOrderEta`, `getBatchEta`, `calculateBatchEta`).
5. `DeliveryEtaService.java`: Pure Java application service implementing `DeliveryEtaUseCase` with sequence simulation and service buffers.

### C. Adapters & Infrastructure
1. `ZoneModeHeuristicRoutingAdapter.java`: Implements `LastMileRoutingPort` using Haversine formula $\times 1.3$ road circuity and zone/mode speed table.
2. `InMemoryEtaCacheAdapter.java`: Implements `EtaCachePort` with tenant-aware composite keys.
3. `SpringDeliveryEtaEventPublisher.java`: Implements `DeliveryEtaEventPublisherPort` using Spring `ApplicationEventPublisher`.
4. `DeliveryEtaConfig.java`: Spring `@Configuration` defining beans and wiring ports.
5. `DeliveryEtaController.java`: REST controller exposing:
   - `GET /api/v1/deliveries/orders/{orderId}/eta`
   - `POST /api/v1/deliveries/orders/{orderId}/eta/calculate`
   - `GET /api/v1/deliveries/batches/{batchId}/eta`
   - `POST /api/v1/deliveries/batches/{batchId}/eta/calculate`

### D. Frontend Integration & E2E
1. `frontend/src/features/delivery/eta/api/deliveryEtaApi.ts`: Frontend client for single order and batch ETAs.
2. `frontend/src/features/delivery/batches/pages/DeliveryBatchListPage.tsx`: Enhanced drawer with Route Projection summary card, stop ETA arrival times, SLA badges, and "Recalculate ETA" action.
3. `frontend/e2e/tests/delivery/deliveryEta.spec.ts`: Playwright test verifying ETA rendering, stop breakdown, and recalculation flow.

---

## 3. Verification & Test Evidence

| Test Suite | Command | Result |
|---|---|---|
| **ETA Unit & Web Tests** | `mvn test -Dtest="EtaModelTest,ZoneModeHeuristicRoutingAdapterTest,DeliveryEtaServiceTest,DeliveryEtaControllerTest"` | **13 / 13 PASSED (100%)** |
| **Hexagonal & Modulith Architecture** | `mvn test -Dtest="HexagonalLayerArchitectureTest,ModuleBoundaryArchitectureTest,ApplicationModulesTest"` | **25 / 25 PASSED (100%)** |
| **Static Code Analysis** | `mvn checkstyle:check pmd:check spotbugs:check` | **0 Violations / 0 Bugs** |
| **Full Delivery Module Tests** | `mvn test -Dtest="com.transportlogistics.app.delivery.**"` | **182 Passed, 0 Failures, 0 Errors, 9 Skipped** |
| **Frontend Vitest Suite** | `docker run --rm node:20-slim npm test -- --run` | **55 Files / 251 Tests PASSED (100%)** |
| **Frontend Build** | `docker run --rm node:20-slim npm run build` | **Build Succeeded (tsc + vite)** |

---

## 4. Next Step

Hand off to **`MVP-1.4-US67-LAST-MILE-ETA-FINAL-ACCEPTANCE-001`** for independent acceptance verification.
