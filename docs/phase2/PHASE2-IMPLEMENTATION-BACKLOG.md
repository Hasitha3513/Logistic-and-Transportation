# Phase-2 Implementation Backlog (PHASE2-IMPLEMENTATION-BACKLOG)

**Phase:** Phase-2 Advanced Routing + Commercial Freight & Cargo  
**Release Baseline:** `v1.0.0-rc.1` (RC-1 Certified)  
**Generation Date:** August 24, 2026  
**Authoritative Sources:** `docs/phase2/PHASE2-SCOPE-RECONCILIATION-001.md`; `docs/phase2/US24-FREIGHT-ORDER-CONTRACT-001.md`; `docs/phase2/US25-CARGO-MANIFEST-CONTRACT-001.md`.  

> **PHASE2-SCOPE-RECON-001 — RESOLVED AND FROZEN:** The authoritative source assigns reporting to US-29 and cargo exceptions to US-30. US-26 is **Plan Loads** and US-27 is **Validate Weight and Volume**. Freight charges are owned by **US-47 — Manage Transport Billing**, outside US-24 through US-30, and are no longer an active task in this Phase-2 backlog. The superseded mapping remains recorded below under Historical Reconciliation.

---

## Task Inventory

```text
P2-01: Route History & Disruption Management (US-21, US-23)
P2-02: Route Optimization & Analytics (US-20, US-22)
P2-03: Freight Order Domain, Persistence & REST APIs (US-24)
P2-04-R1: Cargo Manifest & Consignment Verification (US-25)
P2-05-R1: Load Planning (US-26)
P2-05-R2: Weight & Volume Validation (US-27)
P2-06-R1: Freight Insurance, Claims & Settlements (US-28)
P2-07-R1: Freight Reporting (US-29)
P2-08-R1: Cargo Exception Management (US-30)
```

---

### Task Details

#### Task ID: `P2-01` [COMPLETE]
- **Status:** **COMPLETE** (Delivered & Certified on August 24, 2026)
- **User Stories:** `US-21` (Maintain Route History), `US-23` (Handle Route Disruptions)
- **Objective:** Introduce versioned route revision snapshotting and operational disruption detour handling inside the `routing` module.
- **Dependencies:** Existing `Route` aggregate and `V1`/`V5` schema.
- **Backend Delivered:**
  - `RouteRevision` domain snapshot model and `RouteRevisionEntity` capturing immutable snapshots upon route creation, modification, and deactivation.
  - `RouteDisruption` aggregate with disruption type (`ROAD_CLOSURE`, `ACCIDENT`, `WEATHER`, `RESTRICTION`), severity (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), active time window, status (`ACTIVE`, `RESOLVED`), and detour route reference.
  - Outbound Spring Application Events: `RouteDisruptionCreatedEvent`, `RouteDisruptionResolvedEvent`.
- **Frontend Delivered:**
  - `RouteRevisionSection`: Displays version history snapshots (`v1`, `v2`) with planned parameters and diff details in Route details drawer.
  - `RouteDisruptionsSection` & `RouteDisruptionModal`: Active disruption reporting and resolution actions with RBAC.
  - `ActiveDisruptionsBanner`: Network-wide disruption alert banner across Route library.
- **Database Migration:** `V30__route_history_and_disruptions.sql` applied and validated.
- **Tests & Verification:** 735 backend tests PASS (0 failures), 22 architecture tests PASS, 184 Vitest tests PASS, 219 Playwright tests PASS across Chromium, Firefox, WebKit.
- **Documentation:** `docs/phase2/P2-01-ROUTE-HISTORY-DISRUPTIONS.md`.

---

#### Task ID: `P2-02` [COMPLETE]
- **Status:** **COMPLETE** (Delivered & verified on August 25, 2026)
- **User Stories:** `US-20` (Optimize Routes), `US-22` (Analyze Route Performance)
- **Objective:** Provide automated waypoint sequencing optimization and planned-versus-actual performance analytics.
- **Dependencies:** `P2-01`, `TripService` historical actuals.
- **Backend Delivered:**
  - Deterministic nearest-neighbour plus bounded 2-opt solver for up to 50 ordered stops, using real organization location coordinates and never fabricated fallback distances.
  - Preview and transactional apply workflows; apply revalidates the canonical proposal and creates an immutable route revision atomically.
  - Route performance projection over Trip actual odometers, timestamps and delay events through an exposed module contract.
- **Frontend Delivered:**
  - Permission-aware waypoint optimizer modal with ordered before/after sequences and distance/duration savings.
  - Route performance summary in the route detail view with honest empty-data rendering.
