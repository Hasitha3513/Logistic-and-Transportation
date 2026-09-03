# Post-MVP 1.3 Strategic Roadmap Rebaseline
**Task ID:** `MVP-POST-1.3-ROADMAP-REBASELINE-001`  
**Date:** 2026-08-31  
**Status:** `APPROVED & BASELINED`  
**Current Release Milestone:** `MVP 1.3 CLOSED (7/7 COMPLETE)`  
**Overall Story Accounting:** `57 COMPLETE / 30 DEFERRED / 87 TOTAL (65.5% COMPLETE)`  

---

## 1. Executive Summary & Baseline State

With the formal closure of **MVP 1.3 (Delivery Operations Foundation)** on 2026-08-31, all 7 core delivery user stories (`US-56` through `US-62`) are fully implemented, verified with PostgreSQL 16 / Flyway V51 schema integrity, backed by 100% passing tests (unit, web, concurrency, and frontend Vitest), and documented.

The total user story inventory remains strictly bounded from `US-01` through `US-87`:
- **57 User Stories COMPLETE & ACCEPTED**
- **30 User Stories DEFERRED TO POST-MVP RELEASE BANDS**
- **0 User Stories IN PROGRESS or UNASSIGNED**

This document establishes the strategic, architecture-driven roadmap rebaseline for the subsequent release phase, evaluating technical dependencies, platform leverage, business value, and operational risks across the 30 deferred user stories.

---

## 2. Canonical 87-Story Register Summary

### A. Completed & Accepted Stories (57 Stories)
- **Module 1: Fleet Management (8/8):** `US-01`, `US-02`, `US-03`, `US-04`, `US-05`, `US-06`, `US-07`, `US-08`
- **Module 2: Trip Management (8/8):** `US-09`, `US-10`, `US-11`, `US-12`, `US-13`, `US-14`, `US-15`, `US-16`
- **Module 3: Route Management (7/7):** `US-17`, `US-18`, `US-19`, `US-20`, `US-21`, `US-22`, `US-23`
- **Module 4: Freight & Cargo Operations (7/7):** `US-24`, `US-25`, `US-26`, `US-27`, `US-28`, `US-29`, `US-30`
- **Module 5: Fuel & Bunker Management (5/8):** `US-31`, `US-32`, `US-33`, `US-34`, `US-36` *(US-35, US-37, US-38 deferred)*
- **Module 6: Driver Management (7/8):** `US-39`, `US-40`, `US-41`, `US-42`, `US-43`, `US-44`, `US-45` *(US-46 deferred)*
- **Module 9: Delivery Operations (7/7):** `US-56`, `US-57`, `US-58`, `US-59`, `US-60`, `US-61`, `US-62`
- **Module 11: Cross-Cutting & Security (4/8):** `US-71`, `US-74`, `US-75`, `US-77` *(US-72, US-73, US-76, US-78 deferred)*
- **Module 12: Supporting Architecture (4/9):** `US-79`, `US-80`, `US-81`, `US-83` *(US-82, US-84, US-85, US-86, US-87 deferred)*

