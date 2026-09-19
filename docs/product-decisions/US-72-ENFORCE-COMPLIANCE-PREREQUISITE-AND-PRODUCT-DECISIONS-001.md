# US-72 Enforce Compliance Prerequisite and Product Decisions

## Governance status

```text
Decision package: PROPOSED / NOT APPROVED
CS01 inactive domain foundations: AUTHORIZED / COMPLETE
Policy evaluation and production enforcement: NOT AUTHORIZED
Migration: NOT AUTHORIZED OR RESERVED
Permission/API/event activation: NOT AUTHORIZED
Completed stories: 73 / 87
Flyway head at review: V105
```

## Narrow CS01 authorization

`US-72-ENFORCE-COMPLIANCE-CS01-DOMAIN-PORTS-001` is authorized only for inactive,
framework-neutral foundations. This authorization does not approve D1-D11, select a jurisdiction,
designate policy authority, activate a policy, or establish legal, regulatory or tax meaning.

CS01 may define Tenant-qualified policy/version references, opaque jurisdiction/scope references,
effective-time values, the seven structural check identifiers, evidence-state vocabulary, minimized
source-fact references, evaluation request/result structures, decision-effect vocabulary and only those
owner-specific query ports whose current source facts are sufficiently defined.

The following remain explicitly unapproved: mandatory/advisory classification, thresholds, aggregation
precedence, operational enforcement, override/appeal behavior, retention duration, authority assignments,
policy publication, persistence, migrations, APIs, permissions, events and frontend behavior. Missing policy
authority or configuration can produce only an unavailable/unevaluated result; it cannot produce `ALLOW`
or be translated into an operational block without a separately approved integration policy.

Current source-contract review permits structural query ports for Fleet vehicle-document facts,
Fleet-owned Driver eligibility facts, Freight cargo/hazmat facts and Billing supplied tax facts. Regional
operation and retention-disposition source meanings are not sufficiently defined and remain deferred.

This package selects US-72 as the recommended next independent MVP workstream and consolidates the
material decisions that must be approved before implementation. It does not assert any jurisdictional
law, create a Compliance bounded context, or authorize a rule, schema, permission, endpoint, event, or
blocking effect.

## Authoritative intent and current reality

The original requirement is **US-72 — Enforce Compliance**: a Compliance Officer validates vehicle,
driver, cargo, hazmat, tax, regional and retention rules so regulated operations remain compliant.
The source boundary says that the fact-owning domains retain their business data while one Compliance
gate determines whether an operation may proceed, proceed with restrictions, or be blocked. Acceptance
requires applicable rules to be evaluated, policy-directed flagging/blocking, and an auditable decision.

The repository does not currently contain a Compliance bounded context. Existing similarly named code
does not satisfy US-72:

- Fleet owns document, licence and medical records plus expiry notifications. The notification scanner
  warns about expiring facts; it is not a cross-domain decision gate.
- Freight owns manifest commodity/customs/hazardous facts. It exposes no approved US-72 fact contract.
- Billing V72 retains supplied tax facts and a temporary configured compliance adapter. Billing does not
  calculate jurisdictional tax; US-72 remains the intended decision owner.
- Audit, Identity/RBAC, Workflow, Notification, Document and Integration foundations exist, but no
  Compliance policy/evidence lifecycle has been approved or implemented.

## Dependency assessment

US-72 can proceed independently after this package is approved. US-47 now supplies the billing/tax fact
boundary, and US-74, US-75, US-77, US-80, US-81 and US-83 are accepted. Wave A integration foundations
are also accepted. These are satisfied platform dependencies, not blockers.

US-48, US-50, US-51, US-52, US-53, US-54 and US-55 physical/provider acceptance holds are not US-72
dependencies. The recommended Phase-1 catalogue below consumes authoritative record facts, not physical
GPS evidence. No compliance rule may infer facts from unavailable Tracking evidence. A future policy that
explicitly depends on telemetry would require its own approved source contract and acceptance evidence.

US-76 depends on US-72 implementation according to the roadmap; US-76 is not a prerequisite for US-72.
The remaining blocker is qualified policy authority and product approval of the decisions below.

## Decision matrix

