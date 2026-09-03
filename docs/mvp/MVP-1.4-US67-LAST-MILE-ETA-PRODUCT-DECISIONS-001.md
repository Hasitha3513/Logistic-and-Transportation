# Product Decisions & Domain Contract Freeze: US-67 Calculate Last-Mile ETA

**Task ID:** `MVP-1.4-US67-LAST-MILE-ETA-PRODUCT-DECISIONS-001`  
**User Story:** `US-67` — Calculate Last-Mile ETA  
**Date:** 2026-09-01  
**Status:** `PRODUCT_DECISIONS_FROZEN / GOVERNANCE_RECONCILED`
**Implementation State:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED`

> **Governance reconciliation (2026-09-01):** Commit
> `ab0b3965beffa14d3e78697946f1a5883b5bba27` is accepted as the existing,
> non-destructive implementation baseline. The earlier `NOT_STARTED` label was
> a sequencing/documentation mismatch, not authority to revert implementation.
> Independent acceptance remains blocked by the findings recorded in
> `MVP-1.4-US67-LAST-MILE-ETA-FINAL-ACCEPTANCE-001.md`.

> **PRODUCT_DECISION_RECONCILIATION — APPROVED (2026-09-01):**
> `MVP_PROVIDER_STRATEGY = HEURISTIC_ONLY`. External routing providers are
> deferred and external-provider fallback is not applicable to MVP 1.4.
> `LastMileRoutingPort` remains the provider-neutral future extension seam.

---

## 1. Metadata & Precondition Gate

- **Requirements Baseline:** `docs/requirements/Traspotation & logistic.docx` (US-67), `docs/requirements/US-61-US-70-UseCase-Activity-Sequence-Diagrams.md` (US-67)
- **Precondition Verification:**
  - `US-63`: Delivery Zones — COMPLETE (Accepted)
  - `US-64`: Delivery Slots — COMPLETE (Accepted)
  - `US-65`: Riders — COMPLETE (Accepted)
  - `US-66`: Batch Delivery Orders — COMPLETE (Accepted, `docs/mvp/MVP-1.4-US66-BATCH-DELIVERY-ORDERS-FINAL-ACCEPTANCE-001.md`)
  - Flyway Baseline: `V55__delivery_batches_us66.sql` PASS
- **Release Band Progress:** 61 / 87 Stories Complete (70.1%), 26 / 87 Deferred.

---

## 2. Authoritative Requirement Extraction

- **Primary Actor:** Last-Mile Planner / Dispatcher / Courier Rider
- **Goal:** Calculate and recalculate dynamic transit and arrival times (ETA) for individual delivery orders and batched multi-stop delivery routes, accounting for travel mode, stop service buffers, zone transit speeds, and route sequence changes.
- **Business Purpose:** Provide realistic, deterministic arrival predictions to dispatch operators and customers, identify at-risk delivery slot commitments, and adjust schedules when operational delays occur.
- **Dependencies:**
  - `DeliveryOrder` (US-56): Destination location, delivery window, status.
  - `DeliveryZone` (US-63): Zone type (`URBAN_DENSE`, `SUBURBAN`, `RURAL`, `SPECIAL_SECURITY`), default transit speeds, depot location.
  - `DeliverySlot` (US-64): Time-window start/end for SLA risk assessment.
  - `DeliveryRider` (US-65): Rider mode/transport capability (`BICYCLE`, `MOTORBIKE`, `VAN`, `CAR`, `WALKER`).
  - `DeliveryBatch` (US-66): Batched order stops and sequence hints (`sequenceHint`).
  - `LocationMaster` / `DeliveryLocationLookupPort`: Origin and destination geographic coordinates (latitude/longitude).

---

## 3. Domain Ownership & Boundaries

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│                                DELIVERY MODULE (Owner)                                   │
│                                                                                          │
│  - Owns Last-Mile ETA orchestration, subject models (Order/Batch), SLA risk assessment. │
│  - Pure Domain: EtaEstimate, BatchEtaEstimate, EtaStatus, EtaSource, EtaConfidence.     │
│  - Inbound Port: DeliveryEtaUseCase                                                     │
│  - Outbound Port: LastMileRoutingPort (abstract routing interface)                       │
└────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                         │ Consumes via Port / In-Memory Adapter
┌────────────────────────────────────────▼─────────────────────────────────────────────────┐
│                                ROUTING HEURISTIC ADAPTER                                 │
│                                                                                          │
│  - MVP Adapter: Zone & Mode Aware Haversine / Road Distance Heuristic Engine              │
│  - Future external providers remain deferred behind LastMileRoutingPort                   │
│  - Route module (US-18/US-20) remains untouched for line-haul freight & standard routes  │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

- **Domain Ownership:** `com.transportlogistics.app.delivery` owns last-mile ETA orchestration, calculation queries, SLA status evaluations, and event publishing.
- **Route Module Boundary:** Delivery consumes routing distance/travel-time calculations via a domain-neutral outbound port `LastMileRoutingPort`. Delivery never duplicates line-haul route engines and Route never receives last-mile delivery batch dependencies.
- **Trip Boundary:** Zero interaction with Trip persistence or Trip lifecycle.
- **Telematics / GPS Boundary (US-48..US-55):** Live continuous GPS stream ingestion remains deferred. US-67 uses rider's last known checkpoint / dispatch depot origin for calculations.

---

## 4. Exact ETA Definition & Calculation Models

### 4.1 Supported ETA Subjects
1. **Single Delivery Order ETA:** Estimated travel time and arrival timestamp from an origin point (e.g. depot or rider starting location) to the delivery destination location.
2. **Batched Multi-Stop Delivery ETA:** Cumulative stop-by-stop ETA calculation for all active orders in a `DeliveryBatch`, evaluated in deterministic sequence order.

### 4.2 Calculation Formulation & Invariants
$$\text{ETA}_{\text{Stop } i} = \text{Departure Time} + \sum_{k=1}^{i} \left( \text{Transit Time}_{k-1 \to k} + \text{Service Duration}_{k-1} \right)$$
Where:
- $\text{Transit Time} = \frac{\text{Distance}_{k-1 \to k}}{\text{Speed}(\text{Mode}, \text{ZoneType})} \times \text{Traffic Modifier}$
- $\text{Service Duration} = \text{Fixed stop buffer (e.g. 5 mins for standard doorstep delivery, 10 mins for high-density apartments)}$.
- $\text{Speed Defaults by Mode & Zone}$:
  - `BICYCLE` in `URBAN_DENSE`: 15 km/h
  - `MOTORBIKE` in `URBAN_DENSE`: 25 km/h, in `SUBURBAN`: 40 km/h
  - `VAN` / `CAR` in `URBAN_DENSE`: 20 km/h, in `SUBURBAN`: 45 km/h, in `RURAL`: 60 km/h
  - `WALKER` in `URBAN_DENSE`: 5 km/h

### 4.3 Slot & Window SLA Risk Evaluation
For each stop $i$ with arrival time $\text{ETA}_i$ and associated `DeliverySlot` $[S_{\text{start}}, S_{\text{end}}]$:
- **`ON_TIME`**: $\text{ETA}_i \le S_{\text{end}} - 15 \text{ mins}$
- **`AT_RISK`**: $S_{\text{end}} - 15 \text{ mins} < \text{ETA}_i \le S_{\text{end}}$
- **`LATE`**: $\text{ETA}_i > S_{\text{end}}$

---

## 5. Input, Origin, and Destination Contracts

1. **Origin Source:**
   - For unassigned / draft batches: Delivery Zone depot location (`DeliveryZone.depotLocationId`).
   - For assigned / dispatched batches: Current Rider last known location or zone depot.
   - For sequential batch stops ($i > 1$): Destination location of stop $i-1$.
2. **Destination Source:**
   - Authoritative `DeliveryOrder.destinationLocationId` resolved via `DeliveryLocationLookupPort`.
   - Coordinates strictly validated: Latitude $[-90.0, 90.0]$, Longitude $[-180.0, 180.0]$. Missing coordinates reject calculation with `DELIVERY_ETA_COORDINATES_MISSING`.
3. **Stop Sequencing Authority:**
   - Stop sequence is strictly dictated by `DeliveryBatchOrder.sequenceHint` (or created order index).
   - **US-67 does NOT re-optimize stop order.** It computes the forward simulation of the assigned sequence.

---

## 6. Real-Time Classification, Freshness, & Caching

- **Real-Time Classification:** `SCHEDULE_AND_ZONE_AWARE` (honestly labeled; not claiming live satellite GPS unless telematic provider is active).
- **Freshness TTL:** ETA calculations are valid for **15 minutes** (`staleAt = calculatedAt + 15m`).
- **Recalculation / Invalidation Triggers:**
  - `DeliveryBatch` order membership changed (order added / removed).
  - `DeliveryBatch` rider assigned / reassigned (alters transport mode and starting location).
  - `DeliveryBatch` dispatched (resets base departure time to actual dispatch timestamp).
  - `DeliveryOrder` destination location updated / corrected (US-62 exception).
  - Explicit manual operator refresh via REST API.

---

## 7. Routing Provider Strategy & Failure Mechanics

1. **MVP Provider Strategy:** **`HEURISTIC_ONLY`**, using the domain-neutral `LastMileRoutingPort` interface.
2. **MVP Adapter:** In-memory Zone & Mode Road Distance Heuristic Adapter (Haversine distance with accepted circuity factor $1.3\times$ and zone-specific speed curves).
3. **External Provider:** **`DEFERRED`**. No external endpoint, credential, traffic feed, quota, timeout, retry, or rate-limit contract is part of MVP 1.4.
4. **Source Semantics:**
   - `EtaSource.HEURISTIC` is the normal MVP production result.
   - `EtaSource.HEURISTIC_FALLBACK` is reserved for a future adapter/decorator that actually attempts an upstream provider and falls back.
   - `EtaSource.EXTERNAL_PROVIDER` is reserved for a future external adapter.
   - Reserved sources must not be emitted by the current MVP execution path.
5. **Current Failure Behavior:** Invalid/missing coordinates produce `DELIVERY_ETA_COORDINATES_MISSING`; invalid operational input uses existing domain validation; `DELIVERY_ETA_ROUTE_UNAVAILABLE` applies only if the heuristic cannot produce a valid estimate.
6. **Future Extension:** An external adapter plus fallback decorator may be introduced behind `LastMileRoutingPort` without changing the ETA domain model, `DeliveryEtaUseCase`, REST API, or frontend contract.

---

## 8. Persistence & Database Migration Decision

- **Architecture Decision:** **Operational Cache & Projection Model**
- **Persistence Scope:**
  - In MVP 1.4, last-mile ETAs are operational projections computed on demand and cached in memory with a short TTL (15 min).
  - Calculated ETA fields (`estimatedArrivalAt`, `estimatedDurationSeconds`, `distanceMeters`, `slaStatus`) are returned in REST response DTOs and broadcast in domain events (`DeliveryEtaCalculatedEvent`).
  - Historical ETA logging table is deferred to Post-MVP telematics/analytics.
- **Flyway Migration Decision:** **`NO_FLYWAY_MIGRATION_REQUIRED`**
  - Schema remains at accepted head **`V55`**.
  - New RBAC permission `DELIVERY_ETA_VIEW` and `DELIVERY_ETA_CALCULATE` can be evaluated via existing `DELIVERY_BATCH_VIEW` / `DELIVERY_ORDER_VIEW` or seeded in a forward migration if dedicated permissions are mandated. For MVP 1.4, reusing `DELIVERY_ORDER_VIEW` and `DELIVERY_BATCH_VIEW` provides complete, secure RBAC without schema churn.

---

## 9. REST API & Error Contracts

### 9.1 API Endpoints
Base Path: `/api/v1/deliveries`

| Method | Endpoint | Description | Required Permission |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/deliveries/orders/{orderId}/eta` | Get current cached or computed ETA for a delivery order | `DELIVERY_ORDER_VIEW` |
| `POST` | `/api/v1/deliveries/orders/{orderId}/eta/calculate` | Force recalculation of single order ETA | `DELIVERY_ORDER_UPDATE` |
| `GET` | `/api/v1/deliveries/batches/{batchId}/eta` | Get cumulative multi-stop ETA projection for a batch | `DELIVERY_BATCH_VIEW` |
| `POST` | `/api/v1/deliveries/batches/{batchId}/eta/calculate` | Force recalculation of multi-stop batch ETA | `DELIVERY_BATCH_UPDATE` |

