# US26 Load Planning Contract Correction

**Task:** `P2-LOAD-CONTRACT-CORRECTION-001`  
**Status:** FROZEN FOR IMPLEMENTATION  
**Story status:** US-26 remains **PARTIAL**  
**Scope:** Contract correction and implementation plan only; no production code, migration, permission, or test change

## Problem

The implemented Load Planning feature cannot currently prove US26-AC2 or US26-AC3:

- `CargoManifestItem` and `CargoManifestLookupPort.ManifestItemPlanningView` expose hazardous classification but no structured fragile or temperature-sensitive facts.
- `LoadPlan` treats planner-authored `specialHandlingNotes` containing `FRAGILE` or `TEMPERATURE` as authoritative classification.
- create and update persist work without structural validation, while `validate-layout` is diagnostic only.
- the load plan has no readiness state or explicit command that prevents a structurally invalid configuration from proceeding normally.

Free text is unsuitable as a mandatory safety input because spelling, language, omission, and incidental keyword use can change validation results. The correction must remain additive and must not move weight, volume, payload, GVW, or axle validation from US-27 into US-26.

## Evidence

The current source establishes the following baseline:

| Area | Current evidence | Contract consequence |
| :--- | :--- | :--- |
| Manifest item | Structured customs and hazardous fields only | Manifest requires two additive special-cargo facts |
| Load Planning lookup | No fragile or temperature fields | Focused lookup view must expose both facts read-only |
| Fragile validation | Substring search in placement notes | Replace with manifest-owned structured classification |
| Temperature validation | Substring search in placement notes | Replace with manifest-owned structured classification |
| Draft persistence | create/update save without layout validation | Preserve work-in-progress draft saves |
| Structural validation | Explicit diagnostic endpoint | Reuse the policy inside a transactional ready command |
| Concurrency | load-plan update uses `version` and returns conflict for stale data | Ready command must require the expected version |
| Schema chain | Current latest migration is `V36` | First implementation migration must use the next available version after rechecking the chain |

## US26-AC2 Interpretation

US26-AC2 requires structured knowledge of whether manifested cargo is fragile or temperature-sensitive and enforceable placement rules based on the real Load Plan concepts. It does not require numeric temperature ranges, refrigeration telemetry, a hazardous-material compatibility matrix, or US-27 capacity calculations.

### Special Cargo Ownership

US-25 `CargoManifestItem` owns execution-grade cargo classification facts. Load Planning consumes those facts but cannot redefine them.

| Fact | Owner | Field | Type | Required | Initial/default behavior | Validation |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Fragile classification | Cargo Manifest item | `fragile` | nullable boolean | Required to be known before manifest finalization for new/edited items | Existing rows migrate as `NULL`/UNKNOWN; no text parsing | `TRUE`, `FALSE`, or historical `UNKNOWN`; finalization rejects UNKNOWN for newly classifiable data |
| Temperature-sensitive classification | Cargo Manifest item | `temperatureSensitive` / SQL `temperature_sensitive` | nullable boolean | Required to be known before manifest finalization for new/edited items | Existing rows migrate as `NULL`/UNKNOWN; no ranges or units | `TRUE`, `FALSE`, or historical `UNKNOWN`; finalization rejects UNKNOWN for newly classifiable data |

Boolean classification is the smallest source-supported representation. Numeric minimum/maximum temperatures and units are not authorized because the current source does not define cold-chain ranges.

### Free-Text Notes

`specialHandlingNotes` remains optional descriptive operator information. It is **NON-AUTHORITATIVE** and must never determine fragile or temperature-sensitive classification, readiness, or a mandatory violation. Future authoritative logic must remove keyword/sub-string matching.

### Manifest Boundary

- Manifest create remains aggregate-only; items continue to be added or updated through item commands.
- Manifest item create/update requests and responses gain additive nullable `fragile` and `temperatureSensitive` fields.
- Manifest item writes accept omitted/null classifications as UNKNOWN for wire compatibility, but the first-party UI requires an explicit `true` or `false` choice and Manifest finalization rejects UNKNOWN.
- Manifest readiness/finalization includes `SPECIAL_CARGO_CLASSIFICATION_MISSING` for any item whose classification remains UNKNOWN.
- Existing customs and hazardous semantics remain unchanged.
- A finalized manifest remains read-only under the current US-25 contract. Correcting historical finalized UNKNOWN classifications requires the separately governed post-finalization correction capability; this task does not implement or absorb US-30.

