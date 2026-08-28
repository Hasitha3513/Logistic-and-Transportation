# Transportation & Logistics — Master Traceability & MVP Roadmap

> **Last Updated:** 2026-08-28
> **Current Status:** **MVP 1.0 (Core)** and **MVP 1.1 (Route & Freight)** are complete; **MVP 1.2 (Fuel Management Core)** is closed with approved deferments.
> **Next Active Scope:** MVP 1.3 Delivery Operations (US-56 through US-62), beginning with source-contract freeze; no production implementation is authorized by this roadmap task.

---

## 🚦 Executive Summary Dashboard

| Phase | Module / Scope | Scope Band | Status | Progress |
| :--- | :--- | :--- | :--- | :--- |
| **Phase 0** | Foundation / Platform Baseline | Core Enabler | ✅ VERIFIED | 100% |
| **Phase 1** | Core MVP 1.0 (Fleet, Trip, Route, Driver, Enablers) | Core MVP | ✅ COMPLETE | 34 / 34 (100%) |
| **Phase 2A**| Advanced Route (US-20 to US-23) | Expanded MVP 1.1 | ✅ COMPLETE | 4 / 4 (100%) |
| **Phase 2B**| Freight & Cargo (US-24 to US-30) | Expanded MVP 1.1 | ✅ COMPLETE | 7 / 7 (100%) |
| **Phase 3** | Fuel Management (US-31 to US-38) | Expanded MVP 1.2 | ✅ CLOSED_WITH_APPROVED_DEFERMENTS | 5 Complete / 3 Deferred (US-35, 37, 38) |
| **Phase 4** | Delivery Operations (US-56 to US-62) | MVP 1.3 | 🟡 SELECTED / CONTRACT PENDING | 0 / 7 |
| **Post-MVP A** | Deferred Fuel (US-35, US-37, US-38) | Post-MVP | ⏸ DEFERRED | 0 / 3 |
| **Post-MVP B** | Driver Payroll Link & Transport Billing (US-46, US-47) | Post-MVP | ⏸ DEFERRED | 0 / 2 |
| **Post-MVP C** | GPS & Live Tracking (US-48 to US-55) | Post-MVP | ⏸ DEFERRED | 0 / 8 |
| **Post-MVP D** | Last-Mile Delivery (US-63 to US-70) | Post-MVP | ⏸ DEFERRED | 0 / 8 |
| **Post-MVP E** | Advanced Platform / Ops (US-72, 73, 76, 78, 82, 84-87) | Post-MVP | ⏸ DEFERRED | 0 / 9 |

---

## 🎯 Immediate Next Queue (What to build next)

