# Transport & Logistics — MVP / Product Roadmap

> **Last reconciled:** 2026-08-30
> **Authority:** Original requirements, frozen contracts/decisions, verified implementation evidence, derived UML, then prior roadmap.
> **Current active expansion:** MVP 1.3 Delivery Operations. US-56 and US-57 are accepted and complete; MVP 1.3 production story closure is 2/7 with 5 stories remaining (US-58 through US-62).

## 1. Purpose and Authority

This roadmap reconciles the complete product scope against the original DOCX, mind map, derived UML, frozen MVP contracts and current repository evidence. Newer authoritative evidence supersedes stale roadmap wording.

- **Authoritative product register:** US-01 through US-87
- **Total user stories:** 87
- **US-88:** NOT DEFINED
- **US-89:** NOT DEFINED
- **US-90:** NOT DEFINED

The `US-81-US-90` derived UML file does not authorize US-88 through US-90.

## 2. Product Scope at a Glance

The register covers Fleet Management, Trip Management, Route Management, Freight & Cargo, Fuel Management, Driver Management, Transport Billing, GPS / Real-Time Tracking, Delivery Management, Last-Mile Delivery, Offline Sync, Compliance, External Integrations, Security, Audit & Reporting, Mobile Operations, Notifications, Operational Exception Management, Master Data, Workflow, Scheduling, Operational Analytics, Document Management, Global System Failure Handling, Data Integrity, Operational Disruptions and User Risk.

- MVP 1.0 Core: `COMPLETE` — 34/34.
- MVP 1.1A Advanced Route: `COMPLETE` — 4/4.
- MVP 1.1B Freight & Cargo: `COMPLETE` — 7/7.
- MVP 1.2 Fuel: `CLOSED_WITH_APPROVED_DEFERMENTS` — 5 complete, 3 deferred.
- MVP 1.3 Delivery Operations: current active expansion — US-56 and US-57 `COMPLETE` (2/7); US-58 through US-62 not started.

## 3. Authoritative Story Register — US-01 through US-87

### Fleet Management — US-01 to US-08

- [x] `US-01` — Manage Vehicle Master — `COMPLETE`
- [x] `US-02` — Manage Fleet Categories — `COMPLETE`
- [x] `US-03` — Manage Vehicle Documents — `COMPLETE`
- [x] `US-04` — Allocate Vehicles — `COMPLETE`
- [x] `US-05` — Maintain Fuel & Lubricant Logs — `COMPLETE`
- [x] `US-06` — Maintain Running Logs — `COMPLETE`
- [x] `US-07` — Link Maintenance to Availability — `COMPLETE`
- [x] `US-08` — Handle Fleet Allocation Edge Cases — `COMPLETE`

### Trip Management — US-09 to US-16

- [x] `US-09` — Create Trip Orders — `COMPLETE`
- [x] `US-10` — Assign Driver and Vehicle — `COMPLETE`
- [x] `US-11` — Assign Route — `COMPLETE`
- [x] `US-12` — Start and End Trip — `COMPLETE`
- [x] `US-13` — Maintain Trip Log — `COMPLETE`
- [x] `US-14` — Complete Trip — `COMPLETE`
- [x] `US-15` — Handle Trip Exceptions — `COMPLETE`
- [x] `US-16` — Authorize Trip — `COMPLETE`

### Route Management — US-17 to US-23

- [x] `US-17` — Define Routes — `COMPLETE`
- [x] `US-18` — Calculate Distance and ETA — `COMPLETE`
- [x] `US-19` — Plan Multi-Stop Routes — `COMPLETE`
- [x] `US-20` — Optimize Routes — `COMPLETE`
- [x] `US-21` — Maintain Route History — `COMPLETE`
- [x] `US-22` — Analyze Route Performance — `COMPLETE`
- [x] `US-23` — Handle Route Disruptions — `COMPLETE`

### Freight & Cargo — US-24 to US-30