### B. Deferred Stories Register (30 Stories)
1. `US-35`: Manage Fuel Cards (External 3rd-party fuel card provider API integration)
2. `US-37`: Analyze Fuel Performance (Thermodynamic vehicle fuel modeling & thermal efficiency)
3. `US-38`: Handle Fuel Exceptions (AI fuel theft heuristics & automated drain alarms)
4. `US-46`: Process Driver Payroll Link (Enterprise ERP / HR payroll ledger sync)
5. `US-47`: Manage Transport Billing (Accounts receivable freight invoicing & ERP billing)
6. `US-48`: Track Vehicles Live (High-throughput telematics stream ingestion & live map)
7. `US-49`: Manage Geofences (Polygon geospatial boundary entry/exit trigger engine)
8. `US-50`: Monitor Speed (Real-time IoT speed threshold alerting & event logging)
9. `US-51`: Monitor Idle Time (Engine telemetry idling detection & carbon footprint tracking)
10. `US-52`: Monitor Route Deviations (Off-corridor real-time GPS tracking deviation alarms)
11. `US-53`: Replay Journeys (Historical breadcrumb coordinate timeline replay player)
12. `US-54`: View Tracking Dashboard (Fleetwide multi-vehicle live tracking geospatial board)
13. `US-55`: Handle GPS Edge Cases (Satellite tunnel dead-reckoning & signal smoothing)
14. `US-63`: Manage Delivery Zones (Urban courier dispatch polygon micro-zones)
15. `US-64`: Manage Delivery Slots (Customer-facing dynamic booking slot capacity engine)
16. `US-65`: Manage Riders (Urban courier 2-wheeler / bicycle rider roster & shifts)
17. `US-66`: Batch Delivery Orders (Automated density clustering for urban multi-drop routes)
18. `US-67`: Calculate Last-Mile ETA (Pedestrian / bike congestion routing ETA heuristics)
19. `US-68`: Handle Last-Mile Exceptions (Gate code missing, high-rise access denial)
20. `US-69`: Receive Delivery Notifications (End-customer SMS / WhatsApp / Push live tracking updates)
21. `US-70`: Use Customer Self-Service (Public end-customer rescheduling & tracking web portal)
22. `US-72`: Enforce Compliance (Statutory vehicle inspection compliance & regulatory audits)
23. `US-73`: Manage External Integrations (Generic 3PL webhook connector platform)
24. `US-76`: Support Mobile Operations (Dedicated iOS / Android React Native native container)
25. `US-78`: Manage Operational Exceptions (Cross-module enterprise incident orchestration)
26. `US-82`: Use Operational Analytics (Executive cross-functional business intelligence board)
27. `US-84`: Handle Global System Failures (Multi-region active-active database failover)
28. `US-85`: Protect Data Integrity (Cross-module distributed ledger consistency scanner)
29. `US-86`: Handle Operational Disruptions (Fleetwide regional disaster rerouting workflows)
30. `US-87`: Detect User Risk (Operator security anomaly & credential abuse detection)

---

## 3. Architecture Readiness & Foundation Leverage

The platform currently provides robust foundations that directly benefit subsequent phases:
- **Multi-Tenant Foundation:** Thread-local `TenantExecutionContext`, `DeliveryTenantContextPort`, composite `(id, tenant_id)` DB foreign keys, and strict partial unique indexes.
- **Hexagonal Core & Event Decoupling:** Spring Modulith application event bus, provider-neutral outbound repository ports, and isolated web/persistence adapters.
- **Evidence & File Storage:** `DeliveryEvidenceStoragePort` with MIME validation, checksum generation, and safe local disk reference abstraction.
- **Offline Sync Coordinator:** Generic `offline_sync_operation` idempotency infrastructure (`US-71`) proven in POD capture (`US-58`).
- **Notification Engine:** Event-driven rule matching, channel routing, and templated dispatching (`US-77`).
- **Scheduling Coordinator:** Cron schedule triggers and operational task execution (`US-81`).

---

## 4. Evaluation of Candidate Next Release Slices

### Candidate A: Phase 2.1 — Telematics & Real-Time GPS Tracking (`US-48` to `US-55`)
- **Scope:** 8 stories (`US-48` Live Tracking, `US-49` Geofences, `US-50` Speed, `US-51` Idle Time, `US-52` Route Deviations, `US-53` Journey Replay, `US-54` Tracking Dashboard, `US-55` GPS Edge Cases).
- **Pros:** High business visibility, rich live geospatial UX, direct synergy with Trip and Route modules.
- **Cons / Risks:** Requires establishing WebSocket / MQTT streaming infrastructure, high-frequency time-series telemetry storage (e.g. TimescaleDB or partitioned tables), and significant IoT sensor simulator harness.
- **Architecture Readiness:** Moderate (requires telemetry stream adapter and geospatial indexing).

### Candidate B: Phase 1.4 — Last-Mile Urban Courier & Customer Engagement (`US-63` to `US-70`)
- **Scope:** 8 stories (`US-63` Delivery Zones, `US-64` Delivery Slots, `US-65` Riders, `US-66` Batching, `US-67` Last-Mile ETA, `US-68` Last-Mile Exceptions, `US-69` Notifications, `US-70` Customer Portal).
- **Pros:** Directly builds upon the newly closed MVP 1.3 Delivery Foundation (`US-56`–`US-62`), maximum reuse of existing `delivery_order`, `proof_of_delivery`, `delivery_attempt`, and `delivery_exception` tables.
- **Cons / Risks:** `US-69` and `US-70` require external SMS/Email provider integration and unauthenticated public customer portal tokenization.
- **Architecture Readiness:** High (Delivery Modulith boundary is mature and fully accepted).