- [x] ✅ **US-27 Weight & Volume:** Completed in `P2-WEIGHT-VOLUME-CARGO-MEASUREMENTS-004`; forward Flyway migration `V42`, domain model, persistence, API contracts, lookup port, UI inputs/displays, and verified PASS/FAIL/INCOMPLETE outcomes.
- [x] ✅ **US-30 Cargo Exceptions:** Completed in `P2-CARGO-EXCEPTION-001`; 6 exception types, 5-state lifecycle (OPEN/HELD/ESCALATED/RESOLVED/REJECTED), immutable history, 8 REST endpoints, RBAC, React frontend, 40 backend tests and 8 Playwright E2E all PASS.
- [x] **Tenant Business Authority:** `TENANT-BUSINESS-AUTHORITY-001` froze the MVP architecture contract; no implementation was performed.
- [x] ✅ **Tenant Clean Initialization:** `TENANT-CLEAN-INITIALIZATION-DECISION-001` authorized clean CLTS-LK initialization; no recoverable legacy database or backfill requirement exists.
- [x] ✅ **Tenant Foundation:** `TENANT-FOUNDATION-IMPLEMENTATION-001` implemented the canonical tenant, explicit membership, trusted server-side resolution, request-bounded context, V43, and clean PostgreSQL initialization.
- [x] ✅ **Tenant Operational Data Retrofit:** `TENANT-OPERATIONAL-DATA-RETROFIT-001` implemented V44 membership-scoped roles, operational discriminators, repository/direct-ID isolation, tenant-aware scheduled jobs, and Freight/Reporting-source acceptance.
- [x] ✅ **US-29 Freight Reporting:** Completed in `P2-FREIGHT-REPORTING-001`; tenant-scoped summaries, shipment/capacity reporting, honest incomplete outcomes, bounded CSV export, dedicated RBAC, frontend, and regression coverage.
- [x] **MVP 1.2 Fuel Closure:** Formally closed in `MVP-1.2-FUEL-CLOSURE-001` (`CLOSED_WITH_APPROVED_DEFERMENTS` with US-35, US-37, US-38 deferred to Post-MVP).
- [x] ✅ **MVP 1.1 Route & Freight Final Closure:** `MVP-1.1-FREIGHT-FINAL-CLOSURE-001` supersedes the earlier conditional closure; Advanced Route is 4/4 and Freight is 7/7 COMPLETE.
- [x] ✅ **Workspace Entry:** `FIX-WORKSPACE-ENTRY-001` connects the authenticated `/workspace` landing action to the permission-aware `/` application entry route without client-side Tenant selection or authority.
- [x] ✅ **MVP 1.3 Expansion Planning:** `MVP-EXPANSION-ROADMAP-001-R1` reconciled story mappings and selected Delivery Operations (US-56 through US-62); exact source contracts must be frozen before implementation.
- [ ] 🟡 **MVP 1.3 Delivery Contract Freeze:** `MVP-1.3-DELIVERY-OPERATIONS-CONTRACT-001` — recover/verify the original US-56 through US-62 definitions, freeze ownership, APIs, events, tenancy, RBAC and acceptance tests, then authorize the first implementation slice.

---

## MVP 1.3 Selection Record

### Source conflict and authoritative decision

| Difference found | Conflicting roadmap state | Authoritative decision | Evidence |
| :--- | :--- | :--- | :--- |
| MVP 1.1 lifecycle | Phase 2 said `IN CLOSURE` | MVP 1.1 Route & Freight is `COMPLETE` (Advanced Route 4/4; Freight 7/7) | `docs/mvp/MVP-current-status.md`; `docs/mvp/MVP-1.1-FREIGHT-FINAL-CLOSURE-001.md` |
| US-46 | Driver Performance & Scoring | **Process Driver Payroll Link**; remains deferred | US-41 already owns completed Driver Performance; authoritative current traceability assigns US-46 to Driver Payroll |
| US-47 | Driver Payroll & Billing | **Manage Transport Billing**; remains deferred and separate from US-46 | `docs/phase2/PHASE2-SCOPE-RECONCILIATION-001.md`; current traceability |
| Maintenance Linkage | Counted as an unnumbered third Phase 4 story | Not a new story; US-07 maintenance-to-availability linkage is complete. Additional workshop/work-order scope requires formal new stories | `docs/mvp/MVP-story-traceability-current.md`; current Fleet source |
| Fuel US-31–38 | Roadmap titles drifted from delivered/closed scope | Use the reconciled mapping in Phase 3 below; preserve 5 complete and 3 deferred statuses | `docs/mvp/MVP-1.2-FUEL-CLOSURE-001.md`; source and E2E evidence |
| Advanced Platform/Ops | Detailed list contained only five IDs while dashboard counted nine | Remaining IDs are **US-72, US-73, US-76, US-78, US-82, US-84, US-85, US-86, US-87** | `docs/mvp/MVP-story-traceability-current.md` |
| Multi-tenancy | Presented as future isolation implementation | Current-scope Tenant foundation and operational isolation are implemented and accepted; only distinct future platform products remain deferred | Tenant ADR, V43/V44, and central architecture standards |
| Original story register | Repository records acknowledge a consolidated US-01–87 source, but it is not stored or linked locally | Do not invent individual titles for US-48–70 or remaining Platform/Ops stories; freeze the applicable source contracts before implementation | `docs/phase-2-entry-criteria.md`; repository-wide source search |