- [x] `US-24` — Manage Freight Orders — `COMPLETE`
- [x] `US-25` — Manage Cargo Manifest — `COMPLETE`
- [x] `US-26` — Plan Cargo Loads — `COMPLETE`
- [x] `US-27` — Calculate Weight and Volume — `COMPLETE`
- [x] `US-28` — Manage Freight Insurance — `COMPLETE`
- [x] `US-29` — Generate Freight Reports — `COMPLETE`
- [x] `US-30` — Handle Cargo Exceptions — `COMPLETE`

### Fuel Management — US-31 to US-38

- [x] `US-31` — Issue Fuel — `COMPLETE`
- [x] `US-32` — Manage Fuel Purchases — `COMPLETE`
- [x] `US-33` — Track Mileage — `COMPLETE`
- [x] `US-34` — Allocate Fuel Cost — `COMPLETE`
- [ ] `US-35` — Manage Fuel Cards — `DEFERRED`
- [x] `US-36` — Manage Fuel Bunkers — `COMPLETE`
- [ ] `US-37` — Analyze Fuel Performance — `DEFERRED`
- [ ] `US-38` — Handle Fuel Exceptions — `DEFERRED`

### Driver Management — US-39 to US-46

- [x] `US-39` — Manage Driver Profiles — `COMPLETE`
- [x] `US-40` — Manage Driver Licensing — `COMPLETE`
- [x] `US-41` — Assess Driver Performance — `COMPLETE`
- [x] `US-42` — Manage Violations — `COMPLETE`
- [x] `US-43` — Manage Driver Medical Fitness — `COMPLETE`
- [x] `US-44` — Manage Drug Tests — `COMPLETE`
- [x] `US-45` — Handle Driver Exceptions — `COMPLETE`
- [ ] `US-46` — Process Driver Payroll Link — `DEFERRED`

### Transport Billing — US-47

- [ ] `US-47` — Manage Transport Billing — `DEFERRED`

### GPS / Real-Time Tracking — US-48 to US-55

- [ ] `US-48` — Track Vehicles Live — `DEFERRED`
- [ ] `US-49` — Manage Geofences — `DEFERRED`
- [ ] `US-50` — Monitor Speed — `DEFERRED`
- [ ] `US-51` — Monitor Idle Time — `DEFERRED`
- [ ] `US-52` — Monitor Route Deviations — `DEFERRED`
- [ ] `US-53` — Replay Journeys — `DEFERRED`
- [ ] `US-54` — View Tracking Dashboard — `DEFERRED`
- [ ] `US-55` — Handle GPS Edge Cases — `DEFERRED`

### Delivery Management — US-56 to US-62

- [x] `US-56` — Manage Delivery Orders — `✅ COMPLETE`
- [x] `US-57` — Capture Proof of Delivery — `✅ COMPLETE`
- [ ] `US-58` — Capture Signature and Photo Offline — `🟡 IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`
- [ ] `US-59` — Manage Failed Deliveries — `NOT_STARTED`
- [ ] `US-60` — Schedule Re-Delivery — `NOT_STARTED`
- [ ] `US-61` — Analyze Delivery Performance — `NOT_STARTED`
- [ ] `US-62` — Handle Delivery Exceptions — `NOT_STARTED`

### Last-Mile Delivery — US-63 to US-70

- [ ] `US-63` — Manage Delivery Zones — `DEFERRED`
- [ ] `US-64` — Manage Delivery Slots — `DEFERRED`
- [ ] `US-65` — Manage Riders — `DEFERRED`
- [ ] `US-66` — Batch Delivery Orders — `DEFERRED`
- [ ] `US-67` — Calculate Last-Mile ETA — `DEFERRED`
- [ ] `US-68` — Handle Last-Mile Exceptions — `DEFERRED`
- [ ] `US-69` — Receive Delivery Notifications — `DEFERRED`
- [ ] `US-70` — Use Customer Self-Service — `DEFERRED`

### Cross-Cutting / Platform — US-71 to US-78