### Load Planning Boundary

`CargoManifestLookupPort.ManifestItemPlanningView` gains nullable `fragile` and `temperatureSensitive` facts. The adapter maps them from the finalized manifest. Load Planning reads them and never mutates manifest quantity, packing, commodity, customs, hazardous, fragile, or temperature data.

UNKNOWN classification produces `LOAD_PLAN_SPECIAL_CARGO_CLASSIFICATION_MISSING` and blocks structural readiness. It does not prevent saving a draft.

### Minimum Enforceable Placement Rules

The rules intentionally use only existing logical placement concepts:

1. **Fragile separation:** a fragile item must not have a nonblank `stackGroup` shared with any other placement. An unstacked fragile item is compliant. This contract does not infer vertical ordering or load-bearing capacity.
2. **Temperature-sensitive separation:** every temperature-sensitive item must have a nonblank `zoneReference`; all placements sharing that logical zone must also be temperature-sensitive. Mixing temperature-sensitive and standard cargo in one zone is invalid.
3. A logical temperature zone proves separation only. It does **not** claim refrigeration capability, a maintained temperature, or a numeric range. Those capabilities require a separately approved Fleet/load-space contract.
4. Hazard compatibility continues to use structured manifest hazardous data and the existing conservative rule: hazardous and non-hazardous items may not share a stack group or logical zone. No unapproved hazmat matrix is introduced.

## US26-AC3 Interpretation

US26-AC3 permits incomplete or invalid work to be saved as a draft but forbids it from becoming structurally ready. `STRUCTURALLY_READY` is deliberately distinct from final operational approval: US-27 still owns weight/volume/capacity validation required before final approval.

### Readiness Lifecycle

```text
DRAFT
  | explicit ready command + all US-26 structural checks pass
  v
STRUCTURALLY_READY
  | any material plan mutation or authoritative input invalidation
  v
DRAFT
```

- New plans are `DRAFT`.
- Create and update may persist incomplete or structurally invalid drafts.
- `POST /v1/freight/load-plans/{id}/ready` is the sole transition to `STRUCTURALLY_READY`.
- The command authorizes the caller, requires an expected version, reloads the current finalized manifest and vehicle through approved ports, evaluates all mandatory structural checks, and persists state plus audit metadata atomically.
- Violations return a stable conflict/business-validation response and leave the plan `DRAFT` without partial state changes.
- This lifecycle has no generic status mutation endpoint and no final `APPROVED` state. Final approval remains dependent on the separately corrected US-27 contract.

### Mandatory Structural Checks Before Ready

- manifest exists and remains finalized;
- selected vehicle exists and remains active;
- every manifest item is placed exactly once;
- no placement refers to an item outside the manifest;
- placement order values are unique and nonnegative;
- loading sequence values are unique and nonnegative;
- hazardous stack-group and zone compatibility rules pass;
- fragile stack separation passes using structured manifest data;
- temperature-sensitive logical-zone separation passes using structured manifest data;
- fragile and temperature classifications are known for every manifest item.

Payload, gross/net weight, volume, GVW, axle limits, refrigeration performance, and legal capacity are explicitly excluded.

### Mutation and Invalidation

- A material update to vehicle, placements, placement order, zone, stack group, container reference, loading sequence, or manifest-bound planning facts automatically returns the plan to `DRAFT` and clears `readyAt`/`readyBy`.
- Notes-only edits do not affect structural readiness because notes are non-authoritative; they still use optimistic concurrency and audit.
- The current finalized manifest cannot ordinarily change. A future authorized manifest correction that changes a planning fact must publish/trigger an internal invalidation so every affected ready plan returns to `DRAFT` before further operational use.
- The ready command always rereads authoritative inputs, preventing approval from relying solely on an earlier diagnostic result.

### Concurrency and Audit

- Ready requests include required `version`; a stale version returns HTTP `409` using repository conflict conventions (`LOAD_PLAN_STALE_VERSION` or the existing equivalent).
- Successful readiness increments the aggregate version.
- Add `readiness_status`, `ready_at`, and `ready_by` to Load Plan persistence.
- `ready_at` and `ready_by` are both null in `DRAFT` and both non-null in `STRUCTURALLY_READY`; enforce the pair with a database check.
- Existing `createdAt/By`, `updatedAt/By`, and `version` conventions remain authoritative. Reopen metadata is unnecessary because material updates and normal audit fields provide the minimum required trail.

