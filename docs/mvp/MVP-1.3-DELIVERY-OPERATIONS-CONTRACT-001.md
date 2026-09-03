# MVP 1.3 Delivery Operations Contract Freeze

**Task:** `MVP-1.3-DELIVERY-OPERATIONS-CONTRACT-001-R2`  
**Date:** 2026-08-28  
**Mode:** SOURCE-FIRST CONTRACT FREEZE  
**Status:** CONTRACT_FROZEN  
**Production implementation:** Not authorized by this document  
**Current migration baseline:** `V45__freight_reporting_permissions.sql`; no `V46` is created or reserved by this task

## 1. Blocker Resolution

The previous `MVP-1.3-DELIVERY-OPERATIONS-CONTRACT-001` task was blocked because the original requirements document was not present in the workspace. The blocker is resolved: `docs/requirements/Traspotation & logistic.docx` is present and readable as a Microsoft Word 2007+ document.

Text was extracted to `/tmp/kilo/mvp13-contract/Traspotation & logistic.txt` for source inspection only. The DOCX remains the primary authority.

## 2. Source Authority

### Primary Product Authority

- `docs/requirements/Traspotation & logistic.docx`
- Extracted source lines used for this freeze:
- Delivery Management feature map: lines 452-492 of the extracted text
- US-56 through US-62 story statements and priorities: lines 1881-1968
- US-56 through US-62 acceptance criteria: lines 2546-2573
- Feature/entity traceability: lines 4061-4088

### Secondary Evidence

- `US-51-US-60-UseCase-Activity-Sequence-Diagrams.md`
- `US-61-US-70-UseCase-Activity-Sequence-Diagrams.md`
- `MVP_ROADMAP.md`
- `docs/mvp/MVP-current-status.md`
- `docs/mvp/MVP-story-traceability-current.md`
- `docs/openapi-contract-inventory.md`
- `central-knowledge-base/01_INTEGRATION_REGISTRY/api_interfaces.md`
- `central-knowledge-base/01_INTEGRATION_REGISTRY/event_contracts.md`
- `central-knowledge-base/02_MODULES_KNOWLEDGE/transportation_and_logistics.md`

### Source Conflicts

| Topic | Source conflict | Decision |
| :--- | :--- | :--- |
| US-60 title | DOCX story title says **Schedule Re-Delivery**; task hint says **Manage Re-Delivery** | Freeze **Schedule Re-Delivery** as the authoritative title. |
| US-62 exception count | DOCX narrative and feature map list six cases, while the story sentence names five: wrong address, refusal, partial delivery, damage, OTP mismatch. Derived UML also includes customer unavailable. | Freeze the explicit source feature map with six exception types: customer unavailable, wrong address, delivery refusal, partial delivery, damaged delivery, OTP mismatch. Record that the story sentence omits customer unavailable. |
| Frontend framework wording | User prompt mentions Refine and Ant Design Pro Components; project governance prohibits adding Refine/Ant Design Pro Components without approval, and current source uses React, TypeScript, Ant Design, TanStack Query, React Hook Form and Zod. | Freeze frontend contract against current project stack. Refine/Ant Design Pro Components are not authorized by this contract. |

## 3. Authoritative Story Contracts

### US-56 — Manage Delivery Orders

- **Actor:** Delivery Manager
- **User story:** As a Delivery Manager, I want delivery priority, service type, time windows and instructions maintained, so that delivery requirements are clear.
- **Priority:** High
- **Related feature:** Delivery Orders
- **Source requirements:** Create Delivery Order; Set Delivery Priority; Define Service Type; Define Delivery Window; Record Delivery Instructions.
- **Source acceptance criteria:** Delivery Orders store service type, window, priority and instructions. Required customer/location data is validated. Invalid orders cannot progress to assignment.
- **Scope boundary:** US-56 defines what must be delivered, with what priority/service conditions, and when. POD, failed attempts, re-delivery, and analytics are outside US-56.

### US-57 — Capture Proof of Delivery

- **Actor:** Rider
- **User story:** As a Rider, I want to capture signature, photo, barcode, timestamp and geo-tag evidence, so that delivery completion can be proven.
- **Priority:** High
- **Related feature:** POD
- **Source requirements:** Capture Digital Signature; Capture Delivery Photo; Scan Barcode; Capture Timestamp; Capture Geo-Tag.
- **Source acceptance criteria:** POD supports configured signature/photo/barcode evidence. Timestamp and geo-tag are captured where available. Required proof must validate before successful completion.
- **Scope boundary:** Signature, photo and barcode are configurable evidence types. Timestamp, geo-tag where available, validation, confirmation and saving form the common POD workflow.

### US-58 — Capture Signature and Photo Offline

- **Actor:** Rider
- **User story:** As a Rider, I want offline signature/photo capture with quality, retake and consent controls, so that proof can still be collected without connectivity.
- **Priority:** High
- **Related feature:** Signatures/Photos
- **Source requirements:** Capture Signature Offline; Capture Photo Offline; Validate Image Quality; Request Photo Retake; Record Customer Consent.
- **Source acceptance criteria:** Offline proof is securely queued. Photo quality may require retake. Consent information is retained where configured.
- **Scope boundary:** US-57 defines overall POD. US-58 owns offline capture and synchronization behavior for signatures and photos only. It must reuse US-71 offline synchronization rather than creating a second offline framework.

