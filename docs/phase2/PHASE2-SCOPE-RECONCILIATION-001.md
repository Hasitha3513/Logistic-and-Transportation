# Phase-2 Freight & Cargo Scope Reconciliation

**Task:** `PHASE2-SCOPE-RECON-001`  
**Decision date:** August 25, 2026  
**Status:** FROZEN  
**Source:** `D:\Traspotation & logistic.docx`, section **Freight & Cargo Manager**, stories US-24 through US-30, their use-case diagrams, acceptance-criteria register, and feature traceability register.

The DOCX does not expose stable rendered-page metadata for these sections, so source locations use the named section and story headings rather than guessed page numbers. The user-story text and traceability register name the actor **Freight Manager**; the diagrams render the same actor as **Freight & Cargo Manager**. This freeze treats the latter as a presentation label, not a separate role.

## 1. Executive summary

The authoritative source confirms eleven Phase-2 stories from US-20 through US-30. Five are complete (US-20 through US-24), and six remain (US-25 through US-30). The active Freight/Cargo map is now frozen. US-29 is reporting, US-30 is cargo exceptions, and freight charges are owned outside this sequence by US-47 — Manage Transport Billing.

## 2. Sources reviewed

- `D:\Traspotation & logistic.docx`: Freight & Cargo Manager story narratives, use-case diagrams, acceptance-criteria register, and feature/entity traceability register for US-24 through US-30.
- The same source's Finance/Billing section for US-47 — Manage Transport Billing, used only to identify authoritative Freight Charges ownership.
- `AGENTS.md` architecture and documentation governance.
- `PHASE2-GAP-001-ROUTE-FREIGHT-CARGO.md`, `PHASE2-SCOPE-MATRIX.md`, `PHASE2-IMPLEMENTATION-BACKLOG.md`, `US24-FREIGHT-ORDER-CONTRACT-001.md`, and `P2-03-FREIGHT-ORDER.md`.

## 3. Final decision

The authoritative source defines this sequence:

1. US-24 — Manage Freight Orders
2. US-25 — Manage Cargo Manifest
3. US-26 — Plan Loads
4. US-27 — Validate Weight and Volume
5. US-28 — Manage Freight Insurance
6. US-29 — Generate Freight Reports
7. US-30 — Handle Cargo Exceptions

This decision resolves `PHASE2-SCOPE-RECONCILIATION-REQUIRED`. Freight charge calculation is not assigned to US-24 through US-30. The source assigns trip costing, freight billing, applicable costs/surcharges, penalties, cost-center allocation, validation and billing finalization to **US-47 — Manage Transport Billing**, actor **Billing Officer**, related feature **Billing**. US-47 is outside this Phase-2 implementation sequence; no billing implementation is authorized by this reconciliation.

The previously approved `US24-FREIGHT-ORDER-CONTRACT-001` remains authoritative for the implemented US-24 API. Source-diagram labels such as submit and confirm describe validation/confirmation of captured requirements; they do not establish a persisted Freight Order lifecycle, status enum, or additional REST command in the frozen US-24 slice.

## 4. Authoritative story map and frozen contracts

### US-24 — Manage Freight Orders

- **Actor:** Freight Manager
- **User story:** As a Freight Manager, I want to create freight orders with priority, SLA and special-handling requirements, so that shipment requirements are documented.
- **Priority:** High
- **Related feature:** Orders / Freight Orders
- **Related sub-features:** capture shipment requirements, set priority, define SLA, special-handling requirements, validate and save the order, duplicate detection, invalid-order handling.
- **Acceptance criteria:** Freight Orders retain priority, SLA and handling requirements; mandatory shipment details are required before confirmation; rush priority is distinguishable from normal priority.
- **Frozen ownership:** customer, origin/destination, requested schedule/SLA, priority, special handling, minimal shipment lines, create/list/view/update, validation, audit and optimistic concurrency.
- **Excluded:** manifest-grade cargo, customs, hazmat, physical load placement, weight/capacity validation, insurance, reports, exceptions and a persisted order lifecycle.
- **Dependencies:** organization Customer and Location public lookups.
- **Downstream:** US-25 manifests reference saved Freight Orders; US-29 reports Freight Order data.
- **Next slice:** COMPLETE in P2-03-R1.

### US-25 — Manage Cargo Manifest