## API Changes

All changes are additive except removal of keyword semantics from internal validation.

| Contract | Proposed change |
| :--- | :--- |
| Manifest item create/update request | Add optional nullable JSON fields `fragile` and `temperatureSensitive`; omitted/null means UNKNOWN and preserves existing clients |
| Manifest item response | Add nullable `fragile` and `temperatureSensitive` so historical UNKNOWN remains representable |
| Manifest readiness response | Add stable `SPECIAL_CARGO_CLASSIFICATION_MISSING` failures where applicable |
| Load Planning lookup port | Add nullable `fragile` and `temperatureSensitive` to `ManifestItemPlanningView` |
| Load Plan response | Add `readinessStatus`, `readyAt`, and `readyBy` |
| Ready command | Add `POST /v1/freight/load-plans/{id}/ready` with `{ "version": <long> }` |
| Layout validation response | Preserve diagnostic endpoint; return structured special-cargo and UNKNOWN-classification violations |

Conceptual stable errors/violations are:

- `LOAD_PLAN_STRUCTURAL_VIOLATIONS`
- `LOAD_PLAN_FRAGILE_RULE_FAILED`
- `LOAD_PLAN_TEMPERATURE_RULE_FAILED`
- `LOAD_PLAN_SPECIAL_CARGO_CLASSIFICATION_MISSING`
- `LOAD_PLAN_STALE_VERSION`

The implementation must map these through the existing API error envelope and retain field-level details where appropriate.

## Security

The existing `LOAD_PLAN_MANAGE` permission is sufficient for marking a plan structurally ready because readiness is validation of the same planning work, not independent managerial or financial approval. `LOAD_PLAN_VIEW` remains read-only. No new permission or role is justified by the current source.

Future final operational approval, if separated by responsibility after US-27 is corrected, may require its own permission under that later contract. This document does not pre-authorize one.

## Database Plan

A forward migration is required. The current migration chain ends at `V36`; the implementation task must recheck the chain and use the next unclaimed version rather than editing V32 or V34.

Conceptual additive changes:

```sql
ALTER TABLE cargo_manifest_item
    ADD COLUMN fragile BOOLEAN NULL,
    ADD COLUMN temperature_sensitive BOOLEAN NULL;

ALTER TABLE load_plan
    ADD COLUMN readiness_status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN ready_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN ready_by VARCHAR(128) NULL;
```

Required constraints:

- `readiness_status IN ('DRAFT', 'STRUCTURALLY_READY')`;
- `ready_at` and `ready_by` are either both null or both populated;
- `STRUCTURALLY_READY` requires both readiness audit fields;
- `DRAFT` requires both readiness audit fields to be null.

Do not add physical cross-module foreign keys, tenant fields, temperature ranges, or an approval-history table in this correction. Tenant remediation remains paused and requires its own approved forward-migration plan.

## Legacy Data

- Existing manifest rows receive `NULL` for both classifications, meaning UNKNOWN.
- No migration may parse descriptions, packing information, hazardous details, or `specialHandlingNotes` to derive authoritative values.
- Existing load plans become `DRAFT`; historical validation diagnostics remain non-authoritative.
- A draft may be viewed and edited despite UNKNOWN classifications, but it cannot become structurally ready.
- Existing finalized manifests with UNKNOWN values remain ineligible for readiness until an authorized post-finalization data-correction mechanism exists. The UI must explain this condition rather than silently treating UNKNOWN as false.
- No historical row is automatically represented as safe.

## Frontend Contract

### Cargo Manifest

- Add required `Fragile` and `Temperature sensitive` checkboxes/switches to the item editor for new or editable manifest items.
- Display explicit `FRAGILE`, `TEMPERATURE SENSITIVE`, and `CLASSIFICATION REQUIRED` indicators in item details.
- Map UNKNOWN readiness failures to clear corrective guidance.
- Do not ask for temperature ranges and do not infer state from any text field.

### Load Planning