### US-59 — Manage Failed Deliveries

- **Actor:** Delivery Manager
- **User story:** As a Delivery Manager, I want failure reason, escalation, contact attempts and RTO tracked, so that unsuccessful deliveries have a controlled outcome.
- **Priority:** High
- **Related feature:** Failures
- **Source requirements:** Record Failure Reason; Escalate Failed Delivery; Record Contact Attempts; Initiate Return to Origin.
- **Source acceptance criteria:** Failed attempts require reason. Contact attempts and escalation are retained. RTO status remains distinguishable from completed delivery.
- **Scope boundary:** US-59 records why the delivery failed and decides what should happen next. Re-delivery scheduling mechanics belong to US-60.

### US-60 — Schedule Re-Delivery

- **Actor:** Delivery Manager
- **User story:** As a Delivery Manager, I want customer preference and slot availability considered during auto or agent rescheduling, so that another delivery attempt can be planned.
- **Priority:** High
- **Related feature:** Re-Delivery
- **Source requirements:** Record Customer Preference; Check Delivery Slot Availability; Auto-Reschedule Delivery; Agent-Reschedule Delivery.
- **Source acceptance criteria:** Rescheduling checks customer preference and capacity. Auto/agent rescheduling remain traceable. Over-capacity slot is rejected.
- **Scope boundary:** US-59 determines that another attempt is needed. US-60 decides when the next attempt happens. US-64 Delivery Slots is not implemented by US-60; US-60 consumes slot/capacity availability through a port until Last-Mile slot capability is implemented.

### US-61 — Analyze Delivery Performance

- **Actor:** Delivery Manager
- **User story:** As a Delivery Manager, I want success rates, delays, attempts and regional performance analyzed, so that delivery operations can be improved.
- **Priority:** Medium
- **Related feature:** Delivery Analytics
- **Source requirements:** Calculate Delivery Success Rate; Analyze Delivery Delay; Track Delivery Attempts; Analyze Regional Performance.
- **Source acceptance criteria:** Success, delays, attempts and regional metrics derive from delivery records. Incomplete attempts are handled consistently. Analytics does not modify delivery outcomes.
- **Scope boundary:** US-56 through US-60 generate operational records. US-61 is read-only analysis over those records. Source does not provide exact formulas, numerator/denominator definitions, or incomplete-attempt formula treatment; those formulas require an implementation-level product/architecture decision before coding.

### US-62 — Handle Delivery Exceptions

- **Actor:** Delivery Manager
- **User story:** As a Delivery Manager, I want wrong address, refusal, partial delivery, damage and OTP mismatch handled, so that exceptional delivery outcomes are recorded correctly.
- **Priority:** High
- **Related feature:** Delivery Edge Cases
- **Source requirements:** Handle Customer Unavailable; Handle Wrong Address; Handle Delivery Refusal; Handle Partial Delivery; Handle Damaged Delivery; Handle OTP Mismatch.
- **Source acceptance criteria:** Wrong address/refusal/partial/damage states are separate. OTP mismatch prevents normal POD completion. Resolved exception retains prior attempts.
- **Scope boundary:** US-62 owns delivery-specific exception recording, status, outcome and history. US-30 Cargo Exceptions, US-15 Trip Exceptions and US-23 Route Disruptions remain separate bounded capabilities.

## 4. Delivery Feature Source Model

| Story | Feature | Source requirements | Source acceptance criteria |
| :--- | :--- | :--- | :--- |
| US-56 | Delivery Orders | Create order; set priority; define service type/window; record instructions | Store service type/window/priority/instructions; validate customer/location; invalid orders cannot progress to assignment |
| US-57 | POD | Signature; photo; barcode; timestamp; geo-tag | Configured evidence; timestamp/geo-tag where available; required proof validates before completion |
| US-58 | Signatures/Photos | Offline signature/photo; quality; retake; consent | Securely queue offline proof; retake may be required; retain consent where configured |
| US-59 | Failures | Failure reason; escalation; contact attempts; RTO | Failure reason required; contacts/escalation retained; RTO distinguishable from completed delivery |
| US-60 | Re-Delivery | Customer preference; slot availability; auto/agent reschedule | Check preference/capacity; trace auto/agent scheduling; reject over-capacity slot |
| US-61 | Delivery Analytics | Success rate; delay; attempts; regional performance | Derive metrics from delivery records; handle incomplete attempts consistently; analytics is read-only |
| US-62 | Delivery Edge Cases | Customer unavailable; wrong address; refusal; partial; damage; OTP mismatch | Separate exception states; OTP mismatch blocks normal POD completion; resolved exception retains prior attempts |

## 5. Bounded Context and Module Ownership

**Decision:** Create a dedicated Spring Modulith business module named `delivery` when implementation begins.

Rationale: Delivery Operations has its own source-defined lifecycle, actors, aggregate candidates, POD/evidence policy, failed-attempt handling, re-delivery scheduling and analytics. It is not a sub-feature of Trip, Freight or Routing. It references those modules but does not belong inside them.

Required conceptual package structure:

```text
delivery/
  domain/
  application/
  ports/
    inbound/
    outbound/
  adapters/
    inbound/
      web/
    outbound/
      persistence/
      integration/
```

