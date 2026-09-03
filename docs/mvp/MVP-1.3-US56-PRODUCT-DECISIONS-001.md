# US-56 Delivery Order Product Decisions

**Task:** `MVP-1.3-US56-PRODUCT-DECISIONS-001`  
**Date:** 2026-08-28  
**Status:** COMPLETE  
**Implementation:** Not performed  
**US-56:** `READY_FOR_IMPLEMENTATION`

## 1. Source Evidence

`docs/requirements/Traspotation & logistic.docx` defines US-56 as maintaining delivery priority, service type, time windows and instructions. Its acceptance criteria require those values to be stored, require customer/location validation, and prohibit invalid orders from progressing to assignment. It does not define priority values, service-type values, an assignment target, or assignment eligibility facts.

The frozen Delivery contract assigns Delivery Order ownership to Delivery and identifies the missing catalogues as implementation-time decisions. Existing Trip priority strings and Freight service-level/priority strings are not authoritative taxonomies and have different bounded-context meanings. Rider assignment belongs to later Last-Mile scope (notably US-65/66); Trip driver/vehicle assignment remains Trip-owned.

## 2. Delivery Priority Catalogue

| Machine value | Label | Meaning | Order | Provenance |
| :--- | :--- | :--- | ---: | :--- |
| `LOW` | Low | Work may be sequenced after normal commitments within its delivery window. | 10 | `PRODUCT_DECISION` |
| `NORMAL` | Normal | Ordinary delivery urgency and the default. | 20 | `PRODUCT_DECISION` |
| `HIGH` | High | Preferential operational attention within the agreed window. | 30 | `PRODUCT_DECISION` |
| `URGENT` | Urgent | Highest recorded urgency; operators should prioritize planning attention. | 40 | `PRODUCT_DECISION` |

- Owner: Delivery bounded context (`ARCHITECTURAL_DECISION`).
- Representation: stable Delivery-owned enum/value object; not configurable in MVP 1.3.
- API field: optional on create with server default `NORMAL`; non-null in domain/persistence/response.
- Mutability: editable in `DRAFT` and `READY_FOR_ASSIGNMENT`; changing it returns the order to `DRAFT` for revalidation. Immutable after later assignment/execution begins.
- US-56 effect: record, validate, display, filter and sort by explicit ordinal only. It does not independently determine eligibility, pricing, SLA, route, capacity, or scheduling. Later batching/scheduling may consume it under separately approved rules.
- Unknown/inactive behavior: unsupported values are rejected; no inactive state exists in the fixed MVP catalogue.

## 3. Delivery Service-Type Catalogue

| Machine value | Label | Definition | Provenance |
| :--- | :--- | :--- | :--- |
| `STANDARD` | Standard | Ordinary delivery service within the explicit delivery window. | `PRODUCT_DECISION` |
| `EXPRESS` | Express | Expedited service classification within the explicit delivery window. | `PRODUCT_DECISION` |
| `SAME_DAY` | Same Day | Service intended for completion on the operational day selected by the explicit window. | `PRODUCT_DECISION` |
| `SCHEDULED` | Scheduled | Service planned for a customer-agreed explicit window. | `PRODUCT_DECISION` |

- Owner: Delivery bounded context (`ARCHITECTURAL_DECISION`).
- Representation: stable Delivery-owned enum/value object; configuration/master-data management is deferred.
- API field: optional on create with server default `STANDARD`; non-null in domain/persistence/response.
- Mutability: same rules as priority; a change returns a ready order to `DRAFT`.
- Ordering: none; service types are categories, not ranks.
- `AFFECTS_US56`: storage, validation, display and filtering. Every type requires a valid explicit delivery window.
- `DEFERRED_TO_LATER_SCOPE`: scheduling capacity, SLA measurement, route selection, POD, re-delivery and analytics.
- `NO_EFFECT`: priority, assignment eligibility and pricing. No type silently changes another field.
- `SAME_DAY` is a classification only in US-56; no tenant-time-zone same-calendar-day rule is invented in this slice.