| ID | Topic | Existing approved boundary | Recommended Phase-1 choice | Approval still required |
| :--- | :--- | :--- | :--- | :--- |
| D1 | Jurisdictions and authority | No jurisdiction or legal catalogue is approved. Billing stores supplied jurisdiction/tax facts but does not decide tax compliance. | Configure an explicit Tenant policy scope using opaque jurisdiction/policy codes supplied by a named qualified policy authority. Ship no active jurisdictional rules by default. | Product owner must name the initial supported jurisdiction set; qualified regulatory/tax authority must own each rule source, interpretation and effective date. |
| D2 | Ownership | Source domains own facts; a justified `compliance` decision/evidence context may own policy versions, evaluations, decisions, overrides and appeals. No foreign persistence access. | Create a dedicated top-level Compliance module using typed published fact ports. Keep Fleet, Driver/Fleet, Freight, Billing and Document records with their owners. | Approve the dedicated module and the current Fleet ownership of driver compliance facts until any separately governed Driver extraction. |
| D3 | Phase-1 checks | Original scope requires vehicle, driver, cargo, hazmat, tax/billing, regional and retention evaluation. | Freeze the seven typed checks in the catalogue below. A check is executable only when its policy and required fact contract are approved; unavailable checks return `UNKNOWN`, never a guessed pass. | Approve each check, its exact required facts, and the named policy authority. |
| D4 | Decision effects | Original requirement permits proceed, proceed with restrictions, or block; roadmap requires default fail-closed for mandatory controls. | Outcomes: `ALLOW`, `ADVISORY`, `RESTRICT`, `BLOCK`, `UNKNOWN`. Mandatory missing/expired/conflicting/unknown evidence returns `BLOCK`; advisory checks return `ADVISORY` or `UNKNOWN` without blocking. The policy version, not application code, classifies a check as mandatory or advisory. | Approve the effect per check and operation type. Qualified authority must approve every mandatory rule. |
| D5 | Evidence states | No cross-domain state model exists. | Preserve `PRESENT_VALID`, `MISSING`, `EXPIRED`, `CONFLICTING`, `UNKNOWN`, `NOT_APPLICABLE`. `UNKNOWN` is not compliant. `NOT_APPLICABLE` requires an explicit policy applicability result, not absent data. | Approve fail-closed behavior for mandatory checks and the exact conflict resolution authority. |
| D6 | Policy versions | Roadmap requires effective dating and authorized replacement; accepted rollback rule forbids deleting a published version. | Immutable policy versions use half-open `[effectiveFrom,effectiveTo)` UTC intervals, Tenant plus jurisdiction/policy code identity, optimistic versioning, and one effective published version per identity/time. Drafts may be edited; published versions are replaced forward, never rewritten. Evaluations snapshot policy/version and minimized fact identities. | Approve publication authority, scheduling rules, overlap handling and emergency withdrawal procedure. |
| D7 | Overrides, appeal and SoD | RBAC/ABAC/SoD and audit are mandatory; no roles or permissions are approved for US-72. | A reasoned, time-bounded override applies only to an explicitly overrideable `RESTRICT` or `BLOCK`. The evaluator/requester cannot approve their own override. Appeals preserve the original decision and create a separate reviewed disposition. No override may rewrite policy or evidence. | Approve which checks are overrideable, maximum duration, two-person rules, appeal authority and role-to-permission grants. |
| D8 | Audit, retention and privacy | US-75 Audit and US-83 Document exist. Retention duration and legal basis are not approved. | Store minimized decision snapshots, rule/version IDs, fact references/hashes, outcome, reason codes, actors and timestamps. Exclude diagnoses, raw documents, cargo narrative, credentials and unnecessary PII. Evidence attachments remain US-83 logical references. Retention is policy-classified and Tenant-qualified; no purge is implemented until an exact approved duration/hold rule exists. | Qualified authority must approve retention periods, legal holds, erasure restrictions and access roles for each evidence class. |
| D9 | Public contracts and events | No US-72 API/event contract exists. P1-01 is the durable event mechanism. | Provide explicit command/query contracts for policy drafts/publication, evaluation, decision read/search, override request/decision and appeal. Publish only minimized, versioned decision facts after commit when a downstream use is approved. No generic expression language, arbitrary status patch, foreign repository or cross-module SQL. | Approve route family, request/response fields, consumers, event names/payloads, idempotency and error semantics. |
| D10 | Security and UI | Tenant isolation, server-derived identity and backend authorization are mandatory. No US-72 permission catalogue exists. | Separate view, evaluate, policy-manage, override-request, override-approve, appeal-review and audit-view authorities. Use ABAC for operation/check/jurisdiction applicability and enforce SoD server-side. Operator UI shows evidence state, effective policy/version, outcome, restrictions and safe next action without exposing medical detail or raw protected documents. | Approve permission codes, role grants, ABAC attributes, masking and operator workflows. |
| D11 | Acceptance | Roadmap requires typed checks, PostgreSQL, security, architecture, frontend and deterministic acceptance; legal sign-off cannot be simulated. | Require deterministic technical evidence for all outcomes and failures plus qualified authority sign-off on the activated policy catalogue. Controlled fixtures prove software behavior only. Physical GPS/device evidence is not required unless a future approved rule consumes telemetry. | Approve the acceptance matrix, responsible signatories and the boundary between technical PASS and policy certification. |