- Display Manifest-owned fragile/temperature classifications as read-only facts beside each item.
- Preserve zone, stack group, container, placement-order, loading-sequence, and descriptive-notes controls.
- Show `DRAFT` or `STRUCTURALLY READY` and readiness audit metadata.
- Provide an authorized `Mark structurally ready` action that submits the current version.
- Present structured violations when readiness fails; do not expose raw backend exceptions.
- Any material replan updates the UI to `DRAFT`; notes-only updates retain the returned readiness state.
- A view-only user can inspect classification and readiness but cannot save, replan, or mark ready.

## Test Contract

Future backend and frontend coverage must prove:

- structured fragile and temperature fields round-trip through Manifest domain, API, persistence, and UI;
- UNKNOWN classification blocks Manifest finalization for new data and Load Plan readiness for legacy data;
- free text containing or omitting keywords never changes classification or validation;
- fragile cargo cannot share a stack group;
- temperature-sensitive cargo requires a nonblank exclusive logical zone;
- compatible hazardous and ordinary configurations follow the frozen conservative rule;
- incomplete or invalid drafts can be saved;
- all structural checks execute transactionally before readiness;
- invalid readiness leaves state and audit unchanged;
- valid readiness records actor/time and increments version;
- material replan invalidates readiness while notes-only mutation does not;
- stale update and stale readiness return `409`;
- `LOAD_PLAN_VIEW` cannot mutate or mark ready and direct unauthorized calls return `403`;
- persistence constraints and a clean Flyway migration chain pass;
- migration leaves historical classifications UNKNOWN and never parses free text.

## Playwright Contract

The acceptance implementation must provide a dedicated cross-browser suite covering:

1. `E2E-P2-LOAD-001` — structured fragile cargo rule;
2. `E2E-P2-LOAD-002` — structured temperature-sensitive cargo rule;
3. `E2E-P2-LOAD-003` — invalid draft saves but cannot become ready;
4. `E2E-P2-LOAD-004` — valid plan becomes structurally ready;
5. `E2E-P2-LOAD-005` — material edit invalidates readiness;
6. `E2E-P2-LOAD-006` — view-only user cannot mark ready;
7. `E2E-P2-LOAD-007` — direct unauthorized ready command returns `403`;
8. `E2E-P2-LOAD-008` — stale ready command returns `409`.

All logical cases must pass on Chromium, Firefox, and WebKit, followed by the complete approved Playwright regression. These cases must not assert US-27 payload/GVW/volume/axle compliance.

## Definition of Done

This correction is implemented only when:

- Manifest owns structured nullable fragile and temperature-sensitive facts;
- first-party new writes make both facts explicit, compatible clients may send UNKNOWN, and finalization prevents UNKNOWN from being treated as complete;
- free text is non-authoritative;
- the focused Manifest lookup exposes structured facts read-only;
- the minimum stack and logical-zone rules are enforced;
- drafts remain saveable;
- only a structurally valid current version can become `STRUCTURALLY_READY`;
- material edits and changed authoritative inputs invalidate readiness;
- US-27 remains owner of capacity validation and final approval prerequisites;
- API, persistence, frontend, RBAC, unit/integration, migration, and cross-browser contracts pass;
- US-26 remains PARTIAL until the dedicated acceptance task closes every gate;
- tenant work remains paused and US-30 remains separate.

## Implementation Sequence

1. **P2-LOAD-CORR-001 — Manifest structured special-cargo fields:** implement additive domain/persistence fields, UNKNOWN-safe forward migration, finalization validation, and migration tests.
2. **P2-LOAD-CORR-002 — Manifest API/frontend capture:** add request/response fields, form controls, indicators, readiness feedback, and focused tests.
3. **P2-LOAD-CORR-003 — Load Planning lookup/domain refactor:** expose structured facts through the focused port, remove keyword authority, and enforce frozen stack/zone/UNKNOWN rules.
4. **P2-LOAD-CORR-004 — Structural readiness boundary:** add `DRAFT`/`STRUCTURALLY_READY`, ready command, audit, concurrency, atomic revalidation, and material-change invalidation.
5. **P2-LOAD-CORR-005 — Backend/frontend acceptance tests:** close domain, application, persistence, controller, RBAC, concurrency, migration, and UI regression coverage.
6. **P2-LOAD-CORR-006 — Dedicated Playwright acceptance closure:** execute the eight logical cases across all configured browsers, full regression, and then resume `P2-LOAD-ACCEPTANCE-001` for the final status decision.