## 4. Assignment Target and Scope

Final model: `NONE_IN_US56` (`ARCHITECTURAL_DECISION`).

- Assignment is not a US-56 use case. The source requires readiness validation before assignment, not assignment implementation.
- Creation never requires assignment; an order remains unassigned.
- US-56 exposes `validate-readiness`, which may move `DRAFT` to `READY_FOR_ASSIGNMENT` but does not choose or persist a target.
- No reassignment, assignment lifecycle, generic `targetType + targetId`, Rider/Courier, Driver, Vehicle, Trip, Route, Delivery Run or composite assignment is implemented.
- Target ownership and eligibility are deferred to a source-backed assignment/Last-Mile contract. US-57/58 Rider execution must not be pulled into US-56.
- Persistence decision: `NO_ASSIGNMENT_COLUMNS_IN_US56`.

## 5. US-56 Readiness Facts

These are order-readiness facts, not assignment-target eligibility:

| Fact | Classification | Owner | Consumption | Unknown behavior |
| :--- | :--- | :--- | :--- | :--- |
| Current Tenant established | `REQUIRED_NOW` | Tenancy | `CurrentTenant` / `TenantExecutionContext` | `FAIL_CLOSED` |
| Customer exists, active, same Tenant | `REQUIRED_NOW` | Organization | Delivery customer lookup port/public adapter | `FAIL_CLOSED` |
| Origin location exists, active, same Tenant | `REQUIRED_NOW` | Organization | Delivery location lookup port/public adapter | `FAIL_CLOSED` |
| Destination location exists, active, same Tenant | `REQUIRED_NOW` | Organization | Delivery location lookup port/public adapter | `FAIL_CLOSED` |
| Origin differs from destination | `REQUIRED_NOW` | Delivery | Local invariant | Reject |
| Window start is strictly before window end | `REQUIRED_NOW` | Delivery | Local invariant | Reject |
| Priority/service type are supported | `REQUIRED_NOW` | Delivery | Local catalogue | Reject |
| Driver/Rider/Vehicle/Trip/Route availability, licence, documents, capacity, workload or conflicts | `DEFERRED` | Owning future provider | No US-56 call | `NOT_APPLICABLE` |

Cross-tenant and unknown external references return tenant-safe validation/not-found behavior without existence leakage. `UNKNOWN` is never converted to eligible.

## 6. Lifecycle

- Initial status: `DRAFT` (`ARCHITECTURAL_DECISION`).
- Successful readiness validation: `DRAFT -> READY_FOR_ASSIGNMENT`.
- Readiness validates customer, locations, window, priority, service type and required order data.
- Priority, service type, window, instructions and references may be changed in `DRAFT` or `READY_FOR_ASSIGNMENT`; any change produces `DRAFT` and requires revalidation.
- Assignment does not occur in US-56. States from `ASSIGNED` onward remain future lifecycle placeholders and make US-56 requirement fields immutable when later implemented.
- Cancellation is not required by the US-56 source requirements and is deferred rather than expanded through this decision task.

## 7. API, Database, Frontend and RBAC Impact

| Field/action | Contract |
| :--- | :--- |
| `priority` | Enum string; create optional/default `NORMAL`; update optional; unsupported/null-on-update rejected; mutable only before assignment |
| `serviceType` | Enum string; create optional/default `STANDARD`; update optional; unsupported/null-on-update rejected; mutable only before assignment |
| Assignment reference | Absent from US-56 requests, responses and persistence |
| Readiness | Explicit command with optimistic `version`; requires `DELIVERY_ASSIGN`; validates only order readiness |

Database: `priority VARCHAR(...) NOT NULL DEFAULT 'NORMAL'` and `service_type VARCHAR(...) NOT NULL DEFAULT 'STANDARD'` with checks for the frozen catalogues. No assignment columns. Tenant-scoped Delivery Order schema remains required in the implementation task.