## Recommended Phase-1 typed check catalogue

No row is active until its policy owner and effects are approved.

| Check code | Fact owner and required minimized source data | Recommended behavior |
| :--- | :--- | :--- |
| `VEHICLE_DOCUMENT_ELIGIBILITY` | Fleet: Tenant/Vehicle, document type, active/verified state, valid-from/to, mandatory-for-operation indicator, immutable fact/version ID. | Evaluate policy-required document types at operation time. Missing, expired or conflicting mandatory evidence blocks; upcoming expiry may be advisory only if policy says so. |
| `DRIVER_ELIGIBILITY` | Current Fleet/Driver boundary: Tenant/Driver, licence class/endorsements, validity interval, suspension/restriction result, medical-fitness result/validity, immutable fact/version IDs. No diagnosis. | Require the approved class/endorsement and valid non-suspended, policy-required fitness evidence. Unknown mandatory evidence blocks. |
| `CARGO_DOCUMENT_ELIGIBILITY` | Freight: Tenant/Freight order or manifest, commodity code, customs applicability/completion result, required document fact references and effective state. | Validate only the approved commodity/document catalogue. Free-text cargo or customs notes are not policy authority. |
| `HAZMAT_ELIGIBILITY` | Freight: Tenant/manifest, hazardous flag/classification, quantity/weight class, declared handling requirements and approved driver/vehicle eligibility fact references. | `hazardous=true` without an approved classification or required eligibility evidence is not compliant. Specific classes and handling rules require qualified authority. |
| `BILLING_TAX_FACT_ELIGIBILITY` | Billing: Tenant/billing record, supplied/not-supplied state, category, jurisdiction, taxable amount, rate/amount, exemption, provenance and immutable snapshot hash. | Validate completeness/consistency against an approved policy; never calculate tax or treat `NOT_SUPPLIED` as exempt/zero-rated. |
| `REGIONAL_OPERATION_ELIGIBILITY` | Operation owner plus approved reference-data owner: Tenant, operation identity/type/time, origin/destination/region codes and explicit policy applicability facts. | Evaluate only configured approved region codes. No GPS-derived jurisdiction, address inference or unapproved external rule feed. |
| `RETENTION_DISPOSITION_ELIGIBILITY` | Record owner/US-83: Tenant, record/evidence class, creation/effective dates, hold status and policy classification. | Decide retain/hold/eligible-for-disposition; never delete foreign records directly. Execution remains with the owning module through a separately approved command. |

## Recommended API, event and persistence boundaries

These are proposals, not authorized contracts.

- API: `/api/v1/compliance/policies`, `/api/v1/compliance/evaluations`,
  `/api/v1/compliance/decisions`, explicit override and appeal command subresources, and bounded
  Tenant-qualified keyset searches. No generic status mutation or expression upload.
- API errors: safe not-found for foreign Tenant identity; explicit policy/fact unavailable,
  concurrency, idempotency-conflict and authorization codes; no medical, tax-document or raw cargo detail.
- Events: one minimized decision-recorded event family and separately approved override/appeal outcomes,
  carried by P1-01 at-least-once delivery with `(tenantId,eventId)` consumer idempotency. No consumer is
  assumed by this package.
- Persistence: a future forward migration may create Tenant-leading policy/version, decision/check-result,
  evidence-reference, override/appeal and command-idempotency structures. Published policy and decision
  evidence are immutable; mutable workflow records use optimistic locking. Exact DDL and the then-current
  next free migration require separate authorization. No migration number is reserved here.
- Cross-module relationships are logical UUID/code references. Compliance uses published ports/events and
  never accesses another module's entity, repository or table.

## Required technical and acceptance evidence

1. Unit/property tests for applicability, every evidence state, effect ordering and immutable version rules.
2. Contract tests for each owner-produced typed fact, including Tenant A/B, effective time and safe absence.
3. PostgreSQL migration/upgrade, constraints, effective-date overlap, idempotency, concurrency and rollback.
4. Literal `/api/v1/...` RBAC, ABAC, SoD, paging, optimistic concurrency, privacy and safe-error tests.
5. P1-01 publication/redelivery and consumer-idempotency tests for every approved event consumer.
6. Architecture/Modulith/table-ownership tests proving no foreign persistence access or internal type leak.
7. Frontend component, accessibility, TypeScript, production build and real Chromium journeys.
8. Deterministic allow/advisory/restrict/block/unknown, replacement, override, appeal and foreign-Tenant
   journeys using labelled controlled policies.