- **Actor:** Freight Manager
- **User story:** As a Freight Manager, I want to maintain manifest items, classifications, customs and hazmat information, so that cargo is traceable and compliant.
- **Priority:** High
- **Related feature:** Manifest
- **Related sub-features:** associate a Freight Order, assign manifest reference, create/view manifest, add/update cargo items, commodity classification, conditional customs and hazmat details, line and completeness validation, finalization, unmanifested-cargo detection.
- **Acceptance criteria:** A manifest contains identifiable cargo line items; commodity, customs and hazmat information can be recorded; unmanifested cargo is flagged before controlled dispatch.
- **Frozen ownership:** separate manifest aggregate and cargo items, packing metadata without invented value lists, classification, conditional customs/hazmat data, validation and finalization.
- **Excluded:** physical placement and stacking, vehicle capacity calculations, gross/net/cubic/axle validation, insurance claims, reports and exception resolution.
- **Dependencies:** saved US-24 Freight Order.
- **Downstream:** US-26 plans manifested cargo; US-27 validates its supplied measurements; US-29 reports manifest/compliance data; US-30 controls manifest-related exceptions.
- **Next slice:** P2-04-R1.

### US-26 — Plan Loads

- **Actor:** Freight Manager
- **User story:** As a Freight Manager, I want to plan weight distribution, stacking, pallet/container placement and special cargo separation, so that cargo is loaded safely.
- **Priority:** High
- **Related feature:** Load Planning
- **Related sub-features:** select manifest and vehicle/load space, retrieve manifested cargo, plan weight distribution and placement, pallet/container assignment, stacking and compatibility rules, fragile separation, temperature-controlled placement, save/replan/reject a load plan.
- **Acceptance criteria:** Load planning validates weight distribution and compatibility; fragile and temperature-sensitive placement rules can be applied; an invalid load configuration cannot be approved normally.
- **Frozen ownership:** physical cargo positioning, stacking, compatibility, separation and loading sequence within a load plan.
- **Excluded:** authoritative gross/net/cubic/axle-limit compliance calculations, manifest content, insurance, reports and exception workflow.
- **Dependencies:** finalized US-25 manifest plus focused Fleet vehicle/load-space data.
- **Downstream:** US-27 validates the resulting load configuration; US-29 reports utilization.
- **Next slice:** P2-05-R1.

### US-27 — Validate Weight and Volume

- **Actor:** Freight Manager
- **User story:** As a Freight Manager, I want gross, net, cubic and axle loading validated, so that overload and capacity violations are prevented.
- **Priority:** High
- **Related feature:** Weight/Volume
- **Related sub-features:** retrieve cargo measurements, calculate gross/net/cubic values, check vehicle capacity, retrieve applicable axle limits, validate axle load, identify missing/invalid measurements, overload, volume exceedance and axle violations, record validation outcome and require replanning or rejection.
- **Acceptance criteria:** Gross, net and cubic values derive from supplied cargo data; overweight or axle-limit violations are identified; capacity validation occurs before load approval.
- **Frozen ownership:** measurement-derived weight/volume calculations and legal/physical capacity validation of a US-26 load plan.
- **Excluded:** cargo placement/rearrangement, manifest maintenance, insurance, reports and exception resolution.
- **Dependencies:** US-25 cargo measurements, US-26 load plan, focused Fleet capacity data and an authoritative axle-limit source where configured.
- **Downstream:** US-29 reports utilization/compliance; US-30 handles weight-gap and related controlled exceptions.
- **Next slice:** P2-05-R2.

### US-28 — Manage Freight Insurance

- **Actor:** Freight Manager
- **User story:** As a Freight Manager, I want to map policies, coverage, premiums, damage assessment, claims and settlements, so that cargo risks are financially managed.
- **Priority:** Medium
- **Related feature:** Insurance
- **Related sub-features:** map/select policy, validate policy status and coverage, premium calculation when required by policy, claim creation, damage assessment, claim review/status, additional information, approval/rejection/dispute, settlement and settlement history.
- **Acceptance criteria:** Cargo/freight can be associated with a valid policy; claims retain assessment, workflow and settlement history; claim settlement requires an authorized status transition.
- **Frozen ownership:** cargo insurance policy association, coverage/premium information, damage claims, claim workflow and settlement history.
- **Excluded:** manifest editing, load planning, weight validation, reporting aggregation and general cargo-exception orchestration.
- **Dependencies:** US-24 Freight Order and/or US-25 manifested cargo; US-30 may conditionally initiate a claim.
- **Downstream:** US-29 reports claim/compliance information; US-30 may branch to an insurance claim without owning claim adjudication.
- **Next slice:** P2-06-R1.