- [x] `US-71` — Support Offline Data Synchronization — `COMPLETE`
- [ ] `US-72` — Enforce Compliance — `DEFERRED`
- [ ] `US-73` — Manage External Integrations — `DEFERRED`
- [x] `US-74` — Manage Security — `COMPLETE`
- [x] `US-75` — Maintain Audit and Reports — `COMPLETE`
- [ ] `US-76` — Support Mobile Operations — `DEFERRED`
- [x] `US-77` — Manage Notification Rules — `COMPLETE`
- [ ] `US-78` — Manage Operational Exceptions — `DEFERRED`

### Supporting Capabilities — US-79 to US-83

- [x] `US-79` — Manage Master Data — `COMPLETE`
- [x] `US-80` — Configure Workflows — `COMPLETE`
- [x] `US-81` — Manage Scheduling — `COMPLETE`
- [ ] `US-82` — Use Operational Analytics — `DEFERRED`
- [x] `US-83` — Manage Documents — `COMPLETE`

### Global Edge Cases / Resilience — US-84 to US-87

- [ ] `US-84` — Handle Global System Failures — `DEFERRED`
- [ ] `US-85` — Protect Data Integrity — `DEFERRED`
- [ ] `US-86` — Handle Operational Disruptions — `DEFERRED`
- [ ] `US-87` — Detect User Risk — `DEFERRED`

## 4. MVP Phase Dashboard

| Phase | Scope | Story Count | Status |
| :--- | :--- | ---: | :--- |
| Phase 0 | Architecture / Platform Baseline | Enabler | `FOUNDATIONAL_CAPABILITY` |
| MVP 1.0 | Fleet US-01–08; Trip US-09–16; Basic Route US-17–19; Driver US-39–45; required enablers | 34 | `COMPLETE` — 34/34 |
| MVP 1.1A | Advanced Route US-20–23 | 4 | `COMPLETE` — 4/4 |
| MVP 1.1B | Freight & Cargo US-24–30 | 7 | `COMPLETE` — 7/7 |
| MVP 1.2 | Fuel US-31–38 | 8 | `CLOSED_WITH_APPROVED_DEFERMENTS` — 5 complete, 3 deferred |
| MVP 1.3 | Delivery Operations US-56–62 | 7 | `IN_PROGRESS`; US-56 and US-57 complete (2/7) |

Post-MVP bands contain Fuel US-35/37/38, Driver/Billing US-46/47, GPS US-48–55, Last-Mile US-63–70 and Advanced Platform/Ops US-72/73/76/78/82/84–87.

## 5. MVP 1.0 — Core

MVP 1.0 is `COMPLETE`: **34/34**. It contains Fleet US-01–08 (8), Trip US-09–16 (8), Basic Route US-17–19 (3), Driver US-39–45 (7), and required enablers US-71/74/75/77/79/80/81/83 (8). US-46 and US-47 are excluded.

## 6. MVP 1.1A — Advanced Route

US-20 through US-23 are `COMPLETE`: **4/4**.

## 7. MVP 1.1B — Freight & Cargo

US-24 through US-30 are `COMPLETE`: **7/7**. US-29 Generate Freight Reports is complete with Tenant-scoped reporting sources. Its historical Tenant-isolation blocker is superseded by the implemented Tenant foundation and operational-data retrofit.

## 8. MVP 1.2 — Fuel

MVP 1.2 is `CLOSED_WITH_APPROVED_DEFERMENTS`: US-31/32/33/34/36 are `COMPLETE`; US-35/37/38 are `DEFERRED`. Deferred stories remain in the register and are not double-counted.

## 9. MVP 1.3 — Delivery Operations

MVP 1.3 is the **current active expansion**. Its frozen scope is US-56 through US-62. US-56 and US-57 are accepted and complete. US-58 through US-62 remain not started.

Delivery remains Tenant-owned. Cross-module references are UUID/logical references behind ports; direct cross-module persistence access is prohibited.

## 10. Current Active Delivery Work

### US-57 — Capture Proof of Delivery