Frontend: required single-select controls with the four labels above; defaults Normal and Standard; no free text, remote catalogue, assignment selector or eligible-target query. Backend validation remains authoritative.

RBAC: create uses `DELIVERY_CREATE`, edits use `DELIVERY_UPDATE`, reads use `DELIVERY_VIEW`, and readiness validation uses the already-frozen `DELIVERY_ASSIGN`. No new permission is introduced.

## 8. Test Contract

- Accept every priority/service-type value; reject unsupported and explicit invalid/null update values; prove create defaults.
- Prove priority ordering is explicit and has no automatic eligibility effect.
- Prove every service type stores without implicit priority, pricing, routing, SLA, POD or assignment changes.
- Prove edits in `DRAFT`/`READY_FOR_ASSIGNMENT` return status to `DRAFT`; later assigned/execution states reject edits.
- Prove readiness succeeds only with current Tenant, active same-Tenant customer/locations, distinct locations, valid window and supported catalogues.
- Prove unknown, inactive and cross-Tenant references fail closed without leakage.
- No assignment-target, reassignment or target-eligibility tests belong to US-56.

## 9. Decision Matrix

| Decision | Final value/rule | Provenance | Owner | US-56 | Implementation impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Priority | `LOW`, `NORMAL`, `HIGH`, `URGENT`; default `NORMAL` | `PRODUCT_DECISION` | Delivery | Required | Enum, checks, selects, tests |
| Priority behavior | Recorded urgency only | `PRODUCT_DECISION` | Delivery | Required | No automatic scheduling/eligibility |
| Service type | `STANDARD`, `EXPRESS`, `SAME_DAY`, `SCHEDULED`; default `STANDARD` | `PRODUCT_DECISION` | Delivery | Required | Enum, checks, selects, tests |
| Service behavior | Classification plus explicit window; no implicit cross-scope effects | `PRODUCT_DECISION` | Delivery | Required | No pricing/SLA/POD logic |
| Assignment target | `NONE_IN_US56` | `ARCHITECTURAL_DECISION` | Future contract | No | No request/response/column/selector |
| Assignment eligibility | Deferred; US-56 validates order readiness only | `ARCHITECTURAL_DECISION` | Provider modules later | No | No target ports invoked |
| Unknown references | Fail closed and tenant-safe | `EXISTING_DOMAIN_REUSE` | Tenancy/Organization | Required | Same-Tenant lookup tests |
| Initial/readiness state | `DRAFT`; validated order becomes `READY_FOR_ASSIGNMENT` | `ARCHITECTURAL_DECISION` | Delivery | Required | Explicit readiness command/history |

No implementation-critical product ambiguity remains for US-56.

## 10. Delivery Number Policy Addendum

`MVP-1.3-US56-DELIVERY-NUMBER-POLICY-001` freezes the remaining Delivery-number ambiguity:

- `SERVER_GENERATED`, immutable Delivery-owned `DeliveryNumber`.
- Exact format `DEL-YYYY-NNNNNN` (uppercase, 15 characters, six-digit sequence), prefix `DEL`.
- Counter scope `PER_TENANT_PER_YEAR`, using the authoritative Tenant's local calendar year; starts at 1 and fails after 999999 without wrapping.
- Database-atomic allocation is required; gaps are allowed and consumed values are never reused.
- No explicit US-56 idempotency key; a separate successful client resubmission creates a separate order and number.
- Final guard `UNIQUE (tenant_id, delivery_number)`; collision uses at most three allocation attempts before sanitized failure.
- Create/update requests cannot set the number; create/detail/list responses expose it.
- Persistence uses a Delivery-owned counter keyed by `(tenant_id, calendar_year)`; implementation remains deferred to the R3 production task.

Full provenance, failure and test semantics are recorded in `docs/mvp/MVP-1.3-US56-DELIVERY-NUMBER-POLICY-001.md`.