### US-29 — Generate Freight Reports

- **Actor:** Freight Manager
- **User story:** As a Freight Manager, I want shipment, utilization, claim and compliance reports, so that freight performance and risk are visible.
- **Priority:** Medium
- **Related feature:** Reports / Freight Reports
- **Related sub-features:** reporting period and criteria, shipment-status report, capacity-utilization report, insurance-claims report, compliance report, incomplete-data flagging, compliance exceptions, high claim activity, low utilization, filter and export.
- **Acceptance criteria:** Shipment, utilization, claim and compliance information can be reported; report permissions are enforced; reporting does not alter transactional data.
- **Frozen ownership:** read-only reporting over data produced by US-24 through US-28, with explicit incomplete-data semantics and export where supported.
- **Excluded:** mutation of Freight Orders, manifests, load plans, validations, policies, claims or exceptions; freight charge/rating calculations.
- **Dependencies:** US-24 through US-28 read models and the reporting module boundary.
- **Downstream:** none within the frozen US-24 through US-30 sequence.
- **Next slice:** P2-07-R1.

### US-30 — Handle Cargo Exceptions

- **Actor:** Freight Manager
- **User story:** As a Freight Manager, I want to manage damage, partials, weight gaps, hazardous materials, unmanifested cargo and seal tampering, so that cargo exceptions are controlled.
- **Priority:** High
- **Related feature:** Freight Edge Cases
- **Related sub-features:** classify exception and severity, assess impact, determine corrective action, damage, partial shipment, weight discrepancy, hazardous-material exception, unmanifested cargo, seal tampering, cargo/manifest status, exception history, holds, escalation, rejection, release, authorized manifest correction and optional insurance claim.
- **Acceptance criteria:** Damage, partial-shipment and seal-tampering events create an auditable exception; hazardous or unmanifested cargo receives the appropriate restriction; closed exceptions retain resolution history.
- **Frozen ownership:** the common auditable exception lifecycle and its six source-defined exception types, restrictions, corrective outcomes and resolution history.
- **Excluded:** ordinary manifest editing, load-plan calculation, weight/volume calculation, insurance claim adjudication and reporting aggregation.
- **Dependencies:** US-25 manifest/cargo data; US-27 validation outcomes; conditional integration with US-28 claims; Trip dispatch-readiness boundary for holds/releases.
- **Downstream:** none within the frozen US-24 through US-30 sequence.
- **Next slice:** P2-08-R1.

## 5. Previous map, conflicts and resolution

| Story | Authoritative source title | Prior gap title | Prior backlog title | Prior matrix title | Conflict | Resolution |
|---|---|---|---|---|:---:|---|
| US-24 | Manage Freight Orders | Manage Freight Orders | Manage Freight Orders | Manage Freight Orders | No | Keep the existing reconciled US-24 contract. |
| US-25 | Manage Cargo Manifest | Manage Cargo Manifest | Manage Cargo Manifest | Manage Cargo Manifest | No | Freeze the separate manifest boundary. |
| US-26 | Plan Loads | Plan Cargo Loads | Plan Cargo Loads | Plan Cargo Loads | Yes | Rename to the exact source title; retain only source-supported load-placement scope. |
| US-27 | Validate Weight and Volume | Calculate Weight and Volume | Calculate Weight and Volume | Calculate Weight and Volume | Yes | Rename and treat calculation as supporting validation, not the owned story title. |
| US-28 | Manage Freight Insurance | Manage Freight Insurance | Manage Freight Insurance | Manage Freight Insurance | No | Expand the boundary to include source-defined claims and settlements. |
| US-29 | Generate Freight Reports | Calculate Freight Charges | Calculate Freight Charges | Calculate Freight Charges | Yes | Replace the planning-only charge story with the source-defined reporting story. |
| US-30 | Handle Cargo Exceptions | Generate Freight Reports | Generate Freight Reports | Generate Freight Reports | Yes | Move reporting to US-29 and freeze the source-defined cargo-exception story at US-30. |

Historical note: the August 24 planning documents inferred freight rating/charges at US-29 and reporting at US-30. P2-03-R1 flagged that mismatch without changing later stories. This reconciliation uses the source story register, acceptance criteria and traceability register to correct the active plan while preserving this explanation. Freight charge ownership is not unresolved: it belongs to source story US-47 and remains outside the Phase-2 scope.

### Charge ownership