### Candidate comparison

Scores are relative (`Low`, `Medium`, `High`) and describe delivery cost/risk, not product importance.

| Candidate | Business value | Dependency readiness / reuse | External integration | DB / Tenant / RBAC impact | Frontend / QA cost | Complexity | Decision |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| A. Driver Payroll Link + Transport Billing | High | Medium; reuses Trip, Driver, Freight and Customer facts but combines two separate financial concerns | High (payroll/accounting) | High | High | High | Post-MVP; split contracts first |
| B. Deferred Fuel completion | Medium | Medium; Fuel core is ready, but the three stories are not one cohesive slice | High for cards; analytics/anomaly data dependencies | Medium-High | High | High | Preserve approved deferments |
| C. GPS / Live Tracking | High | Medium; Route/Trip/Vehicle reuse is strong | High (devices, maps, streaming) | High | High | Very High | Post-MVP until telematics contracts exist |
| D. Delivery Operations | **High** | **High; directly extends completed Trip, Route, Freight, Customer and Location capabilities** | Medium (ePOD/signature/storage choices require contract) | High but bounded; all operational rows must be Tenant-scoped | High | Medium-High | **Selected MVP 1.3** |
| E. Last-Mile Delivery | High | Medium; depends on Delivery Operations and often GPS/customer messaging | High | High | Very High | Very High | Post-MVP after Delivery Operations |
| F. Advanced Platform/Ops | Strategic | Mixed; nine heterogeneous stories | High (SSO/ERP/SaaS services) | Very High | Very High | Very High | Post-MVP; decompose per story |

### Selected MVP 1.3 — Delivery Operations

- **Stories:** US-56 through US-62.
- **Selection rationale:** It is the most coherent next value stream and maximizes reuse of the completed Freight, Trip, Route and Organization foundations. It establishes the delivery execution boundary required before Last-Mile capabilities without depending first on live telematics, payroll, accounting or fuel-card providers.
- **Scope gate:** The range is selected, but production work remains blocked until the original story definitions and acceptance criteria are recovered and frozen. Range-level evidence supports delivery orders and electronic proof of delivery; individual story titles must not be guessed.
- **Tenant posture:** Every new operational aggregate, command/query, API, event, repository operation, background job and table must be Tenant-scoped through server-side `CurrentTenant` / `TenantExecutionContext`. No client Tenant authority is introduced.

### Planned implementation order

1. `MVP-1.3-DELIVERY-OPERATIONS-CONTRACT-001` — freeze the exact US-56–62 source mapping, ownership boundaries, lifecycle and acceptance criteria.
2. Freeze Delivery inbound/outbound ports and its logical references to Freight Orders, Trips, Customers and Locations; prohibit cross-module persistence joins.
3. Define Tenant-scoped delivery-order lifecycle, permissions, audit requirements and forward-only schema plan.
4. Implement the smallest delivery-order vertical slice with backend domain/application/adapters and focused tests.
5. Add assignment/execution milestones and exception behavior in source-defined order.
6. Add electronic proof-of-delivery only after signature/media retention, privacy and storage authority are approved.
7. Complete React workflows, accessibility, API/security tests, PostgreSQL isolation tests and Chromium acceptance; then perform release closure.

### Technical enablers

- Existing server-side Tenant membership resolution and V44 operational isolation patterns.
- Existing Trip, Route, Freight and Organization public boundaries; any missing Delivery-facing port requires an explicit contract update.
- New Delivery permissions must be defined centrally and enforced by backend RBAC; frontend visibility remains non-authoritative.
- Evidence storage, signatures, notifications, offline behavior and idempotency require explicit source-backed decisions before implementation.