- **Database Scope:** None (transient optimization + queries against existing trip histories).
- **Tests:** Algorithm edge cases and determinism, application/controller/persistence/security tests, frontend component tests and cross-browser Playwright scenarios.
- **Out of Scope:** Real-time turn-by-turn navigation engine.
- **Definition of Done:** **MET** — optimization is no worse than the submitted order, canonical apply is revisioned, and performance stats are rendered from real Trip data.

---

#### Task ID: `P2-03` [COMPLETE]
- **Status:** **COMPLETE** (Delivered and verified on August 25, 2026 against `US24-FREIGHT-ORDER-CONTRACT-001`)
- **User Stories:** `US-24` (Manage Freight Orders)
- **Objective:** Establish the new top-level Spring Modulith `freight` module with the source-aligned `FreightOrder` commercial shipment request.
- **Dependencies:** `organization` module (`CustomerLookup`, `LocationLookup`).
- **Backend Scope:**
  - New module `com.transportlogistics.app.freight` with Hexagonal Architecture.
  - `FreightOrder` aggregate and minimal `FreightOrderLine` children with no planning-only lifecycle.
  - Stable business number, organization-owned reference validation, actor/timestamp audit metadata, optimistic concurrency, and provider-neutral events.
  - Inbound ports, use cases, persistence adapters, and REST controller at `/api/v1/freight/orders`.
- **Frontend Scope:**
  - Feature-first structure: `frontend/src/features/freight/orders/`.
  - List, create, details and edit pages, TanStack Query hooks, React Hook Form + Zod validation, and minimal line editor.
- **Database Scope:** Forward migration `V31__freight_order_foundation.sql`.
- **Tests:** Domain/application/persistence/security/architecture, frontend component, and six logical Playwright cases across all browsers.
- **Out of Scope:** Cargo Manifest and Load Planning (deferred to P2-04 / P2-05).
- **Definition of Done:** Create/list/detail/update, RBAC, validation, audit, concurrency, frontend and regression gates are green. Confirmation/cancellation/state transitions are explicitly not part of the frozen US-24 contract.
- **Historical Reconciliation:** Earlier planning proposed `DRAFT -> CONFIRMED -> ASSIGNED -> IN_TRANSIT -> DELIVERED -> CANCELLED` plus confirm/cancel endpoints. The source-aligned R1 contract removed those assumptions from active US-24 scope while preserving this note.

---

#### Task ID: `P2-04-R1`
- **Status:** COMPLETE — source-aligned US-25 implemented in `V32`; next slice is P2-05-R1 / US-26.
- **User Stories:** `US-25` (Manage Cargo Manifest)
- **Objective:** Implement the frozen Cargo Manifest aggregate, identifiable cargo items, packing data, commodity classification, conditional customs/hazmat information, validation and finalization.
- **Dependencies:** `P2-03-R1`; saved Freight Orders through a focused boundary. Trip and Vehicle are not required US-25 references.
- **Backend Scope:**
  - Feature-first `manifest` slice inside the existing `freight` module.
  - `CargoManifest` aggregate with owned cargo items; do not invent package/classification value catalogues.
  - Item, customs, hazmat, completeness, finalization, audit and optimistic-concurrency rules.
  - Explicit finalization command; finalized manifests reject ordinary edits.
- **Frontend Scope:**
  - `frontend/src/features/freight/manifests/`.
  - Permission-aware list, create, details, edit, cargo-item management, validation feedback and finalization.
- **Database Scope:** Use the next available forward migration after the existing V31 Freight Order foundation; do not reuse V31 or modify historical migrations.
- **Tests:** Frozen US-25 domain/application/persistence/concurrency/controller/security/architecture/frontend and cross-browser E2E matrix.
- **Out of Scope:** Trip/Vehicle ownership, load placement, weight/capacity validation, insurance, reports, exception correction, physical barcode hardware and freight charges.
- **Definition of Done:** `US25-CARGO-MANIFEST-CONTRACT-001.md` is fully implemented with immutable normal behavior after validated finalization.

---

#### Task ID: `P2-05-R1` [COMPLETE]
- **Status:** **COMPLETE** (Delivered & Verified on August 25, 2026)
- **User Stories:** `US-26` (Plan Loads)
- **Objective:** Plan physical cargo distribution, stacking, pallet/container placement, compatibility, fragile/temperature separation and loading sequence.
- **Dependencies:** `P2-04-R1`, focused Fleet vehicle/load-space boundary.
- **Backend Scope:** `LoadPlan` boundary and placement/compatibility policies in `com.transportlogistics.app.freight.loadplanning`.
- **Frontend Scope:** `frontend/src/features/freight/loadPlanning/` physical placement and replanning experience.
- **Database Scope:** Forward migrations `V33__load_plan_permissions.sql` and `V34__load_plan_tables.sql`.
- **Tests:** Placement, stacking, compatibility, separation, invalid configuration, replan and RBAC cases.
- **Definition of Done:** A source-aligned plan can be saved/replanned and invalid physical configurations cannot be approved normally.