The source assigns Freight Charges to **US-47 — Manage Transport Billing**, actor **Billing Officer**, related feature **Billing**. Its user story owns trip costing, freight billing, surcharges, penalties and cost-center allocation. Its acceptance criteria require applicable cost/surcharge calculation, separately visible penalties/cost-center allocation, and audited final billing changes. This is evidence of ownership only; US-47 implementation is not part of Phase-2 or this task.

### Report ownership

**US-29 — Generate Freight Reports** owns read-only shipment, utilization, claim and compliance reports. It consumes existing facts, enforces report permissions and never mutates source transactions.

### Cargo exception ownership

**US-30 — Handle Cargo Exceptions** owns the common auditable lifecycle for the six source-defined exception types: damage, partial shipment, weight discrepancy, hazardous-material exception, unmanifested cargo and seal tampering. It does not absorb normal Manifest, Load Plan, Weight/Volume or Insurance behavior.

## 6. API ownership

No API is implemented by this document. The frozen ownership is:

| Story | Planned API responsibility |
|---|---|
| US-24 | Existing paginated order list, create, detail and optimistic update only. |
| US-25 | Manifest list/create/detail/update, nested cargo-item add/update, and explicit finalize command. |
| US-26 | Load-plan list/create/detail/update and placement/compatibility validation. |
| US-27 | Explicit weight/volume validation command for a load plan and read-only validation result. |
| US-28 | Insurance policy association and explicit claim assessment/status/settlement commands. |
| US-29 | Read-only freight report queries, filters and export. |
| US-30 | Exception list/create/detail plus explicit hold, escalate, reject, release and resolve commands where supported by the final domain model. |

There is no Phase-2 freight-charge endpoint under US-24 through US-30. A future billing endpoint belongs to US-47 and requires its own contract; this task does not propose its path.

## 7. Frontend ownership

| Story | Feature path | Responsibility |
|---|---|---|
| US-24 | `freight/orders` | Existing list/create/details/edit experience. |
| US-25 | `freight/manifests` | Manifest list/create/details/edit, cargo items, validation and finalization. |
| US-26 | `freight/loadPlanning` | Physical placement, stacking, separation and compatibility planning. |
| US-27 | `freight/loadPlanning` validation section | Weight, volume, capacity and axle-result presentation; no duplicated calculator in the order form. |
| US-28 | `freight/insurance` | Policies, claims, assessment and settlement workflow. |
| US-29 | `freight/reports` | Read-only shipment, utilization, claim and compliance reporting. |
| US-30 | `freight/exceptions` | Exception intake, restrictions, corrective action and resolution history. |

`freight/charges` is removed from the active US-24 through US-30 plan. Future billing UI belongs to US-47 and requires its own feature/module decision.

## 8. Database ownership

- **Current latest migration:** `V31__freight_order_foundation.sql`.
- **Next future migration owner:** P2-04-R1 / US-25 Cargo Manifest, using the next available version only after rechecking the chain when implementation begins.
- US-26 owns only load-plan persistence.
- US-27 owns only durable validation results or authoritative measurement data proven necessary by its implementation contract.
- US-28 owns insurance policies, claims and settlements.
- US-29 should prefer read projections over transactional tables; any projection migration belongs to reporting.
- US-30 owns cargo-exception and resolution-history persistence.
- No migration is reserved for freight charges in this story range. Future billing persistence belongs conceptually to US-47 and is not numbered or designed here.

## 9. E2E ownership plan

Every listed logical scenario must run on Chromium, Firefox and WebKit when its story is implemented.

| Story | Logical E2E cases | Dependencies |
|---|---|---|
| US-24 | Existing `E2E-P2-FRT-001` through `006`: create, details, update, validation, view-only RBAC and direct 403. | Organization lookups and implemented Freight Order API. |
| US-25 | Create manifest from Freight Order; add/update cargo item; conditional customs; conditional hazmat; validation rejection; finalization/edit restriction; view/manage/finalize RBAC. | US-24. |
| US-26 | Create load plan; assign positions; enforce fragile/temperature separation; detect compatibility conflict; replan invalid configuration; RBAC. | Finalized US-25 manifest and Fleet lookup. |
| US-27 | Validate gross/net/cubic values; detect overload; detect volume exceedance; detect axle violation; surface missing measurements; require replanning; RBAC. | US-25, US-26 and Fleet capacity/axle data. |
| US-28 | Associate valid policy; reject insufficient/invalid coverage; create and assess claim; authorized claim status transition; partial/full settlement history; RBAC. | US-24/US-25; optional US-30 trigger. |
| US-29 | Shipment report; utilization report; claims report; compliance report; incomplete-source indicator; filter/export; report RBAC and read-only proof. | Read models from US-24 through US-28. |
| US-30 | Damage exception; partial shipment; weight discrepancy; hazardous exception; unmanifested cargo hold; seal tampering; authorized correction/claim branch; resolution-history retention; RBAC. | US-25, US-27, US-28 and Trip readiness boundary. |