The domain and application layers must not depend on Spring MVC, JPA/Hibernate, Jackson, HTTP, React, storage providers, or another module's JPA entities/repositories/internal services.

## 6. Domain Model

### Aggregates

| Aggregate | Source basis | Ownership and invariants |
| :--- | :--- | :--- |
| `DeliveryOrder` | US-56, US-60, US-61 | Tenant-owned root. Stores service type, delivery window, priority, instructions, customer/location references, status, assignment/progression eligibility and version. Invalid order cannot progress to assignment. |
| `DeliveryAttempt` | US-57, US-59, US-60, US-61, US-62 | Attempt-level record for POD, failed attempts, re-delivery scheduling and exception history. Failed attempts require reason. RTO and completed states remain distinct. |
| `ProofOfDelivery` | US-57, US-58 | Captures configured evidence metadata and validation state. Required proof must validate before successful delivery completion. |
| `DeliveryException` | US-62 | Delivery-specific exception case with separate source exception types and retained prior attempts. OTP mismatch prevents normal POD completion. |

### Value Objects and Policies

- `DeliveryNumber`: tenant-scoped business key.
- `DeliveryWindow`: source-backed planned delivery time window.
- `DeliveryPriority`: controlled priority value; exact values require implementation-time product decision if the DOCX does not define a value catalogue.
- `ServiceType`: controlled service type; exact values require implementation-time product decision if the DOCX does not define a value catalogue.
- `DeliveryInstruction`: bounded instruction text.
- `EvidenceRequirement`: configured signature/photo/barcode requirement.
- `EvidenceReference`: metadata/reference to stored signature/photo/barcode evidence.
- `FailureReason`: mandatory for failed attempt; exact catalogue requires implementation-time product decision unless supplied separately.
- `DeliveryExceptionType`: customer unavailable, wrong address, delivery refusal, partial delivery, damaged delivery, OTP mismatch.

## 7. Lifecycle

The DOCX defines business outcomes but not exact enum names. The following lifecycle is an **ARCHITECTURAL DECISION** for implementation consistency and must be reviewed before coding.

| State | Meaning |
| :--- | :--- |
| `DRAFT` | Delivery order exists but is not assignment-ready. |
| `READY_FOR_ASSIGNMENT` | Required customer/location/order data is valid. |
| `ASSIGNED` | Delivery is assigned to an execution actor/resource. |
| `OUT_FOR_DELIVERY` | Delivery attempt is in execution. |
| `DELIVERED` | Required POD is valid and completion is confirmed. |
| `FAILED` | Attempt failed with mandatory failure reason and retained contact/escalation/RTO data. |
| `REDELIVERY_SCHEDULED` | Next attempt is scheduled after capacity/preference validation. |
| `RETURN_TO_ORIGIN` | Failed delivery is routed to RTO and remains distinct from completion. |
| `CANCELLED` | Delivery is cancelled by authorized administrative action. |

### Transition Matrix

| Transition | Actor | Command | Preconditions | Result | Audit behavior | Failure behavior |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Create order | Delivery Manager | `createDeliveryOrder` | Customer/location/service/window/priority/instructions pass validation | `DRAFT` or `READY_FOR_ASSIGNMENT` | Record creation | Return validation error; no order created |
| Update order | Delivery Manager | `updateDeliveryOrder` | Not delivered/cancelled; optimistic version matches | Updated order | Record change | 409 on stale version; validation errors on invalid data |
| Validate readiness | Delivery Manager | `validateDeliveryOrder` | Required customer/location data exists | `READY_FOR_ASSIGNMENT` | Record readiness decision | Invalid orders cannot progress to assignment |
| Assign | Delivery Manager | `assignDelivery` | `READY_FOR_ASSIGNMENT`; assignment target eligible through port | `ASSIGNED` | Record assignment | Reject invalid transition/target |
| Start attempt | Rider | `startDeliveryAttempt` | `ASSIGNED` | `OUT_FOR_DELIVERY` | Record attempt start | Reject invalid transition |
| Capture POD | Rider | `captureProofOfDelivery` | Active attempt; evidence configuration satisfied | POD validation state stored | Record POD/evidence metadata | Missing/invalid evidence rejects completion |
| Complete | Rider | `completeDelivery` | Required POD valid | `DELIVERED` | Record completion | Reject when POD missing/invalid or exception blocks completion |
| Fail attempt | Delivery Manager or Rider as permitted | `recordFailedDelivery` | Active attempt; failure reason supplied | `FAILED` | Record failure, contact attempts, escalation/RTO state | Reject missing reason |
| Schedule re-delivery | Delivery Manager | `scheduleRedelivery` | Failed attempt eligible; preference/capacity valid | `REDELIVERY_SCHEDULED` | Record auto/agent method and slot decision | Reject over-capacity or ineligible request |
| Record exception | Delivery Manager | `recordDeliveryException` | Delivery/attempt exists in same tenant | Exception case retained | Record exception state/history | OTP mismatch blocks normal POD completion |
| Resolve exception | Delivery Manager | `resolveDeliveryException` | Exception open; outcome supplied | Resolved exception retains prior attempts | Record resolution | Reject missing outcome/stale version |
| Cancel | Delivery Manager | `cancelDelivery` | Not delivered; authorized reason | `CANCELLED` | Record cancellation | Reject invalid transition |

## 8. Cross-Module Ports