### 9.2 Standard Error Codes
- `DELIVERY_ETA_SUBJECT_NOT_FOUND` (404)
- `DELIVERY_ETA_COORDINATES_MISSING` (400)
- `DELIVERY_ETA_INVALID_STATE` (409)
- `DELIVERY_ETA_ROUTE_UNAVAILABLE` (422)
- `DELIVERY_ETA_PERMISSION_DENIED` (403)

---

## 10. Concurrency, Multi-Tenancy & Stale-Write Protection

- **Tenant Isolation:** All lookups strictly enforce `tenant_id` from `TenantContextExecutor`. Cross-tenant subject lookups return `404 Not Found`.
- **Stale-Write Protection:** Calculation requests compute an `inputFingerprint` (hash of stop IDs, sequence, coordinates, departure time). Responses older than the aggregate version are discarded.
- **Race Condition Safety:** Concurrent recalculations for the same batch return identical deterministic values or update the cache atomically.

---

## 11. Domain Events

1. **`DeliveryEtaCalculatedEvent`**:
   - Fields: `eventId`, `tenantId`, `subjectType` (`ORDER` or `BATCH`), `subjectId`, `estimatedArrivalAt`, `totalDurationSeconds`, `totalDistanceMeters`, `slaStatus`, `calculatedAt`, `actor`.
   - Privacy: Zero customer PII or raw provider credentials in event payload.