## 10. Frozen acceptance criteria

The source supplies three concise acceptance statements per story. The Given/When/Then form below is a traceability restatement of those statements, not additional business behavior.

| AC ID | Given | When | Then |
|---|---|---|---|
| US24-AC1 | A Freight Order captures shipment requirements | It is saved or retrieved | Priority, SLA and handling requirements are retained. |
| US24-AC2 | A Freight Order lacks mandatory shipment details | Confirmation/readiness validation is requested | Validation fails and the order is not treated as confirmed/ready. |
| US24-AC3 | Normal and rush demand exist | Priority is recorded or displayed | Rush priority is distinguishable from normal priority. |
| US25-AC1 | A manifest is created for a Freight Order | Cargo is recorded | The manifest contains identifiable cargo line items. |
| US25-AC2 | Cargo requires classification, customs or hazmat data | The manifest item is maintained | Applicable commodity, customs and hazmat information can be recorded. |
| US25-AC3 | Known cargo is absent from the manifest | Controlled-dispatch readiness is evaluated | The unmanifested cargo is flagged. |
| US26-AC1 | Manifested cargo is being positioned | A load plan is validated | Weight distribution and cargo compatibility are checked. |
| US26-AC2 | Fragile or temperature-sensitive cargo is present | Placement is planned | Applicable separation or temperature-placement rules can be applied. |
| US26-AC3 | A load configuration violates mandatory placement/compatibility rules | Normal approval is attempted | The invalid configuration cannot be approved normally. |
| US27-AC1 | Supplied cargo measurements exist | Weight and volume are validated | Gross, net and cubic values derive from those supplied measurements. |
| US27-AC2 | A load exceeds weight or applicable axle limits | Validation runs | The overweight or axle-limit violation is identified. |
| US27-AC3 | A load awaits approval | Approval readiness is evaluated | Capacity validation occurs before approval. |
| US28-AC1 | Freight/cargo has insurance requirements | A policy is associated | The association requires a valid policy. |
| US28-AC2 | An insurance claim is processed | Assessment, workflow or settlement changes occur | Assessment, workflow and settlement history are retained. |
| US28-AC3 | A claim settlement transition is requested | The actor lacks required authority | The settlement transition is rejected. |
| US29-AC1 | US-24 through US-28 have produced source data | A freight report is generated | Shipment, utilization, claim and compliance information can be reported. |
| US29-AC2 | A user requests a freight report | Authorization is evaluated | Report permissions are enforced. |
| US29-AC3 | A report query executes | Results are calculated or exported | Source transactional data is not modified. |
| US30-AC1 | Damage, partial shipment or seal tampering occurs | An exception is recorded | An auditable cargo exception is created. |
| US30-AC2 | Hazardous or unmanifested cargo is detected | Exception policy is evaluated | The appropriate restriction is applied. |
| US30-AC3 | A cargo exception is resolved and closed | Its history is later retrieved | Resolution history remains retained. |

## 11. Module ownership

| Story | Owning module | Boundary |
|---|---|---|
| US-24 | `freight` | Implemented `order` feature; organization references through public lookups. |
| US-25 | `freight` | Future `manifest` feature; no Trip/Fleet repository access. |
| US-26 | `freight` | Future load-planning feature using focused Fleet data. |
| US-27 | `freight` | Validation policy/results within the load-planning capability; Fleet limits via a public boundary. |
| US-28 | `freight` | Future insurance policy/claim capability. |
| US-29 | `reporting` | Reporting owns queries/projections and consumes public Freight read contracts. |
| US-30 | `freight` | Future cargo-exception capability; Trip readiness and insurance integration through focused boundaries/events. |

US-47 owns Freight Charges at the story level. Its future module placement requires a separate architecture contract because no Billing top-level module is authorized by this task.

## 12. Security ownership

No permission is added by this audit.