Delivery must use outbound ports and integration adapters. Direct dependency on another module's JPA entity, repository or internal service is prohibited.

| Port | Provider | Purpose | Notes |
| :--- | :--- | :--- | :--- |
| `DeliveryFreightOrderPort` | Freight | Validate logical freight order reference and obtain immutable delivery-relevant facts where applicable | IDs only; no Freight JPA entity exposure |
| `DeliveryTripPort` | Trip | Validate trip eligibility and associate delivery execution with trip where source-backed | No Trip repository injection |
| `DeliveryRoutePort` | Routing | Validate route/location path where needed | No route mutation by Delivery |
| `DeliveryCustomerPort` | Organization | Validate customer/recipient facts | Required by US-56 customer validation |
| `DeliveryLocationPort` | Organization | Validate origin/destination/delivery location | Required by US-56 location validation |
| `DeliveryDocumentPort` | Document capability / existing document metadata owner | Store or retrieve evidence metadata references | No binary evidence in Delivery JPA entities by default |
| `DeliveryNotificationPort` | Notification | Emit notification requests for internal actor notifications only | US-69 customer notification remains Post-MVP unless explicitly promoted |
| `DeliveryOfflineSyncPort` | Offline Sync | Register/replay offline POD evidence operations | Reuse US-71 semantics |
| `DeliveryActorPort` | Identity/RBAC | Resolve authenticated actor and permissions for audit labels | Role assignment remains tenant-membership scoped |
| `DeliveryTenantPort` | Tenancy | Resolve current tenant from server-side context | No request-supplied tenant authority |
| `DeliverySlotAvailabilityPort` | Future Last-Mile / scheduling capability | Check capacity for re-delivery | Required by US-60; does not implement US-64 |

## 9. Tenancy Contract

All Delivery operational data is tenant-owned. Tenant authority is resolved server-side through `CurrentTenant` / `TenantExecutionContext`.

Never accept tenant authority from request body, query parameter, `X-Tenant-ID`, JWT-only claims, browser storage or frontend state.

Tenant isolation is mandatory for create, detail, list, pagination, count, update, cancellation, direct-ID lookup, POD capture, offline replay, analytics, exports, exceptions, status history and evidence metadata. Cross-tenant direct IDs must return not-found/forbidden without leaking existence.

Every Delivery repository method, query, projection, background job and cache key must include tenant scope. Offline replay must validate the operation tenant against the server-side membership active at processing time.

## 10. RBAC Contract

Permission names follow current project convention: uppercase capability/action strings enforced by Spring Security and exposed to the frontend as non-authoritative visibility metadata.

| Permission | Source-backed actions |
| :--- | :--- |
| `DELIVERY_VIEW` | List/detail Delivery Orders, attempts, POD metadata and exceptions within tenant |
| `DELIVERY_CREATE` | Create Delivery Orders |
| `DELIVERY_UPDATE` | Update Delivery Orders before terminal states |
| `DELIVERY_ASSIGN` | Validate readiness and assign delivery |
| `DELIVERY_EXECUTE` | Start delivery attempt / mark out-for-delivery |
| `DELIVERY_POD_CAPTURE` | Capture POD signature/photo/barcode/timestamp/geo-tag metadata |
| `DELIVERY_COMPLETE` | Complete delivery after POD validation |
| `DELIVERY_FAIL` | Record failed delivery, contact attempts, escalation and RTO state |
| `DELIVERY_REDELIVER` | Schedule re-delivery using customer preference and capacity |
| `DELIVERY_EXCEPTION_MANAGE` | Create/update/resolve Delivery Exceptions |
| `DELIVERY_REPORT_VIEW` | View Delivery Analytics |

Backend enforcement is mandatory. Frontend route/action visibility is advisory only.

## 11. API Contract

Existing project route families generally use `/api/v1` conventions, with some older modules preserving other paths. Delivery is new and should use `/api/v1/deliveries`.