#### Task ID: `P2-05-R2` [COMPLETE]
- **Status:** **COMPLETE** (Delivered & Verified on August 25, 2026)
- **User Stories:** `US-27` (Validate Weight and Volume)
- **Objective:** Calculate gross/net/cubic values from supplied data and validate vehicle payload, volume and applicable axle limits before load approval.
- **Dependencies:** `P2-04-R1`, `P2-05-R1`, focused Fleet capacity and authoritative axle-limit boundaries.
- **Backend Scope:** Provider-neutral validation service/result with explicit missing-data, overload, volume-exceedance and axle-violation reasons.
- **Frontend Scope:** Validation results and corrective guidance inside `freight/loadPlanning`.
- **Tests:** Gross/net/cubic derivation, precision, missing/invalid measurements, overload, volume, axle, replan/reject and RBAC.
- **Definition of Done:** Capacity validation occurs before approval and violations cannot be represented as compliant.

---

#### Task ID: `P2-06-R1` [COMPLETE]
- **Status:** **COMPLETE** (Delivered & Verified on August 25, 2026)
- **User Stories:** `US-28` (Manage Freight Insurance)
- **Objective:** Manage cargo policy association, policy status/coverage/premium information, damage claims, authorized claim workflow and settlements.
- **Dependencies:** US-24/US-25; US-30 may later initiate the optional claim branch.
- **Backend Scope:** Insurance policy, claim, assessment, status and settlement history with explicit authorized transitions in `com.transportlogistics.app.freight.insurance`.
- **Frontend Scope:** `frontend/src/features/freight/insurance/` policy, claim, assessment and settlement views/actions.
- **Database Scope:** Forward migrations `V35__freight_insurance_permissions.sql` and `V36__freight_insurance_tables.sql`.
- **Tests:** Policy validity/coverage, claim assessment and transition authorization, settlement audit, over-settlement prevention, RBAC, architecture, Vitest and Playwright E2E.
- **Documentation:** `docs/phase2/P2-06-R1-FREIGHT-INSURANCE.md`.
- **Definition of Done:** Valid cargo-policy association and auditable claim-to-settlement workflow match the source acceptance criteria.

---

#### Task ID: `P2-07-R1`
- **User Stories:** `US-29` (Generate Freight Reports)
- **Objective:** Provide read-only shipment, capacity-utilization, insurance-claim and compliance reporting with honest incomplete-data semantics.
- **Dependencies:** US-24 through US-28 read models and the `reporting` module boundary.
- **Backend Scope:** Typed non-mutating report queries, reporting-period/criteria filters, incomplete-source detection and export boundary where supported.
- **Frontend Scope:** `frontend/src/features/freight/reports/` for shipment, utilization, claim and compliance reports.
- **Database Scope:** Prefer read projections; create a forward migration only if an owned projection requires persistence.
- **Tests:** Aggregation, incomplete data, non-mutation, report permission, filter/export and cross-browser E2E.
- **Out of Scope:** Freight rating/charges and all source-transaction mutations.
- **Definition of Done:** Source-defined reports are permission-protected and cannot alter transactional data.

#### Task ID: `P2-08-R1`
- **User Stories:** `US-30` (Handle Cargo Exceptions)
- **Objective:** Manage the common auditable lifecycle for damage, partial shipment, weight discrepancy, hazardous-material, unmanifested-cargo and seal-tampering exceptions.
- **Dependencies:** US-25, US-27, optional US-28 claim branch and a focused Trip dispatch-readiness boundary.
- **Backend Scope:** Exception type/severity, impact, corrective action, holds/escalation/rejection/release, authorized manifest correction and retained resolution history.
- **Frontend Scope:** `frontend/src/features/freight/exceptions/` intake, restrictions, action and history experience.
- **Database Scope:** Later forward migration owned by P2-08-R1.
- **Tests:** All six source-defined types, restrictions, correction/claim branches, closure history, RBAC and cross-browser E2E.
- **Out of Scope:** Ordinary manifest editing, load/weight calculation, claim adjudication and reporting aggregation.
- **Definition of Done:** Controlled cargo exceptions are auditable, hazardous/unmanifested cargo is restricted appropriately, and closed history is retained.

---

## Historical Reconciliation

The August 24 planning backlog assigned **Calculate Freight Charges** to US-29 and **Generate Freight Reports** to US-30, grouped insurance and rating into P2-06, and omitted the source-defined cargo-exception story. P2-03-R1 retained a warning rather than changing unverified scope. `PHASE2-SCOPE-RECON-001` subsequently verified the authoritative source and replaced that active mapping: US-29 is reporting, US-30 is cargo exceptions, and freight charges belong to **US-47 — Manage Transport Billing** outside this Phase-2 sequence. This note preserves the superseded planning record rather than silently erasing it.