| Gate | Current state |
| :--- | :--- |
| POD product decisions | `COMPLETE` — frozen in `MVP-1.3-US57-POD-PRODUCT-DECISIONS-001.md` |
| Flyway migration & schema | `COMPLETE` — `V47__delivery_proof_of_delivery_us57.sql` |
| Domain Aggregate & invariants | `COMPLETE` — `ProofOfDelivery`, `PodEvidence`, `PodEvidenceType` |
| RBAC & Security endpoints | `COMPLETE` — `DELIVERY_POD_CAPTURE`, `DELIVERY_POD_VIEW` |
| Storage & streaming adapter | `COMPLETE` — `DeliveryEvidenceStoragePort`, `LocalDeliveryEvidenceStorageAdapter` |
| Frontend POD interface | `COMPLETE` — `ProofOfDeliverySection.tsx`, API hooks |
| Production verification | `COMPLETE` — Checkstyle (0), PMD (0), SpotBugs (0), ESLint (0), TS build (0 errors), 987 tests run (0 failures, 0 errors) |
| Playwright Chromium E2E | `COMPLETE` — 6/6 tests passed (US-56 & US-57 online POD flows) |
| Final acceptance document | `COMPLETE` — `docs/mvp/MVP-1.3-US57-PROOF-OF-DELIVERY-FINAL-ACCEPTANCE-001.md` |

- **Blocker:** `NONE`.
- **Status:** `✅ COMPLETE`.

### US-58 — Capture Signature and Photo Offline

| Gate | Current state |
| :--- | :--- |
| Offline POD product decisions | `COMPLETE` — frozen in `MVP-1.3-US58-OFFLINE-POD-PRODUCT-DECISIONS-001.md` |
| Client execution & IndexedDB model | `COMPLETE` — React SPA + IndexedDB outbox queue (`features/offlineSync`) |
| Quality, Retake & Consent rules | `COMPLETE` — Resolution checks, decodability, mandatory consent, draft retake |
| Sync granularity & idempotency | `COMPLETE` — Atomic `DELIVERY_POD_OFFLINE_SYNC` with UUID operationId |
| OfflineSync module boundary | `COMPLETE` — Delivery implements `OfflineProofOfDeliveryRecorder`; `DeliveryPodOfflineOperationHandler` handles outbox transport |
| RBAC & Tenant revalidation | `COMPLETE` — Server-side revalidation of `DELIVERY_POD_CAPTURE` and `TenantExecutionContext` |
| Implementation & Verification | `COMPLETE` — Checkstyle (0), PMD (0), SpotBugs (0), ESLint (0), TS build (0 errors), 995 tests passed |

- **Blocker:** `NONE`.
- **Status:** `🟡 IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`.
- **Next task:** `MVP-1.3-US58-OFFLINE-POD-FINAL-ACCEPTANCE-001`.

## 11. Deferred / Post-MVP Product Scope

- **Deferred Fuel:** US-35, US-37, US-38.
- **Driver Payroll / Billing:** US-46, US-47.
- **GPS / Tracking:** US-48 through US-55 (8 stories).
- **Last-Mile:** US-63 through US-70 (8 stories).
- **Advanced Platform / Operations:** US-72, US-73, US-76, US-78, US-82, US-84, US-85, US-86, US-87 (**9 stories**).

## 12. Platform / Cross-Cutting Capabilities

- `FOUNDATIONAL_CAPABILITY`: RBAC, audit, notifications, offline/store-and-forward, workflows, scheduling/resource calendars, document metadata/versioning patterns, Tenant isolation, server-side `CurrentTenant` / `TenantExecutionContext`, tenant-membership-scoped runtime RBAC, forward-only Flyway and hexagonal/Spring Modulith boundaries.
- ABAC, MFA and SSO appear in security governance; their architectural presence does not prove completion of every related product workflow.
- OCR, advanced analytics, external integrations, mobile operations, global resilience and broader operational exceptions remain story-scoped future work unless explicitly completed in the register.

`FOUNDATIONAL_CAPABILITY` is not equivalent to `USER_STORY_COMPLETE`.

## 13. Story Accounting