| Method | Route | Permission | Request | Response | Status / errors | Tenant and concurrency |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/deliveries` | `DELIVERY_VIEW` | Filters: status, customer, date/window, page, size | Page of delivery summaries | 200 | Tenant-scoped query |
| `POST` | `/api/v1/deliveries` | `DELIVERY_CREATE` | Customer/location refs, service type, window, priority, instructions, optional freight/trip refs | Delivery detail | 201; validation errors | Server tenant assigned; version starts at 0 |
| `GET` | `/api/v1/deliveries/{deliveryId}` | `DELIVERY_VIEW` | Path ID | Delivery detail | 200 or tenant-safe not-found | Direct-ID lookup tenant-scoped |
| `PATCH` | `/api/v1/deliveries/{deliveryId}` | `DELIVERY_UPDATE` | Mutable US-56 fields plus `version` | Delivery detail | 200; 409 stale version | Tenant and optimistic version required |
| `POST` | `/api/v1/deliveries/{deliveryId}/validate-readiness` | `DELIVERY_ASSIGN` | `version` | Readiness result/detail | 200; validation errors | Invalid order cannot progress |
| `POST` | `/api/v1/deliveries/{deliveryId}/assign` | `DELIVERY_ASSIGN` | Assignment target, note, `version` | Delivery detail | 200; invalid transition | Assignment target through public port |
| `POST` | `/api/v1/deliveries/{deliveryId}/start-attempt` | `DELIVERY_EXECUTE` | Attempt context, `version` | Attempt detail | 200; invalid transition | Tenant-scoped attempt |
| `POST` | `/api/v1/deliveries/{deliveryId}/pod` | `DELIVERY_POD_CAPTURE` | Configured evidence references/metadata, timestamp/geo-tag where available, consent where configured, `version` | POD detail | 200/201; invalid proof | Does not expose public evidence URLs |
| `POST` | `/api/v1/deliveries/{deliveryId}/complete` | `DELIVERY_COMPLETE` | POD confirmation, `version` | Delivery detail | 200; `POD_REQUIRED`/`INVALID_POD` | Required proof validated first |
| `POST` | `/api/v1/deliveries/{deliveryId}/fail` | `DELIVERY_FAIL` | Failure reason, contact attempts, escalation/RTO data, `version` | Failed attempt detail | 200; missing reason | RTO distinct from completion |
| `POST` | `/api/v1/deliveries/{deliveryId}/redeliveries` | `DELIVERY_REDELIVER` | Customer preference, requested slot, auto/agent method, `version` | Scheduled re-delivery detail | 201; over-capacity/ineligible | Capacity via slot port |
| `POST` | `/api/v1/deliveries/{deliveryId}/exceptions` | `DELIVERY_EXCEPTION_MANAGE` | Exception type, details, optional attempt/evidence refs, `version` | Exception detail | 201; invalid type | OTP mismatch blocks normal completion |
| `PATCH` | `/api/v1/deliveries/{deliveryId}/exceptions/{exceptionId}` | `DELIVERY_EXCEPTION_MANAGE` | Outcome/resolution, `version` | Exception detail | 200; 409 stale version | Retain prior attempts |
| `GET` | `/api/v1/deliveries/reports/performance` | `DELIVERY_REPORT_VIEW` | Period, region/location, service type | Analytics response with incomplete markers | 200 | Read-only tenant-scoped aggregation |

Business error codes must follow the existing API error envelope. Frozen candidate codes: `DELIVERY_NOT_FOUND`, `INVALID_DELIVERY_TRANSITION`, `DELIVERY_VALIDATION_FAILED`, `DELIVERY_ALREADY_COMPLETED`, `POD_REQUIRED`, `INVALID_POD`, `FAILED_DELIVERY_REASON_REQUIRED`, `REDELIVERY_NOT_ALLOWED`, `DELIVERY_SLOT_OVER_CAPACITY`, `DELIVERY_EXCEPTION_NOT_FOUND`, `OTP_MISMATCH_BLOCKS_COMPLETION`, `DELIVERY_VERSION_CONFLICT`, `DELIVERY_REFERENCE_NOT_ELIGIBLE`.

## 12. Events

Delivery events are provider-neutral domain/application events. They must use deterministic tenant identity and stable IDs and must not expose JPA entities.

### Required Internal Business Events

- `DeliveryOrderCreated`
- `DeliveryOrderReadyForAssignment`
- `DeliveryAssigned`
- `DeliveryAttemptStarted`
- `ProofOfDeliveryCaptured`
- `DeliveryCompleted`
- `DeliveryFailed`
- `RedeliveryScheduled`
- `DeliveryExceptionRecorded`
- `DeliveryExceptionResolved`
- `DeliveryCancelled`

### Optional Integration Events

External publication is not active until registered in `central-knowledge-base/01_INTEGRATION_REGISTRY/event_contracts.md` with exact envelope, payload, consumers, idempotency and retention. Likely future families: delivery status updates, proof-of-delivery captured, failed delivery/RTO, re-delivery scheduled.

## 13. Database Conceptual Plan

No Flyway migration is created by this contract. The next implementation must re-check migration order before creating any `V46` migration.

| Table | Ownership | Tenant | Key constraints and indexes |
| :--- | :--- | :--- | :--- |
| `delivery_order` | Delivery aggregate root | `tenant_id UUID NOT NULL` | `id UUID PK`; tenant-scoped unique `delivery_number`; logical UUID refs to freight order/trip/customer/locations; service type, window, priority, instructions; status; `version`; audit columns; indexes on tenant/status/window/customer |
| `delivery_attempt` | Attempt entity under Delivery | `tenant_id UUID NOT NULL` | `id UUID PK`; `delivery_order_id` internal FK; attempt number unique per delivery; status/outcome; started/completed/failed timestamps; failure reason/contact/escalation/RTO fields; `version`; audit; tenant/order indexes |
| `proof_of_delivery` | POD entity under attempt | `tenant_id UUID NOT NULL` | `id UUID PK`; internal FK to attempt; evidence validation state; captured timestamp; geo-tag fields nullable; consent metadata where configured; `version`; audit; unique active POD per attempt if required |
| `delivery_evidence` | Evidence metadata/reference | `tenant_id UUID NOT NULL` | `id UUID PK`; internal FK to POD; evidence type signature/photo/barcode; storage reference/document ID; hash/checksum; content metadata; no public URL by default; audit; indexes by POD/type |
| `delivery_exception` | Delivery exception entity | `tenant_id UUID NOT NULL` | `id UUID PK`; internal FK to delivery/attempt; exception type; severity/status/outcome; resolution; `version`; audit; indexes on tenant/type/status |
| `delivery_status_history` | Immutable audit/history | `tenant_id UUID NOT NULL` | `id UUID PK`; delivery/attempt/exception refs; from/to status; action; actor; reason/details; occurred_at; tenant/time indexes |

Cross-module references remain UUID primitives with logical FK documentation only. No physical foreign key to Freight, Trip, Route, Organization, Notification, Offline Sync or Document tables is approved by this contract.

## 14. POD Storage and Privacy

Delivery owns POD business metadata and validation state. Binary signature/photo files should not be stored as large JPA entity blobs. Store evidence via metadata/reference using an existing Document/storage public contract when available.

Evidence access rules:

- Evidence references are tenant-scoped.
- Evidence URLs are not public by default.
- View/download requires `DELIVERY_VIEW` plus implementation-specific evidence access decision where needed.
- POD capture requires `DELIVERY_POD_CAPTURE`.
- Evidence changes are audited with actor, timestamp, evidence type, storage reference and hash/checksum.
- Retention policy is an open product/security decision unless the Document subsystem supplies an approved policy.
- Recipient names, signatures, photos, geo-tags and timestamps are privacy-sensitive operational data.

## 15. Offline Contract

US-58 must reuse US-71 Offline Synchronization. No delivery-specific offline framework is approved.

Required offline semantics:

- Client supplies a stable operation ID/idempotency key for each offline POD capture.
- Payload includes delivery/attempt identifiers, evidence metadata, source timestamp, evidence hash/checksum, consent metadata where configured and no trusted client tenant authority.
- Server resolves tenant from authenticated membership at sync time and validates that target delivery/attempt belongs to the same tenant.
- Duplicate operation IDs return prior acknowledgement.
- Stale delivery version returns conflict without overwriting delivered/failed/re-delivery state.
- Upload retry is safe and idempotent.
- Evidence integrity is validated through hash/checksum and metadata consistency.
- Server acknowledgement includes accepted/rejected/conflict result and authoritative IDs.

## 16. Analytics Contract

US-61 source-backed metrics:

- Delivery success rate
- Delivery delays
- Delivery attempts
- Regional performance

Source-backed rules:

- Metrics derive from delivery records.
- Incomplete attempts are handled consistently.
- Analytics does not modify delivery outcomes.

Formula decisions not present in source and still required before implementation:

- Success-rate numerator/denominator.
- Delay basis: delivery window end, promised time, planned arrival, actual completion or another source.
- Attempt counting for re-delivery chains.
- Regional dimension source: destination location, customer region, route region or configured operational region.
- INCOMPLETE/UNKNOWN behavior for missing timestamps, missing region, open attempts and cancelled/RTO outcomes.

Until those decisions are made, analytics endpoints must label incomplete source data and must not infer missing facts.

## 17. Exception Contract

US-62 exception types:

- Customer unavailable
- Wrong address
- Delivery refusal
- Partial delivery
- Damaged delivery
- OTP mismatch

Boundary rules:

- US-62 Delivery Exceptions are not US-59 Failed Deliveries; a failed delivery may create or reference a delivery exception, but failure recording remains distinct.
- US-62 does not schedule the next attempt; re-delivery scheduling belongs to US-60.
- Damaged delivery is not a US-30 Cargo Exception unless source cargo/manifest facts require a separate Freight-side cargo exception through a port/event.
- Trip cancellation/rejection remains US-15 Trip Exception behavior.
- Route obstruction/disruption remains US-23 Routing behavior.
- OTP mismatch prevents normal POD completion until resolved or an authorized alternate outcome is recorded.

## 18. Frontend Contract

Implementation must use current project stack and layout rules: React, TypeScript, Ant Design, TanStack Query, React Hook Form, Zod, shared Axios client and `AppLayout` as the single shell owner. Refine and Ant Design Pro Components are not approved dependencies.

Minimum workflows:

- Delivery list with tenant-scoped server pagination/filtering and permission guard.
- Delivery create/edit form for US-56 fields and reference lookups.
- Delivery details with status, attempts, POD, failures, re-delivery and exception sections.
- Assignment/readiness action UI guarded by backend permissions.
- Rider execution/POD capture workflow with configurable evidence requirements.
- Offline POD queue/status UI only through US-71 sync semantics.
- Failed delivery form with mandatory reason, contact attempts, escalation and RTO status.
- Re-delivery scheduling form using customer preference and capacity lookup.
- Delivery analytics page with source-data incomplete markers.
- Delivery exception intake/resolution workflow.

## 19. Acceptance Traceability Matrix

| Story | Source AC | Domain behavior | API | RBAC | Persistence | Frontend | Future test evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| US-56 | Store service type/window/priority/instructions | DeliveryOrder invariant | Create/update/detail | `DELIVERY_CREATE`, `DELIVERY_UPDATE`, `DELIVERY_VIEW` | `delivery_order` | Create/edit/details | Domain + controller validation + E2E |
| US-56 | Validate required customer/location data | Customer/location ports required | Create/update/readiness | `DELIVERY_CREATE`, `DELIVERY_ASSIGN` | logical refs only | Reference validation errors | Application port tests |
| US-56 | Invalid orders cannot progress to assignment | Readiness policy blocks assign | Validate/assign | `DELIVERY_ASSIGN` | status/history | Assignment disabled/error | Domain transition tests |
| US-57 | Configured signature/photo/barcode evidence | EvidenceRequirement policy | POD capture | `DELIVERY_POD_CAPTURE` | POD/evidence metadata | Evidence inputs | POD validation tests |
| US-57 | Timestamp and geo-tag captured where available | POD metadata | POD capture | `DELIVERY_POD_CAPTURE` | POD fields | Display metadata | API and frontend tests |
| US-57 | Required proof validates before completion | Completion precondition | Complete | `DELIVERY_COMPLETE` | status/history | Completion blocked | Domain lifecycle tests |
| US-58 | Offline proof securely queued | Offline operation model | Offline sync replay | `DELIVERY_POD_CAPTURE` | offline + POD metadata | Queue status | Idempotency tests |
| US-58 | Photo quality may require retake | Client/server validation policy | POD/offline capture | `DELIVERY_POD_CAPTURE` | evidence metadata | Retake prompt | Frontend + API validation |
| US-58 | Consent retained where configured | Consent metadata policy | POD/offline capture | `DELIVERY_POD_CAPTURE` | POD/evidence metadata | Consent input/display | Privacy/security tests |
| US-59 | Failed attempts require reason | Failure invariant | Fail command | `DELIVERY_FAIL` | attempt/history | Required reason form | Domain/API validation |
| US-59 | Contact attempts and escalation retained | Failure record | Fail/update failure | `DELIVERY_FAIL` | attempt fields/history | Failure details | Persistence tests |
| US-59 | RTO distinct from completed delivery | State policy | Fail/RTO | `DELIVERY_FAIL` | status/history | RTO state display | Lifecycle tests |
| US-60 | Preference and capacity checked | Scheduling policy + slot port | Redelivery create | `DELIVERY_REDELIVER` | attempt/order status | Scheduling form | Application port tests |
| US-60 | Auto/agent rescheduling traceable | Scheduling method audit | Redelivery create | `DELIVERY_REDELIVER` | history/audit | Method display | Audit tests |
| US-60 | Over-capacity slot rejected | Capacity validation | Redelivery create | `DELIVERY_REDELIVER` | no invalid schedule | Capacity error | Domain/API tests |
| US-61 | Metrics derive from delivery records | Read-only projection | Report query | `DELIVERY_REPORT_VIEW` | read projection/query | Analytics page | Query tests |
| US-61 | Incomplete attempts handled consistently | Incomplete semantics | Report query | `DELIVERY_REPORT_VIEW` | report model | Incomplete markers | Analytics tests |
| US-61 | Analytics does not modify outcomes | Read-only use case | Report query | `DELIVERY_REPORT_VIEW` | no writes | Read-only page | Non-mutation tests |
| US-62 | Exception states separate | Exception type invariant | Exception create | `DELIVERY_EXCEPTION_MANAGE` | exception table | Type-specific form | Domain tests |
| US-62 | OTP mismatch prevents normal POD completion | Completion guard | Exception/create complete | `DELIVERY_EXCEPTION_MANAGE`, `DELIVERY_COMPLETE` | exception/history | Blocking state | Lifecycle/API tests |
| US-62 | Resolved exception retains prior attempts | History invariant | Resolve exception | `DELIVERY_EXCEPTION_MANAGE` | attempt + exception history | History view | Persistence/E2E tests |

## 20. Test Contract

Future implementation must include:

- Domain tests for DeliveryOrder readiness, POD validation, failed delivery, re-delivery, exception types and lifecycle transitions.
- Application service tests for all commands, cross-module ports, event publishing and audit behavior.
- Persistence tests for tenant scoping, optimistic versioning, logical references, status history and evidence metadata.
- API tests for validation, error envelope, status codes, business errors, concurrency and tenant-safe direct-ID lookup.
- Security tests for every Delivery permission.
- Tenant A/B isolation tests for list/detail/update/POD/offline/analytics/exception queries.
- Offline/idempotency tests for duplicate operation IDs, stale versions, hash mismatch and retry acknowledgement.
- POD/evidence privacy tests for authorization and absence of public evidence URLs.
- Analytics tests for read-only behavior and incomplete-source reporting.
- Frontend Vitest tests for permission visibility, forms and error mapping.
- Chromium Playwright E2E for the full US-56 to US-62 happy paths and critical negative paths.
- Architecture tests for module boundaries and hexagonal layering.
- PostgreSQL integration tests planned; known Testcontainers Docker API mismatch remains an environment/tooling risk, not a product acceptance failure.

## 21. Implementation Slices

1. `MVP-1.3-DELIVERY-MODULE-FOUNDATION-001` — create `delivery` module skeleton, ports, permissions seed plan, domain primitives, no broad UI.
2. `MVP-1.3-US56-DELIVERY-ORDER-001` — implement Delivery Orders, tenant-scoped persistence, readiness validation, list/detail/create/update UI and tests.
3. `MVP-1.3-US57-PROOF-OF-DELIVERY-001` — implement POD metadata/evidence references, validation and completion command.
4. `MVP-1.3-US58-OFFLINE-POD-001` — integrate offline signature/photo capture with US-71 idempotent sync and evidence integrity.
5. `MVP-1.3-US59-FAILED-DELIVERY-001` — implement failed-attempt reason/contact/escalation/RTO behavior.
6. `MVP-1.3-US60-REDELIVERY-001` — implement re-delivery scheduling with customer preference and capacity port.
7. `MVP-1.3-US61-DELIVERY-ANALYTICS-001` — implement read-only analytics after formula decisions are approved.
8. `MVP-1.3-US62-DELIVERY-EXCEPTIONS-001` — implement delivery-specific exception cases and resolution history.
9. `MVP-1.3-DELIVERY-OPERATIONS-CLOSURE-001` — full regression, E2E, roadmap/status closure and knowledge-base synchronization.

## 22. Open Decisions

- Exact Delivery priority value catalogue.
- Exact Delivery service type value catalogue.
- Assignment target model: Rider/Courier, driver, vehicle, trip dispatch actor or another source-defined resource.
- Exact POD evidence configuration authority and default required evidence per tenant/use case.
- Evidence storage provider and retention schedule.
- Barcode payload format and validation authority.
- Geo-tag precision, privacy masking and retention.
- Failure reason catalogue.
- Escalation owner/routing semantics for failed deliveries.
- RTO workflow boundary if it affects Trip/Freight inventory or customer notifications.
- Re-delivery slot provider while US-64 remains Post-MVP.
- US-61 analytics formulas and incomplete-data treatment.
- Whether Delivery status update notifications remain internal only or promote any US-69 customer notification behavior.

## 23. Documentation / Governance Effects

- `MVP_ROADMAP.md` should move MVP 1.3 Delivery Operations from `SELECTED / CONTRACT PENDING` to `SELECTED / CONTRACT_FROZEN` without marking US-56 through US-62 complete.
- `docs/mvp/MVP-current-status.md` should record MVP 1.3 as contract frozen and set the next implementation task to `MVP-1.3-DELIVERY-MODULE-FOUNDATION-001`.
- No production code, migration, controller, entity, frontend route or tests are implemented by this task.

## 24. US-56 Product Decision Addendum

**Authority:** `MVP-1.3-US56-PRODUCT-DECISIONS-001`  
**Status:** FROZEN — US-56 is `READY_FOR_IMPLEMENTATION`  
**Nature:** The DOCX supplies the requirements; the catalogues and boundary decisions below are explicitly product/architectural decisions, not source quotations.

- `PRODUCT_DECISION`: Delivery-owned priority catalogue is `LOW`, `NORMAL`, `HIGH`, `URGENT`; create default is `NORMAL`. Priority records urgency only and has no automatic US-56 scheduling, pricing or eligibility effect.
- `PRODUCT_DECISION`: Delivery-owned service-type catalogue is `STANDARD`, `EXPRESS`, `SAME_DAY`, `SCHEDULED`; create default is `STANDARD`. Types require an explicit valid window but do not imply priority, pricing, route, SLA, POD, re-delivery or assignment rules.
- `ARCHITECTURAL_DECISION`: Assignment target is `NONE_IN_US56`. US-56 validates order readiness but does not assign Rider/Courier, Driver, Vehicle, Trip, Route, Delivery Run or a generic target.
- `ARCHITECTURAL_DECISION`: `NO_ASSIGNMENT_COLUMNS_IN_US56`. Assignment identity, reassignment and target eligibility are deferred to a source-backed later contract.
- `EXISTING_DOMAIN_REUSE`: readiness fails closed unless server Tenant context exists and customer/origin/destination are active same-Tenant references. Origin and destination differ; window start precedes end; priority and service type are supported.
- Lifecycle: create as `DRAFT`; readiness validation may produce `READY_FOR_ASSIGNMENT`; changing US-56 requirement fields returns a ready order to `DRAFT`; later assignment/execution states make those fields immutable.

## 25. US-56 Delivery Number Policy Addendum

`MVP-1.3-US56-DELIVERY-NUMBER-POLICY-001` resolves and supersedes the R2 `NEW_IMPLEMENTATION_CRITICAL_PRODUCT_AMBIGUITY` blocker without implementing production behavior.

- Delivery numbers are immutable and server-generated; create/update requests cannot supply or change them, while create/detail/list responses expose them.
- Exact representation is uppercase `DEL-YYYY-NNNNNN`, with a six-digit zero-padded sequence starting at `000001`.
- Allocation is database-atomic and scoped per Tenant and authoritative Tenant-local calendar year. A new year creates a new scope; gaps are allowed and numbers are never reused.
- The sequence fails after `999999` without wrapping. `SELECT MAX(...) + 1` is prohibited.
- US-56 adds no explicit idempotency-key framework. Independent client resubmission is a new create request.
- `UNIQUE (tenant_id, delivery_number)` is the final collision guard; collision recovery is bounded and errors are sanitized.
- Delivery owns the `DeliveryNumber` value object and generator port; R3 owns the forward Flyway migration and infrastructure adapter.

**Previous R2 blocker:** `RESOLVED`  
**US-56 status:** `READY_FOR_IMPLEMENTATION`  
**Next implementation:** `MVP-1.3-US56-DELIVERY-ORDERS-001-R3`
- RBAC remains `DELIVERY_VIEW`, `DELIVERY_CREATE`, `DELIVERY_UPDATE`, and `DELIVERY_ASSIGN` for readiness. No permission is added by this decision.

The complete provenance, API/database/frontend impact, fact classification and test contract are in `docs/mvp/MVP-1.3-US56-PRODUCT-DECISIONS-001.md`.