9. Qualified policy-owner review of every activated rule, source citation, version, effective date, effect,
   overrideability and retention class. Technical fixtures are not legal certification.

## Proposed bounded implementation sequence

These identifiers are governed planning labels. Only CS01 has received a narrow authorization and is
complete as inactive structure; CS02–CS07 remain unauthorized proposals.

1. `US-72-ENFORCE-COMPLIANCE-CS01-DOMAIN-PORTS-001` — framework-neutral policy, evidence,
   decision and typed fact contracts; no schema, API, permission or activation.
2. `US-72-ENFORCE-COMPLIANCE-CS02-PERSISTENCE-001` — separately authorized forward migration and
   adapters after exact DDL and current Flyway head are reviewed.
3. `US-72-ENFORCE-COMPLIANCE-CS03-TYPED-FACT-INTEGRATION-001` — owner-published Fleet/Driver,
   Freight, Billing and retention adapters with contract tests.
4. `US-72-ENFORCE-COMPLIANCE-CS04-EVALUATION-LIFECYCLE-001` — effective policy selection,
   typed evaluation, immutable decision evidence and approved effects.
5. `US-72-ENFORCE-COMPLIANCE-CS05-API-RBAC-AUDIT-001` — separately authorized permissions,
   literal APIs, ABAC/SoD, overrides, appeals and audit integration.
6. `US-72-ENFORCE-COMPLIANCE-CS06-FRONTEND-001` — accessible operator policy/decision workflows.
7. `US-72-ENFORCE-COMPLIANCE-CS07-POSTGRES-CONCURRENCY-PERFORMANCE-001` — real PostgreSQL
   concurrency, query plans, recovery and measured performance.
8. Independent technical closure and final acceptance, including qualified policy-owner sign-off.

## Approval gate

The narrow CS01 authorization permits only the inactive structural foundations recorded above. Before
CS02 or any policy evaluation, persistence, integration or production enforcement may execute, the product
owner must approve D1-D11, identify the initial jurisdiction/policy scope and designate qualified
regulatory/tax authority. That authority must approve the Phase-1 catalogue, mandatory/advisory
classification, effects, overrideability and retention rules. Until then, US-72 remains
`IMPLEMENTATION_IN_PROGRESS / CS01_COMPLETE_INACTIVE / POLICY_APPROVAL_PENDING`.

Software implementation after approval does not require a physical GPS device because this Phase-1 package
does not consume telemetry. All existing Tracking production-source and physical-acceptance holds remain
unchanged and cannot be inherited as US-72 acceptance evidence.

## Historical full-package authorization proposal

```text
The following original proposal is retained for traceability. Its full-package conditions were not used
to infer approval from the narrower CS01 authorization:

Approve D1-D11 in
docs/product-decisions/US-72-ENFORCE-COMPLIANCE-PREREQUISITE-AND-PRODUCT-DECISIONS-001.md
and execute the newly proposed first change set:

US-72-ENFORCE-COMPLIANCE-CS01-DOMAIN-PORTS-001

Approval must name the initial supported jurisdiction/policy scope and the qualified regulatory/tax
policy authority. It must approve the seven Phase-1 typed checks, mandatory/advisory classification,
ALLOW/ADVISORY/RESTRICT/BLOCK/UNKNOWN effects, evidence-state behavior, effective-dated immutable
policy versions, override/appeal/segregation-of-duties rules, retention/privacy boundaries, API/event
direction and acceptance responsibilities documented in D1-D11.

For CS01 only, create the framework-neutral Compliance domain model and inbound/outbound ports for
policy identity/version, typed evidence states, evaluation requests, per-check results, aggregate
decisions, overrides/appeals and the seven owner-specific fact contracts. Preserve explicit Tenant and
effective-time identity everywhere. Keep source domains as fact owners. Do not query foreign persistence,
use a generic expression language, or expose medical/raw-document/cargo narrative/tax-document data.

CS01 must not create a migration, permission, REST controller, frontend, event producer, active policy,
role grant, jurisdictional rule or blocking integration. It must not reserve a migration number. Add
domain/contract validation, Tenant-isolation, effective-time, UNKNOWN/NOT_APPLICABLE, privacy and
architecture tests. Record all contracts as approved-but-not-activated and identify any fact-source gap
before subsequent persistence/integration work.

Keep Flyway V105 and accounting 73/87. Preserve every Tracking production-source and physical-acceptance
hold. Stop if the named policy authority, jurisdiction scope or any D1-D11 approval remains absent.
```