| Partition | Story IDs | Count |
| :--- | :--- | ---: |
| MVP 1.0 | US-01–19, US-39–45, US-71/74/75/77/79/80/81/83 | 34 |
| MVP 1.1A Advanced Route | US-20–23 | 4 |
| MVP 1.1B Freight & Cargo | US-24–30 | 7 |
| MVP 1.2 Fuel | US-31–38 | 8 |
| Remaining product scope | US-46–70 plus US-72/73/76/78/82/84–87 | 34 |
| **Total** | **US-01 through US-87** | **87** |

The remaining 34 includes MVP 1.3. Fuel deferments remain within Fuel's 8 and are not duplicated.

| Current status | Count | Composition |
| :--- | ---: | :--- |
| `COMPLETE` | 52 | MVP 1.0 (34), Advanced Route (4), Freight (7), Fuel complete subset (5), Delivery US-56 & US-57 (2) |
| `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING` | 1 | Delivery US-58 (1) |
| `NOT_STARTED` | 4 | US-59–62 |
| `DEFERRED` | 30 | Fuel 3; US-46–55 10; US-63–70 8; Advanced Platform/Ops 9 |
| **Total** | **87** | No duplicates or omissions |

## 14. Future Product Expansion — Not Yet Part of US-01..US-87

US-07 covers maintenance/breakdown information affecting vehicle availability and scheduling. It is not a complete Maintenance Management, Work Order, Job Card, Parts Inventory or Inspection Management product.

Potential future expansion includes Full Maintenance Management, Work Orders, Job Cards, Parts / Inventory and Inspection Management. These are not assigned US-88, US-89 or US-90 and require separate requirements and story-definition work.

## 15. Immediate Execution Queue

1. Final acceptance, hardening, regression validation, and closure for `US-58` Offline Proof of Delivery (`MVP-1.3-US58-OFFLINE-POD-FINAL-ACCEPTANCE-001`).

## 16. Change / Reconciliation Notes

- Register corrected to US-01 through US-87; US-88 through US-90 are undefined.
- Original story titles replace implementation-oriented paraphrases.
- US-41 is Assess Driver Performance; US-46 is Process Driver Payroll Link; US-47 is Manage Transport Billing.
- US-29 and all Freight stories are `COMPLETE` (7/7).
- Fuel is 5 `COMPLETE`, 3 `DEFERRED`, and `CLOSED_WITH_APPROVED_DEFERMENTS`.
- Delivery US-56–62 is MVP 1.3 and no longer `CONTRACT_PENDING`.
- Delivery-number ambiguity is resolved and implementation KB commit `1b579f61481276d4bc47518163d18e9c7c1d7af1` is verified on `origin/main`.
- Advanced Platform/Ops contains 9 stories: US-72/73/76/78/82/84/85/86/87.
- US-07 is availability linkage, not a complete workshop/maintenance product.
- Tenant isolation and membership-scoped RBAC are completed current-scope architecture, not future stories.

## 17. Evidence Index

- `docs/requirements/Traspotation & logistic.docx`
- `docs/requirements/Mind-Map-Trasportation-and-Logistic.txt`
- `docs/requirements/US-01-US-10-UseCase-Activity-Sequence-Diagrams.md` through `docs/requirements/US-81-US-90-UseCase-Activity-Sequence-Diagrams.md`
- `docs/mvp/MVP-current-status.md` and `docs/mvp/MVP-story-traceability-current.md`
- `docs/mvp/MVP-1.1-FREIGHT-FINAL-CLOSURE-001.md`
- `docs/mvp/MVP-1.2-FUEL-CLOSURE-001.md`
- `docs/mvp/MVP-1.3-DELIVERY-OPERATIONS-CONTRACT-001.md`
- `docs/mvp/MVP-1.3-DELIVERY-MODULE-FOUNDATION-001.md`
- `docs/mvp/MVP-1.3-US56-PRODUCT-DECISIONS-001.md`
- `docs/mvp/MVP-1.3-US56-DELIVERY-NUMBER-POLICY-001.md`
- Central KB architecture, integration registry and Transportation & Logistics module records