---

## 📋 Detailed Phase Breakdown

### Phase 1: Core MVP 1.0 (✅ 34/34 COMPLETE — 100%)
- [x] **Phase 1A (Fleet Core):**
  - [x] **US-01:** Vehicle Master Registry
  - [x] **US-02:** Vehicle Categories & Specifications
  - [x] **US-03:** Vehicle Operational Status Lifecycle
  - [x] **US-04:** Vehicle Allocation Eligibility
  - [x] **US-05:** Vehicle Fuel Log History
  - [x] **US-06:** Running Log & Reading Tracking
  - [x] **US-07:** Preventive Maintenance Schedule
  - [x] **US-08:** Vehicle Compliance Documents
- [x] **Phase 1B (Trip Core):**
  - [x] **US-09:** Trip Request & Booking
  - [x] **US-10:** Multi-State Trip Lifecycle
  - [x] **US-11:** Trip Approval / Rejection
  - [x] **US-12:** Vehicle Assignment Validation
  - [x] **US-13:** Driver Assignment Validation
  - [x] **US-14:** Trip Dispatch & Departure
  - [x] **US-15:** Trip Start & Completion
  - [x] **US-16:** En-Route Delay, Checkpoint & Incident Logging
- [x] **Phase 1C (Basic Route):**
  - [x] **US-17:** Define Master Routes
  - [x] **US-18:** Calculate Route Distance & ETA
  - [x] **US-19:** Multi-Stop Ordered Sequencing
- [x] **Phase 1D (Driver Management Core):**
  - [x] **US-39:** Driver Profiles & Identification
  - [x] **US-40:** Multi-Class Driver Licensing & Validity
  - [x] **US-41:** Driver Safety & Performance Scoring
  - [x] **US-42:** Traffic Violations & Demerit Tracking
  - [x] **US-43:** Medical Fitness Certification
  - [x] **US-44:** Drug / Alcohol Testing Records
  - [x] **US-45:** Driver Exceptions & Leave Blocking
- [x] **Phase 1E (MVP Technical Enablers):**
  - [x] **US-71:** Offline Sync with Idempotency & SHA-256 Verification
  - [x] **US-74:** Security Architecture, JWT, RBAC & Granular Permissions
  - [x] **US-75:** Audit Trails & Operational Dashboard Aggregations
  - [x] **US-77:** Dynamic Notification Rule Engine & Templates
  - [x] **US-79:** Master Data (Customers, Locations, Departments, Projects, Vendors)
  - [x] **US-80:** Cross-Domain State Machines & Workflow Transactions
  - [x] **US-81:** Scheduled Scanners & Background Expiry Monitors
  - [x] **US-83:** Document Metadata & Compliance Storage

---

### Phase 2: Expanded MVP 1.1 — Route & Freight (✅ COMPLETE)
- [x] **Phase 2A (Advanced Route Intelligence):**
  - [x] **US-20:** Optimize Routes (Heuristic solver, savings preview, transactional apply)
  - [x] **US-21:** Route History & Snapshots (Immutable revision audit trail)
  - [x] **US-22:** Route Performance (Planned vs actual distance/duration variance analysis)
  - [x] **US-23:** Route Disruptions (Real-time obstruction alerts, severity levels, detours)
- [x] ✅ **Phase 2B (Freight & Cargo Management — COMPLETE):**
  - [x] **US-24:** Freight Orders (Commercial orders, cargo requirements, lifecycle)
  - [x] **US-25:** Cargo Manifest (Tri-state fragile/temperature classification, physical measurements, hazardous/customs)
  - [x] **US-26:** Load Planning (Structural readiness boundary, transactional revalidation, optimistic lock)
  - [x] **US-27:** Weight & Volume Validation (Engine + Cargo Manifest measurement integration with PASS/FAIL/INCOMPLETE evaluation)
  - [x] **US-28:** Freight Insurance (Policy underwriting, claims, multi-tranche settlements)
  - [x] ✅ **US-29:** Freight Reporting — tenant-scoped operational summaries, shipment/capacity views, insurance/claim/exception distributions, bounded CSV, dedicated RBAC, and frontend
  - [x] ✅ **US-30:** Cargo Exceptions — 6 types, command-driven lifecycle, immutable history, RBAC, frontend, 40 backend + 8 E2E PASS. Closed by `P2-CARGO-EXCEPTION-001`