### Candidate C: Phase 2.2 — Platform Governance, Compliance & Executive Analytics (`US-72`, `US-78`, `US-82`, `US-87`)
- **Scope:** 4 stories (`US-72` Regulatory Compliance, `US-78` Global Operational Exceptions, `US-82` Executive Analytics, `US-87` User Risk).
- **Pros:** Low UI complexity, strong enterprise audit posture.
- **Cons / Risks:** Lower direct operational value compared to active dispatch/tracking; cross-cutting dependencies across all modules.
- **Architecture Readiness:** High.

---

## 5. Strategic Prioritization Scoring

| Criteria (Weight) | Option A: GPS Tracking | Option B: Last-Mile Delivery | Option C: Platform Governance |
| :--- | :---: | :---: | :---: |
| **Business Value & ROI (25%)** | 4.5 / 5.0 | 4.8 / 5.0 | 3.5 / 5.0 |
| **Direct Foundation Leverage (20%)** | 3.5 / 5.0 | 5.0 / 5.0 | 4.0 / 5.0 |
| **Dependency Readiness (20%)** | 3.0 / 5.0 | 4.5 / 5.0 | 4.0 / 5.0 |
| **Implementation & DB Risk (15%)** | 2.5 / 5.0 *(High)* | 4.0 / 5.0 *(Low)* | 4.0 / 5.0 *(Low)* |
| **Testability & Determinism (20%)** | 3.0 / 5.0 | 4.5 / 5.0 | 4.2 / 5.0 |
| **Weighted Score** | **3.43 / 5.00** | **4.63 / 5.00** | **3.89 / 5.00** |

---

## 6. Recommended Next Phase: **MVP 1.4 — Last-Mile Delivery & Customer Experience**

### Objective & Justification
Option B (**MVP 1.4: Last-Mile Delivery & Customer Experience**) is the highest-leverage, lowest-risk, and most cohesive next step. Having closed MVP 1.3, the core delivery order and POD lifecycle are established. MVP 1.4 naturally extends delivery operations into urban zone micro-dispatch, rider allocation, time-slot scheduling, and customer communication without requiring new streaming infrastructure.

### Target Stories (8 Stories)
1. **`US-63` — Manage Delivery Zones:** Polygon micro-zones and urban dispatch boundaries.
2. **`US-64` — Manage Delivery Slots:** Dynamic booking windows and capacity throttling.
3. **`US-65` — Manage Riders:** Courier rider roster, shift availability, and vehicle/bike pairings.
4. **`US-66` — Batch Delivery Orders:** Clustering algorithm for dense multi-drop urban drop-offs.
5. **`US-67` — Calculate Last-Mile ETA:** Urban routing ETA heuristics considering parking/walking buffers.
6. **`US-68` — Handle Last-Mile Exceptions:** Access codes, gated communities, and high-rise drop-off issues.
7. **`US-69` — Receive Delivery Notifications:** Real-time customer delivery status alerts (SMS/Email).
8. **`US-70` — Use Customer Self-Service:** Tokenized customer portal for rescheduling and live delivery tracking.

---

## 7. Execution Sequence for MVP 1.4

The stories will be executed strictly using the frozen 3-step lifecycle (*Product Decisions $\rightarrow$ Implementation $\rightarrow$ Final Acceptance*):

1. **Step 1: `US-63` (Manage Delivery Zones)** $\rightarrow$ Foundational spatial zone mapping for all last-mile logic.
2. **Step 2: `US-64` (Manage Delivery Slots)** $\rightarrow$ Slot capacity definitions bound to delivery zones.
3. **Step 3: `US-65` (Manage Riders)** $\rightarrow$ Rider roster and shift assignment mapped to zones.
4. **Step 4: `US-66` (Batch Delivery Orders)** $\rightarrow$ Algorithmic batching of `delivery_order` rows by zone and slot.
5. **Step 5: `US-67` (Calculate Last-Mile ETA)** $\rightarrow$ Zone-based ETA calculation.
6. **Step 6: `US-68` (Handle Last-Mile Exceptions)** $\rightarrow$ Specialized last-mile operational exceptions integrated with US-62.
7. **Step 7: `US-69` (Receive Delivery Notifications)** $\rightarrow$ Integration with `notification` module for customer alerts.
8. **Step 8: `US-70` (Use Customer Self-Service)** $\rightarrow$ Public tokenized portal for customer rescheduling.

---

## 8. Exact Next Task Identification

👉 **`MVP-1.4-US63-DELIVERY-ZONES-PRODUCT-DECISIONS-001`**
- **Story:** `US-63 — Manage Delivery Zones`
- **Goal:** Freeze product decisions, domain contracts, polygon schema representation, multi-tenant rules, and API interfaces for Delivery Zone Management.