| Story | Frozen/proposed permissions | Decision |
|---|---|---|
| US-24 | `FREIGHT_ORDER_VIEW`, `FREIGHT_ORDER_MANAGE` | Already implemented. |
| US-25 | `CARGO_MANIFEST_VIEW`, `CARGO_MANIFEST_MANAGE`, `CARGO_MANIFEST_FINALIZE` | Separate view, mutable-draft and finalization authority. |
| US-26 | `LOAD_PLAN_MANAGE` | Owns physical plan mutation; later contract may add a view permission only if repository convention requires it. |
| US-27 | `LOAD_PLAN_MANAGE` | Validation is part of controlled load-plan management; do not reuse Freight Order authority. |
| US-28 | `CARGO_INSURANCE_MANAGE` | Proposed planning authority; claim-action splits require the US-28 contract. |
| US-29 | `FREIGHT_REPORT_VIEW` | Read-only report authority. |
| US-30 | `CARGO_EXCEPTION_VIEW`, `CARGO_EXCEPTION_MANAGE` | A separate permission family is required because exception restriction/resolution is not manifest editing. |

`FREIGHT_CHARGE_MANAGE` belongs conceptually to US-47 and is removed from this Phase-2 permission plan.

## 13. Test ownership

| Story | Domain tests | Application tests | Persistence tests | Controller tests | Frontend tests | Playwright ownership |
|---|---|---|---|---|---|---|
| US-24 | Order/line invariants | Create/update/lookups/concurrency | Lines, paging, version | CRUD validation/RBAC/errors | List/form/lines/details/edit/RBAC/errors | Existing FRT-001–006 on all browsers |
| US-25 | Item, conditional data, completeness, finalization/edit restrictions | Freight Order lookup, validation, finalize transaction/concurrency | Aggregate/items/audit/version | List/create/detail/update/items/finalize/RBAC/errors | List/form/items/details/finalize/RBAC/errors | Seven logical cases in section 9 on all browsers |
| US-26 | Placement, stacking, compatibility, special separation | Plan/replan/reject orchestration | Load plan and placement ordering | Plan commands, conflict diagnostics, RBAC | Placement/replan/diagnostics/RBAC | Six logical cases in section 9 on all browsers |
| US-27 | Gross/net/cubic and capacity/axle policies | Data retrieval, validation outcome, replan requirement | Measurements/results only if persisted | Validate/result/error/RBAC | Validation results and correction guidance | Seven logical cases in section 9 on all browsers |
| US-28 | Policy, claim transition and settlement rules | Association, assessment, authorization and settlement | Policy/claim/history/version | Policy/claim commands/RBAC/errors | Policy/claim/settlement/RBAC/errors | Six logical cases in section 9 on all browsers |
| US-29 | Aggregation and incomplete-data semantics | Read-port orchestration/non-mutation | Projection integration where required | Query/filter/export/RBAC | Report/filter/export/empty/incomplete/RBAC | Seven logical cases in section 9 on all browsers |
| US-30 | Six exception types, restrictions and resolution | Intake, hold/release, correction/claim branch | Exception and resolution history/version | Explicit commands/RBAC/errors | Intake/actions/history/RBAC/errors | Eight logical cases in section 9 on all browsers |

## 14. Updated implementation sequence

Completed task identities remain unchanged:

- P2-01: US-21 and US-23 — COMPLETE
- P2-02: US-20 and US-22 — COMPLETE
- P2-03/P2-03-R1: US-24 — COMPLETE

Remaining sequence:

1. P2-04-R1 — US-25 Manage Cargo Manifest
2. P2-05-R1 — US-26 Plan Loads
3. P2-05-R2 — US-27 Validate Weight and Volume
4. P2-06-R1 — US-28 Manage Freight Insurance
5. P2-07-R1 — US-29 Generate Freight Reports
6. P2-08-R1 — US-30 Handle Cargo Exceptions

Phase-2 remains 11 stories (US-20 through US-30): 5 complete and 6 remaining.

## 15. Unresolved questions

There is no blocking story-ID or ownership ambiguity. Implementation contracts must still select source-backed value catalogues for packing, commodity, customs, hazmat and axle limits rather than inventing values. The future architectural placement and public API for US-47 Billing are deliberately outside this reconciliation.

## 16. Freeze rules

- The story titles, actor, user-story text, acceptance criteria and ownership boundaries above are frozen for Phase-2 planning.
- A future implementation may refine DTO fields and endpoint shapes only within the owned capability and repository architecture.
- It may not move behavior between these stories, invent freight charges in this range, add manifest behavior to US-24, or move weight/capacity validation into US-26 without an approved contract amendment.
- P2-04-R1 must begin by reading this document and `US25-CARGO-MANIFEST-CONTRACT-001.md`.