---

## 12. Scope Exclusions (Mandatory Boundary Guards)

- **US-68 (Last-Mile Exceptions):** No exception escalation state machine in US-67 (**NONE**).
- **US-69 (Notifications):** No SMS/Push/Email dispatch implementation in US-67 (**NONE**).
- **US-70 (Customer Self-Service):** No customer rescheduling web portal (**NONE**).
- **US-48..US-55 (Telematics & Live GPS):** No background GPS socket listeners or OBD-II polling (**NONE**).

---

## 13. Acceptance Verification Matrix

| Test ID | Scenario | Expected Outcome |
| :--- | :--- | :--- |
| **VM67-01** | Single order ETA calculation with valid coordinates | Computes arrival time, distance, duration, SLA status |
| **VM67-02** | Multi-stop batch cumulative ETA calculation | Sequential accumulation of travel time + stop buffers |
| **VM67-03** | Missing destination coordinates | Fails safely with `DELIVERY_ETA_COORDINATES_MISSING` |
| **VM67-04** | Multi-tenant isolation & IDOR | Cross-tenant order/batch calculation returns 404 |
| **VM67-05** | Transport mode speed adjustment | Bicycle vs Van yields distinct transit times |
| **VM67-06** | Slot SLA risk status | Evaluates `ON_TIME`, `AT_RISK`, `LATE` correctly |
| **VM67-07** | Batch order sequence alteration | Recomputes stop arrival times based on updated sequence |
| **VM67-08** | External provider timeout/fallback | `NOT_APPLICABLE_EXTERNAL_PROVIDER_DEFERRED`; normal MVP source remains `HEURISTIC` |
| **VM67-09** | Freshness & stale indicator | Returns `isStale = true` after 15 minutes TTL |
| **VM67-10** | Architecture compliance | Zero leakage into Route internals; domain purity preserved |

---

## 14. Acceptance Remediation Next Step

**Next Immediate Task:** `MVP-1.4-US67-LAST-MILE-ETA-ACCEPTANCE-REMEDIATION-001-RERUN`
(Close the independently verified stale-write, invalidation, rider-mode,
concurrency, security, and real-path Chromium evidence gaps;
then rerun final acceptance.)