*Closure Record:* [`docs/mvp/MVP-1.1-FREIGHT-CLOSURE-001.md`](file:///home/hasitha-wijerathna/Documents/transport-logistics-modulith/transport-logistics-modulith/docs/mvp/MVP-1.1-FREIGHT-CLOSURE-001.md)

---

### Phase 3: Expanded MVP 1.2 — Fuel Management (✅ CLOSED_WITH_APPROVED_DEFERMENTS)
- [x] **Phase 3A (Fuel Core):**
  - [x] **US-31:** Issue Fuel (voucher workflow, vehicle limit validation, bunker debit)
  - [x] **US-32:** Manage Fuel Purchases (vendor catalogue, receiving, variance and bunker credit)
  - [x] **US-33:** Track Mileage (authoritative monotonic Vehicle Reading ledger)
  - [x] **US-34:** Allocate Fuel Cost (Trip fuel-cost snapshots and consumption metrics)
  - [x] **US-36:** Manage Fuel Bunkers (tank master, immutable stock ledger, dip variance and transfers)
- [ ] ⏸ **Phase 3B (Fuel Deferments — Approved Post-MVP):**
  - [ ] ⏸ **US-35:** Manage Fuel Cards (provider integration and statement reconciliation) *(Deferred to Post-MVP)*
  - [ ] ⏸ **US-37:** Analyze Fuel Performance (predictive analytics and theft/anomaly intelligence) *(Deferred to Post-MVP)*
  - [ ] ⏸ **US-38:** Handle Fuel Exceptions (unauthorized/off-network fill-up investigation) *(Deferred to Post-MVP)*

*Closure Record:* [`docs/mvp/MVP-1.2-FUEL-CLOSURE-001.md`](file:///home/hasitha-wijerathna/Documents/transport-logistics-modulith/transport-logistics-modulith/docs/mvp/MVP-1.2-FUEL-CLOSURE-001.md)

---

### Phase 4: Delivery Operations (🟡 MVP 1.3 SELECTED — CONTRACT PENDING)

- [ ] 🟡 **US-56 through US-62:** Delivery Operations. Range selected for MVP 1.3; individual titles and acceptance criteria require source-contract freeze before production implementation.
- [x] ✅ **Maintenance-to-availability linkage:** Already delivered by US-07; it is not a new numbered MVP 1.3 story.
- **New workshop/work-order/inspection scope:** `NEW PRODUCT SCOPE / REQUIRES FORMAL USER STORIES`.

---

### Post-MVP Scope (⏸ DEFERRED)

- **Deferred Fuel:** US-35, US-37, US-38.
- **Commercial / Driver:** US-46 — **Process Driver Payroll Link**; US-47 — **Manage Transport Billing**. These are separate concerns and require independent contracts.
- **GPS & Live Tracking:** US-48 through US-55.
- **Last-Mile Delivery:** US-63 through US-70; depends on the Delivery Operations foundation and likely on GPS/customer-notification capabilities.
- **Advanced Platform / Ops:** US-72, US-73, US-76, US-78, US-82, US-84, US-85, US-86 and US-87.
- **Tenant clarification:** Tenant foundation, membership-scoped role assignment, operational Tenant scoping and current-scope isolation are already implemented/accepted. No roadmap item may describe those completed capabilities as future work; only separately source-defined platform products remain deferred.
